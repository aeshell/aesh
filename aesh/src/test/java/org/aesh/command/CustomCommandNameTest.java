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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;

import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.container.AeshCommandContainer;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.junit.Test;

/**
 * A command instance or class can be registered under a given name and
 * aliases instead of its {@code @CommandDefinition} ones (#706). The custom
 * aliases replace the annotation aliases.
 */
public class CustomCommandNameTest {

    @CommandDefinition(name = "greet", description = "greet", aliases = { "hi" })
    public static class GreetCommand implements Command<CommandInvocation> {
        @Option(name = "name", description = "who")
        String who = "world";

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            GreetCommand.ran = true;
            return CommandResult.SUCCESS;
        }

        static boolean ran;
    }

    @CommandDefinition(name = "other", description = "other")
    public static class OtherCommand implements Command<CommandInvocation> {
        static boolean ran;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            OtherCommand.ran = true;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "grp", description = "group")
    public static class GroupCmd implements GroupCommand<CommandInvocation> {
        @Override
        public List<Command<CommandInvocation>> getCommands() {
            return Collections.emptyList();
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "sub", description = "sub")
    public static class SubCmd implements Command<CommandInvocation> {
        static boolean ran;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            SubCmd.ran = true;
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testInstanceUnderCustomNameAndAlias() throws Exception {
        GreetCommand.ran = false;
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(new GreetCommand(), "welcome", "w").create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("welcome --name Bob"));
        assertTrue(GreetCommand.ran);

        GreetCommand.ran = false;
        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("w"));
        assertTrue(GreetCommand.ran);
    }

    @Test
    public void testCustomNameShowsInHelp() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(new GreetCommand(), "welcome", "w").create();
        CommandContainer<CommandInvocation> container = registry.getCommand("welcome", "");
        assertEquals("welcome", container.getParser().getProcessedCommand().name());
        assertTrue(container.getParser().getProcessedCommand().getAliases().contains("w"));
        assertTrue(container.getParser().printHelp().contains("welcome"));
    }

    @Test
    public void testSameClassTwiceUnderDifferentNames() throws Exception {
        GreetCommand.ran = false;
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(GreetCommand.class)
                .command(GreetCommand.class, "other-greet").create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("greet"));
        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("other-greet"));
        assertTrue(GreetCommand.ran);
    }

    @Test
    public void testDeferredClassUnderCustomName() throws Exception {
        GreetCommand.ran = false;
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(GreetCommand.class, "late", "l").create();
        assertTrue("deferred custom name must be listed",
                registry.getAllCommandNames().contains("late"));

        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();
        // Alias resolves through the deferred record and builds on first use.
        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("l"));
        assertTrue(GreetCommand.ran);
    }

    @Test
    public void testGroupChildUnderCustomName() throws Exception {
        SubCmd.ran = false;
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        CommandContainer<CommandInvocation> group = builder.create(new GroupCmd());
        ((AeshCommandContainer<CommandInvocation>) group).addChild(builder.create(new SubCmd(), "renamed", "r"));
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(group).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("grp renamed"));
        assertTrue(SubCmd.ran);

        SubCmd.ran = false;
        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("grp r"));
        assertTrue(SubCmd.ran);
    }

    @Test
    public void testCustomNameCollisionIsIgnored() throws Exception {
        GreetCommand.ran = false;
        OtherCommand.ran = false;
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(GreetCommand.class)
                .command(OtherCommand.class, "greet").create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        // The colliding custom registration is ignored, like annotation collisions.
        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("greet"));
        assertTrue(GreetCommand.ran);
        assertTrue(!OtherCommand.ran);
    }

    @Test
    public void testEmptyNameIsRejected() throws Exception {
        try {
            new AeshCommandContainerBuilder<CommandInvocation>().create(new GreetCommand(), "");
            fail("expected failure for empty custom name");
        } catch (CommandLineParserException expected) {
            assertTrue(expected.getMessage().contains("must not be empty"));
        }
        try {
            AeshCommandRegistryBuilder.<CommandInvocation> builder()
                    .command(GreetCommand.class, "");
            fail("expected failure for empty custom name");
        } catch (CommandRegistryException expected) {
            assertTrue(expected.getMessage().contains("must not be empty"));
        }
    }
}
