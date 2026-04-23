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

package org.aesh.command;

import java.util.EnumSet;

import org.aesh.command.activator.CommandActivatorProvider;
import org.aesh.command.activator.OptionActivatorProvider;
import org.aesh.command.completer.CompleterInvocationProvider;
import org.aesh.command.converter.ConverterInvocationProvider;
import org.aesh.command.impl.AeshCommandRuntime;
import org.aesh.command.impl.invocation.DefaultCommandInvocationBuilder;
import org.aesh.command.impl.registry.MutableCommandRegistryImpl;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationBuilder;
import org.aesh.command.invocation.CommandInvocationProvider;
import org.aesh.command.operator.OperatorType;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.shell.Shell;
import org.aesh.command.validator.ValidatorInvocationProvider;
import org.aesh.console.AeshContext;
import org.aesh.console.DefaultAeshContext;

/**
 *
 * @author Aesh team
 */
public class AeshCommandRuntimeBuilder<CI extends CommandInvocation> {

    public static final EnumSet<OperatorType> ALL_OPERATORS = EnumSet.allOf(OperatorType.class);

    private static final EnumSet<OperatorType> NO_OPERATORS = EnumSet.noneOf(OperatorType.class);

    private CommandRegistry<CI> registry;
    private CommandInvocationProvider<CI> commandInvocationProvider;
    private CommandNotFoundHandler commandNotFoundHandler;
    private CompleterInvocationProvider completerInvocationProvider;
    private ConverterInvocationProvider converterInvocationProvider;
    private ValidatorInvocationProvider validatorInvocationProvider;
    private OptionActivatorProvider optionActivatorProvider;
    private CommandActivatorProvider commandActivatorProvider;
    private AeshContext ctx;
    private CommandInvocationBuilder<CI> commandInvocationBuilder;
    private Shell shell;

    private boolean parseBrackets;
    private EnumSet<OperatorType> operators;

    private AeshCommandRuntimeBuilder() {
    }

    public static <T extends CommandInvocation> AeshCommandRuntimeBuilder<T> builder() {
        return new AeshCommandRuntimeBuilder<>();
    }

    public AeshCommandRuntimeBuilder<CI> parseBrackets(boolean parseBrackets) {
        this.parseBrackets = parseBrackets;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> operators(EnumSet<OperatorType> operators) {
        this.operators = operators;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> commandRegistry(CommandRegistry<CI> registry) {
        this.registry = registry;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> commandInvocationProvider(CommandInvocationProvider<CI> commandInvocationProvider) {
        this.commandInvocationProvider = commandInvocationProvider;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> commandNotFoundHandler(CommandNotFoundHandler commandNotFoundHandler) {
        this.commandNotFoundHandler = commandNotFoundHandler;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> completerInvocationProvider(
            CompleterInvocationProvider completerInvocationProvider) {
        this.completerInvocationProvider = completerInvocationProvider;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> converterInvocationProvider(
            ConverterInvocationProvider converterInvocationProvider) {
        this.converterInvocationProvider = converterInvocationProvider;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> validatorInvocationProvider(
            ValidatorInvocationProvider validatorInvocationProvider) {
        this.validatorInvocationProvider = validatorInvocationProvider;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> optionActivatorProvider(
            OptionActivatorProvider optionActivatorProvider) {
        this.optionActivatorProvider = optionActivatorProvider;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> commandActivatorProvider(
            CommandActivatorProvider commandActivatorProvider) {
        this.commandActivatorProvider = commandActivatorProvider;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> shell(Shell shell) {
        this.shell = shell;
        return this;
    }

    @SuppressWarnings("unchecked")
    public AeshCommandRuntimeBuilder<CI> commandInvocationBuilder(CommandInvocationBuilder commandInvocationBuilder) {
        this.commandInvocationBuilder = commandInvocationBuilder;
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> aeshContext(AeshContext ctx) {
        this.ctx = ctx;
        return this;
    }

    @SuppressWarnings("unchecked")
    public AeshCommandRuntimeBuilder<CI> settings(Settings<? extends CommandInvocation> settings) {
        this.commandInvocationProvider = (CommandInvocationProvider<CI>) settings.commandInvocationProvider();
        this.commandNotFoundHandler = settings.commandNotFoundHandler();
        this.completerInvocationProvider = settings.completerInvocationProvider();
        this.converterInvocationProvider = settings.converterInvocationProvider();
        this.validatorInvocationProvider = settings.validatorInvocationProvider();
        this.optionActivatorProvider = settings.optionActivatorProvider();
        this.commandActivatorProvider = settings.commandActivatorProvider();
        this.registry = (CommandRegistry<CI>) settings.commandRegistry();
        this.ctx = settings.aeshContext();
        this.operators = settings.operatorParserEnabled() ? EnumSet.allOf(OperatorType.class) : null;
        return this;
    }

    @SuppressWarnings("unchecked")
    public CommandRuntime<CI> build() {
        if (registry == null) {
            registry = new MutableCommandRegistryImpl<>();
        }

        if (commandInvocationProvider == null) {
            commandInvocationProvider = new CommandInvocationProvider<CI>() {
            };
        }

        if (commandInvocationBuilder == null)
            commandInvocationBuilder = (CommandInvocationBuilder) new DefaultCommandInvocationBuilder(shell);

        if (ctx == null) {
            ctx = new DefaultAeshContext();
        }

        if (operators == null) {
            operators = NO_OPERATORS;
        }

        return new AeshCommandRuntime<>(ctx, registry, commandInvocationProvider,
                commandNotFoundHandler, completerInvocationProvider, converterInvocationProvider,
                validatorInvocationProvider, optionActivatorProvider, commandActivatorProvider,
                commandInvocationBuilder, parseBrackets, operators);
    }
}
