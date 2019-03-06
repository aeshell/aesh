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

import org.aesh.command.Command;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.container.CommandContainerBuilder;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ParsedCommand;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.registry.MutableCommandRegistry;
import org.aesh.parser.ParsedLine;
import org.aesh.readline.completion.CompleteOperation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class MutableCommandRegistryImpl<CI extends CommandInvocation> implements MutableCommandRegistry<CI> {

    private final Map<String, CommandContainer<CI>> registry = new HashMap<>();
    private final Map<String, CommandContainer<CI>> aliases = new HashMap<>();

    private CommandContainerBuilder<CI> containerBuilder;

    private final List<CommandRegistrationListener> listeners = new ArrayList<>();

    public void setCommandContainerBuilder(CommandContainerBuilder<CI> containerBuilder) {
        this.containerBuilder = containerBuilder;
    }

    @Override
    public CommandContainer<CI> getCommand(String name, String line) throws CommandNotFoundException {
        if(registry.containsKey(name))
            return registry.get(name);
        //group command
        else if(name.contains(" ")) {
            String[] names = name.split(" ");
            if(registry.containsKey(names[0])) {
                return registry.get(names[0]);
            }
            throw new CommandNotFoundException("Command: " + names[0] + " was not found.", names[0]);
        }
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
        if(parsedLine.words().size() == 0) {
            //add all
            for(CommandContainer<CI> command : registry.values()) {
                ProcessedCommand<? extends Command<CI>, CI> com = command.getParser().getProcessedCommand();
                if (com.getActivator().isActivated(new ParsedCommand(com)))
                    co.addCompletionCandidate(com.name());
            }
        }
        else {
            for(CommandContainer<CI> command : registry.values()) {
                ProcessedCommand<? extends Command<CI>, CI> com = command.getParser().getProcessedCommand();
                if(com.name().startsWith(parsedLine.selectedWord().word()) &&
                        com.getActivator().isActivated(new ParsedCommand(com))) {
                    co.addCompletionCandidate(com.name());
                    co.setOffset(co.getCursor()-parsedLine.selectedWord().word().length());
                    if(parsedLine.selectedIndex() < parsedLine.size()-1)
                        co.doAppendSeparator(false);
                }
            }
        }
    }

    @Override
    public Set<String> getAllCommandNames() {
        return registry.keySet();
    }

    @Override
    public void addCommand(CommandContainer<CI> container) {
        putIntoRegistry(container);
    }

    @Override
    public void addCommand(Command command) throws CommandRegistryException {
        try {
            putIntoRegistry(getBuilder().create(command));
        }
        catch(CommandLineParserException e) {
            throw new CommandRegistryException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void addCommand(Class<? extends Command> command) throws CommandRegistryException {
        try {
            putIntoRegistry(getBuilder().create(command));
        }
        catch(CommandLineParserException e) {
            throw new CommandRegistryException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void addAllCommands(List<Command> commands) throws CommandRegistryException {
        if(commands != null) {
            for(Command command : commands)
                addCommand(command);
        }
    }

    @Override
    public void addAllCommandContainers(List<CommandContainer<CI>> commands) {
        if(commands != null) {
            for(CommandContainer<CI> command : commands)
                addCommand(command);
        }
    }

    @Override
    public boolean contains(String commandName) {
        return registry.containsKey(commandName) || aliases.containsKey(commandName);

    }

    private void putIntoRegistry(CommandContainer<CI> commandContainer) {
        if (!commandContainer.haveBuildError()
                && !contains(commandContainer.getParser().getProcessedCommand())) {
            registry.put(commandContainer.getParser().getProcessedCommand().name(),
                    commandContainer);
            ProcessedCommand<? extends Command<CI>, CI> command = commandContainer.getParser().
                    getProcessedCommand();
            for (String alias : command.getAliases()) {
                aliases.put(alias, commandContainer);
            }
            emit(commandContainer.getParser().getProcessedCommand().name(),
                    REGISTRATION_ACTION.ADDED);
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
            ProcessedCommand<? extends Command<CI>, CI> command = container.getParser().getProcessedCommand();
            for (String alias : command.getAliases()) {
                aliases.remove(alias);
            }
            emit(name, REGISTRATION_ACTION.REMOVED);
        }
    }

    private CommandContainerBuilder<CI> getBuilder() {
        if(containerBuilder == null)
            containerBuilder = new AeshCommandContainerBuilder<>();
        return containerBuilder;
    }

    @Override
    public CommandContainer<CI> getCommandByAlias(String alias) throws CommandNotFoundException {
        if (aliases.containsKey(alias)) {
            return aliases.get(alias);
        } else {
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

}
