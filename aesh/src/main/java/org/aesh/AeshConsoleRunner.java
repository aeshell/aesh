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
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.readline.Prompt;
import org.aesh.readline.ReadlineConsole;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class AeshConsoleRunner {
    private List<Class<? extends Command>> commands;
    private Settings settings;
    private CommandRegistry registry;
    private Prompt prompt;
    private ReadlineConsole console;

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
        if(commands.isEmpty())
            throw new RuntimeException("No commands added, nothing to run");

        try {
            AeshCommandRegistryBuilder builder = AeshCommandRegistryBuilder.builder();
            for(Class<? extends Command> command : commands)
                builder.command(command);
            registry = builder.create();
            if(settings == null)
                settings = SettingsBuilder.builder()
                        .commandRegistry(registry)
                        .enableAlias(false)
                        .enableExport(false)
                        .enableMan(false)
                        .build();

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
