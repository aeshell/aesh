/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.paste;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.OutputStream;

import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.Prompt;
import org.junit.Test;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConsolePasteTest extends BaseConsoleTest {

    @Test
    public void paste() throws Exception {
        invokeTestConsole(4, new Setup() {
            @Override
            public void call(Console console, OutputStream out) throws IOException {
                String pasteLine1 =
                        "connect" + Config.getLineSeparator() +
                        "admin" + Config.getLineSeparator() +
                        "admin!";
                String pasteLine2 = "234"+ Config.getLineSeparator() + "exit"+ Config.getLineSeparator();
                out.write(pasteLine1.getBytes());
                out.write(pasteLine2.getBytes());
            }
        }, new Verify() {
           boolean password = false;
           @Override
           public int call(Console console, ConsoleOperation op) {
               if (op.getBuffer().equals("admin")) {
                   console.setPrompt(new Prompt("", new Character('\u0000')));
                   password = true;
                   return 0;
               }
               if(password) {
                   assertEquals("admin!234", op.getBuffer());
                   password = false;
               }
               return 0;
           }
        });
    }
}
