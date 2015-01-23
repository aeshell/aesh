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
package org.jboss.aesh.cl.internal;

import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.populator.CommandPopulator;
import org.jboss.aesh.cl.result.NullResultHandler;
import org.jboss.aesh.cl.result.ResultHandler;
import org.jboss.aesh.cl.validator.CommandValidator;
import org.jboss.aesh.cl.validator.NullCommandValidator;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.util.ReflectionUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Build a {@link org.jboss.aesh.cl.internal.ProcessedCommand} object using the Builder pattern.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 * @author <a href="mailto:danielsoro@gmail.com">Daniel Cunha (soro)</a>
 */
public class ProcessedCommandBuilder {

    private String name;
    private String description;
    private CommandValidator<?> validator;
    private ResultHandler resultHandler;
    private ProcessedOption argument;
    private final List<ProcessedOption> options;
    private CommandPopulator populator;
    private Command command;

    public ProcessedCommandBuilder() {
        options = new ArrayList<>();
    }

    public ProcessedCommandBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ProcessedCommandBuilder description(String usage) {
        this.description = usage;
        return this;
    }

    public ProcessedCommandBuilder argument(ProcessedOption argument) {
        this.argument = argument;
        return this;
    }

    public ProcessedCommandBuilder validator(CommandValidator<?> validator) {
        this.validator = validator;
        return this;
    }

    public ProcessedCommandBuilder validator(Class<? extends CommandValidator> validator) {
        this.validator = initValidator(validator);
        return this;
    }

    private CommandValidator initValidator(Class<? extends CommandValidator> validator) {
        if(validator != null && !validator.equals(NullCommandValidator.class))
            return ReflectionUtil.newInstance(validator);
        else
            return new NullCommandValidator();
    }

    public ProcessedCommandBuilder resultHandler(Class<? extends ResultHandler> resultHandler) {
        this.resultHandler = initResultHandler(resultHandler);
        return this;
    }

    private ResultHandler initResultHandler(Class<? extends ResultHandler> resultHandler) {
        if(resultHandler != null && !resultHandler.equals(NullResultHandler.class))
            return ReflectionUtil.newInstance(resultHandler);
        else
            return new NullResultHandler();
    }

    public ProcessedCommandBuilder resultHandler(ResultHandler resultHandler) {
        this.resultHandler = resultHandler;
        return this;
    }

    public ProcessedCommandBuilder populator(CommandPopulator populator) {
        this.populator = populator;
        return this;
    }

    public ProcessedCommandBuilder command(Command command) {
        this.command = command;
        return this;
    }

    public ProcessedCommandBuilder command(Class<? extends Command> command) {
        this.command = ReflectionUtil.newInstance(command);
        return this;
    }

    public ProcessedCommandBuilder addOption(ProcessedOption option) {
        this.options.add(option);
        return this;
    }

    public ProcessedCommandBuilder addOptions(List<ProcessedOption> options) {
        if(options != null)
            this.options.addAll(options);
        return this;
    }

    public ProcessedCommand create() throws CommandLineParserException {
        if(name == null || name.length() < 1)
            throw new CommandLineParserException("The parameter name must be defined");

        if(validator == null)
            validator = new NullCommandValidator();

        if(resultHandler == null)
            resultHandler = new NullResultHandler();

        return new ProcessedCommand(name, command, description, validator, resultHandler, argument, options, populator);
    }
}
