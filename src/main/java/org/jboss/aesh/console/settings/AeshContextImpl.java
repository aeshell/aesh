/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.settings;

import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.io.Resource;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshContextImpl implements AeshContext {

    private Resource cwd;

    AeshContextImpl(Resource cwd) {
        if(cwd != null && (!cwd.isLeaf() && cwd.exists()))
            this.cwd = cwd;
        else
            throw new IllegalArgumentException("Current working directory must be a directory");
    }

    @Override
    public Resource getCurrentWorkingDirectory() {
        return cwd;
    }

    @Override
    public void setCurrentWorkingDirectory(Resource cwd) {
        if(!cwd.isLeaf())
            this.cwd = cwd;
        else
            throw new IllegalArgumentException("Current working directory must be a directory");
    }
}
