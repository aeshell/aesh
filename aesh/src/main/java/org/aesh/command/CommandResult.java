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
 * The result of a command execution, aligned with POSIX exit code conventions.
 * <p>
 * POSIX defines exit codes as 0-255:
 * <ul>
 * <li>0 — success</li>
 * <li>1 — general error</li>
 * <li>2 — misuse of shell command (e.g., invalid arguments)</li>
 * <li>127 — command not found</li>
 * <li>128+N — killed by signal N (e.g., 130 = SIGINT/Ctrl-C)</li>
 * </ul>
 *
 * @author Aesh team
 */
public class CommandResult {

    /** Successful execution (exit code 0). */
    public static final CommandResult SUCCESS = new CommandResult(0);

    /** General failure (exit code 1). */
    public static final CommandResult FAILURE = new CommandResult(1);

    /** Invalid arguments or usage error (exit code 2). Equivalent to Bash EX_USAGE. */
    public static final CommandResult USAGE_ERROR = new CommandResult(2);

    /** Command not found (exit code 127). */
    public static final CommandResult COMMAND_NOT_FOUND = new CommandResult(127);

    /** Interrupted by signal, e.g., Ctrl-C / SIGINT (exit code 130 = 128 + 2). */
    public static final CommandResult INTERRUPTED = new CommandResult(130);

    private final int result;

    /**
     * Converts the given result integer into a {@link CommandResult}.
     *
     * @param result the exit code
     * @return a shared constant for well-known codes, or a new instance for custom codes
     */
    public static CommandResult valueOf(final int result) {
        switch (result) {
            case 0:
                return SUCCESS;
            case 1:
                return FAILURE;
            case 2:
                return USAGE_ERROR;
            case 127:
                return COMMAND_NOT_FOUND;
            case 130:
                return INTERRUPTED;
            default:
                return new CommandResult(result);
        }
    }

    private CommandResult(int result) {
        this.result = result;
    }

    /**
     * Returns the raw result value.
     *
     * @return the exit code
     */
    public int getResultValue() {
        return result;
    }

    /**
     * Returns the exit code clamped to the POSIX range 0-255, suitable for
     * use with {@code System.exit()}.
     *
     * @return the exit code in the range 0-255
     * @since 3.16
     */
    public int getExitCode() {
        if (result < 0)
            return 1;
        if (result > 255)
            return 255;
        return result;
    }

    public boolean isSuccess() {
        return getResultValue() == 0;
    }

    public boolean isFailure() {
        return getResultValue() != 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.result;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CommandResult other = (CommandResult) obj;
        if (result != other.result)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CommandResult{" + result + "}";
    }
}
