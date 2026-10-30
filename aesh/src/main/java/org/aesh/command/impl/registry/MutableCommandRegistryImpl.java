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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.DefaultValueProvider;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.container.CommandContainerBuilder;
import org.aesh.command.impl.completer.FuzzyCompletionMatcher;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ParsedCommand;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.AeshCommandLineParser;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.metadata.CommandMetadataProvider;
import org.aesh.command.metadata.MetadataProviderRegistry;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.registry.MutableCommandRegistry;
import org.aesh.parser.ParsedLine;
import org.aesh.readline.completion.CompleteOperation;
import org.aesh.terminal.formatting.TerminalString;

/**
 * @author Aesh team
 */
public class MutableCommandRegistryImpl<CI extends CommandInvocation> implements MutableCommandRegistry<CI> {

    private final Map<String, CommandContainer<CI>> registry = new HashMap<>();
    private final Map<String, CommandContainer<CI>> aliases = new HashMap<>();

    private CommandContainerBuilder<CI> containerBuilder;
    private DefaultValueProvider defaultValueProvider;
    private InvocationProviders invocationProviders;

    private final List<CommandRegistrationListener> listeners = new ArrayList<>();
    /**
     * How each registered command was sourced, keyed by canonical command
     * name. Class-registered commands can be rebuilt per execution for
     * stage isolation (#642); user-supplied instances and prebuilt
     * containers keep the shared container under an explicit share policy.
     */
    private final Map<String, CommandSource> sources = new HashMap<>();

    private enum CommandSourceKind {
        CLASS,
        SHARED
    }

    private static final class CommandSource {
        final CommandSourceKind kind;
        final Class<? extends Command> clazz;
        /** Recorded alias names for deferred class registrations (no build yet). */
        final List<String> aliases;

        private CommandSource(CommandSourceKind kind, Class<? extends Command> clazz, List<String> aliases) {
            this.kind = kind;
            this.clazz = clazz;
            this.aliases = aliases;
        }

        static CommandSource forClass(Class<? extends Command> clazz, List<String> aliases) {
            return new CommandSource(CommandSourceKind.CLASS, clazz, aliases);
        }

        static CommandSource shared() {
            return new CommandSource(CommandSourceKind.SHARED, null, Collections.<String> emptyList());
        }
    }

    public void setCommandContainerBuilder(CommandContainerBuilder<CI> containerBuilder) {
        this.containerBuilder = containerBuilder;
    }

    /**
     * Set a registry-level DefaultValueProvider that applies to all commands
     * that don't declare their own per-command provider.
     */
    public void setDefaultValueProvider(DefaultValueProvider provider) {
        this.defaultValueProvider = provider;
    }

    /**
     * Set the runtime invocation providers, applied to every container the
     * registry builds from now on (deferred class registrations included).
     * Already-built containers keep whatever was applied before; runtimes
     * refresh those through their update path.
     */
    public void setInvocationProviders(InvocationProviders providers) {
        this.invocationProviders = providers;
    }

    /**
     * Whether the named command already has a built shared container.
     * Lets runtimes refresh already-materialized definitions without
     * forcing deferred class registrations to build (#678).
     *
     * @param name the command name
     * @return true when a shared container exists for the name
     */
    public boolean isBuilt(String name) {
        return registry.containsKey(name);
    }

    @Override
    public CommandContainer<CI> getCommand(String name, String line) throws CommandNotFoundException {
        if (registry.containsKey(name))
            return registry.get(name);
        else if (name.contains(" ")) {
            String[] names = name.split(" ");
            return materialize(names[0]);
        } else if (sources.containsKey(name))
            return materialize(name);
        else
            throw new CommandNotFoundException("Command: " + name + " was not found.", name);
    }

    @Override
    public List<CommandLineParser<CI>> getChildCommandParsers(String parent) throws CommandNotFoundException {
        CommandContainer<CI> c = getCommand(parent, "");
        if (c == null) {
            throw new CommandNotFoundException("Command: " + parent + " was not found.", parent);
        }
        return Collections.unmodifiableList(c.getParser().getAllChildParsers());
    }

