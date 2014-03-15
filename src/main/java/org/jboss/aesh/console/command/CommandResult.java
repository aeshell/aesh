/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.command;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public enum CommandResult {
    SUCCESS,
    FAILURE;

    private int result = 0;

    public void setResultValue(int result) {
        this.result = result;
    }

    public int getResultValue() {
        return result;
    }
}
