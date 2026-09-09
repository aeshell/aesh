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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.Executable;
import org.aesh.command.Execution;
import org.aesh.command.PipelineConfig;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.result.ResultHandler;
import org.aesh.terminal.tty.Signal;
import org.aesh.tty.TestConnection;
import org.junit.Test;

public class CommandJobTest {

    static class FakeExecution implements Execution<CommandInvocation> {
        volatile CommandResult result;
        final AtomicInteger runs = new AtomicInteger();

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
            runs.incrementAndGet();
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

    private static CommandJob runInline(FakeExecution execution) {
        ProcessManager manager = new ProcessManager(null) {
            @Override
            public void processFinished(CommandJob job) {
            }
        };
        CommandJob job = new CommandJob(manager, new TestConnection(), execution, "test", null);
        job.run();
        return job;
    }

    @Test
    public void testSyncSuccess() throws Exception {
        FakeExecution execution = new FakeExecution();
        execution.result = CommandResult.SUCCESS;

        CommandJob job = runInline(execution);

        assertEquals(JobState.COMPLETED, job.state());
        assertFalse(job.isRunning());
        assertEquals(CommandResult.SUCCESS, job.result());
        assertNull(job.error());
        assertNotNull(job.id());
        assertTrue(job.duration().toMillis() >= 0);
        job.awaitCompletion();
    }

    @Test
    public void testDurationZeroBeforeStart() {
        ProcessManager manager = new ProcessManager(null) {
            @Override
            public void processFinished(CommandJob job) {
            }
        };
        CommandJob job = new CommandJob(manager, new TestConnection(), new FakeExecution(), "test", null);

        assertEquals(JobState.CREATED, job.state());
        assertEquals(Duration.ZERO, job.duration());
        assertNull(job.result());
    }

    @Test
    public void testSyncExceptionFails() {
        FakeExecution execution = new FakeExecution() {
            @Override
            public CommandResult execute() throws CommandException {
                throw new CommandException("boom");
            }
        };
        AtomicReference<CommandResult> listenerResult = new AtomicReference<>();
        AtomicReference<Throwable> listenerError = new AtomicReference<>();
        ProcessManager manager = new ProcessManager(null) {
            @Override
            public void processFinished(CommandJob job) {
            }
        };
        CommandJob job = new CommandJob(manager, new TestConnection(), execution, "test",
                new org.aesh.command.CommandExecutionListener() {
                    @Override
                    public void onCommandComplete(String commandLine, CommandResult result, long durationMs) {
                    }

                    @Override
                    public void onCommandComplete(String commandLine, CommandResult result, long durationMs,
                            Throwable error) {
                        listenerResult.set(result);
                        listenerError.set(error);
                    }
                });
        job.run();

        assertEquals(JobState.FAILED, job.state());
        assertEquals(CommandResult.FAILURE, job.result());
        assertTrue(job.error() instanceof CommandException);
        assertEquals(CommandResult.FAILURE, listenerResult.get());
        assertTrue(listenerError.get() instanceof CommandException);
    }

    @Test
    public void testAsyncSuccess() throws Exception {
        FakeExecution execution = new FakeExecution();
        execution.result = CommandResult.SUCCESS;
        ProcessManager manager = new ProcessManager(null) {
            @Override
            public void processFinished(CommandJob job) {
            }
        };
        CommandJob job = new CommandJob(manager, new TestConnection(), execution, "test", null);

        job.start();
        assertTrue(job.awaitCompletion(5, TimeUnit.SECONDS));
        assertEquals(JobState.COMPLETED, job.state());
        assertEquals(1, execution.runs.get());
    }

    @Test
    public void testDoubleStartRunsOnce() throws Exception {
        FakeExecution execution = new FakeExecution();
        execution.result = CommandResult.SUCCESS;
        ProcessManager manager = new ProcessManager(null) {
            @Override
            public void processFinished(CommandJob job) {
            }
        };
        CommandJob job = new CommandJob(manager, new TestConnection(), execution, "test", null);

        job.start();
        job.start();
        assertTrue(job.awaitCompletion(5, TimeUnit.SECONDS));
        assertEquals(1, execution.runs.get());
    }

    @Test
    public void testInterruptMarksInterruptedEvenWhenIgnored() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        FakeExecution execution = new FakeExecution() {
            @Override
            public CommandResult execute() {
                entered.countDown();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                result = CommandResult.SUCCESS;
                return result;
            }
        };
        ProcessManager manager = new ProcessManager(null) {
            @Override
            public void processFinished(CommandJob job) {
            }
        };
        CommandJob job = new CommandJob(manager, new TestConnection(), execution, "test", null);

        job.start();
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        assertEquals(JobState.RUNNING, job.state());
        assertTrue(job.isRunning());
        job.accept(Signal.INT);
        assertEquals(JobState.CANCELLATION_REQUESTED, job.state());
        release.countDown();
        assertTrue(job.awaitCompletion(5, TimeUnit.SECONDS));
        assertEquals(JobState.INTERRUPTED, job.state());
        assertEquals(CommandResult.INTERRUPTED, job.result());
    }

    @Test
    public void testInterruptAfterCompletionIsNoOp() {
        FakeExecution execution = new FakeExecution();
        execution.result = CommandResult.SUCCESS;

        CommandJob job = runInline(execution);
        job.requestInterrupt();

        assertEquals(JobState.COMPLETED, job.state());
        assertEquals(CommandResult.SUCCESS, job.result());
    }

    @Test
    public void testJobIdsUnique() {
        CommandJob first = runInline(new FakeExecution());
        CommandJob second = runInline(new FakeExecution());

        assertNotEquals(first.id(), second.id());
    }

    @Test
    public void testAwaitUpstreamRespectsJoinTimeout() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        Thread blocker = new Thread(() -> {
            entered.countDown();
            try {
                new CountDownLatch(1).await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        blocker.setDaemon(true);
        blocker.start();
        assertTrue(entered.await(5, TimeUnit.SECONDS));

        ProcessManager manager = new ProcessManager(null) {
            @Override
            public void processFinished(CommandJob job) {
            }
        };
        CommandJob job = new CommandJob(manager, new TestConnection(), new FakeExecution(), "test", null);
        job.setUpstreamPipeThreads(Collections.singletonList(blocker));
        job.setPipelineConfig(new PipelineConfig(16, 8192, 200, true));

        long start = System.currentTimeMillis();
        job.awaitUpstreamPipeThreads();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue("Join must time out, took: " + elapsed, elapsed < 10000);
        blocker.join(5000);
        assertFalse(blocker.isAlive());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testDeprecatedProcessForwards() throws Exception {
        FakeExecution execution = new FakeExecution();
        execution.result = CommandResult.SUCCESS;
        ProcessManager manager = new ProcessManager(null) {
            @Override
            public void processFinished(CommandJob job) {
            }
        };
        Process process = new Process(manager, new TestConnection(), execution, "test", null);

        process.start();
        process.join();
        assertFalse(process.isAlive());
        assertTrue(process.pid() >= 1);
        assertEquals(1, execution.runs.get());
    }
}
