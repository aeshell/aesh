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
import org.jboss.aesh.parser.Parser;
import org.jboss.aesh.readline.editing.EditMode;
import org.jboss.aesh.undo.UndoAction;
import org.jboss.aesh.util.LoggerUtil;

import java.util.logging.Logger;

/**
 * @author <a href=mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
abstract class ChangeAction extends MovementAction {

    private EditMode.Status status;
    protected boolean viMode;

    private static final Logger LOGGER = LoggerUtil.getLogger(ChangeAction.class.getName());

    ChangeAction(EditMode.Status  status) {
        this.status = status;
        viMode = false;
    }

    ChangeAction(boolean viMode, EditMode.Status status) {
        this.status = status;
        this.viMode = viMode;
    }

    protected EditMode.Status getStatus() {
        return status;
    }

    protected final void apply(int cursor, InputProcessor inputProcessor) {
        apply(cursor, inputProcessor.getBuffer().getBuffer().getMultiCursor(), inputProcessor);
    }

    protected final void apply(int cursor, int oldCursor, InputProcessor inputProcessor) {
        LOGGER.info("applying "+status+" delta: "+cursor+", current pos: "+oldCursor);
        if(status == EditMode.Status.DELETE || status == EditMode.Status.CHANGE) {
            addActionToUndoStack(inputProcessor);
            if(cursor < oldCursor) {
                //add to pastemanager
                inputProcessor.getBuffer().getPasteManager().addText(new StringBuilder(
                        inputProcessor.getBuffer().getBuffer().getLine().substring(
                                cursor,
                                oldCursor)));
                //delete buffer
                LOGGER.info("buffer before delete: "+inputProcessor.getBuffer().getBuffer().getLine());
                inputProcessor.getBuffer().getBuffer().delete(cursor,
                        oldCursor);
                LOGGER.info("buffer after delete: "+inputProcessor.getBuffer().getBuffer().getLine());
                inputProcessor.getBuffer().moveCursor(cursor-oldCursor);
            }
            else {
                //add to pastemanager
                inputProcessor.getBuffer().getPasteManager().addText(new StringBuilder(
                        inputProcessor.getBuffer().getBuffer().getLine().substring(
                                oldCursor, cursor)));
                //delete buffer
                inputProcessor.getBuffer().getBuffer().delete(
                        oldCursor, cursor);
            }

            //TODO: must check if we're in edit mode
            if(viMode && status == EditMode.Status.DELETE &&
                    oldCursor == inputProcessor.getBuffer().getBuffer().getLine().length())
                inputProcessor.getBuffer().moveCursor(-1);

            inputProcessor.getBuffer().drawLine();
        }
        else if(status == EditMode.Status.MOVE) {
            inputProcessor.getBuffer().moveCursor(cursor - oldCursor);
        }
        else if(status == EditMode.Status.YANK) {
            if(cursor < oldCursor)
                inputProcessor.getBuffer().getPasteManager().addText(
                        new StringBuilder(inputProcessor.getBuffer().getBuffer().getLine().substring(cursor,
                                oldCursor)));
            else if(cursor > oldCursor)
                inputProcessor.getBuffer().getPasteManager().addText(
                        new StringBuilder(inputProcessor.getBuffer().getBuffer().getLine().substring(
                                oldCursor, cursor)));
        }

        else if(status == EditMode.Status.UP_CASE) {
            if(cursor < oldCursor) {
                addActionToUndoStack(inputProcessor);
                for( int i = cursor; i < oldCursor; i++) {
                    inputProcessor.getBuffer().getBuffer().replaceChar(Character.toUpperCase(
                            inputProcessor.getBuffer().getBuffer().getLineNoMask().charAt(i)), i);
                }
            }
            else {
                addActionToUndoStack(inputProcessor);
                for( int i = oldCursor; i < cursor; i++) {
                    inputProcessor.getBuffer().getBuffer().replaceChar(Character.toUpperCase(
                            inputProcessor.getBuffer().getBuffer().getLineNoMask().charAt(i)), i);
                }
            }
            inputProcessor.getBuffer().moveCursor(cursor - oldCursor);
            inputProcessor.getBuffer().drawLine();
        }
        else if(status == EditMode.Status.DOWN_CASE) {
            if(cursor < oldCursor) {
                addActionToUndoStack(inputProcessor);
                for( int i = cursor; i < oldCursor; i++) {
                    inputProcessor.getBuffer().getBuffer().replaceChar(Character.toLowerCase(
                            inputProcessor.getBuffer().getBuffer().getLineNoMask().charAt(i)), i);
                }
            }
            else {
                addActionToUndoStack(inputProcessor);
                for( int i = oldCursor; i < cursor; i++) {
                    inputProcessor.getBuffer().getBuffer().replaceChar(Character.toLowerCase(
                            inputProcessor.getBuffer().getBuffer().getLineNoMask().charAt(i)), i);
                }
            }
            inputProcessor.getBuffer().moveCursor(cursor - oldCursor);
            inputProcessor.getBuffer().drawLine();
        }
        else if(status == EditMode.Status.CAPITALIZE) {
            String word = Parser.findWordClosestToCursor(inputProcessor.getBuffer().getBuffer().getLineNoMask(),
                    oldCursor);
            if(word.length() > 0) {
                addActionToUndoStack(inputProcessor);
                int pos = inputProcessor.getBuffer().getBuffer().getLineNoMask().indexOf(word,
                        oldCursor-word.length());
                if(pos < 0)
                    pos = 0;
                inputProcessor.getBuffer().getBuffer().replaceChar(
                        Character.toUpperCase(inputProcessor.getBuffer().getBuffer().getLineNoMask().charAt(pos)), pos);

                inputProcessor.getBuffer().moveCursor(cursor - oldCursor);
                inputProcessor.getBuffer().drawLine();
            }
        }
    }

    protected final void addActionToUndoStack(InputProcessor inputProcessor) {
        inputProcessor.getBuffer().getUndoManager().addUndo(new UndoAction(
                inputProcessor.getBuffer().getBuffer().getMultiCursor(),
                inputProcessor.getBuffer().getBuffer().getLine()));
    }

}
