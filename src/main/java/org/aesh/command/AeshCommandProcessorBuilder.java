/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
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

/**
 *
 * @author jdenise@redhat.com
 */
public class AeshCommandProcessorBuilder {

    private CommandRegistry registry;
    private CommandInvocationProvider commandInvocationProvider;
    private CommandNotFoundHandler commandNotFoundHandler;
    private CompleterInvocationProvider completerInvocationProvider;
    private ConverterInvocationProvider converterInvocationProvider;
    private ValidatorInvocationProvider validatorInvocationProvider;
    private OptionActivatorProvider optionActivatorProvider;
    private CommandActivatorProvider commandActivatorProvider;
    private AeshContext ctx;

    public AeshCommandProcessorBuilder() {
    }

    public AeshCommandProcessorBuilder commandRegistry(CommandRegistry registry) {
        this.registry = registry;
        return this;
    }

    public AeshCommandProcessorBuilder commandInvocationProvider(CommandInvocationProvider commandInvocationProvider) {
        this.commandInvocationProvider = commandInvocationProvider;
        return this;
    }

    public AeshCommandProcessorBuilder commandNotFoundHandler(CommandNotFoundHandler commandNotFoundHandler) {
        this.commandNotFoundHandler = commandNotFoundHandler;
        return this;
    }

    public AeshCommandProcessorBuilder completerInvocationProvider(CompleterInvocationProvider completerInvocationProvider) {
        this.completerInvocationProvider = completerInvocationProvider;
        return this;
    }

    public AeshCommandProcessorBuilder converterInvocationProvider(ConverterInvocationProvider converterInvocationProvider) {
        this.converterInvocationProvider = converterInvocationProvider;
        return this;
    }

    public AeshCommandProcessorBuilder validatorInvocationProvider(ValidatorInvocationProvider validatorInvocationProvider) {
        this.validatorInvocationProvider = validatorInvocationProvider;
        return this;
    }

    public AeshCommandProcessorBuilder optionActivatorProvider(OptionActivatorProvider optionActivatorProvider) {
        this.optionActivatorProvider = optionActivatorProvider;
        return this;
    }

    public AeshCommandProcessorBuilder commandActivatorProvider(CommandActivatorProvider commandActivatorProvider) {
        this.commandActivatorProvider = commandActivatorProvider;
        return this;
    }

    public AeshCommandProcessorBuilder aeshContext(AeshContext ctx) {
        this.ctx = ctx;
        return this;
    }

    public AeshCommandProcessorImpl build() {
        if (registry == null) {
            registry = new MutableCommandRegistry();
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

        AeshCommandProcessorImpl processor
                = new AeshCommandProcessorImpl(ctx, registry, commandInvocationProvider,
                        commandNotFoundHandler, completerInvocationProvider, converterInvocationProvider,
                        validatorInvocationProvider, optionActivatorProvider, commandActivatorProvider);

        return processor;
    }
}
