/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.jreadline.console;

import org.jboss.jreadline.console.settings.Settings;
import org.jboss.jreadline.edit.Mode;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jboss.jreadline.console.settings.VariableSettings.*;

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class Config {

    private static String lineSeparator = System.getProperty("line.separator");
    private static String pathSeparator = System.getProperty("file.separator");
    private static boolean posixCompatible =
            !(System.getProperty("os.name").startsWith("Windows") ||
                    System.getProperty("os.name").startsWith("OS/2"));

    public static boolean isOSPOSIXCompatible() {
        return posixCompatible;
    }

    public static String getLineSeparator() {
        return lineSeparator;
    }

    public static String getPathSeparator() {
        return pathSeparator;
    }

    /**
     * Must be able to parse:
     * set variablename value
     * keyname: function-name or macro
     * "keyseq": function-name or macro
     *
     * Lines starting with # are comments
     * Lines starting with $ are conditional init constructs
     *
     */
    protected static void parseInputrc(Settings settings) throws IOException {
        if(!settings.getInputrc().isFile()) {
            System.out.println("not a file");
            return;
        }

        Pattern variablePattern = Pattern.compile("^set\\s+(\\S+)\\s+(\\S+)$");
        Pattern commentPattern = Pattern.compile("^#.*");
        Pattern keyNamePattern = Pattern.compile("^\\b:\\s+\\b");
        Pattern keySeqPattern = Pattern.compile("^\"keyseq:\\s+\\b");
        Pattern startConstructs = Pattern.compile("^\\$if");
        Pattern endConstructs = Pattern.compile("^\\$endif");


        BufferedReader reader =
                new BufferedReader( new FileReader(settings.getInputrc()));

        String line;
        boolean constructMode = false;
        while( (line = reader.readLine()) != null) {
            if(line.trim().length() < 1)
                continue;
            //first check if its a comment
            if(commentPattern.matcher(line).matches())
                continue;

            if(startConstructs.matcher(line).matches()) {
                constructMode = true;
                continue;
            }
            else if(endConstructs.matcher(line).matches()) {
                constructMode = false;
                continue;
            }

            if(constructMode) {

            }
            //everything other than if/else
            else {
                // variable settings
                Matcher variableMatcher = variablePattern.matcher(line);
                if(variableMatcher.matches()) {
                    parseVariables(variableMatcher.group(1), variableMatcher.group(2), settings);
                }
            }

        }

    }

    private static void parseVariables(String variable, String value, Settings settings) {
        if (variable.equals(EDITING_MODE.getVariable())) {
            if(EDITING_MODE.getValues().contains(value)) {
                if(value.equals("vi"))
                    settings.setEditMode(Mode.VI);
                else
                    settings.setEditMode(Mode.EMACS);
            }
            // should log some error
            else
                System.out.println("Value "+value+" not accepted for: "+variable+
                        ", only: "+EDITING_MODE.getValues());

        }
        else if(variable.equals(BELL_STYLE.getVariable())) {
            if(BELL_STYLE.getValues().contains(value))
                settings.setBellStyle(value);
            else
                System.out.println("Value "+value+" not accepted for: "+variable+
                        ", only: "+BELL_STYLE.getValues());
        }
        else if(variable.equals(HISTORY_SIZE.getVariable())) {
            try {
                settings.setHistorySize(Integer.parseInt(value));
            }
            catch (NumberFormatException nfe) {
                //log exception here
            }
        }
        else if(variable.equals(DISABLE_COMPLETION.getVariable())) {
            if(DISABLE_COMPLETION.getValues().contains(value))
                settings.setDisableCompletion(Boolean.parseBoolean(value));
        }
    }
}
