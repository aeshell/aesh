/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.redirect;

import org.jboss.aesh.TestBuffer;
import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOutput;
import org.jboss.aesh.console.operator.ControlOperator;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConsoleRedirection extends BaseConsoleTest {

    public ConsoleRedirection(String test) {
        super(test);
    }

    public void testSimpleRedirectionCommands() throws IOException {
        TestBuffer buffer = new TestBuffer("ls . | find\n");
        //assertEquals("ls .  find", buffer);
    }

    public void testRedirectionCommands() throws IOException {

        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        Console console = getTestConsole(pipedInputStream);
        outputStream.write("ls | find *. -print\n".getBytes());
        ConsoleOutput output = console.read("");
        assertEquals("ls ", output.getBuffer());
        output = console.read("");
        assertEquals(" find *. -print", output.getBuffer());

        if(Config.isOSPOSIXCompatible()) {
            outputStream.write(("ls >"+Config.getTmpDir()+"/foo\\ bar.txt\n").getBytes());
        }
        else {
            outputStream.write(("ls >"+Config.getTmpDir()+"\\foo\\ bar.txt\n").getBytes());
        }
        output = console.read("");
        assertEquals("ls ", output.getBuffer());
        console.pushToStdOut("CONTENT OF FILE");
        outputStream.write("\n".getBytes());
        output = console.read("");
        assertEquals("", output.getBuffer());
        if(Config.isOSPOSIXCompatible())
            assertEquals("CONTENT OF FILE\n", getContentOfFile(Config.getTmpDir()+"/foo bar.txt"));
        else
            assertEquals("CONTENT OF FILE\n", getContentOfFile(Config.getTmpDir()+"\\foo bar.txt"));

        console.stop();
    }

    public void testRedirectIn() throws IOException {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        Console console = getTestConsole(pipedInputStream);
        if(Config.isOSPOSIXCompatible())
            outputStream.write(("ls < "+Config.getTmpDir()+"/foo\\ bar.txt | man\n").getBytes());
        else
            outputStream.write(("ls < "+Config.getTmpDir()+"\\foo\\ bar.txt | man\n").getBytes());

        ConsoleOutput output = console.read("");
        assertEquals("ls ", output.getBuffer());
        assertTrue(output.getStdOut().contains("CONTENT OF FILE"));
        assertEquals(ControlOperator.PIPE, output.getControlOperator());
        output = console.read("");
        assertEquals(" man", output.getBuffer());
        assertEquals(ControlOperator.NONE, output.getControlOperator());

        console.stop();
    }

}
