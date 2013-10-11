/**
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.aesh.cl.completer;

import java.io.File;
import java.io.IOException;

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

    @Test
    public void testCompleterBaseDir() {
        File file = new File(System.getProperty("user.dir"));
        FileOptionCompleter completer = new FileOptionCompleter();
        assertEquals(file, completer.getWorkingDirectory());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompleterIllegalBaseDir() throws IOException {
        File file = File.createTempFile("tmp", ".tmp");
        file.deleteOnExit();
        new FileOptionCompleter(file);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompleterIllegalFilter() throws IOException {
        File file = File.createTempFile("tmp", ".tmp");
        file.deleteOnExit();
        new FileOptionCompleter(file, null);
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
        FileOptionCompleter completer = new FileOptionCompleter(file);
        CompleterData data = new CompleterData("", null);
        completer.complete(data);
        assertNotNull(data.getCompleterValues());
        assertEquals(1, data.getCompleterValues().size());
        assertEquals(child.getName(), data.getCompleterValues().get(0));
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
        FileOptionCompleter completer = new FileOptionCompleter(file);
        CompleterData data = new CompleterData("", null);
        completer.complete(data);
        assertNotNull(data.getCompleterValues());
        assertEquals(2, data.getCompleterValues().size());
        assertTrue(data.getCompleterValues().contains(child.getName()));
        assertTrue(data.getCompleterValues().contains(child.getName()));
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
        FileOptionCompleter completer = new FileOptionCompleter(file);

        CompleterData data = new CompleterData("", null);
        completer.complete(data);
        assertNotNull(data.getCompleterValues());
        assertEquals(2, data.getCompleterValues().size());
        assertTrue(data.getCompleterValues().contains(child.getName() + "/"));
        assertTrue(data.getCompleterValues().contains(child2.getName() + "/"));

        data = new CompleterData("ch", null);
        completer.complete(data);
        assertNotNull(data.getCompleterValues());
        assertEquals(1, data.getCompleterValues().size());
        assertEquals(child.getName(), data.getCompleterValues().get(0));

        data = new CompleterData("child", null);
        completer.complete(data);
        assertNotNull(data.getCompleterValues());
        assertEquals(2, data.getCompleterValues().size());
        assertTrue(data.getCompleterValues().contains(child.getName() + "/"));
        assertTrue(data.getCompleterValues().contains(child2.getName() + "/"));
    }

}
