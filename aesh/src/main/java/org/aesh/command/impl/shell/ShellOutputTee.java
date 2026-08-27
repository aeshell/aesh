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
package org.aesh.command.impl.shell;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.aesh.command.shell.Shell;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.Connection;
import org.aesh.terminal.Key;
import org.aesh.terminal.tty.Size;
import org.aesh.terminal.utils.Config;
import org.aesh.terminal.utils.Parser;

/**
 * Shell decorator that tees command output to a handler while also
 * forwarding it to the delegate shell. Non-output methods delegate
 * directly with no overhead.
 * <p>
 * Only instantiated when a commandOutputHandler is configured, so
 * there is zero overhead on the output path when not in use.
 *
 * @since 3.17
 */
public class ShellOutputTee implements Shell {

    private final Shell delegate;
    private final Consumer<String> handler;

    public ShellOutputTee(Shell delegate, Consumer<String> handler) {
        this.delegate = delegate;
        this.handler = handler;
    }

    @Override
    public void write(String msg, boolean paging) {
        handler.accept(msg);
        delegate.write(msg, paging);
    }

    @Override
    public void writeln(String msg, boolean paging) {
        handler.accept(msg + Config.getLineSeparator());
        delegate.writeln(msg, paging);
    }

    @Override
    public void write(int[] out) {
        handler.accept(Parser.fromCodePoints(out));
        delegate.write(out);
    }

    @Override
    public void write(char out) {
        handler.accept(String.valueOf(out));
        delegate.write(out);
    }

    @Override
    public String readLine() throws InterruptedException {
        return delegate.readLine();
    }

    @Override
    public String readLine(Prompt prompt) throws InterruptedException {
        return delegate.readLine(prompt);
    }

    @Override
    public Key read() throws InterruptedException {
        return delegate.read();
    }

    @Override
    public Key read(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.read(timeout, unit);
    }

    @Override
    public Key read(Prompt prompt) throws InterruptedException {
        return delegate.read(prompt);
    }

    @Override
    public boolean enableAlternateBuffer() {
        return delegate.enableAlternateBuffer();
    }

    @Override
    public boolean enableMainBuffer() {
        return delegate.enableMainBuffer();
    }

    @Override
    public Size size() {
        return delegate.size();
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Connection connection() {
        return delegate.connection();
    }
}
