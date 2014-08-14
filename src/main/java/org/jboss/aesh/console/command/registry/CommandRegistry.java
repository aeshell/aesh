/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.command.registry;

import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.container.CommandContainer;

import java.util.List;
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
     * @param line input
     * @return matching/partly patching commands
     */
    List<String> findAllCommandNames(String line);

    /**
     * @return all specified command names
     */
    Set<String> getAllCommandNames();
}
