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
package org.aesh.command.man;

import org.aesh.command.shell.Shell;
import org.aesh.command.Command;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.readline.action.KeyAction;
import org.aesh.readline.terminal.Key;
import org.aesh.terminal.utils.ANSI;
import org.aesh.terminal.utils.Config;
import org.aesh.readline.util.LoggerUtil;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

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
    private boolean stop;

    public AeshFileDisplayer() {
        stop = false;
    }

    protected void setCommandInvocation(CommandInvocation commandInvocation) {
        this.commandInvocation = commandInvocation;
        //setControlOperator(commandInvocation.getControlOperator());
    }

    protected CommandInvocation getCommandInvocation() {
        return commandInvocation;
    }

    protected Shell getShell() {
        return commandInvocation.getShell();
    }

    protected void afterAttach() throws IOException, InterruptedException {
        number = new StringBuilder();
        searchBuilder = new StringBuilder();
        rows = getShell().size().getHeight();
        columns = getShell().size().getWidth();
        page = new TerminalPage(getFileParser(), columns);
        topVisibleRow = 0;
        topVisibleRowCache = -1;
        stop = false;

        if(commandInvocation.getConfiguration().hasOutputRedirection()) {
            int count = 0;
            for (String line : this.page.getLines()) {
                commandInvocation.print(line);
                count++;
                if (count < this.page.size())
                    commandInvocation.print(Config.getLineSeparator());
            }
            page.clear();
        }
        else {

            if(!page.hasData()) {
                getShell().write("error: input is null...");
                afterDetach();
            }
            else {
                getShell().write(ANSI.ALTERNATE_BUFFER);

                if(this.page.getFileName() != null)
                    display();
                else
                    display();

                processInput();
            }
        }
    }

    protected void afterDetach() {
        if(!commandInvocation.getConfiguration().hasOutputRedirection())
            getShell().write(ANSI.MAIN_BUFFER);

        page.clear();
        topVisibleRow = 0;
    }

    public void processInput() throws IOException, InterruptedException {
        try {
            while(!stop) {
                KeyAction event = getCommandInvocation().input();
                if(event instanceof Key)
                    processOperation( (Key) event);
            }
        }
        catch (InterruptedException e) {
            afterDetach();
            stop = true;
            throw e;
        }
    }

    public void processOperation(Key operation) throws IOException {
        if(operation == Key.q) {
            if(search == TerminalPage.Search.SEARCHING) {
                searchBuilder.append((char) operation.getFirstValue());
                displayBottom();
            }
            else {
                clearNumber();
                afterDetach();
                stop = true;
            }
        }
        else if(operation == Key.j ||
                operation == Key.DOWN ||
                operation == Key.DOWN_2 ||
                operation == Key.ENTER ) {
            if(search == TerminalPage.Search.SEARCHING) {
                if(operation == Key.j) {
                    searchBuilder.append((char) operation.getFirstValue());
                    displayBottom();
                }
                else if(operation == Key.ENTER) {
                    search = TerminalPage.Search.RESULT;
                    findSearchWord(true);
                }
            }
            else if(search == TerminalPage.Search.NOT_FOUND) {
               if(operation == Key.ENTER) {
                   search = TerminalPage.Search.NO_SEARCH;
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
        else if(operation == Key.k ||
                operation == Key.UP ||
                operation == Key.UP_2) {
            if(search == TerminalPage.Search.SEARCHING) {
                if(operation == Key.k)
                searchBuilder.append((char) operation.getFirstValue());
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
        else if(operation == Key.CTRL_F ||
                operation == Key.PGDOWN ||
                operation == Key.SPACE) { // ctrl-f || pgdown || space
            if(search == TerminalPage.Search.SEARCHING) {

            }
            else {
                topVisibleRow = topVisibleRow + ((rows - 1) * getNumber());
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
        else if(operation == Key.CTRL_B ||
                operation == Key.PGUP) { // ctrl-b || pgup
            if(search != TerminalPage.Search.SEARCHING) {
                topVisibleRow = topVisibleRow - ((rows - 1) * getNumber());
                if(topVisibleRow < 0)
                    topVisibleRow = 0;
                display();
                clearNumber();
            }
        }
        //search
        else if(operation == Key.SLASH) {
            if(search == TerminalPage.Search.NO_SEARCH || search == TerminalPage.Search.RESULT) {
                search = TerminalPage.Search.SEARCHING;
                searchBuilder = new StringBuilder();
                displayBottom();
            }
            else if(search == TerminalPage.Search.SEARCHING) {
                searchBuilder.append((char) operation.getFirstValue());
                displayBottom();
            }

        }
        else if(operation == Key.n) {
            if(search == TerminalPage.Search.SEARCHING) {
                searchBuilder.append((char) operation.getFirstValue());
                displayBottom();
            }
            else if(search == TerminalPage.Search.RESULT) {
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
        else if(operation == Key.N) {
            if(search == TerminalPage.Search.SEARCHING) {
                searchBuilder.append((char) operation.getFirstValue());
                displayBottom();
            }
            else if(search == TerminalPage.Search.RESULT) {
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
        else if(operation == Key.G) {
            if(search == TerminalPage.Search.SEARCHING) {
                searchBuilder.append((char) operation.getFirstValue());
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
        else if(operation.isNumber()) {
            if(search == TerminalPage.Search.SEARCHING) {
                searchBuilder.append((char) operation.getFirstValue());
                displayBottom();
            }
            else {
                number.append(Character.getNumericValue(operation.getFirstValue()));
                display();
            }
        }
        else {
            if(search == TerminalPage.Search.SEARCHING &&
                    (Character.isAlphabetic(operation.getFirstValue()))) {
                searchBuilder.append((char) operation.getFirstValue());
                displayBottom();
            }
        }
    }

    private void display() throws IOException {
        if(topVisibleRow != topVisibleRowCache) {
            getShell().clear();
            if(search == TerminalPage.Search.RESULT && searchLines.size() > 0) {
                String searchWord = searchBuilder.toString();
                for(int i=topVisibleRow; i < (topVisibleRow+rows-1); i++) {
                    if(i < page.size()) {
                        String line = page.getLine(i);
                        if(line.contains(searchWord))
                            displaySearchLine(line, searchWord);
                        else
                            getShell().write(line);
                        getShell().write(Config.getLineSeparator());
                    }
                }
                topVisibleRowCache = topVisibleRow;
            }
            else {
                for(int i=topVisibleRow; i < (topVisibleRow+rows-1); i++) {
                    if(i < page.size()) {
                        getShell().write(page.getLine(i)+ Config.getLineSeparator());
                    }
                }
                topVisibleRowCache = topVisibleRow;
            }
            displayBottom();
        }
    }

    /**
     * highlight the specific word thats found in the search
     */
    private void displaySearchLine(String line, String searchWord) throws IOException {
        int start = line.indexOf(searchWord);
        getShell().write(line.substring(0,start));
        getShell().write(ANSI.INVERT_BACKGROUND);
        getShell().write(searchWord);
        getShell().write(ANSI.RESET);
        getShell().write(line.substring(start + searchWord.length(), line.length()));
    }

    public abstract FileParser getFileParser();

    public abstract void displayBottom() throws IOException;

    public void writeToConsole(String word) throws IOException {
        getShell().write(word);
    }

    public void clearBottomLine() throws IOException {
        getShell().write(ANSI.printAnsi("0G"));
        getShell().write(ANSI.printAnsi("2K"));
    }

    public boolean isAtBottom() {
        return topVisibleRow >= (page.size()-rows-1);
    }

    public boolean isAtTop() {
        return topVisibleRow == 0;
    }

    public TerminalPage.Search getSearchStatus() {
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
            search = TerminalPage.Search.NOT_FOUND;
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

    private enum Background {
        NORMAL,
        INVERSE
    }

}
