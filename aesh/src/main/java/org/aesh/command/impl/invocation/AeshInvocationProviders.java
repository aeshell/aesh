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

package org.aesh.command.impl.invocation;

import org.aesh.command.activator.CommandActivatorProvider;
import org.aesh.command.activator.OptionActivatorProvider;
import org.aesh.command.completer.CompleterInvocationProvider;
import org.aesh.command.converter.ConverterInvocationProvider;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.settings.Settings;
import org.aesh.command.validator.ValidatorInvocationProvider;

/**
 * @author Aesh team
 */
public class AeshInvocationProviders implements InvocationProviders {

    private static final ConverterInvocationProvider DEFAULT_CONVERTER = new ConverterInvocationProvider() {
    };
    private static final CompleterInvocationProvider DEFAULT_COMPLETER = new CompleterInvocationProvider() {
    };
    private static final ValidatorInvocationProvider DEFAULT_VALIDATOR = new ValidatorInvocationProvider() {
    };
    private static final OptionActivatorProvider DEFAULT_OPTION_ACTIVATOR = new OptionActivatorProvider() {
    };
    private static final CommandActivatorProvider DEFAULT_COMMAND_ACTIVATOR = new CommandActivatorProvider() {
    };

    private final ConverterInvocationProvider converterInvocationProvider;
    private final CompleterInvocationProvider completerInvocationProvider;
    private final ValidatorInvocationProvider validatorInvocationProvider;
    private final OptionActivatorProvider optionActivatorProvider;
    private final CommandActivatorProvider commandActivatorProvider;

    public AeshInvocationProviders() {
        this(null, null, null, null, null);
    }

    public AeshInvocationProviders(Settings settings) {
        this.converterInvocationProvider = defaultIfNull(settings.converterInvocationProvider(), DEFAULT_CONVERTER);
        this.completerInvocationProvider = defaultIfNull(settings.completerInvocationProvider(), DEFAULT_COMPLETER);
        this.validatorInvocationProvider = defaultIfNull(settings.validatorInvocationProvider(), DEFAULT_VALIDATOR);
        this.optionActivatorProvider = defaultIfNull(settings.optionActivatorProvider(), DEFAULT_OPTION_ACTIVATOR);
        this.commandActivatorProvider = defaultIfNull(settings.commandActivatorProvider(), DEFAULT_COMMAND_ACTIVATOR);
    }

    public AeshInvocationProviders(ConverterInvocationProvider converterInvocationProvider,
            CompleterInvocationProvider completerInvocationProvider,
            ValidatorInvocationProvider validatorInvocationProvider,
            OptionActivatorProvider optionActivatorProvider,
            CommandActivatorProvider commandActivatorProvider) {
        this.converterInvocationProvider = defaultIfNull(converterInvocationProvider, DEFAULT_CONVERTER);
        this.completerInvocationProvider = defaultIfNull(completerInvocationProvider, DEFAULT_COMPLETER);
        this.validatorInvocationProvider = defaultIfNull(validatorInvocationProvider, DEFAULT_VALIDATOR);
        this.optionActivatorProvider = defaultIfNull(optionActivatorProvider, DEFAULT_OPTION_ACTIVATOR);
        this.commandActivatorProvider = defaultIfNull(commandActivatorProvider, DEFAULT_COMMAND_ACTIVATOR);
    }

    private static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    @Override
    public ConverterInvocationProvider getConverterProvider() {
        return converterInvocationProvider;
    }

    @Override
    public CommandActivatorProvider getCommandActivatorProvider() {
        return commandActivatorProvider;
    }

    @Override
    public CompleterInvocationProvider getCompleterProvider() {
        return completerInvocationProvider;
    }

    @Override
    public ValidatorInvocationProvider getValidatorProvider() {
        return validatorInvocationProvider;
    }

    @Override
    public OptionActivatorProvider getOptionActivatorProvider() {
        return optionActivatorProvider;
    }
}
