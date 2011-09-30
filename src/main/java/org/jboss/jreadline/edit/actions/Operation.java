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
package org.jboss.jreadline.edit.actions;

/**
 * An Operation is an Action and a Movement
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public enum Operation {

    SEARCH_PREV(Movement.PREV, Action.SEARCH),
    SEARCH_END(Movement.END, Action.SEARCH),
    SEARCH_PREV_WORD(Movement.PREV_WORD, Action.SEARCH),
    SEARCH_NEXT_WORD(Movement.NEXT_WORD, Action.SEARCH),
    SEARCH_DELETE(Movement.PREV_BIG_WORD, Action.SEARCH),
    SEARCH_EXIT(Movement.NEXT_BIG_WORD, Action.SEARCH),
    SEARCH_INPUT(Movement.ALL, Action.SEARCH),
    NEW_LINE(Action.NEWLINE),
    NO_ACTION(Movement.PREV, Action.NO_ACTION),
    COMPLETE(Movement.NEXT, Action.COMPLETE),
    EDIT(Action.EDIT),
    HISTORY_NEXT(Movement.NEXT, Action.HISTORY),
    HISTORY_PREV(Movement.PREV, Action.HISTORY),
    PREV_CHAR(Movement.PREV, Action.NO_ACTION),
    NEXT_CHAR(Movement.NEXT, Action.NO_ACTION),
    NEXT_WORD(Movement.NEXT_WORD, Action.NO_ACTION),
    PREV_WORD(Movement.PREV_WORD, Action.NO_ACTION),
    NEXT_BIG_WORD(Movement.NEXT_BIG_WORD, Action.NO_ACTION),
    PREV_BIG_WORD(Movement.PREV_BIG_WORD, Action.NO_ACTION),
    MOVE_PREV_CHAR(Movement.PREV, Action.MOVE),
    MOVE_NEXT_CHAR(Movement.NEXT, Action.MOVE),
    MOVE_PREV_WORD(Movement.PREV_WORD, Action.MOVE),
    MOVE_PREV_BIG_WORD(Movement.PREV_BIG_WORD, Action.MOVE),
    MOVE_NEXT_WORD(Movement.NEXT_WORD, Action.MOVE),
    MOVE_NEXT_BIG_WORD(Movement.NEXT_BIG_WORD, Action.MOVE),
    MOVE_BEGINNING(Movement.BEGINNING, Action.MOVE),
    MOVE_END(Movement.END, Action.MOVE),
    DELETE_PREV_CHAR(Movement.PREV, Action.DELETE),
    DELETE_NEXT_CHAR(Movement.NEXT, Action.DELETE),
    DELETE_PREV_WORD(Movement.PREV_WORD, Action.DELETE),
    DELETE_PREV_BIG_WORD(Movement.PREV_BIG_WORD, Action.DELETE),
    DELETE_NEXT_WORD(Movement.NEXT_WORD, Action.DELETE),
    DELETE_NEXT_BIG_WORD(Movement.NEXT_BIG_WORD, Action.DELETE),
    DELETE_BEGINNING(Movement.BEGINNING, Action.DELETE),
    DELETE_END(Movement.END, Action.DELETE),
    DELETE_ALL(Movement.ALL, Action.DELETE),
    CHANGE_PREV_CHAR(Movement.PREV, Action.CHANGE),
    CHANGE_NEXT_CHAR(Movement.NEXT, Action.CHANGE),
    CHANGE_PREV_WORD(Movement.PREV_WORD, Action.CHANGE),
    CHANGE_PREV_BIG_WORD(Movement.PREV_BIG_WORD, Action.CHANGE),
    CHANGE_NEXT_WORD(Movement.NEXT_WORD, Action.CHANGE),
    CHANGE_NEXT_BIG_WORD(Movement.NEXT_BIG_WORD, Action.CHANGE),
    CHANGE_BEGINNING(Movement.BEGINNING, Action.CHANGE),
    CHANGE_END(Movement.END, Action.CHANGE),
    CHANGE_ALL(Movement.ALL, Action.CHANGE),
    CHANGE(Action.NO_ACTION),
    YANK_PREV_CHAR(Movement.PREV, Action.YANK),
    YANK_NEXT_CHAR(Movement.NEXT, Action.YANK),
    YANK_PREV_WORD(Movement.PREV_WORD, Action.YANK),
    YANK_PREV_BIG_WORD(Movement.PREV_BIG_WORD, Action.YANK),
    YANK_NEXT_WORD(Movement.NEXT_WORD, Action.YANK),
    YANK_NEXT_BIG_WORD(Movement.NEXT_BIG_WORD, Action.YANK),
    YANK_BEGINNING(Movement.BEGINNING, Action.YANK),
    YANK_END(Movement.END, Action.YANK),
    YANK_ALL(Movement.ALL, Action.YANK),
    BEGINNING(Movement.BEGINNING, Action.NO_ACTION), //used for '0' in vi mode
    END(Movement.BEGINNING, Action.NO_ACTION),       // used for '$' in vi mode
    INSERT(Action.NO_ACTION),                        // used for 'i' in vi mode
    INSERT_BEGINNING(Action.NO_ACTION),              // used for 'I' in vi mode
    ESCAPE(Action.NO_ACTION),                        // escape

    PASTE_BEFORE(Movement.NEXT, Action.PASTE),
    PASTE_AFTER(Movement.PREV, Action.PASTE),
    PASTE_FROM_CLIPBOARD(Movement.NEXT, Action.PASTE_FROM_CLIPBOARD),
    UNDO(Action.UNDO),
    CASE(Action.CASE),
    ABORT(Action.ABORT),
    REPEAT(Action.NO_ACTION),
    EXIT(Action.EXIT),
    CHANGE_EDIT_MODE(Movement.PREV, Action.CHANGE_EDITMODE);

    private Movement movement;
    private Action action;

    Operation(Action action) {
        this.action = action;
    }

    Operation(Movement movement, Action action) {
        this.movement = movement;
        this.action = action;
    }

    public Movement getMovement() {
        return movement;
    }

    public Action getAction() {
        return action;
    }
}
