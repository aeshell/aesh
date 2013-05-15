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

import java.util.ArrayList;
import java.util.List;

/**
 * Bind default key codes to KeyOperations
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class KeyOperationFactory {

    public static List<KeyOperation> generatePOSIXEmacsMode() {
        List<KeyOperation> keys = generateGenericEmacsKeys();

        keys.add(new KeyOperation(10, Operation.NEW_LINE));

        keys.add(new KeyOperation(31, Operation.UNDO));
        keys.add(new KeyOperation(127, Operation.DELETE_PREV_CHAR));

        //movement
        keys.add(new KeyOperation(new int[]{27,91,65}, Operation.HISTORY_PREV));   //arrow up
        keys.add(new KeyOperation(new int[]{27,91,66}, Operation.HISTORY_NEXT));   //arrow down
        keys.add(new KeyOperation(new int[]{27,91,67}, Operation.MOVE_NEXT_CHAR)); //arrow right
        keys.add(new KeyOperation(new int[]{27,91,68}, Operation.MOVE_PREV_CHAR)); //arrow left

        keys.add(new KeyOperation(new int[]{27,79,65}, Operation.HISTORY_PREV));   //arrow up
        keys.add(new KeyOperation(new int[]{27,79,66}, Operation.HISTORY_NEXT));   //arrow down
        keys.add(new KeyOperation(new int[]{27,79,67}, Operation.MOVE_NEXT_CHAR)); //arrow right
        keys.add(new KeyOperation(new int[]{27,79,68}, Operation.MOVE_PREV_CHAR)); //arrow left

        //meta
        keys.add(new KeyOperation(new int[]{27,102}, Operation.MOVE_NEXT_WORD));   //meta-f
        keys.add(new KeyOperation(new int[]{27,98}, Operation.MOVE_PREV_WORD));    //meta-b
        keys.add(new KeyOperation(new int[]{27,100}, Operation.DELETE_NEXT_WORD)); //meta-d

        //pgup, pgdown, end, home, delete
        keys.add(new KeyOperation(new int[]{27,91,51,126}, Operation.DELETE_NEXT_CHAR)); //Delete
        keys.add(new KeyOperation(new int[]{27,91,53,126}, Operation.PGUP));   //pgup
        keys.add(new KeyOperation(new int[]{27,91,54,126}, Operation.PGDOWN));   //pgdown
        keys.add(new KeyOperation(new int[]{27,79,72}, Operation.MOVE_BEGINNING));  //home
        keys.add(new KeyOperation(new int[]{27,79,70}, Operation.MOVE_END));        //end

        keys.add(new KeyOperation(new int[] {27,10}, Operation.VI_EDIT_MODE)); //meta-ctrl-j

        return keys;
    }

    public static List<KeyOperation> generateWindowsEmacsMode() {
        List<KeyOperation> keys = generateGenericEmacsKeys();
        //keys.add(new KeyOperation(3, Operation.EXIT)); //ctrl-c
        keys.add(new KeyOperation(8, Operation.DELETE_PREV_CHAR)); // backspace
        keys.add(new KeyOperation(13, Operation.NEW_LINE));

        //movement
        keys.add(new KeyOperation(new int[]{224,72}, Operation.HISTORY_PREV));   //arrow up
        keys.add(new KeyOperation(new int[]{224,80}, Operation.HISTORY_NEXT));   //arrow down
        keys.add(new KeyOperation(new int[]{224,77}, Operation.MOVE_NEXT_CHAR)); //arrow right
        keys.add(new KeyOperation(new int[]{224,75}, Operation.MOVE_PREV_CHAR)); //arrow left

        //meta, alt gr on windows
        keys.add(new KeyOperation(new int[]{0,33}, Operation.MOVE_NEXT_WORD));   //meta-f
        keys.add(new KeyOperation(new int[]{0,48}, Operation.MOVE_PREV_WORD));    //meta-b
        keys.add(new KeyOperation(new int[]{0,32}, Operation.DELETE_NEXT_WORD)); //meta-d

        //pgup, pgdown, end, home
        keys.add(new KeyOperation(new int[]{224,83}, Operation.DELETE_NEXT_CHAR)); //Delete
        keys.add(new KeyOperation(new int[]{224,73}, Operation.PGUP));   //pgup
        keys.add(new KeyOperation(new int[]{224,81}, Operation.PGDOWN));   //pgdown
        keys.add(new KeyOperation(new int[]{224,71}, Operation.MOVE_BEGINNING));  //home
        keys.add(new KeyOperation(new int[]{224,79}, Operation.MOVE_END));        //end

        //div
        keys.add(new KeyOperation(new int[]{224,83}, Operation.DELETE_NEXT_CHAR)); //delete

        keys.add(new KeyOperation(new int[] {0,36}, Operation.VI_EDIT_MODE)); //meta-ctrl-j

        return keys;
    }

    private static List<KeyOperation> generateGenericEmacsKeys() {
        List<KeyOperation> keys = new ArrayList<KeyOperation>();
        keys.add(new KeyOperation(1, Operation.MOVE_BEGINNING));
        keys.add(new KeyOperation(2, Operation.MOVE_PREV_CHAR));
        //ctrl-d, if pressed on a line with chars it will cause the
        //action delete_next_char else exit
        keys.add(new KeyOperation(4, Operation.EXIT));
        keys.add(new KeyOperation(5, Operation.MOVE_END));
        keys.add(new KeyOperation(6, Operation.MOVE_NEXT_CHAR));
        keys.add(new KeyOperation(7, Operation.ABORT));
        keys.add(new KeyOperation(8, Operation.DELETE_PREV_CHAR));
        keys.add(new KeyOperation(9, Operation.COMPLETE));
        keys.add(new KeyOperation(11, Operation.DELETE_END));
        keys.add(new KeyOperation(12, Operation.CLEAR));
        keys.add(new KeyOperation(14, Operation.HISTORY_NEXT));
        keys.add(new KeyOperation(16, Operation.HISTORY_PREV));
        keys.add(new KeyOperation(18, Operation.SEARCH_PREV));
        keys.add(new KeyOperation(19, Operation.SEARCH_NEXT_WORD));
        keys.add(new KeyOperation(21, Operation.DELETE_BEGINNING));
        keys.add(new KeyOperation(22, Operation.PASTE_FROM_CLIPBOARD));
        keys.add(new KeyOperation(23, Operation.DELETE_PREV_BIG_WORD));
        //keys.add(new KeyOperation(24, Operation.NO_ACTION)); ctrl-x
        keys.add(new KeyOperation(25, Operation.PASTE_BEFORE));

        keys.add(new KeyOperation(new int[] {24,21}, Operation.UNDO)); //ctrl-x ctrl-u

        return keys;
    }

    public static List<KeyOperation> generatePOSIXViMode() {
        List<KeyOperation> keys = generateGenericViMode();
        keys.add(new KeyOperation(10, Operation.NEW_LINE));

        //movement
        keys.add(new KeyOperation(new int[]{27,91,65}, Operation.HISTORY_PREV, Action.EDIT));   //arrow up
        keys.add(new KeyOperation(new int[]{27,91,66}, Operation.HISTORY_NEXT, Action.EDIT));   //arrow down
        keys.add(new KeyOperation(new int[]{27,91,67}, Operation.MOVE_NEXT_CHAR, Action.EDIT)); //arrow right
        keys.add(new KeyOperation(new int[]{27,91,68}, Operation.MOVE_PREV_CHAR, Action.EDIT)); //arrow left

        keys.add(new KeyOperation(new int[]{27,79,65}, Operation.HISTORY_PREV, Action.EDIT));   //arrow up
        keys.add(new KeyOperation(new int[]{27,79,66}, Operation.HISTORY_NEXT, Action.EDIT));   //arrow down
        keys.add(new KeyOperation(new int[]{27,79,67}, Operation.MOVE_NEXT_CHAR, Action.EDIT)); //arrow right
        keys.add(new KeyOperation(new int[]{27,79,68}, Operation.MOVE_PREV_CHAR, Action.EDIT)); //arrow left

        keys.add(new KeyOperation(new int[]{27,91,51,126}, Operation.DELETE_NEXT_CHAR, Action.COMMAND)); //Delete

        keys.add(new KeyOperation(new int[]{27,91,53,126}, Operation.PGUP));   //pgup
        keys.add(new KeyOperation(new int[]{27,91,54,126}, Operation.PGDOWN));   //pgdown

        return keys;
    }


    public static List<KeyOperation> generateWindowsViMode() {
        List<KeyOperation> keys = generateGenericViMode();
        keys.add(new KeyOperation(13, Operation.NEW_LINE));
        keys.add(new KeyOperation(new int[]{224,83}, Operation.DELETE_NEXT_CHAR, Action.COMMAND)); //Delete

        keys.add(new KeyOperation(new int[]{224,73}, Operation.PGUP));   //pgup
        keys.add(new KeyOperation(new int[]{224,81}, Operation.PGDOWN));   //pgdown

        return keys;
    }


    private static List<KeyOperation> generateGenericViMode() {
        List<KeyOperation> keys = new ArrayList<KeyOperation>();
        //ctrl-d, if pressed on a line with chars it will cause the
        //action delete_next_char else exit
        keys.add(new KeyOperation(4, Operation.EXIT));
        keys.add(new KeyOperation(5, Operation.EMACS_EDIT_MODE)); //ctrl-e
        keys.add(new KeyOperation(9, Operation.COMPLETE)); //tab
        keys.add(new KeyOperation(12, Operation.CLEAR)); //ctrl-l

        //search
        keys.add(new KeyOperation(18, Operation.SEARCH_PREV));

        //edit
        keys.add(new KeyOperation(27, Operation.ESCAPE)); //escape
        keys.add(new KeyOperation(115, Operation.CHANGE_NEXT_CHAR)); //s
        keys.add(new KeyOperation(83, Operation.CHANGE_ALL)); //S
        keys.add(new KeyOperation(100, Operation.DELETE_ALL)); //d
        keys.add(new KeyOperation(68, Operation.DELETE_END)); //D
        keys.add(new KeyOperation(99, Operation.CHANGE)); //c
        keys.add(new KeyOperation(67, Operation.CHANGE_END)); //C
        keys.add(new KeyOperation(97, Operation.MOVE_NEXT_CHAR)); //a
        keys.add(new KeyOperation(65, Operation.MOVE_END)); //A
        keys.add(new KeyOperation(48, Operation.BEGINNING)); //0
        keys.add(new KeyOperation(36, Operation.END)); //$
        keys.add(new KeyOperation(120, Operation.DELETE_NEXT_CHAR)); //x
        keys.add(new KeyOperation(88, Operation.DELETE_PREV_CHAR, Action.COMMAND)); //X
        keys.add(new KeyOperation(112, Operation.PASTE_AFTER)); //p
        keys.add(new KeyOperation(80, Operation.PASTE_BEFORE)); //P
        keys.add(new KeyOperation(105, Operation.INSERT)); //i
        keys.add(new KeyOperation(73, Operation.INSERT_BEGINNING)); //I
        keys.add(new KeyOperation(126, Operation.CASE)); //~
        keys.add(new KeyOperation(121, Operation.YANK_ALL)); //y

        //replace
        keys.add(new KeyOperation(114, Operation.REPLACE)); //r

        //movement
        keys.add(new KeyOperation(104, Operation.PREV_CHAR)); //h
        keys.add(new KeyOperation(108, Operation.NEXT_CHAR)); //l
        keys.add(new KeyOperation(106, Operation.HISTORY_NEXT)); //j
        keys.add(new KeyOperation(107, Operation.HISTORY_PREV)); //k
        keys.add(new KeyOperation(98, Operation.PREV_WORD)); //b
        keys.add(new KeyOperation(66, Operation.PREV_BIG_WORD)); //B
        keys.add(new KeyOperation(119, Operation.NEXT_WORD)); //w
        keys.add(new KeyOperation(87, Operation.NEXT_BIG_WORD)); //W
        keys.add(new KeyOperation(32, Operation.NEXT_CHAR)); //space

        //repeat
        keys.add(new KeyOperation(46, Operation.REPEAT)); //.
        //undo
        keys.add(new KeyOperation(117, Operation.UNDO)); //u
        //backspace
        keys.add(new KeyOperation(127, Operation.DELETE_PREV_CHAR));

        return keys;
    }

    public static KeyOperation findOperation(List<KeyOperation> operations, int[] input) {
        for(KeyOperation operation : operations) {
            if(operation.equalValues(input))
                return operation;
        }
        return null;

    }

    public static boolean containNewLine(int[] input) {
        int newLine = getNewLine();
        for(int i : input)
            if(i == newLine)
                return true;
        return false;
    }

    public static int getNewLine() {
        return Config.isOSPOSIXCompatible() ? 10 : 13;
    }
}
