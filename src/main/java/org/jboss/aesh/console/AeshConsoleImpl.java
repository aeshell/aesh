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
import org.jboss.aesh.cl.validator.CommandValidatorException;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.console.command.invocation.AeshCommandInvocation;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.invocation.CommandInvocationProvider;
import org.jboss.aesh.console.command.invocation.CommandInvocationServices;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.registry.AeshInternalCommandRegistry;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.ConsoleCommand;
import org.jboss.aesh.console.helper.ManProvider;
import org.jboss.aesh.console.man.Man;
import org.jboss.aesh.console.settings.CommandNotFoundHandler;
import org.jboss.aesh.console.settings.Settings;
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

    private final Logger logger = LoggerUtil.getLogger(AeshConsoleImpl.class.getName());
    private final ManProvider manProvider;
    private final CommandNotFoundHandler commandNotFoundHandler;
    private AeshInternalCommandRegistry internalRegistry;
    private String commandInvocationProvider = CommandInvocationServices.DEFAULT_PROVIDER_NAME;

    AeshConsoleImpl(Settings settings, CommandRegistry registry,
                    CommandInvocationServices commandInvocationServices,
                    CommandNotFoundHandler commandNotFoundHandler,
                    ManProvider manProvider) {
        this.registry = registry;
        this.commandInvocationServices = commandInvocationServices;
        this.commandNotFoundHandler = commandNotFoundHandler;
        this.manProvider = manProvider;
        console = new Console(settings);
        console.setConsoleCallback(new AeshConsoleCallback(this));
        console.addCompletion(new AeshCompletion());
        processSettings(settings);
    }

    @Override
    public void start() {
        try {
            console.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            console.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CommandRegistry getCommandRegistry() {
        return registry;
    }

    @Override
    public void setPrompt(Prompt prompt) {
        try {
            console.setPrompt(prompt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Prompt getPrompt() {
        return console.getPrompt();
    }

    @Override
    public void attachConsoleCommand(ConsoleCommand consoleCommand) {
        console.attachProcess(consoleCommand);
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
        CommandContainer commandContainer = null;
        try {
            commandContainer = registry.getCommand(commandName, "");
        }
        catch (CommandNotFoundException e) {
            e.printStackTrace();
        }
        if(commandContainer != null)
            return commandContainer.getParser().printHelp();
        else
            return "";
    }

    @Override
    public void setCurrentCommandInvocationProvider(String name) {
        this.commandInvocationProvider = name;
    }

    @Override
    public void registerCommandInvocationProvider(String name, CommandInvocationProvider commandInvocationProvider) {
        commandInvocationServices.registerProvider(name, commandInvocationProvider);
    }

    @Override
    public ManProvider getManProvider() {
        return manProvider;
    }

    public String getBuffer() {
        if(console != null)
            return console.getBuffer();
        else
            return "";
    }

    private void processSettings(Settings settings) {
        if(settings.isManEnabled()) {
            internalRegistry = new AeshInternalCommandRegistry();
            internalRegistry.addCommand(new Man(manProvider));
        }
    }

    private List<String> completeCommandName(String input) {
        List<String> matchedCommands = new ArrayList<String>();
        try {
            for(String commandName : registry.getAllCommandNames()) {
                if(commandName.startsWith(input))
                    matchedCommands.add(commandName);
            }
            if(internalRegistry != null) {
                for(String commandName : internalRegistry.getAllCommandNames())
                    if(commandName.startsWith(input))
                        matchedCommands.add(commandName);
            }
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Error retrieving command names from CommandRegistry",e);
        }

        return matchedCommands;
    }

    /**
     * try to return the command in the given registry
     * if the given registry do not find the command, check if we have a
     * internal registry and if its there.
     * @param name command name
     * @param line command line
     * @return command
     * @throws CommandNotFoundException
     */
    private CommandContainer getCommand(String name, String line) throws CommandNotFoundException {
        try {
            return registry.getCommand(name, line);
        }
        catch (CommandNotFoundException e) {
            if(internalRegistry != null) {
                CommandContainer cc = internalRegistry.getCommand(name);
                if(cc != null)
                    return cc;
            }
            throw e;
        }
    }

    class AeshCompletion implements Completion {

        @Override
        public void complete(CompleteOperation completeOperation) {
            List<String> completedCommands = completeCommandName(completeOperation.getBuffer());
            if(completedCommands.size() > 0) {
                completeOperation.addCompletionCandidates(completedCommands);
            }
            else {
                try {
                    CommandContainer commandContainer =
                            getCommand( Parser.findFirstWord(completeOperation.getBuffer()), completeOperation.getBuffer());
                    CommandLineCompletionParser completionParser = commandContainer.getParser().getCompletionParser();

                    ParsedCompleteObject completeObject = completionParser.findCompleteObject(completeOperation.getBuffer());
                    completionParser.injectValuesAndComplete(completeObject, commandContainer.getCommand(), completeOperation);
                }
                catch (CommandLineParserException e) {
                    logger.warning(e.getMessage());
                }
                catch (CommandNotFoundException ignored) {
                    if(commandNotFoundHandler != null)
                        commandNotFoundHandler.handleCommandNotFound(completeOperation.getBuffer(), getShell());
                }
                catch (Exception ex) {
                    logger.log(Level.SEVERE, "Runtime exception when completing: "+completeOperation, ex);
                }
            }
        }

    }

    class AeshConsoleCallback implements ConsoleCallback {

        private final AeshConsole console;
        AeshConsoleCallback(AeshConsole aeshConsole) {
            this.console = aeshConsole;
        }
        @Override
        public int readConsoleOutput(ConsoleOperation output) throws IOException {
            CommandResult result = CommandResult.SUCCESS;
            if(output != null && output.getBuffer().trim().length() > 0) {
                try {
                    CommandContainer commandContainer =
                            getCommand( Parser.findFirstWord(output.getBuffer()), output.getBuffer());

                    commandContainer.getParser().getCommandPopulator().populateObject(commandContainer.getCommand(),
                            commandContainer.getParser().parse(output.getBuffer()));
                    //validate the command before execute
                    commandContainer.getParser().getCommand().getValidator().validate(commandContainer.getCommand());
                    result = commandContainer.getCommand().execute(
                            commandInvocationServices.getCommandInvocationProvider(commandInvocationProvider)
                                    .enhanceCommandInvocation(new AeshCommandInvocation(console, output.getControlOperator())));
                }
                catch (CommandLineParserException e) {
                    console.getShell().out().println(e.getMessage());
                    result = CommandResult.FAILURE;
                }
                catch (CommandNotFoundException e) {
                    if(commandNotFoundHandler != null) {
                        commandNotFoundHandler.handleCommandNotFound(output.getBuffer(), getShell());
                    }
                    else {
                        console.getShell().out().print("Command not found: " + Parser.findFirstWord(output.getBuffer()) + Config.getLineSeparator());
                    }
                    result = CommandResult.FAILURE;
                }
                catch (OptionValidatorException e) {
                    console.getShell().out().println(e.getMessage());
                    result = CommandResult.FAILURE;
                }
                catch(CommandValidatorException e) {
                    console.getShell().out().println(e.getMessage());
                    result = CommandResult.FAILURE;
                }
                catch (Exception e) {
                    logger.log(Level.SEVERE, "Exception when parsing/running: "+output.getBuffer(), e);
                    result = CommandResult.FAILURE;
                }
            }
            //empty line
            else if(output != null) {
                result = CommandResult.FAILURE;
            }
            else {
                stop();
                result = CommandResult.FAILURE;
            }

            if(result == CommandResult.SUCCESS)
                return 0;
            else
                return 1;
        }
    }
}
