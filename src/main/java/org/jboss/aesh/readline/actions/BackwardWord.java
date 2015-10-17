/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.readline.actions;

import org.jboss.aesh.console.InputProcessor;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class BackwardWord extends MovementAction {

    private boolean viMode;

    public BackwardWord() {
        this.viMode = false;
    }

    public BackwardWord(boolean viMode) {
        this.viMode = viMode;
    }

    @Override
    public String name() {
        return "backward-word";
    }

    @Override
    public void apply(InputProcessor inputProcessor) {
        int cursor = inputProcessor.getBuffer().getBuffer().getMultiCursor();
        //the cursor position might be > the buffer
        if(cursor > inputProcessor.getBuffer().getBuffer().getLine().length())
            cursor = inputProcessor.getBuffer().getBuffer().getLine().length() - 1;

        if(viMode) {
            String buffer = inputProcessor.getBuffer().getBuffer().getLine();
            while(cursor > 0 && isSpace(buffer.charAt(cursor - 1)))
                cursor--;
            if(cursor > 0 && isDelimiter(buffer.charAt(cursor - 1))) {
                while(cursor > 0 && isDelimiter(buffer.charAt(cursor - 1)))
                    cursor--;
            }
            else {
                while(cursor > 0 && !isDelimiter(buffer.charAt(cursor - 1))) {
                    cursor--;
                }
            }
        }
        else {
            String buffer = inputProcessor.getBuffer().getBuffer().getLine();
            while (cursor > 0 && isDelimiter(buffer.charAt(cursor - 1)))
                cursor--;
            while (cursor > 0 && !isDelimiter(buffer.charAt(cursor - 1)))
                cursor--;
        }

        inputProcessor.getBuffer().moveCursor(cursor-inputProcessor.getBuffer().getBuffer().getMultiCursor());
    }

}
