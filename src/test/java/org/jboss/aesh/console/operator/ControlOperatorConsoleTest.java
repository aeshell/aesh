/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.operator;

import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleCallback;
import org.jboss.aesh.console.ConsoleOperation;
import org.junit.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ControlOperatorConsoleTest extends BaseConsoleTest {

    @Test
    public void controlOperatorTest() throws IOException, InterruptedException {

        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        Console console = getTestConsole(pipedInputStream);
        console.setConsoleCallback(new ConsoleCallback() {
            int counter = 0;
            @Override
            public int readConsoleOutput(ConsoleOperation output) throws IOException {
                if(counter == 0) {
                    assertEquals("ls -la *", output.getBuffer());
                    counter++;
                }
                else if(counter == 1)
                    assertEquals(" foo", output.getBuffer());

                return 0;
            }
        });

        console.start();

        outputStream.write("ls -la *; foo\n".getBytes());

        Thread.sleep(200);
    }
}
