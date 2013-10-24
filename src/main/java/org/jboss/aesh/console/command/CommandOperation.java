/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.command;

import org.jboss.aesh.terminal.Key;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandOperation {

    private Key inputKey;
    private int[] input;

    public CommandOperation(int[] input) {
        inputKey = Key.getKey(input);
        this.input = input;
    }

    public Key getInputKey() {
        return inputKey;
    }

    public int[] getInput() {
        return input;
    }
}
