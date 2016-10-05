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
package org.jboss.aesh.console;

import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.readline.KeyAction;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshProcess implements Runnable, Process {

    private ProcessManager manager;
    private final ConsoleCallback consoleCallback;
    private final ConsoleOperation operation;
    private CommandResult exitResult;
    private Thread myThread;
    private Status status;

    public AeshProcess(int pid, ProcessManager manager,
        ConsoleCallback consoleCallback,
        ConsoleOperation consoleOperation) {
        this.manager = manager;
        this.consoleCallback = consoleCallback;
        this.operation = consoleOperation;
        this.consoleCallback.setProcess(this);
        this.operation.setPid(pid);
        status = Status.FOREGROUND;
    }

    @Override
    public void run() {
        try {
            Thread.currentThread().setName("AeshProcess: " + operation.getPid());
            myThread = Thread.currentThread();
            setExitResult(consoleCallback.execute(operation));
        }
        catch (InterruptedException e) {
            setExitResult(-1);
            // e.printStackTrace();
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
    public KeyAction getInput() throws InterruptedException {
        return manager.getInput(getPID());
    }

    @Override
    public String getInputLine() throws InterruptedException {
        return manager.getInputLine(getPID());
    }

    @Override
    public int getPID() {
        return operation.getPid();
    }

    private void setExitResult(int exitStatus) {
        if (exitStatus == 0) {
            exitResult = CommandResult.SUCCESS;
        }
        else {
            exitResult = CommandResult.valueOf(exitStatus);
        }
    }

    @Override
    public CommandResult getExitResult() {
        return exitResult;
    }

    @Override
    public void interrupt() {
        if (myThread != null)
            myThread.interrupt();
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void updateStatus(Status status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof AeshProcess))
            return false;

        AeshProcess that = (AeshProcess) o;

        return operation.getPid() == that.operation.getPid();
    }

    @Override
    public int hashCode() {
        return operation.getPid();
    }

    @Override
    public String toString() {
        return "AeshProcess{" +
            "pid=" + operation.getPid() +
            ", manager=" + manager +
            ", consoleCallback=" + consoleCallback +
            ", operation=" + operation +
            '}';
    }
}
