/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.settings;

import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.helper.InterruptHook;
import org.jboss.aesh.edit.EditMode;
import org.jboss.aesh.edit.EmacsEditMode;
import org.jboss.aesh.edit.KeyOperationFactory;
import org.jboss.aesh.edit.KeyOperationManager;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.edit.ViEditMode;
import org.jboss.aesh.terminal.POSIXTerminal;
import org.jboss.aesh.terminal.Terminal;
import org.jboss.aesh.terminal.WindowsTerminal;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * Settings object that is parsed when Console is initialized.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class SettingsImpl implements Settings {

    private String name;
    private Mode editMode = Mode.EMACS;
    private File historyFile;
    private int historySize = 500;
    private boolean historyDisabled = false;
    private boolean historyPersistent = true;
    private String bellStyle;
    private boolean ansiConsole = true;
    private InputStream inputStream;
    private PrintStream stdOut;
    private PrintStream stdErr;
    private Terminal terminal;
    private boolean readInputrc = true;
    private File inputrc;
    private boolean isLogging = false;
    private String logFile;
    private boolean disableCompletion = false;
    private boolean readAhead = true;
    private QuitHandler quitHandler;
    private KeyOperationManager operationManager = new KeyOperationManager();
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

    protected SettingsImpl() {
    }

    protected SettingsImpl(Settings baseSettings) {
        setName(baseSettings.getName());
        setMode(baseSettings.getMode());
        setHistoryFile(baseSettings.getHistoryFile());
        setHistorySize(baseSettings.getHistorySize());
        setBellStyle(baseSettings.getBellStyle());
        setAnsiConsole(baseSettings.isAnsiConsole());
        setInputStream(baseSettings.getInputStream());
        setStdOut(baseSettings.getStdOut());
        setStdErr(baseSettings.getStdErr());
        setTerminal(baseSettings.getTerminal());
        setInputrc(baseSettings.getInputrc());
        setLogging(baseSettings.isLogging());
        setDisableCompletion(baseSettings.isDisableCompletion());
        setLogFile(baseSettings.getLogFile());
        setReadInputrc(baseSettings.doReadInputrc());
        setHistoryDisabled(baseSettings.isHistoryDisabled());
        setHistoryPersistent(baseSettings.isHistoryPersistent());
        setReadAhead(baseSettings.isReadAhead());
        setAliasFile(baseSettings.getAliasFile());
        setAliasEnabled(baseSettings.isAliasEnabled());
        setPersistAlias(baseSettings.doPersistAlias());
        setQuitHandler(baseSettings.getQuitHandler());
        setInterruptHook(baseSettings.getInterruptHook());
        enableOperatorParser(baseSettings.isOperatorParserEnabled());
        setManEnabled(baseSettings.isManEnabled());
        setAeshContext(baseSettings.getAeshContext());
        setExportEnabled(baseSettings.isExportEnabled());
        setExportFile(baseSettings.getExportFile());
        setPersistExport(baseSettings.doPersistExport());
    }

    public void resetToDefaults() {
        setName("aesh");
        editMode = Mode.EMACS;
        historyFile = null;
        historySize = 500;
        historyDisabled = false;
        historyPersistent = true;
        bellStyle = null;
        ansiConsole = true;
        inputStream = null;
        setStdOut(null);
        setStdErr(null);
        terminal = null;
        readInputrc = true;
        isLogging = false;
        logFile = null;
        disableCompletion = false;
        setQuitHandler(null);
        operationManager.clear();
        setAliasEnabled(true);
    }

    /**
     * Set the name of the cli
     *
     * @param name name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the name of the cli
     *
     * @return default is aesh
     */
    @Override
    public String getName() {
        if(name == null)
            name = "aesh";
        return name;
    }

    /**
     * Either Emacs or Vi mode.
     * Emacs is default if not set
     *
     * @return editing mode
     */
    @Override
    public Mode getMode() {
        return editMode;
    }

    public void setMode(Mode editMode) {
        this.editMode = editMode;
    }

    /**
     * Get EditMode based on os and mode
     *
     * @return edit mode
     */
    @Override
    public EditMode getEditMode() {
        if(Config.isOSPOSIXCompatible()) {
            if(getMode() == Mode.EMACS)
                return new EmacsEditMode(getOperationManager());
            else
                return new ViEditMode(getOperationManager());
        }
        else {
            if(getMode() == Mode.EMACS)
                return new EmacsEditMode(getOperationManager());
            else
                return new ViEditMode(getOperationManager());
        }
    }

    @Override
    public void resetEditMode() {
        operationManager.clear();
    }

    @Override
    public KeyOperationManager getOperationManager() {
        if(operationManager.getOperations().size() < 1) {
            if(getMode() == Mode.EMACS)
                operationManager.addOperations(KeyOperationFactory.generateEmacsMode());
            else
                operationManager.addOperations(KeyOperationFactory.generateViMode());
        }
        return operationManager;
    }

    /**
     * If not set the history file will be:
     * $HOME/.aesh_history
     *
     * @return history file
     */
    @Override
    public File getHistoryFile() {
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
    public int getHistorySize() {
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
    public String getBellStyle() {
        return bellStyle;
    }

    public void setBellStyle(String bellStyle) {
        this.bellStyle = bellStyle;
    }

    /**
     * @return true if its an ansi console
     */
    @Override
    public boolean isAnsiConsole() {
        return ansiConsole;
    }

    /**
     * Possible to override the type of console
     *
     * @param ansiConsole is it an ansi compatible console?
     */
    public void setAnsiConsole(boolean ansiConsole) {
        this.ansiConsole = ansiConsole;
    }

    /**
     * If not set, System.in will be used
     *
     * @return input
     */
    @Override
    public InputStream getInputStream() {
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
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * If not set System.out is used
     * @return out
     */
    @Override
    public PrintStream getStdOut() {
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
    public PrintStream getStdErr() {
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
     * Use the specified terminal implementation
     * If not set, aesh will try to use the best suited one
     * @return terminal
     */
    @Override
    public Terminal getTerminal() {
        if(terminal == null) {
            if(Config.isOSPOSIXCompatible())
                terminal = new POSIXTerminal();
            else
                terminal = new WindowsTerminal();
        }

        return terminal;
    }

    /**
     * Specify which terminal impl to use
     * @param terminal term
     */
    public void setTerminal(Terminal terminal) {
        this.terminal = terminal;
    }

    /**
     * Get the inputrc file, if not set it defaults to:
     * System.getProperty("user.home")+Config.getPathSeparator()+".inputrc"
     *
     * @return inputrc
     */
    @Override
    public File getInputrc() {
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
    public boolean isLogging() {
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
    public boolean isDisableCompletion() {
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
    public String getLogFile() {
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
    public boolean doReadInputrc() {
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
    public boolean isHistoryDisabled() {
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
    public boolean isHistoryPersistent() {
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

    /**
     * Read all bytes on buffer if its available
     * Set to true by default
     * WARNING: Do not set this to false if unsure
     *
     * @return readAhead
     */
    @Override
    public boolean isReadAhead() {
        return readAhead;
    }

    /**
     * Read all bytes on buffer if its available
     * Set to true by default
     * WARNING: Do not set this to false if unsure
     *
     * @param readAhead read
     */
    public void setReadAhead(boolean readAhead) {
        this.readAhead = readAhead;
    }

    public void setAliasFile(File file) {
        this.aliasFile = file;
    }

    @Override
    public File getAliasFile() {
        if(aliasFile == null)
            aliasFile = new File(Config.getHomeDir()+Config.getPathSeparator()+".aesh_aliases");

        return aliasFile;
    }

    @Override
    public boolean isAliasEnabled() {
        return aliasEnabled;
    }

    public void setAliasEnabled(boolean enabled) {
        aliasEnabled = enabled;
    }

    public void setPersistAlias(boolean persist) {
        persistAlias = persist;
    }

    @Override
    public boolean doPersistAlias() {
        return persistAlias;
    }

    public void setQuitHandler(QuitHandler qh) {
        quitHandler = qh;
    }

    @Override
    public QuitHandler getQuitHandler() {
        return quitHandler;
    }

    public void setInterruptHook(InterruptHook hook) {
        interruptHook = hook;
    }

    @Override
    public boolean hasInterruptHook() {
        return interruptHook != null;
    }

    @Override
    public InterruptHook getInterruptHook() {
        return interruptHook;
    }

    public void enableOperatorParser(boolean enable) {
        enableOperatorParser = enable;
    }

    @Override
    public boolean isOperatorParserEnabled() {
        return enableOperatorParser;
    }

    @Override
    public void switchMode() {
       if(editMode == Mode.VI)
           editMode = Mode.EMACS;
        else
           editMode = Mode.VI;
    }

    @Override
    public boolean isManEnabled() {
        return manEnabled;
    }

    public void setManEnabled(boolean enabled) {
        this.manEnabled = enabled;
    }

    @Override
    public AeshContext getAeshContext() {
        if(aeshContext == null)
            aeshContext = new AeshContextImpl();
        return aeshContext;
    }

    public void setAeshContext(AeshContext aeshContext) {
        this.aeshContext = aeshContext;
    }

    @Override
    public File getExportFile() {
        if(exportFile == null)
            exportFile = new File(Config.getHomeDir()+Config.getPathSeparator()+".aesh_export");
        return exportFile;
    }

    public void setExportFile(File exportFile) {
        if(exportFile != null)
            this.exportFile = exportFile;
    }

    @Override
    public boolean isExportEnabled() {
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
    public boolean doPersistExport() {
        return persistExport;
    }
}
