/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aesh.command.Command;
import org.aesh.command.CommandLifecycle;
import org.aesh.command.HelpEntry;
import org.aesh.command.HelpSectionProvider;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.provider.NullHelpSectionProvider;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.map.MapCommand;
import org.aesh.command.map.MapCommandPopulator;
import org.aesh.command.map.MapProcessedCommand;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.parser.MutuallyExclusiveOptionException;
import org.aesh.command.parser.OptionParserException;
import org.aesh.command.parser.RequiredOptionException;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.console.AeshContext;
import org.aesh.parser.LineParser;
import org.aesh.parser.ParsedLine;
import org.aesh.parser.ParsedLineIterator;
import org.aesh.parser.ParsedWord;
import org.aesh.parser.ParserStatus;
import org.aesh.terminal.utils.ANSIBuilder;
import org.aesh.terminal.utils.Config;

/**
 * A simple command line parser.
 * It parses a given string based on the Command given and
 *
 * It can also print a formatted usage/help information.
 *
 * @author Aesh team
 */
public class AeshCommandLineParser<CI extends CommandInvocation> implements CommandLineParser<CI> {

    private final ProcessedCommand<Command<CI>, CI> processedCommand;
    private List<CommandLineParser<CI>> childParsers;
    private Map<String, Class<? extends Command>> lazyChildClasses;
    private InvocationProviders storedInvocationProviders;
    private boolean isChild = false;
    private ProcessedOption lastParsedOption;
    private boolean parsedCommand = false;
    private LineParser lineParser;
    private CompleteStatus completeStatus;
    private AeshCommandLineParser<CI> parent;
    private boolean ansiMode = true;

    @SuppressWarnings("unchecked")
    public AeshCommandLineParser(ProcessedCommand<? extends Command<CI>, CI> processedCommand) {
        this.processedCommand = (ProcessedCommand<Command<CI>, CI>) processedCommand;
    }

    @Override
    public void addChildParser(CommandLineParser<CI> commandLineParser) throws CommandLineParserException {
        if (processedCommand.hasArgument() || processedCommand.hasArguments())
            throw new CommandLineParserException("Group commands can not have arguments defined");
        if (childParsers == null)
            childParsers = new ArrayList<>();
        commandLineParser.setChild(true);
        childParsers.add(commandLineParser);
        if (commandLineParser instanceof AeshCommandLineParser)
            ((AeshCommandLineParser<CI>) commandLineParser).setParent(this);
    }

    public List<CommandLineParser<CI>> getChildParsers() {
        return childParsers;
    }

    @SuppressWarnings("unchecked")
    public void addLazyChild(String name, Class<? extends Command> clazz) throws CommandLineParserException {
        if (processedCommand.hasArgument() || processedCommand.hasArguments())
            throw new CommandLineParserException("Group commands can not have arguments defined");
        if (lazyChildClasses == null)
            lazyChildClasses = new HashMap<>();
        lazyChildClasses.put(name, clazz);
    }

    public void storeInvocationProviders(InvocationProviders providers) {
        this.storedInvocationProviders = providers;
    }

    @SuppressWarnings("unchecked")
    private void resolveAllLazyChildren() {
        if (lazyChildClasses == null || lazyChildClasses.isEmpty())
            return;
        AeshCommandContainerBuilder<CI> builder = new AeshCommandContainerBuilder<>();
        for (Class<? extends Command> clazz : new ArrayList<>(lazyChildClasses.values())) {
            try {
                CommandContainer<CI> container = builder.create(clazz);
                addChildParser(container.getParser());
                applyStoredProviders(container.getParser());
            } catch (CommandLineParserException e) {
                // best-effort: skip unresolvable children
            }
        }
        lazyChildClasses.clear();
    }

    @SuppressWarnings("unchecked")
    private CommandLineParser<CI> resolveLazyChild(String name) {
        if (lazyChildClasses == null)
            return null;
        Class<? extends Command> clazz = lazyChildClasses.remove(name);
        if (clazz == null)
            return null;
        try {
            AeshCommandContainerBuilder<CI> builder = new AeshCommandContainerBuilder<>();
            CommandContainer<CI> container = builder.create(clazz);
            addChildParser(container.getParser());
            applyStoredProviders(container.getParser());
            return container.getParser();
        } catch (CommandLineParserException e) {
            return null;
        }
    }

