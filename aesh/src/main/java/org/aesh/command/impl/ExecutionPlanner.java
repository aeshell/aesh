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
package org.aesh.command.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aesh.command.CommandResult;
import org.aesh.command.Execution;
import org.aesh.command.impl.operator.PipeOperator;
import org.aesh.command.invocation.CommandInvocation;

public class ExecutionPlanner<CI extends CommandInvocation> {

    public interface Unit<CI extends CommandInvocation> {
        boolean isPipeline();

        List<Execution<CI>> executions();
    }

    public static final class Single<CI extends CommandInvocation> implements Unit<CI> {
        private final Execution<CI> execution;

        public Single(Execution<CI> execution) {
            this.execution = execution;
        }

        @Override
        public boolean isPipeline() {
            return false;
        }

        @Override
        public List<Execution<CI>> executions() {
            return Collections.singletonList(execution);
        }

        public Execution<CI> execution() {
            return execution;
        }
    }

    public static final class Pipeline<CI extends CommandInvocation> implements Unit<CI> {
        private final List<Execution<CI>> stages;

        public Pipeline(List<Execution<CI>> stages) {
            this.stages = Collections.unmodifiableList(new ArrayList<>(stages));
        }

        @Override
        public boolean isPipeline() {
            return true;
        }

        @Override
        public List<Execution<CI>> executions() {
            return stages;
        }

        public List<Execution<CI>> upstreamStages() {
            return stages.subList(0, stages.size() - 1);
        }

        public Execution<CI> terminalStage() {
            return stages.get(stages.size() - 1);
        }
    }

    private final List<Execution<CI>> executions;
    private final Set<Execution<CI>> skipped = new HashSet<>();

    public ExecutionPlanner(List<Execution<CI>> executions) {
        this.executions = executions;
    }

    public Unit<CI> nextUnit() {
        int index = firstPendingIndex();
        if (index < 0)
            return null;
        int start;
        if (index == 0) {
            start = 0;
        } else {
            start = gatedStart(index);
            if (start < 0)
                return null;
        }
        List<Execution<CI>> chain = new ArrayList<>();
        int position = start;
        while (position < executions.size()
                && executions.get(position).getExecutable() instanceof PipeOperator) {
            chain.add(executions.get(position));
            position += 1;
        }
        if (position < executions.size())
            chain.add(executions.get(position));
        if (chain.size() == 1 && !(chain.get(0).getExecutable() instanceof PipeOperator))
            return new Single<>(chain.get(0));
        return new Pipeline<>(chain);
    }

    public boolean hasSkipped() {
        return !skipped.isEmpty();
    }

    public boolean hasMoreUnits() {
        return firstPendingIndex() >= 0;
    }

    public List<Execution<CI>> skippedExecutions() {
        return Collections.unmodifiableList(new ArrayList<>(skipped));
    }

    public void clearSkipped() {
        for (Execution<CI> execution : skipped)
            execution.clearQueuedLine();
    }

    private int firstPendingIndex() {
        for (int i = 0; i < executions.size(); i++) {
            if (executions.get(i).getResult() == null && !skipped.contains(executions.get(i)))
                return i;
        }
        return -1;
    }

    private int gatedStart(int index) {
        int n = 1;
        while (executions.get(index - n).getResult() == null)
            n += 1;
        CommandResult lastResult = executions.get(index - n).getResult();
        int i = index - 1;
        while (i < executions.size() - 1) {
            Execution<CI> exec = executions.get(i);
            if (exec.getExecutable().canExecuteNext(lastResult))
                return i + 1;
            i += 1;
            skipped.add(executions.get(i));
        }
        return -1;
    }
}
