/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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

import static org.aesh.terminal.utils.ANSI.MOVE_LINE_DOWN;
import static org.aesh.terminal.utils.ANSI.MOVE_LINE_UP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.aesh.command.shell.Shell;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.Key;
import org.aesh.terminal.utils.ANSI;

/**
 * Interactive selector components for terminal prompts.
 * <p>
 * Used both by the annotation-based selector system ({@code @Option(selector=...)})
 * and programmatically from {@code CommandInvocation} convenience methods.
 */
public class Selector {

    private final SelectorType type;
    private final ArrayList<String> defaultValues;
    private final String message;

    public Selector(SelectorType type, String[] defaultValues, String message) {
        this(type, Arrays.asList(defaultValues), message);
    }

    public Selector(SelectorType type, List<String> defaultValues, String message) {
        if (type == null)
            throw new IllegalArgumentException("SelectorType can not be null");
        this.type = type;
        this.defaultValues = new ArrayList<>();
        if (defaultValues != null)
            this.defaultValues.addAll(defaultValues);
        this.message = message;
    }

    public List<String> doSelect(Shell shell) throws InterruptedException {
        switch (type) {
            case INPUT:
                return input(shell);
            case PASSWORD:
                return passwd(shell);
            case SELECT:
                return select(shell);
            case MULTI_SELECT:
            case SELECTIONS:
                return multiSelect(shell);
            case CONFIRM:
                List<String> result = new ArrayList<>(1);
                result.add(String.valueOf(confirm(shell, message,
                        !defaultValues.isEmpty() && "true".equalsIgnoreCase(defaultValues.get(0)))));
                return result;
            case EXPAND:
                // For annotation-based expand, defaultValues are "key:description" pairs
                return expandFromDefaults(shell);
            default:
                return new ArrayList<>();
        }
    }

    // --- Static convenience methods for programmatic use ---

    /**
     * Show a yes/no confirmation prompt.
     *
     * @param shell the shell for I/O
     * @param message the question to ask
     * @param defaultValue the value returned when Enter is pressed
     * @return true for yes, false for no
     */
    public static boolean confirm(Shell shell, String message, boolean defaultValue) throws InterruptedException {
        String hint = defaultValue ? "(Y/n)" : "(y/N)";
        shell.write("? " + message + " " + hint + " ");
        while (true) {
            Key key = shell.read();
            if (key == Key.y || key == Key.Y) {
                shell.writeln("Yes");
                return true;
            }
            if (key == Key.n || key == Key.N) {
                shell.writeln("No");
                return false;
            }
            if (key == Key.ENTER || key == Key.ENTER_2 || key == Key.CTRL_M) {
                shell.writeln(defaultValue ? "Yes" : "No");
                return defaultValue;
            }
        }
    }

    /**
     * Show a single-select list. Returns the selected value.
     *
     * @param shell the shell for I/O
     * @param message the prompt message
     * @param choices the list of choices
     * @return the selected value, or null if no selection
     */
    public static String singleSelect(Shell shell, String message, List<String> choices) throws InterruptedException {
        if (choices == null || choices.isEmpty())
            return null;

        List<SelectLine> lines = new ArrayList<>(choices.size());
        for (String choice : choices)
            lines.add(new SelectLine(choice, shell.size().getWidth()));

        shell.writeln(message + "  [arrow keys to move, Enter to select]");
        lines.get(0).setFocus(true);

        // Render initial list
        for (SelectLine line : lines)
            shell.writeln(line.printSingleSelect());

        shell.write(ANSI.CURSOR_HIDE);
        shell.write(ANSI.moveRowsUp(lines.size()));

        int focusLine = 0;
        while (true) {
            try {
                Key in = shell.read();
                if (in == Key.ENTER || in == Key.ENTER_2 || in == Key.CTRL_M || in == Key.SPACE) {
                    shell.write(ANSI.moveRowsDown(lines.size() - focusLine));
                    shell.write(ANSI.CURSOR_SHOW);
                    return choices.get(focusLine);
                }
                if (in == Key.UP || in == Key.UP_2) {
                    if (focusLine > 0) {
                        lines.get(focusLine).setFocus(false);
                        // Re-render current line
                        shell.write(ANSI.CURSOR_START);
                        shell.write(ANSI.ERASE_WHOLE_LINE);
                        shell.write(lines.get(focusLine).printSingleSelect());
                        focusLine--;
                        lines.get(focusLine).setFocus(true);
                        shell.write(MOVE_LINE_UP);
                        shell.write(ANSI.CURSOR_START);
                        shell.write(ANSI.ERASE_WHOLE_LINE);
                        shell.write(lines.get(focusLine).printSingleSelect());
                        shell.write(ANSI.CURSOR_START);
                    }
                } else if (in == Key.DOWN || in == Key.DOWN_2) {
                    if (focusLine < lines.size() - 1) {
                        lines.get(focusLine).setFocus(false);
                        shell.write(ANSI.CURSOR_START);
                        shell.write(ANSI.ERASE_WHOLE_LINE);
                        shell.write(lines.get(focusLine).printSingleSelect());
                        focusLine++;
                        lines.get(focusLine).setFocus(true);
                        shell.write(MOVE_LINE_DOWN);
                        shell.write(ANSI.CURSOR_START);
                        shell.write(ANSI.ERASE_WHOLE_LINE);
                        shell.write(lines.get(focusLine).printSingleSelect());
                        shell.write(ANSI.CURSOR_START);
                    }
                }
            } catch (InterruptedException e) {
                shell.write(ANSI.moveRowsDown(lines.size() - focusLine));
                shell.write(ANSI.CURSOR_SHOW);
                throw e;
            }
        }
    }

