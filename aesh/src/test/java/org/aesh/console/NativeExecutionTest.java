package org.aesh.console;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.command.CommandResult;
import org.aesh.terminal.Connection;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Command mapping for shell escape (#568). Pure function tests — no OS
 * process is started, so Windows branches are covered on any platform.
 */
public class NativeExecutionTest {

    @Test
    public void testWindowsBatchUsesCmd() {
        assertArrayEquals(new String[] { "cmd", "/c", "deploy.bat --prod" },
                NativeCommand.build("deploy.bat --prod", true, false));
    }

    @Test
    public void testWindowsBareCommandUsesCmd() {
        assertArrayEquals(new String[] { "cmd", "/c", "dir" },
                NativeCommand.build("dir", true, true));
    }

    @Test
    public void testWindowsPs1PrefersPwsh() {
        assertArrayEquals(
                new String[] { "pwsh", "-ExecutionPolicy", "Bypass", "-File", "deploy.ps1", "--prod" },
                NativeCommand.build("deploy.ps1 --prod", true, true));
    }

    @Test
    public void testWindowsPs1FallsBackToPowershell() {
        assertArrayEquals(
                new String[] { "powershell", "-ExecutionPolicy", "Bypass", "-File", "deploy.ps1" },
                NativeCommand.build("deploy.ps1", true, false));
    }

    @Test
    public void testWindowsPs1ExtensionCaseInsensitive() {
        assertArrayEquals(
                new String[] { "powershell", "-ExecutionPolicy", "Bypass", "-File", "DEPLOY.PS1" },
                NativeCommand.build("DEPLOY.PS1", true, false));
    }

    @Test
    public void testWindowsPs1QuotedPathWithSpaces() {
        assertArrayEquals(
                new String[] { "pwsh", "-ExecutionPolicy", "Bypass", "-File", "C:\\My Scripts\\deploy.ps1",
                        "--prod" },
                NativeCommand.build("\"C:\\My Scripts\\deploy.ps1\" --prod", true, true));
    }

    @Test
    public void testUnixUsesSh() {
        assertArrayEquals(new String[] { "sh", "-c", "ls -la" },
                NativeCommand.build("ls -la", false, false));
    }

    @Test
    public void testUnixPs1UsesPwshWhenAvailable() {
        assertArrayEquals(
                new String[] { "pwsh", "-ExecutionPolicy", "Bypass", "-File", "deploy.ps1" },
                NativeCommand.build("deploy.ps1", false, true));
    }

    @Test
    public void testUnixPs1FallsBackToSh() {
        assertArrayEquals(new String[] { "sh", "-c", "deploy.ps1" },
                NativeCommand.build("deploy.ps1", false, false));
    }

    /**
     * Records terminal writes verbatim, preserving write granularity.
     */
    static class RecordingConnection extends TestConnection {
        final List<String> writes = Collections.synchronizedList(new ArrayList<>());

        @Override
        public Connection write(String s) {
            writes.add(s);
            return this;
        }

        String text() {
            StringBuilder text = new StringBuilder();
            for (String write : writes)
                text.append(write);
            return text.toString();
        }
    }

    /**
     * Blocks terminal writes once {@code passThroughChars} have been
     * delivered, modelling a slow consumer. Gating happens before the
     * write lands, so the delivered count is exact while blocked.
     */
    static class GatedConnection extends TestConnection {
        final StringBuilder text = new StringBuilder();
        final CountDownLatch firstWrite = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger delivered = new AtomicInteger();
        final int passThroughChars;

        GatedConnection(int passThroughChars) {
            this.passThroughChars = passThroughChars;
        }

        @Override
        public Connection write(String s) {
            if (delivered.get() >= passThroughChars) {
                try {
                    if (!release.await(30, TimeUnit.SECONDS))
                        throw new IllegalStateException("test gate was never released");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return this;
                }
            }
            firstWrite.countDown();
            delivered.addAndGet(s.length());
            text.append(s);
            return this;
        }
    }

    /**
     * Serves scripted chunks regardless of the requested length, so read
     * boundaries land exactly where the test puts them.
     */
    static class ScriptedStream extends InputStream {
        private final java.util.ArrayDeque<byte[]> chunks = new java.util.ArrayDeque<>();

        ScriptedStream(byte[]... chunks) {
            for (byte[] chunk : chunks)
                this.chunks.add(chunk);
        }

        @Override
        public int read(byte[] buffer, int offset, int length) {
            byte[] chunk = chunks.poll();
            if (chunk == null)
                return -1;
            int n = Math.min(length, chunk.length);
            System.arraycopy(chunk, 0, buffer, offset, n);
            if (n < chunk.length)
                chunks.addFirst(java.util.Arrays.copyOfRange(chunk, n, chunk.length));
            return n;
        }

        @Override
        public int read() {
            byte[] one = new byte[1];
            return read(one, 0, 1) == -1 ? -1 : one[0] & 0xFF;
        }
    }

    @Test
    public void testSlowConsumerReceivesAllBytes() throws Exception {
        byte[] data = new byte[4096];
        java.util.Arrays.fill(data, (byte) 'x');
        GatedConnection connection = new GatedConnection(1024);
        NativeExecution execution = new NativeExecution(connection, "unused");

        Thread pump = new Thread(() -> execution.pumpStream(new ByteArrayInputStream(data)));
        pump.setDaemon(true);
        pump.start();

        assertTrue("first chunk should be delivered",
                connection.firstWrite.await(10, TimeUnit.SECONDS));
        // Let the pump reach the gated write; only the first chunk may be out.
        Thread.sleep(500);
        assertTrue("pump must wait for the slow consumer", pump.isAlive());
        assertEquals(1024, connection.text.length());

        connection.release.countDown();
        pump.join(10000);
        assertFalse("pump must finish after release", pump.isAlive());
        assertEquals(4096, connection.text.length());
    }

    @Test
    public void testSplitMultibyteCharacterRoundTrips() {
        RecordingConnection connection = new RecordingConnection();
        NativeExecution execution = new NativeExecution(connection, "unused");
        // 'A' + first byte of U+20AC, then the remaining two bytes + 'B'.
        InputStream split = new ScriptedStream(
                new byte[] { 0x41, (byte) 0xE2 },
                new byte[] { (byte) 0x82, (byte) 0xAC, 0x42 });

        execution.pumpStream(split);

        assertEquals("A€B", connection.text());
    }

    @Test
    public void testFailingStreamEndsPumpPromptly() throws Exception {
        RecordingConnection connection = new RecordingConnection();
        NativeExecution execution = new NativeExecution(connection, "unused");
        InputStream failing = new InputStream() {
            boolean failed;

            @Override
            public int read(byte[] buffer, int offset, int length) throws IOException {
                if (!failed) {
                    failed = true;
                    buffer[offset] = 'q';
                    return 1;
                }
                throw new IOException("boom");
            }

            @Override
            public int read() throws IOException {
                byte[] one = new byte[1];
                int n = read(one, 0, 1);
                return n == -1 ? -1 : one[0] & 0xFF;
            }
        };

        long start = System.currentTimeMillis();
        execution.pumpStream(failing);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue("pump must not hang on stream failure, took: " + elapsed, elapsed < 5000);
        assertEquals("q", connection.text());
    }

    @Test
    public void testExecuteReturnsOnlyAfterDelivery() throws Exception {
        // Needs sh; Windows dispatch is covered by the mapping tests above.
        if (System.getProperty("os.name", "").toLowerCase().contains("win"))
            return;

        GatedConnection connection = new GatedConnection(0);
        NativeExecution execution = new NativeExecution(connection, "echo slow-delivery-marker");
        AtomicReference<CommandResult> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                result.set(execution.execute());
            } catch (Throwable e) {
                error.set(e);
            }
        });
        worker.setDaemon(true);
        worker.start();

        // The child exits in milliseconds; while writes are gated the worker
        // must still be inside the delivery join.
        Thread.sleep(2000);
        assertTrue("execute must wait for delivery", worker.isAlive());

        connection.release.countDown();
        worker.join(10000);
        assertFalse("execute must finish after release", worker.isAlive());
        assertNull("execute must stay quiet, got: " + error.get(), error.get());
        assertEquals(CommandResult.SUCCESS, result.get());
        assertTrue("marker must be delivered, got: " + connection.text,
                connection.text.toString().contains("slow-delivery-marker"));
    }
}
