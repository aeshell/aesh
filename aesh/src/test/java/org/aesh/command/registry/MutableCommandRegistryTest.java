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
package org.aesh.command.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.MutableCommandRegistryImpl;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.parser.LineParser;
import org.aesh.parser.ParsedLine;
import org.aesh.terminal.formatting.TerminalString;
import org.junit.Test;

/**
 * @author Aesh team
 */
public class MutableCommandRegistryTest {

    @Test
    public void testFindCommandNames() throws CommandRegistryException {

        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        registry.addCommand(Command1.class);
        registry.addCommand(Command2.class);
        registry.addCommand(Command3.class);
        registry.addCommand(GroupCommand1.class);

        AeshCompleteOperation co = new AeshCompleteOperation(null, "fo", 3);
        ParsedLine parsedLine = new LineParser()
                .input(co.getBuffer())
                .cursor(co.getCursor())
                .parseBrackets(true)
                .parse();
        registry.completeCommandName(co, parsedLine);
        assertEquals(1, co.getCompletionCandidates().size());
        assertEquals("foo", co.getCompletionCandidates().get(0).toString());

        co = new AeshCompleteOperation(null, "foo", 4);
        parsedLine = new LineParser()
                .input(co.getBuffer())
                .cursor(co.getCursor())
                .parseBrackets(true)
                .parse();
        registry.completeCommandName(co, parsedLine);
        assertEquals(1, co.getCompletionCandidates().size());
        assertEquals("foo", co.getCompletionCandidates().get(0).toString());

        co = new AeshCompleteOperation(null, "", 0);
        parsedLine = new LineParser()
                .input(co.getBuffer())
                .cursor(co.getCursor())
                .parseBrackets(true)
                .parse();
        registry.completeCommandName(co, parsedLine);
        assertEquals(4, co.getCompletionCandidates().size());
        assertTrue(co.getCompletionCandidates().contains(new TerminalString("bar", true)));
        assertTrue(co.getCompletionCandidates().contains(new TerminalString("group", true)));
    }

    @CommandDefinition(name = "foo", description = "")
    public class Command1 implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "bar", description = "")
    public class Command2 implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "help", description = "")
    public class Command3 implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "group", description = "", groupCommands = { Command3.class })
    public class GroupCommand1 implements Command {

        @Option
        private boolean help;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testClassRegistrationDefersBuild() throws Exception {
        CountRootCommand.constructions = 0;
        CountSubCommand.constructions = 0;

        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        registry.addCommand(CountRootCommand.class);

        assertTrue(registry.contains("countroot"));
        assertTrue(registry.getAllCommandNames().contains("countroot"));
        assertEquals("registration must not instantiate", 0, CountRootCommand.constructions);
        assertEquals("registration must not build children either", 0, CountSubCommand.constructions);
    }

    @Test
    public void testRunOnceExecutionBuildsOnce() {
        CountRootCommand.constructions = 0;
        CountSubCommand.constructions = 0;

        CommandResult result = org.aesh.AeshRuntimeRunner.builder().command(CountRootCommand.class)
                .args("countsub").execute();

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("root must construct exactly once per run-once execution", 1,
                CountRootCommand.constructions);
        assertEquals("selected child must construct exactly once", 1, CountSubCommand.constructions);
    }

    @Test
    public void testUnannotatedClassFailsFast() {
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        try {
            registry.addCommand(NotACommand.class);
            assertTrue("unannotated class must fail at registration", false);
        } catch (CommandRegistryException expected) {
        }
    }

    @Test
    public void testBrokenDefinitionFailsAtFirstUse() throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        // Valid annotation: registration records name and aliases without validating fields
        registry.addCommand(BrokenCommand.class);
        assertTrue(registry.contains("broken"));
        try {
            registry.createExecutionContainer("broken");
            assertTrue("broken definition must fail on first build", false);
        } catch (CommandNotFoundException expected) {
            assertNotNull("first-use failure must chain the build cause",
                    expected.getCause());
        }
    }

    @Test
    public void testAliasResolvesWithoutPriorBuild() throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        registry.addCommand(AliasedCommand.class);

        assertEquals("aliased",
                registry.getCommandByAlias("al").getParser().getProcessedCommand().name());
    }

    @Test
    public void testRemoveDeferredCommand() throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        registry.addCommand(AliasedCommand.class);
        assertTrue(registry.contains("aliased"));
        assertTrue(registry.contains("al"));

        registry.removeCommand("aliased");
        assertFalse(registry.contains("aliased"));
        assertFalse("recorded aliases go with the record", registry.contains("al"));
        assertFalse(registry.getAllCommandNames().contains("aliased"));
    }

    @Test
    public void testDuplicateRegistrationIgnored() throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        registry.addCommand(CountRootCommand.class);
        registry.addCommand(CountRootCommand.class);
        registry.addCommand(RenamedRootCommand.class);

        List<String> names = new java.util.ArrayList<>(registry.getAllCommandNames());
        int count = 0;
        for (String name : names) {
            if (name.equals("countroot"))
                count++;
        }
        assertEquals(1, count);
    }

    @CommandDefinition(name = "countroot", description = "", groupCommands = { CountSubCommand.class })
    public static class CountRootCommand implements Command<CommandInvocation> {
        static int constructions;

        public CountRootCommand() {
            constructions++;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "countsub", description = "")
    public static class CountSubCommand implements Command<CommandInvocation> {
        static int constructions;

        public CountSubCommand() {
            constructions++;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "countroot", description = "")
    public static class RenamedRootCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class NotACommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "broken", description = "")
    public static class BrokenCommand implements Command<CommandInvocation> {
        @OptionList
        private String broken;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "aliased", description = "", aliases = { "al" })
    public static class AliasedCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

}
