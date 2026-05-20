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
package org.aesh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.util.completer.ShellCompletionGenerator.ShellType;
import org.junit.Test;

public class AeshRuntimeRunnerTest {

    @Test
    public void testCommandResult() throws InterruptedException {
        CommandResult result = AeshRuntimeRunner.builder().command(Bar1Command.class).execute();
        Thread.sleep(200);
        assertEquals(CommandResult.SUCCESS.getResultValue(), result.getResultValue());

        result = AeshRuntimeRunner.builder().command(Bar2Command.class).execute();
        Thread.sleep(200);
        assertEquals(CommandResult.FAILURE.getResultValue(), result.getResultValue());

    }

    @Test
    public void testInstantiatedCommand() throws InterruptedException {
        Bar1Command bar1Cmd = new Bar1Command();

        CommandResult result = AeshRuntimeRunner.builder().command(bar1Cmd).execute();
        Thread.sleep(200);
        assertEquals(CommandResult.SUCCESS.getResultValue(), result.getResultValue());
        assertEquals(100, bar1Cmd.getSomeVal());

    }

    @Test
    public void testArgsWithEmbeddedQuotes() {
        CaptureCommand.reset();
        CommandResult result = AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("-c", "Hello \"World\"", "myarg")
                .execute();
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("Hello \"World\"", CaptureCommand.lastCode);
        assertEquals("myarg", CaptureCommand.lastArg);
    }

    @Test
    public void testArgsWithBackslashes() {
        CaptureCommand.reset();
        CommandResult result = AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("-c", "path\\to\\file", "myarg")
                .execute();
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("path\\to\\file", CaptureCommand.lastCode);
        assertEquals("myarg", CaptureCommand.lastArg);
    }

    @Test
    public void testArgsWithNewlines() {
        CaptureCommand.reset();
        String multiline = "line1\nline2\nline3";
        CommandResult result = AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("-c", multiline, "myarg")
                .execute();
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals(multiline, CaptureCommand.lastCode);
        assertEquals("myarg", CaptureCommand.lastArg);
    }

    @Test
    public void testArgsWithMixedSpecialChars() {
        CaptureCommand.reset();
        String javaCode = "public class Hello {\n    public static void main(String... args) {\n"
                + "        System.out.println(\"Hello\");\n    }\n}";
        CommandResult result = AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("-c", javaCode, "firstarg")
                .execute();
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals(javaCode, CaptureCommand.lastCode);
        assertEquals("firstarg", CaptureCommand.lastArg);
    }

    @Test
    public void testArgsWithSpacesOnly() {
        CaptureCommand.reset();
        CommandResult result = AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("-c", "hello world", "my arg")
                .execute();
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("hello world", CaptureCommand.lastCode);
        assertEquals("my arg", CaptureCommand.lastArg);
    }

    @Test
    public void testArgsWithSingleQuotes() {
        CaptureCommand.reset();
        CommandResult result = AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("-c", "it's a test", "myarg")
                .execute();
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("it's a test", CaptureCommand.lastCode);
        assertEquals("myarg", CaptureCommand.lastArg);
    }

    @Test
    public void testArgsWithOperatorChars() {
        CaptureCommand.reset();
        CommandResult result = AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("-c", "echo hello; echo world | grep foo", "myarg")
                .execute();
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("echo hello; echo world | grep foo", CaptureCommand.lastCode);
        assertEquals("myarg", CaptureCommand.lastArg);
    }

    @Test
    public void testNullArgs() {
        CaptureCommand.reset();
        CommandResult result = AeshRuntimeRunner.builder()
                .command(Bar1Command.class)
                .execute();
        assertEquals(CommandResult.SUCCESS, result);
    }

    @CommandDefinition(name = "capture", description = "captures args for testing")
    public static class CaptureCommand implements Command<CommandInvocation> {
        @Option(shortName = 'c', description = "Code")
        private String code;
        @Argument(description = "First argument")
        private String arg;

        static String lastCode;
        static String lastArg;

