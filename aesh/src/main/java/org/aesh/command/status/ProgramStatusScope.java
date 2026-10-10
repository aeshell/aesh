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

import java.util.concurrent.atomic.AtomicBoolean;

import org.aesh.terminal.utils.ProgramStatus;

/**
 * Execution-scoped program-status reporting context.
 * <p>
 * The current scope is installed on the executing thread by the execution
 * driver ({@code CommandJob} on the console path,
 * {@code AeshCommandRuntime} on the runtime path) and baked into each
 * built command invocation — invocations multiply per execution, so
 * resolving the scope at report time would bind a stashed invocation to
 * whatever execution happens to run later on the same thread.
 * <p>
 * The scope closes when its execution finishes. Reports (and clears) after
 * that fail closed, so finished or abandoned workers can never overwrite
 * newer work — including when a leaked thread outlives its execution.
 * Clearing the thread binding when the driver returns is still required:
 * it keeps invocations built outside any execution unscoped.
 *
 * @since 3.18.3
 */
public final class ProgramStatusScope {

    private static final ThreadLocal<ProgramStatusScope> CURRENT = new ThreadLocal<>();

    private final ProgramStatusReporter reporter;
    private final AtomicBoolean open = new AtomicBoolean(true);

    /**
     * Create a scope writing through the given reporter.
     *
     * @param reporter the connection-bound reporter, never null
     */
    public ProgramStatusScope(ProgramStatusReporter reporter) {
        if (reporter == null)
            throw new NullPointerException("reporter");
        this.reporter = reporter;
    }

    /**
     * @return the scope installed on the current thread, or null
     */
    public static ProgramStatusScope current() {
        return CURRENT.get();
    }

    /**
     * Install a scope on the current thread. Framework use only: installed
     * by the execution driver around the run, always cleared afterwards.
     *
     * @param scope the scope to install, or null to install nothing
     */
    public static void install(ProgramStatusScope scope) {
        if (scope == null)
            CURRENT.remove();
        else
            CURRENT.set(scope);
    }

    /**
     * @return true while the owning execution is still running
     */
    public boolean isOpen() {
        return open.get();
    }

    /**
     * Close the scope: all further reports and clears fail closed.
     * Idempotent.
     */
    public void close() {
        open.set(false);
    }

    /**
     * @param status the report to write, or null
     * @return true when written, false when closed, unavailable, or failed
     */
    public boolean report(ProgramStatus status) {
        if (!open.get() || status == null)
            return false;
        return reporter.report(status);
    }

    /**
     * @param taskId child task id to clear, or null/empty for no-op
     * @return true when written, false when closed, unavailable, or failed
     */
    public boolean clear(String taskId) {
        if (!open.get())
            return false;
        return reporter.clear(taskId);
    }
}
