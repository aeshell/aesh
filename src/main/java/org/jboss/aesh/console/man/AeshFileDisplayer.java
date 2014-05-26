/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.man;

import org.jboss.aesh.console.Buffer;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.CommandOperation;
import org.jboss.aesh.console.operator.ControlOperator;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.terminal.Shell;
import org.jboss.aesh.util.ANSI;
import org.jboss.aesh.util.LoggerUtil;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.jboss.aesh.console.man.TerminalPage.Search;
/**
 * An abstract command used to display files
 * Implemented similar to less
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public abstract class AeshFileDisplayer implements Command {

    private int rows;
    private int columns;
    private int topVisibleRow;
    private int topVisibleRowCache; //only rewrite page if rowCache != row
    private TerminalPage page;
    private StringBuilder number;
    private TerminalPage.Search search = TerminalPage.Search.NO_SEARCH;
    private StringBuilder searchBuilder;
    private List<Integer> searchLines;
    private static final Logger LOGGER = LoggerUtil.getLogger(AeshFileDisplayer.class.getName());
    private CommandInvocation commandInvocation;
    private ControlOperator operation;
    private boolean stop;

    public AeshFileDisplayer() {
        stop = false;
    }

    protected void setCommandInvocation(CommandInvocation commandInvocation) {
        this.commandInvocation = commandInvocation;
        setControlOperator(commandInvocation.getControlOperator());
    }

    protected CommandInvocation getCommandInvocation() {
        return commandInvocation;
    }

    protected void setControlOperator(ControlOperator operator) {
        this.operation = operator;
    }

    protected Shell getShell() {
        return commandInvocation.getShell();
    }

    protected void afterAttach() throws IOException, InterruptedException {
        number = new StringBuilder();
        searchBuilder = new StringBuilder();
        rows = getShell().getSize().getHeight();
        columns = getShell().getSize().getWidth();
        page = new TerminalPage(getFileParser(), columns);
        topVisibleRow = 0;
        topVisibleRowCache = -1;
        stop = false;

        if(operation.isRedirectionOut()) {
            int count=0;
            for(String line : this.page.getLines()) {
                getShell().out().print(line);
                count++;
                if(count < this.page.size())
                    getShell().out().print(Config.getLineSeparator());
            }
            getShell().out().flush();

            afterDetach();
            getShell().out().flush();
        }
        else {
            if(!page.hasData()) {
                getShell().out().println("error: input is null...");
                afterDetach();
            }
            else {
                getShell().out().print(ANSI.getAlternateBufferScreen());

                if(this.page.getFileName() != null)
                    display();
                else
                    display();

                processInput();
            }
        }
    }

    protected void afterDetach() throws IOException {
        if(!operation.isRedirectionOut())
            getShell().out().print(ANSI.getMainBufferScreen());

        page.clear();
        topVisibleRow = 0;
    }

    public void processInput() throws IOException, InterruptedException {
        try {
            while(!stop) {
                processOperation(getCommandInvocation().getInput());
            }
        }
        catch (InterruptedException e) {
            afterDetach();
            stop = true;
            throw e;
        }
    }

    public void processOperation(CommandOperation operation) throws IOException {
        if(operation.getInputKey() == Key.q) {
            if(search == Search.SEARCHING) {
                searchBuilder.append((char) operation.getInput()[0]);
                displayBottom();
            }
            else {
                clearNumber();
                afterDetach();
                stop = true;
            }
        }
        else if(operation.getInputKey() == Key.j ||
                operation.getInputKey() == Key.DOWN ||
                operation.getInputKey() == Key.DOWN_2 ||
                operation.getInputKey() == Key.ENTER ) {
            if(search == Search.SEARCHING) {
                if(operation.getInputKey() == Key.j) {
                    searchBuilder.append((char) operation.getInput()[0]);
                    displayBottom();
                }
                else if(operation.getInputKey() == Key.ENTER) {
                    search = Search.RESULT;
                    findSearchWord(true);
                }
            }
            else if(search == Search.NOT_FOUND) {
               if(operation.getInputKey() == Key.ENTER) {
                   search = Search.NO_SEARCH;
                   clearBottomLine();
                   displayBottom();
               }
            }
            else {
                topVisibleRow = topVisibleRow + getNumber();
                if(topVisibleRow > (page.size()-rows-1)) {
                    topVisibleRow = page.size()-rows-1;
                    if(topVisibleRow < 0)
                        topVisibleRow = 0;
                    display();
                }
                else
                    display();
                clearNumber();
            }
        }
        else if(operation.getInputKey() == Key.k ||
                operation.getInputKey() == Key.UP ||
                operation.getInputKey() == Key.UP_2) {
            if(search == Search.SEARCHING) {
                if(operation.getInputKey() == Key.k)
                searchBuilder.append((char) operation.getInput()[0]);
                displayBottom();
            }
            else {
                topVisibleRow = topVisibleRow - getNumber();
                if(topVisibleRow < 0)
                    topVisibleRow = 0;
                display();
                clearNumber();
            }
        }
        else if(operation.getInputKey() == Key.CTRL_F ||
                operation.getInputKey() == Key.PGDOWN ||
                operation.getInputKey() == Key.SPACE) { // ctrl-f || pgdown || space
            if(search == Search.SEARCHING) {

            }
            else {
                topVisibleRow = topVisibleRow + rows*getNumber()-1;
                if(topVisibleRow > (page.size()-rows-1)) {
                    topVisibleRow = page.size()-rows-1;
                    if(topVisibleRow < 0)
                        topVisibleRow = 0;
                    display();
                }
                else
                    display();
                clearNumber();
            }
        }
        else if(operation.getInputKey() == Key.CTRL_B ||
                operation.getInputKey() == Key.PGUP) { // ctrl-b || pgup
            if(search != Search.SEARCHING) {
                topVisibleRow = topVisibleRow - rows*getNumber()-1;
                if(topVisibleRow < 0)
                    topVisibleRow = 0;
                display();
                clearNumber();
            }
        }
        //search
        else if(operation.getInputKey() == Key.SLASH) {
            if(search == Search.NO_SEARCH || search == Search.RESULT) {
                search = Search.SEARCHING;
                searchBuilder = new StringBuilder();
                displayBottom();
            }
            else if(search == Search.SEARCHING) {
                searchBuilder.append((char) operation.getInput()[0]);
                displayBottom();
            }

        }
        else if(operation.getInputKey() == Key.n) {
            if(search == Search.SEARCHING) {
                searchBuilder.append((char) operation.getInput()[0]);
                displayBottom();
            }
            else if(search == Search.RESULT) {
                if(searchLines.size() > 0) {
                    for(Integer i : searchLines) {
                        if(i > topVisibleRow+1) {
                            topVisibleRow = i-1;
                            display();
                            return;
                        }
                    }
                    //we didnt find any more
                    displayBottom();
                }
                else {
                    displayBottom();
                }
            }
        }
        else if(operation.getInputKey() == Key.N) {
            if(search == Search.SEARCHING) {
                searchBuilder.append((char) operation.getInput()[0]);
                displayBottom();
            }
            else if(search == Search.RESULT) {
                if(searchLines.size() > 0) {
                    for(int i=searchLines.size()-1; i >= 0; i--) {
                        if(searchLines.get(i) < topVisibleRow) {
                            topVisibleRow = searchLines.get(i)-1;
                            if(topVisibleRow < 0)
                                topVisibleRow = 0;
                            display();
                            return;
                        }
                    }
                    //we didnt find any more
                    displayBottom();
                }
            }
        }
        else if(operation.getInputKey() == Key.G) {
            if(search == Search.SEARCHING) {
                searchBuilder.append((char) operation.getInput()[0]);
                displayBottom();
            }
            else {
                if(number.length() == 0 || getNumber() == 0) {
                    topVisibleRow = page.size()-rows-1;
                    display();
                }
                else {
                    topVisibleRow = getNumber()-1;
                    if(topVisibleRow > page.size()-rows-1) {
                        topVisibleRow = page.size()-rows-1;
                        display();
                    }
                    else {
                        display();
                    }
                }
                clearNumber();
            }
        }
        else if(operation.getInputKey().isNumber()) {
            if(search == Search.SEARCHING) {
                searchBuilder.append((char) operation.getInput()[0]);
                displayBottom();
            }
            else {
                number.append(Character.getNumericValue(operation.getInput()[0]));
                display();
            }
        }
        else {
            if(search == Search.SEARCHING &&
                    (Character.isAlphabetic(operation.getInput()[0]))) {
                searchBuilder.append((char) operation.getInput()[0]);
                displayBottom();
            }
        }
    }

    private void display() throws IOException {
        if(topVisibleRow != topVisibleRowCache) {
            getShell().clear();
            if(search == Search.RESULT && searchLines.size() > 0) {
                String searchWord = searchBuilder.toString();
                for(int i=topVisibleRow; i < (topVisibleRow+rows-1); i++) {
                    if(i < page.size()) {
                        String line = page.getLine(i);
                        if(line.contains(searchWord))
                            displaySearchLine(line, searchWord);
                        else
                            getShell().out().print(line);
                        getShell().out().print(Config.getLineSeparator());
                    }
                }
                topVisibleRowCache = topVisibleRow;
            }
            else {
                for(int i=topVisibleRow; i < (topVisibleRow+rows-1); i++) {
                    if(i < page.size()) {
                        getShell().out().print(page.getLine(i)+ Config.getLineSeparator());
                    }
                }
                topVisibleRowCache = topVisibleRow;
            }
            displayBottom();
        }
        getShell().out().flush();
    }

    /**
     * highlight the specific word thats found in the search
     */
    private void displaySearchLine(String line, String searchWord) throws IOException {
        int start = line.indexOf(searchWord);
        getShell().out().print(line.substring(0,start));
        getShell().out().print(ANSI.getInvertedBackground());
        getShell().out().print(searchWord);
        getShell().out().print(ANSI.reset());
        getShell().out().print(line.substring(start + searchWord.length(), line.length()));
        getShell().out().flush();
    }

    public abstract FileParser getFileParser();

    public abstract void displayBottom() throws IOException;

    public void writeToConsole(String word) throws IOException {
        getShell().out().print(word);
        getShell().out().flush();
    }

    public void clearBottomLine() throws IOException {
        getShell().out().print(Buffer.printAnsi("0G"));
        getShell().out().print(Buffer.printAnsi("2K"));
        getShell().out().flush();
    }

    public boolean isAtBottom() {
        return topVisibleRow >= (page.size()-rows-1);
    }

    public boolean isAtTop() {
        return topVisibleRow == 0;
    }

    public Search getSearchStatus() {
        return search;
    }

    public String getSearchWord() {
        return searchBuilder.toString();
    }

    public int getTopVisibleRow() {
        return topVisibleRow+1;
    }

    private void findSearchWord(boolean forward) throws IOException {
        LOGGER.info("searching for: " + searchBuilder.toString());
        searchLines = page.findWord(searchBuilder.toString());
        LOGGER.info("found: "+searchLines);
        if(searchLines.size() > 0) {
            for(Integer i : searchLines)
                if(i > topVisibleRow) {
                    topVisibleRow = i-1;
                    display();
                    return;
                }
        }
        else {
            search = Search.NOT_FOUND;
            displayBottom();
        }
    }

    /**
     * number written by the user (used to jump to specific commands)
     */
    private int getNumber() {
        if(number.length() > 0)
            return Integer.parseInt(number.toString());
        else
            return 1;
    }

    private void clearNumber() {
        number = new StringBuilder();
    }

    private static enum Background {
        NORMAL,
        INVERSE
    }

}
