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

import org.jboss.jreadline.edit.actions.Operation;

import java.util.ArrayList;
import java.util.List;

/**
 * Bind key codes to KeyOperations
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class KeyOperationManager {

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

        //meta
        keys.add(new KeyOperation(new int[]{27,102}, Operation.MOVE_NEXT_WORD));   //meta-f
        keys.add(new KeyOperation(new int[]{27,98}, Operation.MOVE_PREV_WORD));    //meta-b
        keys.add(new KeyOperation(new int[]{27,100}, Operation.DELETE_NEXT_WORD)); //meta-d

        return keys;
    }

    public static List<KeyOperation> generateWindowsEmacsMode() {
        List<KeyOperation> keys = generateGenericEmacsKeys();
        keys.add(new KeyOperation(3, Operation.EXIT));
        keys.add(new KeyOperation(13, Operation.NEW_LINE));

        //movement
        keys.add(new KeyOperation(new int[]{224,72}, Operation.HISTORY_PREV));   //arrow up
        keys.add(new KeyOperation(new int[]{224,80}, Operation.HISTORY_NEXT));   //arrow down
        keys.add(new KeyOperation(new int[]{224,77}, Operation.MOVE_NEXT_CHAR)); //arrow right
        keys.add(new KeyOperation(new int[]{224,75}, Operation.MOVE_PREV_CHAR)); //arrow left

        //div
        keys.add(new KeyOperation(new int[]{224,83}, Operation.DELETE_NEXT_CHAR)); //delete

        return keys;
    }

    private static List<KeyOperation> generateGenericEmacsKeys() {
        List<KeyOperation> keys = new ArrayList<KeyOperation>();
        keys.add(new KeyOperation(1, Operation.MOVE_BEGINNING));
        keys.add(new KeyOperation(2, Operation.MOVE_PREV_CHAR));
        keys.add(new KeyOperation(4, Operation.DELETE_NEXT_CHAR));
        keys.add(new KeyOperation(5, Operation.MOVE_END));
        keys.add(new KeyOperation(6, Operation.MOVE_NEXT_CHAR));
        keys.add(new KeyOperation(7, Operation.ABORT));
        keys.add(new KeyOperation(8, Operation.DELETE_PREV_CHAR));
        keys.add(new KeyOperation(9, Operation.COMPLETE));
        keys.add(new KeyOperation(11, Operation.DELETE_END));
        keys.add(new KeyOperation(12, Operation.DELETE_ALL));
        keys.add(new KeyOperation(14, Operation.HISTORY_NEXT));
        keys.add(new KeyOperation(16, Operation.HISTORY_PREV));
        keys.add(new KeyOperation(18, Operation.SEARCH_PREV));
        keys.add(new KeyOperation(19, Operation.SEARCH_NEXT_WORD));
        keys.add(new KeyOperation(21, Operation.DELETE_BEGINNING));
        keys.add(new KeyOperation(22, Operation.PASTE_FROM_CLIPBOARD));
        keys.add(new KeyOperation(23, Operation.DELETE_PREV_BIG_WORD));
        keys.add(new KeyOperation(24, Operation.NO_ACTION));
        keys.add(new KeyOperation(25, Operation.PASTE_BEFORE));

        return keys;
    }

    public static List<KeyOperation> generatePOSIXViMode() {
        List<KeyOperation> keys = new ArrayList<KeyOperation>();
        keys.add(new KeyOperation(5, Operation.CHANGE_EDIT_MODE));
        keys.add(new KeyOperation(9, Operation.COMPLETE));
        keys.add(new KeyOperation(10, Operation.NEW_LINE));

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
        keys.add(new KeyOperation(112, Operation.PASTE_AFTER)); //p
        keys.add(new KeyOperation(80, Operation.PASTE_BEFORE)); //P
        keys.add(new KeyOperation(105, Operation.INSERT)); //i
        keys.add(new KeyOperation(73, Operation.INSERT_BEGINNING)); //I
        keys.add(new KeyOperation(126, Operation.CASE)); //~
        keys.add(new KeyOperation(121, Operation.YANK_ALL)); //y

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


}
