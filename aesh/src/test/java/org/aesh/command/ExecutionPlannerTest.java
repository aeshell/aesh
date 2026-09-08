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
 * See the LICENSE for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.aesh.command.impl.ExecutionPlanner;
import org.aesh.command.impl.operator.AndOperator;
import org.aesh.command.impl.operator.EndOperator;
import org.aesh.command.impl.operator.OrOperator;
import org.aesh.command.impl.operator.PipeOperator;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.result.ResultHandler;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.AeshContext;
import org.junit.Test;

public class ExecutionPlannerTest {

    static class FakeExecution implements Execution<CommandInvocation> {
        private final Executable executable;
        private CommandResult result;

        FakeExecution(Executable executable) {
            this.executable = executable;
        }

        @Override
        public CommandInvocation getCommandInvocation() {
            return null;
        }

        @Override
        public Executable getExecutable() {
            return executable;
        }

        @Override
        public Command<CommandInvocation> getCommand() {
            return null;
        }

        @Override
        public void populateCommand() {
        }

        @Override
        public ResultHandler getResultHandler() {
            return null;
        }

        @Override
        public CommandResult execute() {
            return result;
        }

        @Override
        public CommandResult getResult() {
            return result;
        }

        @Override
        public void setResult(CommandResult result) {
            this.result = result;
        }

        @Override
        public void clearQueuedLine() {
        }
    }

    private static AeshContext context() {
        return SettingsBuilder.builder().build().aeshContext();
    }

    private static FakeExecution end() {
        return new FakeExecution(new EndOperator<CommandInvocation>());
    }

    private static FakeExecution and() {
        return new FakeExecution(new AndOperator<CommandInvocation>());
    }

    private static FakeExecution or() {
        return new FakeExecution(new OrOperator<CommandInvocation>());
    }

    private static FakeExecution pipe() {
        return new FakeExecution(new PipeOperator(context()));
    }

    @Test
    public void testSingleCommand() {
        FakeExecution exec = end();
        ExecutionPlanner<CommandInvocation> planner = new ExecutionPlanner<>(Arrays.asList(exec));

        ExecutionPlanner.Unit<CommandInvocation> unit = planner.nextUnit();
        assertFalse(unit.isPipeline());
        assertEquals(exec, unit.executions().get(0));

        exec.setResult(CommandResult.SUCCESS);
        assertNull(planner.nextUnit());
    }

    @Test
    public void testSequentialCommands() {
        FakeExecution first = end();
        FakeExecution second = end();
        FakeExecution third = end();
        ExecutionPlanner<CommandInvocation> planner = new ExecutionPlanner<>(
                Arrays.asList(first, second, third));

        assertEquals(first, planner.nextUnit().executions().get(0));
        first.setResult(CommandResult.SUCCESS);
        assertEquals(second, planner.nextUnit().executions().get(0));
        second.setResult(CommandResult.FAILURE);
        assertEquals(third, planner.nextUnit().executions().get(0));
        third.setResult(CommandResult.SUCCESS);
        assertNull(planner.nextUnit());
        assertFalse(planner.hasSkipped());
    }

    @Test
    public void testAndGateSuccess() {
        FakeExecution first = and();
        FakeExecution second = end();
        ExecutionPlanner<CommandInvocation> planner = new ExecutionPlanner<>(Arrays.asList(first, second));

        assertEquals(first, planner.nextUnit().executions().get(0));
        first.setResult(CommandResult.SUCCESS);
        assertEquals(second, planner.nextUnit().executions().get(0));
    }

    @Test
    public void testAndGateFailureSkips() {
        FakeExecution first = and();
        FakeExecution second = end();
        ExecutionPlanner<CommandInvocation> planner = new ExecutionPlanner<>(Arrays.asList(first, second));

        planner.nextUnit();
        first.setResult(CommandResult.FAILURE);
        assertNull(planner.nextUnit());
        assertTrue(planner.hasSkipped());
        assertEquals(Arrays.asList(second), planner.skippedExecutions());
    }

