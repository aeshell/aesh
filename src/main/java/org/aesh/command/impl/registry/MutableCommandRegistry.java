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

import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.container.CommandContainerBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.readline.completion.CompleteOperation;
import org.aesh.util.LoggerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.util.Parser;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class MutableCommandRegistry<C extends Command> implements CommandRegistry<C> {

    private final Map<String, CommandContainer<C>> registry = new HashMap<>();
    private final Map<String, CommandContainer<C>> aliases = new HashMap<>();

    private CommandContainerBuilder<C> containerBuilder;

    public void setCommandContainerBuilder(CommandContainerBuilder<C> containerBuilder) {
        this.containerBuilder = containerBuilder;
    }

    private static final Logger LOGGER = LoggerUtil.getLogger(MutableCommandRegistry.class.getName());

    @Override
    public CommandContainer<C> getCommand(String name, String line) throws CommandNotFoundException {
        if(registry.containsKey(name))
            return registry.get(name);
        //group command
        else if(name.contains(" ")) {
            String[] names = name.split(" ");
            if(registry.containsKey(names[0])) {
                return registry.get(names[0]);
            }
            throw new CommandNotFoundException("Command: "+names[0]+" was not found.");
        }
        else
            throw new CommandNotFoundException("Command: "+name+" was not found.");
    }

    @Override
    public List<CommandLineParser<C>> getChildCommandParsers(String parent) throws CommandNotFoundException {
        CommandContainer c = getCommand(parent, "");
        if (c == null) {
            throw new CommandNotFoundException("Command: " + parent + " was not found.");
        }
        return Collections.unmodifiableList(c.getParser().getAllChildParsers());
    }

    @Override
    public void completeCommandName(CompleteOperation co) {
        List<String> names = new ArrayList<>();
        for(CommandContainer<C> command : registry.values()) {
            ProcessedCommand<C> com = command.getParser().getProcessedCommand();
            if(com.getName().startsWith(co.getBuffer()) &&
                com.getActivator().isActivated(com)) {
                if(command.getParser().isGroupCommand()) {
                    LOGGER.info("command is a group command");
                    //if we dont have any arguments we'll add the child commands as well
                    if(!com.hasOptions() &&
                            !com.hasArgument()) {
                        LOGGER.info("adding add: "+command.getParser().getAllNames());
                        names.addAll(command.getParser().getAllNames());
                        co.setIgnoreNonEscapedSpace(true);
                    }
                    else
                        names.add(com.getName());
                }
                else
                    names.add(com.getName());
            }
            else if(command.getParser().isGroupCommand() &&
                    co.getBuffer().startsWith(com.getName()) &&
                    com.getActivator().isActivated(com)) {
                String groupLine = Parser.trimInFront( co.getBuffer().substring(com.getName().length()));
                int diff = co.getBuffer().length() - groupLine.length();
                for(CommandLineParser child : command.getParser().getAllChildParsers()) {
                    if(child.getProcessedCommand().getName().startsWith(groupLine) &&
                        child.getProcessedCommand().getActivator().isActivated(child.getProcessedCommand()))
                        names.add(co.getBuffer().substring(0, diff) + child.getProcessedCommand().getName());
                }
            }
        }
        co.addCompletionCandidates(names);
    }

    @Override
    public Set<String> getAllCommandNames() {
        return registry.keySet();
    }

    public void addCommand(CommandContainer<C> container) {
        putIntoRegistry(container);
    }

    public void addCommand(C command) {
        putIntoRegistry(getBuilder().create(command));
    }

    public void addCommand(Class<C> command) {
        putIntoRegistry(getBuilder().create(command));
    }

    public void addAllCommands(List<C> commands) {
        if(commands != null) {
            for(C command : commands)
                addCommand(command);
        }
    }

    public void addAllCommandContainers(List<CommandContainer<C>> commands) {
        if(commands != null) {
            for(CommandContainer<C> command : commands)
                addCommand(command);
        }
    }

    private void putIntoRegistry(CommandContainer<C> commandContainer) {
        if (!commandContainer.haveBuildError()
                && !contains(commandContainer.getParser().getProcessedCommand())) {
            registry.put(commandContainer.getParser().getProcessedCommand().getName(),
                    commandContainer);
            ProcessedCommand<?> command = commandContainer.getParser().
                    getProcessedCommand();
            for (String alias : command.getAliases()) {
                aliases.put(alias, commandContainer);
            }
        }
    }

    private boolean contains(ProcessedCommand<?> command) {
        if (registry.containsKey(command.getName())) {
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
            CommandContainer container = registry.remove(name);
            ProcessedCommand<?> command = container.getParser().
                    getProcessedCommand();
            for (String alias : command.getAliases()) {
                aliases.remove(alias);
            }
        }
    }

    private CommandContainerBuilder<C> getBuilder() {
        if(containerBuilder == null)
            containerBuilder = new AeshCommandContainerBuilder<>();
        return containerBuilder;
    }

    @Override
    public CommandContainer<C> getCommandByAlias(String alias) throws CommandNotFoundException {
        if (aliases.containsKey(alias)) {
            return aliases.get(alias);
        } else {
            throw new CommandNotFoundException("Command: named " + alias + " was not found.");
        }
    }

}
