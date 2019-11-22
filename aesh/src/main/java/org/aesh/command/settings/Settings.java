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
package org.aesh.command.settings;

import org.aesh.command.CommandNotFoundHandler;
import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.CommandActivatorProvider;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.activator.OptionActivatorProvider;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.CompleterInvocationProvider;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.converter.ConverterInvocationProvider;
import org.aesh.command.export.ExportChangeListener;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationProvider;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.command.validator.ValidatorInvocationProvider;
import org.aesh.io.Resource;
import org.aesh.readline.AeshContext;
import org.aesh.readline.alias.AliasManager;
import org.aesh.readline.editing.EditMode;
import org.aesh.terminal.Connection;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.function.Consumer;

/**
 * Object thats define all tunable settings used by Console
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface Settings<CI extends CommandInvocation,
        CI3 extends ConverterInvocation, CI2 extends CompleterInvocation,
        VI extends ValidatorInvocation, OA extends OptionActivator,
        CA extends CommandActivator> extends Cloneable {

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
     * If a custom AliasManager is needed, this will be used
     * Note: Only set this if you know what you're doing.
     *
     * @return custom AliasManager
     */
    AliasManager aliasManager();

    /**
     * Get quit handler
     */
    QuitHandler quitHandler();

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
    CommandRegistry<CI> commandRegistry();

    /**
     *
     * @return get CommandInvocationServices
     */
    CommandInvocationProvider<CI> commandInvocationProvider();

    /**
     * @return get CommandNotFoundHandler
     */
    CommandNotFoundHandler commandNotFoundHandler();

    /**
     * @return CompleterInvocationProvider
     */
    CompleterInvocationProvider<CI2> completerInvocationProvider();

    ConverterInvocationProvider<CI3> converterInvocationProvider();

    ValidatorInvocationProvider<VI> validatorInvocationProvider();

    OptionActivatorProvider<OA> optionActivatorProvider();

    ManProvider manProvider();

    CommandActivatorProvider<CA> commandActivatorProvider();

    Connection connection();

    Object clone();

    InvocationProviders<CA, CI3, CI2, VI, OA> invocationProviders();

    ExportChangeListener exportListener();

    void echoCtrl(boolean echo);

    boolean isEchoCtrl();

    void setInterruptHandler(Consumer<Void> handler);

    Consumer<Void> getInterruptHandler();

    void setConnectionClosedHandler(Consumer<Void> handler);

    Consumer<Void> connectionClosedHandler();

    void redrawPromptOnInterrupt(boolean redraw);

    boolean isRedrawPromptOnInterrupt();

    void setScanForCommandPackages(String... packages);

    String[] getScanForCommandPackages();

    void setEnableSearchInPaging(boolean enable);

    boolean enableSearchInPaging();
}
