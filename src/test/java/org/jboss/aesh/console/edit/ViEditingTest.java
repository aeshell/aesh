/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.edit;

import org.jboss.aesh.console.AeshConsoleCallback;
import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.Mode;
import org.junit.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ViEditingTest extends BaseConsoleTest {

    @Test
    public void testVi() throws IOException, InterruptedException {
        SettingsBuilder builder = new SettingsBuilder();
        builder.mode(Mode.VI);

        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        Console console = getTestConsole(builder, pipedInputStream);

        console.setConsoleCallback(new ViConsoleCallback(console));

        console.start();
        outputStream.write("34".getBytes());
        //esc
        outputStream.write(new byte[]{27});
        //ctrl-e (should switch to emacs mode)
        outputStream.write(new byte[]{5});
        //ctrl-a
        outputStream.write(new byte[]{1});
        outputStream.write(("12"+Config.getLineSeparator()).getBytes());
        outputStream.flush();

        Thread.sleep(100);
        console.stop();
    }

    class ViConsoleCallback extends AeshConsoleCallback {

        Console console;

        ViConsoleCallback(Console console) {
            this.console = console;
        }

        @Override
        public int execute(ConsoleOperation output) {
            assertEquals("1234", output.getBuffer());
            return 0;
        }
    }
}
