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
package org.aesh.util.completer;

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@CommandDefinition(name = "completer", description = "Generates a complete file for a command for posix systems")
public class CompleterCommand implements Command<CommandInvocation> {

    @Option(hasValue = false)
    private boolean help;

    @Argument(required = true, description = "Command class name")
    private String command;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if(help) {
           commandInvocation.println(commandInvocation.getHelpInfo("completer"));
           return CommandResult.SUCCESS;
        }
        else {
                Class<Command<CommandInvocation>> clazz = loadCommand(command);
                if(clazz != null) {
                    CommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
                    try {
                        CommandContainer<CommandInvocation> container = builder.create(clazz);

                        FileCompleterGenerator completerGenerator = new FileCompleterGenerator();

                        Files.write(Paths.get(container.getParser().getProcessedCommand().name().toLowerCase()+"_complete.bash"),
                                completerGenerator.generateCompleterFile(container.getParser()).getBytes(), StandardOpenOption.CREATE);

                    }
                    catch(CommandLineParserException | IOException e) {
                        e.printStackTrace();
                    }
                }
                else
                    commandInvocation.println("Could not load command: "+command);
        }

        return CommandResult.SUCCESS;
    }

    @SuppressWarnings("unchecked")
    private Class<Command<CommandInvocation>> loadCommand(String commandName) {
        try {
            return (Class<Command<CommandInvocation>>) Class.forName(commandName);
        }
        catch(ClassNotFoundException | ClassCastException e) {
            e.printStackTrace();
        }

        return null;
    }

}
