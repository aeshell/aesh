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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeoutException;

import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.Executable;
import org.aesh.command.Execution;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.result.ResultHandler;
import org.junit.Test;

/**
 * Contract tests for the shared upstream-stage settle logic (#638, #645).
 * Every interleaving is driven deterministically by direct calls — no
 * threads, no timing. Both the batch path (AeshCommandRuntime) and the
 * interactive path (CommandJob) delegate to this code, so one suite pins
 * the contract for both.
 */
public class UpstreamOutcomeSupervisorTest {

    static class StubExecution implements Execution<CommandInvocation> {
        volatile CommandResult result;

        @Override
        public CommandInvocation getCommandInvocation() {
            return null;
        }

        @Override
        public Executable getExecutable() {
            return null;
        }

        @Override
        public Command<CommandInvocation> getCommand() {
            return null;
        }

        @Override
        public void populateCommand() {
        }

        @Override
        public ResultHandler getResultHandler() {
            return null;
        }

        @Override
        public CommandResult execute() throws CommandException {
            return result;
        }

        @Override
        public CommandResult getResult() {
            return result;
        }

        @Override
        public void setResult(CommandResult result) {
            this.result = result;
        }

        @Override
        public void clearQueuedLine() {
        }
    }

    @Test
    public void testCompleteStageRecordsOutcome() {
        UpstreamOutcomeSupervisor supervisor = new UpstreamOutcomeSupervisor(1);
        StubExecution stage = new StubExecution();
        RuntimeException failure = new RuntimeException("boom");

        supervisor.stageStarted(0);
        assertTrue(supervisor.completeStage(0, stage, CommandResult.FAILURE, failure));
        assertTrue(supervisor.isCompleted(0));
        // Planner cursor plus immutable outcome, published atomically.
        assertEquals(CommandResult.FAILURE, stage.getResult());
        UpstreamOutcomeSupervisor.Outcome outcome = supervisor.snapshot(0);
        assertEquals(CommandResult.FAILURE, outcome.result());
        assertSame(failure, outcome.error());
        assertTrue(outcome.durationMs() >= 0);
    }

    @Test
    public void testCompleteStageMapsNullToSuccess() {
        UpstreamOutcomeSupervisor supervisor = new UpstreamOutcomeSupervisor(1);
        StubExecution stage = new StubExecution();

        assertTrue(supervisor.completeStage(0, stage, null, null));
        assertEquals("Null terminal maps to SUCCESS like ExecutionImpl",
                CommandResult.SUCCESS, supervisor.snapshot(0).result());
        assertNull(supervisor.snapshot(0).error());
    }

    @Test
    public void testStageStartedCapturesWorker() {
        UpstreamOutcomeSupervisor supervisor = new UpstreamOutcomeSupervisor(1);

        assertNull(supervisor.worker(0));
        assertNull(supervisor.worker(1));
        supervisor.stageStarted(0);
        assertSame(Thread.currentThread(), supervisor.worker(0));
        assertFalse(supervisor.isCompleted(0));
    }

    @Test
    public void testClaimTimeoutOnUnfinishedStage() {
        UpstreamOutcomeSupervisor supervisor = new UpstreamOutcomeSupervisor(1);
        StubExecution stage = new StubExecution();

        supervisor.stageStarted(0);
        supervisor.claimTimeout(0, stage);
        UpstreamOutcomeSupervisor.Outcome outcome = supervisor.snapshot(0);
        assertEquals(CommandResult.FAILURE, outcome.result());
        assertTrue("Only actual timeouts receive timeout errors",
                outcome.error() instanceof TimeoutException);
        assertTrue(outcome.durationMs() >= 0);
        // Planner cursor is marked so the stage is not relaunched.
        assertEquals(CommandResult.FAILURE, stage.getResult());
    }

    @Test
    public void testClaimWithoutStartHasZeroDuration() {
        UpstreamOutcomeSupervisor supervisor = new UpstreamOutcomeSupervisor(1);
        StubExecution stage = new StubExecution();

        supervisor.claimTimeout(0, stage);
        assertEquals(0, supervisor.snapshot(0).durationMs());
    }

