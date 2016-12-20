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
package org.aesh.cl.parser;

import org.aesh.cl.populator.CommandPopulator;
import org.aesh.cl.CommandLine;
import org.aesh.cl.internal.ProcessedCommand;
import org.aesh.console.command.Command;
import org.aesh.parser.AeshLine;

import java.util.List;

/**
 * A command line parser that is created based on a given
 * ProcessedCommand.
 *
 * It must also be able to inject values from a line into a Command object
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface CommandLineParser<C extends Command> {

    /**
     * @return the processed command this parser is generated from
     */
    ProcessedCommand<C> getProcessedCommand();

    /**
     * @return the actual command
     */
    C getCommand();

    /**
     * @return completion parser created to work on this command
     */
    CommandLineCompletionParser getCompletionParser();

    List<String> getAllNames();

    /**
     * @param name command
     * @return child parser that matches the name
     */
    CommandLineParser<? extends Command> getChildParser(String name);

    void addChildParser(CommandLineParser<? extends Command> childParser);

    /**
     * @return all the child parser
     */
    List<CommandLineParser<? extends Command>> getAllChildParsers();

    /**
     * @return command populator to work on this command
     */
    CommandPopulator getCommandPopulator();

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
     * The options found will be returned as a {@link CommandLine} object where
     * they can be queried after.
     *
     * @param line input
     * @return CommandLine
     */
    CommandLine<? extends Command> parse(String line);

    /**
     * Parse a command line with the defined command as base of the rules.
     * If any options are found, but not defined in the command object an
     * CommandLineParserException will be thrown.
     * Also, if a required option is not found or options specified with value,
     * but is not given any value an CommandLineParserException will be thrown.
     *
     * The options found will be returned as a {@link CommandLine} object where
     * they can be queried after.
     *
     * @param line input
     * @param ignoreRequirements if we should ignore
     * @return CommandLine
     */
    CommandLine<? extends Command> parse(String line, boolean ignoreRequirements);

    CommandLine<? extends Command> parse(AeshLine line, boolean ignoreRequirements);

    /**
     * Parse a command line with the defined command as base of the rules.
     * This method is useful when parsing a command line program thats not
     * in aesh, but rather a standalone command that want to parse input
     * parameters.
     *
     * @param lines input
     * @param ignoreRequirements if we should ignore
     * @return CommandLine
     */
    CommandLine<? extends Command> parse(List<String> lines, boolean ignoreRequirements);

    void clear();

    boolean isGroupCommand();

    void setChild(boolean b);
}
