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
package org.aesh.command.export;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aesh.terminal.utils.Config;
import org.aesh.terminal.utils.LoggerUtil;

/**
 * @author Aesh team
 */
public class ExportManager {

    private static final char DOLLAR = '$';
    private final Map<String, String> variables;
    private final Pattern exportPattern = Pattern.compile("^(export)\\s+(\\w+)\\s*=\\s*(\\S+).*$");

    /**
     * Index of the closing brace matching the opening brace at
     * {@code openIndex} (which points at the {@code $} of {@code ${}),
     * or -1 when unbalanced.
     */
    private static int findClosingBrace(String value, int openIndex) {
        int depth = 0;
        for (int i = openIndex + 1; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '{')
                depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0)
                    return i;
            }
        }
        return -1;
    }

    private static final Logger LOGGER = LoggerUtil.getLogger(ExportManager.class.getName());

    private static final String EXPORT = "export";

    private final File exportFile;
    private final boolean exportUsesSystemEnvironment;
    private final ExportChangeListener listener;

    public ExportManager(File exportFile) {
        this(exportFile, false, null);
    }

    public ExportManager(File exportFile, ExportChangeListener listener) {
        this(exportFile, false, listener);
    }

    public ExportManager(File exportFile, boolean exportUsesSystemEnvironment) {
        this(exportFile, exportUsesSystemEnvironment, null);
    }

    public ExportManager(File exportFile, boolean exportUsesSystemEnvironment, ExportChangeListener listener) {
        this.exportFile = exportFile;
        this.exportUsesSystemEnvironment = exportUsesSystemEnvironment;
        variables = new HashMap<>();
        if (exportFile.isFile())
            readVariablesFromFile();
        this.listener = listener;
    }

    private void readVariablesFromFile() {
        try (BufferedReader br = new BufferedReader(new FileReader(exportFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(EXPORT))
                    addVariable(line);
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to read variables from file " + exportFile + ", error: " + e);
        }
    }

    public Set<String> keys() {
        return variables.keySet();
    }

    public String addVariable(String line) {
        Matcher variableMatcher = exportPattern.matcher(line);
        if (variableMatcher.matches()) {
            String name = variableMatcher.group(2);
            String value = variableMatcher.group(3);
            // Handle self-reference in both $NAME and ${NAME} forms
            String dollarName = DOLLAR + name;
            String bracedName = "${" + name + "}";
            if (value.contains(dollarName) || value.contains(bracedName)) {
                String existing = variables.get(name);
                String replacement = existing != null ? existing : "";
                value = value.replace(bracedName, replacement);
                value = value.replace(dollarName, replacement);
            }
            variables.put(name, value);
            if (listener != null)
                listener.exportChange(name, value);
            return null;
        }
        return "export: usage: export [name[=value] ...]";
    }

    /**
     * line that contains a non escaped $
     *
     * @param key input
     * @return line with variables replaced with their value
     */
    public String getValue(String key) {
        if (key.indexOf(DOLLAR) == -1) {
            String value = getVariable(key);

            if (value == null)
                return null;

            if (value.indexOf(DOLLAR) == -1)
                return value;
            else
                return parseValue(value, 0);
        }
        return parseValue(key, 0);
    }

    private String getVariable(String key) {
        if (this.variables.containsKey(key))
            return this.variables.get(key);

        if (this.exportUsesSystemEnvironment)
            return System.getenv().get(key);

        return null;
    }

    private boolean containsKey(String key) {
        boolean result = false;
        result = this.variables.containsKey(key);

        if (!result && this.exportUsesSystemEnvironment)
            result = System.getenv().containsKey(key);

        return result;
    }

    public String getValueIgnoreCase(String name) {
        for (String key : variables.keySet()) {
            if (key.equalsIgnoreCase(name))
                return variables.get(key);
        }

        if (this.exportUsesSystemEnvironment) {
            for (String key : System.getenv().keySet()) {
                if (key.equalsIgnoreCase(name))
                    return System.getenv().get(key);
            }
        }

        return "";
    }

    private static final int MAX_PARSE_DEPTH = 10;

    private int lastExitCode;

    /**
     * Record the exit code of the most recently executed command,
     * used for {@code $?} expansion.
     */
    public void setLastExitCode(int lastExitCode) {
        this.lastExitCode = lastExitCode;
    }

    /**
     * Expand variables in a line, left to right (bash semantics):
     * {@code $NAME}/${NAME} substitute known values and empty for unknown,
     * {@code $?} is the last exit code, {@code $$} the JVM pid,
     * {@code $1}-{@code $9}/{@code $#} expand to empty, {@code \$} is a
     * literal dollar, single-quoted spans are left untouched, and any other
     * stray {@code $} passes through literally (never dropped).
     */
    private String parseValue(String value, int depth) {
        if (value == null)
            return null;

        if (depth > MAX_PARSE_DEPTH)
            return value;

        if (value.indexOf(DOLLAR) == -1) {
            return value;
        }

        StringBuilder out = new StringBuilder(value.length());
        boolean inSingleQuote = false;
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c == '\\' && !inSingleQuote && i + 1 < value.length()
                    && value.charAt(i + 1) == DOLLAR) {
                out.append(DOLLAR);
                i += 2;
                continue;
            }
            if (c == '\'') {
                inSingleQuote = !inSingleQuote;
                out.append(c);
                i++;
                continue;
            }
            if (c == DOLLAR && !inSingleQuote) {
                i = appendExpansion(value, i, out, depth);
                continue;
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    /**
     * Expand the variable reference starting at {@code value.charAt(start)}
     * (which is a dollar), appending to {@code out}.
     *
     * @return the index of the first unconsumed character
     */
    private int appendExpansion(String value, int start, StringBuilder out, int depth) {
        int i = start + 1;
        if (i >= value.length())
            return copyDollar(out, start, i);
        char next = value.charAt(i);
        if (next == '{') {
            int close = findClosingBrace(value, start);
            if (close < 0)
                return copyDollar(out, start, i);
            appendName(value.substring(i + 1, close), out, depth);
            return close + 1;
        }
        if (next == '?') {
            out.append(lastExitCode);
            return i + 1;
        }
        if (next == DOLLAR) {
            out.append(jvmPid());
            return i + 1;
        }
        if (isNameChar(next)) {
            int end = i;
            while (end < value.length() && isNameChar(value.charAt(end)))
                end++;
            appendName(value.substring(i, end), out, depth);
            return end;
        }
        return copyDollar(out, start, i);
    }

    private int copyDollar(StringBuilder out, int start, int next) {
        out.append(DOLLAR);
        return next;
    }

    private static boolean isNameChar(char c) {
        return c == '_' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9';
    }

    private void appendName(String name, StringBuilder out, int depth) {
        if (!containsKey(name))
            return;
        String resolved = getVariable(name);
        if (resolved != null && resolved.indexOf(DOLLAR) > -1)
            resolved = parseValue(resolved, depth + 1);
        if (resolved != null)
            out.append(resolved);
    }

    private static String jvmPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        int at = name.indexOf('@');
        return at > 0 ? name.substring(0, at) : name;
    }

    public String listAllVariables() {
        StringBuilder builder = new StringBuilder();
        for (String key : variables.keySet()) {
            builder.append(key).append('=').append(parseValue(variables.get(key), 0)).append(Config.getLineSeparator());
        }

        if (this.exportUsesSystemEnvironment) {
            for (String key : System.getenv().keySet()) {
                builder.append(key).append('=').append(parseValue(getVariable(key), 0)).append(Config.getLineSeparator());
            }
        }
        return builder.toString();
    }

    public void persistVariables() {
        boolean keepGoing = true;
        try {
            if (exportFile.isFile())
                keepGoing = exportFile.delete();

            if (keepGoing) {
                File parentFile = exportFile.getParentFile();
                if (parentFile != null) {
                    parentFile.mkdirs();
                }
                keepGoing = exportFile.createNewFile();
            }

            if (keepGoing) {
                try (FileWriter fw = new FileWriter(exportFile)) {
                    for (String key : variables.keySet()) {
                        fw.write(EXPORT + " " + key + "=" + variables.get(key) + Config.getLineSeparator());
                    }
                    fw.flush();
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to persist variables to file " + exportFile, e);
        }
    }

    public List<String> getAllNamesWithEquals() {
        List<String> names = new ArrayList<>(variables.size());
        for (String key : variables.keySet())
            names.add(key + "=");

        if (this.exportUsesSystemEnvironment) {
            for (String key : System.getenv().keySet())
                names.add(key + "=");
        }

        return names;
    }

    public List<String> getAllNames() {
        List<String> names = new ArrayList<>(variables.size());
        for (String key : variables.keySet())
            names.add(key);

        if (this.exportUsesSystemEnvironment) {
            for (String key : System.getenv().keySet())
                names.add(key);
        }

        return names;
    }

    public List<String> findAllMatchingKeys(String word) {
        int index = word.lastIndexOf(DOLLAR);
        if (index > -1)
            word = word.substring(index + 1, word.length());
        List<String> keys = new ArrayList<>();
        for (String key : variables.keySet()) {
            if (key.startsWith(word)) {
                if (index > -1)
                    keys.add("$" + key);
                else
                    keys.add(key);
            }
        }

        if (this.exportUsesSystemEnvironment) {
            for (String key : System.getenv().keySet()) {
                if (key.startsWith(word)) {
                    if (index > -1)
                        keys.add("$" + key);
                    else
                        keys.add(key);
                }
            }
        }

        return keys;
    }

}
