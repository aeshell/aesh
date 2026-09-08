/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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
package org.aesh.command.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.command.validator.OptionValidatorException;
import org.junit.Before;
import org.junit.Test;

public class HelpProcessedCommandTest {

    @Before
    public void resetConstructionFlags() {
        HelpCommand.constructed = false;
        CountingHelpConverter.constructions = 0;
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testHelpBuildMatchesFullBuildWithoutConstruction() throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        ProcessedCommand helpPC = builder.buildHelpProcessedCommand(HelpCommand.class);

        assertFalse(HelpCommand.constructed);
        assertEquals(0, CountingHelpConverter.constructions);
        assertNull(helpPC.getCommand());

        ProcessedCommand fullPC = builder.create(new HelpCommand()).getParser().getProcessedCommand();
        assertEquals(fullPC.getOptions().size(), helpPC.getOptions().size());
        assertEquals(fullPC.printHelp("helpcmd", false, false), helpPC.printHelp("helpcmd", false, false));
        assertEquals(fullPC.printHelp("helpcmd", false, true), helpPC.printHelp("helpcmd", false, true));
    }

    @CommandDefinition(name = "helpcmd", description = "help build command", generateHelp = true, version = "1.0")
    public static class HelpCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public HelpCommand() {
            constructed = true;
        }

        @Option(name = "output", converter = CountingHelpConverter.class, description = "Output path", aliases = {
                "out" }, helpGroup = "Output")
        private String output;

        @Option(name = "verbose", hasValue = false, description = "Verbose output")
        private boolean verbose;

        @Argument(description = "Source path")
        private String source;

        @Arguments(description = "Extra paths")
        private List<String> extras;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class CountingHelpConverter implements Converter<String, ConverterInvocation> {
        static int constructions;

        public CountingHelpConverter() {
            constructions++;
        }

        @Override
        public String convert(ConverterInvocation invocation) throws OptionValidatorException {
            return invocation.getInput();
        }
    }
}
