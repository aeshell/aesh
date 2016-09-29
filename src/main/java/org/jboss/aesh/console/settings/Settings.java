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

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;

import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.command.activator.CommandActivatorProvider;
import org.jboss.aesh.console.command.activator.OptionActivatorProvider;
import org.jboss.aesh.console.command.completer.CompleterInvocationProvider;
import org.jboss.aesh.console.command.converter.ConverterInvocationProvider;
import org.jboss.aesh.console.command.invocation.CommandInvocationServices;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.command.validator.ValidatorInvocationProvider;
import org.jboss.aesh.console.helper.InterruptHook;
import org.jboss.aesh.console.helper.ManProvider;
import org.jboss.aesh.io.Resource;
import org.jboss.aesh.readline.editing.EditMode;

/**
 * Object thats define all tunable settings used by Console
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface Settings extends Cloneable {

    /**
     * Get the current Mode.
     * Default mode is Emacs
     *
     */
    EditMode.Mode mode();

    /**
     * Get current edit mode
     */
    EditMode editMode();

    /**
     * Get file where history is stored
     */
    File historyFile();

    /**
     * Get history file access permission, this is used when write to history file.
     *
     * <code>null</code> means default file permission revealed by system.
     */
    FileAccessPermission historyFilePermission();

    /**
     * Get history size
     */
    int historySize();

    /**
     * Get bell style
     * NOTE: Not used yet
     */
    String bellStyle();

    /**
     * Get input stream
     */
    InputStream stdIn();

    /**
     * Get standard output stream
     */
    PrintStream stdOut();

    /**
     * Get standard error stream
     */
    PrintStream stdErr();

    /**
     * Get inputrc file location
     */
    File inputrc();

    /**
     * Are we logging
     */
    boolean logging();

    /**
     * Is completion disabled
     */
    boolean completionDisabled();

    /**
     * Get location of log file
     */
    String logFile();

    /**
     * Do aesh read inputrc during init
     */
    boolean readInputrc();

    /**
     * Is history disabled
     */
    boolean historyDisabled();

    /**
     * Is history persisted during shutdown
     */
    boolean historyPersistent();

    /**
     * Location of alias file
     */
    File aliasFile();

    /**
     * Is alias enabled
     */
    boolean aliasEnabled();

    /**
     * Is alias persisted
     */
    boolean persistAlias();

    /**
     * Get quit handler
     */
    QuitHandler quitHandler();

    /**
     * Get interrupt hook
     */
    InterruptHook interruptHook();

    /**
     * Is operator parser enabled
     */
    boolean operatorParserEnabled();

    /**
     * Is the man command enabled (currently only for AeshConsole)
     */
    boolean manEnabled();

    /**
     * Get aesh context
     */
    AeshContext aeshContext();

    /**
     * Get the export file
     * Defaults to ~/.aesh_export
     */
    File exportFile();

    /**
     * Are export enabled?
     */
    boolean exportEnabled();

    /**
     * Should aesh persist export variables at shutdown
     */
    void setPersistExport(boolean persist);

    /**
     * Is aesh persisting variables at shutdown
     */
    boolean persistExport();

    /**
     * Aesh load environment system.
     */
    void setExportUsesSystemEnvironment(boolean isLoad);

    /**
     * Is load environment system?
     */
    boolean exportUsesSystemEnvironment();

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
    String executeAtStart();

    /**
     * @param executeFileAtStart file that will be read, parsed and executed at start
     */
    void setExecuteFileAtStart(Resource executeFileAtStart);

    /**
     * @return execute file at start
     */
    Resource executeFileAtStart();

    /**
     * @return get resource
     */
    Resource resource();

    /**
     * @return get command registry
     */
    CommandRegistry commandRegistry();

    /**
     *
     * @return get CommandInvocationServices
     */
    CommandInvocationServices commandInvocationServices();

    /**
     * @return get CommandNotFoundHandler
     */
    CommandNotFoundHandler commandNotFoundHandler();

    /**
     * @return CompleterInvocationProvider
     */
    CompleterInvocationProvider completerInvocationProvider();

    ConverterInvocationProvider converterInvocationProvider();

    ValidatorInvocationProvider validatorInvocationProvider();

    OptionActivatorProvider optionActivatorProvider();

    ManProvider manProvider();

    CommandActivatorProvider commandActivatorProvider();

    Object clone();

}
