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

import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandResult;
import org.aesh.command.Executable;
import org.aesh.command.Execution;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.completer.CompleterData;
import org.aesh.command.impl.completer.NullOptionCompleter;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ParsedCommand;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.operator.AndOperator;
import org.aesh.command.impl.operator.AppendOutputRedirectionOperator;
import org.aesh.command.impl.operator.ConfigurationOperator;
import org.aesh.command.impl.operator.DataProvider;
import org.aesh.command.impl.operator.EndOperator;
import org.aesh.command.impl.operator.ExecutableOperator;
import org.aesh.command.impl.operator.InputDelegate;
import org.aesh.command.impl.operator.InputRedirectionOperator;
import org.aesh.command.impl.operator.Operator;
import org.aesh.command.impl.operator.OrOperator;
import org.aesh.command.impl.operator.OutputRedirectionOperator;
import org.aesh.command.impl.operator.PipeOperator;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.aesh.command.operator.OperatorType;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.result.ResultHandler;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.io.PipelineResource;
import org.aesh.io.Resource;
import org.aesh.parser.ParsedLine;
import org.aesh.readline.AeshContext;
import org.aesh.readline.Prompt;
import org.aesh.readline.terminal.formatting.TerminalString;
import org.aesh.selector.Selector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author jdenise@redhat.com
 */
@SuppressWarnings("unchecked")
class Executions {

    private static class ExecutionImpl<T extends CommandInvocation> implements Execution<T> {

        private final ExecutableOperator<T> executable;
        private ProcessedCommand<Command<T>, T> cmd;
        private final CommandInvocationConfiguration invocationConfiguration;
        private final AeshCommandRuntime<T> runtime;
        private final CommandContainer<T> commandContainer;
        private CommandResult result;
        private boolean populated;
        ExecutionImpl(ExecutableOperator<T> executable,
                AeshCommandRuntime<T> runtime,
                CommandInvocationConfiguration invocationConfiguration,
                CommandContainer<T> commandContainer) {
            this.executable = executable;
            this.runtime = runtime;
            this.invocationConfiguration = invocationConfiguration;
            this.commandContainer = commandContainer;
            this.cmd = commandContainer.getParser().getProcessedCommand();
        }

