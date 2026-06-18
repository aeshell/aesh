package org.aesh.console;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
import org.aesh.command.shell.Shell;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.Key;
import org.aesh.terminal.tty.Size;
import org.aesh.terminal.tty.SplitScreen;
import org.aesh.terminal.tty.StatusLine;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Tests for Shell.printAbove() and Shell.registerStatusLine() (#530).
 */
public class ShellFeaturesTest {

    @Test
    public void testPrintAboveFromCommand() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(PrintAboveCommand.class)
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
        console.setPrompt(new Prompt(""));
        console.start();

        connection.read("printabove" + Config.getLineSeparator());
        Thread.sleep(200);
        String output = connection.getOutputBuffer();
        assertTrue("printAbove text should appear in output",
                output.contains("notification from above"));

        console.stop();
    }

    @Test
    public void testPrintAboveNoOpWhenNoConnection() {
        // DefaultShell (used by AeshRuntimeRunner) returns null from connection()
        // printAbove should be a no-op — no exception thrown
        Shell shell = new NoConnectionShell();
        shell.printAbove("should not throw");
        // If we get here, no exception was thrown — test passes
    }

    @Test
    public void testRegisterStatusLineReturnsNullOnTestConnection() {
        // TestConnection implements Connection directly (not AbstractConnection),
        // so registerStatusLine throws UnsupportedOperationException.
        // Shell.registerStatusLine should catch it and return null.
        TestConnection connection = new TestConnection();
        Shell shell = new ShellImpl(connection);
        StatusLine status = shell.registerStatusLine(100);
        assertNull("registerStatusLine should return null on unsupported connection", status);
    }

    @Test
    public void testRegisterStatusLineNoOpWhenNoConnection() {
        Shell shell = new NoConnectionShell();
        StatusLine status = shell.registerStatusLine(100);
        assertNull("registerStatusLine should return null when connection is null", status);
    }

    @Test
    public void testPrintAboveDirectOnConnection() {
        // Verify the Connection.printAbove default works on TestConnection
        // (falls back to write when no handler is set)
        TestConnection connection = new TestConnection();
        connection.openBlocking();
        connection.printAbove("direct message");
        String output = connection.getOutputBuffer();
        assertTrue("printAbove via Connection should write to output",
                output.contains("direct message"));
    }

    // ---- Split-screen tests (#516) ----

    @Test
    public void testEnableSplitScreenReturnsNullWhenNoConnection() {
        Shell shell = new NoConnectionShell();
        SplitScreen split = shell.enableSplitScreen(0.7);
        assertNull("enableSplitScreen should return null when connection is null", split);
    }

    @Test
    public void testSplitScreenReturnsNullWhenNotActive() {
        Shell shell = new NoConnectionShell();
        SplitScreen split = shell.splitScreen();
        assertNull("splitScreen should return null when not active", split);
    }

    @Test
    public void testEnableSplitScreenOnShellImpl() {
        TestConnection connection = new TestConnection();
        Shell shell = new ShellImpl(connection);
        // TestConnection implements Connection directly — splitScreen(double)
        // throws UnsupportedOperationException. ShellImpl delegates to connection().
        // Verify it propagates (unlike registerStatusLine which catches it).
        try {
            shell.enableSplitScreen(0.7);
            // If we get here, the connection supports split-screen (unexpected for TestConnection)
        } catch (UnsupportedOperationException e) {
            // Expected — TestConnection doesn't support split-screen
            assertTrue(e.getMessage().contains("Split screen"));
        }
    }

    @Test
    public void testSetCurrentRegionNoOpWhenNoConnection() {
        Shell shell = new NoConnectionShell();
        // Should not throw
        shell.setCurrentRegion(null);
    }

    // ---- setConverter test (#518) ----

    @Test
    public void testSetConverterOverridesDefault() throws Exception {
        org.aesh.command.impl.internal.ProcessedOption option = org.aesh.command.impl.internal.ProcessedOptionBuilder.builder()
                .name("count")
                .type(Integer.class)
                .fieldName("count")
                .description("a count")
                .optionType(org.aesh.command.impl.internal.OptionType.NORMAL)
                .build();

        // Default converter should be IntegerConverter
        assertNotNull("Option should have a default converter", option.converter());

        // Override with a custom converter that always returns 42
        option.setConverter(invocation -> 42);

        Object result = option.converter().convert(null);
        assertTrue("Custom converter should return 42", Integer.valueOf(42).equals(result));
    }

    // ---- Test commands ----

    @CommandDefinition(name = "printabove", description = "tests printAbove")
    public static class PrintAboveCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.getShell().printAbove("notification from above");
            return CommandResult.SUCCESS;
        }
    }

    /**
     * A minimal Shell implementation with no connection (simulates DefaultShell
     * used by AeshRuntimeRunner).
     */
    private static class NoConnectionShell implements Shell {
        @Override
        public void write(String msg, boolean paging) {
        }

        @Override
        public void writeln(String msg, boolean paging) {
        }

        @Override
        public void write(int[] out) {
        }

        @Override
        public void write(char out) {
        }

        @Override
        public String readLine() {
            return null;
        }

        @Override
        public String readLine(Prompt prompt) {
            return null;
        }

        @Override
        public Key read() {
            return null;
        }

        @Override
        public Key read(long timeout, TimeUnit unit) {
            return null;
        }

        @Override
        public Key read(Prompt prompt) {
            return null;
        }

        @Override
        public boolean enableAlternateBuffer() {
            return false;
        }

        @Override
        public boolean enableMainBuffer() {
            return false;
        }

        @Override
        public Size size() {
            return new Size(80, 24);
        }

        @Override
        public void clear() {
        }

        // connection() returns null (default) — simulates non-interactive mode
    }
}
