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
package org.jboss.aesh.cl.parser;

import org.jboss.aesh.cl.CommandLine;
import org.jboss.aesh.cl.completer.CompleterData;
import org.jboss.aesh.cl.completer.DefaultValueOptionCompleter;
import org.jboss.aesh.cl.internal.OptionType;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.console.InvocationProviders;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.completer.CompleterInvocation;
import org.jboss.aesh.parser.Parser;
import org.jboss.aesh.terminal.TerminalString;

import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandLineCompletionParser implements CommandLineCompletionParser {

    private final AeshCommandLineParser<? extends Command> parser;

    public AeshCommandLineCompletionParser(AeshCommandLineParser<? extends Command> parser) {
        this.parser = parser;
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
        CommandLine cl = parser.parse(line, true);
        //check if we try to complete just after the command name
        if(parser.isGroupCommand()) {
            if (line.trim().equals(parser.getProcessedCommand().getName() + " " +
                    cl.getParser().getProcessedCommand().getName())) {
                if (cl.getParser().getProcessedCommand().getArgument() == null) {
                    //basically an empty string except command name
                    return new ParsedCompleteObject(true, "", 0, cl.getParser().getCompletionParser());
                }
                return new ParsedCompleteObject(null, "", cl.getParser().getProcessedCommand().getArgument().getType(),
                        false, getCorrectCompletionParser(line));
            }
        }
        else if(line.trim().equals(cl.getParser().getProcessedCommand().getName())) {
            if(cl.getParser().getProcessedCommand().getArgument() == null) {
                //basically an empty string except command name
                return new ParsedCompleteObject(true, "", 0, cl.getParser().getCompletionParser());
            }
            return new ParsedCompleteObject(null, "", cl.getParser().getProcessedCommand().getArgument().getType(),
                    false, getCorrectCompletionParser(line));
        }
        //else we try to complete an option,an option value or arguments
        String lastWord = Parser.findEscapedSpaceWordCloseToEnd(line.trim());
        if(lastWord.startsWith("-")) {
            int offset = lastWord.length();
            while(lastWord.startsWith("-"))
                lastWord = lastWord.substring(1);
            if(lastWord.length() == 0)
                return new ParsedCompleteObject(false, null, offset, getCorrectCompletionParser(line));
            else if(cl.getParser().getProcessedCommand().findOptionNoActivatorCheck(lastWord) != null ||
                    cl.getParser().getProcessedCommand().findLongOptionNoActivatorCheck(lastWord) != null)
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
        if(lastWord.startsWith("-") ) {
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
                            CommandLine cl = parser.parse(line, true);
                            if (cl.getParser().getProcessedCommand().hasUniqueLongOption(optionName))
                                return new ParsedCompleteObject(true, optionName, lastWord.length(), true, cl.getParser().getCompletionParser());
                            else
                                return new ParsedCompleteObject(true, optionName, lastWord.length(), false, cl.getParser().getCompletionParser());
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
        CommandLine<? extends Command> cl = parser.parse(line, true);

        //the last word is an argument
        if(cl.getArgument() != null && !cl.getArgument().getValues().isEmpty()) {
            return new ParsedCompleteObject("", endsWithSpace ? "" :
                    cl.getArgument().getValues().get(cl.getArgument().getValues().size() - 1),
                    cl.getArgument().getType(), false, cl.getParser().getCompletionParser());
        }
        //get the last option
        else if (cl.getOptions() != null && cl.getOptions().size() > 0) {
            ProcessedOption po = cl.getOptions().get(cl.getOptions().size()-1);
            //options ends with a separator and thus status should be set accordingly
            if(po.getEndsWithSeparator())
                endsWithSpace = true;

            if(endsWithSpace && po.getValue() != null &&  po.getValue().length() > 0 &&
                    (po.getOptionType() == OptionType.NORMAL || po.getOptionType() == OptionType.BOOLEAN)) {
                if(cl.getArgument() == null)
                    return new ParsedCompleteObject(true, "", 0, cl.getParser().getCompletionParser());
                else
                    return new ParsedCompleteObject(true, cl.getParser().getCompletionParser());
            }
            else if(po.isLongNameUsed() || (po.getShortName() == null || po.getShortName().length() < 1))
                return new ParsedCompleteObject(po.getName(),
                        endsWithSpace ? "" : po.getValues().get(po.getValues().size()-1),
                        po.getType(), true, cl.getParser().getCompletionParser());
            else
                return new ParsedCompleteObject( po.getShortName(),
                        endsWithSpace ? "" : po.getValues().get(po.getValues().size()-1),
                        po.getType(), true, cl.getParser().getCompletionParser());
        }
        //probably something wrong with the parser
        else
            return new ParsedCompleteObject(true, "", 0, getCorrectCompletionParser(line));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void injectValuesAndComplete(ParsedCompleteObject completeObject,
                                        CompleteOperation completeOperation,
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
                    List<String> words = Parser.findAllWords(rest).getWords();
                    try {
                        parser.getCommandPopulator().populateObject(parser.parse(words, true), invocationProviders,
                                completeOperation.getAeshContext(), false, null);
                    }
                    //this should be ignored at some point
                    catch (Exception ignored) { }
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
                        List<String> words = Parser.findAllWords(completeOperation.getBuffer()).getWords();
                        if(words.get(words.size()-1).equals("--") || words.get(words.size()-1).equals("-"))
                            words.remove(words.size()-1);
                        parser.getCommandPopulator().populateObject(parser.parse(words, true),
                                invocationProviders, completeOperation.getAeshContext(), false, null);
                    }
                    //this should be ignored at some point
                    catch (Exception ignored) { }
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
                List<String> words = Parser.findAllWords(rest).getWords();
                parser.getCommandPopulator().populateObject(parser.parse(words, true),
                        invocationProviders, completeOperation.getAeshContext(), false, null);
            }
            //this should be ignored at some point
            catch (Exception ignored) { }

            if(currentOption.getCompleter() != null &&
                    currentOption.getActivator().isActivated(parser.getProcessedCommand())) {
                CompleterInvocation completions =
                        invocationProviders.getCompleterProvider().enhanceCompleterInvocation(
                                new CompleterData(completeOperation.getAeshContext(), completeObject.getValue(), parser.getCommand()));

                currentOption.getCompleter().complete(completions);
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
                                new CompleterData(completeOperation.getAeshContext(), completeObject.getValue(), parser.getCommand()));
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
                List<String> words = Parser.findAllWords(rest).getWords();
                parser.getCommandPopulator().populateObject(parser.parse(words, true),
                        invocationProviders, completeOperation.getAeshContext(), false, null);
            }
            catch (Exception ignored) { }

            if(parser.getProcessedCommand().getArgument() != null &&
                    parser.getProcessedCommand().getArgument().getCompleter() != null) {
                CompleterInvocation completions =
                        invocationProviders.getCompleterProvider().enhanceCompleterInvocation(
                                new CompleterData(completeOperation.getAeshContext(), completeObject.getValue(), parser.getCommand()));
                parser.getProcessedCommand().getArgument().getCompleter().complete(completions);
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
                                new CompleterData(completeOperation.getAeshContext(), completeObject.getValue(), parser.getCommand()));
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

    private void completionSetOffSet(ParsedCompleteObject completeObject, CompleteOperation completeOperation, CompleterInvocation completions) {
        if(completions.getOffset() > 0)
            completeOperation.setOffset( completeOperation.getCursor() - completions.getOffset());
        else if(completeObject.getValue() != null)
            completeOperation.setOffset( completeOperation.getCursor() - completeObject.getValue().length());
    }

    private CommandLineCompletionParser getCorrectCompletionParser(String line) {
        if(!parser.isGroupCommand())
            return this;
        else {
            String childLine = line.trim().substring(parser.getProcessedCommand().getName().length());
            String child = Parser.findFirstWord(childLine);
            CommandLineParser childParser = parser.getChildParser(child);
            if(childParser != null)
                return childParser.getCompletionParser();
            else
                return this;
        }
    }

}
