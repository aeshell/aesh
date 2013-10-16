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
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.parser.Parser;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandLineCompletionParser implements CommandLineCompletionParser {

    private CommandLineParser parser;

    public AeshCommandLineCompletionParser(CommandLineParser parser) {
        this.parser = parser;
    }


    /**
     * 1. find the last "word"
     *   if it starts with '-', we need to check if its a value or name
     * @param line buffer
     * @return ParsedCompleteObject
     */
    @Override
    public ParsedCompleteObject findCompleteObject(String line) throws CommandLineParserException {
        //first we check if it could be a param
        if(Parser.findIfWordEndWithSpace(line)) {
            //check if we try to complete just after the command name
            if(line.trim().equals(parser.getCommand().getName())) {
                if(parser.getCommand().getArgument() == null) {
                    //basically an empty string except command name
                    return new ParsedCompleteObject(true, "", 0);
                }
                return new ParsedCompleteObject(null, "", parser.getCommand().getArgument().getType(), false);
            }

            //else we try to complete an option,an option value or arguments
            String lastWord = Parser.findEscapedSpaceWordCloseToEnd(line.trim());
            if(lastWord.startsWith("-")) {
                int offset = lastWord.length();
                while(lastWord.startsWith("-"))
                    lastWord = lastWord.substring(1);
                if(lastWord.length() == 0)
                    return new ParsedCompleteObject(false, null, offset);
                else if(parser.getCommand().findOption(lastWord) != null ||
                        parser.getCommand().findLongOption(lastWord) != null)
                    return findCompleteObjectValue(line, true);
                else
                    return new ParsedCompleteObject(false, null, offset);
            }
            //last word is a value, need to find out what option its a value for
            else {
                return findCompleteObjectValue(line, true);
            }
        }
        else
            return optionFinder(line);
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
                if(lastWord.equals("-")) {
                    return new ParsedCompleteObject(true, "", 1);
                }
                else if(lastWord.equals("--")) {
                    return new ParsedCompleteObject(true, "", 2);
                }
                else {
                    //we have a complete shortName
                    if(!lastWord.startsWith("--") && lastWord.length() == 2)
                        return new ParsedCompleteObject(true,
                                Parser.trimOptionName(lastWord), lastWord.length(), true);
                    else {
                        String optionName = Parser.trimOptionName(lastWord);
                        if(parser.getCommand().hasLongOption(optionName))
                            return new ParsedCompleteObject(true, optionName, lastWord.length(), true);
                        else
                            return new ParsedCompleteObject(true, optionName, lastWord.length(), false);
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
                    cl.getArgument().getType(), false);
        }
        //get the last option
        else if (cl.getOptions() != null && cl.getOptions().size() > 0) {
            ProcessedOption po = cl.getOptions().get(cl.getOptions().size()-1);
            //options ends with a separator and thus status should be set accordingly
            if(po.getEndsWithSeparator())
                endsWithSpace = true;

            if(endsWithSpace && po.getValue() != null &&  po.getValue().length() > 0 &&
                    (po.getOptionType() == OptionType.NORMAL || po.getOptionType() == OptionType.BOOLEAN)) {
                return new ParsedCompleteObject(true);
            }
            else if(po.isLongNameUsed() || (po.getShortName() == null || po.getShortName().length() < 1))
                return new ParsedCompleteObject(po.getName(),
                        endsWithSpace ? "" : po.getValues().get(po.getValues().size()-1),
                        po.getType(), true);
            else
                return new ParsedCompleteObject( po.getShortName(),
                        endsWithSpace ? "" : po.getValues().get(po.getValues().size()-1),
                        po.getType(), true);
        }
        //probably something wrong with the parser
        else
            return new ParsedCompleteObject(true, "", 0);
    }

    @Override
    public void injectValuesAndComplete(ParsedCompleteObject completeObject, Command command,
                                        CompleteOperation completeOperation) {

        if(completeObject.doDisplayOptions()) {
            //got the whole name, just add a space
            if(completeObject.isCompleteOptionName()) {
                completeOperation.addCompletionCandidate("");
            }
            else {
                parser.parse(completeOperation.getBuffer(), true);
                //we have partial/full name
                if(completeObject.getName() != null && completeObject.getName().length() > 0) {
                    if(parser.getCommand().findPossibleLongNamesWitdDash(completeObject.getName()).size() > 0) {
                        //only one param
                        if(parser.getCommand().findPossibleLongNamesWitdDash(completeObject.getName()).size() == 1) {

                            completeOperation.addCompletionCandidate(
                                    parser.getCommand().findPossibleLongNamesWitdDash(completeObject.getName()).get(0));
                            completeOperation.setOffset( completeOperation.getCursor() - 2 - completeObject.getName().length());
                        }
                        //multiple params
                        else {
                            completeOperation.addCompletionCandidates(parser.getCommand().findPossibleLongNamesWitdDash(completeObject.getName()));
                        }

                    }
                }
                else {
                    if(parser.getCommand().getOptionLongNamesWithDash().size() > 1)
                        completeOperation.addCompletionCandidates(parser.getCommand().getOptionLongNamesWithDash());
                    else if(parser.getCommand().getOptionLongNamesWithDash().size() == 1) {
                        int count = 0;
                        while(completeOperation.getBuffer().substring(0, completeOperation.getBuffer().length()-count).endsWith("-"))
                            count++;
                        completeOperation.addCompletionCandidate(parser.getCommand().getOptionLongNamesWithDash().get(0));
                        completeOperation.setOffset( completeOperation.getCursor() - count);
                    }

                }
            }
        }
        //complete option value
        else if(completeObject.isOption()) {
            ProcessedOption currentOption = parser.getCommand().findOption(completeObject.getName());
            if(currentOption == null)
                currentOption = parser.getCommand().findLongOption(completeObject.getName());

            //split the line on the option name. populate the object, then call the options completer
            String displayName = currentOption.getDisplayName();
            //this shouldnt happen
            if(displayName == null) {
                return;
            }
            String rest = completeOperation.getBuffer().substring(0, completeOperation.getBuffer().lastIndexOf( displayName));

            try {
                parser.getCommandPopulator().populateObject(command, parser.parse(rest), false);
            }
            //this should be ignored at some point
            catch (CommandLineParserException ignored) { }
            catch (OptionValidatorException ignored) { }

            if(currentOption.getCompleter() != null) {
                CompleterData completions = new CompleterData(completeObject.getValue(), command);
                currentOption.getCompleter().complete(completions);
                completeOperation.addCompletionCandidates(completions.getCompleterValues());
                completeOperation.setOffset( completeOperation.getCursor() - completeObject.getOffset());

                if(completions.getCompleterValues().size() == 1) {
                    //if the contain spaces we need to add the number of spaces to the size
                    // of the value.length since they are chopped off during parsing
                    if(completeObject.getValue().indexOf(Parser.SPACE_CHAR) > 0) {
                        completeOperation.setOffset( completeOperation.getCursor() -
                                (completeObject.getOffset() + Parser.findNumberOfSpacesInWord(completeObject.getValue())));
                    }
                    if(completeOperation.getCompletionCandidates().get(0).indexOf(Parser.SPACE_CHAR) > 0)
                        completeOperation.getCompletionCandidates().set(0,
                                Parser.switchSpacesToEscapedSpacesInWord(completeOperation.getCompletionCandidates().get(0)));

                    completeOperation.doAppendSeparator( completions.isAppendSpace());
                }
            }
            //only try to complete default values if completer is null
            else if(currentOption.getDefaultValues().size() > 0) {
                CompleterData completions = new CompleterData(completeObject.getValue(), command);
                new DefaultValueOptionCompleter(currentOption.getDefaultValues()).complete(completions);
                completeOperation.addCompletionCandidates(completions.getCompleterValues());
                completeOperation.setOffset( completeOperation.getCursor() - completeObject.getOffset());

                if(completions.getCompleterValues().size() == 1) {
                    //if the contain spaces we need to add the number of spaces to the size
                    // of the value.length since they are chopped off during parsing
                    if(completeObject.getValue().indexOf(Parser.SPACE_CHAR) > 0) {
                        completeOperation.setOffset( completeOperation.getCursor() -
                                (completeObject.getOffset() + Parser.findNumberOfSpacesInWord(completeObject.getValue())));
                    }
                    if(completeOperation.getCompletionCandidates().get(0).indexOf(Parser.SPACE_CHAR) > 0)
                        completeOperation.getCompletionCandidates().set(0,
                                Parser.switchSpacesToEscapedSpacesInWord(completeOperation.getCompletionCandidates().get(0)));

                    completeOperation.doAppendSeparator( completions.isAppendSpace());
                }
            }
        }
        else if(completeObject.isArgument()) {
            String lastWord = Parser.findEscapedSpaceWordCloseToEnd(completeOperation.getBuffer());
            String rest = completeOperation.getBuffer().substring(0, completeOperation.getBuffer().length() - lastWord.length());
            try {
                parser.getCommandPopulator().populateObject(command, parser.parse(rest), false);
            }
            catch (CommandLineParserException ignored) { }
            catch (OptionValidatorException ignored) { }

            if(parser.getCommand().getArgument() != null &&
                    parser.getCommand().getArgument().getCompleter() != null) {
                CompleterData completions = new CompleterData(completeObject.getValue(), command);
                parser.getCommand().getArgument().getCompleter().complete(completions);
                completeOperation.addCompletionCandidates(completions.getCompleterValues());
                completeOperation.setOffset( completeOperation.getCursor() - completeObject.getOffset());

                if(completions.getCompleterValues().size() == 1) {
                    if(completeObject.getValue().indexOf(Parser.SPACE_CHAR) > 0) {
                        completeOperation.setOffset( completeOperation.getCursor() -
                                (completeObject.getOffset() + Parser.findNumberOfSpacesInWord(completeObject.getValue())));
                    }
                    if(completeOperation.getCompletionCandidates().get(0).indexOf(Parser.SPACE_CHAR) > 0)
                        completeOperation.getCompletionCandidates().set(0,
                                Parser.switchSpacesToEscapedSpacesInWord(completeOperation.getCompletionCandidates().get(0)));

                    completeOperation.doAppendSeparator( completions.isAppendSpace());
                }

            }
            else if(parser.getCommand().getArgument() != null &&
                    parser.getCommand().getArgument().getDefaultValues().size() > 0) {
                CompleterData completions = new CompleterData(completeObject.getValue(), command);
                new DefaultValueOptionCompleter( parser.getCommand().getArgument().getDefaultValues()).complete(completions);
                completeOperation.addCompletionCandidates(completions.getCompleterValues());
                completeOperation.setOffset( completeOperation.getCursor() - completeObject.getOffset());

                if(completions.getCompleterValues().size() == 1) {
                    if(completeObject.getValue().indexOf(Parser.SPACE_CHAR) > 0) {
                        completeOperation.setOffset( completeOperation.getCursor() -
                                (completeObject.getOffset() + Parser.findNumberOfSpacesInWord(completeObject.getValue())));
                    }
                    if(completeOperation.getCompletionCandidates().get(0).indexOf(Parser.SPACE_CHAR) > 0)
                        completeOperation.getCompletionCandidates().set(0,
                                Parser.switchSpacesToEscapedSpacesInWord(completeOperation.getCompletionCandidates().get(0)));

                }
                completeOperation.doAppendSeparator( completions.isAppendSpace());
            }
        }
    }

}
