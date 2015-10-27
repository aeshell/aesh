/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        return "accept-line";
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
