/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.edit.actions.Operation;

import java.io.IOException;

/**
 * A ConsoleCommand is the base of any "external" commands that will run
 * in the foreground of aesh.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface ConsoleCommand {

    /**
     * Called after every operation made by the user
     *
     * @param operation operation
     * @throws IOException stream
     */
    public abstract void processOperation(Operation operation) throws IOException;

    public boolean isAttached();
}
