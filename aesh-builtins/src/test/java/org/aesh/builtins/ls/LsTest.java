/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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
package org.aesh.builtins.ls;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.aesh.builtins.common.AeshTestCommons;
import org.aesh.command.registry.CommandRegistryException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LsTest extends AeshTestCommons {

    private Path tempDir;

    @Before
    public void before() throws IOException {
        tempDir = createTempDirectory();
        Files.createFile(tempDir.resolve("visible.txt"));
    }

    @After
    public void after() throws IOException {
        deleteRecursiveTempDirectory(tempDir);
    }

    @Test
    public void testLs() throws IOException, CommandRegistryException {
        prepare(Ls.class);

        pushToOutput("ls " + tempDir.toFile().getAbsolutePath());
        assertTrue(getStream().contains("visible.txt"));

        finish();
    }

    @Test
    public void testLsLong() throws IOException, CommandRegistryException {
        prepare(Ls.class);

        connection().clearOutputBuffer();
        pushToOutput("ls -l " + tempDir.toFile().getAbsolutePath());
        String output = getStream();
        assertTrue("ls -l output: [" + output + "]", output.contains("visible.txt"));

        finish();
    }

    @Test
    public void testLsHelp() throws IOException, CommandRegistryException {
        prepare(Ls.class);

        connection().clearOutputBuffer();
        pushToOutput("ls -H");
        assertFalse(getStream().contains("visible.txt"));

        finish();
    }
}
