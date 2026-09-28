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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.AeshConsoleRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.terminal.StreamConnection;
import org.junit.Test;

/**
 * Drives the console over readline's stock {@link StreamConnection},
 * proving it under the exact back-to-back, no-readiness-gate shape from
 * #634: each command is written immediately when the previous completion
 * fires, over real piped streams.
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
     * Stock readline connection with a stdin-handler install counter, kept
     * for the re-arm assertion (every completion must be preceded by a
     * re-arm, which installs a fresh handler — #634).
     */
    static class CountingStreamConnection extends StreamConnection {
        final AtomicInteger stdinHandlerSets = new AtomicInteger();

        CountingStreamConnection(PipedInputStream input, ByteArrayOutputStream output) {
            super(StandardCharsets.UTF_8, input, output);
        }

        @Override
        public void setStdinHandler(java.util.function.Consumer<int[]> handler) {
            stdinHandlerSets.incrementAndGet();
            super.setStdinHandler(handler);
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
        // Stock readline connection; unlike the old hand-rolled harness it
        // never closes caller-owned streams, so pipeIn is closed explicitly.
        CountingStreamConnection connection = new CountingStreamConnection(pipeIn, consoleOut);
        AtomicReference<Throwable> readerDeath = new AtomicReference<>();
        connection.setReaderDeathHook(readerDeath::set);

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
                            + " readerDeath=" + readerDeath.get()
                            + threadDump());
                }
            }
            assertTrue("Reader must stay alive for the whole session, death: " + readerDeath.get(),
                    readerDeath.get() == null);
        } finally {
            try {
                testOut.close();
            } catch (Exception ignored) {
            }
            connection.close();
            try {
                pipeIn.close();
            } catch (Exception ignored) {
            }
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
