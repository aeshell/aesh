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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.aesh.command.CommandExecutionListener;
import org.aesh.command.Execution;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.terminal.Connection;
import org.aesh.terminal.tty.Signal;

/**
 * @author Aesh team
 * @deprecated Replaced by {@link CommandJob}, which does not expose thread
 *             primitives. AeshCommandRuntimeBuilder-style internal code uses
 *             CommandJob directly.
 */
@Deprecated
public class Process implements Consumer<Signal> {

    private final CommandJob job;

    public Process(ProcessManager manager, Connection conn,
            Execution<? extends CommandInvocation> execution,
            String commandLine, CommandExecutionListener executionListener) {
        this.job = new CommandJob(manager, conn, execution, commandLine, executionListener);
    }

    public void start() {
        job.start();
    }

    public void run() {
        job.run();
    }

    public void join() throws InterruptedException {
        job.awaitCompletion();
    }

    public void join(long millis) throws InterruptedException {
        job.awaitCompletion(millis, TimeUnit.MILLISECONDS);
    }

    public boolean isAlive() {
        return job.isRunning();
    }

    public void interrupt() {
        job.requestInterrupt();
    }

    /**
     * @deprecated Replaced by {@link CommandJob#id()}. Returns the job sequence
     *             number, not a thread id.
     */
    @Deprecated
    public int pid() {
        return (int) job.id().value();
    }

    @Override
    public void accept(Signal signal) {
        job.accept(signal);
    }

    public Execution<? extends CommandInvocation> execution() {
        return job.execution();
    }

    void setUpstreamPipeThreads(List<Thread> threads) {
        job.setUpstreamPipeThreads(threads);
    }

    List<Thread> getUpstreamPipeThreads() {
        return job.getUpstreamPipeThreads();
    }

    void awaitUpstreamPipeThreads() {
        job.awaitUpstreamPipeThreads();
    }
}
