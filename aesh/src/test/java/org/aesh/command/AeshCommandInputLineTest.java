package org.aesh.command;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
 * Tests for commandInvocation.inputLine() — commands that prompt the user
 * for interactive input during execution.
 */
public class AeshCommandInputLineTest {

    private static final String LINE_SEPARATOR = Config.getLineSeparator();

    /**
     * Basic test: command prompts for a name, user types a response,
     * command uses it.
     */
    @Test
    public void testInputLineWithPrompt() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(AskNameCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        // Send the command
        connection.read("askname" + LINE_SEPARATOR);

        // Wait for the prompt from inputLine(new Prompt("Name: "))
        connection.waitForOutputContaining("Name: ", 5000);

        // Send the user's response
        connection.read("Alice" + LINE_SEPARATOR);

        // Wait for command to complete
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Output should contain greeting",
                connection.getOutputBuffer().contains("Hello, Alice!"));

        console.stop();
    }

    /**
     * Command prints output before prompting, verifying that output
     * is flushed before inputLine() blocks.
     */
    @Test
    public void testOutputBeforeInputLine() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(SetupWizardCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("setup" + LINE_SEPARATOR);

        // Verify output is flushed before the prompt
        connection.waitForOutputContaining("Welcome to setup", 5000);
        connection.waitForOutputContaining("Enter name: ", 5000);

        connection.read("Bob" + LINE_SEPARATOR);

        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Output should contain configured name",
                connection.getOutputBuffer().contains("Configured: Bob"));

        console.stop();
    }

    /**
     * Command calls inputLine() twice in sequence, verifying readline
     * re-arms correctly between calls.
     */
    @Test
    public void testMultipleSequentialInputLines() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(LoginCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("login" + LINE_SEPARATOR);

        // First prompt
        connection.waitForOutputContaining("Username: ", 5000);
        connection.read("alice" + LINE_SEPARATOR);

        // Second prompt
        connection.waitForOutputContaining("Password: ", 5000);
        connection.read("secret" + LINE_SEPARATOR);

        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Output should confirm login",
                connection.getOutputBuffer().contains("Logged in as alice"));

        console.stop();
    }

    /**
     * After a command completes that used inputLine(), verify the next
     * command works normally (readline re-arms properly).
     */
    @Test
    public void testCommandAfterInputLine() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(2);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(AskNameCommand.class)
                .command(EchoCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        // First: interactive command
        connection.read("askname" + LINE_SEPARATOR);
        connection.waitForOutputContaining("Name: ", 5000);
        connection.read("Alice" + LINE_SEPARATOR);

        // Second: normal command (should work after interactive input)
        connection.waitForOutputContaining("Hello, Alice!", 5000);
        connection.read("echo" + LINE_SEPARATOR);

        assertTrue("Both commands should complete", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Echo output should appear",
                connection.getOutputBuffer().contains("echo-ran"));

        console.stop();
    }

    // --- Test commands ---

    @CommandDefinition(name = "askname", description = "Asks for a name")
    public static class AskNameCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) throws InterruptedException {
            String name = ci.inputLine(new Prompt("Name: "));
            ci.println("Hello, " + name + "!");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "setup", description = "Setup wizard")
    public static class SetupWizardCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) throws InterruptedException {
            ci.println("Welcome to setup");
            String name = ci.inputLine(new Prompt("Enter name: "));
            ci.println("Configured: " + name);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "login", description = "Login prompt")
    public static class LoginCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) throws InterruptedException {
            String user = ci.inputLine(new Prompt("Username: "));
            ci.inputLine(new Prompt("Password: "));
            ci.println("Logged in as " + user);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "echo", description = "Prints echo-ran")
    public static class EchoCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            ci.println("echo-ran");
            return CommandResult.SUCCESS;
        }
    }
}
