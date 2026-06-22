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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aesh.command.CommandExecutionListener;
import org.aesh.command.CommandResult;
import org.aesh.command.Execution;
import org.aesh.command.Executor;
import org.aesh.command.impl.operator.PipeOperator;
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
    private CommandExecutionListener executionListener;
    private String commandLine;

    public ProcessManager(Console console) {
        this.console = console;
    }

    public void setExecutionListener(CommandExecutionListener listener) {
        this.executionListener = listener;
    }

    public void execute(Executor<? extends CommandInvocation> executor, Connection conn, String commandLine) {
        this.conn = conn;
        this.executor = executor;
        this.commandLine = commandLine;
        executeNext();
    }

    public boolean hasNext() {
        return executor.hasNext();
    }

    public void processFinished(Process process) {
        // Interrupt and wait for any upstream pipe threads to finish.
        // Interrupting is needed because upstream stages may be blocked on
        // PipedOutputStream.write() if the downstream consumer finished early.
        List<Thread> upstreamThreads = process.getUpstreamPipeThreads();
        if (upstreamThreads != null) {
            for (Thread t : upstreamThreads) {
                t.interrupt();
            }
            for (Thread t : upstreamThreads) {
                try {
                    t.join(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (hasNext()) {
            executeNext();
        } else {
            //if there are commands that wasn't executed we need to clear their data
            if (executor.hasSkipped())
                executor.clearSkippedListData();
            if (console.running())
                console.read();
            else
                conn.close();
        }
    }

    @SuppressWarnings("unchecked")
    public void executeNext() {
        if (!hasNext())
            return;

        Execution<? extends CommandInvocation> exec = executor.getNextExecution();
        if (exec == null)
            return;

        // Check if this execution is a pipe stage — if so, collect the entire chain
        if (exec.getExecutable() instanceof PipeOperator) {
            List<Execution<? extends CommandInvocation>> pipeChain = new ArrayList<>();
            pipeChain.add(exec);

            // Keep consuming executions from the executor until we find a non-pipe terminal
            while (exec.getExecutable() instanceof PipeOperator) {
                // Set a preliminary result so Executor.getNextExecution() can advance
                exec.setResult(CommandResult.SUCCESS);
                Execution<? extends CommandInvocation> next = executor.getNextExecution();
                if (next == null)
                    break;
                pipeChain.add(next);
                exec = next;
            }

            // Run upstream stages in background threads
            List<Thread> upstreamThreads = new ArrayList<>(pipeChain.size() - 1);
            for (int i = 0; i < pipeChain.size() - 1; i++) {
                Execution<? extends CommandInvocation> stage = pipeChain.get(i);
                // Clear the preliminary result — the actual execution will set it
                stage.setResult(null);
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

            // Start all upstream threads
            for (Thread t : upstreamThreads) {
                t.start();
            }

            // Run the last stage as the main Process
            Execution<? extends CommandInvocation> lastStage = pipeChain.get(pipeChain.size() - 1);
            Process mainProcess = new Process(this, conn, lastStage, commandLine, executionListener);
            mainProcess.setUpstreamPipeThreads(upstreamThreads);
            mainProcess.start();
        } else {
            // Single command — run normally
            new Process(this, conn, exec, commandLine, executionListener).start();
        }
    }
}
