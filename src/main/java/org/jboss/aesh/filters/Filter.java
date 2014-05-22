/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.aesh.filters;

import java.io.FileFilter;

import org.jboss.aesh.filters.file.DirectoryFileFilter;
import org.jboss.aesh.filters.file.FileAndDirectoryFilter;
import org.jboss.aesh.filters.file.FileAndDirectoryNoDotNamesFilter;
import org.jboss.aesh.filters.file.OnlyFileFilter;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 * @author <a href="mailto:danielsoro@gmail.com">Daniel Cunha (soro)</a>
 *
 */
public enum Filter {
    FILE(new OnlyFileFilter()),
    DIRECTORY(new DirectoryFileFilter()),
    ALL(new FileAndDirectoryFilter()),
    NO_DOT_NAMES(new FileAndDirectoryNoDotNamesFilter());

    private FileFilter fileFilter;

    private Filter(FileFilter fileFilter) {
        this.fileFilter = fileFilter;
    }

    public FileFilter getFileFilter() {
        return fileFilter;
    }
}