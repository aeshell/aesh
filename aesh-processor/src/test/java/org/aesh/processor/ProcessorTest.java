/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.aesh.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.aesh.command.Command;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.metadata.CommandMetadataProvider;
import org.junit.Test;

/**
 * Tests that the annotation processor generates metadata that is structurally
 * equivalent to what the reflection-based AeshCommandContainerBuilder produces.
 */
public class ProcessorTest {

    // --- Test: Simple command with @Option and @Argument ---

    private static final String SIMPLE_COMMAND_SOURCE = "package test;\n" +
            "\n" +
            "import org.aesh.command.Command;\n" +
            "import org.aesh.command.CommandDefinition;\n" +
            "import org.aesh.command.CommandResult;\n" +
            "import org.aesh.command.invocation.CommandInvocation;\n" +
            "import org.aesh.command.option.Option;\n" +
            "import org.aesh.command.option.Argument;\n" +
            "\n" +
            "@CommandDefinition(name = \"simple\", description = \"A simple test command\")\n" +
            "public class SimpleCommand implements Command<CommandInvocation> {\n" +
            "    @Option(shortName = 'v', description = \"Enable verbose output\")\n" +
            "    private boolean verbose;\n" +
            "\n" +
            "    @Option(name = \"output\", description = \"Output file\", required = true)\n" +
            "    private String outputFile;\n" +
            "\n" +
            "    @Argument(description = \"Source file\")\n" +
            "    private String source;\n" +
            "\n" +
            "    @Override\n" +
            "    public CommandResult execute(CommandInvocation commandInvocation) {\n" +
            "        return CommandResult.SUCCESS;\n" +
            "    }\n" +
            "}\n";

    @Test
    public void testSimpleCommand() throws Exception {
        CompilationResult result = compileWithProcessor(
                new InMemorySource("test.SimpleCommand", SIMPLE_COMMAND_SOURCE));
        assertTrue("Compilation should succeed: " + result.diagnostics, result.success);

        // Load both the command class and the generated metadata provider
        Class<?> commandClass = result.classLoader.loadClass("test.SimpleCommand");
        Class<?> metadataClass = result.classLoader.loadClass("test.SimpleCommand_AeshMetadata");

        assertEquivalence(commandClass, metadataClass);
    }

    // --- Test: All option types ---

    private static final String ALL_OPTIONS_COMMAND_SOURCE = "package test;\n" +
            "\n" +
            "import java.util.List;\n" +
            "import java.util.Map;\n" +
            "\n" +
            "import org.aesh.command.Command;\n" +
            "import org.aesh.command.CommandDefinition;\n" +
            "import org.aesh.command.CommandResult;\n" +
            "import org.aesh.command.invocation.CommandInvocation;\n" +
            "import org.aesh.command.option.Arguments;\n" +
            "import org.aesh.command.option.Option;\n" +
            "import org.aesh.command.option.OptionGroup;\n" +
            "import org.aesh.command.option.OptionList;\n" +
            "\n" +
            "@CommandDefinition(name = \"allopts\", description = \"All option types\")\n" +
            "public class AllOptionsCommand implements Command<CommandInvocation> {\n" +
            "    @Option(shortName = 'n', description = \"Name\")\n" +
            "    private String name;\n" +
            "\n" +
            "    @OptionList(shortName = 'i', description = \"Items\")\n" +
            "    private List<String> items;\n" +
            "\n" +
            "    @OptionGroup(shortName = 'p', description = \"Properties\")\n" +
            "    private Map<String, String> properties;\n" +
            "\n" +
            "    @Arguments(description = \"Files to process\")\n" +
            "    private List<String> files;\n" +
            "\n" +
            "    @Override\n" +
            "    public CommandResult execute(CommandInvocation commandInvocation) {\n" +
            "        return CommandResult.SUCCESS;\n" +
            "    }\n" +
            "}\n";

    @Test
    public void testAllOptionTypes() throws Exception {
        CompilationResult result = compileWithProcessor(
                new InMemorySource("test.AllOptionsCommand", ALL_OPTIONS_COMMAND_SOURCE));
        assertTrue("Compilation should succeed: " + result.diagnostics, result.success);

        Class<?> commandClass = result.classLoader.loadClass("test.AllOptionsCommand");
        Class<?> metadataClass = result.classLoader.loadClass("test.AllOptionsCommand_AeshMetadata");

        assertEquivalence(commandClass, metadataClass);
    }

    // --- Test: Boolean option with negatable ---

