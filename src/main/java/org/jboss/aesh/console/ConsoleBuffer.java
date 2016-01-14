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
package org.jboss.aesh.console;

import org.jboss.aesh.edit.EditMode;
import org.jboss.aesh.edit.PasteManager;
import org.jboss.aesh.edit.actions.Action;
import org.jboss.aesh.edit.actions.EditAction;
import org.jboss.aesh.undo.UndoManager;

import java.io.IOException;
import java.io.PrintStream;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface ConsoleBuffer {

    PrintStream out();

    PrintStream err();

    void changeOutputBuffer(PrintStream output);

    Buffer getBuffer();

    void setEditMode(EditMode editMode);

    UndoManager getUndoManager();

    void addActionToUndoStack();

    PasteManager getPasteManager();

    EditMode getEditMode();

    void moveCursor(final int where);

    void drawLine();

    void drawLine(boolean keepCursorPosition);

    void updateCurrentAction(Action action);

    void syncCursor();

    void replace(int rChar);

    /**
     * Switch case if the character is a letter
     *
     * @throws java.io.IOException stream
     */
    void changeCase();

    void capitalizeWord();

    void writeChar(char input);

    void lowerCaseWord();

    void upperCaseWord();

    void writeChars(int[] input);

    void writeString(String input);

    void displayPrompt();

    void setPrompt(Prompt prompt);

    boolean isPrompted();

    void setPrompted(boolean prompted);

    void setBufferLine(String line);

    void insertBufferLine(String insert, int position);

    boolean paste(int index, boolean before);

    /**
     * Clear an ansi terminal.
     * Set includeBuffer to true if the current buffer should be
     * printed again after clear.
     *
     * @param includeBuffer if true include the current buffer line
     */
    void clear(boolean includeBuffer);

    /**
     * Perform the designated action created by an event
     *
     * @param action console action
     * @return true if nothing goes wrong
     * @throws IOException stream
     */
    boolean performAction(EditAction action) throws IOException;

}
