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
import org.jboss.aesh.edit.EmacsEditMode;
import org.jboss.aesh.edit.KeyOperationFactory;
import org.jboss.aesh.edit.KeyOperationManager;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;
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

            console.setConsoleCallback(new AeshConsoleCallback() {
                @Override
                public int execute(ConsoleOperation output) {
                    assertEquals("1234", output.getBuffer());
                    return 0;
                }
            });

            console.start();
            outputStream.write("34".getBytes());
            //home
            outputStream.write(new byte[]{1});
            outputStream.write(("12"+Config.getLineSeparator()).getBytes());

            Thread.sleep(100);
            console.stop();

        }
    }

    @Test
    public void testOperationParser() {
        if(Config.isOSPOSIXCompatible()) {

            KeyOperationManager keyOperationManager = new KeyOperationManager();
            keyOperationManager.addOperations(KeyOperationFactory.generateEmacsMode());

            EmacsEditMode editMode = new EmacsEditMode(keyOperationManager);

            Operation operation = editMode.parseInput(Key.ESC, "12345");

            assertEquals(Operation.NO_ACTION, operation);

        }
    }
}