    private void applyStoredProviders(CommandLineParser<CI> child) {
        if (storedInvocationProviders != null) {
            child.getProcessedCommand().updateInvocationProviders(storedInvocationProviders);
        }
    }

    @Override
    public void setChild(boolean child) {
        isChild = child;
    }

    private void setParent(AeshCommandLineParser<CI> parent) {
        this.parent = parent;
    }

    @Override
    public CommandLineParser<CI> parsedCommand() {
        if (parsedCommand)
            return this;
        else if (isGroupCommand() && childParsers != null) {
            CommandLineParser<CI> correct;
            for (CommandLineParser<CI> child : childParsers) {
                correct = child.parsedCommand();
                if (correct != null)
                    return correct;
            }
        }
        return null;
    }

    @Override
    public void complete(AeshCompleteOperation completeOperation, InvocationProviders invocationProviders) {

        complete(completeOperation,
                new LineParser().parseLine(completeOperation.getBuffer(), completeOperation.getCursor(), true),
                invocationProviders);
    }

    @Override
    public void complete(AeshCompleteOperation completeOperation, ParsedLine line, InvocationProviders invocationProviders) {
        //first parse
        parse(line.iterator(), Mode.COMPLETION);
        //then use completion parser to populate completeOperation
        parsedCommand().getCompletionParser().injectValuesAndComplete(completeOperation, invocationProviders, line);
    }

    @Override
    public List<String> getAllNames() {
        if (isGroupCommand()) {
            resolveAllLazyChildren();
            List<CommandLineParser<CI>> parsers = getChildParsers();
            List<String> names = new ArrayList<>(parsers.size());
            for (CommandLineParser child : parsers) {
                names.add(processedCommand.name() + " " + child.getProcessedCommand().name());
            }
            return names;
        } else {
            List<String> names = new ArrayList<>(1);
            names.add(processedCommand.name());
            return names;
        }
    }

    public boolean isChild() {
        return isChild;
    }

    @Override
    public CommandLineParser<CI> getChildParser(String name) {
        if (!isGroupCommand())
            return null;
        if (childParsers != null) {
            for (CommandLineParser<CI> clp : childParsers) {
                if (clp.getProcessedCommand().name().equals(name))
                    return clp;
            }
        }
        return resolveLazyChild(name);
    }

    @Override
    public List<CommandLineParser<CI>> getAllChildParsers() {
        if (isGroupCommand()) {
            resolveAllLazyChildren();
            return getChildParsers();
        } else
            return new ArrayList<>();
    }

    @Override
    public ProcessedCommand<Command<CI>, CI> getProcessedCommand() {
        return processedCommand;
    }

    @Override
    public Command<CI> getCommand() {
        return processedCommand.getCommand();
    }

    @Override
    public CommandLineCompletionParser getCompletionParser() {
        return new AeshCommandLineCompletionParser<>(this);
    }

    @Override
    public CommandPopulator<Object, CI> getCommandPopulator() {
        return processedCommand.getCommandPopulator();
    }

    @Override
    public void populateObject(String line, InvocationProviders invocationProviders, AeshContext aeshContext, Mode mode)
            throws CommandLineParserException, OptionValidatorException {
        //first parse, then populate
        parse(line, mode);
        if (mode == Mode.VALIDATE && getProcessedCommand().parserExceptions().size() > 0) {
            throw getProcessedCommand().parserExceptions().get(0);
        } else {
            doPopulate(processedCommand, invocationProviders, aeshContext, mode);
        }
    }

    @Override
    public void doPopulate(ProcessedCommand<Command<CI>, CI> processedCommand, InvocationProviders invocationProviders,
            AeshContext aeshContext, Mode mode) throws CommandLineParserException, OptionValidatorException {
        getCommandPopulator().populateObject(processedCommand, invocationProviders, aeshContext, mode);
        if (isGroupCommand() && childParsers != null) {
            for (CommandLineParser<CI> parser : childParsers) {
                parser.doPopulate(parser.getProcessedCommand(), invocationProviders, aeshContext, mode);
            }
            propagateInheritedOptions();
        }
    }

