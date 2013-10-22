/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.completer;

import java.io.File;

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.util.FileLister;
import org.jboss.aesh.util.FileLister.Filter;

/**
 * Completes {@link File} objects
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class FileOptionCompleter implements OptionCompleter {

    private final File cwd;
    private final Filter filter;

    public FileOptionCompleter() {
        this(new File(System.getProperty("user.dir")), Filter.ALL);
    }

    public FileOptionCompleter(File baseDir) {
        this(baseDir, Filter.ALL);
    }

    public FileOptionCompleter(File baseDir, Filter filter) {
        if (!baseDir.isDirectory()) {
            throw new IllegalArgumentException("Base Dir must be a directory");
        }
        if (filter == null) {
            throw new IllegalArgumentException("A valid filter must be informed");
        }
        this.cwd = baseDir;
        this.filter = filter;
    }

    @Override
    public void complete(CompleterData completerData) {

        CompleteOperation completeOperation = new CompleteOperation(completerData.getGivenCompleteValue(), 0);
        if (completerData.getGivenCompleteValue() == null)
            new FileLister("", cwd, filter).findMatchingDirectories(completeOperation);
        else
            new FileLister(completerData.getGivenCompleteValue(), cwd, filter)
                    .findMatchingDirectories(completeOperation);

        if (completeOperation.getCompletionCandidates().size() > 1) {
            completeOperation.removeEscapedSpacesFromCompletionCandidates();
        }

        completerData.setCompleterTerminalStringValues(completeOperation.getCompletionCandidates());
        if (completerData.getGivenCompleteValue() != null && completerData.getCompleterValues().size() == 1) {
            completerData.setAppendSpace(completeOperation.hasAppendSeparator());
        }
    }

    public File getWorkingDirectory() {
        return cwd;
    }

    public Filter getFilter() {
        return filter;
    }
}
