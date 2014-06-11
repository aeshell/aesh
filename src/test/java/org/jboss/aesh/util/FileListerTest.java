/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jboss.aesh.comparators.PosixFileNameComparator;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.io.DefaultFileResource;
import org.jboss.aesh.io.FileResource;
import org.jboss.aesh.terminal.TerminalString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class FileListerTest {
    private FileResource workingDir;
    private AeshContext aeshContext = new AeshContext() {
        @Override
        public FileResource getCurrentWorkingDirectory() {
            return new DefaultFileResource(Config.getUserDir());
        }
        @Override
        public void setCurrentWorkingDirectory(FileResource cwd) {
        }
    };

    @Before
    public void before() throws IOException {
        File tmpWorkingDir = File.createTempFile("temp", ".FileListerTest");
        tmpWorkingDir .delete();
        tmpWorkingDir.mkdirs();
        workingDir = new DefaultFileResource(tmpWorkingDir);
    }

    @After
    public void after() {
        delete(workingDir, true);
    }

    @Test
    public void testFullCompletionWithSingleSubdirectory() {
        new File(workingDir, "child").mkdir();
        CompleteOperation completion = new CompleteOperation(aeshContext, "cd ", 2);
        new FileLister("", workingDir).findMatchingDirectories(completion);

        List<TerminalString> candidates = completion.getCompletionCandidates();
        assertEquals(1, candidates.size());
        assertEquals("child"+Config.getPathSeparator(), candidates.get(0).getCharacters());
    }

    @Test
    public void testPartialCompletionWithSingleSubdirectory() {
        new File(workingDir, "child").mkdir();
        CompleteOperation completion = new CompleteOperation(aeshContext, "cd ch", 2);
        new FileLister("ch", workingDir).findMatchingDirectories(completion);

        List<TerminalString> candidates = completion.getCompletionCandidates();
        assertEquals(1, candidates.size());
        assertEquals("child"+Config.getPathSeparator(), candidates.get(0).getCharacters());
    }

    @Test
    public void testFullCompletionWithMultipleSubdirectory() {
        new File(workingDir, "child").mkdir();
        new File(workingDir, "child2").mkdir();
        CompleteOperation completion = new CompleteOperation(aeshContext, "cd ", 2);
        new FileLister("", workingDir).findMatchingDirectories(completion);

        List<TerminalString> candidates = completion.getCompletionCandidates();
        assertEquals(2, candidates.size());
        assertTrue("child"+Config.getPathSeparator(),
                candidates.contains(new TerminalString("child"+Config.getPathSeparator(), true)));
        assertTrue("child2"+Config.getPathSeparator(),
                candidates.contains(new TerminalString("child2"+Config.getPathSeparator(), true)));
    }

    @Test
    public void testPartialCompletionWithMultipleSubdirectory() {
        new File(workingDir, "child").mkdir();
        new File(workingDir, "child2").mkdir();
        CompleteOperation completion = new CompleteOperation(aeshContext, "cd ch", 4);
        new FileLister("ch", workingDir).findMatchingDirectories(completion);

        List<TerminalString> candidates = completion.getCompletionCandidates();
        assertEquals(1, candidates.size());
        assertEquals("child", candidates.get(0).getCharacters());
    }

    @Test
    public void testCompletionWithSubdirectory() {
        File test = new File(workingDir, "test");
        test.mkdir();
        new File(test, "main2").mkdir();
        new File(test, "test2").mkdir();

        CompleteOperation completion = new CompleteOperation(aeshContext, "cd test"+Config.getPathSeparator(), 2);
        new FileLister("test"+Config.getPathSeparator(), workingDir).findMatchingDirectories(completion);
        List<TerminalString> candidates = completion.getCompletionCandidates();
        assertEquals(2, candidates.size());
        assertEquals("main2"+Config.getPathSeparator(), candidates.get(0).getCharacters());

    }

    @Test
    public void testDirectoryWithoutSlashCompletion() {
        File test = new File(workingDir, "test");
        test.mkdir();

        CompleteOperation completion = new CompleteOperation(aeshContext, "cd test", 2);
        new FileLister("test", workingDir).findMatchingDirectories(completion);
        List<TerminalString> candidates = completion.getCompletionCandidates();
        assertEquals(1, candidates.size());
        assertEquals("test" + Config.getPathSeparator(), candidates.get(0).getCharacters());
    }

    @Test
    public void testSubDirectoryWithoutSlashCompletion() {
        File test = new File(workingDir, "test");
        test.mkdir();
        new File(test, "child").mkdir();

        CompleteOperation completion = new CompleteOperation(aeshContext, "cd test/child", 2);
        new FileLister("test/child", workingDir).findMatchingDirectories(completion);
        List<TerminalString> candidates = completion.getCompletionCandidates();
        assertEquals(1, candidates.size());
        assertEquals("test/child" + Config.getPathSeparator(), candidates.get(0).getCharacters());
    }

    @Test
    public void testInWorkingDirectoryWithoutSlashCompletion() {
        File workingDir = new File(".");
        File test = new File(workingDir, "test");
        test.mkdir();

        CompleteOperation completion = new CompleteOperation(aeshContext, "cd test", 2);
        new FileLister("test", workingDir).findMatchingDirectories(completion);
        List<TerminalString> candidates = completion.getCompletionCandidates();
        assertEquals(1, candidates.size());
        assertEquals("test" + Config.getPathSeparator(), candidates.get(0).getCharacters());

        delete(new DefaultFileResource(test), true);
    }

    @Test
    public void testDifferentLengthsOneCompletion() {
        new File(workingDir.getAbsolutePath(), "b").mkdir();
        new File(workingDir.getAbsolutePath(), "bb").mkdir();
        new File(workingDir.getAbsolutePath(), "bbb").mkdir();

        CompleteOperation completion = new CompleteOperation(aeshContext, "cd b", 4);
        new FileLister("b", workingDir).findMatchingDirectories(completion);
        List<TerminalString> candidates = completion.getCompletionCandidates();
        assertEquals(3, candidates.size());
        assertEquals("b" + Config.getPathSeparator(), candidates.get(0).getCharacters());
        assertEquals("bb" + Config.getPathSeparator(), candidates.get(1).getCharacters());
        assertEquals("bbb" + Config.getPathSeparator(), candidates.get(2).getCharacters());
    }

    @Test
    public void testWorkingDirFilePrefixCompletion() throws IOException {
        File workingDirFile = new File("prefix");
        workingDirFile.createNewFile();

        new File(workingDir.toString(), "prefixdir").mkdir();

        CompleteOperation completion = new CompleteOperation(aeshContext, "cd prefix", 9);
        new FileLister("prefix", workingDir).findMatchingDirectories(completion);
        List<TerminalString> candidates = completion.getCompletionCandidates();

        assertEquals(1, candidates.size());
        assertEquals("prefixdir" + Config.getPathSeparator(), candidates.get(0).getCharacters());

        delete(new DefaultFileResource(workingDirFile), false);
    }

    public static boolean delete(FileResource file, final boolean recursive) {
        boolean result = false;
        if (recursive) {
            result = _deleteRecursive(file, true);
        } else {
            if ((file.listFileResources() != null) && (file.listFileResources().size() != 0)) {
                throw new RuntimeException("directory not empty");
            }

            result = file.delete();
        }
        return result;
    }

    private static boolean _deleteRecursive(final FileResource file, final boolean collect) {
        boolean result = true;

        List<FileResource> children = file.listFileResources();
        if (children != null) {
            for (FileResource sf : children) {
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