    /**
     * Returns a usage String based on the defined command and options.
     * Useful when printing "help" info etc.
     */
    @Override
    public String printHelp() {
        resolveAllLazyChildren();
        boolean showAll = processedCommand.isFullHelpRequested();
        List<CommandLineParser<CI>> parsers = getChildParsers();
        Map<String, List<HelpEntry>> additionalSections = resolveAdditionalSections();
        boolean hasChildren = parsers != null && parsers.size() > 0;
        boolean hasAdditional = !additionalSections.isEmpty();

        if (hasChildren || hasAdditional) {
            StringBuilder sb = new StringBuilder();
            sb.append(processedCommand.printHelp(helpNames(), false, showAll));

            int maxLength = 0;
            if (hasChildren) {
                for (CommandLineParser child : parsers) {
                    int length = child.getProcessedCommand().name().length();
                    if (length > maxLength)
                        maxLength = length;
                }
            }
            for (List<HelpEntry> entries : additionalSections.values()) {
                for (HelpEntry entry : entries) {
                    if (entry.name().length() > maxLength)
                        maxLength = entry.name().length();
                }
            }

            Map<String, List<String>> outputGroups = new LinkedHashMap<>();
            if (hasChildren) {
                for (CommandLineParser<CI> child : parsers) {
                    String group = child.getProcessedCommand().helpGroup();
                    outputGroups.computeIfAbsent(group.isEmpty() ? "" : group, k -> new ArrayList<>())
                            .add(child.getFormattedCommand(4, maxLength + 2));
                }
            }
            for (Map.Entry<String, List<HelpEntry>> section : additionalSections.entrySet()) {
                List<String> lines = outputGroups.computeIfAbsent(section.getKey(), k -> new ArrayList<>());
                for (HelpEntry he : section.getValue())
                    lines.add(formatHelpEntry(he, 4, maxLength + 2));
            }

            boolean hasNamedGroups = outputGroups.size() > 1 || !outputGroups.containsKey("");

            if (hasNamedGroups) {
                for (Map.Entry<String, List<String>> entry : outputGroups.entrySet()) {
                    if (!entry.getKey().isEmpty()) {
                        sb.append(Config.getLineSeparator())
                                .append(entry.getKey()).append(":")
                                .append(Config.getLineSeparator());
                        for (String line : entry.getValue())
                            sb.append(line).append(Config.getLineSeparator());
                    }
                }
                List<String> defaultGroup = outputGroups.get("");
                if (defaultGroup != null && !defaultGroup.isEmpty()) {
                    sb.append(Config.getLineSeparator())
                            .append("Other:").append(Config.getLineSeparator());
                    for (String line : defaultGroup)
                        sb.append(line).append(Config.getLineSeparator());
                }
            } else {
                sb.append(Config.getLineSeparator())
                        .append(processedCommand.name()).append(" commands:")
                        .append(Config.getLineSeparator());
                for (String line : outputGroups.getOrDefault("", Collections.emptyList()))
                    sb.append(line).append(Config.getLineSeparator());
            }

            return sb.toString();
        } else
            return processedCommand.printHelp(helpNames(), false, showAll);
    }

    private Map<String, List<HelpEntry>> resolveAdditionalSections() {
        HelpSectionProvider provider = processedCommand.getHelpSectionProvider();
        if (provider != null)
            return provider.getAdditionalSections();

        Class<? extends HelpSectionProvider> providerClass = processedCommand.getHelpSectionProviderClass();
        if (providerClass != null && providerClass != NullHelpSectionProvider.class) {
            try {
                provider = providerClass.getDeclaredConstructor().newInstance();
                return provider.getAdditionalSections();
            } catch (Exception e) {
                // fall through
            }
        }
        return Collections.emptyMap();
    }

