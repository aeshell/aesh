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
package org.aesh.command.operator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.PipelineConfig;
import org.aesh.command.PipelineExecutionListener;
import org.aesh.command.PipelineResult;
import org.aesh.command.StageOutcome;
import org.aesh.command.impl.AeshCommandRuntime;
import org.aesh.command.impl.operator.PipeOperator;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.ReadlineConsole;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

public class PipelineOutcomeTest {

    @CommandDefinition(name = "failpipe", description = "upstream that throws")
    public static class FailPipeCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {
            commandInvocation.println("partial");
            throw new CommandException("upstream boom");
        }
    }

    @CommandDefinition(name = "sink", description = "downstream that ignores stdin")
    public static class SinkCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "sinkdrain", description = "downstream draining stdin")
    public static class SinkDrainCommand implements Command<CommandInvocation> {
        static final List<String> lines = new ArrayList<>();

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            try {
                java.io.InputStream stdin = commandInvocation.getStdin();
                if (stdin != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stdin));
                    String line;
                    while ((line = reader.readLine()) != null)
                        lines.add(line);
                }
            } catch (Exception e) {
                return CommandResult.FAILURE;
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "truecmd", description = "upstream writing nothing")
    public static class TrueCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "okpipe", description = "upstream that succeeds")
    public static class OkPipeCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            commandInvocation.println("hello");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "chatter", description = "upstream writing many lines")
    public static class ChatterCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            for (int i = 0; i < 50; i++)
                commandInvocation.println("line-" + i);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "counter", description = "downstream line counter")
    public static class LineCounterCommand implements Command<CommandInvocation> {
        static final AtomicInteger lines = new AtomicInteger();

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            try {
                java.io.InputStream stdin = commandInvocation.getStdin();
                if (stdin != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stdin));
                    while (reader.readLine() != null)
                        lines.incrementAndGet();
                }
            } catch (Exception e) {
                return CommandResult.FAILURE;
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "blocker", description = "upstream that blocks until interrupted")
    public static class BlockerCommand implements Command<CommandInvocation> {
        static final AtomicInteger runs = new AtomicInteger();

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws InterruptedException {
            runs.incrementAndGet();
            new CountDownLatch(1).await(30, TimeUnit.SECONDS);
            return CommandResult.SUCCESS;
        }
    }

    private static CommandRuntime<CommandInvocation> buildRuntime() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(FailPipeCommand.class)
                .command(SinkCommand.class)
                .command(OkPipeCommand.class)
                .command(SinkDrainCommand.class)
                .command(TrueCommand.class)
                .create();
        return AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(registry)
                .operators(EnumSet.allOf(OperatorType.class))
                .build();
    }

    @Test
    public void testRuntimePreservesUpstreamFailure() throws Exception {
        CommandRuntime<CommandInvocation> runtime = buildRuntime();

        CommandResult result = runtime.executeCommand("failpipe | sink");

        assertEquals(CommandResult.SUCCESS, result);
        PipelineResult pipeline = ((AeshCommandRuntime<CommandInvocation>) runtime)
                .lastPipelineResult();
        assertNotNull(pipeline);
        assertEquals(CommandResult.SUCCESS, pipeline.pipelineResult());
        assertEquals(2, pipeline.stages().size());

        StageOutcome upstream = pipeline.stages().get(0);
        assertEquals(0, upstream.stageIndex());
        assertEquals(2, upstream.stageCount());
        assertEquals("FailPipeCommand", upstream.commandName());
        assertEquals(CommandResult.FAILURE, upstream.result());
        assertNotNull(upstream.error());
        assertTrue(upstream.error() instanceof CommandException);
        assertTrue(upstream.durationMs() >= 0);

        StageOutcome terminal = pipeline.stages().get(1);
        assertEquals(CommandResult.SUCCESS, terminal.result());
        assertNull(terminal.error());
    }

    @Test
    public void testRuntimeSuccessPipeline() throws Exception {
        CommandRuntime<CommandInvocation> runtime = buildRuntime();

        CommandResult result = runtime.executeCommand("okpipe | sink");

        assertEquals(CommandResult.SUCCESS, result);
        PipelineResult pipeline = ((AeshCommandRuntime<CommandInvocation>) runtime)
                .lastPipelineResult();
        assertNotNull(pipeline);
        assertEquals(2, pipeline.stages().size());
        assertEquals(CommandResult.SUCCESS, pipeline.stages().get(0).result());
        assertNull(pipeline.stages().get(0).error());
        assertEquals(CommandResult.SUCCESS, pipeline.stages().get(1).result());
    }

    @Test
    public void testFailingUpstreamStillDeliversEof() throws Exception {
        SinkDrainCommand.lines.clear();
        CommandRuntime<CommandInvocation> runtime = buildRuntime();

        CommandResult result = runtime.executeCommand("failpipe | sinkdrain");

        assertEquals(CommandResult.SUCCESS, result);
        assertTrue(SinkDrainCommand.lines.contains("partial"));
        PipelineResult pipeline = ((AeshCommandRuntime<CommandInvocation>) runtime)
                .lastPipelineResult();
        assertNotNull(pipeline);
        assertEquals(CommandResult.FAILURE, pipeline.stages().get(0).result());
        assertTrue(pipeline.stages().get(0).error() instanceof CommandException);
        assertEquals(CommandResult.SUCCESS, pipeline.stages().get(1).result());
    }

    @Test
    public void testSilentUpstreamStillDeliversEof() throws Exception {
        SinkDrainCommand.lines.clear();
        CommandRuntime<CommandInvocation> runtime = buildRuntime();

        CommandResult result = runtime.executeCommand("truecmd | sinkdrain");

        assertEquals(CommandResult.SUCCESS, result);
        assertTrue(SinkDrainCommand.lines.isEmpty());
        PipelineResult pipeline = ((AeshCommandRuntime<CommandInvocation>) runtime)
                .lastPipelineResult();
        assertNotNull(pipeline);
        assertEquals(CommandResult.SUCCESS, pipeline.stages().get(0).result());
    }

    @Test
    public void testPipeBrokenConstant() {
        assertEquals(141, CommandResult.PIPE_BROKEN.getResultValue());
        assertEquals(141, CommandResult.PIPE_BROKEN.getExitCode());
        assertTrue(CommandResult.PIPE_BROKEN.isFailure());
        assertSame(CommandResult.PIPE_BROKEN, CommandResult.valueOf(141));
        assertTrue(PipeOperator.isPipeBroken(new IOException("Pipe closed")));
        assertTrue(PipeOperator.isPipeBroken(
                new RuntimeException(new IOException("Pipe closed"))));
        assertFalse(PipeOperator.isPipeBroken(new IOException("boom")));
        assertFalse(PipeOperator.isPipeBroken(null));
    }

    @Test
    public void testTinyQueueCapacityStillStreams() throws Exception {
        LineCounterCommand.lines.set(0);
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(ChatterCommand.class)
                .command(LineCounterCommand.class)
                .create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(registry)
                .operators(EnumSet.allOf(OperatorType.class))
                .pipelineConfig(new PipelineConfig(1, 512, 2000, true))
                .build();

        CommandResult result = runtime.executeCommand("chatter | counter");

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals(50, LineCounterCommand.lines.get());
    }

    @Test
    public void testJoinTimeoutBoundsBlockedUpstream() throws Exception {
        BlockerCommand.runs.set(0);
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(BlockerCommand.class)
                .command(OkPipeCommand.class)
                .create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(registry)
                .operators(EnumSet.allOf(OperatorType.class))
                .pipelineConfig(new PipelineConfig(16, 8192, 200, true))
                .build();

        long start = System.currentTimeMillis();
        CommandResult result = runtime.executeCommand("blocker | okpipe");
        long elapsed = System.currentTimeMillis() - start;

        assertEquals(CommandResult.SUCCESS, result);
        assertTrue("Join must time out, took: " + elapsed, elapsed < 10000);
        assertEquals(1, BlockerCommand.runs.get());
        PipelineResult pipeline = ((AeshCommandRuntime<CommandInvocation>) runtime)
                .lastPipelineResult();
        assertNotNull(pipeline);
        assertEquals(CommandResult.FAILURE, pipeline.stages().get(0).result());
        assertNotNull(pipeline.stages().get(0).error());
    }

    @Test
    public void testPipelineConfigValidation() {
        try {
            new PipelineConfig(0, 8192, 2000, true);
            fail("Zero queue capacity must be rejected");
        } catch (IllegalArgumentException expected) {
        }
        try {
            new PipelineConfig(16, 0, 2000, true);
            fail("Zero chunk size must be rejected");
        } catch (IllegalArgumentException expected) {
        }
        assertEquals(16, PipelineConfig.DEFAULT.queueCapacityChunks());
        assertEquals(8192, PipelineConfig.DEFAULT.chunkSizeBytes());
        assertEquals(2000, PipelineConfig.DEFAULT.upstreamJoinTimeoutMs());
        assertTrue(PipelineConfig.DEFAULT.interruptUpstream());
    }

    @Test
    public void testBuilderAndSettingsPlumbing() throws Exception {
        PipelineConfig config = new PipelineConfig(4, 1024, 500, false);
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(OkPipeCommand.class)
                .command(SinkCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder
                .<CommandInvocation> builder()
                .commandRegistry(registry)
                .pipelineConfig(config)
                .build();
        assertSame(config, settings.pipelineConfig());

        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(registry)
                .pipelineConfig(config)
                .build();
        assertSame(config, ((AeshCommandRuntime<CommandInvocation>) runtime).pipelineConfig());
    }

    @Test
    public void testInteractiveStageEvents() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch terminalLatch = new CountDownLatch(1);
        CountDownLatch pipelineLatch = new CountDownLatch(1);
        List<StageOutcome> stageEvents = new ArrayList<>();
        List<PipelineResult> pipelineEvents = new ArrayList<>();
        AtomicInteger terminalCalls = new AtomicInteger();

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(FailPipeCommand.class)
                .command(SinkCommand.class)
                .create();

        File historyFile = File.createTempFile("aesh-pipeline-outcome-history", ".txt");
        historyFile.deleteOnExit();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .enableOperatorParser(true)
                .commandRegistry(registry)
                .historyFile(historyFile)
                .commandExecutionListener(new PipelineExecutionListener() {
                    @Override
                    public void onCommandComplete(String commandLine, CommandResult result, long durationMs) {
                        terminalCalls.incrementAndGet();
                        terminalLatch.countDown();
                    }

                    @Override
                    public void onStageComplete(StageOutcome stage) {
                        stageEvents.add(stage);
                    }

                    @Override
                    public void onPipelineComplete(PipelineResult result) {
                        pipelineEvents.add(result);
                        pipelineLatch.countDown();
                    }
                })
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("failpipe | sink" + Config.getLineSeparator());
        assertTrue("Terminal command should complete", terminalLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Pipeline event should fire", pipelineLatch.await(5, TimeUnit.SECONDS));

        assertEquals(1, terminalCalls.get());
        assertEquals(2, stageEvents.size());
        assertEquals(CommandResult.FAILURE, stageEvents.get(0).result());
        assertNotNull(stageEvents.get(0).error());
        assertEquals(CommandResult.SUCCESS, stageEvents.get(1).result());
        assertEquals(1, pipelineEvents.size());
        assertEquals(CommandResult.SUCCESS, pipelineEvents.get(0).pipelineResult());
        assertEquals(2, pipelineEvents.get(0).stages().size());

        console.stop();
    }
}
