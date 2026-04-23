/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.function.Consumer;

import org.aesh.command.CommandNotFoundHandler;
import org.aesh.command.activator.CommandActivatorProvider;
import org.aesh.command.activator.OptionActivatorProvider;
import org.aesh.command.completer.CompleterInvocationProvider;
import org.aesh.command.converter.ConverterInvocationProvider;
import org.aesh.command.export.ExportChangeListener;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationProvider;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.validator.ValidatorInvocationProvider;
import org.aesh.console.AeshContext;
import org.aesh.io.Resource;
import org.aesh.readline.alias.AliasManager;
import org.aesh.readline.editing.EditMode;
import org.aesh.terminal.Connection;
import org.aesh.terminal.utils.LoggerUtil;

/**
 * @author Aesh team
 */
public class SettingsBuilder<CI extends CommandInvocation> {

    private SettingsImpl<CI> settings;

    public static <CI extends CommandInvocation> SettingsBuilder<CI> builder() {
        return new SettingsBuilder<>();
    }

    private SettingsBuilder() {
        settings = new SettingsImpl<>();
    }

    public SettingsBuilder(Settings<CI> baseSettings) {
        settings = new SettingsImpl<>(baseSettings);
    }

    public SettingsBuilder<CI> mode(EditMode.Mode mode) {
        settings.setMode(mode);
        return this;
    }

    public SettingsBuilder<CI> historyFile(File history) {
        settings.setHistoryFile(history);
        return this;
    }

    public SettingsBuilder<CI> historyFilePermission(FileAccessPermission fileAccessPermission) {
        settings.setHistoryFilePermission(fileAccessPermission);
        return this;
    }

    public SettingsBuilder<CI> historySize(int size) {
        settings.setHistorySize(size);
        return this;
    }

    public SettingsBuilder<CI> bellStyle(String bellStyle) {
        settings.setBellStyle(bellStyle);
        return this;
    }

    public SettingsBuilder<CI> inputStream(InputStream inputStream) {
        settings.setStdIn(inputStream);
        return this;
    }

    public SettingsBuilder<CI> outputStream(PrintStream outputStream) {
        settings.setStdOut(outputStream);
        return this;
    }

    public SettingsBuilder<CI> outputStreamError(PrintStream error) {
        settings.setStdErr(error);
        return this;
    }

    public SettingsBuilder<CI> inputrc(File inputrc) {
        settings.setInputrc(inputrc);
        return this;
    }

    public SettingsBuilder<CI> logging(boolean logging) {
        settings.setLogging(logging);
        return this;
    }

    public SettingsBuilder<CI> disableCompletion(boolean disableCompletion) {
        settings.setDisableCompletion(disableCompletion);
        return this;
    }

    public SettingsBuilder<CI> logfile(String logFile) {
        settings.setLogFile(logFile);
        return this;
    }

    public SettingsBuilder<CI> readInputrc(boolean readInputrc) {
        settings.setReadInputrc(readInputrc);
        return this;
    }

    public SettingsBuilder<CI> disableHistory(boolean disableHistory) {
        settings.setHistoryDisabled(disableHistory);
        return this;
    }

    public SettingsBuilder<CI> persistHistory(boolean persistHistory) {
        settings.setHistoryPersistent(persistHistory);
        return this;
    }

    public SettingsBuilder<CI> aliasFile(File aliasFile) {
        settings.setAliasFile(aliasFile);
        return this;
    }

    public SettingsBuilder<CI> enableAlias(boolean enableAlias) {
        settings.setAliasEnabled(enableAlias);
        return this;
    }

    public SettingsBuilder<CI> persistAlias(boolean persistAlias) {
        settings.setPersistAlias(persistAlias);
        return this;
    }

    public SettingsBuilder<CI> aliasManager(AliasManager aliasManager) {
        settings.setAliasManager(aliasManager);
        return this;
    }

    public SettingsBuilder<CI> quitHandler(QuitHandler quitHandler) {
        settings.setQuitHandler(quitHandler);
        return this;
    }

    public SettingsBuilder<CI> parseOperators(boolean parseOperators) {
        settings.enableOperatorParser(parseOperators);
        return this;
    }

    public SettingsBuilder<CI> enableMan(boolean enableMan) {
        settings.setManEnabled(enableMan);
        return this;
    }

    public SettingsBuilder<CI> aeshContext(AeshContext aeshContext) {
        settings.setAeshContext(aeshContext);
        return this;
    }

    public SettingsBuilder<CI> enableExport(boolean enableExport) {
        settings.setExportEnabled(enableExport);
        return this;
    }

