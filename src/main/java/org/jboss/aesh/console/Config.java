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
import org.jboss.aesh.io.FileResource;
import org.jboss.aesh.io.Resource;
import org.jboss.aesh.readline.editing.EditMode;
import org.jboss.aesh.util.LoggerUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

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

    protected static Settings readRuntimeProperties(Settings settings) {
       SettingsBuilder builder = new SettingsBuilder(settings);

            String editMode = System.getProperty("aesh.editmode");
            if(editMode != null && editMode.length() > 0) {
                if(editMode.equalsIgnoreCase("VI"))
                    builder.mode(EditMode.Mode.VI);
                else if(editMode.equalsIgnoreCase("EMACS"))
                    builder.mode(EditMode.Mode.EMACS);
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

            String execute = System.getProperty("aesh.execute");
            if(execute != null && execute.length() > 0)
                builder.setExecuteAtStart(execute);

            String executeFile = System.getProperty("aesh.executefile");
            if(executeFile != null && executeFile.length() > 0) {
               Resource resourceFile =  new FileResource(executeFile);
                if(resourceFile.isLeaf())
                    builder.setExecuteFileAtStart(resourceFile);
            }

        return builder.create();
    }
}
