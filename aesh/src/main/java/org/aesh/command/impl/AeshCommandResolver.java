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

package org.aesh.command.impl;

import org.aesh.command.CommandResolver;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.parser.LineParser;
import org.aesh.parser.ParsedLine;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandResolver<CI extends CommandInvocation> implements CommandResolver<CI> {

    private CommandRegistry<CI> registry;
    private LineParser lineParser;

    public AeshCommandResolver(CommandRegistry<CI> commandRegistry) {
        this.registry = commandRegistry;
        lineParser = new LineParser();
    }

    public CommandRegistry<CI> getRegistry() {
        return registry;
    }

    @Override
    public CommandContainer<CI> resolveCommand(String line) throws CommandNotFoundException {
        return getCommand(lineParser.parseLine(line), line);
    }

    @Override
    public CommandContainer<CI> resolveCommand(ParsedLine line) throws CommandNotFoundException {
        return getCommand(line, line.line());
    }

    @Override
    public CommandContainer<CI> resolveCommand(String name, String line) throws CommandNotFoundException {
        return getCommand(name, line);
    }

    /**
     * try to return the command in the given registry if the given registry do not find the command, check if we have a
     * internal registry and if its there.
     *
     * @param aeshLine parsed command line
     * @param line command line
     * @return command
     * @throws CommandNotFoundException
     */
    private CommandContainer<CI> getCommand(ParsedLine aeshLine, String line) throws CommandNotFoundException {
        return getCommand(aeshLine.words().get(0).word(), line);
    }

    /**
     * try to return the command in the given registry if the given registry do
     * not find the command, check if we have a internal registry and if its
     * there.
     *
     * @param commandName command name
     * @param line command line
     * @return command
     * @throws CommandNotFoundException
     */
    private CommandContainer<CI> getCommand(String commandName, String line) throws CommandNotFoundException {
        try {
            return registry.getCommand(commandName, line);
        }
        catch (CommandNotFoundException e) {
            // Lookup in aliases
            return registry.getCommandByAlias(commandName);
        }
    }

}