        @Override
        public T getCommandInvocation() {
            return runtime.buildCommandInvocation(invocationConfiguration, commandContainer);
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
        public void populateCommand() throws CommandLineParserException, OptionValidatorException {
            if (!populated) {
                cmd = commandContainer.parseAndPopulate(runtime.invocationProviders(), runtime.getAeshContext());
                populated = true;
            }
        }

        @Override
        public ResultHandler getResultHandler() {
            return cmd.resultHandler();
        }

        @Override
        public CommandResult execute() throws CommandException, InterruptedException, CommandValidatorException,
                                                              CommandLineParserException, OptionValidatorException {
            //first we need to parse and populate the command line
            populateCommand();
            //finally we set the command that should be executed
            executable.setCommand(cmd.getCommand());

            if(cmd.validator() != null && !cmd.hasOptionWithOverrideRequired()) {
                cmd.validator().validate(getCommand());
            }
            if (cmd.getActivator() != null) {
                if (!cmd.getActivator().isActivated(new ParsedCommand(cmd))) {
                    result = CommandResult.FAILURE;
                    throw new CommandException("The command is not available in the current context.");
                }
            }

            if (hasRedirectIn()) {
                updateInjectedArgumentWithRedirectedInData();
                if (invocationConfiguration.getPipedData() != null) {
                    result = CommandResult.FAILURE;
                    throw new CommandException("Can't inject both from input and pipe operators");
                }
            }

            if (invocationConfiguration.getPipedData() != null) {
                updateInjectedArgumentWithPipelinedData(new PipelineResource(invocationConfiguration.getPipedData()));
                if (hasRedirectIn()) {
                    result = CommandResult.FAILURE;
                    throw new CommandException("Can't inject both from input and pipe operators");
                }
            }

            if(cmd.hasAskIfNotSet()) {
                for(ProcessedOption option : cmd.getAllAskIfNotSet()) {
                    try {
                        if(option.getOptionType().equals(OptionType.ARGUMENT) ||
                                   option.getOptionType().equals(OptionType.ARGUMENTS))
                            option.addValue(getCommandInvocation().getShell().readLine(
                                    new Prompt("Argument(s) is not set, please provide a value: ")));
                        else
                            option.addValue(getCommandInvocation().getShell().readLine(
                                    new Prompt("Option "+option.name()+", is not set, please provide a value: ")));

                        runtime.populateAskedOption(option);
                    }
                    catch(InterruptedException e) {
                        //input was interrupted, ignore it
                    }
                }
            }

            if(cmd.hasSelector()) {
                for(ProcessedOption option : cmd.getAllSelectors()) {
                    //if we do not have any default values, check if we can use the completer
                    if((option.getDefaultValues() == null || option.getDefaultValues().size() == 0) &&
                               (option.completer() != null && !(option.completer() instanceof NullOptionCompleter))) {
                        //first create a mock CompleterInvocation, then get all the values
                        CompleterData completerMock = new CompleterData(null, "", null);
                        option.completer().complete(completerMock);


                        option.addValues(new Selector(option.selectorType(),
                                completerMock.getCompleterValues().stream().map(TerminalString::getCharacters).collect(Collectors.toList()),
                                option.description()).doSelect(getCommandInvocation().getShell()));

                    }
                    else {
                        option.addValues(new Selector(option.selectorType(), option.getDefaultValues(), option.description())
                                                 .doSelect(getCommandInvocation().getShell()));
                    }
                    runtime.populateAskedOption(option);
                }
            }

            try {
                //if the generated help option is set, we "execute" it instead of normal execution
                if(cmd.generateHelp() && (cmd.isGenerateHelpOptionSet() || !cmd.anyOptionsSet())) {
                    T invocation = getCommandInvocation();
                    invocation.println(invocation.getHelpInfo());
                    result = CommandResult.SUCCESS;
                }
                //if the generated help option is set, we "execute" it instead of normal execution
                else if(cmd.version() != null && (cmd.isGenerateVersionOptionSet())) {
                    T invocation = getCommandInvocation();
                    invocation.println(cmd.name()+" version: "+cmd.version());
                    result = CommandResult.SUCCESS;
                }

                //else we execute as normal
                else
                    result = executable.execute(getCommandInvocation());

                if (getResultHandler() != null) {
                    if (result == null || result.equals(CommandResult.SUCCESS)) {
                        getResultHandler().onSuccess();
                    } else {
                        getResultHandler().onFailure(result);
                    }
                }
                if (result == null) {
                    result = CommandResult.SUCCESS;
                }
            }
            catch (CommandException ex) {
                result = CommandResult.FAILURE;
                throw ex;
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                result = CommandResult.FAILURE;
                throw ex;
            }
            catch(Exception e) {
                result = CommandResult.FAILURE;
                throw new RuntimeException(e);
            }
            finally {
                if (invocationConfiguration.getOutputRedirection() != null) {
                    try {
                        invocationConfiguration.getOutputRedirection().close();
                    }
                    catch (IOException ex) {
                        throw new CommandException(ex);
                    }
                }
            }
            return result;
        }

        private void updateInjectedArgumentWithPipelinedData(PipelineResource resource) {
            ProcessedOption arg = checkProcessedCommandForResourceArgument();
            if(arg != null)
                arg.injectResource(resource, cmd.getCommand());
        }

        private boolean hasRedirectIn() {
            return invocationConfiguration.getInputRedirection() != null;
        }

        private void updateInjectedArgumentWithRedirectedInData() {
            ProcessedOption arg = checkProcessedCommandForResourceArgument();
            if(arg != null)
                arg.injectResource(new PipelineResource(invocationConfiguration.getInputRedirection().read()), cmd.getCommand());
        }

        private ProcessedOption checkProcessedCommandForResourceArgument() {
            if(cmd.hasArguments() &&
                    Resource.class.isAssignableFrom(cmd.getArguments().type())) {
                return cmd.getArguments();
            }
            else if(cmd.hasArgument() &&
                    Resource.class.isAssignableFrom(cmd.getArgument().type())) {
                return cmd.getArgument();
            }
            return null;
        }

        /**
         * Returns the CommandResult or null if not executed.
         *
         * @return
         */
        @Override
        public CommandResult getResult() {
            return result;
        }

        @Override
        public void setResut(CommandResult result) {
            this.result = result;
        }

        @Override
        public void clearQueuedLine() {
            commandContainer.emptyLine();
        }
    }

