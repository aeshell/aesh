/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.jboss.aesh.console.command.CommandOperation;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.util.LoggerUtil;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ProcessManager {

    private Console console;
    private List<Process> processes;
    private ExecutorService executorService;
    private boolean doLogging;
    private int pidCounter = 1;
    private int foregroundProcess = -1;

    private static final Logger LOGGER = LoggerUtil.getLogger(ProcessManager.class.getName());

    public ProcessManager(Console console, boolean log) {
        this.console = console;
        this.doLogging = log;
        processes = new ArrayList<>(20);
        executorService = Executors.newCachedThreadPool();
    }

    public void startNewProcess(ConsoleCallback callback, ConsoleOperation consoleOperation) {
        AeshProcess process = new AeshProcess(pidCounter++, this, callback, consoleOperation);
        if (doLogging)
            LOGGER.info("starting a new process: " + process + ", consoleOperation: " + consoleOperation);

        //atm we cant start a new process if there is one in the foreground
        int currentProcess = getCurrentForegroundProcess();
        if(currentProcess > 0) {
            LOGGER.warning("Cannot start new process since process: "+
                    getProcessByPid(currentProcess)+" is running in the foreground.");
        }
        else {
            processes.add(process);
            foregroundProcess = process.getPID();
            executorService.execute(process);
        }
    }

    private int getCurrentForegroundProcess() {
        return foregroundProcess;
    }

    private Process getProcessByPid(int pid) {
        for(Process p : processes)
            if(p.getPID() == pid)
                return p;
        return null;
    }

    public CommandOperation getInput(int pid) throws InterruptedException {
        if(foregroundProcess == pid)
            return console.getInput();
        else
            return new CommandOperation(Key.UNKNOWN, new int[]{});
    }

    public void putProcessInBackground(int pid) {
        if(foregroundProcess == pid) {
            if(doLogging)
                LOGGER.info("Putting process: "+pid+" into the background.");
            foregroundProcess = -1;
        }
        else if(getProcessByPid(pid) != null) {
            Process p = getProcessByPid(pid);
            if(p.getStatus() == Process.Status.FOREGROUND) {
                if(doLogging)
                    LOGGER.warning("We have another process in the foreground: " +
                            p + ", this should not happen!");
                p.updateStatus(Process.Status.BACKGROUND);
            }
        }
    }

    public void putProcessInForeground(int pid) {
        if(foregroundProcess == -1) {
            Process p = getProcessByPid(pid);
            if(p != null) {
                p.updateStatus(Process.Status.FOREGROUND);
                foregroundProcess = p.getPID();
            }
        }
        else
            if(doLogging)
                LOGGER.info("We already have a process in the foreground: "+
                        foregroundProcess+", cant add another one");
    }

    /**
     * this is the current running process
     */
    public Process getCurrentProcess() {
        return getProcessByPid(foregroundProcess);
    }

    public boolean hasForegroundProcess() {
        return foregroundProcess > 0;
    }

    public void processHaveFinished(Process process) {
        if (doLogging)
            LOGGER.info("process has finished: " + process);
        processes.remove(process);
        if(process.getStatus() == Process.Status.FOREGROUND)
            foregroundProcess = -1;
        console.currentProcessFinished(process);
    }

    public void stop() {
        try {
            if (doLogging)
                LOGGER.info("number of processes in list: " + processes.size());
            processes.clear();
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.MILLISECONDS);
            if (executorService.isTerminated() && doLogging)
                LOGGER.info("Processes are cleaned up and finished...");
            if (executorService.isShutdown() && doLogging)
                LOGGER.info("Executor isShutdown..");
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
