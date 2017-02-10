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
package org.aesh.command.impl;

import org.aesh.command.AeshCommandRuntime;
import org.aesh.command.Command;
import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.console.AeshContext;
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
import org.aesh.command.impl.registry.MutableCommandRegistry;
import org.aesh.command.impl.validator.AeshValidatorInvocationProvider;
import org.aesh.command.validator.ValidatorInvocationProvider;
import org.aesh.console.settings.CommandNotFoundHandler;
import org.aesh.console.settings.DefaultAeshContext;
import org.aesh.console.settings.Settings;

import java.util.function.Consumer;

/**
 *
 * @author jdenise@redhat.com
 */
public class AeshCommandRuntimeBuilder {

    private CommandRegistry<? extends Command> registry;
    private CommandInvocationProvider<? extends CommandInvocation> commandInvocationProvider;
    private CommandNotFoundHandler commandNotFoundHandler;
    private CompleterInvocationProvider<? extends CompleterInvocation> completerInvocationProvider;
    private ConverterInvocationProvider<? extends ConverterInvocation> converterInvocationProvider;
    private ValidatorInvocationProvider<? extends ValidatorInvocation> validatorInvocationProvider;
    private OptionActivatorProvider<? extends OptionActivator> optionActivatorProvider;
    private CommandActivatorProvider<? extends CommandActivator> commandActivatorProvider;
    private AeshContext ctx;

    private AeshCommandRuntimeBuilder() {
    }

    public static AeshCommandRuntimeBuilder builder() {
        return new AeshCommandRuntimeBuilder();
    }

    public AeshCommandRuntimeBuilder commandRegistry(CommandRegistry<? extends Command> registry) {
        this.registry = registry;
        return this;
    }

    private AeshCommandRuntimeBuilder apply(Consumer<AeshCommandRuntimeBuilder> consumer) {
        consumer.accept(this);
        return this;
    }

    public AeshCommandRuntimeBuilder commandInvocationProvider(CommandInvocationProvider<? extends CommandInvocation> commandInvocationProvider) {
        return apply(c -> c.commandInvocationProvider = commandInvocationProvider);
    }

    public AeshCommandRuntimeBuilder commandNotFoundHandler(CommandNotFoundHandler commandNotFoundHandler) {
        return apply(c -> c.commandNotFoundHandler = commandNotFoundHandler);
    }

    public AeshCommandRuntimeBuilder completerInvocationProvider(CompleterInvocationProvider<? extends CompleterInvocation> completerInvocationProvider) {
        return apply(c -> c.completerInvocationProvider = completerInvocationProvider);
    }

    public AeshCommandRuntimeBuilder converterInvocationProvider(ConverterInvocationProvider<? extends ConverterInvocation> converterInvocationProvider) {
        return apply(c -> c.converterInvocationProvider = converterInvocationProvider);
    }

    public AeshCommandRuntimeBuilder validatorInvocationProvider(ValidatorInvocationProvider<? extends ValidatorInvocation> validatorInvocationProvider) {
        return apply(c -> c.validatorInvocationProvider = validatorInvocationProvider);
    }

    public AeshCommandRuntimeBuilder optionActivatorProvider(OptionActivatorProvider<? extends OptionActivator> optionActivatorProvider) {
        return apply(c -> c.optionActivatorProvider = optionActivatorProvider);
    }

    public AeshCommandRuntimeBuilder commandActivatorProvider(CommandActivatorProvider<? extends CommandActivator> commandActivatorProvider) {
        return apply(c -> c.commandActivatorProvider = commandActivatorProvider);
    }

    public AeshCommandRuntimeBuilder aeshContext(AeshContext ctx) {
        return apply(c -> c.ctx = ctx);
    }

    public AeshCommandRuntimeBuilder settings(Settings<? extends Command, ? extends CommandInvocation,
            ? extends ConverterInvocation, ? extends CompleterInvocation, ? extends ValidatorInvocation,
            ? extends OptionActivator, ? extends CommandActivator> settings) {
        return apply(c -> {
            c.commandInvocationProvider = settings.commandInvocationProvider();
            c.commandNotFoundHandler = settings.commandNotFoundHandler();
            c.completerInvocationProvider = settings.completerInvocationProvider();
            c.converterInvocationProvider = settings.converterInvocationProvider();
            c.validatorInvocationProvider = settings.validatorInvocationProvider();
            c.optionActivatorProvider = settings.optionActivatorProvider();
            c.commandActivatorProvider = settings.commandActivatorProvider();
            c.ctx = settings.aeshContext();
        });
    }

    public AeshCommandRuntime build() {
        if (registry == null) {
            registry = new MutableCommandRegistry<>();
        }

        if (commandInvocationProvider == null) {
            commandInvocationProvider = new AeshCommandInvocationProvider();
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

        if (ctx == null) {
            ctx = new DefaultAeshContext();
        }

        return new AeshCommandRuntimeImpl<>(ctx, registry, commandInvocationProvider,
                        commandNotFoundHandler, completerInvocationProvider, converterInvocationProvider,
                        validatorInvocationProvider, optionActivatorProvider, commandActivatorProvider);
    }
}
