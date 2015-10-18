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
 * TODO: change boolean params in constructors to objects/enum
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ForwardWord extends ChangeAction {

    private boolean viMode;
    private boolean removeTrailingSpaces;

    public ForwardWord() {
        super(EditMode.Status.MOVE);
        viMode = false;
    }

    public ForwardWord(boolean viMode, EditMode.Status status) {
        super(status);
        this.viMode = viMode;
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

        apply(cursor, inputProcessor);
    }
}
