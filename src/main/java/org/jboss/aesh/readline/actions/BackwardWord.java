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

import org.jboss.aesh.console.InputProcessor;
import org.jboss.aesh.readline.editing.EditMode;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
abstract class BackwardWord extends ChangeAction {

    private boolean viMode;

    public BackwardWord() {
        super(EditMode.Status.MOVE);
        this.viMode = false;
    }

    public BackwardWord(boolean viMode, EditMode.Status status) {
        super(status);
        this.viMode = viMode;
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
                while(cursor > 0 && isDelimiter(buffer.charAt(cursor - 1)) && !isSpace(buffer.charAt(cursor-1)))
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

        apply(cursor, inputProcessor);
    }

}
