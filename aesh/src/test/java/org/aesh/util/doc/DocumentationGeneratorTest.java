/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.util.doc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.HelpEntry;
import org.aesh.command.HelpSectionProvider;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionGroup;
import org.aesh.command.option.OptionList;
import org.aesh.command.option.OptionVisibility;
import org.aesh.command.parser.CommandLineParserException;
import org.junit.Test;

public class DocumentationGeneratorTest {

    @CommandDefinition(name = "deploy", description = "Deploy an application")
    public static class DeployCommand implements Command<CommandInvocation> {
        @Option(shortName = 'e', name = "environment", defaultValue = "dev", description = "Target environment")
        String environment;

        @Option(shortName = 'f', name = "force", hasValue = false, description = "Force deployment")
        boolean force;

        @Option(name = "timeout", defaultValue = "30", description = "Timeout in seconds")
        int timeout;

        @Option(name = "secret", hasValue = false, visibility = OptionVisibility.HIDDEN)
        boolean secret;

        @OptionList(shortName = 't', name = "tags", description = "Deployment tags")
        List<String> tags;

        @OptionGroup(shortName = 'D', description = "Properties")
        Map<String, String> properties;

        @Argument(description = "Application name", required = true)
        String application;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "sub1", description = "First subcommand")
    public static class Sub1Command implements Command<CommandInvocation> {
        @Option(name = "value", description = "A value")
        String value;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "sub2", description = "Second subcommand")
    public static class Sub2Command implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "app", description = "Application CLI", groupCommands = { Sub1Command.class,
            Sub2Command.class })
    public static class AppCommand implements Command<CommandInvocation> {
        @Option(name = "verbose", hasValue = false, description = "Verbose output")
        boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "negtest", description = "Negatable test")
    public static class NegatableCommand implements Command<CommandInvocation> {
        @Option(name = "cds", hasValue = false, negatable = true, description = "Enable CDS")
        boolean cds;

        @Option(name = "opt", aliases = { "o", "option" }, description = "An option")
        String opt;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    // --- AsciiDoc tests ---

    @Test
    public void testAsciidocSimpleCommand() throws CommandLineParserException {
        String doc = DocumentationGenerator.builder()
                .commandClass(DeployCommand.class)
                .format(DocFormat.ASCIIDOC)
                .generateSingle();

        assertNotNull(doc);
        assertTrue("Should contain title", doc.contains("= DEPLOY"));
        assertTrue("Should contain NAME section", doc.contains("== NAME"));
        assertTrue("Should contain SYNOPSIS section", doc.contains("== SYNOPSIS"));
        // DESCRIPTION section is omitted when it would just repeat the one-liner from NAME
        assertFalse("Single-line description should NOT have separate DESCRIPTION section",
                doc.contains("== DESCRIPTION"));
        assertTrue("Should contain OPTIONS section", doc.contains("== OPTIONS"));
        assertTrue("Should contain ARGUMENTS section", doc.contains("== ARGUMENTS"));

        // Options should be rendered
        assertTrue("Should contain --environment", doc.contains("--environment"));
        assertTrue("Should contain -e short name", doc.contains("-e"));
        assertTrue("Should contain --force", doc.contains("--force"));
        assertTrue("Should contain --timeout", doc.contains("--timeout"));
        assertTrue("Should contain --tags", doc.contains("--tags"));
        assertTrue("Should contain description", doc.contains("Target environment"));

        // Default values
        assertTrue("Should contain default value", doc.contains("Default: `dev`"));

        // Hidden options should NOT appear
        assertFalse("Should not contain hidden --secret", doc.contains("--secret"));

        // Arguments
        assertTrue("Should contain application argument", doc.contains("<application>"));
    }

    @Test
    public void testAsciidocGroupCommand() throws CommandLineParserException {
        String doc = DocumentationGenerator.builder()
                .commandClass(AppCommand.class)
                .format(DocFormat.ASCIIDOC)
                .generateSingle();

        assertTrue("Should contain COMMANDS section", doc.contains("== COMMANDS"));
        assertTrue("Should contain sub1 link", doc.contains("sub1"));
        assertTrue("Should contain sub2 link", doc.contains("sub2"));
        assertTrue("Should contain subcommand description", doc.contains("First subcommand"));
        assertTrue("Should contain [COMMAND] in synopsis", doc.contains("[COMMAND]"));

        // Subcommand documentation should be included inline (#483)
        assertTrue("Should contain sub1 title", doc.contains("= APP-SUB1"));
        assertTrue("Should contain sub2 title", doc.contains("= APP-SUB2"));
        assertTrue("Should contain sub1 NAME section", doc.contains("app sub1 -- First subcommand"));
        assertTrue("Should contain sub2 NAME section", doc.contains("app sub2 -- Second subcommand"));
    }

    @Test
    public void testAsciidocCrossRefPrefix() throws CommandLineParserException {
        String doc = DocumentationGenerator.builder()
                .commandClass(AppCommand.class)
                .format(DocFormat.ASCIIDOC)
                .crossRefPrefix("myapp:cli:")
                .generateSingle();

        assertTrue("Should contain Antora xref", doc.contains("xref:myapp:cli:app-sub1.adoc"));
    }

    @Test
    public void testAsciidocNegatableAndAliases() throws CommandLineParserException {
        String doc = DocumentationGenerator.builder()
                .commandClass(NegatableCommand.class)
                .format(DocFormat.ASCIIDOC)
                .generateSingle();

        assertTrue("Should show negatable format", doc.contains("--[no-]cds"));
        assertTrue("Should show aliases", doc.contains("`--o`"));
        assertTrue("Should show aliases", doc.contains("`--option`"));
    }

    @Test
    public void testAsciidocOptionGroup() throws CommandLineParserException {
        String doc = DocumentationGenerator.builder()
                .commandClass(DeployCommand.class)
                .format(DocFormat.ASCIIDOC)
                .generateSingle();

        assertTrue("Should contain -D option", doc.contains("-D"));
        assertTrue("Should contain key=value placeholder", doc.contains("<key>=<value>"));
    }

    // --- Markdown tests ---

    @Test
    public void testMarkdownSimpleCommand() throws CommandLineParserException {
        String doc = DocumentationGenerator.builder()
                .commandClass(DeployCommand.class)
                .format(DocFormat.MARKDOWN)
                .generateSingle();

        assertNotNull(doc);
        assertTrue("Should contain title", doc.contains("# DEPLOY"));
        assertTrue("Should contain NAME section", doc.contains("## NAME"));
        assertTrue("Should contain SYNOPSIS section", doc.contains("## SYNOPSIS"));
        assertTrue("Should contain OPTIONS section", doc.contains("## OPTIONS"));
        assertTrue("Should contain code block", doc.contains("```"));
        assertTrue("Should contain --environment", doc.contains("--environment"));
        assertFalse("Should not contain hidden --secret", doc.contains("--secret"));
    }

    @Test
    public void testMarkdownGroupCommand() throws CommandLineParserException {
        String doc = DocumentationGenerator.builder()
                .commandClass(AppCommand.class)
                .format(DocFormat.MARKDOWN)
                .generateSingle();

        assertTrue("Should contain COMMANDS section", doc.contains("## COMMANDS"));
        assertTrue("Should contain markdown link", doc.contains("[**sub1**](app-sub1.md)"));
    }

    // --- File generation tests ---

    @Test
    public void testFileGeneration() throws CommandLineParserException, IOException {
        Path tempDir = Files.createTempDirectory("aesh-doc-test");
        try {
            DocumentationGenerator.builder()
                    .commandClass(AppCommand.class)
                    .format(DocFormat.ASCIIDOC)
                    .outputDir(tempDir.toFile())
                    .generate();

            // Should generate files for parent and children
            assertTrue("Should create app.adoc", new File(tempDir.toFile(), "app.adoc").exists());
            assertTrue("Should create app-sub1.adoc", new File(tempDir.toFile(), "app-sub1.adoc").exists());
            assertTrue("Should create app-sub2.adoc", new File(tempDir.toFile(), "app-sub2.adoc").exists());

            // Verify content
            String appContent = new String(Files.readAllBytes(tempDir.resolve("app.adoc")));
            assertTrue("Parent doc should link to sub1", appContent.contains("app-sub1.adoc"));
        } finally {
            // Cleanup
            Files.walk(tempDir).sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile).forEach(File::delete);
        }
    }

    @Test
    public void testNavFileGeneration() throws CommandLineParserException, IOException {
        Path tempDir = Files.createTempDirectory("aesh-doc-nav-test");
        try {
            File navFile = new File(tempDir.toFile(), "nav.adoc");
            DocumentationGenerator.builder()
                    .commandClass(AppCommand.class)
                    .format(DocFormat.ASCIIDOC)
                    .outputDir(tempDir.toFile())
                    .navFile(navFile)
                    .generate();

            assertTrue("Nav file should exist", navFile.exists());
            String nav = new String(Files.readAllBytes(navFile.toPath()));
            assertTrue("Nav should contain app", nav.contains("app.adoc"));
            assertTrue("Nav should contain sub1", nav.contains("app-sub1.adoc"));
            assertTrue("Nav should contain sub2", nav.contains("app-sub2.adoc"));
        } finally {
            Files.walk(tempDir).sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile).forEach(File::delete);
        }
    }

    @Test
    public void testNavFileWithCrossRefPrefix() throws CommandLineParserException, IOException {
        Path tempDir = Files.createTempDirectory("aesh-doc-xref-test");
        try {
            File navFile = new File(tempDir.toFile(), "nav.adoc");
            DocumentationGenerator.builder()
                    .commandClass(AppCommand.class)
                    .format(DocFormat.ASCIIDOC)
                    .crossRefPrefix("jbang:cli:")
                    .outputDir(tempDir.toFile())
                    .navFile(navFile)
                    .generate();

            String nav = new String(Files.readAllBytes(navFile.toPath()));
            assertTrue("Nav should use xref prefix", nav.contains("xref:jbang:cli:"));
        } finally {
            Files.walk(tempDir).sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile).forEach(File::delete);
        }
    }

    // --- HelpSectionProvider tests ---

    public static class TestHelpProvider implements HelpSectionProvider {
        @Override
        public String getHeader() {
            return "This is a powerful CLI tool for deployment.";
        }

        @Override
        public String getFooter() {
            return "Report bugs to https://github.com/example/deploy/issues";
        }

        @Override
        public Map<String, List<HelpEntry>> getAdditionalSections() {
            Map<String, List<HelpEntry>> sections = new LinkedHashMap<>();
            sections.put("Examples", Arrays.asList(
                    new HelpEntry("deploy --env prod myapp", "Deploy to production"),
                    new HelpEntry("deploy --force myapp", "Force redeploy")));
            return sections;
        }
    }

    @CommandDefinition(name = "helpcmd", description = "Command with help sections", helpSectionProvider = TestHelpProvider.class)
    public static class HelpProviderCommand implements Command<CommandInvocation> {
        @Option(name = "env", description = "Environment")
        String env;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testAsciidocWithHelpSectionProvider() throws CommandLineParserException {
        String doc = DocumentationGenerator.builder()
                .commandClass(HelpProviderCommand.class)
                .format(DocFormat.ASCIIDOC)
                .generateSingle();

        // Header content should appear
        assertTrue("Should contain header", doc.contains("powerful CLI tool"));
        // Footer content should appear
        assertTrue("Should contain footer", doc.contains("Report bugs"));
        // Additional sections should appear
        assertTrue("Should contain EXAMPLES section", doc.contains("EXAMPLES"));
        assertTrue("Should contain example entry", doc.contains("deploy --env prod myapp"));
        assertTrue("Should contain example description", doc.contains("Deploy to production"));
    }

    @Test
    public void testMarkdownWithHelpSectionProvider() throws CommandLineParserException {
        String doc = DocumentationGenerator.builder()
                .commandClass(HelpProviderCommand.class)
                .format(DocFormat.MARKDOWN)
                .generateSingle();

        assertTrue("Should contain header", doc.contains("powerful CLI tool"));
        assertTrue("Should contain footer", doc.contains("Report bugs"));
        assertTrue("Should contain EXAMPLES section", doc.contains("## EXAMPLES"));
        assertTrue("Should contain example entry", doc.contains("deploy --env prod myapp"));
    }

    // --- Variable resolution tests ---

    public static class VariableHelpProvider implements HelpSectionProvider {
        @Override
        public String getHeader() {
            return "${COMMAND-NAME} is a tool for ${ROOT-COMMAND-NAME}";
        }

        @Override
        public String getFooter() {
            return "Run ${COMMAND-FULL-NAME} --help for more info";
        }

        @Override
        public Map<String, List<HelpEntry>> getAdditionalSections() {
            return java.util.Collections.emptyMap();
        }
    }

    @CommandDefinition(name = "varcmd", description = "Variable test", helpSectionProvider = VariableHelpProvider.class)
    public static class VariableCommand implements Command<CommandInvocation> {
        @Option(name = "opt")
        String opt;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testVariableResolutionInHelpSectionProvider() throws CommandLineParserException {
        String doc = DocumentationGenerator.builder()
                .commandClass(VariableCommand.class)
                .format(DocFormat.ASCIIDOC)
                .generateSingle();

        // Variables should be resolved, not appear as literal ${...}
        assertFalse("Should not contain raw ${COMMAND-NAME}", doc.contains("${COMMAND-NAME}"));
        assertFalse("Should not contain raw ${ROOT-COMMAND-NAME}", doc.contains("${ROOT-COMMAND-NAME}"));
        assertTrue("Should contain resolved command name", doc.contains("varcmd is a tool for varcmd"));
    }

    @CommandDefinition(name = "completion", description = "Generate completion script for ${ROOT-COMMAND-NAME}")
    public static class DescriptionVarCommand implements Command<CommandInvocation> {
        @Option(name = "shell", description = "Shell type for ${COMMAND-NAME}")
        String shell;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testVariableResolutionInCommandAndOptionDescription() throws CommandLineParserException {
        String doc = DocumentationGenerator.builder()
                .commandClass(DescriptionVarCommand.class)
                .format(DocFormat.ASCIIDOC)
                .generateSingle();

        // ${ROOT-COMMAND-NAME} in command description should be resolved
        assertFalse("Should not contain raw ${ROOT-COMMAND-NAME}", doc.contains("${ROOT-COMMAND-NAME}"));
        assertTrue("Should contain resolved command name in description",
                doc.contains("Generate completion script for completion"));

        // ${COMMAND-NAME} in option description should be resolved
        assertFalse("Should not contain raw ${COMMAND-NAME} in option", doc.contains("${COMMAND-NAME}"));
        assertTrue("Should contain resolved command name in option description",
                doc.contains("Shell type for completion"));
    }

    // --- Test: ${ROOT-COMMAND-NAME} in child command description listed in parent ---

    @CommandDefinition(name = "rootvar-child", description = "Generate completion for ${ROOT-COMMAND-NAME}")
    public static class RootVarChildCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "rootvar", description = "Root command", groupCommands = { RootVarChildCommand.class })
    public static class RootVarGroupCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testRootCommandNameResolvedInChildDescriptionListing() throws CommandLineParserException {
        String doc = DocumentationGenerator.builder()
                .commandClass(RootVarGroupCommand.class)
                .format(DocFormat.ASCIIDOC)
                .generateSingle();

        // The COMMANDS section lists children with their descriptions.
        // ${ROOT-COMMAND-NAME} in child description should resolve to "rootvar"
        assertFalse("Should not contain raw ${ROOT-COMMAND-NAME}",
                doc.contains("${ROOT-COMMAND-NAME}"));
        assertTrue("Should contain resolved root name in child listing",
                doc.contains("Generate completion for rootvar"));
    }

    // --- Nav file depth tests ---

    @CommandDefinition(name = "leaf", description = "Leaf command")
    public static class LeafCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mid", description = "Mid group", groupCommands = { LeafCommand.class })
    public static class MidGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "top", description = "Top group", groupCommands = { MidGroup.class })
    public static class TopGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testNavFileGrandchildDepth() throws CommandLineParserException, IOException {
        Path tempDir = Files.createTempDirectory("aesh-doc-depth-test");
        try {
            File navFile = new File(tempDir.toFile(), "nav.adoc");
            DocumentationGenerator.builder()
                    .commandClass(TopGroup.class)
                    .format(DocFormat.ASCIIDOC)
                    .outputDir(tempDir.toFile())
                    .navFile(navFile)
                    .generate();

            String nav = new String(Files.readAllBytes(navFile.toPath()));

            // Top level should be *
            assertTrue("Top should be single *", nav.contains("* "));
            // Mid should be **
            assertTrue("Mid should be **", nav.contains("** "));
            // Leaf (grandchild) should be ***
            assertTrue("Leaf should be ***", nav.contains("*** "));
        } finally {
            Files.walk(tempDir).sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile).forEach(File::delete);
        }
    }
}
