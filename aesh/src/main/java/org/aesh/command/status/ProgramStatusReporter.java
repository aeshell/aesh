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
package org.aesh.command.status;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.aesh.terminal.Connection;
import org.aesh.terminal.TerminalFeatures;
import org.aesh.terminal.utils.ProgramStatus;

/**
 * Framework-owned writer for OSC 7501 program-status reports.
 * <p>
 * Reporting is best-effort and strictly opt-in: reports go to the original
 * terminal connection supplied at creation, never to redirected command
 * output, pipes, or files. When reporting is disabled or no connection is
 * available, every method is a quiet no-op returning {@code false} — no
 * terminal queries are issued, no threads are started, and no terminal is
 * initialized to satisfy a report.
 * <p>
 * When a stable application name is configured and the report carries
 * none, it is applied; an explicitly set name is never overwritten.
 * Plain text and typed values only — callers never build escape
 * sequences. Encoding and validation belong to aesh-readline's
 * {@code ProgramStatus} model.
 *
 * @since 3.19
 */
public final class ProgramStatusReporter {

    private static final Logger LOGGER = Logger.getLogger(ProgramStatusReporter.class.getName());

    private static final ProgramStatusReporter DISABLED = new ProgramStatusReporter(false, null, null);

    private final boolean enabled;
    private final String appName;
    private final Connection connection;
    private volatile TerminalFeatures features;

    private ProgramStatusReporter(boolean enabled, String appName, Connection connection) {
        this.enabled = enabled;
        this.appName = appName;
        this.connection = connection;
    }

    /**
     * @return a reporter that never reports
     */
    public static ProgramStatusReporter disabled() {
        return DISABLED;
    }

    /**
     * Create a reporter bound to the original terminal connection.
     * Returns the shared disabled instance when reporting is not enabled
     * or no connection is available, so callers allocate nothing in those
     * paths.
     *
     * @param enabled true when reporting is opted in
     * @param appName stable application name, or null for none
     * @param connection the original terminal connection, or null
     * @return an enabled reporter, or the disabled instance
     */
    public static ProgramStatusReporter create(boolean enabled, String appName, Connection connection) {
        if (!enabled || connection == null)
            return DISABLED;
        return new ProgramStatusReporter(true, appName, connection);
    }

    /**
     * @return true when this reporter will attempt to write reports
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Write a program-status report to the bound terminal connection.
     *
     * @param status the report to write, or null
     * @return true when the report was written, false when disabled,
     *         unavailable, or the write failed
     */
    public boolean report(ProgramStatus status) {
        if (!enabled || status == null)
            return false;
        try {
            TerminalFeatures current = features;
            if (current == null) {
                current = new TerminalFeatures(connection);
                features = current;
            }
            current.writeProgramStatus(withAppName(status));
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Program-status report failed", e);
            return false;
        }
    }

    private ProgramStatus withAppName(ProgramStatus status) {
        if (appName == null || status.app() != null)
            return status;
        ProgramStatus.Builder builder = ProgramStatus.builder(status.state()).app(appName);
        if (status.kind() != null)
            builder.kind(status.kind());
        if (status.id() != null)
            builder.id(status.id());
        if (status.title() != null)
            builder.title(status.title());
        if (status.progress() != null)
            builder.progress(status.progress().intValue());
        if (status.message() != null)
            builder.message(status.message());
        return builder.build();
    }
}
