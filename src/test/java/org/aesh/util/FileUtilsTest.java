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
package org.aesh.util;

import org.aesh.readline.AeshContext;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.io.Resource;
import org.aesh.io.FileResource;
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
        Settings settings = SettingsBuilder.builder()
                .inputStream(pis)
                .outputStream(new PrintStream(new ByteArrayOutputStream()))
                .logging(true)
                .build();

        aeshContext = settings.aeshContext();
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
