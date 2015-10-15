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
public class DeletePrevChar implements Action {
    @Override
    public String name() {
        return "delete-prev-char";
    }

    @Override
    public void apply(InputProcessor inputProcessor) {
        if(inputProcessor.getBuffer().getBuffer().isMasking()) {
            if(inputProcessor.getBuffer().getBuffer().getPrompt().getMask() == 0) {
                deleteWithMaskEnabled(inputProcessor.getBuffer());
                return;
            }
        }
        deleteNoMasking(inputProcessor.getBuffer());
    }

    private void deleteNoMasking(ConsoleBuffer consoleBuffer) {
        int cursor = consoleBuffer.getBuffer().getMultiCursor();
        if(cursor > 0) {
            int lineSize = consoleBuffer.getBuffer().getLine().length();
            if(cursor > lineSize)
                cursor = lineSize;

            consoleBuffer.getPasteManager().addText(new StringBuilder(
                    consoleBuffer.getBuffer().getLine().substring(cursor - 1, cursor)));
            consoleBuffer.getBuffer().delete(cursor - 1, cursor);
            consoleBuffer.moveCursor(-1);
            consoleBuffer.drawLine();
        }
    }

    private void deleteWithMaskEnabled(ConsoleBuffer consoleBuffer) {
        if(consoleBuffer.getBuffer().getLineNoMask().length() > 0) {
            consoleBuffer.getBuffer().delete(consoleBuffer.getBuffer().getLineNoMask().length() - 1,
                    consoleBuffer.getBuffer().getLineNoMask().length());
            consoleBuffer.moveCursor(-1);
            consoleBuffer.drawLine();
        }
    }
}
