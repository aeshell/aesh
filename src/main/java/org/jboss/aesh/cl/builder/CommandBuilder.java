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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aesh.cl.builder;

import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.parser.OptionParserException;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedCommandBuilder;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.internal.ProcessedOptionBuilder;
import org.jboss.aesh.cl.parser.AeshCommandLineParser;
import org.jboss.aesh.cl.result.ResultHandler;
import org.jboss.aesh.cl.validator.CommandValidator;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.container.AeshCommandContainer;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.util.ReflectionUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder to create commands during runtime
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandBuilder<C extends Command> {

    private String name;
    private String description;
    private C command;
    private CommandValidator<?> validator;
    private ResultHandler resultHandler;
    private ProcessedOption argument;
    private List<ProcessedOption> options;
    private List<CommandBuilder<? extends Command>> children;
    private CommandLineParserException parserException;

    public CommandBuilder() {
    }

    public CommandBuilder<C> name(String name) {
        this.name = name;
        return this;
    }

    public CommandBuilder<C> description(String description) {
        this.description = description;
        return this;
    }

    public CommandBuilder<C> command(C command) {
        this.command = command;
        return this;
    }

    public CommandBuilder<C> command(Class<C> command) {
        this.command = ReflectionUtil.newInstance(command);
        return this;
    }

    public CommandBuilder<C> validator(CommandValidator<?> commandValidator) {
        this.validator = commandValidator;
        return this;
    }

    public CommandBuilder<C> validator(Class<? extends CommandValidator> commandValidator) {
        this.validator = ReflectionUtil.newInstance(commandValidator);
        return this;
    }

    public CommandBuilder<C> resultHandler(ResultHandler resultHandler) {
        this.resultHandler = resultHandler;
        return this;
    }

    public CommandBuilder<C> resultHandler(Class<? extends ResultHandler> resultHandler) {
        this.resultHandler = ReflectionUtil.newInstance(resultHandler);
        return this;
    }

    public CommandBuilder<C> argument(ProcessedOption argument) {
        this.argument = argument;
        return this;
    }

    public CommandBuilder<C> addOption(ProcessedOption option) {
        if(options == null)
            options = new ArrayList<>();
        options.add(option);
        return this;
    }

    public CommandBuilder<C> addOption(ProcessedOptionBuilder option) {
        if(options == null)
            options = new ArrayList<>();
        try {
            options.add(option.create());
        }
        catch (OptionParserException ope) {
            parserException = ope;
        }
        return this;
    }

    public CommandBuilder<C> addOptions(List<ProcessedOption> options) {
        if(this.options == null)
            this.options = new ArrayList<>();
        this.options.addAll(options);
        return this;
    }

    public CommandBuilder<C> addChild(CommandBuilder<? extends Command> child) {
        if(children == null)
            children = new ArrayList<>();
        this.children.add(child);
        return this;
    }

    public CommandBuilder<C> addChildren(List<CommandBuilder<? extends Command>> children) {
        if(this.children == null)
            this.children = new ArrayList<>();
        this.children.addAll(children);
        return this;
    }

    public CommandContainer<C> generate() {
        try {
            if(parserException != null) {
                return new AeshCommandContainer<>(parserException.getMessage());
            }
            return new AeshCommandContainer<>(generateParser());
        }
        catch (CommandLineParserException e) {
            return new AeshCommandContainer<>(e.getMessage());
        }
    }

    private AeshCommandLineParser<C> generateParser() throws CommandLineParserException {
        if(command == null)
            throw new CommandLineParserException("Command object is null, cannot create command");
        ProcessedCommand<C> processedCommand = generateProcessedCommand();
        AeshCommandLineParser<C> parser = new AeshCommandLineParser<>(processedCommand);
        if(children != null) {
            for(CommandBuilder<? extends Command> builder : children) {
                parser.addChildParser(builder.generateParser());
            }
        }
        return parser;
    }

    private ProcessedCommand<C> generateProcessedCommand() throws CommandLineParserException {
        return new ProcessedCommandBuilder<C>()
                .name(name)
                .command(command)
                .description(description)
                .addOptions(options)
                .resultHandler(resultHandler)
                .validator(validator)
                .argument(argument)
                .create();
    }
}
