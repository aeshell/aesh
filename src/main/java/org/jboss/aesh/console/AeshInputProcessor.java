/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.console.command.CommandOperation;
import org.jboss.aesh.console.helper.Search;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.edit.actions.Action;
import org.jboss.aesh.edit.actions.EditActionManager;
import org.jboss.aesh.edit.actions.Movement;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.history.History;
import org.jboss.aesh.history.SearchDirection;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.undo.UndoAction;
import org.jboss.aesh.util.ANSI;
import org.jboss.aesh.util.LoggerUtil;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshInputProcessor implements InputProcessor {

    private Buffer buffer;

    private Search search;
    private History history;
    private Settings settings;

    private ConsoleBuffer consoleBuffer;

    private CompletionHandler completionHandler;

    private Action prevAction = Action.EDIT;

    private static final Pattern endsWithBackslashPattern = Pattern.compile(".*\\s\\\\$");

    //used to optimize text deletion
    private static final char[] resetLineAndSetCursorToStart =
            (ANSI.saveCursor()+ANSI.getStart()+"0G"+ANSI.getStart()+"2K").toCharArray();

    private static final Logger LOGGER = LoggerUtil.getLogger(AeshInputProcessor.class.getName());

    AeshInputProcessor(ConsoleBuffer consoleBuffer,
                       History history, Settings settings,
                       CompletionHandler completionHandler ) {

        this.consoleBuffer = consoleBuffer;
        this.buffer = consoleBuffer.getBuffer();
        this.history = history;
        this.settings = settings;
        this.completionHandler = completionHandler;
    }

    @Override
    public String parseOperation(CommandOperation commandOperation) throws IOException {

        Operation operation = consoleBuffer.getEditMode().parseInput(commandOperation.getInputKey(),
                consoleBuffer.getBuffer().getLine());
        if(commandOperation.getInputKey() != Key.UNKNOWN)
            operation.setInput(commandOperation.getInputKey().getKeyValues());
        else
            operation.setInput(new int[]{ commandOperation.getInput()[commandOperation.getPosition()]});

        Action action = operation.getAction();

        if (action == Action.EDIT) {
            consoleBuffer.writeChars(operation.getInput());
        }
        //make sure that every action except delete is ignored when masking is enabled
        else if(consoleBuffer.getBuffer().isMasking()) {
            if(action == Action.DELETE) {
                if(consoleBuffer.getBuffer().getPrompt().getMask() == 0)
                    deleteWithMaskEnabled();
                else
                    consoleBuffer.performAction(EditActionManager.parseAction(operation, buffer.getCursor(), buffer.length()));
            }
        }
        // For search movement is used a bit differently.
        // It only triggers what kind of search action thats performed
        else if(action == Action.SEARCH && !settings.isHistoryDisabled()) {

            if(search == null)
                search = new Search(operation, operation.getInput()[0]);
            else {
                search.setOperation(operation);
                search.setInput(operation.getInput()[0]);
            }
            doSearch(search);
            if(search.isFinished())
                return search.getResult();
        }
        else if(action == Action.MOVE || action == Action.DELETE ||
                action == Action.CHANGE || action == Action.YANK) {
            consoleBuffer.performAction(EditActionManager.parseAction(operation, buffer.getCursor(), buffer.length()));
        }
        else if(action == Action.ABORT) {
        }
        else if(action == Action.CASE) {
            consoleBuffer.addActionToUndoStack();
            consoleBuffer.changeCase();
        }
        else if(action == Action.COMPLETE) {
            complete();
        }
        else if(action == Action.EXIT || action == Action.EOF ||
                action == Action.INTERRUPT || action == Action.IGNOREEOF) {
            if(settings.hasInterruptHook()) {
                //settings.getInterruptHook().handleInterrupt(this, action);
            }
            else {
                //all action other than IGNOREEOF will stop aesh
                if(action != Action.IGNOREEOF) {
                    consoleBuffer.out().println();
                    consoleBuffer.out().println("we should stop aesh here...");
                    /*
                    if(processManager.hasRunningProcess())
                        stop();
                    else {
                        doStop();
                    }
                    */
                }
            }
        }
        else if(action == Action.HISTORY) {
            if(operation.getMovement() == Movement.NEXT)
                getHistoryElement(true);
            else if(operation.getMovement() == Movement.PREV)
                getHistoryElement(false);
        }
        else if(action == Action.UNDO) {
            undo();
        }
        else if(action == Action.PASTE_FROM_CLIPBOARD) {
            consoleBuffer.addActionToUndoStack();
            //paste();
        }
        else if(action == Action.PASTE) {
            if(operation.getMovement() == Movement.NEXT)
                consoleBuffer.paste(0, true);
            else
                consoleBuffer.paste(0, false);
        }
        else if(action == Action.CHANGE_EDITMODE) {
            changeEditMode(operation.getMovement());
        }
        else if(action == Action.CLEAR) {
            consoleBuffer.clear(true);
        }
        else if(action == Action.REPLACE) {
            consoleBuffer.replace(operation.getInput()[0]);
        }
        else if(action == Action.NO_ACTION) {
            //atm do nothing
        }

        //a hack to get history working
        if(action == Action.HISTORY && !settings.isHistoryDisabled())
            prevAction = action;

        //in the end we check for a newline
        if(action == Action.NEWLINE) {
            // clear the undo stack for each new line
            consoleBuffer.getUndoManager().clear();
            if(!buffer.isMasking()) {// dont push to history if masking
                //dont push lines that end with \ to history
                if(!endsWithBackslashPattern.matcher(buffer.getLine()).find()) {
                    if(buffer.isMultiLine())
                        addToHistory(buffer.getMultiLineBuffer()+buffer.getLine());
                    else
                        addToHistory(buffer.getLine());
                }
            }
            prevAction = Action.NEWLINE;
            //moveToEnd();
            consoleBuffer.moveCursor(buffer.totalLength());
            consoleBuffer.out().print(Config.getLineSeparator());
            String result;
            if(buffer.isMultiLine()) {
                result = buffer.getMultiLineBuffer() + buffer.getLineNoMask();
            }
            else
                result = buffer.getLineNoMask();
            buffer.reset();
            return result;
        }

        return null;

    }

    @Override
    public History getHistory() {
        return history;
    }

    @Override
    public void clearBufferAndDisplayPrompt() {
        consoleBuffer.getBuffer().reset();
        consoleBuffer.getUndoManager().clear();
        prevAction = Action.NEWLINE;
        consoleBuffer.displayPrompt();
    }

    /**
     * Parse the Search object
     *
     * @param search search
     * @throws IOException stream
     */
    private void doSearch(Search search) throws IOException {

        switch (search.getOperation().getMovement()) {
            //init a previous doSearch
            case PREV:
                history.setSearchDirection(SearchDirection.REVERSE);
                search.setSearchTerm( new StringBuilder(buffer.getLine()));
                if (search.getSearchTerm().length() > 0) {
                    search.setResult( history.search(search.getSearchTerm().toString()));
                }
                break;

            case NEXT:
                history.setSearchDirection(SearchDirection.FORWARD);
                search.setSearchTerm(new StringBuilder(buffer.getLine()));
                if (search.getSearchTerm().length() > 0) {
                    search.setResult( history.search(search.getSearchTerm().toString()));
                }
                break;

            case PREV_WORD:
                history.setSearchDirection(SearchDirection.REVERSE);
                if (search.getSearchTerm().length() > 0)
                    search.setResult( history.search(search.getSearchTerm().toString()));
                break;

            case NEXT_WORD:
                history.setSearchDirection(SearchDirection.FORWARD);
                if(search.getSearchTerm().length() > 0)
                    search.setResult(history.search(search.getSearchTerm().toString()));
                break;

            case PREV_BIG_WORD:
                if (search.getSearchTerm().length() > 0)
                    search.getSearchTerm().deleteCharAt(search.getSearchTerm().length() - 1);
                break;
            // new doSearch input, append to doSearch
            case ALL:
                search.getSearchTerm().appendCodePoint(search.getInput());
                //check if the new searchTerm will find anything
                String tmpResult = history.search(search.getSearchTerm().toString());
                if(tmpResult == null) {
                    search.getSearchTerm().deleteCharAt(search.getSearchTerm().length()-1);
                }
                else {
                    search.setResult(tmpResult);
                }
                break;
            // pressed enter, ending the doSearch
            case END:
                // Set buffer to the found string.
                if (search.getResult() != null) {
                    consoleBuffer.moveCursor(-buffer.getCursor());
                    consoleBuffer.setBufferLine(search.getResult());
                    consoleBuffer.drawLine();
                    consoleBuffer.out().println();
                    search.setResult( buffer.getLineNoMask());
                    search.setFinished(true);
                    return;
                }
                else {
                    consoleBuffer.moveCursor(-buffer.getCursor());
                    consoleBuffer.setBufferLine("");
                    consoleBuffer.drawLine();
                }
                break;

            //exiting doSearch (with esc)
            case NEXT_BIG_WORD:
                if(search.getResult() != null) {
                    consoleBuffer.moveCursor(-buffer.getCursor());
                    consoleBuffer.setBufferLine(search.getResult());
                    search.setResult(null);
                }
                else {
                    consoleBuffer.moveCursor(-buffer.getCursor());
                    consoleBuffer.setBufferLine("");
                }
                //drawLine();
                break;
            default:
                break;
        }
        // if we're still in doSearch mode, print the doSearch status
        if (consoleBuffer.getEditMode().getCurrentAction() == Action.SEARCH) {
            if (search.getSearchTerm().length() == 0) {
                if(search.getResult() != null)
                    printSearch("", search.getResult());
                else
                    printSearch("", "");
            }
            else {
                if (search.getResult() == null) {
                    //beep();
                }
                else {
                    printSearch(search.getSearchTerm().toString(),
                            search.getResult());
                }
            }
        }
        // otherwise, restore the line
        else {
            consoleBuffer.drawLine();
            consoleBuffer.out().print(Buffer.printAnsi((buffer.getPrompt().getLength() + 1) + "G"));
            consoleBuffer.out().flush();
        }
    }

    private void complete() {
        if(completionHandler != null) {
            try {
                completionHandler.complete(consoleBuffer.out(), consoleBuffer.getBuffer());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * If movement == PREV setting VI mode
     * if movement == NEXT setting EMACS mode
     *
     * @param movement specifying vi/emacs mode
     */
    private void changeEditMode(Movement movement) {
        if(consoleBuffer.getEditMode().getMode() == Mode.EMACS && movement == Movement.PREV) {
            settings.switchMode();
            settings.resetEditMode();
        }
        else if(consoleBuffer.getEditMode().getMode() == Mode.VI && movement == Movement.NEXT) {
            settings.switchMode();
            settings.resetEditMode();
        }
        consoleBuffer.setEditMode(settings.getEditMode());
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

        prevAction = Action.HISTORY;
        if(fromHistory != null) {
            consoleBuffer.setBufferLine(fromHistory);
            consoleBuffer.moveCursor(-buffer.getCursor() + buffer.length());
            consoleBuffer.drawLine();
        }
    }

    private void addToHistory(String line) {
        if(!settings.isHistoryDisabled())
            history.push(line);
    }

    /**
     * A simple hack to ensure that delete works when masking is enabled and
     * the mask character is set to null (empty).
     * The only operation that will work when the mask character is set to 0 is
     * delete.
     *
     * @throws IOException
     */
    private void deleteWithMaskEnabled() throws IOException {
        if(buffer.getLineNoMask().length() > 0)
            buffer.delete(buffer.getLineNoMask().length()-1, buffer.getLineNoMask().length());
    }


    private void printSearch(String searchTerm, String result) throws IOException {
        //cursor should be placed at the index of searchTerm
        int cursor = result.indexOf(searchTerm);

        StringBuilder builder;
        if(history.getSearchDirection() == SearchDirection.REVERSE)
            builder = new StringBuilder("(reverse-i-search) `");
        else
            builder = new StringBuilder("(forward-i-search) `");
        builder.append(searchTerm).append("': ");
        cursor += builder.length();
        builder.append(result);
        buffer.disablePrompt(true);
        consoleBuffer.moveCursor(-buffer.getCursor());
        consoleBuffer.out().print(ANSI.moveCursorToBeginningOfLine());
        consoleBuffer.out().print(ANSI.getStart() + "2K");
        consoleBuffer.setBufferLine(builder.toString());
        consoleBuffer.moveCursor(cursor);
        consoleBuffer.drawLine(buffer.getLine());
        buffer.disablePrompt(false);
        consoleBuffer.out().flush();
    }

    /**
     * Perform an undo
     *
     * @throws IOException if redraw fails
     */
    private void undo() throws IOException {
        UndoAction ua = consoleBuffer.getUndoManager().getNext();
        if(ua != null) {
            consoleBuffer.setBufferLine(ua.getBuffer());
            consoleBuffer.drawLine();
            consoleBuffer.moveCursor(ua.getCursorPosition() - buffer.getCursor());
        }
    }

}
