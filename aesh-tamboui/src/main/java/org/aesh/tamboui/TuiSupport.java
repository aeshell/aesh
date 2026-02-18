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

import java.io.IOException;

import org.aesh.command.shell.Shell;
import org.aesh.terminal.Connection;

import dev.tamboui.backend.aesh.AeshBackend;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;

/**
 * Static utility class providing factory methods for integrating TamboUI
 * with aesh commands via the Shell's Connection.
 *
 * @author Aesh team
 */
public final class TuiSupport {

    private TuiSupport() {
    }

    /**
     * Create an AeshBackend from a Shell's Connection.
     *
     * @param shell the aesh Shell
     * @return a new AeshBackend wrapping the shell's connection
     * @throws IllegalStateException if the shell has no connection
     */
    public static AeshBackend createBackend(Shell shell) throws IOException {
        Connection conn = shell.connection();
        if (conn == null) {
            throw new IllegalStateException("Shell does not have a terminal connection");
        }
        return new AeshBackend(new NonClosingConnection(conn));
    }

    /**
     * Create a TuiConfig.Builder pre-configured with the Shell's Connection as backend.
     *
     * @param shell the aesh Shell
     * @return a TuiConfig.Builder with backend already set
     * @throws IllegalStateException if the shell has no connection
     */
    public static TuiConfig.Builder configBuilder(Shell shell) throws IOException {
        return TuiConfig.builder()
                .backend(createBackend(shell))
                .shutdownHook(false);
    }

    /**
     * Create a TuiRunner wired to the Shell's terminal using default configuration.
     *
     * @param shell the aesh Shell
     * @return a new TuiRunner ready to use
     * @throws Exception if runner creation fails
     * @throws IllegalStateException if the shell has no connection
     */
    public static TuiRunner createRunner(Shell shell) throws Exception {
        return TuiRunner.create(configBuilder(shell).build());
    }

    /**
     * Create a TuiRunner with custom config, wired to the Shell's terminal.
     * The backend on the provided builder will be overridden with the Shell's connection.
     *
     * @param shell the aesh Shell
     * @param configBuilder a pre-configured TuiConfig.Builder (backend will be set)
     * @return a new TuiRunner ready to use
     * @throws Exception if runner creation fails
     * @throws IllegalStateException if the shell has no connection
     */
    public static TuiRunner createRunner(Shell shell, TuiConfig.Builder configBuilder) throws Exception {
        configBuilder.backend(createBackend(shell));
        return TuiRunner.create(configBuilder.build());
    }

    /**
     * Create a ToolkitRunner wired to the Shell's terminal using default configuration.
     *
     * @param shell the aesh Shell
     * @return a new ToolkitRunner ready to use
     * @throws Exception if runner creation fails
     * @throws IllegalStateException if the shell has no connection
     */
    public static ToolkitRunner createToolkitRunner(Shell shell) throws Exception {
        return ToolkitRunner.create(configBuilder(shell).build());
    }
}
