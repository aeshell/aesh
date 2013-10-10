/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.edit.actions.Operation;

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
    private static final int[] UP = new int[]{27,91,65};
    private static final int[] DOWN = new int[]{27,91,66};
    private static final int[] RIGHT = new int[]{27,91,67};
    private static final int[] LEFT = new int[]{27,91,68};

    private static final int[] UP2 = new int[]{27,79,65};
    private static final int[] DOWN2 = new int[]{27,79,66};
    private static final int[] RIGHT2 = new int[]{27,79,67};
    private static final int[] LEFT2 = new int[]{27,79,68};

    public KeyOperationManager() {
       operations = new ArrayList<KeyOperation>();
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

    private boolean exists(KeyOperation operation) {
        for(KeyOperation ko : operations)
            if(Arrays.equals(ko.getKeyValues(), operation.getKeyValues()))
                return true;

        return false;
    }

    private void checkAndRemove(KeyOperation ko) {
        Iterator<KeyOperation> iter = operations.iterator();
        while(iter.hasNext()) {
            KeyOperation operation = iter.next();
            if(Arrays.equals(operation.getKeyValues(), ko.getKeyValues()) &&
                    operation.getWorkingMode().equals(ko.getWorkingMode())) {
                iter.remove();
                return;
            }
        }
    }

    public KeyOperation findOperation(int[] input) {
        for(KeyOperation operation : operations) {
            if(operation.equalValues(input))
                return operation;
        }

        return findOtherOperations(input);
    }

    /**
     * If we cant find an operation in Key we its either a char we havent mapped
     * or possibly a block that contains several identical chars.
     * @param input
     * @return
     */
    private KeyOperation findOtherOperations(int[] input) {
        if(startsWithEscape(input)) {
            if(Config.isOSPOSIXCompatible()) {
                if(startsFirstArgWithSecond(input, UP) && input.length % UP.length == 0)
                    return findOperation(UP);
                else if(startsFirstArgWithSecond(input, UP2) && input.length % UP2.length == 0)
                    return findOperation(UP2);
                if(startsFirstArgWithSecond(input, DOWN) && input.length % DOWN.length == 0)
                    return findOperation(UP);
                else if(startsFirstArgWithSecond(input, DOWN2) && input.length % DOWN2.length == 0)
                    return findOperation(DOWN2);
                if(startsFirstArgWithSecond(input, LEFT) && input.length % LEFT.length == 0)
                    return findOperation(LEFT);
                else if(startsFirstArgWithSecond(input, LEFT2) && input.length % LEFT2.length == 0)
                    return findOperation(LEFT2);
                if(startsFirstArgWithSecond(input, DOWN) && input.length % DOWN.length == 0)
                    return findOperation(DOWN);
                else if(startsFirstArgWithSecond(input, DOWN2) && input.length % DOWN2.length == 0)
                    return findOperation(DOWN2);
                else
                    return new KeyOperation(new int[]{27}, Operation.NO_ACTION);
            }
            //we're lazy and doesnt do much for windows here
            else
                return new KeyOperation(new int[]{224}, Operation.NO_ACTION);
        }
        //doesnt start with esc, lets just say its an input
        else {
            return null;
        }
    }

    private boolean startsWithEscape(int[] input) {
        if(Config.isOSPOSIXCompatible())
            return input[0] == 27;
        else
            return input[0] == 224;
    }

    private boolean startsFirstArgWithSecond(int[] input1, int[] input2) {
        if(input1.length < input2.length)
            return false;
        else {
            for(int i=0; i < input2.length; i++) {
                if(input1[i] != input2[i])
                    return false;
            }
            return true;
        }
    }

}
