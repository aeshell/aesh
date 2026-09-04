package org.aesh.selector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
import org.aesh.console.ReadlineConsole;
import org.aesh.terminal.Key;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Tests for the interactive selector components (#610).
 */
public class SelectorTest {

    private static final String NL = Config.getLineSeparator();

    // --- Confirmation prompt tests ---

    @Test
    public void testConfirmYes() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);
        ConfirmCommand.reset();

        ReadlineConsole console = buildConsole(connection, latch);
        console.start();

        connection.read("confirmcmd" + NL);
        // Wait for the prompt to appear
        connection.waitForOutputContaining("(Y/n)", 5000);
        // Press 'y'
        connection.read(Key.y);
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Should have confirmed", ConfirmCommand.result.get());

        console.stop();
    }

    @Test
    public void testConfirmNo() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);
        ConfirmCommand.reset();

        ReadlineConsole console = buildConsole(connection, latch);
        console.start();

        connection.read("confirmcmd" + NL);
        connection.waitForOutputContaining("(Y/n)", 5000);
        connection.read(Key.n);
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        assertFalse("Should have declined", ConfirmCommand.result.get());

        console.stop();
    }

    @Test
    public void testConfirmDefault() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);
        ConfirmCommand.reset();

        ReadlineConsole console = buildConsole(connection, latch);
        console.start();

        connection.read("confirmcmd" + NL);
        connection.waitForOutputContaining("(Y/n)", 5000);
        // Press Enter — should use default (true)
        connection.read(Key.ENTER);
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Default should be true", ConfirmCommand.result.get());

        console.stop();
    }

    // --- Single select tests ---

    @Test
    public void testSingleSelect() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);
        SelectCommand.reset();

        ReadlineConsole console = buildConsole(connection, latch);
        console.start();

        connection.read("selectcmd" + NL);
        // Wait for choices to render
        connection.waitForOutputContaining("staging", 5000);
        // Arrow down to "staging" (second item)
        connection.read(Key.DOWN);
        Thread.sleep(50);
        // Select it
        connection.read(Key.ENTER);
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        assertEquals("staging", SelectCommand.result.get());

        console.stop();
    }

    // --- Programmatic confirm via CommandInvocation ---

    @Test
    public void testConfirmViaCommandInvocation() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);
        ConfirmCommand.reset();

        ReadlineConsole console = buildConsole(connection, latch);
        console.start();

        connection.read("confirmcmd" + NL);
        connection.waitForOutputContaining("(Y/n)", 5000);
        connection.read(Key.y);
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Should confirm via ci.confirm()", ConfirmCommand.result.get());

        console.stop();
    }

    // --- Text input with validation ---

    @Test
    public void testPromptWithValidation() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);
        ValidateCommand.reset();

        ReadlineConsole console = buildConsole(connection, latch);
        console.start();

        connection.read("validatecmd" + NL);
        // Wait for prompt
        connection.waitForOutputContaining("Project name:", 5000);
        // Type invalid input
        connection.read("INVALID!" + NL);
        // Should show error and re-prompt
        connection.waitForOutputContaining("lowercase", 5000);
        // Type valid input
        connection.read("my-project" + NL);
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        assertEquals("my-project", ValidateCommand.result.get());

        console.stop();
    }

    // --- SelectLine rendering ---

    @Test
    public void testSelectLineRendering() {
        SelectLine line = new SelectLine("Option A", 80);

        // Default: unfocused, unselected
        String plain = line.printSingleSelect();
        assertNotNull(plain);

        // Focus it
        line.setFocus(true);
        String focused = line.printSingleSelect();
        assertNotNull(focused);
        assertFalse("Focused should differ from unfocused", focused.equals(plain));

        // Multi-select rendering
        String multiPlain = line.print();
        assertNotNull(multiPlain);

        line.select();
        String multiSelected = line.print();
        assertTrue("Should be selected", line.isSelected());
        assertFalse("Selected should differ from unselected", multiSelected.equals(multiPlain));
    }

    // --- Helper methods ---

    private ReadlineConsole buildConsole(TestConnection connection,
            CountDownLatch latch) throws IOException, CommandRegistryException {
        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(ConfirmCommand.class)
                .command(SelectCommand.class)
                .command(ValidateCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .logging(true)
                .build();

        return new ReadlineConsole(settings);
    }

    // --- Test commands ---

    @CommandDefinition(name = "confirmcmd", description = "Tests confirm()")
    public static class ConfirmCommand implements Command<CommandInvocation> {
        static final AtomicReference<Boolean> result = new AtomicReference<>();

        static void reset() {
            result.set(null);
        }

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException, InterruptedException {
            boolean confirmed = ci.confirm("Proceed?", true);
            result.set(confirmed);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "selectcmd", description = "Tests select()")
    public static class SelectCommand implements Command<CommandInvocation> {
        static final AtomicReference<String> result = new AtomicReference<>();

        static void reset() {
            result.set(null);
        }

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException, InterruptedException {
            String selected = ci.select("Choose environment:",
                    java.util.Arrays.asList("development", "staging", "production"));
            result.set(selected);
            ci.println("Selected: " + selected);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "validatecmd", description = "Tests prompt with validation")
    public static class ValidateCommand implements Command<CommandInvocation> {
        static final AtomicReference<String> result = new AtomicReference<>();

        static void reset() {
            result.set(null);
        }

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException, InterruptedException {
            String name = ci.prompt("Project name:",
                    input -> input.matches("[a-z][a-z0-9-]*") ? null : "Must be lowercase alphanumeric with hyphens");
            result.set(name);
            ci.println("Project: " + name);
            return CommandResult.SUCCESS;
        }
    }
}
