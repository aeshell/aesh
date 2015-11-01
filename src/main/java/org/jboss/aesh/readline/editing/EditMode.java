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
package org.jboss.aesh.readline.editing;

import org.jboss.aesh.readline.Action;
import org.jboss.aesh.readline.KeyEvent;
import org.jboss.aesh.readline.Variable;
import org.jboss.aesh.terminal.Key;

import java.util.Arrays;

/**
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface EditMode {

    Action parse(KeyEvent event);

    void updateIgnoreEOF(int eof);

    void addVariable(Variable variable, String value);

    void addAction(int[] input, String action);

    default KeyEvent createKeyEvent(int[] input) {
        Key key = Key.getKey(input);
        if(key != null)
            return key;
        else {
            return new KeyEvent() {
                private int[] key = input;

                @Override
                public int getCodePointAt(int index) throws IndexOutOfBoundsException {
                    return key[index];
                }

                @Override
                public int length() {
                    return key.length;
                }

                @Override
                public String name() {
                    return Arrays.toString(key);
                }
            };
        }
    }

    enum Status {
        DELETE,
        MOVE,
        YANK,
        CHANGE,
        EDIT,
        COMMAND,
        SEARCH,
        UP_CASE,
        DOWN_CASE,
        CAPITALIZE,
        IGNORE_EOF,
        COMPLETE
    }

    enum Mode {
        EMACS, VI
    }
}
