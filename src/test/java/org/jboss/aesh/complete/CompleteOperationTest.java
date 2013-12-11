/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.complete;

import junit.framework.TestCase;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.Config;

import java.io.File;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CompleteOperationTest extends TestCase {

    private AeshContext aeshContext = new AeshContext() {
        @Override
        public File getCurrentWorkingDirectory() {
            return new File(Config.getUserDir());
        }
        @Override
        public void setCurrentWorkingDirectory(File cwd) {
        }
    };

    public CompleteOperationTest(String name) {
        super(name);
    }

    public void testGetFormattedCompletionCandidates() {
        CompleteOperation co = new CompleteOperation(aeshContext, "ls foob", 6);
        co.addCompletionCandidate("foobar");
        co.addCompletionCandidate("foobars");
        co.setOffset(3);

        List<String> formattedCandidates = co.getFormattedCompletionCandidates();

        assertEquals("bar", formattedCandidates.get(0));
        assertEquals("bars", formattedCandidates.get(1));
    }

    public void testRemoveEscapedSpacesFromCompletionCandidates() {
        CompleteOperation co = new CompleteOperation(aeshContext, "ls foob", 6);
        co.addCompletionCandidate("foo\\ bar");
        co.addCompletionCandidate("foo\\ bars");
        co.setOffset(3);

        co.removeEscapedSpacesFromCompletionCandidates();

        assertEquals("foo bar", co.getCompletionCandidates().get(0).getCharacters());
        assertEquals("foo bars", co.getCompletionCandidates().get(1).getCharacters());
    }
}
