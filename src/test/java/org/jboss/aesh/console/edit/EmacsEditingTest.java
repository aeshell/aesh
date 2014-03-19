/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.edit;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.OutputStream;

import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.edit.EmacsEditMode;
import org.jboss.aesh.edit.KeyOperationFactory;
import org.jboss.aesh.edit.KeyOperationManager;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;
import org.junit.Assume;
import org.junit.Test;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class EmacsEditingTest extends BaseConsoleTest {

    @Test
    public void testEmacs() throws Exception {
        Assume.assumeTrue(Config.isOSPOSIXCompatible());

        invokeTestConsole(new Setup() {
            @Override
            public void call(Console console, OutputStream out) throws IOException {
                out.write("34".getBytes());
                //home
                out.write(new byte[]{1});
                out.write(("12"+Config.getLineSeparator()).getBytes());
            }
        }, new Verify() {
           @Override
           public int call(Console console, ConsoleOperation op) {
               assertEquals("1234", op.getBuffer());
               return 0;
           }
        });
    }

    @Test
    public void testOperationParser() {
        Assume.assumeTrue(Config.isOSPOSIXCompatible());

        KeyOperationManager keyOperationManager = new KeyOperationManager();
        keyOperationManager.addOperations(KeyOperationFactory.generateEmacsMode());

        EmacsEditMode editMode = new EmacsEditMode(keyOperationManager);

        Operation operation = editMode.parseInput(Key.ESC, "12345");

        assertEquals(Operation.NO_ACTION, operation);
    }
}
