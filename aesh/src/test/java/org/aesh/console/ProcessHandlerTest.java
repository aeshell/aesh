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
package org.aesh.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.Executable;
import org.aesh.command.Execution;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.result.ResultHandler;
import org.aesh.terminal.tty.Signal;
import org.aesh.tty.TestConnection;
import org.junit.Test;

public class ProcessHandlerTest {

    static class RecordingConnection extends TestConnection {
        final List<Object> stdinHandlerSets = new ArrayList<>();
        final List<Object> signalHandlerSets = new ArrayList<>();

        @Override
        public void setStdinHandler(Consumer<int[]> handler) {
            stdinHandlerSets.add(handler);
            super.setStdinHandler(handler);
        }

        @Override
        public void setSignalHandler(Consumer<Signal> handler) {
            signalHandlerSets.add(handler);
            super.setSignalHandler(handler);
        }
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testRunNeverTouchesStdinHandler() {
        RecordingConnection connection = new RecordingConnection();
        Consumer<int[]> preCommand = ints -> {
        };
        Consumer<Signal> preSignal = signal -> {
        };
        connection.setStdinHandler(preCommand);
        connection.setSignalHandler(preSignal);
        connection.stdinHandlerSets.clear();
        connection.signalHandlerSets.clear();

        ProcessManager manager = new ProcessManager(null) {
            @Override
            public void processFinished(CommandJob job) {
            }
        };
        Execution<CommandInvocation> execution = new Execution<CommandInvocation>() {
            @Override
            public CommandInvocation getCommandInvocation() {
                return null;
            }

            @Override
            public Executable getExecutable() {
                return null;
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
                return CommandResult.SUCCESS;
            }

            @Override
            public CommandResult getResult() {
                return CommandResult.SUCCESS;
            }

            @Override
            public void setResult(CommandResult result) {
            }

            @Override
            public void clearQueuedLine() {
            }
        };

        new CommandJob(manager, connection, execution, "test", null).run();

        assertTrue("CommandJob.run must not touch the stdin handler, saw: " + connection.stdinHandlerSets,
                connection.stdinHandlerSets.isEmpty());
        assertEquals(preSignal, connection.signalHandler());
    }
}
