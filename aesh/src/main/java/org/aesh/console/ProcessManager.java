/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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
package org.aesh.console;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aesh.command.CommandExecutionListener;
import org.aesh.command.CommandResult;
import org.aesh.command.Execution;
import org.aesh.command.Executor;
import org.aesh.command.impl.ExecutionPlanner;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.terminal.Connection;
import org.aesh.terminal.utils.LoggerUtil;

/**
 * Manages command execution within the interactive console.
 * <p>
 * For pipe chains ({@code cmd1 | cmd2 | cmd3}), upstream stages are run in
 * background threads while the last stage runs as the main Process. This
 * enables streaming data flow with back-pressure via
 * {@link java.io.PipedOutputStream}/{@link java.io.PipedInputStream}.
 *
 * @author Aesh team
 */
public class ProcessManager {

    private static final Logger LOGGER = LoggerUtil.getLogger(ProcessManager.class.getName());

    private Connection conn;
    private final Console console;
    private Executor<? extends CommandInvocation> executor;
    private ExecutionPlanner<? extends CommandInvocation> planner;
    private final Queue<Session> sessions = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean scheduling = new AtomicBoolean();
    private CommandExecutionListener executionListener;
    private String commandLine;
    private volatile CommandJob activeJob;
    private boolean synchronous;

    public ProcessManager(Console console) {
        this.console = console;
    }

    /**
     * When true, commands run on the calling thread instead of a separate
     * Process thread. Used for non-interactive/piped input where synchronous
     * execution prevents input loss between readline cycles (#609).
     */
    public void setSynchronous(boolean synchronous) {
        this.synchronous = synchronous;
    }

    public void setExecutionListener(CommandExecutionListener listener) {
        this.executionListener = listener;
    }

    /**
     * Returns true if there is a command process currently executing.
     * Used by ReadlineConsole to defer connection close on EOF until
     * the active process completes.
     */
    public boolean hasActiveProcess() {
        CommandJob job = activeJob;
        return job != null && job.isRunning();
    }

    public void execute(Executor<? extends CommandInvocation> executor, Connection conn, String commandLine) {
        sessions.add(new Session(executor, conn, commandLine));
        drain();
    }

    public boolean hasNext() {
        return planner != null && planner.hasMoreUnits();
    }

    public void processFinished(CommandJob job) {
        activeJob = null;
        drain();
    }

    @SuppressWarnings("unchecked")
    public void executeNext() {
        drain();
    }

    private void drain() {
        if (!scheduling.compareAndSet(false, true))
            return;
        try {
            while (true) {
                if (planner == null) {
                    Session session = sessions.poll();
                    if (session == null)
                        return;
                    this.conn = session.connection;
                    this.executor = session.executor;
                    this.commandLine = session.commandLine;
                    this.planner = new ExecutionPlanner<>(executor.getExecutions());
                }
                ExecutionPlanner.Unit<? extends CommandInvocation> unit = planner.nextUnit();
                if (unit == null) {
                    if (planner.hasSkipped())
                        planner.clearSkipped();
                    planner = null;
                    if (console.running() && !console.isClosePending())
                        console.read();
                    else
                        conn.close();
                    continue;
                }
                if (unit.isPipeline()) {
                    launchPipeline(unit.executions());
                    scheduling.set(false);
                    return;
                }
                Execution<? extends CommandInvocation> exec = unit.executions().get(0);
                if (!synchronous) {
                    launchSingle(exec);
                    scheduling.set(false);
                    return;
                }
                runInline(exec);
            }
        } finally {
            scheduling.set(false);
        }
    }

    private static final class Session {
        private final Executor<? extends CommandInvocation> executor;
        private final Connection connection;
        private final String commandLine;

        private Session(Executor<? extends CommandInvocation> executor, Connection connection,
                String commandLine) {
            this.executor = executor;
            this.connection = connection;
            this.commandLine = commandLine;
        }
    }

    private <T extends CommandInvocation> void launchPipeline(List<Execution<T>> pipeChain) {
        List<Thread> upstreamThreads = new ArrayList<>(pipeChain.size() - 1);
        for (int i = 0; i < pipeChain.size() - 1; i++) {
            Execution<T> stage = pipeChain.get(i);
            Thread t = new Thread(() -> {
                try {
                    stage.execute();
                } catch (Exception e) {
                    stage.setResult(CommandResult.FAILURE);
                    LOGGER.log(Level.FINE, "Upstream pipe stage exception", e);
                }
            }, "aesh-pipe-" + i);
            t.setDaemon(true);
            upstreamThreads.add(t);
        }

        for (Thread t : upstreamThreads) {
            t.start();
        }

        Execution<T> lastStage = pipeChain.get(pipeChain.size() - 1);
        CommandJob mainJob = new CommandJob(this, conn, lastStage, commandLine, executionListener);
        mainJob.setUpstreamPipeThreads(upstreamThreads);
        activeJob = mainJob;
        mainJob.start();
    }

    private void launchSingle(Execution<? extends CommandInvocation> exec) {
        CommandJob job = new CommandJob(this, conn, exec, commandLine, executionListener);
        activeJob = job;
        job.start();
    }

    private void runInline(Execution<? extends CommandInvocation> exec) {
        CommandJob job = new CommandJob(this, conn, exec, commandLine, executionListener);
        activeJob = job;
        job.run();
    }
}
