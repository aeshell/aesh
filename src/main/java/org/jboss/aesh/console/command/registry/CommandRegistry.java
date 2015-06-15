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

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.container.CommandContainer;

import java.util.Set;

/**
 * A simple registry where all the Commands are stored
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface CommandRegistry {

    /**
     * @param name command name
     * @param line current terminal buffer line
     * @return the matching CommandContainer's name
     * @throws org.jboss.aesh.console.command.CommandNotFoundException
     */
    CommandContainer getCommand(String name, String line) throws CommandNotFoundException;

    /**
     * Based on input, find all commands that match or partly match
     *
     *
     * @param completeOperation@return matching/partly patching commands
     */
    void completeCommandName(CompleteOperation completeOperation);

    /**
     * @return all specified command names
     */
    Set<String> getAllCommandNames();

    void removeCommand(String name);
}
