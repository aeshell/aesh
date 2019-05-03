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

import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.impl.completer.CompleterData;
import org.aesh.command.impl.completer.DefaultValueOptionCompleter;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ParsedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.parser.ParsedLine;
import org.aesh.parser.ParsedWord;
import org.aesh.parser.ParserStatus;
import org.aesh.readline.AeshContext;
import org.aesh.readline.terminal.formatting.TerminalString;
import org.aesh.readline.util.Parser;
import org.aesh.selector.SelectorType;

import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandLineCompletionParser<CI extends CommandInvocation> implements CommandLineCompletionParser {

    private final AeshCommandLineParser<CI> parser;

    public AeshCommandLineCompletionParser(AeshCommandLineParser<CI> parser) {
        this.parser = parser;
    }

    @Override
    public void injectValuesAndComplete(AeshCompleteOperation completeOperation, InvocationProviders invocationProviders,
                                        ParsedLine line) {
        //first inject values in command
        doInjectValues(invocationProviders, completeOperation.getContext());

        //we have parsed one or more options and their values
        if(parser.getProcessedCommand().completeStatus().status().equals(CompleteStatus.Status.COMPLETE_OPTION)) {
            //space and end, we display other options/arguments or option value if the option have a list of values
            //- if it ends with a separator we also try to complete an option value
            ParsedWord.Status selectedWordStatus = line.selectedWord() != null ? line.selectedWord().status() : line.lastWord().status();
            if ((line.spaceAtEnd() || parser.lastParsedOption().getEndsWithSeparator()) && selectedWordStatus == ParsedWord.Status.OK) {
                if(parser.lastParsedOption() != null) {
                        if (parser.lastParsedOption().getValue() == null ||
                                parser.lastParsedOption().hasMultipleValues()) {
                            //need to complete option value
                            //extra check to make sure that lists are properly parsed
                            if(line.spaceAtEnd() && parser.lastParsedOption().getValueSeparator() == ' ')
                                parser.lastParsedOption().setEndsWithSeparator(true);
                            doCompleteOptionValue(invocationProviders, completeOperation, parser.lastParsedOption(), selectedWordStatus);
                        }
                        //complete options if there are no arguments, else complete arguments
                        else {
                            if(parser.getProcessedCommand().hasArguments() ||
                                    parser.getProcessedCommand().hasArgumentWithNoValue()) {
                                //complete arguments
                                doProcessArgument(completeOperation, invocationProviders, line);
                            }
                            else {
                                //list options
                                doListOptions(completeOperation, "");
                            }
                        }
                }
                //complete options if there are no arguments, else complete arguments
                else {
                    if(parser.getProcessedCommand().hasArguments() ||
                            parser.getProcessedCommand().hasArgumentWithNoValue()) {
                        //ParsedWord lastWord = line.selectedWord();
                        //if(lastWord != null)
                        //    parser.getProcessedCommand().getArguments().addValue(lastWord.word());
                        doProcessArgument(completeOperation, invocationProviders, line);
                    }
                    else
                        doListOptions(completeOperation, "");
                }
            }
            //no space means we should try to complete the value of the last parsed option
            //or unclosed quote/bracket
            else {
                //need to make sure that the open brackets or quotes negates ends on separators
                if(selectedWordStatus != ParsedWord.Status.OK)
                    parser.lastParsedOption().setEndsWithSeparator(false);
                doCompleteOptionValue(invocationProviders, completeOperation, parser.lastParsedOption(), selectedWordStatus);
            }
        }
        //partial long option name, contains atleast --
        else if(parser.getProcessedCommand().completeStatus().status().equals(CompleteStatus.Status.LONG_OPTION)) {
            //not option value so we'll complete all options not already listed
            doListOptions(completeOperation, "--"+parser.getProcessedCommand().completeStatus().value());
        }
        //partial short option, contains atleast -
        else if(parser.getProcessedCommand().completeStatus().status().equals(CompleteStatus.Status.SHORT_OPTION)) {
            //if we do not have any value, we add another -
            if(parser.getProcessedCommand().completeStatus().value().length() == 0) {
                        completeOperation.addCompletionCandidate("-");
                        completeOperation.doAppendSeparator(false);
                        //completeOperation.setOffset( completeOperation.getCursor() - count);
                        completeOperation.setOffset( completeOperation.getCursor());
            }
        }
        //we have an option, but no value
        else if(parser.getProcessedCommand().completeStatus().status().equals(CompleteStatus.Status.OPTION_MISSING_VALUE)) {
            //we need to complete a value
            //if we have a short option without a value lets check for missing equals
            if(!parser.lastParsedOption().isLongNameUsed() &&
                    parser.lastParsedOption().getValue() == null && !parser.lastParsedOption().getEndsWithSeparator()
                    && !line.spaceAtEnd()) {
                //TODO: fix this
                //do nothing
            }
            else if(parser.lastParsedOption().isLongNameUsed() &&
                    !parser.lastParsedOption().getEndsWithSeparator() &&
                    !line.spaceAtEnd() && !line.selectedWord().word().endsWith("=") &&
                    parser.lastParsedOption().hasValue()) {
                completeOperation.addCompletionCandidate("=");
                completeOperation.setOffset(completeOperation.getCursor());
                completeOperation.doAppendSeparator(false);
            }
            else if(!parser.lastParsedOption().hasValue() &&
                    !parser.lastParsedOption().getEndsWithSeparator() &&
                    !line.spaceAtEnd()) {
                completeOperation.addCompletionCandidate(" ");
                completeOperation.setOffset(completeOperation.getCursor());
                completeOperation.doAppendSeparator(false);
            }
            //complete value
            else {
                doCompleteOptionValue(invocationProviders, completeOperation,
                        parser.lastParsedOption(),line.selectedWord() != null ? line.selectedWord().status() : line.lastWord().status());
            }
        }
        //argument
        else if(parser.getProcessedCommand().completeStatus().status().equals(CompleteStatus.Status.ARGUMENT)) {
            doProcessArgument(completeOperation, invocationProviders, line);
        }
        //group command
        else if(parser.getProcessedCommand().completeStatus().status().equals(CompleteStatus.Status.GROUP_COMMAND)) {
            doProcessGroupCommand(completeOperation, parser.getProcessedCommand().completeStatus().value(), line);
        }
        //append space after group command
        else if(parser.getProcessedCommand().completeStatus().status().equals(CompleteStatus.Status.APPEND_SPACE)) {
            completeOperation.addCompletionCandidate(" ");
            completeOperation.doAppendSeparator(false);
            completeOperation.setOffset(completeOperation.getCursor());
        }
    }

    private void doProcessGroupCommand(AeshCompleteOperation completeOperation, String name, ParsedLine line) {
        if(name.length() == 0) {
            for (CommandLineParser clp : parser.getAllChildParsers()) {
                if(clp.getProcessedCommand().getActivator().isActivated(new ParsedCommand(clp.getProcessedCommand())))
                    completeOperation.addCompletionCandidate(clp.getProcessedCommand().name());
            }
            if(completeOperation.getCompletionCandidates().size() == 1)
                completeOperation.setOffset(completeOperation.getCursor());
        }
        else {
            for (CommandLineParser child : parser.getAllChildParsers()) {
                if (child.getProcessedCommand().name().startsWith(name) &&
                        child.getProcessedCommand().getActivator().isActivated(new ParsedCommand(child.getProcessedCommand()))) {
                    completeOperation.addCompletionCandidate(child.getProcessedCommand().name());
                    completeOperation.setOffset(completeOperation.getCursor()-name.length());
                }
            }
        }
        if(completeOperation.getCompletionCandidates().size() == 1 && !line.cursorAtEnd())
            completeOperation.doAppendSeparator(false);
    }

    private void doProcessArgument(AeshCompleteOperation completeOperation, InvocationProviders invocationProviders, ParsedLine line) {
           ProcessedOption arg =
                    parser.getProcessedCommand().hasArguments() ?
                            parser.getProcessedCommand().getArguments() :
                            parser.getProcessedCommand().getArgument();
            //first check if arg is argument, if so check if it already have a value, if so to an option complete
            if(arg.getOptionType() == OptionType.ARGUMENT &&
                    (arg.getValue() != null || !arg.activator().isActivated(new ParsedCommand(parser.getProcessedCommand())))) {
                //list options
                doListOptions(completeOperation, "");
            }
            //if arguments, but not activated
            else if(arg.getOptionType() == OptionType.ARGUMENTS && !arg.activator().isActivated(new ParsedCommand(parser.getProcessedCommand())))
                //list options
                doListOptions(completeOperation, "");
            //argument(s)
            else {
                if (parser.getProcessedCommand().completeStatus().value() != null &&
                        parser.getProcessedCommand().completeStatus().value().length() > 0)
                    arg.addValue(parser.getProcessedCommand().completeStatus().value());
                else
                    //set this to true since we do not want to use previous values in the completion value
                    arg.setEndsWithSeparator(true);
                //for now just default to Status.OK
                boolean haveCompletion = false;

                if(parser.getProcessedCommand().completeStatus().value() != null &&
                        parser.getProcessedCommand().completeStatus().value().length() > 0)
                    haveCompletion = doCompleteOptionValue(invocationProviders, completeOperation, arg,
                            line.selectedWord().status());
                else {
                    //if status is ok, we send ok. if not we'll send open quote for now
                    haveCompletion = doCompleteOptionValue(invocationProviders, completeOperation, arg,
                            line.status() == ParserStatus.OK ? ParsedWord.Status.OK : ParsedWord.Status.OPEN_QUOTE);
                }

                /*
                We have 2 cases in which we do display options:
                - we don't have completion candidates
                - the arg has a completer
                - we are not completing an argument value
                - whatever the fact that the arg is required or not
                or
                - we don't have completion candidates
                - arg is not required
                - we are not completing an argument value

                So if arg is required and has no completer, the options will be not proposed
                user will have to enter something.
                 */
                if (!haveCompletion) {
                    if ((arg.completer() != null || !arg.isRequired())
                            && (parser.getProcessedCommand().completeStatus().value() == null
                            || parser.getProcessedCommand().completeStatus().value().length() == 0)) {
                        doListOptions(completeOperation, "");
                    }
                }
            }
    }

    private void doListOptions(AeshCompleteOperation completeOperation, String value) {
        List<TerminalString> optionNamesWithDash;
        if(value.length() < 3)
            optionNamesWithDash = parser.getProcessedCommand().getOptionLongNamesWithDash();
        else
            optionNamesWithDash = parser.getProcessedCommand().findPossibleLongNamesWithDash(value.substring(2));

        if(optionNamesWithDash.size() > 1) {
            completeOperation.addCompletionCandidatesTerminalString(optionNamesWithDash);
            completeOperation.setOffset(completeOperation.getCursor() - value.length());
            completeOperation.setIgnoreStartsWith(false);

        }
        else if(optionNamesWithDash.size() == 1) {
            if(optionNamesWithDash.get(0).isFormatted())
                completeOperation.addCompletionCandidate(new TerminalString(optionNamesWithDash.get(0).getCharacters(), true));
            else
                completeOperation.addCompletionCandidate(optionNamesWithDash.get(0));
            //we should always have -- at the end here so always subtract 2
            completeOperation.setOffset( completeOperation.getCursor() - value.length());
            //do not append separator, we do that in the getOptionLongNames
            if(optionNamesWithDash.get(0).getCharacters().endsWith("="))
                completeOperation.doAppendSeparator(false);
        }

    }

    @SuppressWarnings("unchecked")
    private boolean doCompleteOptionValue(InvocationProviders invocationProviders, AeshCompleteOperation completeOperation,
                                          ProcessedOption currentOption, ParsedWord.Status selectedWordStatus) {
        String value = currentOption.getLastValue();
        //if value is null or ends with a separator
        if(value == null || currentOption.getEndsWithSeparator())
            value = "";

        if(currentOption.completer() != null && currentOption.hasValue() &&
                currentOption.activator().isActivated(new ParsedCommand(parser.getProcessedCommand()))) {
            CompleterInvocation completions =
                    invocationProviders.getCompleterProvider().enhanceCompleterInvocation(
                            new CompleterData(completeOperation.getContext(), value, parser.getCommand()));

            currentOption.completer().complete(completions);
            completeOperation.addCompletionCandidatesTerminalString(completions.getCompleterValues());
            verifyCompleteValue(completeOperation, completions, value, selectedWordStatus, currentOption);
        }
        //only try to complete default values if completer is null
        else if(currentOption.getDefaultValues().size() > 0 && currentOption.selectorType() == SelectorType.NO_OP) {
            CompleterInvocation completions =
                    invocationProviders.getCompleterProvider().enhanceCompleterInvocation(
                            new CompleterData(completeOperation.getContext(), value, parser.getCommand()));
            new DefaultValueOptionCompleter(currentOption.getDefaultValues()).complete(completions);
            completeOperation.addCompletionCandidatesTerminalString(completions.getCompleterValues());
            verifyCompleteValue(completeOperation, completions, value, selectedWordStatus, currentOption);
        }
        else if(!currentOption.hasValue()) {
            completeOperation.doAppendSeparator(true);
        }

        return completeOperation.getCompletionCandidates().size() > 0;
    }

    public static void verifyCompleteValue(AeshCompleteOperation completeOperation,
            CompleterInvocation completions,
            String value, ParsedWord.Status selectedWordStatus, ProcessedOption currentOption) {

        if (completions.getOffset() >= 0) {
            // We must remove the number of spaces present in the candidate to inline
            // only for the part in between the offset and the end.
            int numberSpaces = 0;
            if (selectedWordStatus == ParsedWord.Status.OK
                    && completions.getCompleterValues().size() == 1) {
                numberSpaces = Parser.
                        findNumberOfSpacesInWord(value.substring(value.length() - completions.getOffset(), value.length()));
            }
            completeOperation.setOffset(completeOperation.getCursor() - completions.getOffset() - numberSpaces);
        } else {
            // value doesn't contain escape, we need to substract them only if no quote nor bracket.
            // In there are some quote or bracket then selectedWordStatus != ParsedWord.Status.OK
            completeOperation.setOffset(completeOperation.getCursor() - value.length()
                    - (selectedWordStatus == ParsedWord.Status.OK ? Parser.findNumberOfSpacesInWord(value) : 0));
        }

        // Escape spaces if no bracket nor quote
        if (selectedWordStatus == ParsedWord.Status.OK
                && completeOperation.getCompletionCandidates().size() == 1
                && (completeOperation.getCompletionCandidates().get(0).containSpaces())) {
            completeOperation.getCompletionCandidates().get(0).switchSpacesToEscapedSpaces();
        }

        if (completions.getCompleterValues().size() == 1) {
            completeOperation.doAppendSeparator(completions.isAppendSpace());
            if(currentOption != null)
                completeOperation.setSeparator(currentOption.getValueSeparator());
        }
        //finally set flags
        completeOperation.setIgnoreOffset(completions.doIgnoreOffset());
        completeOperation.setIgnoreStartsWith(completions.isIgnoreStartsWith());
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

}
