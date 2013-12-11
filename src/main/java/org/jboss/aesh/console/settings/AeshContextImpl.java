/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.settings;

import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.Config;

import java.io.File;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshContextImpl implements AeshContext {

    private File cwd;

    AeshContextImpl(File cwd) {
        if(cwd != null && cwd.isDirectory())
            this.cwd = cwd;
        else
            throw new IllegalArgumentException("Current working directory must be a directory");
    }

    AeshContextImpl() {
        this.cwd = new File(Config.getUserDir());
    }

    @Override
    public File getCurrentWorkingDirectory() {
        return cwd;
    }

    @Override
    public void setCurrentWorkingDirectory(File cwd) {
        if(cwd.isDirectory())
            this.cwd = cwd;
        else
            throw new IllegalArgumentException("Current working directory must be a directory");
    }
}
