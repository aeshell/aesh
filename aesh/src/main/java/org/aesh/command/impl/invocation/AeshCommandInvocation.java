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

import org.aesh.command.CommandRuntime;
import org.aesh.command.container.CommandContainer;
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
import org.aesh.readline.Console;
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

    public AeshCommandInvocation(Console console, Shell shell,
                                 CommandRuntime<AeshCommandInvocation> runtime,
                                 CommandInvocationConfiguration config,
                                 CommandContainer<AeshCommandInvocation> commandContainer) {
        this.console = console;
        this.runtime = runtime;
        this.config = config;
        this.commandContainer = commandContainer;
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

}
