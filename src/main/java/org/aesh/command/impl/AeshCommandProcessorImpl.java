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

import org.aesh.command.AeshCommandProcessor;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandResolver;
import org.aesh.command.CommandResult;
import org.aesh.command.Executor;
import org.aesh.command.Shell;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.CommandLineCompletionParser;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.parser.CommandLineParserException;
import org.aesh.command.impl.parser.ParsedCompleteObject;
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
import org.aesh.readline.Prompt;
import org.aesh.readline.action.KeyAction;
import org.aesh.terminal.Key;
import org.aesh.tty.Size;

/**
 * Implementation of the Command processor.
 *
 * @author jdenise@redhat.com
 */
class AeshCommandProcessorImpl<C extends Command, CI extends CommandInvocation, CO extends AeshCompleteOperation> implements AeshCommandProcessor<CI, CO> {

    private static class DefaultCommandInvocation implements CommandInvocation {

        private static class DefaultShell implements Shell {

            @Override
            public void write(String out) {
                System.out.print(out);
            }

            @Override
            public void writeln(String out) {
                System.out.println(out);
            }

            @Override
            public void write(int[] out) {
                // Not supported.
            }

            @Override
            public String readLine() throws InterruptedException {
                return null;
            }

            @Override
            public String readLine(Prompt prompt) throws InterruptedException {
                return null;
            }

            @Override
            public Key read() throws InterruptedException {
                return null;
            }

            @Override
            public Key read(Prompt prompt) throws InterruptedException {
                return null;
            }

            @Override
            public boolean enableAlternateBuffer() {
                return false;
            }

            @Override
            public boolean enableMainBuffer() {
                return false;
            }

            @Override
            public Size size() {
                return new Size(1, -1);
            }

            @Override
            public void clear() {
            }
        }

        private final Shell shell = new DefaultShell();
        private final AeshCommandProcessorImpl<? extends Command, ? extends CommandInvocation, ? extends AeshCompleteOperation> processor;

        DefaultCommandInvocation(AeshCommandProcessorImpl<? extends Command, ? extends CommandInvocation, ? extends AeshCompleteOperation> processor) {
            this.processor = processor;
        }

        @Override
        public Shell getShell() {
            return shell;
        }

        @Override
        public void setPrompt(Prompt prompt) {
        }

        @Override
        public Prompt getPrompt() {
            return null;
        }

        @Override
        public String getHelpInfo(String commandName) {
            return processor.getHelpInfo(commandName);
        }

        @Override
        public void stop() {

        }

        @Override
        public AeshContext getAeshContext() {
            return processor.getAeshContext();
        }

        // XXX JFDENISE SHOULD BE REMOVED
        @Override
        public KeyAction input() throws InterruptedException {
            return null;
        }

        @Override
        public String inputLine() throws InterruptedException {
            return null;
        }

        @Override
        public String inputLine(Prompt prompt) throws InterruptedException {
            return null;
        }

        // XXX JFDENISE SHOULD BE REMOVED
        @Override
        public int pid() {
            return -1;
        }

        // XXX JFDENISE SHOULD BE REMOVED
        @Override
        public void putProcessInBackground() {
        }

        // XXX JFDENISE SHOULD BE REMOVED
        @Override
        public void putProcessInForeground() {
        }

        @Override
        public void executeCommand(String input) throws CommandNotFoundException,
                CommandLineParserException,
                OptionValidatorException,
                CommandValidatorException,
                CommandException,
                InterruptedException {
            processor.executeCommand(input);
        }

        @Override
        public void print(String msg) {
            shell.write(msg);
        }

        @Override
        public void println(String msg) {
            shell.writeln(msg);
        }

        @Override
        public Executor<? extends CommandInvocation> buildExecutor(String line) throws CommandNotFoundException,
                CommandLineParserException,
                OptionValidatorException,
                CommandValidatorException {
            return processor.buildExecutor(line);
        }

    }

    private final CommandRegistry<C> registry;
    private final CommandInvocationProvider<CI> commandInvocationProvider;
    private final InvocationProviders invocationProviders;

    private static final Logger LOGGER = Logger.getLogger(AeshCommandProcessorImpl.class.getName());
    private final CommandNotFoundHandler commandNotFoundHandler;

    private final CommandResolver<? extends Command> commandResolver;
    private final AeshContext ctx;

    AeshCommandProcessorImpl(AeshContext ctx,
            CommandRegistry<C> registry,
            CommandInvocationProvider<CI> commandInvocationProvider,
            CommandNotFoundHandler commandNotFoundHandler,
            CompleterInvocationProvider completerInvocationProvider,
            ConverterInvocationProvider converterInvocationProvider,
            ValidatorInvocationProvider validatorInvocationProvider,
            OptionActivatorProvider optionActivatorProvider,
            CommandActivatorProvider commandActivatorProvider) {
        this.ctx = ctx;
        this.registry = registry;
        commandResolver = new AeshCommandResolver<>(registry);
        this.commandInvocationProvider = commandInvocationProvider;
        this.commandNotFoundHandler = commandNotFoundHandler;
        this.invocationProviders
                = new AeshInvocationProviders(converterInvocationProvider, completerInvocationProvider,
                        validatorInvocationProvider, optionActivatorProvider, commandActivatorProvider);
        processAfterInit();
    }

    @Override
    public CommandRegistry<C> getCommandRegistry() {
        return registry;
    }

    @Override
    public AeshContext getAeshContext() {
        return ctx;
    }

    public String getHelpInfo(String commandName) {
        try (CommandContainer<? extends Command> commandContainer = commandResolver.resolveCommand(commandName)) {
            if (commandContainer != null) {
                return commandContainer.printHelp(commandName);
            }
        } catch (Exception e) { // ignored
        }
        return "";
    }

    @Override
    public void complete(CO completeOperation) {
        registry.completeCommandName(completeOperation);

        if (completeOperation.getCompletionCandidates().size() < 1) {
            try (CommandContainer<? extends Command> commandContainer = commandResolver.resolveCommand(completeOperation.getBuffer())) {

                CommandLineCompletionParser completionParser = commandContainer
                        .getParser().getCompletionParser();

                ParsedCompleteObject completeObject = completionParser
                        .findCompleteObject(completeOperation.getBuffer(),
                                completeOperation.getCursor());
                completeObject.getCompletionParser().injectValuesAndComplete(completeObject,
                        completeOperation, invocationProviders);
            } catch (Exception ex) {
                LOGGER.log(Level.FINER,
                        "Exception when completing: "
                        + completeOperation, ex);
            }
        }
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
                            new DefaultCommandInvocation(this)));

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
            // XXX JFDENISE, SHOULD BE REMOVED.
            if (commandNotFoundHandler != null) {
                commandNotFoundHandler.handleCommandNotFound(line, null);
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
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Exception while iterating commands.", e);
        }
    }

    @Override
    public Executor<CI> buildExecutor(String line) throws CommandNotFoundException,
            CommandLineParserException, OptionValidatorException,
            CommandValidatorException {
        // XXX JFDENISE, for now no OPERATORS
        Command<CI> c = getPopulatedCommand(line);
        CI ic = commandInvocationProvider.enhanceCommandInvocation(
                new DefaultCommandInvocation(this));
        Executor<CI> executor = new Executor<>(ic, c);
        return executor;
    }

    private Command<CI> getPopulatedCommand(String commandLine) throws CommandNotFoundException,
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
        return container.getParser().parsedCommand().getCommand();
    }
}
