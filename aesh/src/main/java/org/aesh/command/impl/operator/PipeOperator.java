/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command.impl.operator;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.aesh.command.PipelineConfig;
import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.aesh.console.AeshContext;

/**
 * Pipe operator that streams data between commands using a bounded blocking queue.
 * <p>
 * Pipeline stages run concurrently. The writer blocks when the queue is full
 * (back-pressure), and the reader blocks when the queue is empty. This provides
 * Unix-like pipe semantics without the thread-identity restrictions of
 * {@link java.io.PipedInputStream}/{@link java.io.PipedOutputStream}.
 * <p>
 * Unlike file redirection, pipe output does not strip ANSI codes -- the receiving
 * command may be color-aware.
 *
 * @author Aesh team
 * @since 3.15
 */
public class PipeOperator extends EndOperator implements
        ConfigurationOperator, DataProvider {

    /** Sentinel value placed in the queue to signal EOF. */
    private static final byte[] EOF = new byte[0];

    private final BlockingQueue<byte[]> queue;
    private final AeshContext context;
    private final AtomicLong truncatedBytes = new AtomicLong();
    private volatile boolean consumerGone;
    private final int chunkSizeBytes;
    private final int queueCapacityChunks;
    private CommandInvocationConfiguration config;
    private BlockingQueue<byte[]> errorQueue;
    private PipeQueueDelegate errorDelegate;

    /**
     * Output delegate for pipe -- writes to the blocking queue without
     * stripping ANSI codes.
     */
    private class PipeQueueDelegate extends OutputDelegate {

        private final BlockingQueue<byte[]> target;

        PipeQueueDelegate(BlockingQueue<byte[]> target) {
            this.target = target;
        }

        @Override
        protected BufferedWriter buildWriter() throws IOException {
            return new BufferedWriter(new OutputStreamWriter(new QueueOutputStream(target)), chunkSizeBytes);
        }

        /**
         * Write directly without ANSI stripping. Pipes preserve all data
         * since the receiving command may be color-aware.
         */
        @Override
        public void write(String msg) {
            try {
                if (writer == null && exception == null) {
                    writer = buildWriter();
                }
                if (writer != null) {
                    writer.append(msg);
                    writer.flush(); // flush to push data through the pipe promptly
                }
            } catch (IOException e) {
                exception = e;
            }
        }

        @Override
        public void close() throws IOException {
            try {
                if (writer != null)
                    writer.close();
                else if (!target.offer(EOF)) {
                    target.clear();
                    target.offer(EOF);
                }
            } catch (IOException e) {
                if (!isRoutinePipeClose(e)) {
                    if (exception == null)
                        exception = e;
                }
            } finally {
                if (exception != null && !isRoutinePipeClose(exception)) {
                    throw exception;
                }
            }
        }
    }

    private static boolean isRoutinePipeClose(IOException error) {
        String message = error.getMessage();
        return message != null && message.contains("Pipe closed");
    }

    public long truncatedBytes() {
        return truncatedBytes.get();
    }

    private void discardForEof(BlockingQueue<byte[]> target) {
        if (!target.offer(EOF)) {
            long dropped = 0;
            for (byte[] chunk : target.toArray(new byte[0][]))
                dropped += chunk.length;
            target.clear();
            truncatedBytes.addAndGet(dropped);
            target.offer(EOF);
        }
    }

    public static boolean isPipeBroken(Throwable error) {
        while (error != null) {
            if (error instanceof PipeBrokenException)
                return true;
            error = error.getCause();
        }
        return false;
    }

    /**
     * OutputStream that writes byte chunks into the blocking queue.
     * Closing sends an EOF sentinel.
     */
    private class QueueOutputStream extends OutputStream {
        private final BlockingQueue<byte[]> target;
        private volatile boolean closed;

        QueueOutputStream(BlockingQueue<byte[]> target) {
            this.target = target;
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[] { (byte) b }, 0, 1);
        }

        private static final long OFFER_WAIT_MS = 100;

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (closed)
                throw new IOException("Pipe closed");
            byte[] chunk = new byte[len];
            System.arraycopy(b, off, chunk, 0, len);
            try {
                while (!target.offer(chunk, OFFER_WAIT_MS, TimeUnit.MILLISECONDS)) {
                    if (consumerGone)
                        throw new PipeBrokenException();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Pipe closed");
            }
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                // Use offer instead of put — if the queue is full and we're
                // interrupted, offer returns false without blocking. Clear
                // the queue first to make room for the EOF sentinel.
                discardForEof(target);
            }
        }
    }

    /**
     * InputStream that reads byte chunks from the blocking queue.
     * Returns -1 (EOF) when the EOF sentinel is received.
     */
    private class QueueInputStream extends InputStream {
        private final BlockingQueue<byte[]> source;
        private byte[] currentChunk;
        private int pos;
        private volatile boolean eof;
        private volatile boolean closed;

        QueueInputStream(BlockingQueue<byte[]> source) {
            this.source = source;
        }

        @Override
        public int read() throws IOException {
            byte[] buf = new byte[1];
            int n = read(buf, 0, 1);
            return n == -1 ? -1 : buf[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (eof || closed)
                return -1;
            while (currentChunk == null || pos >= currentChunk.length) {
                try {
                    currentChunk = source.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return -1;
                }
                if (currentChunk == EOF || closed) {
                    eof = true;
                    return -1;
                }
                pos = 0;
            }
            int available = currentChunk.length - pos;
            int toRead = Math.min(available, len);
            System.arraycopy(currentChunk, pos, b, off, toRead);
            pos += toRead;
            return toRead;
        }

        @Override
        public int available() {
            if (eof || closed)
                return 0;
            int chunkAvail = currentChunk != null ? currentChunk.length - pos : 0;
            return chunkAvail + (source.isEmpty() ? 0 : 1);
        }

        @Override
        public void close() {
            closed = true;
            consumerGone = true;
            // Drain the queue so upstream's put() unblocks
            discardForEof(source);
        }
    }

    public PipeOperator(AeshContext context) {
        this(context, PipelineConfig.DEFAULT);
    }

    public PipeOperator(AeshContext context, PipelineConfig config) {
        this.context = context;
        this.queue = new ArrayBlockingQueue<>(config.queueCapacityChunks());
        this.chunkSizeBytes = config.chunkSizeBytes();
        this.queueCapacityChunks = config.queueCapacityChunks();
    }

    @Override
    public CommandInvocationConfiguration getConfiguration() throws IOException {
        if (config == null) {
            config = new CommandInvocationConfiguration(context, new PipeQueueDelegate(queue), null);
        }
        return config;
    }

    public synchronized OutputDelegate getErrorDelegate() {
        if (errorDelegate == null) {
            errorQueue = new ArrayBlockingQueue<>(queueCapacityChunks);
            errorDelegate = new PipeQueueDelegate(errorQueue);
        }
        return errorDelegate;
    }

    public boolean hasErrorData() {
        return errorQueue != null && !errorQueue.isEmpty();
    }

    public OutputDelegate getMergingErrorDelegate() {
        return new PipeQueueDelegate(queue);
    }

    public BufferedInputStream getErrorData() {
        if (errorQueue == null)
            return null;
        return new BufferedInputStream(new QueueInputStream(errorQueue));
    }

    @Override
    public void setArgument(String value) {
        // NOOP
    }

    @Override
    public BufferedInputStream getData() {
        return new BufferedInputStream(new QueueInputStream(queue));
    }
}