    private static final String BOOLEAN_COMMAND_SOURCE = "package test;\n" +
            "\n" +
            "import org.aesh.command.Command;\n" +
            "import org.aesh.command.CommandDefinition;\n" +
            "import org.aesh.command.CommandResult;\n" +
            "import org.aesh.command.invocation.CommandInvocation;\n" +
            "import org.aesh.command.option.Option;\n" +
            "\n" +
            "@CommandDefinition(name = \"boolcmd\", description = \"Boolean options test\")\n" +
            "public class BooleanCommand implements Command<CommandInvocation> {\n" +
            "    @Option(hasValue = false, description = \"Flag\")\n" +
            "    private boolean flag;\n" +
            "\n" +
            "    @Option(hasValue = false, negatable = true, description = \"Negatable flag\")\n" +
            "    private Boolean negatableFlag;\n" +
            "\n" +
            "    @Override\n" +
            "    public CommandResult execute(CommandInvocation commandInvocation) {\n" +
            "        return CommandResult.SUCCESS;\n" +
            "    }\n" +
            "}\n";

    @Test
    public void testBooleanOptions() throws Exception {
        CompilationResult result = compileWithProcessor(
                new InMemorySource("test.BooleanCommand", BOOLEAN_COMMAND_SOURCE));
        assertTrue("Compilation should succeed: " + result.diagnostics, result.success);

        Class<?> commandClass = result.classLoader.loadClass("test.BooleanCommand");
        Class<?> metadataClass = result.classLoader.loadClass("test.BooleanCommand_AeshMetadata");

        assertEquivalence(commandClass, metadataClass);
    }

    // --- Test: Command with default values ---

    private static final String DEFAULT_VALUES_SOURCE = "package test;\n" +
            "\n" +
            "import org.aesh.command.Command;\n" +
            "import org.aesh.command.CommandDefinition;\n" +
            "import org.aesh.command.CommandResult;\n" +
            "import org.aesh.command.invocation.CommandInvocation;\n" +
            "import org.aesh.command.option.Option;\n" +
            "\n" +
            "@CommandDefinition(name = \"defaults\", description = \"Default values test\")\n" +
            "public class DefaultValuesCommand implements Command<CommandInvocation> {\n" +
            "    @Option(defaultValue = {\"hello\", \"world\"}, description = \"Greeting\")\n" +
            "    private String greeting;\n" +
            "\n" +
            "    @Option(defaultValue = \"42\", description = \"Count\")\n" +
            "    private int count;\n" +
            "\n" +
            "    @Override\n" +
            "    public CommandResult execute(CommandInvocation commandInvocation) {\n" +
            "        return CommandResult.SUCCESS;\n" +
            "    }\n" +
            "}\n";

    @Test
    public void testDefaultValues() throws Exception {
        CompilationResult result = compileWithProcessor(
                new InMemorySource("test.DefaultValuesCommand", DEFAULT_VALUES_SOURCE));
        assertTrue("Compilation should succeed: " + result.diagnostics, result.success);

        Class<?> commandClass = result.classLoader.loadClass("test.DefaultValuesCommand");
        Class<?> metadataClass = result.classLoader.loadClass("test.DefaultValuesCommand_AeshMetadata");

        assertEquivalence(commandClass, metadataClass);
    }

    // --- Test: Command with aliases and version ---

    private static final String ALIASED_SOURCE = "package test;\n" +
            "\n" +
            "import org.aesh.command.Command;\n" +
            "import org.aesh.command.CommandDefinition;\n" +
            "import org.aesh.command.CommandResult;\n" +
            "import org.aesh.command.invocation.CommandInvocation;\n" +
            "import org.aesh.command.option.Option;\n" +
            "\n" +
            "@CommandDefinition(name = \"aliased\", description = \"Aliased command\", aliases = {\"al\", \"a\"}, " +
            "version = \"1.0\", generateHelp = true)\n" +
            "public class AliasedCommand implements Command<CommandInvocation> {\n" +
            "    @Option(description = \"Option\")\n" +
            "    private String opt;\n" +
            "\n" +
            "    @Override\n" +
            "    public CommandResult execute(CommandInvocation commandInvocation) {\n" +
            "        return CommandResult.SUCCESS;\n" +
            "    }\n" +
            "}\n";

