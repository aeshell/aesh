/*
 * Copyright 2019 Red Hat, Inc.
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
package org.aesh.util.graal;

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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
@CommandDefinition(name = "graalreflection", description = "Generates a json file to help graal generate a native image")
public class GraalReflectionCommand implements Command {

    @Option(hasValue = false)
    private boolean help;

    @Argument(required = true, description = "Command class name")
    private String command;


    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.getHelpInfo("graalreflection");
        } else {
            Class<Command<CommandInvocation>> clazz = loadCommand(command);
            if (clazz != null) {
                CommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
                try {
                    CommandContainer<CommandInvocation> container = builder.create(clazz);
                    GraalReflectionFileGenerator graalFileGenerator = new GraalReflectionFileGenerator();
                    try (BufferedWriter w = Files.newBufferedWriter(Paths.get(container.getParser().getProcessedCommand().name().toLowerCase() + "_reflection.json"), StandardOpenOption.CREATE)) {
                        graalFileGenerator.generateReflection(container.getParser(), w);
                    }
                    container.getParser().getProcessedCommand();

                } catch (CommandLineParserException | IOException e) {
                    e.printStackTrace();
                }
            } else
                commandInvocation.println("Could not load command: " + command);
        }
        return CommandResult.SUCCESS;
    }

    @SuppressWarnings("unchecked")
    private Class<Command<CommandInvocation>> loadCommand(String commandName) {
        try {
            return (Class<Command<CommandInvocation>>) Class.forName(commandName);
        } catch (ClassNotFoundException | ClassCastException e) {
            e.printStackTrace();
        }

        return null;
    }
}
