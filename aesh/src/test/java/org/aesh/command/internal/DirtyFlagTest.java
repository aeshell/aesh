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
package org.aesh.command.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.parser.CompleteStatus;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.console.AeshContext;
import org.aesh.console.DefaultAeshContext;
import org.junit.Test;

/**
 * Tests for the dirty flag optimization (#576) that skips
 * {@code ProcessedCommand.clear()} on freshly constructed commands.
 *
 * @author Aesh team
 */
public class DirtyFlagTest {

    private final InvocationProviders invocationProviders = new AeshInvocationProviders();
    private final AeshContext aeshContext = new DefaultAeshContext();

    // ---- Test: fresh command is not dirty ----

    @Test
    public void testFreshCommandIsNotDirty() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>()
                .create(new DirtyTestCmd<>()).getParser();
        ProcessedCommand<?, ?> pc = clp.getProcessedCommand();

        assertFalse("Fresh ProcessedCommand should not be dirty", pc.isDirty());
    }

    // ---- Test: command becomes dirty after parse (execution path) ----

    @Test
    public void testCommandIsDirtyAfterParse() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>()
                .create(new DirtyTestCmd<>()).getParser();
        ProcessedCommand<?, ?> pc = clp.getProcessedCommand();

        assertFalse("Before parse, should not be dirty", pc.isDirty());

        clp.parse("dirty-test --name hello world", CommandLineParser.Mode.STRICT);

        assertTrue("After parse, should be dirty", pc.isDirty());
        assertEquals("hello", pc.findLongOptionNoActivatorCheck("name").getValue());
    }

    // ---- Test: clear() resets dirty flag and option values ----

    @Test
    public void testClearResetsDirtyAndValues() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>()
                .create(new DirtyTestCmd<>()).getParser();
        ProcessedCommand<?, ?> pc = clp.getProcessedCommand();

        // First parse sets values
        clp.parse("dirty-test --name hello --verbose world", CommandLineParser.Mode.STRICT);
        assertTrue(pc.isDirty());
        assertEquals("hello", pc.findLongOptionNoActivatorCheck("name").getValue());

        // Explicit clear
        pc.clear();
        assertFalse("After clear, should not be dirty", pc.isDirty());
        assertNull("After clear, option value should be null",
                pc.findLongOptionNoActivatorCheck("name").getValue());
    }

    // ---- Test: second parse clears state from first parse (execution path) ----

    @Test
    public void testSecondParseClearsFirstParseValues() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>()
                .create(new DirtyTestCmd<>()).getParser();
        ProcessedCommand<?, ?> pc = clp.getProcessedCommand();

        // First parse: set --name and --verbose
        clp.parse("dirty-test --name first --verbose firstArg", CommandLineParser.Mode.STRICT);
        assertEquals("first", pc.findLongOptionNoActivatorCheck("name").getValue());
        assertNotNull(pc.findLongOptionNoActivatorCheck("verbose").getValue());

        // Second parse: different options -- first parse values must be cleared
        clp.parse("dirty-test --name second secondArg", CommandLineParser.Mode.STRICT);
        assertEquals("second", pc.findLongOptionNoActivatorCheck("name").getValue());
        // --verbose was NOT set in second parse, so it should be cleared
        assertNull("--verbose from first parse should be cleared",
                pc.findLongOptionNoActivatorCheck("verbose").getValue());
    }

    // ---- Test: first parse on fresh command skips clear (performance check) ----

    @Test
    public void testFirstParseOnFreshCommandSkipsClear() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>()
                .create(new DirtyTestCmd<>()).getParser();
        ProcessedCommand<?, ?> pc = clp.getProcessedCommand();

        // Verify not dirty before first parse
        assertFalse(pc.isDirty());

        // Parse successfully -- this should work even though clear() was a no-op
        clp.parse("dirty-test --name hello world", CommandLineParser.Mode.STRICT);
        assertEquals(0, pc.parserExceptions().size());
        assertEquals("hello", pc.findLongOptionNoActivatorCheck("name").getValue());

        // And populate should also work
        @SuppressWarnings("unchecked")
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> typedPc = (ProcessedCommand<Command<CommandInvocation>, CommandInvocation>) (ProcessedCommand<?, ?>) pc;
        clp.getCommandPopulator().populateObject(typedPc, invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        DirtyTestCmd<?> cmd = (DirtyTestCmd<?>) pc.getCommand();
        assertEquals("hello", cmd.name);
        assertEquals("world", cmd.arg);
    }

    // ---- Test: completion on fresh command skips clear ----

    @Test
    public void testCompletionOnFreshCommandSkipsClear() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>()
                .create(new DirtyTestCmd<>()).getParser();
        ProcessedCommand<?, ?> pc = clp.getProcessedCommand();

        assertFalse("Should not be dirty before completion", pc.isDirty());

        // Run completion parse
        clp.parse("dirty-test --n", CommandLineParser.Mode.COMPLETION);

        assertTrue("Should be dirty after completion parse", pc.isDirty());
        assertNotNull(pc.completeStatus());
        assertEquals(CompleteStatus.Status.LONG_OPTION, pc.completeStatus().status());
    }

    // ---- Test: second completion clears state from first completion ----

    @Test
    public void testSecondCompletionClearsFirstCompletionState() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>()
                .create(new DirtyTestCmd<>()).getParser();
        ProcessedCommand<?, ?> pc = clp.getProcessedCommand();

        // First completion: partial option
        clp.parse("dirty-test --name hello ", CommandLineParser.Mode.COMPLETION);
        assertEquals("hello", pc.findLongOptionNoActivatorCheck("name").getValue());
        assertTrue(pc.isDirty());

        // Second completion: different input -- first state must be cleared
        clp.parse("dirty-test --verb", CommandLineParser.Mode.COMPLETION);
        assertNull("--name from first completion should be cleared",
                pc.findLongOptionNoActivatorCheck("name").getValue());
        assertEquals(CompleteStatus.Status.LONG_OPTION, pc.completeStatus().status());
    }

    // ---- Test: parse after completion clears completion state ----

    @Test
    public void testParseAfterCompletionClearsState() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>()
                .create(new DirtyTestCmd<>()).getParser();
        ProcessedCommand<?, ?> pc = clp.getProcessedCommand();

        // Completion first
        clp.parse("dirty-test --name hello ", CommandLineParser.Mode.COMPLETION);
        assertEquals("hello", pc.findLongOptionNoActivatorCheck("name").getValue());
        assertNotNull(pc.completeStatus());

        // Now full parse -- completion state should be cleared
        clp.parse("dirty-test --verbose world", CommandLineParser.Mode.STRICT);
        assertNull("completeStatus should be cleared after strict parse",
                pc.completeStatus());
        assertNull("--name from completion should be cleared",
                pc.findLongOptionNoActivatorCheck("name").getValue());
        assertNotNull("--verbose should be set",
                pc.findLongOptionNoActivatorCheck("verbose").getValue());
    }

    // ---- Test: completion after parse clears parse state ----

    @Test
    public void testCompletionAfterParseClearsParseState() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>()
                .create(new DirtyTestCmd<>()).getParser();
        ProcessedCommand<?, ?> pc = clp.getProcessedCommand();

        // Full parse first
        clp.parse("dirty-test --name hello world", CommandLineParser.Mode.STRICT);
        assertEquals("hello", pc.findLongOptionNoActivatorCheck("name").getValue());
        assertEquals(0, pc.parserExceptions().size());

        // Now completion -- parse state should be cleared
        clp.parse("dirty-test --verb", CommandLineParser.Mode.COMPLETION);
        assertNull("--name from parse should be cleared",
                pc.findLongOptionNoActivatorCheck("name").getValue());
        assertEquals(CompleteStatus.Status.LONG_OPTION, pc.completeStatus().status());
    }

    // ---- Test: markDirty/isDirty lifecycle ----

    @Test
    public void testMarkDirtyLifecycle() throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>()
                .create(new DirtyTestCmd<>()).getParser();
        ProcessedCommand<?, ?> pc = clp.getProcessedCommand();

        // Initially clean
        assertFalse(pc.isDirty());

        // Manual markDirty
        pc.markDirty();
        assertTrue(pc.isDirty());

        // clear() resets
        pc.clear();
        assertFalse(pc.isDirty());

        // clear() on clean is a no-op (no exception, fast path)
        pc.clear();
        assertFalse(pc.isDirty());
    }

    // ---- Test command ----

    @CommandDefinition(name = "dirty-test", description = "Command for dirty flag testing")
    public static class DirtyTestCmd<CI extends CommandInvocation> implements Command<CI> {
        @Option(shortName = 'n', description = "Name")
        private String name;

        @Option(shortName = 'v', hasValue = false, description = "Verbose")
        private boolean verbose;

        @Argument(description = "Input file")
        private String arg;

        @Override
        public CommandResult execute(CI ci) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }
}
