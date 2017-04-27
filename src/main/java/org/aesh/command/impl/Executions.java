/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandResult;
import org.aesh.command.Executable;
import org.aesh.command.Execution;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.operator.AppendOutputRedirectionOperator;
import org.aesh.command.impl.operator.EndOperator;
import org.aesh.command.impl.operator.OutputRedirectionOperator;
import org.aesh.command.impl.operator.PipeOperator;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.aesh.command.impl.operator.ConfigurationOperator;
import org.aesh.command.impl.operator.DataProvider;
import org.aesh.command.impl.operator.ExecutableOperator;
import org.aesh.command.impl.operator.Operator;
import org.aesh.command.operator.OperatorType;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.result.ResultHandler;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.console.AeshContext;
import org.aesh.parser.ParsedLine;
import org.aesh.command.validator.CommandValidatorException;

/**
 *
 * @author jdenise@redhat.com
 */
class Executions {

    private static class ExecutionImpl<T extends CommandInvocation> implements Execution {

        private final ExecutableOperator executable;
        private final ProcessedCommand<? extends Command> cmd;
        private final T invocation;
        public ExecutionImpl(ExecutableOperator executable, T invocation, ProcessedCommand<? extends Command> cmd) {
            this.executable = executable;
            this.invocation = invocation;
            this.cmd = cmd;
            this.executable.setCommand(cmd.getCommand());
        }

        @Override
        public T getCommandInvocation() {
            return invocation;
        }

        /**
         * @return the executable
         */
        @Override
        public Executable getExecutable() {
            return executable;
        }

        /**
         * @return the cmd
         */
        @Override
        public Command<T> getCommand() {
            return cmd.getCommand();
        }

        @Override
        public ResultHandler getResultHandler() {
            return cmd.resultHandler();
        }

        @Override
        public CommandResult execute() throws CommandException, InterruptedException,
                CommandValidatorException {
            if (cmd.validator() != null && !cmd.hasOptionWithOverrideRequired()) {
                cmd.validator().validate(getCommand());
            }
            CommandResult result = executable.execute(getCommandInvocation());

            if (getResultHandler() != null) {
                if (result == null || result.equals(CommandResult.SUCCESS)) {
                    getResultHandler().onSuccess();
                } else {
                    getResultHandler().onFailure(result);
                }
            }
            return result;
        }
    }

    private enum State {
        NEED_COMMAND,
        NEED_OPERATOR,
        NEED_ARGUMENT
    }

    static <CI extends CommandInvocation> List<Execution<CI>> buildExecution(List<ParsedLine> fullLine, AeshCommandRuntime runtime)
            throws CommandNotFoundException, CommandLineParserException, OptionValidatorException, IOException {
        State state = State.NEED_COMMAND;
        ProcessedCommand<? extends Command> processedCommand = null;
        boolean newParsedLine = false;
        ConfigurationOperator config = null;
        DataProvider dataProvider = null;
        List<Execution<CI>> executions = new ArrayList<>();
        for (ParsedLine pl : fullLine) {
            newParsedLine = false;
            while (!newParsedLine) {
                switch (state) {
                    case NEED_COMMAND: {
                        processedCommand = runtime.getPopulatedCommand(pl);
                        state = State.NEED_OPERATOR;
                        break;
                    }
                    case NEED_ARGUMENT: {
                        if (config == null) {
                            throw new IllegalArgumentException("Invalid " + pl.line());
                        }
                        config.setArgument(pl.line().trim());
                        state = State.NEED_OPERATOR;
                        break;
                    }
                    case NEED_OPERATOR: {
                        OperatorType ot = pl.operator();
                        Operator op = buildOperator(pl.operator(), runtime.getAeshContext());
                        if (ot.isConfiguration()) {
                            config = (ConfigurationOperator) op;
                        }
                        if (ot.isConfiguration() && ot.hasArgument()) {
                            state = State.NEED_ARGUMENT;
                        } else {
                            // The operator must be an executor one
                            if (!(op instanceof ExecutableOperator)) {
                                throw new IllegalArgumentException("Op " + ot + " is not executable");
                            }
                            ExecutableOperator exec = (ExecutableOperator) op;
                            CommandInvocationConfiguration invocationConfiguration = config == null
                                    ? new CommandInvocationConfiguration(runtime.getAeshContext(), null,
                                            dataProvider) : config.getConfiguration();
                            Execution execution = new ExecutionImpl(exec,
                                    runtime.buildCommandInvocation(invocationConfiguration), processedCommand);
                            if (exec instanceof DataProvider) {
                                dataProvider = (DataProvider) exec;
                            } else {
                                dataProvider = null;
                            }
                            executions.add(execution);
                            config = null;
                            state = State.NEED_COMMAND;
                        }
                        newParsedLine = true;
                        break;
                    }
                }
            }
        }
        if (state == State.NEED_OPERATOR) {
            // The implicit execution operator is missing.
            ExecutableOperator exec = (ExecutableOperator) buildOperator(OperatorType.NONE,
                    runtime.getAeshContext());
            CommandInvocationConfiguration invocationConfiguration = config == null
                    ? new CommandInvocationConfiguration(runtime.getAeshContext(), null,
                            dataProvider) : config.getConfiguration();
            Execution execution = new ExecutionImpl(exec,
                    runtime.buildCommandInvocation(invocationConfiguration), processedCommand);
            executions.add(execution);
        }
        return executions;
    }

    private static Operator buildOperator(OperatorType op, AeshContext context) {
        if (op == null) {
            return null;
        }
        switch (op) {
            case NONE:
            case END: {
                return new EndOperator();
            }
            case REDIRECT_OUT: {
                return new OutputRedirectionOperator(context);
            }
            case APPEND_OUT: {
                return new AppendOutputRedirectionOperator(context);
            }
            case PIPE: {
                return new PipeOperator(context);
            }
        }
        throw new IllegalArgumentException("Unsupported operator " + op);
    }
}
