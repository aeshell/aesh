/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
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
package org.jboss.jreadline.console;

import org.jboss.jreadline.edit.EditMode;
import org.jboss.jreadline.edit.EmacsEditMode;
import org.jboss.jreadline.edit.PasteManager;
import org.jboss.jreadline.edit.actions.*;
import org.jboss.jreadline.history.History;
import org.jboss.jreadline.history.InMemoryHistory;
import org.jboss.jreadline.terminal.POSIXTerminal;
import org.jboss.jreadline.terminal.Terminal;
import org.jboss.jreadline.undo.UndoAction;
import org.jboss.jreadline.undo.UndoManager;

import java.io.*;

/**
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class Console {

    private InputStream inStream;
    private Writer outStream;
    private Buffer buffer;
    private Terminal terminal;

    private UndoManager undoManager;
    private PasteManager pasteManager;
    private EditMode editMode;
    private History history;

    private Action prevAction = Action.EDIT;

    private static final String CR = System.getProperty("line.separator");

    public Console() throws IOException {
        this(new FileInputStream(FileDescriptor.in),
             new PrintWriter( new OutputStreamWriter(System.out)));

    }

    public Console(InputStream in, Writer out)  {
        this(in, out, null);
    }

    public Console(InputStream in, Writer out, Terminal terminal) {
        this(in, out, terminal, null);
    }

    public Console(InputStream in, Writer out, Terminal terminal, EditMode mode) {
        if(terminal == null)
            setTerminal(new POSIXTerminal());
        else
            setTerminal(terminal);

        if(mode == null)
            editMode = new EmacsEditMode();
        else
            editMode = mode;

        undoManager = new UndoManager();
        pasteManager = new PasteManager();
        buffer = new Buffer(null);
        history = new InMemoryHistory();

        setInStream(in);
        setOutWriter(out);
    }

    private void setTerminal(Terminal term) {
        terminal = term;
        terminal.init();
    }

    private void setInStream(InputStream is) {
        inStream = is;
    }

    private void setOutWriter(Writer out) {
        outStream = out;
    }

    private void flushOut() throws IOException {
        outStream.flush();
    }

    public void reset() throws Exception {
        terminal.reset();
    }

    public String read(String prompt) throws IOException {

        buffer.reset(prompt);
        outStream.write(buffer.getPrompt());
        flushOut();
        StringBuilder searchTerm = null;
        StringBuilder result = null;

        while(true) {

            int c = terminal.read(inStream);
            //System.out.println("got int:"+c);
            if (c == -1) {
                return null;
            }
            Operation operation = editMode.parseInput(c);

            Action action = operation.getAction();

            if (action == Action.EDIT) {
                writeChar(c);
            }
            // For search movement is used a bit differently.
            // It only triggers what kind of search action thats performed
            else if(action == Action.SEARCH) {

                switch (operation.getMovement()) {
                    //init a previous search
                    case PREV:
                        searchTerm = new StringBuilder(buffer.getLine());
                        if (searchTerm.length() > 0) {
                            result = history.searchPrevious(searchTerm.toString());
                        }
                        break;

                    case PREV_WORD:
                        result = history.searchPrevious(searchTerm.toString());

                        break;

                    case PREV_BIG_WORD:

                        if (searchTerm.length() > 0) {
                            searchTerm.deleteCharAt(searchTerm.length() - 1);
                        }

                        break;
                    // new search input, append to search
                    case ALL:
                        searchTerm.appendCodePoint(c);
                        //check if the new searchTerm will find anything
                        StringBuilder tmpResult = history.searchPrevious(searchTerm.toString());
                        //
                        if(tmpResult == null) {
                            searchTerm.deleteCharAt(searchTerm.length()-1);
                        }
                        else {
                            result = new StringBuilder(tmpResult.toString());
                        }
                        //result = history.searchPrevious(searchTerm.toString());
                        break;
                    // pressed enter, ending the search
                    case END:
                        // Set buffer to the found string.
                        if (result != null) {
                            buffer.setLine(new StringBuilder(result));
                            result = null;
                            redrawLine();
                            printNewline();
                            return buffer.getLine().toString();
                        }
                        redrawLine();
                        break;

                    case NEXT_BIG_WORD:
                        if(result != null) {
                            buffer.setLine(new StringBuilder(result));
                            result = null;
                        }
                        //redrawLine();
                        break;
                }
                // if we're still in search mode, print the search status
                if (editMode.getCurrentAction() == Action.SEARCH) {
                    if (searchTerm.length() == 0) {
                        if(result != null)
                            printSearch("", result.toString());
                        else
                            printSearch("", "");
                    }
                    else {
                        if (result == null) {
                            //beep();
                            //System.out.println("result");
                        }
                        else {
                            printSearch(searchTerm.toString(), result.toString());
                        }
                    }
                }
                // otherwise, restore the line
                else {
                    redrawLine();
                    outStream.write(Buffer.printAnsi((buffer.getPrompt().length()+1)+"G"));
                    flushOut();
                }


            }

            else if(action == Action.MOVE || action == Action.DELETE ||
                    action == Action.CHANGE || action == Action.YANK) {
                performAction(EditActionManager.parseAction(operation, buffer.getCursor(), buffer.length()));
            }
            else if(action == Action.ABORT) {

            }
            else if(action == Action.CASE) {
                addActionToUndoStack();
                changeCase();
            }
            else if(action == Action.COMPLETE) {
                //complete();
            }
            else if(action == Action.EXIT) {
                //deleteCurrentCharacter();
            }
            else if(action == Action.HISTORY) {
                if(operation.getMovement() == Movement.NEXT)
                    getHistory(true);
                else if(operation.getMovement() == Movement.PREV)
                    getHistory(false);
            }
            else if(action == Action.NEWLINE) {
                // clear the undo stack for each new line
                clearUndoStack();
                addToHistory(buffer.getLine());
                prevAction = Action.NEWLINE;
                //moveToEnd();
                printNewline(); // output newline
                return buffer.getLine().toString();
            }
            else if(action == Action.UNDO) {
                undo();
            }
            else if(action == Action.PASTE_FROM_CLIPBOARD) {
                addActionToUndoStack();
                //paste();
            }
            else if(action == Action.PASTE) {
                if(operation.getMovement() == Movement.NEXT)
                    doPaste(0, true);
                else
                    doPaste(0, false);
            }
            else if(action == Action.NO_ACTION) {
                //atm do nothing
            }

            if(action == Action.HISTORY)
                prevAction = action;

            flushOut();
        }

    }

    private void getHistory(boolean first) throws IOException {
        // first add current line to history
        if(prevAction == Action.NEWLINE) {
            history.setCurrent(buffer.getLine());
        }
        //get next
        if(first) {
            StringBuilder next = history.getNextFetch();
            if(next != null) {
                buffer.setLine(next);
                moveCursor(buffer.length()-buffer.getCursor());
                redrawLine();
            }
        }
        // get previous
        else {
           StringBuilder prev = history.getPreviousFetch();
            if(prev != null) {
                buffer.setLine(prev);
                //buffer.setCursor(buffer.length());
                moveCursor(buffer.length()-buffer.getCursor());
                redrawLine();
            }
        }
        prevAction = Action.HISTORY;
    }

    private void addToHistory(StringBuilder line) {
        history.push(new StringBuilder(line));
    }

    private void writeChar(int c) throws IOException {
       buffer.write((char) c);
       outStream.write(c);

       redrawLineFromCursor();
    }

    /**
     * Perform the designated action created by an event
     *
     * @param action console action
     * @return true if nothing goes wrong
     * @throws IOException stream
     */
    private boolean performAction(EditAction action) throws IOException {
        action.doAction(buffer.getLine());
        if(action.getAction() == Action.MOVE) {
            moveCursor((action.getEnd() - action.getStart()));
            return true;
        }
        else if(action.getAction() == Action.DELETE ||
                action.getAction() == Action.CHANGE) {
            //first trigger undo action
            addActionToUndoStack();

            if(action.getEnd() > action.getStart()) {
                // only if start != cursor we need to move it
                if(action.getStart() != buffer.getCursor()) {
                    moveCursor(action.getStart() - buffer.getCursor());
                }
                addToPaste(buffer.getLine().substring(action.getStart(), action.getEnd()));
                buffer.getLine().delete(action.getStart(), action.getEnd());
            }
            else {
                addToPaste(buffer.getLine().substring(action.getEnd(), action.getStart()));
                buffer.getLine().delete(action.getEnd(), action.getStart());
                moveCursor((action.getEnd() - action.getStart()));
            }
            redrawLineFromCursor();
        }
        else if(action.getAction() == Action.YANK) {
            if(action.getEnd() > action.getStart()) {
                addToPaste(buffer.getLine().substring(action.getStart(), action.getEnd()));
            }
            else {
                addToPaste(buffer.getLine().substring(action.getEnd(), action.getStart()));
            }
        }

        return true;
    }

    /**
     * Add current text and cursor position to the undo stack
     *
     * @throws IOException if getCursorPosition() fails
     */
    private void addActionToUndoStack() throws IOException {
        UndoAction ua = new UndoAction(buffer.getCursor(), buffer.getLine().toString());
        undoManager.addUndo(ua);
    }


    private void clearUndoStack() {
        undoManager.clear();
    }

    private void addToPaste(String buffer) {
        pasteManager.addText(new StringBuilder(buffer));
    }

    /**
     * Paste previous yanked word/char either before or on the cursor position
     *
     * @param index which yank index
     * @param before cursor
     * @return true if everything went as expected
     * @throws IOException if redraw failed
     */
    private boolean doPaste(int index, boolean before) throws IOException {
        StringBuilder pasteBuffer = pasteManager.get(index);
        if(pasteBuffer == null)
            return false;

        addActionToUndoStack();
        if(before || buffer.getCursor() >= buffer.getLine().length()) {
            buffer.getLine().insert(buffer.getCursor(), pasteBuffer);
            redrawLine();
        }
        else {
            buffer.getLine().insert(buffer.getCursor() + 1, pasteBuffer);
            redrawLine();
            //move cursor one char
            moveCursor(1);
        }
        return true;
    }

    public final void moveCursor(final int where) throws IOException {
        outStream.write(buffer.move(where));
        flushOut();
    }

    private void redrawLineFromCursor() throws IOException {

        outStream.write(Buffer.printAnsi("s")); //save cursor
        outStream.write(Buffer.printAnsi("0J")); // clear line from position
        flushOut();

        outStream.write(buffer.getLineFrom(buffer.getCursor()));
        // move cursor to saved pos
        outStream.write(Buffer.printAnsi("u"));
        flushOut();
    }

    private void redrawLine() throws IOException {
        drawLine(buffer.getPrompt()+buffer.getLine().toString());
    }

    private void drawLine(String line) throws IOException {
        outStream.write(Buffer.printAnsi("s")); //save cursor
        //move cursor to 0. - need to do this to clear the entire line
        outStream.write(Buffer.printAnsi("0G"));
        outStream.write(Buffer.printAnsi("2K")); // clear line
        flushOut();

        outStream.write(line);

        // move cursor to saved pos
        outStream.write(Buffer.printAnsi("u"));
        flushOut();
    }

    private void printSearch(String searchTerm, String result) throws IOException {
        //cursor should be placed at the index of searchTerm
        int cursor = result.indexOf(searchTerm);

        StringBuilder out = new StringBuilder("(reverse-i-search) `");
        out.append(searchTerm).append("': ");
        cursor += out.length();
        out.append(result); //.append("\u001b[K");
        drawLine(out.toString());
        outStream.write(Buffer.printAnsi((cursor+1) + "G"));
        flushOut();
    }

    /**
     * Insert a newline
     *
     * @throws java.io.IOException stream
     */
    private void printNewline() throws IOException {
        outStream.write(CR);
        flushOut();
    }

      /**
     * Switch case if the character is a letter
     *
     * @throws java.io.IOException stream
     */
    private void changeCase() throws IOException {
        if(buffer.changeCase()) {
           moveCursor(1);
            redrawLine();
        }
    }

    /**
     * Perform an undo
     *
     * @return true if nothing fails
     * @throws IOException if redraw fails
     */
    private boolean undo() throws IOException {
        UndoAction ua = undoManager.getNext();
        if(ua != null) {
            buffer.clear();
            buffer.write(ua.getBuffer());
            redrawLine();
            //move the cursor to the saved position
            outStream.write(Buffer.printAnsi((ua.getCursorPosition() + buffer.getPrompt().length() + 1) + "G"));
            flushOut();
            //sync terminal cursor with jreadline
            buffer.setCursor(ua.getCursorPosition());

            return true;
        }
        else
            return false;
    }
}