    private String formatHelpEntry(HelpEntry entry, int offset, int descriptionStart) {
        ANSIBuilder ansiBuilder = ANSIBuilder.builder(ansiMode);
        if (offset > 0)
            ansiBuilder.append(String.format("%" + offset + "s", ""));
        ansiBuilder.blueText(entry.name());
        int descOffset = descriptionStart - entry.name().length();
        if (descOffset > 0)
            ansiBuilder.append(String.format("%" + descOffset + "s", ""));
        else
            ansiBuilder.append(" ");
        if (entry.description() != null)
            ansiBuilder.append(entry.description());
        return ansiBuilder.toString();
    }

    private String helpNames() {
        if (isChild()) {
            return parent.helpNames() + " " + processedCommand.name();
        }
        return processedCommand.name();
    }

    /**
     * Parse a command line with the defined command as base of the rules.
     * If any options are found, but not defined in the command object an
     * CommandLineParserException will be thrown.
     * Also, if a required option is not found or options specified with value,
     * but is not given any value an OptionParserException will be thrown.
     *
     * @param line input
     */
    @Override
    public void parse(String line) {
        parse(line, Mode.STRICT);
    }

    @Override
    public void parse(ParsedLineIterator iterator, Mode mode) {
        clear();
        Command<CI> cmd = processedCommand.getCommand();
        if (mode != Mode.COMPLETION && cmd instanceof CommandLifecycle) {
            ((CommandLifecycle) cmd).beforeParse();
        }
        if (iterator.hasNextWord()) {
            String command = iterator.pollWord();
            if (processedCommand.name().equals(command)
                    || processedCommand.getAliases().contains(command)) {
                if (isGroupCommand() && iterator.hasNextWord()) {
                    CommandLineParser<CI> clp = getChildParser(iterator.peekWord());
                    if (clp == null) {
                        //if the user have written garbage in the next word, we need to check
                        // eg: group GARBAGE <tab>
                        if (iterator.isNextWordCursorWord() ||
                                iterator.peekWord().startsWith("--") || iterator.peekWord().startsWith("-"))
                            doParse(iterator, mode);
                        else {
                            processedCommand
                                    .addParserException(new CommandLineParserException("'" + command + " " + iterator.peekWord()
                                            + "' is not part of the " + command + " commands. See 'help " + command + "'."));
                            if (mode == Mode.COMPLETION) {
                                parsedCommand = true;
                                processedCommand.setCompleteStatus(new CompleteStatus(CompleteStatus.Status.INVALID_INPUT, ""));
                            }
                        }
                    }
                    //we have a group command
                    else {
                        //remove the child name
                        clp.parse(iterator, mode);
                    }
                } else
                    doParse(iterator, mode);
            }
        } else if (iterator.parserError() != null)
            processedCommand.addParserException(new CommandLineParserException(iterator.parserError()));
    }

