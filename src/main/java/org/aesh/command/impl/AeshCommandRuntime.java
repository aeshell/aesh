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
import org.aesh.command.impl.parser.CommandLineCompletionParser;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.parser.ParsedCompleteObject;
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
import org.aesh.command.container.CommandContainerResult;
import org.aesh.command.converter.ConverterInvocationProvider;
import org.aesh.command.invocation.CommandInvocationProvider;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.validator.ValidatorInvocationProvider;
import org.aesh.console.settings.CommandNotFoundHandler;
import org.aesh.parser.LineParser;
import org.aesh.parser.ParsedLine;

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

    public AeshCommandRuntime(AeshContext ctx,
                              CommandRegistry<C> registry,
                              CommandInvocationProvider<CI> commandInvocationProvider,
                              CommandNotFoundHandler commandNotFoundHandler,
                              CompleterInvocationProvider completerInvocationProvider,
                              ConverterInvocationProvider converterInvocationProvider,
                              ValidatorInvocationProvider validatorInvocationProvider,
                              OptionActivatorProvider optionActivatorProvider,
                              CommandActivatorProvider commandActivatorProvider,
                              CommandInvocationBuilder<CI> commandInvocationBuilder) {
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
            InterruptedException {
        ResultHandler resultHandler = null;
        try (CommandContainer<? extends Command> container = commandResolver.resolveCommand(line)) {
            resultHandler = container.getParser().getProcessedCommand().resultHandler();
            CommandContainerResult ccResult = container.executeCommand(
                    LineParser.parseLine(line),
                    invocationProviders,
                    ctx,
                    commandInvocationProvider.enhanceCommandInvocation(
                            commandInvocationBuilder.build(this)));

            CommandResult result = ccResult.getCommandResult();

            if (result == CommandResult.SUCCESS && resultHandler != null) {
                resultHandler.onSuccess();
            } else if (resultHandler != null) {
                resultHandler.onFailure(result);
            }
        } catch (CommandLineParserException | CommandValidatorException | OptionValidatorException e) {
            if (resultHandler != null) {
                resultHandler.onValidationFailure(CommandResult.FAILURE, e);
            }
            throw e;
        } catch (CommandException cmd) {
            if (resultHandler != null) {
                resultHandler.onExecutionFailure(CommandResult.FAILURE, cmd);
            }
            throw cmd;
        } catch (CommandNotFoundException cmd) {
            if (commandNotFoundHandler != null) {
                commandNotFoundHandler.handleCommandNotFound(line, commandInvocationBuilder.build(this).getShell());
            }
            throw cmd;
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
            CommandValidatorException {
        // XXX JFDENISE, for now no OPERATORS

        //Command<CI> c = getPopulatedCommand(line);
        ProcessedCommand<C> processedCommand = getPopulatedCommand(line);
        return new Executor<>(commandInvocationProvider.enhanceCommandInvocation(
                commandInvocationBuilder.build(this)),
                processedCommand.getCommand(), processedCommand.resultHandler());
    }

    private ProcessedCommand<C> getPopulatedCommand(String commandLine) throws CommandNotFoundException,
            CommandLineParserException, OptionValidatorException {
        if (commandLine == null || commandLine.isEmpty()) {
            return null;
        }

        ParsedLine aeshLine = LineParser.parseLine(commandLine);
        if (aeshLine.words().isEmpty()) {
            return null;
        }
        String opName = aeshLine.words().get(0).word();
        CommandContainer<C> container = registry.getCommand(opName, commandLine);
        if (container == null) {
            throw new CommandNotFoundException("No command handler for '" + opName + "'.");
        }
        container.getParser().parse(commandLine, false);
        if(container.getParser().getProcessedCommand().parserExceptions().size() > 0) {
            throw new CommandLineParserException("Invalid Command " + commandLine +". Error: "+
            container.getParser().getProcessedCommand().parserExceptions().get(0));
        }
        container.getParser().parsedCommand().getCommandPopulator().populateObject(container.getParser().parsedCommand().getProcessedCommand(),
                invocationProviders, getAeshContext(), true);
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
        commandResolver.getRegistry().completeCommandName(completeOperation);
        if (completeOperation.getCompletionCandidates().size() < 1) {

            try (CommandContainer commandContainer = commandResolver.resolveCommand( completeOperation.getBuffer())) {

                CommandLineCompletionParser completionParser = commandContainer
                        .getParser().getCompletionParser();

                ParsedCompleteObject completeObject = completionParser
                        .findCompleteObject(completeOperation.getBuffer(),
                                completeOperation.getCursor());
                completeObject.getCompletionParser().injectValuesAndComplete(completeObject,
                        completeOperation, invocationProviders);
            }
            catch (CommandLineParserException e) {
                LOGGER.warning(e.getMessage());
            }
            catch (CommandNotFoundException ignored) {
            }
            catch (Exception ex) {
                LOGGER.log(Level.SEVERE,
                        "Runtime exception when completing: "
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
