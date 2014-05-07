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
public class PrevWordAction extends EditAction {

    private final Mode mode;

    public PrevWordAction(int start, Action action, Mode mode) {
        super(start, action);
        this.mode = mode;
    }

    @Override
    public void doAction(String buffer) {
        int cursor = getStart();
        //the cursor position might be > the buffer
        if(cursor > buffer.length())
            cursor = buffer.length()-1;

        //move back every space
        if(mode == Mode.EMACS) {
            while (cursor > 0 && isDelimiter(buffer.charAt(cursor - 1)))
                cursor--;
            while (cursor > 0 && !isDelimiter(buffer.charAt(cursor - 1)))
                cursor--;
        }
        else {
            while(cursor > 0 && isSpace(buffer.charAt(cursor-1)))
                cursor--;
            if(cursor > 0 && isDelimiter(buffer.charAt(cursor-1))) {
                while(cursor > 0 && isDelimiter(buffer.charAt(cursor-1)))
                    cursor--;
            }
            else {
                while(cursor > 0 && !isDelimiter(buffer.charAt(cursor-1))) {
                    cursor--;
                }
            }
        }

        setEnd(cursor);
    }
}
