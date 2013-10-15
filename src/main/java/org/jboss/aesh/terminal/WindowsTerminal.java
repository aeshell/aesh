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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.fusesource.jansi.AnsiOutputStream;
import org.fusesource.jansi.WindowsAnsiOutputStream;
import org.fusesource.jansi.internal.WindowsSupport;
import org.jboss.aesh.console.reader.AeshPrintStream;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.util.LoggerUtil;

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class WindowsTerminal extends AbstractTerminal {

    private AeshPrintStream stdOut;
    private AeshPrintStream stdErr;
    private InputStream input;
    private TerminalSize size;

    private long ttyPropsLastFetched;
    private static long TIMEOUT_PERIOD = 2000;

    private static final Logger logger = LoggerUtil.getLogger(POSIXTerminal.class.getName());

    public WindowsTerminal(Settings settings) {
        super(settings, logger);
    }

    @Override
    public void init(InputStream inputStream, OutputStream stdOut, OutputStream stdErr) {
        //setting up reader
        try {
            //AnsiConsole.systemInstall();
            this.stdOut = new AeshPrintStream( new WindowsAnsiOutputStream(stdOut), true);
            this.stdErr = new AeshPrintStream( new WindowsAnsiOutputStream(stdErr), true);
        }
        catch (Exception ioe) {
            this.stdOut = new AeshPrintStream( new AnsiOutputStream(stdOut), true);
            this.stdErr = new AeshPrintStream( new AnsiOutputStream(stdErr), true);
        }

        this.input = inputStream;
    }

    @Override
    public int[] read(boolean readAhead) throws IOException {
        if(settings.isAnsiConsole())
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

    private int getHeight() {
        int height;
        height = WindowsSupport.getWindowsTerminalHeight();
        ttyPropsLastFetched = System.currentTimeMillis();
        if(height < 1) {
            if(settings.isLogging())
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
            if(settings.isLogging())
                logger.log(Level.SEVERE, "Fetched terminal width is "+width+", setting it to 80");
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

    private boolean propertiesTimedOut() {
        return (System.currentTimeMillis() -ttyPropsLastFetched) > TIMEOUT_PERIOD;
    }

    @Override
    public AeshPrintStream err() {
        return stdErr;
    }

    @Override
    public AeshPrintStream out() {
        return stdOut;
    }
}

