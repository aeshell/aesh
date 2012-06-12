/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
