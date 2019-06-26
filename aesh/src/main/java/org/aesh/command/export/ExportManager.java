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
package org.aesh.command.export;

import org.aesh.readline.util.LoggerUtil;
import org.aesh.terminal.utils.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ExportManager {

    private static final char DOLLAR = '$';
    private final Map<String, String> variables;
    private final Pattern exportPattern = Pattern.compile("^(export)\\s+(\\w+)\\s*=\\s*(\\S+).*$");
    private final Pattern variableDollarFirstPattern = Pattern.compile("\\$(\\w+|\\{(\\w+)\\})(.*)");
    private final Pattern variablePattern = Pattern.compile("(.*)\\$(\\w+|\\{(\\w+)\\})(.*)");

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
        }
        catch (IOException e) {
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
            if (value.contains(String.valueOf(DOLLAR + name))) {
                value = value.replace(String.valueOf(DOLLAR + name), variables.get(name));
            }
            variables.put(name, value);
            if(listener != null)
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
                return parseValue(value);
        }
        return parseValue(key);
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

    private String parseValue(String value) {
        if (value == null)
            return null;

        if (value.indexOf(DOLLAR) == -1) {
            return value;
        }

        if (value.indexOf(DOLLAR) == 0) {
            Matcher matcher = variableDollarFirstPattern.matcher(value);
            if (matcher.matches()) {
                String group1 = matcher.group(1);
                String group2 = matcher.group(3);

                if (matcher.group(2) != null)
                    group1 = matcher.group(2);

                if (group1 != null && containsKey(group1)) {
                    if (group2 != null && group2.indexOf(DOLLAR) > -1) {
                        if (getVariable(group1).indexOf(DOLLAR) == -1)
                            return getVariable(group1) + parseValue(group2);
                        else
                            return parseValue(getVariable(group1)) + parseValue(group2);

                    }

                    if (getVariable(group1).indexOf(DOLLAR) == -1)
                        return getVariable(group1) + group2;
                    else
                        return parseValue(getVariable(group1)) + group2;
                }
                return group2;
            }
            return null;
        }

        Matcher matcher = variablePattern.matcher(value);
        if (matcher.matches()) {
            String group1 = matcher.group(1);
            String group2 = matcher.group(2);
            String group3 = matcher.group(4);

            if (matcher.group(3) != null)
                group2 = matcher.group(3);

            if (group2 != null && containsKey(group2)) {
                if (group3 != null && group3.indexOf(DOLLAR) > -1) {
                    if (getVariable(group2).indexOf(DOLLAR) == -1)
                        return parseValue(group1) + getVariable(group2) + parseValue(group3);
                    else
                        return parseValue(group1) + parseValue(getVariable(group2)) + parseValue(group3);
                }

                if (getVariable(group2).indexOf(DOLLAR) == -1)
                    return parseValue(group1) + getVariable(group2) + group3;

                return parseValue(group1) + parseValue(getVariable(group2)) + group3;
            }
            return group1 + group3;
        }
        return null;
    }

    public String listAllVariables() {
        StringBuilder builder = new StringBuilder();
        for (String key : variables.keySet()) {
            builder.append(key).append('=').append(parseValue(variables.get(key))).append(Config.getLineSeparator());
        }

        if (this.exportUsesSystemEnvironment) {
            for (String key : System.getenv().keySet()) {
                builder.append(key).append('=').append(parseValue(variables.get(key))).append(Config.getLineSeparator());
            }
        }
        return builder.toString();
    }

    public void persistVariables() {
        boolean keepGoing = true;
        try {
            if (exportFile.isFile())
                keepGoing = exportFile.delete();

            if(keepGoing) {
                File parentFile = exportFile.getParentFile();
                if (parentFile != null) {
                    parentFile.mkdirs();
                }
                keepGoing = exportFile.createNewFile();
            }

            if(keepGoing) {
                FileWriter fw = new FileWriter(exportFile);
                for(String key : variables.keySet()) {
                    fw.write(EXPORT + " " + key + "=" + variables.get(key) + Config.getLineSeparator());
                }
                fw.flush();
                fw.close();
            }
        }
        catch (IOException e) {
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
