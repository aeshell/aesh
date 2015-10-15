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

import org.jboss.aesh.console.command.CmdOperation;
import org.jboss.aesh.console.command.CommandOperation;
import org.jboss.aesh.console.keymap.KeyMap;

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
     * A blocking call that will return user input from the terminal
     *
     * @return user input
     * @throws InterruptedException
     */
    <T> CmdOperation<T> getInput(KeyMap<T> keyMap) throws InterruptedException;

    /**
     * A blocking call that will return user input from the terminal
     * after the user has pressed enter.
     *
     * @return user input line
     * @throws InterruptedException
     */
    String getInputLine() throws InterruptedException;

    /**
     * Internally callback method
     *
     * @param process current process
     */
    void setProcess(Process process);

}