    private enum State {
        NEED_COMMAND,
        NEED_OPERATOR,
        NEED_ARGUMENT
    }

    static <CI extends CommandInvocation> List<Execution<CI>> buildExecution(List<ParsedLine> fullLine,
            AeshCommandRuntime<CI> runtime)
            throws CommandNotFoundException, CommandLineParserException, IOException {
        State state = State.NEED_COMMAND;
        CommandContainer<CI> processedCommand = null;
        boolean newParsedLine;
        ConfigurationOperator config = null;
        DataProvider dataProvider = null;
        InputDelegate inDelegate = null;
        CommandInvocationConfiguration invocationConfiguration;
        List<Execution<CI>> executions = new ArrayList<>();
        for (ParsedLine pl : fullLine) {
            newParsedLine = false;
            if(!pl.hasWords())
                throw new CommandLineParserException(pl.errorMessage());
            while (!newParsedLine) {
                switch (state) {
                    case NEED_COMMAND: {
                        processedCommand = runtime.findCommandContainer(pl);
                        state = State.NEED_OPERATOR;
                        break;
                    }
                    case NEED_ARGUMENT: {
                        if (config == null) {
                            throw new IllegalArgumentException("Invalid " + pl.line());
                        }
                        config.setArgument(pl.firstWord().word());
                        state = State.NEED_OPERATOR;
                        break;
                    }
                    case NEED_OPERATOR: {
                        OperatorType ot = pl.operator();
                        Operator op = buildOperator(pl.operator(), runtime.getAeshContext());
                        if (ot.isConfiguration()) {
                            if (config != null) { // input provider prior to an output consumer.
                                if(config.getConfiguration().getInputRedirection() == null) {
                                    throw new IllegalArgumentException("Invalid operators structure");
                                }
                                inDelegate = config.getConfiguration().getInputRedirection();
                            }
                            config = (ConfigurationOperator) op;
                        }
                        if (ot.isConfiguration() && ot.hasArgument()) {
                            state = State.NEED_ARGUMENT;
                        }
                        else {
                            // The operator must be an executor one
                            if (!(op instanceof ExecutableOperator)) {
                                throw new IllegalArgumentException("Op " + ot + " is not executable");
                            }
                            if (processedCommand == null) {
                                throw new IllegalArgumentException("Invalid command line, command is missing.");
                            }
                            ExecutableOperator<CI> exec = (ExecutableOperator) op;
                            invocationConfiguration = config == null
                                    ? new CommandInvocationConfiguration(runtime.getAeshContext(), dataProvider)
                                    : new CommandInvocationConfiguration(runtime.getAeshContext(),
                                            config.getConfiguration().getOutputRedirection(),
                                            inDelegate == null ? config.getConfiguration().getInputRedirection() : inDelegate,
                                            dataProvider);
                            Execution<CI> execution = new ExecutionImpl<>(exec, runtime,
                                    invocationConfiguration, processedCommand);
                            if (exec instanceof DataProvider) {
                                dataProvider = (DataProvider) exec;
                            } else {
                                dataProvider = null;
                            }
                            executions.add(execution);
                            config = null;
                            inDelegate = null;
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
            invocationConfiguration = config == null
                    ? new CommandInvocationConfiguration(runtime.getAeshContext(), dataProvider)
                    : config.getConfiguration();
            Execution<CI> execution = new ExecutionImpl<CI>(exec, runtime, invocationConfiguration, processedCommand);
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
            case REDIRECT_IN: {
                return new InputRedirectionOperator(context);
            }
            case AND: {
                return new AndOperator();
            }
            case OR: {
                return new OrOperator();
            }
        }
        throw new IllegalArgumentException("Unsupported operator " + op);
    }
}
