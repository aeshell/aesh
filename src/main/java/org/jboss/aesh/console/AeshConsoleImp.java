/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.CommandLineCompletionParser;
import org.jboss.aesh.cl.CommandLineParser;
import org.jboss.aesh.cl.ParsedCompleteObject;
import org.jboss.aesh.cl.ParserGenerator;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.console.reader.AeshPrintWriter;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.util.LoggerUtil;
import org.jboss.aesh.parser.Parser;
import org.jboss.aesh.util.ReflectionUtil;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshConsoleImp implements AeshConsole {

    private Map<CommandLineParser, Command> commands;
    private Console console;

    Logger logger = LoggerUtil.getLogger(AeshConsoleImp.class.getName());

    AeshConsoleImp(Settings settings) {
        commands = new HashMap<CommandLineParser, Command>();
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
    public void addCommand(Class<? extends Command> command) {
        if(verifyCommand(command)) {
            try {
                commands.put(ParserGenerator.generateCommandLineParser(command), ReflectionUtil.newInstance(command));
            } catch (CommandLineParserException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void addCommand(Command command) {
        if(verifyCommand(command)) {
            try {
                commands.put(ParserGenerator.generateCommandLineParser(command), command);
            } catch (CommandLineParserException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void removeCommand(Class<? extends Command> command) {
        if(verifyCommand(command)) {
            String commandName = command.getAnnotation(CommandDefinition.class).name();

            for(CommandLineParser parser : commands.keySet()) {
                if(parser.getCommand().getName().equals(commandName)) {
                    commands.remove(parser);
                    return;
                }
            }
        }
    }

    @Override
    public void removeCommand(Command command) {
        removeCommand(command.getClass());
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
    public void attachConsoleCommand(ConsoleCommand consoleCommand) {
        console.attachProcess(consoleCommand);
    }

    /*
    @Override
    public AeshInputStream in() {
        return null;
    }
    */

    private CommandLineParser findCommand(String input) {
        String name = Parser.findFirstWord(input);
        for(CommandLineParser parser : commands.keySet()) {
            logger.info("findCommand, commandName: "+parser.getCommand().getName()+", firstName: "+name);
            if(parser.getCommand().getName().equals(name))
                return parser;
        }

        return null;
    }

    private List<String> completeCommandName(String input) {
        List<String> matchedCommands = new ArrayList<String>();
        for(CommandLineParser parser : commands.keySet()) {
            if(parser.getCommand().getName().startsWith(input))
                matchedCommands.add(parser.getCommand().getName());
        }

        return matchedCommands;
    }

    private boolean verifyCommand(Class<? extends Command> command) {
        return command.getAnnotation(CommandDefinition.class) != null;
    }

    private boolean verifyCommand(Command command) {
        return command.getClass().getAnnotation(CommandDefinition.class) != null;
    }

    class AeshCompletion implements Completion {

        @Override
        public void complete(CompleteOperation completeOperation) {
            List<String> completedCommands = completeCommandName(completeOperation.getBuffer());
            if(completedCommands.size() > 0) {
                completeOperation.addCompletionCandidates(completedCommands);
            }
            else {
                CommandLineParser currentCommand = findCommand(completeOperation.getBuffer());
                if(currentCommand != null) {
                    CommandLineCompletionParser completionParser = new CommandLineCompletionParser(currentCommand);
                    try {

                        ParsedCompleteObject completeObject = completionParser.findCompleteObject(completeOperation.getBuffer());
                        logger.info("completeObject: "+completeObject);
                        completionParser.injectValuesAndComplete(completeObject, commands.get(currentCommand), completeOperation);

                    }
                    catch (CommandLineParserException e) {
                        logger.warning(e.getMessage());
                        //if(e instanceof ArgumentParserException)
                        //    logger.info("User trying to complete a command without arguments");
                    }
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
        public int readConsoleOutput(ConsoleOutput output) throws IOException {
            CommandResult result;
            if(output != null && output.getBuffer().trim().length() > 0) {
                CommandLineParser calledCommand = findCommand(output.getBuffer());
                if(calledCommand != null) {
                    try {
                        calledCommand.populateObject(commands.get(calledCommand), output.getBuffer());
                        result = commands.get(calledCommand).execute(console);
                    }
                    catch (CommandLineParserException e) {
                        console.out().println(e.getMessage());
                        result = CommandResult.FAILURE;
                    }
                }
                else {
                    console.out().print("Command not found: " + Parser.findFirstWord(output.getBuffer()) + Config.getLineSeparator());
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
