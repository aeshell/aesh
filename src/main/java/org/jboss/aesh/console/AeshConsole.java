/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import java.io.PrintStream;

import org.jboss.aesh.console.command.CommandInvocationProvider;
import org.jboss.aesh.console.command.CommandRegistry;
import org.jboss.aesh.console.command.ConsoleCommand;
import org.jboss.aesh.console.reader.AeshPrintStream;
import org.jboss.aesh.console.reader.AeshStandardStream;
import org.jboss.aesh.terminal.Shell;

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
     * Attach a ConsoleCommand to the console. All input received
     * to the console will be sent directly to the
     * ConsoleCommand.processOperation(..)
     */
    void attachConsoleCommand(ConsoleCommand consoleCommand);

    /**
     * Get the standard out {@link PrintStream}
     */
    AeshPrintStream out();

    /**
     * Get the standard error {@link PrintStream}
     */
    AeshPrintStream err();

    /**
     * Specify the prompt
     */
    void setPrompt(Prompt prompt);

    /**
     * Get the current Prompt
     */
    Prompt getPrompt();

    /**
     * Get the possible input stream
     */
    AeshStandardStream in();

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

    void setCurrentCommandInvocationProvider(String name);

    void registerCommandInvocationProvider(String name, CommandInvocationProvider commandInvocationProvider);
}
