/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import org.fusesource.jansi.AnsiOutputStream;
import org.fusesource.jansi.WindowsAnsiOutputStream;
import org.fusesource.jansi.internal.WindowsSupport;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.util.ANSI;
import org.jboss.aesh.util.LoggerUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class WindowsTerminal extends AbstractTerminal {

    private Writer stdOut;
    private Writer stdErr;
    private InputStream input;
    private int width;
    private int height;
    private TerminalSize size;

    private long ttyPropsLastFetched;
    private static long TIMEOUT_PERIOD = 2000;

    private static final Logger logger = LoggerUtil.getLogger(POSIXTerminal.class.getName());

    public WindowsTerminal() {
        super(logger);
    }

    @Override
    public void init(InputStream inputStream, OutputStream stdOut, OutputStream stdErr) {
        //setting up reader
        try {
            //AnsiConsole.systemInstall();
            this.stdOut = new PrintWriter( new OutputStreamWriter(new WindowsAnsiOutputStream(stdOut)));
            this.stdErr = new PrintWriter( new OutputStreamWriter(new WindowsAnsiOutputStream(stdErr)));
        }
        catch (Exception ioe) {
            this.stdOut = new PrintWriter( new OutputStreamWriter(new AnsiOutputStream(stdOut)));
            this.stdErr = new PrintWriter( new OutputStreamWriter(new AnsiOutputStream(stdErr)));
        }

        this.input = inputStream;
    }

    @Override
    public int[] read(boolean readAhead) throws IOException {
        if(Settings.getInstance().isAnsiConsole())
            return new int[] {WindowsSupport.readByte()};
        else {
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
    }

    @Override
    public void writeToStdOut(String out) throws IOException {
        if(out != null && out.length() > 0) {
            stdOut.write(out);
            stdOut.flush();
        }
    }

    @Override
    public void writeToStdOut(char[] out) throws IOException {
        if(out != null && out.length > 0) {
            stdOut.write(out);
            stdOut.flush();
        }
    }

    @Override
    public void writeToStdOut(char out) throws IOException {
        stdOut.write(out);
        stdOut.flush();
    }

    @Override
    public void writeToStdErr(String err) throws IOException {
        if(err != null && err.length() > 0) {
            stdOut.write(err);
            stdOut.flush();
        }
    }

    @Override
    public void writeToStdErr(char[] err) throws IOException {
        if(err != null && err.length > 0) {
            stdOut.write(err);
            stdOut.flush();
        }
    }

    @Override
    public void writeToStdErr(char err) throws IOException {
        stdOut.write(err);
        stdOut.flush();
    }

    private int getHeight() {
        int height;
        height = WindowsSupport.getWindowsTerminalHeight();
        ttyPropsLastFetched = System.currentTimeMillis();
        if(height < 1) {
            if(Settings.getInstance().isLogging())
                logger.log(Level.SEVERE, "Fetched terminal height is "+height+", setting it to 24");
            height = 24;
        }
        return height;
    }

    private int getWidth() {
        int width;
        width = WindowsSupport.getWindowsTerminalWidth();
        ttyPropsLastFetched = System.currentTimeMillis();
        if(width < 1) {
            if(Settings.getInstance().isLogging())
                logger.log(Level.SEVERE, "Fetched terminal width is "+width+", setting it to 80");
            width = 80;
        }
        return width;
    }

    @Override
    public TerminalSize getSize() {
        if(propertiesTimedOut()) {
            size.setHeight(getHeight());
            size.setWidth(getWidth());
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

    private boolean propertiesTimedOut() {
        return (System.currentTimeMillis() -ttyPropsLastFetched) > TIMEOUT_PERIOD;
    }
}