    @Test
    public void testAliasedCommand() throws Exception {
        CompilationResult result = compileWithProcessor(
                new InMemorySource("test.AliasedCommand", ALIASED_SOURCE));
        assertTrue("Compilation should succeed: " + result.diagnostics, result.success);

        Class<?> commandClass = result.classLoader.loadClass("test.AliasedCommand");
        Class<?> metadataClass = result.classLoader.loadClass("test.AliasedCommand_AeshMetadata");

        assertEquivalence(commandClass, metadataClass);
    }

    // --- Test: Class hierarchy with options in superclass ---

    private static final String BASE_COMMAND_SOURCE = "package test;\n" +
            "\n" +
            "import org.aesh.command.Command;\n" +
            "import org.aesh.command.CommandResult;\n" +
            "import org.aesh.command.invocation.CommandInvocation;\n" +
            "import org.aesh.command.option.Option;\n" +
            "\n" +
            "public abstract class BaseCommand implements Command<CommandInvocation> {\n" +
            "    @Option(description = \"Debug mode\", hasValue = false)\n" +
            "    private boolean debug;\n" +
            "}\n";

    private static final String CHILD_COMMAND_SOURCE = "package test;\n" +
            "\n" +
            "import org.aesh.command.CommandDefinition;\n" +
            "import org.aesh.command.CommandResult;\n" +
            "import org.aesh.command.invocation.CommandInvocation;\n" +
            "import org.aesh.command.option.Option;\n" +
            "\n" +
            "@CommandDefinition(name = \"child\", description = \"Child command\")\n" +
            "public class ChildCommand extends BaseCommand {\n" +
            "    @Option(description = \"Name\")\n" +
            "    private String name;\n" +
            "\n" +
            "    @Override\n" +
            "    public CommandResult execute(CommandInvocation commandInvocation) {\n" +
            "        return CommandResult.SUCCESS;\n" +
            "    }\n" +
            "}\n";

    @Test
    public void testClassHierarchy() throws Exception {
        CompilationResult result = compileWithProcessor(
                new InMemorySource("test.BaseCommand", BASE_COMMAND_SOURCE),
                new InMemorySource("test.ChildCommand", CHILD_COMMAND_SOURCE));
        assertTrue("Compilation should succeed: " + result.diagnostics, result.success);

        Class<?> commandClass = result.classLoader.loadClass("test.ChildCommand");
        Class<?> metadataClass = result.classLoader.loadClass("test.ChildCommand_AeshMetadata");

        assertEquivalence(commandClass, metadataClass);
    }

    // --- Test: Group command ---

    private static final String SUB_COMMAND1_SOURCE = "package test;\n" +
            "\n" +
            "import org.aesh.command.Command;\n" +
            "import org.aesh.command.CommandDefinition;\n" +
            "import org.aesh.command.CommandResult;\n" +
            "import org.aesh.command.invocation.CommandInvocation;\n" +
            "import org.aesh.command.option.Option;\n" +
            "\n" +
            "@CommandDefinition(name = \"sub1\", description = \"Subcommand 1\")\n" +
            "public class SubCommand1 implements Command<CommandInvocation> {\n" +
            "    @Option(description = \"Value\")\n" +
            "    private String value;\n" +
            "\n" +
            "    @Override\n" +
            "    public CommandResult execute(CommandInvocation commandInvocation) {\n" +
            "        return CommandResult.SUCCESS;\n" +
            "    }\n" +
            "}\n";

    private static final String SUB_COMMAND2_SOURCE = "package test;\n" +
            "\n" +
            "import org.aesh.command.Command;\n" +
            "import org.aesh.command.CommandDefinition;\n" +
            "import org.aesh.command.CommandResult;\n" +
            "import org.aesh.command.invocation.CommandInvocation;\n" +
            "\n" +
            "@CommandDefinition(name = \"sub2\", description = \"Subcommand 2\")\n" +
            "public class SubCommand2 implements Command<CommandInvocation> {\n" +
            "    @Override\n" +
            "    public CommandResult execute(CommandInvocation commandInvocation) {\n" +
            "        return CommandResult.SUCCESS;\n" +
            "    }\n" +
            "}\n";

