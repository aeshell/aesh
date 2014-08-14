/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.command.registry;

import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.container.AeshCommandContainer;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.container.CommandContainerBuilder;
import org.jboss.aesh.util.ReflectionUtil;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandRegistryBuilder {

    private final MutableCommandRegistry commandRegistry;

    public AeshCommandRegistryBuilder() {
        commandRegistry = new MutableCommandRegistry();
    }

    public AeshCommandRegistryBuilder containerBuilder(CommandContainerBuilder builder) {
        commandRegistry.setCommandContainerBuilder(builder);
        return this;
    }

    public AeshCommandRegistryBuilder command(Class<? extends Command> command) {
        commandRegistry.addCommand(command);
        return this;
    }

    public AeshCommandRegistryBuilder commands(Class<? extends Command>... commands) {
        for (Class<? extends Command> c : commands) {
            commandRegistry.addCommand(c);
        }
        return this;
    }

    public AeshCommandRegistryBuilder command(ProcessedCommand processedCommand,
            Class<? extends Command> command) {
        commandRegistry.addCommand(new AeshCommandContainer(processedCommand,
                ReflectionUtil.newInstance(command)));
        return this;
    }

    public AeshCommandRegistryBuilder command(ProcessedCommand processedCommand,
            Command command) {
        commandRegistry.addCommand(new AeshCommandContainer(processedCommand, command));
        return this;
    }

    public AeshCommandRegistryBuilder command(CommandContainer commandContainer) {
        commandRegistry.addCommand(commandContainer);
        return this;
    }


    public AeshCommandRegistryBuilder command(CommandLineParser parser) {
        commandRegistry.addCommand(new AeshCommandContainer(parser));
        return this;
    }

    public AeshCommandRegistryBuilder command(Command command) {
        commandRegistry.addCommand(command);
        return this;
    }

    public CommandRegistry create() {
        return commandRegistry;
    }

}
