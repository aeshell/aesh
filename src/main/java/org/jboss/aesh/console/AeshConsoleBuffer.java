/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.edit.EditMode;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.edit.PasteManager;
import org.jboss.aesh.edit.ViEditMode;
import org.jboss.aesh.edit.actions.Action;
import org.jboss.aesh.edit.actions.EditAction;
import org.jboss.aesh.parser.Parser;
import org.jboss.aesh.terminal.Shell;
import org.jboss.aesh.undo.UndoAction;
import org.jboss.aesh.undo.UndoManager;
import org.jboss.aesh.util.ANSI;
import org.jboss.aesh.util.LoggerUtil;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshConsoleBuffer implements ConsoleBuffer {

    private EditMode editMode;
    private PrintStream err;
    private PrintStream out;

    private Buffer buffer;
    private Shell shell;

    private UndoManager undoManager;
    private PasteManager pasteManager;

    private Action prevAction = Action.EDIT;

    private boolean isLogging = true;

    //used to optimize text deletion
    private static final char[] resetLineAndSetCursorToStart =
            (ANSI.saveCursor()+ANSI.getStart()+"0G"+ANSI.getStart()+"2K").toCharArray();

    private static final Logger LOGGER = LoggerUtil.getLogger(AeshConsoleBuffer.class.getName());

    AeshConsoleBuffer(Prompt prompt, Shell shell, EditMode editMode) {
        this.out = shell.out();
        this.err = shell.err();
        this.buffer = new Buffer(prompt);
        this.shell = shell;
        pasteManager = new PasteManager();
        undoManager = new UndoManager();
        this.editMode = editMode;

        LOGGER.info("prompt: "+this.buffer.getPrompt().getPromptAsString());
    }

    @Override
    public Buffer getBuffer() {
        return this.buffer;
    }

    @Override
    public PrintStream out() {
        return out;
    }

    @Override
    public PrintStream err() {
        return err;
    }

    @Override
    public void setEditMode(EditMode editMode) {
        this.editMode = editMode;
    }

    @Override
    public UndoManager getUndoManager() {
        return undoManager;
    }

    @Override
    public PasteManager getPasteManager() {
        return pasteManager;
    }

    @Override
    public EditMode getEditMode() {
        return editMode;
    }

    @Override
    public void moveCursor(int where) {
        if(editMode.getMode() == Mode.VI &&
                (editMode.getCurrentAction() == Action.MOVE ||
                        editMode.getCurrentAction() == Action.DELETE)) {
            out.print(buffer.move(where, shell.getSize().getWidth(), true));
        }
        else {
            out.print(buffer.move(where, shell.getSize().getWidth()));
        }
        out.flush();
    }

    @Override
    public void drawLine() {
        LOGGER.info("drawing: "+buffer.getPrompt().getPromptAsString() + buffer.getLine());
        drawLine(buffer.getPrompt().getPromptAsString() + buffer.getLine());
    }

    @Override
    public void drawLine(String line) {
        //need to clear more than one line
        if(line.length() > shell.getSize().getWidth() ||
                (buffer.getDelta() < 0 && line.length()+ Math.abs(buffer.getDelta()) > shell.getSize().getWidth())) {
            if(buffer.getDelta() == -1 && buffer.getCursor() >= buffer.length() && Config.isOSPOSIXCompatible())
                redrawMultipleLinesBackspace();
            else
                redrawMultipleLines();
        }
        // only clear the current line
        else {
            //most deletions are backspace from the end of the line so we've
            //optimize that like this.
            //NOTE: this doesnt work with history, need to find a better solution
            if(buffer.getDelta() == -1 && buffer.getCursor() >= buffer.length()
                    && prevAction != Action.HISTORY) {
                out.print(Parser.SPACE_CHAR + ANSI.getStart() + "1D"); //move cursor to left
            }
            else {
                //save cursor, move the cursor to the beginning, reset line
                out.print(resetLineAndSetCursorToStart);
                if(!buffer.isPromptDisabled())
                    displayPrompt();
                //write line and restore cursor
                out.print(buffer.getLine() + ANSI.restoreCursor());
            }
        }
        out.flush();
    }

    private void redrawMultipleLines() {
        int currentRow = 0;
        if(buffer.getCursorWithPrompt() > 0)
            currentRow = buffer.getCursorWithPrompt() / shell.getSize().getWidth();
        if(currentRow > 0 && buffer.getCursorWithPrompt() % shell.getSize().getWidth() == 0)
            currentRow--;

        if(isLogging) {
            LOGGER.info("actual position: " + shell.getCursor());
            LOGGER.info("currentRow:" + currentRow + ", cursorWithPrompt:" + buffer.getCursorWithPrompt()
                    + ", width:" + shell.getSize().getWidth() + ", height:" + shell.getSize().getHeight()
                    + ", delta:" + buffer.getDelta() + ", buffer:" + buffer.getLine());
        }

        out.print(ANSI.saveCursor()); //save cursor

        if(currentRow > 0)
            for(int i=0; i<currentRow; i++)
                out.print(Buffer.printAnsi("A")); //move to top

        out.print(Buffer.printAnsi("0G")); //clear

        if(!buffer.isPromptDisabled())
            displayPrompt();
        out.print(buffer.getLine());
        //if the current line.length < compared to previous we add spaces to the end
        // to overwrite the old chars (wtb a better way of doing this)
        if(buffer.getDelta() < 0) {
            StringBuilder sb = new StringBuilder();
            for(int i=0; i > buffer.getDelta(); i--)
                sb.append(' ');
            out.print(sb.toString());
        }

        // move cursor to saved pos
        out.print(ANSI.restoreCursor());
    }

    private void redrawMultipleLinesBackspace() {
        out.print(Parser.SPACE_CHAR + ANSI.getStart() + "1D"); //move cursor to left
    }

    @Override
    public void syncCursor() {
        if(buffer.getCursor() != buffer.getLine().length()) {
            out.print(Buffer.printAnsi((
                    Math.abs(buffer.getCursor() -
                            buffer.getLine().length()) + "D")));
            out.flush();
        }
    }

    @Override
    public void replace(int rChar) {
        addActionToUndoStack();
        buffer.replaceChar((char) rChar);
        drawLine();
    }

    @Override
    public void changeCase() {
        if(buffer.changeCase()) {
            moveCursor(1);
            drawLine();
        }
    }

    @Override
    public void capitalizeWord() {
        String word = Parser.findWordClosestToCursor(buffer.getLineNoMask(), buffer.getCursor());
        if(word.length() > 0) {
            int pos = buffer.getLineNoMask().indexOf(word, buffer.getCursor()-word.length());
            if(pos < 0)
                pos = 0;
            buffer.replaceChar(Character.toUpperCase(buffer.getLineNoMask().charAt(pos)), pos);
            drawLine();
        }
    }

    @Override
    public void lowerCaseWord() {
        String word = Parser.findWordClosestToCursor(buffer.getLineNoMask(), buffer.getCursor());
        if(word.length() > 0) {
            int pos = buffer.getLineNoMask().indexOf(word, buffer.getCursor()-word.length());
            if(pos < 0)
                pos = 0;
            for(int i = 0; i < word.length(); i++) {
                buffer.replaceChar(Character.toLowerCase(buffer.getLineNoMask().charAt(pos+i)), pos+i);
            }
            drawLine();
        }
    }

    @Override
    public void upperCaseWord() {
        String word = Parser.findWordClosestToCursor(buffer.getLineNoMask(), buffer.getCursor());
        if(word.length() > 0) {
            int pos = buffer.getLineNoMask().indexOf(word, buffer.getCursor()-word.length());
            if(pos < 0)
                pos = 0;
            for(int i = 0; i < word.length(); i++) {
                buffer.replaceChar(Character.toUpperCase(buffer.getLineNoMask().charAt(pos+i)), pos+i);
            }
            drawLine();
        }
    }

    @Override
    public void writeChars(int[] chars) {
        for(int c : chars)
            writeChar((char) c);
    }

    @Override
    public void writeString(String input) {
        for(char c : input.toCharArray())
            writeChar(c);
    }

    @Override
    public void writeChar(char c) {

        buffer.write(c);
        //if mask is set and not set to 0 (nullvalue) we write out
        //the masked char. if masked is set to 0 we write nothing
        if(buffer.getPrompt().isMasking()) {
            if(buffer.getPrompt().getMask() != 0)
                out.print(buffer.getPrompt().getMask());
        }
        else {
            out.print(c);
        }

        // add a 'fake' new line when inserting at the edge of terminal
        if(buffer.getCursorWithPrompt() > shell.getSize().getWidth() &&
                buffer.getCursorWithPrompt() % shell.getSize().getWidth() == 1) {
            out.print((char) 32);
            out.print((char) 13);
        }

        // if we insert somewhere other than the end of the line we need to redraw from cursor
        if(buffer.getCursor() < buffer.length()) {
            //check if we just started a new line, if we did we need to make sure that we add one
            if(buffer.totalLength() > shell.getSize().getWidth() &&
                    (buffer.totalLength()-1) % shell.getSize().getWidth() == 1) {
                int ansiCurrentRow = shell.getCursor().getRow();
                int currentRow = (buffer.getCursorWithPrompt() / shell.getSize().getWidth());
                if(currentRow > 0 && buffer.getCursorWithPrompt() % shell.getSize().getWidth() == 0)
                    currentRow--;

                int totalRows = buffer.totalLength() / shell.getSize().getWidth();
                if(totalRows > 0 && buffer.totalLength() % shell.getSize().getWidth() == 0)
                    totalRows--;

                if(ansiCurrentRow+(totalRows-currentRow) > shell.getSize().getHeight()) {
                    out.print(Buffer.printAnsi("1S")); //adding a line
                    out.print(Buffer.printAnsi("1A")); // moving up a line
                }
            }
            drawLine();
        }
        out.flush();
    }

    @Override
    public void displayPrompt() {
        displayPrompt(buffer.getPrompt());
    }

    @Override
    public void setPrompt(Prompt prompt) {
        if(!buffer.getPrompt().equals(prompt)) {
            buffer.updatePrompt(prompt);
            //only update the prompt if Console is running
            //set cursor position line.length

            //if(running) { we might need to do something smarter here...
                displayPrompt(prompt);
                if(buffer.getLine().length() > 0) {
                    out().print(buffer.getLine());
                    buffer.setCursor(buffer.getLine().length());
                    out().flush();
                }
            //}
        }
    }

    @Override
    public void setBufferLine(String newLine) {
               //must make sure that there are enough space for the
        // line thats about to be injected
        if((newLine.length()+buffer.getPrompt().getLength()) >= shell.getSize().getWidth() &&
                newLine.length() >= buffer.getLine().length()) {
            int currentRow = shell.getCursor().getRow();
            if(currentRow > -1) {
                int cursorRow = buffer.getCursorWithPrompt() / shell.getSize().getWidth();
                if(currentRow + (newLine.length() / shell.getSize().getWidth()) - cursorRow >= shell.getSize().getHeight()) {
                    int numNewRows = currentRow +
                            ((newLine.length()+buffer.getPrompt().getLength()) / shell.getSize().getWidth()) -
                            cursorRow - shell.getSize().getHeight();
                    //if the line is exactly equal to termWidth we need to add another row
                    if((newLine.length()+buffer.getPrompt().getLength()) % shell.getSize().getWidth() == 0)
                        numNewRows++;
                    if(numNewRows > 0) {
                        if(isLogging) {
                            int totalRows = (newLine.length()+buffer.getPrompt().getLength()) / shell.getSize().getWidth() +1;
                            LOGGER.info("ADDING "+numNewRows+", totalRows:"+totalRows+
                                    ", currentRow:"+currentRow+", cursorRow:"+cursorRow);
                        }
                        out.print(Buffer.printAnsi(numNewRows + "S"));
                        out.print(Buffer.printAnsi(numNewRows + "A"));
                        out.flush();
                    }
                }
            }
        }
        buffer.setLine(newLine);
    }

    @Override
    public void insertBufferLine(String insert, int position) {
        if((insert.length()+buffer.totalLength()) >= shell.getSize().getWidth()) { //&&
            //(insert.length()+buffer.totalLength()) > buffer.getLine().length()) {
            int currentRow = shell.getCursor().getRow();
            if(currentRow > -1) {
                int newLine = insert.length()+buffer.totalLength();
                int cursorRow = buffer.getCursorWithPrompt() / shell.getSize().getWidth();
                if(currentRow + (newLine / shell.getSize().getWidth()) - cursorRow >= shell.getSize().getHeight()) {
                    int numNewRows = currentRow + (newLine / shell.getSize().getWidth()) - cursorRow - shell.getSize().getHeight();
                    //if the line is exactly equal to termWidth we need to add another row
                    if((insert.length()+buffer.totalLength()) % shell.getSize().getWidth() == 0)
                        numNewRows++;
                    if(numNewRows > 0) {
                        out.print(Buffer.printAnsi(numNewRows + "S"));
                        out.print(Buffer.printAnsi(numNewRows + "A"));
                        out.flush();
                    }
                }
            }
        }
        buffer.insert(position, insert);
    }

    private void displayPrompt(Prompt prompt) {
        if(prompt.hasANSI()) {
            out.print(ANSI.getStart() + "0G" + ANSI.getStart() + "2K");
            out.print(prompt.getANSI());
        }
        else
            out.print(ANSI.getStart() + "0G" + ANSI.getStart() + "2K" + prompt.getPromptAsString());
        out.flush();
    }

    @Override
    public boolean performAction(EditAction action) throws IOException {
        action.doAction(buffer.getLine());
        if(action.getAction() == Action.MOVE) {
            moveCursor((action.getEnd() - action.getStart()));
            return true;
        }
        else if(action.getAction() == Action.DELETE || action.getAction() == Action.CHANGE) {
            //first trigger undo action
            addActionToUndoStack();

            if(action.getEnd() > action.getStart()) {
                // only if start != cursor we need to move it
                if(action.getStart() != buffer.getCursor()) {
                    moveCursor(action.getStart() - buffer.getCursor());
                }
                addToPaste(buffer.getLine().substring(action.getStart(), action.getEnd()));
                buffer.delete(action.getStart(), action.getEnd());
            }
            else {
                addToPaste(buffer.getLine().substring(action.getEnd(), action.getStart()));
                buffer.delete(action.getEnd(), action.getStart());
                moveCursor((action.getEnd() - action.getStart()));
            }

            if(editMode.getMode() == Mode.VI && buffer.getCursor() == buffer.length()) {
                if(!((ViEditMode) editMode).isInEditMode())
                    moveCursor(-1);
            }
            drawLine();
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
     * Paste previous yanked word/char either before or on the cursor position
     *
     * @param index which yank index
     * @param before cursor
     * @return true if everything went as expected
     */
    @Override
    public boolean paste(int index, boolean before) {
        StringBuilder pasteBuffer = pasteManager.get(index);
        if(pasteBuffer == null)
            return false;

        addActionToUndoStack();
        if(before || buffer.getCursor() >= buffer.getLine().length()) {
            insertBufferLine(pasteBuffer.toString(), buffer.getCursor());
            drawLine();
        }
        else {
            insertBufferLine(pasteBuffer.toString(), buffer.getCursor() + 1);
            drawLine();
            //move cursor one char
            moveCursor(1);
        }
        return true;
    }

    /**
     * Add current text and cursor position to the undo stack
     */
    @Override
    public void addActionToUndoStack() {
        UndoAction ua = new UndoAction(buffer.getCursor(), buffer.getLine());
        undoManager.addUndo(ua);
    }

    private void addToPaste(String buffer) {
        pasteManager.addText(new StringBuilder(buffer));
    }

    /**
     * Clear an ansi terminal.
     * Set includeBuffer to true if the current buffer should be
     * printed again after clear.
     *
     * @param includeBuffer if true include the current buffer line
     */
    @Override
    public void clear(boolean includeBuffer) {
        //(windows fix)
        if(!Config.isOSPOSIXCompatible())
            out().print(Config.getLineSeparator());
        //first clear console
        out().print(ANSI.clearScreen());
        //move cursor to correct position
        out().print(Buffer.printAnsi("1;1H"));
        //then write prompt
        if(includeBuffer) {
            displayPrompt();
            out().print(buffer.getLine());
        }
        out().flush();
    }

}
