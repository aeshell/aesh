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
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.parser.OptionParserException;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.command.impl.result.NullResultHandler;
import org.aesh.command.result.ResultHandler;
import org.aesh.command.validator.CommandValidator;
import org.aesh.command.impl.validator.NullCommandValidator;
import org.aesh.util.ReflectionUtil;
import org.aesh.command.parser.CommandLineParserException;
import java.util.List;
import org.aesh.command.impl.internal.ParsedCommand;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.readline.util.Parser;

public class MapProcessedCommandBuilder {

    private static final ProcessedOptionProvider EMPTY_PROVIDER = new ProcessedOptionProvider() {
        @Override
        public List<ProcessedOption> getOptions(List<ProcessedOption> options) {
            return Collections.emptyList();
        }
    };

    public static class MapProcessedCommand extends ProcessedCommand<MapCommand> {

        private final ProcessedOptionProvider provider;
        private List<ProcessedOption> currentOptions;
        private final boolean initialized;
        private final boolean lookup;
        MapProcessedCommand(String name,
                List<String> aliases,
                MapCommand command,
                String description,
                CommandValidator validator,
                ResultHandler resultHandler,
                ProcessedOption arguments,
                List<ProcessedOption> options,
                ProcessedOption argument,
                CommandPopulator populator,
                ProcessedOptionProvider provider,
                CommandActivator activator,
                boolean lookup) throws OptionParserException {
            super(name, aliases, command, description, validator, resultHandler, arguments,
                    options, argument, populator, activator);
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
        public ProcessedOption searchAllOptions(String input) {
            if (!initialized) {
                return super.searchAllOptions(input);
            }
            if (lookup && completeStatus() == null) {
                return null;
            }
            if (input.startsWith("--")) {
                ProcessedOption currentOption = null;
                if (input.contains("=")) {
                    String optName = input.substring(2, input.indexOf("="));
                    currentOption = findLongOptionNoActivatorCheck(optName);
                } else {
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
            if (lookup && completeStatus() == null) {
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
            if (lookup && completeStatus() == null) {
                return null;
            }
            // First check in parent (static options).
            for (ProcessedOption option : super.getOptions()) {
                if (option.name() != null && option.name().equals(name)) {
                    return option;
                }
            }

            // Then in dynamics
            for (ProcessedOption option : provider.getOptions(currentOptions)) {
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
                currentOptions = provider.getOptions(currentOptions);
                allOptions.addAll(currentOptions);
            }
            return allOptions;
        }

        @Override
        public void clear() {
            MapCommand cmd = getCommand();
            cmd.resetAll();
            super.clear();
        }
    }

    public interface ProcessedOptionProvider {

        List<ProcessedOption> getOptions(List<ProcessedOption> currentOptions);
    }
    private ProcessedOptionProvider provider;
    private String name;
    private String description;
    private CommandValidator<?> validator;
    private ResultHandler resultHandler;
    private ProcessedOption arguments;
    private ProcessedOption argument;
    private final List<ProcessedOption> options;
    private CommandPopulator populator;
    private MapCommand command;
    private List<String> aliases;
    private CommandActivator activator;
    private boolean lookup;

    public MapProcessedCommandBuilder() {
        options = new ArrayList<>();
    }

    public MapProcessedCommandBuilder lookupAtCompletionOnly(boolean lookup) {
        this.lookup = lookup;
        return this;
    }

    public MapProcessedCommandBuilder name(String name) {
        this.name = name;
        return this;
    }

    public MapProcessedCommandBuilder name(List<String> aliases) {
        this.aliases = aliases == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(aliases);
        return this;
    }

    public MapProcessedCommandBuilder description(String usage) {
        this.description = usage;
        return this;
    }

    public MapProcessedCommandBuilder optionProvider(ProcessedOptionProvider provider) {
        this.provider = provider;
        return this;
    }

    public MapProcessedCommandBuilder arguments(ProcessedOption arguments) {
        this.arguments = arguments;
        return this;
    }

    public MapProcessedCommandBuilder argument(ProcessedOption argument) {
        this.argument = argument;
        return this;
    }

    public MapProcessedCommandBuilder validator(CommandValidator<?> validator) {
        this.validator = validator;
        return this;
    }

    public MapProcessedCommandBuilder validator(Class<? extends CommandValidator> validator) {
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

    public MapProcessedCommandBuilder resultHandler(Class<? extends ResultHandler> resultHandler) {
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

    public MapProcessedCommandBuilder resultHandler(ResultHandler resultHandler) {
        this.resultHandler = resultHandler;
        return this;
    }

    /**
     * By default a map populator will inject values in the MapCommand
     *
     * @param populator
     * @return
     */
    public MapProcessedCommandBuilder populator(CommandPopulator populator) {
        this.populator = populator;
        return this;
    }

    public MapProcessedCommandBuilder command(MapCommand command) {
        this.command = command;
        return this;
    }

    public MapProcessedCommandBuilder command(Class<? extends MapCommand> command) {
        this.command = ReflectionUtil.newInstance(command);
        return this;
    }

    public MapProcessedCommandBuilder addOption(ProcessedOption option) {
        this.options.add(option);
        return this;
    }

    public MapProcessedCommandBuilder addOptions(List<ProcessedOption> options) {
        if (options != null) {
            this.options.addAll(options);
        }
        return this;
    }

    public MapProcessedCommandBuilder activator(CommandActivator activator) {
        this.activator = activator;
        return this;
    }

    public MapProcessedCommand create() throws CommandLineParserException {
        if (name == null || name.length() < 1) {
            throw new CommandLineParserException("The parameter name must be defined");
        }

        if (validator == null) {
            validator = new NullCommandValidator();
        }

        if (resultHandler == null) {
            resultHandler = new NullResultHandler();
        }

        if (populator == null) {
            populator = new MapCommandPopulator(command);
        }

        return new MapProcessedCommand(name,
                aliases,
                command,
                description,
                validator,
                resultHandler,
                arguments,
                options,
                argument,
                populator,
                provider,
                activator,
                lookup);
    }
}
