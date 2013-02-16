/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.masking;

import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOutput;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.edit.KeyOperation;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConsoleMaskingTest extends BaseConsoleTest {

    public ConsoleMaskingTest(String test) {
        super(test);
    }

    public void testMasking() throws IOException {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        KeyOperation deletePrevChar =  new KeyOperation(Key.CTRL_H, Operation.DELETE_PREV_CHAR);

        Console console = getTestConsole(pipedInputStream);
        outputStream.write(("mypassword").getBytes());
        outputStream.write(deletePrevChar.getFirstValue());
        outputStream.write(("\n").getBytes());
        ConsoleOutput output = console.read(new Prompt(""), new Character('\u0000'));
        assertEquals("mypasswor", output.getBuffer());

    }

}
