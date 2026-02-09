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
package org.aesh.command.impl.invocation;

import org.aesh.command.CommandRuntime;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.context.CommandContext;
import org.aesh.command.invocation.CommandInvocationBuilder;
import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.aesh.command.shell.Shell;
import org.aesh.console.Console;
import org.aesh.console.ReadlineConsole;

/**
 * @author Aesh team
 */
public class AeshCommandInvocationBuilder implements CommandInvocationBuilder<AeshCommandInvocation> {

    private final Shell shell;
    private final Console console;

    public AeshCommandInvocationBuilder(Shell shell, Console console) {
        this.shell = shell;
        this.console = console;
    }

    @Override
    public AeshCommandInvocation build(CommandRuntime<AeshCommandInvocation> runtime,
            CommandInvocationConfiguration config,
            CommandContainer<AeshCommandInvocation> commandContainer) {
        // Get CommandContext from ReadlineConsole if available
        CommandContext ctx = null;
        if (console instanceof ReadlineConsole) {
            ctx = ((ReadlineConsole) console).getCommandContext();
        }
        if (ctx != null && ctx.isInSubCommandMode()) {
            return new AeshCommandInvocation(console, shell, runtime, config, commandContainer, ctx);
        }
        return new AeshCommandInvocation(console, shell, runtime, config, commandContainer);
    }

    @Override
    public AeshCommandInvocation build(CommandRuntime<AeshCommandInvocation> runtime,
            CommandInvocationConfiguration config,
            CommandContainer<AeshCommandInvocation> commandContainer,
            CommandContext commandContext) {
        return new AeshCommandInvocation(console, shell, runtime, config, commandContainer, commandContext);
    }

}
