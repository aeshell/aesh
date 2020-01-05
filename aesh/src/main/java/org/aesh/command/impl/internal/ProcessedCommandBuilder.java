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
package org.aesh.command.impl.internal;

import org.aesh.command.impl.activator.NullCommandActivator;
import org.aesh.command.impl.validator.NullCommandValidator;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.command.impl.result.NullResultHandler;
import org.aesh.command.result.ResultHandler;
import org.aesh.command.validator.CommandValidator;
import org.aesh.command.Command;
import org.aesh.util.ReflectionUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.aesh.command.activator.CommandActivator;

/**
 * Build a {@link ProcessedCommand} object using the Builder pattern.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 * @author <a href="mailto:danielsoro@gmail.com">Daniel Cunha (soro)</a>
 */
public class ProcessedCommandBuilder<C extends Command<CI>, CI extends CommandInvocation> {

    private String name;
    private String description;
    private CommandValidator<C,CI> validator;
    private ResultHandler resultHandler;
    private ProcessedOption arguments;
    private ProcessedOption arg;
    private final List<ProcessedOption> options;
    private CommandPopulator<Object, CI> populator;
    private C command;
    private List<String> aliases;
    private CommandActivator activator;
    private boolean generateHelp;
    private boolean disableParsing;
    private String version;

    private ProcessedCommandBuilder() {
        options = new ArrayList<>();
    }

    public static <T extends Command<I>, I extends CommandInvocation> ProcessedCommandBuilder<T,I> builder() {
        return new ProcessedCommandBuilder<>();
    }

    public ProcessedCommandBuilder<C,CI> name(String name) {
        this.name = name;
        return this;
    }

    public ProcessedCommandBuilder<C,CI> aliases(List<String> aliases) {
        this.aliases = aliases == null ? Collections.emptyList()
                : Collections.unmodifiableList(aliases);
        return this;
    }

    public ProcessedCommandBuilder<C,CI> description(String usage) {
        this.description = usage;
        return this;
    }

    public ProcessedCommandBuilder<C,CI> version(String version) {
        this.version = version;
        return this;
    }

    public ProcessedCommandBuilder<C,CI> generateHelp(boolean help) {
        this.generateHelp = help;
        return this;
    }

    public ProcessedCommandBuilder<C,CI> disableParsing(boolean disableParsing) {
        this.disableParsing = disableParsing;
        return this;
    }

    public ProcessedCommandBuilder<C,CI> arguments(ProcessedOption arguments) {
        this.arguments = arguments;
        return this;
    }

    public ProcessedCommandBuilder<C,CI> argument(ProcessedOption argument) {
        this.arg = argument;
        return this;
    }

    public ProcessedCommandBuilder<C,CI> validator(CommandValidator<C,CI> validator) {
        this.validator = validator;
        return this;
    }

    public ProcessedCommandBuilder<C,CI> validator(Class<? extends CommandValidator<C,CI>> validator) {
        this.validator = initValidator(validator);
        return this;
    }

    @SuppressWarnings("unchecked")
    private CommandValidator<C,CI> initValidator(Class<? extends CommandValidator<C,CI>> validator) {
        if(validator != null && !validator.equals(NullCommandValidator.class))
            return ReflectionUtil.newInstance(validator);
        else
            return (CommandValidator<C, CI>) new NullCommandValidator();
    }

    public ProcessedCommandBuilder<C,CI> resultHandler(Class<? extends ResultHandler> resultHandler) {
        this.resultHandler = initResultHandler(resultHandler);
        return this;
    }

    private ResultHandler initResultHandler(Class<? extends ResultHandler> resultHandler) {
        if(resultHandler != null && !resultHandler.equals(NullResultHandler.class))
            return ReflectionUtil.newInstance(resultHandler);
        else
            return new NullResultHandler();
    }

    public ProcessedCommandBuilder<C,CI> resultHandler(ResultHandler resultHandler) {
        this.resultHandler = resultHandler;
        return this;
    }

    public ProcessedCommandBuilder<C,CI> populator(CommandPopulator<Object, CI> populator) {
        this.populator = populator;
        return this;
    }

    public ProcessedCommandBuilder<C,CI> activator(CommandActivator activator) {
        this.activator = activator;
        return this;
    }

    public ProcessedCommandBuilder<C,CI> activator(Class<? extends CommandActivator> activator) {
        this.activator = initActivator(activator);
        return this;
    }

    private CommandActivator initActivator(Class<? extends CommandActivator> activator) {
        if(activator != null && activator != NullCommandActivator.class)
            return ReflectionUtil.newInstance(activator);
        else
            return new NullCommandActivator();
    }

    public ProcessedCommandBuilder<C,CI> command(C command) {
        this.command = command;
        return this;
    }

    @SuppressWarnings("unchecked")
    public ProcessedCommandBuilder<C,CI> command(Class command) {
        this.command = (C) ReflectionUtil.newInstance(command);
        return this;
    }

    public ProcessedCommandBuilder<C,CI> addOption(ProcessedOption option) {
        this.options.add(option);
        return this;
    }

    public ProcessedCommandBuilder<C,CI> addOptions(List<ProcessedOption> options) {
        if(options != null)
            this.options.addAll(options);
        return this;
    }

    @SuppressWarnings("unchecked")
    public ProcessedCommand<C,CI> create() throws CommandLineParserException {
        if(name == null || name.length() < 1)
            throw new CommandLineParserException("The parameter name must be defined");

        if(validator == null)
            validator = (CommandValidator<C, CI>) new NullCommandValidator();

        if(resultHandler == null)
            resultHandler = new NullResultHandler();

        return new ProcessedCommand<>(name, aliases, command, description, validator,
                resultHandler, generateHelp, disableParsing, version, arguments, options, arg, populator, activator);
    }
}
