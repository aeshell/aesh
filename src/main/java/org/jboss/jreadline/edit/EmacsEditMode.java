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
 * TODO:
 * - add support for different os key values (mainly windows)
 *
 * Trying to follow the gnu readline impl found here:
 * http://cnswww.cns.cwru.edu/php/chet/readline/readline.html
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class EmacsEditMode implements EditMode {

    private final static short CTRL_A = 1;
    private final static short CTRL_B = 2;
    //private final static short CTRL_C = 3;
    private final static short CTRL_D = 4;
    private final static short CTRL_E = 5;
    private final static short CTRL_F = 6;
    private final static short CTRL_G = 7;
    private final static short CTRL_H = 8;
    private final static short CTRL_I = 9;     //TAB
    private static final short ENTER = 10;

    private final static short CTRL_K = 11;
    private final static short CTRL_L = 12;
    private final static short CR = 13;
    private final static short CTRL_N = 14;
    private final static short CTRL_P = 16;
    private final static short CTRL_R = 18;
    private final static short CTRL_S = 19;
    private final static short CTRL_U = 21;
    private final static short CTRL_V = 22;
    private final static short CTRL_W = 23;
    private final static short CTRL_X = 24;
    private final static short CTRL_Y = 25; // yank
    private final static short ESCAPE = 27;
    private final static short CTRL__ = 31;
    private final static short ARROW_START = 91;
    private final static short LEFT = 68;
    private final static short RIGHT = 67;
    private final static short UP = 65;
    private final static short DOWN = 66;
    private final static short BACKSPACE = 127;
    private final static short F = 102; // needed to handle M-f
    private final static short B = 98; // needed to handle M-b
    private final static short D = 100; // needed to handle M-d

    private boolean arrowStart = false;
    private boolean arrowPrefix = false;
    private boolean ctrl_xState = false;

    private Action mode = Action.EDIT;

    @Override
    public Operation parseInput(int input) {

        //if we're already in search mode
        //TODO: Add support for arrow keys and escape
        if(mode == Action.SEARCH) {
            if(input == ENTER) {
                mode = Action.EDIT;
                return new Operation(Movement.END, Action.SEARCH);
            }
            else if(input == CTRL_R) {
                return new Operation(Movement.PREV_WORD, Action.SEARCH);
            }
            else if(input == CTRL_S) {
                return new Operation(Movement.NEXT_WORD, Action.SEARCH);
            }
            else if(input == BACKSPACE) {
                return new Operation(Movement.PREV_BIG_WORD, Action.SEARCH);
            }
            else if(input == ESCAPE) {
                mode = Action.EDIT;
                return new Operation(Movement.NEXT_BIG_WORD, Action.SEARCH);
            }

            // search input
            else {
                return new Operation(Movement.ALL, Action.SEARCH);
            }
        }


        if(input == ENTER)
            return new Operation(Movement.PREV, Action.NEWLINE);
        else if(input == BACKSPACE)
            return new Operation(Movement.PREV, Action.DELETE);
        if(input == CTRL_A)
            return new Operation(Movement.BEGINNING, Action.MOVE);
        else if(input == CTRL_B)
            return new Operation(Movement.PREV, Action.MOVE);
        else if(input == CTRL_D)
            return new Operation(Movement.NEXT, Action.DELETE);
        else if(input == CTRL_E)
            return new Operation(Movement.END, Action.MOVE);
        else if(input == CTRL_F)
            return new Operation(Movement.NEXT, Action.MOVE);
        else if(input == CTRL_G)
            return new Operation(Movement.PREV, Action.ABORT);
        else if(input == CTRL_H)
            return new Operation(Movement.PREV, Action.DELETE);
        else if(input == CTRL_I)
            return new Operation(Movement.PREV, Action.COMPLETE);
        else if(input == CTRL_K)
            return new Operation(Movement.END, Action.DELETE);
        else if(input == CTRL_L)
            return new Operation(Movement.ALL, Action.DELETE); //TODO: should change to clear screen
        else if(input == CTRL_N)
            return new Operation(Movement.NEXT, Action.HISTORY);
        else if(input == CTRL_P)
            return new Operation(Movement.PREV, Action.HISTORY);
        else if(input == CTRL__)
            return new Operation(Action.UNDO);

        else if(input == CTRL_U) {
            //only undo if C-x have been pressed first
            if(ctrl_xState) {
                ctrl_xState = false;
                return new Operation(Action.UNDO);
            }
            else
                return new Operation(Movement.BEGINNING, Action.DELETE);
        }
        else if(input == CTRL_V)
            return new Operation(Movement.NEXT, Action.PASTE_FROM_CLIPBOARD);
        // Kill from the cursor to the previous whitespace
        else if(input == CTRL_W)
            return new Operation(Movement.PREV_BIG_WORD, Action.DELETE);

        //  Yank the most recently killed text back into the buffer at the cursor.
        else if(input == CTRL_Y)
            return new Operation(Movement.NEXT, Action.PASTE);
        else if(input == CR)
            return new Operation(Movement.BEGINNING, Action.MOVE);
        // search
        else if(input == CTRL_R) {
            mode = Action.SEARCH;
            return new Operation(Movement.PREV, Action.SEARCH);
        }
        else if(input == CTRL_S) {
            mode = Action.SEARCH;
            return new Operation(Movement.NEXT, Action.SEARCH);
        }

        //enter C-x state
        else if(input == CTRL_X) {
            ctrl_xState = true;
            return new Operation(Action.NO_ACTION);
        }

        // handle meta keys
        else if(input == F && arrowStart) {
            arrowStart = false;
            return new Operation(Movement.NEXT_WORD, Action.MOVE);
        }
        else if(input == B && arrowStart) {
            arrowStart = false;
            return new Operation(Movement.PREV_WORD, Action.MOVE);
        }
        else if(input == D && arrowStart) {
            arrowStart = false;
            return new Operation(Movement.NEXT_WORD, Action.DELETE);
        }


        // handle arrow keys
        else if(input == ESCAPE) {
            // if we've already gotten a escape
            if(arrowStart) {
                arrowStart = false;
                return new Operation(Action.NO_ACTION);
            }
            //new escape, set status as arrowStart
            if(!arrowPrefix && !arrowStart) {
                arrowStart = true;
                return new Operation(Action.NO_ACTION);
            }
        }
        else if(input == ARROW_START) {
            if(arrowStart) {
                arrowPrefix = true;
                return new Operation(Action.NO_ACTION);
            }
        }
        else if(input == UP) {
            if(arrowPrefix && arrowStart) {
                arrowPrefix = arrowStart = false;
                return new Operation(Movement.PREV, Action.HISTORY);
            }
        }
        else if(input == DOWN) {
            if(arrowPrefix && arrowStart) {
                arrowPrefix = arrowStart = false;
                return new Operation(Movement.NEXT, Action.HISTORY);
            }
        }
        else if(input == LEFT) {
            if(arrowPrefix && arrowStart) {
                arrowPrefix = arrowStart = false;
                return new Operation(Movement.PREV, Action.MOVE);
            }
        }
        else if(input == RIGHT) {
            if(arrowPrefix && arrowStart) {
                arrowPrefix = arrowStart = false;
                return new Operation(Movement.NEXT, Action.MOVE);
            }
        }
        //if we get down here, we can safely reset arrow status booleans
        arrowPrefix = arrowStart = false;

        return new Operation(Action.EDIT);
    }

    @Override
    public Action getCurrentAction() {
        return mode;
    }

}
