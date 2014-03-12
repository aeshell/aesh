/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.edit.EditMode;
import org.jboss.aesh.edit.PasteManager;
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

    Buffer getBuffer();

    void setEditMode(EditMode editMode);

    UndoManager getUndoManager();

    void addActionToUndoStack();

    PasteManager getPasteManager();

    EditMode getEditMode();

    void moveCursor(final int where);

    void drawLine();

    void drawLine(String line);

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
