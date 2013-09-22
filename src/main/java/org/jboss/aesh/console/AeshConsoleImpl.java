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
import java.util.logging.Logger;

import org.jboss.aesh.cl.parser.CommandLineCompletionParser;
import org.jboss.aesh.cl.parser.ParsedCompleteObject;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.console.command.CommandContainer;
import org.jboss.aesh.console.command.CommandInvocationImpl;
import org.jboss.aesh.console.command.CommandInvocationProvider;
import org.jboss.aesh.console.command.CommandInvocationServices;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.CommandRegistry;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.ConsoleCommand;
import org.jboss.aesh.console.reader.AeshPrintWriter;
import org.jboss.aesh.console.reader.AeshStandardStream;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.terminal.Shell;
import org.jboss.aesh.util.LoggerUtil;
import org.jboss.aesh.parser.Parser;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshConsoleImpl implements AeshConsole {

    private Console console;
    private CommandRegistry registry;
    private CommandInvocationServices commandInvocationServices;

    Logger logger = LoggerUtil.getLogger(AeshConsoleImpl.class.getName());
    private String commandInvocationProvider = CommandInvocationServices.DEFAULT_PROVIDER_NAME;

    AeshConsoleImpl(Settings settings, CommandRegistry registry,
                    CommandInvocationServices commandInvocationServices) {
        this.registry = registry;
        this.commandInvocationServices = commandInvocationServices;
        //commands = new HashMap<CommandLineParser, Command>();
        console = new Console(settings);
        console.setConsoleCallback(new AeshConsoleCallback(this));
        console.addCompletion(new AeshCompletion());
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
    public AeshPrintWriter err() {
        return console.err();
    }

    @Override
    public AeshPrintWriter out() {
        return console.out();
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
    public AeshStandardStream in() {
        return console.in();
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

    private List<String> completeCommandName(String input) {
        List<String> matchedCommands = new ArrayList<String>();
        for(String commandName : registry.getAllCommandNames()) {
            if(commandName.startsWith(input))
                matchedCommands.add(commandName);
        }

        return matchedCommands;
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
                    CommandContainer commandContainer = registry.getCommand(
                            Parser.findFirstWord(completeOperation.getBuffer()), completeOperation.getBuffer());
                    CommandLineCompletionParser completionParser = new CommandLineCompletionParser(commandContainer.getParser());

                    ParsedCompleteObject completeObject = completionParser.findCompleteObject(completeOperation.getBuffer());
                    completionParser.injectValuesAndComplete(completeObject, commandContainer.getCommand(), completeOperation);
                }
                catch (CommandLineParserException e) {
                    logger.warning(e.getMessage());
                    //if(e instanceof ArgumentParserException)
                    //    logger.info("User trying to complete a command without arguments");
                }
                catch (CommandNotFoundException ignored) {
                }
            }
        }

    }

    class AeshConsoleCallback implements ConsoleCallback {

        private AeshConsole console;
        AeshConsoleCallback(AeshConsole aeshConsole) {
            this.console = aeshConsole;
        }
        @Override
        public int readConsoleOutput(ConsoleOperation output) throws IOException {
            CommandResult result = CommandResult.SUCCESS;
            if(output != null && output.getBuffer().trim().length() > 0) {
                //CommandLineParser calledCommandParser = findCommand(output.getBuffer());
                try {
                    CommandContainer commandContainer = registry.getCommand(
                            Parser.findFirstWord(output.getBuffer()), output.getBuffer());
                    //calledCommand.populateObject(commands.get(calledCommand), output.getBuffer());
                    commandContainer.getParser().populateObject(commandContainer.getCommand(), output.getBuffer());
                    result = commandContainer.getCommand().execute(
                            commandInvocationServices.getCommandInvocationProvider(commandInvocationProvider)
                                    .enhanceCommandInvocation( new CommandInvocationImpl( console, output.getControlOperator())));
                }
                catch (CommandLineParserException e) {
                    console.out().println(e.getMessage());
                    result = CommandResult.FAILURE;
                }
                catch (CommandNotFoundException e) {
                    console.out().print("Command not found: " + Parser.findFirstWord(output.getBuffer()) + Config.getLineSeparator());
                    result = CommandResult.FAILURE;
                }
                catch (OptionValidatorException e) {
                    console.out().println(e.getMessage());
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
