/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
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
package org.jboss.aesh.edit;

import org.jboss.aesh.edit.actions.Action;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;

/**
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class KeyOperation {

    private final Key key;
    private final Operation operation;
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

        return key == that.key &&
                operation == that.operation &&
                workingMode == that.workingMode;
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
