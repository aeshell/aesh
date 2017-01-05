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
import org.aesh.command.registry.CommandRegistry;

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

    public AeshCommandRegistryBuilder command(ProcessedCommand processedCommand) {
        commandRegistry.addCommand(new AeshCommandContainer(processedCommand));
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
