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
package org.aesh.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.PipelineExecutionListener;
import org.aesh.command.PipelineResult;
import org.aesh.command.StageOutcome;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.terminal.StreamConnection;
import org.aesh.terminal.tty.Signal;
import org.junit.Test;

/**
 * The session exit status ({@code $?}) must be published before the next
 * buffered command is parsed/preprocessed. Completion used to fire the
 * listener (which records the exit code) only after the drain had already
 * re-armed readline and launched the next command, so {@code fail\nshow $?}
 * delivered in one chunk showed {@code 0} (#646).
 * <p>
 * All multi-line sessions below are written in a single chunk with no
 * delay between lines — waiting for the previous completion first would
 * hide the ordering bug.
 */
public class ExitStatusOrderTest {

    @CommandDefinition(name = "fail", description = "fails")
    public static class FailCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.FAILURE;
        }
    }

    @CommandDefinition(name = "ok", description = "succeeds")
    public static class OkCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "show", description = "records its positional argument")
    public static class ShowCommand implements Command<CommandInvocation> {
        @Argument(description = "value")
        String value;

        static volatile String seen;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            seen = value;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "okpipe", description = "upstream printing hello")
    public static class OkPipeCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            commandInvocation.println("hello");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "block", description = "blocks until interrupted")
    public static class BlockCommand implements Command<CommandInvocation> {
        static CountDownLatch entered = new CountDownLatch(1);

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws InterruptedException {
            entered.countDown();
            new CountDownLatch(1).await(30, TimeUnit.SECONDS);
            return CommandResult.SUCCESS;
        }
    }

    private static final class Session implements AutoCloseable {
        final PipedOutputStream testOut;
        final PipedInputStream pipeIn;
        final StreamConnection connection;
        final ReadlineConsole console;
        final AtomicReference<CountDownLatch> completionLatch = new AtomicReference<>(new CountDownLatch(1));
        final AtomicReference<Throwable> readerDeath = new AtomicReference<>();
        final Thread replThread;

        Session() throws Exception {
            this(null);
        }

        Session(PipelineExecutionListener customListener) throws Exception {
            ShowCommand.seen = null;
            pipeIn = new PipedInputStream(4096);
            testOut = new PipedOutputStream(pipeIn);
            ByteArrayOutputStream consoleOut = new ByteArrayOutputStream();
            connection = new StreamConnection(StandardCharsets.UTF_8, pipeIn, consoleOut);
            connection.setReaderDeathHook(readerDeath::set);

            CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder
                    .<CommandInvocation> builder()
                    .command(FailCommand.class)
                    .command(OkCommand.class)
                    .command(ShowCommand.class)
                    .command(OkPipeCommand.class)
                    .command(BlockCommand.class)
                    .create();
            File exportFile = File.createTempFile("aesh-exit-status-export", ".txt");
            exportFile.deleteOnExit();
            File historyFile = File.createTempFile("aesh-exit-status-history", ".txt");
            historyFile.deleteOnExit();
            Settings<CommandInvocation> settings = SettingsBuilder.builder()
                    .connection(connection)
                    .enableOperatorParser(true)
                    .commandRegistry(registry)
                    .exportFile(exportFile)
                    .historyFile(historyFile)
                    .commandExecutionListener(customListener != null ? customListener
                            : (line, result, durationMs) -> completionLatch.get().countDown())
                    .logging(true)
                    .build();

            console = new ReadlineConsole(settings);
            // start() blocks in openBlocking: run the REPL on its own thread.
            // Bytes written before arming wait in the pipe, so no readiness
            // gate is needed.
            Thread repl = new Thread(() -> {
                try {
                    console.start();
                } catch (Exception e) {
                    readerDeath.set(e);
                }
            });
            repl.setDaemon(true);
            repl.setName("aesh-test-repl");
            repl.start();
            replThread = repl;
        }

        void writeChunk(String chunk) throws Exception {
            testOut.write(chunk.getBytes(StandardCharsets.UTF_8));
            testOut.flush();
        }

        @Override
        public void close() {
            // Teardown problems fail loudly: swallowing them can hide a
            // wedged reader or a leaked worker (#661).
            List<Throwable> cleanupErrors = TestSessions.errors();
            TestSessions.closeQuietly(() -> console.stop(), "console.stop", cleanupErrors);
            TestSessions.closeQuietly(testOut::close, "testOut", cleanupErrors);
            TestSessions.closeQuietly(connection::close, "connection", cleanupErrors);
            TestSessions.closeQuietly(pipeIn::close, "pipeIn", cleanupErrors);
            TestSessions.joinQuietly(replThread, "repl", cleanupErrors);
            if (readerDeath.get() != null)
                cleanupErrors.add(new IllegalStateException("reader death: " + readerDeath.get()));
            TestSessions.assertQuiet(cleanupErrors);
        }
    }

    @Test
    public void testBufferedFailThenShowSeesOne() throws Exception {
        try (Session session = new Session()) {
            CountDownLatch both = new CountDownLatch(2);
            session.completionLatch.set(both);

            session.writeChunk("fail\nshow $?\n");

            if (!both.await(15, TimeUnit.SECONDS)) {
                fail("Buffered commands never completed, readerDeath=" + session.readerDeath.get());
            }
            assertEquals("1", ShowCommand.seen);
        }
    }

    @Test
    public void testBufferedOkThenShowSeesZero() throws Exception {
        try (Session session = new Session()) {
            CountDownLatch both = new CountDownLatch(2);
            session.completionLatch.set(both);

            session.writeChunk("ok\nshow $?\n");

            if (!both.await(15, TimeUnit.SECONDS)) {
                fail("Buffered commands never completed, readerDeath=" + session.readerDeath.get());
            }
            assertEquals("0", ShowCommand.seen);
        }
    }

    @Test
    public void testBufferedNotFoundThenShowSees127() throws Exception {
        try (Session session = new Session()) {
            CountDownLatch both = new CountDownLatch(2);
            session.completionLatch.set(both);

            session.writeChunk("boguscmd\nshow $?\n");

            if (!both.await(15, TimeUnit.SECONDS)) {
                fail("Buffered commands never completed, readerDeath=" + session.readerDeath.get());
            }
            assertEquals("127", ShowCommand.seen);
        }
    }

    @Test
    public void testBufferedPipelineThenShowSeesOneAndOrderHolds() throws Exception {
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(2);
        PipelineExecutionListener listener = new PipelineExecutionListener() {
            @Override
            public void onCommandComplete(String line, CommandResult result, long durationMs) {
                events.add("command:" + line.trim());
                done.countDown();
            }

            @Override
            public void onStageComplete(StageOutcome stage) {
                events.add("stage:" + stage.stageIndex());
            }

            @Override
            public void onPipelineComplete(PipelineResult result) {
                events.add("pipeline:" + result.pipelineResult().getResultValue());
            }
        };
        try (Session session = new Session(listener)) {
            session.writeChunk("okpipe | fail\nshow $?\n");

            if (!done.await(15, TimeUnit.SECONDS)) {
                fail("Buffered commands never completed, readerDeath=" + session.readerDeath.get()
                        + " events=" + events);
            }
            assertEquals("1", ShowCommand.seen);
            // Guaranteed order: pipeline-internal events fire sequentially on
            // the terminal job's worker thread before the drain launches the
            // next command — so stage:0, stage:1, pipeline:1 lead. The two
            // command: callbacks race across worker threads (no cross-command
            // FIFO in the listener contract), so only their set is asserted.
            assertEquals(
                    Arrays.asList("stage:0", "stage:1", "pipeline:1"),
                    events.subList(0, 3));
            assertEquals(
                    new HashSet<>(Arrays.asList(
                            "command:okpipe | fail", "command:show 1")),
                    new HashSet<>(events.subList(3, events.size())));
            assertEquals(5, events.size());
        }
    }

    @Test
    public void testInterruptedThenShowSees130() throws Exception {
        BlockCommand.entered = new CountDownLatch(1);
        try (Session session = new Session()) {
            CountDownLatch blockDone = new CountDownLatch(1);
            session.completionLatch.set(blockDone);

            session.writeChunk("block\n");
            assertTrue("blocker should start", BlockCommand.entered.await(10, TimeUnit.SECONDS));

            CommandJob job = (CommandJob) session.connection.signalHandler();
            job.accept(Signal.INT);
            assertTrue("interrupted command should complete",
                    blockDone.await(10, TimeUnit.SECONDS));

            // Sent after completion, so the exit status is necessarily fresh;
            // guards the sink path for interruption outcomes.
            CountDownLatch showDone = new CountDownLatch(1);
            session.completionLatch.set(showDone);
            session.writeChunk("show $?\n");
            assertTrue("show should complete", showDone.await(10, TimeUnit.SECONDS));
            assertEquals("130", ShowCommand.seen);
        }
    }
}
