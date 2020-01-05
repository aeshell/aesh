/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
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
package org.aesh.command.map;

import java.util.ArrayList;
import java.util.Collections;

import org.aesh.command.activator.CommandActivator;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.command.impl.result.NullResultHandler;
import org.aesh.command.result.ResultHandler;
import org.aesh.command.validator.CommandValidator;
import org.aesh.command.impl.validator.NullCommandValidator;
import org.aesh.util.ReflectionUtil;
import org.aesh.command.parser.CommandLineParserException;
import java.util.List;

public class MapProcessedCommandBuilder<CI extends CommandInvocation> {

    private MapProcessedOptionProvider provider;
    private String name;
    private String description;
    private CommandValidator<MapCommand<CI>,CI> validator;
    private ResultHandler resultHandler;
    private ProcessedOption arguments;
    private ProcessedOption argument;
    private final List<ProcessedOption> options;
    private CommandPopulator<Object,CI> populator;
    private MapCommand<CI> command;
    private List<String> aliases;
    private CommandActivator activator;
    private boolean lookup;
    private boolean generateHelp;
    private boolean disableParsing;
    private String version;

    private MapProcessedCommandBuilder() {
        options = new ArrayList<>();
    }

    public static <T extends CommandInvocation> MapProcessedCommandBuilder<T> builder() {
        return new MapProcessedCommandBuilder<>();
    }

    public MapProcessedCommandBuilder<CI> lookupAtCompletionOnly(boolean lookup) {
        this.lookup = lookup;
        return this;
    }

    public MapProcessedCommandBuilder<CI> name(String name) {
        this.name = name;
        return this;
    }

    public MapProcessedCommandBuilder<CI> name(List<String> aliases) {
        this.aliases = aliases == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(aliases);
        return this;
    }

    public MapProcessedCommandBuilder<CI> description(String usage) {
        this.description = usage;
        return this;
    }

    public MapProcessedCommandBuilder<CI> version(String version) {
        this.version = version;
        return this;
    }

    public MapProcessedCommandBuilder<CI> generateHelp(boolean help) {
        this.generateHelp = help;
        return this;
    }

    public MapProcessedCommandBuilder<CI> disableParsing(boolean disableParsing) {
        this.disableParsing = disableParsing;
        return this;
    }

    public MapProcessedCommandBuilder<CI> optionProvider(MapProcessedOptionProvider provider) {
        this.provider = provider;
        return this;
    }

    public MapProcessedCommandBuilder<CI> arguments(ProcessedOption arguments) {
        this.arguments = arguments;
        return this;
    }

    public MapProcessedCommandBuilder<CI> argument(ProcessedOption argument) {
        this.argument = argument;
        return this;
    }

    public MapProcessedCommandBuilder<CI> validator(CommandValidator<MapCommand<CI>,CI> validator) {
        this.validator = validator;
        return this;
    }

    @SuppressWarnings("unchecked")
    public MapProcessedCommandBuilder<CI> validator(Class<? extends CommandValidator> validator) {
        this.validator = initValidator(validator);
        return this;
    }

    private CommandValidator initValidator(Class<? extends CommandValidator> validator) {
        if (validator != null && !validator.equals(NullCommandValidator.class)) {
            return ReflectionUtil.newInstance(validator);
        } else {
            return new NullCommandValidator();
        }
    }

    public MapProcessedCommandBuilder<CI> resultHandler(Class<? extends ResultHandler> resultHandler) {
        this.resultHandler = initResultHandler(resultHandler);
        return this;
    }

    private ResultHandler initResultHandler(Class<? extends ResultHandler> resultHandler) {
        if (resultHandler != null && !resultHandler.equals(NullResultHandler.class)) {
            return ReflectionUtil.newInstance(resultHandler);
        } else {
            return new NullResultHandler();
        }
    }

    public MapProcessedCommandBuilder<CI> resultHandler(ResultHandler resultHandler) {
        this.resultHandler = resultHandler;
        return this;
    }

    /**
     * By default a map populator will inject values in the MapCommand
     *
     * @param populator
     * @return
     */
    public MapProcessedCommandBuilder<CI> populator(CommandPopulator<Object,CI> populator) {
        this.populator = populator;
        return this;
    }

    public MapProcessedCommandBuilder<CI> command(MapCommand<CI> command) {
        this.command = command;
        return this;
    }

    public MapProcessedCommandBuilder<CI> command(Class<? extends MapCommand<CI>> command) {
        this.command = ReflectionUtil.newInstance(command);
        return this;
    }

    public MapProcessedCommandBuilder<CI> addOption(ProcessedOption option) {
        this.options.add(option);
        return this;
    }

    public MapProcessedCommandBuilder<CI> addOptions(List<ProcessedOption> options) {
        if (options != null) {
            this.options.addAll(options);
        }
        return this;
    }

    public MapProcessedCommandBuilder<CI> activator(CommandActivator activator) {
        this.activator = activator;
        return this;
    }

    @SuppressWarnings("unchecked")
    public MapProcessedCommand<CI> create() throws CommandLineParserException {
        if (name == null || name.length() < 1) {
            throw new CommandLineParserException("The parameter name must be defined");
        }

        if (validator == null) {
            validator = (CommandValidator) new NullCommandValidator();
        }

        if (resultHandler == null) {
            resultHandler = new NullResultHandler();
        }

        if (populator == null) {
            populator = new MapCommandPopulator<>(command);
        }

        return new MapProcessedCommand<>(name,
                aliases,
                command,
                description,
                validator,
                resultHandler,
                generateHelp,
                disableParsing,
                version,
                arguments,
                options,
                argument,
                populator,
                provider,
                activator,
                lookup);
    }
}
