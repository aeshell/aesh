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
package org.aesh.builtins.less;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SimpleFileParserTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testLoadPageFromFile() throws IOException {
        File file = folder.newFile("page.txt");
        Files.write(file.toPath(), Arrays.asList("line one", "line two"),
                StandardCharsets.UTF_8);

        SimpleFileParser parser = new SimpleFileParser();
        parser.setFile(file);
        List<String> page = parser.loadPage(80);

        assertTrue(page.toString().contains("line one"));
        assertTrue(page.toString().contains("line two"));
    }

    @Test
    public void testLoadPageFromString() throws IOException {
        SimpleFileParser parser = new SimpleFileParser();
        parser.readPageAsString("alpha\nbeta");
        List<String> page = parser.loadPage(80);

        assertTrue(page.toString().contains("alpha"));
        assertTrue(page.toString().contains("beta"));
    }

    @Test
    public void testLoadPageFromStream() throws IOException {
        SimpleFileParser parser = new SimpleFileParser();
        parser.setFile(new ByteArrayInputStream("streamed\n".getBytes(StandardCharsets.UTF_8)));
        List<String> page = parser.loadPage(80);

        assertTrue(page.toString().contains("streamed"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetFileRejectsDirectory() throws IOException {
        new SimpleFileParser().setFile(folder.getRoot());
    }

    @Test
    public void testName() throws IOException {
        File file = folder.newFile("named.txt");
        SimpleFileParser parser = new SimpleFileParser();
        parser.setFile(file);
        assertEquals(file.getAbsolutePath(), parser.getName());
    }
}
