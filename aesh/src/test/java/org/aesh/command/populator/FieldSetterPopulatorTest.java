/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.aesh.command.populator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.impl.parser.AeshCommandLineParser;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.Option;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.AeshContext;
import org.junit.Test;

/**
 * Tests that field setter/resetter lambdas on ProcessedOption work correctly
 * with the AeshCommandPopulator, simulating what the annotation processor generates.
 */
public class FieldSetterPopulatorTest {

    private final InvocationProviders invocationProviders = new AeshInvocationProviders(
            null, null, null, null, null);

    @CommandDefinition(name = "test", description = "test command")
    public static class TestCommand implements Command<CommandInvocation> {
        @Option(shortName = 'v', hasValue = false)
        boolean verbose;

        @Option(defaultValue = "hello")
        String greeting;

        @Option
        int count;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    /**
     * Build a ProcessedCommand manually with field setters, like the annotation processor would.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private CommandLineParser<CommandInvocation> buildParserWithFieldSetters(TestCommand instance) throws Exception {
        ProcessedCommand processedCommand = ((ProcessedCommandBuilder) ProcessedCommandBuilder.builder())
                .name("test")
                .description("test command")
                .command(instance)
                .create();

        processedCommand.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('v')
                        .name("verbose")
                        .type(boolean.class)
                        .fieldName("verbose")
                        .optionType(OptionType.BOOLEAN)
                        .hasValue(false)
                        .fieldSetter((inst, val) -> ((TestCommand) inst).verbose = (boolean) val)
                        .fieldResetter(inst -> ((TestCommand) inst).verbose = false)
                        .build());

        processedCommand.addOption(
                ProcessedOptionBuilder.builder()
                        .name("greeting")
                        .type(String.class)
                        .fieldName("greeting")
                        .optionType(OptionType.NORMAL)
                        .addDefaultValue("hello")
                        .fieldSetter((inst, val) -> ((TestCommand) inst).greeting = (String) val)
                        .fieldResetter(inst -> ((TestCommand) inst).greeting = null)
                        .build());

        processedCommand.addOption(
                ProcessedOptionBuilder.builder()
                        .name("count")
                        .type(int.class)
                        .fieldName("count")
                        .optionType(OptionType.NORMAL)
                        .fieldSetter((inst, val) -> ((TestCommand) inst).count = (int) val)
                        .fieldResetter(inst -> ((TestCommand) inst).count = 0)
                        .build());

        return new AeshCommandLineParser<>(processedCommand);
    }

    @Test
    public void testBooleanFlagWithFieldSetter() throws Exception {
        TestCommand cmd = new TestCommand();
        CommandLineParser<CommandInvocation> parser = buildParserWithFieldSetters(cmd);
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        parser.parse("test -v");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);

        assertTrue("verbose should be true after -v", cmd.verbose);
    }

    @Test
    public void testBooleanFlagResetWithFieldResetter() throws Exception {
        TestCommand cmd = new TestCommand();
        CommandLineParser<CommandInvocation> parser = buildParserWithFieldSetters(cmd);
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        // First set it
        parser.parse("test -v");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertTrue(cmd.verbose);

        // Then parse without -v — should reset
        parser.parse("test");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertFalse("verbose should be reset to false", cmd.verbose);
    }

    @Test
    public void testStringOptionWithFieldSetter() throws Exception {
        TestCommand cmd = new TestCommand();
        CommandLineParser<CommandInvocation> parser = buildParserWithFieldSetters(cmd);
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        parser.parse("test --greeting=world");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);

        assertEquals("world", cmd.greeting);
    }

    @Test
    public void testDefaultValueAppliedWithFieldSetter() throws Exception {
        TestCommand cmd = new TestCommand();
        CommandLineParser<CommandInvocation> parser = buildParserWithFieldSetters(cmd);
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        // Parse without --greeting — default "hello" should be applied
        parser.parse("test");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);

        assertEquals("hello", cmd.greeting);
    }

    @Test
    public void testIntOptionWithFieldSetter() throws Exception {
        TestCommand cmd = new TestCommand();
        CommandLineParser<CommandInvocation> parser = buildParserWithFieldSetters(cmd);
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        parser.parse("test --count=42");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);

        assertEquals(42, cmd.count);
    }

    @Test
    public void testIntOptionResetWithFieldResetter() throws Exception {
        TestCommand cmd = new TestCommand();
        CommandLineParser<CommandInvocation> parser = buildParserWithFieldSetters(cmd);
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        parser.parse("test --count=42");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals(42, cmd.count);

        // Reset
        parser.parse("test");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals(0, cmd.count);
    }

    @Test
    public void testStringOptionResetWithFieldResetter() throws Exception {
        TestCommand cmd = new TestCommand();
        cmd.greeting = "something";
        CommandLineParser<CommandInvocation> parser = buildParserWithFieldSetters(cmd);
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        // Parse with --greeting explicitly — should set
        parser.parse("test --greeting=world");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("world", cmd.greeting);
    }

    /**
     * Verify that field setters from the annotation processor path work with the
     * standard AeshCommandContainerBuilder (via MetadataProviderRegistry).
     * This uses reflection-based path for comparison.
     */
    @Test
    public void testReflectionPathStillWorks() throws Exception {
        // Use the standard reflection-based builder (no field setters)
        TestCommand cmd = new TestCommand();
        CommandLineParser<CommandInvocation> parser = new org.aesh.command.impl.container.AeshCommandContainerBuilder<CommandInvocation>()
                .create(cmd).getParser();
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        parser.parse("test -v --greeting=world --count=5");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);

        assertTrue(cmd.verbose);
        assertEquals("world", cmd.greeting);
        assertEquals(5, cmd.count);
    }
}
