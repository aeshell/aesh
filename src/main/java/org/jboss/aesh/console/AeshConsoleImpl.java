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
package org.jboss.aesh.console;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.aesh.cl.internal.ProcessedCommand;

import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.parser.CommandLineCompletionParser;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.parser.ParsedCompleteObject;
import org.jboss.aesh.cl.result.ResultHandler;
import org.jboss.aesh.cl.validator.CommandValidatorException;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.activator.AeshOptionActivatorProvider;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.container.CommandContainerResult;
import org.jboss.aesh.console.command.invocation.AeshCommandInvocation;
import org.jboss.aesh.console.command.invocation.CommandInvocationProvider;
import org.jboss.aesh.console.command.invocation.CommandInvocationServices;
import org.jboss.aesh.console.command.registry.AeshInternalCommandRegistry;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.export.ExportManager;
import org.jboss.aesh.console.helper.ManProvider;
import org.jboss.aesh.console.man.Man;
import org.jboss.aesh.console.settings.CommandNotFoundHandler;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.parser.AeshLine;
import org.jboss.aesh.parser.Parser;
import org.jboss.aesh.readline.Prompt;
import org.jboss.aesh.readline.completion.CompleteOperation;
import org.jboss.aesh.readline.completion.Completion;
import org.jboss.aesh.util.LoggerUtil;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshConsoleImpl implements AeshConsole {

    private final CommandRegistry registry;
    private final CommandInvocationServices commandInvocationServices;
    private final InvocationProviders invocationProviders;

    private static final Logger LOGGER = LoggerUtil.getLogger(AeshConsoleImpl.class.getName());
    private final ManProvider manProvider;
    private final CommandNotFoundHandler commandNotFoundHandler;
    private AeshInternalCommandRegistry internalRegistry;
    private String commandInvocationProvider = CommandInvocationServices.DEFAULT_PROVIDER_NAME;

    AeshConsoleImpl(Settings settings ) {
        this.registry = settings.commandRegistry();
        this.commandInvocationServices = settings.commandInvocationServices();
        this.commandNotFoundHandler = settings.commandNotFoundHandler();
        this.manProvider = settings.manProvider();
        this.invocationProviders = new AeshInvocationProviders(settings);


        processAfterInit(settings);
    }

    @Override
    public void start() {
        //console.start();
    }

    @Override
    public void stop() {
        //console.stop();
    }

    @Override
    public CommandRegistry getCommandRegistry() {
        return registry;
    }

    @Override
    public void setPrompt(Prompt prompt) {

    }

    @Override
    public Prompt getPrompt() {
        return null;
    }

    @Override
    public void clear() {
        /*
        try {
            //console.clear();
        }
        catch (IOException ignored) {
        }
        */
    }

    @Override
    public String getHelpInfo(String commandName) {
        try (CommandContainer commandContainer = getCommand(commandName, "")) {
            if (commandContainer != null) {
                return commandContainer.printHelp(commandName);
            }
        } catch (Exception e) { // ignored
        }
        return "";
    }

    @Override
    public void setCurrentCommandInvocationProvider(String name) {
        this.commandInvocationProvider = name;
    }

    @Override
    public void registerCommandInvocationProvider(String name,
        CommandInvocationProvider commandInvocationProvider) {
        commandInvocationServices.registerProvider(name,
            commandInvocationProvider);
    }

    @Override
    public ManProvider getManProvider() {
        return manProvider;
    }

    @Override
    public AeshContext getAeshContext() {
        return null;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public ExportManager getExportManager() {
        return null;
    }

    public String getBuffer() {
        return null;
    }

    public void putProcessInBackground(int pid) {
    }

    public void putProcessInForeground(int pid) {
    }

    public void execute(String input) throws InterruptedException {
    }

    public void push(String input) {
    }

    public void changeOutputStream(PrintStream output) {
    }

    private void processAfterInit(Settings settings) {
        if (settings.manEnabled()) {
            internalRegistry = new AeshInternalCommandRegistry();
            internalRegistry.addCommand(new Man(manProvider));
        }
        try {
            for (String commandName : registry.getAllCommandNames()) {
                ProcessedCommand cmd = registry.getCommand(commandName, "").getParser().getProcessedCommand();
                List<CommandLineParser<?>> childParsers = registry.getChildCommandParsers(commandName);
                if(!(invocationProviders.getOptionActivatorProvider() instanceof AeshOptionActivatorProvider)) {
                    //we have a custom OptionActivatorProvider, and need to process all options
                    cmd.updateInvocationProviders(invocationProviders);
                    for (CommandLineParser<?> child : childParsers) {
                        child.getProcessedCommand().updateInvocationProviders(invocationProviders);
                    }
                }
                if(!settings.isAnsiConsole()) {
                    cmd.updateSettings(settings);
                    for (CommandLineParser<?> child : childParsers) {
                        child.getProcessedCommand().updateSettings(settings);
                    }
                }
            }
        }
        catch(Exception e) {
        }

    }

    private void completeCommandName(CompleteOperation co) {
        registry.completeCommandName(co);
        if(internalRegistry != null) {
            for (String internalCommand : internalRegistry.getAllCommandNames())
                if (internalCommand.startsWith(co.getBuffer()))
                    co.addCompletionCandidate(internalCommand);
        }
    }

    /**
     * try to return the command in the given registry if the given registry do not find the command, check if we have a
     * internal registry and if its there.
     *
     * @param aeshLine parsed command line
     * @param line command line
     * @return command
     * @throws CommandNotFoundException
     */
    private CommandContainer getCommand(AeshLine aeshLine, String line) throws CommandNotFoundException {
        return getCommand(aeshLine.getWords().get(0), line);
    }

    /**
     * try to return the command in the given registry if the given registry do
     * not find the command, check if we have a internal registry and if its
     * there.
     *
     * @param commandName command name
     * @param line command line
     * @return command
     * @throws CommandNotFoundException
     */
    private CommandContainer getCommand(String commandName, String line) throws CommandNotFoundException {
        try {
            return registry.getCommand(commandName, line);
            //return commandContainer;
        } catch (CommandNotFoundException e) {
            // Lookup in aliases
            try {
                return registry.getCommandByAlias(commandName);
            } catch (CommandNotFoundException e2) {
                if (internalRegistry != null) {
                    CommandContainer cc = internalRegistry.getCommand(commandName);
                    if (cc != null) {
                        return cc;
                    }
                }
                throw e;
            }
        }
    }

    /*
    class AeshCompletion implements Completion {

        @Override
        public void complete(CompleteOperation completeOperation) {
            completeCommandName(completeOperation);
            if (completeOperation.getCompletionCandidates().size() < 1) {

                AeshLine aeshLine = Parser.findAllWords(completeOperation.getBuffer());
                try (CommandContainer commandContainer = getCommand( aeshLine, completeOperation.getBuffer())) {

                    CommandLineCompletionParser completionParser = commandContainer
                        .getParser().getCompletionParser();

                    ParsedCompleteObject completeObject = completionParser
                            .findCompleteObject(completeOperation.getBuffer(),
                                    completeOperation.getCursor());
                    completeObject.getCompletionParser().injectValuesAndComplete(completeObject, completeOperation, invocationProviders);
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

    }
        */

    class AeshConsoleCallbackImpl extends AeshConsoleCallback {

        private final AeshConsoleImpl console;
        private CommandResult result;

        AeshConsoleCallbackImpl(AeshConsoleImpl aeshConsole) {
            this.console = aeshConsole;
        }

        @Override
        @SuppressWarnings("unchecked")
        public int execute(ConsoleOperation output) throws InterruptedException {
            /*
            if (output != null && output.getBuffer().trim().length() > 0) {
                ResultHandler resultHandler = null;
                AeshLine aeshLine = Parser.findAllWords(output.getBuffer());
                try (CommandContainer commandContainer = getCommand( aeshLine, output.getBuffer())) {
                    resultHandler = commandContainer.getParser().getProcessedCommand().getResultHandler();
                    CommandContainerResult ccResult =
                            commandContainer.executeCommand(aeshLine, invocationProviders, getAeshContext(),
                            commandInvocationServices.getCommandInvocationProvider(
                                    commandInvocationProvider).enhanceCommandInvocation(
                                    new AeshCommandInvocation(console,
                                        output.getControlOperator(),
                                         output.getPid(), this)));

                    result = ccResult.getCommandResult();

                    if(result == CommandResult.SUCCESS && resultHandler != null)
                        resultHandler.onSuccess();
                    else if(resultHandler != null)
                        resultHandler.onFailure(result);
                }
                catch (CommandLineParserException | CommandValidatorException | OptionValidatorException e) {
                    getShell().out().println(e.getMessage());
                    result = CommandResult.FAILURE;
                    if(resultHandler != null)
                        resultHandler.onValidationFailure(result, e);
                }
                catch (CommandNotFoundException cnfe) {
                    getShell().out().println(cnfe.getMessage());
                    result = CommandResult.FAILURE;
                    if(commandNotFoundHandler != null)
                        commandNotFoundHandler.handleCommandNotFound(output.getBuffer(), getShell());
                } catch (CommandException cmd) {
                    getShell().out().println(cmd.getMessage());
                    result = CommandResult.FAILURE;
                    if (resultHandler != null) {
                        resultHandler.onExecutionFailure(result, cmd);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if(e instanceof InterruptedException)
                        throw (InterruptedException) e;
                    else {
                        LOGGER.log(Level.SEVERE, "Exception when parsing/running: "
                                + output.getBuffer(), e);
                        getShell().out().println(
                                "Exception when parsing/running: "
                                        + output.getBuffer() + ", "
                                        + e.getMessage()
                        );
                        result = CommandResult.FAILURE;
                        if (resultHandler != null)
                            resultHandler.onValidationFailure(result, e);
                    }
                }
            }
            // empty line
            else if (output != null) {
                result = CommandResult.FAILURE;
            }
            else {
                stop();
                result = CommandResult.FAILURE;
            }

            if (result == CommandResult.SUCCESS) {
                return 0;
            } else {
                return 1;
            }
        */
            return 0;
        }
    }
}
