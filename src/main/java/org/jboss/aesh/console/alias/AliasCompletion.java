/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.alias;

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.parser.Parser;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AliasCompletion implements Completion {

    private static final String ALIAS = "alias";
    private static final String ALIAS_SPACE = "alias ";
    private static final String UNALIAS = "unalias";
    private static final String UNALIAS_SPACE = "unalias ";
    private AliasManager manager;

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
            completeOperation.setOffset(completeOperation.getCursor());
        }
        else if(completeOperation.getBuffer().startsWith(ALIAS_SPACE) ||
                completeOperation.getBuffer().startsWith(UNALIAS_SPACE)) {
            String word = Parser.findCurrentWordFromCursor(completeOperation.getBuffer(), completeOperation.getCursor());
            completeOperation.addCompletionCandidates(manager.findAllMatchingNames(word));
            completeOperation.setOffset(completeOperation.getCursor()-word.length());
        }
    }

}
