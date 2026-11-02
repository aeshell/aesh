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
package org.aesh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandDefinitionException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.OptionList;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistry;
import org.junit.Test;

/**
 * A command whose definition is invalid reports the definition error, not
 * "Command not found" (#701).
 */
public class AeshRuntimeRunnerInvalidDefinitionTest {

    @Test
    public void testInvalidOptionReportsTheDefinitionError() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        CommandResult result = executeWithStderr(err, AeshRuntimeRunner.builder()
                .command(InvalidOptionListCommand.class)
                .args("--dep", "a,b"));

        assertEquals(CommandResult.FAILURE, result);
        assertTrue(err.toString(), err.toString().contains("invalid-option-list"));
        assertTrue(err.toString(), err.toString().contains("OptionList field must not be a multi-dimensional array"));
        assertFalse(err.toString(), err.toString().contains("Command not found"));
    }

    @Test
    public void testDuplicateArgumentsReportsTheDefinitionError() {
        // A group with an argument used to be invalid, but #702 allows it now;
        // duplicate @Arguments fields are still rejected at build time (#699).
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        CommandResult result = executeWithStderr(err, AeshRuntimeRunner.builder()
                .command(DuplicateArgumentsCommand.class)
                .args("a", "b"));

        assertEquals(CommandResult.FAILURE, result);
        assertTrue(err.toString(),
                err.toString().contains("Only one @Arguments field is allowed per command"));
        assertFalse(err.toString(), err.toString().contains("Command not found"));
    }

    @Test
    public void testCommandNotFoundHandlerIsNotCalled() {
        AtomicBoolean called = new AtomicBoolean();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        CommandResult result = executeWithStderr(err, AeshRuntimeRunner.builder()
                .commandNotFoundHandler((line, output) -> called.set(true))
                .command(InvalidOptionListCommand.class)
                .args());

        assertEquals(CommandResult.FAILURE, result);
        assertFalse("A definition error is not a command that was not found", called.get());
        assertTrue(err.toString(), err.toString().contains("OptionList field must not be a multi-dimensional array"));
    }

    @Test
    public void testRegistryThrowsCommandDefinitionExceptionWithTheCause() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(InvalidOptionListCommand.class)
                .create();

        try {
            registry.createExecutionContainer("invalid-option-list");
            fail("Expected the definition error");
        } catch (CommandDefinitionException e) {
            assertEquals("invalid-option-list", e.getCommandName());
            assertTrue(e.getCause() instanceof CommandLineParserException);
            assertTrue(e.getMessage(), e.getMessage().contains("OptionList field must not be a multi-dimensional array"));
        }
    }

    private static CommandResult executeWithStderr(ByteArrayOutputStream err, AeshRuntimeRunner runner) {
        PrintStream origErr = System.err;
        System.setErr(new PrintStream(err));
        try {
            return runner.execute();
        } finally {
            System.setErr(origErr);
        }
    }

    @CommandDefinition(name = "invalid-option-list", description = "an @OptionList multi-dimensional array field")
    public static class InvalidOptionListCommand implements Command<CommandInvocation> {
        @OptionList(name = "dep")
        private String[][] deps;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "duplicate-arguments", description = "two @Arguments fields")
    public static class DuplicateArgumentsCommand implements Command<CommandInvocation> {
        @Arguments
        private List<String> first;

        @Arguments
        private List<String> second;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }
}
