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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.aesh.AeshRuntimeRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.parser.CommandLineParserException;
import org.junit.Test;

/**
 * Tests that a custom CommandContainerBuilder is used for lazy child resolution (#517).
 */
public class CustomContainerBuilderTest {

    /** Tracks how many times the custom builder's create(Class) is called. */
    private static final AtomicInteger customBuilderCallCount = new AtomicInteger();

    /**
     * A custom builder that delegates to AeshCommandContainerBuilder but tracks calls.
     */
    public static class TrackingContainerBuilder extends AeshCommandContainerBuilder<CommandInvocation> {
        @Override
        public CommandContainer<CommandInvocation> create(Class<? extends Command> command)
                throws CommandLineParserException {
            customBuilderCallCount.incrementAndGet();
            return super.create(command);
        }
    }

    @Test
    public void testCustomBuilderUsedForLazyChildren() {
        customBuilderCallCount.set(0);

        AeshRuntimeRunner.builder()
                .containerBuilder(new TrackingContainerBuilder())
                .command(AppGroupCmd.class)
                .args("sub1", "--name", "test")
                .execute();

        // The custom builder should have been called for:
        // 1. The root group command (AppGroupCmd)
        // 2. The lazy child (Sub1Cmd) when resolved at parse time
        assertTrue("Custom builder should be called at least twice (root + child), was: "
                + customBuilderCallCount.get(), customBuilderCallCount.get() >= 2);
    }

    @Test
    public void testCustomBuilderUsedForAllLazyChildren() {
        customBuilderCallCount.set(0);

        AeshRuntimeRunner.builder()
                .containerBuilder(new TrackingContainerBuilder())
                .command(AppGroupCmd.class)
                .args("--help")
                .execute();

        // --help resolves ALL lazy children. Custom builder should be called for:
        // 1. Root group command
        // 2. Sub1Cmd
        // 3. Sub2Cmd
        assertTrue("Custom builder should be called at least 3 times (root + 2 children), was: "
                + customBuilderCallCount.get(), customBuilderCallCount.get() >= 3);
    }

    @Test
    public void testCustomBuilderPropagatedToNestedGroups() {
        customBuilderCallCount.set(0);

        AeshRuntimeRunner.builder()
                .containerBuilder(new TrackingContainerBuilder())
                .command(OuterGroupCmd.class)
                .args("mid", "leaf", "--value", "test")
                .execute();

        // Custom builder should be called for:
        // 1. OuterGroupCmd (root)
        // 2. MidGroupCmd (lazy child of outer)
        // 3. LeafCmd (lazy child of mid)
        assertTrue("Custom builder should be called at least 3 times (root + mid + leaf), was: "
                + customBuilderCallCount.get(), customBuilderCallCount.get() >= 3);
    }

    @Test
    public void testDefaultBuilderStillWorks() {
        // Without a custom builder, everything should work as before
        CommandResult result = AeshRuntimeRunner.builder()
                .command(AppGroupCmd.class)
                .args("sub1", "--name", "hello")
                .execute();

        assertEquals(CommandResult.SUCCESS, result);
    }

    // --- Test commands ---

    @CommandDefinition(name = "sub1", description = "Sub command 1")
    public static class Sub1Cmd implements Command<CommandInvocation> {
        @Option
        public String name;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "sub2", description = "Sub command 2")
    public static class Sub2Cmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "app", description = "App group", groupCommands = { Sub1Cmd.class, Sub2Cmd.class })
    public static class AppGroupCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    // --- Nested group commands for propagation test ---

    @CommandDefinition(name = "leaf", description = "Leaf command")
    public static class LeafCmd implements Command<CommandInvocation> {
        @Option
        public String value;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "mid", description = "Mid group", groupCommands = { LeafCmd.class })
    public static class MidGroupCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "outer", description = "Outer group", groupCommands = { MidGroupCmd.class })
    public static class OuterGroupCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }
}
