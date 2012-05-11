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
import org.jboss.jreadline.terminal.Terminal;
import org.jboss.jreadline.util.LoggerUtil;

import java.io.*;
import java.util.logging.Logger;
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
        Logger logger = LoggerUtil.getLogger("Config");
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
        Logger logger = LoggerUtil.getLogger("Config");
        if (variable.equals(EDITING_MODE.getVariable())) {
            if(EDITING_MODE.getValues().contains(value)) {
                if(value.equals("vi"))
                    settings.setEditMode(Mode.VI);
                else
                    settings.setEditMode(Mode.EMACS);
            }
            // should log some error
            else
                logger.warning("Value "+value+" not accepted for: "+variable+
                        ", only: "+EDITING_MODE.getValues());

        }
        else if(variable.equals(BELL_STYLE.getVariable())) {
            if(BELL_STYLE.getValues().contains(value))
                settings.setBellStyle(value);
            else
                logger.warning("Value "+value+" not accepted for: "+variable+
                        ", only: "+BELL_STYLE.getValues());
        }
        else if(variable.equals(HISTORY_SIZE.getVariable())) {
            try {
                settings.setHistorySize(Integer.parseInt(value));
            }
            catch (NumberFormatException nfe) {
                logger.warning("Value "+value+" not accepted for: "+variable+
                        ", it must be an integer.");
            }
        }
        else if(variable.equals(DISABLE_COMPLETION.getVariable())) {
            if(DISABLE_COMPLETION.getValues().contains(value)) {
                if(value.equals("on"))
                    settings.setDisableCompletion(true);
                else
                    settings.setDisableCompletion(false);
            }
            else
                logger.warning("Value "+value+" not accepted for: "+variable+
                        ", only: "+DISABLE_COMPLETION.getValues());
        }
    }
    
    protected static void readRuntimeProperties(Settings settings) {
        try {
            String term = System.getProperty("jreadline.terminal");
            if(term != null && term.length() > 0) {
                settings.setTerminal((Terminal) settings.getClass().getClassLoader().loadClass(term).newInstance());
            }
            String editMode = System.getProperty("jreadline.editmode");
            if(editMode != null && editMode.length() > 0) {
                if(editMode.equalsIgnoreCase("VI"))
                    settings.setEditMode(Mode.VI);
                else if(editMode.equalsIgnoreCase("EMACS"))
                    settings.setEditMode(Mode.EMACS);
            }
            String readInputrc = System.getProperty("jreadline.readinputrc");
            if(readInputrc != null && readInputrc.length() > 0)
                if(readInputrc.equalsIgnoreCase("true") ||
                        readInputrc.equalsIgnoreCase("false"))
                    settings.setReadInputrc(Boolean.parseBoolean(readInputrc));

            String inputrc = System.getProperty("jreadline.inputrc");
            if(inputrc != null && inputrc.length() > 0)
                if(new File(inputrc).isFile())
                    settings.setInputrc(new File(inputrc));
            
            String historyFile = System.getProperty("jreadline.historyfile");
            if(historyFile != null && historyFile.length() > 0)
                if(new File(historyFile).isFile())
                    settings.setInputrc(new File(historyFile));
            
            String historyPersistent = System.getProperty("jreadline.historypersistent");
            if(historyPersistent != null && historyPersistent.length() > 0)
                if(historyPersistent.equalsIgnoreCase("true") || 
                        historyPersistent.equalsIgnoreCase("false"))
                    settings.setHistoryPersistent(Boolean.parseBoolean(historyPersistent));
            
            String historyDisabled = System.getProperty("jreadline.historydisabled");
            if(historyDisabled != null && historyDisabled.length() > 0)
                if(historyDisabled.equalsIgnoreCase("true") ||
                        historyDisabled.equalsIgnoreCase("false"))
                    settings.setHistoryDisabled(Boolean.parseBoolean(historyDisabled));
           
            String historySize = System.getProperty("jreadline.historysize");
            if(historySize != null && historySize.length() > 0)
                settings.setHistorySize(Integer.parseInt(historySize));
            
            String doLogging = System.getProperty("jreadline.logging");
            if(doLogging != null && doLogging.length() > 0)
                if(doLogging.equalsIgnoreCase("true") ||
                        doLogging.equalsIgnoreCase("false"))
                    settings.setLogging(Boolean.parseBoolean(doLogging));
            
            String logFile = System.getProperty("jreadline.logfile");
            if(logFile != null && logFile.length() > 0)
                settings.setLogFile(logFile);

            String disableCompletion = System.getProperty("jreadline.disablecompletion");
            if(disableCompletion != null && disableCompletion.length() > 0)
                if(disableCompletion.equalsIgnoreCase("true") ||
                        disableCompletion.equalsIgnoreCase("false"))
                    settings.setDisableCompletion(Boolean.parseBoolean(disableCompletion));

          }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
