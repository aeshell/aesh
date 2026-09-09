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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The aggregate outcome of a pipeline execution.
 * <p>
 * Follows Unix semantics: {@link #pipelineResult()} is the last stage's
 * result. Every stage's individual outcome is retained in {@link #stages()}
 * for diagnostics, listeners, and tests.
 *
 * @author Aesh team
 */
public final class PipelineResult {

    private final CommandResult pipelineResult;
    private final List<StageOutcome> stages;

    public PipelineResult(CommandResult pipelineResult, List<StageOutcome> stages) {
        this.pipelineResult = pipelineResult;
        this.stages = Collections.unmodifiableList(new ArrayList<>(stages));
    }

    public CommandResult pipelineResult() {
        return pipelineResult;
    }

    public List<StageOutcome> stages() {
        return stages;
    }

    @Override
    public String toString() {
        return "PipelineResult{result=" + pipelineResult + ", stages=" + stages + "}";
    }
}
