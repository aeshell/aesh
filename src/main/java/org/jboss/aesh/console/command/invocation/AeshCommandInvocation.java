/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.command.invocation;

import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleImpl;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.ConsoleCallback;
import org.jboss.aesh.console.ProcessDelegater;
import org.jboss.aesh.console.ProcessManager;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.CommandOperation;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.operator.ControlOperator;
import org.jboss.aesh.terminal.Shell;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public final class AeshCommandInvocation implements CommandInvocation {

    private final AeshConsole aeshConsole;
    private final ControlOperator controlOperator;
    private final ConsoleCallback callback;
    private final ProcessDelegater processDelegater;
    private final int pid;

    public AeshCommandInvocation(AeshConsole aeshConsole, ControlOperator controlOperator,
                                 int pid, ConsoleCallback callback) {
        this.aeshConsole = aeshConsole;
        this.controlOperator = controlOperator;
        this.pid = pid;
        this.callback = callback;
        this.processDelegater = new ProcessDelegater(aeshConsole.getConsole());
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
    public CommandOperation getInput() throws InterruptedException {
        return callback.getInput();
    }

    @Override
    public int getPid() {
        return pid;
    }

    @Override
    public void putProcessInBackground() {
        processDelegater.putProcessInBackground(pid);
    }

    @Override
    public void putProcessInForeground() {
        processDelegater.putProcessInForeground(pid);
    }

    @Override
    public void executeCommand(String input) {
        processDelegater.execute(input);
    }
}
