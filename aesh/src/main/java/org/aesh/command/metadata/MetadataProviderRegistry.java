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

import java.util.ArrayList;
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

    private static volatile List<MetadataRegistry> registries;
    private static volatile ClassLoader registriesClassLoader;

    /**
     * Per-class cache using ClassValue. Entries are automatically removed
     * when the class's classloader is garbage-collected, preventing stale
     * cross-classloader references (#519).
     */
    private static final ClassValue<CommandMetadataProvider<?>> cache = new ClassValue<CommandMetadataProvider<?>>() {
        @Override
        protected CommandMetadataProvider<?> computeValue(Class<?> cls) {
            String className = cls.getName();
            for (MetadataRegistry registry : getRegistries()) {
                @SuppressWarnings("rawtypes")
                CommandMetadataProvider provider = registry.get(className);
                if (provider != null) {
                    return provider;
                }
            }
            return ABSENT;
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
     * Look up a provider for the given command class.
     * <p>
     * On first call for a given class, queries all discovered
     * {@link MetadataRegistry} instances. The result is cached for
     * subsequent lookups. Negative results (no provider found) are
     * also cached to avoid repeated registry iteration.
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
        CommandMetadataProvider<?> result = cache.get(commandClass);
        return result == ABSENT ? null : (CommandMetadataProvider<C>) result;
    }

    private static List<MetadataRegistry> getRegistries() {
        ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
        List<MetadataRegistry> result = registries;
        // Reload if registries haven't been loaded yet or classloader changed
        if (result == null || registriesClassLoader != currentCL) {
            synchronized (MetadataProviderRegistry.class) {
                result = registries;
                if (result == null || registriesClassLoader != currentCL) {
                    result = loadRegistries();
                    registries = result;
                    registriesClassLoader = currentCL;
                }
            }
        }
        return result;
    }

    private static List<MetadataRegistry> loadRegistries() {
        List<MetadataRegistry> list = new ArrayList<>();
        ServiceLoader<MetadataRegistry> loader = ServiceLoader.load(MetadataRegistry.class);
        for (MetadataRegistry registry : loader) {
            list.add(registry);
        }
        return list;
    }

    /**
     * Reset the registry, forcing re-discovery on next access.
     * <p>
     * Note: with the ClassValue-based cache, explicit reset is typically
     * not needed for classloader changes — the cache is automatically
     * GC'd when the classloader is collected. This method is still useful
     * for test frameworks that reuse the same classloader but need to
     * force re-discovery of registries.
     */
    public static void reset() {
        synchronized (MetadataProviderRegistry.class) {
            registries = null;
            registriesClassLoader = null;
        }
    }
}
