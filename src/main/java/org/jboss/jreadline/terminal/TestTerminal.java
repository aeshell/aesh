/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.terminal;

import java.io.*;

/**
 * A dummy terminal used for tests
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class TestTerminal implements Terminal {
    
    private InputStream input;
    private Writer writer;

    @Override
    public void init(InputStream inputStream, OutputStream stdOut, OutputStream stdErr) {
        input = inputStream;
        writer = new PrintWriter(new OutputStreamWriter(stdOut));
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
    public int getHeight() {
        return 24;
    }

    @Override
    public int getWidth() {
        return 80;
    }

    @Override
    public boolean isEchoEnabled() {
        return false;
    }

    @Override
    public void reset() throws IOException {
    }
}
