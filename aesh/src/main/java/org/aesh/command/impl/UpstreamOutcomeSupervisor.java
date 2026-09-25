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

import org.aesh.command.CommandResult;
import org.aesh.command.Execution;

/**
 * Coordinates upstream pipeline stage outcomes between the thread running a
 * stage and the thread settling it after a join timeout.
 * <p>
 * A cancelled stage keeps unwinding after the join timeout expires: its
 * {@code Execution} briefly records {@code INTERRUPTED} while propagating,
 * before the task handler records the terminal result. Without coordination
 * the settle path (or a later snapshot) can observe that transient value
 * instead of the timeout outcome. Once settle claims a stage, the task's
 * late writes are suppressed and snapshots use the claimed value, so the
 * recorded outcome is deterministic.
 * <p>
 * Shared by the batch path ({@code AeshCommandRuntime}) and the interactive
 * path ({@code ProcessManager}/{@code CommandJob}) so the settle contract
 * is implemented once.
 */
public final class UpstreamOutcomeSupervisor {

    private final Object outcomeLock = new Object();
    private final boolean[] settled;
    private final CommandResult[] claimed;

    /**
     * Creates a supervisor for the given number of upstream stages.
     *
     * @param upstreamCount number of upstream stages (excludes the terminal stage)
     */
    public UpstreamOutcomeSupervisor(int upstreamCount) {
        this.settled = new boolean[upstreamCount];
        this.claimed = new CommandResult[upstreamCount];
    }

    /**
     * Records a task thread's terminal outcome unless settle already claimed
     * the stage.
     *
     * @param index stage index
     * @param stageErrors caller-owned error array, may be updated
     * @param stage the upstream stage execution
     * @param error the thrown error, recorded when not settled
     * @param terminalResult the terminal result to record when not settled
     * @return true when recorded, false when suppressed (stage settled)
     */
    public boolean recordTaskOutcome(int index, Throwable[] stageErrors, Execution stage,
            Throwable error, CommandResult terminalResult) {
        synchronized (outcomeLock) {
            if (settled[index]) {
                return false;
            }
            stageErrors[index] = error;
            stage.setResult(terminalResult);
            return true;
        }
    }

    /**
     * Claims a stage after its join timed out. Overwrites a missing result
     * or a transient {@code INTERRUPTED} (which a still-running task cannot
     * have produced terminally — it is our own cancel propagating) with
     * {@code FAILURE} plus the timeout error, and snapshots the claimed
     * value. A genuine terminal result is kept as-is.
     *
     * @param index stage index
     * @param stage the upstream stage execution
     * @param stageErrors caller-owned error array, may be updated
     * @param timeout the timeout error to record when claiming
     */
    public void claimTimeout(int index, Execution stage, Throwable[] stageErrors, Throwable timeout) {
        synchronized (outcomeLock) {
            settled[index] = true;
            CommandResult current = stage.getResult();
            if (current == null || current == CommandResult.INTERRUPTED) {
                if (stageErrors[index] == null)
                    stageErrors[index] = timeout;
                stage.setResult(CommandResult.FAILURE);
                claimed[index] = CommandResult.FAILURE;
            } else {
                claimed[index] = current;
            }
        }
    }

    /**
     * Returns the claimed outcome for a settled stage, or null when the
     * stage was never settled. Callers must re-read live state when null.
     *
     * @param index stage index
     * @return the claimed result, or null
     */
    public CommandResult claimedResult(int index) {
        synchronized (outcomeLock) {
            if (index < 0 || index >= settled.length || !settled[index])
                return null;
            return claimed[index];
        }
    }
}
