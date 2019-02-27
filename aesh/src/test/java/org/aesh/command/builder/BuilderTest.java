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
package org.aesh.command.builder;

import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.validator.NullCommandValidator;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.parser.CommandLineParserBuilder;
import org.aesh.command.impl.result.NullResultHandler;
import org.aesh.command.Command;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class BuilderTest {

    @Test
    public void testBuilder() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder();
        pb.name("foo").description("foo is bar");
        pb.addOption(
                ProcessedOptionBuilder.builder().description("filename given").shortName('f').name("filename")
                        .type(String.class).hasValue(true).build());
        pb.argument(ProcessedOptionBuilder.builder().shortName('\u0000').name("")
                .description("argument!!").type(Integer.class).optionType(OptionType.ARGUMENT).hasValue(true).build());

        CommandLineParser clp = CommandLineParserBuilder.builder().processedCommand(pb.create()).create();

        clp.parse("foo -f test1.txt baAar");
        assertEquals("test1.txt", clp.getProcessedCommand().findOption("f").getValue());
        assertEquals("baAar", clp.getProcessedCommand().getArgument().getValue());
    }

    @Test
    public void testBuilder2() throws CommandLineParserException {

        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder().name("less").description("less is more");
        pb.addOption(
                ProcessedOptionBuilder.builder().description("version").shortName('V').name("version")
                        .hasValue(false).required(true).type(String.class).build());
        pb.addOption(
                ProcessedOptionBuilder.builder().description("is verbose").shortName('v').name("verbose")
                        .hasValue(false).type(String.class).build());

        pb.addOption(
                ProcessedOptionBuilder.builder().description("attributes").shortName('D').name("attributes")
                        .isProperty(true).type(String.class).build());

        pb.addOption(
                ProcessedOptionBuilder.builder().description("values").name("values").shortName('a')
                        .valueSeparator(',')
                        .hasMultipleValues(true).type(String.class).build());

        pb.arguments(ProcessedOptionBuilder.builder().shortName('\u0000').name("").hasMultipleValues(true)
                .optionType(OptionType.ARGUMENTS).type(String.class).build());

        CommandLineParser clp = CommandLineParserBuilder.builder().processedCommand(pb.create()).create();

        clp.parse("less -V test1.txt");
        assertEquals("true", clp.getProcessedCommand().findOption("V").getValue());
        assertEquals("test1.txt", clp.getProcessedCommand().getArguments().getValues().get(0));

        clp.parse("less -V -Dfoo1=bar1 -Dfoo2=bar2 test1.txt");
        assertEquals("bar2", clp.getProcessedCommand().findOption("D").getProperties().get("foo2"));

        clp.parse("less -V -Dfoo1=bar1 -Dfoo2=bar2 --values f1,f2,f3 test1.txt");
        assertEquals("f2", clp.getProcessedCommand().findLongOption("values").getValues().get(1));
        assertEquals("test1.txt", clp.getProcessedCommand().getArguments().getValues().get(0));
    }

    @Test
    public void testBuilder3() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder().name("less").description("less is more");
        pb.addOption(
                ProcessedOptionBuilder.builder()
                        .description("version")
                        .name("version")
                        .shortName('v')
                        .hasValue(false)
                        .required(true)
                        .type(String.class)
                        .build());
        pb.addOption(
                ProcessedOptionBuilder.builder()
                        .description("is verbose")
                        .name("verbose")
                        .hasValue(false)
                        .shortName('e')
                        .type(String.class)
                        .build());

        pb.arguments(ProcessedOptionBuilder.builder().shortName('\u0000').name("").hasMultipleValues(true)
                .optionType(OptionType.ARGUMENTS).type(String.class).build());

        CommandLineParser clp = CommandLineParserBuilder.builder().processedCommand(pb.create()).create();

        assertEquals("version", clp.getProcessedCommand().findOption("v").name());
        assertEquals("verbose", clp.getProcessedCommand().findOption("e").name());

        clp.parse("less -v -e test1.txt");
        assertEquals("true", clp.getProcessedCommand().findOption("v").getValue());
        assertEquals("true", clp.getProcessedCommand().findOption("e").getValue());
        assertEquals("test1.txt", clp.getProcessedCommand().getArguments().getValues().get(0));
    }

    @Test
    public void testParameterInt() throws CommandLineParserException {
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> processedCommand =
                ProcessedCommandBuilder.builder()
                        .name("foo")
                        .description("")
                        .validator(NullCommandValidator.class)
                        .resultHandler(NullResultHandler.class)
                        .create();
        processedCommand.addOption(ProcessedOptionBuilder.builder().name("foo1").shortName('f').type(String.class).build());
        processedCommand.addOption(ProcessedOptionBuilder.builder().name("foo2").shortName('o').type(String.class).build());
        processedCommand.addOption(ProcessedOptionBuilder.builder().name("foo3").shortName('3').type(String.class).build());

        assertEquals("f", processedCommand.getOptions().get(0).shortName());
        assertEquals("o", processedCommand.getOptions().get(1).shortName());
        assertEquals("3", processedCommand.getOptions().get(2).shortName());
    }

}
