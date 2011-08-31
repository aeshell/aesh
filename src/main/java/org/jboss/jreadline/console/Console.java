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
import org.jboss.jreadline.terminal.POSIXTerminal;
import org.jboss.jreadline.terminal.Terminal;
import org.jboss.jreadline.undo.UndoAction;
import org.jboss.jreadline.undo.UndoManager;

import java.io.*;
import java.util.Arrays;

/**
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class Console {

    private final static int TAB_WIDTH = 4;

    private InputStream inStream;
    private Writer outStream;
    private Buffer buffer;
    private Terminal terminal;
    private java.io.Reader reader;

    private UndoManager undoManager;
    private PasteManager pasteManager;
    private EditMode editMode;

    private static final String CR = System.getProperty("line.separator");

    public Console() throws IOException {
        this(new FileInputStream(FileDescriptor.in),
                new PrintWriter(
                        new OutputStreamWriter(System.out,
                                System.getProperty("jline.terminal.WindowsTerminal.output.encoding", System.getProperty("file.encoding")))));
    }

    public Console(InputStream in, Writer out)  {
        this(in, out, null);
    }

    public Console(InputStream in, Writer out, Terminal terminal) {
        if(terminal == null)
            setTerminal(initTerminal());

        editMode = new EmacsEditMode();
        undoManager = new UndoManager();
        pasteManager = new PasteManager();
        buffer = new Buffer(null);

        setInStream(in);
        setOutWriter(out);

    }



    private Terminal initTerminal() {
        Terminal t = new POSIXTerminal();
        t.init();
        return t;
    }

    private void setTerminal(Terminal terminal) {
        this.terminal = terminal;
    }

    private void setInStream(InputStream is) {
        inStream = is;
        /*
        inStream = new FilterInputStream(is) {
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (b == null) {
                    throw new NullPointerException();
                } else if (off < 0 || len < 0 || len > b.length - off) {
                    throw new IndexOutOfBoundsException();
                } else if (len == 0) {
                    return 0;
                }

                int c = read();
                if (c == -1) {
                    return -1;
                }
                b[off] = (byte)c;
                return 1;
            }
        };
        this.reader = new InputStreamReader( inStream);
        */
    }

    private void setOutWriter(Writer out) {
        //outStream = new OutputStreamWriter(out);
        outStream = out;
    }

    private void flushOut() throws IOException {
        outStream.flush();
    }

    public String read(String prompt) throws IOException {

        buffer.reset(prompt);
        outStream.write(buffer.getPrompt());
        flushOut();

        while(true) {

            int c = terminal.read(inStream);
            //int c = reader.read();
            //System.out.println("got int:"+c);
            if (c == -1) {
                return null;
            }
            Operation operation = editMode.parseInput(c);

            Action action = operation.getAction();

            if (action == Action.EDIT) {
                /*
                if (c != 0) { // ignore null chars
                    ActionListener tAction = triggeredActions.get(c);
                    if (tAction != null) {
                        tAction.actionPerformed(null);
                    }
                    else {
                        if(!undoManager.isEmpty()) {
                            addActionToUndoStack();
                        }
                        putChar(c, true);
                    }
                }
                */
                writeChar(c, true);
            }
            // For search movement is used a bit differently.
            // It only triggers what kind of search action thats performed
            else if(action == Action.SEARCH) {
                /*
                switch (operation.getMovement()) {
                    //init a previous search
                    case PREV:
                        searchTerm = new StringBuffer(buf.getBuffer());
                        if (searchTerm.length() > 0) {
                            searchIndex = history.searchBackwards(searchTerm.toString());
                            if (searchIndex == -1) {
                                beep();
                            }
                            printSearchStatus(searchTerm.toString(),
                                    searchIndex > -1 ? history.getHistory(searchIndex) : "");
                        } else {
                            searchIndex = -1;
                            printSearchStatus("", "");
                        }
                        break;

                    case PREV_WORD:
                        if (searchIndex == -1) {
                            searchIndex = history.searchBackwards(searchTerm.toString());
                        } else {
                            searchIndex = history.searchBackwards(searchTerm.toString(), searchIndex);
                        }
                        break;

                    case PREV_BIG_WORD:
                        if (searchTerm.length() > 0) {
                            searchTerm.deleteCharAt(searchTerm.length() - 1);
                            searchIndex = history.searchBackwards(searchTerm.toString());
                        }
                        break;
                    // new search input, append to search
                    case ALL:
                        searchTerm.appendCodePoint(c);
                        searchIndex = history.searchBackwards(searchTerm.toString());
                        break;
                    // pressed enter, ending the search
                    case END:
                        // Set buffer and cursor position to the found string.
                        if (searchIndex != -1) {
                            history.setCurrentIndex(searchIndex);
                            setBuffer(history.current());
                            buf.setCursor(history.current().indexOf(searchTerm.toString()));
                        }
                        break;
                }
                // if we're still in search mode, print the search status
                if (editMode.getCurrentAction() == Action.SEARCH) {
                    if (searchTerm.length() == 0) {
                        printSearchStatus("", "");
                    }
                    else {
                        if (searchIndex == -1) {
                            beep();
                        }
                        else {
                            printSearchStatus(searchTerm.toString(), history.getHistory(searchIndex));
                        }
                    }
                }
                // otherwise, restore the line
                else {
                    restoreLine();
                }
                */

            }

            else if(action == Action.MOVE || action == Action.DELETE ||
                    action == Action.CHANGE || action == Action.YANK) {
                performAction(EditActionManager.parseAction(operation, buffer.getCursor(), buffer.length()));
            }
            else if(action == Action.ABORT) {

            }
            else if(action == Action.CASE) {
                addActionToUndoStack();
                //changeCase();
            }
            else if(action == Action.COMPLETE) {
                //complete();
            }
            else if(action == Action.EXIT) {
                //deleteCurrentCharacter();
            }
            else if(action == Action.HISTORY) {
                //if(operation.getMovement() == Movement.NEXT)
                    //moveHistory(true);
                //else if(operation.getMovement() == Movement.PREV)
                    //moveHistory(false);
            }
            else if(action == Action.NEWLINE) {
                // clear the undo stack for each new line
                clearUndoStack();
                moveToEnd();
                printNewline(); // output newline
                String outBuffer = finishBuffer();
                //if(!matchInternalCommand(outBuffer))
                    return outBuffer;
                //else {
                //    drawLine();
                //}
            }
            else if(action == Action.UNDO) {
                //undo();
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

            flushOut();
        }

    }

   private void writeChar(int c, boolean b) throws IOException {
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
            moveInternal((action.getEnd() - action.getStart()));
            return true;
        }
        else if(action.getAction() == Action.DELETE ||
                action.getAction() == Action.CHANGE) {
            //first trigger undo action
            addActionToUndoStack();

            if(action.getEnd() > action.getStart()) {
                // only if start != cursor we need to move it
                if(action.getStart() != buffer.getCursor()) {
                    moveInternal(action.getStart()-buffer.getCursor());
                }
                addToPaste(buffer.getLine().substring(action.getStart(), action.getEnd()));
                buffer.getLine().delete(action.getStart(), action.getEnd());
            }
            else {
                addToPaste(buffer.getLine().substring(action.getEnd(), action.getStart()));
                buffer.getLine().delete(action.getEnd(), action.getStart());
                moveInternal((action.getEnd()-action.getStart()));
            }
            //drawBuffer(1);
            redrawLine();
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
            //drawBuffer(1);
            redrawLine();
        }
        else {
            buffer.getLine().insert(buffer.getCursor() + 1, pasteBuffer);
            //drawBuffer(1);
            redrawLine();
            //move cursor one char
            moveCursor(1);
        }
        return true;
    }

         /**
     * Move the cursor <i>where</i> characters, without checking the current
     * buffer.
     *
     * @param where the number of characters to move to the right or left.
     * @throws java.io.IOException stream
     */
    private void moveInternal(final int where) throws IOException {
        outStream.write(buffer.move(where));
        // debug ("move cursor " + where + " ("
        // + buf.getCursor() + " => " + (buf.getCursor() + where) + ")");
        /*
        buffer.setCursor(buffer.getCursor() + where);

        if (where < 0) {
            back(Math.abs(where));
        } else {
            int width = terminal.getWidth();
            int cursor = buffer.getCursor();
            int oldLine = (cursor - where) / width;
            int newLine = cursor / width;
            if (newLine > oldLine) {
                printANSISequence((newLine - oldLine) + "B");
            }
            printANSISequence(1 +(cursor % width) + "G");
        }
        */
        flushOut();
    }


    public final int moveCursor(final int num) throws IOException {
        outStream.write(buffer.move(num));
        return num;
    }

    public final void drawLine() throws IOException {
        /*
        if (prompt != null) {
            printString(prompt);
        }
        */

        /*
        printCharacters(buffer.getLine().toString().toCharArray());

        if (buffer.length() != buffer.getCursor()) {// not at end of line
            back(buffer.length() - buffer.getCursor() - 1); // sync
        }
        */
    }

    private void redrawLineFromCursor() throws IOException {
        if(buffer.getCursor() == buffer.length())
            return;

        //System.out.println("position:"+position+", cursor:"+buffer.getCursor());
        outStream.write(Buffer.printAnsi("s")); //save cursor
        flushOut();
        outStream.write(Buffer.printAnsi("0J")); // clear line from position
        flushOut();

        outStream.write(buffer.getLineFrom(buffer.getCursor()));
        // move cursor to saved pos
        outStream.write(Buffer.printAnsi("u"));
        flushOut();
    }

    private void redrawLine() throws IOException {
        //outStream.write('\r');
        //outStream.write(Buffer.printAnsi(buffer.getPrompt().length()+"G"));
        outStream.write(Buffer.printAnsi("s")); //save cursor
        //move cursor to 0. - need to do this to clear the entire line
        outStream.write(Buffer.printAnsi("0G"));
        outStream.write(Buffer.printAnsi("2K")); // clear line

        outStream.write(buffer.getPrompt());
        outStream.write(buffer.getLineFrom(0));

        // move cursor to saved pos
        outStream.write(Buffer.printAnsi("u"));
        flushOut();
    }

           /**
     * Redraw the rest of the buffer from the cursor onwards. This is necessary
     * for inserting text into the buffer.
     */
           /*
    private void drawBuffer(final int clear) throws IOException {
        // debug ("drawBuffer: " + clear);
        if (buffer.getCursor() == buffer.length() && clear == 0) {
            return;
        }
        char[] chars = buffer.getLine().substring(buffer.getCursor()).toCharArray();
        //if (mask != null) {
        //    Arrays.fill(chars, mask);
        //}

        printCharacters(chars);
        if(clear > 0)
               Buffer.printAnsi("J");

            if (chars.length > 0) {
                // don't ask, it seems to work
                back(Math.max(chars.length - 1, 1));
            }

        flushOut();
    }
    */

    private boolean moveToEnd() throws IOException {
        return moveCursor(buffer.length() - buffer.getCursor()) > 0;
    }

    /**
     * Output a platform-dependant newline.
     * @throws java.io.IOException stream
     */
    public final void printNewline() throws IOException {
        //outStream.write(Buffer.printAnsi(CR));
        outStream.write(CR);
        flushOut();
    }

       /**
     * Clear the buffer and add its contents to the history.
     *
     * @return the former contents of the buffer.
     */
    final String finishBuffer() {
        String str = buffer.getLine().toString();

        // we only add it to the history if the buffer is not empty
        // and if mask is null, since having a mask typically means
        // the string was a password. We clear the mask after this call
        /*
        if (str.length() > 0) {
            if (mask == null) {
                history.addToHistory(str);
            } else {
                mask = null;
            }
        }
        */

        //history.moveToEnd();
        //buffer.clear();

        return str;
    }

   private void drawBuffer(final int clear) throws IOException {
        // debug ("drawBuffer: " + clear);
        if (buffer.getCursor() == buffer.length() && clear == 0) {
            return;
        }
        char[] chars = buffer.getLine().substring(buffer.getCursor()).toCharArray();

        printCharacters(chars);
        clearAhead(clear);

       if (chars.length > 0) {
           // don't ask, it seems to work
           back(Math.max(chars.length - 1, 1));
       }

       flushOut();
    }

        private void printCharacters(final char[] c) throws IOException {
        int len = 0;
        for (char aC : c) {
            if (aC == '\t')
                len += TAB_WIDTH;
            else
                len++;
        }

        char cbuf[];
        if (len == c.length) {
            cbuf = c;
        } else {
            cbuf = new char[len];
            int pos = 0;
            for (char aC : c) {
                if (aC == '\t') {
                    Arrays.fill(cbuf, pos, pos + TAB_WIDTH, ' ');
                    pos += TAB_WIDTH;
                }
                else {
                    cbuf[pos] = aC;
                    pos++;
                }
            }
        }

        outStream.write(cbuf);
    }

    private void clearAhead(final int num) throws IOException {
        if (num == 0) {
            return;
        }

        printANSISequence("J");

    }

    private void printANSISequence(String sequence) throws IOException {
        outStream.write(27);
        outStream.write('[');
        outStream.write(sequence);
        flushOut();
    }

    private void back(final int num) throws IOException {
        if (num == 0) return;

        int width = terminal.getWidth();
        int cursor = buffer.getCursor();
        // debug("back: " + cursor + " + " + num + " on " + width);
        int currRow = (cursor + num) / width;
        int newRow = cursor / width;
        int newCol = cursor % width + 1;
        // debug("    old row: " + currRow + " new row: " + newRow);
        if (newRow < currRow) {
            printANSISequence((currRow - newRow) + "A");
        }
        printANSISequence(newCol + "G");
        flushOut();
    }




}
