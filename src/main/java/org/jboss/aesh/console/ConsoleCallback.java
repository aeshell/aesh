/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.console.command.CommandOperation;

/**
 * Implementation of this interface will be called when a user press the
 * "enter/return" key.
 * The return value is to indicate if the outcome was a success or not.
 * Return 0 for success and something else for failure (typical 1 or -1).
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface ConsoleCallback {

    /**
     * Will be executed when a return is pressed
     *
     * @param output the consoleOperation
     * @return 0 for success or 1/-1 for failure.
     * @throws InterruptedException
     */
    int execute(ConsoleOperation output) throws InterruptedException;

    /**
     * A blocking call that will return user input from the terminal
     *
     * @return user input
     * @throws InterruptedException
     */
    CommandOperation getInput() throws InterruptedException;

    /**
     * Internally callback method
     *
     * @param process current process
     */
    void setProcess(Process process);

}