    private void doParse(ParsedLineIterator iter, Mode mode) {
        parsedCommand = true;
        if (mode == Mode.COMPLETION)
            doParseCompletion(iter);
        else {
            try {
                boolean argumentMarker = false;
                while (iter.hasNextWord()) {
                    ParsedWord word = iter.peekParsedWord();
                    if (argumentMarker || processedCommand.disableParsing()) {
                        setArgStatus(word.word());
                        iter.pollParsedWord();
                    } else {
                        lastParsedOption = processedCommand.searchAllOptions(word.word());
                        if (lastParsedOption == null && parent != null) {
                            lastParsedOption = searchParentInheritedOption(word.word());
                        }
                        if (lastParsedOption != null) {
                            lastParsedOption.parser().parse(iter, lastParsedOption);
                        } else {
                            //if we have a -- and its not at the end of the line it is used as a
                            //marker to signal that all the values after it are arguments, so we will ignore this
                            if (word.word().equals("--") && !iter.isNextWordCursorWord()) {
                                argumentMarker = true;
                            } else {
                                // Unknown commands are possible with a dynamic command (MapCommand)
                                // In this case we shouldn't validate the option and pass it down to
                                // the populator for Map injection.
                                boolean unknown = false;
                                if (!processedCommand.stopAtFirstPositional() && word.word().startsWith("-")) {
                                    if (word.word().startsWith("--") || word.word().length() == 2) {
                                        // invalid short names and long names should be rejected.
                                        if (!(processedCommand.getCommand() instanceof MapCommand)) {
                                            processedCommand.addParserException(
                                                    new OptionParserException("The option " + word.word()
                                                            + " is unknown."));
                                        } else {
                                            unknown = true;
                                        }
                                    }
                                }
                                if (unknown) {
                                    // Pass down the option directly to the populator.
                                    MapCommandPopulator pop = (MapCommandPopulator) processedCommand.getCommandPopulator();
                                    pop.addUnknownOption(word.word());
                                } else if (isGroupCommand()) {
                                    // Check if this word is a subcommand name.
                                    // When group command options are parsed before the subcommand,
                                    // e.g. "cli -c cliarg command -c commandarg", we need to
                                    // recognize "command" as a subcommand and delegate parsing.
                                    CommandLineParser<CI> clp = getChildParser(word.word());
                                    if (clp != null) {
                                        // Do NOT poll the word - child's parse() expects the
                                        // command name to still be in the iterator.
                                        // Reset parsedCommand so parsedCommand() returns the child, not us.
                                        parsedCommand = false;
                                        clp.parse(iter, mode);
                                        return;
                                    } else {
                                        setArgStatus(word.word());
                                        if (processedCommand.stopAtFirstPositional())
                                            argumentMarker = true;
                                    }
                                } else {
                                    setArgStatus(word.word());
                                    if (processedCommand.stopAtFirstPositional())
                                        argumentMarker = true;
                                }
                            }
                            iter.pollParsedWord();
                        }
                    }
                }
            } catch (OptionParserException ope) {
                processedCommand.addParserException(ope);
            }
            if (mode == Mode.STRICT) {
                ProcessedCommand copy = processedCommand;
                if (copy instanceof MapProcessedCommand) {
                    MapCommand mc = (MapCommand) copy.getCommand();
                    if (!mc.checkForRequiredOptions(iter.baseLine())) {
                        return;
                    }
                }
                RequiredOptionException re = checkForMissingRequiredOptions(processedCommand);
                if (re != null)
                    processedCommand.addParserException(re);
                MutuallyExclusiveOptionException me = checkForMutuallyExclusiveOptions(processedCommand);
                if (me != null)
                    processedCommand.addParserException(me);
            }
        }
    }

    private void setArgStatus(String word) {
        if (processedCommand.hasArgumentWithNoValue()) {
            processedCommand.getArgument().addValue(word);
        } else if (processedCommand.hasArguments()) {
            processedCommand.getArguments().addValue(word);
        } else {
            processedCommand.addParserException(
                    new OptionParserException(
                            "A value " + word + " was given as an argument, but the command do not support it."));
        }
    }

