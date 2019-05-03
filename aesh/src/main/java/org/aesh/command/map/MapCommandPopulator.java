/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.aesh.command.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.aesh.command.Command;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.readline.AeshContext;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.selector.SelectorType;

/**
 *
 * Populator for MapCommand.
 *
 * @author jdenise@redhat.com
 */
public class MapCommandPopulator<O extends Object, CI extends CommandInvocation> implements CommandPopulator<O, CI> {

    private final MapCommand<CI> instance;
    private final Map<String, String> unknownOptions = new HashMap<>();

    MapCommandPopulator(MapCommand<CI> instance) {
        Objects.requireNonNull(instance);
        this.instance = instance;
    }

    @Override
    public void populateObject(ProcessedCommand<Command<CI>, CI> processedCommand,
            InvocationProviders invocationProviders,
            AeshContext aeshContext, CommandLineParser.Mode validate)
            throws CommandLineParserException, OptionValidatorException {
        if (processedCommand.parserExceptions().size() > 0) {
            throw processedCommand.parserExceptions().get(0);
        }

        // Populate with unknown ones first.
        // Unknown are options passed prior the option provider
        // was able to compute the set.
        for (String name : unknownOptions.keySet()) {
            instance.setValue(name, unknownOptions.get(name));
        }

        if (processedCommand.getArguments() != null) {
            if (processedCommand.getArguments().getValues().size() > 0) {
                List<Object> tmpSet = new ArrayList<>();
                for (String in : processedCommand.getArguments().getValues()) {
                    tmpSet.add(processedCommand.getArguments().doConvert(in, invocationProviders,
                            instance, aeshContext, validate == CommandLineParser.Mode.VALIDATE));
                }
                instance.setValue(processedCommand.getArguments().name(), tmpSet);
            } else if (processedCommand.getArguments().getDefaultValues().size() > 0 &&
                     processedCommand.getArguments().selectorType() == SelectorType.NO_OP) {
                List<Object> tmpSet = new ArrayList<>();
                for (String in : processedCommand.getArguments().getDefaultValues()) {
                    tmpSet.add(processedCommand.getArguments().doConvert(in, invocationProviders,
                            instance, aeshContext, validate == CommandLineParser.Mode.VALIDATE));
                }
                instance.setValue(processedCommand.getArguments().name(), tmpSet);
            } else {
                instance.resetValue(processedCommand.getArguments().name());
            }
        }

        if (processedCommand.getArgument() != null) {
            if (processedCommand.getArgument().getValues().size() > 0) {
                String val = processedCommand.getArgument().getValue();
                if (val != null) {
                    instance.setValue(processedCommand.getArgument().name(),
                            processedCommand.getArgument().
                            doConvert(val, invocationProviders, instance, aeshContext,
                                    validate == CommandLineParser.Mode.VALIDATE));
                } else if (processedCommand.getArgument().getDefaultValues().size() > 0 &&
                         processedCommand.getArguments().selectorType() == SelectorType.NO_OP) {
                    instance.setValue(processedCommand.getArgument().name(),
                            processedCommand.getArgument().getDefaultValues().get(0));
                } else {
                    instance.resetValue(processedCommand.getArgument().name());
                }
            } else {
                instance.resetValue(processedCommand.getArgument().name());
            }
        }

        // At this point, if no dynamic options have been retrieved it means
        // that no dynamic options have been provided, so no need to compute the set now.
        @SuppressWarnings("unchecked")
        MapProcessedCommand<CommandInvocation> mpc = (MapProcessedCommand) processedCommand;
        for (ProcessedOption option : mpc.getCurrentOptions()) {
            // Do not erase the value that would have been set as an unknown option.
            if (!unknownOptions.containsKey(option.name())) {
                if (option.getValue() != null) {
                    instance.setValue(option.name(),
                            option.doConvert(option.getValue(), invocationProviders,
                                    instance, aeshContext, validate == CommandLineParser.Mode.VALIDATE));
                } else if (option.getDefaultValues().size() > 0) {
                    instance.setValue(option.name(), option.getDefaultValues().get(0));
                } else {
                    instance.resetValue(option.name());
                }
            }
        }
        unknownOptions.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public O getObject() {
        return (O) instance;
    }

    public void addUnknownOption(String opt) {
        if (opt.startsWith("--")) {
            opt = opt.substring(2);
        } else if (opt.startsWith("-")) {
            opt = opt.substring(1);
        }
        int i = opt.indexOf("=");
        String name = opt;
        String value = null;
        if (i > 0) {
            name = opt.substring(0, i);
            value = opt.substring(i + 1);
        }
        unknownOptions.put(name, value);
    }
}
