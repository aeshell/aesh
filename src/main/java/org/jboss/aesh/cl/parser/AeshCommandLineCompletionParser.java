/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.parser;

import org.jboss.aesh.cl.CommandLine;
import org.jboss.aesh.cl.completer.CompleterData;
import org.jboss.aesh.cl.completer.DefaultValueOptionCompleter;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.OptionType;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.validator.OptionValidatorException;
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

    private final AeshCommandLineParser parser;

    public AeshCommandLineCompletionParser(AeshCommandLineParser parser) {
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
        //first we check if its a group command
        if(parser.getProcessedCommand().isGroupCommand())
            return parsingGroupCommand(line, cursor);
        //then we check if it could be a param
        else if(Parser.findIfWordEndWithSpace(line)) {
            return endsWithSpace(line);
        }
        //lastly we'll check if we can find an option
        else
            return optionFinder(line);
    }

    //we assume that this line has atleast two words,
    // (if not it should not have called this method)
    private ParsedCompleteObject parsingGroupCommand(String line, int cursor) throws CommandLineParserException {
        int stripIndex = line.indexOf(' ', Parser.findFirstWord(line).length()-1);
        AeshCommandLineParser childParser = parser.getChildParser(
                Parser.findFirstWord(Parser.findFirstWord(line.substring(stripIndex))));

        if(childParser != null)
            return childParser.getCompletionParser().findCompleteObject(line.substring(stripIndex), cursor);
        else
            throw new CommandLineParserException("Could not find child for line: "+line);
    }

    private ParsedCompleteObject endsWithSpace(String line) throws CommandLineParserException {
        //check if we try to complete just after the command name
        if(line.trim().equals(parser.getProcessedCommand().getName())) {
            if(parser.getProcessedCommand().getArgument() == null) {
                //basically an empty string except command name
                return new ParsedCompleteObject(true, "", 0, this);
            }
            return new ParsedCompleteObject(null, "", parser.getProcessedCommand().getArgument().getType(), false, this);
        }

        //else we try to complete an option,an option value or arguments
        String lastWord = Parser.findEscapedSpaceWordCloseToEnd(line.trim());
        if(lastWord.startsWith("-")) {
            int offset = lastWord.length();
            while(lastWord.startsWith("-"))
                lastWord = lastWord.substring(1);
            if(lastWord.length() == 0)
                return new ParsedCompleteObject(false, null, offset, this);
            else if(parser.getProcessedCommand().findOptionNoActivatorCheck(lastWord) != null ||
                    parser.getProcessedCommand().findLongOptionNoActivatorCheck(lastWord) != null)
                return findCompleteObjectValue(line, true);
            else
                return new ParsedCompleteObject(false, null, offset, this);
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
                        return new ParsedCompleteObject(true, "", 1, this);
                    case "--":
                        return new ParsedCompleteObject(true, "", 2, this);
                    default:
                        //we have a complete shortName
                        if (!lastWord.startsWith("--") && lastWord.length() == 2)
                            return new ParsedCompleteObject(true,
                                    Parser.trimOptionName(lastWord), lastWord.length(), true, this);
                        else {
                            String optionName = Parser.trimOptionName(lastWord);
                            if (parser.getProcessedCommand().hasUniqueLongOption(optionName))
                                return new ParsedCompleteObject(true, optionName, lastWord.length(), true, this);
                            else
                                return new ParsedCompleteObject(true, optionName, lastWord.length(), false, this);
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
        CommandLine cl = parser.parse(line, true);

        //the last word is an argument
        if(cl.getArgument() != null && !cl.getArgument().getValues().isEmpty()) {
            return new ParsedCompleteObject("", endsWithSpace ? "" :
                    cl.getArgument().getValues().get(cl.getArgument().getValues().size() - 1),
                    cl.getArgument().getType(), false, this);
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
                    return new ParsedCompleteObject(true, "", 0, this);
                else
                    return new ParsedCompleteObject(true, this);
            }
            else if(po.isLongNameUsed() || (po.getShortName() == null || po.getShortName().length() < 1))
                return new ParsedCompleteObject(po.getName(),
                        endsWithSpace ? "" : po.getValues().get(po.getValues().size()-1),
                        po.getType(), true, this);
            else
                return new ParsedCompleteObject( po.getShortName(),
                        endsWithSpace ? "" : po.getValues().get(po.getValues().size()-1),
                        po.getType(), true, this);
        }
        //probably something wrong with the parser
        else
            return new ParsedCompleteObject(true, "", 0, this);
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
                parser.parse(completeOperation.getBuffer(), true);
                //we have partial/full name
                if(completeObject.getName() != null && completeObject.getName().length() > 0) {
                    String rest = completeOperation.getBuffer().substring(0, completeOperation.getBuffer().lastIndexOf( completeObject.getName()));
                    try {
                        parser.getCommandPopulator().populateObject(parser.getCommand(), parser.parse(rest), invocationProviders,
                                completeOperation.getAeshContext(), false);
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
                        parser.getCommandPopulator().populateObject(parser.getCommand(), parser.parse(completeOperation.getBuffer()),
                                invocationProviders, completeOperation.getAeshContext(), false);
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
                parser.getCommandPopulator().populateObject(parser.getCommand(), parser.parse(rest), invocationProviders,
                        completeOperation.getAeshContext(), false);
            }
            //this should be ignored at some point
            catch (CommandLineParserException | OptionValidatorException ignored) { }

            if(currentOption.getCompleter() != null &&
                    currentOption.getActivator().isActivated(parser.getProcessedCommand())) {
                CompleterInvocation completions =
                        invocationProviders.getCompleterProvider().enhanceCompleterInvocation(
                                new CompleterData(completeOperation.getAeshContext(), completeObject.getValue(), parser.getCommand()));

                currentOption.getCompleter().complete(completions);
                completeOperation.addCompletionCandidatesTerminalString(completions.getCompleterValues());
                completeOperation.setOffset( completeOperation.getCursor() - completeObject.getOffset());
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
                completeOperation.setOffset( completeOperation.getCursor() - completeObject.getOffset());
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
                parser.getCommandPopulator().populateObject(parser.getCommand(), parser.parse(rest), invocationProviders,
                        completeOperation.getAeshContext(), false);
            }
            catch (CommandLineParserException | OptionValidatorException ignored) { }

            if(parser.getProcessedCommand().getArgument() != null &&
                    parser.getProcessedCommand().getArgument().getCompleter() != null) {
                CompleterInvocation completions =
                        invocationProviders.getCompleterProvider().enhanceCompleterInvocation(
                                new CompleterData(completeOperation.getAeshContext(), completeObject.getValue(), parser.getCommand()));
                parser.getProcessedCommand().getArgument().getCompleter().complete(completions);
                completeOperation.addCompletionCandidatesTerminalString(completions.getCompleterValues());
                completeOperation.setOffset( completeOperation.getCursor() - completeObject.getOffset());
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
            else if(parser.getProcessedCommand().getArgument() != null &&
                    parser.getProcessedCommand().getArgument().getDefaultValues().size() > 0) {
                CompleterInvocation completions =
                        invocationProviders.getCompleterProvider().enhanceCompleterInvocation(
                                new CompleterData(completeOperation.getAeshContext(), completeObject.getValue(), parser.getCommand()));
                new DefaultValueOptionCompleter( parser.getProcessedCommand().getArgument().getDefaultValues()).complete(completions);
                completeOperation.addCompletionCandidatesTerminalString(completions.getCompleterValues());
                completeOperation.setOffset( completeOperation.getCursor() - completeObject.getOffset());
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

}
