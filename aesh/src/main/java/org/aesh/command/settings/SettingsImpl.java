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
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.export.ExportChangeListener;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationProvider;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.readline.AeshContext;
import org.aesh.command.activator.OptionActivatorProvider;
import org.aesh.command.validator.ValidatorInvocationProvider;
import org.aesh.io.Resource;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.activator.CommandActivatorProvider;
import org.aesh.command.completer.CompleterInvocationProvider;
import org.aesh.command.converter.ConverterInvocationProvider;
import org.aesh.io.FileResource;
import org.aesh.readline.DefaultAeshContext;
import org.aesh.readline.alias.AliasManager;
import org.aesh.readline.editing.EditMode;
import org.aesh.readline.editing.EditModeBuilder;
import org.aesh.terminal.Connection;
import org.aesh.terminal.utils.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.function.Consumer;

/**
 * Settings object that is parsed when Console is initialized.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class SettingsImpl<CI extends CommandInvocation,
        CI3 extends ConverterInvocation, CI2 extends CompleterInvocation,
        VI extends ValidatorInvocation, OA extends OptionActivator,
        CA extends CommandActivator> implements Settings {

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
    private CommandActivatorProvider<CA> commandActivatorProvider;
    private OptionActivatorProvider<OA> optionActivatorProvider;
    private CommandRegistry<CI> commandRegistry;
    private CommandInvocationProvider<CI> commandInvocationProvider;
    private CommandNotFoundHandler commandNotFoundHandler;
    private CompleterInvocationProvider<CI2> completerInvocationProvider;
    private ConverterInvocationProvider<CI3> converterInvocationProvider;
    private ValidatorInvocationProvider<VI> validatorInvocationProvider;
    private ManProvider manProvider;
    private Connection connection;
    private InvocationProviders<CA, CI3, CI2, VI, OA> invocationProviders;
    private ExportChangeListener exportListener;
    private boolean redrawPrompt = true;
    private boolean echoCtrl = true;
    private Consumer<Void> interruptHandler;
    private String[] scanPackages;
    private boolean enableSearchPaging;
    private AliasManager aliasManager;
    private Consumer<Void> connectionClosedHandler;

    SettingsImpl() {
    }

    protected SettingsImpl(Settings<CI, CI3, CI2, VI, OA, CA> baseSettings) {
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
        setCommandInvocationProvider(baseSettings.commandInvocationProvider());
        setCommandNotFoundHandler(baseSettings.commandNotFoundHandler());
        setCompleterInvocationProvider(baseSettings.completerInvocationProvider());
        setConverterInvocationProvider(baseSettings.converterInvocationProvider());
        setValidatorInvocationProvider(baseSettings.validatorInvocationProvider());
        setManProvider(baseSettings.manProvider());
        setConnection(baseSettings.connection());
        setInvocationProviders(baseSettings.invocationProviders());
        setExportListener(baseSettings.exportListener());
        echoCtrl(baseSettings.isEchoCtrl());
        redrawPromptOnInterrupt(baseSettings.isRedrawPromptOnInterrupt());
        setInterruptHandler(baseSettings.getInterruptHandler());
        setScanForCommandPackages(baseSettings.getScanForCommandPackages());
        setEnableSearchInPaging(baseSettings.enableSearchInPaging());
        setAliasManager(baseSettings.aliasManager());
        setConnectionClosedHandler(baseSettings.connectionClosedHandler());
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
        aliasManager = null;
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

    @Override
    public AliasManager aliasManager() {
        return aliasManager;
    }

    public void setAliasManager(AliasManager aliasManager) {
        this.aliasManager = aliasManager;
    }

    public void setQuitHandler(QuitHandler qh) {
        quitHandler = qh;
    }

    @Override
    public QuitHandler quitHandler() {
        return quitHandler;
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
        if(execute != null) {
            if(execute.endsWith(Config.getLineSeparator()))
                this.execute = execute;
            else
                this.execute = execute + Config.getLineSeparator();
        }
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
    public CommandRegistry<CI> commandRegistry() {
        return commandRegistry;
    }

    @Override
    public CommandInvocationProvider<CI> commandInvocationProvider() {
        return commandInvocationProvider;
    }

    @Override
    public CommandNotFoundHandler commandNotFoundHandler() {
        return commandNotFoundHandler;
    }

    @Override
    public CompleterInvocationProvider<CI2> completerInvocationProvider() {
        return completerInvocationProvider;
    }

    @Override
    public ConverterInvocationProvider<CI3> converterInvocationProvider() {
        return converterInvocationProvider;
    }

    @Override
    public ValidatorInvocationProvider<VI> validatorInvocationProvider() {
        return validatorInvocationProvider;
    }

    @Override
    public OptionActivatorProvider<OA> optionActivatorProvider() {
        return optionActivatorProvider;
    }

    @Override
    public ManProvider manProvider() {
        return manProvider;
    }

    @Override
    public CommandActivatorProvider<CA> commandActivatorProvider() {
        return commandActivatorProvider;
    }

    @Override
    public Connection connection() {
        return connection;
    }

    public void setCommandActivatorProvider(CommandActivatorProvider<CA> commandActivatorProvider) {
        this.commandActivatorProvider = commandActivatorProvider;
    }

    public void setOptionActivatorProvider(OptionActivatorProvider<OA> optionActivatorProvider) {
        this.optionActivatorProvider = optionActivatorProvider;
    }

    public void setCommandRegistry(CommandRegistry<CI> commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    public void setCommandInvocationProvider(CommandInvocationProvider<CI> commandInvocationProvider) {
        this.commandInvocationProvider = commandInvocationProvider;
    }

    public void setCommandNotFoundHandler(CommandNotFoundHandler commandNotFoundHandler) {
        this.commandNotFoundHandler = commandNotFoundHandler;
    }

    public void setCompleterInvocationProvider(CompleterInvocationProvider<CI2> completerInvocationProvider) {
        this.completerInvocationProvider = completerInvocationProvider;
    }

    public void setConverterInvocationProvider(ConverterInvocationProvider<CI3> converterInvocationProvider) {
        this.converterInvocationProvider = converterInvocationProvider;
    }

    public void setValidatorInvocationProvider(ValidatorInvocationProvider<VI> validatorInvocationProvider) {
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
    public InvocationProviders<CA, CI3, CI2, VI, OA> invocationProviders() {
        return invocationProviders;
    }

    @Override
    public ExportChangeListener exportListener() {
        return exportListener;
    }

    public void setExportListener(ExportChangeListener listener) {
        this.exportListener = listener;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void setInvocationProviders(InvocationProviders<CA, CI3, CI2, VI, OA> invocationProviders) {
        this.invocationProviders = invocationProviders;
    }

    @Override
    public void echoCtrl(boolean echoCtrl) {
        this.echoCtrl = echoCtrl;
    }

    @Override
    public boolean isEchoCtrl() {
        return echoCtrl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setInterruptHandler(Consumer interruptHandler) {
        this.interruptHandler = interruptHandler;
    }

    @Override
    public Consumer<Void> getInterruptHandler() {
        return interruptHandler;
    }

    @Override
    public void setConnectionClosedHandler(Consumer handler) {
        this.connectionClosedHandler = handler;

    }

    @Override
    public Consumer<Void> connectionClosedHandler() {
        return connectionClosedHandler;
    }

    @Override
    public void redrawPromptOnInterrupt(boolean redrawPrompt) {
        this.redrawPrompt = redrawPrompt;
    }

    @Override
    public boolean isRedrawPromptOnInterrupt() {
        return redrawPrompt;
    }

    @Override
    public void setScanForCommandPackages(String... packages) {
        this.scanPackages = packages;
    }

    @Override
    public String[] getScanForCommandPackages() {
        return scanPackages;
    }

    @Override
    public void setEnableSearchInPaging(boolean enable) {
        enableSearchPaging = enable;
    }

    @Override
    public boolean enableSearchInPaging() {
        return enableSearchPaging;
    }
}
