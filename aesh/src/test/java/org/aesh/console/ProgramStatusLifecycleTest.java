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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.terminal.StreamConnection;
import org.aesh.terminal.tty.Signal;
import org.aesh.terminal.utils.ProgramStatus;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Phase 2 (#676): automatic lifecycle publication over a live console.
 * Asserts raw OSC 7501 bytes on the terminal connection: work start
 * before execution, terminal outcomes before prompt handoff, error for
 * pre-job failures, idle for real cancellation, and no report bytes in
 * redirected files. Disabled reporting stays completely quiet.
 */
public class ProgramStatusLifecycleTest {

    private static final String APP = "smoke";

    @CommandDefinition(name = "ok", description = "ok")
    public static class OkCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            commandInvocation.println("ok-output");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "fail", description = "fail")
    public static class FailCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.FAILURE;
        }
    }

    @CommandDefinition(name = "slow", description = "slow")
    public static class SlowCommand implements Command<CommandInvocation> {
        static CountDownLatch entered = new CountDownLatch(1);
        static CountDownLatch release = new CountDownLatch(1);

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws InterruptedException {
            entered.countDown();
            release.await(30, TimeUnit.SECONDS);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "block", description = "block")
    public static class BlockCommand implements Command<CommandInvocation> {
        static CountDownLatch entered = new CountDownLatch(1);

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws InterruptedException {
            entered.countDown();
            new CountDownLatch(1).await(30, TimeUnit.SECONDS);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "progress", description = "progress")
    public static class ProgressCommand implements Command<CommandInvocation> {
        static volatile CommandInvocation stashed;
        static volatile boolean progressResult;
        static volatile boolean blockedResult;
        static volatile boolean clearResult;

        static void reset() {
            stashed = null;
            progressResult = false;
            blockedResult = false;
            clearResult = false;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            stashed = commandInvocation;
            progressResult = commandInvocation.reportProgramStatus(ProgramStatus.builder(
                    ProgramStatus.State.WORKING).progress(50).message("half").build());
            blockedResult = commandInvocation.reportProgramStatus(ProgramStatus.builder(
                    ProgramStatus.State.BLOCKED).kind(ProgramStatus.BlockedKind.QUESTION)
                    .message("Sure?").build());
            commandInvocation.reportProgramStatus(ProgramStatus.builder(
                    ProgramStatus.State.WORKING).id("task/1").message("sub").build());
            clearResult = commandInvocation.clearProgramStatus("task/1");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "up", description = "up")
    public static class UpCommand implements Command<CommandInvocation> {
        static volatile boolean reported;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            reported = commandInvocation.reportProgramStatus(ProgramStatus.builder(
                    ProgramStatus.State.WORKING).message("up-work").build());
            commandInvocation.println("piped");
            return CommandResult.SUCCESS;
        }
    }

    private static final class Session implements AutoCloseable {
        final PipedOutputStream testOut;
        final PipedInputStream pipeIn;
        final StreamConnection connection;
        final ByteArrayOutputStream consoleOut;
        final ReadlineConsole console;
        final AtomicReference<CountDownLatch> completionLatch = new AtomicReference<>(new CountDownLatch(1));
        final AtomicReference<Throwable> readerDeath = new AtomicReference<>();
        final Thread replThread;

        Session(boolean programStatus) throws Exception {
            pipeIn = new PipedInputStream(4096);
            testOut = new PipedOutputStream(pipeIn);
            consoleOut = new ByteArrayOutputStream();
            connection = new StreamConnection(StandardCharsets.UTF_8, pipeIn, consoleOut);
            connection.setReaderDeathHook(readerDeath::set);

            CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder
                    .<CommandInvocation> builder()
                    .command(OkCommand.class)
                    .command(FailCommand.class)
                    .command(SlowCommand.class)
                    .command(BlockCommand.class)
                    .command(ProgressCommand.class)
                    .command(UpCommand.class)
                    .create();
            File exportFile = File.createTempFile("aesh-ps-export", ".txt");
            exportFile.deleteOnExit();
            File historyFile = File.createTempFile("aesh-ps-history", ".txt");
            historyFile.deleteOnExit();
            Settings<CommandInvocation> settings = SettingsBuilder.builder()
                    .connection(connection)
                    .enableOperatorParser(true)
                    .commandRegistry(registry)
                    .exportFile(exportFile)
                    .historyFile(historyFile)
                    .enableProgramStatus(programStatus)
                    .programStatusAppName(APP)
                    .commandExecutionListener(
                            (line, result, durationMs) -> completionLatch.get().countDown())
                    .logging(true)
                    .build();

            console = new ReadlineConsole(settings);
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

        String output() {
            synchronized (consoleOut) {
                return new String(consoleOut.toByteArray(), StandardCharsets.UTF_8);
            }
        }

        void awaitBytes(String needle, long timeoutMs) throws Exception {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (!output().contains(needle)) {
                if (System.currentTimeMillis() >= deadline)
                    fail("Timed out waiting for bytes <" + needle + ">, readerDeath="
                            + readerDeath.get() + " output=" + output());
                Thread.sleep(25);
            }
        }

        @Override
        public void close() {
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

    private static String sequence(ProgramStatus.State state) {
        return ProgramStatus.builder(state).app(APP).build().toSequence();
    }

    @Test
    public void testDisabledWritesNothing() throws Exception {
        try (Session session = new Session(false)) {
            CountDownLatch done = new CountDownLatch(1);
            session.completionLatch.set(done);
            session.writeChunk("ok\n");
            assertTrue("command should complete",
                    done.await(15, TimeUnit.SECONDS));
            assertFalse("disabled reporting must stay quiet, got: " + session.output(),
                    session.output().contains("7501"));
        }
    }

    @Test
    public void testWorkingThenDone() throws Exception {
        SlowCommand.entered = new CountDownLatch(1);
        SlowCommand.release = new CountDownLatch(1);
        try (Session session = new Session(true)) {
            CountDownLatch done = new CountDownLatch(1);
            session.completionLatch.set(done);
            session.writeChunk("slow\n");
            assertTrue("slow command should start",
                    SlowCommand.entered.await(10, TimeUnit.SECONDS));
            session.awaitBytes(sequence(ProgramStatus.State.WORKING), 10000);
            SlowCommand.release.countDown();
            assertTrue("slow command should complete",
                    done.await(15, TimeUnit.SECONDS));
            String output = session.output();
            assertTrue("done must publish, got: " + output,
                    output.contains(sequence(ProgramStatus.State.DONE)));
            assertTrue("working must precede done",
                    output.indexOf(sequence(ProgramStatus.State.WORKING)) < output.indexOf(sequence(ProgramStatus.State.DONE)));
        }
    }

    @Test
    public void testFailureReportsError() throws Exception {
        try (Session session = new Session(true)) {
            CountDownLatch done = new CountDownLatch(1);
            session.completionLatch.set(done);
            session.writeChunk("fail\n");
            assertTrue("fail command should complete",
                    done.await(15, TimeUnit.SECONDS));
            String output = session.output();
            assertTrue("working must publish, got: " + output,
                    output.contains(sequence(ProgramStatus.State.WORKING)));
            assertTrue("error must publish, got: " + output,
                    output.contains(sequence(ProgramStatus.State.ERROR)));
        }
    }

    @Test
    public void testPreJobFailureReportsError() throws Exception {
        try (Session session = new Session(true)) {
            CountDownLatch done = new CountDownLatch(1);
            session.completionLatch.set(done);
            session.writeChunk("boguscmd\n");
            assertTrue("unknown command should complete",
                    done.await(15, TimeUnit.SECONDS));
            assertTrue("error must publish for unknown commands, got: " + session.output(),
                    session.output().contains(sequence(ProgramStatus.State.ERROR)));
        }
    }

    @Test
    public void testCancellationReportsIdle() throws Exception {
        BlockCommand.entered = new CountDownLatch(1);
        try (Session session = new Session(true)) {
            CountDownLatch blockDone = new CountDownLatch(1);
            session.completionLatch.set(blockDone);
            session.writeChunk("block\n");
            assertTrue("blocker should start",
                    BlockCommand.entered.await(10, TimeUnit.SECONDS));

            CommandJob job = (CommandJob) session.connection.signalHandler();
            job.accept(Signal.INT);
            assertTrue("interrupted command should complete",
                    blockDone.await(15, TimeUnit.SECONDS));
            assertTrue("idle must publish on cancellation, got: " + session.output(),
                    session.output().contains(sequence(ProgramStatus.State.IDLE)));
        }
    }

    @Test
    public void testRedirectedFileExcludesReports() throws Exception {
        File target = File.createTempFile("aesh-ps-redirect", ".txt");
        target.deleteOnExit();
        try (Session session = new Session(true)) {
            CountDownLatch done = new CountDownLatch(1);
            session.completionLatch.set(done);
            session.writeChunk("ok > " + target.getAbsolutePath() + "\n");
            assertTrue("redirected command should complete",
                    done.await(15, TimeUnit.SECONDS));
            String filed = new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8);
            assertFalse("redirected file must not carry reports, got: " + filed,
                    filed.contains("7501"));
            assertTrue("done must still publish on the terminal, got: " + session.output(),
                    session.output().contains(sequence(ProgramStatus.State.DONE)));
        }
    }

    private static String sequenced(ProgramStatus status) {
        ProgramStatus.Builder builder = ProgramStatus.builder(status.state());
        if (status.kind() != null)
            builder.kind(status.kind());
        if (status.id() != null)
            builder.id(status.id());
        if (status.title() != null)
            builder.title(status.title());
        if (status.progress() != null)
            builder.progress(status.progress().intValue());
        if (status.message() != null)
            builder.message(status.message());
        return builder.app(APP).build().toSequence();
    }

    @Test
    public void testExplicitProgressAndBlocked() throws Exception {
        ProgressCommand.reset();
        try (Session session = new Session(true)) {
            CountDownLatch done = new CountDownLatch(1);
            session.completionLatch.set(done);
            session.writeChunk("progress\n");
            assertTrue("progress command should complete",
                    done.await(15, TimeUnit.SECONDS));
            assertTrue("explicit progress must return true", ProgressCommand.progressResult);
            assertTrue("explicit blocked must return true", ProgressCommand.blockedResult);
            assertTrue("explicit clear must return true", ProgressCommand.clearResult);
            String output = session.output();
            assertTrue("progress bytes must publish, got: " + output,
                    output.contains(sequenced(ProgramStatus.builder(ProgramStatus.State.WORKING)
                            .progress(50).message("half").build())));
            assertTrue("blocked bytes must publish, got: " + output,
                    output.contains(sequenced(ProgramStatus.builder(ProgramStatus.State.BLOCKED)
                            .kind(ProgramStatus.BlockedKind.QUESTION).message("Sure?").build())));
            assertTrue("clear bytes must publish, got: " + output,
                    output.contains(ProgramStatus.clearSequence("task/1")));
        }
    }

    @Test
    public void testStashedInvocationFailsClosed() throws Exception {
        ProgressCommand.reset();
        String before;
        try (Session session = new Session(true)) {
            CountDownLatch done = new CountDownLatch(1);
            session.completionLatch.set(done);
            session.writeChunk("progress\n");
            assertTrue("progress command should complete",
                    done.await(15, TimeUnit.SECONDS));
            before = session.output();
        }
        assertFalse("stashed report after finish must fail",
                ProgressCommand.stashed.reportProgramStatus(
                        ProgramStatus.builder(ProgramStatus.State.WORKING).build()));
        assertFalse("stashed clear after finish must fail",
                ProgressCommand.stashed.clearProgramStatus("task/1"));
    }

    @Test
    public void testUpstreamExplicitReports() throws Exception {
        UpCommand.reported = false;
        try (Session session = new Session(true)) {
            CountDownLatch done = new CountDownLatch(1);
            session.completionLatch.set(done);
            session.writeChunk("up | ok\n");
            assertTrue("pipeline should complete",
                    done.await(15, TimeUnit.SECONDS));
            assertTrue("upstream explicit report must return true", UpCommand.reported);
            assertTrue("upstream bytes must publish, got: " + session.output(),
                    session.output().contains(sequenced(ProgramStatus.builder(
                            ProgramStatus.State.WORKING).message("up-work").build())));
        }
    }

    @Test
    public void testRuntimeExplicitUnavailableByDefault() {
        ProgressCommand.reset();
        org.aesh.AeshRuntimeRunner.builder()
                .command(ProgressCommand.class)
                .args()
                .execute();
        assertFalse("runtime without configuration must not report",
                ProgressCommand.progressResult);
    }

    @Test
    public void testRuntimeExplicitWithConnection() {
        ProgressCommand.reset();
        TestConnection connection = new TestConnection(false);
        org.aesh.AeshRuntimeRunner.builder()
                .command(ProgressCommand.class)
                .args()
                .enableProgramStatus(true)
                .programStatusAppName("rt")
                .programStatusConnection(connection)
                .execute();
        assertTrue("runtime explicit report must return true", ProgressCommand.progressResult);
        String expected = ProgramStatus.builder(ProgramStatus.State.WORKING)
                .progress(50).message("half").app("rt").build().toSequence();
        assertTrue("runtime bytes must publish, got: " + connection.getOutputBuffer(),
                connection.getOutputBuffer().contains(expected));
    }
}
