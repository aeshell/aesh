/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
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
package org.aesh.command.impl;

import java.util.concurrent.TimeoutException;

import org.aesh.command.CommandResult;
import org.aesh.command.Execution;

/**
 * Owns upstream pipeline stage outcomes for both execution paths.
 * <p>
 * The task thread reports lifecycle transitions ({@link #stageStarted},
 * {@link #completeStage}) while the settle thread finalizes unfinished
 * stages ({@link #claimTimeout}); snapshots ({@link #snapshot}) publish an
 * immutable result/error/duration that late task writes cannot change.
 * <p>
 * Completion is explicit state, never an exit-code guess: a stage that
 * genuinely returned {@code INTERRUPTED} is completed, so a later claim is
 * a no-op and the value is preserved. Only a stage that never completed
 * can be claimed as timed out, receiving {@code FAILURE} plus a timeout
 * error allocated on that path alone. The transient {@code INTERRUPTED} a
 * cancelled task writes to its {@code Execution} mid-unwind never reaches
 * a snapshot — snapshots are built from supervisor state, not live reads.
 * <p>
 * The {@code Execution} result write inside {@link #completeStage} is the
 * planner cursor ({@code ExecutionPlanner} relaunches units with null
 * results), not the outcome record; it must stay.
 * <p>
 * Shared by the batch path ({@code AeshCommandRuntime}) and the interactive
 * path ({@code ProcessManager}/{@code CommandJob}) so the settle contract
 * is implemented once.
 */
public final class UpstreamOutcomeSupervisor {

    /**
     * Immutable finalized outcome of one upstream stage.
     */
    public static final class Outcome {
        private final CommandResult result;
        private final Throwable error;
        private final long durationMs;

        Outcome(CommandResult result, Throwable error, long durationMs) {
            this.result = result;
            this.error = error;
            this.durationMs = durationMs;
        }

        public CommandResult result() {
            return result;
        }

        public Throwable error() {
            return error;
        }

        public long durationMs() {
            return durationMs;
        }
    }

    private final Object outcomeLock = new Object();
    private final boolean[] completed;
    private final boolean[] finalized;
    private final CommandResult[] results;
    private final Throwable[] errors;
    private final long[] durationsMs;
    private final long[] startMs;
    private final Thread[] workers;

    /**
     * Creates a supervisor for the given number of upstream stages.
     *
     * @param upstreamCount number of upstream stages (excludes the terminal stage)
     */
    public UpstreamOutcomeSupervisor(int upstreamCount) {
        this.completed = new boolean[upstreamCount];
        this.finalized = new boolean[upstreamCount];
        this.results = new CommandResult[upstreamCount];
        this.errors = new Throwable[upstreamCount];
        this.durationsMs = new long[upstreamCount];
        this.startMs = new long[upstreamCount];
        this.workers = new Thread[upstreamCount];
    }

    /**
     * Records that the task thread started running a stage, capturing the
     * worker thread (for explicit unwind joins on the settle path) and the
     * start time durations are measured from.
     *
     * @param index stage index
     */
    public void stageStarted(int index) {
        synchronized (outcomeLock) {
            if (finalized[index])
                return;
            startMs[index] = System.currentTimeMillis();
            workers[index] = Thread.currentThread();
        }
    }

    /**
     * Records a task thread's terminal outcome — for every terminal,
     * including normally returned results — unless settle already finalized
     * the stage. A null result maps to {@code SUCCESS}, matching
     * {@code ExecutionImpl}'s own default.
     *
     * @param index stage index
     * @param stage the upstream stage execution (result write is the planner cursor)
     * @param result the terminal result to record when not finalized
     * @param error the thrown error, recorded when not finalized
     * @return true when recorded, false when suppressed (stage finalized)
     */
    public boolean completeStage(int index, Execution stage, CommandResult result, Throwable error) {
        synchronized (outcomeLock) {
            if (finalized[index]) {
                return false;
            }
            if (result == null)
                result = CommandResult.SUCCESS;
            completed[index] = true;
            finalized[index] = true;
            results[index] = result;
            errors[index] = error;
            durationsMs[index] = durationSinceStart(index);
            stage.setResult(result);
            return true;
        }
    }

    /**
     * Finalizes a stage that never completed as timed out: {@code FAILURE}
     * plus a timeout error (allocated here, so normal joins allocate none)
     * and the duration up to the claim. A completed stage — including a
     * genuine {@code INTERRUPTED} — is left untouched.
     * <p>
     * The {@code Execution} write is the planner cursor only:
     * {@code ExecutionPlanner} relaunches units whose result is still null,
     * so a claimed-yet-running stage must read as finished. Snapshots never
     * read this field.
     *
     * @param index stage index
     * @param stage the upstream stage execution (cursor write on claim)
     */
    public void claimTimeout(int index, Execution stage) {
        synchronized (outcomeLock) {
            if (finalized[index]) {
                return;
            }
            finalized[index] = true;
            results[index] = CommandResult.FAILURE;
            if (errors[index] == null)
                errors[index] = new TimeoutException("Upstream stage join timed out");
            durationsMs[index] = durationSinceStart(index);
            stage.setResult(CommandResult.FAILURE);
        }
    }

    /**
     * Returns the finalized outcome for a stage, or null when it was never
     * finalized. Late task writes cannot change a returned outcome.
     *
     * @param index stage index
     * @return the finalized outcome, or null
     */
    public Outcome snapshot(int index) {
        synchronized (outcomeLock) {
            if (index < 0 || index >= finalized.length || !finalized[index])
                return null;
            return new Outcome(results[index], errors[index], durationsMs[index]);
        }
    }

    /**
     * Returns the worker thread captured by {@link #stageStarted}, or null
     * when the task never started.
     *
     * @param index stage index
     * @return the worker thread, or null
     */
    public Thread worker(int index) {
        synchronized (outcomeLock) {
            if (index < 0 || index >= workers.length)
                return null;
            return workers[index];
        }
    }

    /**
     * Returns true once the task thread recorded its terminal outcome,
     * whether normally or by throwing.
     *
     * @param index stage index
     * @return true when completed
     */
    public boolean isCompleted(int index) {
        synchronized (outcomeLock) {
            return index >= 0 && index < completed.length && completed[index];
        }
    }

    private long durationSinceStart(int index) {
        long start = startMs[index];
        return start == 0 ? 0 : System.currentTimeMillis() - start;
    }
}
