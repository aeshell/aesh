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
package org.aesh.builtins.cat;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.aesh.builtins.common.AeshTestCommons;
import org.aesh.command.registry.CommandRegistryException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CatTest extends AeshTestCommons {

    private Path tempDir;

    @Before
    public void before() throws IOException {
        tempDir = createTempDirectory();
        Files.write(tempDir.resolve("hello.txt"),
                Arrays.asList("first line", "", "third line"), StandardCharsets.UTF_8);
    }

    @After
    public void after() throws IOException {
        deleteRecursiveTempDirectory(tempDir);
    }

    @Test
    public void testCat() throws IOException, CommandRegistryException {
        prepare(Cat.class);
        String file = tempDir.resolve("hello.txt").toFile().getAbsolutePath();

        pushToOutput("cat " + file);
        String output = getStream();
        assertTrue(output.contains("first line"));
        assertTrue(output.contains("third line"));

        finish();
    }

    @Test
    public void testCatNumbered() throws IOException, CommandRegistryException {
        prepare(Cat.class);
        String file = tempDir.resolve("hello.txt").toFile().getAbsolutePath();

        connection().clearOutputBuffer();
        pushToOutput("cat -n " + file);
        String output = getStream();
        assertTrue(output.contains("1"));
        assertTrue(output.contains("first line"));

        finish();
    }

    @Test
    public void testCatMissingFile() throws IOException, CommandRegistryException {
        prepare(Cat.class);

        connection().clearOutputBuffer();
        pushToOutput("cat " + tempDir.resolve("missing.txt").toFile().getAbsolutePath());
        assertTrue("missing file output: [" + getStream() + "]", getStream().contains("cat:"));

        finish();
    }
}
