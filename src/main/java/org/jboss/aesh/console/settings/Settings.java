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
package org.jboss.aesh.console.settings;

import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.helper.InterruptHook;
import org.jboss.aesh.edit.EditMode;
import org.jboss.aesh.edit.KeyOperationManager;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.io.Resource;
import org.jboss.aesh.terminal.Terminal;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * Object thats define all tunable settings used by Console
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface Settings extends Cloneable {
    /**
     * Get the name of the cli
     * Default is "aesh"
     */
    String getName();

    /**
     * Get the current Mode.
     * Default mode is Emacs
     *
     */
    Mode getMode();

    /**
     * Get current edit mode
     */
    EditMode getEditMode();

    /**
     * Reset edit mode
     */
    void resetEditMode();

    /**
     * Get KeyOperationManager
     */
    KeyOperationManager getOperationManager();

    /**
     * Get file where history is stored
     */
    File getHistoryFile();

    /**
     * Get history file access permission, this is used when write to history file.
     *
     * <code>null</code> means default file permission revealed by system.
     */
    FileAccessPermission getHistoryFilePermission();

    /**
     * Get history size
     */
    int getHistorySize();

    /**
     * Get bell style
     * NOTE: Not used yet
     */
    String getBellStyle();

    /**
     * If the current console is an ANSI console
     */
    boolean isAnsiConsole();

    /**
     * Get input stream
     */
    InputStream getInputStream();

    /**
     * Get standard output stream
     */
    PrintStream getStdOut();

    /**
     * Get standard error stream
     */
    PrintStream getStdErr();

    /**
     * Get current terminal
     */
    Terminal getTerminal();

    /**
     * Get inputrc file location
     */
    File getInputrc();

    /**
     * Are we logging
     */
    boolean isLogging();

    /**
     * Is completion disabled
     */
    boolean isCompletionDisabled();

    /**
     * Get location of log file
     */
    String getLogFile();

    /**
     * Do aesh read inputrc during init
     */
    boolean doReadInputrc();

    /**
     * Is history disabled
     */
    boolean isHistoryDisabled();

    /**
     * Is history persisted during shutdown
     */
    boolean isHistoryPersistent();

    /**
     * Location of alias file
     */
    File getAliasFile();

    /**
     * Is alias enabled
     */
    boolean isAliasEnabled();

    /**
     * Is alias persisted
     */
    boolean doPersistAlias();

    /**
     * Get quit handler
     */
    QuitHandler getQuitHandler();

    /**
     * Is an interrupt hook defined
     */
    boolean hasInterruptHook();

    /**
     * Get interrupt hook
     */
    InterruptHook getInterruptHook();

    /**
     * Is operator parser enabled
     */
    boolean isOperatorParserEnabled();

    /**
     * Switch from Emacs to Vi mode (or back)
     */
    void switchMode();

    /**
     * Is the man command enabled (currently only for AeshConsole)
     */
    boolean isManEnabled();

    /**
     * Get aesh context
     */
    AeshContext getAeshContext();

    /**
     * Get the export file
     * Defaults to ~/.aesh_export
     */
    File getExportFile();

    /**
     * Are export enabled?
     */
    boolean isExportEnabled();

    /**
     * Should aesh persist export variables at shutdown
     */
    void setPersistExport(boolean persist);

    /**
     * Is aesh persisting variables at shutdown
     */
    boolean doPersistExport();

    /**
     * Aesh load environment system.
     */
    void setExportUsesSystemEnvironment(boolean isLoad);

    /**
     * Is load environment system?
     */
    boolean doExportUsesSystemEnvironment();

    /**
     * Specify
     * @param resource
     */
    void setResource(Resource resource);

    /**
     * @param execute specify a string that will be pushed to the input stream at start
     */
    void setExecuteAtStart(String execute);

    /**
     * @return execute at start string
     */
    String getExecuteAtStart();

    /**
     * @param executeFileAtStart file that will be read, parsed and executed at start
     */
    void setExecuteFileAtStart(Resource executeFileAtStart);

    /**
     * @return execute file at start
     */
    Resource getExecuteFileAtStart();

    Resource getResource();

    Object clone();

}
