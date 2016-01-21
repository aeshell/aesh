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
package org.jboss.aesh.console;

import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.edit.mapper.KeyMapper;
import org.jboss.aesh.io.FileResource;
import org.jboss.aesh.io.Resource;
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

    private static final String lineSeparator = System.getProperty("line.separator");
    private static final String pathSeparator = System.getProperty("file.separator");
    private static final String tmpDir = System.getProperty("java.io.tmpdir");
    private static final boolean posixCompatible = checkPosixCompability();
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

    public static String getOS() {
        return System.getProperty("os.name");
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
        else
            return !System.getProperty("os.name").startsWith("OS/2");
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
        Pattern keyQuoteNamePattern = Pattern.compile("(^\"\\\\\\S+)(\":\\s+)(\\S+)");
        Pattern keyNamePattern = Pattern.compile("(^\\S+)(:\\s+)(\\S+)");
        Pattern startConstructs = Pattern.compile("^\\$if");
        Pattern endConstructs = Pattern.compile("^\\$endif");

        try {
            BufferedReader reader = new BufferedReader( new FileReader(settings.getInputrc()));

            String line;
            boolean constructMode = false;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() < 1)
                    continue;
                //first check if its a comment
                if (commentPattern.matcher(line).matches())
                    continue;

                if (startConstructs.matcher(line).matches()) {
                    constructMode = true;
                    continue;
                } else if (endConstructs.matcher(line).matches()) {
                    constructMode = false;
                    continue;
                }

                if (!constructMode) {
                    Matcher variableMatcher = variablePattern.matcher(line);
                    if (variableMatcher.matches()) {
                        parseVariables(variableMatcher.group(1), variableMatcher.group(2), builder);
                    }
                    //TODO: currently the inputrc parser is posix only
                    if (Config.isOSPOSIXCompatible()) {
                        Matcher keyQuoteMatcher = keyQuoteNamePattern.matcher(line);
                        if (keyQuoteMatcher.matches()) {
                            builder.create().getOperationManager().addOperationIgnoreWorkingMode(
                                    KeyMapper.mapQuoteKeys(keyQuoteMatcher.group(1),
                                            keyQuoteMatcher.group(3)));
                        } else {
                            Matcher keyMatcher = keyNamePattern.matcher(line);
                            if (keyMatcher.matches()) {
                                builder.create().getOperationManager().addOperationIgnoreWorkingMode(KeyMapper.mapKeys(keyMatcher.group(1), keyMatcher.group(3)));
                            }
                        }
                    }
                }
            }
        }
        catch(IOException e) {
           LOGGER.log(Level.WARNING, "Failed to read .inputrc: ", e);
        }
        //KeyMapper throws IllegalArgumentException if it fails parsing the file
        catch(IllegalArgumentException iae) {
            LOGGER.log(Level.WARNING, "Exception during reading of inputrc: ", iae);
        }
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

            String ansi = System.getProperty("aesh.ansi");
            if(ansi != null && ansi.length() > 0)
                if(ansi.equalsIgnoreCase("true") || ansi.equalsIgnoreCase("false"))
                    builder.ansi(Boolean.parseBoolean(ansi));

            String execute = System.getProperty("aesh.execute");
            if(execute != null && execute.length() > 0)
                builder.setExecuteAtStart(execute);

            String executeFile = System.getProperty("aesh.executefile");
            if(executeFile != null && executeFile.length() > 0) {
               Resource resourceFile =  new FileResource(executeFile);
                if(resourceFile.isLeaf())
                    builder.setExecuteFileAtStart(resourceFile);
            }
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
