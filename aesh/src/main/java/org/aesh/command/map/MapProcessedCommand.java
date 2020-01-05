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
package org.aesh.command.map;

import org.aesh.command.activator.CommandActivator;
import org.aesh.command.impl.internal.ParsedCommand;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser.Mode;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.parser.OptionParserException;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.command.result.ResultHandler;
import org.aesh.command.validator.CommandValidator;
import org.aesh.readline.util.Parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class MapProcessedCommand<CI extends CommandInvocation> extends ProcessedCommand<MapCommand<CI>, CI> {

    private final MapProcessedOptionProvider provider;
    private List<ProcessedOption> currentOptions;
    private final boolean initialized;
    private final boolean lookup;
    private Mode mode;

    private static final MapProcessedOptionProvider EMPTY_PROVIDER = options -> Collections.emptyList();

    MapProcessedCommand(String name,
                        List<String> aliases,
                        MapCommand<CI> command,
                        String description,
                        CommandValidator<MapCommand<CI>, CI> validator,
                        ResultHandler resultHandler,
                        boolean generateHelp,
                        boolean disableParsing,
                        String version,
                        ProcessedOption arguments,
                        List<ProcessedOption> options,
                        ProcessedOption argument,
                        CommandPopulator<Object,CI> populator,
                        MapProcessedOptionProvider provider,
                        CommandActivator activator,
                        boolean lookup) throws OptionParserException {
        super(name, aliases, command, description, validator, resultHandler, generateHelp,
                disableParsing, version, arguments, options, argument, populator, activator);
        initialized = true;
        this.provider = provider == null ? EMPTY_PROVIDER : provider;
        this.lookup = lookup;
    }

    @Override
    protected void updateOptionsInvocationProviders(InvocationProviders invocationProviders) {
        //Only update static options.
        for (ProcessedOption option : super.getOptions()) {
            option.updateInvocationProviders(invocationProviders);
        }
    }
    @Override
    public List<ProcessedOption> getOptions() {
        if (!initialized) {
            return super.getOptions();
        }
        return getOptions(true);
    }

    @Override
    public boolean hasAskIfNotSet() {
        for (ProcessedOption opt : getOptions(false)) {
            if (opt.askIfNotSet() && opt.hasValue() && opt.getValues().isEmpty() && !opt.hasDefaultValue()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ProcessedOption searchAllOptions(String input) {
        if (!initialized) {
            return super.searchAllOptions(input);
        }
        if (lookup && !Mode.COMPLETION.equals(mode)) {
            return null;
        }
        if (input.startsWith("--")) {
            ProcessedOption currentOption = findLongOptionNoActivatorCheck(input.substring(2));
            if (currentOption == null && input.contains("=")) {
                currentOption = startWithLongOptionNoActivatorCheck(input.substring(2));
            }
            if (currentOption != null) {
                currentOption.setLongNameUsed(true);
            } //need to handle spaces in option names
            else if (Parser.containsNonEscapedSpace(input)) {
                return searchAllOptions(Parser.switchSpacesToEscapedSpacesInWord(input));
            }

            return currentOption;
        } else {
            return super.searchAllOptions(input);
        }
    }

    @Override
    public ProcessedOption findLongOption(String name) {
        if (!initialized) {
            return super.findLongOption(name);
        }
        if (lookup && !Mode.COMPLETION.equals(mode)) {
            return null;
        }
        for (ProcessedOption option : getOptions(false)) {
            if (option.name() != null
                        && option.name().equals(name)
                        && option.activator().isActivated(new ParsedCommand(this))) {
                return option;
            }
        }
        for (ProcessedOption option : getOptions(true)) {
            if (option.name() != null
                        && option.name().equals(name)
                        && option.activator().isActivated(new ParsedCommand(this))) {
                return option;
            }
        }
        return null;
    }
    @Override
    public ProcessedOption findLongOptionNoActivatorCheck(String name) {
        if (!initialized) {
            return super.findLongOptionNoActivatorCheck(name);
        }
        if (lookup && !Mode.COMPLETION.equals(mode)) {
            return null;
        }
        // First check in parent (static options).
        for (ProcessedOption option : getOptions(false)) {
            if (option.name() != null && option.name().equals(name)) {
                return option;
            }
        }

        // Then in dynamics
        for (ProcessedOption option : getOptions(true)) {
            if (option.name() != null && option.name().equals(name)) {
                return option;
            }
        }
        return null;
    }

    @Override
    public void clearOptions() {
        for (ProcessedOption processedOption : getCurrentOptions()) {
            processedOption.clear();
        }
    }

    List<ProcessedOption> getCurrentOptions() {
        List<ProcessedOption> allOptions = new ArrayList<>(super.getOptions());
        if (currentOptions != null) {
            allOptions.addAll(currentOptions);
        }
        return allOptions;
    }

    public List<ProcessedOption> getOptions(boolean dynamic) {
        List<ProcessedOption> allOptions = new ArrayList<>(super.getOptions());
        // During super construction, properties are retrieved. In this case
        // provider is not already set.
        if (provider != null && dynamic) {
            if (currentOptions == null || currentOptions.isEmpty()) {
                currentOptions = provider.getOptions(currentOptions);
            }
            allOptions.addAll(currentOptions);
        }
        return allOptions;
    }

    @Override
    public void clear() {
        MapCommand cmd = getCommand();
        mode = null;
        cmd.resetAll();
        super.clear();
        // null after the currentOptions have been cleared by the super.clear();
        currentOptions = null;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

}
