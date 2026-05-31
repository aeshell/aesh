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
import java.util.concurrent.ConcurrentHashMap;

import org.aesh.command.Command;

/**
 * Registry that discovers {@link MetadataRegistry} implementations via
 * {@link ServiceLoader} and lazily looks up {@link CommandMetadataProvider}
 * instances on demand. Only the requested command's metadata class is loaded
 * and instantiated — other commands remain untouched.
 * <p>
 * Thread-safe with lazy initialization and per-command caching.
 *
 * @author Aesh team
 */
public final class MetadataProviderRegistry {

    private static volatile List<MetadataRegistry> registries;
    private static final ConcurrentHashMap<Class<?>, CommandMetadataProvider<?>> cache = new ConcurrentHashMap<>();

    /** Sentinel value cached when no provider exists for a command class. */
    private static final CommandMetadataProvider<?> ABSENT = null;

    private MetadataProviderRegistry() {
    }

    /**
     * Look up a provider for the given command class.
     * <p>
     * On first call for a given class, queries all discovered
     * {@link MetadataRegistry} instances. The result is cached for
     * subsequent lookups.
     *
     * @param commandClass the command class to look up
     * @param <C> the command type
     * @return the provider, or null if no generated provider exists
     */
    @SuppressWarnings("unchecked")
    public static <C extends Command> CommandMetadataProvider<C> getProvider(Class<C> commandClass) {
        CommandMetadataProvider<?> cached = cache.get(commandClass);
        if (cached != null) {
            return (CommandMetadataProvider<C>) cached;
        }

        String className = commandClass.getName();
        for (MetadataRegistry registry : getRegistries()) {
            CommandMetadataProvider<?> provider = registry.get(className);
            if (provider != null) {
                cache.put(commandClass, provider);
                return (CommandMetadataProvider<C>) provider;
            }
        }
        return null;
    }

    private static List<MetadataRegistry> getRegistries() {
        List<MetadataRegistry> result = registries;
        if (result == null) {
            synchronized (MetadataProviderRegistry.class) {
                result = registries;
                if (result == null) {
                    result = loadRegistries();
                    registries = result;
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
     * Package-private for testing.
     */
    static void reset() {
        synchronized (MetadataProviderRegistry.class) {
            registries = null;
        }
        cache.clear();
    }
}
