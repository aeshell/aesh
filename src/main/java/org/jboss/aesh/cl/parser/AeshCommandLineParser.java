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
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.populator.CommandPopulator;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.parser.AeshLine;
import org.jboss.aesh.parser.Parser;
import org.jboss.aesh.parser.ParserStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple command line parser.
 * It parses a given string based on the Command given and
 * returns a {@link org.jboss.aesh.cl.CommandLine}
 *
 * It can also print a formatted usage/help information.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandLineParser<C extends Command> implements CommandLineParser<C> {

    private final ProcessedCommand<C> processedCommand;
    private static final String EQUALS = "=";
    private List<CommandLineParser<? extends Command>> childParsers;
    private boolean isChild = false;

    public AeshCommandLineParser(ProcessedCommand<C> processedCommand) {
        this.processedCommand = processedCommand;
    }

    @Override
    public void addChildParser(CommandLineParser<? extends Command> commandLineParser) {
        if(childParsers == null)
            childParsers = new ArrayList<>();
        commandLineParser.setChild(true);
        childParsers.add(commandLineParser);
    }

    public List<CommandLineParser<? extends Command>> getChildParsers() {
        return childParsers;
    }

    @Override
    public void setChild(boolean child) {
        isChild = child;
    }

    @Override
    public List<String> getAllNames() {
        if (isGroupCommand()) {
            List<CommandLineParser<? extends Command>> parsers = getChildParsers();
            List<String> names = new ArrayList<>(parsers.size());
            for (CommandLineParser child : parsers) {
                names.add(processedCommand.getName()+" "+child.getProcessedCommand().getName());
            }
            return names;
        }
        else {
            List<String> names = new ArrayList<>(1);
            names.add(processedCommand.getName());
            return names;
        }
    }

    public boolean isChild() {
        return isChild;
    }

    @Override
    public CommandLineParser<? extends Command> getChildParser(String name) {
        if(!isGroupCommand())
            return null;
        for (CommandLineParser clp : getChildParsers()) {
            if(clp.getProcessedCommand().getName().equals(name))
                return clp;
        }
        return null;
    }

    @Override
    public List<CommandLineParser<? extends Command>> getAllChildParsers() {
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
    public CommandPopulator getCommandPopulator() {
        return processedCommand.getCommandPopulator();
    }

    /**
     * Returns a usage String based on the defined command and options.
     * Useful when printing "help" info etc.
     */
    @Override
    public String printHelp() {
        List<CommandLineParser<? extends Command>> parsers = getChildParsers();
        if (parsers != null && parsers.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(processedCommand.printHelp())
                    .append(Config.getLineSeparator())
                    .append(processedCommand.getName())
                    .append(" commands:")
                    .append(Config.getLineSeparator());
            for (CommandLineParser child : parsers)
                sb.append("    ").append(child.getProcessedCommand().getName()).append(Config.getLineSeparator());

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
     * The options found will be returned as a {@link org.jboss.aesh.cl.CommandLine} object where
     * they can be queried after.
     *
     * @param line input
     * @return CommandLine
     */
    @Override
    public CommandLine<? extends Command> parse(String line) {
        return parse(line, false);
    }

    @Override
    public CommandLine<? extends Command> parse(AeshLine line, boolean ignoreRequirements) {
        if(line.getWords().size() > 0) {
            if (processedCommand.getName().equals(line.getWords().get(0))
                    || processedCommand.getAliases().contains(line.getWords().get(0))) {
                if(isGroupCommand() && line.getWords().size() > 1) {
                   CommandLineParser<? extends Command> clp = getChildParser(line.getWords().get(1));
                    if(clp == null)
                        return parse(line.getWords(), ignoreRequirements);
                    //we have a group command
                    else
                        return clp.parse(line.getWords(), ignoreRequirements);
                }
                else
                    return parse(line.getWords(), ignoreRequirements);
            }
        }
        else if(line.getStatus() != ParserStatus.OK)
            return new CommandLine<>(new CommandLineParserException(line.getErrorMessage()));

        return new CommandLine<>(new CommandLineParserException("Command:"+ processedCommand +", not found in: "+line));
    }
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
    @Override
    public CommandLine<? extends Command> parse(String line, boolean ignoreRequirements) {
        return parse(Parser.findAllWords(line), ignoreRequirements);
    }

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
    @Override
    public CommandLine<? extends Command> parse(List<String> lines, boolean ignoreRequirements) {
        AeshCommandLineParserHelper helper = new AeshCommandLineParserHelper(processedCommand);
        lines.remove(0);
        if(isChild)
            lines.remove(0);

        clear();
        CommandLine<? extends Command> commandLine = new CommandLine<>(this);
        helper.parse(commandLine, lines, ignoreRequirements);
        return commandLine;
    }

    @Override
    public void clear() {
        processedCommand.clear();
        if(isGroupCommand()) {
            for (CommandLineParser child : getChildParsers())
                child.getProcessedCommand().clear();
        }
    }

    @Override
    public boolean isGroupCommand() {
        List<CommandLineParser<? extends Command>> parsers = getChildParsers();
        return parsers != null && parsers.size() > 0;
    }

    @Override
    public String toString() {
        return "CommandLineParser{" +
                "processedCommand=" + processedCommand +
                "command=" + processedCommand.getCommand() +
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
