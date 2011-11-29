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


import org.jboss.jreadline.console.reader.CharInputStreamReader;

import java.io.*;

/**
 * Terminal that should work on most POSIX systems
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class POSIXTerminal implements Terminal {

    private int height = -1;
    private int width = -1;
    private boolean echoEnabled;
    private String ttyConfig;
    private String ttyProps;
    private long ttyPropsLastFetched;
    private boolean restored = false;

    private CharInputStreamReader reader;
    private Writer writer;

    @Override
    public void init(InputStream inputStream, OutputStream outputStream) {
        // save the initial tty configuration
        try {
            ttyConfig = stty("-g");

            // sanity check
            if ((ttyConfig.length() == 0)
                    || ((!ttyConfig.contains("=")) && (!ttyConfig.contains(":")))) {
                throw new RuntimeException("Unrecognized stty code: " + ttyConfig);
            }

            // set the console to be character-buffered instead of line-buffered
            // -ixon will give access to ctrl-s/ctrl-q
            stty("-ixon -icanon min 1");

            // disable character echoing
            stty("-echo");
            echoEnabled = false;

            //setting up reader
            reader = new CharInputStreamReader(inputStream);
        }
        catch (IOException ioe) {
            System.err.println("TTY failed with: " + ioe.getMessage());
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        // at exit, restore the original tty configuration (for JDK 1.3+)
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void start() {
                try {
                    reset();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        writer = new PrintWriter( new OutputStreamWriter(outputStream));
    }

    /**
     * @see org.jboss.jreadline.terminal.Terminal
     */
    @Override
    public int read() throws IOException {
        return reader.read();
    }

    /**
     * @see org.jboss.jreadline.terminal.Terminal
     */
    @Override
    public void write(String out) throws IOException {
        if(out != null && out.length() > 0) {
            writer.write(out);
            writer.flush();
        }
    }

    /**
     * @see org.jboss.jreadline.terminal.Terminal
     */
    @Override
    public void write(char[] out) throws IOException {
        if(out != null && out.length > 0) {
            writer.write(out);
            writer.flush();
        }
    }

    /**
     * @see org.jboss.jreadline.terminal.Terminal
     */
    @Override
    public void write(char out) throws IOException {
        writer.write(out);
        writer.flush();
    }

    /**
     * @see org.jboss.jreadline.terminal.Terminal
     */
    @Override
    public int getHeight() {
        if(height < 0) {
            try {
                height = getTerminalProperty("rows");
            }
            catch (Exception e) { /*ignored */ }
            if(height < 0)
                height = 24;
        }

        return height;
    }

    /**
     * @see org.jboss.jreadline.terminal.Terminal
     */
    @Override
    public int getWidth() {
        if(width < 0) {
            try {
                width = getTerminalProperty("columns");
            }
            catch (Exception e) { /* ignored */ }

            if(width < 0)
                width = 80;
        }
        return width;
    }

    /**
     * @see org.jboss.jreadline.terminal.Terminal
     */
    @Override
    public boolean isEchoEnabled() {
        return echoEnabled;
    }

    /**
     * @see org.jboss.jreadline.terminal.Terminal
     */
    public void reset() throws Exception {
        if(!restored) {
            if (ttyConfig != null) {
                stty(ttyConfig);
                ttyConfig = null;
                restored = true;
            }
        }
    }

    private int getTerminalProperty(String prop) throws IOException, InterruptedException {
        // tty properties are cached so we don't have to worry too much about getting term widht/height
        if (ttyProps == null || System.currentTimeMillis() - ttyPropsLastFetched > 1000) {
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
    protected static String stty(final String args)
            throws IOException, InterruptedException {
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
               e.printStackTrace();
            }
        }

        return new String(bout.toByteArray());
    }

}
