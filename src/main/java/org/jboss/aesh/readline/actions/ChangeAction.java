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

    protected void apply(int cursor, InputProcessor inputProcessor) {
        LOGGER.info("applying "+status+" delta: "+cursor+", current pos: "+inputProcessor.getBuffer().getBuffer().getMultiCursor());
        if(status == EditMode.Status.DELETE || status == EditMode.Status.CHANGE) {
            addActionToUndoStack(inputProcessor);
            if(cursor < inputProcessor.getBuffer().getBuffer().getMultiCursor()) {
                //add to pastemanager
                inputProcessor.getBuffer().getPasteManager().addText(new StringBuilder(
                        inputProcessor.getBuffer().getBuffer().getLine().substring(
                                cursor,
                                inputProcessor.getBuffer().getBuffer().getMultiCursor())));
                //delete buffer
                LOGGER.info("buffer before delete: "+inputProcessor.getBuffer().getBuffer().getLine());
                inputProcessor.getBuffer().getBuffer().delete(cursor,
                        inputProcessor.getBuffer().getBuffer().getMultiCursor());
                LOGGER.info("buffer after delete: "+inputProcessor.getBuffer().getBuffer().getLine());
                inputProcessor.getBuffer().moveCursor(inputProcessor.getBuffer().getBuffer().getMultiCursor()-cursor);
            }
            else {
                //add to pastemanager
                inputProcessor.getBuffer().getPasteManager().addText(new StringBuilder(
                        inputProcessor.getBuffer().getBuffer().getLine().substring(
                                inputProcessor.getBuffer().getBuffer().getMultiCursor(), cursor)));
                //delete buffer
                inputProcessor.getBuffer().getBuffer().delete(
                        inputProcessor.getBuffer().getBuffer().getMultiCursor(), cursor);
            }

            //TODO: must check if we're in edit mode
            //if(viMode && inputProcessor.getBuffer().getBuffer().getMultiCursor() ==
            // inputProcessor.getBuffer().getBuffer().getLine().length())
            //    inputProcessor.getBuffer().moveCursor(-1);

            inputProcessor.getBuffer().drawLine();
        }
        else if(status == EditMode.Status.MOVE) {
            inputProcessor.getBuffer().moveCursor(cursor - inputProcessor.getBuffer().getBuffer().getMultiCursor());
        }
    }

    private void addActionToUndoStack(InputProcessor inputProcessor) {
        inputProcessor.getBuffer().getUndoManager().addUndo(new UndoAction(
                inputProcessor.getBuffer().getBuffer().getMultiCursor(),
                inputProcessor.getBuffer().getBuffer().getLine()));
    }

}
