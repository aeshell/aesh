/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.console.reader.AeshPrintWriter;
import org.jboss.aesh.console.reader.AeshStandardStream;

import java.io.StringReader;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface AeshConsole {

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

    void removeCommand(Class<? extends Command> command);

    void removeCommand(Command command);

    void attachConsoleCommand(ConsoleCommand consoleCommand);

    AeshPrintWriter out();

    AeshPrintWriter err();

    void setPrompt(Prompt prompt);

    AeshStandardStream in();
}
