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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.Executable;
import org.aesh.command.Execution;
import org.aesh.command.Executor;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.result.ResultHandler;
import org.aesh.readline.Readline;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.Connection;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Pins the single drain handoff from #638: every drain request is recorded,
 * so a turn release always picks up work that arrived while the turn was
 * held — with no takeover wait, parking, or extra drain thread.
 * <p>
 * The interleavings are forced deterministically by holding the turn with
 * {@link ProcessManager#acquireDrainTurnForTest()}: the racing thread is
 * joined before the release, so only genuine async command execution waits
 * on a (generous) bound — never the interleaving itself.
 */
public class ProcessManagerDrainTest {

    static class StubConsole implements Console {
        final AtomicInteger rearms = new AtomicInteger();
        volatile boolean running = true;

        @Override
        public void stop() {
            running = false;
        }

        @Override
        public boolean running() {
            return running;
        }

        @Override
        public void setPrompt(Prompt prompt) {
        }

        @Override
        public Prompt prompt() {
            return null;
        }

        @Override
        public AeshContext context() {
            return null;
        }

        @Override
        public String helpInfo(String commandName) {
            return null;
        }

        @Override
        public void read(Connection conn, Readline readline) {
        }

        @Override
        public void read() {
            rearms.incrementAndGet();
        }

        @Override
        public void setPrompt(String s) {
        }
    }

    /**
     * Records its own result, like a real execution: the planner keys the
     * next unit off a non-null result, so a fake that never records would
     * be relaunched on every drain turn.
     */
    static class DrainExecution implements Execution<CommandInvocation> {
        final AtomicInteger runs = new AtomicInteger();
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
        public CommandResult execute() {
            runs.incrementAndGet();
            result = CommandResult.SUCCESS;
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

    /**
     * Stays inside the execution until released, so the test can hold the
     * drain turn across the worker's whole lifecycle.
     */
    static class GatedExecution extends DrainExecution {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final boolean selfInterrupt;

        GatedExecution(boolean selfInterrupt) {
            this.selfInterrupt = selfInterrupt;
        }

        @Override
        public CommandResult execute() {
            runs.incrementAndGet();
            entered.countDown();
            try {
                if (!release.await(30, TimeUnit.SECONDS))
                    throw new RuntimeException("test gate was never released");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("test gate interrupted", e);
            }
            if (selfInterrupt)
                Thread.currentThread().interrupt();
            result = CommandResult.SUCCESS;
            return result;
        }
    }

    private static Executor<CommandInvocation> executorFor(DrainExecution execution) {
        return new Executor<>(Collections.<Execution<CommandInvocation>> singletonList(execution));
    }

    @Test
    public void testSessionQueuedDuringHeldTurnIsDrainedOnRelease() throws Exception {
        StubConsole console = new StubConsole();
        ProcessManager manager = new ProcessManager(console);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicInteger completions = new AtomicInteger();
        manager.setExecutionListener((line, result, durationMs) -> {
            completions.incrementAndGet();
            completed.countDown();
        });
        DrainExecution execution = new DrainExecution();

        // Simulate the owner observing an empty queue: the turn is held, so
        // no drain loop can run while the racer below queues its session.
        assertTrue(manager.acquireDrainTurnForTest());
        Thread racer = new Thread(
                () -> manager.execute(executorFor(execution), new TestConnection(), "ok"));
        racer.setDaemon(true);
        racer.start();
        racer.join(10000);
        assertFalse("drain request must not block while the turn is held", racer.isAlive());

        // Release through the production release-then-recheck path: the
        // recorded request must trigger a follow-up turn even though the
        // racer already gave up its own acquire attempt.
        manager.releaseDrainTurnForTest();

        assertTrue("queued session was never drained", completed.await(10, TimeUnit.SECONDS));
        assertEquals(1, execution.runs.get());
        assertEquals(1, completions.get());
        assertEquals("session must complete before readline is re-armed", 1, console.rearms.get());
    }

    @Test
    public void testCompletionDuringHeldTurnDoesNotBlockAndIsDrainedOnRelease() throws Exception {
        DrainHandle handle = runSessionToCompletionWhileTurnHeld(false);

        // The old post-completion path parked here for up to 10 seconds
        // waiting for the turn; the handoff must return immediately.
        assertTrue("completion must not block while the turn is held",
                handle.completed.await(5, TimeUnit.SECONDS));
        assertEquals("recorded request must wait for the release", 0, handle.console.rearms.get());

        handle.manager.releaseDrainTurnForTest();

        assertEquals(1, handle.execution.runs.get());
        assertEquals(1, handle.completions.get());
        assertEquals("post-completion drain must re-arm exactly once", 1, handle.console.rearms.get());
    }

    @Test
    public void testInterruptedCompletionIsStillDrainedOnRelease() throws Exception {
        DrainHandle handle = runSessionToCompletionWhileTurnHeld(true);

        assertTrue("interrupted completion must still finish",
                handle.completed.await(5, TimeUnit.SECONDS));

        handle.manager.releaseDrainTurnForTest();

        assertEquals(1, handle.execution.runs.get());
        assertEquals(1, handle.completions.get());
        assertEquals("interrupted completion must not drop the re-arm", 1, handle.console.rearms.get());
    }

    @Test
    public void testSynchronousInlineExecutionRearmsExactlyOnce() throws Exception {
        StubConsole console = new StubConsole();
        ProcessManager manager = new ProcessManager(console);
        manager.setSynchronous(true);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicInteger completions = new AtomicInteger();
        manager.setExecutionListener((line, result, durationMs) -> {
            completions.incrementAndGet();
            completed.countDown();
        });
        DrainExecution execution = new DrainExecution();

        // Same-thread re-entrancy: the inline job completes on the drain
        // owner thread, whose post-completion request must not deadlock or
        // double-run; the outer drain loop picks the continuation up itself.
        manager.execute(executorFor(execution), new TestConnection(), "ok");

        assertTrue(completed.await(10, TimeUnit.SECONDS));
        assertEquals(1, execution.runs.get());
        assertEquals(1, completions.get());
        assertEquals(1, console.rearms.get());
    }

    private static final class DrainHandle {
        final ProcessManager manager;
        final StubConsole console;
        final GatedExecution execution;
        final CountDownLatch completed;
        final AtomicInteger completions;

        DrainHandle(ProcessManager manager, StubConsole console, GatedExecution execution,
                CountDownLatch completed, AtomicInteger completions) {
            this.manager = manager;
            this.console = console;
            this.execution = execution;
            this.completed = completed;
            this.completions = completions;
        }
    }

    /**
     * Runs a single-command session whose worker is released while the test
     * thread holds the drain turn — the forced form of the #634 window
     * where a tiny command finishes before the launcher releases the turn.
     * Returns with the turn still held and the worker finished.
     */
    private static DrainHandle runSessionToCompletionWhileTurnHeld(boolean selfInterrupt) throws Exception {
        StubConsole console = new StubConsole();
        ProcessManager manager = new ProcessManager(console);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicInteger completions = new AtomicInteger();
        manager.setExecutionListener((line, result, durationMs) -> {
            completions.incrementAndGet();
            completed.countDown();
        });
        GatedExecution execution = new GatedExecution(selfInterrupt);

        manager.execute(executorFor(execution), new TestConnection(), "ok");
        assertTrue("command never started", execution.entered.await(10, TimeUnit.SECONDS));
        // The launch released the turn; take it to simulate the launcher
        // still being inside its launch call when the worker finishes.
        assertTrue(manager.acquireDrainTurnForTest());
        execution.release.countDown();
        return new DrainHandle(manager, console, execution, completed, completions);
    }
}
