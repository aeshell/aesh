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
package org.aesh.selector;

import org.aesh.command.shell.Shell;
import org.aesh.readline.Prompt;
import org.aesh.readline.terminal.Key;
import org.aesh.terminal.utils.ANSI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.aesh.terminal.utils.ANSI.MOVE_LINE_DOWN;
import static org.aesh.terminal.utils.ANSI.MOVE_LINE_UP;

public class Selector {

    private final SelectorType type;
    private final ArrayList<String> defaultValues;
    private final String message;

    public Selector(SelectorType type, String[] defaultValues, String message) {
       this(type, Arrays.asList(defaultValues), message);
    }

    /**
     * Refactoring using: Extract Method
     * For better Readability and Code Understanding I used the Extract Method for Refactoring the below code
     * validateSelectorType and initializeDefaultValues methods were extracted from the constructor.
     * @param type
     * @param defaultValues
     * @param message
     */
    public Selector(SelectorType type, List<String> defaultValues, String message) {
        this.type = validateSelectorType(type);
        this.defaultValues = initializeDefaultValues(defaultValues);
        this.message = message;
    }

    private SelectorType validateSelectorType(SelectorType type) {
        if (type == null)
            throw new IllegalArgumentException("SelectorType can not be null");
        return type;
    }

    private ArrayList<String> initializeDefaultValues(List<String> defaultValues) {
        ArrayList<String> values = new ArrayList<>();
        if (defaultValues != null)
            values.addAll(defaultValues);
        return values;
    }

    /**
     * Refactoring using: Renamed method
     * doSelect was renamed to performSelection, for better readability.
     * @param shell
     * @return List of Strings
     * @throws InterruptedException
     */
    public List<String> performSelection(Shell shell) throws InterruptedException {
       if(type == SelectorType.INPUT)
           return input(shell);
       else if(type == SelectorType.PASSWORD)
           return password(shell);
       else if(type == SelectorType.SELECT)
           return select(shell);
       else if(type == SelectorType.SELECTIONS)
           return multiSelect(shell);
       else
           return new ArrayList<>();
    }

    private List<String> multiSelect(Shell shell) {
        MultiSelect multiSelect = new MultiSelect(shell, defaultValues, message);
        return multiSelect.doSelect();
    }


    /**
     * Refactoring using: Decompose conditional
     * select method was having Complicated conditional logic with their actions,
     * make code harder to read and understand, So I Introduced new methods for conditional logic,
     * as well as action block and gave them meaningful names, then replaced that conditional logic with,
     * newly created method calls! printDefaultValues(), handleKeyPress(), isSelectionKey(), finalizeSelection(),
     * isArrowUp(), isArrowDown(), moveFocusUp(), and moveFocusDown() methods used
     * @param shell
     * @return List of Strings
     */
    private List<String> select(Shell shell) {
        List<String> out = new ArrayList<>(1);

        printDefaultValues(shell);

        shell.write(ANSI.CURSOR_HIDE);
        int[] moveToFirstLine = new int[]{27, '[', 48 + defaultValues.size(), 'A'};
        shell.write(moveToFirstLine);

        boolean waitingForEnter = true;
        int focusLine = 0;
        while (waitingForEnter) {
            try {
                Key key = shell.read();
                handleKeyPress(shell, out, key, focusLine);
                if (isSelectionKey(key)) {
                    waitingForEnter = false;
                    finalizeSelection(shell, focusLine);
                } else if (isArrowUp(key)) {
                    focusLine = moveFocusUp(shell, focusLine);
                } else if (isArrowDown(key)) {
                    focusLine = moveFocusDown(shell, focusLine);
                }
            } catch (InterruptedException e) {
                finalizeSelection(shell, focusLine);
            }
        }

        return out;
    }

    private void printDefaultValues(Shell shell) {
        shell.writeln(message + "  [Use arrow up/down to move and enter/space to select]");
        for (int i = 0; i < defaultValues.size(); i++) {
            if (i == 0)
                shell.writeln("> " + defaultValues.get(i));
            else
                shell.writeln("  " + defaultValues.get(i));
        }
    }

    private void handleKeyPress(Shell shell, List<String> out, Key key, int focusLine) {
        if (isSelectionKey(key)) {
            out.add(defaultValues.get(focusLine));
        }
    }

    private boolean isSelectionKey(Key key) {
        return key == Key.ENTER || key == Key.ENTER_2 || key == Key.CTRL_M || key == Key.SPACE;
    }

    private void finalizeSelection(Shell shell, int focusLine) {
        int moveDown = defaultValues.size() - focusLine;
        shell.write(new int[]{27, '[', 48 + moveDown, 'B'});
        shell.write(ANSI.CURSOR_SHOW);
    }

    private boolean isArrowUp(Key key) {
        return key == Key.UP || key == Key.UP_2;
    }

    private boolean isArrowDown(Key key) {
        return key == Key.DOWN || key == Key.DOWN_2;
    }

    private int moveFocusUp(Shell shell, int focusLine) {
        if (focusLine > 0) {
            focusLine--;
            shell.write(' ');
            shell.write(MOVE_LINE_UP);
            shell.write(ANSI.CURSOR_START);
            shell.write('>');
            shell.write(ANSI.CURSOR_START);
        }
        return focusLine;
    }

    private int moveFocusDown(Shell shell, int focusLine) {
        if (focusLine < defaultValues.size() - 1) {
            focusLine++;
            shell.write(' ');
            shell.write(MOVE_LINE_DOWN);
            shell.write(ANSI.CURSOR_START);
            shell.write('>');
            shell.write(ANSI.CURSOR_START);
        }
        return focusLine;
    }

    /**
     * Refactoring using: Renamed method
     * changed pass to password giving meaningful name!
     * @param shell
     * @return List of Strings
     * @throws InterruptedException
     */
    private List<String> password(Shell shell) throws InterruptedException {
        List<String> out = new ArrayList<>(1);
        out.add(shell.readLine(new Prompt(message+" ", '*')));
        return out;
    }

    private List<String> input(Shell shell) throws InterruptedException {
       List<String> out = new ArrayList<>(1);
       out.add(shell.readLine(new Prompt(message+" ")));
       return out;
    }
}
