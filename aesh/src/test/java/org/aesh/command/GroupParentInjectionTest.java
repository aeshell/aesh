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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.aesh.command.activator.CommandActivator;
import org.aesh.command.impl.internal.ParsedCommand;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.option.ParentCommand;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.console.AeshContext;
import org.aesh.console.DefaultAeshContext;
import org.junit.Test;

/**
 * Verifies that {@code @ParentCommand} fields are injected in normal
 * (non-sub-command-mode) execution, where the invocation carries no
 * {@code CommandContext}. The selected child is linked to its direct parent
 * during population, so the reference is in place before validators,
 * activators and execution run (#685).
 */
public class GroupParentInjectionTest {

    @CommandDefinition(name = "cli", description = "root", groupCommands = { Health.class })
    public static class GroupCli implements Command<CommandInvocation> {
        @Option(name = "c", description = "connection string")
        String connectionString;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "health", description = "health check")
    public static class Health implements Command<CommandInvocation> {
        @ParentCommand
        GroupCli parent;

        static boolean executed;
        static String seenConnectionString;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            executed = true;
            seenConnectionString = parent != null ? parent.connectionString : null;
            return CommandResult.SUCCESS;
        }
    }

    private CommandRuntime<CommandInvocation> runtimeFor(Class<? extends Command> root) throws Exception {
        return runtimeFor(root, null);
    }

    private CommandRuntime<CommandInvocation> runtimeFor(Class<? extends Command> root, AeshContext context)
            throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder().command(root).create();
        AeshCommandRuntimeBuilder<CommandInvocation> builder = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(registry);
        if (context != null)
            builder.aeshContext(context);
        return builder.build();
    }

    @Test
    public void testParentInjectedInNormalMode() throws Exception {
        Health.executed = false;
        Health.seenConnectionString = null;
        CommandRuntime<CommandInvocation> runtime = runtimeFor(GroupCli.class);

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("cli --c conn health"));
        assertTrue("child must have executed", Health.executed);
        assertEquals("child must see the populated parent value", "conn", Health.seenConnectionString);
    }

    @Test
    public void testRepeatedExecutionSeesLatestValues() throws Exception {
        CommandRuntime<CommandInvocation> runtime = runtimeFor(GroupCli.class);

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("cli --c one health"));
        assertEquals("one", Health.seenConnectionString);
        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("cli --c two health"));
        assertEquals("two", Health.seenConnectionString);
    }

    @CommandDefinition(name = "root", description = "root", groupCommands = { Mid.class })
    public static class Root implements Command<CommandInvocation> {
        @Option(name = "c", description = "connection string")
        String connectionString;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mid", description = "middle", groupCommands = { Leaf.class })
    public static class Mid implements Command<CommandInvocation>, CommandLifecycle {
        @ParentCommand
        Root parent;

        static boolean afterParseRan;
        static AeshContext ctxAtAfterParse;

        @Override
        public void setContext(AeshContext context) {
            ctxAtAfterParse = context;
        }

        @Override
        public void afterParse() {
            afterParseRan = true;
        }

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "leaf", description = "leaf")
    public static class Leaf implements Command<CommandInvocation> {
        @ParentCommand
        Mid parent;

        static Class<?> seenParentClass;
        static String seenRootValue;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            seenParentClass = parent.getClass();
            seenRootValue = parent.parent.connectionString;
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testNestedGroupsInjectDirectParent() throws Exception {
        Leaf.seenParentClass = null;
        Leaf.seenRootValue = null;
        Mid.afterParseRan = false;
        Mid.ctxAtAfterParse = null;
        AeshContext ctx = new DefaultAeshContext();
        CommandRuntime<CommandInvocation> runtime = runtimeFor(Root.class, ctx);

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("root --c conn mid leaf"));
        assertEquals("leaf must link to its direct parent", Mid.class, Leaf.seenParentClass);
        assertEquals("values must flow through the whole chain", "conn", Leaf.seenRootValue);
        assertTrue("mid-level afterParse must run", Mid.afterParseRan);
        assertSame("mid must receive the runtime context", ctx, Mid.ctxAtAfterParse);
    }

    @CommandDefinition(name = "db", description = "root", groupCommands = { HealthCheck.class })
    public static class Db implements Command<CommandInvocation>, CommandLifecycle {
        @Option(name = "c", description = "connection string")
        String connectionString;

        boolean connected;

        @Override
        public void afterParse() {
            connected = connectionString != null;
        }

        public boolean isConnected() {
            return connected;
        }

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "health", description = "health check", activator = ConnectedOnly.class)
    public static class HealthCheck implements Command<CommandInvocation> {
        @ParentCommand
        Db parent;

        static boolean executed;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            executed = true;
            return CommandResult.SUCCESS;
        }
    }

    public static class ConnectedOnly implements CommandActivator {
        @Override
        public boolean isActivated(ParsedCommand command) {
            return ((HealthCheck) command.command()).parent.isConnected();
        }
    }

    @Test
    public void testActivatorSeesParentEstablishedState() throws Exception {
        HealthCheck.executed = false;
        CommandRuntime<CommandInvocation> runtime = runtimeFor(Db.class);

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("db --c conn health"));
        assertTrue("child must have executed once connected", HealthCheck.executed);
    }

    @Test
    public void testActivatorRejectsWhenParentNotConnected() throws Exception {
        HealthCheck.executed = false;
        CommandRuntime<CommandInvocation> runtime = runtimeFor(Db.class);

        try {
            runtime.executeCommand("db health");
            fail("expected rejection when the parent never connected");
        } catch (CommandException expected) {
            assertTrue(expected.getMessage().contains("not available"));
        }
        assertTrue("child must not have executed", !HealthCheck.executed);
    }
}
