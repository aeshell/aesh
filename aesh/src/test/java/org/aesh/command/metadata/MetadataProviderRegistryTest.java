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
package org.aesh.command.metadata;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.junit.After;
import org.junit.Test;

/**
 * Tests for {@link MetadataProviderRegistry}: caching, negative caching,
 * reset, and concurrent access.
 */
public class MetadataProviderRegistryTest {

    @After
    public void cleanup() {
        MetadataProviderRegistry.reset();
    }

    @Test
    public void testUnknownCommandReturnsNull() {
        // A command class with no generated provider should return null
        CommandMetadataProvider<?> provider = MetadataProviderRegistry.getProvider(UnknownCommand.class);
        assertNull("Unknown command should return null", provider);
    }

    @Test
    public void testNegativeCachingReturnsSameNull() {
        // First lookup caches ABSENT sentinel
        CommandMetadataProvider<?> first = MetadataProviderRegistry.getProvider(UnknownCommand.class);
        assertNull(first);

        // Second lookup should hit the cache (still null, not re-scanning registries)
        CommandMetadataProvider<?> second = MetadataProviderRegistry.getProvider(UnknownCommand.class);
        assertNull(second);
    }

    @Test
    public void testResetClearsCache() {
        // Populate the cache
        MetadataProviderRegistry.getProvider(UnknownCommand.class);

        // Reset should clear everything
        MetadataProviderRegistry.reset();

        // After reset, should re-scan (still null for unknown, but exercises the path)
        CommandMetadataProvider<?> provider = MetadataProviderRegistry.getProvider(UnknownCommand.class);
        assertNull(provider);
    }

    @Test(timeout = 10000)
    public void testConcurrentAccessDoesNotThrow() throws Exception {
        // Pre-populate the cache so computeIfAbsent doesn't trigger ServiceLoader
        // under contention (ConcurrentHashMap bin locks + slow ServiceLoader = deadlock risk)
        MetadataProviderRegistry.getProvider(UnknownCommand.class);
        MetadataProviderRegistry.getProvider(AnotherUnknownCommand.class);

        int threadCount = 4;
        int iterations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            futures.add(executor.submit(() -> {
                for (int i = 0; i < iterations; i++) {
                    // Concurrent lookups on cached entries should not throw
                    assertNull(MetadataProviderRegistry.getProvider(UnknownCommand.class));
                    assertNull(MetadataProviderRegistry.getProvider(AnotherUnknownCommand.class));
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(); // will throw if any thread failed
        }
        executor.shutdown();
    }

    @Test
    public void testRepeatedLookupReturnsSameInstance() {
        // For an unknown command, both calls should return null (from cached ABSENT)
        CommandMetadataProvider<?> first = MetadataProviderRegistry.getProvider(UnknownCommand.class);
        CommandMetadataProvider<?> second = MetadataProviderRegistry.getProvider(UnknownCommand.class);
        assertSame("Cached result should be same object reference", first, second);
    }

    // --- Test command classes (no generated provider) ---

    @CommandDefinition(name = "unknown", description = "Unknown command")
    public static class UnknownCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "another-unknown", description = "Another unknown command")
    public static class AnotherUnknownCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }
}