    private static final String GROUP_COMMAND_SOURCE = "package test;\n" +
            "\n" +
            "import org.aesh.command.Command;\n" +
            "import org.aesh.command.CommandResult;\n" +
            "import org.aesh.command.GroupCommandDefinition;\n" +
            "import org.aesh.command.invocation.CommandInvocation;\n" +
            "import org.aesh.command.option.Option;\n" +
            "\n" +
            "@GroupCommandDefinition(name = \"group\", description = \"Group command\",\n" +
            "        groupCommands = {SubCommand1.class, SubCommand2.class})\n" +
            "public class GroupTestCommand implements Command<CommandInvocation> {\n" +
            "    @Option(description = \"Shared option\")\n" +
            "    private String shared;\n" +
            "\n" +
            "    @Override\n" +
            "    public CommandResult execute(CommandInvocation commandInvocation) {\n" +
            "        return CommandResult.SUCCESS;\n" +
            "    }\n" +
            "}\n";

    @Test
    public void testGroupCommand() throws Exception {
        CompilationResult result = compileWithProcessor(
                new InMemorySource("test.SubCommand1", SUB_COMMAND1_SOURCE),
                new InMemorySource("test.SubCommand2", SUB_COMMAND2_SOURCE),
                new InMemorySource("test.GroupTestCommand", GROUP_COMMAND_SOURCE));
        assertTrue("Compilation should succeed: " + result.diagnostics, result.success);

        Class<?> commandClass = result.classLoader.loadClass("test.GroupTestCommand");
        Class<?> metadataClass = result.classLoader.loadClass("test.GroupTestCommand_AeshMetadata");

        // Verify group command metadata
        CommandMetadataProvider provider = (CommandMetadataProvider) metadataClass.newInstance();
        assertTrue("Should be a group command", provider.isGroupCommand());
        assertEquals("Should have 2 subcommands", 2, provider.groupCommandClasses().length);

        assertEquivalence(commandClass, metadataClass);
    }

    // --- Test: @Mixin support ---

    private static final String LOGGING_MIXIN_SOURCE = "package test;\n" +
            "\n" +
            "import org.aesh.command.option.Option;\n" +
            "\n" +
            "public class LoggingMixin {\n" +
            "    @Option(hasValue = false, description = \"Enable verbose output\")\n" +
            "    boolean verbose;\n" +
            "\n" +
            "    @Option(description = \"Log level\", defaultValue = \"INFO\")\n" +
            "    String level;\n" +
            "}\n";

    private static final String MIXIN_COMMAND_SOURCE = "package test;\n" +
            "\n" +
            "import org.aesh.command.Command;\n" +
            "import org.aesh.command.CommandDefinition;\n" +
            "import org.aesh.command.CommandResult;\n" +
            "import org.aesh.command.invocation.CommandInvocation;\n" +
            "import org.aesh.command.option.Mixin;\n" +
            "import org.aesh.command.option.Option;\n" +
            "import org.aesh.command.option.Argument;\n" +
            "\n" +
            "@CommandDefinition(name = \"mixcmd\", description = \"Command with mixin\")\n" +
            "public class MixinCommand implements Command<CommandInvocation> {\n" +
            "    @Mixin\n" +
            "    LoggingMixin logging;\n" +
            "\n" +
            "    @Option(description = \"Output file\")\n" +
            "    String output;\n" +
            "\n" +
            "    @Argument(description = \"Source file\")\n" +
            "    String source;\n" +
            "\n" +
            "    @Override\n" +
            "    public CommandResult execute(CommandInvocation commandInvocation) {\n" +
            "        return CommandResult.SUCCESS;\n" +
            "    }\n" +
            "}\n";

    @Test
    public void testMixinCommand() throws Exception {
        CompilationResult result = compileWithProcessor(
                new InMemorySource("test.LoggingMixin", LOGGING_MIXIN_SOURCE),
                new InMemorySource("test.MixinCommand", MIXIN_COMMAND_SOURCE));
        assertTrue("Compilation should succeed: " + result.diagnostics, result.success);

        Class<?> commandClass = result.classLoader.loadClass("test.MixinCommand");
        Class<?> metadataClass = result.classLoader.loadClass("test.MixinCommand_AeshMetadata");

        assertEquivalence(commandClass, metadataClass);
    }

    // --- Test: @Option with generic type (List<String>) should erase generics (#397) ---

    private static final String GENERIC_OPTION_SOURCE = "package test;\n" +
            "\n" +
            "import java.util.List;\n" +
            "\n" +
            "import org.aesh.command.Command;\n" +
            "import org.aesh.command.CommandDefinition;\n" +
            "import org.aesh.command.CommandResult;\n" +
            "import org.aesh.command.invocation.CommandInvocation;\n" +
            "import org.aesh.command.option.Option;\n" +
            "\n" +
            "@CommandDefinition(name = \"generic\", description = \"Generic option test\")\n" +
            "public class GenericOptionCommand implements Command<CommandInvocation> {\n" +
            "    @Option(name = \"docs\", description = \"Documentation reference\")\n" +
            "    List<String> docs;\n" +
            "\n" +
            "    @Override\n" +
            "    public CommandResult execute(CommandInvocation commandInvocation) {\n" +
            "        return CommandResult.SUCCESS;\n" +
            "    }\n" +
            "}\n";

