/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit;

import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class KeyOperationManager {

    private List<KeyOperation> operations;

    public KeyOperationManager() {
       operations = new ArrayList<>();
    }

    public KeyOperationManager(List<KeyOperation> operations) {
        this.operations = new ArrayList<>();
        this.operations.addAll(operations);
    }

    public List<KeyOperation> getOperations() {
        return operations;
    }

    public void clear() {
        operations.clear();
    }

    public void addOperations(List<KeyOperation> newOperations) {
        for(KeyOperation ko : newOperations) {
            checkAndRemove(ko);
            operations.add(ko);
        }
    }

    public void addOperation(KeyOperation operation ) {
        checkAndRemove(operation);
        operations.add(operation);
    }

    public void addOperationIgnoreWorkingMode(KeyOperation operation ) {
        checkAndRemoveIgnoreWorkingMode(operation);
        operations.add(operation);
    }

    private boolean exists(KeyOperation operation) {
        for(KeyOperation ko : operations)
            if(Arrays.equals(ko.getKeyValues(), operation.getKeyValues()))
                return true;

        return false;
    }

    private void checkAndRemoveIgnoreWorkingMode(KeyOperation ko) {
        checkAndRemove(ko, true);
    }

    private void checkAndRemove(KeyOperation ko) {
      checkAndRemove(ko, false);
    }

    private void checkAndRemove(KeyOperation ko, boolean ignoreWorkingMode) {
        Iterator<KeyOperation> iter = operations.iterator();
        while(iter.hasNext()) {
            KeyOperation operation = iter.next();
            if(ignoreWorkingMode) {
                if(Arrays.equals(operation.getKeyValues(), ko.getKeyValues())) {
                    iter.remove();
                    return;
                }
            }
            else {
                if(Arrays.equals(operation.getKeyValues(), ko.getKeyValues()) &&
                        operation.getWorkingMode().equals(ko.getWorkingMode())) {
                    iter.remove();
                    return;
                }
            }
        }
    }

    public KeyOperation findOperation(Key key) {
        for(KeyOperation operation : operations)
            if(operation.getKey() == key)
                return operation;

        return null;
    }

    public KeyOperation findOperation(int[] input) {
        for(KeyOperation operation : operations) {
            if(operation.getKey().equalTo(input))
                return operation;
        }

        return findOtherOperations(input);
    }

    /**
     * If we cant find an operation in Key we its either a char we haven't mapped
     * or possibly a block that contains several identical chars.
     * @param input input
     * @return found operation
     */
    private KeyOperation findOtherOperations(int[] input) {
        if(Key.startsWithEscape(input)) {
            if(Key.UP.inputStartsWithKey(input) && input.length % Key.UP.getKeyValues().length == 0)
                return findOperation(Key.UP);
            else if(Key.UP_2.inputStartsWithKey(input) && input.length % Key.UP_2.getKeyValues().length == 0)
                return findOperation(Key.UP_2);
            if(Key.DOWN.inputStartsWithKey(input) && input.length % Key.DOWN.getKeyValues().length == 0)
                return findOperation(Key.DOWN);
            else if(Key.DOWN_2.inputStartsWithKey(input) && input.length % Key.DOWN_2.getKeyValues().length == 0)
                return findOperation(Key.DOWN_2);
            if(Key.LEFT.inputStartsWithKey(input) && input.length % Key.LEFT.getKeyValues().length == 0)
                return findOperation(Key.LEFT);
            else if(Key.LEFT_2.inputStartsWithKey(input) && input.length % Key.LEFT_2.getKeyValues().length == 0)
                return findOperation(Key.LEFT_2);
            if(Key.RIGHT.inputStartsWithKey(input) && input.length % Key.RIGHT.getKeyValues().length == 0)
                return findOperation(Key.RIGHT);
            else if(Key.RIGHT_2.inputStartsWithKey(input) && input.length % Key.RIGHT_2.getKeyValues().length == 0)
                return findOperation(Key.RIGHT_2);
            else
                return new KeyOperation(Key.ESC, Operation.NO_ACTION);
        }
        //doesnt start with esc, lets just say its an input
        else {
            return null;
        }

    }

}
