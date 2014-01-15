/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.command;

import org.jboss.aesh.terminal.Key;

import java.util.Arrays;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandOperation {

    private final Key inputKey;
    private final int[] input;
    private final int position;

    public CommandOperation(int[] input) {
        inputKey = Key.getKey(input);
        this.input = input;
        position = inputKey.getKeyValues().length;
    }

    public CommandOperation(Key key, int[] input) {
        inputKey = key;
        this.input = input;
        position = inputKey.getKeyValues().length;
    }

    public CommandOperation(Key key) {
        inputKey = key;
        this.input = key.getKeyValues();
        position = inputKey.getKeyValues().length;
    }

    public CommandOperation(Key key, int[] input, int position) {
        inputKey = key;
        this.input = input;
        this.position = position;
    }

    public Key getInputKey() {
        return inputKey;
    }

    public int[] getInput() {
        return input;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return "CommandOperation{" +
                "inputKey=" + inputKey +
                ", input=" + Arrays.toString(input) +
                ", position=" + position +
                '}';
    }
}
