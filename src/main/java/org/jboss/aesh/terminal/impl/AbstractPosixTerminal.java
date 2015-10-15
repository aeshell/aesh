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

import java.io.IOError;
import java.io.IOException;

import org.jboss.aesh.terminal.api.Attributes;
import org.jboss.aesh.terminal.api.Size;

public abstract class AbstractPosixTerminal extends AbstractTerminal {

    protected final Pty pty;
    protected final Attributes originalAttributes;

    public AbstractPosixTerminal(String name, String type, Pty pty) throws IOException {
        super(name, type);
        assert pty != null;
        this.pty = pty;
        this.originalAttributes = this.pty.getAttr();
    }

    protected Pty getPty() {
        return pty;
    }

    public Attributes getAttributes() {
        try {
            return pty.getAttr();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public void setAttributes(Attributes attr) {
        try {
            pty.setAttr(attr);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public Size getSize() {
        try {
            return pty.getSize();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public void setSize(Size size) {
        try {
            pty.setSize(size);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public void close() throws IOException {
        pty.setAttr(originalAttributes);
        pty.close();
    }
}
