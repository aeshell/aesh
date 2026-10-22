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

import org.aesh.console.AeshContext;

/**
 * Optional lifecycle interface for commands that need hooks around
 * the parse/execution cycle.
 * <p>
 * When a command is reused across multiple invocations (e.g., in test suites
 * or long-running applications), this interface provides hooks for:
 * <ul>
 * <li>{@code setContext(AeshContext)} — receiving the active console context</li>
 * <li>{@code beforeParse()} — resetting external state before parsing</li>
 * <li>{@code afterParse()} — post-processing after field population, before execution</li>
 * </ul>
 * <p>
 * Note: Æsh already resets option fields to their default values between
 * parse cycles. {@code beforeParse()} is for resetting <em>external</em> state
 * that is not managed by Æsh (e.g., static flags, shared configuration).
 *
 * @author Aesh team
 */
public interface CommandLifecycle {

    /**
     * Called with the active {@link AeshContext} immediately before each
     * lifecycle hook ({@code beforeParse()} and {@code afterParse()}).
     * <p>
     * Commands that need access to console facilities (e.g., a connection,
     * environment lookups) should store the context here, since no other
     * reference is passed into the hooks. The value may be null when Æsh
     * runs without a console (e.g., pure command-line mode).
     */
    default void setContext(AeshContext context) {
    }

    /**
     * Called before each parse cycle, giving the command a chance to
     * reset any external state from a previous invocation.
     * <p>
     * This is called after Æsh clears its internal parsed option values
     * but before the new command line is parsed.
     */
    default void beforeParse() {
    }

    /**
     * Called after the command has been parsed and option fields populated,
     * but before {@code execute()} runs.
     * <p>
     * Use this for post-processing that depends on parsed option values,
     * such as splitting argument lists, resolving derived state, or
     * validating cross-option constraints.
     */
    default void afterParse() {
    }
}
