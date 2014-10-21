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
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
