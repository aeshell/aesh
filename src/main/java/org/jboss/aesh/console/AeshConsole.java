/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.console.reader.AeshPrintWriter;
import org.jboss.aesh.console.reader.AeshStandardStream;
import org.jboss.aesh.terminal.TerminalSize;

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
     * Add a Command to the AeshConsole
     */
    void addCommand(Class<? extends Command> command);

    /**
     * Add a Command to the AeshConsole
     */
    void addCommand(Command command);

    /**
     * Remove the given command from the Console
     * @param command
     */
    void removeCommand(Class<? extends Command> command);

    /**
     * Remove the given command from the Console.
     */
    void removeCommand(Command command);

    /**
     * Attach a ConsoleCommand to the console. All input received
     * to the console will be sent directly to the
     * ConsoleCommand.processOperation(..)
     */
    void attachConsoleCommand(ConsoleCommand consoleCommand);

    /**
     * Get the standard out PrintWriter
     */
    AeshPrintWriter out();

    /**
     * Get the standard error PrintWriter
     */
    AeshPrintWriter err();

    /**
     * Specify the prompt
     */
    void setPrompt(Prompt prompt);

    /**
     * Get the possible input stream
     */
    AeshStandardStream in();

    /**
     * Get the terminal size
     */
    TerminalSize getTerminalSize();
}
