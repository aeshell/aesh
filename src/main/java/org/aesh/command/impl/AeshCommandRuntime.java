/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aesh.command.CommandRuntime;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandResolver;
import org.aesh.command.CommandResult;
import org.aesh.command.Executor;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocationBuilder;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.result.ResultHandler;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.console.AeshContext;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.impl.activator.AeshOptionActivatorProvider;
import org.aesh.command.activator.CommandActivatorProvider;
import org.aesh.command.activator.OptionActivatorProvider;
import org.aesh.command.completer.CompleterInvocationProvider;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.converter.ConverterInvocationProvider;
import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.aesh.command.invocation.CommandInvocationProvider;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.validator.ValidatorInvocationProvider;
import org.aesh.console.settings.CommandNotFoundHandler;
import org.aesh.parser.LineParser;
import org.aesh.parser.ParsedLine;
import org.aesh.command.Execution;
import org.aesh.command.operator.OperatorType;

/**
 * Implementation of the Command processor.
 *
 * @author jdenise@redhat.com
 */
public class AeshCommandRuntime<C extends Command, CI extends CommandInvocation>
        implements CommandRuntime<CI>, CommandRegistry.CommandRegistrationListener {

    private final CommandRegistry<C> registry;
    private final CommandInvocationProvider<CI> commandInvocationProvider;
    private final InvocationProviders invocationProviders;

    private static final Logger LOGGER = Logger.getLogger(AeshCommandRuntime.class.getName());
    private final CommandNotFoundHandler commandNotFoundHandler;

    private final CommandResolver<? extends Command> commandResolver;
    private final AeshContext ctx;
    private final CommandInvocationBuilder<CI> commandInvocationBuilder;

    private final boolean parseBrackets;
    private final EnumSet<OperatorType> operators;

    public AeshCommandRuntime(AeshContext ctx,
            CommandRegistry<C> registry,
            CommandInvocationProvider<CI> commandInvocationProvider,
            CommandNotFoundHandler commandNotFoundHandler,
            CompleterInvocationProvider completerInvocationProvider,
            ConverterInvocationProvider converterInvocationProvider,
            ValidatorInvocationProvider validatorInvocationProvider,
            OptionActivatorProvider optionActivatorProvider,
            CommandActivatorProvider commandActivatorProvider,
            CommandInvocationBuilder<CI> commandInvocationBuilder,
            boolean parseBrackets,
            EnumSet<OperatorType> operators) {
        this.ctx = ctx;
        this.registry = registry;
        commandResolver = new AeshCommandResolver<>(registry);
        this.commandInvocationProvider = commandInvocationProvider;
        this.commandNotFoundHandler = commandNotFoundHandler;
        this.commandInvocationBuilder = commandInvocationBuilder;
        this.invocationProviders
                = new AeshInvocationProviders(converterInvocationProvider, completerInvocationProvider,
                        validatorInvocationProvider, optionActivatorProvider, commandActivatorProvider);
        processAfterInit();
        registry.addRegistrationListener(this);
        this.parseBrackets = parseBrackets;
        this.operators = operators;
    }

    @Override
    public CommandRegistry<C> getCommandRegistry() {
        return registry;
    }

    @Override
    public AeshContext getAeshContext() {
        return ctx;
    }

    @Override
    public CommandInvocationBuilder<CI> commandInvocationBuilder() {
        return commandInvocationBuilder;
    }

    @Override
    public void executeCommand(String line) throws CommandNotFoundException,
            CommandLineParserException,
            OptionValidatorException,
            CommandValidatorException,
            CommandException,
            InterruptedException,
            IOException {
        ResultHandler resultHandler = null;

        Executor<CI> executor = null;
        try {
            executor = buildExecutor(line);
        } catch (CommandLineParserException | CommandValidatorException | OptionValidatorException e) {
            if (resultHandler != null) {
                resultHandler.onValidationFailure(CommandResult.FAILURE, e);
            }
            throw e;
        } catch (CommandNotFoundException cmd) {
            if (commandNotFoundHandler != null) {
                commandNotFoundHandler.handleCommandNotFound(line,
                        commandInvocationBuilder.build(this, null).getShell());
            }
            throw cmd;
        }
        for (Execution exec : executor.getExecutions()) {
            try {
                exec.execute();
            } catch (CommandException cmd) {
                if (resultHandler != null) {
                    resultHandler.onExecutionFailure(CommandResult.FAILURE, cmd);
                }
                throw cmd;
            } catch (CommandValidatorException e) {
                if (resultHandler != null) {
                    resultHandler.onValidationFailure(CommandResult.FAILURE, e);
                }
                throw e;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                if (resultHandler != null) {
                    resultHandler.onValidationFailure(CommandResult.FAILURE, ex);
                }
                throw ex;
            } catch (Exception e) {
                if (resultHandler != null) {
                    resultHandler.onValidationFailure(CommandResult.FAILURE, e);
                }
                throw new RuntimeException(e);
            }
        }
    }

    private void processAfterInit() {
        try {
            for (String commandName : registry.getAllCommandNames()) {
                updateCommand(commandName);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Exception while iterating commands.", e);
        }
    }

    private void updateCommand(String commandName) throws CommandNotFoundException {
        ProcessedCommand cmd = registry.getCommand(commandName, "").getParser().getProcessedCommand();
        List<? extends CommandLineParser<C>> childParsers = registry.getChildCommandParsers(commandName);
        if (!(invocationProviders.getOptionActivatorProvider() instanceof AeshOptionActivatorProvider)) {
            //we have a custom OptionActivatorProvider, and need to process all options
            cmd.updateInvocationProviders(invocationProviders);
            for (CommandLineParser<?> child : childParsers) {
                child.getProcessedCommand().updateInvocationProviders(invocationProviders);
            }
        }
    }

    @Override
    public Executor<CI> buildExecutor(String line) throws CommandNotFoundException,
            CommandLineParserException, OptionValidatorException,
            CommandValidatorException, IOException {
        List<ParsedLine> lines = new LineParser().parseLine(line, -1, parseBrackets, operators);
        List<Execution<CI>> executions = Executions.buildExecution(lines, this);
        return new Executor(executions);
    }

    CI buildCommandInvocation(CommandInvocationConfiguration config) {
        return commandInvocationProvider.
                enhanceCommandInvocation(commandInvocationBuilder.build(this, config));
    }

    ProcessedCommand<C> getPopulatedCommand(ParsedLine aeshLine) throws CommandNotFoundException,
            CommandLineParserException, OptionValidatorException {
        String commandLine = aeshLine.line();
        if (aeshLine.words().isEmpty()) {
            return null;
        }
        String opName = aeshLine.words().get(0).word();
        CommandContainer container = commandResolver.resolveCommand(opName, commandLine);
        if (container == null) {
            throw new CommandNotFoundException("No command handler for '" + opName + "'.");
        }
        container.getParser().parse(aeshLine.iterator(), CommandLineParser.Mode.STRICT);
        if (container.getParser().getProcessedCommand().parserExceptions().size() > 0) {
            throw new CommandLineParserException("Invalid Command " + commandLine + ". Error: "
                    + container.getParser().getProcessedCommand().parserExceptions().get(0));
        }
        container.getParser().parsedCommand().getCommandPopulator().populateObject(container.getParser().parsedCommand().getProcessedCommand(),
                invocationProviders, getAeshContext(), CommandLineParser.Mode.VALIDATE);
        return container.getParser().parsedCommand().getProcessedCommand();
    }

    @Override
    public void registrationAction(String commandName, CommandRegistry.REGISTRATION_ACTION action) {
        if (action == CommandRegistry.REGISTRATION_ACTION.ADDED) {
            try {
                updateCommand(commandName);
            } catch (Exception e) {
                LOGGER.log(Level.FINER, "Exception while iterating commands.", e);
            }
        }
    }

    @Override
    public void complete(AeshCompleteOperation completeOperation) {
        ParsedLine parsedLine = new LineParser()
                .input(completeOperation.getBuffer())
                .cursor(completeOperation.getCursor())
                .parseBrackets(true)
                .parse();

        if(parsedLine.selectedIndex() == 0 || //possible command name
                parsedLine.line().length() == 0) {
            commandResolver.getRegistry().completeCommandName(completeOperation, parsedLine);
        }
        if (completeOperation.getCompletionCandidates().size() < 1) {

            try (CommandContainer commandContainer = commandResolver.resolveCommand(parsedLine)) {

                commandContainer.getParser()
                        .complete(completeOperation, parsedLine, invocationProviders);
            }
            catch (CommandNotFoundException ignored) {
            }
            catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Runtime exception when completing: "
                        + completeOperation, ex);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        registry.removeRegistrationListener(this);
        super.finalize();
    }
}
