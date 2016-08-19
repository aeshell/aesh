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
package org.jboss.aesh.console.command.invocation;

import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.CommandOperation;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.operator.ControlOperator;
import org.jboss.aesh.terminal.Shell;

/**
 * A CommandInvocation is the value object passed to a Command when it is executed.
 * It contain references to the current ControlOperator, registry, shell, ++
 *
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
     * @return a formatted usage/help info from the specified command
     */
    String getHelpInfo(String commandName);

    /**
     * Stop the console and end the session
     */
    void stop();

    /**
     * Get AeshContext
     */
    AeshContext getAeshContext();

    /**
     * A blocking call that will return user input from the terminal
     *
     * @return user input
     * @throws InterruptedException
     */
    CommandOperation getInput() throws InterruptedException;

    /**
     * A blocking call that will return user input from the terminal
     * after the user has pressed enter.
     *
     * @return user input line
     * @throws InterruptedException
     */
    String getInputLine() throws InterruptedException;

    /**
     * The process id.
     *
     * @return pid
     */
    int getPid();

    /**
     * Put the current process in the background
     */
    void putProcessInBackground();

    /**
     * Put the current process in the foreground
     */
    void putProcessInForeground();

    /**
     * This will push the input to the input stream where aesh will
     * parse it and execute it as a normal "user input".
     * The input will not be visible for the user.
     * Note that if this command still has the foreground this input
     * will just be sitting on the queue.
     *
     * @param input command input
     */
    void executeCommand(String input);

   /**
    * Print a message on console
    * @param msg
    */
    void print(String msg);

    /**
     * Print a new line with a message on console;
     * @param msg
     */
    void println(String msg);


    /**
     *
     * @return true if Console.echo is true
     */
    boolean isEchoing();

    /**
     * Set the Console to be echoing or not
     * @param echo state
     */
    void setEcho(boolean echo);

    /**
     * Retrieve the command from a command line. The arguments in the command
     * line are injected into the Command fields.
     *
     * @param commandLine The input command.
     * @return The command or null if the string was not mapping onto a Command.
     * @throws org.jboss.aesh.console.command.CommandNotFoundException If the
     * command is not found in the registry.
     * @throws org.jboss.aesh.console.command.CommandException If the Command is
     * not properly structured.
     * @throws org.jboss.aesh.cl.parser.CommandLineParserException If the line
     * parsing fails.
     * @throws org.jboss.aesh.cl.validator.OptionValidatorException
     */
    Command getPopulatedCommand(String commandLine) throws CommandNotFoundException,
            CommandException, CommandLineParserException, OptionValidatorException;
}
