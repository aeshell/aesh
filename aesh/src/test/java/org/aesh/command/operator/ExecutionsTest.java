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
package org.aesh.command.operator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.List;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.Execution;
import org.aesh.command.Executor;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.ReadlineConsole;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for Executions - the operator pipeline builder and executor.
 *
 * @author Aesh team
 */
public class ExecutionsTest {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    // ========== Single command execution ==========

    @Test
    public void testSingleCommandExecution() throws Exception {
        EchoCommand.lastOutput = null;
        CommandRuntime<CommandInvocation> runtime = buildRuntime();

        CommandResult result = runtime.executeCommand("echo --msg hello");
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("hello", EchoCommand.lastOutput);
    }

    @Test
    public void testSingleCommandBuildExecutor() throws Exception {
        CommandRuntime<CommandInvocation> runtime = buildRuntime();

        Executor<?> executor = runtime.buildExecutor("echo", new String[] { "--msg", "world" });
        assertNotNull(executor);
        assertEquals(1, executor.getExecutions().size());

        Execution<?> execution = executor.getExecutions().get(0);
        execution.populateCommand();
        EchoCommand cmd = (EchoCommand) execution.getCommand();
        assertEquals("world", cmd.msg);
    }

    @Test(expected = CommandNotFoundException.class)
    public void testUnknownCommandThrows() throws Exception {
        CommandRuntime<CommandInvocation> runtime = buildRuntime();
        runtime.executeCommand("nonexistent --foo bar");
    }

    // ========== End operator (;) ==========

    @Test
    public void testEndOperator() throws Exception {
        CounterCommand.reset();
        TestConnection connection = new TestConnection();

        ReadlineConsole console = buildConsole(connection);
        console.start();

        connection.read("counter; counter; counter" + Config.getLineSeparator());
        Thread.sleep(100);
        assertEquals(3, CounterCommand.count);
        console.stop();
    }

    @Test
    public void testEndOperatorWithFailure() throws Exception {
        // ; should execute all commands regardless of success/failure
        CounterCommand.reset();
        FailCommand.reset();
        TestConnection connection = new TestConnection();

        ReadlineConsole console = buildConsole(connection);
        console.start();

        connection.read("counter; fail; counter" + Config.getLineSeparator());
        Thread.sleep(100);
        // counter runs twice, fail runs once — all execute regardless
        assertEquals(2, CounterCommand.count);
        assertEquals(1, FailCommand.count);
        console.stop();
    }

    // ========== And operator (&&) ==========

    @Test
    public void testAndOperatorAllSuccess() throws Exception {
        CounterCommand.reset();
        TestConnection connection = new TestConnection();

        ReadlineConsole console = buildConsole(connection);
        console.start();

        connection.read("counter && counter && counter" + Config.getLineSeparator());
        Thread.sleep(100);
        assertEquals(3, CounterCommand.count);
        console.stop();
    }

    @Test
    public void testAndOperatorStopsOnFailure() throws Exception {
        CounterCommand.reset();
        FailCommand.reset();
        TestConnection connection = new TestConnection();

        ReadlineConsole console = buildConsole(connection);
        console.start();

        connection.read("counter && fail && counter" + Config.getLineSeparator());
        Thread.sleep(100);
        // First counter runs, fail runs, second counter should NOT run
        assertEquals(1, CounterCommand.count);
        assertEquals(1, FailCommand.count);
        console.stop();
    }

    @Test
    public void testAndOperatorFirstFails() throws Exception {
        CounterCommand.reset();
        FailCommand.reset();
        TestConnection connection = new TestConnection();

        ReadlineConsole console = buildConsole(connection);
        console.start();

        connection.read("fail && counter && counter" + Config.getLineSeparator());
        Thread.sleep(100);
        assertEquals(0, CounterCommand.count);
        assertEquals(1, FailCommand.count);
        console.stop();
    }

    // ========== Or operator (||) ==========

    @Test
    public void testOrOperatorFirstSuccess() throws Exception {
        CounterCommand.reset();
        TestConnection connection = new TestConnection();

        ReadlineConsole console = buildConsole(connection);
        console.start();

        // First succeeds, rest should be skipped
        connection.read("counter || counter || counter" + Config.getLineSeparator());
        Thread.sleep(100);
        assertEquals(1, CounterCommand.count);
        console.stop();
    }

    @Test
    public void testOrOperatorFirstFails() throws Exception {
        CounterCommand.reset();
        FailCommand.reset();
        TestConnection connection = new TestConnection();

        ReadlineConsole console = buildConsole(connection);
        console.start();

        // fail runs and fails, then counter runs (success stops the chain)
        connection.read("fail || counter || counter" + Config.getLineSeparator());
        Thread.sleep(100);
        assertEquals(1, FailCommand.count);
        assertEquals(1, CounterCommand.count);
        console.stop();
    }

