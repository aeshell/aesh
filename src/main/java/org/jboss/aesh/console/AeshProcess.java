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
public class AeshProcess implements Runnable, Process {

    private int pid;
    private ProcessManager manager;
    private ConsoleCallback consoleCallback;
    private ConsoleOperation operation;

    public AeshProcess(int pid, ProcessManager manager,
                       ConsoleCallback consoleCallback,
                       ConsoleOperation consoleOperation) {
        this.pid = pid;
        this.manager = manager;
        this.consoleCallback = consoleCallback;
        this.operation = consoleOperation;
        this.consoleCallback.setProcess(this);
    }

    @Override
    public void run() {
        try {
            Thread.currentThread().setName("AeshProcess: " + pid);
            //todo: we need to make sure exit status is used properly later
            int exitStatus = consoleCallback.execute(operation);
        }
        finally {
            manager.processHaveFinished(this);
        }
    }

    @Override
    public void setManager(ProcessManager manager) {
        this.manager = manager;
    }

    @Override
    public CommandOperation getInput() throws InterruptedException {
        return manager.getInput();
    }

    @Override
    public int getPID() {
        return pid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AeshProcess)) return false;

        AeshProcess that = (AeshProcess) o;

        return pid == that.pid;
    }

    @Override
    public int hashCode() {
        return pid;
    }

    @Override
    public String toString() {
        return "AeshProcess{" +
                "pid=" + pid +
                ", manager=" + manager +
                ", consoleCallback=" + consoleCallback +
                ", operation=" + operation +
                '}';
    }
}
