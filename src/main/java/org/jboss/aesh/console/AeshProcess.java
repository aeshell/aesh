/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.console.command.CommandOperation;
import org.jboss.aesh.console.command.CommandResult;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshProcess implements Runnable, Process {

    private int pid;
    private ProcessManager manager;
    private ConsoleCallback consoleCallback;
    private ConsoleOperation operation;
    private CommandResult exitResult;
    private Thread myThread;

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
            myThread = Thread.currentThread();
            setExitResult( consoleCallback.execute(operation));
        }
        catch (InterruptedException e) {
            setExitResult(-1);
            //e.printStackTrace();
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

    private void setExitResult(int exitStatus) {
        if(exitStatus == 0) {
            exitResult = CommandResult.SUCCESS;
            exitResult.setResultValue(0);
        }
        else {
            exitResult = CommandResult.FAILURE;
            exitResult.setResultValue(exitStatus);
        }
    }

    @Override
    public CommandResult getExitResult() {
        return exitResult;
    }

    @Override
    public void interrupt() {
        if(myThread != null)
            myThread.interrupt();
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
