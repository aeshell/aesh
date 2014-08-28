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
public class EditActionManager {

    public static EditAction parseAction(Operation operation, int cursor, int length, Mode mode) {

        if (operation.getAction() == Action.MOVE || operation.getAction() == Action.YANK) {
                switch (operation.getMovement()) {
                    case NEXT:
                        return new SimpleAction(cursor, operation.getAction(), cursor+1);
                    case PREV:
                        return new SimpleAction(cursor, operation.getAction(), cursor-1);
                    case NEXT_WORD:
                        return new NextWordAction(cursor, operation.getAction(), mode);
                    case NEXT_BIG_WORD:
                        return new NextSpaceWordAction(cursor, operation.getAction());
                    case PREV_WORD:
                        return new PrevWordAction(cursor, operation.getAction(), mode);
                    case PREV_BIG_WORD:
                        return new PrevSpaceWordAction(cursor, operation.getAction());
                    case BEGINNING:
                        return new SimpleAction(cursor, operation.getAction(), 0);
                    case END:
                        return new SimpleAction(cursor, operation.getAction(), length);
                    case ALL:
                        return new SimpleAction(0, operation.getAction(), length);
                }
        }

        else if(operation.getAction() == Action.DELETE || operation.getAction() == Action.CHANGE) {
            switch (operation.getMovement()) {
                case NEXT:
                    return new DeleteAction(cursor, operation.getAction());
                case PREV:
                    return new DeleteAction(cursor, operation.getAction(), true);
                case NEXT_WORD:
                    return new NextWordAction(cursor, operation.getAction(), mode);
                case NEXT_BIG_WORD:
                    return new NextSpaceWordAction(cursor, operation.getAction());
                case PREV_WORD:
                    return new PrevWordAction(cursor, operation.getAction(), mode);
                case PREV_BIG_WORD:
                    return new PrevSpaceWordAction(cursor, operation.getAction());
                case BEGINNING:
                    return new SimpleAction(cursor, operation.getAction(), 0);
                case END:
                    return new SimpleAction(cursor, operation.getAction(), length);
                case ALL:
                        return new SimpleAction(0, operation.getAction(), length);
            }
        }

        return new SimpleAction(cursor, Action.NO_ACTION);
    }
}
