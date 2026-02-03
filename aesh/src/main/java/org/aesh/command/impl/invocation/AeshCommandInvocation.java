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

package org.aesh.command.impl.invocation;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.aesh.command.Command;
import org.aesh.command.CommandRuntime;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.context.CommandContext;
import org.aesh.command.impl.shell.ShellOutputDelegate;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.Executor;
import org.aesh.command.shell.Shell;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.aesh.console.Console;
import org.aesh.console.ReadlineConsole;
import org.aesh.readline.Prompt;
import org.aesh.readline.action.KeyAction;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public final class AeshCommandInvocation implements CommandInvocation {

    private final Console console;
    private final Shell shell;
    private final CommandRuntime<AeshCommandInvocation> runtime;
    private final CommandInvocationConfiguration config;
    private final CommandContainer<AeshCommandInvocation> commandContainer;
    private final CommandContext commandContext;

    public AeshCommandInvocation(Console console, Shell shell,
                                 CommandRuntime<AeshCommandInvocation> runtime,
                                 CommandInvocationConfiguration config,
                                 CommandContainer<AeshCommandInvocation> commandContainer) {
        this(console, shell, runtime, config, commandContainer, null);
    }

    public AeshCommandInvocation(Console console, Shell shell,
                                 CommandRuntime<AeshCommandInvocation> runtime,
                                 CommandInvocationConfiguration config,
                                 CommandContainer<AeshCommandInvocation> commandContainer,
                                 CommandContext commandContext) {
        this.console = console;
        this.runtime = runtime;
        this.config = config;
        this.commandContainer = commandContainer;
        this.commandContext = commandContext;
        //if we have output redirection, use output delegate
        if (getConfiguration() != null && getConfiguration().getOutputRedirection() != null) {
            this.shell = new ShellOutputDelegate(shell, getConfiguration().getOutputRedirection());
        }
        //use default shell for no redirections
        else
            this.shell = shell;
    }

    @Override
    public Shell getShell() {
        return shell;
    }

    @Override
    public void setPrompt(Prompt prompt) {
        console.setPrompt(prompt);
    }

    @Override
    public Prompt getPrompt() {
        return console.prompt();
    }

    @Override
    public String getHelpInfo(String commandName) {
        return console.helpInfo(commandName);
    }

    @Override
    public String getHelpInfo() {
        return commandContainer.getParser().parsedCommand().printHelp();
    }

    @Override
    public void stop() {
        console.stop();
    }

    @Override
    public KeyAction input() throws InterruptedException {
        return shell.read();
    }

    @Override
    public KeyAction input(long timeout, TimeUnit unit) throws InterruptedException {
        return shell.read(timeout,unit);
    }

    @Override
    public String inputLine() throws InterruptedException {
        return shell.readLine();
    }

    @Override
    public String inputLine(Prompt prompt) throws InterruptedException {
        return shell.readLine(prompt);
    }

    @Override
    public void executeCommand(String input) throws CommandNotFoundException,
            CommandLineParserException,
            OptionValidatorException,
            CommandValidatorException,
            CommandException, InterruptedException, IOException {
            runtime.executeCommand(input);
    }

    @Override
    public Executor<AeshCommandInvocation> buildExecutor(String line) throws CommandNotFoundException,
            CommandLineParserException,
            OptionValidatorException,
            CommandValidatorException, IOException {
        return runtime.buildExecutor(line);
    }


    @Override
    public void print(String msg, boolean page) {
        shell.write(msg, page);
    }

    @Override
    public void println(String msg, boolean page) {
        shell.writeln(msg, page);
    }

    @Override
    public CommandInvocationConfiguration getConfiguration() {
        return config;
    }

    @Override
    public CommandContext getCommandContext() {
        // If we have a direct reference, use it
        if (commandContext != null) {
            return commandContext;
        }
        // Otherwise try to get it from the console
        if (console instanceof ReadlineConsole) {
            return ((ReadlineConsole) console).getCommandContext();
        }
        return null;
    }

    @Override
    public boolean enterSubCommandMode(Command<?> command) {
        if (!(console instanceof ReadlineConsole)) {
            return false;
        }

        ReadlineConsole readlineConsole = (ReadlineConsole) console;
        CommandContext ctx = readlineConsole.getCommandContext();
        if (ctx == null) {
            return false;
        }

        // Check if sub-command mode is enabled
        if (!ctx.getSettings().isEnabled()) {
            return false;
        }

        // Push the current command onto the context
        ctx.push(commandContainer.getParser(), command);

        // Update the prompt to show the context
        String newPrompt = ctx.buildPrompt(true);
        console.setPrompt(new Prompt(newPrompt));

        // Print entry message if configured
        String commandName = commandContainer.getParser().getProcessedCommand().name();
        String enterMessage = ctx.formatEnterMessage(commandName);
        if (enterMessage != null) {
            println(enterMessage);
        }
        String exitHint = ctx.formatExitHint();
        if (exitHint != null) {
            println(exitHint);
        }
        println("");

        return true;
    }

    @Override
    public boolean exitSubCommandMode() {
        if (!(console instanceof ReadlineConsole)) {
            return false;
        }

        ReadlineConsole readlineConsole = (ReadlineConsole) console;
        CommandContext ctx = readlineConsole.getCommandContext();
        if (ctx == null || !ctx.isInSubCommandMode()) {
            return false;
        }

        // Pop the context
        ctx.pop();

        // Update the prompt
        if (ctx.isInSubCommandMode()) {
            console.setPrompt(new Prompt(ctx.buildPrompt(true)));
        } else {
            console.setPrompt(new Prompt(ctx.getOriginalPrompt()));
        }

        return true;
    }

}
