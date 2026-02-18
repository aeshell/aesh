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
package org.aesh.tamboui;

import java.nio.charset.Charset;
import java.util.function.Consumer;

import org.aesh.terminal.Attributes;
import org.aesh.terminal.Connection;
import org.aesh.terminal.Device;
import org.aesh.terminal.tty.Capability;
import org.aesh.terminal.tty.Signal;
import org.aesh.terminal.tty.Size;

/**
 * A Connection wrapper that delegates everything to the underlying connection
 * but makes {@link #close()} a no-op. This prevents TamboUI's
 * {@code AeshBackend.close()} from closing the aesh Connection, which would
 * terminate the aesh session.
 *
 * @author Aesh team
 */
class NonClosingConnection implements Connection {

    private final Connection delegate;

    NonClosingConnection(Connection delegate) {
        this.delegate = delegate;
    }

    @Override
    public void close() {
        // intentionally empty — aesh owns this connection's lifecycle
    }

    @Override
    public void close(int exit) {
        // intentionally empty
    }

    @Override
    public Device device() {
        return delegate.device();
    }

    @Override
    public Size size() {
        return delegate.size();
    }

    @Override
    public Consumer<Size> getSizeHandler() {
        return delegate.getSizeHandler();
    }

    @Override
    public void setSizeHandler(Consumer<Size> handler) {
        delegate.setSizeHandler(handler);
    }

    @Override
    public Consumer<Signal> getSignalHandler() {
        return delegate.getSignalHandler();
    }

    @Override
    public void setSignalHandler(Consumer<Signal> handler) {
        delegate.setSignalHandler(handler);
    }

    @Override
    public Consumer<int[]> getStdinHandler() {
        return delegate.getStdinHandler();
    }

    @Override
    public void setStdinHandler(Consumer<int[]> handler) {
        delegate.setStdinHandler(handler);
    }

    @Override
    public Consumer<int[]> stdoutHandler() {
        return delegate.stdoutHandler();
    }

    @Override
    public void setCloseHandler(Consumer<Void> closeHandler) {
        delegate.setCloseHandler(closeHandler);
    }

    @Override
    public Consumer<Void> getCloseHandler() {
        return delegate.getCloseHandler();
    }

    @Override
    public void openBlocking() {
        delegate.openBlocking();
    }

    @Override
    public void openNonBlocking() {
        delegate.openNonBlocking();
    }

    @Override
    public boolean reading() {
        return delegate.reading();
    }

    @Override
    public boolean put(Capability capability, Object... params) {
        return delegate.put(capability, params);
    }

    @Override
    public Attributes getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public void setAttributes(Attributes attr) {
        delegate.setAttributes(attr);
    }

    @Override
    public Charset inputEncoding() {
        return delegate.inputEncoding();
    }

    @Override
    public Charset outputEncoding() {
        return delegate.outputEncoding();
    }

    @Override
    public boolean supportsAnsi() {
        return delegate.supportsAnsi();
    }
}
