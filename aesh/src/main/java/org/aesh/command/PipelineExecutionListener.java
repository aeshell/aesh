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
 * A {@link CommandExecutionListener} that additionally receives per-stage
 * pipeline events.
 * <p>
 * The interactive path fires {@link #onStageComplete(StageOutcome)} for every
 * pipeline stage (including upstream stages, which otherwise have no
 * listener) and {@link #onPipelineComplete(PipelineResult)} once per
 * pipeline. The existing {@code onCommandComplete} contract is unchanged and
 * still fires for the terminal stage.
 * <p>
 * Ordering guarantee: within one pipeline, {@code stage*} events fire, then
 * {@code pipeline}, then the terminal {@code onCommandComplete} — sequentially
 * on the terminal job's thread, with the pipeline events strictly before the
 * drain launches the next command. There is deliberately no FIFO order
 * <em>across</em> commands: the terminal callback and the next command's
 * callbacks run on different worker threads and may interleave.
 *
 * @author Aesh team
 */
public interface PipelineExecutionListener extends CommandExecutionListener {

    default void onStageComplete(StageOutcome stage) {
    }

    default void onPipelineComplete(PipelineResult result) {
    }
}
