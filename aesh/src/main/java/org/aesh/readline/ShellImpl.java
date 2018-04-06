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
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.readline;

import org.aesh.command.shell.Shell;
import org.aesh.readline.action.ActionDecoder;
import org.aesh.readline.terminal.Key;
import org.aesh.terminal.Connection;
import org.aesh.terminal.tty.Capability;
import org.aesh.terminal.tty.Size;
import org.aesh.utils.Config;

import java.util.concurrent.CountDownLatch;
import org.aesh.terminal.Attributes;
import org.aesh.utils.ANSI;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
class ShellImpl implements Shell {

    private Connection connection;
    private Readline readline;
    private StringBuilder outputCollector;

    ShellImpl(Connection connection, Readline readline) {
        this.connection = connection;
        this.readline = readline;
    }

    void startCollectOutput() {
        outputCollector = null;
    }

    // handle "a la" 'more' scrolling
    // Doesn't take into account wrapped lines (lines that are longer than the
    // terminal width. This could make a page to skip some lines.
    void printCollectedOutput() {
        if (outputCollector == null) {
            return;
        }
        try {
            String line = outputCollector.toString();
            if (line.isEmpty()) {
                return;
            }
            // '\R' will match any line break.
            // -1 to keep empty lines at the end of content.
            String[] lines = line.split("\\R", -1);
            int max = connection.size().getHeight();
            int currentLines = 0;
            int allLines = 0;
            while (allLines < lines.length) {
                if (currentLines > max - 2) {
                    try {
                        connection.write(ANSI.CURSOR_SAVE);
                        int percentage = (allLines * 100) / lines.length;
                        connection.write("--More(" + percentage + "%)--");
                        Key k = read();
                        connection.write(ANSI.CURSOR_RESTORE);
                        connection.stdoutHandler().accept(ANSI.ERASE_LINE_FROM_CURSOR);
                        if (k == null) { // interrupted, exit.
                            allLines = lines.length;
                        } else {
                            switch (k) {
                                case SPACE: {
                                    currentLines = 0;
                                    break;
                                }
                                case ENTER:
                                case CTRL_M: { // On Mac, CTRL_M...
                                    currentLines -= 1;
                                    break;
                                }
                                case q: {
                                    allLines = lines.length;
                                    break;
                                }
                            }
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ex);
                    }
                } else {
                    String l = lines[allLines];
                    currentLines += 1;
                    allLines += 1;
                    // Do not add an extra \n
                    // The \n has been added by the previous line.
                    if (allLines == lines.length) {
                        if (l.isEmpty()) {
                            continue;
                        }
                    }
                    connection.write(l + Config.getLineSeparator());
                }
            }
        } finally {
            outputCollector = null;
        }
    }

    @Override
    public void write(String msg, boolean page) {
        if (connection.supportsAnsi() && page) {
            if (outputCollector == null) {
                outputCollector = new StringBuilder();
            }
            outputCollector.append(msg);
        } else {
            connection.write(msg);
        }
    }

    @Override
    public void writeln(String msg, boolean page) {
        if (connection.supportsAnsi() && page) {
            if (outputCollector == null) {
                outputCollector = new StringBuilder();
            }
            outputCollector.append(msg).append(Config.getLineSeparator());
        } else {
            connection.write(msg + Config.getLineSeparator());
        }
    }

    @Override
    public void write(int[] out) {
        connection.stdoutHandler().accept(out);
    }

    @Override
    public void write(char out) {
       connection.stdoutHandler().accept(new int[]{out});
    }

    @Override
    public String readLine() throws InterruptedException {
        return readLine(new Prompt(""));
    }

    @Override
    public String readLine(Prompt prompt) throws InterruptedException {
        printCollectedOutput();
        startCollectOutput();
        final String[] out = {null};
        CountDownLatch latch = new CountDownLatch(1);
        readline.readline(connection, prompt, event -> {
            out[0] = event;
            latch.countDown();
        });
        try {
            // Wait until interrupted
            latch.await();
        }
        finally {
            connection.setStdinHandler(null);
        }
        return out[0];
    }

    @Override
    public Key read() throws InterruptedException {
        ActionDecoder decoder = new ActionDecoder();
        final Key[] key = {null};
        CountDownLatch latch = new CountDownLatch(1);
        Attributes attributes = connection.enterRawMode();
        try {
            connection.setStdinHandler(keys -> {
                decoder.add(keys);
                if (decoder.hasNext()) {
                    key[0] = Key.findStartKey(decoder.next().buffer().array());
                    latch.countDown();
                }
            });
            try {
                // Wait until interrupted
                latch.await();
            } finally {
                connection.setStdinHandler(null);
            }
        } finally {
            connection.setAttributes(attributes);
        }
        return key[0];
    }


    @Override
    public Key read(Prompt prompt) throws InterruptedException {
        //TODO
        return null;
    }

    @Override
    public boolean enableAlternateBuffer() {
        return connection.put(Capability.enter_ca_mode);
    }

    @Override
    public boolean enableMainBuffer() {
        return connection.put(Capability.exit_ca_mode);
    }

    @Override
    public Size size() {
        return connection.size();
    }

    @Override
    public void clear() {
        connection.put(Capability.clear_screen);
    }
}
