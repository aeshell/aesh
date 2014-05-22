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
import org.jboss.aesh.util.LoggerUtil;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ProcessManager {

    private Console console;
    private List<Process> processes;
    private ExecutorService executorService;
    private boolean doLogging;
    private int pidCounter;

    private static final Logger LOGGER = LoggerUtil.getLogger(ProcessManager.class.getName());

    public ProcessManager(Console console, boolean log) {
        this.console = console;
        this.doLogging = log;
        processes = new ArrayList<>(1);
        executorService = Executors.newCachedThreadPool();
    }

    public void startNewProcess(ConsoleCallback callback, ConsoleOperation consoleOperation) {
        AeshProcess process = new AeshProcess(pidCounter++, this, callback, consoleOperation);
        if (doLogging)
            LOGGER.info("starting a new process: " + process + ", consoleOperation: " + consoleOperation);

        executorService.execute(process);
        processes.add(process);

    }

    // TODO: need to check if the process fetching has "focus"
    public CommandOperation getInput() throws InterruptedException {
        return console.getInput();
    }

    /**
     * this is the current running process
     */
    public Process getCurrentProcess() {
        return processes.get(0);
    }

    public boolean hasRunningProcess() {
        return processes.size() > 0;
    }

    public void processHaveFinished(Process process) {
        if (doLogging)
            LOGGER.info("process has finished: " + process);
        processes.remove(process);
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
