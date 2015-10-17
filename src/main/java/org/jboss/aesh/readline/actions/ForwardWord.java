/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.readline.actions;

import org.jboss.aesh.console.InputProcessor;
import org.jboss.aesh.readline.editing.EditMode;

/**
 * TODO: change boolean params in constructors to objects/enum
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ForwardWord extends MovementAction {

    private boolean viMode;
    private boolean removeTrailingSpaces;
    private EditMode.Status status;

    public ForwardWord() {
        viMode = false;
    }

    public ForwardWord(boolean viMode, EditMode.Status status) {
        this.viMode = viMode;
        this.status = status;
        if(status == EditMode.Status.CHANGE)
            this.removeTrailingSpaces = false;
    }

    @Override
    public String name() {
        return "forward-word";
    }

    @Override
    public void apply(InputProcessor inputProcessor) {
        int cursor = inputProcessor.getBuffer().getBuffer().getMultiCursor();
        String buffer = inputProcessor.getBuffer().getBuffer().getLine();

        if(viMode) {
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
        else {
            while (cursor < buffer.length() && (isDelimiter(buffer.charAt(cursor))))
                cursor++;
            while (cursor < buffer.length() && !isDelimiter(buffer.charAt(cursor)))
                cursor++;
        }

        //if we end up on a space we move past that too
        if(removeTrailingSpaces)
            if(cursor < buffer.length() && isSpace(buffer.charAt(cursor)))
                while(cursor < buffer.length() && isSpace(buffer.charAt(cursor)))
                    cursor++;

        if(status == EditMode.Status.DELETE) {

        }
        else if(status == EditMode.Status.CHANGE) {

        }
        else if(status == EditMode.Status.MOVE) {
            inputProcessor.getBuffer().moveCursor(cursor-inputProcessor.getBuffer().getBuffer().getMultiCursor());

        }
    }
}
