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

package org.aesh.command.invocation;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.aesh.command.Command;
import org.aesh.command.Executor;
import org.aesh.command.impl.context.CommandContext;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.shell.Shell;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.readline.Prompt;
import org.aesh.terminal.KeyAction;

/**
 * A CommandInvocation is the value object passed to a Command when it is executed.
 * It contain references to the current ControlOperator, registry, shell, ++
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface CommandInvocation {

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
     * @return a formatted usage/help info from the running command
     */
    String getHelpInfo();

    /**
     * Stop the console and end the session
     */
    void stop();

    /**
     * Get the configuration.
     *
     * @return The configuration.
     */
    CommandInvocationConfiguration getConfiguration();
    /**
     * A blocking call that will return user input from the terminal
     *
     * @return user input
     * @throws InterruptedException
     */
    KeyAction input() throws InterruptedException;

    /**
     * A blocking call with a timeout that will return user input from the terminal
     *
     * @return user input or null if it times out
     * @throws InterruptedException on timeout
     */
    KeyAction input(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * A blocking call that will return user input from the terminal
     * after the user has pressed enter.
     *
     * @return user input line
     * @throws InterruptedException
     */
    String inputLine() throws InterruptedException;

    /**
     * A blocking call that will return user input from the terminal
     * after the user has pressed enter.
     *
     * @return user input line
     * @throws InterruptedException
     */
    String inputLine(Prompt prompt) throws InterruptedException;

    /**
     * This will push the input to the input stream where aesh will
     * parse it and execute it as a normal "user input".
     * The input will not be visible for the user.
     * Note that if this command still has the foreground this input
     * will just be sitting on the queue.
     *
     * @param input command input
     */
    void executeCommand(String input) throws CommandNotFoundException,
            CommandLineParserException,
            OptionValidatorException,
            CommandValidatorException,
            CommandException,
            InterruptedException,
            IOException;

    Executor<? extends CommandInvocation> buildExecutor(String line) throws CommandNotFoundException,
            CommandLineParserException,
            OptionValidatorException,
            CommandValidatorException,
            IOException;

   /**
    * Print a message on console
    * @param msg
    */
    default void print(String msg) {
        print(msg, false);
    }

    /**
     * Print a new line with a message on console;
     * @param msg
     */
    default void println(String msg) {
        println(msg, false);
    }

    /**
    * Print a message on console
    * @param msg
     * @param paging true to pause output for long content
    */
    void print(String msg, boolean paging);

    /**
     * Print a new line with a message on console;
     * @param msg
     * @param paging true to pause output for long content
     */
    void println(String msg, boolean paging);

    // ========== Parent Command Context Methods ==========

    /**
     * Get the current command context for sub-command mode.
     * The context provides access to parent command values and state.
     *
     * @return the command context, or null if not available
     */
    default CommandContext getCommandContext() {
        return null;
    }

    /**
     * Get a value from a parent command by field or option name.
     * Searches from immediate parent up to root.
     *
     * @param name The field or option name
     * @param type The expected type
     * @param <T> the value type
     * @return The value, or null if not found
     */
    default <T> T getParentValue(String name, Class<T> type) {
        CommandContext ctx = getCommandContext();
        return ctx != null ? ctx.getParentValue(name, type) : null;
    }

    /**
     * Get a value from a parent command with a default value.
     *
     * @param name The field or option name
     * @param type The expected type
     * @param defaultValue The default value if not found
     * @param <T> the value type
     * @return The value, or defaultValue if not found
     */
    default <T> T getParentValue(String name, Class<T> type, T defaultValue) {
        CommandContext ctx = getCommandContext();
        return ctx != null ? ctx.getParentValue(name, type, defaultValue) : defaultValue;
    }

    /**
     * Get the immediate parent command instance.
     *
     * @return the parent command, or null if not in sub-command mode
     */
    default Command<?> getParentCommand() {
        CommandContext ctx = getCommandContext();
        return ctx != null ? ctx.getParentCommand() : null;
    }

    /**
     * Get a specific parent command by type.
     *
     * @param type the command class to find
     * @param <T> the command type
     * @return the matching parent command, or null if not found
     */
    default <T extends Command<?>> T getParentCommand(Class<T> type) {
        CommandContext ctx = getCommandContext();
        return ctx != null ? ctx.getParentCommand(type) : null;
    }

    /**
     * Check if currently executing in sub-command mode.
     *
     * @return true if in sub-command mode
     */
    default boolean isInSubCommandMode() {
        CommandContext ctx = getCommandContext();
        return ctx != null && ctx.isInSubCommandMode();
    }

    // ========== Inherited Value Access Methods ==========

    /**
     * Get an inherited value from parent commands.
     * Only returns values from options marked with inherited=true.
     *
     * @param name The field or option name
     * @param type The expected type
     * @param <T> the value type
     * @return The inherited value, or null if not found
     */
    default <T> T getInheritedValue(String name, Class<T> type) {
        CommandContext ctx = getCommandContext();
        return ctx != null ? ctx.getInheritedValue(name, type) : null;
    }

    /**
     * Get an inherited value from parent commands with a default.
     * Only returns values from options marked with inherited=true.
     *
     * @param name The field or option name
     * @param type The expected type
     * @param defaultValue The default value if not found
     * @param <T> the value type
     * @return The inherited value, or defaultValue if not found
     */
    default <T> T getInheritedValue(String name, Class<T> type, T defaultValue) {
        CommandContext ctx = getCommandContext();
        return ctx != null ? ctx.getInheritedValue(name, type, defaultValue) : defaultValue;
    }

    /**
     * Enter sub-command mode for the current group command.
     * This pushes the current command onto the context stack and changes the prompt.
     * Subsequent commands will have access to this command's values via
     * {@link #getParentValue} and {@link #getParentCommand}.
     *
     * Type 'exit' to leave sub-command mode.
     *
     * @param command The group command instance to push onto the context
     * @return true if sub-command mode was entered successfully
     */
    default boolean enterSubCommandMode(Command<?> command) {
        return false; // Default implementation does nothing
    }

    /**
     * Exit the current sub-command mode level.
     * This pops the current context and restores the previous prompt.
     *
     * @return true if a context level was exited, false if not in sub-command mode
     */
    default boolean exitSubCommandMode() {
        return false; // Default implementation does nothing
    }

}
