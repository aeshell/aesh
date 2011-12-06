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

import org.fusesource.jansi.AnsiOutputStream;
import org.fusesource.jansi.WindowsAnsiOutputStream;
import org.fusesource.jansi.internal.WindowsSupport;

import java.io.*;

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class WindowsTerminal implements Terminal {

    private Writer writer;

    @Override
    public void init(InputStream inputStream, OutputStream outputStream) {
        if(inputStream == System.in) {
            System.out.println("Using System.in");
        }

        //setting up reader
        try {
            //AnsiConsole.systemInstall();
            writer = new PrintWriter( new OutputStreamWriter(new WindowsAnsiOutputStream(outputStream)));
        }
        catch (Exception ioe) {
            writer = new PrintWriter( new OutputStreamWriter(new AnsiOutputStream(outputStream)));
        }
    }

    @Override
    public int[] read(boolean readAhead) throws IOException {
        //return reader.read();
        return new int[] {WindowsSupport.readByte()};
    }

    @Override
    public void write(String out) throws IOException {
        if(out != null && out.length() > 0) {
            writer.write(out);
            writer.flush();
        }
    }

    @Override
    public void write(char[] out) throws IOException {
        if(out != null && out.length > 0) {
            writer.write(out);
            writer.flush();
        }
    }

    @Override
    public void write(char out) throws IOException {
        writer.write(out);
        writer.flush();
    }

    @Override
    public int getHeight() {
        return WindowsSupport.getWindowsTerminalHeight();
    }

    @Override
    public int getWidth() {
        return WindowsSupport.getWindowsTerminalWidth();
    }

    @Override
    public boolean isEchoEnabled() {
        return false;  
    }

    @Override
    public void reset() throws IOException {
    }
}

