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
package org.aesh.cl;

import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.parser.CommandLineCompletionParser;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.parser.CompleteStatus;
import org.aesh.command.impl.parser.ParsedCompleteObject;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.console.AeshContext;
import org.aesh.console.settings.SettingsBuilder;
import org.aesh.io.FileResource;
import org.aesh.io.Resource;
import org.aesh.parser.LineParser;
import org.aesh.parser.ParsedLine;
import org.aesh.util.Config;
import org.junit.Test;

import java.util.List;
import org.aesh.command.CommandException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParseCompleteObjectTest {

    private final AeshContext aeshContext = new AeshContext() {
        @Override
        public Resource getCurrentWorkingDirectory() {
            return new FileResource(Config.getUserDir());
        }
        @Override
        public void setCurrentWorkingDirectory(Resource cwd) {
        }
    };

    @Test
    public void testNewCompletionParser() throws Exception {
        CommandLineParser<ParseCompleteTest1> clp = new AeshCommandContainerBuilder<ParseCompleteTest1>().create(ParseCompleteTest1.class).getParser();
        CommandLineCompletionParser completeParser = clp.getCompletionParser();

        clp.parse("test -e foo1", CommandLineParser.Mode.COMPLETION);
        assertEquals("foo1", clp.getProcessedCommand().findOption("e").getValue());
        assertTrue(clp.getProcessedCommand().findOption("e").isCursorValue());
        assertEquals(CompleteStatus.Status.COMPLETE_OPTION, clp.getProcessedCommand().completeStatus().status());

        clp.parse("test -e foo1 ", CommandLineParser.Mode.COMPLETION);
        assertEquals("foo1", clp.getProcessedCommand().findOption("e").getValue());
        assertFalse(clp.getProcessedCommand().findOption("e").isCursorValue());
        assertEquals(CompleteStatus.Status.COMPLETE_OPTION, clp.getProcessedCommand().completeStatus().status());

        clp.parse("test --X", CommandLineParser.Mode.COMPLETION);
        assertTrue(clp.getProcessedCommand().findOption("e").hasValue());
    }

    @Test
    public void testNewCompletonParserOptionInjection() throws Exception {
        CommandLineParser<ParseCompleteTest1> clp = new AeshCommandContainerBuilder<ParseCompleteTest1>().create(ParseCompleteTest1.class).getParser();
        InvocationProviders ip = SettingsBuilder.builder().build().invocationProviders();
        AeshCompleteOperation co = new AeshCompleteOperation(aeshContext, "test --", 7);

        clp.complete(co, ip);
        assertEquals(4, co.getFormattedCompletionCandidates().size());

        co = new AeshCompleteOperation(aeshContext, "test --X foo --", 15);
        clp.complete(co, ip);
        assertEquals(3, co.getFormattedCompletionCandidates().size());
        assertEquals("foo", clp.getCommand().X);

        co = new AeshCompleteOperation(aeshContext, "test --X foo -", 14);
        clp.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("-", co.getFormattedCompletionCandidates().get(0));
        assertEquals("foo", clp.getCommand().X);

        co = new AeshCompleteOperation(aeshContext, "test --foo ", 10);
        clp.complete(co, ip);
        assertEquals(2, co.getFormattedCompletionCandidates().size());
        assertEquals("true", co.getFormattedCompletionCandidates().get(0));
        assertEquals("false", co.getFormattedCompletionCandidates().get(1));

        co = new AeshCompleteOperation(aeshContext, "test --foo t", 11);
        clp.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("rue", co.getFormattedCompletionCandidates().get(0));

    }


    @Test
    public void testParseCompleteObject() throws Exception {
        CommandLineParser<ParseCompleteTest1> clp = new AeshCommandContainerBuilder<ParseCompleteTest1>().create(ParseCompleteTest1.class).getParser();
        CommandLineCompletionParser completeParser = clp.getCompletionParser();

        ParsedCompleteObject pco = completeParser.findCompleteObject("test -e foo1", 100);
        assertEquals("foo1", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertTrue(pco.isOption());

        pco = completeParser.findCompleteObject("test -X", 100);
        assertFalse(pco.isOption());

        pco = completeParser.findCompleteObject("test -f false --equal tru", 100);
        assertEquals("tru", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertEquals("equal", pco.getName());
        assertTrue(pco.isOption());
        assertFalse(pco.doDisplayOptions());

        pco = completeParser.findCompleteObject("test -f false --equal file\\ with\\ spaces\\ ", 100);
        assertEquals("file with spaces ", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertEquals("equal", pco.getName());
        assertTrue(pco.isOption());
        assertFalse(pco.doDisplayOptions());

        pco = completeParser.findCompleteObject("test -f=true --equal ", 100);
        assertEquals("", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertEquals("equal", pco.getName());
        assertTrue(pco.isOption());
        assertFalse(pco.doDisplayOptions());

        pco = completeParser.findCompleteObject("test -f true --equ ", 100);
        assertFalse(pco.isArgument());
        assertFalse(pco.doDisplayOptions());
        assertFalse(pco.isOption());
        assertFalse(pco.isCompleteOptionName());

        pco = completeParser.findCompleteObject("test --equal true foo.txt", 100);
        assertEquals("foo.txt", pco.getValue());
        //assertEquals(String.class, pco.getStyle());
        assertTrue(pco.isArgument());

        pco = completeParser.findCompleteObject("test -e", 100);
        assertTrue(pco.doDisplayOptions());
        assertTrue(pco.isCompleteOptionName());
        assertEquals("e", pco.getName());
        clp.getProcessedCommand().clear();
        assertEquals("--equal", clp.getProcessedCommand().findPossibleLongNamesWitdDash(pco.getName()).get(0).getCharacters());

        pco = completeParser.findCompleteObject("test --eq", 100);
        assertTrue(pco.doDisplayOptions());
        assertFalse(pco.isCompleteOptionName());
        assertEquals("eq", pco.getName());
        assertEquals(4, pco.getOffset());
        clp.getProcessedCommand().clear();
        assertEquals("--equal", clp.getProcessedCommand().findPossibleLongNamesWitdDash(pco.getName()).get(0).getCharacters());

        clp.getProcessedCommand().clear();
        pco = completeParser.findCompleteObject("test --", 100);
        assertTrue(pco.doDisplayOptions());
        assertEquals("", pco.getName());
        assertEquals(2, pco.getOffset());
        assertEquals(4, clp.getProcessedCommand().getOptionLongNamesWithDash().size());

        pco = completeParser.findCompleteObject("test --equal true  ", 100);
        assertTrue(pco.isArgument());

        pco = completeParser.findCompleteObject("test -f", 100);
        assertTrue(pco.doDisplayOptions());
        assertTrue(pco.isCompleteOptionName());

        pco = completeParser.findCompleteObject("test --equal", 100);
        assertTrue(pco.doDisplayOptions());
        assertTrue(pco.isCompleteOptionName());

        pco = completeParser.findCompleteObject("test --f", 100);
        assertTrue(pco.doDisplayOptions());
        assertFalse(pco.isCompleteOptionName());

        pco = completeParser.findCompleteObject("test ", 100);
        assertTrue(pco.isArgument());
        assertFalse(pco.doDisplayOptions());
        assertFalse(pco.isOption());
        assertTrue(pco.getValue().length() == 0);

        pco = completeParser.findCompleteObject("test a", 100);
        assertTrue(pco.isArgument());
        assertFalse(pco.doDisplayOptions());
        assertFalse(pco.isOption());

        pco = completeParser.findCompleteObject("test a1 b1 ", 100);
        assertTrue(pco.isArgument());
        assertFalse(pco.doDisplayOptions());
        assertFalse(pco.isOption());
        assertTrue(pco.getValue() == null || pco.getValue().length() == 0);

        pco = completeParser.findCompleteObject("test a\\ ", 100);
        assertTrue(pco.isArgument());
        assertFalse(pco.doDisplayOptions());
        assertFalse(pco.isOption());
        assertEquals("a ",  pco.getValue());
    }

    @Test
    public void testParseCompleteObjectWithEquals() throws Exception {
        CommandLineParser<ParseCompleteTest1> clp = new AeshCommandContainerBuilder<ParseCompleteTest1>().create(ParseCompleteTest1.class).getParser();
        CommandLineCompletionParser completeParser = clp.getCompletionParser();

        ParsedCompleteObject pco = completeParser.findCompleteObject("test -e=foo1", 100);
        assertEquals("foo1", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertTrue(pco.isOption());

        pco = completeParser.findCompleteObject("test -f=true --equal=", 100);
        assertEquals("", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertEquals("equal", pco.getName());
        assertTrue(pco.isOption());
        assertFalse(pco.doDisplayOptions());
    }

    @Test
    public void testParseCompleteObject2() throws Exception {
        CommandLineParser clp = new AeshCommandContainerBuilder<ParseCompleteTest2>().create(ParseCompleteTest2.class).getParser();
        CommandLineCompletionParser completeParser = clp.getCompletionParser();

        ParsedCompleteObject pco = completeParser.findCompleteObject("test -e ", 100);
        assertEquals(Boolean.class, pco.getType());
        assertTrue(pco.isOption());

        pco = completeParser.findCompleteObject("test ", 100);
        assertFalse(pco.isOption());
        assertFalse(pco.isCompleteOptionName());
        assertFalse(pco.isArgument());
        assertTrue(pco.doDisplayOptions());
    }

    @Test
    public void testParseCompleteObject3() throws Exception {
        CommandLineParser clp = new AeshCommandContainerBuilder<ParseCompleteTest3>().create(ParseCompleteTest3.class).getParser();
        CommandLineCompletionParser completeParser = clp.getCompletionParser();

        ParsedCompleteObject pco = completeParser.findCompleteObject("test -v 1 2 3 ", 100);
        assertEquals(String.class, pco.getType());
        assertTrue(pco.isOption());
        assertEquals("",pco.getValue());

        pco = completeParser.findCompleteObject("test -v 1 2 3", 100);
        assertEquals(String.class, pco.getType());
        assertTrue(pco.isOption());
        assertEquals("3",pco.getValue());
    }

    @Test
    public void testCursorInsideBuffer() throws Exception {
        CommandLineParser clp = new AeshCommandContainerBuilder<ParseCompleteTest1>().create(ParseCompleteTest1.class).getParser();
        CommandLineCompletionParser completeParser = clp.getCompletionParser();

        ParsedCompleteObject pco = completeParser.findCompleteObject("test -e foo1  asdfjeaasdfae", 12);
        assertEquals("foo1", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertTrue(pco.isOption());

        pco = completeParser.findCompleteObject("test --equal tru  -f false ", 16);
        assertEquals("tru", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertEquals("equal", pco.getName());
        assertTrue(pco.isOption());
        assertFalse(pco.doDisplayOptions());

        pco = completeParser.findCompleteObject("test -f false --equal file\\ with\\ spaces\\ ", 100);
        assertEquals("file with spaces ", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertEquals("equal", pco.getName());
        assertTrue(pco.isOption());
        assertFalse(pco.doDisplayOptions());

        pco = completeParser.findCompleteObject("test --equal  -f=true ", 13);
        assertEquals("", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertEquals("equal", pco.getName());
        assertTrue(pco.isOption());
        assertFalse(pco.doDisplayOptions());

        pco = completeParser.findCompleteObject("test --equ  -f true", 11);
        assertFalse(pco.isArgument());
        assertFalse(pco.doDisplayOptions());
        assertFalse(pco.isOption());
        assertFalse(pco.isCompleteOptionName());

        pco = completeParser.findCompleteObject("test --equal true foo.txt  bar.txt", 25);
        assertEquals("foo.txt", pco.getValue());
        assertTrue(pco.isArgument());
    }

    @Test
    public void testCompletionWithNoArguments() throws Exception {
        CommandLineParser clp = new AeshCommandContainerBuilder<ParseCompleteTest2>().create(ParseCompleteTest2.class).getParser();
        CommandLineCompletionParser completeParser = clp.getCompletionParser();

        ParsedCompleteObject pco = completeParser.findCompleteObject("test ", 4);
        assertFalse(pco.isArgument());
        assertTrue(pco.doDisplayOptions());

        pco = completeParser.findCompleteObject("test -X foo1 ", 13);
        assertFalse(pco.isArgument());
        assertTrue(pco.doDisplayOptions());
    }

    @Test
    public void testGroupCompletion() throws Exception {
        CommandLineParser clp = new AeshCommandContainerBuilder<ParseCompleteGroupTest>().create(ParseCompleteGroupTest.class).getParser();
        CommandLineCompletionParser completeParser = clp.getCompletionParser();

        ParsedCompleteObject pco = completeParser.findCompleteObject("group child1 --en", 20);
        assertTrue(pco.doDisplayOptions());
        assertFalse(pco.isCompleteOptionName());
        assertEquals("en", pco.getName());
        assertEquals(4, pco.getOffset());

        clp.getProcessedCommand().clear();
        pco = completeParser.findCompleteObject("group child1 --enable foo", 25);
        assertEquals("foo", pco.getValue());
        assertTrue(pco.isOption());
        assertEquals(String.class, pco.getType());

        pco = completeParser.findCompleteObject("group child1 --en", 20);
        assertTrue(pco.doDisplayOptions());
        assertEquals("", pco.getValue());
        assertFalse(pco.isCompleteOptionName());
        assertEquals("en", pco.getName());
        assertEquals(4, pco.getOffset());


        clp.getProcessedCommand().clear();
        pco = completeParser.findCompleteObject("group child2 --", 100);
        assertTrue(pco.doDisplayOptions());
        assertEquals("", pco.getName());
        assertEquals(2, pco.getOffset());


        clp.getProcessedCommand().clear();
        pco = completeParser.findCompleteObject("group child1 ", 100);
        assertFalse(pco.isOption());
        assertFalse(pco.isCompleteOptionName());
        assertFalse(pco.isArgument());
        assertTrue(pco.doDisplayOptions());

        clp.getProcessedCommand().clear();
        pco = completeParser.findCompleteObject("group child1 --enable foo --", 100);
        assertEquals("", pco.getValue());
        assertTrue(pco.doDisplayOptions());

    }

    @CommandDefinition(name = "test", description = "a simple test")
    public class ParseCompleteTest1 extends TestCommand {

        @Option(name = "X", description = "enable X")
        private String X;

        @Option(shortName = 'f', name = "foo", description = "enable foo")
        private Boolean foo;

        @Option(shortName = 'e', name = "equal", description = "enable equal", required = true)
        private String equal;

        @Option(shortName = 'D', description = "define properties", required = true)
        private String define;

        @Arguments
        private List<String> arguments;

    }

    @CommandDefinition(name = "test", description = "a simple test")
    public class ParseCompleteTest2 extends TestCommand {

        @Option(shortName = 'X', description = "enable X")
        private String X;

        @Option(shortName = 'f', name = "foo", description = "enable foo")
        private String foo;

        @Option(shortName = 'e', name = "equal", description = "enable equal", required = true, defaultValue = "false")
        private Boolean equal;

        @Option(shortName = 'D', description = "define properties",
                required = true)
        private String define;
    }

    @CommandDefinition(name = "test", description = "a simple test")
    public class ParseCompleteTest3 extends TestCommand {

        @Option(shortName = 'X', description = "enable X")
        private String X;

        @OptionList(shortName = 'v', name = "value", description = "enable equal")
        private List<String> values;

        @Option(shortName = 'D', description = "define properties",
                required = true)
        private String define;
    }

    @GroupCommandDefinition(name = "group", description = "groups",
            groupCommands = {ParseCompleteGroupChild1.class, ParseCompleteGroupChild2.class})
    public class ParseCompleteGroupTest extends TestCommand {

    }

    @CommandDefinition(name = "child1", description = "im child1")
    public class ParseCompleteGroupChild1 extends TestCommand {
        @Option
        private String enable;

        @Option boolean help;
    }

    @CommandDefinition(name = "child2", description = "im child2")
    public class ParseCompleteGroupChild2 extends TestCommand {
        @Option
        private String print;

        @Option
        private String help;
    }

    public class TestCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }
}
