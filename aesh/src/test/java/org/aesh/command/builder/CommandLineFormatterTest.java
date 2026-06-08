/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command.builder;

import static org.aesh.terminal.utils.Config.getLineSeparator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aesh.AeshConsoleRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.HelpEntry;
import org.aesh.command.HelpSectionProvider;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.parser.AeshCommandLineParser;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.terminal.utils.ANSI;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * @author Aesh team
 */
public class CommandLineFormatterTest {

    @Test
    public void formatter() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("man").description("[OPTION...]");

        pb.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('d')
                        .name("debug")
                        .description("emit debugging messages")
                        .type(String.class)
                        .build());

        pb.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('D')
                        .name("default")
                        .description("reset all options to their default values")
                        .type(String.class)
                        .build());

        CommandLineParser clp = new AeshCommandLineParser<>(pb.create());

        String help = stripAnsi(clp.printHelp());
        assertTrue(help.contains("--debug=<debug>"));
        assertTrue(help.contains("--default=<default>"));
        assertTrue(help.contains("emit debugging messages"));
        assertTrue(help.contains("reset all options to their default values"));
    }

    @Test
    public void formatter2() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("man").description("[OPTION...]");

        pb.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('d')
                        .name("debug")
                        .description("emit debugging messages")
                        .type(String.class)
                        .build());

        pb.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('D')
                        .name("default")
                        .required(true)
                        .description("reset all options to their default values")
                        .type(String.class)
                        .build());

        pb.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('f')
                        .name("file")
                        .hasValue(true)
                        .argument("filename")
                        .description("set the filename")
                        .type(String.class)
                        .build());

        CommandLineParser clp = new AeshCommandLineParser<>(pb.create());

        String help = stripAnsi(clp.printHelp());
        assertTrue(help.contains("--debug=<debug>"));
        assertTrue(help.contains("--default=<default>"));
        assertTrue(help.contains("--file=<filename>"));
        assertTrue(help.contains("emit debugging messages"));
        assertTrue(help.contains("reset all options to their default values"));
        assertTrue(help.contains("set the filename"));
    }

    @Test
    public void groupFormatter() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> git = ProcessedCommandBuilder.builder()
                .name("git").description("[OPTION...]");
        git.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('h')
                        .name("help")
                        .description("display help info")
                        .type(boolean.class)
                        .build());

        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> rebase = ProcessedCommandBuilder.builder()
                .name("rebase").description("[OPTION...]");
        rebase.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('f')
                        .name("foo")
                        .required(true)
                        .description("reset all options to their default values")
                        .type(String.class)
                        .build());

        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> branch = ProcessedCommandBuilder.builder()
                .name("branch").description("branching");
        branch.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('b')
                        .name("bar")
                        .required(true)
                        .description("reset all options to their default values")
                        .type(String.class)
                        .build());

        CommandLineParser<CommandInvocation> clpGit = new AeshCommandLineParser<>(git.create());
        CommandLineParser<CommandInvocation> clpBranch = new AeshCommandLineParser<>(branch.create());
        CommandLineParser<CommandInvocation> clpRebase = new AeshCommandLineParser<>(rebase.create());

        clpGit.updateAnsiMode(false);
        clpBranch.updateAnsiMode(false);
        clpRebase.updateAnsiMode(false);

        clpGit.addChildParser(clpBranch);
        clpGit.addChildParser(clpRebase);

        String help = clpGit.printHelp();
        assertTrue("Synopsis should contain [-h]", help.contains("[-h]"));
        assertTrue("Synopsis should contain [COMMAND]", help.contains("[COMMAND]"));
        assertTrue("Should list help option", help.contains("--help"));
        assertTrue("Should list branch subcommand", help.contains("branch"));
        assertTrue("Should list rebase subcommand", help.contains("rebase"));

    }

    @Test
    public void testCommandAppearsAfterAllOptions() throws CommandLineParserException {
        // Create a group command with many options to trigger synopsis wrapping (#479)
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> cmd = ProcessedCommandBuilder.builder()
                .name("acp").description("acp tool");
        for (String optName : new String[] { "agent", "agent-binary", "agent-args", "prompt",
                "model", "provider", "request-timeout", "prompt-timeout", "permission-mode",
                "backup", "workspace-path", "skill-path", "log-level" }) {
            cmd.addOption(ProcessedOptionBuilder.builder()
                    .name(optName).description("Option " + optName).type(String.class).build());
        }

        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> sub = ProcessedCommandBuilder.builder()
                .name("registry").description("Registry sub");

        CommandLineParser<CommandInvocation> clpCmd = new AeshCommandLineParser<>(cmd.create());
        CommandLineParser<CommandInvocation> clpSub = new AeshCommandLineParser<>(sub.create());
        clpCmd.updateAnsiMode(false);
        clpSub.updateAnsiMode(false);
        clpCmd.addChildParser(clpSub);

        String help = clpCmd.printHelp();
        // [COMMAND] should appear AFTER the last option in the synopsis, not in the middle
        int commandIdx = help.indexOf("[COMMAND]");
        int lastOptionInSynopsis = help.lastIndexOf("]", commandIdx);
        assertTrue("[COMMAND] should exist in help", commandIdx > 0);
        // Find where the synopsis block ends (blank line)
        String sep = System.lineSeparator();
        int synopsisEnd = help.indexOf(sep + sep);
        assertTrue("[COMMAND] should be within the synopsis block", commandIdx < synopsisEnd);
        // [COMMAND] should be after the last option bracket in the synopsis
        assertTrue("[COMMAND] at " + commandIdx + " should appear after last option ] at " + lastOptionInSynopsis,
                commandIdx > lastOptionInSynopsis);
    }

    @Test
    public void testChildFormatter() throws InterruptedException {
        TestConnection connection = new TestConnection();

        AeshConsoleRunner runner = AeshConsoleRunner.builder().command(BaseCommand.class).connection(connection);
        runner.start();

        connection.read("base git rebase --help" + getLineSeparator());
        // Wait for help output to complete before checking — don't clear buffer
        // as the output may already be partially written on slow CI runners
        String rebaseHelp = connection.waitForOutputContaining("--force", 5000);
        assertTrue(rebaseHelp.contains("--force"));
        assertTrue(rebaseHelp.contains("force your commits"));
        assertTrue(rebaseHelp.contains("--help"));
        assertTrue(rebaseHelp.contains("--test=<test>"));
        assertTrue(rebaseHelp.contains("the branch you want to rebase on"));

        runner.stop();
    }

    @Test
    public void testChildFormatter2() throws InterruptedException {
        TestConnection connection = new TestConnection();

        AeshConsoleRunner runner = AeshConsoleRunner.builder().command(BaseCommand.class).connection(connection);
        runner.start();

        connection.read("base git checkout --help" + getLineSeparator());
        String checkoutHelp = connection.waitForOutputContaining("--quiet", 5000);
        assertTrue(checkoutHelp.contains("--quiet"));
        assertTrue(checkoutHelp.contains("Suppress feedback messages"));
        assertTrue(checkoutHelp.contains("--force"));
        assertTrue(checkoutHelp.contains("--help"));
        assertTrue(checkoutHelp.contains("--test=<test>"));
        assertTrue(checkoutHelp.contains("the branch you want to checkout"));

        runner.stop();
    }

    @Test
    public void testHelpGrouping() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("myapp").description("My application");

        pb.addOption(ProcessedOptionBuilder.builder()
                .name("json").description("Output as JSON").type(boolean.class)
                .helpGroup("Output Format").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("xml").description("Output as XML").type(boolean.class)
                .helpGroup("Output Format").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("user").description("Username").type(String.class)
                .helpGroup("Authentication").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("password").description("Password").type(String.class)
                .helpGroup("Authentication").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("verbose").description("Verbose output").type(boolean.class)
                .build());

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pb.create());

        String help = clp.printHelp();

        // Named groups appear before default "Options:"
        int outputFormatIdx = help.indexOf("Output Format:");
        int authIdx = help.indexOf("Authentication:");
        int optionsIdx = help.indexOf("Options:");

        assertTrue("Output Format group should appear", outputFormatIdx > 0);
        assertTrue("Authentication group should appear", authIdx > 0);
        assertTrue("Options group should appear", optionsIdx > 0);
        assertTrue("Output Format should appear before Authentication", outputFormatIdx < authIdx);
        assertTrue("Authentication should appear before Options", authIdx < optionsIdx);

        // Verify options are under their groups (search after the group heading to skip synopsis)
        assertTrue("--json should appear after Output Format",
                help.indexOf("--json", outputFormatIdx) > outputFormatIdx);
        assertTrue("--xml should appear after Output Format",
                help.indexOf("--xml", outputFormatIdx) > outputFormatIdx);
        assertTrue("--json in body should appear before Authentication",
                help.indexOf("--json", outputFormatIdx) < authIdx);
        assertTrue("--user should appear after Authentication",
                help.indexOf("--user", authIdx) > authIdx);
        assertTrue("--password should appear after Authentication",
                help.indexOf("--password", authIdx) > authIdx);
        assertTrue("--verbose should appear after Options",
                help.indexOf("--verbose", optionsIdx) > optionsIdx);
    }

    @Test
    public void testHelpGroupingNoGroups() throws CommandLineParserException {
        // When no helpGroup is set, all options appear under "Options:" as before
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("simple").description("Simple command");

        pb.addOption(ProcessedOptionBuilder.builder()
                .name("foo").description("Foo option").type(String.class).build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("bar").description("Bar option").type(String.class).build());

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pb.create());

        String help = stripAnsi(clp.printHelp());
        assertTrue(help.contains("--foo=<foo>"));
        assertTrue(help.contains("--bar=<bar>"));
        assertTrue(help.contains("Foo option"));
        assertTrue(help.contains("Bar option"));
    }

    @Test
    public void testHelpGroupingAllGrouped() throws CommandLineParserException {
        // When all options have a helpGroup, no default "Options:" section appears
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("allgrouped").description("All grouped");

        pb.addOption(ProcessedOptionBuilder.builder()
                .name("alpha").description("Alpha").type(String.class)
                .helpGroup("Group A").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("beta").description("Beta").type(String.class)
                .helpGroup("Group B").build());

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pb.create());

        String help = clp.printHelp();
        assertTrue("Group A should appear", help.contains("Group A:"));
        assertTrue("Group B should appear", help.contains("Group B:"));
        assertTrue("Default Options: should not appear when all options are grouped",
                !help.contains("Options:"));
    }

    @Test
    public void testHelpGroupingFromAnnotation() throws CommandLineParserException {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        CommandLineParser<CommandInvocation> clp = builder.create(new GroupedOptionsCommand()).getParser();

        String help = clp.printHelp();

        int outputIdx = help.indexOf("Output:");
        int optionsIdx = help.indexOf("Options:");
        assertTrue("Output section should appear", outputIdx > 0);
        assertTrue("Default Options: should appear", optionsIdx > 0);
        assertTrue("--json should appear after Output:", help.indexOf("--json", outputIdx) > outputIdx);
        assertTrue("--verbose should appear after Options:", help.indexOf("--verbose", optionsIdx) > optionsIdx);
    }

    @Test
    public void testHelpGroupingOptionListFromAnnotation() throws CommandLineParserException {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        CommandLineParser<CommandInvocation> clp = builder.create(new GroupedOptionListCommand()).getParser();

        String help = clp.printHelp();

        int filtersIdx = help.indexOf("Filters:");
        assertTrue("Filters section should appear", filtersIdx > 0);
        assertTrue("--include should appear after Filters:", help.indexOf("--include", filtersIdx) > filtersIdx);
    }

    @CommandDefinition(name = "grouped", description = "Grouped options test")
    public static class GroupedOptionsCommand implements Command<CommandInvocation> {
        @Option(description = "Output as JSON", hasValue = false, helpGroup = "Output")
        boolean json;

        @Option(description = "Output as XML", hasValue = false, helpGroup = "Output")
        boolean xml;

        @Option(description = "Verbose output", hasValue = false)
        boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "filterapp", description = "Filter app")
    public static class GroupedOptionListCommand implements Command<CommandInvocation> {
        @OptionList(description = "Include patterns", helpGroup = "Filters")
        List<String> include;

        @Option(description = "Verbose output", hasValue = false)
        boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testCommandHelpGrouping() throws CommandLineParserException {
        // Test that subcommands with helpGroup are grouped in parent's help output
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> parent = ProcessedCommandBuilder.builder()
                .name("cli").description("CLI tool");

        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> build = ProcessedCommandBuilder.builder()
                .name("build").description("Build the project");
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> test = ProcessedCommandBuilder.builder()
                .name("test").description("Run tests");
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> deploy = ProcessedCommandBuilder.builder()
                .name("deploy").description("Deploy artifacts");
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> info = ProcessedCommandBuilder.builder()
                .name("info").description("Show project info");

        CommandLineParser<CommandInvocation> clpParent = new AeshCommandLineParser<>(parent.create());
        CommandLineParser<CommandInvocation> clpBuild = new AeshCommandLineParser<>(build.create());
        CommandLineParser<CommandInvocation> clpTest = new AeshCommandLineParser<>(test.create());
        CommandLineParser<CommandInvocation> clpDeploy = new AeshCommandLineParser<>(deploy.create());
        CommandLineParser<CommandInvocation> clpInfo = new AeshCommandLineParser<>(info.create());

        // Set helpGroup on subcommands
        clpBuild.getProcessedCommand().setHelpGroup("Development");
        clpTest.getProcessedCommand().setHelpGroup("Development");
        clpDeploy.getProcessedCommand().setHelpGroup("Operations");

        clpParent.addChildParser(clpBuild);
        clpParent.addChildParser(clpTest);
        clpParent.addChildParser(clpDeploy);
        clpParent.addChildParser(clpInfo);

        String help = clpParent.printHelp();

        // Named groups appear first, ungrouped commands under "Other:"
        int devIdx = help.indexOf("Development:");
        int opsIdx = help.indexOf("Operations:");
        int otherIdx = help.indexOf("Other:");

        assertTrue("Development group should appear", devIdx > 0);
        assertTrue("Operations group should appear", opsIdx > 0);
        assertTrue("Other group should appear for ungrouped commands", otherIdx > 0);
        assertTrue("Development should appear before Operations", devIdx < opsIdx);
        assertTrue("Operations should appear before Other", opsIdx < otherIdx);

        // Verify commands are under their groups
        assertTrue("build should appear after Development", help.indexOf("build") > devIdx);
        assertTrue("test should appear after Development", help.indexOf("test") > devIdx);
        assertTrue("deploy should appear after Operations", help.indexOf("deploy") > opsIdx);
        assertTrue("info should appear after Other", help.indexOf("info") > otherIdx);
    }

    @Test
    public void testCommandHelpGroupingNoGroups() throws CommandLineParserException {
        // Without helpGroup, subcommands appear under "<name> commands:" as before
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> parent = ProcessedCommandBuilder.builder()
                .name("app").description("App tool");
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> sub1 = ProcessedCommandBuilder.builder()
                .name("sub1").description("Subcommand 1");
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> sub2 = ProcessedCommandBuilder.builder()
                .name("sub2").description("Subcommand 2");

        CommandLineParser<CommandInvocation> clpParent = new AeshCommandLineParser<>(parent.create());
        clpParent.addChildParser(new AeshCommandLineParser<>(sub1.create()));
        clpParent.addChildParser(new AeshCommandLineParser<>(sub2.create()));

        String help = clpParent.printHelp();

        assertTrue("Should use traditional '<name> commands:' heading",
                help.contains("app commands:"));
        assertTrue("Should not have 'Other:' heading", !help.contains("Other:"));
    }

    @Test
    public void testCommandHelpGroupingFromAnnotation() throws CommandLineParserException {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        CommandLineParser<CommandInvocation> clp = builder.create(ToolGroupCommand.class).getParser();

        String help = clp.printHelp();

        assertTrue("Source Control group should appear", help.contains("Source Control:"));
        assertTrue("Build group should appear", help.contains("Build:"));
        assertTrue("gitcmd should appear after Source Control",
                help.indexOf("gitcmd") > help.indexOf("Source Control:"));
        assertTrue("svncmd should appear after Source Control",
                help.indexOf("svncmd") > help.indexOf("Source Control:"));
        assertTrue("compile should appear after Build",
                help.indexOf("compile") > help.indexOf("Build:"));
    }

    @CommandDefinition(name = "tool", description = "Tool suite", groupCommands = { GitSubCmd.class, SvnSubCmd.class,
            CompileSubCmd.class })
    public static class ToolGroupCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "gitcmd", description = "Git operations", helpGroup = "Source Control")
    public static class GitSubCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "svncmd", description = "SVN operations", helpGroup = "Source Control")
    public static class SvnSubCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "compile", description = "Compile project", helpGroup = "Build")
    public static class CompileSubCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testHelpSectionProviderProgrammatic() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> parent = ProcessedCommandBuilder.builder()
                .name("jbang").description("JBang tool");
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> run = ProcessedCommandBuilder.builder()
                .name("run").description("Run a script");
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> build = ProcessedCommandBuilder.builder()
                .name("build").description("Build a script");

        CommandLineParser<CommandInvocation> clpParent = new AeshCommandLineParser<>(parent.create());
        clpParent.addChildParser(new AeshCommandLineParser<>(run.create()));
        clpParent.addChildParser(new AeshCommandLineParser<>(build.create()));

        clpParent.getProcessedCommand().setHelpSectionProvider(() -> {
            Map<String, List<HelpEntry>> sections = new LinkedHashMap<>();
            sections.put("External", Arrays.asList(
                    new HelpEntry("one", "plugin one"),
                    new HelpEntry("two", "plugin two")));
            return sections;
        });

        String help = clpParent.printHelp();

        assertTrue("External section should appear", help.contains("External:"));
        assertTrue("plugin one should appear", help.contains("one"));
        assertTrue("plugin two should appear", help.contains("two"));
        assertTrue("Built-in commands should still appear", help.contains("run"));
        assertTrue("Built-in commands should still appear", help.contains("build"));
    }

    @Test
    public void testHelpSectionProviderMergesWithExistingGroups() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> parent = ProcessedCommandBuilder.builder()
                .name("cli").description("CLI tool");
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> compile = ProcessedCommandBuilder.builder()
                .name("compile").description("Compile sources");

        CommandLineParser<CommandInvocation> clpParent = new AeshCommandLineParser<>(compile.create());
        CommandLineParser<CommandInvocation> clpCompile = new AeshCommandLineParser<>(
                ProcessedCommandBuilder.<Command<CommandInvocation>, CommandInvocation> builder()
                        .name("compile").description("Compile sources").create());
        clpCompile.getProcessedCommand().setHelpGroup("Build");

        CommandLineParser<CommandInvocation> clpMain = new AeshCommandLineParser<>(parent.create());
        clpMain.addChildParser(clpCompile);

        clpMain.getProcessedCommand().setHelpSectionProvider(() -> {
            Map<String, List<HelpEntry>> sections = new LinkedHashMap<>();
            sections.put("Build", Arrays.asList(
                    new HelpEntry("package", "Package artifacts")));
            sections.put("External", Arrays.asList(
                    new HelpEntry("my-plugin", "Custom plugin")));
            return sections;
        });

        String help = clpMain.printHelp();

        assertTrue("Build group should appear", help.contains("Build:"));
        assertTrue("External group should appear", help.contains("External:"));
        assertTrue("compile should be in Build group", help.indexOf("compile") > help.indexOf("Build:"));
        assertTrue("package should be in Build group (merged)", help.indexOf("package") > help.indexOf("Build:"));
        assertTrue("my-plugin should be in External group", help.indexOf("my-plugin") > help.indexOf("External:"));
    }

    @Test
    public void testHelpSectionProviderNoChildParsers() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> parent = ProcessedCommandBuilder.builder()
                .name("app").description("App tool");

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(parent.create());

        clp.getProcessedCommand().setHelpSectionProvider(() -> {
            Map<String, List<HelpEntry>> sections = new LinkedHashMap<>();
            sections.put("Plugins", Arrays.asList(
                    new HelpEntry("ext-one", "Extension one"),
                    new HelpEntry("ext-two", "Extension two")));
            return sections;
        });

        String help = clp.printHelp();

        assertTrue("Plugins section should appear", help.contains("Plugins:"));
        assertTrue("ext-one should appear", help.contains("ext-one"));
        assertTrue("ext-two should appear", help.contains("ext-two"));
    }

    @Test
    public void testHelpSectionProviderFromAnnotation() throws CommandLineParserException {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        CommandLineParser<CommandInvocation> clp = builder.create(AppWithPluginsCommand.class).getParser();

        String help = clp.printHelp();

        assertTrue("External section should appear", help.contains("External:"));
        assertTrue("plugin-a should appear", help.contains("plugin-a"));
        assertTrue("plugin-b should appear", help.contains("plugin-b"));
        assertTrue("Built-in run command should appear", help.contains("run"));
    }

    @Test
    public void testHelpSectionProviderNullDescription() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> parent = ProcessedCommandBuilder.builder()
                .name("tool").description("Tool");

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(parent.create());

        clp.getProcessedCommand().setHelpSectionProvider(() -> {
            Map<String, List<HelpEntry>> sections = new LinkedHashMap<>();
            sections.put("External", Arrays.asList(
                    new HelpEntry("no-desc", null),
                    new HelpEntry("with-desc", "Has description")));
            return sections;
        });

        String help = clp.printHelp();

        assertTrue("no-desc should appear", help.contains("no-desc"));
        assertTrue("with-desc should appear", help.contains("with-desc"));
    }

    public static class TestHelpSectionProvider implements HelpSectionProvider {
        @Override
        public Map<String, List<HelpEntry>> getAdditionalSections() {
            Map<String, List<HelpEntry>> sections = new LinkedHashMap<>();
            sections.put("External", Arrays.asList(
                    new HelpEntry("plugin-a", "First plugin"),
                    new HelpEntry("plugin-b", "Second plugin")));
            return sections;
        }
    }

    @CommandDefinition(name = "app", description = "App with plugins", groupCommands = {
            PluginRunSubCmd.class }, helpSectionProvider = TestHelpSectionProvider.class)
    public static class AppWithPluginsCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "run", description = "Run something")
    public static class PluginRunSubCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "base", description = "", groupCommands = { GitCommand.class })
    public static class BaseCommand implements Command {

        @Option(hasValue = false)
        private boolean help;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "git", description = "", groupCommands = { GitCommit.class, GitRebase.class,
            GitCheckout.class })
    public static class GitCommand implements Command {

        @Option(hasValue = false)
        private boolean help;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "commit", description = "")
    public static class GitCommit implements Command {

        @Option(shortName = 'a', hasValue = false)
        private boolean all;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "rebase", description = "Reapply commits on top of another base tip")
    public static class GitRebase implements Command {

        @Option(hasValue = false, description = "force your commits")
        private boolean force;

        @Option(hasValue = false, description = "display this help info")
        private boolean help;

        @Option
        private String test;

        @Argument(description = "the branch you want to rebase on")
        private String branch;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            if (help)
                commandInvocation.println(commandInvocation.getHelpInfo("base git rebase"));

            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "checkout", description = "Switch branches or restore working tree files")
    public static class GitCheckout implements Command {

        @Option(hasValue = false, description = "Suppress feedback messages")
        private boolean quiet;

        @Option(hasValue = false, description = "Proceed even if the index or the working tree differs from HEAD")
        private boolean force;

        @Option(hasValue = false, description = "display this help info")
        private boolean help;

        @Option
        private String test;

        @Argument(description = "the branch you want to checkout")
        private String branch;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            if (help)
                commandInvocation.println(commandInvocation.getHelpInfo());

            return CommandResult.SUCCESS;
        }
    }

    // ========== Header and Footer tests ==========

    @Test
    public void testHelpSectionProviderHeader() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> cmd = ProcessedCommandBuilder.builder()
                .name("mycli").description("My CLI tool");

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(cmd.create());

        clp.getProcessedCommand().setHelpSectionProvider(new HelpSectionProvider() {
            @Override
            public Map<String, List<HelpEntry>> getAdditionalSections() {
                return Collections.emptyMap();
            }

            @Override
            public String getHeader() {
                return "My CLI Tool v1.0\nA powerful command line interface";
            }
        });

        String help = clp.printHelp();

        // Header should appear after the Usage line
        assertTrue("Header should be present", help.contains("My CLI Tool v1.0"));
        assertTrue("Multi-line header should work", help.contains("A powerful command line interface"));
        int headerPos = help.indexOf("My CLI Tool v1.0");
        int usagePos = help.indexOf("Usage:");
        assertTrue("Header should appear after Usage", headerPos > usagePos);
    }

    @Test
    public void testHelpSectionProviderFooter() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> cmd = ProcessedCommandBuilder.builder()
                .name("mycli").description("My CLI tool");

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(cmd.create());

        clp.getProcessedCommand().setHelpSectionProvider(new HelpSectionProvider() {
            @Override
            public Map<String, List<HelpEntry>> getAdditionalSections() {
                return Collections.emptyMap();
            }

            @Override
            public String getFooter() {
                return "Copyright (c) 2026 Aesh Project";
            }
        });

        String help = clp.printHelp();

        // Footer should appear after everything
        assertTrue("Footer should be present", help.contains("Copyright (c) 2026 Aesh Project"));
        int footerPos = help.indexOf("Copyright (c) 2026");
        int usagePos = help.indexOf("Usage:");
        assertTrue("Footer should appear after Usage", footerPos > usagePos);
    }

    @Test
    public void testHelpSectionProviderHeaderAndFooter() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> parent = ProcessedCommandBuilder.builder()
                .name("jbang").description("JBang tool");
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> run = ProcessedCommandBuilder.builder()
                .name("run").description("Run a script");

        CommandLineParser<CommandInvocation> clpParent = new AeshCommandLineParser<>(parent.create());
        clpParent.addChildParser(new AeshCommandLineParser<>(run.create()));

        clpParent.getProcessedCommand().setHelpSectionProvider(new HelpSectionProvider() {
            @Override
            public Map<String, List<HelpEntry>> getAdditionalSections() {
                return Collections.emptyMap();
            }

            @Override
            public String getHeader() {
                return "jbang - Unleash the power of Java";
            }

            @Override
            public String getFooter() {
                return "See https://jbang.dev for more info";
            }
        });

        String help = clpParent.printHelp();

        // Verify ordering: usage, then header, then footer
        int headerPos = help.indexOf("Unleash the power");
        int usagePos = help.indexOf("Usage:");
        int footerPos = help.indexOf("https://jbang.dev");

        assertTrue("Header should be present", headerPos >= 0);
        assertTrue("Usage should be present", usagePos >= 0);
        assertTrue("Footer should be present", footerPos >= 0);
        assertTrue("Usage before Header", usagePos < headerPos);
        assertTrue("Header before Footer", headerPos < footerPos);
        // Subcommand should still be listed
        assertTrue("Subcommand run should appear", help.contains("run"));
    }

    @Test
    public void testHelpSectionProviderHeaderFooterInterpolation() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> root = ProcessedCommandBuilder.builder()
                .name("jbang").description("JBang tool");
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> completion = ProcessedCommandBuilder.builder()
                .name("completion").description("Generate completion");

        CommandLineParser<CommandInvocation> rootParser = new AeshCommandLineParser<>(root.create());
        CommandLineParser<CommandInvocation> childParser = new AeshCommandLineParser<>(completion.create());
        rootParser.addChildParser(childParser);

        childParser.getProcessedCommand().setHelpSectionProvider(new HelpSectionProvider() {
            @Override
            public Map<String, List<HelpEntry>> getAdditionalSections() {
                return Collections.emptyMap();
            }

            @Override
            public String getHeader() {
                return "header ${COMMAND-NAME} ${COMMAND-FULL-NAME} ${ROOT-COMMAND-NAME} ${PARENT-COMMAND-NAME}";
            }

            @Override
            public String getFooter() {
                return "footer ${PARENT-COMMAND-FULL-NAME}";
            }
        });

        String help = childParser.printHelp();
        assertTrue(help.contains("header completion jbang completion jbang jbang"));
        assertTrue(help.contains("footer jbang"));
    }

    @Test
    public void testHelpSectionProviderHeaderWithAnnotation() throws CommandLineParserException {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        CommandLineParser<CommandInvocation> clp = builder.create(AppWithHeaderFooterCommand.class).getParser();

        String help = clp.printHelp();

        assertTrue("Header from annotation provider should appear", help.contains("Welcome to AppHF"));
        assertTrue("Footer from annotation provider should appear", help.contains("License: Apache 2.0"));
    }

    @Test
    public void testDescriptionVariableInterpolationInHelp() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> root = ProcessedCommandBuilder.builder()
                .name("root").description("Root cmd");
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> child = ProcessedCommandBuilder.builder()
                .name("child")
                .description("cmd=${COMMAND-NAME}, full=${COMMAND-FULL-NAME}, root=${ROOT-COMMAND-NAME}, "
                        + "parent=${PARENT-COMMAND-NAME}, parentfull=${PARENT-COMMAND-FULL-NAME}, unknown=${UNKNOWN}");
        child.addOption(ProcessedOptionBuilder.builder()
                .name("mode")
                .type(String.class)
                .description("default=${DEFAULT-VALUE}; fallback=${FALLBACK-VALUE}; choices=${COMPLETION-CANDIDATES}")
                .addDefaultValue("fast")
                .addAllAllowedValues(new String[] { "fast", "safe" })
                .build());
        child.argument(ProcessedOptionBuilder.builder()
                .shortName('\u0000')
                .name("")
                .type(String.class)
                .optionType(org.aesh.command.impl.internal.OptionType.ARGUMENT)
                .description("arg for ${COMMAND-NAME} under ${PARENT-COMMAND-NAME}")
                .build());

        CommandLineParser<CommandInvocation> rootParser = new AeshCommandLineParser<>(root.create());
        CommandLineParser<CommandInvocation> childParser = new AeshCommandLineParser<>(child.create());
        rootParser.addChildParser(childParser);

        String help = childParser.printHelp();

        assertTrue(help.contains("cmd=child, full=root child, root=root, parent=root, parentfull=root"));
        assertTrue(help.contains("unknown=${UNKNOWN}"));
        assertTrue(help.contains("default=fast; fallback=fast; choices=fast, safe"));
        assertTrue(help.contains("arg for child under root"));
    }

    @Test
    public void testDescriptionVariableInterpolationInParentCommandListing() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> root = ProcessedCommandBuilder.builder()
                .name("tool").description("Tool root");
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> child = ProcessedCommandBuilder.builder()
                .name("gen")
                .description("Generate for ${ROOT-COMMAND-NAME} via ${COMMAND-FULL-NAME}");

        CommandLineParser<CommandInvocation> rootParser = new AeshCommandLineParser<>(root.create());
        CommandLineParser<CommandInvocation> childParser = new AeshCommandLineParser<>(child.create());
        rootParser.addChildParser(childParser);

        String help = rootParser.printHelp();
        assertTrue(help.contains("Generate for tool via tool gen"));
    }

    @Test
    public void testOptionOrderOverridesInHelp() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("ordered").description("Order test");

        pb.addOption(ProcessedOptionBuilder.builder()
                .name("third")
                .description("third")
                .type(boolean.class)
                .build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("second")
                .description("second")
                .type(boolean.class)
                .order(20)
                .build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("first")
                .description("first")
                .type(boolean.class)
                .order(10)
                .build());

        AeshCommandLineParser<CommandInvocation> parser = new AeshCommandLineParser<>(pb.create());
        String help = parser.printHelp();

        assertEquals(Arrays.asList("first", "second", "third"), Arrays.asList(
                parser.getProcessedCommand().getDisplayOptions().get(0).name(),
                parser.getProcessedCommand().getDisplayOptions().get(1).name(),
                parser.getProcessedCommand().getDisplayOptions().get(2).name()));

        int firstIdx = help.indexOf("--first");
        int secondIdx = help.indexOf("--second");
        int thirdIdx = help.indexOf("--third");
        assertTrue("--first missing in help: " + help, firstIdx >= 0);
        assertTrue("--second missing in help: " + help, secondIdx >= 0);
        assertTrue("--third missing in help: " + help, thirdIdx >= 0);
        assertTrue("wrong order in help: " + help, firstIdx < secondIdx);
        assertTrue("wrong order in help: " + help, secondIdx < thirdIdx);
    }

    @Test
    public void testSortOptionsAlphabeticalInHelp() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("sorted").description("Sort test").sortOptions(true);

        pb.addOption(ProcessedOptionBuilder.builder().name("zulu").description("z").type(boolean.class).build());
        pb.addOption(ProcessedOptionBuilder.builder().name("alpha").description("a").type(boolean.class).build());
        pb.addOption(ProcessedOptionBuilder.builder().name("mike").description("m").type(boolean.class).build());

        AeshCommandLineParser<CommandInvocation> parser = new AeshCommandLineParser<>(pb.create());
        String help = parser.printHelp();

        assertEquals(Arrays.asList("alpha", "mike", "zulu"), Arrays.asList(
                parser.getProcessedCommand().getDisplayOptions().get(0).name(),
                parser.getProcessedCommand().getDisplayOptions().get(1).name(),
                parser.getProcessedCommand().getDisplayOptions().get(2).name()));

        int alphaIdx = help.indexOf("--alpha");
        int mikeIdx = help.indexOf("--mike");
        int zuluIdx = help.indexOf("--zulu");
        assertTrue("--alpha missing in help: " + help, alphaIdx >= 0);
        assertTrue("--mike missing in help: " + help, mikeIdx >= 0);
        assertTrue("--zulu missing in help: " + help, zuluIdx >= 0);
        assertTrue("wrong order in help: " + help, alphaIdx < mikeIdx);
        assertTrue("wrong order in help: " + help, mikeIdx < zuluIdx);
    }

    @Test
    public void testSortOptionsRespectsExplicitOrderFirst() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("sortedordered").description("Sort+order test").sortOptions(true);

        pb.addOption(ProcessedOptionBuilder.builder().name("zeta").description("z").type(boolean.class).order(1).build());
        pb.addOption(ProcessedOptionBuilder.builder().name("beta").description("b").type(boolean.class).order(1).build());
        pb.addOption(ProcessedOptionBuilder.builder().name("alpha").description("a").type(boolean.class).order(2).build());

        AeshCommandLineParser<CommandInvocation> parser = new AeshCommandLineParser<>(pb.create());
        String help = parser.printHelp();

        assertEquals(Arrays.asList("beta", "zeta", "alpha"), Arrays.asList(
                parser.getProcessedCommand().getDisplayOptions().get(0).name(),
                parser.getProcessedCommand().getDisplayOptions().get(1).name(),
                parser.getProcessedCommand().getDisplayOptions().get(2).name()));

        int betaIdx = help.indexOf("--beta");
        int zetaIdx = help.indexOf("--zeta");
        int alphaIdx = help.indexOf("--alpha");
        assertTrue("--beta missing in help: " + help, betaIdx >= 0);
        assertTrue("--zeta missing in help: " + help, zetaIdx >= 0);
        assertTrue("--alpha missing in help: " + help, alphaIdx >= 0);
        assertTrue("wrong order in help: " + help, betaIdx < zetaIdx);
        assertTrue("wrong order in help: " + help, zetaIdx < alphaIdx);
    }

    public static class HeaderFooterProvider implements HelpSectionProvider {
        @Override
        public Map<String, List<HelpEntry>> getAdditionalSections() {
            return Collections.emptyMap();
        }

        @Override
        public String getHeader() {
            return "Welcome to AppHF";
        }

        @Override
        public String getFooter() {
            return "License: Apache 2.0";
        }
    }

    @CommandDefinition(name = "apphf", description = "App with header/footer", helpSectionProvider = HeaderFooterProvider.class)
    public static class AppWithHeaderFooterCommand implements Command<CommandInvocation> {
        @Option(description = "verbose output")
        private boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    // --- Issue #441: subcommand declaration order ---

    @Test
    public void testSubcommandDeclarationOrder() throws CommandLineParserException {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        CommandLineParser<CommandInvocation> clp = builder.create(OrderedGroupCommand.class).getParser();

        String help = clp.printHelp();

        // Subcommands should appear in declaration order: run, build, edit, init, info
        int runIdx = help.indexOf("run");
        int buildIdx = help.indexOf("build");
        int editIdx = help.indexOf("edit");
        int initIdx = help.indexOf("init");
        int infoIdx = help.indexOf("info");

        assertTrue("run should appear in help", runIdx > 0);
        assertTrue("build should appear in help", buildIdx > 0);
        assertTrue("edit should appear in help", editIdx > 0);
        assertTrue("init should appear in help", initIdx > 0);
        assertTrue("info should appear in help", infoIdx > 0);

        assertTrue("run before build", runIdx < buildIdx);
        assertTrue("build before edit", buildIdx < editIdx);
        assertTrue("edit before init", editIdx < initIdx);
        assertTrue("init before info", initIdx < infoIdx);
    }

    @CommandDefinition(name = "app", description = "App with ordered subcommands", groupCommands = { SubRun.class,
            SubBuild.class, SubEdit.class, SubInit.class, SubInfo.class })
    public static class OrderedGroupCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "run", description = "Run the app")
    public static class SubRun implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "build", description = "Build the project")
    public static class SubBuild implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "edit", description = "Edit a file")
    public static class SubEdit implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "init", description = "Initialize a project")
    public static class SubInit implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "info", description = "Show project info")
    public static class SubInfo implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    /** Strip ANSI escape sequences from a string for content assertions. */
    private static String stripAnsi(String s) {
        return s.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    /** Extract the "Usage: ..." line from help output regardless of position, stripping ANSI. */
    private static String extractSynopsisLine(String help) {
        for (String line : help.split("\\r?\\n")) {
            String stripped = stripAnsi(line);
            if (stripped.startsWith("Usage:"))
                return stripped;
        }
        return "";
    }

    // --- Help output gap coverage tests ---

    @Test
    public void testSynopsis_MutuallyExclusivePipes() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("export").description("Export data");
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("json").type(boolean.class).hasValue(false)
                .exclusiveWith("xml").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("xml").type(boolean.class).hasValue(false)
                .exclusiveWith("json").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("verbose").type(boolean.class).hasValue(false).build());

        String help = new AeshCommandLineParser<>(pb.create()).printHelp();
        String synopsis = extractSynopsisLine(help);

        assertTrue("Synopsis should contain pipe notation for exclusive options",
                synopsis.contains("--json | --xml") || synopsis.contains("--xml | --json"));
        assertFalse("Exclusive options should NOT appear individually in synopsis",
                synopsis.contains("[--json]") && synopsis.contains("[--xml]"));
    }

    @Test
    public void testSynopsis_RequiredWithoutBrackets() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("deploy").description("Deploy app");
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("target").type(String.class).required(true).build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("verbose").type(boolean.class).hasValue(false).build());

        String help = new AeshCommandLineParser<>(pb.create()).printHelp();
        String synopsis = extractSynopsisLine(help);

        assertTrue("Required option should appear without brackets",
                synopsis.contains(" --target=<target>") && !synopsis.contains("[--target"));
        assertTrue("Optional option should appear with brackets",
                synopsis.contains("[") && synopsis.contains("]"));
    }

    @Test
    public void testSynopsis_OptionalValueNoPlaceholder() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("run").description("Run app");
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("debug").type(String.class).optionalValue(true).addDefaultValue("4004").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("output").type(String.class).build());

        String help = new AeshCommandLineParser<>(pb.create()).printHelp();
        String synopsis = extractSynopsisLine(help);

        assertFalse("optionalValue option should NOT show =<debug> in synopsis",
                synopsis.contains("--debug=<debug>"));
        assertTrue("optionalValue option should appear as bare [--debug]",
                synopsis.contains("[--debug]"));
        assertTrue("Regular value option should show =<output>",
                synopsis.contains("--output=<output>"));
    }

    @Test
    public void testSynopsis_FallbackValueNoPlaceholder() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("run").description("Run app");
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("jfr").type(String.class).fallbackValue("").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("config").type(String.class).build());

        String help = new AeshCommandLineParser<>(pb.create()).printHelp();
        String synopsis = extractSynopsisLine(help);

        assertFalse("fallbackValue option should NOT show =<jfr> in synopsis",
                synopsis.contains("--jfr=<jfr>"));
        assertTrue("fallbackValue option should appear as bare [--jfr]",
                synopsis.contains("[--jfr]"));
        assertTrue("Regular value option should show =<config>",
                synopsis.contains("--config=<config>"));
    }

    @Test
    public void testSynopsis_ValuePlaceholderInSynopsisLine() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("connect").description("Connect to server");
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("host").type(String.class).build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("port").type(int.class).build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("verbose").type(boolean.class).hasValue(false).build());

        String help = new AeshCommandLineParser<>(pb.create()).printHelp();
        String synopsis = extractSynopsisLine(help);

        assertTrue("Synopsis should contain --host=<host>", synopsis.contains("--host=<host>"));
        assertTrue("Synopsis should contain --port=<port>", synopsis.contains("--port=<port>"));
        assertFalse("Boolean --verbose should NOT have =<>", synopsis.contains("--verbose="));
    }

    @Test
    public void testSynopsis_BooleanNoPlaceholder() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("test").description("Test");
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("flag").type(boolean.class).hasValue(false).build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("boolObj").type(Boolean.class).hasValue(false).build());

        String help = new AeshCommandLineParser<>(pb.create()).printHelp();

        assertFalse("boolean flag should not show =<flag>", help.contains("--flag="));
        assertFalse("Boolean object should not show =<boolObj>", help.contains("--boolObj="));
    }

    @Test
    public void testSynopsis_NegatableOption() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("build").description("Build");
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("cds").shortName('c').type(boolean.class).hasValue(false)
                .negatable(true).build());

        String help = new AeshCommandLineParser<>(pb.create()).printHelp();
        String synopsis = extractSynopsisLine(help);

        // Negatable option should show --[no-]name format in synopsis
        assertTrue("Negatable option should show --[no-]cds in synopsis",
                synopsis.contains("[--[no-]cds]"));
    }

    @Test
    public void testHelp_MultipleIndexedArguments() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("copy").description("Copy files");
        org.aesh.command.impl.internal.ProcessedCommand<Command<CommandInvocation>, CommandInvocation> pc = pb.create();
        pc.addArgument(ProcessedOptionBuilder.builder()
                .name("").type(String.class).fieldName("source")
                .paramLabel("source").index("0")
                .optionType(org.aesh.command.impl.internal.OptionType.ARGUMENT).build());
        pc.addArgument(ProcessedOptionBuilder.builder()
                .name("").type(String.class).fieldName("dest")
                .paramLabel("dest").index("1")
                .optionType(org.aesh.command.impl.internal.OptionType.ARGUMENT).build());

        String help = new AeshCommandLineParser<>(pc).printHelp();

        assertTrue("Should show <source> label", help.contains("<source>"));
        assertTrue("Should show <dest> label", help.contains("<dest>"));
        // Verify order: source before dest
        int srcIdx = help.indexOf("<source>");
        int destIdx = help.indexOf("<dest>");
        assertTrue("source should appear before dest", srcIdx < destIdx);
    }

    @Test
    public void testHelp_VersionOptionShown() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("myapp").description("My app").version("2.0.0");

        String help = new AeshCommandLineParser<>(pb.create()).printHelp();

        assertTrue("Version option should appear in help", help.contains("--version"));
        assertTrue("Version description should appear", help.contains("version"));
    }

    // --- ANSI styling tests ---

    @Test
    public void testAnsi_OptionNamesYellow() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("styled").description("Styled help");
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("output").type(String.class).description("Output file").build());

        String help = new AeshCommandLineParser<>(pb.create()).printHelp();

        // Option names should be wrapped in yellow ANSI
        assertTrue("Option name should have yellow ANSI",
                help.contains(ANSI.YELLOW_TEXT + "--output"));
        assertTrue("Option name should be reset after",
                help.contains("--output" + ANSI.RESET));
    }

    @Test
    public void testAnsi_ValuePlaceholderCyan() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("styled").description("Styled help");
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("config").type(String.class).description("Config file").build());

        String help = new AeshCommandLineParser<>(pb.create()).printHelp();

        // Value placeholder should be wrapped in cyan ANSI
        assertTrue("Value placeholder should have cyan ANSI",
                help.contains(ANSI.CYAN_TEXT + "=<config>"));
        assertTrue("Value placeholder should be reset after",
                help.contains("=<config>" + ANSI.RESET));
    }

    @Test
    public void testAnsi_BooleanNoPlaceholderStyling() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("styled").description("Styled help");
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("verbose").type(boolean.class).hasValue(false).description("Verbose").build());

        String help = new AeshCommandLineParser<>(pb.create()).printHelp();

        // Boolean option should have yellow name but no cyan placeholder
        assertTrue("Boolean option should have yellow ANSI",
                help.contains(ANSI.YELLOW_TEXT + "--verbose"));
        assertFalse("Boolean option should NOT have cyan placeholder",
                help.contains(ANSI.CYAN_TEXT));
    }

    @Test
    public void testAnsi_RequiredOptionBold() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("styled").description("Styled help");
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("target").type(String.class).required(true).description("Target").build());

        String help = new AeshCommandLineParser<>(pb.create()).printHelp();

        // Required option should have bold + yellow
        assertTrue("Required option should have bold",
                help.contains(ANSI.BOLD));
        assertTrue("Required option should have yellow",
                help.contains(ANSI.YELLOW_TEXT));
    }

    @Test
    public void testAnsi_ArgumentLabelCyan() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("styled").description("Styled help");
        pb.argument(ProcessedOptionBuilder.builder()
                .name("").type(String.class).fieldName("file")
                .paramLabel("file").description("Input file")
                .optionType(org.aesh.command.impl.internal.OptionType.ARGUMENT).build());

        String help = new AeshCommandLineParser<>(pb.create()).printHelp();

        // Argument label should have cyan styling
        assertTrue("Argument label should have cyan ANSI",
                help.contains(ANSI.CYAN_TEXT));
        String stripped = stripAnsi(help);
        assertTrue("Stripped help should contain <file>", stripped.contains("<file>"));
    }

    @Test
    public void testAnsi_DisabledWithAnsiModeFalse() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("plain").description("Plain help");
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("output").type(String.class).description("Output").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("verbose").type(boolean.class).hasValue(false).description("Verbose").build());

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pb.create());
        clp.updateAnsiMode(false);
        String help = clp.printHelp();

        // No ANSI escape sequences should be present
        assertFalse("No ANSI escapes when ansiMode=false",
                help.contains("\u001B["));
        // Content should still be present
        assertTrue(help.contains("--output=<output>"));
        assertTrue(help.contains("--verbose"));
        assertTrue(help.contains("Output"));
    }

    @Test
    public void testAnsi_SynopsisAndOptionsStrippedWhenAnsiDisabled() throws CommandLineParserException {
        // #499: Verify that ALL ANSI codes are suppressed in help output when ansiMode=false,
        // including synopsis (bold command name, yellow options, cyan placeholders).
        // This test would have caught the bug where ProcessedCommand.printHelp() used the
        // option's isAnsiMode() which was still true even when the parser had ansiMode=false.
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("deploy").description("Deploy application");
        pb.addOption(ProcessedOptionBuilder.builder()
                .shortName('e').name("environment").type(String.class)
                .description("Target environment").required(true).build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .shortName('f').name("force").type(boolean.class).hasValue(false)
                .description("Force deployment").negatable(true).build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .shortName('v').name("verbose").type(boolean.class).hasValue(false)
                .description("Verbose output").build());

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pb.create());
        clp.updateAnsiMode(false);
        String help = clp.printHelp();

        // Synopsis should have NO ANSI codes
        assertFalse("Synopsis should not contain ANSI bold",
                help.contains("\u001B[1m"));
        assertFalse("Synopsis should not contain ANSI yellow",
                help.contains("\u001B[0;33m"));
        assertFalse("Synopsis should not contain ANSI cyan",
                help.contains("\u001B[0;36m"));
        assertFalse("Synopsis should not contain any ESC code",
                help.contains("\u001B["));

        // Content should still be correct
        assertTrue("Should contain Usage:", help.contains("Usage:"));
        assertTrue("Should contain deploy", help.contains("deploy"));
        assertTrue("Should contain --environment", help.contains("--environment"));
        assertTrue("Should contain negatable --[no-]force", help.contains("[no-]force"));
        assertTrue("Should contain --verbose", help.contains("--verbose"));
    }

    @Test
    public void testAnsi_ProcessedCommandPrintHelpRespectsAnsiMode() throws CommandLineParserException {
        // #499: Verify ProcessedCommand.printHelp() directly respects option ansiMode,
        // not just the parser's ansiMode. This catches the case where options are created
        // but ansiMode is not propagated from parser to options.
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("test").description("Test");
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("opt").type(String.class).description("An option").build());

        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> pc = pb.create();

        // Manually set options to ansiMode=false (simulating NO_COLOR propagation)
        for (ProcessedOption opt : pc.getOptions()) {
            opt.updateAnsiMode(false);
        }

        String help = pc.printHelp("test", false, false);
        assertFalse("ProcessedCommand.printHelp should not contain ANSI when options have ansiMode=false",
                help.contains("\u001B["));
    }

    @Test
    public void testAnsi_SubcommandStyling() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> parent = ProcessedCommandBuilder.builder()
                .name("app").description("App");
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> child = ProcessedCommandBuilder.builder()
                .name("run").description("Run something");

        CommandLineParser<CommandInvocation> parentParser = new AeshCommandLineParser<>(parent.create());
        parentParser.addChildParser(new AeshCommandLineParser<>(child.create()));

        String help = parentParser.printHelp();

        // Subcommand name should have styling (blue text via ANSIBuilder)
        String stripped = stripAnsi(help);
        assertTrue("Stripped help should contain subcommand 'run'", stripped.contains("run"));
        // The raw help should contain ANSI codes (from ANSIBuilder.blueText)
        assertTrue("Help should contain ANSI escape codes",
                help.contains("\u001B["));
    }

    // --- Issue #455: OptionGroup rendering ---

    @Test
    public void testHelp_OptionGroupShowsKeyValueSyntax() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("run").description("Run app");
        pb.addOption(ProcessedOptionBuilder.builder()
                .shortName('D').name("properties")
                .type(String.class).optionType(org.aesh.command.impl.internal.OptionType.GROUP)
                .valueSeparator(',')
                .description("set a system property").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("verbose").type(boolean.class).hasValue(false)
                .description("Verbose output").build());

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pb.create());
        clp.updateAnsiMode(false);
        String help = clp.printHelp();

        // OptionGroup should show key=value syntax, not just =<properties>
        assertTrue("OptionGroup should show <key>=<value> in options section",
                help.contains("=<key=value>"));
        assertFalse("OptionGroup should NOT show =<properties>",
                help.contains("=<properties>"));
    }

    @Test
    public void testSynopsis_OptionGroupShowsKeyValueSyntax() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("run").description("Run app");
        pb.addOption(ProcessedOptionBuilder.builder()
                .shortName('D').name("properties")
                .type(String.class).optionType(org.aesh.command.impl.internal.OptionType.GROUP)
                .valueSeparator(',')
                .description("set a system property").build());

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pb.create());
        clp.updateAnsiMode(false);
        String help = clp.printHelp();
        String synopsis = extractSynopsisLine(help);

        // Synopsis should show -D<key>=<value>, not -D=<properties>
        assertTrue("Synopsis should show <key>=<value> for OptionGroup",
                synopsis.contains("-D<key>=<value>"));
    }

    // --- Issue #457: Synopsis wrapping ---

    @Test
    public void testSynopsis_WrapsAtWidth() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("longcmd").description("A command with many options");
        // Add enough options to exceed 80 columns
        for (String name : new String[] { "alpha", "bravo", "charlie", "delta", "echo",
                "foxtrot", "golf", "hotel", "india", "juliet" }) {
            pb.addOption(ProcessedOptionBuilder.builder()
                    .name(name).type(String.class).description("Option " + name).build());
        }

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pb.create());
        clp.updateAnsiMode(false);
        String help = clp.printHelp();

        // Find the synopsis block (may span multiple lines starting from "Usage:")
        String[] lines = help.split("\\r?\\n");
        boolean foundUsage = false;
        int synopsisLineCount = 0;
        for (String line : lines) {
            if (line.startsWith("Usage:")) {
                foundUsage = true;
                synopsisLineCount++;
                assertTrue("Synopsis line should not exceed 80 chars: '" + line + "'",
                        line.length() <= 82); // small margin for edge cases
            } else if (foundUsage && line.startsWith("        ")) {
                // Continuation line (indented)
                synopsisLineCount++;
                assertTrue("Continuation line should not exceed 80 chars: '" + line + "'",
                        line.length() <= 82);
            } else if (foundUsage) {
                break;
            }
        }
        assertTrue("Synopsis should wrap to multiple lines", synopsisLineCount > 1);
    }

    // --- Issue #458: Option column width capping ---

    @Test
    public void testHelp_LongOptionNameWrapsDescription() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("app").description("Test app");
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("very-long-option-name-that-exceeds-column-width")
                .type(String.class).description("A description").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("short").type(boolean.class).hasValue(false)
                .description("Short option").build());

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pb.create());
        clp.updateAnsiMode(false);
        String help = clp.printHelp();

        // The long option name should have its description on the next line
        String[] lines = help.split("\\r?\\n");
        boolean foundLongOpt = false;
        boolean descOnNextLine = false;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("--very-long-option-name") && !lines[i].contains("A description")) {
                foundLongOpt = true;
                // Description should be on the next line, indented
                if (i + 1 < lines.length && lines[i + 1].trim().equals("A description")) {
                    descOnNextLine = true;
                }
            }
        }
        assertTrue("Long option should be present", foundLongOpt);
        assertTrue("Description should wrap to next line for long option names", descOnNextLine);
    }

    // --- Issue #463: Synopsis ANSI codes correct ---

    @Test
    public void testAnsi_SynopsisHasCorrectEscapeCodes() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("app").description("Test app");
        pb.addOption(ProcessedOptionBuilder.builder()
                .shortName('v').name("verbose").type(boolean.class).hasValue(false)
                .description("Verbose").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("config").type(String.class).description("Config file").build());

        String help = new AeshCommandLineParser<>(pb.create()).printHelp();

        // Find the synopsis line (contains "Usage:")
        String synopsisLine = null;
        for (String line : help.split("\\r?\\n")) {
            if (line.contains("Usage:")) {
                synopsisLine = line;
                break;
            }
        }
        assertNotNull("Synopsis line should exist", synopsisLine);

        // Every ANSI code should have the ESC prefix (\u001B)
        // Check that there are no bare "[0;33m" or "[0;36m" without preceding \u001B
        assertFalse("ANSI codes should not be bare (missing ESC before [0;33m)",
                synopsisLine.matches(".*[^\\u001B]\\[0;33m.*"));
        assertFalse("ANSI codes should not be bare (missing ESC before [0;36m)",
                synopsisLine.matches(".*[^\\u001B]\\[0;36m.*"));

        // Verify yellow and cyan are present with ESC prefix
        assertTrue("Synopsis should contain yellow ANSI with ESC",
                synopsisLine.contains(ANSI.YELLOW_TEXT));
        assertTrue("Synopsis should contain bold ANSI with ESC",
                synopsisLine.contains(ANSI.BOLD) || synopsisLine.contains("\u001B[1m"));

        // Verify every [ that's part of an ANSI code has \u001B before it
        // (ANSI codes match pattern \u001B\[\d+(;\d+)*m)
        // Check no orphaned ANSI codes exist by ensuring all CSI sequences start with ESC
        for (int i = 0; i < synopsisLine.length(); i++) {
            if (synopsisLine.charAt(i) == '[' && i > 0) {
                char prev = synopsisLine.charAt(i - 1);
                // If this looks like a CSI sequence (followed by digits and 'm'),
                // it must be preceded by ESC
                String rest = synopsisLine.substring(i);
                if (rest.matches("^\\[\\d+(;\\d+)*m.*")) {
                    assertEquals("CSI sequence at position " + i + " must be preceded by ESC",
                            '\u001B', prev);
                }
            }
        }
    }

    @Test
    public void testAnsi_SynopsisWrappedHasCorrectEscapeCodes() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("myapp").description("Test app");
        // Add enough options to trigger wrapping
        pb.addOption(ProcessedOptionBuilder.builder()
                .shortName('h').name("help").type(boolean.class).hasValue(false).build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .shortName('v').name("verbose").type(boolean.class).hasValue(false).build());
        for (String name : new String[] { "alpha", "bravo", "charlie", "delta", "echo",
                "foxtrot", "golf", "hotel" }) {
            pb.addOption(ProcessedOptionBuilder.builder()
                    .name(name).type(String.class).description("Option " + name).build());
        }

        String help = new AeshCommandLineParser<>(pb.create()).printHelp();

        // Check ALL lines for bare ANSI codes (missing ESC prefix)
        String[] lines = help.split("\\r?\\n");
        for (String line : lines) {
            for (int i = 1; i < line.length(); i++) {
                if (line.charAt(i) == '[') {
                    String rest = line.substring(i);
                    if (rest.matches("^\\[\\d+(;\\d+)*m.*")) {
                        assertEquals(
                                "CSI at pos " + i + " in line must have ESC prefix",
                                '\u001B', line.charAt(i - 1));
                    }
                }
            }
        }
    }

    // --- Issue #460: OptionGroup short-name-only ---

    @Test
    public void testHelp_OptionGroupShortNameOnly() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("run").description("Run app");
        pb.addOption(ProcessedOptionBuilder.builder()
                .shortName('D').name("")
                .type(String.class).optionType(org.aesh.command.impl.internal.OptionType.GROUP)
                .valueSeparator(',').fieldName("properties")
                .description("set a system property").build());

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pb.create());
        clp.updateAnsiMode(false);
        String help = clp.printHelp();

        // Should show -D=<key=value> without --properties
        assertTrue("Should contain -D", help.contains("-D"));
        assertFalse("Should NOT contain --properties", help.contains("--properties"));
        assertTrue("Should show key=value syntax", help.contains("=<key=value>"));
    }

    // --- Issue #461: Positional arguments inline ---

    @Test
    public void testHelp_PositionalArgsInlineNoSectionHeaders() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("copy").description("Copy files");
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("verbose").type(boolean.class).hasValue(false)
                .description("Verbose output").build());
        pb.argument(ProcessedOptionBuilder.builder()
                .name("").type(String.class).fieldName("source")
                .paramLabel("source").description("Source file")
                .optionType(org.aesh.command.impl.internal.OptionType.ARGUMENT).build());

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pb.create());
        clp.updateAnsiMode(false);
        String help = clp.printHelp();

        // Should NOT have separate "Argument:" section header
        assertFalse("Should not have 'Argument:' section header",
                help.contains("Argument:"));
        // But the argument should still appear with its label and description
        assertTrue("Should contain <source> label", help.contains("<source>"));
        assertTrue("Should contain description", help.contains("Source file"));
        // Should be under the Options section
        assertTrue("Should have Options section", help.contains("Options:"));
    }

    @Test
    public void testHelp_MultiLineOptionDescription() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("deploy").description("Deploy application");
        pb.addOption(ProcessedOptionBuilder.builder()
                .shortName('e').name("env").type(String.class)
                .description("Target environment\nMust be one of: dev, staging, prod")
                .build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .shortName('f').name("force").type(boolean.class).hasValue(false)
                .description("Force deployment").build());

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pb.create());
        clp.updateAnsiMode(false);
        String help = clp.printHelp();
        String[] lines = help.split(System.lineSeparator());

        // Find the line with "Target environment"
        int envLine = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("Target environment")) {
                envLine = i;
                break;
            }
        }
        assertTrue("Should find 'Target environment' in help", envLine >= 0);

        // The continuation line should be indented to align with the description
        assertTrue("Should have continuation line", envLine + 1 < lines.length);
        String continuation = lines[envLine + 1];
        assertTrue("Continuation should contain 'Must be one of'",
                continuation.contains("Must be one of"));

        // The indentation of the continuation should match the description start
        int descStart = lines[envLine].indexOf("Target environment");
        int contStart = continuation.indexOf("Must be");
        assertEquals("Continuation should align with description start", descStart, contStart);
    }

    @Test
    public void testHelp_MultiLineOptionDescription_ThreeLines() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("run").description("Run a script");
        pb.addOption(ProcessedOptionBuilder.builder()
                .shortName('d').name("debug").type(String.class)
                .description("Enable debug mode\nDefault port: 4004\nUse host:port format")
                .build());

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pb.create());
        clp.updateAnsiMode(false);
        String help = clp.printHelp();
        String[] lines = help.split(System.lineSeparator());

        // Find the three description lines
        int firstLine = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("Enable debug mode")) {
                firstLine = i;
                break;
            }
        }
        assertTrue("Should find first description line", firstLine >= 0);
        assertTrue("Should have second line", lines[firstLine + 1].contains("Default port: 4004"));
        assertTrue("Should have third line", lines[firstLine + 2].contains("Use host:port format"));

        // All three should be aligned
        int col1 = lines[firstLine].indexOf("Enable debug");
        int col2 = lines[firstLine + 1].indexOf("Default port");
        int col3 = lines[firstLine + 2].indexOf("Use host:port");
        assertEquals("Line 2 should align with line 1", col1, col2);
        assertEquals("Line 3 should align with line 1", col1, col3);
    }

    @Test
    public void testHelp_MultiLineCommandDescription() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("deploy").description("Deploy application\n\nUse with caution in production");
        pb.addOption(ProcessedOptionBuilder.builder()
                .shortName('v').name("verbose").type(boolean.class).hasValue(false)
                .description("Verbose").build());

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pb.create());
        clp.updateAnsiMode(false);
        String help = clp.printHelp();

        assertTrue("Should contain first line", help.contains("Deploy application"));
        assertTrue("Should contain third line", help.contains("Use with caution in production"));
    }

    @Test
    public void testHelp_SingleLineDescriptionUnchanged() throws CommandLineParserException {
        // Verify single-line descriptions still work without any extra overhead
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("test").description("Test command");
        pb.addOption(ProcessedOptionBuilder.builder()
                .shortName('v').name("verbose").type(boolean.class).hasValue(false)
                .description("Enable verbose output").build());

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pb.create());
        clp.updateAnsiMode(false);
        String help = clp.printHelp();

        assertTrue("Should contain description", help.contains("Enable verbose output"));
        // Single-line description should be on one line with the option
        String[] lines = help.split(System.lineSeparator());
        for (String line : lines) {
            if (line.contains("--verbose")) {
                assertTrue("Description should be on same line as option",
                        line.contains("Enable verbose output"));
                break;
            }
        }
    }

    @Test
    public void testHelp_TextBlockIndentationStripped() throws CommandLineParserException {
        // Simulate a text block with common leading whitespace:
        // """
        //     Target environment.
        //     Must be one of: dev, staging, prod.
        //     Defaults to $APP_ENV."""
        String textBlockDesc = "    Target environment.\n    Must be one of: dev, staging, prod.\n    Defaults to $APP_ENV.";

        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("run").description("Run a script");
        pb.addOption(ProcessedOptionBuilder.builder()
                .shortName('e').name("env").type(String.class)
                .description(textBlockDesc)
                .build());

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pb.create());
        clp.updateAnsiMode(false);
        String help = clp.printHelp();
        String[] lines = help.split(System.lineSeparator());

        // Find the env option line
        int envLine = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("Target environment")) {
                envLine = i;
                break;
            }
        }
        assertTrue("Should find 'Target environment' in help", envLine >= 0);

        // The leading indentation should be stripped — "Target environment" should
        // appear right after the option column, not with extra leading spaces
        assertFalse("Should not have extra leading spaces before 'Target'",
                lines[envLine].contains("    Target"));

        // Continuation lines should be aligned with the first line
        assertTrue("Should have continuation line", envLine + 1 < lines.length);
        int descStart = lines[envLine].indexOf("Target");
        int contStart = lines[envLine + 1].indexOf("Must be");
        assertEquals("Continuation should align", descStart, contStart);
    }

    @Test
    public void testNegatableShortNameInFlagCluster() throws CommandLineParserException {
        // #504: Negatable options with short names should appear in the grouped flags cluster
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("app").description("App");
        pb.addOption(ProcessedOptionBuilder.builder()
                .shortName('h').name("help").type(boolean.class).hasValue(false).description("Help").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .shortName('v').name("verbose").type(boolean.class).hasValue(false).description("Verbose").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .shortName('o').name("offline").type(boolean.class).hasValue(false)
                .negatable(true).description("Offline mode").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .shortName('x').name("stacktrace").type(boolean.class).hasValue(false)
                .negatable(true).description("Show stacktrace").build());

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pb.create());
        clp.updateAnsiMode(false);
        String help = clp.printHelp();

        // Short names of negatable options should be in the flag cluster
        assertTrue("Synopsis should contain -o and -x in cluster: " + help,
                help.contains("[-hvox]"));
        // Negatable long forms should also appear separately
        assertTrue("Synopsis should contain --[no-]offline: " + help,
                help.contains("[--[no-]offline]"));
        assertTrue("Synopsis should contain --[no-]stacktrace: " + help,
                help.contains("[--[no-]stacktrace]"));
    }

}
