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

import org.aesh.command.Command;
import org.aesh.command.CommandNotFoundHandler;
import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.export.ExportChangeListener;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.activator.OptionActivatorProvider;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationProvider;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.readline.AeshContext;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.activator.CommandActivatorProvider;
import org.aesh.command.completer.CompleterInvocationProvider;
import org.aesh.command.converter.ConverterInvocationProvider;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.impl.registry.MutableCommandRegistryImpl;
import org.aesh.command.validator.ValidatorInvocationProvider;
import org.aesh.io.Resource;
import org.aesh.readline.editing.EditMode;
import org.aesh.terminal.Connection;
import org.aesh.readline.util.LoggerUtil;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class SettingsBuilder {

    private SettingsImpl<? extends Command<? extends CommandInvocation>,? extends CommandInvocation,
            ? extends ConverterInvocation, ? extends CompleterInvocation,
            ? extends ValidatorInvocation, ? extends OptionActivator,
            ? extends CommandActivator> settings;

    private SettingsBuilder apply(Consumer<SettingsBuilder> consumer) {
        consumer.accept(this);
        return this;
    }

    public static SettingsBuilder builder() {
        return new SettingsBuilder();
    }

    private SettingsBuilder() {
        settings = new SettingsImpl<>();
    }

    public SettingsBuilder(Settings<? extends Command,? extends CommandInvocation,
            ? extends ConverterInvocation, ? extends CompleterInvocation,
            ? extends ValidatorInvocation, ? extends OptionActivator,
            ? extends CommandActivator> baseSettings) {
       settings = (SettingsImpl<? extends Command,? extends CommandInvocation,
            ? extends ConverterInvocation, ? extends CompleterInvocation,
            ? extends ValidatorInvocation, ? extends OptionActivator,
            ? extends CommandActivator>) baseSettings.clone();
    }

    public SettingsBuilder mode(EditMode.Mode mode) {
        return apply(c -> c.settings.setMode(mode));
    }

    public SettingsBuilder historyFile(File history) {
        return apply(c -> c.settings.setHistoryFile(history));
    }

    public SettingsBuilder historyFilePermission(FileAccessPermission fileAccessPermission) {
        return apply(c -> c.settings.setHistoryFilePermission(fileAccessPermission));
    }

    public SettingsBuilder historySize(int size) {
        return apply(c -> c.settings.setHistorySize(size));
    }

    public SettingsBuilder bellStyle(String bellStyle) {
        return apply(c -> c.settings.setBellStyle(bellStyle));
    }

    public SettingsBuilder inputStream(InputStream inputStream) {
        return apply(c -> c.settings.setStdIn(inputStream));
    }

    public SettingsBuilder outputStream(PrintStream outputStream) {
        return apply(c -> c.settings.setStdOut(outputStream));
    }

    public SettingsBuilder outputStreamError(PrintStream error) {
        return apply(c -> c.settings.setStdErr(error));
    }

    public SettingsBuilder inputrc(File inputrc) {
        return apply(c -> c.settings.setInputrc(inputrc));
    }

    public SettingsBuilder logging(boolean logging) {
        return apply(c -> c.settings.setLogging(logging));
    }

    public SettingsBuilder disableCompletion(boolean disableCompletion) {
        return apply(c -> c.settings.setDisableCompletion(disableCompletion));
    }

    public SettingsBuilder logfile(String logFile) {
        return apply(c -> c.settings.setLogFile(logFile));
    }

    public SettingsBuilder readInputrc(boolean readInputrc) {
        return apply(c -> c.settings.setReadInputrc(readInputrc));
    }

    public SettingsBuilder disableHistory(boolean disableHistory) {
        return apply(c -> c.settings.setHistoryDisabled(disableHistory));
    }

    public SettingsBuilder persistHistory(boolean persistHistory) {
        return apply(c -> c.settings.setHistoryPersistent(persistHistory));
    }

    public SettingsBuilder aliasFile(File aliasFile) {
        return apply(c -> c.settings.setAliasFile(aliasFile));
    }

    public SettingsBuilder enableAlias(boolean enableAlias) {
        return apply(c -> c.settings.setAliasEnabled(enableAlias));
    }

    public SettingsBuilder persistAlias(boolean persistAlias) {
        return apply(c -> c.settings.setPersistAlias(persistAlias));
    }

    public SettingsBuilder quitHandler(QuitHandler quitHandler) {
        return apply(c -> c.settings.setQuitHandler(quitHandler));
    }

    public SettingsBuilder parseOperators(boolean parseOperators) {
        return apply(c -> c.settings.enableOperatorParser(parseOperators));
    }

    public SettingsBuilder enableMan(boolean enableMan) {
        return apply(c -> c.settings.setManEnabled(enableMan));
    }

    public SettingsBuilder aeshContext(AeshContext aeshContext) {
        return apply(c -> c.settings.setAeshContext(aeshContext));
    }

    public SettingsBuilder enableExport(boolean enableExport) {
        return apply(c -> c.settings.setExportEnabled(enableExport));
    }

    public SettingsBuilder exportFile(File exportFile) {
        return apply(c -> c.settings.setExportFile(exportFile));
    }

    public SettingsBuilder setPersistExport(boolean persistExport) {
        return apply(c -> c.settings.setPersistExport(persistExport));
    }

    public SettingsBuilder setExportUsesSystemEnvironment(boolean isLoad) {
        return apply(c -> c.settings.setExportUsesSystemEnvironment(isLoad));
    }

    public SettingsBuilder setFileResource(Resource resource) {
        return apply(c -> c.settings.setResource(resource));
    }

    public SettingsBuilder setExecuteAtStart(String execute) {
        return apply(c -> c.settings.setExecuteAtStart(execute));
    }

    public SettingsBuilder setExecuteFileAtStart(Resource executeFile) {
        return apply(c -> c.settings.setExecuteFileAtStart(executeFile));
    }

     public SettingsBuilder commandActivatorProvider(CommandActivatorProvider commandActivatorProvider) {
         return apply(c -> c.settings.setCommandActivatorProvider(commandActivatorProvider));
    }

    public SettingsBuilder optionActivatorProvider(OptionActivatorProvider optionActivatorProvider) {
        return apply(c -> c.settings.setOptionActivatorProvider(optionActivatorProvider));
    }

    public SettingsBuilder commandRegistry(CommandRegistry commandRegistry) {
        return apply(c -> c.settings.setCommandRegistry(commandRegistry));
    }

    public SettingsBuilder commandInvocationProvider(CommandInvocationProvider commandInvocationProvider) {
        return apply(c -> c.settings.setCommandInvocationProvider(commandInvocationProvider));
    }

    public SettingsBuilder commandNotFoundHandler(CommandNotFoundHandler commandNotFoundHandler) {
        return apply(c -> c.settings.setCommandNotFoundHandler(commandNotFoundHandler));
    }

    public SettingsBuilder completerInvocationProvider(CompleterInvocationProvider completerInvocationProvider) {
        return apply(c -> c.settings.setCompleterInvocationProvider(completerInvocationProvider));
    }

    public SettingsBuilder converterInvocationProvider(ConverterInvocationProvider converterInvocationProvider) {
        return apply(c -> c.settings.setConverterInvocationProvider(converterInvocationProvider));
    }

    public SettingsBuilder validatorInvocationProvider(ValidatorInvocationProvider validatorInvocationProvider) {
        return apply(c -> c.settings.setValidatorInvocationProvider(validatorInvocationProvider));
    }

    public SettingsBuilder manProvider(ManProvider manProvider) {
        return apply(c -> c.settings.setManProvider(manProvider));
    }

    public SettingsBuilder invocationProviders(InvocationProviders invocationProviders) {
        return apply(c -> c.settings.setInvocationProviders(invocationProviders));
    }

    public SettingsBuilder connection(Connection connection) {
        return apply(c -> c.settings.setConnection(connection));
    }

    public SettingsBuilder enableOperatorParser(boolean enabled) {
        return apply(c -> c.settings.enableOperatorParser(enabled));
    }

    public SettingsBuilder exportListener(ExportChangeListener listener) {
        return apply(c -> c.settings.setExportListener(listener));
    }

    public SettingsBuilder echoCtrl(boolean echo) {
        return apply(c -> c.settings.echoCtrl(echo));
    }

    public SettingsBuilder redrawPromptOnInterrupt(boolean redraw) {
        return apply(c -> c.settings.redrawPromptOnInterrupt(redraw));
    }

    public SettingsBuilder setInterruptHandler(Consumer<Void> consumer) {
        return apply(c -> c.settings.setInterruptHandler(consumer));
    }

    public SettingsBuilder setScanForCommandPackages(String... packages) {
        return apply(c -> c.settings.setScanForCommandPackages(packages));
    }

    public Settings<? extends Command<? extends CommandInvocation>,? extends CommandInvocation,
            ? extends ConverterInvocation, ? extends CompleterInvocation,
            ? extends ValidatorInvocation, ? extends OptionActivator,
            ? extends CommandActivator> build() {
        if(settings.logging())
            LoggerUtil.doLog();

        if(settings.commandRegistry() == null)
            settings.setCommandRegistry(new MutableCommandRegistryImpl<>());

        if(settings.commandInvocationProvider() == null)
            settings.setCommandInvocationProvider((CommandInvocationProvider) commandInvocation -> commandInvocation);

        if(settings.completerInvocationProvider() == null)
            settings.setCompleterInvocationProvider((CompleterInvocationProvider) completerInvocation -> completerInvocation);

        if(settings.converterInvocationProvider() == null)
            settings.setConverterInvocationProvider((ConverterInvocationProvider) converterInvocation -> converterInvocation);

        if(settings.validatorInvocationProvider() == null)
            settings.setValidatorInvocationProvider((ValidatorInvocationProvider) validatorInvocation -> validatorInvocation);

        if(settings.optionActivatorProvider() == null)
            settings.setOptionActivatorProvider((OptionActivatorProvider) optionActivator -> optionActivator);

        if(settings.commandActivatorProvider() == null)
            settings.setCommandActivatorProvider((CommandActivatorProvider) commandActivator -> commandActivator);

        if(settings.invocationProviders() == null)
            settings.setInvocationProviders(new AeshInvocationProviders(settings.converterInvocationProvider(),
                    settings.completerInvocationProvider(), settings.validatorInvocationProvider(),
                    settings.optionActivatorProvider(), settings.commandActivatorProvider()));

        return settings;
    }
}
