/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.history;

import org.jboss.jreadline.JReadlineTestCase;
import org.jboss.jreadline.TestBuffer;
import org.jboss.jreadline.console.settings.Settings;

import java.io.IOException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class HistoryTest extends JReadlineTestCase {

    public HistoryTest(String test) {
        super(test);
    }

    public void testHistory() throws IOException {
        TestBuffer buffer = new TestBuffer();

        buffer.append("1234")
                .append(TestBuffer.getNewLine()).append("567")
                .append(TestBuffer.getNewLine()).append(TestBuffer.EMACS_HISTORY_PREV)
                .append(TestBuffer.getNewLine());

        assertEquals("567", buffer, true);


        buffer = new TestBuffer();
        buffer.append("1234")
                .append(TestBuffer.getNewLine()).append("567")
                .append(TestBuffer.getNewLine()).append("89")
                .append(TestBuffer.getNewLine())
                .append(TestBuffer.EMACS_HISTORY_PREV)
                .append(TestBuffer.EMACS_HISTORY_PREV)
                .append(TestBuffer.EMACS_HISTORY_PREV).append(TestBuffer.getNewLine());

        assertEquals("1234", buffer, true);

        Settings.getInstance().setHistoryDisabled(true);

        buffer = new TestBuffer();
        buffer.append("1234")
                .append(TestBuffer.getNewLine()).append("567")
                .append(TestBuffer.getNewLine())
                .append(TestBuffer.EMACS_HISTORY_PREV).append(TestBuffer.getNewLine());

        assertEquals("", buffer,true);

        Settings.getInstance().resetToDefaults();

    }

    public void testHistorySize() {
        History history = new InMemoryHistory(20);

        for(int i=0; i < 25; i++)
            history.push(String.valueOf(i));


        assertEquals(20, history.size());
        assertEquals("24", history.getPreviousFetch());
    }

    public void testClear() {
        History history = new InMemoryHistory(10);
        history.push("1");
        history.push("2");

        assertEquals("2", history.getPreviousFetch());
        history.clear();
        assertEquals(null, history.getPreviousFetch());
    }

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
