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
package org.jboss.aesh.console;

import org.jboss.aesh.console.command.invocation.CommandInvocationProvider;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.export.ExportManager;
import org.jboss.aesh.console.helper.ManProvider;

/**
 * A Console that manages Commands and properly execute them.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface AeshConsole {

    /**
     * Start the Console. Open stream and set the proper terminal settings.
     */
    void start();

    /**
     * Stop the Console, close streams and reset terminal settings.
     */
    void stop();

    /**
     * Get the CommandRegistry
     */
    CommandRegistry getCommandRegistry();

    /**
     * Specify the prompt
     */
    void setPrompt(Prompt prompt);

    /**
     * Get the current Prompt
     */
    Prompt getPrompt();

    /**
     *
     * @return get shell
     */
    Shell getShell();

    /**
     * Clear the terminal screen
     */
    void clear();

    /**
     * Get a formatted usage/help info from the specified command
     */
    String getHelpInfo(String commandName);

    /**
     * Specify the current CommandInvocationProvider
     */
    void setCurrentCommandInvocationProvider(String name);

    /**
     * Register a new CommandInvocationProvider
     *
     * @param name
     *            the name
     * @param commandInvocationProvider
     *            the provider
     */
    void registerCommandInvocationProvider(String name,
                                           CommandInvocationProvider commandInvocationProvider);

    ManProvider getManProvider();

    /**
     * Get the AeshContext
     */
    AeshContext getAeshContext();

    /**
     * Is the console currently running?
     */
    boolean isRunning();

    ExportManager getExportManager();
}
