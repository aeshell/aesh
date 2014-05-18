/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.edit.mapper.KeyMapper;
import org.jboss.aesh.terminal.Terminal;
import org.jboss.aesh.util.LoggerUtil;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
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
    private static boolean posixCompatible = checkPosixCompability();
    private static boolean cygwin = false;

    private static final Logger LOGGER = LoggerUtil.getLogger(Config.class.getName());

    public static boolean isOSPOSIXCompatible() {
        return posixCompatible;
    }

    public static boolean isCygwin() {
        return cygwin;
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

    public static String getHomeDir() {
        return System.getProperty("user.home");
    }

    public static String getUserDir() {
        return System.getProperty("user.dir");
    }

    private static boolean checkPosixCompability() {
        if(System.getProperty("os.name").startsWith("Windows")) {
            //need to check if we're running under cygwin
            try {
                java.lang.Process process = Runtime.getRuntime().exec(new String[]{"uname"});
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                int c;
                InputStream in = process.getInputStream();
                while ((c = in.read()) != -1) {
                    bout.write(c);
                }
                process.waitFor();

                String output = new String(bout.toByteArray());
                if(output.toLowerCase().contains("cygwin")) {
                    cygwin = true;
                    return true;
                }
            }
            catch (IOException | InterruptedException e) {
                //silently ignore that we're not running cygwin
            }

            return false;
        }
        else if( System.getProperty("os.name").startsWith("OS/2"))
            return false;
        else
            return true;
    }

    /**
     * TODO: clean this shit up!
     *
     * Must be able to parse:
     * set variablename value
     * keyname: function-name or macro
     * "keyseq": function-name or macro
     *
     * Lines starting with # are comments
     * Lines starting with $ are conditional init constructs
     *
     */
    protected static Settings parseInputrc(Settings settings) throws IOException {
        if(!settings.getInputrc().isFile()) {
            if(settings.isLogging())
                LOGGER.info("Error while parsing: "+settings.getInputrc().getAbsolutePath()+" couldn't find file.");
            return settings;
        }
        SettingsBuilder builder = new SettingsBuilder(settings);

        Pattern variablePattern = Pattern.compile("^set\\s+(\\S+)\\s+(\\S+)$");
        Pattern commentPattern = Pattern.compile("^#.*");
        //Pattern keyNamePattern = Pattern.compile("^\\b:\\s+\\b");
        Pattern keyQuoteNamePattern = Pattern.compile("(^\"\\\\\\S+)(\":\\s+)(\\S+)");
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
                    parseVariables(variableMatcher.group(1), variableMatcher.group(2), builder);
                }
                //TODO: currently the inputrc parser is posix only
                if(Config.isOSPOSIXCompatible()) {
                    Matcher keyQuoteMatcher = keyQuoteNamePattern.matcher(line);
                    if(keyQuoteMatcher.matches()) {
                        builder.create().getOperationManager().addOperationIgnoreWorkingMode(
                                KeyMapper.mapQuoteKeys(keyQuoteMatcher.group(1),
                                        keyQuoteMatcher.group(3)));
                    }
                    else {
                        Matcher keyMatcher = keyNamePattern.matcher(line);
                        if(keyMatcher.matches()) {
                            builder.create().getOperationManager().addOperationIgnoreWorkingMode(KeyMapper.mapKeys(keyMatcher.group(1), keyMatcher.group(3)));
                        }
                    }
                }
            }

        }

        //finally close
        reader.close();

        return builder.create();
    }

    private static void parseVariables(String variable, String value, SettingsBuilder builder) {
        if (variable.equals(EDITING_MODE.getVariable())) {
            if(EDITING_MODE.getValues().contains(value)) {
                if(value.equals("vi"))
                    builder.mode(Mode.VI);
                else
                    builder.mode(Mode.EMACS);
            }
            // should log some error
            else if(builder.create().isLogging())
                LOGGER.warning("Value "+value+" not accepted for: "+variable+
                        ", only: "+EDITING_MODE.getValues());

        }
        else if(variable.equals(BELL_STYLE.getVariable())) {
            if(BELL_STYLE.getValues().contains(value))
                builder.bellStyle(value);
            else if(builder.create().isLogging())
                LOGGER.warning("Value "+value+" not accepted for: "+variable+
                        ", only: "+BELL_STYLE.getValues());
        }
        else if(variable.equals(HISTORY_SIZE.getVariable())) {
            try {
                builder.historySize(Integer.parseInt(value));
            }
            catch (NumberFormatException nfe) {
                if(builder.create().isLogging())
                    LOGGER.warning("Value "+value+" not accepted for: "
                            +variable+", it must be an integer.");
            }
        }
        else if(variable.equals(DISABLE_COMPLETION.getVariable())) {
            if(DISABLE_COMPLETION.getValues().contains(value)) {
                if(value.equals("on"))
                    builder.disableCompletion(true);
                else
                    builder.disableCompletion(false);
            }
            else if(builder.create().isLogging())
                LOGGER.warning("Value "+value+" not accepted for: "+variable+
                        ", only: "+DISABLE_COMPLETION.getValues());
        }
    }

    protected static Settings readRuntimeProperties(Settings settings) {
       SettingsBuilder builder = new SettingsBuilder(settings);
        try {
            String term = System.getProperty("aesh.terminal");
            if(term != null && term.length() > 0) {
                builder.terminal((Terminal) settings.getClass().getClassLoader().loadClass(term).newInstance());
            }
            String editMode = System.getProperty("aesh.editmode");
            if(editMode != null && editMode.length() > 0) {
                if(editMode.equalsIgnoreCase("VI"))
                    builder.mode(Mode.VI);
                else if(editMode.equalsIgnoreCase("EMACS"))
                    builder.mode(Mode.EMACS);
            }
            String readInputrc = System.getProperty("aesh.readinputrc");
            if(readInputrc != null && readInputrc.length() > 0)
                if(readInputrc.equalsIgnoreCase("true") ||
                        readInputrc.equalsIgnoreCase("false"))
                    builder.readInputrc(Boolean.parseBoolean(readInputrc));

            String inputrc = System.getProperty("aesh.inputrc");
            if(inputrc != null && inputrc.length() > 0)
                if(new File(inputrc).isFile())
                    builder.inputrc(new File(inputrc));

            String historyFile = System.getProperty("aesh.historyfile");
            if(historyFile != null && historyFile.length() > 0)
                if(new File(historyFile).isFile())
                    builder.historyFile(new File(historyFile));

            String historyPersistent = System.getProperty("aesh.historypersistent");
            if(historyPersistent != null && historyPersistent.length() > 0)
                if(historyPersistent.equalsIgnoreCase("true") ||
                        historyPersistent.equalsIgnoreCase("false"))
                    builder.persistHistory(Boolean.parseBoolean(historyPersistent));

            String historyDisabled = System.getProperty("aesh.historydisabled");
            if(historyDisabled != null && historyDisabled.length() > 0)
                if(historyDisabled.equalsIgnoreCase("true") ||
                        historyDisabled.equalsIgnoreCase("false"))
                    builder.disableHistory(Boolean.parseBoolean(historyDisabled));

            String historySize = System.getProperty("aesh.historysize");
            if(historySize != null && historySize.length() > 0)
                builder.historySize(Integer.parseInt(historySize));

            String doLogging = System.getProperty("aesh.logging");
            if(doLogging != null && doLogging.length() > 0)
                if(doLogging.equalsIgnoreCase("true") ||
                        doLogging.equalsIgnoreCase("false"))
                    builder.logging(Boolean.parseBoolean(doLogging));

            String logFile = System.getProperty("aesh.logfile");
            if(logFile != null && logFile.length() > 0)
                builder.logfile(logFile);

            String disableCompletion = System.getProperty("aesh.disablecompletion");
            if(disableCompletion != null && disableCompletion.length() > 0)
                if(disableCompletion.equalsIgnoreCase("true") ||
                        disableCompletion.equalsIgnoreCase("false"))
                    builder.disableCompletion(Boolean.parseBoolean(disableCompletion));

          }
        catch (ClassNotFoundException e) {
            if(settings.isLogging())
                LOGGER.log(Level.SEVERE, "Fail while finding class: ", e);
        } catch (InstantiationException e) {
            if(settings.isLogging())
                LOGGER.log(Level.SEVERE, "Fail while instantiating class: ", e);
        } catch (IllegalAccessException e) {
            if(settings.isLogging())
                LOGGER.log(Level.SEVERE, "Fail while accessing class: ", e);
        }

        return builder.create();
    }
}
