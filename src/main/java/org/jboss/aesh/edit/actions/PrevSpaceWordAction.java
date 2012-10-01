/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit.actions;

/**
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class PrevSpaceWordAction extends EditAction {

    public PrevSpaceWordAction(int start, Action action) {
        super(start, action);
    }

    @Override
    public void doAction(String buffer) {
        int cursor = getStart();

        //the cursor position in jline might be > the buffer
        if(cursor > buffer.length())
            cursor = buffer.length()-1;

        //move back every potential space first
        while(cursor > 0 && isSpace(buffer.charAt(cursor-1)))
            cursor--;

        while(cursor > 0 && !isSpace(buffer.charAt(cursor-1)))
            cursor--;

        setEnd(cursor);
    }
}
