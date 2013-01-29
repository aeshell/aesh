/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;

/**
 * A dummy terminal used for tests
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class TestTerminal implements Terminal {

    private InputStream input;
    private Writer writer;
    private TerminalSize size;

    @Override
    public void init(InputStream inputStream, OutputStream stdOut, OutputStream stdErr) {
        input = inputStream;
        writer = new PrintWriter(new OutputStreamWriter(stdOut));
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
    public void writeToStdOut(String out) throws IOException {
        if(out != null && out.length() > 0) {
            writer.write(out);
            writer.flush();
        }
    }

    @Override
    public void writeToStdOut(char[] out) throws IOException {
        if(out != null && out.length > 0) {
            writer.write(out);
            writer.flush();
        }
    }

    @Override
    public void writeToStdOut(char out) throws IOException {
        writer.write(out);
        writer.flush();
    }

    @Override
    public void writeToStdErr(String err) throws IOException {
        if(err != null && err.length() > 0) {
            writer.write(err);
            writer.flush();
        }
    }

    @Override
    public void writeToStdErr(char[] err) throws IOException {
        if(err != null && err.length > 0) {
            writer.write(err);
            writer.flush();
        }
    }

    @Override
    public void writeToStdErr(char err) throws IOException {
        writer.write(err);
        writer.flush();
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
    public void setCursor(CursorPosition cp) throws IOException {
    }

    @Override
    public void moveCursor(int r, int c) throws IOException {
    }

    @Override
    public void writeChar(TerminalCharacter character) throws IOException {
        writeToStdOut(character.getCharacter());
    }

    @Override
    public void writeChars(List<TerminalCharacter> chars) throws IOException {
        for(TerminalCharacter c : chars)
            writeToStdOut(c.getCharacter());
    }

     @Override
    public boolean isEchoEnabled() {
        return false;
    }

    @Override
    public void reset() throws IOException {
    }

    @Override
    public void clear() throws IOException {
    }
}
