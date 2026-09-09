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
package org.aesh.command;

/**
 * Resource and timing configuration for pipeline ({@code |}) execution.
 * <p>
 * All values are validated: queue capacity and chunk size must be at least 1.
 * A join timeout of zero or less waits indefinitely.
 *
 * @author Aesh team
 */
public final class PipelineConfig {

    public static final PipelineConfig DEFAULT = new PipelineConfig(16, 8192, 2000, true);

    private final int queueCapacityChunks;
    private final int chunkSizeBytes;
    private final long upstreamJoinTimeoutMs;
    private final boolean interruptUpstream;

    public PipelineConfig(int queueCapacityChunks, int chunkSizeBytes,
            long upstreamJoinTimeoutMs, boolean interruptUpstream) {
        if (queueCapacityChunks < 1)
            throw new IllegalArgumentException("queueCapacityChunks must be at least 1");
        if (chunkSizeBytes < 1)
            throw new IllegalArgumentException("chunkSizeBytes must be at least 1");
        this.queueCapacityChunks = queueCapacityChunks;
        this.chunkSizeBytes = chunkSizeBytes;
        this.upstreamJoinTimeoutMs = upstreamJoinTimeoutMs;
        this.interruptUpstream = interruptUpstream;
    }

    public int queueCapacityChunks() {
        return queueCapacityChunks;
    }

    public int chunkSizeBytes() {
        return chunkSizeBytes;
    }

    public long upstreamJoinTimeoutMs() {
        return upstreamJoinTimeoutMs;
    }

    public boolean interruptUpstream() {
        return interruptUpstream;
    }
}
