/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.edit.mapper.KeyMapper;
import org.jboss.aesh.terminal.Terminal;
import org.jboss.aesh.util.LoggerUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jboss.aesh.console.settings.VariableSettings.BELL_STYLE;
import static org.jboss.aesh.console.settings.VariableSettings.DISABLE_COMPLETION;
import static org.jboss.aesh.console.settings.VariableSettings.EDITING_MODE;
import static org.jboss.aesh.console.settings.VariableSettings.HISTORY_SIZE;

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class Config {

    private static String lineSeparator = System.getProperty("line.separator");
    private static String pathSeparator = System.getProperty("file.separator");
    private static String tmpDir = System.getProperty("java.io.tmpdir");
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

    public static String getTmpDir() {
        return tmpDir;
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
            logger.info("Error while parsing: "+settings.getInputrc().getAbsolutePath()+" couldn't find file.");
            return;
        }

        Pattern variablePattern = Pattern.compile("^set\\s+(\\S+)\\s+(\\S+)$");
        Pattern commentPattern = Pattern.compile("^#.*");
        //Pattern keyNamePattern = Pattern.compile("^\\b:\\s+\\b");
        Pattern keyQuoteNamePattern = Pattern.compile("(^\\\"\\S+)(\\\":\\s+)(\\S+)");
        Pattern keyNamePattern = Pattern.compile("(^\\S+)(:\\s+)(\\S+)");
        Pattern keySeqPattern = Pattern.compile("^\"keyseq:\\s+\\b");
        Pattern startConstructs = Pattern.compile("^\\$if");
        Pattern endConstructs = Pattern.compile("^\\$endif");

        Pattern keyOperationPattern = Pattern.compile("(^\\\"\\\\M-\\[D:)(\\s+)(\\S+)");

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
                //TODO: currently the inputrc parser is posix only
                if(Config.isOSPOSIXCompatible()) {
                    Matcher keyQuoteMatcher = keyQuoteNamePattern.matcher(line);
                    if(keyQuoteMatcher.matches()) {
                        settings.getOperationManager().addOperation(
                                KeyMapper.mapQuoteKeys(keyQuoteMatcher.group(1),
                                        keyQuoteMatcher.group(3)));
                    }
                    Matcher keyMatcher = keyNamePattern.matcher(line);
                    if(keyMatcher.matches()) {
                        settings.getOperationManager().addOperation(KeyMapper.mapKeys(keyMatcher.group(1), keyMatcher.group(3)));
                    }
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
            String term = System.getProperty("aesh.terminal");
            if(term != null && term.length() > 0) {
                settings.setTerminal((Terminal) settings.getClass().getClassLoader().loadClass(term).newInstance());
            }
            String editMode = System.getProperty("aesh.editmode");
            if(editMode != null && editMode.length() > 0) {
                if(editMode.equalsIgnoreCase("VI"))
                    settings.setEditMode(Mode.VI);
                else if(editMode.equalsIgnoreCase("EMACS"))
                    settings.setEditMode(Mode.EMACS);
            }
            String readInputrc = System.getProperty("aesh.readinputrc");
            if(readInputrc != null && readInputrc.length() > 0)
                if(readInputrc.equalsIgnoreCase("true") ||
                        readInputrc.equalsIgnoreCase("false"))
                    settings.setReadInputrc(Boolean.parseBoolean(readInputrc));

            String inputrc = System.getProperty("aesh.inputrc");
            if(inputrc != null && inputrc.length() > 0)
                if(new File(inputrc).isFile())
                    settings.setInputrc(new File(inputrc));

            String historyFile = System.getProperty("aesh.historyfile");
            if(historyFile != null && historyFile.length() > 0)
                if(new File(historyFile).isFile())
                    settings.setHistoryFile(new File(historyFile));

            String historyPersistent = System.getProperty("aesh.historypersistent");
            if(historyPersistent != null && historyPersistent.length() > 0)
                if(historyPersistent.equalsIgnoreCase("true") ||
                        historyPersistent.equalsIgnoreCase("false"))
                    settings.setHistoryPersistent(Boolean.parseBoolean(historyPersistent));

            String historyDisabled = System.getProperty("aesh.historydisabled");
            if(historyDisabled != null && historyDisabled.length() > 0)
                if(historyDisabled.equalsIgnoreCase("true") ||
                        historyDisabled.equalsIgnoreCase("false"))
                    settings.setHistoryDisabled(Boolean.parseBoolean(historyDisabled));

            String historySize = System.getProperty("aesh.historysize");
            if(historySize != null && historySize.length() > 0)
                settings.setHistorySize(Integer.parseInt(historySize));

            String doLogging = System.getProperty("aesh.logging");
            if(doLogging != null && doLogging.length() > 0)
                if(doLogging.equalsIgnoreCase("true") ||
                        doLogging.equalsIgnoreCase("false"))
                    settings.setLogging(Boolean.parseBoolean(doLogging));

            String logFile = System.getProperty("aesh.logfile");
            if(logFile != null && logFile.length() > 0)
                settings.setLogFile(logFile);

            String disableCompletion = System.getProperty("aesh.disablecompletion");
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
