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

import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Shell;
import org.jboss.aesh.readline.Console;
import org.jboss.aesh.readline.Prompt;
import org.jboss.aesh.readline.ReadlineConsole;
import org.jboss.aesh.readline.action.KeyAction;

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
    public KeyAction getInput() throws InterruptedException {
        return shell.read();
    }

    @Override
    public String getInputLine() throws InterruptedException {
        return shell.readLine();
    }

    @Override
    public int getPid() {
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
    public void executeCommand(String input) throws InterruptedException {
        //TODO:
        //console.execute(input);
    }

   @Override
   public void print(String msg) {
      shell.write(msg);
   }

    @Override
    public void println(String msg) {
        shell.write(msg+Config.getLineSeparator());
    }

}