    private void doParseCompletion(ParsedLineIterator iter) {
        ProcessedCommand copy = processedCommand;
        if (copy instanceof MapProcessedCommand) {
            ((MapProcessedCommand) copy).setMode(Mode.COMPLETION);
        }
        if (!iter.hasNextWord()) {
            if (isGroupCommand())
                processedCommand.setCompleteStatus(new CompleteStatus(CompleteStatus.Status.GROUP_COMMAND, ""));
            else {
                //child commands that ends after its name, must be able to append space
                if (iter.baseLine().size() == (iter.baseLine().selectedIndex() + 1) &&
                        lastParsedOption == null) {
                    //append space
                    if (iter.baseLine().status() == ParserStatus.OK)
                        processedCommand.setCompleteStatus(new CompleteStatus(CompleteStatus.Status.APPEND_SPACE, ""));
                    //we have unclosed quote, lets parse it as an argument
                    else
                        processedCommand.setCompleteStatus(new CompleteStatus(CompleteStatus.Status.ARGUMENT, ""));
                }
                //we list all the options
                else
                    processedCommand.setCompleteStatus(new CompleteStatus(CompleteStatus.Status.COMPLETE_OPTION, ""));
            }
        } else {
            try {
                boolean argumentMarker = false; //argumentMarker is set to true if we have found "--" inside the line
                while (iter.hasNextWord()) {
                    //first check if we have passed the selected word, if so lets stop
                    if (iter.baseLine().selectedIndex() > -1 &&
                            iter.pastCursorWord() &&
                            processedCommand.completeStatus() != null)
                        return;
                    ParsedWord word = iter.peekParsedWord();
                    //first check if argumentMarker has been set
                    if (argumentMarker) {
                        setCompletionArgStatus(word.word());
                        iter.pollParsedWord();
                    } else {
                        lastParsedOption = processedCommand.searchAllOptions(word.word());
                        if (lastParsedOption == null && parent != null) {
                            lastParsedOption = searchParentInheritedOption(word.word());
                        }
                        if (lastParsedOption != null) {
                            //if current word is cursor word, we need to check if the current option name
                            //might be part of another option name: eg: list and listFolders
                            if (iter.isNextWordCursorWord() && !word.word().contains("=")
                                    && processedCommand.findPossibleLongNames(word.word()).size() > 1) {
                                processedCommand.setCompleteStatus(
                                        new CompleteStatus(CompleteStatus.Status.LONG_OPTION, word.word().substring(2)));
                                iter.pollParsedWord();
                            } else {
                                lastParsedOption.parser().parse(iter, lastParsedOption);
                                if (!iter.hasNextWord()) {
                                    if (lastParsedOption.hasValue() || iter.baseLine().spaceAtEnd())
                                        processedCommand.setCompleteStatus(
                                                new CompleteStatus(CompleteStatus.Status.COMPLETE_OPTION, ""));
                                    //if the option do not have any value, set missing value status for easier processing
                                    else
                                        processedCommand.setCompleteStatus(
                                                new CompleteStatus(CompleteStatus.Status.OPTION_MISSING_VALUE, ""));
                                }
                            }
                        }
                        //if we have -- that stands alone it's a marker for separation of options and arguments
                        else if (word.word().equals("--") && !iter.isNextWordCursorWord()) {
                            argumentMarker = true;
                            iter.pollParsedWord();
                        }
                        //got a partial option
                        else if (word.word().startsWith("--")) {
                            processedCommand.setCompleteStatus(
                                    new CompleteStatus(CompleteStatus.Status.LONG_OPTION, word.word().substring(2)));
                            iter.pollParsedWord();
                        } else if (word.word().startsWith("-")) {
                            processedCommand.setCompleteStatus(
                                    new CompleteStatus(CompleteStatus.Status.SHORT_OPTION, word.word().substring(1)));
                            iter.pollParsedWord();
                        }
                        //we're completing arguments or group command names
                        else {
                            //check for group command completion even after options have been parsed
                            if (isGroupCommand()) {
                                // Check if this word matches a known child parser
                                CommandLineParser<CI> clp = getChildParser(word.word());
                                if (clp != null && !iter.isNextWordCursorWord()) {
                                    // Fully matched subcommand, delegate to it
                                    parsedCommand = false;
                                    clp.parse(iter, Mode.COMPLETION);
                                    return;
                                }
                                if (iter.isNextWordCursorWord())
                                    processedCommand.setCompleteStatus(
                                            new CompleteStatus(CompleteStatus.Status.GROUP_COMMAND, word.word()));
                                else if (iter.baseLine().cursorAtEnd() && iter.baseLine().spaceAtEnd())
                                    processedCommand
                                            .setCompleteStatus(new CompleteStatus(CompleteStatus.Status.GROUP_COMMAND, ""));
                                else if (processedCommand.stopAtFirstPositional())
                                    argumentMarker = true;
                            } else if (iter.isNextWordCursorWord()) {
                                if (processedCommand.getArguments() != null ||
                                        (processedCommand.getArgument() != null
                                                && processedCommand.getArgument().getValue() == null)) {
                                    processedCommand
                                            .setCompleteStatus(new CompleteStatus(CompleteStatus.Status.ARGUMENT, word.word()));
                                } else {
                                    processedCommand
                                            .setCompleteStatus(new CompleteStatus(CompleteStatus.Status.ARGUMENT_ERROR, null));
                                }
                            } else {
                                setCompletionArgStatus(word.word());
                                if (processedCommand.stopAtFirstPositional())
                                    argumentMarker = true;
                            }
                            iter.pollParsedWord();
                        }
                    }
                }
                if (argumentMarker && processedCommand.completeStatus() == null)
                    setCompletionArgStatus(null);
            } catch (OptionParserException e) {
                //TODO: needs to be improved
                //ignored for now
                processedCommand.setCompleteStatus(new CompleteStatus(CompleteStatus.Status.OPTION_MISSING_VALUE, ""));
            }
        }
    }

