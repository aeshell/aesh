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
package org.aesh.command.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.parser.AeshCommandLineParser;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
import org.aesh.command.parser.MutuallyExclusiveOptionException;
import org.aesh.terminal.formatting.TerminalString;
import org.junit.Test;

public class MutuallyExclusiveOptionTest {

    @Test
    public void testMutuallyExclusiveOptionsFromAnnotation() throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        CommandLineParser<CommandInvocation> parser = builder.create(new OutputFormatCommand()).getParser();

        // Using only --json should succeed
        parser.parse("output --json");
        assertEquals(0, parser.getProcessedCommand().parserExceptions().size());
        parser.clear();

        // Using only --xml should succeed
        parser.parse("output --xml");
        assertEquals(0, parser.getProcessedCommand().parserExceptions().size());
        parser.clear();

        // Using both --json and --xml should fail with MutuallyExclusiveOptionException
        parser.parse("output --json --xml");
        assertEquals(1, parser.getProcessedCommand().parserExceptions().size());
        assertTrue(parser.getProcessedCommand().parserExceptions().get(0) instanceof MutuallyExclusiveOptionException);
        String msg = parser.getProcessedCommand().parserExceptions().get(0).getMessage();
        assertTrue(msg.contains("json"));
        assertTrue(msg.contains("xml"));
    }

    @Test
    public void testMutuallyExclusiveOptionsFromBuilder() throws Exception {
        @SuppressWarnings("unchecked")
        org.aesh.command.impl.internal.ProcessedCommand processedCommand = ProcessedCommandBuilder.builder()
                .name("test")
                .command(new OutputFormatCommand())
                .create();

        processedCommand.addOption(
                ProcessedOptionBuilder.builder()
                        .name("json")
                        .hasValue(false)
                        .type(Boolean.class)
                        .fieldName("json")
                        .exclusiveWith("xml")
                        .build());

        processedCommand.addOption(
                ProcessedOptionBuilder.builder()
                        .name("xml")
                        .hasValue(false)
                        .type(Boolean.class)
                        .fieldName("xml")
                        .exclusiveWith("json")
                        .build());

        processedCommand.addOption(
                ProcessedOptionBuilder.builder()
                        .name("verbose")
                        .hasValue(false)
                        .type(Boolean.class)
                        .fieldName("verbose")
                        .build());

        AeshCommandLineParser<CommandInvocation> parser = new AeshCommandLineParser<>(processedCommand);

        // Both set — should produce error
        parser.parse("test --json --xml");
        assertEquals(1, parser.getProcessedCommand().parserExceptions().size());
        assertTrue(parser.getProcessedCommand().parserExceptions().get(0) instanceof MutuallyExclusiveOptionException);
        parser.clear();

        // --json alone is fine
        parser.parse("test --json --verbose");
        assertEquals(0, parser.getProcessedCommand().parserExceptions().size());
    }

    @Test
    public void testCompletionFiltersExclusiveOptions() throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        CommandLineParser<CommandInvocation> parser = builder.create(new OutputFormatCommand()).getParser();

        // Before any option is set, all options should be available
        List<TerminalString> allOptions = parser.getProcessedCommand().getOptionLongNamesWithDash();
        assertTrue(containsOption(allOptions, "--json"));
        assertTrue(containsOption(allOptions, "--xml"));
        assertTrue(containsOption(allOptions, "--verbose"));

        // After setting --json, --xml should be filtered out
        parser.parse("output --json", CommandLineParser.Mode.COMPLETION);
        CommandLineParser<CommandInvocation> parsed = parser.parsedCommand();
        List<TerminalString> remaining = parsed.getProcessedCommand().getOptionLongNamesWithDash();
        assertFalse(containsOption(remaining, "--xml"));
        assertTrue(containsOption(remaining, "--verbose"));
    }

    @Test
    public void testNonExclusiveOptionsAllowed() throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        CommandLineParser<CommandInvocation> parser = builder.create(new OutputFormatCommand()).getParser();

        // --json + --verbose is fine (not mutually exclusive)
        parser.parse("output --json --verbose");
        assertEquals(0, parser.getProcessedCommand().parserExceptions().size());
    }

    @Test
    public void testExclusiveWithOptionList() throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        CommandLineParser<CommandInvocation> parser = builder.create(new ListExclusiveCommand()).getParser();

        parser.parse("listcmd --items a,b --single x");
        assertEquals(1, parser.getProcessedCommand().parserExceptions().size());
        assertTrue(parser.getProcessedCommand().parserExceptions().get(0) instanceof MutuallyExclusiveOptionException);
    }

    private boolean containsOption(List<TerminalString> options, String name) {
        for (TerminalString ts : options) {
            if (ts.getCharacters().startsWith(name))
                return true;
        }
        return false;
    }

    @CommandDefinition(name = "output", description = "output format test")
    public static class OutputFormatCommand implements Command<CommandInvocation> {
        @Option(hasValue = false, exclusiveWith = { "xml" })
        public boolean json;

        @Option(hasValue = false, exclusiveWith = { "json" })
        public boolean xml;

        @Option(hasValue = false)
        public boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "listcmd", description = "list exclusive test")
    public static class ListExclusiveCommand implements Command<CommandInvocation> {
        @OptionList(exclusiveWith = { "single" })
        public java.util.List<String> items;

        @Option(exclusiveWith = { "items" })
        public String single;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }
}
