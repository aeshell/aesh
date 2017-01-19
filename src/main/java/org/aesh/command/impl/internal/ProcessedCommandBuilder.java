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
import org.aesh.command.impl.parser.CommandLineParserException;
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
public class ProcessedCommandBuilder<C extends Command> {

    private String name;
    private String description;
    private CommandValidator<C> validator;
    private ResultHandler resultHandler;
    private ProcessedOption argument;
    private final List<ProcessedOption> options;
    private CommandPopulator<Object,C> populator;
    private C command;
    private List<String> aliases;
    private CommandActivator activator;

    public ProcessedCommandBuilder() {
        options = new ArrayList<>();
    }

    public ProcessedCommandBuilder<C> name(String name) {
        this.name = name;
        return this;
    }

    public ProcessedCommandBuilder<C> aliases(List<String> aliases) {
        this.aliases = aliases == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(aliases);
        return this;
    }

    public ProcessedCommandBuilder<C> description(String usage) {
        this.description = usage;
        return this;
    }

    public ProcessedCommandBuilder<C> argument(ProcessedOption argument) {
        this.argument = argument;
        return this;
    }

    public ProcessedCommandBuilder<C> validator(CommandValidator<C> validator) {
        this.validator = validator;
        return this;
    }

    public ProcessedCommandBuilder<C> validator(Class<? extends CommandValidator<C>> validator) {
        this.validator = initValidator(validator);
        return this;
    }

    private CommandValidator<C> initValidator(Class<? extends CommandValidator<C>> validator) {
        if(validator != null && !validator.equals(NullCommandValidator.class))
            return ReflectionUtil.newInstance(validator);
        else
            return new NullCommandValidator<>();
    }

    public ProcessedCommandBuilder<C> resultHandler(Class<? extends ResultHandler> resultHandler) {
        this.resultHandler = initResultHandler(resultHandler);
        return this;
    }

    private ResultHandler initResultHandler(Class<? extends ResultHandler> resultHandler) {
        if(resultHandler != null && !resultHandler.equals(NullResultHandler.class))
            return ReflectionUtil.newInstance(resultHandler);
        else
            return new NullResultHandler();
    }

    public ProcessedCommandBuilder<C> resultHandler(ResultHandler resultHandler) {
        this.resultHandler = resultHandler;
        return this;
    }

    public ProcessedCommandBuilder<C> populator(CommandPopulator<Object, C> populator) {
        this.populator = populator;
        return this;
    }

    public ProcessedCommandBuilder<C> activator(CommandActivator activator) {
        this.activator = activator;
        return this;
    }

    public ProcessedCommandBuilder activator(Class<? extends CommandActivator> activator) {
        this.activator = initActivator(activator);
        return this;
    }

    private CommandActivator initActivator(Class<? extends CommandActivator> activator) {
        if(activator != null && activator != NullCommandActivator.class)
            return ReflectionUtil.newInstance(activator);
        else
            return new NullCommandActivator();
    }

    public ProcessedCommandBuilder<C> command(C command) {
        this.command = command;
        return this;
    }

    public ProcessedCommandBuilder<C> command(Class<C> command) {
        this.command = ReflectionUtil.newInstance(command);
        return this;
    }

    public ProcessedCommandBuilder<C> addOption(ProcessedOption option) {
        this.options.add(option);
        return this;
    }

    public ProcessedCommandBuilder<C> addOptions(List<ProcessedOption> options) {
        if(options != null)
            this.options.addAll(options);
        return this;
    }

    public ProcessedCommand<C> create() throws CommandLineParserException {
        if(name == null || name.length() < 1)
            throw new CommandLineParserException("The parameter name must be defined");

        if(validator == null)
            validator = new NullCommandValidator<>();

        if(resultHandler == null)
            resultHandler = new NullResultHandler();

        return new ProcessedCommand<>(name, aliases, command, description, validator,
                resultHandler, argument, options, populator, activator);
    }
}
