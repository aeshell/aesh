/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    private final Map<String,CommandContainer> registry = new HashMap<String, CommandContainer>();

    private CommandContainerBuilder containerBuilder;

    public void setCommandContainerBuilder(CommandContainerBuilder containerBuilder) {
        this.containerBuilder = containerBuilder;
    }

    @Override
    public CommandContainer getCommand(String name, String line) throws CommandNotFoundException {
        if(registry.containsKey(name))
            return registry.get(name);
        else
            throw new CommandNotFoundException("Command: "+name+" was not found.");
    }

    @Override
    public List<String> findAllCommandNames(String line) {
        List<String> names = new ArrayList<>();
        for(CommandContainer command : registry.values()) {
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
        putIntoRegistry(getBuilder().build(command));
    }

    public void addCommand(Class<? extends Command> command) {
        putIntoRegistry(getBuilder().build(command));
    }

    private void putIntoRegistry(CommandContainer commandContainer) {
        if(!commandContainer.haveBuildError() &&
                !registry.containsKey(commandContainer.getParser().getProcessedCommand().getName()))
            registry.put(commandContainer.getParser().getProcessedCommand().getName(), commandContainer);
    }

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
