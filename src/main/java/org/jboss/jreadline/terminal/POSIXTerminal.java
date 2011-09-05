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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class POSIXTerminal implements Terminal {

    public static final short ARROW_START = 27;
    public static final short ARROW_PREFIX = 91;
    public static final short ARROW_LEFT = 68;
    public static final short ARROW_RIGHT = 67;
    public static final short ARROW_UP = 65;
    public static final short ARROW_DOWN = 66;
    public static final short O_PREFIX = 79;
    public static final short HOME_CODE = 72;
    public static final short END_CODE = 70;

    public static final short DEL_THIRD = 51;
    public static final short DEL_SECOND = 126;

    private final static char DELETE = 127;
    private final static char BACKSPACE = '\b';

    private int height = -1;
    private int width = -1;
    private boolean echoEnabled;
    private String ttyConfig;
    private String ttyProps;
    private long ttyPropsLastFetched;
    private boolean backspaceDeleteSwitched = false;
    private static String sttyCommand = System.getProperty("jline.sttyCommand", "stty");


    String encoding = System.getProperty("input.encoding", "UTF-8");
    //ReplayPrefixOneCharInputStream replayStream = new ReplayPrefixOneCharInputStream(encoding);
    //InputStreamReader replayReader;

    @Override
    public void init() {
        // save the initial tty configuration
        try {
            ttyConfig = stty("-g");

            // sanity check
            if ((ttyConfig.length() == 0)
                    || ((ttyConfig.indexOf("=") == -1)
                    && (ttyConfig.indexOf(":") == -1))) {
                throw new RuntimeException("Unrecognized stty code: " + ttyConfig);
            }

            //checkBackspace();

            // set the console to be character-buffered instead of line-buffered
            stty("-icanon min 1");

            // disable character echoing
            stty("-echo");
            echoEnabled = false;
        }
        catch (IOException ioe) {
            System.err.println("TTY failed with: " + ioe.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // at exit, restore the original tty configuration (for JDK 1.3+)
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void start() {
                try {
                    restoreTerminal();
                } catch (Exception e) {
                    e.printStackTrace();
                    //ignored
                }
            }
        });

    }

    public int read(InputStream in) throws IOException {
        int c = 0;

        c = in.read();

        /*
        if (isBackspaceDeleteSwitched()) {
            //TODO: this is just a temp fix since it will still fail for
            // c-l, c-space

                if (c == DELETE)
                    c = BACKSPACE;
                else if (c == BACKSPACE)
                    c = DELETE;

        }
        */

        // handle unicode characters, thanks for a patch from amyi@inf.ed.ac.uk
        if (c > 128) {
          // handle unicode characters longer than 2 bytes,
          // thanks to Marc.Herbert@continuent.com
            //replayStream.setInput(c, in);
//            replayReader = new InputStreamReader(replayStream, encoding);
            //c = replayReader.read();

        }

        return c;
    }

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

    @Override
    public boolean isEchoEnabled() {
        return echoEnabled;
    }

    private void restoreTerminal() throws Exception {
        if (ttyConfig != null) {
            stty(ttyConfig);
            ttyConfig = null;
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
        // speed 38400 baud; rows = 49; columns = 111; ypixels = 0; xpixels = 0;
        for (StringTokenizer tok = new StringTokenizer(ttyProps, ";\n");
             tok.hasMoreTokens();) {
            String str = tok.nextToken().trim();

            if (str.startsWith(prop)) {
                int index = str.lastIndexOf(" ");

                return Integer.parseInt(str.substring(index).trim());
            } else if (str.endsWith(prop)) {
                int index = str.indexOf(" ");

                return Integer.parseInt(str.substring(0, index).trim());
            }
        }

        return -1;
    }

       /**
     *  Execute the stty command with the specified arguments
     *  against the current active terminal.
     */
    protected static String stty(final String args)
                        throws IOException, InterruptedException {
        return exec("stty " + args + " < /dev/tty").trim();
    }

    /**
     *  Execute the specified command and return the output
     *  (both stdout and stderr).
     */
    private static String exec(final String cmd)
                        throws IOException, InterruptedException {
        return exec(new String[] {
                        "sh",
                        "-c",
                        cmd
                    });
    }

    /**
     *  Execute the specified command and return the output
     *  (both stdout and stderr).
     */
    private static String exec(final String[] cmd)
                        throws IOException, InterruptedException {
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
	    } finally {
		    try {in.close();} catch (Exception e) {}
		    try {err.close();} catch (Exception e) {}
		    try {out.close();} catch (Exception e) {}
	    }

        String result = new String(bout.toByteArray());

        return result;
    }


}
