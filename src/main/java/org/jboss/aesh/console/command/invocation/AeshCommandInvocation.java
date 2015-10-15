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

import org.jboss.aesh.console.AeshConsoleImpl;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.ConsoleCallback;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.CmdOperation;
import org.jboss.aesh.console.command.CommandOperation;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.keymap.KeyMap;
import org.jboss.aesh.console.operator.ControlOperator;
import org.jboss.aesh.readline.KeyEvent;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.terminal.Shell;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public final class AeshCommandInvocation implements CommandInvocation {

    private final AeshConsoleImpl aeshConsole;
    private final ControlOperator controlOperator;
    private final ConsoleCallback callback;
    private final int pid;

    public AeshCommandInvocation(AeshConsoleImpl aeshConsole, ControlOperator controlOperator,
                                 int pid, ConsoleCallback callback) {
        this.aeshConsole = aeshConsole;
        this.controlOperator = controlOperator;
        this.pid = pid;
        this.callback = callback;
    }
    @Override
    public ControlOperator getControlOperator() {
        return controlOperator;
    }

    @Override
    public CommandRegistry getCommandRegistry() {
        return aeshConsole.getCommandRegistry();
    }

    @Override
    public Shell getShell() {
        return aeshConsole.getShell();
    }

    @Override
    public void setPrompt(Prompt prompt) {
        aeshConsole.setPrompt(prompt);
    }

    @Override
    public Prompt getPrompt() {
        return aeshConsole.getPrompt();
    }

    @Override
    public String getHelpInfo(String commandName) {
        return aeshConsole.getHelpInfo(commandName);
    }

    @Override
    public void stop() {
        aeshConsole.stop();
    }

    @Override
    public AeshContext getAeshContext() {
        return aeshConsole.getAeshContext();
    }

    @Override
    public Key getInput() throws InterruptedException {
        return callback.getInput();
    }

    @Override
    public <T> CmdOperation<T> getInput(KeyMap<T> keyMap) throws InterruptedException {
        return callback.getInput(keyMap);
    }

    @Override
    public String getInputLine() throws InterruptedException {
        return callback.getInputLine();
    }

    @Override
    public int getPid() {
        return pid;
    }

    @Override
    public void putProcessInBackground() {
        aeshConsole.putProcessInBackground(pid);
    }

    @Override
    public void putProcessInForeground() {
        aeshConsole.putProcessInForeground(pid);
    }

    @Override
    public void executeCommand(String input) throws InterruptedException {
        aeshConsole.execute(input);
    }

   @Override
   public void print(String msg) {
      this.aeshConsole.getShell().out().print(msg);
   }

    @Override
    public void println(String msg) {
        this.aeshConsole.getShell().out().println(msg);
    }

}
