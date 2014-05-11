/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.console.command.CommandOperation;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public abstract class AeshConsoleCallback implements ConsoleCallback {

    private Process process;

    public AeshConsoleCallback() {
    }

    @Override
    public CommandOperation getInput() throws InterruptedException {
        return process.getInput();
    }

    @Override
    public void setProcess(Process process) {
        this.process = process;
    }

}
