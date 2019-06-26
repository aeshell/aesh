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

import org.aesh.impl.util.FileLister;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.aesh.comparators.PosixFileNameComparator;
import org.aesh.readline.AeshContext;
import org.aesh.io.Resource;
import org.aesh.io.FileResource;

import org.aesh.readline.DefaultAeshContext;
import org.aesh.terminal.utils.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class FileListerTest {
    private Resource workingDir;
    private static final String PATH_SEPARATOR = Config.getPathSeparator();
    private final AeshContext aeshContext = new DefaultAeshContext();

    @Before
    public void before() throws IOException {
        File tmpWorkingDir = File.createTempFile("temp", ".FileListerTest");
        tmpWorkingDir .delete();
        tmpWorkingDir.mkdirs();
        workingDir = new FileResource(tmpWorkingDir);
    }

    @After
    public void after() {
        delete(workingDir, true);
    }

    @Test
    public void testFileNameStartingWithDotCompletion() throws IOException {
        Files.createFile(new File(workingDir.toString()+Config.getPathSeparator()+".testfile").toPath());

        List<String> candidates = new ArrayList<>();
        new FileLister("", workingDir).findMatchingDirectories(candidates);
        assertEquals(1, candidates.size());
        assertEquals(".testfile", candidates.get(0));
    }

    @Test
    public void testFullCompletionWithSingleSubdirectory() {
        new File(workingDir.toString(), "child").mkdir();
        List<String> candidates = new ArrayList<>();
        new FileLister("", workingDir).findMatchingDirectories(candidates);

        assertEquals(1, candidates.size());
        assertEquals("child" + Config.getPathSeparator(), candidates.get(0));
    }

    @Test
    public void testPartialCompletionWithSingleSubdirectory() {
        new File(workingDir.toString(), "child").mkdir();
        List<String> candidates = new ArrayList<>();
        new FileLister("ch", workingDir).findMatchingDirectories(candidates);

        assertEquals(1, candidates.size());
        assertEquals("child" + Config.getPathSeparator(), candidates.get(0));
    }

    @Test
    public void testFullCompletionWithMultipleSubdirectory() {
        new File(workingDir.toString(), "child").mkdir();
        new File(workingDir.toString(), "child2").mkdir();
        List<String> candidates = new ArrayList<>();
        new FileLister("", workingDir).findMatchingDirectories(candidates);
        assertEquals(2, candidates.size());
        assertTrue("child"+Config.getPathSeparator(),
                candidates.contains("child" + Config.getPathSeparator()));
        assertTrue("child2"+Config.getPathSeparator(),
                candidates.contains("child2" + Config.getPathSeparator()));
    }

    @Test
    public void testPartialCompletionWithMultipleSubdirectory() {
        new File(workingDir.toString(), "child1").mkdir();
        new File(workingDir.toString(), "child2").mkdir();
        List<String> candidates = new ArrayList<>();
        new FileLister("ch", workingDir).findMatchingDirectories(candidates);
        assertEquals(2, candidates.size());
        assertEquals("child1" + Config.getPathSeparator(), candidates.get(0));
    }

    @Test
    public void testCompletionWithSubdirectory() {
        File test = new File(workingDir.toString(), "test");
        test.mkdir();
        new File(test, "main2").mkdir();
        new File(test, "test2").mkdir();

        List<String> candidates = new ArrayList<>();
        new FileLister("test" + Config.getPathSeparator(), workingDir).findMatchingDirectories(candidates);
        assertEquals(2, candidates.size());
        assertEquals("main2" + Config.getPathSeparator(), candidates.get(0));

    }

    @Test
    public void testDirectoryWithoutSlashCompletion() {
        File test = new File(workingDir.toString(), "test");
        test.mkdir();

        List<String> candidates = new ArrayList<>();
        new FileLister("test", workingDir).findMatchingDirectories(candidates);
        assertEquals(1, candidates.size());
        assertEquals("test" + Config.getPathSeparator(), candidates.get(0));
    }

    @Test
    public void testSubDirectoryWithoutSlashCompletion() {
        File test = new File(workingDir.toString(), "test123");
        test.mkdir();
        File child = new File(test, "child");
        child.mkdir();
        List<String> candidates = new ArrayList<>();
        int offset = new FileLister("test123" + PATH_SEPARATOR + "child", workingDir).findMatchingDirectories(candidates);
        assertEquals(8, offset);
        assertEquals(1, candidates.size());
        assertEquals("child" + Config.getPathSeparator(), candidates.get(0));
        child.delete();
        test.delete();
    }

    @Test
    public void testInWorkingDirectoryWithoutSlashCompletion() {
        final File workingDir = new File(System.getProperty("java.io.tmpdir"));
        File test = new File(workingDir, "test123");
        test.mkdir();
        List<String> candidates = new ArrayList<>();
        new FileLister("test123", new FileResource(workingDir)).
                findMatchingDirectories(candidates);
        assertEquals(1, candidates.size());
        assertEquals("test123" + Config.getPathSeparator(), candidates.get(0));

        delete(new FileResource(test), true);
    }

    @Test
    public void testDifferentLengthsOneCompletion() {
        new File(workingDir.getAbsolutePath(), "b").mkdir();
        new File(workingDir.getAbsolutePath(), "bb").mkdir();
        new File(workingDir.getAbsolutePath(), "bbb").mkdir();

        List<String> candidates = new ArrayList<>();
        new FileLister("b", workingDir).findMatchingDirectories(candidates);
        assertEquals(3, candidates.size());
        assertEquals("b" + Config.getPathSeparator(), candidates.get(0));
        assertEquals("bb" + Config.getPathSeparator(), candidates.get(1));
        assertEquals("bbb" + Config.getPathSeparator(), candidates.get(2));
    }

    @Test
    public void testWorkingDirFilePrefixCompletion() throws IOException {
        File workingDirFile = new File(System.getProperty("java.io.tmpdir"), "prefix");
        workingDirFile.createNewFile();

        new File(workingDir.toString(), "prefixdir").mkdir();

        List<String> candidates = new ArrayList<>();
        new FileLister("prefix", workingDir).findMatchingDirectories(candidates);

        assertEquals(1, candidates.size());
        assertEquals("prefixdir" + Config.getPathSeparator(), candidates.get(0));

        delete(new FileResource(workingDirFile), false);
    }

    public static boolean delete(Resource file, final boolean recursive) {
        boolean result = false;
        if (recursive) {
            result = _deleteRecursive(file, true);
        } else {
            if ((file.list() != null) && (file.list().size() != 0)) {
                throw new RuntimeException("directory not empty");
            }

            result = file.delete();
        }
        return result;
    }

    private static boolean _deleteRecursive(final Resource file, final boolean collect) {
        boolean result = true;

        List<Resource> children = file.list();
        if (children != null) {
            for (Resource sf : children) {
                if (sf.isDirectory()) {
                    if (!_deleteRecursive(sf, false))
                        result = false;
                } else {
                    if (!sf.delete())
                        result = false;
                }
            }
        }

        return file.delete() && result;
    }

    @Test
    public void posixFileNameComparatorTest() {
       PosixFileNameComparator comparator = new PosixFileNameComparator();

       assertEquals("a".compareToIgnoreCase("b"), comparator.compare("a", "b"));
       assertEquals("a".compareToIgnoreCase("b"), comparator.compare(".a", "b"));
       assertEquals("a".compareToIgnoreCase("b"), comparator.compare("a", ".b"));

       assertEquals("a".compareToIgnoreCase("b"), comparator.compare("A", "B"));

       assertEquals("A".compareToIgnoreCase("b"), comparator.compare("A", "b"));
       assertEquals("a".compareToIgnoreCase("B"), comparator.compare("a", "B"));
       assertEquals("a".compareToIgnoreCase("B"), comparator.compare(".a", ".B"));
       assertEquals("a".compareToIgnoreCase("B"), comparator.compare(".A", ".b"));

       assertEquals("A".compareToIgnoreCase("b"), comparator.compare(".A", "b"));
       assertEquals("A".compareToIgnoreCase("b"), comparator.compare("A", ".b"));
       assertEquals("a".compareToIgnoreCase("B"), comparator.compare("a", ".B"));
       assertEquals("a".compareToIgnoreCase("B"), comparator.compare(".a", "B"));
    }
}
