/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.builder;

import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.exception.OptionParserException;
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
public class CommandBuilder {

    private String name;
    private String description;
    private Command command;
    private CommandValidator<?> validator;
    private ResultHandler resultHandler;
    private ProcessedOption argument;
    private List<ProcessedOption> options;
    private List<CommandBuilder> children;
    private CommandLineParserException parserException;

    public CommandBuilder() {
    }

    public CommandBuilder name(String name) {
        this.name = name;
        return this;
    }

    public CommandBuilder description(String description) {
        this.description = description;
        return this;
    }

    public CommandBuilder command(Command command) {
        this.command = command;
        return this;
    }

    public CommandBuilder command(Class<? extends Command> command) {
        this.command = ReflectionUtil.newInstance(command);
        return this;
    }

    public CommandBuilder validator(CommandValidator<?> commandValidator) {
        this.validator = commandValidator;
        return this;
    }

    public CommandBuilder validator(Class<? extends CommandValidator> commandValidator) {
        this.validator = ReflectionUtil.newInstance(commandValidator);
        return this;
    }

    public CommandBuilder resultHandler(ResultHandler resultHandler) {
        this.resultHandler = resultHandler;
        return this;
    }

    public CommandBuilder resultHandler(Class<? extends ResultHandler> resultHandler) {
        this.resultHandler = ReflectionUtil.newInstance(resultHandler);
        return this;
    }

    public CommandBuilder argument(ProcessedOption argument) {
        this.argument = argument;
        return this;
    }

    public CommandBuilder addOption(ProcessedOption option) {
        if(options == null)
            options = new ArrayList<>();
        options.add(option);
        return this;
    }

    public CommandBuilder addOption(ProcessedOptionBuilder option) {
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

    public CommandBuilder addOptions(List<ProcessedOption> options) {
        if(this.options == null)
            this.options = new ArrayList<>();
        this.options.addAll(options);
        return this;
    }

    public CommandBuilder addChild(CommandBuilder child) {
        if(children == null)
            children = new ArrayList<>();
        this.children.add(child);
        return this;
    }

    public CommandBuilder addChildren(List<CommandBuilder> children) {
        if(this.children == null)
            this.children = new ArrayList<>();
        this.children.addAll(children);
        return this;
    }

    public CommandContainer generate() {
        try {
            if(parserException != null)
                return new AeshCommandContainer(parserException.getMessage());
            else
                return new AeshCommandContainer(generateParser());
        }
        catch (CommandLineParserException e) {
            return new AeshCommandContainer(e.getMessage());
        }
    }

    private AeshCommandLineParser generateParser() throws CommandLineParserException {
        if(command == null)
            throw new CommandLineParserException("Command object is null, cannot create command");
        ProcessedCommand processedCommand = generateProcessedCommand();
        AeshCommandLineParser parser = new AeshCommandLineParser(processedCommand, command);
        if(children != null) {
            for(CommandBuilder builder : children) {
                parser.addChildParser(builder.generateParser());
            }
        }
        return parser;
    }

    private ProcessedCommand generateProcessedCommand() throws CommandLineParserException {
        return new ProcessedCommandBuilder()
                .name(name)
                .description(description)
                .addOptions(options)
                .resultHandler(resultHandler)
                .validator(validator)
                .argument(argument)
                .create();
    }
}
