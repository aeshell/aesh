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
package org.aesh.console.command.invocation;

import org.aesh.cl.parser.CommandLineParserException;
import org.aesh.cl.validator.CommandValidatorException;
import org.aesh.cl.validator.OptionValidatorException;
import org.aesh.command.Executor;
import org.aesh.console.AeshContext;
import org.aesh.console.Shell;
import org.aesh.console.command.CommandException;
import org.aesh.console.command.CommandNotFoundException;
import org.aesh.readline.Console;
import org.aesh.readline.Prompt;
import org.aesh.readline.action.KeyAction;
import org.aesh.util.Config;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public final class AeshCommandInvocation implements CommandInvocation {

    private final Console console;
    private final Shell shell;

    public AeshCommandInvocation(Console console, Shell shell) {
        this.console = console;
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
    public void stop() {
        console.stop();
    }

    @Override
    public AeshContext getAeshContext() {
        return console.context();
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
    public int pid() {
        //TODO
        return 0;
    }

    @Override
    public void putProcessInBackground() {
        //TODO
        //console.putProcessInBackground(pid);
    }

    @Override
    public void putProcessInForeground() {
        //TODO
        //console.putProcessInForeground(pid);
    }

    @Override
    public void executeCommand(String input) throws CommandNotFoundException,
            CommandLineParserException,
            OptionValidatorException,
            CommandValidatorException,
            CommandException {
        //TODO:
        //console.execute(input);
    }

    @Override
    public Executor<? extends CommandInvocation> buildExecutor(String line) throws CommandNotFoundException,
            CommandLineParserException,
            OptionValidatorException,
            CommandValidatorException {
        //TODO:
        //console.buildExecutor(input);
        return null;
    }


   @Override
   public void print(String msg) {
      shell.write(msg);
   }

    @Override
    public void println(String msg) {
        shell.write(msg+ Config.getLineSeparator());
    }

}
