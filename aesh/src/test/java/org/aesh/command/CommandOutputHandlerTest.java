package org.aesh.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.aesh.AeshConsoleRunner;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

public class CommandOutputHandlerTest {

    @Test
    public void testOutputHandlerCapturesCommandOutput() throws Exception {
        TestConnection connection = new TestConnection();
        StringBuilder captured = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandOutputHandler(captured::append)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .build();

        AeshConsoleRunner.builder()
                .settings(settings)
                .command(OutputCommand.class)
                .addExitCommand()
                .start();

        connection.read("output --msg hello" + Config.getLineSeparator());
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));

        String output = captured.toString();
        assertTrue("Handler should capture command output",
                output.contains("hello"));

        connection.read("exit" + Config.getLineSeparator());
    }

    @Test
    public void testOutputHandlerDoesNotCapturePrompt() throws Exception {
        TestConnection connection = new TestConnection();
        StringBuilder captured = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandOutputHandler(captured::append)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .build();

        AeshConsoleRunner.builder()
                .settings(settings)
                .command(OutputCommand.class)
                .addExitCommand()
                .prompt("[test]$ ")
                .start();

        connection.read("output --msg hello" + Config.getLineSeparator());
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));

        String output = captured.toString();
        assertFalse("Handler should NOT capture prompt text",
                output.contains("[test]$"));

        connection.read("exit" + Config.getLineSeparator());
    }

    @Test
    public void testOutputHandlerTeesToTerminal() throws Exception {
        TestConnection connection = new TestConnection();
        StringBuilder captured = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandOutputHandler(captured::append)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .build();

        AeshConsoleRunner.builder()
                .settings(settings)
                .command(OutputCommand.class)
                .addExitCommand()
                .start();

        connection.read("output --msg hello" + Config.getLineSeparator());
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));

        // Output should appear in both the handler AND the terminal
        assertTrue("Handler should have output", captured.toString().contains("hello"));
        assertTrue("Terminal should also have output",
                connection.getOutputBuffer().contains("hello"));

        connection.read("exit" + Config.getLineSeparator());
    }

    @Test
    public void testNoHandlerNoOverhead() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);

        // No commandOutputHandler set
        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .build();

        AeshConsoleRunner.builder()
                .settings(settings)
                .command(OutputCommand.class)
                .addExitCommand()
                .start();

        connection.read("output --msg hello" + Config.getLineSeparator());
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));

        // Terminal should still have the output
        assertTrue("Terminal should have output",
                connection.getOutputBuffer().contains("hello"));

        connection.read("exit" + Config.getLineSeparator());
    }

    @Test
    public void testMultipleCommandOutputsSeparated() throws Exception {
        TestConnection connection = new TestConnection();
        StringBuilder captured = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(2);

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandOutputHandler(captured::append)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .build();

        AeshConsoleRunner.builder()
                .settings(settings)
                .command(OutputCommand.class)
                .addExitCommand()
                .start();

        connection.read("output --msg first" + Config.getLineSeparator());
        connection.read("output --msg second" + Config.getLineSeparator());
        assertTrue("Both commands should complete", latch.await(5, TimeUnit.SECONDS));

        String output = captured.toString();
        assertTrue("Should capture first command output", output.contains("first"));
        assertTrue("Should capture second command output", output.contains("second"));

        connection.read("exit" + Config.getLineSeparator());
    }

    // --- Test commands ---

    @CommandDefinition(name = "output", description = "Prints a message")
    public static class OutputCommand implements Command<CommandInvocation> {
        @Option(name = "msg", required = true)
        private String msg;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            ci.println(msg);
            return CommandResult.SUCCESS;
        }
    }
}
