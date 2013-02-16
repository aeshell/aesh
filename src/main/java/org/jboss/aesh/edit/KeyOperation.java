/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit;

import org.jboss.aesh.edit.actions.Action;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;

/**
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class KeyOperation {

    private Key key;
    private Operation operation;
    private Action workingMode = Action.NO_ACTION;

    public KeyOperation(Key key, Operation operation) {
        this.key = key;
        this.operation = operation;
    }

    public KeyOperation(Key key, Operation operation, Action workingMode) {
        this.key = key;
        this.operation = operation;
        this.workingMode = workingMode;
    }

    public Key getKey() {
        return key;
    }

    public int[] getKeyValues() {
        return key.getKeyValues();
    }

    public int getFirstValue() {
        return key.getFirstValue();
    }

    public Operation getOperation() {
        return operation;
    }

    public Action getWorkingMode() {
        return workingMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KeyOperation)) return false;

        KeyOperation that = (KeyOperation) o;

        if (key != that.key) return false;
        if (operation != that.operation) return false;
        if (workingMode != that.workingMode) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + operation.hashCode();
        result = 31 * result + workingMode.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "KeyOperation{" +
                "key=" + key +
                ", operation=" + operation +
                ", workingMode=" + workingMode +
                '}';
    }
}