    public SettingsBuilder<CI> exportFile(File exportFile) {
        settings.setExportFile(exportFile);
        return this;
    }

    public SettingsBuilder<CI> setPersistExport(boolean persistExport) {
        settings.setPersistExport(persistExport);
        return this;
    }

    public SettingsBuilder<CI> setExportUsesSystemEnvironment(boolean isLoad) {
        settings.setExportUsesSystemEnvironment(isLoad);
        return this;
    }

    public SettingsBuilder<CI> setFileResource(Resource resource) {
        settings.setResource(resource);
        return this;
    }

    public SettingsBuilder<CI> setExecuteAtStart(String execute) {
        settings.setExecuteAtStart(execute);
        return this;
    }

    public SettingsBuilder<CI> setExecuteFileAtStart(Resource executeFile) {
        settings.setExecuteFileAtStart(executeFile);
        return this;
    }

    public SettingsBuilder<CI> commandActivatorProvider(
            CommandActivatorProvider commandActivatorProvider) {
        settings.setCommandActivatorProvider(commandActivatorProvider);
        return this;
    }

    public SettingsBuilder<CI> optionActivatorProvider(OptionActivatorProvider optionActivatorProvider) {
        settings.setOptionActivatorProvider(optionActivatorProvider);
        return this;
    }

    public SettingsBuilder<CI> commandRegistry(CommandRegistry<CI> commandRegistry) {
        settings.setCommandRegistry(commandRegistry);
        return this;
    }

    public SettingsBuilder<CI> commandInvocationProvider(
            CommandInvocationProvider<CI> commandInvocationProvider) {
        settings.setCommandInvocationProvider(commandInvocationProvider);
        return this;
    }

    public SettingsBuilder<CI> commandNotFoundHandler(CommandNotFoundHandler commandNotFoundHandler) {
        settings.setCommandNotFoundHandler(commandNotFoundHandler);
        return this;
    }

    public SettingsBuilder<CI> completerInvocationProvider(
            CompleterInvocationProvider completerInvocationProvider) {
        settings.setCompleterInvocationProvider(completerInvocationProvider);
        return this;
    }

    public SettingsBuilder<CI> converterInvocationProvider(
            ConverterInvocationProvider converterInvocationProvider) {
        settings.setConverterInvocationProvider(converterInvocationProvider);
        return this;
    }

    public SettingsBuilder<CI> validatorInvocationProvider(
            ValidatorInvocationProvider validatorInvocationProvider) {
        settings.setValidatorInvocationProvider(validatorInvocationProvider);
        return this;
    }

    public SettingsBuilder<CI> manProvider(ManProvider manProvider) {
        settings.setManProvider(manProvider);
        return this;
    }

    public SettingsBuilder<CI> invocationProviders(
            InvocationProviders invocationProviders) {
        settings.setInvocationProviders(invocationProviders);
        return this;
    }

    public SettingsBuilder<CI> connection(Connection connection) {
        settings.setConnection(connection);
        return this;
    }

    public SettingsBuilder<CI> enableOperatorParser(boolean enabled) {
        settings.enableOperatorParser(enabled);
        return this;
    }

    public SettingsBuilder<CI> exportListener(ExportChangeListener listener) {
        settings.setExportListener(listener);
        return this;
    }

    public SettingsBuilder<CI> echoCtrl(boolean echo) {
        settings.echoCtrl(echo);
        return this;
    }

    public SettingsBuilder<CI> redrawPromptOnInterrupt(boolean redraw) {
        settings.redrawPromptOnInterrupt(redraw);
        return this;
    }

    public SettingsBuilder<CI> setInterruptHandler(Consumer<Void> consumer) {
        settings.setInterruptHandler(consumer);
        return this;
    }

    public SettingsBuilder<CI> setConnectionClosedHandler(Consumer<Void> consumer) {
        settings.setConnectionClosedHandler(consumer);
        return this;
    }

    public SettingsBuilder<CI> setScanForCommandPackages(String... packages) {
        settings.setScanForCommandPackages(packages);
        return this;
    }

    public SettingsBuilder<CI> enableSearchInPaging(boolean enable) {
        settings.setEnableSearchInPaging(enable);
        return this;
    }

    public SettingsBuilder<CI> subCommandModeSettings(SubCommandModeSettings subCommandModeSettings) {
        settings.setSubCommandModeSettings(subCommandModeSettings);
        return this;
    }

    public Settings<CI> build() {
        if (settings.logging())
            LoggerUtil.doLog();

        if (settings.invocationProviders() == null)
            settings.setInvocationProviders(new AeshInvocationProviders(settings));

        return settings;
    }
}