    private void setCompletionArgStatus(String word) {
        //add the value to argument/arguments
        if (processedCommand.hasArgumentWithNoValue()) {
            processedCommand.getArgument().addValue(word);
            processedCommand.setCompleteStatus(new CompleteStatus(CompleteStatus.Status.ARGUMENT, null));
        } else if (processedCommand.hasArguments()) {
            processedCommand.getArguments().addValue(word);
            processedCommand.setCompleteStatus(new CompleteStatus(CompleteStatus.Status.ARGUMENT, null));
        } else if (processedCommand.hasArgument()) {
            //singular argument already filled and no @Arguments to overflow into
            processedCommand.setCompleteStatus(new CompleteStatus(CompleteStatus.Status.ARGUMENT_ERROR, null));
        }
    }

    private RequiredOptionException checkForMissingRequiredOptions(ProcessedCommand<? extends Command<CI>, CI> command) {
        for (ProcessedOption o : command.getOptions()) {
            if (doCheckForMissingRequiredOption(o))
                return new RequiredOptionException("Option: " + o.getDisplayName() + " is required for this command.");
        }
        if (command.getArgument() != null) {
            if (doCheckForMissingRequiredOption(command.getArgument()))
                return generateRequiredExceptionFor(command.getArgument(), false);
        }
        if (command.getArguments() != null) {
            if (doCheckForMissingRequiredOption(command.getArguments()))
                return generateRequiredExceptionFor(command.getArguments(), true);
        }

        return null;
    }

    private RequiredOptionException generateRequiredExceptionFor(ProcessedOption argument, boolean plural) {
        final String description = argument.description();
        String msg;
        if (description != null && !description.isEmpty()) {
            msg = description;
        } else {
            msg = "Argument '" + argument.getFieldName() + "'";
        }
        msg += (plural ? " are " : " is ") + "required for this command.";
        return new RequiredOptionException(msg);
    }

    private boolean doCheckForMissingRequiredOption(ProcessedOption o) {
        if (o.isRequired() && o.getValue() == null) {
            boolean found = false;
            for (ProcessedOption po : processedCommand.getOptions()) {
                if (po.getValue() != null && po.doOverrideRequired()) {
                    found = true;
                    break;
                }
            }
            if (!found)
                return true;
        }
        return false;
    }

    private MutuallyExclusiveOptionException checkForMutuallyExclusiveOptions(
            ProcessedCommand<? extends Command<CI>, CI> command) {
        for (ProcessedOption o : command.getOptions()) {
            if (o.getExclusiveWith().isEmpty() || o.getValue() == null)
                continue;
            for (String exclusiveName : o.getExclusiveWith()) {
                ProcessedOption other = command.findLongOptionNoActivatorCheck(exclusiveName);
                if (other != null && other.getValue() != null) {
                    return new MutuallyExclusiveOptionException(
                            "Options --" + o.name() + " and --" + other.name() + " are mutually exclusive.");
                }
            }
        }
        return null;
    }

    @Override
    public ProcessedOption lastParsedOption() {
        return lastParsedOption;
    }

    /**
     * Parse a command line with the defined command as base of the rules.
     * If any options are found, but not defined in the command object an
     * CommandLineParserException will be thrown.
     * Also, if a required option is not found or options specified with value,
     * but is not given any value an CommandLineParserException will be thrown.
     *
     * @param line input
     * @param mode parser mode
     */
    @Override
    public void parse(String line, Mode mode) {
        if (lineParser == null)
            lineParser = new LineParser();
        parse(lineParser.parseLine(line, line.length(), true).iterator(), mode);
    }

