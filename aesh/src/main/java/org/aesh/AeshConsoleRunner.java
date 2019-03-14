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
package org.aesh;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.readline.Prompt;
import org.aesh.readline.ReadlineConsole;
import org.aesh.terminal.Connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Use the AeshConsoleRunner when you want to easily create an interactive CLI application.
 *
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class AeshConsoleRunner {
    private List<Class<? extends Command>> commands;
    private Settings settings;
    private Prompt prompt;
    private ReadlineConsole console;
    private Connection connection;

    private AeshConsoleRunner() {
        commands = new ArrayList<>();
    }

    public static AeshConsoleRunner builder() {
        return new AeshConsoleRunner();
    }

    public AeshConsoleRunner commands(Class<? extends Command>... commands) {
        if(commands != null)
            this.commands.addAll(Arrays.asList(commands));
        return this;
    }

    public AeshConsoleRunner command(Class<? extends Command> commands) {
        if(commands != null)
            this.commands.add(commands);
        return this;
    }

    public AeshConsoleRunner commands(List<Class<Command>> commands) {
        if(commands != null)
            this.commands.addAll(commands);
        return this;
    }

    public AeshConsoleRunner settings(Settings settings) {
        if(settings != null)
            this.settings = settings;
        return this;
    }

    public AeshConsoleRunner connection(Connection connection) {
        this.connection = connection;
        return this;
    }

    public AeshConsoleRunner prompt(String prompt) {
        if(prompt != null)
            this.prompt = new Prompt(prompt);
        if(console != null && console.running())
            console.setPrompt(this.prompt);
        return this;
    }

    public AeshConsoleRunner prompt(Prompt prompt) {
        if(prompt != null)
            this.prompt = prompt;
        if(console != null && console.running())
            console.setPrompt(this.prompt);
        return this;
    }

    public AeshConsoleRunner addExitCommand() {
        commands.add(ExitCommand.class);
        return this;
    }

    public void start() {
        if(console == null) {
            init();
            if(prompt != null)
                console.setPrompt(prompt);
            try {
                console.start();
            }
            catch (IOException e) {
                throw new RuntimeException("Exception while starting the console: "+e.getMessage());
            }
        }
    }

    public void stop() {
        if(console != null && console.running())
            console.stop();
    }

    @SuppressWarnings("unchecked")
    private void init() {
        if(commands.isEmpty() && (settings == null ||
                                          settings.commandRegistry() == null ||
                                          settings.commandRegistry().getAllCommandNames().isEmpty()))
            throw new RuntimeException("No commands added, nothing to run");

        try {
            if(settings == null) {
                AeshCommandRegistryBuilder registryBuilder = AeshCommandRegistryBuilder.builder();
                for(Class<? extends Command> command : commands)
                    registryBuilder.command(command);
                settings = SettingsBuilder.builder()
                                   .commandRegistry(registryBuilder.create())
                                   .enableAlias(false)
                                   .enableExport(false)
                                   .enableMan(false)
                                   .persistHistory(false)
                                   .connection(connection)
                                   .build();
            }
            //user added its own settings object, but no commands in registry
            else if(!commands.isEmpty() &&
                            (settings.commandRegistry() == null ||
                                     settings.commandRegistry().getAllCommandNames().isEmpty())) {

                AeshCommandRegistryBuilder registryBuilder = AeshCommandRegistryBuilder.builder();
                for(Class<? extends Command> command : commands)
                    registryBuilder.command(command);
                SettingsBuilder settingsBuilder = new SettingsBuilder(settings)
                                                   .commandRegistry(registryBuilder.create());

                if(connection != null)
                    settingsBuilder.connection(connection);

                settings = settingsBuilder.build();
            }

            console = new ReadlineConsole(settings);
        }
        catch (CommandRegistryException e) {
            throw new RuntimeException("Error when adding command: "+e.getMessage());
        }
    }

    @CommandDefinition(name = "exit", description = "exit the program", aliases = {"quit"})
    public static class ExitCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            commandInvocation.stop();
            return CommandResult.SUCCESS;
        }
    }
}
