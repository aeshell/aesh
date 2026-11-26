/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.aesh.command.impl.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.junit.Test;

/**
 * Execution-scoped forks resolve group children lazily (#799): a fork
 * executes exactly one route, so eagerly building every sibling was waste.
 * Registration-time materialization stays eager (the shared container
 * amortizes it, #577), and help rendered from a fork still lists every
 * sibling because {@code printHelp()} resolves lazy children on demand.
 */
public class ExecutionForkLazyResolutionTest {

    @CommandDefinition(name = "countinga", description = "child a")
    public static class CountingA implements Command<CommandInvocation> {
        static int constructions;

        public CountingA() {
            constructions++;
        }

        @Option(hasValue = false, description = "A flag")
        public boolean flag;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "countingb", description = "child b")
    public static class CountingB implements Command<CommandInvocation> {
        static int constructions;

        public CountingB() {
            constructions++;
        }

        @Option(hasValue = false, description = "B flag")
        public boolean flag;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "root", description = "root group", groupCommands = { CountingA.class, CountingB.class })
    public static class RootGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testExecutionForkBuildsOnlyTheMatchedRoute() throws Exception {
        CountingA.constructions = 0;
        CountingB.constructions = 0;
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        registry.addCommand(RootGroup.class);

        // Deferred registration: nothing built yet
        assertEquals(0, CountingA.constructions + CountingB.constructions);

        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry)
                .build();
        CommandResult result = runtime.executeCommand("root", new String[] { "countingb", "--flag" });
        assertEquals(CommandResult.SUCCESS, result);
        // The fork built the root and the matched child only; the sibling
        // was never constructed (#799)
        assertEquals("matched child must be built exactly once", 1, CountingB.constructions);
        assertEquals("unmatched sibling must not be built by the fork", 0, CountingA.constructions);
        runtime.close();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testRegistrationMaterializationStaysEager() throws Exception {
        CountingA.constructions = 0;
        CountingB.constructions = 0;
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        registry.addCommand(RootGroup.class);

        // materialize() builds the shared container with the configured
        // builder: eager child resolution is unchanged (#577)
        CommandContainer<CommandInvocation> shared = registry.getCommand("root", "");
        assertNotNull(shared);
        assertEquals("shared build constructs every child", 1, CountingA.constructions);
        assertEquals("shared build constructs every child", 1, CountingB.constructions);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testHelpFromExecutionForkListsAllSiblings() throws Exception {
        CountingA.constructions = 0;
        CountingB.constructions = 0;
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        registry.addCommand(RootGroup.class);

        CommandContainer<CommandInvocation> fork = registry.createExecutionContainer("root");
        assertNotNull(fork);
        // The fork has not resolved any child yet
        assertEquals(0, CountingA.constructions + CountingB.constructions);

        // Rendering help resolves the lazy siblings on demand: the listing
        // stays complete
        String help = fork.getParser().printHelp();
        assertTrue("help must list the 'countinga' sibling: " + help,
                help.contains("countinga"));
        assertTrue("help must list the 'countingb' sibling: " + help,
                help.contains("countingb"));
        assertEquals("printHelp resolves siblings on demand", 1, CountingA.constructions);
        assertEquals("printHelp resolves siblings on demand", 1, CountingB.constructions);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testForkParsesSelectedSubcommand() throws Exception {
        CountingA.constructions = 0;
        CountingB.constructions = 0;
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        registry.addCommand(RootGroup.class);

        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry)
                .build();
        // Repeated executions each fork lazily and parse the selected child
        for (int i = 0; i < 3; i++) {
            CommandResult result = runtime.executeCommand("root", new String[] { "countinga", "--flag" });
            assertEquals(CommandResult.SUCCESS, result);
        }
        assertEquals(3, CountingA.constructions);
        assertEquals("sibling stays unbuilt across repeated forks", 0, CountingB.constructions);
        runtime.close();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testForkInheritsRegistryHelpToggles() throws Exception {
        // The fork builder mirrors the registry-level settings getBuilder()
        // applies, including changes made after the fork builder was cached,
        // and buildFromProvider resolves them into the fork at build time
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        registry.setHelpDefaults(java.util.EnumSet.of(org.aesh.command.HelpFeature.SHOW_DEFAULT_VALUES));
        registry.addCommand(RootGroup.class);

        CommandContainer<CommandInvocation> fork = registry.createExecutionContainer("root");
        ProcessedCommand<?, CommandInvocation> forkRoot = fork.getParser().getProcessedCommand();
        assertTrue("fork must inherit fleet help toggles at build time", forkRoot.showDefaultValues());
    }
}
