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
                for(KeyOperation operation : operations) {
                    if(operation.isEscapeCommand() &&
                           startsFirstArgWithSecond(input, operation.getKeyValues()) &&
                            input.length % operation.getKeyValues().length == 0)
                        return operation;
                }
                return new KeyOperation(new int[]{27}, Operation.NO_ACTION);
            }
            //we're lazy and doesnt do much for windows here
            else
                return new KeyOperation(new int[]{224}, Operation.NO_ACTION);
        }
        else if(startsWithBackspace(input)) {
            if(Config.isOSPOSIXCompatible())
                return findOtherOperations(new int[]{127});
            else
                return findOtherOperations(new int[]{8});
        }
        //doesnt start with esc/backspace, lets just say its an input
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

    private boolean startsWithBackspace(int[] input) {
        if(Config.isOSPOSIXCompatible())
            return input[0] == 127;
        else
            return input[0] == 8;
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
