/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.export;

import org.jboss.aesh.console.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ExportManager {

    private static final char DOLLAR = '$';
    private Map<String,String> variables;
    private Pattern exportPattern = Pattern.compile("^(export)\\s+(\\w+)\\s*=\\s*(\\S+).*$");
    private Pattern variableDollarFirstPattern = Pattern.compile("\\$(\\w+|\\{(\\w+)\\})(.*)");
    private Pattern variablePattern = Pattern.compile("(.*)\\$(\\w+|\\{(\\w+)\\})(.*)");

    private static final String EXPORT = "export";

    private File exportFile;

    public ExportManager(File exportFile) throws IOException {
        this.exportFile = exportFile;
        variables = new HashMap<>();
        if(exportFile.isFile())
            readVariablesFromFile();
    }

    private void readVariablesFromFile() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(exportFile));
        String line;
        while((line = br.readLine()) != null) {
            if(line.startsWith(EXPORT))
                addVariable(line);
        }
    }

    public void addVariable(String line) {
        Matcher variableMatcher = exportPattern.matcher(line);
        if(variableMatcher.matches()) {
            String name = variableMatcher.group(2);
            String value = variableMatcher.group(3);
            //need to make sure we dont get a line like: export FOO=$FOO
            if(value.contains(String.valueOf(DOLLAR+name))) {
                value = value.replace(String.valueOf(DOLLAR+name), variables.get(name));
            }
            variables.put(name, value);
        }
    }

    /**
     * line that contains a non escaped $
     * @param key input
     * @return line with variables replaced with their value
     */
    public String getValue(String key) {
        if(key.indexOf(DOLLAR) == -1) {
            String value = variables.get(key);
            if(value.indexOf(DOLLAR) == -1)
                return value;
            else
                return parseValue(value);
        }
        else {
            return parseValue(key);
        }
    }

    private String parseValue(String value) {
        if(value.indexOf(DOLLAR) == -1) {
            return value;
        }
        else {
            if(value.indexOf(DOLLAR) == 0) {
                Matcher matcher = variableDollarFirstPattern.matcher(value);
                if(matcher.matches()) {
                    String group1 = matcher.group(1);
                    //if the variable is written as {name} it will be in group2
                    if(matcher.group(2) != null)
                        group1 = matcher.group(2);
                    String group2 = matcher.group(3);

                    if(group1 != null && variables.containsKey(group1)) {
                        if(group2 != null && group2.indexOf(DOLLAR) > -1) {
                            if(variables.get(group1).indexOf(DOLLAR) == -1)
                                return variables.get(group1) + parseValue(group2);
                            else
                                return parseValue(variables.get(group1)) + parseValue(group2);
                        }
                        else {
                            if(variables.get(group1).indexOf(DOLLAR) == -1)
                                return variables.get(group1) + group2;
                            else
                                return parseValue(variables.get(group1)) + group2;
                        }
                    }
                    else
                        return group2;
                }
                return "";
            }
            else {
                 Matcher matcher = variablePattern.matcher(value);
                if(matcher.matches()) {
                    String group1 = matcher.group(1);
                    String group2 = matcher.group(2);
                    //if the variable is written as {name} it will be in group3
                    if(matcher.group(3) != null)
                        group2 = matcher.group(3);
                    String group3 = matcher.group(4);

                    if(group2 != null && variables.containsKey(group2)) {
                        if(group3 != null && group3.indexOf(DOLLAR) > -1) {
                            if(variables.get(group2).indexOf(DOLLAR) == -1)
                                return group1 + variables.get(group2) + parseValue(group3);
                            else
                                return group1 + parseValue(variables.get(group2)) + parseValue(group3);
                        }
                        else {
                            if(variables.get(group2).indexOf(DOLLAR) == -1)
                                return group1 + variables.get(group2) + group3;
                            else
                                return group1 + parseValue(variables.get(group2)) + group3;
                        }
                    }
                    else
                        return group1 + group3;
                }
                return "";
            }
        }
    }

    public String listAllVariables() {
        StringBuilder builder = new StringBuilder();
        for(String key : variables.keySet()) {
            builder.append(key).append('=').append(parseValue(variables.get(key))).append(Config.getLineSeparator());
        }
        return builder.toString();
    }

    public void persistVariables() throws IOException {
        if(exportFile.isFile())
            exportFile.delete();

        FileWriter fw = new FileWriter(exportFile);
        for(String key : variables.keySet())
            fw.write(EXPORT+" "+key+"="+variables.get(key)+Config.getLineSeparator());

        fw.flush();
        fw.close();
    }

    public List<String> getAllNames() {
        List<String> names = new ArrayList<>(variables.size());
        for(String key : variables.keySet())
            names.add(key+"=");
        return names;
    }
}
