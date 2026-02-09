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
package org.aesh.command.invocation;

import org.aesh.command.CommandRuntime;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.context.CommandContext;

/**
 * @author Aesh team
 */
public interface CommandInvocationBuilder<CI extends CommandInvocation> {

    CI build(CommandRuntime<CI> runtime,
            CommandInvocationConfiguration configuration, CommandContainer<CI> commandContainer);

    /**
     * Build a command invocation with a command context for sub-command mode.
     *
     * @param runtime the command runtime
     * @param configuration the invocation configuration
     * @param commandContainer the command container
     * @param commandContext the command context (may be null)
     * @return the command invocation
     */
    default CI build(CommandRuntime<CI> runtime,
            CommandInvocationConfiguration configuration,
            CommandContainer<CI> commandContainer,
            CommandContext commandContext) {
        // Default implementation ignores context for backward compatibility
        return build(runtime, configuration, commandContainer);
    }
}
