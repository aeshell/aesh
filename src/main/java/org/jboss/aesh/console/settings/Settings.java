/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.settings;

import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.helper.InterruptHook;
import org.jboss.aesh.edit.EditMode;
import org.jboss.aesh.edit.KeyOperationManager;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.terminal.Terminal;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * Object thats define all tunable settings used by Console
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface Settings {
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
    boolean isDisableCompletion();

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
     * Do aesh read ahead
     * @return
     */
    boolean isReadAhead();

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
}
