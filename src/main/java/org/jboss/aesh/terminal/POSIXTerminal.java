/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.aesh.console.reader.AeshInputStream;
import org.jboss.aesh.console.reader.ConsoleInputSession;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.util.LoggerUtil;

/**
 * Terminal that should work on most POSIX systems
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class POSIXTerminal extends AbstractTerminal {

    private TerminalSize size;
    private boolean echoEnabled;
    private String ttyConfig;
    private String ttyProps;
    private long ttyPropsLastFetched;
    private boolean restored = false;

    private AeshInputStream input;
    private PrintStream stdOut;
    private PrintStream stdErr;

    private static long TIMEOUT_PERIOD = 3000;

    private static final Logger logger = LoggerUtil.getLogger(POSIXTerminal.class.getName());

    public POSIXTerminal(final Settings settings) {
        super(settings, logger);
    }

    @Override
    public void init(InputStream inputStream, PrintStream stdOut, PrintStream stdErr) {
        // save the initial tty configuration
        try {
            ttyConfig = stty("-g");

            // sanity check
            if ((ttyConfig.length() == 0)
                    || ((!ttyConfig.contains("=")) && (!ttyConfig.contains(":")))) {
                if(settings.isLogging())
                    logger.log(Level.SEVERE, "Unrecognized stty code: "+ttyConfig);
                throw new RuntimeException("Unrecognized stty code: " + ttyConfig);
            }

            // set the console to be character-buffered instead of line-buffered
            // -ixon will give access to ctrl-s/ctrl-q
            stty("-ixon -icanon min 1");

            // disable character echoing
            stty("-echo");
            echoEnabled = false;

            //setting up input
            input =  new ConsoleInputSession(inputStream).getExternalInputStream();
        }
        catch (IOException ioe) {
            if(settings.isLogging())
                logger.log(Level.SEVERE, "tty failed: ",ioe);
        }
        catch (InterruptedException e) {
            if(settings.isLogging())
                logger.log(Level.SEVERE, "failed while waiting for process to end: ",e);
            e.printStackTrace();
        }

        this.stdOut = stdOut;
        this.stdErr = stdErr;
        size = new TerminalSize(getHeight(), getWidth());
    }

    /**
     * @see org.jboss.aesh.terminal.Terminal
     */
    @Override
    public int[] read(boolean readAhead) throws IOException {
        if(readAhead) {
            return input.readAll();
        }
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
        if(propertiesTimedOut()) {
            size.setHeight(getHeight());
            size.setWidth(getWidth());
        }
        return size;
    }

    private int getHeight() {
        int height = 0;
        try {
            height = getTerminalProperty("rows");
        }
        catch (Exception e) {
            if(settings.isLogging())
                logger.log(Level.SEVERE,"Failed to fetch terminal height: ",e);
        }
        //cant use height < 1
        if(height < 1)
            height = 24;

        return height;
    }

    private int getWidth() {
        int width = 0;
        try {
            width = getTerminalProperty("columns");
        }
        catch (Exception e) {
            if(settings.isLogging())
                logger.log(Level.SEVERE,"Failed to fetch terminal width: ",e);
        }
        //cant use with < 1
        if(width < 1)
            width = 80;

        return width;
    }

    /**
     * @see org.jboss.aesh.terminal.Terminal
     */
    @Override
    public boolean isEchoEnabled() {
        return echoEnabled;
    }

    /**
     * @see org.jboss.aesh.terminal.Terminal
     */
    @Override
    public void reset() throws IOException {
        if(!restored) {
            if (ttyConfig != null) {
                try {
                    stty(ttyConfig);
                    ttyConfig = null;
                    restored = true;
                }
                catch (InterruptedException e) {
                    if(settings.isLogging())
                        logger.log(Level.SEVERE,"Failed to reset terminal: ",e);
                }
            }
        }
    }

    private boolean propertiesTimedOut() {
        return (System.currentTimeMillis() -ttyPropsLastFetched) > TIMEOUT_PERIOD;
    }

    private int getTerminalProperty(String prop) throws IOException, InterruptedException {
        // tty properties are cached so we don't have to worry too much about getting term width/height
        if (ttyProps == null || propertiesTimedOut()) {
            ttyProps = stty("-a");
            ttyPropsLastFetched = System.currentTimeMillis();
        }
        // need to be able handle both output formats:
        // speed 9600 baud; 24 rows; 140 columns;
        // and:
        // speed 38400 baud; rows = 49; columns = 111;
        for (String str : ttyProps.split(";")) {
            str = str.trim();
            if (str.startsWith(prop)) {
                int index = str.lastIndexOf(" ");

                return Integer.parseInt(str.substring(index).trim());
            }
            else if (str.endsWith(prop)) {
                int index = str.indexOf(" ");

                return Integer.parseInt(str.substring(0, index).trim());
            }
        }

        return -1;
    }

    /**
     * Run stty with arguments on the active terminal
     *
     * @param args arguments
     * @return output
     * @throws IOException stream
     * @throws InterruptedException stream
     */
    protected static String stty(final String args) throws IOException, InterruptedException {
        return exec("stty " + args + " < /dev/tty").trim();
    }

    /**
     * Run a command and return the output
     *
     * @param cmd what to execute
     * @return output
     * @throws java.io.IOException stream
     * @throws InterruptedException stream
     */
    private static String exec(final String cmd) throws IOException, InterruptedException {
        return exec(new String[] { "sh", "-c", cmd });
    }

    /**
     * Run a command and return the output
     *
     * @param cmd the command
     * @return output
     * @throws IOException stream
     * @throws InterruptedException stream
     */
    private static String exec(final String[] cmd) throws IOException, InterruptedException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        Process p = Runtime.getRuntime().exec(cmd);
        int c;
        InputStream in = null;
        InputStream err = null;
        OutputStream out = null;

        try {
            in = p.getInputStream();

            while ((c = in.read()) != -1) {
                bout.write(c);
            }

            err = p.getErrorStream();

            while ((c = err.read()) != -1) {
                bout.write(c);
            }

            out = p.getOutputStream();

            p.waitFor();
        }
        finally {
            try {
                if(in != null)
                    in.close();
                if(err != null)
                    err.close();
                if(out != null)
                    out.close();
            }
            catch (Exception e) {
                logger.log(Level.SEVERE,"Failed to close streams: ",e);
            }
        }

        return new String(bout.toByteArray());
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
