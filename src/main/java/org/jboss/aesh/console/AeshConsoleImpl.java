/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.parser.CommandLineCompletionParser;
import org.jboss.aesh.cl.parser.ParsedCompleteObject;
import org.jboss.aesh.cl.result.ResultHandler;
import org.jboss.aesh.cl.validator.CommandValidatorException;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.activator.AeshOptionActivatorProvider;
import org.jboss.aesh.console.command.activator.OptionActivatorProvider;
import org.jboss.aesh.console.command.completer.CompleterInvocationProvider;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.container.CommandContainerResult;
import org.jboss.aesh.console.command.converter.ConverterInvocationProvider;
import org.jboss.aesh.console.command.invocation.AeshCommandInvocation;
import org.jboss.aesh.console.command.invocation.CommandInvocationProvider;
import org.jboss.aesh.console.command.invocation.CommandInvocationServices;
import org.jboss.aesh.console.command.registry.AeshInternalCommandRegistry;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.command.validator.ValidatorInvocationProvider;
import org.jboss.aesh.console.export.ExportManager;
import org.jboss.aesh.console.helper.ManProvider;
import org.jboss.aesh.console.man.Man;
import org.jboss.aesh.console.settings.CommandNotFoundHandler;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.parser.AeshLine;
import org.jboss.aesh.parser.Parser;
import org.jboss.aesh.terminal.Shell;
import org.jboss.aesh.util.LoggerUtil;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshConsoleImpl implements AeshConsole {

    private final Console console;
    private final CommandRegistry registry;
    private final CommandInvocationServices commandInvocationServices;
    private final InvocationProviders invocationProviders;

    private static final Logger LOGGER = LoggerUtil.getLogger(AeshConsoleImpl.class.getName());
    private final ManProvider manProvider;
    private final CommandNotFoundHandler commandNotFoundHandler;
    private AeshInternalCommandRegistry internalRegistry;
    private String commandInvocationProvider = CommandInvocationServices.DEFAULT_PROVIDER_NAME;

    AeshConsoleImpl(Settings settings, CommandRegistry registry,
        CommandInvocationServices commandInvocationServices,
        CommandNotFoundHandler commandNotFoundHandler,
        CompleterInvocationProvider completerInvocationProvider,
        ConverterInvocationProvider converterInvocationProvider,
        ValidatorInvocationProvider validatorInvocationProvider,
        OptionActivatorProvider optionActivatorProvider,
        ManProvider manProvider) {
        this.registry = registry;
        this.commandInvocationServices = commandInvocationServices;
        this.commandNotFoundHandler = commandNotFoundHandler;
        this.manProvider = manProvider;
        this.invocationProviders =
            new AeshInvocationProviders(converterInvocationProvider, completerInvocationProvider,
                validatorInvocationProvider, optionActivatorProvider);

        console = new Console(settings);
        console.setConsoleCallback(new AeshConsoleCallbackImpl(this));
        console.addCompletion(new AeshCompletion());
        processAfterInit(settings);
    }

    @Override
    public void start() {
        console.start();
    }

    @Override
    public void stop() {
        console.stop();
    }

    @Override
    public CommandRegistry getCommandRegistry() {
        return registry;
    }

    @Override
    public void setPrompt(Prompt prompt) {
        console.setPrompt(prompt);
    }

    @Override
    public Prompt getPrompt() {
        return console.getPrompt();
    }

    @Override
    public Shell getShell() {
        return console.getShell();
    }

    @Override
    public void clear() {
        try {
            console.clear();
        }
        catch (IOException ignored) {
        }
    }

    @Override
    public String getHelpInfo(String commandName) {
        try (CommandContainer commandContainer = registry.getCommand(commandName, "")) {
            if (commandContainer != null)
                return commandContainer.getParser().printHelp();
        }
        catch (Exception e) { // ignored
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
        return console.getAeshContext();
    }

    @Override
    public boolean isRunning() {
        return console.isRunning();
    }

    @Override
    public ExportManager getExportManager() {
        return console.getExportManager();
    }

    public String getBuffer() {
        return console.getBuffer();
    }

    public InputProcessor getInputProcessor() {
        return console.getInputProcessor();
    }

    public void putProcessInBackground(int pid) {
        console.putProcessInBackground(pid);
    }

    public void putProcessInForeground(int pid) {
        console.putProcessInForeground(pid);
    }

    public void execute(String input) {
        console.pushToInputStream(input);
    }

    private void processAfterInit(Settings settings) {
        if (settings.isManEnabled()) {
            internalRegistry = new AeshInternalCommandRegistry();
            internalRegistry.addCommand(new Man(manProvider));
        }
        if(!(invocationProviders.getOptionActivatorProvider() instanceof AeshOptionActivatorProvider)) {
            //we have a custom OptionActivatorProvider, and need to process all options
            try {
                for (String commandName : registry.getAllCommandNames()) {
                    registry.getCommand(commandName, "").getParser().getProcessedCommand().processAfterInit(invocationProviders);
                }
            }
            catch (CommandNotFoundException e) {
                e.printStackTrace();
            }


        }
    }

    private List<String> completeCommandName(String input) {
        List<String> matchedCommands = registry.findAllCommandNames(input);
        if(matchedCommands == null)
            matchedCommands = new ArrayList<>();
        for(String internalCommand : internalRegistry.getAllCommandNames())
            if(internalCommand.startsWith(input))
                matchedCommands.add(internalCommand);

        return matchedCommands;

        /*

        try {
            Set<String> allCommandNames = new TreeSet<>();
            Set<String> registryCommandNames = registry.getAllCommandNames();
            if(registryCommandNames != null)
                allCommandNames.addAll(registryCommandNames);
            if (internalRegistry != null) {
                allCommandNames.addAll(internalRegistry.getAllCommandNames());
            }
            for (String commandName : allCommandNames) {
                if (commandName.startsWith(input))
                    matchedCommands.add(commandName);
            }
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Error retrieving command names from CommandRegistry", e);
        }

        return matchedCommands;
        */
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
        try {
            return registry.getCommand(aeshLine.getWords().get(0), line);
            //return commandContainer;
        }
        catch (CommandNotFoundException e) {
            if (internalRegistry != null) {
                CommandContainer cc = internalRegistry.getCommand(aeshLine.getWords().get(0));
                if (cc != null)
                    return cc;
            }
            throw e;
        }
    }

    class AeshCompletion implements Completion {

        @Override
        public void complete(CompleteOperation completeOperation) {
            List<String> completedCommands = completeCommandName(completeOperation.getBuffer());
            if (completedCommands != null && completedCommands.size() > 0) {
                completeOperation.addCompletionCandidates(completedCommands);
            }
            else {
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

    class AeshConsoleCallbackImpl extends AeshConsoleCallback {

        private final AeshConsoleImpl console;
        private CommandResult result;

        AeshConsoleCallbackImpl(AeshConsoleImpl aeshConsole) {
            this.console = aeshConsole;
        }

        @Override
        @SuppressWarnings("unchecked")
        public int execute(ConsoleOperation output) throws InterruptedException {
            if (output != null && output.getBuffer().trim().length() > 0) {
                ResultHandler resultHandler = null;
                AeshLine aeshLine = Parser.findAllWords(output.getBuffer());
                try (CommandContainer commandContainer = getCommand( aeshLine, output.getBuffer())) {
                    CommandContainerResult ccResult =
                            commandContainer.executeCommand(aeshLine, invocationProviders, getAeshContext(),
                            commandInvocationServices.getCommandInvocationProvider(
                                    commandInvocationProvider).enhanceCommandInvocation(
                                    new AeshCommandInvocation(console,
                                        output.getControlOperator(),
                                         output.getPid(), this)));

                    result = ccResult.getCommandResult();
                    resultHandler = ccResult.getResultHandler();

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
                }
                catch (Exception e) {
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
        }
    }
}
