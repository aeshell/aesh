package org.aesh.console;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Tests for dynamic prompt supplier (#530).
 */
public class PromptSupplierTest {

    @Test
    public void testPromptSupplierUsed() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();
        AtomicInteger counter = new AtomicInteger(0);

        ReadlineConsole console = buildConsole(connection, () -> {
            counter.incrementAndGet();
            return new Prompt("dynamic> ");
        });
        console.start();

        String output = connection.waitForOutputContaining("dynamic> ", 5000);
        assertTrue("Prompt supplier output should appear", output.contains("dynamic> "));
        assertTrue("Supplier should have been called at least once", counter.get() >= 1);

        console.stop();
    }

    @Test
    public void testPromptSupplierTakesPrecedence() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(NoopCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder
                .builder()
                .commandRegistry(registry)
                .enableOperatorParser(true)
                .connection(connection)
                .setPersistExport(false)
                .logging(true)
                .promptSupplier(() -> new Prompt("supplier> "))
                .build();

        // Also set a static prompt — supplier should win
        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt("static> "));
        console.start();

        String output = connection.waitForOutputContaining("supplier> ", 5000);
        assertTrue("Prompt supplier should take precedence over static prompt",
                output.contains("supplier> "));

        console.stop();
    }

    @Test
    public void testPromptSupplierCalledPerCycle() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        ReadlineConsole console = buildConsole(connection, () -> {
            int n = counter.incrementAndGet();
            return new Prompt("prompt-" + n + "> ");
        }, latch);
        console.start();

        // Wait for the first prompt to appear
        connection.waitForOutputContaining("prompt-1> ", 5000);
        // Execute a command to trigger a second readline cycle
        connection.read("noop" + Config.getLineSeparator());
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));

        // Wait for second prompt to appear
        String output = connection.waitForOutputContaining("prompt-2> ", 5000);
        assertTrue("Supplier should have been called at least twice (once per readline cycle)",
                counter.get() >= 2);
        assertTrue("First prompt should appear", output.contains("prompt-1> "));
        assertTrue("Second prompt should appear", output.contains("prompt-2> "));

        console.stop();
    }

    @Test
    public void testNoPromptSupplierUsesStaticPrompt() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(NoopCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder
                .builder()
                .commandRegistry(registry)
                .enableOperatorParser(true)
                .connection(connection)
                .setPersistExport(false)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt("static> "));
        console.start();

        String output = connection.waitForOutputContaining("static> ", 5000);
        assertTrue("Static prompt should be used when no supplier is set",
                output.contains("static> "));

        console.stop();
    }

    // ---- Helper ----

    @SuppressWarnings("unchecked")
    private ReadlineConsole buildConsole(TestConnection connection,
            java.util.function.Supplier<Prompt> supplier)
            throws CommandRegistryException, IOException {
        return buildConsole(connection, supplier, null);
    }

    @SuppressWarnings("unchecked")
    private ReadlineConsole buildConsole(TestConnection connection,
            java.util.function.Supplier<Prompt> supplier, CountDownLatch latch)
            throws CommandRegistryException, IOException {
        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(NoopCommand.class)
                .create();

        SettingsBuilder<CommandInvocation> builder = SettingsBuilder
                .builder()
                .commandRegistry(registry)
                .enableOperatorParser(true)
                .connection(connection)
                .setPersistExport(false)
                .logging(true)
                .promptSupplier(supplier);

        if (latch != null) {
            builder.commandExecutionListener((line, result, durationMs) -> latch.countDown());
        }

        return new ReadlineConsole(builder.build());
    }

    // ---- Test commands ----

    @CommandDefinition(name = "noop", description = "does nothing")
    public static class NoopCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }
}
