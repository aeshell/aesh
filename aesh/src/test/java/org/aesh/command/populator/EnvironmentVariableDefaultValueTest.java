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
package org.aesh.command.populator;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.parser.CommandLineParserBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.Option;
import org.aesh.command.impl.activator.AeshCommandActivatorProvider;
import org.aesh.command.impl.activator.AeshOptionActivatorProvider;
import org.aesh.command.impl.completer.AeshCompleterInvocationProvider;
import org.aesh.command.impl.converter.AeshConverterInvocationProvider;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.impl.populator.AeshCommandPopulator;
import org.aesh.command.impl.validator.AeshValidatorInvocationProvider;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.AeshContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test for environment variable resolution in @Option defaultValue.
 */
public class EnvironmentVariableDefaultValueTest {

    private final InvocationProviders invocationProviders = new AeshInvocationProviders(
            SettingsBuilder.builder()
                    .converterInvocationProvider(new AeshConverterInvocationProvider())
                    .completerInvocationProvider(new AeshCompleterInvocationProvider())
                    .validatorInvocationProvider(new AeshValidatorInvocationProvider())
                    .optionActivatorProvider(new AeshOptionActivatorProvider())
                    .commandActivatorProvider(new AeshCommandActivatorProvider()).build());

    /**
     * Test that environment variable syntax $(ENV_VAR) in defaultValue is resolved.
     * This test uses the PATH environment variable which should be available in most environments.
     */
    @Test
    public void testEnvironmentVariableResolution() throws Exception {
        // Use PATH which should be set in most environments
        String pathValue = System.getenv("PATH");
        assertNotNull("PATH environment variable should be set", pathValue);

        TestPopulatorWithEnvVar command = new TestPopulatorWithEnvVar();
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> commandBuilder = ProcessedCommandBuilder.builder()
                .name("test")
                .populator(new AeshCommandPopulator<>(command))
                .description("a simple test");

        commandBuilder.addOption(ProcessedOptionBuilder.builder()
                .name("path")
                .description("test path option")
                .fieldName("pathOption")
                .type(String.class)
                .addDefaultValue("$(PATH)")
                .build());

        CommandLineParser<CommandInvocation> parser = CommandLineParserBuilder.builder()
                .processedCommand(commandBuilder.create())
                .create();

        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        // Parse without specifying the option, so the default value should be used
        parser.parse("test");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        // The default value should be the value of the PATH environment variable
        assertEquals(pathValue, command.pathOption);
    }

    /**
     * Test that when the environment variable doesn't exist, the original value is kept.
     */
    @Test
    public void testNonExistentEnvironmentVariable() throws Exception {
        TestPopulatorWithEnvVar command = new TestPopulatorWithEnvVar();
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> commandBuilder = ProcessedCommandBuilder.builder()
                .name("test")
                .populator(new AeshCommandPopulator<>(command))
                .description("a simple test");

        commandBuilder.addOption(ProcessedOptionBuilder.builder()
                .name("myvar")
                .description("test option")
                .fieldName("myVarOption")
                .type(String.class)
                .addDefaultValue("$(NON_EXISTENT_VAR_12345)")
                .build());

        CommandLineParser<CommandInvocation> parser = CommandLineParserBuilder.builder()
                .processedCommand(commandBuilder.create())
                .create();

        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        // Parse without specifying the option, so the default value should be used
        parser.parse("test");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        // The default value should remain unchanged since the env var doesn't exist
        assertEquals("$(NON_EXISTENT_VAR_12345)", command.myVarOption);
    }

    /**
     * Test that regular default values (not environment variables) are not affected.
     */
    @Test
    public void testRegularDefaultValue() throws Exception {
        TestPopulatorWithEnvVar command = new TestPopulatorWithEnvVar();
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> commandBuilder = ProcessedCommandBuilder.builder()
                .name("test")
                .populator(new AeshCommandPopulator<>(command))
                .description("a simple test");

        commandBuilder.addOption(ProcessedOptionBuilder.builder()
                .name("regular")
                .description("test option")
                .fieldName("regularOption")
                .type(String.class)
                .addDefaultValue("regularValue")
                .build());

        CommandLineParser<CommandInvocation> parser = CommandLineParserBuilder.builder()
                .processedCommand(commandBuilder.create())
                .create();

        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        // Parse without specifying the option, so the default value should be used
        parser.parse("test");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        // The default value should remain unchanged
        assertEquals("regularValue", command.regularOption);
    }

    /**
     * Test with HOME environment variable which is also commonly available.
     */
    @Test
    public void testHomeEnvironmentVariable() throws Exception {
        String homeValue = System.getenv("HOME");
        // Skip test if HOME is not set
        if (homeValue == null) {
            return;
        }

        TestPopulatorWithEnvVar command = new TestPopulatorWithEnvVar();
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> commandBuilder = ProcessedCommandBuilder.builder()
                .name("test")
                .populator(new AeshCommandPopulator<>(command))
                .description("a simple test");

        commandBuilder.addOption(ProcessedOptionBuilder.builder()
                .name("home")
                .description("test home option")
                .fieldName("homeOption")
                .type(String.class)
                .addDefaultValue("$(HOME)")
                .build());

        CommandLineParser<CommandInvocation> parser = CommandLineParserBuilder.builder()
                .processedCommand(commandBuilder.create())
                .create();

        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        parser.parse("test");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        assertEquals(homeValue, command.homeOption);
    }

    /**
     * Simple command class for testing with environment variable default values.
     */
    public static class TestPopulatorWithEnvVar implements Command<CommandInvocation> {
        public String pathOption;
        public String myVarOption;
        public String regularOption;
        public String homeOption;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    /**
     * Test environment variable resolution using the @Option annotation.
     * This test uses the USER environment variable which should be available in most Unix environments.
     */
    @Test
    public void testAnnotationBasedEnvironmentVariable() throws Exception {
        String userValue = System.getenv("USER");
        // Skip test if USER is not set
        if (userValue == null) {
            return;
        }

        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new AnnotatedEnvVarCommand())
                .getParser();

        AnnotatedEnvVarCommand command = (AnnotatedEnvVarCommand) parser.getCommand();
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        // Parse without specifying the option, so the default value should be used
        parser.parse("envtest");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), 
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        // The default value should be the value of the USER environment variable
        assertEquals(userValue, command.getUsername());
    }

    /**
     * Command class using @Option annotation with environment variable default value.
     */
    @CommandDefinition(name = "envtest", description = "a test for environment variables")
    public static class AnnotatedEnvVarCommand implements Command<CommandInvocation> {

        @Option(defaultValue = "$(USER)")
        private String username;

        public String getUsername() {
            return username;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) 
                throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }
}
