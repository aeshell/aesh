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
package org.jboss.aesh.terminal.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Console implementation with embedded line disciplined.
 *
 * This console is well-suited for supporting incoming external
 * connections, such as from the network (through telnet, ssh,
 * or any kind of protocol).
 * The console will start consuming the input in a separate thread
 * to generate interruption events.
 *
 * @see LineDisciplineTerminal
 */
public class ExternalTerminal extends LineDisciplineTerminal {

    private final AtomicBoolean closed = new AtomicBoolean();
    private final Thread pumpThread;
    protected final InputStream masterInput;

    public ExternalTerminal(String name, String type,
                            InputStream masterInput, OutputStream masterOutput,
                            String encoding) throws IOException {
        super(name, type, masterOutput, encoding);
        this.masterInput = masterInput;
        this.pumpThread = new Thread(this::pump, toString() + " input pump thread");
        this.pumpThread.start();
    }

    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            pumpThread.interrupt();
            super.close();
        }
    }

    public void pump() {
        try {
            while (true) {
                int c = masterInput.read();
                if (c < 0 || closed.get()) {
                    break;
                }
                processInputByte((char) c);
            }
        } catch (IOException e) {
            try {
                close();
            } catch (Throwable t) {
                e.addSuppressed(t);
            }
            if (!closed.get()) {
                e.printStackTrace();
            }
        }
    }

}
