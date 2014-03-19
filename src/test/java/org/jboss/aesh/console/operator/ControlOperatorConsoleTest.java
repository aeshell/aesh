/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.operator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.TestConsoleCallback;
import org.junit.Test;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ControlOperatorConsoleTest extends BaseConsoleTest {

    @Test
    public void controlOperatorTest() throws Throwable {

        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        CountDownLatch latch = new CountDownLatch(1);
        List<Throwable> exceptions = new ArrayList<Throwable>();

        Console console = getTestConsole(pipedInputStream);
        console.setConsoleCallback(new TestConsoleCallback(latch, exceptions) {
            int counter = 0;
            @Override
            public int verify(ConsoleOperation output) {
                if(counter == 0) {
                    assertEquals("ls -la *", output.getBuffer());
                    counter++;
                }
                else if(counter == 1)
                    assertEquals(" foo", output.getBuffer());

                return 0;
            }
        });

        console.start();

        outputStream.write(("ls -la *; foo" + Config.getLineSeparator()).getBytes());


        if(!latch.await(200, TimeUnit.MILLISECONDS)) {
           fail("Failed waiting for Console to finish");
        }
        console.stop();
        if(exceptions.size() > 0) {
           throw exceptions.get(0);
        }
    }
}
