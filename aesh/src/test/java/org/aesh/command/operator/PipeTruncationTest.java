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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.command.PipelineConfig;
import org.aesh.command.impl.operator.OutputDelegate;
import org.aesh.command.impl.operator.PipeBrokenException;
import org.aesh.command.impl.operator.PipeOperator;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.AeshContext;
import org.junit.Test;

public class PipeTruncationTest {

    private static AeshContext context() {
        return SettingsBuilder.builder().build().aeshContext();
    }

    @Test
    public void testFreshPipeReportsZero() {
        PipeOperator pipe = new PipeOperator(context());
        assertEquals(0, pipe.truncatedBytes());
    }

    @Test
    public void testConsumerCloseCountsUnreadBytes() throws Exception {
        PipeOperator pipe = new PipeOperator(context(), new PipelineConfig(1, 8192, 2000, true));

        AtomicReference<Throwable> producerError = new AtomicReference<>();
        CountDownLatch producerDone = new CountDownLatch(1);
        Thread producer = new Thread(() -> {
            try {
                // 16 chars bypass the writer buffer in one chunk; the second
                // chunk blocks in the offer loop until the consumer closes
                pipe.getConfiguration().getOutputRedirection().write("0123456789ABCDEF");
                pipe.getConfiguration().getOutputRedirection().write("0123456789ABCDEF");
            } catch (Throwable e) {
                producerError.set(e);
            } finally {
                producerDone.countDown();
            }
        });
        producer.setDaemon(true);
        producer.start();

        // Load-bearing: the producer must be blocked in its second write
        // (not merely started) before the consumer closes. No hook exists
        // for offer-loop blockage, so this parks deterministically short.
        Thread.sleep(500);
        BufferedInputStream input = pipe.getData();
        input.close();

        assertTrue("Producer must finish after consumer close",
                producerDone.await(5, TimeUnit.SECONDS));
        assertTrue("Producer must observe the broken pipe, got: " + producerError.get(),
                producerError.get() instanceof PipeBrokenException);
        assertEquals(16, pipe.truncatedBytes());
    }

    @Test
    public void testDrainedPipeCountsNothing() throws Exception {
        PipeOperator pipe = new PipeOperator(context(), new PipelineConfig(16, 8192, 2000, true));

        pipe.getConfiguration().getOutputRedirection().write("hello");
        pipe.getConfiguration().getOutputRedirection().close();

        BufferedInputStream input = pipe.getData();
        byte[] buf = new byte[64];
        while (input.read(buf) != -1) {
        }
        input.close();

        assertEquals(0, pipe.truncatedBytes());
    }

    @Test
    public void testFullQueueClosePreservesData() throws Exception {
        testCloseWithFullQueuePreservesData(new PipelineConfig(1, 512, 2000, true),
                new String[] { "hello\n" });
    }

    @Test
    public void testFullQueueClosePreservesDataDefaultCapacity() throws Exception {
        PipelineConfig config = PipelineConfig.DEFAULT;
        String[] pieces = new String[config.queueCapacityChunks()];
        for (int i = 0; i < pieces.length; i++)
            pieces[i] = "c" + i + ";";
        testCloseWithFullQueuePreservesData(config, pieces);
    }

    /**
     * Writes exactly enough chunks to fill the queue — no write ever blocks
     * — then closes while it is full. The consumer starts draining only
     * after the producer had time to reach (and, pre-fix, blow through) the
     * close, so close-before-read ordering must not matter (#640).
     */
    private static void testCloseWithFullQueuePreservesData(PipelineConfig config, String[] pieces)
            throws Exception {
        if (pieces.length != config.queueCapacityChunks())
            throw new IllegalArgumentException("pieces must exactly fill the queue");
        StringBuilder expected = new StringBuilder();
        for (String piece : pieces)
            expected.append(piece);
        PipeOperator pipe = new PipeOperator(context(), config);
        OutputDelegate out = pipe.getConfiguration().getOutputRedirection();
        AtomicReference<Throwable> producerError = new AtomicReference<>();
        Thread producer = new Thread(() -> {
            try {
                // One chunk per write: each write() flushes separately and
                // every piece fits the chunk-size buffer.
                for (String piece : pieces)
                    out.write(piece);
                out.close();
            } catch (Throwable e) {
                producerError.set(e);
            }
        });
        producer.setDaemon(true);
        producer.start();

        // Let the producer fill the queue and reach close. Pre-fix close
        // never waits, so it is long gone; post-fix it parks for room.
        // Either way the consumer below must see every accepted byte.
        Thread.sleep(500);
        BufferedInputStream input = pipe.getData();
        ByteArrayOutputStream received = new ByteArrayOutputStream();
        byte[] buf = new byte[64];
        int n;
        while ((n = input.read(buf)) != -1)
            received.write(buf, 0, n);
        input.close();

        producer.join(5000);
        assertFalse("producer close must terminate", producer.isAlive());
        assertNull("producer close must stay quiet, got: " + producerError.get(), producerError.get());
        assertEquals(expected.toString(), received.toString("UTF-8"));
        assertEquals("normal EOF must not count truncation", 0, pipe.truncatedBytes());
    }

    @Test
    public void testInterruptedCloseStillDeliversEof() throws Exception {
        PipeOperator pipe = new PipeOperator(context(), new PipelineConfig(1, 512, 2000, true));
        OutputDelegate out = pipe.getConfiguration().getOutputRedirection();
        out.write("hello\n");

        AtomicReference<Throwable> closeError = new AtomicReference<>();
        Thread closer = new Thread(() -> {
            try {
                out.close();
            } catch (Throwable e) {
                closeError.set(e);
            }
        });
        closer.setDaemon(true);
        closer.start();
        // Load-bearing: no consumer ever reads, so the close parks waiting
        // for room; the alive-assert below proves waiting, not racing.
        Thread.sleep(500);
        assertTrue("close must wait for room instead of dropping data", closer.isAlive());
        closer.interrupt();
        closer.join(5000);

        assertFalse("interrupted close must terminate", closer.isAlive());
        // The delegate treats "Pipe closed" as routine, so the cancelled
        // close stays quiet — the contract that matters is liveness below.
        assertNull("cancelled close stays quiet, got: " + closeError.get(), closeError.get());
        // The cancel path forces EOF so a downstream take() can never hang
        // on a missing sentinel.
        BufferedInputStream input = pipe.getData();
        assertEquals(-1, input.read());
        input.close();
    }
}
