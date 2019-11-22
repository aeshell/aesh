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
import org.aesh.readline.alias.AliasManager;
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
@SuppressWarnings("unchecked")
public class SettingsBuilder<CI extends CommandInvocation,
            CO extends ConverterInvocation, COM extends CompleterInvocation,
            VI extends ValidatorInvocation, OA extends OptionActivator,
            CA extends CommandActivator> {

    private SettingsImpl<CI, CO, COM, VI, OA, CA> settings;

    private SettingsBuilder<CI, CO, COM, VI, OA, CA> apply(Consumer<SettingsBuilder<CI, CO, COM, VI, OA, CA>> consumer) {
        consumer.accept(this);
        return this;
    }

    public static <CI extends CommandInvocation,
            CO extends ConverterInvocation, COM extends CompleterInvocation,
            VI extends ValidatorInvocation, OA extends OptionActivator,
            CA extends CommandActivator> SettingsBuilder<CI,CO,COM,VI,OA,CA> builder() {
        return new SettingsBuilder<>();
    }

    private SettingsBuilder() {
        settings = new SettingsImpl<>();
    }

    public SettingsBuilder(Settings<CI, CO, COM, VI, OA, CA> baseSettings) {
        settings = new SettingsImpl<>(baseSettings);
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> mode(EditMode.Mode mode) {
        return apply(c -> c.settings.setMode(mode));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> historyFile(File history) {
        return apply(c -> c.settings.setHistoryFile(history));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> historyFilePermission(FileAccessPermission fileAccessPermission) {
        return apply(c -> c.settings.setHistoryFilePermission(fileAccessPermission));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> historySize(int size) {
        return apply(c -> c.settings.setHistorySize(size));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> bellStyle(String bellStyle) {
        return apply(c -> c.settings.setBellStyle(bellStyle));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> inputStream(InputStream inputStream) {
        return apply(c -> c.settings.setStdIn(inputStream));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> outputStream(PrintStream outputStream) {
        return apply(c -> c.settings.setStdOut(outputStream));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> outputStreamError(PrintStream error) {
        return apply(c -> c.settings.setStdErr(error));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> inputrc(File inputrc) {
        return apply(c -> c.settings.setInputrc(inputrc));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> logging(boolean logging) {
        return apply(c -> c.settings.setLogging(logging));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> disableCompletion(boolean disableCompletion) {
        return apply(c -> c.settings.setDisableCompletion(disableCompletion));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> logfile(String logFile) {
        return apply(c -> c.settings.setLogFile(logFile));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> readInputrc(boolean readInputrc) {
        return apply(c -> c.settings.setReadInputrc(readInputrc));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> disableHistory(boolean disableHistory) {
        return apply(c -> c.settings.setHistoryDisabled(disableHistory));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> persistHistory(boolean persistHistory) {
        return apply(c -> c.settings.setHistoryPersistent(persistHistory));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> aliasFile(File aliasFile) {
        return apply(c -> c.settings.setAliasFile(aliasFile));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> enableAlias(boolean enableAlias) {
        return apply(c -> c.settings.setAliasEnabled(enableAlias));
    }

    public SettingsBuilder <CI,CO,COM,VI,OA,CA>persistAlias(boolean persistAlias) {
        return apply(c -> c.settings.setPersistAlias(persistAlias));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> aliasManager(AliasManager aliasManager) {
        return apply(c -> c.settings.setAliasManager(aliasManager));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> quitHandler(QuitHandler quitHandler) {
        return apply(c -> c.settings.setQuitHandler(quitHandler));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> parseOperators(boolean parseOperators) {
        return apply(c -> c.settings.enableOperatorParser(parseOperators));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> enableMan(boolean enableMan) {
        return apply(c -> c.settings.setManEnabled(enableMan));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> aeshContext(AeshContext aeshContext) {
        return apply(c -> c.settings.setAeshContext(aeshContext));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> enableExport(boolean enableExport) {
        return apply(c -> c.settings.setExportEnabled(enableExport));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> exportFile(File exportFile) {
        return apply(c -> c.settings.setExportFile(exportFile));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> setPersistExport(boolean persistExport) {
        return apply(c -> c.settings.setPersistExport(persistExport));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> setExportUsesSystemEnvironment(boolean isLoad) {
        return apply(c -> c.settings.setExportUsesSystemEnvironment(isLoad));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> setFileResource(Resource resource) {
        return apply(c -> c.settings.setResource(resource));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> setExecuteAtStart(String execute) {
        return apply(c -> c.settings.setExecuteAtStart(execute));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> setExecuteFileAtStart(Resource executeFile) {
        return apply(c -> c.settings.setExecuteFileAtStart(executeFile));
    }

     public SettingsBuilder<CI,CO,COM,VI,OA,CA> commandActivatorProvider(CommandActivatorProvider commandActivatorProvider) {
         return apply(c -> c.settings.setCommandActivatorProvider(commandActivatorProvider));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> optionActivatorProvider(OptionActivatorProvider optionActivatorProvider) {
        return apply(c -> c.settings.setOptionActivatorProvider(optionActivatorProvider));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> commandRegistry(CommandRegistry commandRegistry) {
        return apply(c -> c.settings.setCommandRegistry(commandRegistry));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> commandInvocationProvider(CommandInvocationProvider commandInvocationProvider) {
        return apply(c -> c.settings.setCommandInvocationProvider(commandInvocationProvider));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> commandNotFoundHandler(CommandNotFoundHandler commandNotFoundHandler) {
        return apply(c -> c.settings.setCommandNotFoundHandler(commandNotFoundHandler));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> completerInvocationProvider(CompleterInvocationProvider completerInvocationProvider) {
        return apply(c -> c.settings.setCompleterInvocationProvider(completerInvocationProvider));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> converterInvocationProvider(ConverterInvocationProvider converterInvocationProvider) {
        return apply(c -> c.settings.setConverterInvocationProvider(converterInvocationProvider));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> validatorInvocationProvider(ValidatorInvocationProvider validatorInvocationProvider) {
        return apply(c -> c.settings.setValidatorInvocationProvider(validatorInvocationProvider));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> manProvider(ManProvider manProvider) {
        return apply(c -> c.settings.setManProvider(manProvider));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> invocationProviders(InvocationProviders<CA,CO,COM,VI,OA> invocationProviders) {
        return apply(c -> c.settings.setInvocationProviders(invocationProviders));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> connection(Connection connection) {
        return apply(c -> c.settings.setConnection(connection));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> enableOperatorParser(boolean enabled) {
        return apply(c -> c.settings.enableOperatorParser(enabled));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> exportListener(ExportChangeListener listener) {
        return apply(c -> c.settings.setExportListener(listener));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> echoCtrl(boolean echo) {
        return apply(c -> c.settings.echoCtrl(echo));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> redrawPromptOnInterrupt(boolean redraw) {
        return apply(c -> c.settings.redrawPromptOnInterrupt(redraw));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> setInterruptHandler(Consumer<Void> consumer) {
        return apply(c -> c.settings.setInterruptHandler(consumer));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> setConnectionClosedHandler(Consumer<Void> consumer) {
        return apply(c -> c.settings.setConnectionClosedHandler(consumer));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> setScanForCommandPackages(String... packages) {
        return apply(c -> c.settings.setScanForCommandPackages(packages));
    }

    public SettingsBuilder<CI,CO,COM,VI,OA,CA> enableSearchInPaging(boolean enable) {
        return apply(c -> c.settings.setEnableSearchInPaging(enable));
    }

    public Settings<CI,CO,COM,VI,OA,CA> build() {
        if(settings.logging())
            LoggerUtil.doLog();

        if(settings.commandRegistry() == null)
            settings.setCommandRegistry(new MutableCommandRegistryImpl<>());

        if(settings.commandInvocationProvider() == null)
            settings.setCommandInvocationProvider(commandInvocation -> (CI) commandInvocation);

        if(settings.completerInvocationProvider() == null)
            settings.setCompleterInvocationProvider(completerInvocation -> (COM) completerInvocation);

        if(settings.converterInvocationProvider() == null)
            settings.setConverterInvocationProvider(converterInvocation -> (CO) converterInvocation);

        if(settings.validatorInvocationProvider() == null)
            settings.setValidatorInvocationProvider(validatorInvocation -> (VI) validatorInvocation);

        if(settings.optionActivatorProvider() == null)
            settings.setOptionActivatorProvider(optionActivator -> (OA) optionActivator);

        if(settings.commandActivatorProvider() == null)
            settings.setCommandActivatorProvider(commandActivator -> (CA) commandActivator);

        if(settings.invocationProviders() == null)
            settings.setInvocationProviders(new AeshInvocationProviders(settings.converterInvocationProvider(),
                    settings.completerInvocationProvider(), settings.validatorInvocationProvider(),
                    settings.optionActivatorProvider(), settings.commandActivatorProvider()));

        return settings;
    }
}
