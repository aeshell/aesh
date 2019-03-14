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

import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandRuntime;
import org.aesh.command.Executor;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.shell.Shell;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.readline.Prompt;
import org.aesh.readline.action.KeyAction;
import org.aesh.readline.terminal.Key;
import org.aesh.terminal.tty.Size;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class DefaultCommandInvocation implements CommandInvocation{


    private final Shell shell = new DefaultShell();

    private final CommandRuntime<DefaultCommandInvocation> processor;

    private final CommandInvocationConfiguration config;

    private final CommandContainer<DefaultCommandInvocation> commandContainer;

    public DefaultCommandInvocation(CommandRuntime< DefaultCommandInvocation> processor,
                                    CommandInvocationConfiguration config,
                                    CommandContainer<DefaultCommandInvocation> commandContainer) {
        this.processor = processor;
        this.config = config;
        this.commandContainer = commandContainer;
    }

    @Override
    public Shell getShell() {
        return shell;
    }

    @Override
    public void setPrompt(Prompt prompt) {
    }

    @Override
    public Prompt getPrompt() {
        return null;
    }

    @Override
    public String getHelpInfo(String commandName) {
        return processor.commandInfo(commandName);
    }

    @Override
    public String getHelpInfo() {
        return commandContainer.getParser().parsedCommand().printHelp();
    }

    @Override
    public void stop() {

    }

    // XXX JFDENISE SHOULD BE REMOVED
    @Override
    public KeyAction input() {
        return null;
    }

    @Override
    public String inputLine() {
        return null;
    }

    @Override
    public String inputLine(Prompt prompt) {
        return null;
    }

    @Override
    public void executeCommand(String input) throws CommandNotFoundException,
            CommandLineParserException,
            OptionValidatorException,
            CommandValidatorException,
            CommandException,
            InterruptedException,
            IOException {
        processor.executeCommand(input);
    }

    @Override
    public void print(String msg, boolean paging) {
        shell.write(msg, paging);
    }

    @Override
    public void println(String msg, boolean paging) {
        shell.writeln(msg, paging);
    }

    @Override
    public Executor<? extends CommandInvocation> buildExecutor(String line) throws CommandNotFoundException,
            CommandLineParserException,
            OptionValidatorException,
            CommandValidatorException,
            IOException {
        return processor.buildExecutor(line);
    }

    @Override
    public CommandInvocationConfiguration getConfiguration() {
        return config;
    }

    private static class DefaultShell implements Shell {

        @Override
        public void write(String out, boolean paging) {
            System.out.print(out);
        }

        @Override
        public void writeln(String out, boolean paging) {
            System.out.println(out);
        }

        @Override
        public void write(int[] out) {
            // Not supported.
        }

        @Override
        public void write(char out) {
            System.out.println(out);
        }

        @Override
        public String readLine() {
            return null;
        }

        @Override
        public String readLine(Prompt prompt) {
            return null;
        }

        @Override
        public Key read() {
            return null;
        }

        @Override
        public Key read(Prompt prompt) {
            return null;
        }

        @Override
        public boolean enableAlternateBuffer() {
            return false;
        }

        @Override
        public boolean enableMainBuffer() {
            return false;
        }

        @Override
        public Size size() {
            return new Size(1, -1);
        }

        @Override
        public void clear() {
        }
    }
}
