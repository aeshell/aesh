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
import org.aesh.command.impl.completer.CompleterData;
import org.aesh.command.impl.completer.DefaultValueOptionCompleter;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.Command;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.console.AeshContext;
import org.aesh.parser.LineParser;
import org.aesh.parser.ParsedLine;
import org.aesh.parser.ParsedWord;
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

    @Override
    public void injectValuesAndComplete(AeshCompleteOperation completeOperation, InvocationProviders invocationProviders,
                                        ParsedLine line) {
        //if the parser is a child, we need to change the line accordingly
        if(parser.isChild()) {
            line = new LineParser().parseLine(line.line().substring(line.line().indexOf(parser.getAllNames().get(0))));
        }
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
                            doCompleteOptionValue(invocationProviders, completeOperation, parser.lastParsedOption());
                        }
                        //complete options if there are no arguments, else complete arguments
                        else {
                            if(parser.getProcessedCommand().hasArgument()) {
                                //complete argument
                                doCompleteOptionValue(invocationProviders, completeOperation,
                                        parser.getProcessedCommand().getArgument());
                            }
                            else {
                                //list options
                                doListOptions(completeOperation, "");
                            }
                        }
                }
                //complete options if there are no arguments, else complete arguments
                else {
                    if(parser.getProcessedCommand().hasArgument()) {
                        ParsedWord lastWord = line.selectedWord();
                        if(lastWord != null)
                            parser.getProcessedCommand().getArgument().addValue(lastWord.word());

                        doCompleteOptionValue(invocationProviders, completeOperation, parser.getProcessedCommand().getArgument());
                        //completeArgument(completeOperation, invocationProviders, line);
                    }
                    else
                        doListOptions(completeOperation, "--");
                }
            }
            //no space means we should try to complete the value of the last parsed option
            else {
                doCompleteOptionValue(invocationProviders, completeOperation, parser.lastParsedOption());
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
                        //completeOperation.setOffset( completeOperation.getCursor() - count);
                        completeOperation.setOffset( completeOperation.getCursor());
            }
        }
        //we have an option, but no value
        else if(parser.getProcessedCommand().completeStatus().status().equals(CompleteStatus.Status.OPTION_MISSING_VALUE)) {
            //we need to complete a value
            doCompleteOptionValue(invocationProviders, completeOperation, parser.lastParsedOption());
        }
        else if(parser.getProcessedCommand().completeStatus().status().equals(CompleteStatus.Status.ARGUMENT)) {
            if(parser.getProcessedCommand().completeStatus().value() != null)
                parser.getProcessedCommand().getArgument().addValue(parser.getProcessedCommand().completeStatus().value());
            doCompleteOptionValue(invocationProviders, completeOperation, parser.getProcessedCommand().getArgument());
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
            completeOperation.setOffset( completeOperation.getCursor());
            completeOperation.setIgnoreStartsWith(true);
        }
        else if(optionNamesWithDash.size() == 1) {
            completeOperation.addCompletionCandidate(optionNamesWithDash.get(0));
            //we should always have -- at the end here so always subtract 2
            completeOperation.setOffset( completeOperation.getCursor() - value.length());
        }

    }

    private void doCompleteOptionValue(InvocationProviders invocationProviders, AeshCompleteOperation completeOperation,
                                       ProcessedOption currentOption) {
        //ProcessedOption currentOption = parser.lastParsedOption();
        //String value = parser.getProcessedCommand().completeStatus().value();
        String value = currentOption.getLastValue();
        if(value == null)
            value = "";

        //set offset
        //completeOperation.setOffset(completeOperation.getCursor() - value.length());

        if(currentOption.completer() != null &&
                currentOption.activator().isActivated(parser.getProcessedCommand())) {
            CompleterInvocation completions =
                    invocationProviders.getCompleterProvider().enhanceCompleterInvocation(
                            new CompleterData(completeOperation.getContext(), value, parser.getCommand()));

            currentOption.completer().complete(completions);
            completeOperation.addCompletionCandidatesTerminalString(completions.getCompleterValues());
            verifyCompleteValue(completeOperation, completions, value);
            //calcOffset(value, completeOperation, completions);
            completeOperation.setIgnoreOffset(completions.doIgnoreOffset());
            completeOperation.setIgnoreStartsWith(completions.isIgnoreStartsWith());

            //if(completions.getCompleterValues().size() == 1) {
            //    verifyCompleteValue(completeOperation, completions, value);
            //}
        }
        //only try to complete default values if completer is null
        else if(currentOption.getDefaultValues().size() > 0) {
            CompleterInvocation completions =
                    invocationProviders.getCompleterProvider().enhanceCompleterInvocation(
                            new CompleterData(completeOperation.getContext(), value, parser.getCommand()));
            new DefaultValueOptionCompleter(currentOption.getDefaultValues()).complete(completions);
            completeOperation.addCompletionCandidatesTerminalString(completions.getCompleterValues());
            //calcOffset(value, completeOperation, completions);
            completeOperation.setIgnoreOffset(completions.doIgnoreOffset());
            completeOperation.setIgnoreStartsWith(completions.isIgnoreStartsWith());

            if(completions.getCompleterValues().size() == 1) {

            }
        }
    }

    private void verifyCompleteValue(AeshCompleteOperation completeOperation,
                                     CompleterInvocation completions,
                                     String value) {
        //if the contain spaces we need to add the number of spaces to the size
        // of the value.length since they are chopped off during parsing
        if(value.indexOf(Parser.SPACE_CHAR) > 0) {
            completeOperation.setOffset( completeOperation.getCursor() -
                    (value.length() + Parser.findNumberOfSpacesInWord(value)));
        }
        else if(value != null)
            completeOperation.setOffset( completeOperation.getCursor() - value.length());
        else
            completeOperation.setOffset( completeOperation.getCursor() - completions.getOffset());

        if(completions.getCompleterValues().size() == 1) {
            if (completeOperation.getCompletionCandidates().get(0).containSpaces())
                completeOperation.getCompletionCandidates().get(0).switchSpacesToEscapedSpaces();

            completeOperation.doAppendSeparator(completions.isAppendSpace());
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

}
