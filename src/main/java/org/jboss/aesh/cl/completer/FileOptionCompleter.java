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
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class FileOptionCompleter implements OptionCompleter {
    @Override
    public List<String> complete(String completeValue) {

        CompleteOperation completeOperation = new CompleteOperation(completeValue, completeValue.length());

        new FileLister(completeValue, new File(System.getProperty("user.dir"))).findMatchingDirectories(completeOperation);
        return completeOperation.getCompletionCandidates();
    }
}
