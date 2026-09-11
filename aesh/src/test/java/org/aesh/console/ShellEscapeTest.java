package org.aesh.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandExecutionListener;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.tty.Signal;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Tests for shell escape (! prefix) feature (#566).
 */
public class ShellEscapeTest {

    @Test
    public void testShellEscapeExecutesNativeCommand() throws Exception {
        // Skip on Windows — echo command syntax differs
        if (System.getProperty("os.name", "").toLowerCase().contains("win"))
            return;

        TestConnection connection = new TestConnection();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(TestCmd.class)
                .create();

        Settings settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .enableShellEscape(true)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        connection.clearOutputBuffer();
        connection.read("!echo hello-from-shell" + Config.getLineSeparator());
        String output = connection.waitForOutputContaining("hello-from-shell", 5000);
        assertTrue("Output should contain native command result, got: " + output,
                output.contains("hello-from-shell"));

        console.stop();
    }

    @Test
    public void testShellEscapeDisabledByDefault() throws Exception {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(TestCmd.class)
                .create();

        // No enableShellEscape(true) — default is disabled
        Settings settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        connection.clearOutputBuffer();
        connection.read("!echo should-not-run" + Config.getLineSeparator());
        // When disabled, "!echo" becomes a command-not-found error written to output
        String output = connection.waitForOutputContaining("not found", 5000);
        if (!output.contains("not found"))
            output = connection.waitForOutputContaining("not a", 1000);
        // When disabled, "!echo" goes to the command registry which doesn't find it.
        // The error message contains the line, but the native command should NOT have
        // actually executed — verify the output doesn't start with the command result
        // (it should contain a "not found" error instead)
        assertTrue("Should have command-not-found error when shell escape is disabled",
                output.contains("not found") || output.contains("not a"));

        console.stop();
    }

    @Test
    public void testShellEscapeEmptyCommand() throws Exception {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(TestCmd.class)
                .create();

        Settings settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .enableShellEscape(true)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        // Just "!" with no command — should not crash, just return to prompt
        connection.clearOutputBuffer();
        connection.read("!" + Config.getLineSeparator());
        Thread.sleep(200);

        // Should not have errored — console should still be running
        String output = connection.getOutputBuffer();
        assertFalse("Empty ! should not produce an error",
                output.contains("error") || output.contains("Error"));

        console.stop();
    }

    @Test
    public void testNormalCommandsStillWorkWithShellEscape() throws Exception {
        TestConnection connection = new TestConnection();
        TestCmd.executed = false;

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(TestCmd.class)
                .create();

        CountDownLatch latch = new CountDownLatch(1);

        Settings settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .enableShellEscape(true)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        // Normal command (no ! prefix) should work as usual
        connection.read("testcmd" + Config.getLineSeparator());
        assertTrue("Command should complete within 5 seconds",
                latch.await(5, TimeUnit.SECONDS));
        assertTrue("Normal command should have executed", TestCmd.executed);

        console.stop();
    }

    @Test
    public void testNativeExitCodePropagated() throws Exception {
        if (System.getProperty("os.name", "").toLowerCase().contains("win"))
            return;

        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<CommandResult> listenerResult = new AtomicReference<>();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(TestCmd.class)
                .create();

        Settings settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .enableShellEscape(true)
                .commandExecutionListener(new CommandExecutionListener() {
                    @Override
                    public void onCommandComplete(String commandLine, CommandResult result, long durationMs) {
                    }

                    @Override
                    public void onCommandComplete(String commandLine, CommandResult result, long durationMs,
                            Throwable error) {
                        listenerResult.set(result);
                        latch.countDown();
                    }
                })
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        connection.read("!exit 42" + Config.getLineSeparator());
        assertTrue("Native command should complete", latch.await(5, TimeUnit.SECONDS));
        assertEquals(42, listenerResult.get().getResultValue());

        console.stop();
    }

    @Test
    public void testNativeFailureExitCode() throws Exception {
        if (System.getProperty("os.name", "").toLowerCase().contains("win"))
            return;

        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<CommandResult> listenerResult = new AtomicReference<>();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(TestCmd.class)
                .create();

        Settings settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .enableShellEscape(true)
                .commandExecutionListener(new CommandExecutionListener() {
                    @Override
                    public void onCommandComplete(String commandLine, CommandResult result, long durationMs) {
                    }

                    @Override
                    public void onCommandComplete(String commandLine, CommandResult result, long durationMs,
                            Throwable error) {
                        listenerResult.set(result);
                        latch.countDown();
                    }
                })
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        connection.read("!exit 3" + Config.getLineSeparator());
        assertTrue("Native command should complete", latch.await(5, TimeUnit.SECONDS));
        assertTrue(listenerResult.get().isFailure());
        assertEquals(3, listenerResult.get().getResultValue());

        console.stop();
    }

    @Test
    public void testNativeInterruptMapsToInterrupted() throws Exception {
        if (System.getProperty("os.name", "").toLowerCase().contains("win"))
            return;

        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<CommandResult> listenerResult = new AtomicReference<>();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(TestCmd.class)
                .create();

        Settings settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .enableShellEscape(true)
                .commandExecutionListener(new CommandExecutionListener() {
                    @Override
                    public void onCommandComplete(String commandLine, CommandResult result, long durationMs) {
                    }

                    @Override
                    public void onCommandComplete(String commandLine, CommandResult result, long durationMs,
                            Throwable error) {
                        listenerResult.set(result);
                        latch.countDown();
                    }
                })
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        Thread interrupter = new Thread(() -> {
            long deadline = System.currentTimeMillis() + 5000;
            while (!(connection.signalHandler() instanceof CommandJob)) {
                if (System.currentTimeMillis() > deadline)
                    return;
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            connection.signalHandler().accept(Signal.INT);
        });
        interrupter.setDaemon(true);
        interrupter.start();

        connection.read("!sleep 30" + Config.getLineSeparator());

        long start = System.currentTimeMillis();
        assertTrue("Interrupted native command should complete", latch.await(40, TimeUnit.SECONDS));
        long elapsed = System.currentTimeMillis() - start;
        assertEquals(CommandResult.INTERRUPTED, listenerResult.get());
        assertTrue("Interrupt must preempt the sleep, took: " + elapsed, elapsed < 15000);

        console.stop();
    }

    @Test
    public void testNativeStdinForwardedToProcess() throws Exception {
        if (System.getProperty("os.name", "").toLowerCase().contains("win"))
            return;

        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(TestCmd.class)
                .create();

        Settings settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .enableShellEscape(true)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        connection.clearOutputBuffer();
        // read() dispatches synchronously: !cat blocks the calling thread
        // until cat exits, so run it on a background thread and feed stdin
        // from the test thread once the forwarder is installed.
        Thread runner = new Thread(() -> connection.read("!cat" + Config.getLineSeparator()));
        runner.setDaemon(true);
        runner.start();

        long deadline = System.currentTimeMillis() + 5000;
        while (!isStdinForwarderInstalled(connection)) {
            if (System.currentTimeMillis() > deadline)
                fail("Stdin forwarder was not installed");
            Thread.sleep(10);
        }
        connection.read("hello-stdin" + Config.getLineSeparator());
        connection.read("" + '\u0004');
        assertTrue("cat should exit on EOF", latch.await(10, TimeUnit.SECONDS));
        runner.join(5000);
        String output = connection.getOutputBuffer();
        assertTrue("Output should contain forwarded stdin, got: " + output,
                output.contains("hello-stdin"));

        console.stop();
    }

    private static boolean isStdinForwarderInstalled(TestConnection connection) {
        Object handler = connection.stdinHandler();
        return handler != null && handler.getClass().getSimpleName().contains("StdinForwarder");
    }

    @Test
    public void testStdinHandlerRestoredAfterNative() throws Exception {
        if (System.getProperty("os.name", "").toLowerCase().contains("win"))
            return;

        TestConnection connection = new TestConnection();
        TestCmd.executed = false;
        CountDownLatch nativeLatch = new CountDownLatch(1);
        CountDownLatch cmdLatch = new CountDownLatch(1);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(TestCmd.class)
                .create();

        Settings settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .enableShellEscape(true)
                .commandExecutionListener((line, result, durationMs) -> {
                    if (line.trim().startsWith("!"))
                        nativeLatch.countDown();
                    else
                        cmdLatch.countDown();
                })
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        connection.read("!echo hi" + Config.getLineSeparator());
        assertTrue("Native command should complete", nativeLatch.await(5, TimeUnit.SECONDS));

        connection.read("testcmd" + Config.getLineSeparator());
        assertTrue("REPL should accept input after native command",
                cmdLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Normal command should have executed", TestCmd.executed);

        console.stop();
    }

    @Test
    public void testStdinHandlerRestoredAfterInterrupt() throws Exception {
        if (System.getProperty("os.name", "").toLowerCase().contains("win"))
            return;

        TestConnection connection = new TestConnection();
        TestCmd.executed = false;
        CountDownLatch nativeLatch = new CountDownLatch(1);
        CountDownLatch cmdLatch = new CountDownLatch(1);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(TestCmd.class)
                .create();

        Settings settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .enableShellEscape(true)
                .commandExecutionListener((line, result, durationMs) -> {
                    if (line.trim().startsWith("!"))
                        nativeLatch.countDown();
                    else
                        cmdLatch.countDown();
                })
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        Thread interrupter = new Thread(() -> {
            long deadline = System.currentTimeMillis() + 5000;
            while (!(connection.signalHandler() instanceof CommandJob)) {
                if (System.currentTimeMillis() > deadline)
                    return;
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            connection.signalHandler().accept(Signal.INT);
        });
        interrupter.setDaemon(true);
        interrupter.start();

        connection.read("!sleep 30" + Config.getLineSeparator());
        assertTrue("Interrupted native command should complete", nativeLatch.await(40, TimeUnit.SECONDS));

        connection.read("testcmd" + Config.getLineSeparator());
        assertTrue("REPL should accept input after interrupt",
                cmdLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Normal command should have executed", TestCmd.executed);

        console.stop();
    }

    @CommandDefinition(name = "testcmd", description = "test command")
    public static class TestCmd implements Command<CommandInvocation> {
        static volatile boolean executed;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            executed = true;
            return CommandResult.SUCCESS;
        }
    }
}
