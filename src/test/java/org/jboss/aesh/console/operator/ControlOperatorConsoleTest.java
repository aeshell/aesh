/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.operator;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.OutputStream;

import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.junit.Test;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ControlOperatorConsoleTest extends BaseConsoleTest {

    @Test
    public void controlOperatorTest() throws Throwable {
        invokeTestConsole(2, new Setup() {
            @Override
            public void call(Console console, OutputStream out) throws IOException {
                out.write(("ls -la *; foo" + Config.getLineSeparator()).getBytes());
            }
        }, new Verify() {
            int counter = 0;
            @Override
            public int call(Console console, ConsoleOperation op) {
                if(counter == 0) {
                    assertEquals("ls -la *", op.getBuffer());
                    counter++;
                }
                else if(counter == 1)
                    assertEquals(" foo", op.getBuffer());

                return 0;
            }
        });
    }
}
