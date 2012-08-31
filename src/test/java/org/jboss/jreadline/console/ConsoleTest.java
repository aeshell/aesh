/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.console;

import org.jboss.jreadline.JReadlineTestCase;
import org.jboss.jreadline.TestBuffer;
import org.jboss.jreadline.console.operator.ControlOperator;
import org.jboss.jreadline.console.settings.Settings;
import org.jboss.jreadline.edit.Mode;
import org.jboss.jreadline.terminal.TestTerminal;

import java.io.*;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConsoleTest extends JReadlineTestCase {

    public ConsoleTest(String test) {
        super(test);
    }

    public void testSimpleRedirectionCommands() throws IOException {
        TestBuffer buffer = new TestBuffer("ls . | find\n");
        assertEquals("ls .  find", buffer);

        /* not needed for now
        buffer = new TestBuffer("ls > find\n");
        assertEquals("ls ", buffer);

        buffer = new TestBuffer("ls 2>> find\n\n");
        assertEquals("ls ", buffer);
        */

    }

    public void testRedirectionCommands() throws IOException {

        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        Console console = getTestConsole(pipedInputStream);
        outputStream.write("ls | find *. -print\n".getBytes());
        ConsoleOutput output = console.read(null);
        assertEquals("ls ", output.getBuffer());
        output = console.read(null);
        assertEquals(" find *. -print", output.getBuffer());

        if(Config.isOSPOSIXCompatible()) {
            outputStream.write("ls >/tmp/foo\\ bar.txt\n".getBytes());
            output = console.read(null);
            assertEquals("ls ", output.getBuffer());
            console.pushToStdOut("CONTENT OF FILE");
            outputStream.write("\n".getBytes());
            output = console.read(null);
            assertEquals("", output.getBuffer());
            assertEquals("CONTENT OF FILE\n", getContentOfFile("/tmp/foo bar.txt"));

            console.stop();
        }
    }

    public void testRedirectIn() throws IOException {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        if(Config.isOSPOSIXCompatible()) {
            Console console = getTestConsole(pipedInputStream);
            outputStream.write("ls < /tmp/foo\\ bar.txt | man\n".getBytes());
            ConsoleOutput output = console.read(null);
            assertEquals("ls ", output.getBuffer());
            assertEquals("CONTENT OF FILE\n", output.getStdOut());
            assertEquals(ControlOperator.PIPE, output.getControlOperator());
            output = console.read(null);
            assertEquals(" man", output.getBuffer());
            assertEquals(ControlOperator.NONE, output.getControlOperator());

            console.stop();
        }
    }


    private Console getTestConsole(InputStream is) throws IOException {
        Settings settings = Settings.getInstance();
        settings.setReadInputrc(false);
        settings.setTerminal(new TestTerminal());
        settings.setInputStream(is);
        settings.setStdOut(new ByteArrayOutputStream());
        settings.setEditMode(Mode.EMACS);
        settings.resetEditMode();
        settings.setReadAhead(false);
        if(!Config.isOSPOSIXCompatible())
            settings.setAnsiConsole(false);
        Console console = new Console(settings);

        return console;
    }

    private String getContentOfFile(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();

        }
        finally {
            br.close();
        }
    }
}
