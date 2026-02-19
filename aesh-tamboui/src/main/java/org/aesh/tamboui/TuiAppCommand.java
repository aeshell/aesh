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
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyEvent;

/**
 * Abstract base class for aesh commands that use TamboUI's ToolkitRunner
 * with declarative Element-based rendering.
 * <p>
 * Subclasses implement {@link #render()} to return the UI element tree.
 * Optionally override {@link #onKeyEvent(KeyEvent, ToolkitRunner)} for
 * custom key handling and {@link #onStart(ToolkitRunner)} for initialization.
 * <p>
 * Example:
 *
 * <pre>
 * {@literal @}CommandDefinition(name = "status", description = "System status")
 * public class StatusCommand extends TuiAppCommand {
 *     {@literal @}Override
 *     protected Element render() {
 *         return panel("System Status",
 *             text("Hello from TamboUI!")
 *         ).rounded();
 *     }
 * }
 * </pre>
 *
 * @author Aesh team
 */
public abstract class TuiAppCommand implements Command<CommandInvocation> {

    /**
     * Return the UI element tree. Called each frame to produce the UI.
     *
     * @return the root Element to render
     */
    protected abstract Element render();

    /**
     * Handle key events. Return true if handled, false to pass through.
     * Default implementation quits on 'q' or Ctrl+C.
     *
     * @param event the key event
     * @param runner the ToolkitRunner
     * @return true if the event was handled
     */
    protected boolean onKeyEvent(KeyEvent event, ToolkitRunner runner) {
        if (event.isQuit()) {
            runner.quit();
            return true;
        }
        return false;
    }

    /**
     * Called after the TUI starts. Override to run initialization logic
     * such as scheduling background tasks.
     *
     * @param runner the ToolkitRunner
     */
    protected void onStart(ToolkitRunner runner) {
    }

    /**
     * Override to customize TUI configuration (tick rate, mouse capture, etc.).
     * The backend is already set.
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
            try (ToolkitRunner runner = ToolkitRunner.create(config)) {
                runner.eventRouter().addGlobalHandler(event -> {
                    if (event instanceof KeyEvent) {
                        return onKeyEvent((KeyEvent) event, runner)
                                ? EventResult.HANDLED
                                : EventResult.UNHANDLED;
                    }
                    return EventResult.UNHANDLED;
                });
                onStart(runner);
                runner.run(this::render);
            }
            return CommandResult.SUCCESS;
        } catch (Exception e) {
            throw new CommandException("TUI error: " + e.getMessage(), e);
        }
    }
}
