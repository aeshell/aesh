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
package org.aesh.command.impl;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class PipeThreadsTest {

    @Test
    public void testFactoryCreatesDaemonThreads() throws Exception {
        AtomicReference<Thread> seen = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Thread thread = PipeThreads.daemonFactory("test-pipe-").newThread(() -> {
            seen.set(Thread.currentThread());
            done.countDown();
        });

        assertTrue(thread.getName().startsWith("test-pipe-"));
        assertTrue(thread.isDaemon());
        thread.start();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertTrue(seen.get().isDaemon());
    }

    @Test
    public void testSharedPoolReused() throws Exception {
        assertSame(PipeThreads.sharedPool(), PipeThreads.sharedPool());
        assertTrue(PipeThreads.sharedPool().submit(() -> 42).get(5, TimeUnit.SECONDS) == 42);
    }

    @Test
    public void testNewThreadHelper() {
        Thread thread = PipeThreads.newThread(() -> {
        }, "custom-name");
        assertTrue(thread.getName().equals("custom-name"));
        assertTrue(thread.isDaemon());
    }
}
