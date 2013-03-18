/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.alias;

import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleCallback;
import org.jboss.aesh.console.ConsoleOutput;
import org.jboss.aesh.console.settings.Settings;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConsoleAliasTest extends BaseConsoleTest {


    @Test
    public void alias() throws IOException, InterruptedException {
        Settings.getInstance().setPersistAlias(false);
        Settings.getInstance().setAliasFile(Config.isOSPOSIXCompatible() ?
                new File("src/test/resources/alias1") : new File("src\\test\\resources\\alias1"));
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        Console console = getTestConsole(pipedInputStream);
        console.setConsoleCallback(new AliasConsoleCallback(console));
        console.start();

        outputStream.write("ll\n".getBytes());
        outputStream.write("grep -l\n".getBytes());

        Thread.sleep(100);
    }

    class AliasConsoleCallback implements ConsoleCallback {

        private int count = 0;
        Console console;

        AliasConsoleCallback(Console console) {
            this.console = console;
        }

        @Override
        public int readConsoleOutput(ConsoleOutput output) throws IOException {
            if(count == 0)
                assertEquals("ls -alF", output.getBuffer());
            else if(count == 1) {
                assertEquals("grep --color=auto -l", output.getBuffer());
                console.stop();
            }
            count++;
            return 0;
        }
    }

}
