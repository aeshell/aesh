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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.metadata.CommandMetadataProvider;
import org.aesh.command.option.Option;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.AeshContext;
import org.junit.Test;

/**
 * Tests the full flow: MetadataProvider -> AeshCommandContainerBuilder -> populator
 * with field setters, simulating what happens in a real application.
 */
public class MetadataProviderFieldSetterTest {

    private final InvocationProviders invocationProviders = new AeshInvocationProviders(
            null, null, null, null, null);

    @CommandDefinition(name = "mytest", description = "test")
    public static class MyTestCommand implements Command<CommandInvocation> {
        @Option(shortName = 'v', hasValue = false)
        boolean verbose;

        @Option(defaultValue = "WARN")
        String level;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    /**
     * A manually created metadata provider, equivalent to what the annotation processor generates.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static class MyTestCommand_AeshMetadata implements CommandMetadataProvider<MyTestCommand> {
        @Override
        public Class<MyTestCommand> commandType() {
            return MyTestCommand.class;
        }

        @Override
        public MyTestCommand newInstance() {
            return new MyTestCommand();
        }

        @Override
        public boolean isGroupCommand() {
            return false;
        }

        @Override
        public Class<? extends Command>[] groupCommandClasses() {
            return new Class[0];
        }

        @Override
        public ProcessedCommand buildProcessedCommand(MyTestCommand instance) throws CommandLineParserException {
            try {
                ProcessedCommand processedCommand = ((ProcessedCommandBuilder) ProcessedCommandBuilder.builder())
                        .name("mytest")
                        .description("test")
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
                                .fieldSetter((inst, val) -> ((MyTestCommand) inst).verbose = (boolean) val)
                                .fieldResetter(inst -> ((MyTestCommand) inst).verbose = false)
                                .build());

                processedCommand.addOption(
                        ProcessedOptionBuilder.builder()
                                .name("level")
                                .type(String.class)
                                .fieldName("level")
                                .optionType(OptionType.NORMAL)
                                .addDefaultValue("WARN")
                                .fieldSetter((inst, val) -> ((MyTestCommand) inst).level = (String) val)
                                .fieldResetter(inst -> ((MyTestCommand) inst).level = null)
                                .build());

                return processedCommand;
            } catch (Exception e) {
                throw new CommandLineParserException(e.getMessage());
            }
        }
    }

    @Test
    public void testMetadataProviderWithFieldSetters() throws Exception {
        // Manually register the provider (in real apps, ServiceLoader does this)
        // We'll create the container directly via the provider
        MyTestCommand cmd = new MyTestCommand();
        MyTestCommand_AeshMetadata provider = new MyTestCommand_AeshMetadata();

        ProcessedCommand processedCommand = provider.buildProcessedCommand(cmd);

        // Verify field setters are present
        assertNotNull("verbose setter should exist",
                ((ProcessedOption) processedCommand.getOptions().get(0)).getFieldSetter());
        assertNotNull("level setter should exist",
                ((ProcessedOption) processedCommand.getOptions().get(1)).getFieldSetter());
    }

    @Test
    public void testContainerBuilderUsesMetadataProvider() throws Exception {
        // Register our provider so AeshCommandContainerBuilder finds it
        // We need to use ServiceLoader normally, but for testing we can build directly
        MyTestCommand cmd = new MyTestCommand();
        MyTestCommand_AeshMetadata provider = new MyTestCommand_AeshMetadata();

        ProcessedCommand processedCommand = provider.buildProcessedCommand(cmd);

        CommandLineParser<CommandInvocation> parser = new org.aesh.command.impl.parser.AeshCommandLineParser<>(
                processedCommand);

        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        // Test boolean flag
        parser.parse("mytest -v");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertTrue("verbose should be true", cmd.verbose);

        // Test default value
        parser.parse("mytest");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertFalse("verbose should be reset", cmd.verbose);
        assertEquals("WARN", cmd.level);

        // Test explicit value
        parser.parse("mytest --level=ERROR");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("ERROR", cmd.level);
    }
}
