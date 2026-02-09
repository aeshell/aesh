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
package org.aesh;

import java.io.IOException;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.ReadlineConsole;
import org.aesh.readline.Prompt;
import org.aesh.terminal.Connection;

/**
 * Use the AeshConsoleRunner when you want to easily create an interactive CLI application.
 *
 * @author Aesh team
 */
public class AeshConsoleRunner {
    private AeshCommandRegistryBuilder<CommandInvocation> registryBuilder;
    private Settings settings;
    private Prompt prompt;
    private ReadlineConsole console;
    private Connection connection;

    private AeshConsoleRunner() {
    }

    public static AeshConsoleRunner builder() {
        return new AeshConsoleRunner();
    }

    public AeshConsoleRunner commands(Class<? extends Command>... commands) {
        if (commands != null) {
            ensureRegistryBuilderInitialized();
            try {
                registryBuilder.commands(commands);
            } catch (CommandRegistryException e) {
                throw new RuntimeException("Error when adding commands: " + e.getMessage(), e);
            }
        }
        return this;
    }

    public AeshConsoleRunner command(Class<? extends Command> command) {
        if (command != null) {
            ensureRegistryBuilderInitialized();
            try {
                registryBuilder.command(command);
            } catch (CommandRegistryException e) {
                throw new RuntimeException("Error when adding command: " + e.getMessage(), e);
            }
        }
        return this;
    }

    public AeshConsoleRunner commandRegistryBuilder(AeshCommandRegistryBuilder<CommandInvocation> commandRegistryBuilder) {
        if (registryBuilder != null) {
            throw new RuntimeException("Cannot set CommandRegistryBuilder after it has been initialized. " +
                    "CommandRegistryBuilder must be set before adding any commands.");
        }
        if (commandRegistryBuilder != null) {
            this.registryBuilder = commandRegistryBuilder;
        }
        return this;
    }

    private void ensureRegistryBuilderInitialized() {
        if (registryBuilder == null) {
            registryBuilder = AeshCommandRegistryBuilder.builder();
        }
    }

    public AeshConsoleRunner settings(Settings settings) {
        if (settings != null)
            this.settings = settings;
        return this;
    }

    public AeshConsoleRunner connection(Connection connection) {
        this.connection = connection;
        return this;
    }

    public AeshConsoleRunner prompt(String prompt) {
        if (prompt != null)
            this.prompt = new Prompt(prompt);
        if (console != null && console.running())
            console.setPrompt(this.prompt);
        return this;
    }

    public AeshConsoleRunner prompt(Prompt prompt) {
        if (prompt != null)
            this.prompt = prompt;
        if (console != null && console.running())
            console.setPrompt(this.prompt);
        return this;
    }

    public AeshConsoleRunner addExitCommand() {
        ensureRegistryBuilderInitialized();
        try {
            registryBuilder.command(ExitCommand.class);
        } catch (CommandRegistryException e) {
            throw new RuntimeException("Error when adding exit command: " + e.getMessage(), e);
        }
        return this;
    }

    public void start() {
        if (console == null) {
            init();
            if (prompt != null)
                console.setPrompt(prompt);
            try {
                console.start();
            } catch (IOException e) {
                throw new RuntimeException("Exception while starting the console: " + e.getMessage());
            }
        }
    }

    public void stop() {
        if (console != null && console.running())
            console.stop();
    }

    @SuppressWarnings("unchecked")
    private void init() {
        // Build the command registry from the builder
        CommandRegistry<CommandInvocation> builtRegistry = null;
        if (registryBuilder != null) {
            try {
                builtRegistry = registryBuilder.create();
            } catch (Exception e) {
                throw new RuntimeException("Error creating command registry: " + e.getMessage(), e);
            }
        }

        // Check if both builder and settings.commandRegistry have commands
        if (builtRegistry != null && !builtRegistry.getAllCommandNames().isEmpty() &&
                settings != null && settings.commandRegistry() != null &&
                !settings.commandRegistry().getAllCommandNames().isEmpty()) {
            throw new RuntimeException(
                    "Cannot define commands in both AeshConsoleRunner (via command() or commandRegistryBuilder()) " +
                            "and Settings.commandRegistry(). Please use only one method to specify commands.");
        }

        // Determine which registry to use
        CommandRegistry<CommandInvocation> finalRegistry = null;
        if (builtRegistry != null && !builtRegistry.getAllCommandNames().isEmpty()) {
            finalRegistry = builtRegistry;
        } else if (settings != null && settings.commandRegistry() != null &&
                !settings.commandRegistry().getAllCommandNames().isEmpty()) {
            finalRegistry = settings.commandRegistry();
        }

        // Validate that we have at least one command
        if (finalRegistry == null || finalRegistry.getAllCommandNames().isEmpty()) {
            throw new RuntimeException("No commands added, nothing to run");
        }

        try {
            if (settings == null) {
                settings = SettingsBuilder.builder()
                        .commandRegistry(finalRegistry)
                        .enableAlias(false)
                        .enableExport(false)
                        .enableMan(false)
                        .persistHistory(false)
                        .connection(connection)
                        .build();
            }
            // User added their own settings object, but we need to add or replace the registry
            else if (settings.commandRegistry() == null ||
                    settings.commandRegistry().getAllCommandNames().isEmpty()) {
                SettingsBuilder settingsBuilder = new SettingsBuilder(settings)
                        .commandRegistry(finalRegistry);

                if (connection != null)
                    settingsBuilder.connection(connection);

                settings = settingsBuilder.build();
            }

            console = new ReadlineConsole(settings);
        } catch (Exception e) {
            throw new RuntimeException("Error when initializing console: " + e.getMessage(), e);
        }
    }

    @CommandDefinition(name = "exit", description = "exit the program", aliases = { "quit" })
    public static class ExitCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            commandInvocation.stop();
            return CommandResult.SUCCESS;
        }
    }
}
