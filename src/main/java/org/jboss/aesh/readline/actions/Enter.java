/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.readline.actions;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.ConsoleBuffer;
import org.jboss.aesh.console.InputProcessor;
import org.jboss.aesh.parser.Parser;
import org.jboss.aesh.readline.Action;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Enter implements Action {

    private static final String ENDS_WITH_BACKSLASH = " \\";

    @Override
    public String name() {
        return "enter";
    }

    @Override
    public void apply(InputProcessor inputProcessor) {
        ConsoleBuffer consoleBuffer = inputProcessor.getBuffer();
        consoleBuffer.getUndoManager().clear();
        boolean isCurrentLineEnding = true;
        if(!consoleBuffer.getBuffer().isMasking()) {// dont push to history if masking

            //dont push lines that end with \ to history
            if(consoleBuffer.getBuffer().getLine().endsWith(ENDS_WITH_BACKSLASH)) {
                consoleBuffer.getBuffer().setMultiLine(true);
                consoleBuffer.getBuffer().updateMultiLineBuffer();
                isCurrentLineEnding = false;
            }
            else if(Parser.doesStringContainOpenQuote(consoleBuffer.getBuffer().getMultiLine())) {
                consoleBuffer.getBuffer().setMultiLine(true);
                consoleBuffer.getBuffer().updateMultiLineBuffer();
                isCurrentLineEnding = false;
            }
            else if( inputProcessor.getHistory().isEnabled()) {
                if(consoleBuffer.getBuffer().isMultiLine())
                   inputProcessor.getHistory().push(consoleBuffer.getBuffer().getMultiLineBuffer() + consoleBuffer.getBuffer().getLine());
                else
                    inputProcessor.getHistory().push(consoleBuffer.getBuffer().getLine());
            }
        }

        consoleBuffer.moveCursor(consoleBuffer.getBuffer().totalLength());
        consoleBuffer.out().print(Config.getLineSeparator());

        String result;
        if(consoleBuffer.getBuffer().isMultiLine()) {
            result = consoleBuffer.getBuffer().getMultiLineBuffer() + consoleBuffer.getBuffer().getLineNoMask();
        }
        else
            result = consoleBuffer.getBuffer().getLineNoMask();
        //search = null;
        if(isCurrentLineEnding) {
            consoleBuffer.getBuffer().setMultiLine(false);
            consoleBuffer.getBuffer().reset();
            inputProcessor.setReturnValue(result);
        }
        else
            consoleBuffer.displayPrompt();

    }
}
