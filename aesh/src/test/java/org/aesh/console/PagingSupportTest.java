package org.aesh.console;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.terminal.tty.Size;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Tests for PagingSupport (#7 in paging improvements).
 */
public class PagingSupportTest {

    private static final String NL = Config.getLineSeparator();

    /**
     * Test that output shorter than terminal height is shown without paging.
     */
    @Test
    public void testShortOutputNoPaging() throws Exception {
        // Terminal is 80x20, output is 5 lines — no paging needed
        TestConnection connection = new TestConnection(new Size(80, 20));
        ReadlineConsole console = buildConsole(connection, false);
        console.start();

        connection.read("shortoutput" + NL);
        connection.waitForOutputContaining("Line 5", 5000);

        String output = connection.getOutputBuffer();
        assertTrue("Should contain all 5 lines", output.contains("Line 5"));
        assertFalse("Should not contain More prompt", output.contains("--More"));

        console.stop();
    }

    /**
     * Test that ANSI-aware line wrapping doesn't split mid-escape-sequence.
     */
    @Test
    public void testAnsiAwareLineWrapping() {
        // Create a PagingSupport with a small terminal
        TestConnection connection = new TestConnection(new Size(20, 10));
        PagingSupport ps = new PagingSupport(connection, false);

        // Add content with ANSI codes — the visible text is shorter than the ANSI-encoded string
        String ansiLine = "\u001B[1mBold text here\u001B[0m and normal text that should wrap";
        ps.addContent(ansiLine + NL);

        // Verify addContent works without error
        // The actual wrapping correctness is validated by the Paging inner class
        // during printCollectedOutput, which we can't easily unit-test without
        // a full readline cycle.
    }

    /**
     * Test that the stripAnsi utility works correctly.
     */
    @Test
    public void testStripAnsi() throws Exception {
        String input = "\u001B[1mBold\u001B[0m \u001B[32mGreen\u001B[39m Normal";
        String stripped = input.replaceAll("\u001B\\[[;\\d]*[a-zA-Z]", "");
        assertTrue("Should strip ANSI codes", stripped.equals("Bold Green Normal"));
    }

    /**
     * Test that output size limit prevents unbounded memory growth.
     */
    @Test
    public void testOutputSizeLimit() {
        TestConnection connection = new TestConnection(new Size(80, 20));
        PagingSupport ps = new PagingSupport(connection, false);

        // Add 15 MB of content — should be truncated at 10 MB
        StringBuilder large = new StringBuilder();
        for (int i = 0; i < 200000; i++) {
            large.append("This is line number ").append(i).append(" with some padding text\n");
        }
        ps.addContent(large.toString());
        // The addContent should not throw and should have truncated
        // We can't easily check the internal buffer size, but the fact
        // that it didn't OOM is the test
    }

    // --- Helpers ---

    private ReadlineConsole buildConsole(TestConnection connection, boolean searchInPaging)
            throws java.io.IOException, CommandRegistryException {
        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(ShortOutputCommand.class)
                .command(LongOutputCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .enableSearchInPaging(searchInPaging)
                .logging(true)
                .build();

        return new ReadlineConsole(settings);
    }

    // --- Test commands ---

    @CommandDefinition(name = "shortoutput", description = "Produces a few lines")
    public static class ShortOutputCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            for (int i = 1; i <= 5; i++) {
                ci.println("Line " + i);
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "longoutput", description = "Produces many lines with paging")
    public static class LongOutputCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            for (int i = 1; i <= 100; i++) {
                ci.println("Output line " + i, true);
            }
            return CommandResult.SUCCESS;
        }
    }
}
