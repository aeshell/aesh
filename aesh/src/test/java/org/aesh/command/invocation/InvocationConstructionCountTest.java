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
package org.aesh.command.invocation;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.invocation.DefaultCommandInvocationBuilder;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.registry.CommandRegistry;
import org.junit.Test;

/**
 * Establishes how often invocation builders/providers run per execution:
 * {@code ExecutionImpl} builds a fresh invocation for population context,
 * sub-command checks and the command call itself (#665). Sharing one
 * instance was evaluated and rejected: the temporaries scalar-replace
 * today, so caching them onto the heap regresses allocation, and the
 * build/enhance call counts are observable to custom providers. This
 * test pins the rebuild contract.
 */
public class InvocationConstructionCountTest {

    @CommandDefinition(name = "probe", description = "probe")
    public static class ProbeCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    static final AtomicInteger builds = new AtomicInteger();
    static final AtomicInteger enhances = new AtomicInteger();

    public static class CountingBuilder implements CommandInvocationBuilder<CommandInvocation> {
        private final DefaultCommandInvocationBuilder delegate = new DefaultCommandInvocationBuilder(null);

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public CommandInvocation build(CommandRuntime<CommandInvocation> runtime,
                CommandInvocationConfiguration configuration,
                CommandContainer<CommandInvocation> commandContainer) {
            builds.incrementAndGet();
            return ((CommandInvocationBuilder) delegate).build(runtime, configuration,
                    commandContainer);
        }
    }

    public static class CountingProvider implements CommandInvocationProvider<CommandInvocation> {
        @Override
        public CommandInvocation enhanceCommandInvocation(CommandInvocation invocation) {
            enhances.incrementAndGet();
            return invocation;
        }
    }

    @Test
    public void testInvocationsBuiltPerExecution() throws Exception {
        builds.set(0);
        enhances.set(0);
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder
                .<CommandInvocation> builder()
                .command(ProbeCommand.class)
                .create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder
                .<CommandInvocation> builder()
                .commandRegistry(registry)
                .commandInvocationBuilder(new CountingBuilder())
                .commandInvocationProvider(new CountingProvider())
                .build();

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("probe"));

        // Population context, sub-command check and command call each
        // build (and enhance) exactly once: three fresh invocations per
        // execution. Do not share: the call counts are observable to
        // custom providers, and caching regresses allocation (the
        // temporaries scalar-replace today, a cached instance escapes).
        assertEquals(3, builds.get());
        assertEquals(3, enhances.get());
    }
}
