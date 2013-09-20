/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.command;

import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.operator.ControlOperator;
import org.jboss.aesh.console.reader.AeshStandardStream;
import org.jboss.aesh.terminal.Shell;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface CommandInvocation {

    /**
     * @return the control operator connected with this invocation
     */
    ControlOperator getControlOperator();

     /**
     * @return the CommandRegistry
     */
    CommandRegistry getCommandRegistry();

    /**
     * Attach a ConsoleCommand to the console. All input received
     * to the console will be sent directly to the
     * ConsoleCommand.processOperation(..)
     */
    void attachConsoleCommand(ConsoleCommand consoleCommand);

    /**
     * @return the shell
     */
    Shell getShell();

    /**
     * Specify the prompt
     */
    void setPrompt(Prompt prompt);

    /**
     * @return Get the current Prompt
     */
    Prompt getPrompt();

    /**
     * @return the possible input stream
     */
    AeshStandardStream in();

    /**
     * @return a formatted usage/help info from the specified command
     */
    String getHelpInfo(String commandName);

    /**
     * Stop the console and end the session
     */
    void stop();
}

interface CommandContext<T extends CommandInvocation> {

    T enhanceCommandInvocation(CommandInvocation commandInvocation);

}
