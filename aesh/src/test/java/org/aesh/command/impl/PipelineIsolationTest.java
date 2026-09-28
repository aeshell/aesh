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
package org.aesh.command.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.impl.Executions.PopulateProbe;
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
 * Repeated occurrences of one command in a pipeline must observe their own
 * invocation's options, not the last population to win the shared command
 * instance (#642).
 * <p>
 * The probe forces the interleaving deterministically: the second stage's
 * population waits until the first stage's execution has begun, and the
 * first stage reads its field only after the second population landed. The
 * assertion is order-insensitive (a set), so either arrival order proves
 * the point.
 */
public class PipelineIsolationTest {

    @CommandDefinition(name = "repeat", description = "echoes its option value")
    public static class RepeatCommand implements Command<CommandInvocation> {
        @Option(name = "value", description = "value")
        String value;

        static final AtomicInteger arrivals = new AtomicInteger();
        static final List<String> observed = Collections.synchronizedList(new ArrayList<>());
        static volatile PopulateProbe probe;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            PopulateProbe current = probe;
            if (current != null) {
                if (arrivals.incrementAndGet() == 1) {
                    current.firstExecuteBegun.countDown();
                    current.await(current.secondPopulated);
                } else {
                    current.await(current.bothPopulated);
                }
            }
            observed.add(String.valueOf(value));
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testRepeatedCommandObservesOwnOptionValue() throws Exception {
        PopulateProbe probe = new PopulateProbe();
        PopulateProbe.installed = probe;
        RepeatCommand.probe = probe;
        RepeatCommand.arrivals.set(0);
        RepeatCommand.observed.clear();
        try {
            CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder
                    .<CommandInvocation> builder()
                    .command(RepeatCommand.class)
                    .create();
            CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder
                    .<CommandInvocation> builder()
                    .commandRegistry(registry)
                    .operators(EnumSet.allOf(OperatorType.class))
                    .build();

            CommandResult result = runtime.executeCommand("repeat --value first | repeat --value second");

            assertEquals(CommandResult.SUCCESS, result);
            assertTrue("both stages must execute, got: " + RepeatCommand.observed,
                    probe.bothPopulated.await(10, java.util.concurrent.TimeUnit.SECONDS)
                            && RepeatCommand.observed.size() == 2);
            Set<String> seen = new HashSet<>(RepeatCommand.observed);
            assertEquals("each stage must observe its own value, got: " + seen,
                    new HashSet<>(java.util.Arrays.asList("first", "second")), seen);
        } finally {
            PopulateProbe.installed = null;
            RepeatCommand.probe = null;
        }
    }

    @CommandDefinition(name = "repeatgen", description = "generated-path repeat")
    public static class RepeatGenCommand implements Command<CommandInvocation> {
        String value;

        static final AtomicInteger arrivals = new AtomicInteger();
        static final List<String> observed = Collections.synchronizedList(new ArrayList<>());
        static volatile PopulateProbe probe;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            PopulateProbe current = probe;
            if (current != null) {
                if (arrivals.incrementAndGet() == 1) {
                    current.firstExecuteBegun.countDown();
                    current.await(current.secondPopulated);
                } else {
                    current.await(current.bothPopulated);
                }
            }
            observed.add(String.valueOf(value));
            return CommandResult.SUCCESS;
        }
    }

    /**
     * Hand-written equivalent of annotation-processor output: field setters,
     * no runtime reflection for injection.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static class RepeatGenCommand_AeshMetadata implements CommandMetadataProvider<RepeatGenCommand> {
        @Override
        public Class<RepeatGenCommand> commandType() {
            return RepeatGenCommand.class;
        }

        @Override
        public RepeatGenCommand newInstance() {
            return new RepeatGenCommand();
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
        public String commandName() {
            return "repeatgen";
        }

        @Override
        public ProcessedCommand buildProcessedCommand(RepeatGenCommand instance)
                throws CommandLineParserException {
            try {
                ProcessedCommand processedCommand = ((ProcessedCommandBuilder) ProcessedCommandBuilder.builder())
                        .name("repeatgen")
                        .description("generated-path repeat")
                        .command(instance)
                        .create();
                processedCommand.addOption(
                        ProcessedOptionBuilder.builder()
                                .name("value")
                                .type(String.class)
                                .fieldName("value")
                                .optionType(OptionType.NORMAL)
                                .fieldSetter((inst, val) -> ((RepeatGenCommand) inst).value = (String) val)
                                .fieldResetter(inst -> ((RepeatGenCommand) inst).value = null)
                                .build());
                return processedCommand;
            } catch (Exception e) {
                throw new CommandLineParserException(e.getMessage());
            }
        }
    }

    @Test
    public void testGeneratedMetadataPathIsolatesStages() throws Exception {
        MetadataProviderRegistry.reset();
        MetadataProviderRegistry.register(className -> {
            if (className.equals(RepeatGenCommand.class.getName()))
                return new RepeatGenCommand_AeshMetadata();
            return null;
        });
        PopulateProbe probe = new PopulateProbe();
        PopulateProbe.installed = probe;
        RepeatGenCommand.probe = probe;
        RepeatGenCommand.arrivals.set(0);
        RepeatGenCommand.observed.clear();
        try {
            CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder
                    .<CommandInvocation> builder()
                    .command(RepeatGenCommand.class)
                    .create();
            CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder
                    .<CommandInvocation> builder()
                    .commandRegistry(registry)
                    .operators(EnumSet.allOf(OperatorType.class))
                    .build();

            CommandResult result = runtime.executeCommand("repeatgen --value first | repeatgen --value second");

            assertEquals(CommandResult.SUCCESS, result);
            assertTrue("both stages must execute, got: " + RepeatGenCommand.observed,
                    RepeatGenCommand.observed.size() == 2);
            Set<String> seen = new HashSet<>(RepeatGenCommand.observed);
            assertEquals("each stage must observe its own value, got: " + seen,
                    new HashSet<>(java.util.Arrays.asList("first", "second")), seen);
        } finally {
            PopulateProbe.installed = null;
            RepeatGenCommand.probe = null;
            MetadataProviderRegistry.reset();
        }
    }

    @Test
    public void testSuppliedInstanceSequentialReuse() throws Exception {
        RepeatCommand.probe = null;
        RepeatCommand.arrivals.set(0);
        RepeatCommand.observed.clear();
        try {
            // User-supplied singleton: deliberate sequential reuse keeps
            // working, repopulated per execution (explicit share policy).
            CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder
                    .<CommandInvocation> builder()
                    .command(new RepeatCommand())
                    .create();
            CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder
                    .<CommandInvocation> builder()
                    .commandRegistry(registry)
                    .operators(EnumSet.allOf(OperatorType.class))
                    .build();

            assertEquals(CommandResult.SUCCESS, runtime.executeCommand("repeat --value first"));
            assertEquals(CommandResult.SUCCESS, runtime.executeCommand("repeat --value second"));

            assertEquals(java.util.Arrays.asList("first", "second"), RepeatCommand.observed);
        } finally {
            RepeatCommand.observed.clear();
        }
    }

    @CommandDefinition(name = "gsub", description = "")
    public static class GSubCommand implements Command<CommandInvocation> {
        @Option(name = "value", description = "value")
        String value;

        static final AtomicInteger arrivals = new AtomicInteger();
        static final List<String> observed = Collections.synchronizedList(new ArrayList<>());
        static volatile PopulateProbe probe;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            PopulateProbe current = probe;
            if (current != null) {
                if (arrivals.incrementAndGet() == 1) {
                    current.firstExecuteBegun.countDown();
                    current.await(current.secondPopulated);
                } else {
                    current.await(current.bothPopulated);
                }
            }
            observed.add(String.valueOf(value));
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "grp", description = "", groupCommands = { GSubCommand.class })
    public static class GrpCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testGroupRepeatedSubcommandIsolatesStages() throws Exception {
        PopulateProbe probe = new PopulateProbe();
        PopulateProbe.installed = probe;
        GSubCommand.probe = probe;
        GSubCommand.arrivals.set(0);
        GSubCommand.observed.clear();
        try {
            CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder
                    .<CommandInvocation> builder()
                    .command(GrpCommand.class)
                    .create();
            CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder
                    .<CommandInvocation> builder()
                    .commandRegistry(registry)
                    .operators(EnumSet.allOf(OperatorType.class))
                    .build();

            CommandResult result = runtime.executeCommand("grp gsub --value first | grp gsub --value second");

            assertEquals(CommandResult.SUCCESS, result);
            assertTrue("both stages must execute, got: " + GSubCommand.observed,
                    GSubCommand.observed.size() == 2);
            Set<String> seen = new HashSet<>(GSubCommand.observed);
            assertEquals("each stage must observe its own value, got: " + seen,
                    new HashSet<>(java.util.Arrays.asList("first", "second")), seen);
        } finally {
            PopulateProbe.installed = null;
            GSubCommand.probe = null;
        }
    }
}
