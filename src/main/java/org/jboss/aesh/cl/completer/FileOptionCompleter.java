/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.completer;

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.util.FileLister;

import java.io.File;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class FileOptionCompleter implements OptionCompleter {

    public void complete(CompleterData completerData) {

        CompleteOperation completeOperation = new CompleteOperation(completerData.getGivenCompleteValue(), 0);
        if(completerData.getGivenCompleteValue() == null)
            new FileLister("", new File(System.getProperty("user.dir"))).findMatchingDirectories(completeOperation);
        else
            new FileLister(completerData.getGivenCompleteValue(),
                    new File(System.getProperty("user.dir"))).findMatchingDirectories(completeOperation);


        if(completeOperation.getCompletionCandidates().size() > 1) {
            completeOperation.removeEscapedSpacesFromCompletionCandidates();
        }

        completerData.setCompleterValues( completeOperation.getCompletionCandidates());
        if(completerData.getGivenCompleteValue() != null && completerData.getCompleterValues().size() == 1) {
            completerData.setAppendSpace(completeOperation.hasAppendSeparator());
        }

    }
}
