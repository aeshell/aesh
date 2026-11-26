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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ServiceLoader;

import org.aesh.command.Command;

/**
 * Registry that discovers {@link MetadataRegistry} implementations via
 * {@link ServiceLoader} and lazily looks up {@link CommandMetadataProvider}
 * instances on demand. Only the requested command's metadata class is loaded
 * and instantiated — other commands remain untouched.
 * <p>
 * Uses {@link ClassValue} for per-class caching, which is automatically
 * garbage-collected when a classloader is unloaded. This avoids
 * {@code ClassCastException} when frameworks reload classes with a new
 * classloader (e.g., Quarkus dev mode, {@code @QuarkusMainTest}).
 * <p>
 * Thread-safe with lazy initialization and per-command caching.
 *
 * @author Aesh team
 */
public final class MetadataProviderRegistry {

    /**
     * Registries registered explicitly through {@link #register} or named by
     * {@link #useOnly}. Consulted first on every lookup and populated with
     * zero class path I/O, so {@code register(...)} never triggers a scan
     * (#794).
     */
    private static volatile List<MetadataRegistry> explicitRegistries;

    /**
     * Registries discovered from the class path, loaded lazily on the first
     * lookup that no explicit registry can answer. Cached per context
     * classloader so a fresh classloader (dev-mode reload) gets a fresh
     * scan while explicit registrations survive.
     */
    private static volatile List<MetadataRegistry> discoveredRegistries;
    private static volatile ClassLoader discoveredRegistriesClassLoader;

    /** True while a {@link #useOnly} set stands: discovery is skipped entirely. */
    private static volatile boolean discoveryDisabled;

    /**
     * Bumped every time the meaning of the registry set changes
     * ({@link #register} adding a registry, {@link #useOnly} replacing the
     * set, {@link #reset}, a discovery load, or a classloader-triggered
     * reload). Cached entries computed under an older generation are
     * recomputed on next lookup.
     */
    private static volatile long cacheGeneration;

    /**
     * Cached lookup plus the generation it was computed under. Comparing
     * the generation on read invalidates entries without enumerating
     * {@code ClassValue} keys and without giving up per-classloader
     * garbage collection.
     */
    private static final class CacheEntry {
        final CommandMetadataProvider<?> provider;
        final long generation;

        CacheEntry(CommandMetadataProvider<?> provider, long generation) {
            this.provider = provider;
            this.generation = generation;
        }
    }

    /**
     * Per-class cache using ClassValue. Entries are automatically removed
     * when the class's classloader is garbage-collected, preventing stale
     * cross-classloader references (#519).
     */
    private static final ClassValue<CacheEntry> cache = new ClassValue<CacheEntry>() {
        @Override
        protected CacheEntry computeValue(Class<?> cls) {
            // Walk the class hierarchy to support CDI interceptor/proxy
            // subclasses whose runtime class differs from the original
            // @CommandDefinition-annotated class (#608)
            Class<?> current = cls;
            while (current != null && current != Object.class) {
                String className = current.getName();
                // Explicit registries answer without any class path I/O;
                // discovery runs only when they cannot answer (#794)
                for (MetadataRegistry registry : snapshotExplicitRegistries()) {
                    @SuppressWarnings("rawtypes")
                    CommandMetadataProvider provider = registry.get(className);
                    if (provider != null) {
                        return new CacheEntry(provider, cacheGeneration);
                    }
                }
                for (MetadataRegistry registry : discoveryRegistries()) {
                    @SuppressWarnings("rawtypes")
                    CommandMetadataProvider provider = registry.get(className);
                    if (provider != null) {
                        return new CacheEntry(provider, cacheGeneration);
                    }
                }
                current = current.getSuperclass();
            }
            return new CacheEntry(ABSENT, cacheGeneration);
        }
    };

    /** Sentinel for command classes that have no generated provider. Must never escape getProvider(). */
    @SuppressWarnings("rawtypes")
    private static final CommandMetadataProvider ABSENT = new CommandMetadataProvider() {
        public Class commandType() {
            throw new UnsupportedOperationException("ABSENT sentinel");
        }

        public Command newInstance() {
            throw new UnsupportedOperationException("ABSENT sentinel");
        }

        public org.aesh.command.impl.internal.ProcessedCommand buildProcessedCommand(Command instance) {
            throw new UnsupportedOperationException("ABSENT sentinel");
        }

        public boolean isGroupCommand() {
            throw new UnsupportedOperationException("ABSENT sentinel");
        }

        public Class[] groupCommandClasses() {
            throw new UnsupportedOperationException("ABSENT sentinel");
        }

        public String commandName() {
            throw new UnsupportedOperationException("ABSENT sentinel");
        }
    };

