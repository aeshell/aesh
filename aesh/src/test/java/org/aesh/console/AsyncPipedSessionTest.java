/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
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
package org.aesh.console;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.AeshConsoleRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.terminal.AbstractConnection;
import org.aesh.terminal.Attributes;
import org.aesh.terminal.BaseDevice;
import org.aesh.terminal.Device;
import org.aesh.terminal.EventDecoder;
import org.aesh.terminal.tty.Capability;
import org.aesh.terminal.tty.Size;
import org.aesh.terminal.utils.Parser;
import org.junit.Test;

/**
 * Mirrors the downstream harness shape from #634: a custom
 * {@link AbstractConnection} with its own blocking reader thread feeding the
 * connection {@link EventDecoder}, reporting interactive (async execution),
 * driven over real piped streams with each command written immediately when
 * the previous completion fires (no readiness gate between commands).
 */
public class AsyncPipedSessionTest {

    @CommandDefinition(name = "hello", description = "hello")
    public static class HelloCommand implements Command<CommandInvocation> {
        static final java.util.concurrent.atomic.AtomicInteger executions = new java.util.concurrent.atomic.AtomicInteger();
        static final java.util.List<String> executedNames = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        @Option(name = "name", description = "name")
        String name;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            executions.incrementAndGet();
            executedNames.add(String.valueOf(name));
            ci.println("Hello " + name);
            return CommandResult.SUCCESS;
        }
    }

    /**
     * Minimal stream-backed connection: own blocking reader thread delivering
     * through the {@link EventDecoder} (which buffers when no stdin handler
     * is set), async execution via {@code isInteractive() == true}.
     */
    static class ReaderThreadConnection extends AbstractConnection {
        private final Device device = new BaseDevice("test");
        private final Size size = new Size(120, 40);
        private final InputStream input;
        private final OutputStream output;
        private volatile boolean closed = false;
        private Thread readerThread;
        final java.util.concurrent.atomic.AtomicInteger stdinHandlerSets = new java.util.concurrent.atomic.AtomicInteger();

        ReaderThreadConnection(InputStream input, OutputStream output) {
            this.input = input;
            this.output = output;
            this.attributes = new Attributes();
            this.eventDecoder = new EventDecoder(this.attributes);
            this.stdout = data -> {
                try {
                    output.write(Parser.fromCodePoints(data).getBytes(StandardCharsets.UTF_8));
                    output.flush();
                } catch (IOException e) {
                    // Connection closed
                }
            };
        }

        @Override
        public Device device() {
            return device;
        }

        @Override
        public Size size() {
            return size;
        }

        @Override
        public void close() {
            closed = true;
            try {
                input.close();
            } catch (IOException e) {
                // Ignore
            }
            if (closeHandler != null) {
                closeHandler.accept(null);
            }
        }

        @Override
        public void openBlocking() {
            startReader();
            try {
                if (readerThread != null) {
                    readerThread.join();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void openNonBlocking() {
            startReader();
        }

        @Override
        public void setStdinHandler(java.util.function.Consumer<int[]> handler) {
            stdinHandlerSets.incrementAndGet();
            super.setStdinHandler(handler);
        }

        @Override
        public boolean put(Capability capability, Object... params) {
            return false;
        }

        @Override
        public Charset inputEncoding() {
            return StandardCharsets.UTF_8;
        }

        @Override
        public Charset outputEncoding() {
            return StandardCharsets.UTF_8;
        }

        @Override
        public boolean supportsAnsi() {
            return false;
        }

        @Override
        public boolean isInteractive() {
            return true;
        }

        private void startReader() {
            if (readerThread != null) {
                return;
            }
            readerThread = new Thread(() -> {
                byte[] buffer = new byte[1024];
                try {
                    while (!closed) {
                        int n = input.read(buffer);
                        if (n == -1) {
                            break;
                        }
                        if (n > 0) {
                            String text = new String(buffer, 0, n, StandardCharsets.UTF_8);
                            eventDecoder.accept(Parser.toCodePoints(text));
                        }
                    }
                } catch (IOException e) {
                    // Stream closed, exit reader
                }
            }, "aesh-test-reader");
            readerThread.setDaemon(true);
            readerThread.start();
        }
    }

    @Test
    public void testBackToBackCommandsAsyncOverPipes() throws Exception {
        runSession(30);
    }

    private void runSession(int commandCount) throws Exception {
        HelloCommand.executions.set(0);
        HelloCommand.executedNames.clear();
        PipedInputStream pipeIn = new PipedInputStream(4096);
        PipedOutputStream testOut = new PipedOutputStream(pipeIn);
        ByteArrayOutputStream consoleOut = new ByteArrayOutputStream();
        ReaderThreadConnection connection = new ReaderThreadConnection(pipeIn, consoleOut);

        CountDownLatch readyLatch = new CountDownLatch(1);
        AtomicReference<CountDownLatch> completionLatch = new AtomicReference<>(new CountDownLatch(1));
        java.util.List<String> completedLines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        Thread replThread = new Thread(() -> AeshConsoleRunner.builder()
                .connection(connection)
                .command(HelloCommand.class)
                .commandExecutionListener((line, result, durationMs) -> {
                    completedLines.add(line);
                    completionLatch.get().countDown();
                })
                .onReady(readyLatch::countDown)
                .start());
        replThread.setDaemon(true);
        replThread.start();

        try {
            assertTrue("Console should become ready within 10 seconds",
                    readyLatch.await(10, TimeUnit.SECONDS));

            for (int i = 0; i < commandCount; i++) {
                CountDownLatch latch = new CountDownLatch(1);
                completionLatch.set(latch);
                String cmd = "hello --name=Name" + i + "\n";
                testOut.write(cmd.getBytes(StandardCharsets.UTF_8));
                testOut.flush();
                if (!latch.await(15, TimeUnit.SECONDS)) {
                    fail("Command " + i + " never completed. executions="
                            + HelloCommand.executions.get() + " executedNames=" + HelloCommand.executedNames
                            + " completedLines=" + completedLines
                            + " stdinHandlerSets=" + connection.stdinHandlerSets.get()
                            + threadDump());
                }
            }
        } finally {
            try {
                testOut.close();
            } catch (Exception ignored) {
            }
            connection.close();
        }
    }

    private static String threadDump() {
        StringBuilder sb = new StringBuilder("\n--- thread dump ---\n");
        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            Thread t = entry.getKey();
            String name = t.getName();
            if (name.startsWith("surefire-") || name.equals("main")) {
                continue;
            }
            sb.append('"').append(name).append("\" state=").append(t.getState()).append('\n');
            for (StackTraceElement frame : entry.getValue()) {
                sb.append("    at ").append(frame).append('\n');
            }
        }
        return sb.toString();
    }
}
