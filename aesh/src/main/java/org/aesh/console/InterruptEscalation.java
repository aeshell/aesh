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

/**
 * Tracks repeated interrupts for a command that ignored the first one.
 * The first repeat arms a grace watcher; the second repeat — or grace
 * expiry — means the job must be abandoned. Thread-safe: concurrent
 * signal deliveries are serialized and idempotent.
 */
final class InterruptEscalation {

    static final long GRACE_MS = 2000;

    private int repeats;
    private boolean watcherStarted;

    /**
     * Records a repeated interrupt.
     *
     * @param graceExpired run when the grace period expires with the job
     *        still unresponsive; invoked on a daemon watcher thread
     * @return true when the job must be abandoned immediately
     */
    synchronized boolean noteRepeat(Runnable graceExpired) {
        repeats++;
        if (repeats == 1 && !watcherStarted) {
            watcherStarted = true;
            Thread watcher = new Thread(() -> {
                try {
                    Thread.sleep(GRACE_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                graceExpired.run();
            }, "aesh-escalation-watch");
            watcher.setDaemon(true);
            watcher.start();
            return false;
        }
        return repeats >= 2;
    }
}
