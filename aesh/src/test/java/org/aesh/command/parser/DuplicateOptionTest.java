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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import org.aesh.AeshRuntimeRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.Toggle;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
import org.junit.Test;

/**
 * A repeated single-value option keeps its last value. With
 * {@code failOnDuplicateOptions} the repeat is a usage error instead.
 */
public class DuplicateOptionTest {

    @Test
    public void testLastValueWinsWithEquals() throws Exception {
        RepeatCommand command = execute(new RepeatCommand(), "c --x=a --x=b --n=1 --n=2");

        assertEquals("b", command.x);
        assertEquals(Integer.valueOf(2), command.n);
    }

    @Test
    public void testLastValueWinsWithSpace() throws Exception {
        RepeatCommand command = execute(new RepeatCommand(), "c --x a --x b");

        assertEquals("b", command.x);
    }

    @Test
    public void testLastValueWinsAcrossShortAndLongForm() throws Exception {
        RepeatCommand command = execute(new RepeatCommand(), "c -x a --x=b");

        assertEquals("b", command.x);
    }

    @Test
    public void testNegatedPairLastWins() throws Exception {
        RepeatCommand command = execute(new RepeatCommand(), "c --verbose --no-verbose");

        assertFalse(command.verbose);

        command = execute(new RepeatCommand(), "c --no-verbose --verbose");

        assertTrue(command.verbose);
    }

    @Test
    public void testRepeatedFlagStaysAllowed() throws Exception {
        RepeatCommand command = execute(new RepeatCommand(), "c --json --json");

        assertTrue(command.json);
    }

    @Test
    public void testRepeatedListUnaffected() throws Exception {
        RepeatListCommand command = new RepeatListCommand();
        execute(command, "c --tags a --tags b");

        assertEquals(java.util.Arrays.asList("a", "b"), command.tags);
    }

    @Test
    public void testStrictRejectsDuplicate() {
        try {
            execute(new StrictCommand(), "c --x=a --x=b");
            fail("Expected OptionParserException");
        } catch (OptionParserException e) {
            assertTrue("Message was: " + e.getMessage(),
                    e.getMessage().contains("The option --x is given more than once"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testStrictReportsUsageError() {
        CommandResult[] result = new CommandResult[1];
        String stderr = captureStderr(() -> result[0] = AeshRuntimeRunner.builder()
                .command(StrictCommand.class)
                .args("--x=a", "--x=b")
                .execute());

        assertEquals(CommandResult.USAGE_ERROR, result[0]);
        assertTrue("stderr was: " + stderr, stderr.contains("The option --x is given more than once"));
    }

    @Test
    public void testStrictSingleUseIsFine() throws Exception {
        StrictCommand command = execute(new StrictCommand(), "c --x=a");

        assertEquals("a", command.x);
    }

    @Test
    public void testStrictOffByDefault() throws Exception {
        RepeatCommand command = execute(new RepeatCommand(), "c --x=a --x=b");

        assertEquals("b", command.x);
    }

    @Test
    public void testStrictSkippedInCompletionMode() throws Exception {
        org.aesh.command.impl.container.AeshCommandContainerBuilder<CommandInvocation> builder = new org.aesh.command.impl.container.AeshCommandContainerBuilder<>();
        org.aesh.command.impl.parser.CommandLineParser<CommandInvocation> parser = builder.create(new StrictCommand())
                .getParser();

        // Must not record a duplicate error: completing after a repeat is normal
        parser.parse("c --x=a --x=",
                org.aesh.command.impl.parser.CommandLineParser.Mode.COMPLETION);

        for (CommandLineParserException e : parser.getProcessedCommand().parserExceptions()) {
            assertFalse("Completion must not fail on repeats, got: " + e.getMessage(),
                    e.getMessage().contains("more than once"));
        }
    }

    private static <T extends Command<CommandInvocation>> T execute(T command, String... lines) throws Exception {
        org.aesh.command.CommandRuntime<CommandInvocation> runtime = org.aesh.command.AeshCommandRuntimeBuilder
                .<CommandInvocation> builder()
                .commandRegistry(org.aesh.command.impl.registry.AeshCommandRegistryBuilder
                        .<CommandInvocation> builder().command(command).create())
                .build();
        for (String line : lines)
            assertEquals(CommandResult.SUCCESS, runtime.executeCommand(line));
        return command;
    }

    private static String captureStderr(Runnable action) {
        PrintStream original = System.err;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setErr(new PrintStream(baos));
        try {
            action.run();
        } finally {
            System.setErr(original);
        }
        return baos.toString();
    }

    @CommandDefinition(name = "c", description = "Repeatable options")
    public static class RepeatCommand implements Command<CommandInvocation> {
        @Option(shortName = 'x', description = "A value")
        String x;

        @Option(description = "A number")
        Integer n;

        @Option(hasValue = false, negatable = true, description = "Verbose")
        boolean verbose;

        @Option(name = "json", hasValue = false, description = "JSON output")
        boolean json;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "c", description = "Strict repeats", failOnDuplicateOptions = Toggle.ON)
    public static class StrictCommand implements Command<CommandInvocation> {
        @Option(description = "A value")
        String x;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "c", description = "Repeatable lists")
    public static class RepeatListCommand implements Command<CommandInvocation> {
        @OptionList(description = "Tags")
        List<String> tags;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
