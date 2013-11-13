/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.jboss.aesh.console.reader.AeshStandardStream;

/**
 * A dummy terminal used for tests
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class TestTerminal implements Terminal, Shell {

    private InputStream input;
    private PrintStream writer;
    private TerminalSize size;

    @Override
    public void init(InputStream inputStream, PrintStream stdOut, PrintStream stdErr) {
        input = inputStream;
        writer = new PrintStream(stdOut, true);
        size = new TerminalSize(24,80);
    }

    @Override
    public int[] read(boolean readAhead) throws IOException {
        int input = this.input.read();
        int available = this.input.available();
        if(available > 1 && readAhead) {
            int[] in = new int[available];
            in[0] = input;
            for(int c=1; c < available; c++ )
                in[c] = this.input.read();

            return in;
        }
        else
            return new int[] {input};
    }

    @Override
    public TerminalSize getSize() {
        return size;
    }

    @Override
    public CursorPosition getCursor() {
        return new CursorPosition(0,0);
    }

    @Override
    public void setCursor(CursorPosition cp) {
    }

    @Override
    public void moveCursor(int r, int c) {
    }

    @Override
    public boolean isMainBuffer() {
        return true;
    }

    @Override
    public void enableAlternateBuffer() {
        //do nothing
    }

    @Override
    public void enableMainBuffer() {
        //do nothing
    }

    @Override
    public boolean isEchoEnabled() {
        return false;
    }

    @Override
    public void reset() throws IOException {
    }

    @Override
    public Shell getShell() {
        return this;
    }

    @Override
    public void clear() throws IOException {
    }

    @Override
    public PrintStream err() {
        return writer;
    }

    @Override
    public AeshStandardStream in() {
        return new AeshStandardStream(new BufferedInputStream(input));
    }

    @Override
    public PrintStream out() {
        return writer;
    }

}
