/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.history;

import org.jboss.aesh.TestBuffer;
import org.jboss.aesh.console.AeshConsoleCallback;
import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.junit.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class HistoryTest extends BaseConsoleTest {

    @Test
    public void testHistory() throws IOException, InterruptedException {

        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        Console console = getTestConsole(pipedInputStream);
        console.setConsoleCallback(new AeshConsoleCallback() {
            private int count = 0;
            @Override
            public int execute(ConsoleOperation output) {
                if(count == 0)
                    assertEquals("1234", output.getBuffer());
                else if(count == 1)
                    assertEquals("567", output.getBuffer());
                else if(count == 2)
                    assertEquals("1234", output.getBuffer());
                else if(count == 3)
                    assertEquals("567", output.getBuffer());

                count++;
                return 0;
            }
        });
        console.start();

        outputStream.write(("1234"+ Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(20);
        outputStream.write(("567"+Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(20);
        outputStream.write(TestBuffer.EMACS_HISTORY_PREV);
        outputStream.write(TestBuffer.EMACS_HISTORY_PREV);
        outputStream.write("\n".getBytes());
        outputStream.flush();
        Thread.sleep(20);
        outputStream.write(TestBuffer.EMACS_HISTORY_PREV);
        outputStream.write(TestBuffer.EMACS_HISTORY_PREV);
        outputStream.write("\n".getBytes());


        Thread.sleep(100);
        console.stop();
    }

    @Test
    public void testHistorySize() {
        History history = new InMemoryHistory(20);

        for(int i=0; i < 25; i++)
            history.push(String.valueOf(i));


        assertEquals(20, history.size());
        assertEquals("24", history.getPreviousFetch());
    }

    @Test
    public void testClear() {
        History history = new InMemoryHistory(10);
        history.push("1");
        history.push("2");

        assertEquals("2", history.getPreviousFetch());
        history.clear();
        assertEquals(null, history.getPreviousFetch());
    }

    @Test
    public void testDupes() {
        History history = new InMemoryHistory(10);
        history.push("1");
        history.push("2");
        history.push("3");
        history.push("1");
        assertEquals("1", history.getPreviousFetch());
        assertEquals("3", history.getPreviousFetch());
        assertEquals("2", history.getPreviousFetch());
        assertEquals("2", history.getPreviousFetch());
    }
}
