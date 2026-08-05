package org.aesh.console;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.readline.prompt.Prompt;
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
        Thread.sleep(500);

        String output = connection.getOutputBuffer();
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
        Thread.sleep(200);

        String output = connection.getOutputBuffer();
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
