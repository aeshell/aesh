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
 * @author <a href=mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
abstract class ChangeAction extends MovementAction {

    private EditMode.Status status;

    ChangeAction(EditMode.Status  status) {
        this.status = status;
    }

    protected EditMode.Status getStatus() {
        return status;
    }

    protected void apply(int cursor, InputProcessor inputProcessor) {
        if(status == EditMode.Status.DELETE || status == EditMode.Status.CHANGE) {
            if(cursor < inputProcessor.getBuffer().getBuffer().getMultiCursor()) {
                //add to pastemanager
                inputProcessor.getBuffer().getPasteManager().addText(new StringBuilder(
                        inputProcessor.getBuffer().getBuffer().getLine().substring(
                                cursor,
                                inputProcessor.getBuffer().getBuffer().getMultiCursor())));
                //delete buffer
                inputProcessor.getBuffer().getBuffer().delete(cursor,
                        inputProcessor.getBuffer().getBuffer().getMultiCursor());
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
            inputProcessor.getBuffer().moveCursor(cursor - inputProcessor.getBuffer().getBuffer().getMultiCursor());
        }
        else if(status == EditMode.Status.MOVE) {
            inputProcessor.getBuffer().moveCursor(cursor - inputProcessor.getBuffer().getBuffer().getMultiCursor());
        }
    }

}