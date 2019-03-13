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
import org.aesh.command.Command;
import org.aesh.command.container.CommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.container.AeshCommandContainer;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;

import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
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

    public CommandRegistry<CI> create() {
        return commandRegistry;
    }

}
