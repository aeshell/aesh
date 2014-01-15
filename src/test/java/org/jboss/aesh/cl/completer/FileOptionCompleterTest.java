/**
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.aesh.cl.completer;

import java.io.File;
import java.io.IOException;

import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.terminal.TerminalString;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TestCase for {@link FileOptionCompleter}
 *
 * @author <a href="ggastald@redhat.com">George Gastaldi</a>
 */
public class FileOptionCompleterTest {

    private AeshContext aeshContext = new AeshContext() {
        private File cwd;
        @Override
        public File getCurrentWorkingDirectory() {
            if(cwd == null)
                cwd = new File(Config.getUserDir());
            return cwd;
        }
        @Override
        public void setCurrentWorkingDirectory(File cwd) {
            if(!cwd.isDirectory())
                throw new IllegalArgumentException("Current working directory must be a directory");
            this.cwd = cwd;
        }
    };

    @Test(expected = IllegalArgumentException.class)
    public void testCompleterIllegalBaseDir() throws IOException {
        File file = File.createTempFile("tmp", ".tmp");
        file.deleteOnExit();
        aeshContext.setCurrentWorkingDirectory(file);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompleterIllegalFilter() throws IOException {
        File file = File.createTempFile("tmp", ".tmp");
        file.deleteOnExit();
        new FileOptionCompleter(null);
    }

    @Test
    public void testCompleterSingleFile() throws IOException {
        File file = File.createTempFile("tmp", ".tmp");
        file.delete();
        file.mkdir();
        file.deleteOnExit();
        File child = new File(file, "child.txt");
        child.createNewFile();
        child.deleteOnExit();
        aeshContext.setCurrentWorkingDirectory(file);
        FileOptionCompleter completer = new FileOptionCompleter();
        CompleterData data = new CompleterData(aeshContext, "", null);
        completer.complete(data);
        assertNotNull(data.getCompleterValues());
        assertEquals(1, data.getCompleterValues().size());
        assertEquals(child.getName(), data.getCompleterValues().get(0).getCharacters());
    }

    @Test
    public void testCompleterMultipleFile() throws IOException {
        File file = File.createTempFile("tmp", ".tmp");
        file.delete();
        file.mkdir();
        file.deleteOnExit();
        File child = new File(file, "child.txt");
        child.createNewFile();
        child.deleteOnExit();
        File child2 = new File(file, "child2.txt");
        child2.createNewFile();
        child2.deleteOnExit();
        aeshContext.setCurrentWorkingDirectory(file);
        FileOptionCompleter completer = new FileOptionCompleter();
        CompleterData data = new CompleterData(aeshContext, "", null);
        completer.complete(data);
        assertNotNull(data.getCompleterValues());
        assertEquals(2, data.getCompleterValues().size());
        assertTrue(data.getCompleterValues().contains(new TerminalString(child.getName(), true)));
        assertTrue(data.getCompleterValues().contains(new TerminalString(child.getName(), true)));
    }

    @Test
    public void testCompleterMultipleDirectory() throws IOException {
        File file = File.createTempFile("tmp", ".tmp");
        file.delete();
        file.mkdir();
        file.deleteOnExit();
        File child = new File(file, "child");
        child.mkdir();
        child.deleteOnExit();
        File child2 = new File(file, "child2");
        child2.mkdir();
        child2.deleteOnExit();
        FileOptionCompleter completer = new FileOptionCompleter();
        aeshContext.setCurrentWorkingDirectory(file);

        CompleterData data = new CompleterData(aeshContext, "", null);
        completer.complete(data);
        assertNotNull(data.getCompleterValues());
        assertEquals(2, data.getCompleterValues().size());
        assertTrue(data.getCompleterValues().contains(new TerminalString(child.getName() + Config.getPathSeparator(), true)));
        assertTrue(data.getCompleterValues().contains(new TerminalString(child2.getName() + Config.getPathSeparator(), true)));

        data = new CompleterData(aeshContext, "ch", null);
        completer.complete(data);
        assertNotNull(data.getCompleterValues());
        assertEquals(1, data.getCompleterValues().size());
        assertEquals(child.getName(), data.getCompleterValues().get(0).getCharacters());

        data = new CompleterData(aeshContext, "child", null);
        completer.complete(data);
        assertNotNull(data.getCompleterValues());
        assertEquals(2, data.getCompleterValues().size());
        assertTrue(data.getCompleterValues().contains(new TerminalString(child.getName() + Config.getPathSeparator(), true)));
        assertTrue(data.getCompleterValues().contains(new TerminalString(child2.getName() + Config.getPathSeparator(), true)));
    }

}
