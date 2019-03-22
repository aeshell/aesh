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
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.Command;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.readline.AeshContext;
import org.aesh.parser.ParsedLine;
import org.aesh.parser.ParsedLineIterator;

import java.util.List;

/**
 * A command line parser that is created based on a given
 * ProcessedCommand.
 *
 * It must also be able to inject values from a line into a Command object
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface CommandLineParser<CI extends CommandInvocation> {

    /**
     * @return the processed command this parser is generated from
     */
    ProcessedCommand<Command<CI>, CI> getProcessedCommand();

    /**
     * @return the actual command
     */
    Command<CI> getCommand();

    /**
     * @return completion parser created to work on this command
     */
    CommandLineCompletionParser getCompletionParser();

    void complete(AeshCompleteOperation completeOperation, ParsedLine line, InvocationProviders invocationProviders);

    List<String> getAllNames();

    /**
     * @param name command
     * @return child parser that matches the name
     */
    CommandLineParser<CI> getChildParser(String name);

    void addChildParser(CommandLineParser<CI> childParser) throws CommandLineParserException;

    /**
     * @return all the child parser
     */
    List<CommandLineParser<CI>> getAllChildParsers();

    /**
     * @return command populator to work on this command
     */
    CommandPopulator<Object, CI> getCommandPopulator();


    /**
     * Direct call to CommandPopulator to populate this command
     *
     * @param invocationProviders providers
     * @param aeshContext context
     * @param mode validate
     * @throws CommandLineParserException parser exception
     * @throws OptionValidatorException validator exception
     */
    void populateObject(String line, InvocationProviders invocationProviders,
                        AeshContext aeshContext, Mode mode) throws CommandLineParserException, OptionValidatorException;

    /**
     * Returns a usage String based on the defined command and options.
     * Useful when printing "help" info etc.
     */
    String printHelp();

    /**
     * Parse a command line with the defined command as base of the rules.
     * If any options are found, but not defined in the command object an
     * CommandLineParserException will be thrown.
     * Also, if a required option is not found or options specified with value,
     * but is not given any value an OptionParserException will be thrown.
     *
     * The parsed values will be added to the ProcessedCommand and its options
     *
     * @param line input
     */
    void parse(String line);

    ProcessedOption lastParsedOption();

    /**
     * Parse a command line with the defined command as base of the rules.
     * If any options are found, but not defined in the command object an
     * CommandLineParserException will be thrown.
     * Also, if a required option is not found or options specified with value,
     * but is not given any value an CommandLineParserException will be thrown.
     *
     * @param line input
     * @param mode which parser mode
     */
    void parse(String line, Mode mode);

    void parse(ParsedLineIterator iterator, Mode mode);

    void clear();

    boolean isGroupCommand();

    void setChild(boolean b);

    /**
     * Will return the correct command's parser. Used when a command have child commands and
     * we need to know which parser to use for completions.
     *
     * @return  correct parser
     */
    CommandLineParser<CI> parsedCommand();

    /**
     * Parse a command line and populate the
     * @param completeOperation
     * @param invocationProviders
     */
    void complete(AeshCompleteOperation completeOperation, InvocationProviders invocationProviders);

    void doPopulate(ProcessedCommand<Command<CI>, CI> processedCommand, InvocationProviders invocationProviders, AeshContext aeshContext, Mode mode) throws CommandLineParserException, OptionValidatorException;

    String getFormattedCommand(int offset, int descriptionStart);

    void updateAnsiMode(boolean mode);

    enum Mode {
        COMPLETION, STRICT, VALIDATE, NONE
    }
}
