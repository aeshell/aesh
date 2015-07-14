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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.aesh.console.Config;
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

    private ConsoleInputSession input;
    private PrintStream stdOut;
    private PrintStream stdErr;

    private static final long TIMEOUT_PERIOD = 3000;

    private static final Logger LOGGER = LoggerUtil.getLogger(POSIXTerminal.class.getName());

    public POSIXTerminal() {
        super(LOGGER);
    }

    @Override
    public void init(Settings settings) {
        this.settings = settings;
        // save the initial tty configuration, this should work on posix and cygwin
        try {
            ttyConfig = stty("-g");

            // sanity check
            if ((ttyConfig.length() == 0)
                    || ((!ttyConfig.contains("=")) && (!ttyConfig.contains(":")))) {
                if(settings.isLogging())
                    LOGGER.log(Level.SEVERE, "Unrecognized stty code: "+ttyConfig);
                throw new RuntimeException("Unrecognized stty code: " + ttyConfig);
            }

            if(Config.isCygwin()) {
                stty("-ixon -icanon min 1 intr undef -echo");
            }
            else {
                // set the console to be character-buffered instead of line-buffered
                // -ixon will give access to ctrl-s/ctrl-q
                //intr undef ctrl-c will no longer send the interrupt signal
                //icrnl, translate carriage return to newline (needed when aesh is started in the background)
                //susb undef, ctrl-z will no longer send the stop signal
                stty("-ixon -icanon min 1 intr undef icrnl susp undef");

                // disable character echoing
                stty("-echo");
            }
            echoEnabled = false;

        }
        catch (IOException ioe) {
            if(settings.isLogging())
                LOGGER.log(Level.SEVERE, "tty failed: ",ioe);
        }
        catch (InterruptedException e) {
            if(settings.isLogging())
                LOGGER.log(Level.SEVERE, "failed while waiting for process to end: ",e);
            e.printStackTrace();
        }

        //setting up input
        //input = new AeshInputStream(settings.getInputStream());
        input = new ConsoleInputSession(settings.getInputStream());

        this.stdOut = settings.getStdOut();
        this.stdErr = settings.getStdErr();
        size = new TerminalSize(getHeight(), getWidth());
    }

    /**
     * @see org.jboss.aesh.terminal.Terminal
     */
    @Override
    public int[] read() throws IOException {
        return input.readAll();
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
                LOGGER.log(Level.SEVERE,"Failed to fetch terminal height: ",e);
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
                LOGGER.log(Level.SEVERE,"Failed to fetch terminal width: ",e);
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
                        LOGGER.log(Level.SEVERE,"Failed to reset terminal: ",e);
                }
            }
        }
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
        settings.getInputStream().close();
        input.stop();
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
                LOGGER.log(Level.SEVERE,"Failed to close streams: ",e);
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
