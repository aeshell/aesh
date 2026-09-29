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
package org.aesh.command.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.operator.OperatorType;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.parser.LineParser;
import org.aesh.parser.ParsedLine;
import org.aesh.parser.ParserStatus;
import org.junit.Test;

/**
 * Quoted-word boundaries and empty arguments (#651).
 * <p>
 * Closing quotes do not split words ({@code ab"cd"ef} is one token) and a
 * quote pair closed over nothing still denotes a token ({@code probe ''}
 * keeps its empty argument). Operator characters inside quotes stay quoted,
 * and empty pre-tokenized words must not crash option parsing.
 */
public class QuotedArgumentsTest {

    @CommandDefinition(name = "greet", description = "greets")
    public static class GreetCommand implements Command<CommandInvocation> {
        @Option(name = "value", description = "value")
        String value;

        @Argument(description = "target")
        String target;

        static volatile String seenValue;
        static volatile String seenTarget;
        static volatile boolean executed;

        static void reset() {
            seenValue = null;
            seenTarget = null;
            executed = false;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            seenValue = value;
            seenTarget = target;
            executed = true;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "collect", description = "collects")
    public static class CollectCommand implements Command<CommandInvocation> {
        @OptionList(name = "items", description = "items")
        public List<String> items;

        @Argument(description = "target")
        public String target;

        static volatile List<String> seenItems;
        static volatile String seenTarget;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            seenItems = items;
            seenTarget = target;
            return CommandResult.SUCCESS;
        }
    }

    private static List<String> wordsOf(String line) {
        ParsedLine parsed = new LineParser().parseLine(line, line.length());
        assertEquals(ParserStatus.OK, parsed.status());
        List<String> words = new java.util.ArrayList<>();
        for (org.aesh.parser.ParsedWord word : parsed.words())
            words.add(word.word());
        return words;
    }

    private static CommandRuntime<CommandInvocation> buildRuntime(Class<? extends Command>... commands)
            throws Exception {
        AeshCommandRegistryBuilder<CommandInvocation> builder = AeshCommandRegistryBuilder
                .<CommandInvocation> builder();
        for (Class<? extends Command> command : commands)
            builder.command(command);
        CommandRegistry<CommandInvocation> registry = builder.create();
        return AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(registry)
                .operators(EnumSet.allOf(OperatorType.class))
                .build();
    }

    @Test
    public void testEmptySingleQuotedArgumentPreserved() {
        assertEquals(java.util.Arrays.asList("probe", ""), wordsOf("probe ''"));
        assertEquals(java.util.Arrays.asList("probe", "", "hello"), wordsOf("probe '' hello"));
    }

    @Test
    public void testAdjacentFragmentsJoin() {
        assertEquals(java.util.Arrays.asList("probe", "abcdef"), wordsOf("probe ab\"cd\"ef"));
        assertEquals(java.util.Arrays.asList("probe", "abcd"), wordsOf("probe 'a'b\"c\"d"));
        assertEquals(java.util.Arrays.asList("ab"), wordsOf("\"a\"b"));
    }

    @Test
    public void testEscapedQuoteInsideQuotes() {
        // Backslash-quote inside quotes stays literal (existing convention),
        // and the word still joins across the closing quote.
        assertEquals(java.util.Arrays.asList("a\\\"b"), wordsOf("\"a\\\"b\""));
    }

    @Test
    public void testOperatorCharacterInsideQuotes() {
        Set<OperatorType> operators = EnumSet.allOf(OperatorType.class);
        List<ParsedLine> lines = new LineParser().parseLine("echo \"a|b\"", 9, false, operators);
        assertEquals(1, lines.size());
        assertEquals(OperatorType.NONE, lines.get(0).operator());
        assertEquals("echo", lines.get(0).words().get(0).word());
        assertEquals("a|b", lines.get(0).words().get(1).word());
    }

    @Test
    public void testEmptyQuotedOptionValueEndToEnd() throws Exception {
        GreetCommand.reset();
        CommandRuntime<CommandInvocation> runtime = buildRuntime(GreetCommand.class);

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("greet --value ''"));
        assertEquals("", GreetCommand.seenValue);
        assertNull(GreetCommand.seenTarget);
    }

    @Test
    public void testEmptyQuotedPositionalEndToEnd() throws Exception {
        GreetCommand.reset();
        CommandRuntime<CommandInvocation> runtime = buildRuntime(GreetCommand.class);

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("greet --value x ''"));
        assertEquals("x", GreetCommand.seenValue);
        assertEquals("", GreetCommand.seenTarget);
    }

    @Test
    public void testJoinedFragmentsEndToEnd() throws Exception {
        GreetCommand.reset();
        CommandRuntime<CommandInvocation> runtime = buildRuntime(GreetCommand.class);

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("greet ab\"cd\"ef"));
        assertNull(GreetCommand.seenValue);
        assertEquals("abcdef", GreetCommand.seenTarget);
    }

    @Test
    public void testEmptyPreTokenizedWordDoesNotCrash() throws Exception {
        CollectCommand.seenItems = null;
        CollectCommand.seenTarget = null;
        CommandRuntime<CommandInvocation> runtime = buildRuntime(CollectCommand.class);

        // Used to die in peekWord().charAt(0) on the empty word. The empty
        // element survives as its own argv slot (the positional), on both
        // the pre-tokenized and the string path.
        assertEquals(CommandResult.SUCCESS,
                runtime.executeCommand("collect", new String[] { "--items", "first", "" }));
        assertEquals(java.util.Arrays.asList("first"), CollectCommand.seenItems);
        assertEquals("", CollectCommand.seenTarget);

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("collect --items first ''"));
        assertEquals(java.util.Arrays.asList("first"), CollectCommand.seenItems);
        assertEquals("", CollectCommand.seenTarget);
    }

    @Test
    public void testCompletionCursorOnEmptyQuotedWord() {
        ParsedLine line = new LineParser().parseLine("probe ''", 8);
        assertEquals("", line.selectedWord().word());
        assertEquals(0, line.wordCursor());
    }
}
