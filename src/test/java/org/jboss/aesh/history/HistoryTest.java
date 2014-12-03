/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.history;

import org.jboss.aesh.AeshTestCase;
import org.jboss.aesh.TestBuffer;
import org.jboss.aesh.console.settings.Settings;

import java.io.File;
import java.io.IOException;

import org.jboss.aesh.console.settings.FileAccessPermission;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class HistoryTest extends AeshTestCase {

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

    public void testFileHistoryPermission() throws IOException{
        File historyFile = new File("aesh-history-file.test.1");
        historyFile.deleteOnExit();
        int maxSize = 10;
        FileAccessPermission perm = new FileAccessPermission();
        perm.setExecutable(false);
        perm.setExecutableOwnerOnly(false);
        perm.setReadable(true);
        perm.setReadableOwnerOnly(true);
        perm.setWritable(true);
        perm.setWritableOwnerOnly(true);
        FileHistory history = new FileHistory("aesh-history-file.test.1", maxSize, perm);
        history.push("1");
        history.stop(); // it will write history to local file
        assertTrue(historyFile.canRead());
        assertFalse(historyFile.canExecute());
        assertTrue(historyFile.canWrite());

        historyFile = new File("aesh-history-file.test.2");
        historyFile.deleteOnExit();
        perm = new FileAccessPermission();
        perm.setExecutable(true);
        perm.setExecutableOwnerOnly(true);
        perm.setReadable(false);
        perm.setReadableOwnerOnly(true);
        perm.setWritable(true);
        perm.setWritableOwnerOnly(true);
        history = new FileHistory("aesh-history-file.test.2", maxSize, perm);
        history.push("1");
        history.stop(); // it will write history to local file
        assertFalse(historyFile.canRead());
        assertTrue(historyFile.canExecute());
        assertTrue(historyFile.canWrite());

        historyFile = new File("aesh-history-file.test.3");
        historyFile.deleteOnExit();
        perm = new FileAccessPermission();
        perm.setExecutable(false);
        perm.setExecutableOwnerOnly(true);
        perm.setReadable(false);
        perm.setReadableOwnerOnly(true);
        perm.setWritable(false);
        perm.setWritableOwnerOnly(true);
        history = new FileHistory("aesh-history-file.test.3", maxSize, perm);
        history.push("1");
        history.stop(); // it will write history to local file
        assertFalse(historyFile.canRead());
        assertFalse(historyFile.canExecute());
        assertFalse(historyFile.canWrite());
    }
}
