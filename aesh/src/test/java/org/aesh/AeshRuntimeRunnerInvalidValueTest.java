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
import java.util.EnumSet;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.operator.OperatorType;
import org.aesh.command.option.Option;
import org.aesh.command.validator.OptionValidator;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.validator.ValidatorInvocation;
import org.junit.Test;

/**
 * A value that a converter or a validator refuses is a usage error: the runtime throws
 * {@link OptionValidatorException}, not a RuntimeException, and the runner prints the message
 * and returns {@link CommandResult#USAGE_ERROR}.
 */
public class AeshRuntimeRunnerInvalidValueTest {

    @Test
    public void testConverterRefusesTheValue() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        CommandResult result = executeWithStderr(err, AeshRuntimeRunner.builder()
                .command(ValueCommand.class)
                .args("--level", "x"));

        assertEquals(CommandResult.USAGE_ERROR, result);
        assertTrue(err.toString(), err.toString().contains("Not a number: x"));
        assertFalse(err.toString(), err.toString().contains("RuntimeException"));
    }

    @Test
    public void testValidatorRefusesTheValue() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        CommandResult result = executeWithStderr(err, AeshRuntimeRunner.builder()
                .command(ValueCommand.class)
                .args("--mode", "warp"));

        assertEquals(CommandResult.USAGE_ERROR, result);
        assertTrue(err.toString(), err.toString().contains("Unsupported mode warp"));
        assertFalse(err.toString(), err.toString().contains("RuntimeException"));
    }

    @Test
    public void testBuiltInConverterRefusesTheValue() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        CommandResult result = executeWithStderr(err, AeshRuntimeRunner.builder()
                .lazyStartup(false)
                .command(ValueCommand.class)
                .args("--count", "x"));

        assertEquals(CommandResult.USAGE_ERROR, result);
        assertTrue(err.toString(), err.toString().contains("Invalid integer value"));
        assertFalse(err.toString(), err.toString().contains("RuntimeException"));
    }

    @Test
    public void testRuntimeThrowsOptionValidatorException() throws Exception {
        try (CommandRuntime<CommandInvocation> runtime = buildRuntime()) {
            try {
                runtime.executeCommand("value --count x");
                fail("Expected OptionValidatorException");
            } catch (OptionValidatorException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("Invalid integer value"));
            }
            // the pre-tokenized form, as AeshRuntimeRunner calls it
            try {
                runtime.executeCommand("value", new String[] { "--level", "x" });
                fail("Expected OptionValidatorException");
            } catch (OptionValidatorException e) {
                assertEquals("Not a number: x", e.getMessage());
            }
            assertEquals(CommandResult.SUCCESS, runtime.executeCommand("value --level 3 --mode fast --count 2"));
            assertEquals(Integer.valueOf(3), ValueCommand.lastLevel);
        }
    }

    @Test
    public void testLastPipelineStageThrowsOptionValidatorException() throws Exception {
        try (CommandRuntime<CommandInvocation> runtime = buildRuntime()) {
            try {
                runtime.executeCommand("hello | value --count x");
                fail("Expected OptionValidatorException");
            } catch (OptionValidatorException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("Invalid integer value"));
            }
        }
    }

    private static CommandRuntime<CommandInvocation> buildRuntime() throws Exception {
        return AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(AeshCommandRegistryBuilder.<CommandInvocation> builder()
                        .command(ValueCommand.class)
                        .command(HelloCommand.class)
                        .create())
                .operators(EnumSet.allOf(OperatorType.class))
                .build();
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

    public static class LevelConverter implements Converter<Integer, ConverterInvocation> {
        @Override
        public Integer convert(ConverterInvocation invocation) throws OptionValidatorException {
            try {
                return Integer.valueOf(invocation.getInput());
            } catch (NumberFormatException e) {
                throw new OptionValidatorException("Not a number: " + invocation.getInput());
            }
        }
    }

    public static class ModeValidator implements OptionValidator<ValidatorInvocation<String, Command>> {
        @Override
        public void validate(ValidatorInvocation<String, Command> invocation) throws OptionValidatorException {
            if (!"fast".equals(invocation.getValue()) && !"slow".equals(invocation.getValue())) {
                throw new OptionValidatorException("Unsupported mode " + invocation.getValue());
            }
        }
    }

    @CommandDefinition(name = "value", description = "options with converters and validators")
    public static class ValueCommand implements Command<CommandInvocation> {
        static Integer lastLevel;

        @Option(name = "level", converter = LevelConverter.class)
        private Integer level;

        @Option(name = "mode", validator = ModeValidator.class)
        private String mode;

        @Option(name = "count")
        private int count;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            lastLevel = level;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "hello", description = "writes a line")
    public static class HelloCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("hello");
            return CommandResult.SUCCESS;
        }
    }
}
