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

package org.aesh.command;

import java.util.EnumSet;
import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.impl.AeshCommandRuntime;
import org.aesh.command.impl.invocation.DefaultCommandInvocationBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationBuilder;
import org.aesh.command.shell.Shell;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.readline.AeshContext;
import org.aesh.command.impl.activator.AeshCommandActivatorProvider;
import org.aesh.command.impl.activator.AeshOptionActivatorProvider;
import org.aesh.command.activator.CommandActivatorProvider;
import org.aesh.command.activator.OptionActivatorProvider;
import org.aesh.command.impl.completer.AeshCompleterInvocationProvider;
import org.aesh.command.completer.CompleterInvocationProvider;
import org.aesh.command.impl.converter.AeshConverterInvocationProvider;
import org.aesh.command.converter.ConverterInvocationProvider;
import org.aesh.command.impl.invocation.AeshCommandInvocationProvider;
import org.aesh.command.invocation.CommandInvocationProvider;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.impl.registry.MutableCommandRegistryImpl;
import org.aesh.command.impl.validator.AeshValidatorInvocationProvider;
import org.aesh.command.validator.ValidatorInvocationProvider;
import org.aesh.readline.DefaultAeshContext;
import org.aesh.command.settings.Settings;

import java.util.function.Consumer;
import org.aesh.command.operator.OperatorType;

/**
 *
 * @author jdenise@redhat.com
 */
public class AeshCommandRuntimeBuilder<CI extends CommandInvocation> {

    public static final EnumSet<OperatorType> ALL_OPERATORS = EnumSet.allOf(OperatorType.class);

    private static final EnumSet<OperatorType> NO_OPERATORS = EnumSet.noneOf(OperatorType.class);

    private CommandRegistry<CI> registry;
    private CommandInvocationProvider<CI> commandInvocationProvider;
    private CommandNotFoundHandler commandNotFoundHandler;
    private CompleterInvocationProvider<? extends CompleterInvocation> completerInvocationProvider;
    private ConverterInvocationProvider<? extends ConverterInvocation> converterInvocationProvider;
    private ValidatorInvocationProvider<? extends ValidatorInvocation> validatorInvocationProvider;
    private OptionActivatorProvider<? extends OptionActivator> optionActivatorProvider;
    private CommandActivatorProvider<? extends CommandActivator> commandActivatorProvider;
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

    private AeshCommandRuntimeBuilder<CI> apply(Consumer<AeshCommandRuntimeBuilder<CI>> consumer) {
        consumer.accept(this);
        return this;
    }

    public AeshCommandRuntimeBuilder<CI> commandInvocationProvider(CommandInvocationProvider<CI> commandInvocationProvider) {
        return apply(c -> c.commandInvocationProvider = commandInvocationProvider);
    }

    public AeshCommandRuntimeBuilder<CI> commandNotFoundHandler(CommandNotFoundHandler commandNotFoundHandler) {
        return apply(c -> c.commandNotFoundHandler = commandNotFoundHandler);
    }

    public AeshCommandRuntimeBuilder<CI> completerInvocationProvider(CompleterInvocationProvider<? extends CompleterInvocation> completerInvocationProvider) {
        return apply(c -> c.completerInvocationProvider = completerInvocationProvider);
    }

    public AeshCommandRuntimeBuilder<CI> converterInvocationProvider(ConverterInvocationProvider<? extends ConverterInvocation> converterInvocationProvider) {
        return apply(c -> c.converterInvocationProvider = converterInvocationProvider);
    }

    public AeshCommandRuntimeBuilder<CI> validatorInvocationProvider(ValidatorInvocationProvider<? extends ValidatorInvocation> validatorInvocationProvider) {
        return apply(c -> c.validatorInvocationProvider = validatorInvocationProvider);
    }

    public AeshCommandRuntimeBuilder<CI> optionActivatorProvider(OptionActivatorProvider<? extends OptionActivator> optionActivatorProvider) {
        return apply(c -> c.optionActivatorProvider = optionActivatorProvider);
    }

    public AeshCommandRuntimeBuilder<CI> commandActivatorProvider(CommandActivatorProvider<? extends CommandActivator> commandActivatorProvider) {
        return apply(c -> c.commandActivatorProvider = commandActivatorProvider);
    }

    public AeshCommandRuntimeBuilder<CI> shell(Shell shell) {
        return apply(c -> c.shell = shell);
    }

    @SuppressWarnings("unchecked")
    public AeshCommandRuntimeBuilder<CI> commandInvocationBuilder(CommandInvocationBuilder commandInvocationBuilder) {
        return apply(c -> c.commandInvocationBuilder = commandInvocationBuilder);
    }

    public AeshCommandRuntimeBuilder<CI> aeshContext(AeshContext ctx) {
        return apply(c -> c.ctx = ctx);
    }

    @SuppressWarnings("unchecked")
    public AeshCommandRuntimeBuilder<CI> settings(Settings<? extends CommandInvocation,
            ? extends ConverterInvocation, ? extends CompleterInvocation, ? extends ValidatorInvocation,
            ? extends OptionActivator, ? extends CommandActivator> settings) {
        return apply(c -> {
            c.commandInvocationProvider = (CommandInvocationProvider<CI>) settings.commandInvocationProvider();
            c.commandNotFoundHandler = settings.commandNotFoundHandler();
            c.completerInvocationProvider = settings.completerInvocationProvider();
            c.converterInvocationProvider = settings.converterInvocationProvider();
            c.validatorInvocationProvider = settings.validatorInvocationProvider();
            c.optionActivatorProvider = settings.optionActivatorProvider();
            c.commandActivatorProvider = settings.commandActivatorProvider();
            c.registry = (CommandRegistry<CI>) settings.commandRegistry();
            c.ctx = settings.aeshContext();
            c.operators = settings.operatorParserEnabled() ? EnumSet.allOf(OperatorType.class) : null;
        });
    }

    @SuppressWarnings("unchecked")
    public CommandRuntime<CI> build() {
        if (registry == null) {
            registry = new MutableCommandRegistryImpl<>();
        }

        if (commandInvocationProvider == null) {
            commandInvocationProvider = new AeshCommandInvocationProvider<>();
        }

        if (completerInvocationProvider == null) {
            completerInvocationProvider = new AeshCompleterInvocationProvider();
        }

        if (converterInvocationProvider == null) {
            converterInvocationProvider = new AeshConverterInvocationProvider();
        }

        if (validatorInvocationProvider == null) {
            validatorInvocationProvider = new AeshValidatorInvocationProvider();
        }

        if (optionActivatorProvider == null) {
            optionActivatorProvider = new AeshOptionActivatorProvider();
        }

        if (commandActivatorProvider == null) {
            commandActivatorProvider = new AeshCommandActivatorProvider();
        }

        if(commandInvocationBuilder == null)
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
