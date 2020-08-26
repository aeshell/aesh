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

import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandNotFoundHandler;
import org.aesh.command.CommandResolver;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.Execution;
import org.aesh.command.Executor;
import org.aesh.command.activator.CommandActivatorProvider;
import org.aesh.command.activator.OptionActivatorProvider;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.CompleterInvocationProvider;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.converter.ConverterInvocationProvider;
import org.aesh.command.impl.activator.AeshOptionActivatorProvider;
import org.aesh.command.impl.completer.CompleterData;
import org.aesh.command.impl.completer.FileOptionCompleter;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.impl.parser.AeshCommandLineCompletionParser;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationBuilder;
import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.aesh.command.invocation.CommandInvocationProvider;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.operator.OperatorType;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.validator.ValidatorInvocationProvider;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.parser.LineParser;
import org.aesh.parser.ParsedLine;
import org.aesh.parser.ParserStatus;
import org.aesh.readline.AeshContext;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the Command processor.
 *
 * @author jdenise@redhat.com
 */
public class AeshCommandRuntime<CI extends CommandInvocation>
        implements CommandRuntime<CI>, CommandRegistry.CommandRegistrationListener {

    private final CommandRegistry<CI> registry;
    private final CommandInvocationProvider<CI> commandInvocationProvider;
    private final InvocationProviders invocationProviders;

    private static final Logger LOGGER = Logger.getLogger(AeshCommandRuntime.class.getName());
    private final CommandNotFoundHandler commandNotFoundHandler;

    private final CommandResolver<CI> commandResolver;
    private final AeshContext ctx;
    private final CommandInvocationBuilder<CI> commandInvocationBuilder;

    private final boolean parseBrackets;
    private final EnumSet<OperatorType> operators;

    public AeshCommandRuntime(AeshContext ctx,
            CommandRegistry<CI> registry,
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
    public CommandRegistry<CI> getCommandRegistry() {
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
    public InvocationProviders invocationProviders() {
        return invocationProviders;
    }

    @Override
    public CommandResult executeCommand(String line) throws CommandNotFoundException,
            CommandLineParserException,
            CommandValidatorException,
            CommandException,
            InterruptedException,
            IOException {

        Executor<CI> executor;
        try {
            executor = buildExecutor(line);
        }
        catch (CommandLineParserException e) {
            throw e;
        } catch (CommandNotFoundException cmd) {
            if (commandNotFoundHandler != null) {
                commandNotFoundHandler.handleCommandNotFound(line,
                        commandInvocationBuilder.build(this, null, null).getShell());
            }
            throw cmd;
        }
        Execution exec;
        CommandResult result = null;
        while ((exec = executor.getNextExecution()) != null) {
            try {
                result = exec.execute();
            } catch (CommandException cmd) {
                if (exec.getResultHandler() != null) {
                    exec.getResultHandler().onExecutionFailure(CommandResult.FAILURE, cmd);
                }
                throw cmd;
            } catch (CommandValidatorException | CommandLineParserException e) {
                if (exec.getResultHandler() != null) {
                    exec.getResultHandler().onValidationFailure(CommandResult.FAILURE, e);
                }
                throw e;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                if (exec.getResultHandler() != null) {
                    exec.getResultHandler().onValidationFailure(CommandResult.FAILURE, ex);
                }
                throw ex;
            } catch (Exception e) {
                if (exec.getResultHandler() != null) {
                    exec.getResultHandler().onValidationFailure(CommandResult.FAILURE, e);
                }
                throw new RuntimeException(e);
            }
        }
        if(result != null)
            return result;
        else
            return CommandResult.FAILURE;
    }

    @Override
    public CommandResult executeCommand(String... lines) throws CommandNotFoundException, CommandLineParserException, OptionValidatorException, CommandValidatorException, CommandException, InterruptedException, IOException {
        if(lines == null || lines.length == 0)
            throw new CommandException("No input lines");
        CommandResult result = null;
        for(String line : lines) {
            result = executeCommand(line);
            if(result == CommandResult.FAILURE)
                return result;
        }
        return result;
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
        ProcessedCommand<Command<CI>, CI> cmd = registry.getCommand(commandName, "").getParser().getProcessedCommand();
        List<CommandLineParser<CI>> childParsers = registry.getChildCommandParsers(commandName);
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
            CommandLineParserException, IOException {
        LOGGER.fine("Command: " + line);
        List<ParsedLine> lines = new LineParser().parseLine(line, -1, parseBrackets, operators);
        List<Execution<CI>> executions = Executions.buildExecution(lines, this);
        return new Executor<>(executions);
    }

    CI buildCommandInvocation(CommandInvocationConfiguration config, CommandContainer<CI> commandContainer) {
        return commandInvocationProvider.
                enhanceCommandInvocation(commandInvocationBuilder.build(this, config, commandContainer));
    }

    CommandContainer<CI> findCommandContainer(ParsedLine aeshLine) throws CommandNotFoundException {
        if (aeshLine.words().isEmpty()) {
            return null;
        }
        final String name = aeshLine.firstWord().word();
        CommandContainer<CI> container =
                commandResolver.resolveCommand(name, aeshLine.line());
        if (container == null) {
            throw new CommandNotFoundException("No command handler for '"+name+ "'.",name);
        }
        container.addLine(aeshLine);
        return container;
    }

    void populateAskedOption(ProcessedOption option) {
        try {
            option.injectValueIntoField(option.parent().getCommand(), invocationProviders, getAeshContext(), false);
        }
        catch(OptionValidatorException e) {
            LOGGER.log(Level.WARNING, "Trying to inject value: "+option.getValue()+", into option: "+option.name()+" failed", e);
        }
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
        if(operators.isEmpty())
            simpleComplete(completeOperation);
        else {
            completeWithOperators(completeOperation);
        }
    }

    private void completeWithOperators(AeshCompleteOperation completeOperation) {
        List<ParsedLine> lines = new LineParser()
                .input(completeOperation.getBuffer())
                .cursor(completeOperation.getCursor())
                .parseBrackets(true)
                .operators(operators)
                .parseWithOperators();

        if(!lines.isEmpty()) {
            for(int i=0; i < lines.size(); i++) {
                if (lines.get(i).cursor() > -1) {
                    if(i == 0) {
                        doSimpleComplete(completeOperation, lines.get(i));
                        return;
                    }
                    //we need to check the previous line
                    //if it is redirect/append out we should use a file completer
                    else {
                        if(OperatorType.isAppendOrRedirectInOrOut(lines.get(i-1).operator())) {
                            //do file completion
                            FileOptionCompleter completer = new FileOptionCompleter();
                            CompleterInvocation invocation
                                    = new CompleterData(completeOperation.getContext(),
                                            lines.get(i).selectedWord().word(), null);
                            completer.complete(invocation);
                            completeOperation.addCompletionCandidatesTerminalString(invocation.getCompleterValues());
                            AeshCommandLineCompletionParser.verifyCompleteValue(completeOperation,
                                    invocation,
                                    lines.get(i).selectedWord().word(),
                                    lines.get(i).selectedWord().status(), null);
                            return;
                        }
                        else {
                            doSimpleComplete(completeOperation, lines.get(i));
                            return;
                        }
                    }
                }
            }
            //we should not end up here, but if we do, use the last line
            doSimpleComplete(completeOperation, lines.get(lines.size() - 1));
        }
        simpleComplete(completeOperation);

    }

    private void simpleComplete(AeshCompleteOperation completeOperation) {
        ParsedLine parsedLine = new LineParser()
                .input(completeOperation.getBuffer())
                .cursor(completeOperation.getCursor())
                .parseBrackets(true)
                .parse();

        doSimpleComplete(completeOperation, parsedLine);
    }

    private void doSimpleComplete(AeshCompleteOperation completeOperation, ParsedLine parsedLine) {
        if((parsedLine.selectedIndex() == 0 || //possible command name
                parsedLine.words().size() == 0) && ParserStatus.okForCompletion(parsedLine.status())) {
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
