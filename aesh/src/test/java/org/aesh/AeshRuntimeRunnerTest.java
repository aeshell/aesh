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
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
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

}
