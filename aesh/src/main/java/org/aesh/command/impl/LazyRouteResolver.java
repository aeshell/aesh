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
package org.aesh.command.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.GroupCommand;
import org.aesh.command.metadata.CommandMetadataProvider;
import org.aesh.command.metadata.MetadataProviderRegistry;
import org.aesh.command.parser.SubcommandNotFoundException;

public final class LazyRouteResolver {

    private LazyRouteResolver() {
    }

    public static void validate(Class<? extends Command> rootClass, String[] args)
            throws SubcommandNotFoundException {
        if (rootClass == null)
            return;
        Class<? extends Command> current = rootClass;
        int index = 0;
        while (args != null && index < args.length) {
            String token = args[index];
            if (token == null || (token.length() > 0 && token.charAt(0) == '-'))
                return;
            ChildRoute route = findChild(current, token);
            if (route == null)
                return;
            if (route.childClass == null)
                throw new SubcommandNotFoundException(route.parentName, token, route.availableNames);
            current = route.childClass;
            index++;
        }
    }

    public static String commandName(Class<? extends Command> commandClass) {
        CommandMetadataProvider<?> provider = MetadataProviderRegistry.getProvider(commandClass);
        if (provider != null)
            return provider.commandName();
        CommandDefinition definition = commandClass.getAnnotation(CommandDefinition.class);
        if (definition != null)
            return definition.name();
        return commandClass.getSimpleName();
    }

    static Class<? extends Command> matchChild(Class<? extends Command> parent, String token) {
        ChildRoute route = findChild(parent, token);
        return route == null ? null : route.childClass;
    }

    private static ChildRoute findChild(Class<? extends Command> parent, String token) {
        CommandMetadataProvider<?> provider = MetadataProviderRegistry.getProvider(parent);
        if (provider != null) {
            if (!provider.isGroupCommand())
                return null;
            return findChildFromProvider(provider, parent, token);
        }
        return findChildFromAnnotation(parent, token);
    }

    private static ChildRoute findChildFromProvider(CommandMetadataProvider<?> provider,
            Class<? extends Command> parent, String token) {
        if (GroupCommand.class.isAssignableFrom(parent))
            return null;
        Class<? extends Command>[] classes = provider.groupCommandClasses();
        String[][] names = provider.groupCommandNamesAndAliases();
        if (classes == null || names == null || names.length != classes.length)
            return null;
        List<String> available = new ArrayList<>(names.length);
        for (int i = 0; i < names.length; i++) {
            if (names[i] == null || names[i].length == 0)
                return null;
            available.add(names[i][0]);
            for (String name : names[i]) {
                if (token.equals(name))
                    return new ChildRoute(classes[i], null, null);
            }
        }
        return new ChildRoute(null, provider.commandName(), available);
    }

    private static ChildRoute findChildFromAnnotation(Class<? extends Command> parent, String token) {
        if (GroupCommand.class.isAssignableFrom(parent))
            return null;
        CommandDefinition definition = parent.getAnnotation(CommandDefinition.class);
        if (definition == null || definition.groupCommands().length == 0)
            return null;
        List<String> available = new ArrayList<>(definition.groupCommands().length);
        for (Class<? extends Command> child : definition.groupCommands()) {
            String name = childName(child);
            if (name == null)
                return null;
            available.add(name);
            if (token.equals(name))
                return new ChildRoute(child, null, null);
            for (String alias : childAliases(child)) {
                if (token.equals(alias))
                    return new ChildRoute(child, null, null);
            }
        }
        return new ChildRoute(null, definition.name(), available);
    }

    private static String childName(Class<? extends Command> child) {
        CommandMetadataProvider<?> provider = MetadataProviderRegistry.getProvider(child);
        if (provider != null)
            return provider.commandName();
        CommandDefinition definition = child.getAnnotation(CommandDefinition.class);
        if (definition != null)
            return definition.name();
        return null;
    }

    private static List<String> childAliases(Class<? extends Command> child) {
        List<String> aliases = new ArrayList<>();
        CommandMetadataProvider<?> provider = MetadataProviderRegistry.getProvider(child);
        if (provider != null) {
            String[] names = provider.commandAliases();
            if (names != null)
                Collections.addAll(aliases, names);
            return aliases;
        }
        CommandDefinition definition = child.getAnnotation(CommandDefinition.class);
        if (definition != null)
            Collections.addAll(aliases, definition.aliases());
        return aliases;
    }

    private static final class ChildRoute {
        private final Class<? extends Command> childClass;
        private final String parentName;
        private final List<String> availableNames;

        private ChildRoute(Class<? extends Command> childClass, String parentName, List<String> availableNames) {
            this.childClass = childClass;
            this.parentName = parentName;
            this.availableNames = availableNames;
        }
    }
}
