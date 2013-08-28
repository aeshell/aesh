/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.edit;

import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
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
public class EmacsEditingTest extends BaseConsoleTest {

    @Test
    public void testEmacs() throws IOException, InterruptedException {
        if(Config.isOSPOSIXCompatible()) {
            PipedOutputStream outputStream = new PipedOutputStream();
            PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
            Console console = getTestConsole(pipedInputStream);

            console.setConsoleCallback(new ConsoleCallback() {
                @Override
                public int readConsoleOutput(ConsoleOperation output) throws IOException {
                    assertEquals("1234", output.getBuffer());
                    return 0;
                }
            });

            console.start();
            outputStream.write("34".getBytes());
            //home
            outputStream.write(new byte[]{27,79,72});
            outputStream.write("12\n".getBytes());

            Thread.sleep(100);
            console.stop();

        }
    }
}