    private MetadataProviderRegistry() {
    }

    /**
     * Explicitly register a {@link MetadataRegistry} instance.
     * <p>
     * This is useful for:
     * <ul>
     * <li>Native images built with {@code -H:-UseServiceLoaderFeature}</li>
     * <li>Frameworks (Quarkus, Spring) that manage component lifecycle</li>
     * <li>Testing with mock registries</li>
     * </ul>
     * <p>
     * The registration itself performs no class path I/O: lookups consult
     * explicitly registered registries first and fall back to class path
     * discovery only when they cannot answer, so an application whose
     * explicit registry covers its commands never pays the discovery scan
     * (#794). Registered instances keep precedence over discovery; among
     * explicit registrations the most recently registered wins.
     *
     * @param registry the registry to register
     * @since 3.16
     */
    public static void register(MetadataRegistry registry) {
        if (registry == null)
            return;
        synchronized (MetadataProviderRegistry.class) {
            List<MetadataRegistry> current = explicitRegistries;
            if (current == null) {
                List<MetadataRegistry> updated = new ArrayList<>(1);
                updated.add(registry);
                explicitRegistries = updated;
                cacheGeneration++;
            } else if (!current.contains(registry)) {
                // Prepend so explicit registrations take precedence over
                // discovery and over earlier registrations; cached lookups
                // from the older generation are recomputed on next read.
                List<MetadataRegistry> updated = new ArrayList<>(current.size() + 1);
                updated.add(registry);
                updated.addAll(current);
                explicitRegistries = updated;
                cacheGeneration++;
            }
        }
    }

    /**
     * Use exactly these registries and skip class path scanning.
     * <p>
     * Discovering registries opens every JAR on the class path, which costs
     * tens of milliseconds per start on large class paths. An application
     * that knows its registries can skip the scan entirely by naming them
     * here, including aesh's own internal one when alias/export commands
     * are used:
     *
     * <pre>
     * MetadataProviderRegistry.useOnly(new InternalCommandMetadataRegistry(), myRegistry);
     * </pre>
     * <p>
     * No scan happens while this set stands: lookups consult the given
     * registries in order, first hit wins. A later {@link #register} call
     * prepends to the set (explicit registrations take precedence);
     * {@link #reset} clears the set and re-enables discovery. If the context
     * classloader changes afterwards, the existing reload applies, so call
     * this once the classloader is stable (a single call at startup suffices
     * for single-classloader CLIs).
     *
     * @param registries the only registries to consult, in precedence order;
     *        null is ignored, null elements are skipped
     */
    public static void useOnly(MetadataRegistry... registries) {
        if (registries == null)
            return;
        synchronized (MetadataProviderRegistry.class) {
            List<MetadataRegistry> fixed = new ArrayList<>(registries.length);
            for (MetadataRegistry registry : registries) {
                if (registry != null && !fixed.contains(registry))
                    fixed.add(registry);
            }
            explicitRegistries = fixed;
            discoveryDisabled = true;
            discoveredRegistries = null;
            discoveredRegistriesClassLoader = null;
            cacheGeneration++;
        }
    }

    /**
     * Look up a provider for the given command class.
     * <p>
     * On first call for a given class, queries all discovered
     * {@link MetadataRegistry} instances. The result is cached for
     * subsequent lookups. Negative results (no provider found) are
     * also cached to avoid repeated registry iteration.
     * <p>
     * Entries computed before the latest {@link #register} or
     * {@link #reset} are recomputed on next lookup, so both negative and
     * positive entries always reflect registry changes.
     * <p>
     * The cache is per-classloader: when a classloader is garbage-collected,
     * all cached entries for its classes are automatically removed.
     *
     * @param commandClass the command class to look up
     * @param <C> the command type
     * @return the provider, or null if no generated provider exists
     */
    @SuppressWarnings("unchecked")
    public static <C extends Command> CommandMetadataProvider<C> getProvider(Class<C> commandClass) {
        CacheEntry entry = cache.get(commandClass);
        if (entry.generation != cacheGeneration) {
            // Races only cause a benign duplicate recompute: lookup is
            // side-effect free, and the fresher generation always wins on
            // the next read.
            cache.remove(commandClass);
            entry = cache.get(commandClass);
        }
        return entry.provider == ABSENT ? null : (CommandMetadataProvider<C>) entry.provider;
    }

