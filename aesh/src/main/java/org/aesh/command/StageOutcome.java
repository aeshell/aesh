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
 * The recorded outcome of a single pipeline stage.
 * <p>
 * Unix pipe semantics return only the last stage's exit code, so upstream
 * failures would otherwise be invisible. Stage outcomes preserve each
 * stage's result and error for diagnostics, listeners, and tests.
 *
 * @author Aesh team
 */
public final class StageOutcome {

    private final int stageIndex;
    private final int stageCount;
    private final String commandName;
    private final Execution<?> execution;
    private final CommandResult result;
    private final Throwable error;
    private final long durationMs;

    public StageOutcome(int stageIndex, int stageCount, String commandName,
            Execution<?> execution, CommandResult result, Throwable error, long durationMs) {
        this.stageIndex = stageIndex;
        this.stageCount = stageCount;
        this.commandName = commandName;
        this.execution = execution;
        this.result = result;
        this.error = error;
        this.durationMs = durationMs;
    }

    public int stageIndex() {
        return stageIndex;
    }

    public int stageCount() {
        return stageCount;
    }

    public String commandName() {
        return commandName;
    }

    public Execution<?> execution() {
        return execution;
    }

    public CommandResult result() {
        return result;
    }

    public Throwable error() {
        return error;
    }

    public long durationMs() {
        return durationMs;
    }

    @Override
    public String toString() {
        return "StageOutcome{index=" + stageIndex + ", command=" + commandName
                + ", result=" + result + ", error=" + error + "}";
    }
}
