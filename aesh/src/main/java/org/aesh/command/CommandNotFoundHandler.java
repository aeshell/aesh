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

import java.util.function.Consumer;

/**
 * Handler invoked when a command is not found.
 * <p>
 * The {@code output} consumer writes a line of text to the appropriate
 * destination (terminal, stderr, etc.) without requiring a {@code Shell}
 * dependency, making this interface usable in both interactive and
 * non-interactive contexts.
 *
 * @author Aesh team
 */
@FunctionalInterface
public interface CommandNotFoundHandler {

    void handleCommandNotFound(String line, Consumer<String> output);
}
