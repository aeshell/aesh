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

import org.jboss.jreadline.edit.actions.Action;
import org.jboss.jreadline.edit.actions.Operation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class ViEditMode implements EditMode {

    private static final short ESCAPE = 27;
    private static final short ENTER = 10;
    private static final short BACKSPACE = 127;
    private static final short TAB = 9;
    private static final short VI_S = 115;
    private static final short VI_SHIFT_S = 83;
    private static final short VI_D = 100;
    private static final short VI_SHIFT_D = 68;
    private static final short VI_C = 99;
    private static final short VI_SHIFT_C = 67;
    private static final short VI_A = 97;
    private static final short VI_SHIFT_A = 65;
    private static final short VI_0 = 48;
    private static final short VI_$ = 36;
    private static final short VI_X = 120;
    private static final short VI_P = 112;
    private static final short VI_SHIFT_P = 80;
    private static final short VI_I = 105;
    private static final short VI_SHIFT_I = 73;
    private static final short VI_TILDE = 126;
    private static final short VI_Y = 121;
    private static final short CTRL_E = 5;

    //movement
    private static final short VI_H = 104;
    private static final short VI_J = 106;
    private static final short VI_K = 107;
    private static final short VI_L = 108;
    private static final short VI_B = 98;
    private static final short VI_SHIFT_B = 66;
    private static final short VI_W = 119;
    private static final short VI_SHIFT_W = 87;
    private static final short VI_SPACE = 32;

    public static final short VI_ENTER = 10;
    private static final short VI_PERIOD = 46;
    private static final short VI_U = 117;

    //search
    private static final short CTRL_R = 18;


    private Action mode;
    private Action previousMode;

    private Operation previousAction;
    private List<KeyOperation> operations;
    private List<KeyOperation> currentOperations = new ArrayList<KeyOperation>();
    private int operationLevel = 0;

    public ViEditMode(List<KeyOperation> operations) {
        mode = Action.EDIT;
        previousMode = Action.EDIT;
        this.operations = operations;
    }

     private boolean isInEditMode() {
        return (mode == Action.EDIT);
    }

    private void switchEditMode() {
        if(mode == Action.EDIT)
            mode = Action.MOVE;
        else
            mode = Action.EDIT;
    }

    private boolean isDeleteMode() {
        return (mode == Action.DELETE);
    }

    private boolean isChangeMode() {
        return (mode == Action.CHANGE);
    }

    private boolean isYankMode() {
        return (mode == Action.YANK);
    }

    private Operation saveAction(Operation action) {
        previousMode = mode;
        //only save action for redo if its something else than move
        if(action.getAction() != Action.MOVE)
            previousAction = action;

        //if we've done a delete/change/yank we must switch back to move
        if(isDeleteMode() || isYankMode())
            mode = Action.MOVE;
        if(isChangeMode())
            mode = Action.EDIT;

        return action;
    }

    @Override
    public Operation parseInput(int c) {
         //if we're in the middle of parsing a sequence input
        if(operationLevel > 0) {
            Iterator<KeyOperation> operationIterator = currentOperations.iterator();
            while(operationIterator.hasNext())
                if(c != operationIterator.next().getKeyValues()[operationLevel])
                    operationIterator.remove();

        }
        // parse a first sequence input
        else {
            for(KeyOperation ko : operations)
                if(c == ko.getFirstValue())
                    currentOperations.add(ko);
        }

        //search mode need special handling
        if(mode == Action.SEARCH) {
                if(currentOperations.get(0).getOperation() == Operation.NEW_LINE) {
                    mode = Action.EDIT;
                    currentOperations.clear();
                    return Operation.SEARCH_END;
                }
                else if(currentOperations.get(0).getOperation() == Operation.SEARCH_PREV) {
                    currentOperations.clear();
                    return Operation.SEARCH_PREV_WORD;
                }
                else if(currentOperations.get(0).getOperation() == Operation.DELETE_PREV_CHAR) {
                    currentOperations.clear();
                    return Operation.SEARCH_DELETE;
                }
            //if we got more than one we know that it started with esc
                else if(currentOperations.get(0).getOperation() == Operation.ESCAPE) {
                mode = Action.EDIT;
                currentOperations.clear();
                return Operation.SEARCH_EXIT;
            }
            // search input
            else {
                currentOperations.clear();
                return Operation.SEARCH_INPUT;
            }
        } // end search mode

        //if we're already in search mode
        /*
        if(mode == Action.SEARCH) {
            if(c == ENTER) {
                mode = Action.EDIT;
                return Operation.SEARCH_END;
            }
            else if(c == CTRL_R) {
                return Operation.SEARCH_PREV_WORD;
            }
            else if(c == BACKSPACE) {
                return Operation.SEARCH_DELETE;
            }
            else if(c == ESCAPE) {
                mode = Action.EDIT;
                return Operation.SEARCH_EXIT;
            }
            // search input
            else {
                return Operation.SEARCH_INPUT;
            }
        }
        */

        if(currentOperations.isEmpty()) {
            if(isInEditMode())
                return Operation.EDIT;
            else
                return Operation.NO_ACTION;
        }

        else if(currentOperations.size() == 1) {
            Operation operation = currentOperations.get(0).getOperation();
            operationLevel = 0;
            currentOperations.clear();

            if(operation == Operation.NEW_LINE) {
                mode = Action.EDIT; //set to edit after a newline
                return Operation.NEW_LINE;
            }

            else if(operation == Operation.DELETE_PREV_CHAR) {
                if(isInEditMode())
                    return Operation.DELETE_PREV_CHAR;
                else
                    return Operation.NO_ACTION;
            }
            else if(operation == Operation.COMPLETE) {
                if(isInEditMode())
                    return Operation.COMPLETE;
                else
                    return Operation.NO_ACTION;
            }
            else if(operation == Operation.ESCAPE) {
                switchEditMode();
                if(isInEditMode())
                    return Operation.NO_ACTION;
                else
                    return Operation.MOVE_PREV_CHAR;
            }
            else if (operation == Operation.SEARCH_PREV) {
                mode = Action.SEARCH;
                return Operation.SEARCH_PREV;
            }


            if(!isInEditMode())
                return inCommandMode(c, operation);
            else
                return Operation.EDIT;
        }
        else {
            operationLevel++;
            return Operation.NO_ACTION;
        }

        /*
        else if(c == ENTER) {
            mode = Action.EDIT; //set to edit after a newline
            return Operation.NEW_LINE;
        }
        else if(c == BACKSPACE) {
          if(isInEditMode())
              return Operation.DELETE_PREV_CHAR;
            else
              return Operation.NO_ACTION;
        }
        else if(c == TAB) {
            if(isInEditMode())
                return Operation.COMPLETE;
            else
                return Operation.NO_ACTION;
        }
        else if(c == ESCAPE) {
            switchEditMode();
            if(isInEditMode())
                return Operation.NO_ACTION;
            else
                return Operation.MOVE_PREV_CHAR;
        }
        else if (c == CTRL_R) {
            mode = Action.SEARCH;
            return Operation.SEARCH_PREV;
        }

        if(!isInEditMode())
            return inCommandMode(c, operation);
        else
            return Operation.EDIT;
        */
    }

    private Operation inCommandMode(int c, Operation operation) {
        //movement
        if(operation == Operation.PREV_CHAR) {
            if(mode == Action.MOVE)
                return saveAction(Operation.MOVE_PREV_CHAR);
            else if(mode == Action.DELETE)
                return saveAction(Operation.DELETE_PREV_CHAR);
            else if(mode == Action.CHANGE)
                return saveAction(Operation.CHANGE_PREV_CHAR);
            else
                return saveAction(Operation.YANK_PREV_CHAR);
        }
        else if(operation == Operation.NEXT_CHAR) {
            if(mode == Action.MOVE)
                return saveAction(Operation.MOVE_NEXT_CHAR);
            else if(mode == Action.DELETE)
                return saveAction(Operation.DELETE_NEXT_CHAR);
            else if(mode == Action.CHANGE)
                return saveAction(Operation.CHANGE_NEXT_CHAR);
            else
                return saveAction(Operation.YANK_NEXT_CHAR);
        }
        else if(operation == Operation.HISTORY_NEXT) {
            return saveAction(Operation.HISTORY_NEXT);
        }
        else if(operation == Operation.HISTORY_PREV)
            return saveAction(Operation.HISTORY_PREV);
        else if(operation == Operation.PREV_WORD) {
            if(mode == Action.MOVE)
                return saveAction(Operation.MOVE_PREV_WORD);
            else if(mode == Action.DELETE)
                return saveAction(Operation.DELETE_PREV_WORD);
            else if(mode == Action.CHANGE)
                return saveAction(Operation.CHANGE_PREV_WORD);
            else
                return saveAction(Operation.YANK_PREV_WORD);
        }
        else if(operation == Operation.PREV_BIG_WORD) {
            if(mode == Action.MOVE)
                return saveAction(Operation.MOVE_PREV_BIG_WORD);
            else if(mode == Action.DELETE)
                return saveAction(Operation.DELETE_PREV_BIG_WORD);
            else if(mode == Action.CHANGE)
                return saveAction(Operation.CHANGE_PREV_BIG_WORD);
            else
                return saveAction(Operation.YANK_PREV_BIG_WORD);
        }
        else if(operation == Operation.NEXT_WORD) {
            if(mode == Action.MOVE)
                return saveAction(Operation.MOVE_NEXT_WORD);
            else if(mode == Action.DELETE)
                return saveAction(Operation.DELETE_NEXT_WORD);
            else if(mode == Action.CHANGE)
                return saveAction(Operation.CHANGE_NEXT_WORD);
            else
                return saveAction(Operation.YANK_NEXT_WORD);
        }
        else if(operation == Operation.NEXT_BIG_WORD) {
            if(mode == Action.MOVE)
                return saveAction(Operation.MOVE_NEXT_BIG_WORD);
            else if(mode == Action.DELETE)
                return saveAction(Operation.DELETE_NEXT_BIG_WORD);
            else if(mode == Action.CHANGE)
                return saveAction(Operation.CHANGE_NEXT_BIG_WORD);
            else
                return saveAction(Operation.YANK_NEXT_BIG_WORD);
        }
        else if(operation == Operation.BEGINNING) {
            if(mode == Action.MOVE)
                return saveAction(Operation.MOVE_BEGINNING);
            else if(mode == Action.DELETE)
                return saveAction(Operation.DELETE_BEGINNING);
            else if(mode == Action.CHANGE)
                return saveAction(Operation.CHANGE_BEGINNING);
            else
                return saveAction(Operation.YANK_BEGINNING);
        }
        else if(operation == Operation.END) {
            if(mode == Action.MOVE)
                return saveAction(Operation.MOVE_END);
            else if(mode == Action.DELETE)
                return saveAction(Operation.DELETE_END);
            else if(mode == Action.CHANGE)
                return saveAction(Operation.CHANGE_END);
            else
                return saveAction(Operation.YANK_END);
        }

        //edit
        else if(operation == Operation.DELETE_NEXT_CHAR) {
            return saveAction(operation);
        }
        // paste
        else if(operation == Operation.PASTE_AFTER)
           return saveAction(operation);

        else if(operation == Operation.PASTE_BEFORE)
            return saveAction(operation);
        // replace
        else if(operation == Operation.CHANGE_NEXT_CHAR) {
            switchEditMode();
            return saveAction(operation);
        }
        else if(operation == Operation.CHANGE_ALL) {
            mode = Action.CHANGE;
            return saveAction(operation);
        }
        // insert
        else if(operation == Operation.MOVE_NEXT_CHAR) {
            switchEditMode();
            return saveAction(operation);
        }
        else if(operation == Operation.MOVE_END) {
            switchEditMode();
            return saveAction(operation);
        }
        else if(operation == Operation.INSERT) {
            switchEditMode();
            return saveAction(Operation.NO_ACTION);
        }
        else if(operation == Operation.INSERT_BEGINNING) {
            switchEditMode();
            return saveAction(Operation.MOVE_BEGINNING);
        }
        //delete
        else if(operation == Operation.DELETE_ALL) {
            //if we're already in delete-mode, delete the whole line
            if(isDeleteMode())
                return saveAction(operation);
            else
                mode = Action.DELETE;
        }
        else if(operation == Operation.DELETE_END) {
            mode = Action.DELETE;
            return saveAction(operation);
        }
        else if(operation == Operation.CHANGE) {
            if(isChangeMode())
                return saveAction(Operation.CHANGE_ALL);
            else
                mode = Action.CHANGE;
        }
        else if(operation == Operation.CHANGE_END) {
            mode = Action.CHANGE;
            return saveAction(operation);
        }
        /*
        else if(c == VI_ENTER) {
            switchEditMode();
            return Operation.NEW_LINE;
        }
        */
        else if(operation == Operation.REPEAT) {
            mode = previousMode;
            return previousAction;
        }
        else if(operation == Operation.UNDO) {
            return saveAction(operation);
        }
        else if(operation == Operation.CASE) {
            return saveAction(operation);
        }
        else if(operation == Operation.YANK_ALL) {
            //if we're already in yank-mode, yank the whole line
            if(isYankMode())
                return saveAction(operation);
            else
                mode = Action.YANK;
        }
        else if(c == CTRL_E)
            return Operation.CHANGE_EDIT_MODE;

        return Operation.NO_ACTION;
    }

    @Override
    public Action getCurrentAction() {
        return mode;
    }
}
