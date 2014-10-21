/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aesh.history;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.OutputStream;

import org.jboss.aesh.TestBuffer;
import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.junit.Test;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class HistoryTest extends BaseConsoleTest {

    @Test
    public void testHistory() throws Exception {

        invokeTestConsole(4, new Setup() {
            @Override
            public void call(Console console, OutputStream out) throws IOException {
                out.write(("1234"+ Config.getLineSeparator()).getBytes());
                out.flush();

                out.write(("567"+Config.getLineSeparator()).getBytes());
                out.flush();

                out.write(TestBuffer.EMACS_HISTORY_PREV);
                out.write(TestBuffer.EMACS_HISTORY_PREV);
                out.write(Config.getLineSeparator().getBytes());
                out.flush();

                out.write(TestBuffer.EMACS_HISTORY_PREV);
                out.write(TestBuffer.EMACS_HISTORY_PREV);
                out.write(Config.getLineSeparator().getBytes());
            }
        }, new Verify() {
           private int count = 0;
           @Override
           public int call(Console console, ConsoleOperation op) {
               if(count == 0)
                   assertEquals("1234", op.getBuffer());
               else if(count == 1)
                   assertEquals("567", op.getBuffer());
               else if(count == 2)
                   assertEquals("1234", op.getBuffer());
               else if(count == 3)
                   assertEquals("567", op.getBuffer());

               count++;
               return 0;
           }
        });
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
