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
package org.jboss.aesh.console.alias;

import org.jboss.aesh.parser.Parser;
import org.jboss.aesh.readline.completion.CompleteOperation;
import org.jboss.aesh.readline.completion.Completion;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AliasCompletion implements Completion {

    private static final String ALIAS = "alias";
    private static final String ALIAS_SPACE = "alias ";
    private static final String UNALIAS = "unalias";
    private static final String UNALIAS_SPACE = "unalias ";
    private static final String HELP = "--help";
    private final AliasManager manager;

    public AliasCompletion(AliasManager manager) {
        this.manager = manager;
    }

    @Override
    public void complete(CompleteOperation completeOperation) {
        completeOperation.addCompletionCandidates(manager.findAllMatchingNames(completeOperation.getBuffer()));

        if(completeOperation.getBuffer() == null || completeOperation.getBuffer().length() < 1) {
            completeOperation.addCompletionCandidate(ALIAS);
            completeOperation.addCompletionCandidate(UNALIAS);
        }
        else if(ALIAS.startsWith(completeOperation.getBuffer()))
            completeOperation.addCompletionCandidate(ALIAS);
        else if(UNALIAS.startsWith(completeOperation.getBuffer()))
            completeOperation.addCompletionCandidate(UNALIAS);
        else if(completeOperation.getBuffer().equals(ALIAS_SPACE) ||
                completeOperation.getBuffer().equals(UNALIAS_SPACE)) {
            completeOperation.addCompletionCandidates(manager.getAllNames());
            completeOperation.addCompletionCandidate(HELP);
            completeOperation.setOffset(completeOperation.getCursor());
        }
        else if(completeOperation.getBuffer().startsWith(ALIAS_SPACE) ||
                completeOperation.getBuffer().startsWith(UNALIAS_SPACE)) {
            String word = Parser.findCurrentWordFromCursor(completeOperation.getBuffer(), completeOperation.getCursor());
            completeOperation.addCompletionCandidates(manager.findAllMatchingNames(word));
            if (HELP.startsWith(word)) {
                completeOperation.addCompletionCandidate(HELP);
            }
            completeOperation.setOffset(completeOperation.getCursor()-word.length());
        }
    }

}
