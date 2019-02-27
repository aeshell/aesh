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

import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.option.Option;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParserGeneratorTest {

    @Test
    public void testClassGenerator() throws CommandLineParserException {

        Test1<CommandInvocation> test1 = new Test1<>();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(test1).getParser();

        assertEquals("a simple test", parser.getProcessedCommand().description());
        List<ProcessedOption> options = parser.getProcessedCommand().getOptions();
        assertEquals("f", options.get(0).shortName());
        assertEquals("foo", options.get(0).name());
        assertEquals("e", options.get(1).shortName());
        assertEquals("enable e", options.get(1).description());
        assertTrue(options.get(1).hasValue());
        assertTrue(options.get(1).isRequired());
        assertEquals("bar", options.get(2).name());
        assertFalse(options.get(2).hasValue());

        Test2<CommandInvocation> test2 = new Test2<>();
        CommandLineParser<CommandInvocation> parser2 = new AeshCommandContainerBuilder<>().create(test2).getParser();
        assertEquals("more [options] file...", parser2.getProcessedCommand().description());
        options = parser2.getProcessedCommand().getOptions();
        assertEquals("d", options.get(0).shortName());
        assertEquals("V", options.get(1).shortName());

        CommandLineParser<CommandInvocation> parser3 = new AeshCommandContainerBuilder<>().create(new Test3<>()).getParser();
        options = parser3.getProcessedCommand().getOptions();
        assertEquals("t", options.get(0).shortName());
        assertEquals("e", options.get(1).shortName());

    }

    @CommandDefinition(name = "test", description = "a simple test")
    public class Test1<CI extends CommandInvocation> implements Command<CI> {
        @Option(shortName = 'f', name = "foo", description = "enable foo")
        private String foo;

        @Option(shortName = 'e', description = "enable e", required = true)
        private String e;

        @Option(description = "has enabled bar", hasValue = false)
        private Boolean bar;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "test", description = "more [options] file...")
    public class Test2<CI extends CommandInvocation> implements Command<CI> {

        @Option(shortName = 'd', description = "display help instead of ring bell")
        private String display;

        @Option(shortName = 'V', description = "output version information and exit")
        private String version;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "test", description = "more [options] file...")
    public class Test3<CI extends CommandInvocation> implements Command<CI> {

        @Option(shortName = 't', name = "target", description = "target directory")
        private String target;

        @Option(shortName = 'e', description = "test run")
        private String test;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }
}

