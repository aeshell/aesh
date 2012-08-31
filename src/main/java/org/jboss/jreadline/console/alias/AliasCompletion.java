/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.console.alias;

import org.jboss.jreadline.complete.CompleteOperation;
import org.jboss.jreadline.complete.Completion;
import org.jboss.jreadline.console.Console;
import org.jboss.jreadline.console.settings.Settings;
import org.jboss.jreadline.util.Parser;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AliasCompletion implements Completion {

    private String completionName = "alias";
    private AliasManager manager;
    public AliasCompletion(AliasManager manager) {
        this.manager = manager;
    }

    @Override
    public void complete(CompleteOperation completeOperation) {
        completeOperation.addCompletionCandidates(manager.findAllMatchingNames(completeOperation.getBuffer().trim()));

        if(completionName.startsWith(completeOperation.getBuffer()))
            completeOperation.addCompletionCandidate(completionName);
        else if(completeOperation.getBuffer().equals("alias ")) {
            completeOperation.addCompletionCandidates(manager.getAllNames());
        }
        else if(completeOperation.getBuffer().startsWith("alias ")) {
            String word = Parser.findWordClosestToCursor(completeOperation.getBuffer(), completeOperation.getCursor());
            completeOperation.addCompletionCandidates(manager.findAllMatchingNames(word));
            completeOperation.setOffset(completeOperation.getCursor()-word.length());

        }
    }

}
