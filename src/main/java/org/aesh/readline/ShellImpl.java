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

import org.aesh.command.Shell;
import org.aesh.readline.action.ActionDecoder;
import org.aesh.terminal.Key;
import org.aesh.tty.Capability;
import org.aesh.tty.Connection;
import org.aesh.tty.Size;
import org.aesh.util.Config;

import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
class ShellImpl implements Shell {

    private Connection connection;
    private Readline readline;

    public ShellImpl(Connection connection, Readline readline) {
        this.connection = connection;
        this.readline = readline;
    }

    @Override
    public void write(String out) {
        connection.write(out);
    }

    @Override
    public void writeln(String out) {
        connection.write(out + Config.getLineSeparator());
    }

    @Override
    public void write(int[] out) {
        connection.stdoutHandler().accept(out);
    }

    @Override
    public String readLine() throws InterruptedException {
        return readLine(new Prompt(""));
    }

    @Override
    public String readLine(Prompt prompt) throws InterruptedException {
        final String[] out = {null};
        CountDownLatch latch = new CountDownLatch(1);
        readline.readline(connection, prompt, event -> {
            out[0] = event;
            latch.countDown();
            connection.suspend();
        });
        connection.awake();
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
        connection.setStdinHandler(keys -> {
            decoder.add(keys);
            if (decoder.hasNext()) {
                key[0] = Key.findStartKey(decoder.next().buffer().array());
                latch.countDown();
                connection.suspend();
            }
        });
        connection.awake();
        try {
            // Wait until interrupted
            latch.await();
        }
        finally {
            connection.setStdinHandler(null);
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
