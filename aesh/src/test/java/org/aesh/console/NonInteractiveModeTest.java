package org.aesh.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Tests for non-interactive (piped) input mode (#609).
 */
public class NonInteractiveModeTest {

    private static final String NL = Config.getLineSeparator();

    /**
     * Multiple commands sent as piped input should all execute.
     * This is the core bug: with threaded Process execution, commands
     * after the first were lost due to the race between readline
     * re-arming and input delivery.
     */
    @Test
    public void testMultipleCommandsFromPipedInput() throws Exception {
        TestConnection connection = TestConnection.nonInteractive();
        CounterCommand.reset();
        CountDownLatch latch = new CountDownLatch(3);

        ReadlineConsole console = buildNonInteractiveConsole(connection,
                (line, result, durationMs) -> latch.countDown());
        console.start();

        // Send 3 commands at once — simulating piped input
        connection.read("counter" + NL + "counter" + NL + "counter" + NL);
        assertTrue("All 3 commands should complete", latch.await(5, TimeUnit.SECONDS));
        assertEquals(3, CounterCommand.count.get());

        console.stop();
    }

    /**
     * No prompt should be written to output in non-interactive mode.
     */
    @Test
    public void testNoPromptInNonInteractiveMode() throws Exception {
        TestConnection connection = TestConnection.nonInteractive();
        CountDownLatch latch = new CountDownLatch(1);

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(buildRegistry())
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt("[myshell]$ ");
        console.start();

        connection.read("echo --msg hello" + NL);
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));

        String output = connection.getOutputBuffer();
        assertTrue("Output should contain command result", output.contains("hello"));
        assertFalse("Output should NOT contain prompt in non-interactive mode",
                output.contains("[myshell]$"));

        console.stop();
    }

    /**
     * Commands should see isInteractive() == false.
     */
    @Test
    public void testIsInteractiveReturnsFalse() throws Exception {
        TestConnection connection = TestConnection.nonInteractive();
        InteractiveCheckCommand.reset();
        CountDownLatch latch = new CountDownLatch(1);

        ReadlineConsole console = buildNonInteractiveConsole(connection,
                (line, result, durationMs) -> latch.countDown());
        console.start();

        connection.read("interactive-check" + NL);
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        assertFalse("isInteractive() should be false for piped input",
                InteractiveCheckCommand.wasInteractive.get());

        console.stop();
    }

    /**
     * Operators (&&) should work in non-interactive mode.
     */
    @Test
    public void testOperatorsInNonInteractiveMode() throws Exception {
        TestConnection connection = TestConnection.nonInteractive();
        CounterCommand.reset();
        CountDownLatch latch = new CountDownLatch(2);

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(buildRegistry())
                .enableOperatorParser(true)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("counter && counter" + NL);
        assertTrue("Both commands should complete", latch.await(5, TimeUnit.SECONDS));
        assertEquals(2, CounterCommand.count.get());

        console.stop();
    }

    /**
     * Interactive mode (default TestConnection) should still work unchanged.
     */
    @Test
    public void testInteractiveModeUnchanged() throws Exception {
        TestConnection connection = new TestConnection(); // default: interactive
        CountDownLatch latch = new CountDownLatch(1);

        ReadlineConsole console = buildConsole(connection,
                (line, result, durationMs) -> latch.countDown());
        console.start();

        connection.read("echo --msg hello" + NL);
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Output should contain result",
                connection.getOutputBuffer().contains("hello"));

        console.stop();
    }

    // --- Helper methods ---

    private ReadlineConsole buildNonInteractiveConsole(TestConnection connection,
            org.aesh.command.CommandExecutionListener listener) throws IOException, CommandRegistryException {
        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(buildRegistry())
                .enableOperatorParser(true)
                .commandExecutionListener(listener)
                .logging(true)
                .build();
        return new ReadlineConsole(settings);
    }

    private ReadlineConsole buildConsole(TestConnection connection,
            org.aesh.command.CommandExecutionListener listener) throws IOException, CommandRegistryException {
        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(buildRegistry())
                .enableOperatorParser(true)
                .commandExecutionListener(listener)
                .logging(true)
                .build();
        return new ReadlineConsole(settings);
    }

    private CommandRegistry<CommandInvocation> buildRegistry() throws CommandRegistryException {
        return AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(CounterCommand.class)
                .command(EchoCommand.class)
                .command(InteractiveCheckCommand.class)
                .create();
    }

    // --- Test commands ---

    @CommandDefinition(name = "counter", description = "Increments a counter")
    public static class CounterCommand implements Command<CommandInvocation> {
        static final AtomicInteger count = new AtomicInteger(0);

        static void reset() {
            count.set(0);
        }

        @Override
        public CommandResult execute(CommandInvocation ci) {
            count.incrementAndGet();
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "echo", description = "Echoes a message")
    public static class EchoCommand implements Command<CommandInvocation> {
        @Option(name = "msg")
        private String msg;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            if (msg != null)
                ci.println(msg);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "interactive-check", description = "Checks isInteractive()")
    public static class InteractiveCheckCommand implements Command<CommandInvocation> {
        static final AtomicBoolean wasInteractive = new AtomicBoolean(true);

        static void reset() {
            wasInteractive.set(true);
        }

        @Override
        public CommandResult execute(CommandInvocation ci) {
            wasInteractive.set(ci.isInteractive());
            return CommandResult.SUCCESS;
        }
    }
}
