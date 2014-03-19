/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.masking;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.OutputStream;

import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.edit.KeyOperation;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;
import org.junit.Test;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConsoleMaskingTest extends BaseConsoleTest {

    @Test
    public void masking() throws Exception {
        invokeTestConsole(new Setup() {
            @Override
            public void call(Console console, OutputStream out) throws IOException {
                KeyOperation deletePrevChar =  new KeyOperation(Key.CTRL_H, Operation.DELETE_PREV_CHAR);
                console.setPrompt(new Prompt("", '\u0000'));

                out.write(("mypassword").getBytes());
                out.write(deletePrevChar.getFirstValue());
                out.write((Config.getLineSeparator()).getBytes());
                out.flush();
            }
        }, new Verify() {
           @Override
           public int call(Console console, ConsoleOperation op) {
               assertEquals("mypasswor", op.getBuffer());
               return 0;
           }
        });
    }
}
