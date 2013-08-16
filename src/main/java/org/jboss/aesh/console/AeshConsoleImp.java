/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
import org.jboss.aesh.cl.completer.CompleterData;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.util.LoggerUtil;
import org.jboss.aesh.util.Parser;
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
        console.setConsoleCallback(new AeshConsoleCallback());
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
                commands.put(ParserGenerator.generateCommandLineParser(command), checkForInjectConsole( ReflectionUtil.newInstance(command)));
            } catch (CommandLineParserException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void removeCommand(Class<? extends Command> command) {

    }
    
    @Override
    public PrintWriter err() {
    	return console.getStdErr();
    }
    
    @Override
    public PrintWriter out() {
    	return console.getStdOut();
    }

    @Override
    public void setPrompt(Prompt prompt) {
        try {
            console.setPrompt(prompt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private CommandLineParser findCommand(String input) {
        String name = Parser.findFirstWord(input);
        for(CommandLineParser parser : commands.keySet()) {
            logger.info("findCommand, commandName: "+parser.getParameter().getName()+", firstName: "+name);
            if(parser.getParameter().getName().equals(name))
                return parser;
        }

        return null;
    }

    private List<String> completeCommandName(String input) {
        List<String> matchedCommands = new ArrayList<String>();
        for(CommandLineParser parser : commands.keySet()) {
            if(parser.getParameter().getName().startsWith(input))
                matchedCommands.add(parser.getParameter().getName());
        }

        return matchedCommands;
    }

    private boolean verifyCommand(Class<? extends Command> command) {
        return command.getAnnotation(CommandDefinition.class) != null;
    }

    private Command checkForInjectConsole(Command command) {
        for(Field field : command.getClass().getDeclaredFields()) {
            if(field.getType().equals(AeshConsole.class) && field.getAnnotation(InjectConsole.class) != null) {
                if(!Modifier.isPublic(field.getModifiers()))
                    field.setAccessible(true);
                try {
                    field.set(command, this);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return command;
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
                        CompleterData completerData = completionParser.injectValuesAndComplete(completeObject, commands.get(currentCommand),
                                completeOperation.getBuffer());
                        completeOperation.addCompletionCandidates(completerData.getCompleterValues());
                        if(completerData.getCompleterValues().size() == 1 && completerData.getOffset() > 0)
                            completeOperation.setOffset( completerData.getOffset());
                        //completeOperation.setOffset( completeOperation.getCursor());
                        if(!completerData.isAppendSpace())
                            completeOperation.doAppendSeparator(false);

                    } catch (CommandLineParserException e) {
                        e.printStackTrace();
                    }
                }
                //try to complete the current command
            }
        }

    }

    class AeshConsoleCallback implements ConsoleCallback {
        @Override
        public int readConsoleOutput(ConsoleOutput output) throws IOException {
            if(output != null && output.getBuffer().trim().length() > 0) {
                CommandLineParser calledCommand = findCommand(output.getBuffer());
                if(calledCommand != null) {
                    try {
                        calledCommand.populateObject(commands.get(calledCommand), output.getBuffer());
                    }
                    catch (CommandLineParserException e) {
                        e.printStackTrace();
                    }
                    commands.get(calledCommand).execute();
                }
                else
                    console.pushToStdOut("Command not found: "+Parser.findFirstWord(output.getBuffer())+Config.getLineSeparator());
            }
            //empty line
            else if(output != null) {
                return 0;
            }
            else {
                stop();
            }

            return 0;
        }
    }
}
