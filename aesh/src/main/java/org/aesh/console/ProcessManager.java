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
import java.util.function.IntConsumer;
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
import org.aesh.command.impl.UpstreamOutcomeSupervisor;
import org.aesh.command.impl.operator.PipeOperator;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.status.ProgramStatusReporter;
import org.aesh.terminal.Connection;
import org.aesh.terminal.utils.LoggerUtil;
import org.aesh.terminal.utils.ProgramStatus;

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
     * Pending-work handoff for the drain turn. Every drain request
     * ({@link #execute}, {@link #executeNext}, {@link #processFinished})
     * sets this flag before attempting to take the turn, and the turn owner
     * clears it when a turn starts and re-checks it after releasing the
     * turn. A request published before the release is therefore always
     * observed by the owner; a request published after observes a free turn
     * in its own acquire attempt. Exactly one of the two drains, so no
     * wakeup is ever dropped (#638).
     */
    private final AtomicBoolean drainPending = new AtomicBoolean();
    /**
     * Thread currently holding the drain turn, for same-thread re-entrancy
     * detection. Written after acquiring {@code scheduling}, cleared before
     * releasing it.
     */
    private volatile Thread drainOwner;
    private CommandExecutionListener executionListener;
    /**
     * Receives the terminal exit value of every finished job before pipeline
     * events fire and the drain runs, so session state derived from it (such
     * as {@code $?} expansion for the next buffered command) is fresh when
     * the next command parses. Null by default (no recording).
     */
    private volatile IntConsumer exitCodeRecorder;
    private String commandLine;
    private volatile CommandJob activeJob;
    private boolean synchronous;
    private PipelineConfig pipelineConfig = PipelineConfig.DEFAULT;
    private boolean programStatusEnabled;
    private String programStatusAppName;
    private ProgramStatusReporter sessionReporter = ProgramStatusReporter.disabled();
    /**
     * Finalized outcome of the last finished job on the current line, for
     * program-status terminal reporting. Written in
     * {@link #processFinished} under the same identity guard that clears
     * the active slot, so abandoned workers never snapshot; read on the
     * drain turn when the line's planner is exhausted. Null when no job
     * has finished on the current line.
     */
    private volatile TerminalOutcome terminalOutcome;

    private static final class TerminalOutcome {
        final CommandResult result;
        final Throwable error;

        TerminalOutcome(CommandResult result, Throwable error) {
            this.result = result;
            this.error = error;
        }
    }

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
     * Records the terminal exit value of finished jobs ahead of the drain,
     * before the next buffered command parses. Listener callbacks keep
     * their documented stage, pipeline and command ordering; this is state
     * publication, not a callback.
     *
     * @param recorder receives exit values, or null to record nothing
     */
    public void setExitCodeRecorder(IntConsumer recorder) {
        this.exitCodeRecorder = recorder;
    }

    public void setPipelineConfig(PipelineConfig pipelineConfig) {
        if (pipelineConfig != null)
            this.pipelineConfig = pipelineConfig;
    }

    /**
     * Opt-in OSC 7501 program-status reporting for executed lines.
     * Disabled by default; when disabled nothing is allocated, probed,
     * or written. Reports go to each session's original terminal
     * connection, never to redirected command output.
     *
     * @param enabled true to report work start and terminal outcomes
     * @param appName stable application name, or null for none
     */
    public void setProgramStatus(boolean enabled, String appName) {
        this.programStatusEnabled = enabled;
        this.programStatusAppName = appName;
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

    /**
     * Queues a session for execution and requests a drain turn. The turn
     * protocol itself lives in {@link #requestDrain()}.
     */
    public void execute(Executor<? extends CommandInvocation> executor, Connection conn, String commandLine) {
        sessions.add(new Session(executor, conn, commandLine));
        requestDrain();
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

    /**
     * Post-completion handoff: a just-finished job must have its planner
     * continuation (or the readline re-arm/close) run, so completion always
     * records a drain request. The turn protocol itself lives in
     * {@link #requestDrain()}.
     * <p>
     * Callers must clear this job from {@code activeJob} before requesting
     * the drain (done above): the release path treats {@code activeJob ==
     * null} as proof that no launched job is still in flight (see
     * {@link #safeToRedrain()}).
     */
    public void processFinished(CommandJob job) {
        // Identity check: an abandoned worker may finish after a newer job
        // became active; it must not clear another job's slot.
        if (activeJob == job) {
            activeJob = null;
            if (programStatusEnabled)
                terminalOutcome = new TerminalOutcome(job.result(), job.error());
        }
        publishExitCode(job);
        firePipelineEvents(job);
        requestDrain();
    }

    /**
     * Publishes the finalized exit value before pipeline events fire and
     * the drain runs, so a buffered next command expands a fresh
     * {@code $?} when it parses (#646). Uses the finalized job result;
     * listener callbacks are unaffected and keep their order.
     */
    private void publishExitCode(CommandJob job) {
        IntConsumer recorder = exitCodeRecorder;
        if (recorder == null)
            return;
        CommandResult result = job.result();
        if (result == null)
            return;
        try {
            recorder.accept(result.getResultValue());
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Exit code recorder threw exception", e);
        }
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
            List<StageOutcome> stages = new ArrayList<>(stageCount);
            for (int i = 0; i < upstreamCount; i++) {
                // Finalized outcomes only: a live re-read can observe the
                // worker's transient post-timeout INTERRUPTED write landing
                // after the claim. Await finalized every stage, so the
                // snapshot is always present here.
                UpstreamOutcomeSupervisor.Outcome outcome = job.upstreamOutcome(i);
                if (outcome == null)
                    throw new IllegalStateException("Unsettled upstream stage " + i);
                stages.add(new StageOutcome(i, stageCount, names[i], upstream.get(i),
                        outcome.result(), outcome.error(), outcome.durationMs()));
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
        requestDrain();
    }

    /**
     * Publish the line's terminal program-status outcome: the last finished
     * job's finalized result, reported once the planner is exhausted —
     * before the next session is picked up and before the prompt is rearmed
     * or the connection closed. User cancellation reports idle; everything
     * else maps off the finalized result, never raw job state.
     */
    private void reportTerminalOutcome() {
        TerminalOutcome outcome = terminalOutcome;
        if (outcome == null || outcome.result == null)
            return;
        ProgramStatus.State state;
        if (outcome.result == CommandResult.INTERRUPTED)
            state = ProgramStatus.State.IDLE;
        else if (outcome.result.isSuccess())
            state = ProgramStatus.State.DONE;
        else
            state = ProgramStatus.State.ERROR;
        sessionReporter.report(ProgramStatus.builder(state).build());
    }

    /**
     * The single drain handoff shared by {@link #execute},
     * {@link #executeNext} and {@link #processFinished} (#638).
     * <p>
     * Every caller records its request in {@code drainPending} before
     * attempting to take the turn, and the owner re-checks the flag after
     * releasing the turn (see {@link #drainWhilePending}). A request
     * published before the release is observed by the owner; a request
     * published after observes a free turn in its own acquire attempt —
     * either way exactly one side drains, so no wakeup is dropped, with no
     * takeover timeout, parking, or extra drain thread.
     * <p>
     * A contended caller never blocks: it returns immediately and the owner
     * picks the request up. Same-thread re-entrancy (synchronous inline
     * execution, buffered readline input) returns outright — the outer
     * drain loop observes the queued work itself.
     * <p>
     * The turn is never released before worker start: launching only hands
     * work to already-started threads, and a completion that lands during
     * the launch window simply leaves its request flag for the release
     * path, which runs the post-completion drain as a follow-up turn
     * (#634).
     */
    private void requestDrain() {
        if (drainOwner == Thread.currentThread())
            return;
        drainPending.set(true);
        if (scheduling.compareAndSet(false, true))
            drainWhilePending();
    }

    /**
     * Runs owned drain turns until quiescent: each turn claims the pending
     * requests up front, so arrivals during the turn re-arm the flag for
     * the release check below.
     */
    private void drainWhilePending() {
        do {
            runDrainTurn();
        } while (acquireFollowupTurn());
    }

    /**
     * Runs a single owned drain turn, releasing the turn afterwards.
     */
    private void runDrainTurn() {
        drainOwner = Thread.currentThread();
        try {
            drainPending.set(false);
            drainLoop();
        } finally {
            drainOwner = null;
            scheduling.set(false);
        }
    }

    /**
     * Release-path re-check: takes a follow-up turn when a request arrived
     * during (or just before the release of) the previous turn. The check
     * runs after the release, pairing with the publish-before-acquire order
     * in {@link #requestDrain} so neither side can miss the other. When the
     * acquire fails, the new owner owns the still-set flag.
     *
     * @return true when a follow-up turn was acquired
     */
    private boolean acquireFollowupTurn() {
        return drainPending.get() && safeToRedrain() && scheduling.compareAndSet(false, true);
    }

    /**
     * Guards the follow-up turn against continuing a planner whose launched
     * job is still in flight: {@code nextUnit()} keys off a null result, so
     * re-entering now would return the in-flight unit again and launch it a
     * second time. A recorded completion clears {@code activeJob} before
     * requesting its drain, so a null job (or no planner at all) proves the
     * continuation is safe to run. A skipped follow-up leaves its flag set
     * for the in-flight job's own completion drain, preserving launch order.
     *
     * @return true when re-entering the drain loop is safe
     */
    private boolean safeToRedrain() {
        return planner == null || activeJob == null;
    }

    /**
     * Test hook: takes the drain turn as a simulated launcher inside its
     * launch call. The caller must later hand it back via
     * {@link #releaseDrainTurnForTest()}, which runs the production
     * release path.
     *
     * @return true when the turn was acquired
     */
    boolean acquireDrainTurnForTest() {
        if (scheduling.compareAndSet(false, true)) {
            drainOwner = Thread.currentThread();
            return true;
        }
        return false;
    }

    /**
     * Test hook: releases a turn taken with
     * {@link #acquireDrainTurnForTest()} through the production
     * release-then-recheck path, so a request recorded while the turn was
     * held is drained exactly as a racing launcher release would drain it.
     */
    void releaseDrainTurnForTest() {
        drainOwner = null;
        scheduling.set(false);
        if (acquireFollowupTurn())
            drainWhilePending();
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
                terminalOutcome = null;
                sessionReporter = ProgramStatusReporter.create(programStatusEnabled,
                        programStatusAppName, session.connection);
                sessionReporter.report(ProgramStatus.builder(ProgramStatus.State.WORKING).build());
            }
            ExecutionPlanner.Unit<? extends CommandInvocation> unit = planner.nextUnit();
            if (unit == null) {
                reportTerminalOutcome();
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
        // Shared with the job's join-timeout settle path: the supervisor owns
        // completion state and immutable outcomes, so snapshots never read
        // live Execution fields a still-unwinding task may be writing.
        final UpstreamOutcomeSupervisor supervisor = new UpstreamOutcomeSupervisor(upstreamCount);
        List<Thread> upstreamThreads = new ArrayList<>(upstreamCount);
        for (int i = 0; i < upstreamCount; i++) {
            final int stageIndex = i;
            Execution<T> stage = pipeChain.get(i);
            Thread t = PipeThreads.newThread(() -> {
                supervisor.stageStarted(stageIndex);
                try {
                    stage.execute();
                    supervisor.completeStage(stageIndex, stage, stage.getResult(), null);
                } catch (Throwable e) {
                    if (PipeOperator.isPipeBroken(e))
                        supervisor.completeStage(stageIndex, stage,
                                CommandResult.PIPE_BROKEN, e);
                    else
                        supervisor.completeStage(stageIndex, stage,
                                CommandResult.FAILURE, e);
                    LOGGER.log(Level.FINE, "Upstream pipe stage exception", e);
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
                pipeChain.subList(0, upstreamCount)), stageNames);
        mainJob.setUpstreamSupervisor(supervisor);
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
