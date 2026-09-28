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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.PipelineConfig;
import org.aesh.command.impl.operator.OutputDelegate;
import org.aesh.command.impl.operator.PipeBrokenException;
import org.aesh.command.impl.operator.PipeOperator;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.AeshContext;
import org.aesh.console.ReadlineConsole;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Every stdin getter for a pipeline input must return the same usable
 * stream: each {@code getData()} call used to manufacture a fresh wrapper
 * over the shared queue, so the first wrapper stranded buffered bytes while
 * the second consumed the EOF sentinel (#641).
 */
public class PipeInputStabilityTest {

    private static AeshContext context() {
        return SettingsBuilder.builder().build().aeshContext();
    }

    @Test
    public void testRepeatedGettersReturnStableStream() throws Exception {
        PipeOperator pipe = new PipeOperator(context(), new PipelineConfig(16, 512, 2000, true));
        pipe.getConfiguration().getOutputRedirection().write("abc");
        pipe.getConfiguration().getOutputRedirection().close();

        BufferedInputStream first = pipe.getData();
        assertEquals('a', first.read());
        BufferedInputStream second = pipe.getData();
        assertSame("repeated getters must return the same stream", first, second);
        assertEquals('b', second.read());
        assertEquals('c', second.read());
        assertEquals(-1, second.read());
        assertEquals("EOF must stay EOF across getter calls", -1, pipe.getData().read());
    }

    @Test
    public void testRepeatedErrorGettersReturnStableStream() throws Exception {
        PipeOperator pipe = new PipeOperator(context());
        assertNull(pipe.getErrorData());

        pipe.getErrorDelegate().write("boom");
        pipe.getErrorDelegate().close();

        BufferedInputStream first = pipe.getErrorData();
        assertNotNull(first);
        assertEquals('b', first.read());
        assertSame("repeated error getters must return the same stream", first, pipe.getErrorData());
        assertEquals('o', pipe.getErrorData().read());
        assertEquals('o', pipe.getErrorData().read());
        assertEquals('m', pipe.getErrorData().read());
        assertEquals(-1, pipe.getErrorData().read());
        assertEquals("EOF must stay EOF across getter calls", -1, pipe.getErrorData().read());
    }

    @CommandDefinition(name = "abpipe", description = "upstream writing two bytes")
    public static class AbPipeCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            commandInvocation.println("ab");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "twobytes", description = "downstream reading one byte per getter")
    public static class TwoBytesCommand implements Command<CommandInvocation> {
        static volatile int first = -2;
        static volatile int second = -2;
        static volatile boolean sameStream;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            try {
                java.io.InputStream firstGetter = commandInvocation.getStdin();
                first = firstGetter == null ? -1 : firstGetter.read();
                java.io.InputStream secondGetter = commandInvocation.getStdin();
                sameStream = firstGetter == secondGetter;
                second = secondGetter == null ? -1 : secondGetter.read();
            } catch (Exception e) {
                return CommandResult.FAILURE;
            }
            return CommandResult.SUCCESS;
        }
    }

    private static CommandRuntime<CommandInvocation> buildRuntime() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(AbPipeCommand.class)
                .command(TwoBytesCommand.class)
                .create();
        return AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(registry)
                .operators(EnumSet.allOf(OperatorType.class))
                .build();
    }

    @Test
    public void testRuntimeRepeatedStdinReadsPreserveData() throws Exception {
        TwoBytesCommand.first = -2;
        TwoBytesCommand.second = -2;
        TwoBytesCommand.sameStream = false;

        CommandResult result = buildRuntime().executeCommand("abpipe | twobytes");

        assertEquals(CommandResult.SUCCESS, result);
        assertTrue("repeated getStdin() must return the same stream", TwoBytesCommand.sameStream);
        assertEquals('a', TwoBytesCommand.first);
        assertEquals('b', TwoBytesCommand.second);
    }

    @Test
    public void testConsoleRepeatedStdinReadsPreserveData() throws Exception {
        TwoBytesCommand.first = -2;
        TwoBytesCommand.second = -2;
        TwoBytesCommand.sameStream = false;

        TestConnection connection = new TestConnection();
        CountDownLatch completed = new CountDownLatch(1);
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(AbPipeCommand.class)
                .command(TwoBytesCommand.class)
                .create();
        File historyFile = File.createTempFile("aesh-pipe-stdin-history", ".txt");
        historyFile.deleteOnExit();
        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .enableOperatorParser(true)
                .commandRegistry(registry)
                .historyFile(historyFile)
                .commandExecutionListener((line, execResult, durationMs) -> completed.countDown())
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();
        try {
            connection.read("abpipe | twobytes" + Config.getLineSeparator());
            assertTrue("pipeline should complete", completed.await(10, TimeUnit.SECONDS));
            assertTrue("repeated getStdin() must return the same stream", TwoBytesCommand.sameStream);
            assertEquals('a', TwoBytesCommand.first);
            assertEquals('b', TwoBytesCommand.second);
        } finally {
            console.stop();
        }
    }

    @Test
    public void testUncachedCloseReleasesBlockedProducerWithoutCounting() throws Exception {
        PipeOperator pipe = new PipeOperator(context(), new PipelineConfig(1, 512, 2000, true));
        // Cleanup-equivalent close without ever obtaining stdin: must signal
        // the consumer is gone (releasing blocked producers) without
        // manufacturing a reader that discards and counts queued data.
        CommandInvocationConfiguration config = new CommandInvocationConfiguration(context(), pipe);

        AtomicReference<Throwable> producerError = new AtomicReference<>();
        CountDownLatch producerDone = new CountDownLatch(1);
        Thread producer = new Thread(() -> {
            try {
                OutputDelegate out = pipe.getConfiguration().getOutputRedirection();
                out.write("x");
                out.write("y");
            } catch (Throwable e) {
                producerError.set(e);
            } finally {
                producerDone.countDown();
            }
        });
        producer.setDaemon(true);
        producer.start();
        Thread.sleep(500);

        config.closePipedData();

        assertTrue("blocked producer must be released",
                producerDone.await(5, TimeUnit.SECONDS));
        assertTrue("producer must observe the broken pipe, got: " + producerError.get(),
                producerError.get() instanceof PipeBrokenException);
        assertEquals("unread data must not count as truncated", 0, pipe.truncatedBytes());
    }

    @Test
    public void testCachedCloseReleasesBlockedProducer() throws Exception {
        PipeOperator pipe = new PipeOperator(context(), new PipelineConfig(1, 512, 2000, true));
        CommandInvocationConfiguration config = new CommandInvocationConfiguration(context(), pipe);
        // Obtain the stream the command would use, but read nothing from it.
        assertNotNull(config.getPipedData());

        AtomicReference<Throwable> producerError = new AtomicReference<>();
        CountDownLatch producerDone = new CountDownLatch(1);
        Thread producer = new Thread(() -> {
            try {
                OutputDelegate out = pipe.getConfiguration().getOutputRedirection();
                out.write("x");
                out.write("y");
            } catch (Throwable e) {
                producerError.set(e);
            } finally {
                producerDone.countDown();
            }
        });
        producer.setDaemon(true);
        producer.start();
        Thread.sleep(500);

        config.closePipedData();

        assertTrue("blocked producer must be released",
                producerDone.await(5, TimeUnit.SECONDS));
        assertTrue("producer must observe the broken pipe, got: " + producerError.get(),
                producerError.get() instanceof PipeBrokenException);
        assertEquals("closing the real stream drops its unread byte", 1, pipe.truncatedBytes());
    }
}
