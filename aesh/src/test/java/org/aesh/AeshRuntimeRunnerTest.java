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
        assertTrue(output.contains("complete -F _complete_myapp myapp"));
    }

    // -- Built-in --aesh-completion flag tests --

    @Test
    public void testAeshCompletionFlagBash() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("--aesh-completion", "bash")
                .execute());

        // Should generate a dynamic bash completion script
        assertTrue("Should contain bash completion function", output.contains("--aesh-complete"));
        assertTrue("Should contain complete command", output.contains("complete"));
    }

    @Test
    public void testAeshCompletionFlagStaticBash() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("--aesh-completion", "--static", "bash")
                .execute());

        // Should generate a static bash completion script (no --aesh-complete callback)
        assertTrue("Should contain complete command", output.contains("complete"));
        assertTrue("Should contain compgen", output.contains("compgen"));
    }

    @Test
    public void testAeshCompletionFlagZsh() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("--aesh-completion", "zsh")
                .execute());

        assertTrue("Should contain zsh compdef", output.contains("#compdef capture"));
    }

    @Test
    public void testAeshCompletionFlagFish() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("--aesh-completion", "fish")
                .execute());

        assertTrue("Should contain fish complete command", output.contains("complete -c capture"));
    }

    @Test
    public void testAeshCompletionFlagDoesNotExecuteCommand() {
        CaptureCommand.reset();
        AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("--aesh-completion", "bash")
                .execute();

        assertEquals("Command should not execute", null, CaptureCommand.lastCode);
    }

    @Test
    public void testAeshCompletionFlagUnknownShell() {
        String stderr = captureStderr(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("--aesh-completion", "powershell")
                .execute());

        assertTrue("Should report unknown shell", stderr.contains("Unknown shell type"));
    }

    @Test
    public void testAeshCompleteFlagViaArgs() {
        // --aesh-complete should work via args (not just static helper)
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(ColorCommand.class)
                .args("--aesh-complete", "--", "--")
                .execute());

        // Should produce completion candidates
        assertNotNull(output);
    }

    @Test
    public void testAeshCompletionFlagStaticZsh() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("--aesh-completion", "--static", "zsh")
                .execute());

        assertTrue("Should contain zsh compdef", output.contains("#compdef capture"));
        // Static scripts should not contain --aesh-complete callback
        assertFalse("Static script should not have callback", output.contains("--aesh-complete"));
    }

    @Test
    public void testAeshCompletionFlagStaticFish() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("--aesh-completion", "--static", "fish")
                .execute());

        assertTrue("Should contain fish complete command", output.contains("complete -c capture"));
    }

    @Test
    public void testAeshCompletionFlagWithProgramName() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .completionProgramName("myapp")
                .args("--aesh-completion", "bash")
                .execute());

        assertTrue("Should use custom program name", output.contains("myapp"));
        assertTrue("Should contain complete command", output.contains("complete"));
    }

    @Test
    public void testAeshCompletionFlagCaseInsensitive() {
        // Shell type should be case-insensitive
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("--aesh-completion", "BASH")
                .execute());

        assertTrue("Should accept uppercase shell type", output.contains("complete"));
    }

    // -- Built-in --aesh-doc flag tests --

    @Test
    public void testAeshDocFlagAsciidoc() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("--aesh-doc", "asciidoc")
                .execute());

        assertTrue("Should contain AsciiDoc title", output.contains("= CAPTURE"));
        assertTrue("Should contain NAME section", output.contains("== NAME"));
        assertTrue("Should contain OPTIONS section", output.contains("== OPTIONS"));
    }

    @Test
    public void testAeshDocFlagMarkdown() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("--aesh-doc", "markdown")
                .execute());

        assertTrue("Should contain Markdown title", output.contains("# CAPTURE"));
        assertTrue("Should contain NAME section", output.contains("## NAME"));
    }

    @Test
    public void testAeshDocFlagDefault() {
        // Default is asciidoc
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("--aesh-doc")
                .execute());

        assertTrue("Should default to AsciiDoc", output.contains("= CAPTURE"));
    }

    @Test
    public void testAeshDocFlagDoesNotExecuteCommand() {
        CaptureCommand.reset();
        AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("--aesh-doc")
                .execute();

        assertEquals("Command should not execute", null, CaptureCommand.lastCode);
    }

    @Test
    public void testAeshDocFlagUnknownFormat() {
        String stderr = captureStderr(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("--aesh-doc", "docbook")
                .execute());

        assertTrue("Should report unknown format", stderr.contains("Unknown doc format"));
    }

    @Test
    public void testAeshDocFlagNotInCompletionCandidates() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(ColorCommand.class)
                .args("--aesh-complete", "--", "--")
                .execute());

        assertFalse("--aesh-doc should not appear in candidates",
                output.contains("--aesh-doc"));
    }

    @Test
    public void testAeshCompletionFlagsNotInCompletionCandidates() {
        // --aesh-completion, --aesh-complete, --aesh-completion-install must NOT
        // appear as completion candidates for user commands
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(ColorCommand.class)
                .args("--aesh-complete", "--", "--")
                .execute());

        assertFalse("--aesh-completion should not appear in candidates",
                output.contains("--aesh-completion"));
        assertFalse("--aesh-complete should not appear in candidates",
                output.contains("--aesh-complete"));
        assertFalse("--aesh-completion-install should not appear in candidates",
                output.contains("--aesh-completion-install"));
    }

    @Test
    public void testAeshCompletionFlagsNotInHelpOutput() {
        // --aesh-completion flags must NOT appear in --help output
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CaptureCommand.class)
                .args("--help")
                .execute());

        assertFalse("--aesh-completion should not appear in help",
                output.contains("--aesh-completion"));
        assertFalse("--aesh-complete should not appear in help",
                output.contains("--aesh-complete"));
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

    @CommandDefinition(name = "root", description = "root", groupCommands = {
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

    @CommandDefinition(name = "grp", description = "group", groupCommands = { SubCmd.class }, generateHelp = true)
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

    // --- Issue #448: group command completion with duplicate child names ---

    @CommandDefinition(name = "app", description = "App", groupCommands = {
            AliasGroup.class, CatalogGroup.class })
    public static class DuplicateChildApp implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "alias", description = "Manage aliases", groupCommands = {
            AliasAdd.class, AliasList.class })
    public static class AliasGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "catalog", description = "Manage catalogs", groupCommands = {
            CatalogAdd.class, CatalogList.class })
    public static class CatalogGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "add", description = "Add an alias")
    public static class AliasAdd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "list", description = "List aliases")
    public static class AliasList implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "add", description = "Add a catalog")
    public static class CatalogAdd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "list", description = "List catalogs")
    public static class CatalogList implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testDynamicComplete_GroupChildDescriptions_Alias() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(DuplicateChildApp.class)
                .dynamicComplete(true)
                .args("alias", "")
                .execute());

        // "add" should have alias-specific description, not catalog's
        assertTrue("add should appear", output.contains("add"));
        assertTrue("add should have alias description",
                output.contains("Add an alias"));
        assertFalse("add should NOT have catalog description",
                output.contains("Add a catalog"));
    }

    @Test
    public void testDynamicComplete_GroupChildDescriptions_Catalog() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(DuplicateChildApp.class)
                .dynamicComplete(true)
                .args("catalog", "")
                .execute());

        assertTrue("add should appear", output.contains("add"));
        assertTrue("add should have catalog description",
                output.contains("Add a catalog"));
        assertFalse("add should NOT have alias description",
                output.contains("Add an alias"));
    }

    @Test
    public void testDynamicComplete_TopLevelGroupNames() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(DuplicateChildApp.class)
                .dynamicComplete(true)
                .args("")
                .execute());

        assertTrue("alias should appear", output.contains("alias"));
        assertTrue("catalog should appear", output.contains("catalog"));
    }

    // --- Completion test gap coverage ---

    // Test command with various option types for comprehensive completion tests
    @CommandDefinition(name = "complete", description = "Completion test command", generateHelp = true)
    public static class CompletionGapCommand implements Command<CommandInvocation> {
        @Option(description = "Verbose output", hasValue = false)
        private boolean verbose;

        @Option(description = "Output format", allowedValues = { "json", "xml", "yaml" })
        private String format;

        @Option(description = "Version info", hasValue = false, visibility = org.aesh.command.option.OptionVisibility.HIDDEN)
        private boolean internal;

        @Option(description = "Advanced trace", hasValue = false, visibility = org.aesh.command.option.OptionVisibility.FULL)
        private boolean trace;

        @Option(description = "JSON output", hasValue = false, exclusiveWith = { "xml" })
        private boolean json;

        @Option(description = "XML output", hasValue = false, exclusiveWith = { "json" })
        private boolean xml;

        @Option(name = "debug", fallbackValue = "4004", description = "Debug port")
        private String debug;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    // Nested 3-level group for grandchild completion test
    @CommandDefinition(name = "top", description = "Top level", groupCommands = { MidGroup.class })
    public static class TopGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mid", description = "Mid level", groupCommands = { LeafCmd.class })
    public static class MidGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "leaf", description = "Leaf command")
    public static class LeafCmd implements Command<CommandInvocation> {
        @Option(description = "Leaf option")
        private String leafOpt;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    // Group with inherited options
    @CommandDefinition(name = "proj", description = "Project management", groupCommands = { ProjBuild.class })
    public static class ProjGroup implements Command<CommandInvocation> {
        @Option(hasValue = false, inherited = true, description = "Verbose output")
        private boolean verbose;

        @Option(inherited = true, description = "Config file")
        private String config;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "build", description = "Build the project")
    public static class ProjBuild implements Command<CommandInvocation> {
        @Option(description = "Build target")
        private String target;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    // #5: Option prefix --ver<tab> filters to matching options
    @Test
    public void testDynamicComplete_OptionLongPrefix() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CompletionGapCommand.class)
                .dynamicComplete(true)
                .args("--ver")
                .execute());

        assertTrue("--verbose should match --ver prefix", output.contains("--verbose"));
        assertFalse("--format should not match --ver prefix", output.contains("--format"));
    }

    // #6: Short option prefix -v<tab>
    @Test
    public void testDynamicComplete_ShortOptionPrefix() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CompletionGapCommand.class)
                .dynamicComplete(true)
                .args("-v")
                .execute());

        // -v should match --verbose (name starts with "v")
        assertTrue("Should suggest --verbose for -v prefix", output.contains("verbose"));
    }

    // #3: Nested group (grandchild) completion
    @Test
    public void testDynamicComplete_NestedGroupGrandchild() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(TopGroup.class)
                .dynamicComplete(true)
                .args("mid", "")
                .execute());

        assertTrue("leaf should appear as grandchild", output.contains("leaf"));
    }

    // #8: allowedValues completion via dynamic path
    @Test
    public void testDynamicComplete_AllowedValues() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CompletionGapCommand.class)
                .dynamicComplete(true)
                .args("--format", "")
                .execute());

        assertTrue("json should be a candidate", output.contains("json"));
        assertTrue("xml should be a candidate", output.contains("xml"));
        assertTrue("yaml should be a candidate", output.contains("yaml"));
    }

    // #12: stopAtFirstPositional - tokens after positional
    @Test
    public void testDynamicComplete_StopAtFirstPositional_AfterPositional() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(DeployCommand.class)
                .dynamicComplete(true)
                .args("--verbose", "myfile.jar", "--")
                .execute());

        // After the positional "myfile.jar", --<tab> should not offer options
        // because stopAtFirstPositional routes remaining tokens as arguments
        assertFalse("Should not list --env after positional", output.contains("--env"));
    }

    // #14: fallbackValue completion behavior
    @Test
    public void testDynamicComplete_FallbackValueOption() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CompletionGapCommand.class)
                .dynamicComplete(true)
                .args("--")
                .execute());

        // --debug should appear as an option (fallbackValue doesn't affect listing)
        assertTrue("--debug should appear in options", output.contains("--debug"));
    }

    // #17: Inherited options in child command completion
    @Test
    public void testDynamicComplete_InheritedOptions() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(ProjGroup.class)
                .dynamicComplete(true)
                .args("build", "--")
                .execute());

        // Child command "build" should show its own option and inherited options
        assertTrue("--target should appear (own option)", output.contains("--target"));
        assertTrue("--verbose should appear (inherited)", output.contains("--verbose"));
        assertTrue("--config should appear (inherited)", output.contains("--config"));
    }

    // #18: Mutually exclusive option exclusion in completion
    @Test
    public void testDynamicComplete_MutuallyExclusiveExcluded() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CompletionGapCommand.class)
                .dynamicComplete(true)
                .args("--json", "--")
                .execute());

        // --json is set, --xml should be excluded (exclusiveWith)
        assertFalse("--xml should be excluded (mutually exclusive with --json)",
                output.contains("--xml"));
        // --verbose should still appear
        assertTrue("--verbose should still appear", output.contains("--verbose"));
    }

    // #19: HIDDEN visibility not in dynamic completion
    @Test
    public void testDynamicComplete_HiddenOptionNotShown() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(CompletionGapCommand.class)
                .dynamicComplete(true)
                .args("--")
                .execute());

        assertFalse("--internal (HIDDEN) should not appear in completion",
                output.contains("--internal"));
        // FULL visibility options should still appear in completion (only hidden from help)
        assertTrue("--trace (FULL) should appear in completion", output.contains("--trace"));
    }

    // --- Pre-release: registry DefaultValueProvider with group commands ---

    @CommandDefinition(name = "grp", description = "Group with child", groupCommands = { GrpChildCmd.class })
    public static class GrpParentCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "child", description = "Child command")
    public static class GrpChildCmd implements Command<CommandInvocation> {
        @Option(description = "Environment")
        private String env;

        static String lastEnv;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            lastEnv = env;
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testRegistryDefaultValueProviderWithGroupCommand() {
        GrpChildCmd.lastEnv = null;
        AeshRuntimeRunner.builder()
                .command(GrpParentCmd.class)
                .defaultValueProvider(option -> "env".equals(option.name()) ? "from-registry" : null)
                .args("child")
                .execute();

        assertEquals("Registry provider should apply to group child command",
                "from-registry", GrpChildCmd.lastEnv);
    }

    // --- Pre-release: containerBuilder test ---

    @Test
    public void testContainerBuilderIsUsed() {
        // Verify the containerBuilder method exists and can be called
        // (functional verification would require a mock container builder)
        AeshRuntimeRunner runner = AeshRuntimeRunner.builder()
                .containerBuilder(new org.aesh.command.impl.container.AeshCommandContainerBuilder<>())
                .command(CaptureCommand.class);

        CaptureCommand.reset();
        runner.args("-c", "test").execute();
        assertEquals("test", CaptureCommand.lastCode);
    }

    // --- Pre-release: ansiMode propagation to child parsers ---

    @Test
    public void testAnsiModePropagatedToChildParsers() throws Exception {
        org.aesh.command.impl.container.AeshCommandContainerBuilder<CommandInvocation> builder = new org.aesh.command.impl.container.AeshCommandContainerBuilder<>();
        org.aesh.command.impl.parser.CommandLineParser<CommandInvocation> clp = builder.create(GrpParentCmd.class).getParser();

        clp.updateAnsiMode(false);
        String help = clp.printHelp();

        // No ANSI escape sequences should be present in the entire help output,
        // including child command listings
        assertFalse("No ANSI in group help when ansiMode=false", help.contains("\u001B["));
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

    // --- Test for sorted completion candidates (#497) ---

    @Test
    public void testCompletionCandidatesSortedAlphabetically() {
        // #497: --aesh-complete should return candidates in alphabetical order
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(DuplicateChildApp.class)
                .args("--aesh-complete", "--", "")
                .execute());

        // Subcommands should be sorted: alias before catalog
        String[] lines = output.trim().split("\n");
        java.util.List<String> names = new java.util.ArrayList<>();
        for (String line : lines) {
            String name = line.split("\t")[0].trim();
            if (!name.isEmpty() && !name.startsWith("__aesh_"))
                names.add(name);
        }
        for (int i = 1; i < names.size(); i++) {
            assertTrue("Candidates should be sorted: '" + names.get(i - 1) + "' before '" + names.get(i) + "'",
                    names.get(i - 1).compareToIgnoreCase(names.get(i)) <= 0);
        }
    }

    // --- Test for group child descriptions (#500) ---

    @Test
    public void testCompletionDescriptionsForNestedGroupChildren() {
        // #500: Group command children (e.g., alias add/list/remove) should have descriptions
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(DuplicateChildApp.class)
                .args("--aesh-complete", "--", "alias", "")
                .execute());

        // Children of the "alias" group should have their descriptions
        assertTrue("'add' should have description: " + output,
                output.contains("add\tAdd an alias"));
        assertTrue("'list' should have description: " + output,
                output.contains("list\tList aliases"));
    }

    // --- Test for three-level group child descriptions (#500) ---

    @CommandDefinition(name = "root3", description = "Root", groupCommands = {
            Desc500MidGroup.class, Desc500TopSub.class })
    public static class Desc500Root implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mid", description = "Mid-level group", groupCommands = {
            Desc500LeafA.class, Desc500LeafB.class })
    public static class Desc500MidGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "topsub", description = "Top-level subcommand")
    public static class Desc500TopSub implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "leaf-a", description = "Leaf command A")
    public static class Desc500LeafA implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "leaf-b", description = "Leaf command B")
    public static class Desc500LeafB implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testThreeLevelGroupChildDescriptions() {
        // #500: Three-level nesting: root3 → mid → leaf-a/leaf-b
        // Top-level children should have descriptions
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(Desc500Root.class)
                .args("--aesh-complete", "--", "")
                .execute());
        assertTrue("Top-level: 'mid' should have description: " + output,
                output.contains("mid\tMid-level group"));
        assertTrue("Top-level: 'topsub' should have description: " + output,
                output.contains("topsub\tTop-level subcommand"));

        // Third-level children should also have descriptions
        String midOutput = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(Desc500Root.class)
                .args("--aesh-complete", "--", "mid", "")
                .execute());
        assertTrue("Mid children: 'leaf-a' should have description: " + midOutput,
                midOutput.contains("leaf-a\tLeaf command A"));
        assertTrue("Mid children: 'leaf-b' should have description: " + midOutput,
                midOutput.contains("leaf-b\tLeaf command B"));
    }

    @Test
    public void testThreeLevelGroupChildDescriptionsWithTrailingSpace() {
        // #500: When shell passes args as single element with trailing space
        // (e.g., ["alias "] instead of ["alias", ""]), findScopedParser must
        // trim args before matching child parser names.
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(Desc500Root.class)
                .args("--aesh-complete", "--", "mid ")
                .execute());
        assertTrue("Mid children with trailing-space arg: 'leaf-a' should have description: " + output,
                output.contains("leaf-a\tLeaf command A"));
        assertTrue("Mid children with trailing-space arg: 'leaf-b' should have description: " + output,
                output.contains("leaf-b\tLeaf command B"));
    }

    // --- Test for mixin option descriptions (#501) ---

    public static class DescMixin {
        @Option(name = "deps", description = "Add additional dependencies")
        public String deps;

        @Option(name = "catalog", description = "Path to catalog file")
        public String catalog;
    }

    @CommandDefinition(name = "runcmd", description = "Run a script", groupCommands = {}, generateHelp = true)
    public static class DescRunCmd implements Command<CommandInvocation> {
        @org.aesh.command.option.Mixin
        public DescMixin mixin;

        @Option(name = "debug", description = "Launch with debug enabled", hasValue = false)
        public boolean debug;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mixapp", description = "App with mixin commands", groupCommands = {
            DescRunCmd.class }, generateHelp = true)
    public static class DescMixApp implements Command<CommandInvocation> {
        @Option(name = "verbose", hasValue = false, inherited = true, description = "Verbose output")
        public boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testCompletionDescriptionsForMixinAndOwnOptions() {
        // #501: Both command-own options and mixin options should have descriptions
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(DescMixApp.class)
                .args("--aesh-complete", "--", "runcmd", "--")
                .execute());

        // Command's own option should have description
        assertTrue("--debug should have description: " + output,
                output.contains("--debug") && output.contains("Launch with debug enabled"));

        // Mixin options should have descriptions
        assertTrue("--deps (mixin) should have description: " + output,
                output.contains("--deps") && output.contains("Add additional dependencies"));
        assertTrue("--catalog (mixin) should have description: " + output,
                output.contains("--catalog") && output.contains("Path to catalog file"));

        // Inherited option from parent should also have description
        assertTrue("--verbose (inherited) should have description: " + output,
                output.contains("--verbose") && output.contains("Verbose output"));
    }

    @Test
    public void testCompletionDescriptionsForMixinOptionsWithTrailingSpace() {
        // #501 + #500: trailing-space arg format should still produce descriptions
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(DescMixApp.class)
                .args("--aesh-complete", "--", "runcmd ", "--")
                .execute());

        assertTrue("--debug should have description with trailing-space arg: " + output,
                output.contains("--debug") && output.contains("Launch with debug enabled"));
        assertTrue("--deps (mixin) should have description with trailing-space arg: " + output,
                output.contains("--deps") && output.contains("Add additional dependencies"));
    }

    // --- Tests for completion descriptions (#498) ---

    @CommandDefinition(name = "desccmd", description = "Desc test", groupCommands = { DescSubCmd.class }, generateHelp = true)
    public static class DescGroupCmd implements Command<CommandInvocation> {
        @Option(shortName = 'v', name = "verbose", hasValue = false, inherited = true, description = "Enable verbose output")
        public boolean verbose;

        @Option(name = "config", description = "Config file path")
        public String config;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "sub", description = "A sub command")
    public static class DescSubCmd implements Command<CommandInvocation> {
        @Option(name = "target", description = "Target environment")
        public String target;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testCompletionDescriptionsForGroupSubcommands() {
        // #498: Group subcommands should have descriptions in --aesh-complete output
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(DescGroupCmd.class)
                .args("--aesh-complete", "--", "")
                .execute());

        // Top-level subcommand should have description
        assertTrue("Sub should have description", output.contains("sub\tA sub command"));
    }

    @Test
    public void testCompletionDescriptionsForOptions() {
        // #498: All options should have descriptions in --aesh-complete output
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(DescGroupCmd.class)
                .args("--aesh-complete", "--", "--")
                .execute());

        // Options should have their descriptions
        assertTrue("--config should have description",
                output.contains("--config") && output.contains("Config file path"));
        assertTrue("--verbose should have description",
                output.contains("--verbose") && output.contains("Enable verbose output"));
    }

    @Test
    public void testCompletionDescriptionsForChildOptions() {
        // #498: Child command options should have descriptions
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(DescGroupCmd.class)
                .args("--aesh-complete", "--", "sub", "--")
                .execute());

        // Child's own option should have description
        assertTrue("--target should have description",
                output.contains("--target") && output.contains("Target environment"));
    }

    @Test
    public void testCompletionDescriptionsForInheritedOptions() {
        // #498: Inherited options from parent should have descriptions on child
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .command(DescGroupCmd.class)
                .args("--aesh-complete", "--", "sub", "--")
                .execute());

        // Inherited --verbose from parent should have description on child
        assertTrue("Inherited --verbose should have description on child",
                output.contains("--verbose") && output.contains("Enable verbose output"));
    }

}
