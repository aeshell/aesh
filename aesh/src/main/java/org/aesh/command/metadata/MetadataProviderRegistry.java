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

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import org.aesh.command.Command;

/**
 * Registry that discovers and caches {@link CommandMetadataProvider} implementations
 * via {@link ServiceLoader}. Thread-safe with lazy initialization.
 *
 * @author Aesh team
 */
public final class MetadataProviderRegistry {

    private static volatile Map<Class<?>, CommandMetadataProvider<?>> providers;

    private MetadataProviderRegistry() {
    }

    /**
     * Look up a provider for the given command class.
     *
     * @param commandClass the command class to look up
     * @param <C> the command type
     * @return the provider, or null if no generated provider exists
     */
    @SuppressWarnings("unchecked")
    public static <C extends Command> CommandMetadataProvider<C> getProvider(Class<C> commandClass) {
        return (CommandMetadataProvider<C>) getProviders().get(commandClass);
    }

    private static Map<Class<?>, CommandMetadataProvider<?>> getProviders() {
        Map<Class<?>, CommandMetadataProvider<?>> result = providers;
        if (result == null) {
            synchronized (MetadataProviderRegistry.class) {
                result = providers;
                if (result == null) {
                    result = loadProviders();
                    providers = result;
                }
            }
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    private static Map<Class<?>, CommandMetadataProvider<?>> loadProviders() {
        Map<Class<?>, CommandMetadataProvider<?>> map = new ConcurrentHashMap<>();
        ServiceLoader<CommandMetadataProvider> loader = ServiceLoader.load(CommandMetadataProvider.class);
        for (CommandMetadataProvider provider : loader) {
            map.put(provider.commandType(), provider);
        }
        return map;
    }

    /**
     * Reset the registry, forcing re-discovery on next access.
     * Package-private for testing.
     */
    static void reset() {
        synchronized (MetadataProviderRegistry.class) {
            providers = null;
        }
    }
}
