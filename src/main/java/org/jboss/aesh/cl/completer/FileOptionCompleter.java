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
    @Override
    public CompleterData complete(String completeValue) {

        CompleteOperation completeOperation = new CompleteOperation(completeValue, 0);

        new FileLister(completeValue, new File(System.getProperty("user.dir"))).findMatchingDirectories(completeOperation);
        CompleterData completerData = new CompleterData();

        if(completeOperation.getCompletionCandidates().size() > 1) {
            completeOperation.removeEscapedSpacesFromCompletionCandidates();
        }

        completerData.setCompleterValues( completeOperation.getCompletionCandidates());
        if(completeValue != null && completerData.getCompleterValues().size() == 1) {
            completerData.setOffset(completeValue.length());
            completerData.setAppendSpace(false);
        }

        return completerData;
    }
}