    @Test
    public void testClaimIgnoresTransientInterruptedOnUnfinishedStage() {
        UpstreamOutcomeSupervisor supervisor = new UpstreamOutcomeSupervisor(1);
        StubExecution stage = new StubExecution();
        supervisor.stageStarted(0);
        // Mid-unwind artifact of our own cancel propagating (#632): the
        // stage never completed, so the live INTERRUPTED is transient by
        // definition. The claim overwrites it without reading it.
        stage.setResult(CommandResult.INTERRUPTED);

        supervisor.claimTimeout(0, stage);

        UpstreamOutcomeSupervisor.Outcome outcome = supervisor.snapshot(0);
        assertEquals(CommandResult.FAILURE, outcome.result());
        assertTrue(outcome.error() instanceof TimeoutException);
        assertEquals(CommandResult.FAILURE, stage.getResult());
    }

    @Test
    public void testClaimIsNoOpOnCompletedInterrupted() {
        UpstreamOutcomeSupervisor supervisor = new UpstreamOutcomeSupervisor(1);
        StubExecution stage = new StubExecution();
        // A stage that genuinely returned INTERRUPTED (#645): completed,
        // so no timeout may be attached and the value must stand.
        supervisor.stageStarted(0);
        assertTrue(supervisor.completeStage(0, stage, CommandResult.INTERRUPTED, null));

        supervisor.claimTimeout(0, stage);

        UpstreamOutcomeSupervisor.Outcome outcome = supervisor.snapshot(0);
        assertEquals(CommandResult.INTERRUPTED, outcome.result());
        assertNull("Completed stages receive no timeout error", outcome.error());
    }

    @Test
    public void testClaimKeepsCompletedPipeBroken() {
        UpstreamOutcomeSupervisor supervisor = new UpstreamOutcomeSupervisor(1);
        StubExecution stage = new StubExecution();
        RuntimeException broken = new RuntimeException("broken");

        supervisor.stageStarted(0);
        assertTrue(supervisor.completeStage(0, stage, CommandResult.PIPE_BROKEN, broken));
        supervisor.claimTimeout(0, stage);

        UpstreamOutcomeSupervisor.Outcome outcome = supervisor.snapshot(0);
        assertEquals("Genuine terminal result must survive the claim",
                CommandResult.PIPE_BROKEN, outcome.result());
        assertSame(broken, outcome.error());
    }

    @Test
    public void testLateCompleteSuppressedOnceClaimed() {
        UpstreamOutcomeSupervisor supervisor = new UpstreamOutcomeSupervisor(1);
        StubExecution stage = new StubExecution();

        supervisor.stageStarted(0);
        supervisor.claimTimeout(0, stage);
        assertFalse(supervisor.completeStage(0, stage,
                CommandResult.FAILURE, new InterruptedException("late")));
        UpstreamOutcomeSupervisor.Outcome outcome = supervisor.snapshot(0);
        assertEquals("Finalized outcome must stand", CommandResult.FAILURE, outcome.result());
        assertTrue(outcome.error() instanceof TimeoutException);
        assertEquals("Late writes cannot move the cursor either",
                CommandResult.FAILURE, stage.getResult());
    }

    @Test
    public void testLateLiveWriteInvisibleToSnapshot() {
        UpstreamOutcomeSupervisor supervisor = new UpstreamOutcomeSupervisor(1);
        StubExecution stage = new StubExecution();

        supervisor.stageStarted(0);
        supervisor.claimTimeout(0, stage);
        // The worker's unguarded post-timeout write lands on the live
        // Execution only (#632 second window).
        stage.setResult(CommandResult.INTERRUPTED);

        UpstreamOutcomeSupervisor.Outcome outcome = supervisor.snapshot(0);
        assertEquals(CommandResult.FAILURE, outcome.result());
        assertTrue(outcome.error() instanceof TimeoutException);
    }

    @Test
    public void testSnapshotBounds() {
        UpstreamOutcomeSupervisor supervisor = new UpstreamOutcomeSupervisor(1);
        assertNull(supervisor.snapshot(-1));
        assertNull(supervisor.snapshot(1));
        assertNull("Unfinalized stage has no snapshot", supervisor.snapshot(0));
    }
}
