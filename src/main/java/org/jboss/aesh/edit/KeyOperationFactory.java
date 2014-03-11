/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.edit.actions.Action;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;

import java.util.ArrayList;
import java.util.List;

/**
 * Bind default key codes to KeyOperations
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class KeyOperationFactory {

    public static List<KeyOperation> generateEmacsMode() {
        List<KeyOperation> keys = new ArrayList<>();

        if(Key.ENTER.equalTo(Key.ENTER_2.getKeyValues())) {
            keys.add(new KeyOperation(Key.ENTER, Operation.NEW_LINE));
        }
        else {
            keys.add(new KeyOperation(Key.ENTER, Operation.NEW_LINE));
            keys.add(new KeyOperation(Key.ENTER_2, Operation.NEW_LINE));
        }

        keys.add(new KeyOperation(Key.CTRL_A, Operation.MOVE_BEGINNING));
        keys.add(new KeyOperation(Key.CTRL_B, Operation.MOVE_PREV_CHAR));
        keys.add(new KeyOperation(Key.CTRL_C, Operation.INTERRUPT));
        //ctrl-d, if pressed on a line with chars it will cause the
        //action delete_next_char else exit
        keys.add(new KeyOperation(Key.CTRL_D, Operation.EOF));
        keys.add(new KeyOperation(Key.CTRL_E, Operation.MOVE_END));
        keys.add(new KeyOperation(Key.CTRL_F, Operation.MOVE_NEXT_CHAR));
        keys.add(new KeyOperation(Key.CTRL_G, Operation.ABORT));
        keys.add(new KeyOperation(Key.CTRL_H, Operation.DELETE_PREV_CHAR));
        keys.add(new KeyOperation(Key.CTRL_I, Operation.COMPLETE));
        keys.add(new KeyOperation(Key.CTRL_K, Operation.DELETE_END));
        keys.add(new KeyOperation(Key.CTRL_L, Operation.CLEAR));
        keys.add(new KeyOperation(Key.CTRL_N, Operation.HISTORY_NEXT));
        keys.add(new KeyOperation(Key.CTRL_P, Operation.HISTORY_PREV));
        keys.add(new KeyOperation(Key.CTRL_R, Operation.SEARCH_PREV));
        keys.add(new KeyOperation(Key.CTRL_S, Operation.SEARCH_NEXT_WORD));
        keys.add(new KeyOperation(Key.CTRL_U, Operation.DELETE_BEGINNING));
        keys.add(new KeyOperation(Key.CTRL_V, Operation.PASTE_FROM_CLIPBOARD));
        keys.add(new KeyOperation(Key.CTRL_W, Operation.DELETE_PREV_BIG_WORD));
        //keys.add(new KeyOperation(24, Operation.NO_ACTION)); ctrl-x
        keys.add(new KeyOperation(Key.CTRL_Y, Operation.PASTE_BEFORE));
        keys.add(new KeyOperation(Key.CTRL_Z, Operation.NO_ACTION));

        keys.add(new KeyOperation(Key.CTRL_X_CTRL_U, Operation.UNDO)); //ctrl-x ctrl-u

        keys.add(new KeyOperation(Key.UNIT_SEPARATOR, Operation.UNDO));
        if(Config.isOSPOSIXCompatible())
            keys.add(new KeyOperation(Key.BACKSPACE, Operation.DELETE_PREV_CHAR));

        //movement
        if(Key.RIGHT.equalTo(Key.RIGHT_2.getKeyValues())) {
            keys.add(new KeyOperation(Key.RIGHT, Operation.MOVE_NEXT_CHAR, Action.EDIT)); //arrow right
        }
        else {
            keys.add(new KeyOperation(Key.RIGHT, Operation.MOVE_NEXT_CHAR, Action.EDIT)); //arrow right
            keys.add(new KeyOperation(Key.RIGHT_2, Operation.MOVE_NEXT_CHAR, Action.EDIT)); //arrow right
        }
        if(Key.LEFT.equalTo(Key.LEFT_2.getKeyValues())) {
            keys.add(new KeyOperation(Key.LEFT, Operation.MOVE_PREV_CHAR, Action.EDIT)); //arrow left

        }
        else {
            keys.add(new KeyOperation(Key.LEFT, Operation.MOVE_PREV_CHAR, Action.EDIT)); //arrow left
            keys.add(new KeyOperation(Key.LEFT_2, Operation.MOVE_PREV_CHAR, Action.EDIT)); //arrow left
        }
        if(Key.UP.equalTo(Key.UP_2.getKeyValues())) {
            keys.add(new KeyOperation(Key.UP, Operation.HISTORY_PREV, Action.EDIT));   //arrow up
        }
        else {
            keys.add(new KeyOperation(Key.UP, Operation.HISTORY_PREV, Action.EDIT));   //arrow up
            keys.add(new KeyOperation(Key.UP_2, Operation.HISTORY_PREV, Action.EDIT));   //arrow up
        }
        if(Key.DOWN.equalTo(Key.DOWN_2.getKeyValues())) {
            keys.add(new KeyOperation(Key.DOWN, Operation.HISTORY_NEXT, Action.EDIT));   //arrow down
        }
        else {
            keys.add(new KeyOperation(Key.DOWN, Operation.HISTORY_NEXT, Action.EDIT));   //arrow down
            keys.add(new KeyOperation(Key.DOWN_2, Operation.HISTORY_NEXT, Action.EDIT));   //arrow down
        }

        //meta
        keys.add(new KeyOperation(Key.META_f, Operation.MOVE_NEXT_WORD));   //meta-f
        keys.add(new KeyOperation(Key.META_b, Operation.MOVE_PREV_WORD));    //meta-b
        keys.add(new KeyOperation(Key.META_c, Operation.CAPITALISE_WORD));    //meta-b
        keys.add(new KeyOperation(Key.META_d, Operation.DELETE_NEXT_WORD)); //meta-d
        keys.add(new KeyOperation(Key.META_u, Operation.UPPERCASE_WORD)); //meta-u
        keys.add(new KeyOperation(Key.META_l, Operation.LOWERCASE_WORD)); //meta-l
        keys.add(new KeyOperation(Key.META_BACKSPACE, Operation.DELETE_PREV_WORD)); //meta-backspace

        //pgup, pgdown, end, home, delete
        keys.add(new KeyOperation(Key.DELETE, Operation.DELETE_NEXT_CHAR, Action.EDIT)); //Delete
        keys.add(new KeyOperation(Key.PGUP, Operation.PGUP));   //pgup
        keys.add(new KeyOperation(Key.PGDOWN, Operation.PGDOWN));   //pgdown
        keys.add(new KeyOperation(Key.INSERT, Operation.NO_ACTION));        //insert
        keys.add(new KeyOperation(Key.END, Operation.MOVE_END));        //end
        keys.add(new KeyOperation(Key.HOME, Operation.MOVE_BEGINNING));  //home

        keys.add(new KeyOperation(Key.CTRL_DOWN, Operation.MOVE_BEGINNING, Action.EDIT));
        keys.add(new KeyOperation(Key.CTRL_UP, Operation.MOVE_END, Action.EDIT));
        keys.add(new KeyOperation(Key.CTRL_LEFT, Operation.MOVE_PREV_BIG_WORD, Action.EDIT));
        keys.add(new KeyOperation(Key.CTRL_RIGHT, Operation.MOVE_NEXT_BIG_WORD, Action.EDIT));

        keys.add(new KeyOperation(Key.META_CTRL_J, Operation.VI_EDIT_MODE)); //meta-ctrl-j

        //keys that we ignore
        keys.add(new KeyOperation(Key.FILE_SEPARATOR, Operation.NO_ACTION));
        keys.add(new KeyOperation(Key.GROUP_SEPARATOR, Operation.NO_ACTION));
        keys.add(new KeyOperation(Key.RECORD_SEPARATOR, Operation.NO_ACTION));
        keys.add(new KeyOperation(Key.UNIT_SEPARATOR, Operation.NO_ACTION));

        return keys;
    }

    public static List<KeyOperation> generateViMode() {
        List<KeyOperation> keys = new ArrayList<>();

        keys.add(new KeyOperation(Key.ENTER, Operation.NEW_LINE));
        if(Key.ENTER.equalTo(Key.ENTER_2.getKeyValues())) {
            keys.add(new KeyOperation(Key.ENTER, Operation.NEW_LINE));
        }
        else {
            keys.add(new KeyOperation(Key.ENTER, Operation.NEW_LINE));
            keys.add(new KeyOperation(Key.ENTER_2, Operation.NEW_LINE));
        }

        keys.add(new KeyOperation(Key.CTRL_C, Operation.INTERRUPT));
        //ctrl-d, if pressed on a line with chars it will cause the
        //action delete_next_char else exit
        keys.add(new KeyOperation(Key.CTRL_D, Operation.EOF));
        keys.add(new KeyOperation(Key.CTRL_E, Operation.EMACS_EDIT_MODE)); //ctrl-e
        keys.add(new KeyOperation(Key.CTRL_I, Operation.COMPLETE)); //tab
        keys.add(new KeyOperation(Key.CTRL_L, Operation.CLEAR)); //ctrl-l
        keys.add(new KeyOperation(Key.CTRL_Z, Operation.NO_ACTION));

        //search
        keys.add(new KeyOperation(Key.CTRL_R, Operation.SEARCH_PREV));

        //edit
        keys.add(new KeyOperation(Key.ESC, Operation.ESCAPE)); //escape
        keys.add(new KeyOperation(Key.s, Operation.CHANGE_NEXT_CHAR)); //s
        keys.add(new KeyOperation(Key.S, Operation.CHANGE_ALL)); //S
        keys.add(new KeyOperation(Key.d, Operation.DELETE_ALL)); //d
        keys.add(new KeyOperation(Key.D, Operation.DELETE_END)); //D
        keys.add(new KeyOperation(Key.c, Operation.CHANGE)); //c
        keys.add(new KeyOperation(Key.C, Operation.CHANGE_END)); //C
        keys.add(new KeyOperation(Key.a, Operation.MOVE_NEXT_CHAR)); //a
        keys.add(new KeyOperation(Key.A, Operation.MOVE_END)); //A
        keys.add(new KeyOperation(Key.ZERO, Operation.BEGINNING)); //0
        keys.add(new KeyOperation(Key.DOLLAR, Operation.END)); //$
        keys.add(new KeyOperation(Key.x, Operation.DELETE_NEXT_CHAR)); //x
        keys.add(new KeyOperation(Key.X, Operation.DELETE_PREV_CHAR, Action.COMMAND)); //X
        keys.add(new KeyOperation(Key.p, Operation.PASTE_AFTER)); //p
        keys.add(new KeyOperation(Key.P, Operation.PASTE_BEFORE)); //P
        keys.add(new KeyOperation(Key.i, Operation.INSERT)); //i
        keys.add(new KeyOperation(Key.I, Operation.INSERT_BEGINNING)); //I
        keys.add(new KeyOperation(Key.TILDE, Operation.CASE)); //~
        keys.add(new KeyOperation(Key.y, Operation.YANK_ALL)); //y

        //replace
        keys.add(new KeyOperation(Key.r, Operation.REPLACE)); //r

        //movement
        keys.add(new KeyOperation(Key.h, Operation.PREV_CHAR)); //h
        keys.add(new KeyOperation(Key.l, Operation.NEXT_CHAR)); //l
        keys.add(new KeyOperation(Key.j, Operation.HISTORY_NEXT)); //j
        keys.add(new KeyOperation(Key.k, Operation.HISTORY_PREV)); //k
        keys.add(new KeyOperation(Key.b, Operation.PREV_WORD)); //b
        keys.add(new KeyOperation(Key.B, Operation.PREV_BIG_WORD)); //B
        keys.add(new KeyOperation(Key.w, Operation.NEXT_WORD)); //w
        keys.add(new KeyOperation(Key.W, Operation.NEXT_BIG_WORD)); //W
        keys.add(new KeyOperation(Key.SPACE, Operation.NEXT_CHAR)); //space

        //repeat
        keys.add(new KeyOperation(Key.PERIOD, Operation.REPEAT)); //.
        //undo
        keys.add(new KeyOperation(Key.u, Operation.UNDO)); //u
        //backspace
        keys.add(new KeyOperation(Key.BACKSPACE, Operation.DELETE_PREV_CHAR));

        //movement
        if(Key.RIGHT.equalTo(Key.RIGHT_2.getKeyValues())) {
            keys.add(new KeyOperation(Key.RIGHT, Operation.MOVE_NEXT_CHAR, Action.EDIT)); //arrow right
        }
        else {
            keys.add(new KeyOperation(Key.RIGHT, Operation.MOVE_NEXT_CHAR, Action.EDIT)); //arrow right
            keys.add(new KeyOperation(Key.RIGHT_2, Operation.MOVE_NEXT_CHAR, Action.EDIT)); //arrow right
        }
        if(Key.LEFT.equalTo(Key.LEFT_2.getKeyValues())) {
            keys.add(new KeyOperation(Key.LEFT, Operation.MOVE_PREV_CHAR, Action.EDIT)); //arrow left

        }
        else {
            keys.add(new KeyOperation(Key.LEFT, Operation.MOVE_PREV_CHAR, Action.EDIT)); //arrow left
            keys.add(new KeyOperation(Key.LEFT_2, Operation.MOVE_PREV_CHAR, Action.EDIT)); //arrow left
        }
        if(Key.UP.equalTo(Key.UP_2.getKeyValues())) {
            keys.add(new KeyOperation(Key.UP, Operation.HISTORY_PREV, Action.EDIT));   //arrow up
        }
        else {
            keys.add(new KeyOperation(Key.UP, Operation.HISTORY_PREV, Action.EDIT));   //arrow up
            keys.add(new KeyOperation(Key.UP_2, Operation.HISTORY_PREV, Action.EDIT));   //arrow up
        }
        if(Key.DOWN.equalTo(Key.DOWN_2.getKeyValues())) {
            keys.add(new KeyOperation(Key.DOWN, Operation.HISTORY_NEXT, Action.EDIT));   //arrow down
        }
        else {
            keys.add(new KeyOperation(Key.DOWN, Operation.HISTORY_NEXT, Action.EDIT));   //arrow down
            keys.add(new KeyOperation(Key.DOWN_2, Operation.HISTORY_NEXT, Action.EDIT));   //arrow down
        }

        keys.add(new KeyOperation(Key.DELETE, Operation.DELETE_NEXT_CHAR, Action.EDIT)); //Delete
        keys.add(new KeyOperation(Key.PGUP, Operation.PGUP));   //pgup
        keys.add(new KeyOperation(Key.PGDOWN, Operation.PGDOWN));   //pgdown
        keys.add(new KeyOperation(Key.INSERT, Operation.NO_ACTION));        //insert
        keys.add(new KeyOperation(Key.HOME, Operation.MOVE_BEGINNING, Action.EDIT));  //home
        keys.add(new KeyOperation(Key.END, Operation.MOVE_END, Action.EDIT));        //end

        keys.add(new KeyOperation(Key.CTRL_DOWN, Operation.MOVE_BEGINNING, Action.EDIT));
        keys.add(new KeyOperation(Key.CTRL_UP, Operation.MOVE_END, Action.EDIT));
        keys.add(new KeyOperation(Key.CTRL_LEFT, Operation.MOVE_PREV_BIG_WORD, Action.EDIT));
        keys.add(new KeyOperation(Key.CTRL_RIGHT, Operation.MOVE_NEXT_BIG_WORD, Action.EDIT));

        //keys that we ignore
        keys.add(new KeyOperation(Key.FILE_SEPARATOR, Operation.NO_ACTION));
        keys.add(new KeyOperation(Key.GROUP_SEPARATOR, Operation.NO_ACTION));
        keys.add(new KeyOperation(Key.RECORD_SEPARATOR, Operation.NO_ACTION));
        keys.add(new KeyOperation(Key.UNIT_SEPARATOR, Operation.NO_ACTION));

        return keys;
    }

}
