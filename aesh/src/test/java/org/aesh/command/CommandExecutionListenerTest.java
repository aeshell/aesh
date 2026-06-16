package org.aesh.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.AeshConsoleRunner;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

public class CommandExecutionListenerTest {

    @Test
    public void testCallbackFiresOnSuccess() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<CommandResult> capturedResult = new AtomicReference<>();
        AtomicReference<String> capturedLine = new AtomicReference<>();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandExecutionListener((line, result, durationMs) -> {
                    capturedLine.set(line);
                    capturedResult.set(result);
                    latch.countDown();
                })
                .build();

        AeshConsoleRunner.builder()
                .settings(settings)
                .command(SuccessCommand.class)
                .addExitCommand()
                .start();

        connection.read("succeed" + Config.getLineSeparator());
        assertTrue("Callback should fire within 5 seconds", latch.await(5, TimeUnit.SECONDS));

        assertEquals(CommandResult.SUCCESS, capturedResult.get());
        assertNotNull(capturedLine.get());
        assertTrue("Command line should contain 'succeed'", capturedLine.get().contains("succeed"));

        connection.read("exit" + Config.getLineSeparator());
    }

    @Test
    public void testCallbackFiresOnFailure() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<CommandResult> capturedResult = new AtomicReference<>();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandExecutionListener((line, result, durationMs) -> {
                    capturedResult.set(result);
                    latch.countDown();
                })
                .build();

        AeshConsoleRunner.builder()
                .settings(settings)
                .command(FailCommand.class)
                .addExitCommand()
                .start();

        connection.read("fail" + Config.getLineSeparator());
        assertTrue("Callback should fire within 5 seconds", latch.await(5, TimeUnit.SECONDS));

        assertEquals(CommandResult.FAILURE, capturedResult.get());

        connection.read("exit" + Config.getLineSeparator());
    }

    @Test
    public void testCallbackWithCommandOptions() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedLine = new AtomicReference<>();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandExecutionListener((line, result, durationMs) -> {
                    capturedLine.set(line);
                    latch.countDown();
                })
                .build();

        AeshConsoleRunner.builder()
                .settings(settings)
                .command(GreetCommand.class)
                .addExitCommand()
                .start();

        connection.read("greet --name Alice" + Config.getLineSeparator());
        assertTrue("Callback should fire within 5 seconds", latch.await(5, TimeUnit.SECONDS));

        assertTrue("Command line should contain full input",
                capturedLine.get().contains("greet --name Alice"));

        connection.read("exit" + Config.getLineSeparator());
    }

    @Test
    public void testOnCommandCompleteConvenience() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<CommandResult> capturedResult = new AtomicReference<>();

        AeshConsoleRunner.builder()
                .command(SuccessCommand.class)
                .connection(connection)
                .onCommandComplete(result -> {
                    capturedResult.set(result);
                    latch.countDown();
                })
                .addExitCommand()
                .start();

        connection.read("succeed" + Config.getLineSeparator());
        assertTrue("Callback should fire within 5 seconds", latch.await(5, TimeUnit.SECONDS));
        assertEquals(CommandResult.SUCCESS, capturedResult.get());

        connection.read("exit" + Config.getLineSeparator());
    }

    @Test
    public void testCallbackExceptionDoesNotBreakConsole() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch secondLatch = new CountDownLatch(1);

        // First callback throws, second should still fire
        java.util.concurrent.atomic.AtomicInteger callCount = new java.util.concurrent.atomic.AtomicInteger();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandExecutionListener((line, result, durationMs) -> {
                    int count = callCount.incrementAndGet();
                    if (count == 1) {
                        throw new RuntimeException("Intentional test exception");
                    }
                    secondLatch.countDown();
                })
                .build();

        AeshConsoleRunner.builder()
                .settings(settings)
                .command(SuccessCommand.class)
                .addExitCommand()
                .start();

        // First command — callback throws
        connection.read("succeed" + Config.getLineSeparator());
        Thread.sleep(200);

        // Second command — should still work (console not broken)
        connection.read("succeed" + Config.getLineSeparator());
        assertTrue("Second callback should fire despite first throwing",
                secondLatch.await(5, TimeUnit.SECONDS));
        assertEquals(2, callCount.get());

        connection.read("exit" + Config.getLineSeparator());
    }

    @Test
    public void testDurationIsReported() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Long> capturedDuration = new AtomicReference<>();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandExecutionListener((line, result, durationMs) -> {
                    capturedDuration.set(durationMs);
                    latch.countDown();
                })
                .build();

        AeshConsoleRunner.builder()
                .settings(settings)
                .command(SuccessCommand.class)
                .addExitCommand()
                .start();

        connection.read("succeed" + Config.getLineSeparator());
        assertTrue("Callback should fire within 5 seconds", latch.await(5, TimeUnit.SECONDS));

        assertNotNull(capturedDuration.get());
        assertTrue("Duration should be non-negative", capturedDuration.get() >= 0);

        connection.read("exit" + Config.getLineSeparator());
    }

    // --- Test commands ---

    @CommandDefinition(name = "succeed", description = "Always succeeds")
    public static class SuccessCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            ci.println("success");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "fail", description = "Always fails")
    public static class FailCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.FAILURE;
        }
    }

    @CommandDefinition(name = "greet", description = "Greet someone")
    public static class GreetCommand implements Command<CommandInvocation> {
        @Option(name = "name", required = true)
        private String name;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            ci.println("Hello " + name + "!");
            return CommandResult.SUCCESS;
        }
    }
}
