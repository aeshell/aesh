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
package org.jboss.aesh.complete;

import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.io.FileResource;
import org.jboss.aesh.io.Resource;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CompleteOperationTest {

    private final AeshContext aeshContext = new AeshContext() {
        @Override
        public Resource getCurrentWorkingDirectory() {
            return new FileResource(Config.getUserDir());
        }
        @Override
        public void setCurrentWorkingDirectory(Resource cwd) {
        }
    };

    @Test
    public void testGetFormattedCompletionCandidates() {
        CompleteOperation co = new CompleteOperation(aeshContext, "ls foob", 6);
        co.addCompletionCandidate("foobar");
        co.addCompletionCandidate("foobars");
        co.setOffset(3);

        List<String> formattedCandidates = co.getFormattedCompletionCandidates();

        assertEquals("bar", formattedCandidates.get(0));
        assertEquals("bars", formattedCandidates.get(1));
    }

    @Test
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
