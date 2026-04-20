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
package org.aesh.util.completer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.container.CommandContainerBuilder;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.util.completer.ShellCompletionGenerator.ShellType;

/**
 * @author Aesh team
 */
@CommandDefinition(name = "completer", description = "Generates a completion script for a command")
public class CompleterCommand implements Command<CommandInvocation> {

    @Option(hasValue = false)
    private boolean help;

    @Option(name = "shell", shortName = 's', description = "Target shell: BASH, ZSH, or FISH", defaultValue = "BASH")
    private ShellType shell;

    @Argument(required = true, description = "Command class name")
    private String command;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("completer"));
            return CommandResult.SUCCESS;
        }

        Class<Command<CommandInvocation>> clazz = loadCommand(command);
        if (clazz == null) {
            commandInvocation.println("Could not load command: " + command);
            return CommandResult.FAILURE;
        }

        CommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        try {
            CommandContainer<CommandInvocation> container = builder.create(clazz);
            String programName = container.getParser().getProcessedCommand().name().toLowerCase();

            ShellCompletionGenerator generator = ShellCompletionGenerator.forShell(shell);
            String script = generator.generate(container.getParser(), programName);

            String filename = programName + shell.fileExtension();
            Files.write(Paths.get(filename), script.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            commandInvocation.println("Completion script written to: " + filename);

        } catch (CommandLineParserException | IOException e) {
            throw new CommandException("Failed to generate completion script: " + e.getMessage(), e);
        }

        return CommandResult.SUCCESS;
    }

    @SuppressWarnings("unchecked")
    private Class<Command<CommandInvocation>> loadCommand(String commandName) {
        try {
            return (Class<Command<CommandInvocation>>) Class.forName(commandName);
        } catch (ClassNotFoundException | ClassCastException e) {
            // Class not found or wrong type, return null to let caller handle it
        }

        return null;
    }
}
