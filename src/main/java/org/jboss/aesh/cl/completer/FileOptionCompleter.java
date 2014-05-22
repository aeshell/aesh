/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.completer;

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.console.command.completer.CompleterInvocation;
import org.jboss.aesh.filters.Filter;
import org.jboss.aesh.util.FileLister;

/**
 * Completes {@link File} objects
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class FileOptionCompleter implements OptionCompleter<CompleterInvocation> {

    private final Filter filter;

    public FileOptionCompleter() {
        this(Filter.ALL);
    }

    public FileOptionCompleter(Filter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("A valid filter must be informed");
        }
        this.filter = filter;
    }

    @Override
    public void complete(CompleterInvocation completerData) {

        CompleteOperation completeOperation =
                new CompleteOperation(completerData.getAeshContext(), completerData.getGivenCompleteValue(), 0);
        if (completerData.getGivenCompleteValue() == null)
            new FileLister("", completerData.getAeshContext().getCurrentWorkingDirectory(), filter)
                    .findMatchingDirectories(completeOperation);
        else
            new FileLister(completerData.getGivenCompleteValue(),
                    completerData.getAeshContext().getCurrentWorkingDirectory(), filter)
                    .findMatchingDirectories(completeOperation);

        if (completeOperation.getCompletionCandidates().size() > 1) {
            completeOperation.removeEscapedSpacesFromCompletionCandidates();
        }

        completerData.setCompleterValuesTerminalString(completeOperation.getCompletionCandidates());
        if (completerData.getGivenCompleteValue() != null && completerData.getCompleterValues().size() == 1) {
            completerData.setAppendSpace(completeOperation.hasAppendSeparator());
        }

        if(completeOperation.doIgnoreOffset())
            completerData.setIgnoreOffset(completeOperation.doIgnoreOffset());

        completerData.setIgnoreStartsWith(true);
    }

    public Filter getFilter() {
        return filter;
    }
}
