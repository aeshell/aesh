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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Vi implements EditMode {

    private Status status = Status.EDIT;
    private Action previousAction;

    private ActionEvent currentAction;

    private Map<Key, ActionStatus> actions;
    private Map<KeyEvent,ActionStatus> keyEventActions;
    private Map<Key, ActionStatusGroup> actionGroups;
    private Map<Variable,String> variables;

    private static final Logger LOGGER = LoggerUtil.getLogger(Vi.class.getName());

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

    public Vi addAction(Key key, Action action, Status status, Status after, Status actionStatus) {
        actions.put(key, new ActionStatus(action, status, after, actionStatus));
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
    public KeyEvent[] keys() {
        List<KeyEvent> keys = new ArrayList<>(actions.size()+keyEventActions.size()+actionGroups.size());
        actions.keySet().forEach( keys::add);
        actionGroups.keySet().forEach( keys::add);
        keyEventActions.keySet().forEach(keys::add);
        return keys.toArray(new KeyEvent[keys.size()]);
    }

    @Override
    public Status getCurrentStatus() {
        return status;
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
        LOGGER.info("got event:"+event);
        ActionStatus newStatus = getActionStatus(event);
        LOGGER.info("ActionStatus: "+newStatus);
        if(newStatus == null)
            return null;
        else {
            LOGGER.info("current status: "+status);
            if(newStatus.getCurrentStatus() == status) {
                if(newStatus.getAction() instanceof ActionEvent) {
                    currentAction = (ActionEvent) newStatus.getAction();
                    currentAction.input(newStatus.getAction(), event);
                }
                else {
                    if(newStatus.nextStatus == Status.REPEAT) {
                        return previousAction;
                    }
                    else {
                        if(status == Status.DELETE ||
                                newStatus.actionStatus == Status.DELETE ||
                                newStatus.actionStatus == Status.CHANGE)
                            previousAction = newStatus.getAction();
                        status = newStatus.nextStatus;
                    }
                    LOGGER.info("new status is: "+status);
                }
                LOGGER.info("returning action: "+newStatus.getAction());
                return newStatus.getAction();
            }
            else
                return null;
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
        private final Status actionStatus;

        ActionStatus(String action, Status status, Status nextStatus) {
            this.action = ActionMapper.mapToAction(action);
            this.currentStatus = status;
            this.nextStatus = nextStatus;
            this.actionStatus = Status.COMMAND;
        }

        ActionStatus(Action action, Status status, Status nextStatus) {
            this.action = action;
            this.currentStatus = status;
            this.nextStatus = nextStatus;
            this.actionStatus = Status.COMMAND;
        }

        ActionStatus(Action action, Status status, Status nextStatus, Status actionStatus) {
            this.action = action;
            this.currentStatus = status;
            this.nextStatus = nextStatus;
            this.actionStatus = actionStatus;
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

        public Status getActionStatus() {
            return actionStatus;
        }

        @Override
        public String toString() {
            return "ActionStatus{" +
                    "action=" + action +
                    ", currentStatus=" + currentStatus +
                    ", nextStatus=" + nextStatus +
                    '}';
        }
    }
}
