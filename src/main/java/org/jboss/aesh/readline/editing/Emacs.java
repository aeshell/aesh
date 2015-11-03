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
import org.jboss.aesh.readline.ActionEvent;
import org.jboss.aesh.readline.KeyEvent;
import org.jboss.aesh.readline.Variable;
import org.jboss.aesh.readline.actions.ActionMapper;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.util.LoggerUtil;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Emacs implements EditMode {

    private ActionEvent currentAction;

    private Map<Key,Action> actions;
    private Map<Variable,String> variables;
    private Map<KeyEvent,Action> keyEventActions;

    //counting how many times eof been pressed
    protected int eofCounter;
    //default value
    private int ignoreEof = 0;

    private static final Logger LOGGER = LoggerUtil.getLogger(Emacs.class.getName());

    Emacs() {
        actions = new EnumMap<>(Key.class);
        variables = new EnumMap<>(Variable.class);
        keyEventActions = new HashMap<>();
    }

    @Override
    public void addAction(int[] input, String action) {
        Key key = Key.getKey(input);
        if(key != null)
            actions.put(key, ActionMapper.mapToAction(action));
        else
            keyEventActions.put(createKeyEvent(input), ActionMapper.mapToAction(action));
    }

    public void addAction(Key input, String action) {
        actions.put(input, ActionMapper.mapToAction(action));
    }

    public void addAction(Key input, Action action) {
        actions.put(input, action);
    }

    private Action parseKeyEventActions(KeyEvent event) {
        for(KeyEvent key : keyEventActions.keySet()) {
            boolean isEquals = true;
            if(key.length() == event.length()) {
                for(int i=0; i<key.length() && isEquals; i++)
                    if(key.getCodePointAt(i) != event.getCodePointAt(i))
                        isEquals = false;

                if(isEquals)
                    return keyEventActions.get(key);
            }
        }
        return null;
    }

    @Override
    public void addVariable(Variable variable, String value) {
        variables.put(variable, value);
    }

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

    @Override
    public Mode getMode() {
        return Mode.EMACS;
    }

    @Override
    public Action parse(KeyEvent event) {
        //are we already searching, it need to be processed by search action
        if(currentAction != null) {
            if(currentAction.keepFocus()) {
                currentAction.input(getAction(event), event);
                return currentAction;
            }
            else
                currentAction = null;
        }

        return getAction(event);
    }

    @Override
    public String getVariableValue(Variable variable) {
        return variables.get(variable);
    }

    private Action getAction(KeyEvent event) {
        Action action;
        if(event instanceof Key && actions.containsKey(event)) {
            action = actions.get(event);
        }
        else {
            action  = parseKeyEventActions(event);
        }
        if(action != null && action instanceof ActionEvent) {
            currentAction = (ActionEvent) action;
            currentAction.input(action, event);
        }
        return action;
    }

}
