/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.terminal.TerminalString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class FileListerTest {
    private File workingDir;

    @Before
    public void before() throws IOException {
        workingDir = File.createTempFile("aesh", ".FileListerTest");
        workingDir.delete();
        workingDir.mkdirs();
    }

    @After
    public void after() {
        delete(workingDir, true);
    }

    @Test
    public void testFullCompletionWithSingleSubdirectory() {
        new File(workingDir, "child").mkdir();
        CompleteOperation completion = new CompleteOperation("cd ", 2);
        new FileLister("", workingDir).findMatchingDirectories(completion);

        List<TerminalString> candidates = completion.getCompletionCandidates();
        assertEquals(1, candidates.size());
        assertEquals("child/", candidates.get(0).getCharacters());
    }

    @Test
    public void testPartialCompletionWithSingleSubdirectory() {
        new File(workingDir, "child").mkdir();
        CompleteOperation completion = new CompleteOperation("cd ch", 2);
        new FileLister("ch", workingDir).findMatchingDirectories(completion);

        List<TerminalString> candidates = completion.getCompletionCandidates();
        assertEquals(1, candidates.size());
        assertEquals("child/", candidates.get(0).getCharacters());
    }

    @Test
    public void testFullCompletionWithMultipleSubdirectory() {
        new File(workingDir, "child").mkdir();
        new File(workingDir, "child2").mkdir();
        CompleteOperation completion = new CompleteOperation("cd ", 2);
        new FileLister("", workingDir).findMatchingDirectories(completion);

        List<TerminalString> candidates = completion.getCompletionCandidates();
        assertEquals(2, candidates.size());
        assertTrue("child/", candidates.contains(new TerminalString("child/", true)));
        assertTrue("child2/", candidates.contains(new TerminalString("child2/", true)));
    }

    @Test
    public void testPartialCompletionWithMultipleSubdirectory() {
        new File(workingDir, "child").mkdir();
        new File(workingDir, "child2").mkdir();
        CompleteOperation completion = new CompleteOperation("cd ch", 4);
        new FileLister("ch", workingDir).findMatchingDirectories(completion);

        List<TerminalString> candidates = completion.getCompletionCandidates();
        assertEquals(1, candidates.size());
        assertEquals("child", candidates.get(0).getCharacters());
    }

    public static boolean delete(File file, final boolean recursive) {
        boolean result = false;
        if (recursive) {
            result = _deleteRecursive(file, true);
        } else {
            if ((file.listFiles() != null) && (file.listFiles().length != 0)) {
                throw new RuntimeException("directory not empty");
            }

            result = file.delete();
        }
        return result;
    }

    private static boolean _deleteRecursive(final File file, final boolean collect) {
        boolean result = true;

        File[] children = file.listFiles();
        if (children != null) {
            for (File sf : children) {
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
}
