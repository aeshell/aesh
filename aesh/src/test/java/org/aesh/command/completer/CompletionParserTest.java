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
package org.aesh.command.completer;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ParsedCommand;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.parser.CompleteStatus;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.readline.AeshContext;
import org.aesh.readline.DefaultAeshContext;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CompletionParserTest {

    private final AeshContext aeshContext = new DefaultAeshContext();

    @Test
    public void testNewCompletionParser() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>().create(new ParseCompleteTest1<>()).getParser();

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
    public void testNewCompletionParserOptionInjection() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>().create(new ParseCompleteTest1<>()).getParser();
        InvocationProviders ip = SettingsBuilder.builder().build().invocationProviders();
        AeshCompleteOperation co = new AeshCompleteOperation(aeshContext, "test --", 7);

        clp.complete(co, ip);
        assertEquals(5, co.getFormattedCompletionCandidates().size());

        co = new AeshCompleteOperation(aeshContext, "test --X foo --", 15);
        clp.complete(co, ip);
        assertEquals(4, co.getFormattedCompletionCandidates().size());
        assertEquals("foo", ((ParseCompleteTest1) clp.getCommand()).X);

        co = new AeshCompleteOperation(aeshContext, "test --X foo -", 14);
        clp.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("-", co.getFormattedCompletionCandidates().get(0));
        assertEquals("foo", ((ParseCompleteTest1) clp.getCommand()).X);

        co = new AeshCompleteOperation(aeshContext, "test --foo", 9);
        clp.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("=", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "test --foo ", 10);
        clp.complete(co, ip);
        assertEquals(2, co.getFormattedCompletionCandidates().size());
        assertEquals("true", co.getFormattedCompletionCandidates().get(0));
        assertEquals("false", co.getFormattedCompletionCandidates().get(1));

        co = new AeshCompleteOperation(aeshContext, "test --foo=", 10);
        clp.complete(co, ip);
        assertEquals(2, co.getFormattedCompletionCandidates().size());
        assertEquals("true", co.getFormattedCompletionCandidates().get(0));
        assertEquals("false", co.getFormattedCompletionCandidates().get(1));

        co = new AeshCompleteOperation(aeshContext, "test -f ", 7);
        clp.complete(co, ip);
        assertEquals(2, co.getFormattedCompletionCandidates().size());
        assertEquals("true", co.getFormattedCompletionCandidates().get(0));
        assertEquals("false", co.getFormattedCompletionCandidates().get(1));

        co = new AeshCompleteOperation(aeshContext, "test --foo t", 11);
        clp.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("rue", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "test -f t", 11);
        clp.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("rue", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "test --foo=", 10);
        clp.complete(co, ip);
        assertEquals(2, co.getFormattedCompletionCandidates().size());
        assertEquals("true", co.getFormattedCompletionCandidates().get(0));
        assertEquals("false", co.getFormattedCompletionCandidates().get(1));

        co = new AeshCompleteOperation(aeshContext, "test -f=", 7);
        clp.complete(co, ip);
        assertEquals(2, co.getFormattedCompletionCandidates().size());
        assertEquals("true", co.getFormattedCompletionCandidates().get(0));
        assertEquals("false", co.getFormattedCompletionCandidates().get(1));

        co = new AeshCompleteOperation(aeshContext, "test --foo=f", 12);
        clp.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("alse", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "test -f=f", 10);
        clp.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("alse", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "test --com", 10);
        clp.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("plex-value=", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "test --complex-value", 20);
        clp.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("=", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "test --complex-value=", 21);
        clp.complete(co, ip);
        assertEquals(0, co.getFormattedCompletionCandidates().size());

        co = new AeshCompleteOperation(aeshContext, "test --complex-value=\'foo\\ bar bar' ", 36);
        clp.complete(co, ip);
        assertEquals(2, co.getFormattedCompletionCandidates().size());
        assertEquals("foo\\ bar bar", ((ParseCompleteTest1) clp.getCommand()).complexValue);

        co = new AeshCompleteOperation(aeshContext, "test XX", 10);
        clp.complete(co, ip);
        assertEquals(0, co.getFormattedCompletionCandidates().size());

        co = new AeshCompleteOperation(aeshContext, "test -e=foo", 10);
        clp.complete(co, ip);
        assertEquals(0, co.getFormattedCompletionCandidates().size());

    }

    @Test
    public void testNewCompletionParserArgumentInjection() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>().create(new ParseCompleteTest2<>()).getParser();
        InvocationProviders ip = SettingsBuilder.builder().build().invocationProviders();
        AeshCompleteOperation co = new AeshCompleteOperation(aeshContext, "test ", 5);

        clp.complete(co, ip);
        assertEquals(4, co.getFormattedCompletionCandidates().size());

        CommandLineParser<CommandInvocation> clp2 = new AeshCommandContainerBuilder<>().create(new ParseCompleteTest1<>()).getParser();

        co = new AeshCompleteOperation(aeshContext, "test ", 5);
        clp2.complete(co, ip);
        assertEquals(2, co.getFormattedCompletionCandidates().size());
        assertEquals("one!", co.getFormattedCompletionCandidates().get(0));
        assertEquals("two!", co.getFormattedCompletionCandidates().get(1));

        co = new AeshCompleteOperation(aeshContext, "test o", 6);
        clp2.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("ne!", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "test one! ", 10);
        clp2.complete(co, ip);
        assertEquals(2, co.getFormattedCompletionCandidates().size());
        assertEquals("one!", co.getFormattedCompletionCandidates().get(0));
        assertEquals("two!", co.getFormattedCompletionCandidates().get(1));

        co = new AeshCompleteOperation(aeshContext, "test one! on", 12);
        clp2.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("e!", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "test --X foo ", 13);
        clp2.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("BAR!", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "test --X foo B", 14);
        clp2.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("AR!", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "test one! bar", 13);
        clp2.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("\\ 2\\ 3", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "test one! bar\\ 2", 15);
        clp2.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("\\ 3", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "test one! bar\\ 2\\ ", 17);
        clp2.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("3", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "test one! --", 16);
        clp2.complete(co, ip);
        assertEquals(5, co.getFormattedCompletionCandidates().size());

        co = new AeshCompleteOperation(aeshContext, "test one! --f", 16);
        clp2.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("oo=", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "test --X foo -- ", 20);
        clp2.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("BAR!", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "test --X foo -- --", 22);
        clp2.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("--", clp2.getProcessedCommand().getArguments().getValue());

    }


    @Test
    public void testParseCompleteObject3() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>().create(new ParseCompleteTest3<>()).getParser();
        InvocationProviders ip = SettingsBuilder.builder().build().invocationProviders();
        AeshCompleteOperation co = new AeshCompleteOperation(aeshContext, "test -v 1,2,3,", 100);

        clp.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("4", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "test -v 1,2,3,4", 100);
        clp.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("", co.getFormattedCompletionCandidates().get(0));
        assertEquals(',', co.getSeparator());

        co = new AeshCompleteOperation(aeshContext, "test -v 1,2,3,4 -", 100);
        clp.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("-", co.getFormattedCompletionCandidates().get(0));

        clp.parse("test -v 1,2,3,4 foo -D=foo1");
        assertEquals("foo1", clp.getProcessedCommand().findOption("D").getValue());
        assertEquals("foo", clp.getProcessedCommand().getArgument().getValue());

        clp.parse("test -v 1,2,3,4 foo -D=foo1 foo2");
        assertTrue(clp.getProcessedCommand().parserExceptions().size() > 0);

        co = new AeshCompleteOperation(aeshContext, "test -v 1,2,3,4 -D=foot B", 100);
        clp.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("AR", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "test -b ARG --", 100);
        clp.complete(co, ip);
        assertEquals(3, co.getFormattedCompletionCandidates().size());

        co = new AeshCompleteOperation(aeshContext, "test --bool", 100);
        clp.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals(" ", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "test --bool --", 100);
        clp.complete(co, ip);
        assertEquals(3, co.getFormattedCompletionCandidates().size());
        assertTrue(co.getCompletionCandidates().get(0).getCharacters().startsWith("--"));
    }

    @Test
    public void testParseCompleteObject4() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>().create(new ParseCompleteTest4<>()).getParser();
        InvocationProviders ip = SettingsBuilder.builder().build().invocationProviders();
        AeshCompleteOperation co = new AeshCompleteOperation(aeshContext, "test ", 100);

        clp.complete(co, ip);
        assertEquals(0, co.getFormattedCompletionCandidates().size());
    }

    @Test
    public void testParseCompleteObject6() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>().create(new ParseCompleteTest6<>()).getParser();
        InvocationProviders ip = SettingsBuilder.builder().build().invocationProviders();
        AeshCompleteOperation co = new AeshCompleteOperation(aeshContext, "test foo bar", 100);

        clp.complete(co, ip);
        assertEquals(0, co.getFormattedCompletionCandidates().size());
    }

    @Test
    public void testArgumentNotRequired() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>().create(new ParseCompleteTest3<>()).getParser();
        InvocationProviders ip = SettingsBuilder.builder().build().invocationProviders();
        AeshCompleteOperation co = new AeshCompleteOperation(aeshContext, "test ", 100);

        clp.complete(co, ip);
        assertEquals(4, co.getFormattedCompletionCandidates().size());
    }

    @Test
    public void testGroupCompletion() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>().create(new ParseCompleteGroupTest<>()).getParser();
        InvocationProviders ip = SettingsBuilder.builder().build().invocationProviders();
        AeshCompleteOperation co = new AeshCompleteOperation(aeshContext, "group child1 --en", 100);

        clp.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("able=", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "group child1 --enable foo ", 100);
        clp.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("--help=", co.getFormattedCompletionCandidates().get(0));

        co = new AeshCompleteOperation(aeshContext, "group child2 --", 100);
        clp.complete(co, ip);
        assertEquals(2, co.getFormattedCompletionCandidates().size());
        assertEquals("--print=", co.getCompletionCandidates().get(0).getCharacters());
        assertEquals("--help=", co.getCompletionCandidates().get(1).getCharacters());

        co = new AeshCompleteOperation(aeshContext, "group xx ", 100);
        clp.complete(co, ip);
        assertEquals(0, co.getFormattedCompletionCandidates().size());

        co = new AeshCompleteOperation(aeshContext, "group xx yy ", 100);
        clp.complete(co, ip);
        assertEquals(0, co.getFormattedCompletionCandidates().size());

        co = new AeshCompleteOperation(aeshContext, "group xx yy", 100);
        clp.complete(co, ip);
        assertEquals(0, co.getFormattedCompletionCandidates().size());

        co = new AeshCompleteOperation(aeshContext, "group xx", 100);
        clp.complete(co, ip);
        assertEquals(0, co.getFormattedCompletionCandidates().size());
     }

    @Test
    public void testSpaceQuoteCompletion() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>().create(new ParseSpaceTest<>()).getParser();
        InvocationProviders ip = SettingsBuilder.builder().build().invocationProviders();
        AeshCompleteOperation co = new AeshCompleteOperation(aeshContext, "test \"", 100);

        clp.complete(co, ip);
        assertEquals(1, co.getFormattedCompletionCandidates().size());
        assertEquals("foo bar", co.getFormattedCompletionCandidates().get(0));
    }

    @CommandDefinition(name = "test", description = "a simple test1")
    public class ParseCompleteTest1<CI extends CommandInvocation> extends TestCommand<CI> {

        @Option(name = "X", description = "enable X")
        private String X;

        @Option(shortName = 'f', name = "foo", description = "enable foo")
        private Boolean foo;

        @Option(shortName = 'e', name = "equal", description = "enable equal", required = true)
        private String equal;

        @Option(shortName = 'D', description = "define properties", required = true)
        private String define;

        @Option(name = "complex-value", shortName = 'c')
        private String complexValue;

        @Arguments(completer = ParseTestCompleter.class)
        private List<String> arguments;

    }

    @CommandDefinition(name = "test", description = "a simple test2")
    public class ParseCompleteTest2<CI extends CommandInvocation> extends TestCommand<CI> {

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

    @CommandDefinition(name = "test", description = "a simple test3")
    public class ParseCompleteTest3<CI extends CommandInvocation> extends TestCommand<CI> {

        @Option(shortName = 'X', description = "enable X")
        private String X;

        @OptionList(shortName = 'v', name = "value", description = "enable equal", completer = ValueTestCompleter.class)
        private List<String> values;

        @Option(shortName = 'b', hasValue = false)
        private boolean bool;

        @Option(shortName = 'D', description = "define properties",
                required = true)
        private String define;

        @Argument(completer = ArgTestCompleter.class)
        private String arg;
    }

    @CommandDefinition(name = "test", description = "a simple test4")
    public class ParseCompleteTest4<CI extends CommandInvocation> extends TestCommand<CI> {

        @Option(shortName = 'r', activator = Test4Activator.class)
        private String required;

        @Argument
        private String arg;

    }

    @CommandDefinition(name = "test", description = "a simple test3")
    public class ParseCompleteTest5<CI extends CommandInvocation> extends TestCommand<CI> {

        @Option(shortName = 'X', description = "enable X")
        private String X;

        @OptionList(shortName = 'v', name = "value", valueSeparator = ':')
        private List<String> values;

        @Option(shortName = 'b', hasValue = false)
        private boolean bool;

        @Option(shortName = 'D', description = "define properties",
                required = true)
        private String define;

        @Argument(completer = ArgTestCompleter.class)
        private String arg;
    }

    @CommandDefinition(name = "test", description = "a simple test6")
    public class ParseCompleteTest6<CI extends CommandInvocation> extends TestCommand<CI> {

        @Option(description = "define properties")
        private String define;

        @Argument(completer = ArgTestCompleter.class)
        private String arg;
    }

    public class Test4Activator implements OptionActivator {

        @Override
        public boolean isActivated(ParsedCommand parsedCommand) {
            //needs an argument to be activated
            return parsedCommand.argument() != null && parsedCommand.argument().value() != null;
        }
    }



    @GroupCommandDefinition(name = "group", description = "groups",
            groupCommands = {ParseCompleteGroupChild1.class, ParseCompleteGroupChild2.class})
    public class ParseCompleteGroupTest<CI extends CommandInvocation> extends TestCommand<CI> {

    }

    @CommandDefinition(name = "child1", description = "im child1")
    public class ParseCompleteGroupChild1 extends TestCommand<CommandInvocation> {
        @Option
        private String enable;

        @Option
        boolean help;
    }

    @CommandDefinition(name = "child2", description = "im child2")
    public class ParseCompleteGroupChild2 extends TestCommand<CommandInvocation> {
        @Option
        private String print;

        @Option
        private String help;
    }

    public class TestCommand<CI extends CommandInvocation> implements Command<CI> {
        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    public class ParseTestCompleter implements OptionCompleter<CompleterInvocation> {
        @Override
        public void complete(CompleterInvocation completerInvocation) {
            ParseCompleteTest1 test1 = (ParseCompleteTest1) completerInvocation.getCommand();
            if (test1.X != null && test1.X.equals("foo"))
                completerInvocation.addCompleterValue("BAR!");
            else if (completerInvocation.getGivenCompleteValue() == null ||
                    completerInvocation.getGivenCompleteValue().length() == 0) {
                completerInvocation.addCompleterValue("one!");
                completerInvocation.addCompleterValue("two!");
            }
            else if ("one!".startsWith(completerInvocation.getGivenCompleteValue())) {
                completerInvocation.addCompleterValue("one!");
            }
            else if ("bar".startsWith(completerInvocation.getGivenCompleteValue())) {
                completerInvocation.addCompleterValue("bar 2 3");
            }
            else if ("bar 2".startsWith(completerInvocation.getGivenCompleteValue())) {
                completerInvocation.addCompleterValue("bar 2 3");
            }
            else if ("bar 2 ".startsWith(completerInvocation.getGivenCompleteValue())) {
                completerInvocation.addCompleterValue("bar 2 3");
            }
        }
    }

    public class ValueTestCompleter implements OptionCompleter<CompleterInvocation> {
        @Override
        public void complete(CompleterInvocation completerInvocation) {
            ParseCompleteTest3 test3 = (ParseCompleteTest3) completerInvocation.getCommand();
            if(completerInvocation.getGivenCompleteValue() != null &&
                    completerInvocation.getGivenCompleteValue().length() > 0)
                completerInvocation.addCompleterValue(completerInvocation.getGivenCompleteValue());
            else if(test3.values != null)
                completerInvocation.addCompleterValue(String.valueOf(test3.values.size() + 1));
            else
                completerInvocation.addCompleterValue("1");
        }
    }

    public class ArgTestCompleter implements OptionCompleter<CompleterInvocation> {
        @Override
        public void complete(CompleterInvocation completerInvocation) {
            if(completerInvocation.getGivenCompleteValue() != null &&
                    completerInvocation.getGivenCompleteValue().length() > 0)
                completerInvocation.addCompleterValue("BAR");
        }
    }

    @CommandDefinition(name = "test", description = "a simple test4")
    public class ParseSpaceTest<CI extends CommandInvocation> extends TestCommand<CI> {

        @Option(shortName = 'r', activator = Test4Activator.class)
        private String required;

        @Argument(completer = SpaceArgumentCompleter.class)
        private String arg;
    }

    public class SpaceArgumentCompleter implements OptionCompleter<CompleterInvocation> {

        @Override
        public void complete(CompleterInvocation completerInvocation) {
            if(completerInvocation.getGivenCompleteValue() == null ||
                    completerInvocation.getGivenCompleteValue().length() == 0)
                completerInvocation.addCompleterValue("foo bar");

            else if("foo ".startsWith(completerInvocation.getGivenCompleteValue()))
                completerInvocation.addCompleterValue("foo bar");
        }
    }
}

