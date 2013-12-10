/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

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

    public AeshConsoleBuilder() {
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

    public AeshConsoleBuilder manProvider(ManProvider manProvider) {
        this.manProvider = manProvider;
        return this;
    }

    public AeshConsole create() {
        if(settings == null)
            settings = new SettingsBuilder().create();
        if(registry == null) {
            registry = new MutableCommandRegistry();
        }
        if(commandInvocationServices == null)
            commandInvocationServices = new CommandInvocationServices();

        if(completerInvocationProvider == null)
            completerInvocationProvider = new AeshCompleterInvocationProvider();

        if(converterInvocationProvider == null)
            converterInvocationProvider = new AeshConverterInvocationProvider();

        if(validatorInvocationProvider == null)
            validatorInvocationProvider = new AeshValidatorInvocationProvider();

        AeshConsole aeshConsole =
                new AeshConsoleImpl(settings, registry, commandInvocationServices,
                        commandNotFoundHandler, completerInvocationProvider, converterInvocationProvider,
                        validatorInvocationProvider, manProvider);

        if(prompt != null)
            aeshConsole.setPrompt(prompt);

        return aeshConsole;
    }


}
