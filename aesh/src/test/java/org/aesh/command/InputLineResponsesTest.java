package org.aesh.command;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.ReadlineConsole;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Tests for pre-canned inputLine() responses (#607).
 */
public class InputLineResponsesTest {

    private static final String NL = Config.getLineSeparator();

    /**
     * Basic: command calls inputLine(), response comes from queue.
     */
    @Test
    public void testInputLineFromQueue() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(buildRegistry())
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .inputLineResponses("Alice")
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("askname" + NL);
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Output should contain greeting",
                connection.getOutputBuffer().contains("Hello, Alice!"));

        console.stop();
    }

    /**
     * Multiple sequential inputLine() calls consume from queue in order.
     */
    @Test
    public void testMultipleInputLinesFromQueue() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(buildRegistry())
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .inputLineResponses("alice", "secret123")
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("login" + NL);
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Output should confirm login",
                connection.getOutputBuffer().contains("Logged in as alice"));

        console.stop();
    }

    /**
     * Prompt text is still written to output even when using queue.
     */
    @Test
    public void testPromptStillWrittenWithQueue() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(buildRegistry())
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .inputLineResponses("Bob")
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("askname" + NL);
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Prompt text should appear in output",
                connection.getOutputBuffer().contains("Name: "));

        console.stop();
    }

    /**
     * Queue can be refilled between commands.
     */
    @Test
    public void testRefillQueueBetweenCommands() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        AtomicReference<CountDownLatch> currentLatch = new AtomicReference<>(latch1);

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(buildRegistry())
                .commandExecutionListener((line, result, durationMs) -> currentLatch.get().countDown())
                .inputLineResponses("Alice")
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        // First command: uses "Alice" from queue
        connection.read("askname" + NL);
        assertTrue("First command should complete", latch1.await(5, TimeUnit.SECONDS));
        assertTrue(connection.getOutputBuffer().contains("Hello, Alice!"));

        // Refill queue for second command
        currentLatch.set(latch2);
        settings.setInputLineResponses(
                new ConcurrentLinkedQueue<>(java.util.Arrays.asList("Bob")));

        connection.read("askname" + NL);
        assertTrue("Second command should complete", latch2.await(5, TimeUnit.SECONDS));
        assertTrue(connection.getOutputBuffer().contains("Hello, Bob!"));

        console.stop();
    }

    /**
     * In non-interactive mode, empty queue throws instead of deadlocking.
     */
    @Test
    public void testEmptyQueueInNonInteractiveModeThrows() throws Exception {
        TestConnection connection = TestConnection.nonInteractive();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> capturedError = new AtomicReference<>();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(buildRegistry())
                .commandExecutionListener(new CommandExecutionListener() {
                    @Override
                    public void onCommandComplete(String commandLine, CommandResult result, long durationMs) {
                    }

                    @Override
                    public void onCommandComplete(String commandLine, CommandResult result,
                            long durationMs, Throwable error) {
                        capturedError.set(error);
                        latch.countDown();
                    }
                })
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        // No inputLineResponses set — should throw in non-interactive mode
        connection.read("askname" + NL);
        assertTrue("Command should complete (with error)", latch.await(5, TimeUnit.SECONDS));

        Throwable error = capturedError.get();
        assertTrue("Error should not be null, got: " + error, error != null);
        // The IllegalStateException may be wrapped in a RuntimeException by
        // Executions.execute() or received directly from Process.run()
        Throwable cause = error;
        while (cause != null && !(cause instanceof IllegalStateException))
            cause = cause.getCause();
        assertTrue("Error chain should contain IllegalStateException, got: "
                + error.getClass().getName() + ": " + error.getMessage(),
                cause instanceof IllegalStateException);
        assertTrue("Error message should mention inputLineResponses",
                cause.getMessage().contains("inputLineResponses"));

        console.stop();
    }

    /**
     * In non-interactive mode, pre-canned responses work correctly
     * (avoids the deadlock that would occur with the blocking readLine path).
     */
    @Test
    public void testInputLineInNonInteractiveModeWithQueue() throws Exception {
        TestConnection connection = TestConnection.nonInteractive();
        CountDownLatch latch = new CountDownLatch(1);

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(buildRegistry())
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .inputLineResponses("Alice")
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("askname" + NL);
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Output should contain greeting",
                connection.getOutputBuffer().contains("Hello, Alice!"));

        console.stop();
    }

    // --- Helper ---

    private CommandRegistry<CommandInvocation> buildRegistry() throws CommandRegistryException {
        return AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(AskNameCommand.class)
                .command(LoginCommand.class)
                .create();
    }

    // --- Test commands ---

    @CommandDefinition(name = "askname", description = "Asks for a name")
    public static class AskNameCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException, InterruptedException {
            String name = ci.inputLine(new Prompt("Name: "));
            ci.println("Hello, " + name + "!");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "login", description = "Login prompt")
    public static class LoginCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException, InterruptedException {
            String user = ci.inputLine(new Prompt("Username: "));
            ci.inputLine(new Prompt("Password: "));
            ci.println("Logged in as " + user);
            return CommandResult.SUCCESS;
        }
    }
}
