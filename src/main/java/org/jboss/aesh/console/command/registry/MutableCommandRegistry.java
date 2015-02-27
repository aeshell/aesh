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
package org.jboss.aesh.console.command.registry;

import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.container.AeshCommandContainerBuilder;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.container.CommandContainerBuilder;
import org.jboss.aesh.parser.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class MutableCommandRegistry implements CommandRegistry {

    private final Map<String,CommandContainer<Command>> registry = new HashMap<>();

    private CommandContainerBuilder containerBuilder;

    public void setCommandContainerBuilder(CommandContainerBuilder containerBuilder) {
        this.containerBuilder = containerBuilder;
    }

    @Override
    public CommandContainer getCommand(String name, String line) throws CommandNotFoundException {
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
    public List<String> findAllCommandNames(String line) {
        List<String> names = new ArrayList<>();
        for(CommandContainer<Command> command : registry.values()) {
            if(command.getParser().getProcessedCommand().getName().startsWith(line))
                names.add(command.getParser().getProcessedCommand().getName());
            else if(command.getParser().isGroupCommand() &&
                    line.startsWith(command.getParser().getProcessedCommand().getName())) {
                String groupLine = Parser.trimInFront( line.substring(command.getParser().getProcessedCommand().getName().length()));
                int diff = line.length() - groupLine.length();
                for(CommandLineParser child : command.getParser().getAllChildParsers()) {
                    if(child.getProcessedCommand().getName().startsWith(groupLine))
                        names.add(line.substring(0, diff) + child.getProcessedCommand().getName());
                }
            }
        }
        return names;
    }

    @Override
    public Set<String> getAllCommandNames() {
        return registry.keySet();
    }

    public void addCommand(CommandContainer container) {
        putIntoRegistry(container);
    }

    public void addCommand(Command command) {
        putIntoRegistry(getBuilder().create(command));
    }

    public void addCommand(Class<? extends Command> command) {
        putIntoRegistry(getBuilder().create(command));
    }

    public void addAllCommands(List<Command> commands) {
        if(commands != null) {
            for(Command command : commands)
                addCommand(command);
        }
    }

    public void addAllCommandContainers(List<CommandContainer> commands) {
        if(commands != null) {
            for(CommandContainer command : commands)
                addCommand(command);
        }
    }

    private void putIntoRegistry(CommandContainer commandContainer) {
        if(!commandContainer.haveBuildError() &&
                !registry.containsKey(commandContainer.getParser().getProcessedCommand().getName()))
            registry.put(commandContainer.getParser().getProcessedCommand().getName(), commandContainer);
    }

    @Override
    public void removeCommand(String name) {
        if(registry.containsKey(name))
            registry.remove(name);
    }

    private CommandContainerBuilder getBuilder() {
        if(containerBuilder == null)
            containerBuilder = new AeshCommandContainerBuilder();
        return containerBuilder;
    }

}
