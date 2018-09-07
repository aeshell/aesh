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

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
class ShellImpl implements Shell {

    private Connection connection;
    private Readline readline;
    private final PagingSupport pagingSupport;

    ShellImpl(Connection connection, Readline readline) {
        this(connection, readline, false);
    }

    ShellImpl(Connection connection, Readline readline, boolean search) {
        this.connection = connection;
        this.readline = readline;
        pagingSupport = new PagingSupport(connection, readline, search);
    }

    void startCollectOutput() {
        pagingSupport.reset();
    }

    // handle "a la" 'more' scrolling
    // Doesn't take into account wrapped lines (lines that are longer than the
    // terminal width. This could make a page to skip some lines.
    void printCollectedOutput() {
        pagingSupport.printCollectedOutput();
    }

    @Override
    public void write(String msg, boolean page) {
        if (connection.supportsAnsi() && page) {
            pagingSupport.addContent(msg);
        } else {
            connection.write(msg);
        }
    }

    @Override
    public void writeln(String msg, boolean page) {
        if (connection.supportsAnsi() && page) {
            pagingSupport.addContent(msg);
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
        pagingSupport.reset();
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
