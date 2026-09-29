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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

    // --- register() API tests (#540) ---

    @Test
    public void testRegisterExplicitly() {
        MetadataProviderRegistry.reset();
        MetadataProviderRegistry.register(commandClassName -> {
            if (commandClassName.equals(RegisterTestCommand.class.getName())) {
                return new StubProvider();
            }
            return null;
        });

        CommandMetadataProvider<?> provider = MetadataProviderRegistry.getProvider(RegisterTestCommand.class);
        assertNotNull("Explicitly registered provider should be found", provider);
        assertEquals("register-test", provider.commandName());
        MetadataProviderRegistry.reset();
    }

    @Test
    public void testRegisterNull() {
        // Should not throw
        MetadataProviderRegistry.register(null);
    }

    // --- Invalidation on register/reset (#647) ---

    @Test
    public void testStaleAbsentInvalidatedByRegister() {
        MetadataProviderRegistry.reset();
        // Caches the ABSENT sentinel for this class.
        assertNull(MetadataProviderRegistry.getProvider(InvalidationProbeCommand.class));

        MetadataProviderRegistry.register(commandClassName -> {
            if (commandClassName.equals(InvalidationProbeCommand.class.getName()))
                return new InvalidationProbeProvider();
            return null;
        });

        CommandMetadataProvider<?> provider = MetadataProviderRegistry.getProvider(InvalidationProbeCommand.class);
        assertNotNull("Registered provider must become visible", provider);
        assertEquals("invalidation-probe", provider.commandName());
    }

    @Test
    public void testStalePositiveInvalidatedByResetAndRegister() {
        MetadataProviderRegistry.reset();
        MetadataProviderRegistry.register(commandClassName -> {
            if (commandClassName.equals(PrecedenceProbeCommand.class.getName()))
                return new PrecedenceProviderA();
            return null;
        });
        CommandMetadataProvider<?> first = MetadataProviderRegistry.getProvider(PrecedenceProbeCommand.class);
        assertNotNull(first);
        assertEquals("probe-a", first.commandName());

        MetadataProviderRegistry.reset();
        MetadataProviderRegistry.register(commandClassName -> {
            if (commandClassName.equals(PrecedenceProbeCommand.class.getName()))
                return new PrecedenceProviderB();
            return null;
        });

        CommandMetadataProvider<?> second = MetadataProviderRegistry.getProvider(PrecedenceProbeCommand.class);
        assertNotNull(second);
        assertEquals("Replacement provider must win after reset", "probe-b", second.commandName());
        assertNotSame(first, second);
    }

    @Test
    public void testLatestExplicitRegistrationWins() {
        MetadataProviderRegistry.reset();
        MetadataProviderRegistry.register(commandClassName -> {
            if (commandClassName.equals(PrecedenceProbeCommand.class.getName()))
                return new PrecedenceProviderA();
            return null;
        });
        MetadataProviderRegistry.register(commandClassName -> {
            if (commandClassName.equals(PrecedenceProbeCommand.class.getName()))
                return new PrecedenceProviderB();
            return null;
        });

        // Explicit registrations are consulted before discovery, most
        // recently registered first.
        CommandMetadataProvider<?> provider = MetadataProviderRegistry.getProvider(PrecedenceProbeCommand.class);
        assertNotNull(provider);
        assertEquals("probe-b", provider.commandName());
    }

    @Test(timeout = 30000)
    public void testConcurrentLookupRegisterReset() throws Exception {
        MetadataProviderRegistry.reset();
        // Warm up a stale ABSENT entry: without invalidation the final
        // assertion below could never observe the replacement.
        assertNull(MetadataProviderRegistry.getProvider(ConcurrentProbeCommand.class));

        int lookupThreads = 4;
        int iterations = 200;
        CountDownLatch started = new CountDownLatch(lookupThreads + 1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        ExecutorService executor = Executors.newFixedThreadPool(lookupThreads + 1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < lookupThreads; t++) {
                futures.add(executor.submit(() -> {
                    started.countDown();
                    try {
                        if (!release.await(30, TimeUnit.SECONDS))
                            throw new IllegalStateException("test gate was never released");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                    for (int i = 0; i < iterations; i++) {
                        // Mid-churn answers vary; only absence of failure matters.
                        MetadataProviderRegistry.getProvider(ConcurrentProbeCommand.class);
                    }
                    return null;
                }));
            }
            futures.add(executor.submit(() -> {
                started.countDown();
                try {
                    if (!release.await(30, TimeUnit.SECONDS))
                        throw new IllegalStateException("test gate was never released");
                    // Churn registrations with distinct instances so each one
                    // actually changes the registry set.
                    for (int i = 0; i < 20; i++) {
                        MetadataProviderRegistry.register(commandClassName -> null);
                        MetadataProviderRegistry.reset();
                    }
                } catch (Throwable e) {
                    failure.set(e);
                }
                return null;
            }));

            assertTrue(started.await(10, TimeUnit.SECONDS));
            release.countDown();
            for (Future<?> f : futures) {
                f.get();
            }
            assertNull("Churn must stay quiet, got: " + failure.get(), failure.get());
        } finally {
            executor.shutdownNow();
        }

        // Quiesced final state is deterministic: the replacement is visible.
        MetadataProviderRegistry.reset();
        MetadataProviderRegistry.register(commandClassName -> {
            if (commandClassName.equals(ConcurrentProbeCommand.class.getName()))
                return new ConcurrentProbeProvider();
            return null;
        });
        CommandMetadataProvider<?> provider = MetadataProviderRegistry.getProvider(ConcurrentProbeCommand.class);
        assertNotNull(provider);
        assertEquals("concurrent-probe", provider.commandName());
    }

    @Test
    public void testReloadedClassResolvesWithoutCrossLoaderLeak() throws Exception {
        MetadataProviderRegistry.reset();
        MetadataProviderRegistry.register(commandClassName -> {
            if (commandClassName.equals(LoaderProbeCommand.class.getName()))
                return new LoaderProbeProvider();
            return null;
        });
        assertEquals("loader-probe",
                MetadataProviderRegistry.getProvider(LoaderProbeCommand.class).commandName());

        // Same binary name through an isolated child loader: an independent
        // cache entry with the same name-keyed answer, no linkage errors.
        Class<?> reloaded = childFirstLoad(LoaderProbeCommand.class.getName());
        assertNotSame(LoaderProbeCommand.class, reloaded);
        assertEquals("loader-probe", lookup(reloaded).commandName());

        // Invalidation applies across loaders afterwards.
        MetadataProviderRegistry.reset();
        MetadataProviderRegistry.register(commandClassName -> {
            if (commandClassName.equals(LoaderProbeCommand.class.getName()))
                return new LoaderProbeReplacement();
            return null;
        });
        assertEquals("loader-probe-2",
                MetadataProviderRegistry.getProvider(LoaderProbeCommand.class).commandName());
        assertEquals("loader-probe-2", lookup(reloaded).commandName());
    }

    @SuppressWarnings("unchecked")
    private static CommandMetadataProvider<?> lookup(Class<?> commandClass) {
        return MetadataProviderRegistry.getProvider((Class<? extends Command>) commandClass);
    }

    /**
     * Loads one class child-first from the test-classes output, delegating
     * everything else to the parent loader.
     */
    private static Class<?> childFirstLoad(String className) throws Exception {
        URL classesDir = LoaderProbeCommand.class.getProtectionDomain().getCodeSource().getLocation();
        ClassLoader parent = MetadataProviderRegistryTest.class.getClassLoader();
        ClassLoader child = new URLClassLoader(new URL[] { classesDir }, parent) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null && name.equals(className)) {
                        try {
                            loaded = findClass(name);
                        } catch (ClassNotFoundException e) {
                            // fall through to parent below
                        }
                    }
                    if (loaded == null)
                        loaded = super.loadClass(name, false);
                    if (resolve)
                        resolveClass(loaded);
                    return loaded;
                }
            }
        };
        return Class.forName(className, false, child);
    }

    @CommandDefinition(name = "register-test", description = "Test for explicit registration")
    public static class RegisterTestCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static class StubProvider implements CommandMetadataProvider<RegisterTestCommand> {
        public Class<RegisterTestCommand> commandType() {
            return RegisterTestCommand.class;
        }

        public RegisterTestCommand newInstance() {
            return new RegisterTestCommand();
        }

        public org.aesh.command.impl.internal.ProcessedCommand buildProcessedCommand(
                RegisterTestCommand instance) {
            return null;
        }

        public boolean isGroupCommand() {
            return false;
        }

        public Class[] groupCommandClasses() {
            return new Class[0];
        }

        public String commandName() {
            return "register-test";
        }
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

    // --- Invalidation fixtures (#647): dedicated classes per case so no
    // other test can pre-populate their cache entries ---

    public static class InvalidationProbeCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    public static class PrecedenceProbeCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    public static class ConcurrentProbeCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    /**
     * Self-contained on purpose: reloaded through an isolated child loader,
     * so it must not reference the enclosing test class.
     */
    public static class LoaderProbeCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    private abstract static class NamedProvider<C extends Command> implements CommandMetadataProvider<C> {
        private final Class<C> type;
        private final String name;

        NamedProvider(Class<C> type, String name) {
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
        public org.aesh.command.impl.internal.ProcessedCommand buildProcessedCommand(C instance) {
            return null;
        }

        @Override
        public boolean isGroupCommand() {
            return false;
        }

        @Override
        @SuppressWarnings("rawtypes")
        public Class[] groupCommandClasses() {
            return new Class[0];
        }

        @Override
        public String commandName() {
            return name;
        }
    }

    private static class InvalidationProbeProvider extends NamedProvider<InvalidationProbeCommand> {
        InvalidationProbeProvider() {
            super(InvalidationProbeCommand.class, "invalidation-probe");
        }
    }

    private static class PrecedenceProviderA extends NamedProvider<PrecedenceProbeCommand> {
        PrecedenceProviderA() {
            super(PrecedenceProbeCommand.class, "probe-a");
        }
    }

    private static class PrecedenceProviderB extends NamedProvider<PrecedenceProbeCommand> {
        PrecedenceProviderB() {
            super(PrecedenceProbeCommand.class, "probe-b");
        }
    }

    private static class ConcurrentProbeProvider extends NamedProvider<ConcurrentProbeCommand> {
        ConcurrentProbeProvider() {
            super(ConcurrentProbeCommand.class, "concurrent-probe");
        }
    }

    private static class LoaderProbeProvider extends NamedProvider<LoaderProbeCommand> {
        LoaderProbeProvider() {
            super(LoaderProbeCommand.class, "loader-probe");
        }
    }

    private static class LoaderProbeReplacement extends NamedProvider<LoaderProbeCommand> {
        LoaderProbeReplacement() {
            super(LoaderProbeCommand.class, "loader-probe-2");
        }
    }
}
