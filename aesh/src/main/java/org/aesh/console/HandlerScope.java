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

import java.util.function.Consumer;

import org.aesh.terminal.Connection;
import org.aesh.terminal.tty.Signal;

public final class HandlerScope implements AutoCloseable {

    private final Connection connection;
    private final boolean signal;
    private final Consumer<Signal> savedSignalHandler;
    private final Consumer<int[]> savedStdinHandler;
    private boolean closed;

    private HandlerScope(Connection connection, boolean signal,
            Consumer<Signal> savedSignalHandler, Consumer<int[]> savedStdinHandler) {
        this.connection = connection;
        this.signal = signal;
        this.savedSignalHandler = savedSignalHandler;
        this.savedStdinHandler = savedStdinHandler;
    }

    public static HandlerScope stdin(Connection connection, Consumer<int[]> handler) {
        Consumer<int[]> saved = connection.stdinHandler();
        connection.setStdinHandler(handler);
        return new HandlerScope(connection, false, null, saved);
    }

    public static HandlerScope signal(Connection connection, Consumer<Signal> handler) {
        Consumer<Signal> saved = connection.signalHandler();
        connection.setSignalHandler(handler);
        return new HandlerScope(connection, true, saved, null);
    }

    @Override
    public void close() {
        if (closed)
            return;
        closed = true;
        if (signal)
            connection.setSignalHandler(savedSignalHandler);
        else
            connection.setStdinHandler(savedStdinHandler);
    }
}
