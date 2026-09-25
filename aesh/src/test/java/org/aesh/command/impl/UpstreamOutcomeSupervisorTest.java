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
 * Contract tests for the shared upstream-stage settle logic (#638).
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
    public void testTaskOutcomeRecordedWhenUnsettled() {
        UpstreamOutcomeSupervisor supervisor = new UpstreamOutcomeSupervisor(1);
        StubExecution stage = new StubExecution();
        Throwable[] errors = new Throwable[1];
        RuntimeException failure = new RuntimeException("boom");

        assertTrue(supervisor.recordTaskOutcome(0, errors, stage, failure, CommandResult.FAILURE));
        assertEquals(CommandResult.FAILURE, stage.getResult());
        assertSame(failure, errors[0]);
        assertNull("Unsettled stage has no claimed outcome", supervisor.claimedResult(0));
    }

    @Test
    public void testClaimTimeoutOnIdleStage() {
        UpstreamOutcomeSupervisor supervisor = new UpstreamOutcomeSupervisor(1);
        StubExecution stage = new StubExecution();
        Throwable[] errors = new Throwable[1];
        TimeoutException timeout = new TimeoutException("join timed out");

        supervisor.claimTimeout(0, stage, errors, timeout);
        assertEquals(CommandResult.FAILURE, stage.getResult());
        assertSame(timeout, errors[0]);
        assertEquals(CommandResult.FAILURE, supervisor.claimedResult(0));
    }

    @Test
    public void testClaimOverwritesTransientInterrupted() {
        UpstreamOutcomeSupervisor supervisor = new UpstreamOutcomeSupervisor(1);
        StubExecution stage = new StubExecution();
        Throwable[] errors = new Throwable[1];
        // Mid-unwind artifact of our own cancel propagating (#632).
        stage.setResult(CommandResult.INTERRUPTED);

        supervisor.claimTimeout(0, stage, errors, new TimeoutException("join timed out"));
        assertEquals(CommandResult.FAILURE, stage.getResult());
        assertTrue(errors[0] instanceof TimeoutException);
        assertEquals(CommandResult.FAILURE, supervisor.claimedResult(0));
    }

    @Test
    public void testClaimKeepsGenuineTerminalResult() {
        UpstreamOutcomeSupervisor supervisor = new UpstreamOutcomeSupervisor(1);
        StubExecution stage = new StubExecution();
        Throwable[] errors = new Throwable[1];
        stage.setResult(CommandResult.PIPE_BROKEN);

        supervisor.claimTimeout(0, stage, errors, new TimeoutException("join timed out"));
        assertEquals("Genuine terminal result must survive the claim",
                CommandResult.PIPE_BROKEN, stage.getResult());
        assertEquals(CommandResult.PIPE_BROKEN, supervisor.claimedResult(0));
        assertNull("No timeout error when the terminal result stands", errors[0]);
    }

    @Test
    public void testClaimPreservesExistingError() {
        UpstreamOutcomeSupervisor supervisor = new UpstreamOutcomeSupervisor(1);
        StubExecution stage = new StubExecution();
        Throwable[] errors = new Throwable[1];
        RuntimeException original = new RuntimeException("original");
        errors[0] = original;

        supervisor.claimTimeout(0, stage, errors, new TimeoutException("join timed out"));
        assertSame("Pre-existing error must not be overwritten", original, errors[0]);
        assertEquals(CommandResult.FAILURE, supervisor.claimedResult(0));
    }

    @Test
    public void testLateTaskWriteSuppressedOnceSettled() {
        UpstreamOutcomeSupervisor supervisor = new UpstreamOutcomeSupervisor(1);
        StubExecution stage = new StubExecution();
        Throwable[] errors = new Throwable[1];

        supervisor.claimTimeout(0, stage, errors, new TimeoutException("join timed out"));
        assertFalse(supervisor.recordTaskOutcome(0, errors, stage,
                new InterruptedException("late"), CommandResult.FAILURE));
        assertEquals("Settled outcome must stand", CommandResult.FAILURE, stage.getResult());
        assertTrue(errors[0] instanceof TimeoutException);
    }

    @Test
    public void testClaimedResultBounds() {
        UpstreamOutcomeSupervisor supervisor = new UpstreamOutcomeSupervisor(1);
        assertNull(supervisor.claimedResult(-1));
        assertNull(supervisor.claimedResult(1));
    }
}
