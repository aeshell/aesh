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

import org.jboss.aesh.readline.KeyAction;
import org.jboss.aesh.readline.ReadlineConsole;
import org.jboss.aesh.util.LoggerUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ProcessManager {

    private ReadlineConsole console;
    private volatile Map<Integer, Process> processes;
    private ExecutorService executorService;
    private boolean doLogging;
    private int pidCounter = 1;
    private int foregroundProcess = -1;

    private Stack<Process> backgroundStack = new Stack<Process>();

    private static final Logger LOGGER = LoggerUtil.getLogger(ProcessManager.class.getName());

    public ProcessManager(ReadlineConsole console, boolean log) {
        this.console = console;
        this.doLogging = log;
        processes = new HashMap<>(20);
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
            processes.put(process.getPID(),process);
            foregroundProcess = process.getPID();
            executorService.execute(process);
        }
    }

    private int getCurrentForegroundProcess() {
        return foregroundProcess;
    }

    private Process getProcessByPid(int pid) {
        return processes.get(pid);
    }

    public KeyAction getInput(int pid) throws InterruptedException {
        /*
        if(foregroundProcess == pid)
            return console.getInput();
        else
        */
            return null;
    }

    public String getInputLine(int pid) throws InterruptedException {
        /*
        if(foregroundProcess == pid)
            return console.getInputLine();
        else
        */
            return "";
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
        else {
            Process p = getProcessByPid(pid);
            if(p != null) {
                backgroundStack.push(getProcessByPid(pid));
            }
            if (doLogging)
                LOGGER.info("We already have a process in the foreground: " +
                        foregroundProcess + ", pushing: " + pid + " to background stack. " +
                        "Will be pulled when current process ends.");
        }
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

    public boolean hasProcesses() {
        return !processes.isEmpty();
    }

    public void processHaveFinished(Process process) {
        /*
        if (doLogging)
            LOGGER.info("process has finished: " + process);
        processes.remove(process.getPID());
        if(process.getStatus() == Process.Status.FOREGROUND) {
            if(backgroundStack.isEmpty()) {
                foregroundProcess = -1;
            }else{
                foregroundProcess = backgroundStack.pop().getPID();
            }
        }
        console.currentProcessFinished(process);
        */
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
