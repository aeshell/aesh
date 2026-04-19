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
package org.aesh.command.registry;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.aesh.command.CommandNotFoundException;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.parser.ParsedLine;
import org.aesh.readline.completion.CompleteOperation;

/**
 * A simple registry where all the Commands are stored
 *
 * @author Aesh team
 */
public interface CommandRegistry<CI extends CommandInvocation> {

    /**
     * Command registration actions.
     */
    enum REGISTRATION_ACTION {
        ADDED,
        REMOVED
    }

    /**
     * Implemented by listeners to receive added/removed events.
     */
    interface CommandRegistrationListener {
        void registrationAction(String commandName, REGISTRATION_ACTION action);
    }

    /**
     * To register to registry events.
     *
     * @param listener Listener to register.
     */
    void addRegistrationListener(CommandRegistrationListener listener);

    /**
     * To unregister from registry events.
     *
     * @param listener Listener to unregister.
     */
    void removeRegistrationListener(CommandRegistrationListener listener);

    /**
     * @param name command name
     * @param line current terminal buffer line
     * @return the matching CommandContainer's name
     */
    CommandContainer<CI> getCommand(String name, String line) throws CommandNotFoundException;

    /**
     *
     * @param parent The name of the parent command
     * @return The list of child parsers
     */
    List<CommandLineParser<CI>> getChildCommandParsers(String parent) throws CommandNotFoundException;

    /**
     * @param alias command alias
     * @return the matching CommandContainer's alias
     */
    CommandContainer<CI> getCommandByAlias(String alias) throws CommandNotFoundException;

    /**
     * Based on input, find all commands that match or partly match
     *
     *
     * @param completeOperation operation
     * @param parsedLine
     */
    void completeCommandName(CompleteOperation completeOperation, ParsedLine parsedLine);

    /**
     * @return all specified command names
     */
    Set<String> getAllCommandNames();

    /**
     * Returns the names of all subcommands registered under a group command.
     * Returns an empty set if the command is not a group command or not found.
     *
     * @param parentCommandName the name of the parent group command
     * @return set of subcommand names
     */
    default Set<String> getSubcommandNames(String parentCommandName) {
        Set<String> names = new LinkedHashSet<>();
        try {
            List<CommandLineParser<CI>> children = getChildCommandParsers(parentCommandName);
            if (children != null) {
                for (CommandLineParser<CI> child : children) {
                    names.add(child.getProcessedCommand().name());
                }
            }
        } catch (CommandNotFoundException e) {
            // Command not found, return empty set
        }
        return names;
    }

    default boolean contains(String commandName) {
        if (getAllCommandNames().contains(commandName))
            return true;
        try {
            if (getCommandByAlias(commandName) != null)
                return true;
        } catch (CommandNotFoundException e) {
            return false;
        }
        return false;
    }
}
