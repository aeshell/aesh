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
import org.jboss.jreadline.edit.actions.Movement;
import org.jboss.jreadline.edit.actions.Operation;

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

    public ViEditMode() {
       mode = Action.EDIT;
       previousMode = Action.EDIT;
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

        //if we're already in search mode
        if(mode == Action.SEARCH) {
            if(c == ENTER) {
                mode = Action.EDIT;
                return new Operation(Movement.END, Action.SEARCH);
            }
            else if(c == CTRL_R) {
                return new Operation(Movement.PREV_WORD, Action.SEARCH);
            }
            else if(c == BACKSPACE) {
                return new Operation(Movement.PREV_BIG_WORD, Action.SEARCH);
            }
            else if(c == ESCAPE) {
                mode = Action.EDIT;
                return new Operation(Movement.NEXT_BIG_WORD, Action.SEARCH);
            }

            // search input
            else {
                return new Operation(Movement.ALL, Action.SEARCH);
            }
        }

        else if(c == ENTER) {
            mode = Action.EDIT; //set to edit after a newline
            return new Operation(Movement.PREV, Action.NEWLINE);
        }
        else if(c == BACKSPACE) {
          if(isInEditMode())
              return new Operation(Movement.PREV, Action.DELETE);
            else
              return new Operation(Movement.PREV, Action.NO_ACTION);
        }
        else if(c == TAB) {
            if(isInEditMode())
                return new Operation(Movement.NEXT, Action.COMPLETE);
            else
                return new Operation(Movement.PREV, Action.NO_ACTION);
        }
        else if(c == ESCAPE) {
            switchEditMode();
            if(isInEditMode())
                return new Operation(Movement.NEXT, Action.NO_ACTION);
            else
                return new Operation(Movement.PREV, Action.MOVE);
        }
        else if (c == CTRL_R) {
            mode = Action.SEARCH;
            return new Operation(Movement.PREV, Action.SEARCH);
        }

        if(!isInEditMode())
            return inCommandMode(c);
        else
            return new Operation(Movement.NEXT, Action.EDIT);
    }

    private Operation inCommandMode(int c) {
        //movement
        if(c == VI_H)
            return saveAction(new Operation(Movement.PREV, mode));
        else if(c == VI_L || c == VI_SPACE)
            return saveAction(new Operation(Movement.NEXT, mode));
        else if(c == VI_J)
            return saveAction(new Operation(Movement.NEXT, Action.HISTORY));
        else if(c == VI_K)
            return saveAction(new Operation(Movement.PREV, Action.HISTORY));
        else if(c == VI_B)
            return saveAction(new Operation(Movement.PREV_WORD, mode));
        else if(c == VI_SHIFT_B)
            return saveAction(new Operation(Movement.PREV_BIG_WORD, mode));
        else if(c == VI_W)
            return saveAction(new Operation(Movement.NEXT_WORD, mode));
        else if(c == VI_SHIFT_W)
            return saveAction(new Operation(Movement.NEXT_BIG_WORD, mode));
        else if(c == VI_0)
            return saveAction(new Operation(Movement.BEGINNING, mode));
        else if(c == VI_$)
            return saveAction(new Operation(Movement.END, mode));

        //edit
        else if(c == VI_X) {
            return saveAction(new Operation(Movement.NEXT, Action.DELETE));
        }
        // paste
        else if(c == VI_P)
           return saveAction(new Operation(Movement.PREV, Action.PASTE));
        else if(c == VI_SHIFT_P)
            return saveAction(new Operation(Movement.NEXT, Action.PASTE));
        // replace
        else if(c == VI_S) {
            switchEditMode();
            return saveAction(new Operation(Movement.NEXT, Action.DELETE));
        }
        else if(c == VI_SHIFT_S) {
            mode = Action.CHANGE;
            return saveAction(new Operation(Movement.ALL, mode));
        }
        // insert
        else if(c == VI_A) {
            switchEditMode();
            return saveAction(new Operation(Movement.NEXT, Action.MOVE));
        }
        else if(c == VI_SHIFT_A) {
            switchEditMode();
            return saveAction(new Operation(Movement.END, Action.MOVE));
        }
        else if(c == VI_I) {
            switchEditMode();
            return saveAction(new Operation(Action.NO_ACTION));
        }
        else if(c == VI_SHIFT_I) {
            switchEditMode();
            return saveAction(new Operation(Movement.BEGINNING, Action.MOVE));
        }
        //delete
        else if(c == VI_D) {
            //if we're already in delete-mode, delete the whole line
            if(isDeleteMode())
                return saveAction(new Operation(Movement.ALL, Action.DELETE));
            else
                mode = Action.DELETE;
        }
        else if(c == VI_SHIFT_D) {
            mode = Action.DELETE;
            return saveAction(new Operation(Movement.END, Action.DELETE));
        }
        else if(c == VI_C) {
            mode = Action.CHANGE;
        }
        else if(c == VI_SHIFT_C) {
            mode = Action.CHANGE;
            return saveAction(new Operation(Movement.END, Action.CHANGE));
        }
        else if(c == VI_ENTER) {
            switchEditMode();
            return new Operation(Action.NEWLINE);
        }
        else if(c == VI_PERIOD) {
            mode = previousMode;
            return previousAction;
        }
        else if(c == VI_U) {
            return saveAction(new Operation(Action.UNDO));
        }
        else if(c == VI_TILDE) {
            return saveAction(new Operation(Action.CASE));
        }
        else if(c == VI_Y) {
            //if we're already in yank-mode, yank the whole line
            if(isYankMode())
                return saveAction(new Operation(Movement.ALL, Action.YANK));
            else
                mode = Action.YANK;
        }

        return new Operation(Movement.BEGINNING, Action.NO_ACTION);
    }

    @Override
    public Action getCurrentAction() {
        return mode;
    }
}
