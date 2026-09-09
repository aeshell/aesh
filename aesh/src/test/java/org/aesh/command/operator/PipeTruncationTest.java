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
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.command.PipelineConfig;
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
}
