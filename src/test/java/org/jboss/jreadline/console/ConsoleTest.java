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
package org.jboss.jreadline.console;

import org.jboss.jreadline.JReadlineTestCase;
import org.jboss.jreadline.TestBuffer;
import org.jboss.jreadline.console.redirection.Redirection;
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
        outputStream.write("ls | find\n".getBytes());
        ConsoleOutput output = console.read(null);
        assertEquals("ls ", output.getBuffer());
        output = console.read(null);
        assertEquals(" find", output.getBuffer());

        if(Config.isOSPOSIXCompatible()) {
            outputStream.write("ls >/tmp/foo\\ bar.txt\n".getBytes());
            output = console.read(null);
            assertEquals("ls ", output.getBuffer());
            console.pushToStdOut("CONTENT OF FILE");
            outputStream.write("\n".getBytes());
            output = console.read(null);
            assertEquals("", output.getBuffer());
            assertEquals("CONTENT OF FILE\n", getContentOfFile("/tmp/foo bar.txt"));
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
            assertEquals(Redirection.PIPE, output.getRedirection());
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
