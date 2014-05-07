/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit.actions;

import org.jboss.aesh.edit.Mode;

/**
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class NextWordAction extends EditAction {

    private final Mode mode;
    private boolean removeTrailingSpaces = true;

    public NextWordAction(int start, Action action, Mode mode) {
        super(start, action);
        this.mode = mode;
        if(getAction() == Action.CHANGE)
            removeTrailingSpaces = false;
    }

    @Override
    public void doAction(String buffer) {
        int cursor = getStart();

        //if cursor stand on a delimiter, move till its no more delimiters
        if(mode == Mode.EMACS) {
            while (cursor < buffer.length() && (isDelimiter(buffer.charAt(cursor))))
                cursor++;
            while (cursor < buffer.length() && !isDelimiter(buffer.charAt(cursor)))
                cursor++;
        }
        //vi mode
        else {
            if(cursor < buffer.length() && (isDelimiter(buffer.charAt(cursor))))
                while(cursor < buffer.length() && (isDelimiter(buffer.charAt(cursor))))
                    cursor++;
                //if we stand on a non-delimiter
            else {
                while(cursor < buffer.length() && !isDelimiter(buffer.charAt(cursor)))
                    cursor++;
                //if we end up on a space we move past that too
                if(removeTrailingSpaces)
                    if(cursor < buffer.length() && isSpace(buffer.charAt(cursor)))
                        while(cursor < buffer.length() && isSpace(buffer.charAt(cursor)))
                            cursor++;
            }

        }

        //if we end up on a space we move past that too
        if(removeTrailingSpaces)
            if(cursor < buffer.length() && isSpace(buffer.charAt(cursor)))
                while(cursor < buffer.length() && isSpace(buffer.charAt(cursor)))
                    cursor++;

        setEnd(cursor);
    }
}
