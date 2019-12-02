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

    public Selector(SelectorType type, List<String> defaultValues, String message) {
        if(type == null)
            throw new IllegalArgumentException("SelectorType can not be null");
       this.type = type;
       this.defaultValues = new ArrayList<>();
       if(defaultValues != null)
           this.defaultValues.addAll(defaultValues);

       this.message = message;
    }

    public List<String> doSelect(Shell shell) throws InterruptedException {
       if(type == SelectorType.INPUT)
           return input(shell);
       else if(type == SelectorType.PASSWORD)
           return passwd(shell);
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

    private List<String> select(Shell shell) {
        List<String> out = new ArrayList<>(1);

        shell.writeln(message+"  [Use arrow up/down to move and enter/space to select]");
        for(int i=0; i < defaultValues.size();i++) {
            if(i == 0)
                shell.writeln("> "+defaultValues.get(i));
            else
                shell.writeln("  "+defaultValues.get(i));
        }
        shell.write(ANSI.CURSOR_HIDE);
        int[] moveToFirstLine = new int[]{ 27, '[', 48+defaultValues.size(), 'A'};
        shell.write(moveToFirstLine);

        boolean waitingForEnter = true;
        int focusLine = 0;
        while(waitingForEnter) {
            try {
                Key in = shell.read();
                if(in == Key.ENTER || in == Key.ENTER_2 || in == Key.CTRL_M || in == Key.SPACE) {
                    waitingForEnter = false;
                    out.add(defaultValues.get(focusLine));
                    int moveDown = defaultValues.size()-focusLine;
                    shell.write(new int[]{27,'[',48+moveDown,'B'});
                    shell.write(ANSI.CURSOR_SHOW);
                }
                else if(in == Key.UP || in == Key.UP_2) {
                    if(focusLine > 0) {
                        focusLine--;
                        shell.write(' ');
                        shell.write(MOVE_LINE_UP);
                        shell.write(ANSI.CURSOR_START);
                        shell.write('>');
                        shell.write(ANSI.CURSOR_START);
                    }
                }
                else if(in == Key.DOWN || in == Key.DOWN_2) {
                    if(focusLine < defaultValues.size()-1) {
                        focusLine++;
                        shell.write(' ');
                        shell.write(MOVE_LINE_DOWN);
                        shell.write(ANSI.CURSOR_START);
                        shell.write('>');
                        shell.write(ANSI.CURSOR_START);
                    }
                }
            }
            catch(InterruptedException e) {
                int moveDown = defaultValues.size()-focusLine;
                shell.write(new int[]{27,'[',48+moveDown,'B'});
                shell.write(ANSI.CURSOR_SHOW);
            }
        }

        return out;
    }

    private List<String> passwd(Shell shell) throws InterruptedException {
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
