/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.util;

import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.io.FileResource;
import org.jboss.aesh.io.Resource;
import org.jboss.aesh.terminal.TestTerminal;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;

/**
 * @author <a href="mailto:00hf11@gmail.com">Helio Frota</a>
 */
public class FileUtilsTest {

    private Resource resource;
    private AeshContext aeshContext;

    @Before
    public void setUp() throws IOException {
        PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis = new PipedInputStream(pos);
        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .inputStream(pis)
                .outputStream(new PrintStream(new ByteArrayOutputStream()))
                .logging(true)
                .create();

        aeshContext = settings.getAeshContext();
    }

    @Test(expected = IOException.class)
    public void testIOExceptionSaveFile() throws IOException {
        resource = aeshContext.getCurrentWorkingDirectory().newInstance(".");
        FileUtils.saveFile(resource, "foo", false);
    }

    @Test
    public void testSaveFile() throws IOException {
        File file = File.createTempFile("tmp", ".tmp");
        file.delete();
        file.mkdir();
        file.deleteOnExit();
        File child = new File(file, "child.txt");
        child.createNewFile();
        child.deleteOnExit();

        aeshContext.setCurrentWorkingDirectory(new FileResource(file));
        resource = aeshContext.getCurrentWorkingDirectory();

        FileUtils.saveFile(resource.list().get(0), "foo", false);
        File f = new File(resource.list().get(0).getAbsolutePath());
        Assert.assertEquals(new String(Files.readAllBytes(f.toPath())), "foo");
    }

}
