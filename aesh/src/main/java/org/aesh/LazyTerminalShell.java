/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh;

import java.io.Console;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.aesh.command.shell.Shell;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.Connection;
import org.aesh.terminal.Key;
import org.aesh.terminal.tty.Size;
import org.aesh.terminal.tty.TerminalConnection;
import org.aesh.terminal.utils.ANSI;
import org.aesh.terminal.utils.Parser;

/**
 * A Shell implementation that lazily initializes a TerminalConnection
 * on first access to {@link #size()} or {@link #connection()}.
 * <p>
 * This avoids terminal initialization overhead for commands that never
 * query the terminal size or use the connection. Output methods
 * ({@link #write}, {@link #writeln}) always work via System.out
 * without requiring the terminal connection.
 * <p>
 * If terminal initialization fails (e.g., headless/CI environment),
 * falls back to environment variables ($COLUMNS/$LINES) for size
 * and returns null for connection.
 */
class LazyTerminalShell implements Shell {

    private volatile TerminalConnection terminalConnection;
    private volatile boolean initialized;

    private synchronized void ensureInitialized() {
        if (!initialized) {
            initialized = true;
            try {
                terminalConnection = new TerminalConnection();
            } catch (Exception e) {
                // No terminal available (CI, pipe, headless) — fall back
            }
        }
    }

    @Override
    public void write(String out, boolean paging) {
        System.out.print(out);
    }

    @Override
    public void writeln(String out, boolean paging) {
        System.out.println(out);
    }

    @Override
    public void write(int[] out) {
        Console console = System.console();
        if (console != null) {
            console.writer().write(Parser.fromCodePoints(out));
            console.writer().flush();
        }
    }

    @Override
    public void write(char out) {
        System.out.print(out);
    }

    @Override
    public String readLine() {
        return readLine(new Prompt());
    }

    @Override
    public String readLine(Prompt prompt) {
        Console console = System.console();
        if (console != null) {
            if (prompt != null) {
                console.writer().print(Parser.fromCodePoints(prompt.getANSI()));
                console.writer().flush();
                if (prompt.isMasking()) {
                    return new String(console.readPassword());
                }
            }
            return console.readLine();
        }
        return null;
    }

    @Override
    public Key read() {
        return read(null);
    }

    @Override
    public Key read(long timeout, TimeUnit unit) throws InterruptedException {
        return read(null);
    }

    @Override
    public Key read(Prompt prompt) {
        Console console = System.console();
        if (console != null) {
            try {
                if (prompt != null) {
                    console.writer().print(Parser.fromCodePoints(prompt.getANSI()));
                    console.writer().flush();
                }
                int input = console.reader().read();
                return Key.getKey(new int[] { input });
            } catch (IOException e) {
                // I/O error reading key
            }
        }
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
        ensureInitialized();
        if (terminalConnection != null) {
            return terminalConnection.size();
        }
        return detectSizeFromEnv();
    }

    @Override
    public Connection connection() {
        ensureInitialized();
        return terminalConnection;
    }

    @Override
    public void clear() {
        Console console = System.console();
        if (console != null) {
            console.writer().write(Parser.fromCodePoints(ANSI.CLEAR_SCREEN));
        }
    }

    /**
     * Close the underlying TerminalConnection if it was initialized.
     * Called after command execution to restore terminal attributes.
     */
    void close() {
        if (terminalConnection != null) {
            terminalConnection.close();
        }
    }

    private static Size detectSizeFromEnv() {
        int cols = 80;
        int rows = 24;
        try {
            String envCols = System.getenv("COLUMNS");
            if (envCols != null)
                cols = Integer.parseInt(envCols);
        } catch (NumberFormatException ignored) {
        }
        try {
            String envRows = System.getenv("LINES");
            if (envRows != null)
                rows = Integer.parseInt(envRows);
        } catch (NumberFormatException ignored) {
        }
        return new Size(cols, rows);
    }
}