    @Test
    public void testOptionWithGenericType() throws Exception {
        CompilationResult result = compileWithProcessor(
                new InMemorySource("test.GenericOptionCommand", GENERIC_OPTION_SOURCE));
        assertTrue("Compilation should succeed (generic type should be erased): " + result.diagnostics,
                result.success);
    }

    // --- Test: Compile-time validation catches abstract class ---

    private static final String ABSTRACT_COMMAND_SOURCE = "package test;\n" +
            "\n" +
            "import org.aesh.command.Command;\n" +
            "import org.aesh.command.CommandDefinition;\n" +
            "import org.aesh.command.CommandResult;\n" +
            "import org.aesh.command.invocation.CommandInvocation;\n" +
            "\n" +
            "@CommandDefinition(name = \"bad\", description = \"Bad command\")\n" +
            "public abstract class AbstractBadCommand implements Command<CommandInvocation> {\n" +
            "}\n";

    @Test
    public void testAbstractClassError() throws Exception {
        CompilationResult result = compileWithProcessor(
                new InMemorySource("test.AbstractBadCommand", ABSTRACT_COMMAND_SOURCE));
        // Should have compilation errors
        assertTrue("Should report error for abstract command class",
                result.diagnostics.toString().contains("must not be abstract"));
    }

    // --- Equivalence assertion ---

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void assertEquivalence(Class<?> commandClass, Class<?> metadataClass) throws Exception {
        // Build via reflection (existing path)
        AeshCommandContainerBuilder reflectionBuilder = new AeshCommandContainerBuilder();
        ProcessedCommand reflectionPC = reflectionBuilder.create(
                (Command) commandClass.newInstance()).getParser().getProcessedCommand();

        // Build via generated provider
        CommandMetadataProvider provider = (CommandMetadataProvider) metadataClass.newInstance();
        Command instance = (Command) commandClass.newInstance();
        ProcessedCommand generatedPC = provider.buildProcessedCommand(instance);

        // Assert structural equivalence
        assertEquals("Command name", reflectionPC.name(), generatedPC.name());
        assertEquals("Command description", reflectionPC.description(), generatedPC.description());
        assertEquals("Aliases", reflectionPC.getAliases(), generatedPC.getAliases());
        assertEquals("Version",
                reflectionPC.version() != null ? reflectionPC.version() : "",
                generatedPC.version() != null ? generatedPC.version() : "");

        // Compare options
        List<ProcessedOption> reflectionOpts = reflectionPC.getOptions();
        List<ProcessedOption> generatedOpts = generatedPC.getOptions();
        assertEquals("Number of options", reflectionOpts.size(), generatedOpts.size());

        for (int i = 0; i < reflectionOpts.size(); i++) {
            ProcessedOption rOpt = reflectionOpts.get(i);
            // Find matching option by name in generated (order may differ)
            ProcessedOption gOpt = findOptionByName(generatedOpts, rOpt.name());
            assertNotNull("Generated should have option: " + rOpt.name(), gOpt);

            assertEquals("Option name", rOpt.name(), gOpt.name());
            assertEquals("Option shortName for " + rOpt.name(), rOpt.shortName(), gOpt.shortName());
            assertEquals("Option description for " + rOpt.name(), rOpt.description(), gOpt.description());
            assertEquals("Option type for " + rOpt.name(), rOpt.type(), gOpt.type());
            assertEquals("Option fieldName for " + rOpt.name(), rOpt.getFieldName(), gOpt.getFieldName());
            assertEquals("Option optionType for " + rOpt.name(), rOpt.getOptionType(), gOpt.getOptionType());
            assertEquals("Option required for " + rOpt.name(), rOpt.isRequired(), gOpt.isRequired());
            assertEquals("Option defaultValues for " + rOpt.name(), rOpt.getDefaultValues(), gOpt.getDefaultValues());
            assertEquals("Option negatable for " + rOpt.name(), rOpt.isNegatable(), gOpt.isNegatable());
            assertEquals("Option inherited for " + rOpt.name(), rOpt.isInherited(), gOpt.isInherited());
            assertEquals("Option mixinFieldName for " + rOpt.name(), rOpt.getMixinFieldName(), gOpt.getMixinFieldName());
        }

        // Compare argument
        if (reflectionPC.getArgument() != null) {
            assertNotNull("Generated should have argument", generatedPC.getArgument());
            ProcessedOption rArg = reflectionPC.getArgument();
            ProcessedOption gArg = generatedPC.getArgument();
            assertEquals("Argument description", rArg.description(), gArg.description());
            assertEquals("Argument type", rArg.type(), gArg.type());
            assertEquals("Argument fieldName", rArg.getFieldName(), gArg.getFieldName());
            assertEquals("Argument optionType", rArg.getOptionType(), gArg.getOptionType());
            assertEquals("Argument required", rArg.isRequired(), gArg.isRequired());
        }

        // Compare arguments (plural)
        if (reflectionPC.getArguments() != null) {
            assertNotNull("Generated should have arguments", generatedPC.getArguments());
            ProcessedOption rArgs = reflectionPC.getArguments();
            ProcessedOption gArgs = generatedPC.getArguments();
            assertEquals("Arguments description", rArgs.description(), gArgs.description());
            assertEquals("Arguments type", rArgs.type(), gArgs.type());
            assertEquals("Arguments fieldName", rArgs.getFieldName(), gArgs.getFieldName());
            assertEquals("Arguments optionType", rArgs.getOptionType(), gArgs.getOptionType());
        }
    }

