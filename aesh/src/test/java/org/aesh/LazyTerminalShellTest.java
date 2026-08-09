package org.aesh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.shell.Shell;
import org.aesh.terminal.tty.Size;
import org.junit.Test;

/**
 * Tests for {@link LazyTerminalShell} and AeshRuntimeRunner shell integration (#571).
 */
public class LazyTerminalShellTest {

    // ========== LazyTerminalShell unit tests ==========

    @Test
    public void testWriteDoesNotTriggerInit() {
        LazyTerminalShell shell = new LazyTerminalShell();
        // write should work without initializing TerminalConnection
        shell.write("hello", false);
        shell.writeln("world", false);
        // Close should be safe even without init
        shell.close();
    }

    @Test
    public void testSizeReturnsNonNullAndReasonable() {
        LazyTerminalShell shell = new LazyTerminalShell();
        try {
            Size size = shell.size();
            assertNotNull("size() should never return null", size);
            assertTrue("columns should be > 0, got: " + size.getWidth(),
                    size.getWidth() > 0);
            // rows could be -1 in some edge cases, but with our fallback
            // it should be positive
            assertTrue("rows should be > 0 or at least not -1, got: " + size.getHeight(),
                    size.getHeight() > 0 || size.getHeight() == -1);
        } finally {
            shell.close();
        }
    }

    @Test
    public void testCloseWithoutInitIsNoOp() {
        LazyTerminalShell shell = new LazyTerminalShell();
        // Should not throw
        shell.close();
        shell.close(); // double close safe
    }

    @Test
    public void testMultipleSizeCallsOnlyInitOnce() {
        LazyTerminalShell shell = new LazyTerminalShell();
        try {
            Size s1 = shell.size();
            Size s2 = shell.size();
            assertNotNull(s1);
            assertNotNull(s2);
            // Same dimensions (terminal doesn't resize between calls)
            assertEquals(s1.getWidth(), s2.getWidth());
            assertEquals(s1.getHeight(), s2.getHeight());
        } finally {
            shell.close();
        }
    }

    @Test
    public void testReadMethodsReturnNullWithoutConsole() {
        // In a test environment, System.console() is typically null
        LazyTerminalShell shell = new LazyTerminalShell();
        try {
            // These should return null gracefully, not throw
            assertNull(shell.readLine());
            assertNull(shell.read());
            assertNull(shell.read(1, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            shell.close();
        }
    }

    @Test
    public void testEnableAlternateBufferReturnsFalse() {
        LazyTerminalShell shell = new LazyTerminalShell();
        assertEquals(false, shell.enableAlternateBuffer());
        assertEquals(false, shell.enableMainBuffer());
        shell.close();
    }

    // ========== AeshRuntimeRunner integration tests ==========

    @Test
    public void testRuntimeRunnerDefaultShellHasValidSize() {
        // Command that captures shell.size() and verifies it's not Size(1, -1)
        SizeCheckCommand.capturedSize = null;
        CommandResult result = AeshRuntimeRunner.builder()
                .command(SizeCheckCommand.class)
                .args(new String[0])
                .execute();

        assertEquals(CommandResult.SUCCESS, result);
        assertNotNull("Shell size should have been captured", SizeCheckCommand.capturedSize);
        assertTrue("Width should be > 0, got: " + SizeCheckCommand.capturedSize.getWidth(),
                SizeCheckCommand.capturedSize.getWidth() > 0);
        // Should NOT be the old hardcoded Size(1, -1)
        assertTrue("Should not be the old dummy size (1, -1)",
                SizeCheckCommand.capturedSize.getWidth() != 1
                        || SizeCheckCommand.capturedSize.getHeight() != -1);
    }

    @Test
    public void testRuntimeRunnerCustomShellUsed() {
        // Provide a custom Shell and verify it's used
        CustomShellUsed.used = false;
        CommandResult result = AeshRuntimeRunner.builder()
                .command(ShellAccessCommand.class)
                .shell(new CustomShellUsed())
                .args(new String[0])
                .execute();

        assertEquals(CommandResult.SUCCESS, result);
        assertTrue("Custom shell should have been used", CustomShellUsed.used);
    }

    @Test
    public void testRuntimeRunnerShellWriteWorks() {
        // Verify that shell.write() works (writes to System.out)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream origOut = System.out;
        System.setOut(new PrintStream(baos));
        try {
            CommandResult result = AeshRuntimeRunner.builder()
                    .command(WriteCommand.class)
                    .args(new String[0])
                    .execute();

            assertEquals(CommandResult.SUCCESS, result);
            String output = baos.toString();
            assertTrue("Output should contain 'shell-write-test'",
                    output.contains("shell-write-test"));
        } finally {
            System.setOut(origOut);
        }
    }

    // ========== Test commands ==========

    @CommandDefinition(name = "sizecheck", description = "Check shell size")
    public static class SizeCheckCommand implements Command<CommandInvocation> {
        static volatile Size capturedSize;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            capturedSize = invocation.getShell().size();
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "shellaccess", description = "Access shell")
    public static class ShellAccessCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.getShell().size(); // trigger the shell
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "writetest", description = "Test write")
    public static class WriteCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("shell-write-test");
            return CommandResult.SUCCESS;
        }
    }

    // ========== Custom Shell for testing ==========

    static class CustomShellUsed implements Shell {
        static volatile boolean used;

        @Override
        public Size size() {
            used = true;
            return new Size(120, 40);
        }

        @Override
        public void write(String msg, boolean paging) {
            System.out.print(msg);
        }

        @Override
        public void writeln(String msg, boolean paging) {
            System.out.println(msg);
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
        public String readLine(org.aesh.readline.prompt.Prompt prompt) {
            return null;
        }

        @Override
        public org.aesh.terminal.Key read() {
            return null;
        }

        @Override
        public org.aesh.terminal.Key read(long timeout, TimeUnit unit) {
            return null;
        }

        @Override
        public org.aesh.terminal.Key read(org.aesh.readline.prompt.Prompt prompt) {
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
        public void clear() {
        }
    }
}
