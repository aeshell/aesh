/*
 * JBoss, Home of Professional Open Source
 * Copyright 2019 Red Hat Inc. and/or its affiliates and other contributors
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
package org.aesh.selector;

import org.aesh.command.shell.Shell;
import org.aesh.readline.terminal.Key;
import org.aesh.terminal.utils.ANSI;
import org.aesh.terminal.utils.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.aesh.terminal.utils.ANSI.CURSOR_START;
import static org.aesh.terminal.utils.ANSI.MOVE_LINE_DOWN;
import static org.aesh.terminal.utils.ANSI.MOVE_LINE_UP;

public class MultiSelect {

    private final Shell shell;
    private boolean pagination;
    private final String message;
    private int maxDisplayedLines = 0;
    private List<SelectLine> lines;
    private Page page;
    private int focusLine = 0;

    public MultiSelect(Shell shell, List<String> defaultValues, String message) {
        this(shell, message);
        lines = new ArrayList<>(defaultValues.size());
        for(String value : defaultValues)
            lines.add(new SelectLine(value, shell.size().getWidth()));

        pagination = lines.size()+1 > shell.size().getHeight();
    }

    public MultiSelect(Shell shell, String message) {
        this.shell = shell;
        this.message = message;

        maxDisplayedLines = shell.size().getHeight() - 1;
    }

    public void setLines(List<SelectLine> lines) {
        this.lines = new ArrayList<>(lines.size());
        this.lines.addAll(lines);

        pagination = lines.size()+1 > shell.size().getHeight();
    }

    public List<String> doSelect() {
        shell.write(ANSI.CURSOR_HIDE);
        //lets use the other buffer
        if(pagination) {
            shell.write(ANSI.ALTERNATE_BUFFER);
            //write header
        }
        //first we set the first item as focused and display page
        lines.get(focusLine).focus();
        page = new Page(focusLine, calcNumOfDisplayableLines());
        displayPage(page.top(), page.bottom(), focusLine);

        boolean waitingForEnter = true;

        while(waitingForEnter) {
            try {
                Key in = shell.read();
                if(in == Key.ENTER || in == Key.ENTER_2 || in == Key.CTRL_M) {
                    waitingForEnter = false;
                    if(!pagination)
                        shell.write(ANSI.moveRowsDown(page.bottom() - focusLine));

                    shell.write(ANSI.CURSOR_SHOW);
                }
                else if(in == Key.SPACE) {
                    lines.get(focusLine).select();
                    shell.write(lines.get(focusLine).print());
                    shell.write(ANSI.CURSOR_START);
                }
                else if(in == Key.UP || in == Key.UP_2) {
                    if(focusLine > page.top()) {
                        moveUp();
                    }
                    else if(page.top() > 0) {
                        page = new Page(page.top-1, page.bottom()-1);
                        displayPage(page.top(), maxDisplayedLines, focusLine);
                        moveUp();
                    }
                }
                else if(in == Key.PGUP || in == Key.PGUP_2) {
                    //we're already at the bottom, let's try to move the focusLine to the bottom
                    if(page.top() == 0) {
                        //System.out.print("page.top="+page.top+", focusLine="+focusLine);
                        if(focusLine > page.top()) {
                            moveUp(focusLine);
                        }
                    }
                    //try to move focusLine up with maxDisplayedLines
                    else if(page.top() - maxDisplayedLines < 0) {
                        int diffUp = page.top();
                        if(diffUp > 0) {
                            page = new Page(page.top - diffUp, page.bottom() - diffUp);
                            updateFocusLineGoingUp();
                       }
                    }
                    else if(page.top() - maxDisplayedLines >= 0) {
                        page = new Page(page.top-maxDisplayedLines, page.bottom()-maxDisplayedLines);
                        updateFocusLineGoingUp();
                    }
                }
                else if(in == Key.DOWN || in == Key.DOWN_2) {
                    if(focusLine < page.bottom()-1) {
                        moveDown();
                    }
                    else if(page.bottom() < lines.size()) {
                        page = new Page(page.top+1, page.bottom()+1);
                        displayPage(page.top(), maxDisplayedLines, focusLine);
                        moveDown();
                    }
                }
                else if(in == Key.PGDOWN || in == Key.PGDOWN_2) {
                    //we're already at the bottom, let's try to move the focusLine to the bottom
                    if(page.bottom() == lines.size()) {
                        if(focusLine < page.bottom()-1) {
                            moveDown(page.bottom()-focusLine-1);
                        }
                    }
                    //try to move focusLine down with maxDisplayedLines
                    else if(page.bottom() + maxDisplayedLines > lines.size()) {
                        int diffDown = lines.size() - page.bottom();
                        if(diffDown > 0) {
                            page = new Page(page.top + diffDown, page.bottom() + diffDown);
                            updateFocusLineGoingDown();
                        }
                    }
                    else {
                        page = new Page(page.top+maxDisplayedLines, page.bottom()+maxDisplayedLines);
                        updateFocusLineGoingDown();
                    }
                }
            }
            catch(InterruptedException e) {
                if(!pagination)
                    shell.write(ANSI.moveRowsDown(page.bottom() - focusLine));
                shell.write(ANSI.CURSOR_SHOW);
            }
        }

        //let's move back to the original buffer before returning
        if(pagination)
            shell.write(ANSI.MAIN_BUFFER);

        shell.write(ANSI.CURSOR_SHOW);

        return collectSelected();
    }

    private void updateFocusLineGoingDown() {
        lines.get(focusLine).focus();
        focusLine += maxDisplayedLines;
        if(focusLine > lines.size()-1)
            focusLine = lines.size()-1;
        lines.get(focusLine).focus();
        displayPage(page.top(), maxDisplayedLines, focusLine);
    }

    private void updateFocusLineGoingUp() {
        lines.get(focusLine).focus();
        focusLine -= maxDisplayedLines;
        if(focusLine < 0)
            focusLine = 0;
        lines.get(focusLine).focus();
        displayPage(page.top(), maxDisplayedLines, focusLine);
    }

    private void moveDown(int move) {
        lines.get(focusLine).focus();
        focusLine += move;
        lines.get(focusLine).focus();
        shell.write(' ');
        shell.write(ANSI.moveRowsDown(move));
        shell.write(ANSI.CURSOR_START);
        shell.write('>');
        shell.write(ANSI.CURSOR_START);
    }

    private void moveDown() {
        lines.get(focusLine).focus();
        focusLine++;
        lines.get(focusLine).focus();
        shell.write(' ');
        shell.write(MOVE_LINE_DOWN);
        shell.write(ANSI.CURSOR_START);
        shell.write('>');
        shell.write(ANSI.CURSOR_START);
    }

    private void moveUp() {
        lines.get(focusLine).focus();
        focusLine--;
        lines.get(focusLine).focus();
        shell.write(' ');
        shell.write(MOVE_LINE_UP);
        shell.write(ANSI.CURSOR_START);
        shell.write('>');
        shell.write(ANSI.CURSOR_START);
    }

    private void moveUp(int move) {
        lines.get(focusLine).focus();
        focusLine -= move;
        lines.get(focusLine).focus();
        shell.write(' ');
        shell.write(ANSI.moveRowsUp(move));
        shell.write(ANSI.CURSOR_START);
        shell.write('>');
        shell.write(ANSI.CURSOR_START);
    }

    private int calcNumOfDisplayableLines() {
        if(maxDisplayedLines < lines.size())
            return maxDisplayedLines;
        else
            return lines.size();
    }

    private void displayPage(int startLine, int numberOfLines, int focusLine) {
        StringBuilder builder = new StringBuilder();
        builder.append(message+"  [Use arrow up/down to move and space to select. Enter to finish]");
        for(int i = startLine; i < (startLine+numberOfLines); i++) {
            builder.append(Config.getLineSeparator());
            builder.append(lines.get(i).print());
        }
        if(pagination)
            shell.write(ANSI.CLEAR_SCREEN);

        shell.write(builder.toString());
        shell.write(CURSOR_START);
        if(focusLine < startLine + numberOfLines-1) {
            shell.write(ANSI.moveRowsUp(startLine + numberOfLines - focusLine -1));
        }
    }

    private List<String> collectSelected() {
        return lines.stream().filter(SelectLine::isSelected).map(SelectLine::value).collect(Collectors.toList());
    }

    class Page {
        private int top;
        private int bottom;

        Page(int top, int bottom) {
            this.top = top;
            this.bottom = bottom;
        }

        public int top() {
            return top;
        }

        public void setTop(int top) {
            this.top = top;
        }

        public int bottom() {
            return bottom;
        }

        public void setBottom(int bottom) {
            this.bottom = bottom;
        }

        @Override
        public String toString() {
            return "Page{" +
                    "top=" + top +
                    ", bottom=" + bottom +
                    '}';
        }
    }

}
