/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.aesh.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.Executable;
import org.aesh.command.Execution;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.result.ResultHandler;
import org.aesh.terminal.Connection;
import org.aesh.terminal.utils.Config;
import org.aesh.terminal.utils.LoggerUtil;

public final class NativeExecution implements Execution<CommandInvocation> {

    private final Connection connection;
    private final String command;
    private volatile CommandResult result;

    private static final Logger LOGGER = LoggerUtil.getLogger(NativeExecution.class.getName());

    public NativeExecution(Connection connection, String command) {
        this.connection = connection;
        this.command = command;
    }

    @Override
    public CommandInvocation getCommandInvocation() {
        return null;
    }

    @Override
    public Executable getExecutable() {
        return null;
    }

    @Override
    public Command<CommandInvocation> getCommand() {
        return null;
    }

    @Override
    public void populateCommand() {
    }

    @Override
    public ResultHandler getResultHandler() {
        return null;
    }

    @Override
    public CommandResult execute() throws InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(NativeCommand.build(command, Config.isWindows()));
        builder.redirectErrorStream(true);
        java.lang.Process process = null;
        InputStream stream = null;
        Consumer<int[]> savedHandler = connection.stdinHandler();
        try {
            process = builder.start();
            stream = process.getInputStream();
            final InputStream pipeStream = stream;
            Thread pump = new Thread(() -> pumpStream(pipeStream), "aesh-native-pump");
            pump.setDaemon(true);
            pump.start();
            connection.setStdinHandler(new StdinForwarder(process));
            try {
                result = CommandResult.valueOf(process.waitFor());
                return result;
            } finally {
                closeQuietly(stream);
            }
        } catch (InterruptedException e) {
            if (process != null) {
                process.destroyForcibly();
                closeQuietly(process.getInputStream());
                closeQuietly(process.getOutputStream());
                try {
                    process.waitFor(5, TimeUnit.SECONDS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            throw e;
        } catch (IOException e) {
            connection.write("Shell escape error: " + e.getMessage() + Config.getLineSeparator());
            result = CommandResult.FAILURE;
            return result;
        } finally {
            connection.setStdinHandler(savedHandler);
            closeQuietly(stream);
        }
    }

    /**
     * Forwards terminal input to the native process stdin while it runs.
     * Readline is not line-editing during native execution, so input is echoed
     * back for visibility. Ctrl-C (0x03) is not forwarded — it travels the
     * signal path and interrupts the process. Ctrl-D (0x04) closes process
     * stdin to signal EOF.
     */
    private final class StdinForwarder implements Consumer<int[]> {

        private final java.lang.Process process;
        private boolean stdinClosed;

        StdinForwarder(java.lang.Process process) {
            this.process = process;
        }

        @Override
        public synchronized void accept(int[] input) {
            if (stdinClosed)
                return;
            OutputStream stdin = process.getOutputStream();
            try {
                for (int codePoint : input) {
                    if (codePoint == 0x03)
                        continue;
                    if (codePoint == 0x04) {
                        closeStdin();
                        return;
                    }
                    byte[] bytes = new String(new int[] { codePoint }, 0, 1)
                            .getBytes(StandardCharsets.UTF_8);
                    stdin.write(bytes);
                    connection.write(new String(bytes, StandardCharsets.UTF_8));
                }
                stdin.flush();
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Native stdin forward ended", e);
                closeStdin();
            }
        }

        private void closeStdin() {
            stdinClosed = true;
            closeQuietly(process.getOutputStream());
        }
    }

    private void pumpStream(InputStream stream) {
        byte[] buffer = new byte[1024];
        try {
            int length;
            while ((length = stream.read(buffer)) != -1) {
                connection.write(new String(buffer, 0, length));
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Native output pump ended", e);
        }
    }

    private void closeQuietly(java.io.Closeable stream) {
        if (stream == null)
            return;
        try {
            stream.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public CommandResult getResult() {
        return result;
    }

    @Override
    public void setResult(CommandResult result) {
        this.result = result;
    }

    @Override
    public void clearQueuedLine() {
    }
}
