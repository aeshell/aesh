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

import org.jboss.jreadline.complete.Completion;
import org.jboss.jreadline.console.settings.Settings;
import org.jboss.jreadline.edit.*;
import org.jboss.jreadline.edit.actions.*;
import org.jboss.jreadline.history.FileHistory;
import org.jboss.jreadline.history.History;
import org.jboss.jreadline.history.InMemoryHistory;
import org.jboss.jreadline.history.SearchDirection;
import org.jboss.jreadline.terminal.Terminal;
import org.jboss.jreadline.undo.UndoAction;
import org.jboss.jreadline.undo.UndoManager;
import org.jboss.jreadline.util.LoggerUtil;
import org.jboss.jreadline.util.Parser;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A console reader.
 * Supports ansi terminals
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class Console {

    private Buffer buffer;
    private Terminal terminal;

    private UndoManager undoManager;
    private PasteManager pasteManager;
    private EditMode editMode;
    private History history;
    private List<Completion> completionList;
    private Settings settings;

    private Action prevAction = Action.EDIT;

    private boolean displayCompletion = false;
    private boolean askDisplayCompletion = false;
    private boolean running = false;

    private Logger logger = LoggerUtil.getLogger(getClass().getName());

    public Console() throws IOException {
        this(Settings.getInstance());
    }

    public Console(Settings settings) throws IOException {
        reset(settings);
    }

    /**
     * Reset the Console with Settings
     * Can only be called after stop()
     *
     * @param settings
     * @throws IOException
     */
    public void reset(Settings settings) throws IOException {
        if(running)
            throw new RuntimeException("Cant reset an already running Console, must stop if first!");
         if(Settings.getInstance().doReadInputrc())
            Config.parseInputrc(Settings.getInstance());

        setTerminal(settings.getTerminal(),
                settings.getInputStream(), settings.getOutputStream());

        editMode = settings.getFullEditMode();

        undoManager = new UndoManager();
        pasteManager = new PasteManager();
        buffer = new Buffer(null);
        if(settings.isHistoryPersistent())
            history = new FileHistory(settings.getHistoryFile().getAbsolutePath(),
                    settings.getHistorySize());
        else
            history = new InMemoryHistory(settings.getHistorySize());


        completionList = new ArrayList<Completion>();
        this.settings = settings;
        running = true;
    }

     private void setTerminal(Terminal term, InputStream in, OutputStream out) {
        terminal = term;
        terminal.init(in, out);
    }

    /**
     * Get the terminal height
     *
     * @return height
     */
    public int getTerminalHeight() {
        return terminal.getHeight();
    }

    /**
     * Get the terminal width
     *
     * @return width
     */
    public int getTerminalWidth() {
        return terminal.getWidth();
    }

    /**
     * Get the History object
     *
     * @return history
     */
    public History getHistory() {
        return history;
    }

    /**
     * Push text to the console, note that this will not update the internal
     * cursor position.
     * 
     * @param input text
     * @throws IOException stream
     */
    public void pushToConsole(String input) throws IOException {
        if(input != null && input.length() > 0)
            terminal.write(input);
    }

    /**
     * @see #pushToConsole(String)
     *
     * @param input
     * @throws IOException
     */
    public void pushToConsole(char[] input) throws IOException {
        if(input != null && input.length > 0)
        terminal.write(input);
    }

    /**
     * Add a Completion to the completion list
     *
     * @param completion comp
     */
    public void addCompletion(Completion completion) {
        completionList.add(completion);
    }

    /**
     * Add a list of completions to the completion list
     *
     * @param completionList comps
     */
    public void addCompletions(List<Completion> completionList) {
        this.completionList.addAll(completionList);
    }

    /**
     * Stop the Console, close streams, and reset terminals.
     * WARNING: After this is called the Console object must be reset
     * before its used.
     * @throws IOException stream
     */
    public void stop() throws IOException {
        settings.getInputStream().close();
        //setting it to null to prevent uncertain state
        settings.setInputStream(null);
        terminal.reset();
        terminal = null;
        running = false;
    }


    /**
     * Read from the input stream, perform action according to mapped
     * operations/completions/etc
     * Return the stream when a new line is found.
     *
     * @param prompt starting prompt
     * @return input stream
     * @throws IOException stream
     */
    public String read(String prompt) throws IOException {
        return read(prompt, null);
    }

    /**
     * Read from the input stream, perform action according to mapped
     * operations/completions/etc
     * Return the stream when a new line is found.
     *
     * @param prompt starting prompt
     * @param mask if set typed chars will be masked with this specified char
     * @return input stream
     * @throws IOException stream
     */
    public String read(String prompt, Character mask) throws IOException {
        if(!running)
            throw new RuntimeException("Cant reuse a stopped Console before its reset again!");

        buffer.reset(prompt, mask);
        terminal.write(buffer.getPrompt());
        StringBuilder searchTerm = new StringBuilder();
        String result = null;

        while(true) {

            int[] in = terminal.read(settings.isReadAhead());
            //System.out.println("got int:"+c);
            if (in[0] == -1) {
                return null;
            }

            Operation operation = editMode.parseInput(in);

            Action action = operation.getAction();

            if(askDisplayCompletion) {
                askDisplayCompletion = false;
                if('y' == (char) in[0]) {
                    displayCompletion = true;
                    complete();
                }
                //do not display complete, but make sure that the previous line
                // is restored correctly
                else {
                    terminal.write(Config.getLineSeparator());
                    terminal.write(buffer.getLineWithPrompt());
                    syncCursor();
                }
            }
            else if (action == Action.EDIT) {
                writeChar(in[0], mask);
            }
            // For search movement is used a bit differently.
            // It only triggers what kind of search action thats performed
            else if(action == Action.SEARCH && !settings.isHistoryDisabled()) {

                switch (operation.getMovement()) {
                    //init a previous search
                    case PREV:
                        history.setSearchDirection(SearchDirection.REVERSE);
                        searchTerm = new StringBuilder(buffer.getLine());
                        if (searchTerm.length() > 0) {
                            result = history.search(searchTerm.toString());
                        }
                        break;

                    case NEXT:
                        history.setSearchDirection(SearchDirection.FORWARD);
                        searchTerm = new StringBuilder(buffer.getLine());
                        if (searchTerm.length() > 0) {
                            result = history.search(searchTerm.toString());
                        }
                        break;

                    case PREV_WORD:
                        history.setSearchDirection(SearchDirection.REVERSE);
                        if (searchTerm.length() > 0)
                            result = history.search(searchTerm.toString());
                        break;

                    case NEXT_WORD:
                        history.setSearchDirection(SearchDirection.FORWARD);
                        if(searchTerm.length() > 0)
                            result = history.search(searchTerm.toString());
                        break;

                    case PREV_BIG_WORD:
                        if (searchTerm.length() > 0)
                            searchTerm.deleteCharAt(searchTerm.length() - 1);
                        break;
                    // new search input, append to search
                    case ALL:
                        searchTerm.appendCodePoint(in[0]);
                        //check if the new searchTerm will find anything
                        String tmpResult = history.search(searchTerm.toString());
                        //
                        if(tmpResult == null) {
                            searchTerm.deleteCharAt(searchTerm.length()-1);
                        }
                        else {
                            result = new String(tmpResult);
                        }
                        //result = history.searchPrevious(searchTerm.toString());
                        break;
                    // pressed enter, ending the search
                    case END:
                        // Set buffer to the found string.
                        if (result != null) {
                            setBufferLine(result);
                            redrawLine();
                            printNewline();
                            return buffer.getLineNoMask();
                        }
                        redrawLine();
                        break;

                    case NEXT_BIG_WORD:
                        if(result != null) {
                            setBufferLine(result);
                            result = null;
                        }
                        //redrawLine();
                        break;
                }
                // if we're still in search mode, print the search status
                if (editMode.getCurrentAction() == Action.SEARCH) {
                    if (searchTerm.length() == 0) {
                        if(result != null)
                            printSearch("", result);
                        else
                            printSearch("", "");
                    }
                    else {
                        if (result == null) {
                            //beep();
                        }
                        else {
                            printSearch(searchTerm.toString(), result);
                        }
                    }
                }
                // otherwise, restore the line
                else {
                    redrawLine();
                    terminal.write(Buffer.printAnsi((buffer.getPrompt().length()+1)+"G"));
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
                complete();
            }
            else if(action == Action.EXIT) {
                //deleteCurrentCharacter();
            }
            else if(action == Action.HISTORY) {
                if(operation.getMovement() == Movement.NEXT)
                    getHistoryElement(true);
                else if(operation.getMovement() == Movement.PREV)
                    getHistoryElement(false);
            }
            else if(action == Action.NEWLINE) {
                // clear the undo stack for each new line
                clearUndoStack();
                if(mask == null) // dont push to history if masking
                    addToHistory(buffer.getLine());
                prevAction = Action.NEWLINE;
                //moveToEnd();
                printNewline(); // output newline
                return buffer.getLineNoMask();
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
            else if(action == Action.CHANGE_EDITMODE) {
                changeEditMode();
            }
            else if(action == Action.CLEAR) {
                clear();
            }
            else if(action == Action.NO_ACTION) {
                //atm do nothing
            }

            //a hack to get history working
            if(action == Action.HISTORY && !settings.isHistoryDisabled())
                prevAction = action;

        }

    }

    private void changeEditMode() {
        if(editMode.getMode() == Mode.EMACS) {
            if(Config.isOSPOSIXCompatible())
                editMode = new ViEditMode(KeyOperationManager.generatePOSIXViMode());
            else
                editMode = new ViEditMode(KeyOperationManager.generateWindowsViMode());
        }
        else {
            if(Config.isOSPOSIXCompatible())
                editMode = new EmacsEditMode(KeyOperationManager.generatePOSIXEmacsMode());
            else
                editMode = new EmacsEditMode(KeyOperationManager.generateWindowsEmacsMode());
        }
    }

    private void getHistoryElement(boolean first) throws IOException {
        if(settings.isHistoryDisabled())
            return;
        // first add current line to history
        if(prevAction == Action.NEWLINE) {
            history.setCurrent(buffer.getLine());
        }
        //get next
        String fromHistory;
        if(first)
            fromHistory = history.getNextFetch();
        // get previous
        else
           fromHistory = history.getPreviousFetch();

        if(fromHistory != null) {
            setBufferLine(fromHistory);
            moveCursor(buffer.length()-buffer.getCursor());
            redrawLine();
        }
        prevAction = Action.HISTORY;
    }
    
    private void setBufferLine(String newLine) throws IOException {
        //must make sure that there are enough space for the
        // line thats about to be injected
        if((newLine.length()+buffer.getPrompt().length()) >= getTerminalWidth() &&
                newLine.length() >= buffer.getLine().length()) {
            int currentRow = getCurrentRow();
            if(currentRow > -1) {
                int cursorRow = buffer.getCursorWithPrompt() / getTerminalWidth();
                if(currentRow + (newLine.length() / getTerminalWidth()) - cursorRow >= getTerminalHeight()) {
                    int numNewRows = currentRow + (newLine.length() / getTerminalWidth()) - cursorRow - getTerminalHeight();
                    //if the line is exactly equal to termWidth we need to add another row
                    if((newLine.length()+buffer.getPrompt().length()) % getTerminalWidth() == 0)
                        numNewRows++;
                    if(numNewRows > 0) {
                        int totalRows = newLine.length() / getTerminalWidth() +1;
                        //logger.info("ADDING "+numNewRows+", totalRows:"+totalRows+
                        //        ", currentRow:"+currentRow+", cursorRow:"+cursorRow);
                        terminal.write(Buffer.printAnsi(numNewRows+"S"));
                    }
                }
            }
        }
        buffer.setLine(newLine);
    }
    
    private void insertBufferLine(String insert, int position) throws IOException {
        if((insert.length()+buffer.totalLength()) >= getTerminalWidth()) { //&&
                //(insert.length()+buffer.totalLength()) > buffer.getLine().length()) {
            int currentRow = getCurrentRow();
            if(currentRow > -1) {
                int newLine = insert.length()+buffer.length();
                int cursorRow = buffer.getCursorWithPrompt() / getTerminalWidth();
                if(currentRow + (newLine / getTerminalWidth()) - cursorRow >= getTerminalHeight()) {
                    int numNewRows = currentRow + (newLine / getTerminalWidth()) - cursorRow - getTerminalHeight();
                    //if the line is exactly equal to termWidth we need to add another row
                    if((insert.length()+buffer.totalLength()) % getTerminalWidth() == 0)
                        numNewRows++;
                    if(numNewRows > 0) {
                        terminal.write(Buffer.printAnsi(numNewRows+"S"));
                    }
                }
            }
        }
        buffer.insert(position, insert);
    }

    private void addToHistory(String line) {
        if(!settings.isHistoryDisabled())
            history.push(line);
    }

    private void writeChar(int c, Character mask) throws IOException {

        buffer.write((char) c);
        if(mask != null) {
            if(mask == 0)
                terminal.write(' '); //TODO: fix this hack
            else
                terminal.write(mask);
        }
        else {
            terminal.write((char) c);
        }

        // add a 'fake' new line when inserting at the edge of terminal
        if(buffer.getCursorWithPrompt() > getTerminalWidth() &&
                buffer.getCursorWithPrompt() % getTerminalWidth() == 1) {
           terminal.write((char) 32);
            terminal.write((char) 13);
        }

        // if we insert somewhere other than the end of the line we need to redraw from cursor
        if(buffer.getCursor() < buffer.length()) {
            //check if we just started a new line, if we did we need to make sure that we add one
            if(buffer.totalLength() > getTerminalWidth() &&
                    (buffer.totalLength()-1) % getTerminalWidth() == 1) {
                int ansiCurrentRow = getCurrentRow();
                int currentRow = (buffer.getCursorWithPrompt() / getTerminalWidth());
                if(currentRow > 0 && buffer.getCursorWithPrompt() % getTerminalWidth() == 0)
                    currentRow--;

                int totalRows = buffer.totalLength() / getTerminalWidth();
                if(totalRows > 0 && buffer.totalLength() % getTerminalWidth() == 0)
                    totalRows--;

                if(ansiCurrentRow+(totalRows-currentRow) > getTerminalHeight()) {
                    terminal.write(Buffer.printAnsi("1S")); //adding a line
                    terminal.write(Buffer.printAnsi("1A")); // moving up a line
                }
            }
            redrawLine();
        }
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
                moveCursor(-1);
            }
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
        UndoAction ua = new UndoAction(buffer.getCursor(), buffer.getLine());
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
            insertBufferLine(pasteBuffer.toString(), buffer.getCursor());
            redrawLine();
        }
        else {
            //buffer.insert(buffer.getCursor() + 1, pasteBuffer.toString());
            insertBufferLine(pasteBuffer.toString(), buffer.getCursor()+1);
            redrawLine();
            //move cursor one char
            moveCursor(1);
        }
        return true;
    }

    public final void moveCursor(final int where) throws IOException {
        if(editMode.getMode() == Mode.VI &&
                (editMode.getCurrentAction() == Action.MOVE ||
                        editMode.getCurrentAction() == Action.DELETE)) {

            terminal.write(buffer.move(where, getTerminalWidth(), true));
        }
        else {
            terminal.write(buffer.move(where, getTerminalWidth()));
        }
    }

    private void redrawLineFromCursor() throws IOException {

        terminal.write(Buffer.printAnsi("s")); //save cursor
        terminal.write(Buffer.printAnsi("0J")); // clear line from position

        terminal.write(buffer.getLineFrom(buffer.getCursor()));
        // move cursor to saved pos
        terminal.write(Buffer.printAnsi("u"));
    }

    private void redrawLine() throws IOException {
        drawLine(buffer.getPrompt()+ buffer.getLine());
    }

    private void drawLine(String line) throws IOException {
       //need to clear more than one line
        if(line.length() > getTerminalWidth() ||
                (line.length()+ Math.abs(buffer.getDelta()) > getTerminalWidth())) {

            int currentRow = 0;
            if(buffer.getCursorWithPrompt() > 0)
                currentRow = buffer.getCursorWithPrompt() / getTerminalWidth();
            if(currentRow > 0 && buffer.getCursorWithPrompt() % getTerminalWidth() == 0)
                currentRow--;

            terminal.write(Buffer.printAnsi("s")); //save cursor

            if(currentRow > 0)
                for(int i=0; i<currentRow; i++)
                    terminal.write(Buffer.printAnsi("A")); //move to top

            terminal.write(Buffer.printAnsi("0G")); //clear

            terminal.write(line);
            //if the current line.length < compared to previous we add spaces to the end
            // to overwrite the old chars (wtb a better way of doing this)
            if(buffer.getDelta() < 0) {
                StringBuilder sb = new StringBuilder();
                for(int i=0; i > buffer.getDelta(); i--)
                    sb.append(' ');
                terminal.write(sb.toString());
            }

            // move cursor to saved pos
            terminal.write(Buffer.printAnsi("u"));
        }
        // only clear the current line
        else {
            terminal.write(Buffer.printAnsi("s")); //save cursor
            //move cursor to 0. - need to do this to clear the entire line
            terminal.write(Buffer.printAnsi("0G"));
            terminal.write(Buffer.printAnsi("2K")); // clear line

            terminal.write(line);


            // move cursor to saved pos
            terminal.write(Buffer.printAnsi("u"));
        }
    }

    private void printSearch(String searchTerm, String result) throws IOException {
        //cursor should be placed at the index of searchTerm
        int cursor = result.indexOf(searchTerm);

        StringBuilder out;
        if(history.getSearchDirection() == SearchDirection.REVERSE)
            out = new StringBuilder("(reverse-i-search) `");
        else
            out = new StringBuilder("(forward-i-search) `");
        out.append(searchTerm).append("': ");
        cursor += out.length();
        out.append(result); //.append("\u001b[K");
        setBufferLine(out.toString());
        redrawLine();
        //moveCursor(cursor+1);
    }

    /**
     * Insert a newline
     *
     * @throws java.io.IOException stream
     */
    private void printNewline() throws IOException {
        terminal.write(Config.getLineSeparator());
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
     * @throws IOException if redraw fails
     */
    private void undo() throws IOException {
        UndoAction ua = undoManager.getNext();
        if(ua != null) {
            setBufferLine(ua.getBuffer());
            redrawLine();
            moveCursor(ua.getCursorPosition() - buffer.getCursor());
        }
    }

    private void complete() throws IOException {
        if(completionList.size() < 1)
            return;

        List<String> possibleCompletions = new ArrayList<String>();
        for(Completion completion : completionList) {
            List<String> newCompletions = completion.complete(buffer.getLine(), buffer.getCursor());
            if(newCompletions != null && !newCompletions.isEmpty())
                possibleCompletions.addAll( newCompletions);
        }

        // not hits, just return (perhaps we should beep?)
        if(possibleCompletions.size() < 1)
            return;
        // only one hit, do a completion
        else if(possibleCompletions.size() == 1)
            displayCompletion(possibleCompletions.get(0), true);
        // more than one hit...
        else {
            String startsWith = Parser.findStartsWith(possibleCompletions);
            if(startsWith.length() > 0 && startsWith.length() > buffer.getCursor())
                displayCompletion(startsWith, false);
            // display all
            // check size
            else {
                if(possibleCompletions.size() > 100) {
                    if(displayCompletion) {
                        displayCompletions(possibleCompletions);
                        displayCompletion = false;
                    }
                    else {
                        askDisplayCompletion = true;
                        terminal.write(Config.getLineSeparator()+"Display all "+possibleCompletions.size()+ " possibilities? (y or n)");
                    }
                }
                // display all
                else {
                    displayCompletions(possibleCompletions);
                }
            }
        }
    }

    /**
     * Display the completion string in the terminal.
     * If !completion.startsWith(buffer.getLine()) the completion will be added to the line,
     * else it will replace whats at the buffer line.
     *
     * @param completion item
     * @param appendSpace if its an actual complete
     * @throws java.io.IOException stream
     */
    private void displayCompletion(String completion, boolean appendSpace) throws IOException {
        if(completion.startsWith(buffer.getLine())) {
            performAction(new PrevWordAction(buffer.getCursor(), Action.DELETE));
            buffer.write(completion);
            terminal.write(completion);

            //only append space if its an actual complete, not a partial
            if(appendSpace) {
                buffer.write(' ');
                terminal.write(' ');
            }
        }
        else { //if(completion.length() >= buffer.getLine().length()){
            //String rest = completion.substring( buffer.getLine().length());
            buffer.write(completion);
            terminal.write(completion);
        }

        redrawLineFromCursor();
    }

    /**
     * Display all possible completions
     *
     * @param completions all completion items
     * @throws IOException stream
     */
    private void displayCompletions(List<String> completions) throws IOException {
        printNewline();
        terminal.write(Parser.formatCompletions(completions, terminal.getHeight(), terminal.getWidth()));
        terminal.write(buffer.getLineWithPrompt());
        //if we do a complete and the cursor is not at the end of the
        //buffer we need to move it to the correct place
        syncCursor();
    }

    private void syncCursor() throws IOException {
        if(buffer.getCursor() != buffer.getLine().length())
            terminal.write(Buffer.printAnsi((
                    Math.abs( buffer.getCursor()-
                            buffer.getLine().length())+"D")));

    }

    /**
     * Return the row position if we use a ansi terminal
     * Send a terminal: '<ESC>[6n'
     * and we receive the position as: '<ESC>[n;mR'
     * where n = current row and m = current column
     *
     * @return current row
     */
    private int getCurrentRow() {
        if(settings.isAnsiConsole() && Config.isOSPOSIXCompatible()) {
            try {
                terminal.write(Buffer.printAnsi("6n"));
                StringBuilder builder = new StringBuilder(8);
                int row;
                while((row = terminal.read(false)[0]) > -1 && row != 'R') {
                    //while((row = settings.getInputStream().read()) > -1 && row != 'R') {
                    //while((row = settings.getInputStream().read()) > -1 && row != 'R' && row != ';') {
                    if (row != 27 && row != '[') {
                        builder.append((char) row);
                    }
                }
                //return Integer.parseInt(builder.toString());
                return Integer.parseInt(builder.substring(0, builder.indexOf(";")));
            }
            catch (Exception e) {
                if(settings.isLogging())
                    logger.warning("Failed to find current row with ansi code: "+e.getMessage());
                return -1;
            }
        }
        return -1;
    }

    private int getCurrentColumn() {
        if(settings.isAnsiConsole() && Config.isOSPOSIXCompatible()) {
            try {
                terminal.write(Buffer.printAnsi("6n"));
                StringBuilder builder = new StringBuilder(8);
                int row;
                while((row = settings.getInputStream().read()) > -1 && row != 'R' ) {
                    if (row != 27 && row != '[') {
                        builder.append((char) row);
                    }
                }
                return Integer.parseInt(builder.substring(builder.lastIndexOf(";") + 1, builder.length()));
            }
            catch (Exception e) {
                if(settings.isLogging())
                    logger.warning("Failed to find current column with ansi code: "+e.getMessage());
                return -1;
            }
        }
        return -1;
    }

    /**
     * Clear a ansi terminal
     *
     * @throws IOException stream
     */
    public void clear() throws IOException {
        //first clear console
        terminal.write(Buffer.printAnsi("2J"));
        //move cursor to correct position
        terminal.write(Buffer.printAnsi("1;1H"));
        //then write prompt
        terminal.write(buffer.getLineWithPrompt());
    }
}
