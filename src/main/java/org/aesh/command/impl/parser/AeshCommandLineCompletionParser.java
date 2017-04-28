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

import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.completer.CompleterData;
import org.aesh.command.impl.completer.DefaultValueOptionCompleter;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.Command;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.console.AeshContext;
import org.aesh.parser.LineParser;
import org.aesh.parser.ParsedLine;
import org.aesh.parser.ParsedLineIterator;
import org.aesh.parser.ParsedWord;
import org.aesh.readline.completion.CompleteOperation;
import org.aesh.readline.terminal.formatting.TerminalString;
import org.aesh.util.Parser;

import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandLineCompletionParser<C extends Command> implements CommandLineCompletionParser {

    private final AeshCommandLineParser<C> parser;
    private final LineParser lineParser;

    public AeshCommandLineCompletionParser(AeshCommandLineParser<C> parser) {
        this.parser = parser;
        lineParser = new LineParser();
    }


    /**
     * 1. find the last "word"
     *   if it starts with '-', we need to check if its a value or name
     * @param line buffer
     * @return ParsedCompleteObject
     */
    @Override
    public ParsedCompleteObject findCompleteObject(String line, int cursor) throws CommandLineParserException {
        if(cursor < line.length()) {
            line = line.substring(0, cursor);
        }

        parser.clear();
        //first we check if it could be a param
        if(Parser.findIfWordEndWithSpace(line)) {
            return endsWithSpace(line);
        }
        //lastly we'll check if we can find an option
        else
            return optionFinder(line);
    }

    private ParsedCompleteObject endsWithSpace(String line) throws CommandLineParserException {
        parser.parse(line, CommandLineParser.Mode.NONE);
        //check if we try to complete just after the command name
        if(parser.isGroupCommand()) {
            if (line.trim().equals(parser.getProcessedCommand().name() + " " +
                    parser.getProcessedCommand().name())) {
                if (parser.getProcessedCommand().getArgument() == null) {
                    //basically an empty string except command name
                    return new ParsedCompleteObject(true, "", 0, parser.getCompletionParser());
                } else if (parser.getProcessedCommand().getArgument().activator().isActivated(parser.getProcessedCommand())) {
                    return new ParsedCompleteObject(null, "", parser.getProcessedCommand().getArgument().type(),
                            false, getCorrectCompletionParser(line));
                }
                //basically an empty string except command name
                return new ParsedCompleteObject(true, "", 0, parser.getCompletionParser());
            }
        }
        else if(line.trim().equals(parser.getProcessedCommand().name())) {
            if(parser.getProcessedCommand().getArgument() == null) {
                //basically an empty string except command name
                return new ParsedCompleteObject(true, "", 0, parser.getCompletionParser());
            } else if (parser.getProcessedCommand().getArgument().activator().isActivated(parser.getProcessedCommand())) {
                return new ParsedCompleteObject(null, "", parser.getProcessedCommand().getArgument().type(),
                        false, getCorrectCompletionParser(line));
            }
            //basically an empty string except command name
            return new ParsedCompleteObject(true, "", 0, parser.getCompletionParser());
        }
        //else we try to complete an option,an option value or arguments
        String lastWord = Parser.findEscapedSpaceWordCloseToEnd(line.trim());
        if(lastWord.startsWith("-")) {
            int offset = lastWord.length();
            while(lastWord.startsWith("-"))
                lastWord = lastWord.substring(1);
            if(lastWord.length() == 0)
                return new ParsedCompleteObject(false, null, offset, getCorrectCompletionParser(line));
            else if(parser.parsedCommand().getProcessedCommand().findOptionNoActivatorCheck(lastWord) != null ||
                    parser.parsedCommand().getProcessedCommand().findLongOptionNoActivatorCheck(lastWord) != null)
                return findCompleteObjectValue(line, true);
            else
                return new ParsedCompleteObject(false, null, offset, getCorrectCompletionParser(line));
        }
        //last word is a value, need to find out what option its a value for
        else {
            return findCompleteObjectValue(line, true);
        }
    }

    private ParsedCompleteObject optionFinder(String line) throws CommandLineParserException {
        String lastWord = Parser.findEscapedSpaceWordCloseToEnd(line);
        //last word might be an option
        if(lastWord.startsWith("-") && !lastWord.contains("=")) {
            String secLastWord =
                    Parser.findEscapedSpaceWordCloseToEnd(
                            line.substring(0,line.length()-lastWord.length()));
            //second to last word also start with -
            if(secLastWord.startsWith("-")) {
                //do this for now
                return findCompleteObjectValue(line, false);
            }
            //the last word is an option (most likely)
            else {
                switch (lastWord) {
                    case "-":
                        return new ParsedCompleteObject(true, "", 1, getCorrectCompletionParser(line));
                    case "--":
                        return new ParsedCompleteObject(true, "", 2, getCorrectCompletionParser(line));
                    default:
                        //we have a complete shortName
                        if (!lastWord.startsWith("--") && lastWord.length() == 2)
                            return new ParsedCompleteObject(true,
                                    Parser.trimOptionName(lastWord), lastWord.length(), true, getCorrectCompletionParser(line));
                        else {
                            String optionName = Parser.trimOptionName(lastWord);
                            parser.parse(line, CommandLineParser.Mode.NONE);
                            if (parser.parsedCommand().getProcessedCommand().hasUniqueLongOption(optionName))
                                return new ParsedCompleteObject(true, optionName, lastWord.length(), true, parser.parsedCommand().getCompletionParser());
                            else
                                return new ParsedCompleteObject(true, optionName, lastWord.length(), false, parser.parsedCommand().getCompletionParser());
                        }
                }
            }
        }
        else
            return findCompleteObjectValue(line, false);
    }

    /**
     * Only called when we know that the last word is an option value
     * If endsWithSpace is true we set the value to an empty string to indicate a value
     */
    private ParsedCompleteObject findCompleteObjectValue(String line, boolean endsWithSpace) throws CommandLineParserException {
        parser.parse(line, CommandLineParser.Mode.NONE);
        CommandLineParser correctParser = parser.parsedCommand();
        if(correctParser == null)
            correctParser = parser;
        ProcessedCommand<? extends Command> cl = correctParser.getProcessedCommand();

        //the last word is an argument
        if(cl.getArgument() != null && !cl.getArgument().getValues().isEmpty()) {
            return new ParsedCompleteObject("", endsWithSpace ? "" :
                    cl.getArgument().getValues().get(cl.getArgument().getValues().size() - 1),
                    cl.getArgument().type(), false, correctParser.getCompletionParser());
        }
        //get the last option
        //else if (cl.hasOptionsWithInjectedValues()) {
        else if (correctParser.lastParsedOption() != null) {
            ProcessedOption po = correctParser.lastParsedOption();
            //options ends with a separator and thus status should be set accordingly
            if(po.getEndsWithSeparator())
                endsWithSpace = true;

            if(endsWithSpace && po.getValue() != null &&  po.getValue().length() > 0 &&
                    (po.getOptionType() == OptionType.NORMAL || po.getOptionType() == OptionType.BOOLEAN)) {
                if(cl.getArgument() == null)
                    return new ParsedCompleteObject(true, "", 0, correctParser.getCompletionParser());
                else
                    return new ParsedCompleteObject(true, correctParser.getCompletionParser());
            }
            else if(po.isLongNameUsed() || (po.shortName() == null || po.shortName().length() < 1))
                return new ParsedCompleteObject(po.name(),
                        endsWithSpace ? "" : po.getValues().get(po.getValues().size()-1),
                        po.type(), true, correctParser.getCompletionParser());
            else
                return new ParsedCompleteObject( po.shortName(),
                        endsWithSpace ? "" : po.getValues().get(po.getValues().size()-1),
                        po.type(), true, correctParser.getCompletionParser());
        }
        //probably something wrong with the parser
        else
            return new ParsedCompleteObject(true, "", 0, getCorrectCompletionParser(line));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void injectValuesAndComplete(ParsedCompleteObject completeObject,
                                        AeshCompleteOperation completeOperation,
                                        InvocationProviders invocationProviders) {

        if(completeObject.doDisplayOptions()) {
            //got the whole name, just add a space
            if(completeObject.isCompleteOptionName()) {
                completeOperation.addCompletionCandidate("");
            }
            else {
                //we have partial/full name
                if(completeObject.getName() != null && completeObject.getName().length() > 0) {
                    String rest = completeOperation.getBuffer().substring(0, completeOperation.getBuffer().lastIndexOf( completeObject.getName()));
                    ParsedLine parsedLine = lineParser.parseLine(rest);
                    try {
                        //parser.parse(parsedLine.iterator(), true);
                        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders,
                                completeOperation.getContext(), CommandLineParser.Mode.NONE);
                    }
                    //this should be ignored at some point
                    catch (CommandLineParserException | OptionValidatorException ignored) { }
                    List<TerminalString> optionNamesWithDash = parser.getProcessedCommand().findPossibleLongNamesWitdDash(completeObject.getName());
                    if(optionNamesWithDash.size() > 0) {
                        //only one param
                        if(optionNamesWithDash.size() == 1) {
                            completeOperation.addCompletionCandidate( optionNamesWithDash.get(0));
                            completeOperation.setOffset( completeOperation.getCursor() - 2 - completeObject.getName().length());
                        }
                        //multiple params
                        else {
                            completeOperation.addCompletionCandidatesTerminalString(optionNamesWithDash);
                            completeOperation.setOffset( completeOperation.getCursor() - 2 - completeObject.getName().length());
                        }

                    }
                }
                else {
                    try {
                        ParsedLine parsedLine = lineParser.parseLine(completeOperation.getBuffer());
                        if(parsedLine.words().get(parsedLine.words().size()-1).word().equals("--") ||
                                parsedLine.words().get(parsedLine.words().size()-1).word().equals("-"))
                            parsedLine.words().remove(parsedLine.words().size()-1);
                        if(parser.isChild())
                            parsedLine.words().remove(0);
                        parser.parse(parsedLine.iterator(), CommandLineParser.Mode.NONE);
                        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(),
                                invocationProviders, completeOperation.getContext(), CommandLineParser.Mode.NONE);
                    }
                    //this should be ignored at some point
                    catch (CommandLineParserException | OptionValidatorException ignored) { }
                    List<TerminalString> optionNamesWithDash = parser.getProcessedCommand().getOptionLongNamesWithDash();

                    if(optionNamesWithDash.size() > 1)
                        completeOperation.addCompletionCandidatesTerminalString(optionNamesWithDash);
                    else if(optionNamesWithDash.size() == 1) {
                        int count = 0;
                        if(completeOperation.getCursor() < completeOperation.getBuffer().length()) {
                            String line = completeOperation.getBuffer().substring(0, completeOperation.getCursor());
                            while(line.substring(0, line.length()-count).endsWith("-"))
                                count++;
                        }
                        else {
                            while(completeOperation.getBuffer().substring(0, completeOperation.getBuffer().length()-count).endsWith("-"))
                                count++;
                        }
                        completeOperation.addCompletionCandidate(optionNamesWithDash.get(0));
                        completeOperation.setOffset( completeOperation.getCursor() - count);
                    }

                }
            }
        }
        //complete option value
        else if(completeObject.isOption()) {
            ProcessedOption currentOption = parser.getProcessedCommand().findOption(completeObject.getName());
            if(currentOption == null)
                currentOption = parser.getProcessedCommand().findLongOptionNoActivatorCheck(completeObject.getName());

            //split the line on the option name. populate the object, then call the options completer
            String displayName = currentOption.getDisplayName();
            //this shouldnt happen
            if(displayName == null) {
                return;
            }
            String rest = completeOperation.getBuffer().substring(0, completeOperation.getBuffer().lastIndexOf( displayName));

            try {
                //no validation for now when we populate for completion
                parser.getCommandPopulator().populateObject(parser.getProcessedCommand(),
                        invocationProviders, completeOperation.getContext(), CommandLineParser.Mode.NONE);
            }
            //this should be ignored at some point
            catch (CommandLineParserException | OptionValidatorException ignored) { }

            if(currentOption.completer() != null &&
                    currentOption.activator().isActivated(parser.getProcessedCommand())) {
                CompleterInvocation completions =
                        invocationProviders.getCompleterProvider().enhanceCompleterInvocation(
                                new CompleterData(completeOperation.getContext(), completeObject.getValue(), parser.getCommand()));

                currentOption.completer().complete(completions);
                completeOperation.addCompletionCandidatesTerminalString(completions.getCompleterValues());
                completionSetOffSet(completeObject, completeOperation, completions);
                completeOperation.setIgnoreOffset(completions.doIgnoreOffset());
                completeOperation.setIgnoreStartsWith(completions.isIgnoreStartsWith());

                if(completions.getCompleterValues().size() == 1) {
                    //if the contain spaces we need to add the number of spaces to the size
                    // of the value.length since they are chopped off during parsing
                    if(completeObject.getValue().indexOf(Parser.SPACE_CHAR) > 0) {
                        completeOperation.setOffset( completeOperation.getCursor() -
                                (completeObject.getOffset() + Parser.findNumberOfSpacesInWord(completeObject.getValue())));
                    }
                    if(completeOperation.getCompletionCandidates().get(0).containSpaces())
                        completeOperation.getCompletionCandidates().get(0).switchSpacesToEscapedSpaces();

                    completeOperation.doAppendSeparator( completions.isAppendSpace());
                }
            }
            //only try to complete default values if completer is null
            else if(currentOption.getDefaultValues().size() > 0) {
                CompleterInvocation completions =
                        invocationProviders.getCompleterProvider().enhanceCompleterInvocation(
                                new CompleterData(completeOperation.getContext(), completeObject.getValue(), parser.getCommand()));
                new DefaultValueOptionCompleter(currentOption.getDefaultValues()).complete(completions);
                completeOperation.addCompletionCandidatesTerminalString(completions.getCompleterValues());
                completionSetOffSet(completeObject, completeOperation, completions);
                completeOperation.setIgnoreOffset(completions.doIgnoreOffset());
                completeOperation.setIgnoreStartsWith(completions.isIgnoreStartsWith());

                if(completions.getCompleterValues().size() == 1) {
                    //if the contain spaces we need to add the number of spaces to the size
                    // of the value.length since they are chopped off during parsing
                    if(completeObject.getValue().indexOf(Parser.SPACE_CHAR) > 0) {
                        completeOperation.setOffset( completeOperation.getCursor() -
                                (completeObject.getOffset() + Parser.findNumberOfSpacesInWord(completeObject.getValue())));
                    }
                    if(completeOperation.getCompletionCandidates().get(0).containSpaces())
                        completeOperation.getCompletionCandidates().get(0).switchSpacesToEscapedSpaces();

                    completeOperation.doAppendSeparator( completions.isAppendSpace());
                }
            }
        }
        else if(completeObject.isArgument()) {
            String lastWord = Parser.findEscapedSpaceWordCloseToEnd(completeOperation.getBuffer());
            String rest = completeOperation.getBuffer().substring(0, completeOperation.getBuffer().length() - lastWord.length());
            try {
                ParsedLine parsedLine = lineParser.parseLine(rest);
                parser.parse(parsedLine.iterator(), CommandLineParser.Mode.NONE);
                parser.getCommandPopulator().populateObject(parser.getProcessedCommand(),
                        invocationProviders, completeOperation.getContext(), CommandLineParser.Mode.NONE);
            }
            catch (CommandLineParserException | OptionValidatorException ignored) { }

            if(parser.getProcessedCommand().getArgument() != null &&
                    parser.getProcessedCommand().getArgument().completer() != null) {
                CompleterInvocation completions =
                        invocationProviders.getCompleterProvider().enhanceCompleterInvocation(
                                new CompleterData(completeOperation.getContext(), completeObject.getValue(), parser.getCommand()));
                parser.getProcessedCommand().getArgument().completer().complete(completions);
                completeOperation.addCompletionCandidatesTerminalString(completions.getCompleterValues());
                completionSetOffSet(completeObject, completeOperation, completions);
                completeOperation.setIgnoreOffset(completions.doIgnoreOffset());
                completeOperation.setIgnoreStartsWith(completions.isIgnoreStartsWith());

                if(completions.getCompleterValues().size() == 1) {
                    if(completeObject.getValue().indexOf(Parser.SPACE_CHAR) > 0) {
                        completeOperation.setOffset( completeOperation.getCursor() -
                                (completeObject.getOffset() + Parser.findNumberOfSpacesInWord(completeObject.getValue())));
                    }
                    if(completeOperation.getCompletionCandidates().get(0).containSpaces())
                        completeOperation.getCompletionCandidates().get(0).switchSpacesToEscapedSpaces();

                    completeOperation.doAppendSeparator( completions.isAppendSpace());
                }

            }
            else if(parser.getProcessedCommand().containsArgumentWithDefaultValues()) {
                CompleterInvocation completions =
                        invocationProviders.getCompleterProvider().enhanceCompleterInvocation(
                                new CompleterData(completeOperation.getContext(), completeObject.getValue(), parser.getCommand()));
                new DefaultValueOptionCompleter( parser.getProcessedCommand().getArgument().getDefaultValues()).complete(completions);
                completeOperation.addCompletionCandidatesTerminalString(completions.getCompleterValues());
                completionSetOffSet(completeObject, completeOperation, completions);
                completeOperation.setIgnoreOffset(completions.doIgnoreOffset());
                completeOperation.setIgnoreStartsWith(completions.isIgnoreStartsWith());

                if(completions.getCompleterValues().size() == 1) {
                    if(completeObject.getValue().indexOf(Parser.SPACE_CHAR) > 0) {
                        completeOperation.setOffset( completeOperation.getCursor() -
                                (completeObject.getOffset() + Parser.findNumberOfSpacesInWord(completeObject.getValue())));
                    }
                    if(completeOperation.getCompletionCandidates().get(0).containSpaces())
                        completeOperation.getCompletionCandidates().get(0).switchSpacesToEscapedSpaces();

                }
                completeOperation.doAppendSeparator( completions.isAppendSpace());
            }
        }
    }

    @Override
    public void injectValuesAndComplete(AeshCompleteOperation completeOperation, InvocationProviders invocationProviders,
                                        ParsedLine line) {
        //first inject values in command
        doInjectValues(invocationProviders, completeOperation.getContext());

        //we have parsed one or more options and their values
        if(parser.getProcessedCommand().completeStatus().status().equals(CompleteStatus.Status.COMPLETE_OPTION)) {
            //space and end, we display other options/arguments or option value if the option have a list of values
            if (line.spaceAtEnd()) {
                if(parser.lastParsedOption() != null) {
                        if (parser.lastParsedOption().getValue() == null ||
                                parser.lastParsedOption().hasMultipleValues()) {
                            //need to complete option value
                        }
                        //complete argument or options if the command do not have any args defined
                        //complete options if there are no arguments, else complete arguments
                        else {
                            if(parser.getProcessedCommand().hasArgument()) {
                                //complete argument
                            }
                            else {
                                //list options
                            }
                        }
                }
                //complete argument
                else {
                    completeArgument(completeOperation, invocationProviders, line);
                }
            }
            //no space means we should try to complete the value of the last parsed option
            else {
                //first we need to remove the value for the option
                String value = parser.lastParsedOption().getValue();

                if (parser.lastParsedOption().completer() != null &&
                        parser.lastParsedOption().activator().isActivated(parser.getProcessedCommand())) {
                    CompleterInvocation completions =
                            invocationProviders.getCompleterProvider().enhanceCompleterInvocation(
                                    new CompleterData(completeOperation.getContext(), value, parser.getCommand()));

                    parser.lastParsedOption().completer().complete(completions);
                    completeOperation.addCompletionCandidatesTerminalString(completions.getCompleterValues());
                    //completionSetOffSet(completeObject, completeOperation, completions);
                    completeOperation.setOffset(completeOperation.getCursor() - value.length());
                    completeOperation.setIgnoreOffset(completions.doIgnoreOffset());
                    completeOperation.setIgnoreStartsWith(completions.isIgnoreStartsWith());

                    if (completions.getCompleterValues().size() == 1) {
                        //if the contain spaces we need to add the number of spaces to the size
                        // of the value.length since they are chopped off during parsing
                        if (value.indexOf(Parser.SPACE_CHAR) > 0) {
                            completeOperation.setOffset(completeOperation.getCursor() -
                                    (Parser.findNumberOfSpacesInWord(value)));
                        }
                        if (completeOperation.getCompletionCandidates().get(0).containSpaces())
                            completeOperation.getCompletionCandidates().get(0).switchSpacesToEscapedSpaces();

                        completeOperation.doAppendSeparator(completions.isAppendSpace());
                    }
                }
            }
        }
        //partial long option name, contains atleast --
        else if(parser.getProcessedCommand().completeStatus().status().equals(CompleteStatus.Status.LONG_OPTION)) {
            //not option value so we'll complete all options not already listed
            if(parser.getProcessedCommand().completeStatus().value().length() == 0) {
                List<TerminalString> optionNamesWithDash = parser.getProcessedCommand().getOptionLongNamesWithDash();
                    if(optionNamesWithDash.size() > 1)
                        completeOperation.addCompletionCandidatesTerminalString(optionNamesWithDash);
                    else if(optionNamesWithDash.size() == 1) {
                        /*
                        int count = 0;
                        if(completeOperation.getCursor() < completeOperation.getBuffer().length()) {
                            String line = completeOperation.getBuffer().substring(0, completeOperation.getCursor());
                            while(line.substring(0, line.length()-count).endsWith("-"))
                                count++;
                        }
                        else {
                            while(completeOperation.getBuffer().substring(0, completeOperation.getBuffer().length()-count).endsWith("-"))
                                count++;
                        }
                        */
                        completeOperation.addCompletionCandidate(optionNamesWithDash.get(0));
                        //completeOperation.setOffset( completeOperation.getCursor() - count);
                        completeOperation.setOffset( completeOperation.getCursor());
                    }

            }

        }
        //partial short option, contains atleast -
        else if(parser.getProcessedCommand().completeStatus().status().equals(CompleteStatus.Status.SHORT_OPTION)) {
            //if we do not have any value, we add another -
            if(parser.getProcessedCommand().completeStatus().value().length() == 0) {
                        completeOperation.addCompletionCandidate("-");
                        //completeOperation.setOffset( completeOperation.getCursor() - count);
                        completeOperation.setOffset( completeOperation.getCursor());
            }
        }
        //we have an option, but no value
        else if(parser.getProcessedCommand().completeStatus().status().equals(CompleteStatus.Status.OPTION_MISSING_VALUE)) {
            //we need to complete a value
            doCompleteOptionValue(invocationProviders, completeOperation);

        }

        else if(parser.getProcessedCommand().completeStatus().status().equals(CompleteStatus.Status.ARGUMENT)) {

        }

    }

    private void doCompleteOptionValue(InvocationProviders invocationProviders, AeshCompleteOperation completeOperation) {
        ProcessedOption currentOption = parser.lastParsedOption();
        String value = parser.getProcessedCommand().completeStatus().value();
        if(currentOption.completer() != null &&
                currentOption.activator().isActivated(parser.getProcessedCommand())) {
            CompleterInvocation completions =
                    invocationProviders.getCompleterProvider().enhanceCompleterInvocation(
                                new CompleterData(completeOperation.getContext(), value, parser.getCommand()));

                currentOption.completer().complete(completions);
                completeOperation.addCompletionCandidatesTerminalString(completions.getCompleterValues());
                calcOffset(value, completeOperation, completions);
                completeOperation.setIgnoreOffset(completions.doIgnoreOffset());
                completeOperation.setIgnoreStartsWith(completions.isIgnoreStartsWith());

                if(completions.getCompleterValues().size() == 1) {
                    //if the contain spaces we need to add the number of spaces to the size
                    // of the value.length since they are chopped off during parsing
                    if(value.indexOf(Parser.SPACE_CHAR) > 0) {
                        completeOperation.setOffset( completeOperation.getCursor() -
                                (completeOperation.getOffset() + Parser.findNumberOfSpacesInWord(value)));
                    }
                    if(completeOperation.getCompletionCandidates().get(0).containSpaces())
                        completeOperation.getCompletionCandidates().get(0).switchSpacesToEscapedSpaces();

                    completeOperation.doAppendSeparator( completions.isAppendSpace());
                }
            }
            //only try to complete default values if completer is null
            else if(currentOption.getDefaultValues().size() > 0) {
                CompleterInvocation completions =
                        invocationProviders.getCompleterProvider().enhanceCompleterInvocation(
                                new CompleterData(completeOperation.getContext(), value, parser.getCommand()));
                new DefaultValueOptionCompleter(currentOption.getDefaultValues()).complete(completions);
                completeOperation.addCompletionCandidatesTerminalString(completions.getCompleterValues());
                calcOffset(value, completeOperation, completions);
                completeOperation.setIgnoreOffset(completions.doIgnoreOffset());
                completeOperation.setIgnoreStartsWith(completions.isIgnoreStartsWith());

                if(completions.getCompleterValues().size() == 1) {
                    //if the contain spaces we need to add the number of spaces to the size
                    // of the value.length since they are chopped off during parsing
                    if(value.indexOf(Parser.SPACE_CHAR) > 0) {
                        completeOperation.setOffset( completeOperation.getCursor() -
                                (completeOperation.getOffset() + Parser.findNumberOfSpacesInWord(value)));
                    }
                    if(completeOperation.getCompletionCandidates().get(0).containSpaces())
                        completeOperation.getCompletionCandidates().get(0).switchSpacesToEscapedSpaces();

                    completeOperation.doAppendSeparator( completions.isAppendSpace());
                }
            }

    }

    private void doInjectValues(InvocationProviders invocationProviders, AeshContext context) {
        try {
            //no validation for now when we populate for completion
            parser.getCommandPopulator().populateObject(parser.getProcessedCommand(),
                    invocationProviders, context, CommandLineParser.Mode.NONE);
        }
        //this should be ignored at some point
        catch (CommandLineParserException | OptionValidatorException ignored) {
        }
    }

    private void completeArgument(AeshCompleteOperation completeOperation,
                                  InvocationProviders invocationProviders, ParsedLine parsedLine) {
        ParsedWord lastWord = parsedLine.selectedWord();
        try {
            parser.getCommandPopulator().populateObject(parser.getProcessedCommand(),
                    invocationProviders, completeOperation.getContext(), CommandLineParser.Mode.NONE);
        }
        catch (CommandLineParserException | OptionValidatorException ignored) { }

        if(parser.getProcessedCommand().getArgument() != null &&
                parser.getProcessedCommand().getArgument().completer() != null) {
            CompleterInvocation completions =
                    invocationProviders.getCompleterProvider().enhanceCompleterInvocation(
                            new CompleterData(completeOperation.getContext(), lastWord != null ? lastWord.word() : "", parser.getCommand()));
            parser.getProcessedCommand().getArgument().completer().complete(completions);
            completeOperation.addCompletionCandidatesTerminalString(completions.getCompleterValues());
            completeOperation.setOffset(completeOperation.getCursor() - (lastWord != null ? lastWord.word().length() : 0));
            completeOperation.setIgnoreOffset(completions.doIgnoreOffset());
            completeOperation.setIgnoreStartsWith(completions.isIgnoreStartsWith());

            if (completions.getCompleterValues().size() == 1) {
                if(lastWord != null &&
                        lastWord.word().indexOf(Parser.SPACE_CHAR) > 0) {
                    completeOperation.setOffset(completeOperation.getCursor() -
                            (Parser.findNumberOfSpacesInWord(lastWord.word())));
                }
                if (completeOperation.getCompletionCandidates().get(0).containSpaces())
                    completeOperation.getCompletionCandidates().get(0).switchSpacesToEscapedSpaces();

                completeOperation.doAppendSeparator(completions.isAppendSpace());
            }
        }
    }

    private void completionSetOffSet(ParsedCompleteObject completeObject, CompleteOperation completeOperation, CompleterInvocation completions) {
        if(completions.getOffset() >= 0)
            completeOperation.setOffset( completeOperation.getCursor() - completions.getOffset());
        else if(completeObject.getValue() != null)
            completeOperation.setOffset( completeOperation.getCursor() - completeObject.getValue().length());
    }

    private void calcOffset(String value, CompleteOperation completeOperation, CompleterInvocation completions) {
        if(completions.getOffset() >= 0)
            completeOperation.setOffset( completeOperation.getCursor() - completions.getOffset());
        else if(value != null)
            completeOperation.setOffset( completeOperation.getCursor() - value.length());
    }

    private CommandLineCompletionParser getCorrectCompletionParser(String line) {
        if(!parser.isGroupCommand())
            return this;
        else {
            String childLine = line.trim().substring(parser.getProcessedCommand().name().length());
            String child = Parser.findFirstWord(childLine);
            CommandLineParser childParser = parser.getChildParser(child);
            if(childParser != null)
                return childParser.getCompletionParser();
            else
                return this;
        }
    }


}
