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
package org.aesh.tamboui;

import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.shell.Shell;
import org.aesh.terminal.Connection;

import dev.tamboui.backend.aesh.AeshBackend;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;

/**
 * Abstract base class for aesh commands that use TamboUI's TuiRunner
 * for event-loop based TUI rendering.
 * <p>
 * Subclasses implement {@link #runTui(TuiRunner, CommandInvocation)} to define
 * the event handling and rendering logic. Optionally override
 * {@link #configure(TuiConfig.Builder)} to customize tick rate, mouse capture, etc.
 * <p>
 * Example:
 *
 * <pre>
 * {@literal @}CommandDefinition(name = "dashboard", description = "Show dashboard")
 * public class DashboardCommand extends TuiCommand {
 *     {@literal @}Override
 *     protected void runTui(TuiRunner runner, CommandInvocation inv) throws Exception {
 *         runner.run(
 *             (event, r) -> {
 *                 if (event instanceof KeyEvent key && key.isQuit()) {
 *                     r.quit();
 *                     return true;
 *                 }
 *                 return false;
 *             },
 *             frame -> {
 *                 // render widgets to frame
 *             }
 *         );
 *     }
 * }
 * </pre>
 *
 * @author Aesh team
 */
public abstract class TuiCommand implements Command<CommandInvocation> {

    /**
     * Implement TUI logic using the TuiRunner.
     * The runner is already configured and connected to the terminal.
     *
     * @param runner the TuiRunner to use for event loop and rendering
     * @param invocation the command invocation context
     * @throws Exception if TUI execution fails
     */
    protected abstract void runTui(TuiRunner runner, CommandInvocation invocation) throws Exception;

    /**
     * Override to customize TUI configuration (tick rate, mouse capture, etc.).
     * The backend is already set; this lets you configure other options.
     *
     * @param builder the pre-configured TuiConfig builder
     * @return the builder (for chaining)
     */
    protected TuiConfig.Builder configure(TuiConfig.Builder builder) {
        return builder;
    }

    @Override
    public final CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
        Shell shell = invocation.getShell();
        Connection conn = shell.connection();
        if (conn == null) {
            invocation.println("TUI commands require a terminal connection.");
            return CommandResult.FAILURE;
        }
        try {
            AeshBackend backend = new AeshBackend(new NonClosingConnection(conn));
            TuiConfig.Builder builder = TuiConfig.builder()
                    .backend(backend)
                    .shutdownHook(false); // aesh manages the lifecycle
            TuiConfig config = configure(builder).build();
            try (TuiRunner runner = TuiRunner.create(config)) {
                runTui(runner, invocation);
            }
            return CommandResult.SUCCESS;
        } catch (Exception e) {
            throw new CommandException("TUI error: " + e.getMessage(), e);
        }
    }
}
