/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jboss.aesh.terminal.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.jboss.aesh.terminal.utils.InputStreamReader;
import org.jboss.aesh.terminal.utils.ShutdownHooks;
import org.jboss.aesh.terminal.utils.ShutdownHooks.Task;
import org.jboss.aesh.terminal.utils.Signals;

public class PosixSysConsole extends AbstractPosixConsole {

    protected final InputStream input;
    protected final OutputStream output;
    protected final Reader reader;
    protected final PrintWriter writer;
    protected final Map<Signal, Object> nativeHandlers = new HashMap<>();
    protected final Task closer;

    public PosixSysConsole(String name, String type, Pty pty, String encoding, boolean nativeSignals) throws IOException {
        super(name, type, pty);
        assert encoding != null;
        this.input = pty.getSlaveInput();
        this.output = pty.getSlaveOutput();
        this.reader = new InputStreamReader(input, encoding);
        this.writer = new PrintWriter(new OutputStreamWriter(output, encoding));
        parseInfoCmp();
        if (nativeSignals) {
            for (final Signal signal : Signal.values()) {
                nativeHandlers.put(signal, Signals.register(signal.name(), () -> raise(signal)));
            }
        }
        closer = PosixSysConsole.this::close;
        ShutdownHooks.add(closer);
    }

    public Reader reader() {
        return reader;
    }

    public PrintWriter writer() {
        return writer;
    }

    @Override
    public InputStream input() {
        return input;
    }

    @Override
    public OutputStream output() {
        return output;
    }

    @Override
    public void close() throws IOException {
        ShutdownHooks.remove(closer);
        for (Map.Entry<Signal, Object> entry : nativeHandlers.entrySet()) {
            Signals.unregister(entry.getKey().name(), entry.getValue());
        }
        super.close();
    }
}
