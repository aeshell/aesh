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
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.parser.ParserGenerator;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
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

        Test1 test1 = new Test1();
        CommandLineParser parser = ParserGenerator.generateCommandLineParser(test1).getParser();

        assertEquals("a simple test", parser.getProcessedCommand().getDescription());
        List<ProcessedOption> options = parser.getProcessedCommand().getOptions();
        assertEquals("f", options.get(0).getShortName());
        assertEquals("foo", options.get(0).getName());
        assertEquals("e", options.get(1).getShortName());
        assertEquals("enable e", options.get(1).getDescription());
        assertTrue(options.get(1).hasValue());
        assertTrue(options.get(1).isRequired());
        assertEquals("bar", options.get(2).getName());
        assertFalse(options.get(2).hasValue());

        Test2 test2 = new Test2();
        parser = ParserGenerator.generateCommandLineParser(test2).getParser();
        assertEquals("more [options] file...", parser.getProcessedCommand().getDescription());
        options = parser.getProcessedCommand().getOptions();
        assertEquals("d", options.get(0).getShortName());
        assertEquals("V", options.get(1).getShortName());

        parser = ParserGenerator.generateCommandLineParser(Test3.class).getParser();
        options = parser.getProcessedCommand().getOptions();
        assertEquals("t", options.get(0).getShortName());
        assertEquals("e", options.get(1).getShortName());

    }

    @CommandDefinition(name = "test", description = "a simple test")
    public class Test1 implements Command {
        @Option(shortName = 'f', name = "foo", description = "enable foo")
        private String foo;

        @Option(shortName = 'e', description = "enable e", required = true)
        private String e;

        @Option(description = "has enabled bar", hasValue = false)
        private Boolean bar;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "test", description = "more [options] file...")
    public class Test2 implements Command {

        @Option(shortName = 'd', description = "display help instead of ring bell")
        private String display;

        @Option(shortName = 'V', description = "output version information and exit")
        private String version;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "test", description = "more [options] file...")
    public class Test3 implements Command {

        @Option(shortName = 't', name = "target", description = "target directory")
        private String target;

        @Option(shortName = 'e', description = "test run")
        private String test;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }
}

