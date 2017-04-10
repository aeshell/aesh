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
package org.aesh.command.impl.parser;

import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.parser.OptionParserException;
import org.aesh.command.parser.RequiredOptionException;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.command.Command;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.console.AeshContext;
import org.aesh.parser.LineParser;
import org.aesh.parser.ParsedLineIterator;
import org.aesh.parser.ParsedWord;
import org.aesh.util.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple command line parser.
 * It parses a given string based on the Command given and
 *
 * It can also print a formatted usage/help information.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandLineParser<C extends Command> implements CommandLineParser<C> {

    private final ProcessedCommand<C> processedCommand;
    private List<CommandLineParser<C>> childParsers;
    private boolean isChild = false;
    private ProcessedOption lastParsedOption;
    private boolean parsedCommand = false;
    private final LineParser lineParser;
    private CompleteStatus completeStatus;

    public AeshCommandLineParser(ProcessedCommand<C> processedCommand) {
        this.processedCommand = processedCommand;
        lineParser = new LineParser();
    }

    @Override
    public void addChildParser(CommandLineParser<C> commandLineParser) {
        if(childParsers == null)
            childParsers = new ArrayList<>();
        commandLineParser.setChild(true);
        childParsers.add(commandLineParser);
    }

    public List<CommandLineParser<C>> getChildParsers() {
        return childParsers;
    }

    @Override
    public void setChild(boolean child) {
        isChild = child;
    }

    @Override
    public CommandLineParser<C> parsedCommand() {
        if(parsedCommand)
            return this;
        else if(isGroupCommand()) {
            CommandLineParser<C> correct;
            for(CommandLineParser<C> child : childParsers) {
                correct = child.parsedCommand();
                if(correct != null)
                    return correct;
            }
        }
        return null;
    }

    @Override
    public List<String> getAllNames() {
        if (isGroupCommand()) {
            List<CommandLineParser<C>> parsers = getChildParsers();
            List<String> names = new ArrayList<>(parsers.size());
            for (CommandLineParser child : parsers) {
                names.add(processedCommand.name()+" "+child.getProcessedCommand().name());
            }
            return names;
        }
        else {
            List<String> names = new ArrayList<>(1);
            names.add(processedCommand.name());
            return names;
        }
    }

    public boolean isChild() {
        return isChild;
    }

    @Override
    public CommandLineParser<C> getChildParser(String name) {
        if(!isGroupCommand())
            return null;
        for (CommandLineParser clp : getChildParsers()) {
            if(clp.getProcessedCommand().name().equals(name))
                return clp;
        }
        return null;
    }

    @Override
    public List<CommandLineParser<C>> getAllChildParsers() {
        if(isGroupCommand())
            return getChildParsers();
        else
           return new ArrayList<>();
    }

    @Override
    public ProcessedCommand<C> getProcessedCommand() {
        return processedCommand;
    }

    @Override
    public C getCommand() {
        return processedCommand.getCommand();
    }

    @Override
    public CommandLineCompletionParser getCompletionParser() {
        return new AeshCommandLineCompletionParser(this);
    }

    @Override
    public CommandPopulator<Object, C> getCommandPopulator() {
        return processedCommand.getCommandPopulator();
    }

    @Override
    public void populateObject(String line, InvocationProviders invocationProviders, AeshContext aeshContext, Mode mode) throws CommandLineParserException, OptionValidatorException {
        //first parse, then populate
        parse(line, mode);
        if(mode == Mode.VALIDATE && getProcessedCommand().parserExceptions().size() > 0) {
            throw getProcessedCommand().parserExceptions().get(0);
        }
        else {
            getCommandPopulator().populateObject(processedCommand, invocationProviders, aeshContext, mode);
            if(isGroupCommand()) {
                for(CommandLineParser parser : getChildParsers()) {
                    parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, mode);
                }
            }
        }
    }

    /**
     * Returns a usage String based on the defined command and options.
     * Useful when printing "help" info etc.
     */
    @Override
    public String printHelp() {
        List<CommandLineParser<C>> parsers = getChildParsers();
        if (parsers != null && parsers.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(processedCommand.printHelp())
                    .append(Config.getLineSeparator())
                    .append(processedCommand.name())
                    .append(" commands:")
                    .append(Config.getLineSeparator());
            for (CommandLineParser child : parsers)
                sb.append("    ").append(child.getProcessedCommand().name()).append(Config.getLineSeparator());

            return sb.toString();
        }
        else
            return processedCommand.printHelp();
    }

    /**
     * Parse a command line with the defined command as base of the rules.
     * If any options are found, but not defined in the command object an
     * CommandLineParserException will be thrown.
     * Also, if a required option is not found or options specified with value,
     * but is not given any value an OptionParserException will be thrown.
     *
     * @param line input
     */
    @Override
    public void parse(String line) {
        parse(line, Mode.STRICT);
    }

    @Override
    public void parse(ParsedLineIterator iterator, Mode mode) {
        clear();
        if(iterator.hasNextWord()) {
            String command = iterator.pollWord();
            if (processedCommand.name().equals(command)
                    || processedCommand.getAliases().contains(command)) {
                if(isGroupCommand() && iterator.hasNextWord()) {
                   CommandLineParser<C> clp = getChildParser(iterator.peekWord());
                    if(clp == null)
                        doParse(iterator, mode);
                    //we have a group command
                    else {
                        //remove the child name
                        clp.parse(iterator, mode);
                    }
                }
                else
                    doParse(iterator, mode);
            }
        }
        else if(iterator.parserError() != null)
            processedCommand.addParserException(new CommandLineParserException(iterator.parserError()));
    }


    private void doParse(ParsedLineIterator iter, Mode mode) {
        parsedCommand = true;
        if(mode == Mode.COMPLETION)
            doParseCompletion(iter);
        else {
            try {
                while (iter.hasNextWord()) {
                    ParsedWord word = iter.peekParsedWord();
                    lastParsedOption = processedCommand.searchAllOptions(word.word());
                    if (lastParsedOption != null) {
                        lastParsedOption.parser().parse(iter, lastParsedOption);
                    }
                    else {
                        if (processedCommand.hasArgument()) {
                            processedCommand.getArgument().addValue(word.word());
                        }
                        else {
                            processedCommand.addParserException(
                                    new OptionParserException("A value " + word.word() +
                                            " was given as an argument, but the command do not support it."));
                        }
                        iter.pollParsedWord();
                    }

                }
            }
            catch (OptionParserException ope) {
                processedCommand.addParserException(ope);
            }
            if (mode == Mode.STRICT) {
                RequiredOptionException re = checkForMissingRequiredOptions(processedCommand);
                if (re != null)
                    processedCommand.addParserException(re);
            }
        }
    }

    private void doParseCompletion(ParsedLineIterator iter) {
        if(!iter.hasNextWord()) {
            //we list all the options
        }
        else {
            while(iter.hasNextWord()) {
                try {
                    ParsedWord word = iter.peekParsedWord();
                    lastParsedOption = processedCommand.searchAllOptions(word.word());
                    if (lastParsedOption != null) {
                        lastParsedOption.parser().parse(iter, lastParsedOption);
                        if(!iter.hasNextWord())
                           processedCommand.setCompleteStatus(new CompleteStatus(CompleteStatus.Status.COMPLETE_OPTION, ""));
                    }
                    //got a partial option
                    else if(word.word().startsWith("--")) {
                        processedCommand.setCompleteStatus( new CompleteStatus(CompleteStatus.Status.LONG_OPTION, word.word().substring(2)));
                        iter.pollParsedWord();
                    }
                    else if(word.word().startsWith("-")) {
                        processedCommand.setCompleteStatus( new CompleteStatus(CompleteStatus.Status.SHORT_OPTION, word.word().substring(1)));
                        iter.pollParsedWord();
                    }
                    //we're completing an argument
                    else {
                        processedCommand.setCompleteStatus( new CompleteStatus(CompleteStatus.Status.ARGUMENT, word.word()));
                        iter.pollParsedWord();
                    }
                }
                catch (OptionParserException e) {
                    //TODO: needs to be improved
                    //ignored for now
                    processedCommand.setCompleteStatus(new CompleteStatus(CompleteStatus.Status.OPTION_MISSING_VALUE, ""));
                }
            }

        }
    }

    private RequiredOptionException checkForMissingRequiredOptions(ProcessedCommand<C> command) {
        for(ProcessedOption o : command.getOptions())
            if(o.isRequired() && o.getValue() == null) {
                boolean found = false;
                for(ProcessedOption po : command.getOptions()) {
                    if(po.getValue() != null && po.doOverrideRequired()) {
                        found = true;
                        break;
                    }
                    /*
                    else if(po.doOverrideRequired()) {
                        found = true;
                        break;
                    }
                    */
                }
                if(!found)
                    return new RequiredOptionException("Option: "+o.getDisplayName()+" is required for this command.");
            }
        return null;
    }

    @Override
    public ProcessedOption lastParsedOption() {
        return lastParsedOption;
    }

    /**
     * Parse a command line with the defined command as base of the rules.
     * If any options are found, but not defined in the command object an
     * CommandLineParserException will be thrown.
     * Also, if a required option is not found or options specified with value,
     * but is not given any value an CommandLineParserException will be thrown.
     *
     * @param line input
     * @param mode parser mode
     */
    @Override
    public void parse(String line, Mode mode) {
        parse(lineParser.parseLine(line, line.length()).iterator(), mode);
    }

    @Override
    public void clear() {
        //if this is the parsed command, clear it
        if(parsedCommand) {
            processedCommand.clear();
            lastParsedOption = null;
            parsedCommand = false;
            completeStatus = null;
        }
        //else find the parsed command and clear that one
        else {
            CommandLineParser parsed = parsedCommand();
            if (parsed != null)
                parsed.clear();
        }
    }

    @Override
    public boolean isGroupCommand() {
        List<CommandLineParser<C>> parsers = getChildParsers();
        return parsers != null && parsers.size() > 0;
    }

    @Override
    public String toString() {
        return "AeshCommandLineParser{" +
                "processedCommand=" + processedCommand +
                ", childParsers=" + childParsers +
                ", isChild=" + isChild +
                ", lastParsedOption=" + lastParsedOption +
                ", parsedCommand=" + parsedCommand +
                ", completeStatus=" + completeStatus +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AeshCommandLineParser)) return false;

        AeshCommandLineParser that = (AeshCommandLineParser) o;

        return processedCommand.equals(that.processedCommand);

    }

    @Override
    public int hashCode() {
        return processedCommand.hashCode();
    }
}
