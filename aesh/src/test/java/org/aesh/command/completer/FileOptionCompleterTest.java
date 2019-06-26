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

package org.aesh.command.completer;

import java.io.File;
import java.io.IOException;

import org.aesh.command.impl.completer.CompleterData;
import org.aesh.command.impl.completer.FileOptionCompleter;
import org.aesh.readline.AeshContext;
import org.aesh.readline.DefaultAeshContext;
import org.aesh.readline.terminal.formatting.TerminalString;
import org.aesh.terminal.utils.Config;
import org.aesh.io.FileResource;
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

    private final AeshContext aeshContext = new DefaultAeshContext();

    @Test(expected = IllegalArgumentException.class)
    public void testCompleterIllegalBaseDir() throws IOException {
        File file = File.createTempFile("tmp", ".tmp");
        file.deleteOnExit();
        aeshContext.setCurrentWorkingDirectory(new FileResource(file));
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
        aeshContext.setCurrentWorkingDirectory(new FileResource(file));
        FileOptionCompleter completer = new FileOptionCompleter();
        CompleterData data = new CompleterData(aeshContext, "", null);
        completer.complete(data);
        assertNotNull(data.getCompleterValues());
        assertEquals(1, data.getCompleterValues().size());
        assertEquals(child.getName(), data.getCompleterValues().get(0).getCharacters());

        File secretFile = new File(file, ".secret.txt");
        secretFile.createNewFile();
        secretFile.deleteOnExit();
        data = new CompleterData(aeshContext, ".", null);
        completer.complete(data);
        assertNotNull(data.getCompleterValues());
        assertEquals(1, data.getCompleterValues().size());
        assertEquals(secretFile.getName(), data.getCompleterValues().get(0).getCharacters());

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
        aeshContext.setCurrentWorkingDirectory(new FileResource(file));
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
        aeshContext.setCurrentWorkingDirectory(new FileResource(file));

        CompleterData data = new CompleterData(aeshContext, "", null);
        completer.complete(data);
        assertNotNull(data.getCompleterValues());
        assertEquals(2, data.getCompleterValues().size());
        assertTrue(data.getCompleterValues().contains(new TerminalString(child.getName() + Config.getPathSeparator(), true)));
        assertTrue(data.getCompleterValues().contains(new TerminalString(child2.getName() + Config.getPathSeparator(), true)));

        data = new CompleterData(aeshContext, "ch", null);
        completer.complete(data);
        assertNotNull(data.getCompleterValues());
        assertEquals(2, data.getCompleterValues().size());
        assertTrue(data.getCompleterValues().contains(new TerminalString(child.getName() + Config.getPathSeparator(), true)));
        assertTrue(data.getCompleterValues().contains(new TerminalString(child2.getName() + Config.getPathSeparator(), true)));

        data = new CompleterData(aeshContext, "child", null);
        completer.complete(data);
        assertNotNull(data.getCompleterValues());
        assertEquals(2, data.getCompleterValues().size());
        assertTrue(data.getCompleterValues().contains(new TerminalString(child.getName() + Config.getPathSeparator(), true)));
        assertTrue(data.getCompleterValues().contains(new TerminalString(child2.getName() + Config.getPathSeparator(), true)));
    }

}
