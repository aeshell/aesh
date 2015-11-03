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
import org.jboss.aesh.readline.actions.NoAction;
import org.jboss.aesh.terminal.Key;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Vi implements EditMode {

    private static final Action NO_ACTION = new NoAction();
    private Status status;

    private ActionEvent currentAction;

    private Map<Key, ActionStatus> actions;
    private Map<KeyEvent,ActionStatus> keyEventActions;
    private Map<Key, ActionStatusGroup> actionGroups;
    private Map<Variable,String> variables;

    Vi() {
        actions = new EnumMap<>(Key.class);
        variables = new HashMap<>();
        keyEventActions = new HashMap<>();
        actionGroups = new EnumMap<>(Key.class);
    }

    @Override
    public void addAction(int[] input, String action) {
        Key key = Key.getKey(input);
        if(key != null)
            addAction(key, action);
        else
            keyEventActions.put(createKeyEvent(input),
                    new ActionStatus(ActionMapper.mapToAction(action), Status.EDIT, Status.EDIT));
    }

    @Override
    public void addVariable(Variable variable, String value) {
        variables.put(variable, value);
    }

    @Override
    public String getVariableValue(Variable variable) {
        return variables.get(variable);
    }

    public Vi addAction(Key key, String action) {
        return addAction(key, action, Status.EDIT);
    }

    public Vi addAction(Key key, String action, Status status) {
        actions.put(key, new ActionStatus(ActionMapper.mapToAction(action), status, Status.EDIT));
        return this;
    }

    public Vi addAction(Key key, String action, Status status, Status after) {
        actions.put(key, new ActionStatus(ActionMapper.mapToAction(action), status, after));
        return this;
    }

    public Vi addAction(Key key, Action action) {
        return addAction(key, action, Status.EDIT);
    }

    public Vi addAction(Key key, Action action, Status status) {
        actions.put(key, new ActionStatus(action, status, Status.EDIT));
        return this;
    }

    public Vi addAction(Key key, Action action, Status status, Status after) {
        actions.put(key, new ActionStatus(action, status, after));
        return this;
    }

    public Vi addActionGroup(Key key, ActionStatusGroup group) {
        actionGroups.put(key, group);
        return this;
    }

    @Override
    public void updateIgnoreEOF(int eof) {
        //TODO
    }

    @Override
    public Mode getMode() {
        return Mode.VI;
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

    private Action getAction(KeyEvent event) {
        ActionStatus newStatus = getActionStatus(event);
        if(newStatus == null)
            return NO_ACTION;
        else {
            if(newStatus.getCurrentStatus() == status) {
                if(newStatus.getAction() instanceof ActionEvent) {
                    currentAction = (ActionEvent) newStatus.getAction();
                    currentAction.input(newStatus.getAction(), event);
                }
                else {
                    status = newStatus.nextStatus;
                }
                return newStatus.getAction();
            }
            else
                return NO_ACTION;
         }

        /*
        if(event instanceof Key) {
            parseKeyEvent((Key) event);
        }

        if(actions.containsKey(event)) {
            ActionStatus actionStatus =  actions.get(event);
            if(actionStatus.getAction() instanceof ActionEvent) {
                currentAction = (ActionEvent) actionStatus.getAction();
                currentAction.input(actionStatus.getAction(), event);
            }
            return actionStatus.getAction();
        }
        else {
            return null;
        }
        */
    }

    private void parseKeyEvent(Key event) {
        if(Key.ESC == event) {
            if(searchMode()) {
                status = Status.EDIT;
            }
            else
                status = Status.COMMAND;
        }
        //new line
        else if(Key.ENTER == event || Key.ENTER_2 == event ||
                Key.CTRL_J == event || Key.CTRL_K == event) {
            status = Status.EDIT;
        }
    }

    private ActionStatus getActionStatus(KeyEvent event) {
        if(event instanceof Key) {
            ActionStatus actionStatus = actions.get(event);
            if(actionStatus != null)
                return actionStatus;
            else {
                ActionStatusGroup group = actionGroups.get(event);
                if(group != null)
                    return group.getByCurrentStatus(status);
            }
            return null;
        }
        else {
            return parseKeyEventActions(event);
        }
    }

    private ActionStatus parseKeyEventActions(KeyEvent event) {
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

    private boolean deleteMode() {
        return status == Status.DELETE;
    }

    private boolean changeMode() {
        return status == Status.CHANGE;
    }

    private boolean replaceMode() {
        return status == Status.REPLACE;
    }

    private boolean yankMode() {
        return status == Status.YANK;
    }

    private boolean searchMode() {
        return status == Status.SEARCH;
    }

    private boolean editMode() {
        return status == Status.EDIT;
    }

    static final class ActionStatusGroup {

        private final ActionStatus[] actionStatuses;

        ActionStatusGroup(ActionStatus[] statues) {
            this.actionStatuses = statues;
        }

        ActionStatus getByCurrentStatus(Status currentStatus) {
            for(ActionStatus status : actionStatuses)
                if(status.getCurrentStatus() == currentStatus)
                    return status;
            return null;
        }
    }

    static final class ActionStatus {
        private final Action action;
        private final Status currentStatus;
        private final Status nextStatus;

        ActionStatus(String action, Status status, Status nextStatus) {
            this.action = ActionMapper.mapToAction(action);
            this.currentStatus = status;
            this.nextStatus = nextStatus;
        }

        ActionStatus(Action action, Status status, Status nextStatus) {
            this.action = action;
            this.currentStatus = status;
            this.nextStatus = nextStatus;
        }

        public Action getAction() {
            return action;
        }

        public Status getCurrentStatus() {
            return currentStatus;
        }

        public Status getNextStatus() {
            return nextStatus;
        }
    }
}
