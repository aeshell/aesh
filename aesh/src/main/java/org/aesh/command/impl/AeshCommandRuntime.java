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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandNotFoundHandler;
import org.aesh.command.CommandResolver;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.Execution;
import org.aesh.command.Executor;
import org.aesh.command.PipelineConfig;
import org.aesh.command.PipelineResult;
import org.aesh.command.StageOutcome;
import org.aesh.command.activator.CommandActivatorProvider;
import org.aesh.command.activator.OptionActivatorProvider;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.CompleterInvocationProvider;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.converter.ConverterInvocationProvider;
import org.aesh.command.impl.completer.CompleterData;
import org.aesh.command.impl.completer.FileOptionCompleter;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.impl.operator.PipeOperator;
import org.aesh.command.impl.parser.AeshCommandLineCompletionParser;
import org.aesh.command.impl.parser.AeshCommandLineParser;
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
import org.aesh.console.AeshContext;
import org.aesh.parser.LineParser;
import org.aesh.parser.ParsedLine;
import org.aesh.parser.ParsedWord;
import org.aesh.parser.ParserStatus;

/**
 * Implementation of the Command processor.
 *
 * @author Aesh team
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
    private volatile PipelineResult lastPipelineResult;
    private volatile PipelineConfig pipelineConfig = PipelineConfig.DEFAULT;

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
        this.invocationProviders = new AeshInvocationProviders(converterInvocationProvider, completerInvocationProvider,
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

    public PipelineConfig pipelineConfig() {
        return pipelineConfig;
    }

    public void setPipelineConfig(PipelineConfig pipelineConfig) {
        if (pipelineConfig != null)
            this.pipelineConfig = pipelineConfig;
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
        } catch (CommandLineParserException e) {
            throw e;
        } catch (CommandNotFoundException cmd) {
            if (commandNotFoundHandler != null) {
                commandNotFoundHandler.handleCommandNotFound(line,
                        msg -> commandInvocationBuilder.build(this, null, null).getShell().writeln(msg),
                        cmd.getCommandName(), registry.getAllCommandNames());
            }
            throw cmd;
        }
        return runExecutor(executor);
    }

    @Override
    public Executor<CI> buildExecutor(String commandName, String[] args) throws CommandNotFoundException,
            CommandLineParserException,
            IOException {
        return buildExecutorFromArgs(commandName, args);
    }

    @Override
    public CommandResult executeCommand(String commandName, String[] args) throws CommandNotFoundException,
            CommandLineParserException,
            CommandValidatorException,
            CommandException,
            InterruptedException,
            IOException {
        return runExecutor(buildExecutorFromArgs(commandName, args));
    }

    private Executor<CI> buildExecutorFromArgs(String commandName, String[] args)
            throws CommandNotFoundException, CommandLineParserException, IOException {
        // Build a display string for error messages
        StringBuilder displayLine = new StringBuilder(commandName);
        if (args != null) {
            for (String arg : args) {
                displayLine.append(' ').append(arg);
            }
        }

        // Create ParsedLine directly from pre-tokenized args, bypassing LineParser
        List<ParsedWord> words = new ArrayList<>();
        words.add(new ParsedWord(commandName, 0));
        int offset = commandName.length() + 1;
        if (args != null) {
            for (String arg : args) {
                words.add(new ParsedWord(arg, offset));
                offset += arg.length() + 1;
            }
        }
        ParsedLine parsedLine = new ParsedLine(displayLine.toString(), words,
                -1, -1, -1, ParserStatus.OK, "", OperatorType.NONE);

        try {
            List<Execution<CI>> executions = Executions.buildExecution(
                    Collections.singletonList(parsedLine), this);
            return new Executor<>(executions);
        } catch (CommandLineParserException e) {
            throw e;
        } catch (CommandNotFoundException cmd) {
            if (commandNotFoundHandler != null) {
                commandNotFoundHandler.handleCommandNotFound(displayLine.toString(),
                        msg -> commandInvocationBuilder.build(this, null, null).getShell().writeln(msg),
                        cmd.getCommandName(), registry.getAllCommandNames());
            }
            throw cmd;
        }
    }

    private CommandResult runExecutor(Executor<CI> executor) throws CommandException,
            CommandValidatorException, CommandLineParserException, InterruptedException {
        ExecutionPlanner<CI> planner = new ExecutionPlanner<>(executor.getExecutions());
        CommandResult result = null;
        ExecutionPlanner.Unit<CI> unit;
        while ((unit = planner.nextUnit()) != null) {
            List<Execution> pipeChain = new ArrayList<>(unit.executions());
            if (unit.isPipeline()) {
                result = executePipeChain(pipeChain);
            } else {
                result = executeSingle(pipeChain.get(0));
            }
        }
        if (result != null)
            return result;
        else
            return CommandResult.FAILURE;
    }

    /**
     * Execute a single non-piped command.
     */
    private CommandResult executeSingle(Execution exec) throws CommandException,
            CommandValidatorException, CommandLineParserException, InterruptedException {
        try {
            return exec.execute();
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
                exec.getResultHandler().onValidationFailure(CommandResult.INTERRUPTED, ex);
            }
            throw ex;
        } catch (Exception e) {
            if (exec.getResultHandler() != null) {
                exec.getResultHandler().onValidationFailure(CommandResult.FAILURE, e);
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Execute a pipe chain concurrently.
     * <p>
     * All stages except the last are submitted to a thread pool. The last stage
     * runs on the calling thread. The result of the last stage in the pipeline
     * is returned (matching Unix pipe semantics where the exit code is from the
     * last command).
     * <p>
     * If an upstream command fails, its output stream is closed, causing the
     * downstream command to receive EOF on its stdin. If a downstream command
     * finishes early (e.g., {@code head}), the upstream command's write will
     * get an IOException (pipe broken) which is suppressed like SIGPIPE.
     */
    @SuppressWarnings("unchecked")
    private CommandResult executePipeChain(List<Execution> chain) throws CommandException,
            CommandValidatorException, CommandLineParserException, InterruptedException {
        ExecutorService threadPool = PipeThreads.sharedPool();

        int stageCount = chain.size();
        String[] stageNames = new String[stageCount];
        for (int i = 0; i < stageCount; i++)
            stageNames[i] = PipelineStages.commandName(chain.get(i), i);
        Throwable[] stageErrors = new Throwable[stageCount];
        long[] stageDurations = new long[stageCount];

        List<Future<?>> futures = new ArrayList<>(chain.size() - 1);
        try {
            for (int i = 0; i < chain.size() - 1; i++) {
                final int stageIndex = i;
                Execution stage = chain.get(i);
                futures.add(threadPool.submit(() -> {
                    long start = System.currentTimeMillis();
                    try {
                        stage.execute();
                    } catch (Throwable e) {
                        stageErrors[stageIndex] = e;
                        if (PipeOperator.isPipeBroken(e))
                            stage.setResult(CommandResult.PIPE_BROKEN);
                        else
                            stage.setResult(CommandResult.FAILURE);
                    } finally {
                        stageDurations[stageIndex] = System.currentTimeMillis() - start;
                    }
                }));
            }

            // Run the last stage on the calling thread
            Execution lastStage = chain.get(chain.size() - 1);
            long lastStart = System.currentTimeMillis();
            CommandResult result;
            try {
                result = executeSingle(lastStage);
            } catch (CommandException | CommandValidatorException | CommandLineParserException
                    | InterruptedException | RuntimeException | Error e) {
                stageErrors[stageCount - 1] = e;
                throw e;
            } finally {
                stageDurations[stageCount - 1] = System.currentTimeMillis() - lastStart;
            }

            settleUpstream(chain, futures, stageErrors);

            recordPipelineResult(chain, stageNames, stageErrors, stageDurations, result);
            return result;
        } finally {
            for (Future<?> future : futures) {
                if (!future.isDone())
                    future.cancel(true);
            }
        }
    }

    private void settleUpstream(List<Execution> chain, List<Future<?>> futures,
            Throwable[] stageErrors) {
        long timeoutMs = pipelineConfig.upstreamJoinTimeoutMs();
        for (int i = 0; i < futures.size(); i++) {
            Future<?> future = futures.get(i);
            try {
                if (timeoutMs > 0)
                    future.get(timeoutMs, TimeUnit.MILLISECONDS);
                else
                    future.get();
            } catch (java.util.concurrent.ExecutionException e) {
                LOGGER.log(Level.FINE, "Upstream pipe stage failed", e.getCause());
            } catch (java.util.concurrent.TimeoutException | InterruptedException e) {
                if (e instanceof InterruptedException)
                    Thread.currentThread().interrupt();
                LOGGER.log(Level.FINE, "Upstream pipe stage join timed out", e);
                future.cancel(true);
                settleTimedOutStage(chain.get(i), stageErrors, i, e);
            }
        }
    }

    private void settleTimedOutStage(Execution stage, Throwable[] stageErrors, int index, Throwable timeout) {
        if (stage.getResult() == null) {
            if (stageErrors[index] == null)
                stageErrors[index] = timeout;
            stage.setResult(CommandResult.FAILURE);
        }
    }

    private void recordPipelineResult(List<Execution> chain, String[] stageNames,
            Throwable[] stageErrors, long[] stageDurations, CommandResult result) {
        List<StageOutcome> stages = new ArrayList<>(chain.size());
        for (int i = 0; i < chain.size(); i++) {
            Execution stage = chain.get(i);
            stages.add(new StageOutcome(i, chain.size(), stageNames[i], stage,
                    stage.getResult(), stageErrors[i], stageDurations[i]));
        }
        lastPipelineResult = new PipelineResult(result, stages);
    }

    /**
     * The outcome of the most recently executed pipeline on this runtime,
     * or null if the last execution was not a pipeline.
     * <p>
     * The returned {@link CommandResult} still follows Unix semantics (last
     * stage wins); per-stage results and errors are retained for diagnostics.
     */
    public PipelineResult lastPipelineResult() {
        return lastPipelineResult;
    }

    @Override
    public CommandResult executeCommand(String... lines) throws CommandNotFoundException, CommandLineParserException,
            OptionValidatorException, CommandValidatorException, CommandException, InterruptedException, IOException {
        if (lines == null || lines.length == 0)
            throw new CommandException("No input lines");
        CommandResult result = null;
        for (String line : lines) {
            result = executeCommand(line);
            // Stop on any non-zero exit code, consistent with POSIX and the
            // && operator which uses result.isSuccess() (#604)
            if (result != null && result.isFailure())
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
        CommandLineParser<CI> parser = registry.getCommand(commandName, "").getParser();
        parser.getProcessedCommand().updateInvocationProviders(invocationProviders);
        if (parser instanceof AeshCommandLineParser) {
            AeshCommandLineParser<CI> aeshParser = (AeshCommandLineParser<CI>) parser;
            aeshParser.storeInvocationProviders(invocationProviders);
            List<CommandLineParser<CI>> childParsers = aeshParser.getChildParsers();
            if (childParsers != null) {
                for (CommandLineParser<CI> child : childParsers) {
                    child.getProcessedCommand().updateInvocationProviders(invocationProviders);
                    if (child instanceof AeshCommandLineParser)
                        ((AeshCommandLineParser<CI>) child).storeInvocationProviders(invocationProviders);
                }
            }
        } else {
            List<CommandLineParser<CI>> childParsers = registry.getChildCommandParsers(commandName);
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
        return commandInvocationProvider
                .enhanceCommandInvocation(commandInvocationBuilder.build(this, config, commandContainer));
    }

    CommandContainer<CI> findCommandContainer(ParsedLine aeshLine) throws CommandNotFoundException {
        if (aeshLine.words().isEmpty()) {
            return null;
        }
        final String name = aeshLine.firstWord().word();
        CommandContainer<CI> container = commandResolver.resolveCommand(name, aeshLine.line());
        if (container == null) {
            throw new CommandNotFoundException("No command handler for '" + name + "'.", name);
        }
        container.addLine(aeshLine);
        return container;
    }

    void populateAskedOption(ProcessedOption option) {
        try {
            option.injectValueIntoField(option.parent().getCommand(), invocationProviders, getAeshContext(), false);
        } catch (OptionValidatorException e) {
            LOGGER.log(Level.WARNING,
                    "Trying to inject value: " + option.getValue() + ", into option: " + option.name() + " failed", e);
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
        if (operators.isEmpty())
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

        if (!lines.isEmpty()) {
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).cursor() > -1) {
                    if (i == 0) {
                        doSimpleComplete(completeOperation, lines.get(i));
                        return;
                    }
                    //we need to check the previous line
                    //if it is redirect/append out we should use a file completer
                    else {
                        if (OperatorType.isAppendOrRedirectInOrOut(lines.get(i - 1).operator())) {
                            //do file completion
                            FileOptionCompleter completer = new FileOptionCompleter();
                            CompleterInvocation invocation = new CompleterData(completeOperation.getContext(),
                                    lines.get(i).selectedWord().word(), null);
                            completer.complete(invocation);
                            completeOperation.addCompletionCandidatesTerminalString(invocation.getCompleterValues());
                            AeshCommandLineCompletionParser.verifyCompleteValue(completeOperation,
                                    invocation,
                                    lines.get(i).selectedWord().word(),
                                    lines.get(i).selectedWord().status(), null);
                            return;
                        } else {
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
        if ((parsedLine.selectedIndex() == 0 || //possible command name
                parsedLine.words().size() == 0) && ParserStatus.okForCompletion(parsedLine.status())) {
            commandResolver.getRegistry().completeCommandName(completeOperation, parsedLine);
        }
        if (completeOperation.getCompletionCandidates().size() < 1) {

            try (CommandContainer commandContainer = commandResolver.resolveCommand(parsedLine)) {

                commandContainer.getParser()
                        .complete(completeOperation, parsedLine, invocationProviders);
            } catch (CommandNotFoundException ignored) {
            } catch (Exception ex) {
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
