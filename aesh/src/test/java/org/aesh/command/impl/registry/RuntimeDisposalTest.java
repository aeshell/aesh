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
package org.aesh.command.impl.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.ReadlineConsole;
import org.aesh.readline.prompt.Prompt;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Runtime listener lifetime: runtimes register on their (possibly
 * long-lived, shared) registry and must detach explicitly when discarded.
 * Finalizers never reliably ran, so disposal is an explicit lifecycle
 * instead (#666).
 */
public class RuntimeDisposalTest {

    @CommandDefinition(name = "probe", description = "probe")
    public static class ProbeCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    private static MutableCommandRegistryImpl<CommandInvocation> registryWithProbe() throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        registry.addCommand(ProbeCommand.class);
        assertEquals(0, registry.listenerCount());
        return registry;
    }

    @Test
    public void testUndisposedRuntimesAccumulate() throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = registryWithProbe();
        List<CommandRuntime<CommandInvocation>> runtimes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            runtimes.add(AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                    .commandRegistry(registry)
                    .build());
            assertEquals(i + 1, registry.listenerCount());
        }
        for (CommandRuntime<CommandInvocation> runtime : runtimes)
            runtime.close();
        assertEquals(0, registry.listenerCount());
    }

    @Test
    public void testCloseIsIdempotentAndRegistryStaysUsable() throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = registryWithProbe();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder
                .<CommandInvocation> builder()
                .commandRegistry(registry)
                .build();
        assertEquals(1, registry.listenerCount());

        runtime.close();
        runtime.close();
        assertEquals(0, registry.listenerCount());

        // The externally owned registry keeps working; a fresh runtime
        // executes against it without stale interference.
        CommandRuntime<CommandInvocation> fresh = AeshCommandRuntimeBuilder
                .<CommandInvocation> builder()
                .commandRegistry(registry)
                .build();
        try {
            assertEquals(CommandResult.SUCCESS, fresh.executeCommand("probe"));
        } finally {
            fresh.close();
        }
        assertEquals(0, registry.listenerCount());
    }

    @Test
    public void testClosedRuntimeIsReclaimable() throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = registryWithProbe();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder
                .<CommandInvocation> builder()
                .commandRegistry(registry)
                .build();
        WeakReference<CommandRuntime<CommandInvocation>> ref = new WeakReference<>(runtime);
        runtime.close();
        runtime = null;

        boolean cleared = false;
        for (int i = 0; i < 100 && !cleared; i++) {
            System.gc();
            Thread.sleep(50);
            cleared = ref.get() == null;
        }
        assertTrue("closed runtime must be reclaimable", cleared);
    }

    @Test
    public void testConsoleStopDetachesRuntime() throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = registryWithProbe();
        TestConnection connection = new TestConnection();
        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();
        try {
            assertEquals(1, registry.listenerCount());
        } finally {
            console.stop();
        }
        assertEquals(0, registry.listenerCount());
    }

    @Test
    public void testConsoleRestartDoesNotAccumulate() throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = registryWithProbe();
        TestConnection connection = new TestConnection();
        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();
        assertEquals(1, registry.listenerCount());
        console.stop();
        assertEquals(0, registry.listenerCount());
        console.start();
        try {
            assertEquals("restart must replace, not accumulate", 1, registry.listenerCount());
        } finally {
            console.stop();
        }
        assertEquals(0, registry.listenerCount());
    }
}
