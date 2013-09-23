/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.parser;

import org.jboss.aesh.cl.CommandLine;
import org.jboss.aesh.cl.internal.ProcessedCommand;

import java.util.List;

/**
 * A command line parser that is created based on a given
 * ProcessedCommand.
 *
 * It must also be able to inject values from a line into a Command object
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface CommandLineParser {

    /**
     * @return the processed command this parser is generated from
     */
    ProcessedCommand getCommand();

    /**
     * @return completion parser created to work on this command
     */
    CommandLineCompletionParser getCompletionParser();

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
     * The options found will be returned as a {@link org.jboss.aesh.cl.CommandLine} object where
     * they can be queried after.
     *
     * @param line input
     * @return CommandLine
     */
    CommandLine parse(String line);

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
    CommandLine parse(String line, boolean ignoreRequirements);

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
    CommandLine parse(List<String> lines, boolean ignoreRequirements);

}
