package org.jboss.aesh.terminal.impl;

import java.io.IOError;
import java.io.IOException;

import org.jboss.aesh.terminal.api.Attributes;
import org.jboss.aesh.terminal.api.Size;

public abstract class AbstractPosixConsole extends AbstractConsole {

    protected final Pty pty;
    protected final Attributes originalAttributes;

    public AbstractPosixConsole(String name, String type, Pty pty) throws IOException {
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