    @Test
    public void testOrGateFailure() {
        FakeExecution first = or();
        FakeExecution second = end();
        ExecutionPlanner<CommandInvocation> planner = new ExecutionPlanner<>(Arrays.asList(first, second));

        planner.nextUnit();
        first.setResult(CommandResult.FAILURE);
        assertEquals(second, planner.nextUnit().executions().get(0));
    }

    @Test
    public void testOrGateSuccessSkips() {
        FakeExecution first = or();
        FakeExecution second = end();
        ExecutionPlanner<CommandInvocation> planner = new ExecutionPlanner<>(Arrays.asList(first, second));

        planner.nextUnit();
        first.setResult(CommandResult.SUCCESS);
        assertNull(planner.nextUnit());
        assertTrue(planner.hasSkipped());
    }

    @Test
    public void testPipeChainIsSingleUnit() {
        FakeExecution first = pipe();
        FakeExecution second = pipe();
        FakeExecution third = end();
        ExecutionPlanner<CommandInvocation> planner = new ExecutionPlanner<>(
                Arrays.asList(first, second, third));

        ExecutionPlanner.Unit<CommandInvocation> unit = planner.nextUnit();
        assertTrue(unit.isPipeline());
        assertEquals(Arrays.asList(first, second, third), unit.executions());
        ExecutionPlanner.Pipeline<CommandInvocation> pipeline = (ExecutionPlanner.Pipeline<CommandInvocation>) unit;
        assertEquals(Arrays.asList(first, second), pipeline.upstreamStages());
        assertEquals(third, pipeline.terminalStage());

        first.setResult(CommandResult.SUCCESS);
        second.setResult(CommandResult.SUCCESS);
        third.setResult(CommandResult.SUCCESS);
        assertNull(planner.nextUnit());
    }

    @Test
    public void testMixedPipeAndGate() {
        FakeExecution first = pipe();
        FakeExecution second = and();
        FakeExecution third = end();
        ExecutionPlanner<CommandInvocation> planner = new ExecutionPlanner<>(
                Arrays.asList(first, second, third));

        ExecutionPlanner.Unit<CommandInvocation> unit = planner.nextUnit();
        assertTrue(unit.isPipeline());
        assertEquals(Arrays.asList(first, second), unit.executions());

        first.setResult(CommandResult.SUCCESS);
        second.setResult(CommandResult.SUCCESS);
        assertEquals(third, planner.nextUnit().executions().get(0));
    }

    @Test
    public void testMixedPipeAndGateFailure() {
        FakeExecution first = pipe();
        FakeExecution second = and();
        FakeExecution third = end();
        ExecutionPlanner<CommandInvocation> planner = new ExecutionPlanner<>(
                Arrays.asList(first, second, third));

        planner.nextUnit();
        first.setResult(CommandResult.SUCCESS);
        second.setResult(CommandResult.FAILURE);
        assertNull(planner.nextUnit());
        assertEquals(Arrays.asList(third), planner.skippedExecutions());
    }

    @Test
    public void testPlanningMutatesNoResults() {
        FakeExecution first = pipe();
        FakeExecution second = and();
        FakeExecution third = end();
        List<Execution<CommandInvocation>> executions = Arrays.asList(first, second, third);
        ExecutionPlanner<CommandInvocation> planner = new ExecutionPlanner<>(executions);

        planner.nextUnit();
        for (Execution<CommandInvocation> execution : executions)
            assertNull(execution.getResult());
    }

    @Test
    public void testEmpty() {
        ExecutionPlanner<CommandInvocation> planner = new ExecutionPlanner<>(
                Arrays.<Execution<CommandInvocation>> asList());
        assertNull(planner.nextUnit());
        assertFalse(planner.hasSkipped());
    }
}
