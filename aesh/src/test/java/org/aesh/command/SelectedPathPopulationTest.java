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
package org.aesh.command;

import static org.junit.Assert.assertEquals;

import java.util.EnumSet;

import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.metadata.CommandMetadataProvider;
import org.aesh.command.metadata.MetadataProviderRegistry;
import org.aesh.command.operator.OperatorType;
import org.aesh.command.option.Option;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistry;
import org.junit.Test;

/**
 * Population must walk only the selected command path: unselected siblings
 * are never populated, and inherited values propagate top-down so they
 * reach grandchildren and deeper descendants (#648).
 */
public class SelectedPathPopulationTest {

    @CommandDefinition(name = "good", description = "selected sibling")
    public static class GoodCommand implements Command<CommandInvocation> {
        static volatile boolean executed;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            executed = true;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "bad", description = "unselected sibling with an unconvertible default")
    public static class BadCommand implements Command<CommandInvocation> {
        @Option(name = "number", description = "number", defaultValue = "not-a-number")
        int number;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "siblings", description = "group", groupCommands = { GoodCommand.class,
            BadCommand.class })
    public static class SiblingsGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "leaf", description = "leaf")
    public static class NumberLeafCommand implements Command<CommandInvocation> {
        @Option(name = "number", description = "number", inherited = true)
        int number;

        static volatile int sawNumber = Integer.MIN_VALUE;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            sawNumber = number;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mid", description = "mid", groupCommands = { NumberLeafCommand.class })
    public static class NumberMidCommand implements Command<CommandInvocation> {
        @Option(name = "number", description = "number", inherited = true)
        int number;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "root", description = "root", groupCommands = { NumberMidCommand.class })
    public static class NumberRootCommand implements Command<CommandInvocation> {
        @Option(name = "number", description = "number", inherited = true)
        int number;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
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
    public void testUnselectedSiblingConverterNeverRuns() throws Exception {
        GoodCommand.executed = false;
        CommandRuntime<CommandInvocation> runtime = buildRuntime(SiblingsGroup.class);

        CommandResult result = runtime.executeCommand("siblings good");

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals(true, GoodCommand.executed);
    }

    @Test
    public void testInheritedValueReachesGrandchild() throws Exception {
        NumberLeafCommand.sawNumber = Integer.MIN_VALUE;
        CommandRuntime<CommandInvocation> runtime = buildRuntime(NumberRootCommand.class);

        CommandResult result = runtime.executeCommand("root --number 7 mid leaf");

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals(7, NumberLeafCommand.sawNumber);
    }

    @Test
    public void testExplicitChildValueWins() throws Exception {
        NumberLeafCommand.sawNumber = Integer.MIN_VALUE;
        CommandRuntime<CommandInvocation> runtime = buildRuntime(NumberRootCommand.class);

        CommandResult result = runtime.executeCommand("root --number 7 mid --number 9 leaf");

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals(9, NumberLeafCommand.sawNumber);
    }

    @Test
    public void testSequentialReusePopulatesCleanly() throws Exception {
        CommandRuntime<CommandInvocation> runtime = buildRuntime(NumberRootCommand.class);

        NumberLeafCommand.sawNumber = Integer.MIN_VALUE;
        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("root --number 7 mid leaf"));
        assertEquals(7, NumberLeafCommand.sawNumber);

        NumberLeafCommand.sawNumber = Integer.MIN_VALUE;
        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("root --number 8 mid leaf"));
        assertEquals(8, NumberLeafCommand.sawNumber);
    }

    @Test
    public void testLazyChildrenPopulateSelectedPath() throws Exception {
        GoodCommand.executed = false;
        CommandRuntime<CommandInvocation> runtime = buildLazyRuntime(SiblingsGroup.class);

        CommandResult result = runtime.executeCommand("siblings good");

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals(true, GoodCommand.executed);
    }

    @Test
    public void testLazyInheritedValueReachesGrandchild() throws Exception {
        NumberLeafCommand.sawNumber = Integer.MIN_VALUE;
        CommandRuntime<CommandInvocation> runtime = buildLazyRuntime(NumberRootCommand.class);

        CommandResult result = runtime.executeCommand("root --number 7 mid leaf");

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals(7, NumberLeafCommand.sawNumber);
    }

    private static CommandRuntime<CommandInvocation> buildLazyRuntime(Class<? extends Command>... commands)
            throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> containerBuilder = new AeshCommandContainerBuilder<>();
        containerBuilder.setLazyChildResolution(true);
        AeshCommandRegistryBuilder<CommandInvocation> builder = AeshCommandRegistryBuilder
                .<CommandInvocation> builder()
                .containerBuilder(containerBuilder);
        for (Class<? extends Command> command : commands)
            builder.command(command);
        CommandRegistry<CommandInvocation> registry = builder.create();
        return AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(registry)
                .operators(EnumSet.allOf(OperatorType.class))
                .build();
    }

    // --- Generated-metadata path: hand-written equivalents of
    // annotation-processor output (field setters, no runtime reflection) ---

    @CommandDefinition(name = "good", description = "generated selected sibling")
    public static class GoodGenCommand implements Command<CommandInvocation> {
        static volatile boolean executed;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            executed = true;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "bad", description = "generated unselected sibling")
    public static class BadGenCommand implements Command<CommandInvocation> {
        int number;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "siblingsgen", description = "generated group")
    public static class SiblingsGenGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    private abstract static class NamedGenProvider<C extends Command> implements CommandMetadataProvider<C> {
        private final Class<C> type;
        private final String name;

        NamedGenProvider(Class<C> type, String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public Class<C> commandType() {
            return type;
        }

        @Override
        public C newInstance() {
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public boolean isGroupCommand() {
            return false;
        }

        @Override
        @SuppressWarnings("rawtypes")
        public Class<? extends Command>[] groupCommandClasses() {
            return new Class[0];
        }

        @Override
        public String commandName() {
            return name;
        }
    }

    private static class GoodGenProvider extends NamedGenProvider<GoodGenCommand> {
        GoodGenProvider() {
            super(GoodGenCommand.class, "good");
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ProcessedCommand buildProcessedCommand(GoodGenCommand instance)
                throws CommandLineParserException {
            try {
                return ((ProcessedCommandBuilder) ProcessedCommandBuilder.builder())
                        .name("good")
                        .description("generated selected sibling")
                        .command(instance)
                        .create();
            } catch (Exception e) {
                throw new CommandLineParserException(e.getMessage());
            }
        }
    }

    private static class BadGenProvider extends NamedGenProvider<BadGenCommand> {
        BadGenProvider() {
            super(BadGenCommand.class, "bad");
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ProcessedCommand buildProcessedCommand(BadGenCommand instance)
                throws CommandLineParserException {
            try {
                ProcessedCommand processedCommand = ((ProcessedCommandBuilder) ProcessedCommandBuilder.builder())
                        .name("bad")
                        .description("generated unselected sibling")
                        .command(instance)
                        .create();
                processedCommand.addOption(
                        ProcessedOptionBuilder.builder()
                                .name("number")
                                .type(int.class)
                                .fieldName("number")
                                .optionType(OptionType.NORMAL)
                                .addDefaultValue("not-a-number")
                                .fieldSetter((inst, val) -> ((BadGenCommand) inst).number = (int) val)
                                .fieldResetter(inst -> ((BadGenCommand) inst).number = 0)
                                .build());
                return processedCommand;
            } catch (Exception e) {
                throw new CommandLineParserException(e.getMessage());
            }
        }
    }

    private static class SiblingsGenProvider extends NamedGenProvider<SiblingsGenGroup> {
        SiblingsGenProvider() {
            super(SiblingsGenGroup.class, "siblingsgen");
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ProcessedCommand buildProcessedCommand(SiblingsGenGroup instance)
                throws CommandLineParserException {
            try {
                return ((ProcessedCommandBuilder) ProcessedCommandBuilder.builder())
                        .name("siblingsgen")
                        .description("generated group")
                        .command(instance)
                        .create();
            } catch (Exception e) {
                throw new CommandLineParserException(e.getMessage());
            }
        }

        @Override
        public boolean isGroupCommand() {
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Command>[] groupCommandClasses() {
            return new Class[] { GoodGenCommand.class, BadGenCommand.class };
        }

        @Override
        public String[][] groupCommandNamesAndAliases() {
            return new String[][] { { "good" }, { "bad" } };
        }
    }

    @Test
    public void testGeneratedUnselectedSiblingNeverPopulated() throws Exception {
        MetadataProviderRegistry.reset();
        MetadataProviderRegistry.register(className -> {
            if (className.equals(GoodGenCommand.class.getName()))
                return new GoodGenProvider();
            if (className.equals(BadGenCommand.class.getName()))
                return new BadGenProvider();
            if (className.equals(SiblingsGenGroup.class.getName()))
                return new SiblingsGenProvider();
            return null;
        });
        try {
            GoodGenCommand.executed = false;
            CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder
                    .<CommandInvocation> builder()
                    .command(SiblingsGenGroup.class)
                    .create();
            CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder
                    .<CommandInvocation> builder()
                    .commandRegistry(registry)
                    .operators(EnumSet.allOf(OperatorType.class))
                    .build();

            CommandResult result = runtime.executeCommand("siblingsgen good");

            assertEquals(CommandResult.SUCCESS, result);
            assertEquals(true, GoodGenCommand.executed);
        } finally {
            MetadataProviderRegistry.reset();
        }
    }
}
