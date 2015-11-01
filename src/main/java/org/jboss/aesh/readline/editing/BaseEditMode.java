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
package org.jboss.aesh.readline.editing;

import org.jboss.aesh.readline.Action;
import org.jboss.aesh.readline.KeyEvent;
import org.jboss.aesh.readline.Variable;
import org.jboss.aesh.readline.actions.ActionMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href=mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
abstract class BaseEditMode implements EditMode {

    protected Map<KeyEvent, Action> actions;

    protected Map<Variable,String> variables;

    BaseEditMode() {
        actions = new HashMap<>();
        variables = new HashMap<>();
    }

    BaseEditMode(Map<KeyEvent, Action> actions) {
        this.actions = actions;
        variables = new HashMap<>();
    }

    BaseEditMode(Map<int[],String> actions, Map<Variable,String> variables) {
        this.actions = new HashMap<>(actions.size());
        for(int[] key : actions.keySet()) {
            addAction(key, actions.get(key));
        }
        this.variables = variables;
    }

    @Override
    public void addAction(int[] input, String action) {
        actions.replace(createKeyEvent(input), ActionMapper.mapToAction(action));
    }

    @Override
    public void addVariable(Variable variable, String value) {
        variables.replace(variable, value);
    }

    //counting how many times eof been pressed
    protected int eofCounter;
    //default value
    private int ignoreEof = 0;

    @Override
    public void updateIgnoreEOF(int eof) {
        ignoreEof = eof;
    }

    protected void resetEOF()  {
        eofCounter = 0;
    }

    protected int getEofCounter() {
        return eofCounter;
    }
}