    @Override
    public void completeCommandName(CompleteOperation co, ParsedLine parsedLine) {
        materializeAllLenient();
        if (parsedLine.words().isEmpty()) {
            //add all
            for (CommandContainer<CI> command : registry.values()) {
                ProcessedCommand<? extends Command<CI>, CI> com = command.getParser().getProcessedCommand();
                if (com.isActivated(new ParsedCommand(com)))
                    co.addCompletionCandidate(com.name());
            }
        } else {
            for (CommandContainer<CI> command : registry.values()) {
                ProcessedCommand<? extends Command<CI>, CI> com = command.getParser().getProcessedCommand();
                if (com.name().startsWith(parsedLine.selectedWord().word()) &&
                        com.isActivated(new ParsedCommand(com))) {
                    co.addCompletionCandidate(com.name());
                    co.setOffset(co.getCursor() - parsedLine.selectedWord().word().length());
                    if (parsedLine.selectedIndex() < parsedLine.size() - 1)
                        co.setAppendSeparator(false);
                }
            }
            if (co.getCompletionCandidates().isEmpty() && isFuzzyCompletionEnabled() &&
                    FuzzyCompletionMatcher.isEligible(parsedLine.selectedWord().word())) {
                List<String> activatedNames = new ArrayList<>();
                for (CommandContainer<CI> command : registry.values()) {
                    ProcessedCommand<? extends Command<CI>, CI> com = command.getParser().getProcessedCommand();
                    if (com.isActivated(new ParsedCommand(com)))
                        activatedNames.add(com.name());
                }
                List<TerminalString> fuzzyMatches = FuzzyCompletionMatcher.getInstance()
                        .fuzzyMatches(parsedLine.selectedWord().word(), activatedNames);
                if (!fuzzyMatches.isEmpty()) {
                    co.addCompletionCandidatesTerminalString(fuzzyMatches);
                    co.setOffset(co.getCursor());
                    co.setReplacingCurrentWord(true);
                    if (parsedLine.selectedIndex() < parsedLine.size() - 1)
                        co.setAppendSeparator(false);
                }
            }
        }
    }

    private boolean isFuzzyCompletionEnabled() {
        return invocationProviders != null && invocationProviders.fuzzyCompletion();
    }

    @Override
    public Set<String> getAllCommandNames() {
        Set<String> names = new java.util.LinkedHashSet<>(registry.keySet());
        names.addAll(sources.keySet());
        return names;
    }

    @Override
    public void addCommand(CommandContainer<CI> container) {
        sources.put(container.getParser().getProcessedCommand().name(), CommandSource.shared());
        putIntoRegistry(container);
    }

