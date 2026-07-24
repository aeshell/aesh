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

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Handler invoked when a command is not found.
 * <p>
 * The {@code output} consumer writes a line of text to the appropriate
 * destination (terminal, stderr, etc.) without requiring a {@code Shell}
 * dependency, making this interface usable in both interactive and
 * non-interactive contexts.
 * <p>
 * Override the enhanced
 * {@link #handleCommandNotFound(String, Consumer, String, Collection)}
 * method to receive the unknown command name and the list of available
 * commands for "did you mean?" suggestions.
 *
 * @author Aesh team
 */
@FunctionalInterface
public interface CommandNotFoundHandler {

    void handleCommandNotFound(String line, Consumer<String> output);

    /**
     * Enhanced callback that also receives the unknown command name and the
     * commands that were available at the level where the failure occurred.
     * <p>
     * The default implementation delegates to the simple
     * {@link #handleCommandNotFound(String, Consumer)} method, preserving
     * backward compatibility. Override this method to implement intelligent
     * suggestions (e.g., Levenshtein-based "did you mean?" hints).
     *
     * @param line the full command line that was entered
     * @param output consumer for writing output lines
     * @param unknownCommand the specific command token that was not found
     * @param availableCommands the command names that were valid at the failure point
     */
    default void handleCommandNotFound(String line, Consumer<String> output,
            String unknownCommand, Collection<String> availableCommands) {
        handleCommandNotFound(line, output);
    }
}
