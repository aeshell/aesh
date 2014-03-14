/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.export;

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.parser.Parser;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ExportCompletion implements Completion {

    private static final String EXPORT = "export";
    private static final String EXPORT_SPACE = "export ";
    private ExportManager exportManager;

    public ExportCompletion(ExportManager manager) {
        this.exportManager = manager;
    }

    @Override
    public void complete(CompleteOperation completeOperation) {

        if(completeOperation.getBuffer() == null || completeOperation.getBuffer().length() < 1) {
            completeOperation.addCompletionCandidate(EXPORT);
        }
        else if(EXPORT.startsWith(completeOperation.getBuffer()))
            completeOperation.addCompletionCandidate(EXPORT);
        else if(EXPORT_SPACE.equals(completeOperation.getBuffer()) ||
                EXPORT.equals(completeOperation.getBuffer().trim())) {
            completeOperation.addCompletionCandidates(exportManager.getAllNamesWithEquals());
            completeOperation.setOffset(completeOperation.getCursor());
        }
        else if(completeOperation.getBuffer().startsWith(EXPORT_SPACE)) {
            String word = Parser.findCurrentWordFromCursor(completeOperation.getBuffer(), completeOperation.getCursor());
            if(word.length() > 0) {
                completeOperation.addCompletionCandidates( exportManager.findAllMatchingKeys(word));
                if(Parser.containsNonEscapedDollar(word)) {
                    int index = word.lastIndexOf('$');
                    completeOperation.setOffset(completeOperation.getCursor()-(word.length()-index));
                }
                else
                    completeOperation.setOffset(completeOperation.getCursor()-word.length());
            }
        }
        else if(Parser.containsNonEscapedDollar( completeOperation.getBuffer())) {
            String word = Parser.findCurrentWordFromCursor(completeOperation.getBuffer(), completeOperation.getCursor());
            if(Parser.containsNonEscapedDollar(word)) {
                completeOperation.addCompletionCandidates(exportManager.findAllMatchingKeys(word));
                completeOperation.setOffset(completeOperation.getCursor()-word.length());
            }
        }
    }
}
