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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.Executable;
import org.aesh.command.Execution;
import org.aesh.command.PipelineConfig;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.result.ResultHandler;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.tty.Signal;
import org.aesh.terminal.utils.Config;
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

    static FakeExecution ignoringExecution(CountDownLatch entered, AtomicBoolean stop) {
        return new FakeExecution() {
            @Override
            public CommandResult execute() {
                entered.countDown();
                while (!stop.get())
                    Thread.yield();
                result = CommandResult.SUCCESS;
                return result;
            }
        };
    }

    private static ProcessManager testManager() {
        return new ProcessManager(null) {
            @Override
            public void processFinished(CommandJob job) {
            }
        };
    }

    @Test
    public void testRepeatedInterruptAbandonsUnresponsiveJob() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        AtomicBoolean stop = new AtomicBoolean();
        FakeExecution execution = ignoringExecution(entered, stop);
        CommandJob job = new CommandJob(testManager(), new TestConnection(), execution, "test", null);

        job.start();
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        job.accept(Signal.INT);
        assertEquals(JobState.CANCELLATION_REQUESTED, job.state());
        job.accept(Signal.INT);
        assertTrue("Job should be abandoned after grace period",
                job.awaitCompletion(10, TimeUnit.SECONDS));
        assertEquals(JobState.KILLED, job.state());
        assertEquals(CommandResult.KILLED, job.result());
        assertFalse(job.isRunning());
        stop.set(true);
    }

    @Test
    public void testThirdInterruptAbandonsImmediately() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        AtomicBoolean stop = new AtomicBoolean();
        FakeExecution execution = ignoringExecution(entered, stop);
        CommandJob job = new CommandJob(testManager(), new TestConnection(), execution, "test", null);

        job.start();
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        long start = System.currentTimeMillis();
        job.accept(Signal.INT);
        job.accept(Signal.INT);
        job.accept(Signal.INT);
        assertTrue(job.awaitCompletion(10, TimeUnit.SECONDS));
        long elapsed = System.currentTimeMillis() - start;
        assertEquals(CommandResult.KILLED, job.result());
        assertTrue("Third interrupt must skip the grace period, took: " + elapsed, elapsed < 1500);
        stop.set(true);
    }

    @Test
    public void testSecondInterruptReassertsClearedFlag() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger attempts = new AtomicInteger();
        FakeExecution execution = new FakeExecution() {
            @Override
            public CommandResult execute() {
                entered.countDown();
                try {
                    release.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    if (attempts.incrementAndGet() == 1) {
                        Thread.interrupted();
                        try {
                            release.await(30, TimeUnit.SECONDS);
                        } catch (InterruptedException again) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        Thread.currentThread().interrupt();
                    }
                }
                result = CommandResult.SUCCESS;
                return result;
            }
        };
        CommandJob job = new CommandJob(testManager(), new TestConnection(), execution, "test", null);

        job.start();
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        job.accept(Signal.INT);
        long deadline = System.currentTimeMillis() + 5000;
        while (attempts.get() < 1 && System.currentTimeMillis() < deadline)
            Thread.sleep(10);
        assertEquals(1, attempts.get());
        assertEquals(JobState.CANCELLATION_REQUESTED, job.state());
        job.accept(Signal.INT);
        assertTrue(job.awaitCompletion(5, TimeUnit.SECONDS));
        assertEquals(JobState.INTERRUPTED, job.state());
        assertEquals(CommandResult.INTERRUPTED, job.result());
        release.countDown();
    }

    @CommandDefinition(name = "spin", description = "ignores interrupts")
    public static class SpinCommand implements Command<CommandInvocation> {
        static final AtomicBoolean stop = new AtomicBoolean();
        static CountDownLatch entered = new CountDownLatch(1);

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            entered.countDown();
            while (!stop.get())
                Thread.yield();
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "ok", description = "trivial command")
    public static class OkCommand implements Command<CommandInvocation> {
        static volatile boolean executed;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            executed = true;
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testAbandonedJobReclaimsPrompt() throws Exception {
        SpinCommand.stop.set(false);
        SpinCommand.entered = new CountDownLatch(1);
        OkCommand.executed = false;
        CountDownLatch killedLatch = new CountDownLatch(1);
        CountDownLatch okLatch = new CountDownLatch(1);
        AtomicReference<CommandResult> spinResult = new AtomicReference<>();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(SpinCommand.class)
                .command(OkCommand.class)
                .create();

        TestConnection connection = new TestConnection();
        Settings settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .commandExecutionListener((line, result, durationMs) -> {
                    if (line.trim().startsWith("spin")) {
                        spinResult.set(result);
                        killedLatch.countDown();
                    } else {
                        okLatch.countDown();
                    }
                })
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        connection.read("spin" + Config.getLineSeparator());
        assertTrue(SpinCommand.entered.await(5, TimeUnit.SECONDS));
        CommandJob job = (CommandJob) connection.signalHandler();
        job.accept(Signal.INT);
        job.accept(Signal.INT);
        assertTrue("Unresponsive job should be abandoned", killedLatch.await(10, TimeUnit.SECONDS));
        assertEquals(CommandResult.KILLED, spinResult.get());

        connection.read("ok" + Config.getLineSeparator());
        assertTrue("Prompt should accept input after abandon", okLatch.await(5, TimeUnit.SECONDS));
        assertTrue(OkCommand.executed);

        SpinCommand.stop.set(true);
        console.stop();
    }

    @Test
    public void testUpstreamSecondInterrupt() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        Thread flincher = new Thread(() -> {
            entered.countDown();
            CountDownLatch latch = new CountDownLatch(1);
            try {
                latch.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException first) {
                Thread.interrupted();
                try {
                    latch.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException second) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        flincher.setDaemon(true);
        flincher.start();
        assertTrue(entered.await(5, TimeUnit.SECONDS));

        CommandJob job = new CommandJob(testManager(), new TestConnection(), new FakeExecution(), "test",
                null);
        job.setUpstreamPipeThreads(Collections.singletonList(flincher));
        job.setPipelineConfig(new PipelineConfig(16, 8192, 200, true));

        long start = System.currentTimeMillis();
        job.awaitUpstreamPipeThreads();
        long elapsed = System.currentTimeMillis() - start;

        flincher.join(5000);
        assertFalse("Re-interrupted upstream should exit", flincher.isAlive());
        assertTrue("Second interrupt should bound the join, took: " + elapsed, elapsed < 10000);
    }

    @Test
    public void testKilledExitCodeMapping() {
        assertEquals(137, CommandResult.KILLED.getExitCode());
        assertEquals(137, CommandResult.KILLED.getResultValue());
        assertTrue(CommandResult.valueOf(137) == CommandResult.KILLED);
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
