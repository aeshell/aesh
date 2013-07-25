/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.settings;

import org.jboss.aesh.console.helper.InterruptHook;
import org.jboss.aesh.edit.EditMode;
import org.jboss.aesh.edit.KeyOperationManager;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.terminal.Terminal;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface Settings {
    void setName(String name);

    String getName();

    Mode getMode();

    EditMode getEditMode();

    void resetEditMode();

    KeyOperationManager getOperationManager();

    File getHistoryFile();

    int getHistorySize();

    String getBellStyle();

    boolean isAnsiConsole();

    InputStream getInputStream();

    OutputStream getStdOut();

    OutputStream getStdErr();

    Terminal getTerminal();

    File getInputrc();


    boolean isLogging();


    boolean isDisableCompletion();


    String getLogFile();


    boolean doReadInputrc();

    boolean isHistoryDisabled();

    boolean isHistoryPersistent();

    boolean isReadAhead();

    File getAliasFile();

    boolean isAliasEnabled();

    boolean doPersistAlias();

    QuitHandler getQuitHandler();

    boolean hasInterruptHook();

    InterruptHook getInterruptHook();

    void enableOperatorParser(boolean enable);

    boolean isOperatorParserEnabled();

    void switchMode();
}