    /**
     * Look up generated method-command providers for one owning class.
     * <p>
     * Deliberately uncached: lookups happen at container-build time, never
     * on a hot path, and the per-class ClassValue above cannot key lists.
     *
     * @param ownerClass the class declaring {@code @MethodCommand} methods
     * @return the method providers, or an empty list if none
     */
    public static List<MethodCommandMetadataProvider> getMethodProviders(Class<?> ownerClass) {
        List<MethodCommandMetadataProvider> result = new ArrayList<>();
        if (ownerClass == null)
            return result;
        String ownerName = ownerClass.getName();
        for (MetadataRegistry registry : snapshotExplicitRegistries()) {
            collectMethodProviders(registry, ownerName, result);
        }
        for (MetadataRegistry registry : discoveryRegistries()) {
            collectMethodProviders(registry, ownerName, result);
        }
        return result;
    }

    private static void collectMethodProviders(MetadataRegistry registry, String ownerName,
            List<MethodCommandMetadataProvider> result) {
        try {
            List<MethodCommandMetadataProvider> providers = registry.methodProviders(ownerName);
            if (providers != null)
                result.addAll(providers);
        } catch (Exception ignored) {
            // A broken registry must not break unrelated lookups
        }
    }

    /** Explicit registries as an immutable snapshot (empty when none). */
    private static List<MetadataRegistry> snapshotExplicitRegistries() {
        List<MetadataRegistry> explicit = explicitRegistries;
        return explicit != null ? explicit : Collections.emptyList();
    }

    /**
     * Discovered registries, loading them lazily: the class path scan runs
     * only on the first lookup that no explicit registry can answer, and is
     * re-run when the context classloader changes (a fresh classloader may
     * carry fresh generated registries, e.g. dev-mode reloads). Explicit
     * registrations survive a reload (#794).
     */
    private static List<MetadataRegistry> discoveryRegistries() {
        if (discoveryDisabled)
            return Collections.emptyList();
        ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
        List<MetadataRegistry> result = discoveredRegistries;
        if (result == null || discoveredRegistriesClassLoader != currentCL) {
            synchronized (MetadataProviderRegistry.class) {
                result = discoveredRegistries;
                if (result == null || discoveredRegistriesClassLoader != currentCL) {
                    result = loadRegistries();
                    discoveredRegistries = result;
                    discoveredRegistriesClassLoader = currentCL;
                    // The discovery result changed out from under cached
                    // entries; force them to recompute.
                    cacheGeneration++;
                }
            }
        }
        return result;
    }

    private static final String REGISTRY_RESOURCE = "META-INF/aesh/registry";

    private static List<MetadataRegistry> loadRegistries() {
        List<MetadataRegistry> list = new ArrayList<>();

        // 1. Try resource-file discovery (works without ServiceLoader and
        //    without -H:+UseServiceLoaderFeature in native-image, #540)
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null)
                cl = MetadataProviderRegistry.class.getClassLoader();
            Enumeration<URL> resources = cl.getResources(REGISTRY_RESOURCE);
            while (resources.hasMoreElements()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resources.nextElement().openStream()))) {
                    String className;
                    while ((className = reader.readLine()) != null) {
                        className = className.trim();
                        if (className.isEmpty() || className.startsWith("#"))
                            continue;
                        try {
                            Class<?> cls = cl.loadClass(className);
                            list.add((MetadataRegistry) cls.getDeclaredConstructor().newInstance());
                        } catch (Exception ignored) {
                            // Class not found or not instantiable — skip
                        }
                    }
                } catch (IOException ignored) {
                }
            }
        } catch (IOException ignored) {
        }

        // 2. Fall back to ServiceLoader (backward compatible)
        if (list.isEmpty()) {
            ServiceLoader<MetadataRegistry> loader = ServiceLoader.load(MetadataRegistry.class);
            for (MetadataRegistry registry : loader) {
                list.add(registry);
            }
        }

        return list;
    }

    /**
     * Reset the registry, clearing explicit registrations and forcing
     * re-discovery on next access.
     * <p>
     * Note: with the ClassValue-based cache, explicit reset is typically
     * not needed for classloader changes — the cache is automatically
     * GC'd when the classloader is collected. This method is still useful
     * for test frameworks that reuse the same classloader but need to
     * force re-discovery of registries. Cached entries from before the
     * reset are recomputed on next lookup.
     */
    public static void reset() {
        synchronized (MetadataProviderRegistry.class) {
            explicitRegistries = null;
            discoveredRegistries = null;
            discoveredRegistriesClassLoader = null;
            discoveryDisabled = false;
            cacheGeneration++;
        }
    }
}
