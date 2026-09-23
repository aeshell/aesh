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
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aesh.command.CommandExecutionListener;
import org.aesh.command.CommandResult;
import org.aesh.command.Execution;
import org.aesh.command.Executor;
import org.aesh.command.PipelineConfig;
import org.aesh.command.PipelineExecutionListener;
import org.aesh.command.PipelineResult;
import org.aesh.command.StageOutcome;
import org.aesh.command.impl.ExecutionPlanner;
import org.aesh.command.impl.PipeThreads;
import org.aesh.command.impl.PipelineStages;
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
    private ExecutionPlanner<? extends CommandInvocation> planner;
    private final Queue<Session> sessions = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean scheduling = new AtomicBoolean();
    /**
     * Thread currently holding the drain turn, for same-thread re-entrancy
     * detection. Written after acquiring {@code scheduling}, cleared before
     * releasing it.
     */
    private volatile Thread drainOwner;
    /**
     * Bound for a completed job waiting to take its post-completion drain
     * turn when the launcher thread is still inside its launch call (#634).
     * The holder's remaining work is microseconds of thread startup; this
     * is orders of magnitude of margin. Expiry falls back to the historic
     * skip-and-return behavior.
     */
    private static final long DRAIN_TAKEOVER_TIMEOUT_MS = 10000;
    private CommandExecutionListener executionListener;
    private String commandLine;
    private volatile CommandJob activeJob;
    private boolean synchronous;
    private PipelineConfig pipelineConfig = PipelineConfig.DEFAULT;

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

    public void setPipelineConfig(PipelineConfig pipelineConfig) {
        if (pipelineConfig != null)
            this.pipelineConfig = pipelineConfig;
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

    public CommandResult runNative(Execution<? extends CommandInvocation> execution, Connection conn,
            String commandLine) {
        CommandJob job = new CommandJob(this, conn, execution, commandLine, executionListener);
        job.setPipelineConfig(pipelineConfig);
        activeJob = job;
        job.run();
        return job.result();
    }

    public boolean hasNext() {
        return planner != null && planner.hasMoreUnits();
    }

    public void processFinished(CommandJob job) {
        // Identity check: an abandoned worker may finish after a newer job
        // became active; it must not clear another job's slot.
        if (activeJob == job)
            activeJob = null;
        firePipelineEvents(job);
        drainAfterCompletion();
    }

    private void firePipelineEvents(CommandJob job) {
        if (!(executionListener instanceof PipelineExecutionListener) || job.upstreamStageCount() == 0)
            return;
        PipelineExecutionListener listener = (PipelineExecutionListener) executionListener;
        try {
            int upstreamCount = job.upstreamStageCount();
            int stageCount = upstreamCount + 1;
            String[] names = job.getPipelineStageNames();
            List<Execution<? extends CommandInvocation>> upstream = job.getUpstreamStages();
            Throwable[] errors = job.getUpstreamStageErrors();
            long[] durations = job.getUpstreamStageDurations();
            List<StageOutcome> stages = new ArrayList<>(stageCount);
            for (int i = 0; i < upstreamCount; i++) {
                // Prefer the claimed outcome for settled stages: a live
                // re-read can observe the worker's transient post-timeout
                // INTERRUPTED write landing after the claim.
                CommandResult settledResult = job.upstreamSettledResult(i);
                CommandResult stageResult = settledResult != null
                        ? settledResult
                        : upstream.get(i).getResult();
                stages.add(new StageOutcome(i, stageCount, names[i], upstream.get(i),
                        stageResult, errors[i], durations[i]));
            }
            stages.add(new StageOutcome(upstreamCount, stageCount, names[upstreamCount],
                    job.execution(), job.result(), job.error(), job.duration().toMillis()));
            PipelineResult result = new PipelineResult(job.result(), stages);
            for (StageOutcome stage : stages) {
                try {
                    listener.onStageComplete(stage);
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "PipelineExecutionListener.onStageComplete threw exception", e);
                }
            }
            try {
                listener.onPipelineComplete(result);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "PipelineExecutionListener.onPipelineComplete threw exception", e);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Pipeline event dispatch failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void executeNext() {
        drain();
    }

    private void drain() {
        if (!scheduling.compareAndSet(false, true))
            return;
        drainOwner = Thread.currentThread();
        try {
            drainLoop();
        } finally {
            drainOwner = null;
            scheduling.set(false);
        }
    }

    /**
     * Post-completion drain: a just-finished job must re-arm readline (or
     * close), so unlike {@link #drain()} it must not silently skip when the
     * launcher thread is still inside its launch call.
     * <p>
     * After {@code job.start()} returns, the launcher releases the turn one
     * statement later — but under CPU contention the fresh worker routinely
     * wins the race: it runs the whole (tiny) command and reaches this drain
     * while the launcher is still preempted inside the launch, CAS-fails,
     * and the re-arm is skipped forever, wedging the session with no error
     * (#634). So wait (bounded) for the turn instead of skipping.
     * <p>
     * Same-thread re-entrancy (synchronous inline execution) keeps the
     * historic fail-fast behavior: the outer drain will pick up queued work.
     */
    private void drainAfterCompletion() {
        if (!scheduling.compareAndSet(false, true)) {
            if (drainOwner == Thread.currentThread())
                return;
            long deadline = System.currentTimeMillis() + DRAIN_TAKEOVER_TIMEOUT_MS;
            while (!scheduling.compareAndSet(false, true)) {
                if (System.currentTimeMillis() >= deadline) {
                    LOGGER.warning("Timed out waiting for drain turn after command completion;"
                            + " readline may not be re-armed");
                    return;
                }
                if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    return;
                }
                LockSupport.parkNanos(100_000);
            }
        }
        drainOwner = Thread.currentThread();
        try {
            drainLoop();
        } finally {
            drainOwner = null;
            scheduling.set(false);
        }
    }

    private void drainLoop() {
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
                return;
            }
            Execution<? extends CommandInvocation> exec = unit.executions().get(0);
            if (!synchronous) {
                launchSingle(exec);
                return;
            }
            runInline(exec);
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
        int upstreamCount = pipeChain.size() - 1;
        String[] stageNames = new String[pipeChain.size()];
        for (int i = 0; i < pipeChain.size(); i++)
            stageNames[i] = PipelineStages.commandName(pipeChain.get(i), i);
        Throwable[] stageErrors = new Throwable[upstreamCount];
        long[] stageDurations = new long[upstreamCount];
        // Shared with the job's join-timeout settle path so a still-unwinding
        // task cannot overwrite the claimed timeout outcome (transient 130).
        final Object outcomeLock = new Object();
        final boolean[] upstreamSettled = new boolean[upstreamCount];
        List<Thread> upstreamThreads = new ArrayList<>(upstreamCount);
        for (int i = 0; i < upstreamCount; i++) {
            final int stageIndex = i;
            Execution<T> stage = pipeChain.get(i);
            Thread t = PipeThreads.newThread(() -> {
                long start = System.currentTimeMillis();
                try {
                    stage.execute();
                } catch (Throwable e) {
                    synchronized (outcomeLock) {
                        if (!upstreamSettled[stageIndex]) {
                            stageErrors[stageIndex] = e;
                            if (PipeOperator.isPipeBroken(e))
                                stage.setResult(CommandResult.PIPE_BROKEN);
                            else
                                stage.setResult(CommandResult.FAILURE);
                        }
                    }
                    LOGGER.log(Level.FINE, "Upstream pipe stage exception", e);
                } finally {
                    stageDurations[stageIndex] = System.currentTimeMillis() - start;
                }
            }, "aesh-pipe-" + i);
            upstreamThreads.add(t);
        }

        for (Thread t : upstreamThreads) {
            t.start();
        }

        Execution<T> lastStage = pipeChain.get(upstreamCount);
        CommandJob mainJob = new CommandJob(this, conn, lastStage, commandLine, executionListener);
        mainJob.setPipelineConfig(pipelineConfig);
        mainJob.setUpstreamPipeThreads(upstreamThreads);
        mainJob.setUpstreamOutcomes(new ArrayList<Execution<? extends CommandInvocation>>(
                pipeChain.subList(0, upstreamCount)), stageNames, stageErrors, stageDurations);
        mainJob.setUpstreamSettlementGuard(outcomeLock, upstreamSettled);
        activeJob = mainJob;
        mainJob.start();
    }

    private void launchSingle(Execution<? extends CommandInvocation> exec) {
        CommandJob job = new CommandJob(this, conn, exec, commandLine, executionListener);
        job.setPipelineConfig(pipelineConfig);
        activeJob = job;
        job.start();
    }

    private void runInline(Execution<? extends CommandInvocation> exec) {
        CommandJob job = new CommandJob(this, conn, exec, commandLine, executionListener);
        job.setPipelineConfig(pipelineConfig);
        activeJob = job;
        job.run();
    }
}