    @Override
    public void clear() {
        //if this is the parsed command, clear it
        if (parsedCommand) {
            processedCommand.clear();
            lastParsedOption = null;
            parsedCommand = false;
            completeStatus = null;
        }
        //else find the parsed command and clear that one
        else {
            if (processedCommand != null)
                processedCommand.clear();

            CommandLineParser parsed = parsedCommand();
            if (parsed != null)
                parsed.clear();
        }
    }

    @Override
    public boolean isGroupCommand() {
        List<CommandLineParser<CI>> parsers = getChildParsers();
        if (parsers != null && parsers.size() > 0)
            return true;
        return lazyChildClasses != null && !lazyChildClasses.isEmpty();
    }

    @Override
    public String getFormattedCommand(int offset, int descriptionStart) {

        ANSIBuilder ansiBuilder = ANSIBuilder.builder(ansiMode);

        if (offset > 0)
            ansiBuilder.append(String.format("%" + offset + "s", ""));

        ansiBuilder.blueText(getProcessedCommand().name());

        int descOffset = descriptionStart - getProcessedCommand().name().length();

        if (descOffset > 0)
            ansiBuilder.append(String.format("%" + descOffset + "s", ""));
        else
            ansiBuilder.append(" ");

        ansiBuilder.append(getProcessedCommand().description());

        return ansiBuilder.toString();
    }

    /**
     * Search parent parsers for an inherited option matching the given input.
     */
    private ProcessedOption searchParentInheritedOption(String word) {
        AeshCommandLineParser<CI> p = parent;
        while (p != null) {
            ProcessedOption option = p.getProcessedCommand().searchAllOptions(word);
            if (option != null && option.isInherited())
                return option;
            p = p.parent;
        }
        return null;
    }

    /**
     * After populating parent and children, propagate inherited option values
     * from this (parent) command into parsed child commands that have matching fields.
     */
    private void propagateInheritedOptions() {
        for (ProcessedOption parentOpt : processedCommand.getOptions()) {
            if (!parentOpt.isInherited())
                continue;
            Object value = parentOpt.getFieldValue(getCommand());
            if (value == null)
                continue;
            for (CommandLineParser<CI> child : getChildParsers()) {
                if (child.parsedCommand() == null)
                    continue;
                Command<CI> childCmd = child.getCommand();
                ProcessedOption childOpt = child.getProcessedCommand().findLongOptionNoActivatorCheck(parentOpt.name());
                if (childOpt != null) {
                    if (childOpt.getValues() != null && !childOpt.getValues().isEmpty())
                        continue;
                    childOpt.setFieldValue(childCmd, value);
                } else {
                    try {
                        Field childField = findField(childCmd.getClass(), parentOpt.getFieldName());
                        if (childField == null)
                            continue;
                        if (!Modifier.isPublic(childField.getModifiers()))
                            childField.setAccessible(true);
                        childField.set(childCmd, value);
                    } catch (Exception e) {
                        // inherited value propagation is best-effort
                    }
                }
            }
        }
    }

    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            for (Field f : clazz.getDeclaredFields()) {
                if (f.getName().equals(fieldName))
                    return f;
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    @Override
    public void updateAnsiMode(boolean mode) {
        this.ansiMode = mode;
    }

    @Override
    public String toString() {
        return "AeshCommandLineParser{" +
                "processedCommand=" + processedCommand +
                ", childParsers=" + childParsers +
                ", isChild=" + isChild +
                ", lastParsedOption=" + lastParsedOption +
                ", parsedCommand=" + parsedCommand +
                ", completeStatus=" + completeStatus +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof AeshCommandLineParser))
            return false;

        AeshCommandLineParser that = (AeshCommandLineParser) o;

        return processedCommand.equals(that.processedCommand);

    }

    @Override
    public int hashCode() {
        return processedCommand.hashCode();
    }
}
