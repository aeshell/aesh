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
package org.aesh.command.builder;

import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.parser.OptionParserException;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.command.validator.CommandValidator;
import org.aesh.command.Command;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.AeshCommandLineParser;
import org.aesh.command.result.ResultHandler;
import org.aesh.command.impl.container.AeshCommandContainer;
import org.aesh.command.container.CommandContainer;
import org.aesh.util.ReflectionUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builder to build commands during runtime
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 * @author <a href="mailto:danielsoro@gmail.com">Daniel Cunha (soro)</a>
 */
public class CommandBuilder<C extends Command<CommandInvocation>> {

    private String name;
    private String description;
    private boolean generateHelp;
    private boolean disableParsing;
    private String version;
    private C command;
    private CommandValidator<C, CommandInvocation> validator;
    private ResultHandler resultHandler;
    private ProcessedOption argument;
    private List<ProcessedOption> options;
    private List<CommandBuilder> children;
    private CommandLineParserException parserException;
    private CommandPopulator<Object, CommandInvocation> populator;
    private List<String> aliases;

    private CommandBuilder() {
    }

    public static <T extends Command<CommandInvocation>> CommandBuilder<T> builder() {
        return new CommandBuilder<>();
    }

    public CommandBuilder<C> name(String name) {
        this.name = name;
        return this;
    }

    public CommandBuilder<C> aliases(List<String> aliases) {
        this.aliases = aliases == null ? Collections.emptyList()
                : Collections.unmodifiableList(aliases);
        return this;
    }

    public CommandBuilder<C> description(String description) {
        this.description = description;
        return this;
    }

    public CommandBuilder<C> version(String version) {
        this.version = version;
        return this;
    }

    public CommandBuilder<C> generateHelp(boolean help) {
        this.generateHelp = help;
        return this;
    }

    public CommandBuilder<C> disableParsing(boolean disableParsing) {
        this.disableParsing = disableParsing;
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

    public CommandBuilder<C> validator(CommandValidator<C, CommandInvocation> commandValidator) {
        this.validator = commandValidator;
        return this;
    }

    public CommandBuilder<C> validator(Class<CommandValidator<C, CommandInvocation>> commandValidator) {
        this.validator = ReflectionUtil.newInstance(commandValidator);
        return this;
    }

    public CommandBuilder<C> populator(CommandPopulator<Object, CommandInvocation> populator) {
        this.populator = populator;
        return this;
    }

    public CommandBuilder<C> resultHandler(ResultHandler resultHandler) {
        this.resultHandler = resultHandler;
        return this;
    }

    public CommandBuilder<C> resultHandler(Class<ResultHandler> resultHandler) {
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
            options.add(option.build());
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

    public CommandBuilder<C> addChild(CommandBuilder child) {
        if(children == null)
            children = new ArrayList<>();
        this.children.add(child);
        return this;
    }

    public CommandBuilder<C> addChildren(List<CommandBuilder> children) {
        if(this.children == null)
            this.children = new ArrayList<>();
        this.children.addAll(children);
        return this;
    }

    public CommandContainer<CommandInvocation> create() {
        try {
            if(parserException != null) {
                return new AeshCommandContainer<>(parserException.getMessage());
            }
            return new AeshCommandContainer<>(createParser());
        }
        catch (CommandLineParserException e) {
            return new AeshCommandContainer<>(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private AeshCommandLineParser<CommandInvocation> createParser() throws CommandLineParserException {
        if(command == null)
            throw new CommandLineParserException("Command object is null, cannot build command");
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> processedCommand = createProcessedCommand();
        AeshCommandLineParser<CommandInvocation> parser = new AeshCommandLineParser<>(processedCommand);
        if(children != null) {
            for(CommandBuilder builder : children) {
                parser.addChildParser(builder.createParser());
            }
        }
        return parser;
    }

    @SuppressWarnings("unchecked")
    private ProcessedCommand<Command<CommandInvocation>, CommandInvocation> createProcessedCommand() throws CommandLineParserException {
        return ProcessedCommandBuilder.builder()
                .name(name)
                .aliases(aliases)
                .command(command)
                .description(description)
                .generateHelp(generateHelp)
                .disableParsing(disableParsing)
                .version(version)
                .addOptions(options)
                .resultHandler(resultHandler)
                .validator((CommandValidator<Command<CommandInvocation>, CommandInvocation>) validator)
                .arguments(argument)
                .populator(populator)
                .create();
    }
}
