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
 * Optional lifecycle interface for commands that need to reset state
 * between re-entrant parse/execution cycles.
 * <p>
 * When a command is reused across multiple invocations (e.g., in test suites
 * or long-running applications), global state set during {@code execute()}
 * persists between calls. Implementing this interface allows commands to
 * reset that state before each parse cycle.
 * <p>
 * Note: Æsh already resets option fields to their default values between
 * parse cycles. This interface is for resetting <em>external</em> state
 * that is not managed by Æsh (e.g., static flags, shared configuration).
 *
 * @author Aesh team
 */
public interface CommandLifecycle {

    /**
     * Called before each parse cycle, giving the command a chance to
     * reset any external state from a previous invocation.
     * <p>
     * This is called after Æsh clears its internal parsed option values
     * but before the new command line is parsed.
     */
    default void beforeParse() {
    }
}