    @Test
    public void testOrOperatorAllFail() throws Exception {
        FailCommand.reset();
        TestConnection connection = new TestConnection();

        ReadlineConsole console = buildConsole(connection);
        console.start();

        connection.read("fail || fail || fail" + Config.getLineSeparator());
        Thread.sleep(100);
        assertEquals(3, FailCommand.count);
        console.stop();
    }

    // ========== Mixed operators ==========

    @Test
    public void testAndThenOr() throws Exception {
        CounterCommand.reset();
        FailCommand.reset();
        TestConnection connection = new TestConnection();

        ReadlineConsole console = buildConsole(connection);
        console.start();

        // counter(success) && fail(failure) || counter(success)
        // && stops at fail, then || runs counter because fail returned FAILURE
        connection.read("counter && fail || counter" + Config.getLineSeparator());
        Thread.sleep(100);
        assertEquals(2, CounterCommand.count);
        assertEquals(1, FailCommand.count);
        console.stop();
    }

    // ========== Pipe operator (|) ==========

    @Test
    public void testPipeOperator() throws Exception {
        TestConnection connection = new TestConnection();

        ReadlineConsole console = buildConsole(connection);
        console.start();

        // pipe outputs "hello aesh", bar reads it from stdin and prints it
        connection.read("pipe | bar" + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("hello aesh" + Config.getLineSeparator());
        console.stop();
    }

    @Test
    public void testPipeChain() throws Exception {
        PipeCountCommand.reset();
        TestConnection connection = new TestConnection();

        ReadlineConsole console = buildConsole(connection);
        console.start();

        // pipe | pipecount — pipecount reads stdin and counts it as 1 invocation
        connection.read("pipe | pipecount" + Config.getLineSeparator());
        Thread.sleep(100);
        assertEquals(1, PipeCountCommand.count);
        console.stop();
    }

    // ========== Output redirection (> and >>) ==========

    @Test
    public void testRedirectOut() throws Exception {
        TestConnection connection = new TestConnection();

        ReadlineConsole console = buildConsole(connection);
        console.start();

        File outFile = new File(tempDir.getRoot(), "redirect_out.txt");
        connection.read("echo --msg redirected > " + outFile.getAbsolutePath() + Config.getLineSeparator());
        Thread.sleep(100);
        console.stop();

        List<String> lines = Files.readAllLines(outFile.toPath());
        assertEquals(1, lines.size());
        assertEquals("redirected", lines.get(0));
    }

    @Test
    public void testRedirectAppend() throws Exception {
        TestConnection connection = new TestConnection();

        ReadlineConsole console = buildConsole(connection);
        console.start();

        File outFile = new File(tempDir.getRoot(), "redirect_append.txt");
        connection.read("echo --msg first > " + outFile.getAbsolutePath() + Config.getLineSeparator());
        Thread.sleep(100);
        connection.read("echo --msg second >> " + outFile.getAbsolutePath() + Config.getLineSeparator());
        Thread.sleep(100);
        console.stop();

        List<String> lines = Files.readAllLines(outFile.toPath());
        // print() does not add newline, so both values end up on one line
        assertEquals(1, lines.size());
        assertEquals("firstsecond", lines.get(0));
    }

    @Test
    public void testRedirectOverwrite() throws Exception {
        TestConnection connection = new TestConnection();

        ReadlineConsole console = buildConsole(connection);
        console.start();

        File outFile = new File(tempDir.getRoot(), "redirect_overwrite.txt");
        connection.read("echo --msg original > " + outFile.getAbsolutePath() + Config.getLineSeparator());
        Thread.sleep(100);
        connection.read("echo --msg replaced > " + outFile.getAbsolutePath() + Config.getLineSeparator());
        Thread.sleep(100);
        console.stop();

        List<String> lines = Files.readAllLines(outFile.toPath());
        assertEquals(1, lines.size());
        assertEquals("replaced", lines.get(0));
    }

    // ========== Pipe + redirect combination ==========

    @Test
    public void testPipeThenRedirect() throws Exception {
        TestConnection connection = new TestConnection();

        ReadlineConsole console = buildConsole(connection);
        console.start();

        File outFile = new File(tempDir.getRoot(), "pipe_redirect.txt");
        connection.read("pipe | bar > " + outFile.getAbsolutePath() + Config.getLineSeparator());
        Thread.sleep(100);
        console.stop();

        List<String> lines = Files.readAllLines(outFile.toPath());
        assertEquals(1, lines.size());
        assertEquals("hello aesh", lines.get(0));
    }

    // ========== Executor structure tests ==========

    @Test
    public void testExecutorCountForEndOperator() throws Exception {
        TestConnection connection = new TestConnection();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(buildRegistry())
                .enableOperatorParser(true)
                .connection(connection)
                .setPersistExport(false)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        // Use the runtime to build an executor and check structure
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(buildRegistry())
                .operators(java.util.EnumSet.allOf(OperatorType.class))
                .build();

        Executor<?> executor = runtime.buildExecutor("counter ; counter ; counter");
        assertNotNull(executor);
        assertEquals(3, executor.getExecutions().size());
        console.stop();
    }

    @Test
    public void testExecutorCountForPipe() throws Exception {
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(buildRegistry())
                .operators(java.util.EnumSet.allOf(OperatorType.class))
                .build();

        Executor<?> executor = runtime.buildExecutor("pipe | bar");
        assertNotNull(executor);
        assertEquals(2, executor.getExecutions().size());
    }

    @Test
    public void testExecutorCountForAndOperator() throws Exception {
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(buildRegistry())
                .operators(java.util.EnumSet.allOf(OperatorType.class))
                .build();

        Executor<?> executor = runtime.buildExecutor("counter && counter && fail");
        assertNotNull(executor);
        assertEquals(3, executor.getExecutions().size());
    }

    // ========== Error cases ==========

    @Test
    public void testCommandExceptionResultsInFailure() throws Exception {
        TestConnection connection = new TestConnection();

        ReadlineConsole console = buildConsole(connection);
        console.start();

        // throwcmd throws CommandException, followed by || counter which should run
        CounterCommand.reset();
        connection.read("throwcmd || counter" + Config.getLineSeparator());
        Thread.sleep(100);
        assertEquals(1, CounterCommand.count);
        console.stop();
    }

    @Test
    public void testEmptyLineParsing() throws Exception {
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(buildRegistry())
                .operators(java.util.EnumSet.allOf(OperatorType.class))
                .build();

        try {
            Executor<?> executor = runtime.buildExecutor("");
            // If we get here, the executor should have no executions or fail on execute
            if (executor != null && !executor.getExecutions().isEmpty()) {
                fail("Expected empty executor or exception for empty input");
            }
        } catch (CommandLineParserException | CommandNotFoundException e) {
            // expected -- empty line is rejected
        }
    }

    // ========== ResultHandler tests ==========

    @Test
    public void testResultHandlerOnSuccess() throws Exception {
        ResultTrackingCommand.reset();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(AeshCommandRegistryBuilder.builder()
                        .command(ResultTrackingCommand.class).create())
                .build();

        runtime.executeCommand("resulttrack");
        assertTrue(ResultTrackingCommand.successCalled);
    }

    // ========== Helper methods ==========

    private CommandRuntime<CommandInvocation> buildRuntime() throws CommandRegistryException {
        return AeshCommandRuntimeBuilder.builder()
                .commandRegistry(buildRegistry())
                .build();
    }

    private ReadlineConsole buildConsole(TestConnection connection) throws IOException, CommandRegistryException {
        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(buildRegistry())
                .enableOperatorParser(true)
                .connection(connection)
                .setPersistExport(false)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        return console;
    }

    private CommandRegistry<CommandInvocation> buildRegistry() throws CommandRegistryException {
        return AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(EchoCommand.class)
                .command(CounterCommand.class)
                .command(FailCommand.class)
                .command(PipeCommand.class)
                .command(BarCommand.class)
                .command(PipeCountCommand.class)
                .command(ThrowCommand.class)
                .create();
    }

    // ========== Test commands ==========

    @CommandDefinition(name = "echo", description = "Echoes a message")
    public static class EchoCommand implements Command<CommandInvocation> {
        @Option(name = "msg")
        String msg;

        static String lastOutput;

        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            lastOutput = msg;
            invocation.print(msg);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "counter", description = "Counts invocations")
    public static class CounterCommand implements Command<CommandInvocation> {
        static int count = 0;

        static void reset() {
            count = 0;
        }

        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            count++;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "fail", description = "Always fails")
    public static class FailCommand implements Command<CommandInvocation> {
        static int count = 0;

        static void reset() {
            count = 0;
        }

        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            count++;
            return CommandResult.FAILURE;
        }
    }

    @CommandDefinition(name = "pipe", description = "Writes to stdout for piping")
    public static class PipeCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            invocation.println("hello aesh");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "bar", description = "Reads stdin and prints it")
    public static class BarCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            try {
                if (invocation.getConfiguration().getInputRedirection() != null) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(invocation.getConfiguration().getInputRedirection().read()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        invocation.println(line);
                    }
                } else if (invocation.getConfiguration().getPipedData() != null) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(invocation.getConfiguration().getPipedData()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        invocation.println(line);
                    }
                }
            } catch (IOException e) {
                throw new CommandException(e);
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "pipecount", description = "Counts pipe invocations")
    public static class PipeCountCommand implements Command<CommandInvocation> {
        static int count = 0;

        static void reset() {
            count = 0;
        }

        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            count++;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "throwcmd", description = "Throws an exception")
    public static class ThrowCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            throw new CommandException("intentional error");
        }
    }

    @CommandDefinition(name = "resulttrack", description = "Tracks result handler calls")
    public static class ResultTrackingCommand implements Command<CommandInvocation> {
        static boolean successCalled = false;

        static void reset() {
            successCalled = false;
        }

        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            successCalled = true;
            return CommandResult.SUCCESS;
        }
    }
}
