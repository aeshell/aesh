/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit;

import org.jboss.aesh.edit.actions.Action;
import org.jboss.aesh.edit.actions.Operation;

import java.util.Arrays;

/**
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class KeyOperation {

    private int[] keyValues;
    private Operation operation;
    private Action workingMode = Action.NO_ACTION;

    public KeyOperation(int value, Operation operation) {
        keyValues = new int[] {value};
        this.operation = operation;
    }

    public KeyOperation(int[] value, Operation operation) {
        keyValues = value;
        this.operation = operation;
    }

    public KeyOperation(int value, Operation operation, Action workingMode) {
        keyValues = new int[] {value};
        this.operation = operation;
        this.workingMode = workingMode;
    }

    public KeyOperation(int[] value, Operation operation, Action  workingMode) {
        keyValues = value;
        this.operation = operation;
        this.workingMode = workingMode;
    }

    public int[] getKeyValues() {
        return keyValues;
    }

    public int getFirstValue() {
        return keyValues[0];
    }

    public boolean hasMoreThanOneKeyValue() {
        return keyValues.length > 1;
    }

    public Operation getOperation() {
        return operation;
    }

    public Action getWorkingMode() {
        return workingMode;
    }

    public boolean equals(Object o) {
        if(o instanceof KeyOperation) {
            KeyOperation ko = (KeyOperation) o;
            if(ko.getOperation() == operation) {
                if(ko.getKeyValues().length == keyValues.length) {
                    for(int i=0; i < keyValues.length; i++)
                        if(ko.getKeyValues()[i] != keyValues[i])
                            return false;
                    return true;
                }
            }
        }
        return false;
    }

    public int hashCode() {
        return 1481003;
    }

    public String toString() {
        return "Operation: "+operation+", "+Arrays.toString(keyValues);
    }

    public boolean equalValues(int[] values) {
        return Arrays.equals(keyValues, values);
    }


}
