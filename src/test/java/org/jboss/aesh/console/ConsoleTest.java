/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.junit.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConsoleTest extends BaseConsoleTest {


    @Test
    public void multiLine() throws IOException, InterruptedException {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        Console console = getTestConsole(pipedInputStream);
        console.setConsoleCallback(new ConsoleCallback() {
            @Override
            public int readConsoleOutput(ConsoleOutput output) throws IOException {
                assertEquals("ls foo bar", output.getBuffer());
                return 0;
            }
        });
        console.start();

        outputStream.write(("ls \\").getBytes());
        outputStream.write(("\n").getBytes());
        outputStream.write(("foo \\").getBytes());
        outputStream.write(("\n").getBytes());
        outputStream.write(("bar\n").getBytes());
        outputStream.flush();

        Thread.sleep(100);
        console.stop();
    }


}
