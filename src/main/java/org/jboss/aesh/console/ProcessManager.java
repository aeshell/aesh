/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.console.command.CommandOperation;
import org.jboss.aesh.util.LoggerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ProcessManager {

    private Console console;
    private List<Process> processes;
    private Process current;
    private ExecutorService executorService;

    private static final Logger logger = LoggerUtil.getLogger(ProcessManager.class.getName());


    public ProcessManager(Console console) {
        this.console = console;
        processes = new ArrayList<>(1);

        executorService = Executors.newFixedThreadPool(50);
        /*
        executorService = Executors.newFixedThreadPool(10, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                thread.setDaemon(true);
                return thread;
            }
        });
        */
    }

    public void startNewProcess(ConsoleCallback callback, ConsoleOperation consoleOperation) {

        AeshProcess process = new AeshProcess(1, this, callback, consoleOperation);

        logger.info("starting a new process...");

        //process.run();

        executorService.execute(process);

        processes.add(process);
    }

    //TODO: need to check if the process fetching has "focus"
    public CommandOperation getInput() {
        return console.getInput();
    }

    /**
     * this is the current running process
     */
    public Process getCurrentProcess() {
        return processes.get(0);
    }

    public boolean hasRunningProcess() {
        logger.info("do we have a running process: "+(processes.size() > 0));
        return processes.size() > 0;
    }

    public void processHaveFinished(Process process) {
        logger.info("process has finished: "+process);
        console.currentProcessFinished(process);
        processes.remove(process);

    }

    public void stop() {
        try {
            logger.info("number of processes in list: "+processes.size());
            processes.clear();
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.MILLISECONDS);
            if(executorService.isTerminated())
                logger.info("Processes are cleaned up and finished...");
            if(executorService.isShutdown())
                logger.info("Executor isShutdown..");
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
