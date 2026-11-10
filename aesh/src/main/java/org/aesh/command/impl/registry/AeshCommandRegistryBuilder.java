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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.aesh.command.ArgumentsSection;
import org.aesh.command.Command;
import org.aesh.command.DefaultValueProvider;
import org.aesh.command.HelpFeature;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.container.CommandContainerBuilder;
import org.aesh.command.impl.container.AeshCommandContainer;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;

/**
 * @author Aesh team
 */
@SuppressWarnings("unchecked")
public class AeshCommandRegistryBuilder<CI extends CommandInvocation> {

    private final MutableCommandRegistryImpl<CI> commandRegistry;

    public static <T extends CommandInvocation> AeshCommandRegistryBuilder<T> builder() {
        return new AeshCommandRegistryBuilder<>();
    }

    private AeshCommandRegistryBuilder() {
        commandRegistry = new MutableCommandRegistryImpl<>();
    }

    public AeshCommandRegistryBuilder<CI> containerBuilder(CommandContainerBuilder<CI> builder) {
        commandRegistry.setCommandContainerBuilder(builder);
        return this;
    }

    /**
     * Set a registry-level DefaultValueProvider that applies to all commands
     * that don't declare their own per-command provider via the annotation.
     */
    public AeshCommandRegistryBuilder<CI> defaultValueProvider(DefaultValueProvider provider) {
        commandRegistry.setDefaultValueProvider(provider);
        return this;
    }

    /**
     * Set whether parsers built by this registry render help with ANSI
     * colors. Set false to drop colors (for example when stdout is
     * redirected); applied to already-built containers in {@link #create()}
     * and to later builds, without forcing an early materialization.
     */
    public AeshCommandRegistryBuilder<CI> ansiColors(boolean ansiColors) {
        commandRegistry.setAnsiColors(ansiColors);
        return this;
    }

    /**
     * Fleet-wide help toggles applied when a command leaves them
     * {@code INHERIT} (see {@link org.aesh.command.Toggle}). Useful for
     * large CLIs instead of repeating the same attributes on every
     * command; per-command {@code ON}/{@code OFF} always wins.
     */
    public AeshCommandRegistryBuilder<CI> helpDefaults(EnumSet<HelpFeature> helpDefaults) {
        commandRegistry.setHelpDefaults(helpDefaults);
        return this;
    }

    /**
     * Fleet-wide arguments-section placement applied when a command leaves it
     * {@code INHERIT} (see {@link org.aesh.command.ArgumentsSection}).
     * Useful for large CLIs instead of repeating the attribute on every
     * command; an explicit value always wins.
     */
    public AeshCommandRegistryBuilder<CI> argumentsSectionDefault(ArgumentsSection argumentsSectionDefault) {
        commandRegistry.setArgumentsSectionDefault(argumentsSectionDefault);
        return this;
    }

    public AeshCommandRegistryBuilder<CI> command(Class<? extends Command> command) throws CommandRegistryException {
        commandRegistry.addCommand((Class<Command>) command);
        return this;
    }

    public AeshCommandRegistryBuilder<CI> commands(Class<? extends Command>... commands) throws CommandRegistryException {
        for (Class<? extends Command> c : commands) {
            commandRegistry.addCommand((Class<Command>) c);
        }
        return this;
    }

    public AeshCommandRegistryBuilder<CI> commands(List<Class<? extends Command>> commands) throws CommandRegistryException {
        for (Class<? extends Command> c : commands) {
            commandRegistry.addCommand(c);
        }
        return this;
    }

    public AeshCommandRegistryBuilder<CI> command(ProcessedCommand<Command<CI>, CI> processedCommand) {
        commandRegistry.addCommand(new AeshCommandContainer<>(processedCommand));
        return this;
    }

    public AeshCommandRegistryBuilder<CI> command(CommandContainer commandContainer) {
        commandRegistry.addCommand(commandContainer);
        return this;
    }

    public AeshCommandRegistryBuilder<CI> command(CommandLineParser<CI> parser) {
        commandRegistry.addCommand(new AeshCommandContainer<>(parser));
        return this;
    }

    public AeshCommandRegistryBuilder<CI> command(Command command) throws CommandRegistryException {
        commandRegistry.addCommand(command);
        return this;
    }

    /**
     * Register a command class under a given name and aliases instead of its
     * {@code @CommandDefinition} ones (#706). The registration stays deferred:
     * the definition builds on first use. Name/alias collisions are silently
     * ignored, as for annotation registrations.
     *
     * @param command the command class
     * @param name the name to register the command under
     * @param aliases additional names to register the command under
     */
    public AeshCommandRegistryBuilder<CI> command(Class<? extends Command> command, String name,
            String... aliases) throws CommandRegistryException {
        commandRegistry.addCommand(command, name, Arrays.asList(aliases));
        return this;
    }

    /**
     * Register a command instance under a given name and aliases instead of
     * its {@code @CommandDefinition} ones (#706).
     *
     * @param command the command instance
     * @param name the name to register the command under
     * @param aliases additional names to register the command under
     */
    public AeshCommandRegistryBuilder<CI> command(Command command, String name, String... aliases)
            throws CommandRegistryException {
        commandRegistry.addCommand(command, name, Arrays.asList(aliases));
        return this;
    }

    /**
     * Build every deferred class registration now. Used to restore eager
     * timing explicitly; execution and completion paths otherwise build
     * lazily on first use (#678).
     *
     * @throws CommandRegistryException if any definition fails to build
     */
    public void materializeAll() throws CommandRegistryException {
        commandRegistry.materializeAll();
    }

    public CommandRegistry<CI> create() {
        commandRegistry.applyDefaultValueProvider();
        commandRegistry.applyAnsiColors();
        return commandRegistry;
    }

}
