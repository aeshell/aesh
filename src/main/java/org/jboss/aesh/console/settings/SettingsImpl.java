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
import org.jboss.aesh.console.InvocationProviders;
import org.jboss.aesh.console.command.activator.CommandActivatorProvider;
import org.jboss.aesh.console.command.activator.OptionActivatorProvider;
import org.jboss.aesh.console.command.completer.CompleterInvocationProvider;
import org.jboss.aesh.console.command.converter.ConverterInvocationProvider;
import org.jboss.aesh.console.command.invocation.CommandInvocationServices;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.command.validator.ValidatorInvocationProvider;
import org.jboss.aesh.console.helper.InterruptHook;
import org.jboss.aesh.console.helper.ManProvider;
import org.jboss.aesh.io.FileResource;
import org.jboss.aesh.io.Resource;
import org.jboss.aesh.readline.editing.EditMode;
import org.jboss.aesh.readline.editing.EditModeBuilder;
import org.jboss.aesh.tty.Connection;
import org.jboss.aesh.util.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * Settings object that is parsed when Console is initialized.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class SettingsImpl implements Settings {

    private EditMode.Mode editMode = EditMode.Mode.EMACS;
    private File historyFile;
    private FileAccessPermission historyFilePermission;
    private int historySize = 500;
    private boolean historyDisabled = false;
    private boolean historyPersistent = true;
    private String bellStyle;
    private InputStream inputStream;
    private PrintStream stdOut;
    private PrintStream stdErr;
    private boolean readInputrc = true;
    private File inputrc;
    private boolean isLogging = false;
    private String logFile;
    private boolean disableCompletion = false;
    private QuitHandler quitHandler;
    private File aliasFile;
    private boolean aliasEnabled = true;
    private boolean persistAlias = true;
    private InterruptHook interruptHook = null;
    private boolean enableOperatorParser = true;
    private boolean manEnabled = true;
    private AeshContext aeshContext;
    private boolean exportEnabled = true;
    private File exportFile;
    private boolean persistExport = true;
    private boolean exportUsesSystemEnvironment = false;
    private Resource resource;
    private String execute;
    private Resource executeFileAtStart;
    private CommandActivatorProvider commandActivatorProvider;
    private OptionActivatorProvider optionActivatorProvider;
    private CommandRegistry commandRegistry;
    private CommandInvocationServices commandInvocationServices;
    private CommandNotFoundHandler commandNotFoundHandler;
    private CompleterInvocationProvider completerInvocationProvider;
    private ConverterInvocationProvider converterInvocationProvider;
    private ValidatorInvocationProvider validatorInvocationProvider;
    private ManProvider manProvider;
    private Connection connection;
    private InvocationProviders invocationProviders;

    protected SettingsImpl() {
    }

    protected SettingsImpl(Settings baseSettings) {
        setMode(baseSettings.mode());
        setHistoryFile(baseSettings.historyFile());
        setHistoryFilePermission(baseSettings.historyFilePermission());
        setHistorySize(baseSettings.historySize());
        setBellStyle(baseSettings.bellStyle());
        setStdIn(baseSettings.stdIn());
        setStdOut(baseSettings.stdOut());
        setStdErr(baseSettings.stdErr());
        setInputrc(baseSettings.inputrc());
        setLogging(baseSettings.logging());
        setDisableCompletion(baseSettings.completionDisabled());
        setLogFile(baseSettings.logFile());
        setReadInputrc(baseSettings.readInputrc());
        setHistoryDisabled(baseSettings.historyDisabled());
        setHistoryPersistent(baseSettings.historyPersistent());
        setAliasFile(baseSettings.aliasFile());
        setAliasEnabled(baseSettings.aliasEnabled());
        setPersistAlias(baseSettings.persistAlias());
        setQuitHandler(baseSettings.quitHandler());
        setInterruptHook(baseSettings.interruptHook());
        enableOperatorParser(baseSettings.operatorParserEnabled());
        setManEnabled(baseSettings.manEnabled());
        setAeshContext(baseSettings.aeshContext());
        setExportEnabled(baseSettings.exportEnabled());
        setExportFile(baseSettings.exportFile());
        setPersistExport(baseSettings.persistExport());
        setResource(baseSettings.resource());
        setExportUsesSystemEnvironment(baseSettings.exportUsesSystemEnvironment());
        setExecuteAtStart(baseSettings.executeAtStart());
        setCommandActivatorProvider(baseSettings.commandActivatorProvider());
        setOptionActivatorProvider(baseSettings.optionActivatorProvider());
        setCommandRegistry(baseSettings.commandRegistry());
        setCommandInvocationServices(baseSettings.commandInvocationServices());
        setCommandNotFoundHandler(baseSettings.commandNotFoundHandler());
        setCompleterInvocationProvider(baseSettings.completerInvocationProvider());
        setConverterInvocationProvider(baseSettings.converterInvocationProvider());
        setValidatorInvocationProvider(baseSettings.validatorInvocationProvider());
        setManProvider(baseSettings.manProvider());
        setConnection(baseSettings.connection());
        setInvocationProviders(baseSettings.invocationProviders());
    }

    public void resetToDefaults() {
        editMode = EditMode.Mode.EMACS;
        historyFile = null;
        historyFilePermission = null;
        historySize = 500;
        historyDisabled = false;
        historyPersistent = true;
        bellStyle = null;
        inputStream = null;
        setStdOut(null);
        setStdErr(null);
        readInputrc = true;
        isLogging = false;
        logFile = null;
        disableCompletion = false;
        setQuitHandler(null);
        setAliasEnabled(true);
    }

    /**
     * Either Emacs or Vi mode.
     * Emacs is default if not set
     *
     * @return editing mode
     */
    @Override
    public EditMode.Mode mode() {
        return editMode;
    }

    public void setMode(EditMode.Mode editMode) {
        this.editMode = editMode;
    }

    /**
     * Get EditMode based on os and mode
     *
     * @return edit mode
     */
    @Override
    public EditMode editMode() {
        if(readInputrc) {
            try {
                return EditModeBuilder.builder().parseInputrc(new FileInputStream(inputrc())).create();
            }
            catch(FileNotFoundException e) {
                return EditModeBuilder.builder(mode()).create();
            }
        }
        else
            return EditModeBuilder.builder(mode()).create();
    }

    /**
     * @return the historyFilePermission
     */
    public FileAccessPermission historyFilePermission() {
        return historyFilePermission;
    }

    /**
     * @param historyFilePermission the historyFilePermission to set
     */
    public void setHistoryFilePermission(FileAccessPermission historyFilePermission) {
        this.historyFilePermission = historyFilePermission;
    }

    /**
     * If not set the history file will be:
     * $HOME/.aesh_history
     *
     * @return history file
     */
    @Override
    public File historyFile() {
        if(historyFile == null) {
            return new File(System.getProperty("user.home")+
                    Config.getPathSeparator()+".aesh_history");
        }
        else
            return historyFile;
    }

    public void setHistoryFile(File historyFile) {
        this.historyFile = historyFile;
    }

    /**
     * By default history size is 500
     * If its set to -1 the size is unlimited (Integer.MAX_VALUE)
     *
     * @return size
     */
    @Override
    public int historySize() {
        return historySize;
    }

    /**
     * By default history size is 500
     * If its set to -1 the size is unlimited (Integer.MAX_VALUE)
     *
     * @param historySize size
     */
    public void setHistorySize(int historySize) {
        this.historySize = historySize;
    }

    /**
     * By default, bell style is noisy
     * NOTE: Not implemented yet
     *
     * @return bell style
     */
    @Override
    public String bellStyle() {
        return bellStyle;
    }

    public void setBellStyle(String bellStyle) {
        this.bellStyle = bellStyle;
    }

    /**
     * If not set, System.in will be used
     *
     * @return input
     */
    @Override
    public InputStream stdIn() {
        if(inputStream == null) {
            inputStream = System.in;
        }
        return inputStream;
    }

    /**
     * Set where input is coming from
     *
     * @param inputStream input
     */
    public void setStdIn(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * If not set System.out is used
     * @return out
     */
    @Override
    public PrintStream stdOut() {
        if(stdOut == null)
            return System.out;
        else
            return stdOut;
    }

    /**
     * Set where output should go to
     * @param stdOut output
     */
    public void setStdOut(PrintStream stdOut) {
        this.stdOut = stdOut;
    }

     /**
     * If not set System.out is used
     * @return out
     */
    @Override
    public PrintStream stdErr() {
        if(stdErr == null)
            return System.err;
        else
            return stdErr;
    }

    /**
     * Set where output should go to
     * @param stdErr output
     */
    public void setStdErr(PrintStream stdErr) {
        this.stdErr = stdErr;
    }

    /**
     * Get the inputrc file, if not set it defaults to:
     * System.getProperty("user.home")+Config.getPathSeparator()+".inputrc"
     *
     * @return inputrc
     */
    @Override
    public File inputrc() {
        if(inputrc == null) {
            inputrc = new File(System.getProperty("user.home")+Config.getPathSeparator()+".inputrc");
        }
        return inputrc;
    }

    public void setInputrc(File inputrc) {
        this.inputrc = inputrc;
    }

    /**
     * Are we logging?
     *
     * @return logging
     */
    @Override
    public boolean logging() {
        return isLogging;
    }

    /**
     * Set logging, by default set to true
     *
     * @param logging do log
     */
    public void setLogging(boolean logging) {
        isLogging = logging;
    }

    /**
     * Is completion disabled?
     * Set to false by default
     *
     * @return dis completion
     */
    @Override
    public boolean completionDisabled() {
        return disableCompletion;
    }

    /**
     * Set to true do disable completion
     * Set to false by default
     *
     * @param disableCompletion dis
     */
    public void setDisableCompletion(boolean disableCompletion) {
        this.disableCompletion = disableCompletion;
    }

    /**
     * Get log file
     *
     * @return log file
     */
    @Override
    public String logFile() {
        if(logFile == null) {
            logFile = Config.getTmpDir()+Config.getPathSeparator()+"aesh.log";
        }
        return logFile;
    }

    /**
     * Specify a log file
     *
     * @param logFile file
     */
    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    /**
     * Should we read config from inputrc
     * Set to true by default
     *
     * @return do we?
     */
    @Override
    public boolean readInputrc() {
        return readInputrc;
    }

    /**
     * Specify if we should read config from inputrc
     * Set to true by default
     *
     * @param readInputrc specify
     */
    public void setReadInputrc(boolean readInputrc) {
        this.readInputrc = readInputrc;
    }

    /**
     * Is history disabled
     * Set to true to disable history
     *
     * @return historyDisabled
     */
    @Override
    public boolean historyDisabled() {
        return historyDisabled;
    }

    /**
     * Is history disabled
     * Set to true to disable history
     *
     * @param historyDisabled history
     */
    public void setHistoryDisabled(boolean historyDisabled) {
        this.historyDisabled = historyDisabled;
    }

    /**
     * Is the history list persisted to file.
     * Set to true by default
     *
     * @return is history persistent
     */
    @Override
    public boolean historyPersistent() {
        return historyPersistent;
    }

    /**
     * Is the history list persisted to file.
     * Set to true by default
     *
     * @param historyPersistent history
     */
    public void setHistoryPersistent(boolean historyPersistent) {
        this.historyPersistent = historyPersistent;
    }

    public void setAliasFile(File file) {
        this.aliasFile = file;
    }

    @Override
    public File aliasFile() {
        if(aliasFile == null)
            aliasFile = new File(Config.getHomeDir()+Config.getPathSeparator()+".aesh_aliases");

        return aliasFile;
    }

    @Override
    public boolean aliasEnabled() {
        return aliasEnabled;
    }

    public void setAliasEnabled(boolean enabled) {
        aliasEnabled = enabled;
    }

    public void setPersistAlias(boolean persist) {
        persistAlias = persist;
    }

    @Override
    public boolean persistAlias() {
        return persistAlias;
    }

    public void setQuitHandler(QuitHandler qh) {
        quitHandler = qh;
    }

    @Override
    public QuitHandler quitHandler() {
        return quitHandler;
    }

    public void setInterruptHook(InterruptHook hook) {
        interruptHook = hook;
    }

    @Override
    public InterruptHook interruptHook() {
        return interruptHook;
    }

    public void enableOperatorParser(boolean enable) {
        enableOperatorParser = enable;
    }

    @Override
    public boolean operatorParserEnabled() {
        return enableOperatorParser;
    }

    @Override
    public boolean manEnabled() {
        return manEnabled;
    }

    public void setManEnabled(boolean enabled) {
        this.manEnabled = enabled;
    }

    @Override
    public AeshContext aeshContext() {
        if(aeshContext == null)
            aeshContext = new DefaultAeshContext(resource().newInstance(Config.getUserDir()));
        return aeshContext;
    }

    public void setAeshContext(AeshContext aeshContext) {
        this.aeshContext = aeshContext;
    }

    @Override
    public File exportFile() {
        if(exportFile == null)
            exportFile = new File(Config.getHomeDir()+Config.getPathSeparator()+".aesh_export");
        return exportFile;
    }

    public void setExportFile(File exportFile) {
        if(exportFile != null)
            this.exportFile = exportFile;
    }

    @Override
    public boolean exportEnabled() {
        return exportEnabled;
    }

    public void setExportEnabled(boolean exportEnabled) {
        this.exportEnabled = exportEnabled;
    }

    @Override
    public void setPersistExport(boolean persistExport) {
        this.persistExport = persistExport;
    }

    @Override
    public boolean persistExport() {
        return persistExport;
    }

    @Override
    public void setExportUsesSystemEnvironment(boolean isLoad) {
        this.exportUsesSystemEnvironment = isLoad;
    }

    @Override
    public boolean exportUsesSystemEnvironment() {
        return this.exportUsesSystemEnvironment;
    }

    @Override
    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @Override
    public void setExecuteAtStart(String execute) {
        if(execute.endsWith(Config.getLineSeparator()))
            this.execute = execute;
        else
            this.execute = execute + Config.getLineSeparator();
    }

    @Override
    public String executeAtStart() {
        return execute;
    }

    @Override
    public void setExecuteFileAtStart(Resource executeFileAtStart) {
        this.executeFileAtStart = executeFileAtStart;
    }

    @Override
    public Resource executeFileAtStart() {
        return executeFileAtStart;
    }

    @Override
    public Resource resource() {
        if(resource == null)
            resource = new FileResource("");
        return resource;
    }

    @Override
    public CommandRegistry commandRegistry() {
        return commandRegistry;
    }

    @Override
    public CommandInvocationServices commandInvocationServices() {
        return commandInvocationServices;
    }

    @Override
    public CommandNotFoundHandler commandNotFoundHandler() {
        return commandNotFoundHandler;
    }

    @Override
    public CompleterInvocationProvider completerInvocationProvider() {
        return completerInvocationProvider;
    }

    @Override
    public ConverterInvocationProvider converterInvocationProvider() {
        return converterInvocationProvider;
    }

    @Override
    public ValidatorInvocationProvider validatorInvocationProvider() {
        return validatorInvocationProvider;
    }

    @Override
    public OptionActivatorProvider optionActivatorProvider() {
        return optionActivatorProvider;
    }

    @Override
    public ManProvider manProvider() {
        return manProvider;
    }

    @Override
    public CommandActivatorProvider commandActivatorProvider() {
        return commandActivatorProvider;
    }

    @Override
    public Connection connection() {
        return connection;
    }

    public void setCommandActivatorProvider(CommandActivatorProvider commandActivatorProvider) {
        this.commandActivatorProvider = commandActivatorProvider;
    }

    public void setOptionActivatorProvider(OptionActivatorProvider optionActivatorProvider) {
        this.optionActivatorProvider = optionActivatorProvider;
    }

    public void setCommandRegistry(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    public void setCommandInvocationServices(CommandInvocationServices commandInvocationServices) {
        this.commandInvocationServices = commandInvocationServices;
    }

    public void setCommandNotFoundHandler(CommandNotFoundHandler commandNotFoundHandler) {
        this.commandNotFoundHandler = commandNotFoundHandler;
    }

    public void setCompleterInvocationProvider(CompleterInvocationProvider completerInvocationProvider) {
        this.completerInvocationProvider = completerInvocationProvider;
    }

    public void setConverterInvocationProvider(ConverterInvocationProvider converterInvocationProvider) {
        this.converterInvocationProvider = converterInvocationProvider;
    }

    public void setValidatorInvocationProvider(ValidatorInvocationProvider validatorInvocationProvider) {
        this.validatorInvocationProvider = validatorInvocationProvider;
    }

    public void setManProvider(ManProvider manProvider) {
        this.manProvider = manProvider;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InvocationProviders invocationProviders() {
        return invocationProviders;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void setInvocationProviders(InvocationProviders invocationProviders) {
        this.invocationProviders = invocationProviders;
    }
}
