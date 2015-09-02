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
package org.jboss.aesh.terminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.fusesource.jansi.AnsiOutputStream;
import org.fusesource.jansi.WindowsAnsiOutputStream;
import org.fusesource.jansi.internal.WindowsSupport;
import org.jboss.aesh.console.reader.ConsoleInputSession;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.util.LoggerUtil;

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class WindowsTerminal extends AbstractTerminal {

    private PrintStream stdOut;
    private PrintStream stdErr;
    private TerminalSize size;
    private ConsoleInputSession input;

    private long ttyPropsLastFetched;
    private static long TIMEOUT_PERIOD = 2000;

    private static final Logger LOGGER = LoggerUtil.getLogger(WindowsTerminal.class.getName());

    public WindowsTerminal() {
        super(LOGGER);
    }

    @Override
    public void init(Settings settings) {
        this.settings = settings;
        //setting up reader
        try {
            //AnsiConsole.systemInstall();
            this.stdOut = new PrintStream( new WindowsAnsiOutputStream(settings.getStdOut()), true);
            this.stdErr = new PrintStream( new WindowsAnsiOutputStream(settings.getStdErr()), true);

        }
        catch (Exception ioe) {
            this.stdOut = new PrintStream( new AnsiOutputStream(settings.getStdOut()), true);
            this.stdErr = new PrintStream( new AnsiOutputStream(settings.getStdErr()), true);
        }

        if(settings.getInputStream().equals(System.in)) {
            InputStream inStream = new InputStream() {
                @Override
                public int read() throws IOException {
                    return WindowsSupport.readByte();
                }

                @Override
                public int read(byte[] in) throws IOException {
                    byte[] tmp = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(WindowsSupport.readByte()).array();
                    in[0] = tmp[0];
                    return 1;
                }

                public void close() {
                    WindowsSupport.flushConsoleInputBuffer();
                }
            };
            this.input = new ConsoleInputSession(inStream);
            //this.input = new ConsoleInputSession(inStream).getExternalInputStream();
        }
        else {
            this.input = new ConsoleInputSession(settings.getInputStream());
        }
    }

    @Override
    public int[] read() throws IOException {
        return input.readAll();
    }

    @Override
    public boolean hasInput() {
        return input.hasInput();
    }

    private int getHeight() {
        int height;
        height = WindowsSupport.getWindowsTerminalHeight();
        ttyPropsLastFetched = System.currentTimeMillis();
        if(height < 1) {
            if(settings.isLogging())
                LOGGER.log(Level.SEVERE, "Fetched terminal height is "+height+", setting it to 24");
            height = 24;
        }
        return height;
    }

    private int getWidth() {
        int width;
        width = WindowsSupport.getWindowsTerminalWidth();
        ttyPropsLastFetched = System.currentTimeMillis();
        if(width < 1) {
            if(settings.isLogging())
                LOGGER.log(Level.SEVERE, "Fetched terminal width is "+width+", setting it to 80");
            width = 80;
        }
        return width;
    }

    @Override
    public TerminalSize getSize() {
        if(propertiesTimedOut()) {
            if(size == null) {
                size = new TerminalSize(getHeight(), getWidth());
            }
            else {
                size.setHeight(getHeight());
                size.setWidth(getWidth());
            }
        }
        return size;
    }

    @Override
    public boolean isEchoEnabled() {
        return false;
    }

    @Override
    public void reset() throws IOException {
    }

    @Override
    public void writeToInputStream(String data) {
        input.writeToInput(data);
    }

    @Override
    public void changeOutputStream(PrintStream output) {
        stdOut = output;
    }

    @Override
    public void close() throws IOException {
        input.stop();
    }

    private boolean propertiesTimedOut() {
        return (System.currentTimeMillis() -ttyPropsLastFetched) > TIMEOUT_PERIOD;
    }

    @Override
    public PrintStream err() {
        return stdErr;
    }

    @Override
    public PrintStream out() {
        return stdOut;
    }
}

