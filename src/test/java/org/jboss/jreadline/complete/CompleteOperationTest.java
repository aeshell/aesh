/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.complete;

import junit.framework.TestCase;

import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CompleteOperationTest extends TestCase {

    public CompleteOperationTest(String name) {
        super(name);
    }

    public void testGetFormattedCompletionCandidates() {
        CompleteOperation co = new CompleteOperation("ls foob", 6);
        co.addCompletionCandidate("foobar");
        co.addCompletionCandidate("foobars");
        co.setOffset(3);

        List<String> formattedCandidates = co.getFormattedCompletionCandidates();

        assertEquals("bar", formattedCandidates.get(0));
        assertEquals("bars", formattedCandidates.get(1));
    }

    public void testRemoveEscapedSpacesFromCompletionCandidates() {
        CompleteOperation co = new CompleteOperation("ls foob", 6);
        co.addCompletionCandidate("foo\\ bar");
        co.addCompletionCandidate("foo\\ bars");
        co.setOffset(3);

        co.removeEscapedSpacesFromCompletionCandidates();

        assertEquals("foo bar", co.getCompletionCandidates().get(0));
        assertEquals("foo bars", co.getCompletionCandidates().get(1));
    }
}
