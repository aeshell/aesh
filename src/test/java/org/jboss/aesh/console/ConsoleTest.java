/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConsoleTest extends BaseConsoleTest {


    @Test
    public void multiLine() throws Throwable {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        CountDownLatch latch = new CountDownLatch(1);
        List<Throwable> exceptions = new ArrayList<Throwable>();

        Console console = getTestConsole(pipedInputStream);
        console.setConsoleCallback(new TestConsoleCallback(latch, exceptions) {
            @Override
            public int verify(ConsoleOperation output) {
                assertEquals("ls foo bar", output.getBuffer());
                return 0;
            }
        });
        console.start();

        outputStream.write(("ls \\").getBytes());
        outputStream.write((Config.getLineSeparator()).getBytes());
        outputStream.write(("foo \\").getBytes());
        outputStream.write((Config.getLineSeparator()).getBytes());
        outputStream.write(("bar"+Config.getLineSeparator()).getBytes());
        outputStream.flush();

        if(!latch.await(200, TimeUnit.MILLISECONDS)) {
           fail("Failed waiting for Console to finish");
        }
        console.stop();
        if(exceptions.size() > 0) {
           throw exceptions.get(0);
        }
    }


    @Test
    public void testPrintWriter() throws Throwable {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        CountDownLatch latch = new CountDownLatch(1);
        List<Throwable> exceptions = new ArrayList<Throwable>();

        Console console = getTestConsole(pipedInputStream);
        console.setConsoleCallback(new TestConsoleCallback(latch, exceptions) {
            @Override
            public int verify(ConsoleOperation output) {
                assertEquals("ls foo bar", output.getBuffer());
                return 0;
            }
        });
        console.start();

        PrintStream out = console.getShell().out();

        out.print("ls \\");
        out.print(Config.getLineSeparator());
        out.print("foo \\");
        out.print(Config.getLineSeparator());
        out.println("bar");
        outputStream.flush();

        if(!latch.await(200, TimeUnit.MILLISECONDS)) {
           fail("Failed waiting for Console to finish");
        }
        console.stop();
        if(exceptions.size() > 0) {
           throw exceptions.get(0);
        }
    }

}
