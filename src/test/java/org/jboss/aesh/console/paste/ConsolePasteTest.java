/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.paste;

import org.jboss.aesh.console.AeshConsoleCallback;
import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.Prompt;
import org.junit.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConsolePasteTest extends BaseConsoleTest{

    @Test
    public void paste() throws IOException, InterruptedException {
        final PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);


        String pasteLine1 =
                "connect\n" +
                "admin\n" +
                "admin!";
        String pasteLine2 = "234\n" + "exit\n";


        final Console console = getTestConsole(pipedInputStream);
        console.setConsoleCallback(new AeshConsoleCallback() {
            boolean password = false;
            @Override
            public int execute(ConsoleOperation output) {
                if (output.getBuffer().equals("admin")) {
                    console.setPrompt(new Prompt("", new Character('\u0000')));
                    password = true;
                    return 0;
                }
                if(password) {
                    assertEquals("admin!234", output.getBuffer());
                    password = false;
                }
                return 0;
            }
        });
        console.start();
        outputStream.write(pasteLine1.getBytes());
        outputStream.write(pasteLine2.getBytes());

        Thread.sleep(500);

        console.stop();


    }
}