        static void reset() {
            lastCode = null;
            lastArg = null;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            lastCode = code;
            lastArg = arg;
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testGenerateBashCompletion() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .generateCompletion(ShellType.BASH)
                .execute());

        assertTrue(output.contains("#!/usr/bin/env bash"));
        assertTrue(output.contains("_complete_capture"));
        assertTrue(output.contains("--code"));
    }

    @Test
    public void testGenerateZshCompletion() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .generateCompletion(ShellType.ZSH)
                .execute());

        assertTrue(output.contains("#compdef capture"));
        assertTrue(output.contains("--code"));
    }

    @Test
    public void testGenerateFishCompletion() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .generateCompletion(ShellType.FISH)
                .execute());

        assertTrue(output.contains("complete -c capture"));
        assertTrue(output.contains("-l code"));
    }

    @Test
    public void testGenerateCompletionWithCustomProgramName() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .generateCompletion(ShellType.BASH)
                .completionProgramName("myapp")
                .execute());

        assertTrue(output.contains("_complete_myapp"));
        assertTrue(output.contains("complete -o default -F _complete_myapp myapp"));
    }

    @Test
    public void testGenerateCompletionDoesNotExecuteCommand() {
        CaptureCommand.reset();
        AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .generateCompletion(ShellType.BASH)
                .args("-c", "should-not-run")
                .execute();

        // The command should NOT have been executed
        assertEquals(null, CaptureCommand.lastCode);
    }

    // -- Dynamic completion tests --

    @Test
    public void testGenerateDynamicBashCompletion() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .generateDynamicCompletion(ShellType.BASH)
                .execute());

        assertTrue(output.contains("--aesh-complete"));
        assertTrue(output.contains("_complete_capture"));
    }

    @Test
    public void testGenerateDynamicZshCompletion() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .generateDynamicCompletion(ShellType.ZSH)
                .execute());

        assertTrue(output.contains("#compdef capture"));
        assertTrue(output.contains("--aesh-complete"));
    }

    @Test
    public void testGenerateDynamicFishCompletion() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .generateDynamicCompletion(ShellType.FISH)
                .execute());

        assertTrue(output.contains("complete -c capture"));
        assertTrue(output.contains("--aesh-complete"));
    }

    @Test
    public void testGenerateDynamicCompletionWithCustomProgramName() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .generateDynamicCompletion(ShellType.BASH)
                .completionProgramName("myapp")
                .execute());

        assertTrue(output.contains("_complete_myapp"));
        assertTrue(output.contains("complete -o default -F _complete_myapp myapp"));
    }

    @Test
    public void testDynamicCompleteOptions() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(ColorCommand.class)
                .dynamicComplete(true)
                .args("--")
                .execute());

        assertTrue("Should complete option names", output.contains("color"));
    }

    @Test
    public void testDynamicCompleteCustomCompleter() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(ColorCommand.class)
                .dynamicComplete(true)
                .args("--color", "gr")
                .execute());

        assertTrue("Should have custom completer results", output.contains("een"));
    }

    @Test
    public void testDynamicCompleteDoesNotExecuteCommand() {
        CaptureCommand.reset();
        AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .dynamicComplete(true)
                .args("-c", "should-not-run")
                .execute();

        assertEquals("Command should not have been executed", null, CaptureCommand.lastCode);
    }

    @Test
    public void testHandleDynamicCompletion() {
        String output = captureStdout(() -> {
            boolean handled = AeshRuntimeRunner.handleDynamicCompletion(
                    new String[] { "--aesh-complete", "--", "--" },
                    ColorCommand.class);
            assertTrue("Should return true for --aesh-complete", handled);
        });

        assertTrue("Should produce completion output", output.contains("color"));
    }

    @Test
    public void testHandleDynamicCompletionReturnsFalseForNormalArgs() {
        assertFalse("Should return false for normal args",
                AeshRuntimeRunner.handleDynamicCompletion(
                        new String[] { "--verbose" }, ColorCommand.class));
    }

    @Test
    public void testHandleDynamicCompletionReturnsFalseForEmptyArgs() {
        assertFalse("Should return false for empty args",
                AeshRuntimeRunner.handleDynamicCompletion(
                        new String[0], ColorCommand.class));
    }

    @Test
    public void testHandleDynamicCompletionReturnsFalseForNull() {
        assertFalse("Should return false for null args",
                AeshRuntimeRunner.handleDynamicCompletion(null, ColorCommand.class));
    }

    // -- Test command with custom completer --

    public static class ColorCompleter implements OptionCompleter<CompleterInvocation> {
        private static final List<String> COLORS = Arrays.asList(
                "red", "green", "blue", "yellow", "orange", "purple");

        @Override
        public void complete(CompleterInvocation invocation) {
            String input = invocation.getGivenCompleteValue();
            if (input == null || input.isEmpty()) {
                invocation.addAllCompleterValues(COLORS);
            } else {
                String lower = input.toLowerCase();
                for (String color : COLORS) {
                    if (color.startsWith(lower)) {
                        invocation.addCompleterValue(color);
                    }
                }
            }
        }
    }

    @CommandDefinition(name = "colorpick", description = "Pick a color")
    public static class ColorCommand implements Command<CommandInvocation> {
        @Option(completer = ColorCompleter.class, description = "The color")
        private String color;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class AlphaCompleter implements OptionCompleter<CompleterInvocation> {
        @Override
        public void complete(CompleterInvocation invocation) {
            String input = invocation.getGivenCompleteValue();
            if (input == null)
                input = "";
            for (String candidate : Arrays.asList("alpha", "bravo", "charlie")) {
                if (candidate.startsWith(input))
                    invocation.addCompleterValue(candidate);
            }
        }
    }

    public static class NumberCompleter implements OptionCompleter<CompleterInvocation> {
        @Override
        public void complete(CompleterInvocation invocation) {
            String input = invocation.getGivenCompleteValue();
            if (input == null)
                input = "";
            for (String candidate : Arrays.asList("one", "two", "three")) {
                if (candidate.startsWith(input))
                    invocation.addCompleterValue(candidate);
            }
        }
    }

    @CommandDefinition(name = "argonly", description = "argument-only")
    public static class ArgumentOnlyCommand implements Command<CommandInvocation> {
        @Argument(completer = AlphaCompleter.class)
        private String first;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "both", description = "argument and arguments")
    public static class ArgumentAndArgumentsCommand implements Command<CommandInvocation> {
        @Argument(completer = AlphaCompleter.class)
        private String first;

        @Arguments
        private List<String> rest;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "multi", description = "multiple indexed arguments")
    public static class MultipleIndexedArgumentsCommand implements Command<CommandInvocation> {
        @Argument(index = "1", completer = AlphaCompleter.class)
        private String second;

        @Argument(index = "0", completer = NumberCompleter.class)
        private String first;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "root", description = "root", groupCommands = {
            ArgumentOnlyCommand.class,
            ArgumentAndArgumentsCommand.class,
            MultipleIndexedArgumentsCommand.class
    })
    public static class ArgumentCompletionGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testDynamicCompleteArgumentCompleterWithOnlyArgument() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(ArgumentCompletionGroup.class)
                .dynamicComplete(true)
                .args("argonly", "a")
                .execute());

        assertTrue("Should complete @Argument value", output.contains("alpha"));
    }

    @Test
    public void testDynamicCompleteArgumentCompleterWithArgumentAndArguments() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(ArgumentCompletionGroup.class)
                .dynamicComplete(true)
                .args("both", "a")
                .execute());

        assertTrue("Should complete @Argument even when @Arguments exists", output.contains("alpha"));
    }

    @Test
    public void testDynamicCompleteMultipleIndexedArgumentsFirstSlot() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(ArgumentCompletionGroup.class)
                .dynamicComplete(true)
                .args("multi", "t")
                .execute());

        assertTrue("Should use index 0 argument completer", output.contains("two"));
    }

    @Test
    public void testDynamicCompleteMultipleIndexedArgumentsSecondSlot() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(ArgumentCompletionGroup.class)
                .dynamicComplete(true)
                .args("multi", "one", "a")
                .execute());

        assertTrue("Should use index 1 argument completer", output.contains("alpha"));
    }

    private static String captureStdout(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return baos.toString();
    }

    // -- Error handling tests --

    @Test
    public void testParseErrorReturnsExitCode2() {
        CommandResult result = AeshRuntimeRunner.builder()
                .command(RequiredOptionCmd.class)
                .execute();
        assertEquals(2, result.getResultValue());
    }

    @Test
    public void testParseErrorPrintsHelp() {
        String stderr = captureStderr(() -> AeshRuntimeRunner.builder()
                .command(RequiredOptionCmd.class)
                .execute());
        assertTrue("Should print help with option name", stderr.contains("--name"));
    }

    @Test
    public void testInvalidOptionReturnsExitCode2() {
        String stderr = captureStderr(() -> {
            CommandResult result = AeshRuntimeRunner.builder()
                    .command(Bar1Command.class)
                    .args("--nonexistent", "value")
                    .execute();
            assertEquals(2, result.getResultValue());
        });
        assertTrue("Should mention unknown option", stderr.contains("nonexistent"));
    }

    @Test
    public void testAllowedValuesValidationError() {
        try {
            AeshRuntimeRunner.builder()
                    .command(FormatCommand.class)
                    .args("--format", "xml")
                    .execute();
        } catch (RuntimeException e) {
            assertTrue("Should mention invalid value",
                    e.getMessage().contains("xml") || e.getCause().getMessage().contains("xml"));
            return;
        }
        // If no exception, check exit code
    }

    @Test
    public void testAllowedValuesAccepted() {
        FormatCommand.lastFormat = null;
        CommandResult result = AeshRuntimeRunner.builder()
                .command(FormatCommand.class)
                .args("--format", "json")
                .execute();
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("json", FormatCommand.lastFormat);
    }

    // -- Group command tests --

    @Test
    public void testGroupCommandExecution() {
        SubCmd.executed = false;
        SubCmd.lastValue = null;
        CommandResult result = AeshRuntimeRunner.builder()
                .command(GroupCmd.class)
                .args("sub", "--value", "hello")
                .execute();
        assertEquals(CommandResult.SUCCESS, result);
        assertTrue(SubCmd.executed);
        assertEquals("hello", SubCmd.lastValue);
    }

    @Test
    public void testGroupCommandHelpOnParseError() {
        String stderr = captureStderr(() -> AeshRuntimeRunner.builder()
                .command(GroupCmd.class)
                .args("sub", "--unknown")
                .execute());
        assertTrue("Should show subcommand help", stderr.contains("sub"));
        assertTrue("Should mention --value option", stderr.contains("value"));
    }

    @Test
    public void testGroupCommandCompletionScript() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(GroupCmd.class)
                .generateCompletion(ShellType.BASH)
                .execute());
        assertTrue("Should contain group command name", output.contains("grp"));
        assertTrue("Should contain subcommand", output.contains("sub"));
    }

    // -- Enum option test --

    @Test
    public void testEnumOption() {
        EnumCmd.lastLevel = null;
        CommandResult result = AeshRuntimeRunner.builder()
                .command(EnumCmd.class)
                .args("--level", "warn")
                .execute();
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals(LogLevel.WARN, EnumCmd.lastLevel);
    }

    @Test
    public void testEnumOptionCaseInsensitive() {
        EnumCmd.lastLevel = null;
        CommandResult result = AeshRuntimeRunner.builder()
                .command(EnumCmd.class)
                .args("--level", "DEBUG")
                .execute();
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals(LogLevel.DEBUG, EnumCmd.lastLevel);
    }

    @Test
    public void testEnumDynamicCompletion() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(EnumCmd.class)
                .dynamicComplete(true)
                .args("--level", "")
                .execute());
        assertTrue("Should contain enum values", output.contains("debug"));
        assertTrue("Should contain enum values", output.contains("info"));
        assertTrue("Should contain enum values", output.contains("warn"));
    }

    // -- No command registered --

    @Test(expected = RuntimeException.class)
    public void testNoCommandThrows() {
        AeshRuntimeRunner.builder().execute();
    }

    // -- Test command classes --

    @CommandDefinition(name = "reqopt", description = "required option", generateHelp = true)
    public static class RequiredOptionCmd implements Command<CommandInvocation> {
        @Option(name = "name", required = true, description = "A required name")
        private String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "fmt", description = "format command")
    public static class FormatCommand implements Command<CommandInvocation> {
        static String lastFormat;

        @Option(name = "format", allowedValues = { "text", "json", "yaml" })
        private String format;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            lastFormat = format;
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "grp", description = "group", groupCommands = { SubCmd.class }, generateHelp = true)
    public static class GroupCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "sub", description = "subcommand", generateHelp = true)
    public static class SubCmd implements Command<CommandInvocation> {
        static volatile boolean executed;
        static volatile String lastValue;

        @Option(name = "value", description = "A value")
        private String value;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            executed = true;
            lastValue = value;
            return CommandResult.SUCCESS;
        }
    }

    public enum LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    @CommandDefinition(name = "log", description = "log command")
    public static class EnumCmd implements Command<CommandInvocation> {
        static LogLevel lastLevel;

        @Option(name = "level")
        private LogLevel level;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            lastLevel = level;
            return CommandResult.SUCCESS;
        }
    }

    private static String captureStderr(Runnable action) {
        PrintStream original = System.err;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setErr(new PrintStream(baos));
        try {
            action.run();
        } finally {
            System.setErr(original);
        }
        return baos.toString();
    }

    @CommandDefinition(name = "bar1", description = "bar1")
    public static class Bar1Command implements Command {
        private static int someVal = 0;

        public int getSomeVal() {
            return someVal;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            commandInvocation.println("Hello from Bar1");
            someVal = 100;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "bar2", description = "bar2")
    public static class Bar2Command implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            commandInvocation.println("Hello from Bar2, lets fail");
            return CommandResult.FAILURE;
        }
    }

    // ========== Completion install tests ==========

    @Test
    public void testCompletionInstallPathBash() {
        java.io.File path = AeshRuntimeRunner.getCompletionInstallPath(ShellType.BASH, "mycli");
        assertNotNull(path);
        assertTrue("Bash path should contain .bash_completion.d",
                path.getAbsolutePath().contains(".bash_completion.d"));
        assertTrue("Bash path should end with program name",
                path.getName().equals("mycli"));
    }

    @Test
    public void testCompletionInstallPathZsh() {
        java.io.File path = AeshRuntimeRunner.getCompletionInstallPath(ShellType.ZSH, "mycli");
        assertNotNull(path);
        assertTrue("Zsh path should contain .zsh and completions",
                path.getAbsolutePath().contains(".zsh") && path.getAbsolutePath().contains("completions"));
        assertTrue("Zsh completion file should be prefixed with underscore",
                path.getName().equals("_mycli"));
    }

    @Test
    public void testCompletionInstallPathFish() {
        java.io.File path = AeshRuntimeRunner.getCompletionInstallPath(ShellType.FISH, "mycli");
        assertNotNull(path);
        assertTrue("Fish path should contain fish and completions",
                path.getAbsolutePath().contains("fish") && path.getAbsolutePath().contains("completions"));
        assertTrue("Fish completion file should have .fish extension",
                path.getName().equals("mycli.fish"));
    }

    // --- Issue #443: dynamic completion fixes ---

    @CommandDefinition(name = "deploy", description = "Deploy app", stopAtFirstPositional = true)
    public static class DeployCommand implements Command<CommandInvocation> {
        @Option(hasValue = false, description = "Verbose output")
        private boolean verbose;

        @Option(hasValue = false, negatable = true, description = "Enable CDS")
        private boolean cds;

        @Option(description = "Target environment")
        private String env;

        @Argument(description = "Artifact file")
        private String artifact;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testDynamicComplete_PositionalShowsArgumentNotOptions() {
        // Bug #443.1: at a positional argument position, should not list options
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(DeployCommand.class)
                .dynamicComplete(true)
                .args("--verbose", "")
                .execute());

        // After --verbose and a space, cursor is at the argument position
        // Should NOT list --env, --cds etc. as the primary completion
        assertFalse("Should not list --env at argument position", output.contains("--env"));
    }

    @Test
    public void testDynamicComplete_NegatableOptionHasDescription() {
        // Bug #443.2: negated form should have a description
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(DeployCommand.class)
                .dynamicComplete(true)
                .args("--")
                .execute());

        // Both --cds and --no-cds should appear, and --no-cds should have a description
        assertTrue("--cds should appear", output.contains("--cds"));
        assertTrue("--no-cds should appear", output.contains("--no-cds"));
        // --no-cds line should include the description tab-separated
        boolean negatedHasDesc = false;
        for (String line : output.split("\n")) {
            if (line.contains("--no-cds") && line.contains("\t")) {
                negatedHasDesc = true;
                break;
            }
        }
        assertTrue("--no-cds should have a description", negatedHasDesc);
    }

    @Test
    public void testDynamicComplete_AlreadyUsedOptionExcluded() {
        // Bug #443.4: --verbose already specified should not appear in completion
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(DeployCommand.class)
                .dynamicComplete(true)
                .args("--verbose", "--")
                .execute());

        assertFalse("--verbose should not appear (already used)", output.contains("--verbose"));
        assertTrue("--env should still appear", output.contains("--env"));
    }

    // --- Issue #445: registry-level DefaultValueProvider ---

    @CommandDefinition(name = "nodvp", description = "Command without per-command provider")
    public static class NoDvpCommand implements Command<CommandInvocation> {
        @Option(description = "Template")
        private String template;

        static String lastTemplate;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            lastTemplate = template;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "withdvp", description = "Command with per-command provider", defaultValueProvider = PerCommandProvider.class)
    public static class WithDvpCommand implements Command<CommandInvocation> {
        @Option(description = "Template")
        private String template;

        static String lastTemplate;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            lastTemplate = template;
            return CommandResult.SUCCESS;
        }
    }

    public static class RegistryProvider implements org.aesh.command.DefaultValueProvider {
        @Override
        public String defaultValue(org.aesh.command.impl.internal.ProcessedOption option) {
            if ("template".equals(option.name()))
                return "from-registry";
            return null;
        }
    }

    public static class PerCommandProvider implements org.aesh.command.DefaultValueProvider {
        @Override
        public String defaultValue(org.aesh.command.impl.internal.ProcessedOption option) {
            if ("template".equals(option.name()))
                return "from-command";
            return null;
        }
    }

    @Test
    public void testRegistryLevelDefaultValueProvider() {
        NoDvpCommand.lastTemplate = null;
        AeshRuntimeRunner.builder()
                .command(NoDvpCommand.class)
                .defaultValueProvider(new RegistryProvider())
                .execute();

        assertEquals("Registry provider should apply", "from-registry", NoDvpCommand.lastTemplate);
    }

    @Test
    public void testPerCommandProviderOverridesRegistry() {
        WithDvpCommand.lastTemplate = null;
        AeshRuntimeRunner.builder()
                .command(WithDvpCommand.class)
                .defaultValueProvider(new RegistryProvider())
                .execute();

        assertEquals("Per-command provider should override registry", "from-command", WithDvpCommand.lastTemplate);
    }

    @Test
    public void testUserValueOverridesRegistryProvider() {
        NoDvpCommand.lastTemplate = null;
        AeshRuntimeRunner.builder()
                .command(NoDvpCommand.class)
                .defaultValueProvider(new RegistryProvider())
                .args("--template", "user-value")
                .execute();

        assertEquals("User value should override registry provider", "user-value", NoDvpCommand.lastTemplate);
    }

    @Test
    public void testHandleCompletionInstallIgnoresOtherFlags() {
        assertFalse(AeshRuntimeRunner.handleCompletionInstall(
                new String[] { "--help" }, CaptureCommand.class));
        assertFalse(AeshRuntimeRunner.handleCompletionInstall(
                new String[] {}, CaptureCommand.class));
        assertFalse(AeshRuntimeRunner.handleCompletionInstall(
                null, CaptureCommand.class));
    }

}
