/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.readline.actions;

import org.jboss.aesh.console.ConsoleBuffer;
import org.jboss.aesh.console.InputProcessor;
import org.jboss.aesh.readline.Action;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */

public class DeleteChar implements Action {

    @Override
    public String name() {
        return null;
    }

    @Override
    public void apply(InputProcessor inputProcessor) {
        deleteNoMasking(inputProcessor.getBuffer());
    }

    private void deleteNoMasking(ConsoleBuffer consoleBuffer) {
        int cursor = consoleBuffer.getBuffer().getMultiCursor();
        int lineSize = consoleBuffer.getBuffer().getLine().length();
        if(cursor < lineSize) {
            consoleBuffer.addActionToUndoStack();
            consoleBuffer.getPasteManager().addText(new StringBuilder(
                    consoleBuffer.getBuffer().getLine().substring(cursor, cursor+1)));
            consoleBuffer.getBuffer().delete(cursor, cursor+1);
            consoleBuffer.drawLine();
        }
    }
}