    private ProcessedOption findOptionByName(List<ProcessedOption> options, String name) {
        for (ProcessedOption opt : options) {
            if (opt.name().equals(name))
                return opt;
        }
        return null;
    }

    // --- In-memory compilation infrastructure ---

    private CompilationResult compileWithProcessor(InMemorySource... sources) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        Path outputDir = Files.createTempDirectory("aesh-processor-test");

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT,
                    Collections.singletonList(outputDir.toFile()));

            // Add aesh classes to the classpath
            String classpath = System.getProperty("java.class.path");
            String[] cpEntries = classpath.split(File.pathSeparator);
            List<File> cpFiles = new java.util.ArrayList<>();
            for (String entry : cpEntries) {
                cpFiles.add(new File(entry));
            }
            fileManager.setLocation(StandardLocation.CLASS_PATH, cpFiles);

            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, fileManager, diagnostics,
                    Arrays.asList("-proc:only", "-processor", AeshAnnotationProcessor.class.getName()),
                    null, Arrays.asList(sources));

            boolean procSuccess = task.call();

            // Now compile again with generated sources + original sources
            // First, collect generated source files
            List<JavaFileObject> allSources = new java.util.ArrayList<>(Arrays.asList(sources));

            // Find generated source files
            collectGeneratedSources(outputDir, allSources);

            // Compile everything (no annotation processing this time)
            DiagnosticCollector<JavaFileObject> compileDiagnostics = new DiagnosticCollector<>();
            JavaCompiler.CompilationTask compileTask = compiler.getTask(
                    null, fileManager, compileDiagnostics,
                    Arrays.asList("-proc:none"),
                    null, allSources);

            boolean compileSuccess = compileTask.call();
            boolean success = procSuccess && compileSuccess;

            // Build classloader from output
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[] { outputDir.toUri().toURL() },
                    getClass().getClassLoader());

            String allDiags = diagnostics.getDiagnostics().toString() + " " + compileDiagnostics.getDiagnostics().toString();
            return new CompilationResult(success, allDiags, classLoader, outputDir);
        }
    }

    private void collectGeneratedSources(Path dir, List<JavaFileObject> sources) throws IOException {
        Files.walk(dir)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> {
                    try {
                        String content = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                        // Determine the class name from path
                        String relativePath = dir.relativize(p).toString();
                        String className = relativePath.replace(File.separatorChar, '.')
                                .replace(".java", "");
                        sources.add(new InMemorySource(className, content));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static class CompilationResult {
        final boolean success;
        final String diagnostics;
        final URLClassLoader classLoader;
        final Path outputDir;

        CompilationResult(boolean success, String diagnostics, URLClassLoader classLoader, Path outputDir) {
            this.success = success;
            this.diagnostics = diagnostics;
            this.classLoader = classLoader;
            this.outputDir = outputDir;
        }
    }

    private static class InMemorySource extends SimpleJavaFileObject {
        private final String code;

        InMemorySource(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension),
                    Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
