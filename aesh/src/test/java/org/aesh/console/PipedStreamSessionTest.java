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
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.AeshConsoleRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.terminal.tty.TerminalConnection;
import org.junit.Test;

/**
 * Drives an interactive console over real piped streams, sending each command
 * immediately when the previous completion fires (no readiness gate between
 * commands). This exercises the readline re-arm + reader-thread buffering
 * window that in-process {@code TestConnection} tests never hit.
 *
 * <p>
 * Regression test for #634: the second command intermittently never executed
 * (no {@code onCommandComplete}, session wedged until client timeout).
 */
public class PipedStreamSessionTest {

    @CommandDefinition(name = "hello", description = "hello")
    public static class HelloCommand implements Command<CommandInvocation> {
        @Option(name = "name", description = "name")
        String name;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            ci.println("Hello " + name);
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testBackToBackCommandsOverPipes() throws Exception {
        runSession(30);
    }

    private void runSession(int commandCount) throws Exception {
        PipedInputStream pipeIn = new PipedInputStream(4096);
        PipedOutputStream testOut;
        try {
            testOut = new PipedOutputStream(pipeIn);
        } catch (Exception e) {
            pipeIn.close();
            throw e;
        }
        ByteArrayOutputStream consoleOut = new ByteArrayOutputStream();
        TerminalConnection connection = new TerminalConnection(StandardCharsets.UTF_8, pipeIn, consoleOut, null);

        CountDownLatch readyLatch = new CountDownLatch(1);
        AtomicReference<CountDownLatch> completionLatch = new AtomicReference<>(new CountDownLatch(1));

        Thread replThread = new Thread(() -> AeshConsoleRunner.builder()
                .connection(connection)
                .command(HelloCommand.class)
                .commandExecutionListener((line, result, durationMs) -> completionLatch.get().countDown())
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
                    fail("Command " + i + " never completed." + threadDump());
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
            if (name.contains("Aesh") || name.contains("aesh") || name.contains("pipe")
                    || name.contains("Reader") || name.contains("repl") || name.contains("surefire")) {
                sb.append('"').append(name).append("\" state=").append(t.getState()).append('\n');
                StackTraceElement[] stack = entry.getValue();
                int shown = Math.min(stack.length, 25);
                for (int i = 0; i < shown; i++) {
                    sb.append("    at ").append(stack[i]).append('\n');
                }
            }
        }
        return sb.toString();
    }
}