    @Override
    public void addCommand(Command command) throws CommandRegistryException {
        try {
            CommandContainer<CI> container = getBuilder().create(command);
            // The instance is user-supplied: no factory can mint isolated
            // copies, so executions share it under an explicit policy.
            // Sequential reuse keeps working; concurrent pipeline stages
            // observe last-write-wins on its fields.
            sources.put(container.getParser().getProcessedCommand().name(), CommandSource.shared());
            putIntoRegistry(container);
        } catch (CommandLineParserException e) {
            throw new CommandRegistryException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void addCommand(Class<? extends Command> command) throws CommandRegistryException {
        // Record name and aliases without instantiating or building: the
        // shared container materializes on first read (completion, docs,
        // direct lookup) and executions fork straight from the class (#678).
        String name = commandNameOf(command);
        if (name == null)
            throw new CommandRegistryException("Commands must be annotated with @CommandDefinition");
        List<String> aliases = commandAliasesOf(command);
        if (contains(name))
            return;
        boolean collision = false;
        for (String alias : aliases) {
            if (contains(alias)) {
                collision = true;
                break;
            }
        }
        if (collision)
            return;
        sources.put(name, CommandSource.forClass(command, aliases));
        emit(name, REGISTRATION_ACTION.ADDED);
    }

    /**
     * Resolve a command name (or recorded alias owner) to a built shared
     * container, building deferred class registrations on first read.
     * Definition failures surface here instead of at registration, with
     * the same exception type.
     */
    private CommandContainer<CI> materialize(String name) throws CommandNotFoundException {
        CommandContainer<CI> built = registry.get(name);
        if (built != null)
            return built;
        CommandSource source = sources.get(name);
        if (source == null || source.kind != CommandSourceKind.CLASS || source.clazz == null)
            throw new CommandNotFoundException("Command: " + name + " was not found.", name);
        try {
            CommandContainer<CI> container = (CommandContainer<CI>) getBuilder().create(source.clazz);
            if (defaultValueProvider != null)
                injectDefaultValueProvider(container.getParser());
            if (invocationProviders != null)
                applyProviders(container);
            registerBuilt(container);
            CommandContainer<CI> registered = registry.get(name);
            if (registered != null)
                return registered;
            throw new CommandNotFoundException("Command: " + name + " was not found.", name);
        } catch (CommandLineParserException e) {
            CommandNotFoundException notFound = new CommandNotFoundException("Command: " + name + " failed to build.", name);
            notFound.initCause(e);
            throw notFound;
        }
    }

    /**
     * Apply runtime invocation providers to one built container, mirroring
     * the runtime's own update path for containers it resolves directly.
     */
    private void applyProviders(CommandContainer<CI> container) {
        CommandLineParser<CI> parser = container.getParser();
        parser.getProcessedCommand().updateInvocationProviders(invocationProviders);
        if (parser instanceof AeshCommandLineParser) {
            AeshCommandLineParser<CI> aeshParser = (AeshCommandLineParser<CI>) parser;
            aeshParser.storeInvocationProviders(invocationProviders);
            List<CommandLineParser<CI>> childParsers = aeshParser.getChildParsers();
            if (childParsers != null) {
                for (CommandLineParser<CI> child : childParsers) {
                    child.getProcessedCommand().updateInvocationProviders(invocationProviders);
                    if (child instanceof AeshCommandLineParser)
                        ((AeshCommandLineParser<CI>) child).storeInvocationProviders(invocationProviders);
                }
            }
        } else {
            List<CommandLineParser<CI>> childParsers = parser.getAllChildParsers();
            for (CommandLineParser<?> child : childParsers) {
                child.getProcessedCommand().updateInvocationProviders(invocationProviders);
            }
        }
    }

    /**
     * Build every deferred class registration now. Used to restore eager
     * timing explicitly; execution and completion paths otherwise trigger
     * builds lazily on first use (#678).
     *
     * @throws CommandRegistryException if any definition fails to build
     */
    public void materializeAll() throws CommandRegistryException {
        for (String name : new ArrayList<>(sources.keySet())) {
            if (!registry.containsKey(name)) {
                try {
                    materialize(name);
                } catch (CommandNotFoundException e) {
                    throw new CommandRegistryException(e.getMessage(), e.getCause());
                }
            }
        }
    }

    /**
     * Build every deferred class registration, skipping definitions that
     * fail. Completion reads all definitions, so one corrupt command must
     * not hide the rest; failures surface loudly at execution instead.
     */
    private void materializeAllLenient() {
        for (String name : new ArrayList<>(sources.keySet())) {
            if (!registry.containsKey(name)) {
                try {
                    materialize(name);
                } catch (CommandNotFoundException ignored) {
                }
            }
        }
    }

    /**
     * Primary name for a command name or alias, consulting built maps
     * first and recorded deferred entries second — without building.
     */
    private String primaryName(String name) {
        if (registry.containsKey(name) || sources.containsKey(name))
            return name;
        for (Map.Entry<String, CommandSource> entry : sources.entrySet()) {
            if (entry.getValue().aliases.contains(name))
                return entry.getKey();
        }
        if (aliases.containsKey(name)) {
            return aliases.get(name).getParser().getProcessedCommand().name();
        }
        return null;
    }

    private static String commandNameOf(Class<? extends Command> clazz) {
        CommandMetadataProvider provider = MetadataProviderRegistry.getProvider(clazz);
        if (provider != null)
            return provider.commandName();
        CommandDefinition definition = clazz.getAnnotation(CommandDefinition.class);
        if (definition != null)
            return definition.name();
        return null;
    }

    private static List<String> commandAliasesOf(Class<? extends Command> clazz) {
        CommandMetadataProvider provider = MetadataProviderRegistry.getProvider(clazz);
        if (provider != null) {
            String[] aliases = provider.commandAliases();
            if (aliases != null)
                return Arrays.asList(aliases);
            return Collections.<String> emptyList();
        }
        CommandDefinition definition = clazz.getAnnotation(CommandDefinition.class);
        if (definition != null)
            return Arrays.asList(definition.aliases());
        return Collections.<String> emptyList();
    }

    @Override
    public void addAllCommands(List<Command> commands) throws CommandRegistryException {
        if (commands != null) {
            for (Command command : commands)
                addCommand(command);
        }
    }

    @Override
    public void addAllCommandContainers(List<CommandContainer<CI>> commands) {
        if (commands != null) {
            for (CommandContainer<CI> command : commands)
                addCommand(command);
        }
    }

    @Override
    public boolean contains(String commandName) {
        if (registry.containsKey(commandName) || aliases.containsKey(commandName))
            return true;
        if (sources.containsKey(commandName))
            return true;
        for (CommandSource source : sources.values()) {
            if (source.aliases.contains(commandName))
                return true;
        }
        return false;
    }

    private void putIntoRegistry(CommandContainer<CI> commandContainer) {
        if (registerBuilt(commandContainer)) {
            emit(commandContainer.getParser().getProcessedCommand().name(),
                    REGISTRATION_ACTION.ADDED);
        }
    }

    /**
     * Store a built container and its aliases without emitting. Eager
     * registration paths emit through {@link #putIntoRegistry}; deferred
     * class registrations already emitted at record time.
     *
     * @return true when the container was stored
     */
    private boolean registerBuilt(CommandContainer<CI> commandContainer) {
        if (!commandContainer.haveBuildError()
                && !contains(commandContainer.getParser().getProcessedCommand())) {
            registry.put(commandContainer.getParser().getProcessedCommand().name(),
                    commandContainer);
            ProcessedCommand<? extends Command<CI>, CI> command = commandContainer.getParser().getProcessedCommand();
            for (String alias : command.getAliases()) {
                aliases.put(alias, commandContainer);
            }
            return true;
        }
        return false;
    }

    /**
     * Apply the registry-level DefaultValueProvider to all registered commands
     * that don't have their own per-command provider. Called after all commands
     * have been registered and the provider has been set.
     */
    public void applyDefaultValueProvider() {
        if (defaultValueProvider == null)
            return;
        for (CommandContainer<CI> container : registry.values()) {
            injectDefaultValueProvider(container.getParser());
        }
    }

    private void injectDefaultValueProvider(CommandLineParser<CI> parser) {
        ProcessedCommand<? extends Command<CI>, CI> cmd = parser.getProcessedCommand();
        if (cmd.getDefaultValueProvider() == null) {
            cmd.setDefaultValueProvider(defaultValueProvider);
        }
        // Recurse into group command children
        if (parser.isGroupCommand()) {
            for (CommandLineParser<CI> child : parser.getAllChildParsers()) {
                injectDefaultValueProvider(child);
            }
        }
    }

    private boolean contains(ProcessedCommand<? extends Command<CI>, CI> command) {
        if (registry.containsKey(command.name())) {
            return true;
        }
        for (String alias : command.getAliases()) {
            if (aliases.containsKey(alias)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void removeCommand(String name) {
        if (registry.containsKey(name)) {
            CommandContainer<CI> container = registry.remove(name);
            sources.remove(name);
            ProcessedCommand<? extends Command<CI>, CI> command = container.getParser().getProcessedCommand();
            for (String alias : command.getAliases()) {
                aliases.remove(alias);
            }
            emit(name, REGISTRATION_ACTION.REMOVED);
        } else if (sources.containsKey(name)) {
            // Deferred class registration: no container was ever built,
            // so dropping the record (with its recorded aliases) suffices.
            sources.remove(name);
            emit(name, REGISTRATION_ACTION.REMOVED);
        }
    }

    /**
     * Materializes an execution-scoped container for a class-registered
     * command through the same builder that registered it (fresh parser,
     * parse state and command instance, with the builder's lazy and
     * default-value wiring). Anything else keeps the shared container.
     */
    @Override
    public CommandContainer<CI> createExecutionContainer(CommandContainer<CI> shared) {
        CommandSource source = sources.get(shared.getParser().getProcessedCommand().name());
        if (source == null || source.kind != CommandSourceKind.CLASS)
            return shared;
        try {
            CommandContainer<CI> fork = (CommandContainer<CI>) getBuilder().create(source.clazz);
            if (defaultValueProvider != null)
                injectDefaultValueProvider(fork.getParser());
            return fork;
        } catch (CommandLineParserException e) {
            // Registration already built this definition successfully, so a
            // rebuild failure is unexpected (e.g. a nondeterministic dynamic
            // group). Share rather than break execution.
            return shared;
        }
    }

    /**
     * Fork straight from the recorded class for class-registered commands,
     * without materializing the shared container first: the run-once path
     * never pays for a shared build it discards (#678). Anything else
     * resolves the shared container and forks from it as before.
     */
    @Override
    public CommandContainer<CI> createExecutionContainer(String name) throws CommandNotFoundException {
        String primary = primaryName(name);
        CommandSource source = primary == null ? null : sources.get(primary);
        if (source != null && source.kind == CommandSourceKind.CLASS && source.clazz != null) {
            try {
                CommandContainer<CI> fork = (CommandContainer<CI>) getBuilder().create(source.clazz);
                if (defaultValueProvider != null)
                    injectDefaultValueProvider(fork.getParser());
                return fork;
            } catch (CommandLineParserException e) {
                CommandNotFoundException notFound = new CommandNotFoundException("Command: " + name + " failed to build.",
                        name);
                notFound.initCause(e);
                throw notFound;
            }
        }
        CommandContainer<CI> shared;
        try {
            shared = getCommand(name, "");
        } catch (CommandNotFoundException e) {
            shared = getCommandByAlias(name);
        }
        return createExecutionContainer(shared);
    }

    @Override
    public boolean isSharedExecutionContainer(String name) throws CommandNotFoundException {
        String primary = primaryName(name);
        CommandSource source = primary == null ? null : sources.get(primary);
        if (source != null && source.kind == CommandSourceKind.CLASS && source.clazz != null)
            return false;
        if (primary == null) {
            try {
                getCommand(name, "");
            } catch (CommandNotFoundException e) {
                getCommandByAlias(name);
            }
        }
        return true;
    }

    private CommandContainerBuilder<CI> getBuilder() {
        if (containerBuilder == null)
            containerBuilder = new AeshCommandContainerBuilder<>();
        return containerBuilder;
    }

    @Override
    public CommandContainer<CI> getCommandByAlias(String alias) throws CommandNotFoundException {
        if (aliases.containsKey(alias)) {
            return aliases.get(alias);
        } else {
            for (Map.Entry<String, CommandSource> entry : sources.entrySet()) {
                if (entry.getValue().aliases.contains(alias))
                    return materialize(entry.getKey());
            }
            throw new CommandNotFoundException("Command: named " + alias + " was not found.", alias);
        }
    }

    private void emit(String name, REGISTRATION_ACTION action) {
        for (CommandRegistrationListener listener : listeners) {
            listener.registrationAction(name, action);
        }
    }

    @Override
    public void addRegistrationListener(CommandRegistrationListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeRegistrationListener(CommandRegistrationListener listener) {
        listeners.remove(listener);
    }

    /**
     * Number of registered listeners. Test hook for lifetime assertions:
     * runtimes that are never closed accumulate here (#666).
     *
     * @return the listener count
     */
    int listenerCount() {
        return listeners.size();
    }

}
