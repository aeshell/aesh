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
import org.jboss.aesh.readline.Prompt;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshConsoleBuilder {

    private Settings settings;
    private Prompt prompt;
    private List<Command> commands;
    private String execute;

    public AeshConsoleBuilder() {
        commands = new ArrayList<>();
    }

    public AeshConsoleBuilder settings(Settings settings) {
        this.settings = settings;
        return this;
    }

    public AeshConsoleBuilder prompt(Prompt prompt) {
        this.prompt = prompt;
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

        AeshConsoleImpl aeshConsole = new AeshConsoleImpl(settings);

        if(commands.size() > 0 && settings.commandRegistry() instanceof MutableCommandRegistry)
            ((MutableCommandRegistry) settings.commandRegistry()).addAllCommands(commands);

        if(prompt != null)
            aeshConsole.setPrompt(prompt);
        if(execute != null)
            aeshConsole.push(execute);

        return aeshConsole;
    }


}
