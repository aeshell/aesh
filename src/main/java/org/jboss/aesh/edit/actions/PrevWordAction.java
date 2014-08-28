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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
