/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.edit;

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
        return null;
    }

}
