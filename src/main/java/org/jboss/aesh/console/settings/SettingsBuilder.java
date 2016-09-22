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
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.io.Resource;
import org.jboss.aesh.terminal.Terminal;
import org.jboss.aesh.util.LoggerUtil;

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
       settings = (SettingsImpl) baseSettings.clone();
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

    public SettingsBuilder historyFilePermission(FileAccessPermission fileAccessPermission) {
        settings.setHistoryFilePermission(fileAccessPermission);
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

    public SettingsBuilder setExportUsesSystemEnvironment(boolean isLoad) {
        settings.setExportUsesSystemEnvironment(isLoad);
        return this;
    }

    public SettingsBuilder setFileResource(Resource resource) {
        settings.setResource(resource);
        return this;
    }

    public SettingsBuilder setExecuteAtStart(String execute) {
        settings.setExecuteAtStart(execute);
        return this;
    }

    public SettingsBuilder setExecuteFileAtStart(Resource executeFile) {
        settings.setExecuteFileAtStart(executeFile);
        return this;
    }

    public SettingsBuilder parsingQuotes(boolean parsingQuotes) {
        settings.setParsingQuotes(parsingQuotes);
        return this;
    }

    public Settings create() {
        if(settings.isLogging())
            LoggerUtil.doLog();
        return settings;
    }
}
