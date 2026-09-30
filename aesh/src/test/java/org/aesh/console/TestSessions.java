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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Shared teardown and synchronization helpers for asynchronous session
 * tests (piped consoles, background REPL threads).
 * <p>
 * The rule (#661): teardown problems fail loudly. Recording cleanup
 * failures instead of swallowing them keeps a wedged reader, a dead REPL
 * or a leaked worker from turning into a passing test.
 */
public final class TestSessions {

    private TestSessions() {
    }

    /**
     * Closes anything with a no-arg close, recording failures instead of
     * swallowing them. Use with method references ({@code testOut::close}).
     */
    public interface Closer {
        void close() throws Exception;
    }

    /**
     * Awaits a latch, failing with context when it never fires.
     */
    public static void await(CountDownLatch latch, long timeout, TimeUnit unit, String what)
            throws InterruptedException {
        assertTrue(what + " did not happen within " + timeout + " " + unit,
                latch.await(timeout, unit));
    }

    /**
     * Records close failures into {@code errors} for a later
     * {@link #assertQuiet(List)}.
     */
    public static void closeQuietly(Closer closer, String name, List<Throwable> errors) {
        try {
            closer.close();
        } catch (Exception e) {
            errors.add(new java.io.IOException("close " + name + " failed", e));
        }
    }

    /**
     * Joins a worker thread boundedly, recording interrupts and survivors.
     */
    public static void joinQuietly(Thread thread, String name, List<Throwable> errors) {
        joinQuietly(thread, name, errors, 10000);
    }

    /**
     * Joins a worker thread boundedly, recording interrupts and survivors.
     */
    public static void joinQuietly(Thread thread, String name, List<Throwable> errors,
            long timeoutMs) {
        try {
            thread.join(timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errors.add(e);
            return;
        }
        if (thread.isAlive())
            errors.add(new IllegalStateException(name + " thread still alive after close"));
    }

    /**
     * Fails the test when any recorded teardown problem exists.
     */
    public static void assertQuiet(List<Throwable> errors) {
        assertTrue("Session cleanup must stay quiet, errors: " + errors,
                errors.isEmpty());
    }

    /**
     * Fresh error bucket for one session teardown.
     */
    public static List<Throwable> errors() {
        return new ArrayList<>();
    }
}
