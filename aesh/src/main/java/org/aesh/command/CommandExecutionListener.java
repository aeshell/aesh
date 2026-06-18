/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command;

/**
 * Listener for command execution completion events.
 * <p>
 * Called after each command finishes execution, before the prompt is
 * redisplayed. For pipelines ({@code cmd1 | cmd2}), fires for each
 * command in the pipeline.
 * <p>
 * The callback fires on the command execution thread (not the readline
 * event loop). This is safe for test frameworks that use
 * {@link java.util.concurrent.CountDownLatch} or similar synchronization.
 *
 * @author Aesh team
 * @since 3.15
 */
@FunctionalInterface
public interface CommandExecutionListener {

    /**
     * Called after a command finishes execution.
     *
     * @param commandLine the full command line that was executed
     * @param result the {@link CommandResult} (SUCCESS, FAILURE, or custom)
     * @param durationMs wall-clock execution time in milliseconds
     */
    void onCommandComplete(String commandLine, CommandResult result, long durationMs);
}
