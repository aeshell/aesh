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

import java.util.Objects;

import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.console.AeshContext;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.Command;
import org.aesh.command.parser.CommandLineParserException;

/**
 *
 * Populator for MapCommand.
 *
 * @author jdenise@redhat.com
 */
class MapCommandPopulator implements CommandPopulator<Object, Command> {

    private final MapCommand instance;

    MapCommandPopulator(MapCommand instance) {
        Objects.requireNonNull(instance);
        this.instance = instance;
    }

    @Override
    public void populateObject(ProcessedCommand<Command> processedCommand,
                               InvocationProviders invocationProviders,
                               AeshContext aeshContext, CommandLineParser.Mode validate)
            throws CommandLineParserException, OptionValidatorException {
        if (processedCommand.parserExceptions().size() > 0) {
            throw processedCommand.parserExceptions().get(0);
        }
        for (ProcessedOption option : processedCommand.getOptions()) {
            if (option.getValue() != null) {
                instance.setValue(option.name(),
                        option.doConvert(option.getValue(), invocationProviders,
                                instance, aeshContext, validate == CommandLineParser.Mode.VALIDATE));
            } else {
                instance.resetValue(option.name());
            }
        }

        if (processedCommand.getArguments() != null) {
            if (processedCommand.getArguments().getValues().size() > 0) {
                String val = processedCommand.getArguments().getValue();
                if (val != null) {
                    instance.setValue(processedCommand.getArguments().name(),
                            processedCommand.getArguments().
                            doConvert(val,
                                    invocationProviders, instance, aeshContext,
                                    validate == CommandLineParser.Mode.VALIDATE));
                } else {
                    instance.resetValue(processedCommand.getArguments().name());
                }
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
                } else {
                    instance.resetValue(processedCommand.getArgument().name());
                }
            } else {
                instance.resetValue(processedCommand.getArgument().name());
            }
        }
    }

    @Override
    public Object getObject() {
        return instance;
    }
}
