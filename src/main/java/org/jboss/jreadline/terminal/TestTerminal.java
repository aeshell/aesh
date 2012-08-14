/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
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
