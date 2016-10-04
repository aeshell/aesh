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
package org.jboss.aesh;

import org.jboss.aesh.console.CommandResolver;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.command.registry.MutableCommandRegistry;
import org.jboss.aesh.parser.AeshLine;
import org.jboss.aesh.parser.Parser;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandResolver implements CommandResolver {

    private MutableCommandRegistry registry;

    public AeshCommandResolver(MutableCommandRegistry commandRegistry) {
        this.registry = commandRegistry;
    }

    public CommandRegistry getRegistry() {
        return registry;
    }

    @Override
    public CommandContainer resolveCommand(String line) throws CommandNotFoundException {
        AeshLine aeshLine = Parser.findAllWords(line);
        return getCommand( aeshLine, line);
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
    private CommandContainer getCommand(AeshLine aeshLine, String line) throws CommandNotFoundException {
        return getCommand(aeshLine.getWords().get(0), line);
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
    private CommandContainer getCommand(String commandName, String line) throws CommandNotFoundException {
        try {
            return registry.getCommand(commandName, line);
            //return commandContainer;
        } catch (CommandNotFoundException e) {
            // Lookup in aliases
            return registry.getCommandByAlias(commandName);
        }
    }


}
