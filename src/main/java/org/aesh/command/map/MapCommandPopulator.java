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

import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.console.AeshContext;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.Command;
import org.aesh.command.impl.parser.CommandLine;
import org.aesh.command.impl.parser.CommandLineParserException;

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
    public void populateObject(CommandLine<Command> line,
                               InvocationProviders invocationProviders,
                               AeshContext aeshContext, boolean validate)
            throws CommandLineParserException, OptionValidatorException {
        if (line.hasParserError()) {
            throw line.getParserException();
        }
        for (ProcessedOption option : line.getParser().getProcessedCommand().getOptions()) {
            if (line.hasOption(option.getName())) {
                ProcessedOption o = line.getOption(option.getName());
                instance.setValue(o.getName(),
                        o.doConvert(o.getValue(), invocationProviders,
                                instance, aeshContext, validate));
            } else if (option.getDefaultValues().size() > 0) {
                instance.setValue(option.getName(),
                        option.doConvert(option.getDefaultValues().get(0),
                                invocationProviders, instance, aeshContext,
                                validate));
            } else {
                instance.resetValue(option.getName());
            }
        }
        if ((line.getArgument() != null && line.getArgument().getValues().size() > 0)
                || (line.getParser().getProcessedCommand().getArgument() != null
                && line.getParser().getProcessedCommand().getArgument().
                getDefaultValues().size() > 0)) {
            Object val = line.getArgument().getValue();
            if (val == null) {
                instance.setValue(line.getArgument().getName(),
                        line.getArgument().
                        doConvert(line.getArgument().getDefaultValues().get(0),
                                invocationProviders, instance, aeshContext,
                                validate));
            } else {
                instance.setValue(line.getArgument().getName(),
                        line.getArgument().
                        doConvert(line.getArgument().getValue(),
                                invocationProviders, instance, aeshContext,
                                validate));
            }
        } else if (line.getArgument() != null) {
            // Must be named
            instance.resetValue(line.getArgument().getName());
        }
    }

    @Override
    public Object getObject() {
        return instance;
    }
}
