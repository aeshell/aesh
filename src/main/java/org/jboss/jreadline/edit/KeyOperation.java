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

import org.jboss.jreadline.edit.actions.Operation;

/**
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class KeyOperation {

    private int[] keyValues;
    private Operation operation;

    public KeyOperation(int value, Operation operation) {
        keyValues = new int[] {value};
        this.operation = operation;

    }

    public KeyOperation(int[] value, Operation operation) {
        keyValues = value;
        this.operation = operation;
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
        return "Operation: "+operation;
    }


}
