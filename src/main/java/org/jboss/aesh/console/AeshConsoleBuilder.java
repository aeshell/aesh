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
package org.jboss.aesh.console;

import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.activator.AeshOptionActivatorProvider;
import org.jboss.aesh.console.command.activator.OptionActivatorProvider;
import org.jboss.aesh.console.command.completer.AeshCompleterInvocationProvider;
import org.jboss.aesh.console.command.completer.CompleterInvocationProvider;
import org.jboss.aesh.console.command.converter.AeshConverterInvocationProvider;
import org.jboss.aesh.console.command.converter.ConverterInvocationProvider;
import org.jboss.aesh.console.command.invocation.CommandInvocationServices;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.command.registry.MutableCommandRegistry;
import org.jboss.aesh.console.command.validator.AeshValidatorInvocationProvider;
import org.jboss.aesh.console.command.validator.ValidatorInvocationProvider;
import org.jboss.aesh.console.helper.ManProvider;
import org.jboss.aesh.console.settings.CommandNotFoundHandler;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;

import java.util.ArrayList;
import java.util.List;
import org.jboss.aesh.console.command.activator.AeshCommandActivatorProvider;
import org.jboss.aesh.console.command.activator.CommandActivatorProvider;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshConsoleBuilder {

    private Settings settings;
    private Prompt prompt;
    private CommandRegistry registry;
    private CommandInvocationServices commandInvocationServices;
    private CommandNotFoundHandler commandNotFoundHandler;
    private ManProvider manProvider;
    private CompleterInvocationProvider completerInvocationProvider;
    private ConverterInvocationProvider converterInvocationProvider;
    private ValidatorInvocationProvider validatorInvocationProvider;
    private OptionActivatorProvider optionActivatorProvider;
    private CommandActivatorProvider commandActivatorProvider;
    private List<Command> commands;
    private String execute;

    public AeshConsoleBuilder() {
        commands = new ArrayList<>();
    }

    public AeshConsoleBuilder commandRegistry(CommandRegistry registry) {
        this.registry = registry;
        return this;
    }

    public AeshConsoleBuilder settings(Settings settings) {
        this.settings = settings;
        return this;
    }

    public AeshConsoleBuilder prompt(Prompt prompt) {
        this.prompt = prompt;
        return this;
    }

    public AeshConsoleBuilder commandInvocationProvider(CommandInvocationServices commandInvocationServices) {
        this.commandInvocationServices = commandInvocationServices;
        return this;
    }

    public AeshConsoleBuilder commandNotFoundHandler(CommandNotFoundHandler commandNotFoundHandler) {
        this.commandNotFoundHandler = commandNotFoundHandler;
        return this;
    }

    public AeshConsoleBuilder completerInvocationProvider(CompleterInvocationProvider completerInvocationProvider) {
        this.completerInvocationProvider = completerInvocationProvider;
        return this;
    }

    public AeshConsoleBuilder converterInvocationProvider(ConverterInvocationProvider converterInvocationProvider) {
        this.converterInvocationProvider = converterInvocationProvider;
        return this;
    }

    public AeshConsoleBuilder validatorInvocationProvider(ValidatorInvocationProvider validatorInvocationProvider) {
        this.validatorInvocationProvider = validatorInvocationProvider;
        return this;
    }

    public AeshConsoleBuilder optionActivatorProvider(OptionActivatorProvider optionActivatorProvider) {
        this.optionActivatorProvider = optionActivatorProvider;
        return this;
    }

    public AeshConsoleBuilder commandActivatorProvider(CommandActivatorProvider commandActivatorProvider) {
        this.commandActivatorProvider = commandActivatorProvider;
        return this;
    }

    public AeshConsoleBuilder manProvider(ManProvider manProvider) {
        this.manProvider = manProvider;
        return this;
    }

    public AeshConsoleBuilder addCommand(Command command) {
        commands.add(command);
        return this;
    }

    public AeshConsoleBuilder executeAtStart(String execute) {
        this.execute = execute;
        return this;
    }

    public AeshConsole create() {
        if(settings == null)
            settings = new SettingsBuilder().create();
        if(registry == null) {
            registry = new MutableCommandRegistry();
        }

        if(commands.size() > 0 && registry instanceof MutableCommandRegistry)
            ((MutableCommandRegistry) registry).addAllCommands(commands);

        if(commandInvocationServices == null)
            commandInvocationServices = new CommandInvocationServices();

        if(completerInvocationProvider == null)
            completerInvocationProvider = new AeshCompleterInvocationProvider();

        if(converterInvocationProvider == null)
            converterInvocationProvider = new AeshConverterInvocationProvider();

        if(validatorInvocationProvider == null)
            validatorInvocationProvider = new AeshValidatorInvocationProvider();

        if(optionActivatorProvider == null)
            optionActivatorProvider = new AeshOptionActivatorProvider();

        if(commandActivatorProvider == null)
            commandActivatorProvider = new AeshCommandActivatorProvider();

        AeshConsoleImpl aeshConsole =
                new AeshConsoleImpl(settings, registry, commandInvocationServices,
                        commandNotFoundHandler, completerInvocationProvider, converterInvocationProvider,
                        validatorInvocationProvider, optionActivatorProvider, manProvider, commandActivatorProvider);

        if(prompt != null)
            aeshConsole.setPrompt(prompt);
        if(execute != null)
            aeshConsole.execute(execute);

        return aeshConsole;
    }


}
