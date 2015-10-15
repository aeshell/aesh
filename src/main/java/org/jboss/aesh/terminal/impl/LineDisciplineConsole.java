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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.IntConsumer;

import org.jboss.aesh.terminal.api.Attributes;
import org.jboss.aesh.terminal.api.Attributes.ControlChar;
import org.jboss.aesh.terminal.api.Attributes.InputFlag;
import org.jboss.aesh.terminal.api.Attributes.LocalFlag;
import org.jboss.aesh.terminal.api.Attributes.OutputFlag;
import org.jboss.aesh.terminal.api.Console;
import org.jboss.aesh.terminal.api.Size;
import org.jboss.aesh.terminal.utils.InputStreamReader;

/**
 * Abstract console with support for line discipline.
 * The {@link Console} interface represents the slave
 * side of a PTY, but implementations derived from this class
 * will handle both the slave and master side of things.
 *
 * In order to correctly handle line discipline, the console
 * needs to read the input in advance in order to raise the
 * signals as fast as possible.
 * For example, when the user hits Ctrl+C, we can't wait until
 * the application consumes all the read events.
 * The same applies to echoing, when enabled, as the echoing
 * has to happen as soon as the user hit the keyboard, and not
 * only when the application running in the terminal processes
 * the input.
 */
public class LineDisciplineConsole extends AbstractConsole {

    private static final int PIPE_SIZE = 1024;

    /*
     * Master output stream
     */
    protected final OutputStream masterOutput;

    /*
     * Slave input pipe write side
     */
    protected final OutputStream slaveInputPipe;

    /*
     * Slave streams
     */
    protected final InputStream slaveInput;
    protected final Reader slaveReader;
    protected final PrintWriter slaveWriter;
    protected final OutputStream slaveOutput;

    /**
     * Console data
     */
    protected final String encoding;
    protected final Attributes attributes;
    protected final Size size;

    /**
     * Application
     */
    protected IntConsumer application;
    protected CharsetDecoder decoder;
    protected ByteBuffer bytes;
    protected CharBuffer chars;

    public LineDisciplineConsole(String name,
                                 String type,
                                 OutputStream masterOutput,
                                 String encoding) throws IOException {
        super(name, type);
        PipedInputStream input = new PipedInputStream(PIPE_SIZE);
        this.slaveInputPipe = new PipedOutputStream(input);
        // This is a hack to fix a problem in gogo where closure closes
        // streams for commands if they are instances of PipedInputStream.
        // So we need to get around and make sure it's not an instance of
        // that class by using a dumb FilterInputStream class to wrap it.
        this.slaveInput = new FilterInputStream(input) {};
        this.slaveReader = new InputStreamReader(slaveInput, encoding);
        this.slaveOutput = new FilteringOutputStream();
        this.slaveWriter = new PrintWriter(new OutputStreamWriter(slaveOutput, encoding));
        this.masterOutput = masterOutput;
        this.encoding = encoding;
        this.attributes = new Attributes();
        this.size = new Size(50, 160);
        parseInfoCmp();
    }

    public Reader reader() {
        return slaveReader;
    }

    public PrintWriter writer() {
        return slaveWriter;
    }

    @Override
    public InputStream input() {
        return slaveInput;
    }

    @Override
    public OutputStream output() {
        return slaveOutput;
    }

    public Attributes getAttributes() {
        Attributes attr = new Attributes();
        attr.copy(attributes);
        return attr;
    }

    public void setAttributes(Attributes attr) {
        attributes.copy(attr);
    }

    public Size getSize() {
        return new Size(size.getHeight(), size.getWidth());
    }

    public void setSize(Size sz) {
        size.setHeight(sz.getHeight());
        size.setWidth(sz.getWidth());
    }

   @Override
    public void raise(Signal signal) {
        assert signal != null;
        // Do not call clear() atm as this can cause
        // deadlock between reading / writing threads
        // TODO: any way to fix that ?
        /*
        if (!attributes.getLocalFlag(LocalFlag.NOFLSH)) {
            try {
                slaveReader.clear();
            } catch (IOException e) {
                // Ignore
            }
        }
        */
        echoSignal(signal);
        super.raise(signal);
    }

    /**
     * Master input processing.
     * All data coming to the console should be provided
     * using this method.
     *
     * @param c the input byte
     * @throws IOException
     */
    public void processInputByte(int c) throws IOException {
        if (attributes.getLocalFlag(LocalFlag.ISIG)) {
            if (c == attributes.getControlChar(ControlChar.VINTR)) {
                raise(Signal.INT);
                return;
            } else if (c == attributes.getControlChar(ControlChar.VQUIT)) {
                raise(Signal.QUIT);
                return;
            } else if (c == attributes.getControlChar(ControlChar.VSUSP)) {
                raise(Signal.TSTP);
                return;
            } else if (c == attributes.getControlChar(ControlChar.VSTATUS)) {
                raise(Signal.INFO);
            }
        }
        if (c == '\r') {
            if (attributes.getInputFlag(InputFlag.IGNCR)) {
                return;
            }
            if (attributes.getInputFlag(InputFlag.ICRNL)) {
                c = '\n';
            }
        } else if (c == '\n' && attributes.getInputFlag(InputFlag.INLCR)) {
            c = '\r';
        }
        if (attributes.getLocalFlag(LocalFlag.ECHO)) {
            processOutputByte(c);
            masterOutput.flush();
        }
        slaveInputPipe.write(c);
        slaveInputPipe.flush();
    }

    /**
     * Master output processing.
     * All data going to the master should be provided by this method.
     *
     * @param c the output byte
     * @throws IOException
     */
    protected void processOutputByte(int c) throws IOException {
        if (attributes.getOutputFlag(OutputFlag.OPOST)) {
            if (c == '\n') {
                if (attributes.getOutputFlag(OutputFlag.ONLCR)) {
                    masterOutput.write('\r');
                    masterOutput.write('\n');
                    return;
                }
            }
        }
        masterOutput.write(c);
    }

    public void close() throws IOException {
        try {
            slaveReader.close();
        } finally {
            try {
                slaveInputPipe.close();
            } finally {
                try {
                } finally {
                    slaveWriter.close();
                }
            }
        }
    }

    private class FilteringOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            processOutputByte(b);
        }

        @Override
        public void flush() throws IOException {
            masterOutput.flush();
        }

        @Override
        public void close() throws IOException {
            masterOutput.close();
        }
    }
}