    /**
     * Show an expandable choice prompt where each option has a key.
     *
     * @param shell the shell for I/O
     * @param message the prompt message
     * @param choices map of key character to description
     * @return the selected key as a string
     */
    public static String expandChoice(Shell shell, String message, Map<Character, String> choices)
            throws InterruptedException {
        // Build hint string: (Ynagh)
        StringBuilder hint = new StringBuilder("(");
        for (Character key : choices.keySet())
            hint.append(key);
        hint.append(")");

        shell.write("? " + message + " " + hint + " ");

        while (true) {
            Key in = shell.read();
            // Check if the key matches any choice
            for (Map.Entry<Character, String> entry : choices.entrySet()) {
                char choiceKey = entry.getKey();
                if (in.getFirstValue() == choiceKey || in.getFirstValue() == Character.toUpperCase(choiceKey)) {
                    shell.writeln(String.valueOf(choiceKey));
                    return String.valueOf(choiceKey);
                }
            }
            // '?' shows help
            if (in == Key.QUESTION_MARK) {
                shell.writeln("");
                for (Map.Entry<Character, String> entry : choices.entrySet()) {
                    shell.writeln("  " + entry.getKey() + ") " + entry.getValue());
                }
                shell.write("? " + message + " " + hint + " ");
            }
        }
    }

    /**
     * Prompt for text input with validation.
     *
     * @param shell the shell for I/O
     * @param message the prompt message
     * @param validator function that returns null if valid, or an error message if invalid
     * @return the validated input string
     */
    public static String inputWithValidation(Shell shell, String message, Function<String, String> validator)
            throws InterruptedException {
        while (true) {
            String input = shell.readLine(new Prompt(message + " "));
            if (validator == null)
                return input;
            String error = validator.apply(input);
            if (error == null)
                return input;
            shell.writeln("  \u2717 " + error); // ✗ error message
        }
    }

    // --- Private methods for annotation-based flow ---

    private List<String> multiSelect(Shell shell) {
        MultiSelect multiSelect = new MultiSelect(shell, defaultValues, message);
        return multiSelect.doSelect();
    }

    private List<String> select(Shell shell) throws InterruptedException {
        String selected = singleSelect(shell, message, defaultValues);
        List<String> result = new ArrayList<>(1);
        if (selected != null)
            result.add(selected);
        return result;
    }

    private List<String> passwd(Shell shell) throws InterruptedException {
        List<String> out = new ArrayList<>(1);
        out.add(shell.readLine(new Prompt(message + " ", '*')));
        return out;
    }

    private List<String> input(Shell shell) throws InterruptedException {
        List<String> out = new ArrayList<>(1);
        out.add(shell.readLine(new Prompt(message + " ")));
        return out;
    }

    private List<String> expandFromDefaults(Shell shell) throws InterruptedException {
        // defaultValues format: "key:description" (e.g., "y:Yes, overwrite")
        LinkedHashMap<Character, String> choices = new LinkedHashMap<>();
        for (String entry : defaultValues) {
            int colonIdx = entry.indexOf(':');
            if (colonIdx > 0) {
                choices.put(entry.charAt(0), entry.substring(colonIdx + 1).trim());
            }
        }
        String selected = expandChoice(shell, message, choices);
        List<String> result = new ArrayList<>(1);
        result.add(selected);
        return result;
    }
}
