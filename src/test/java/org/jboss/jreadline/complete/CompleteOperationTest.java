/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
