/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.settings;

import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.helper.InterruptHook;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.terminal.Terminal;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class SettingsBuilder {

    SettingsImpl settings;

    public SettingsBuilder() {
        settings = new SettingsImpl();
    }

    public SettingsBuilder(Settings baseSettings) {
        settings = new SettingsImpl();
        settings.setName(baseSettings.getName());
        settings.setMode(baseSettings.getMode());
        settings.setHistoryFile(baseSettings.getHistoryFile());
        settings.setHistorySize(baseSettings.getHistorySize());
        settings.setBellStyle(baseSettings.getBellStyle());
        settings.setAnsiConsole(baseSettings.isAnsiConsole());
        settings.setInputStream(baseSettings.getInputStream());
        settings.setStdOut(baseSettings.getStdOut());
        settings.setStdErr(baseSettings.getStdErr());
        settings.setTerminal(baseSettings.getTerminal());
        settings.setInputrc(baseSettings.getInputrc());
        settings.setLogging(baseSettings.isLogging());
        settings.setDisableCompletion(baseSettings.isDisableCompletion());
        settings.setLogFile(baseSettings.getLogFile());
        settings.setReadInputrc(baseSettings.doReadInputrc());
        settings.setHistoryDisabled(baseSettings.isHistoryDisabled());
        settings.setHistoryPersistent(baseSettings.isHistoryPersistent());
        settings.setReadAhead(baseSettings.isReadAhead());
        settings.setAliasFile(baseSettings.getAliasFile());
        settings.setAliasEnabled(baseSettings.isAliasEnabled());
        settings.setPersistAlias(baseSettings.doPersistAlias());
        settings.setQuitHandler(baseSettings.getQuitHandler());
        settings.setInterruptHook(baseSettings.getInterruptHook());
        settings.enableOperatorParser(baseSettings.isOperatorParserEnabled());
        settings.setManEnabled(baseSettings.isManEnabled());
        settings.setAeshContext(baseSettings.getAeshContext());
        settings.setExportEnabled(baseSettings.isExportEnabled());
        settings.setExportFile(baseSettings.getExportFile());
        settings.setPersistExport(baseSettings.doPersistExport());
    }

    public SettingsBuilder name(String name) {
        settings.setName(name);
        return this;
    }

    public SettingsBuilder mode(Mode mode) {
        settings.setMode(mode);
        return this;
    }

    public SettingsBuilder historyFile(File history) {
        settings.setHistoryFile(history);
        return this;
    }

    public SettingsBuilder historySize(int size) {
        settings.setHistorySize(size);
        return this;
    }

    public SettingsBuilder bellStyle(String bellStyle) {
        settings.setBellStyle(bellStyle);
        return this;
    }

    public SettingsBuilder ansi(boolean ansi) {
        settings.setAnsiConsole(ansi);
        return this;
    }

    public SettingsBuilder inputStream(InputStream inputStream) {
        settings.setInputStream(inputStream);
        return this;
    }

    public SettingsBuilder outputStream(PrintStream outputStream) {
        settings.setStdOut(outputStream);
        return this;
    }

    public SettingsBuilder outputStreamError(PrintStream error) {
        settings.setStdErr(error);
        return this;
    }

    public SettingsBuilder terminal(Terminal terminal) {
        settings.setTerminal(terminal);
        return this;
    }

    public SettingsBuilder inputrc(File inputrc) {
        settings.setInputrc(inputrc);
        return this;
    }

    public SettingsBuilder logging(boolean logging) {
        settings.setLogging(logging);
        return this;
    }

    public SettingsBuilder disableCompletion(boolean disableCompletion) {
        settings.setDisableCompletion(disableCompletion);
        return this;
    }

    public SettingsBuilder logfile(String logFile) {
        settings.setLogFile(logFile);
        return this;
    }

    public SettingsBuilder readInputrc(boolean readInputrc) {
        settings.setReadInputrc(readInputrc);
        return this;
    }

    public SettingsBuilder disableHistory(boolean disableHistory) {
        settings.setHistoryDisabled(disableHistory);
        return this;
    }

    public SettingsBuilder persistHistory(boolean persistHistory) {
        settings.setHistoryPersistent(persistHistory);
        return this;
    }

    public SettingsBuilder readAhead(boolean readAhead) {
        settings.setReadAhead(readAhead);
        return this;
    }

    public SettingsBuilder aliasFile(File aliasFile) {
        settings.setAliasFile(aliasFile);
        return this;
    }

    public SettingsBuilder enableAlias(boolean enableAlias) {
        settings.setAliasEnabled(enableAlias);
        return this;
    }

    public SettingsBuilder persistAlias(boolean persistAlias) {
        settings.setPersistAlias(persistAlias);
        return this;
    }

    public SettingsBuilder quitHandler(QuitHandler quitHandler) {
        settings.setQuitHandler(quitHandler);
        return this;
    }

    public SettingsBuilder interruptHook(InterruptHook interruptHook) {
        settings.setInterruptHook(interruptHook);
        return this;
    }

    public SettingsBuilder parseOperators(boolean parseOperators) {
        settings.enableOperatorParser(parseOperators);
        return this;
    }

    public SettingsBuilder enableMan(boolean enableMan) {
        settings.setManEnabled(enableMan);
        return this;
    }

    public SettingsBuilder aeshContext(AeshContext aeshContext) {
        settings.setAeshContext(aeshContext);
        return this;
    }

    public SettingsBuilder enableExport(boolean enableExport) {
        settings.setExportEnabled(enableExport);
        return this;
    }

    public SettingsBuilder exportFile(File exportFile) {
        settings.setExportFile(exportFile);
        return this;
    }

    public SettingsBuilder setPersistExport(boolean persistExport) {
        settings.setPersistExport(persistExport);
        return this;
    }

    public Settings create() {
        return settings;
    }
}
