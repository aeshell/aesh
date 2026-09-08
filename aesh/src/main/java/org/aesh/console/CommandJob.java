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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aesh.command.CommandException;
import org.aesh.command.CommandExecutionListener;
import org.aesh.command.CommandResult;
import org.aesh.command.Execution;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.terminal.Connection;
import org.aesh.terminal.tty.Signal;
import org.aesh.terminal.utils.Config;
import org.aesh.terminal.utils.LoggerUtil;

public final class CommandJob implements Consumer<Signal> {

    private final ProcessManager manager;
    private final Connection conn;
    private final Execution<? extends CommandInvocation> execution;
    private final String commandLine;
    private final CommandExecutionListener executionListener;
    private final JobId id = JobId.next();
    private final CountDownLatch completed = new CountDownLatch(1);

    private static final Logger LOGGER = LoggerUtil.getLogger(CommandJob.class.getName());
    private volatile JobState state = JobState.CREATED;
    private volatile Thread worker;
    private volatile Throwable error;
    private volatile long startTime;
    private volatile long endTime;
    private volatile List<Thread> upstreamPipeThreads;

    public CommandJob(ProcessManager manager, Connection conn,
            Execution<? extends CommandInvocation> execution,
            String commandLine, CommandExecutionListener executionListener) {
        this.manager = manager;
        this.conn = conn;
        this.execution = execution;
        this.commandLine = commandLine;
        this.executionListener = executionListener;
    }

    public JobId id() {
        return id;
    }

    public JobState state() {
        return state;
    }

    public boolean isRunning() {
        JobState current = state;
        return current == JobState.RUNNING || current == JobState.CANCELLATION_REQUESTED;
    }

    public CommandResult result() {
        return execution.getResult();
    }

    public Throwable error() {
        return error;
    }

    public Duration duration() {
        if (startTime == 0)
            return Duration.ZERO;
        long end = endTime == 0 ? System.currentTimeMillis() : endTime;
        return Duration.ofMillis(end - startTime);
    }

    public void requestInterrupt() {
        Thread currentWorker = worker;
        synchronized (this) {
            if (state != JobState.RUNNING)
                return;
            state = JobState.CANCELLATION_REQUESTED;
        }
        if (currentWorker != null)
            currentWorker.interrupt();
    }

    public void awaitCompletion() throws InterruptedException {
        completed.await();
    }

    public boolean awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
        return completed.await(timeout, unit);
    }

    public void start() {
        synchronized (this) {
            if (state != JobState.CREATED)
                return;
            state = JobState.RUNNING;
        }
        worker = new Thread(this::runJob);
        worker.setDaemon(true);
        worker.start();
    }

    public void run() {
        synchronized (this) {
            if (state != JobState.CREATED)
                return;
            state = JobState.RUNNING;
        }
        worker = Thread.currentThread();
        try {
            runJob();
        } finally {
            if (worker == Thread.currentThread())
                worker = null;
        }
    }

    @Override
    public void accept(Signal signal) {
        switch (signal) {
            case INT:
                if (isRunning()) {
                    LOGGER.fine("got interrupted in Task");
                    requestInterrupt();
                }
                break;
            default:
                break;
        }
    }

    public Execution<? extends CommandInvocation> execution() {
        return execution;
    }

    void setUpstreamPipeThreads(List<Thread> threads) {
        this.upstreamPipeThreads = threads;
    }

    List<Thread> getUpstreamPipeThreads() {
        return upstreamPipeThreads;
    }

    void awaitUpstreamPipeThreads() {
        List<Thread> threads = upstreamPipeThreads;
        upstreamPipeThreads = null;
        if (threads == null)
            return;
        for (Thread t : threads) {
            t.interrupt();
        }
        for (Thread t : threads) {
            try {
                t.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void runJob() {
        startTime = System.currentTimeMillis();
        try (HandlerScope scope = HandlerScope.signal(conn, this)) {
            try {
                execution.execute();
            } catch (CommandValidatorException | CommandException | OptionValidatorException
                    | CommandLineParserException e) {
                error = e;
                execution.setResult(CommandResult.FAILURE);
                conn.write(e.getMessage() + Config.getLineSeparator());
            } catch (InterruptedException e) {
                Thread.interrupted();
                execution.setResult(CommandResult.INTERRUPTED);
            } catch (Exception e) {
                error = e;
                execution.setResult(CommandResult.FAILURE);
                conn.write(e.getMessage() + Config.getLineSeparator());
                LOGGER.log(Level.WARNING,
                        "Uncaught exception when executing the command: " + execution.getCommand().toString(), e);
            } finally {
                awaitUpstreamPipeThreads();
            }
        } finally {
            finish();
        }
    }

    private void finish() {
        endTime = System.currentTimeMillis();
        synchronized (this) {
            if (state == JobState.CANCELLATION_REQUESTED) {
                state = JobState.INTERRUPTED;
                execution.setResult(CommandResult.INTERRUPTED);
            } else if (state == JobState.RUNNING) {
                state = error == null ? JobState.COMPLETED : JobState.FAILED;
            }
        }
        manager.processFinished(this);
        if (executionListener != null) {
            try {
                executionListener.onCommandComplete(commandLine, execution.getResult(),
                        endTime - startTime, error);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "CommandExecutionListener threw exception", e);
            }
        }
        completed.countDown();
    }
}
