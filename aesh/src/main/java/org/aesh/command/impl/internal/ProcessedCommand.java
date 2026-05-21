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
package org.aesh.command.impl.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aesh.command.Command;
import org.aesh.command.DefaultValueProvider;
import org.aesh.command.HelpSectionProvider;
import org.aesh.command.activator.CommandActivator;
import org.aesh.command.impl.parser.CompleteStatus;
import org.aesh.command.impl.populator.AeshCommandPopulator;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.parser.OptionParserException;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.command.result.ResultHandler;
import org.aesh.command.validator.CommandValidator;
import org.aesh.selector.SelectorType;
import org.aesh.terminal.formatting.TerminalString;
import org.aesh.terminal.utils.ANSI;
import org.aesh.terminal.utils.Config;
import org.aesh.terminal.utils.Parser;

/**
 * @author Aesh team
 */
public class ProcessedCommand<C extends Command<CI>, CI extends CommandInvocation> {

    private static final Pattern DESCRIPTION_VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private final String name;
    private final String description;
    private final CommandValidator<C, CI> validator;
    private final ResultHandler resultHandler;
    private final CommandPopulator<Object, CI> populator;
    private final boolean disableParsing;
    private final boolean stopAtFirstPositional;
    private final boolean sortOptions;
    private DefaultValueProvider defaultValueProvider;
    private CommandActivator activator;
    private final boolean generateHelp;
    private String version;
    private String helpUrl;
    private String helpGroup = "";
    private Class<? extends HelpSectionProvider> helpSectionProviderClass;
    private HelpSectionProvider helpSectionProvider;

    private List<ProcessedOption> options;
    private ProcessedOption arguments;
    private ProcessedOption argument;
    private final List<ProcessedOption> argumentOptions;
    private final C command;
    private final List<String> aliases;
    private List<CommandLineParserException> parserExceptions;
    private CompleteStatus completeStatus;
    private java.util.function.BiConsumer<Object, Object> parentCommandInjector;
    private int optionDeclarationCounter;

    public ProcessedCommand(String name, List<String> aliases, C command,
            String description, CommandValidator<C, CI> validator,
            ResultHandler resultHandler,
            boolean generateHelp, boolean disableParsing,
            String version,
            ProcessedOption arguments, List<ProcessedOption> options,
            ProcessedOption argument,
            CommandPopulator<Object, CI> populator, CommandActivator activator) throws OptionParserException {
        this(name, aliases, command, description, validator, resultHandler, generateHelp, disableParsing,
                version, arguments, options, argument, populator, activator, null);
    }

    public ProcessedCommand(String name, List<String> aliases, C command,
            String description, CommandValidator<C, CI> validator,
            ResultHandler resultHandler,
            boolean generateHelp, boolean disableParsing,
            String version,
            ProcessedOption arguments, List<ProcessedOption> options,
            ProcessedOption argument,
            CommandPopulator<Object, CI> populator, CommandActivator activator,
            String helpUrl) throws OptionParserException {
        this(name, aliases, command, description, validator, resultHandler, generateHelp, disableParsing,
                version, arguments, options, argument, populator, activator, helpUrl, false);
    }

    public ProcessedCommand(String name, List<String> aliases, C command,
            String description, CommandValidator<C, CI> validator,
            ResultHandler resultHandler,
            boolean generateHelp, boolean disableParsing,
            String version,
            ProcessedOption arguments, List<ProcessedOption> options,
            ProcessedOption argument,
            CommandPopulator<Object, CI> populator, CommandActivator activator,
            String helpUrl, boolean stopAtFirstPositional) throws OptionParserException {
        this(name, aliases, command, description, validator, resultHandler, generateHelp, disableParsing,
                version, arguments, options, argument, populator, activator, helpUrl, stopAtFirstPositional, null);
    }

    public ProcessedCommand(String name, List<String> aliases, C command,
            String description, CommandValidator<C, CI> validator,
            ResultHandler resultHandler,
            boolean generateHelp, boolean disableParsing,
            String version,
            ProcessedOption arguments, List<ProcessedOption> options,
            ProcessedOption argument,
            CommandPopulator<Object, CI> populator, CommandActivator activator,
            String helpUrl, boolean stopAtFirstPositional,
            DefaultValueProvider defaultValueProvider) throws OptionParserException {
        this(name, aliases, command, description, validator, resultHandler, generateHelp, disableParsing,
                version, arguments, options, argument, populator, activator, helpUrl, stopAtFirstPositional,
                defaultValueProvider, false);
    }

    public ProcessedCommand(String name, List<String> aliases, C command,
            String description, CommandValidator<C, CI> validator,
            ResultHandler resultHandler,
            boolean generateHelp, boolean disableParsing,
            String version,
            ProcessedOption arguments, List<ProcessedOption> options,
            ProcessedOption argument,
            CommandPopulator<Object, CI> populator, CommandActivator activator,
            String helpUrl, boolean stopAtFirstPositional,
            DefaultValueProvider defaultValueProvider,
            boolean sortOptions) throws OptionParserException {
        this.name = name;
        this.description = description;
        this.aliases = aliases == null ? Collections.emptyList() : aliases;
        this.validator = validator;
        this.generateHelp = generateHelp;
        this.disableParsing = disableParsing;
        this.stopAtFirstPositional = stopAtFirstPositional;
        this.sortOptions = sortOptions;
        this.defaultValueProvider = defaultValueProvider;
        this.helpUrl = helpUrl;
        this.resultHandler = resultHandler;
        this.arguments = arguments;
        this.argument = argument;
        this.argumentOptions = new ArrayList<>(1);
        if (argument != null)
            this.argumentOptions.add(argument);
        this.options = new ArrayList<>(
                options.size() + (generateHelp ? 1 : 0) + (version != null && version.length() > 0 ? 1 : 0));
        this.optionDeclarationCounter = 0;
        this.command = command;
        this.activator = activator;
        if (populator == null)
            this.populator = new AeshCommandPopulator<>(this.command);
        else
            this.populator = populator;
        setOptions(options);

        if (generateHelp)
            doGenerateHelp();

        if (version != null && version.length() > 0) {
            this.version = version;
            doGenerateVersion();
        }

        parserExceptions = Collections.emptyList();

        // Capture initial field values for arguments/argument set before command
        if (command != null) {
            if (this.arguments != null)
                this.arguments.captureInitialValue(command);
            for (ProcessedOption argOpt : this.argumentOptions)
                argOpt.captureInitialValue(command);
        }
    }

    public List<ProcessedOption> getOptions() {
        return options;
    }

    public CommandActivator getActivator() {
        return activator;
    }

    public boolean isActivated(ParsedCommand parsedCommand) {
        return activator == null || activator.isActivated(parsedCommand);
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void addOption(ProcessedOption opt) throws OptionParserException {
        verifyThatNamesAreUnique(opt.shortName(), opt.name());
        opt.setDeclarationOrder(optionDeclarationCounter++);
        this.options.add(opt);
        opt.setParent(this);
        if (command != null)
            opt.captureInitialValue(command);
    }

    /**
     * Add an option without verifying name uniqueness. Use only from generated
     * (annotation-processor) code where names are validated at compile time.
     * Skipping the O(N) scan per option eliminates O(N^2) overhead during
     * command registration.
     */
    public void addOptionDirect(ProcessedOption opt) {
        opt.setDeclarationOrder(optionDeclarationCounter++);
        this.options.add(opt);
        opt.setParent(this);
        if (command != null)
            opt.captureInitialValue(command);
    }

    public List<ProcessedOption> getDisplayOptions() {
        List<ProcessedOption> display = new ArrayList<>(getOptions());
        java.util.Comparator<ProcessedOption> byOrder = (left, right) -> {
            int cmp = Integer.compare(left.getOrder(), right.getOrder());
            return cmp;
        };
        java.util.Comparator<ProcessedOption> byDeclarationOrder = (left, right) -> Integer
                .compare(left.getDeclarationOrder(), right.getDeclarationOrder());

        if (sortOptions) {
            java.util.Comparator<ProcessedOption> byName = (left, right) -> {
                String leftName = left.name() != null ? left.name() : "";
                String rightName = right.name() != null ? right.name() : "";
                return leftName.compareTo(rightName);
            };
            display.sort(byOrder.thenComparing(byName).thenComparing(byDeclarationOrder));
        } else {
            display.sort(byOrder.thenComparing(byDeclarationOrder));
        }
        return display;
    }

    private void setOptions(List<ProcessedOption> options) throws OptionParserException {
        for (ProcessedOption opt : options) {
            addOption(opt);
        }
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public CommandValidator<C, CI> validator() {
        return validator;
    }

    public ResultHandler resultHandler() {
        return resultHandler;
    }

    public boolean hasArguments() {
        return arguments != null && arguments.hasMultipleValues();
    }

    public ProcessedOption getArguments() {
        return arguments;
    }

    public void setArguments(ProcessedOption arguments) {
        this.arguments = arguments;
        this.arguments.setParent(this);
        if (command != null)
            arguments.captureInitialValue(command);
    }

    public List<ProcessedOption> getArgumentOptions() {
        return argumentOptions;
    }

    public void addArgument(ProcessedOption arg) {
        if (arg == null)
            return;
        argumentOptions.add(arg);
        if (argument == null)
            argument = arg;
        arg.setParent(this);
        if (command != null)
            arg.captureInitialValue(command);
    }

    public CommandPopulator<Object, CI> getCommandPopulator() {
        return populator;
    }

    public C getCommand() {
        return command;
    }

    public boolean generateHelp() {
        return generateHelp;
    }

    public boolean disableParsing() {
        return disableParsing;
    }

    public boolean stopAtFirstPositional() {
        return stopAtFirstPositional;
    }

    public boolean sortOptions() {
        return sortOptions;
    }

    public DefaultValueProvider getDefaultValueProvider() {
        return defaultValueProvider;
    }

    /**
     * Set the default value provider. Used by the registry to inject a
     * registry-level fallback when the command has no per-command provider.
     */
    public void setDefaultValueProvider(DefaultValueProvider provider) {
        this.defaultValueProvider = provider;
    }

    public String version() {
        return version;
    }

    public String helpUrl() {
        return helpUrl;
    }

    public String helpGroup() {
        return helpGroup;
    }

    public void setHelpGroup(String helpGroup) {
        this.helpGroup = helpGroup != null ? helpGroup : "";
    }

    public Class<? extends HelpSectionProvider> getHelpSectionProviderClass() {
        return helpSectionProviderClass;
    }

    public void setHelpSectionProviderClass(Class<? extends HelpSectionProvider> helpSectionProviderClass) {
        this.helpSectionProviderClass = helpSectionProviderClass;
    }

    public HelpSectionProvider getHelpSectionProvider() {
        return helpSectionProvider;
    }

    public void setHelpSectionProvider(HelpSectionProvider helpSectionProvider) {
        this.helpSectionProvider = helpSectionProvider;
    }

    public void setParentCommandInjector(java.util.function.BiConsumer<Object, Object> injector) {
        this.parentCommandInjector = injector;
    }

    public java.util.function.BiConsumer<Object, Object> getParentCommandInjector() {
        return parentCommandInjector;
    }

    private char verifyThatNamesAreUnique(String name, String longName) throws OptionParserException {
        if (name != null)
            return verifyThatNamesAreUnique(name.charAt(0), longName);
        else
            return verifyThatNamesAreUnique('\u0000', longName);
    }

    private char verifyThatNamesAreUnique(char name, String longName) throws OptionParserException {
        if (longName != null && longName.length() > 0 && findLongOption(longName) != null) {
            throw new OptionParserException("Option --" + longName + " is already added to Param: " + this.toString());
        }
        if (name != '\u0000' && findOption(String.valueOf(name)) != null) {
            throw new OptionParserException("Option -" + name + " is already added to Param: " + this.toString());
        }

        //if name is null, use one based on name
        if (name == '\u0000' && (longName == null || longName.length() == 0))
            throw new OptionParserException("Neither option name and option long name can be both null");

        return name;
    }

    private char findPossibleName(String longName) throws OptionParserException {
        for (int i = 0; i < longName.length(); i++) {
            if (findOption(String.valueOf(longName.charAt(i))) == null)
                return longName.charAt(i);
        }
        //all chars are taken
        throw new OptionParserException("All option names are taken, please specify a unique name");
    }

    public ProcessedOption findOption(String name) {
        for (ProcessedOption option : getOptions())
            if (option.shortName() != null &&
                    option.shortName().equals(name) &&
                    option.isActivated(new ParsedCommand(this)))
                return option;

        return null;
    }

    public ProcessedOption findOptionNoActivatorCheck(String name) {
        for (ProcessedOption option : getOptions())
            if (option.shortName() != null &&
                    option.shortName().equals(name))
                return option;

        return null;
    }

    /**
     * Generic search for matching options with input that start with either -- or -
     *
     * @param input input
     * @return matching option
     */
    public ProcessedOption searchAllOptions(String input) {
        if (input.startsWith("--")) {
            String optionName = input.substring(2);
            ProcessedOption currentOption = findLongOptionNoActivatorCheck(optionName);
            if (currentOption == null && input.contains("="))
                currentOption = startWithLongOptionNoActivatorCheck(optionName);
            // Check for negated options (e.g., --no-verbose)
            if (currentOption == null) {
                currentOption = findNegatedOptionNoActivatorCheck(optionName);
            }
            if (currentOption != null)
                currentOption.setLongNameUsed(true);
            //need to handle spaces in option names
            else if (Parser.containsNonEscapedSpace(input)) {
                return searchAllOptions(Parser.switchSpacesToEscapedSpacesInWord(input));
            }

            return currentOption;
        } else if (input.startsWith("-")) {
            ProcessedOption currentOption = findOption(input.substring(1));
            if (currentOption == null)
                currentOption = startWithOption(input.substring(1));

            if (currentOption != null)
                currentOption.setLongNameUsed(false);

            return currentOption;
        } else {
            // Check for bare long names
            ProcessedOption currentOption = findBareLongOption(input);
            if (currentOption == null && input.contains("=")) {
                currentOption = findBareLongOption(input.substring(0, input.indexOf("=")));
            }
            if (currentOption != null) {
                currentOption.setLongNameUsed(true);
            }
            return currentOption;
        }
    }

    public ProcessedOption findLongOption(String name) {
        for (ProcessedOption option : getOptions())
            if (option.name() != null &&
                    (option.name().equals(name) || option.hasAlias(name)) &&
                    option.isActivated(new ParsedCommand(this)))
                return option;

        return null;
    }

    public ProcessedOption findLongOptionNoActivatorCheck(String name) {
        for (ProcessedOption option : getOptions())
            if (option.name() != null && (option.name().equals(name) || option.hasAlias(name)))
                return option;

        return null;
    }

    /**
     * Find an option by its negated name (e.g., "no-verbose" for option "verbose").
     * If found, marks the option as negated.
     *
     * @param name the negated name to search for
     * @return the matching option, or null if not found
     */
    public ProcessedOption findNegatedOption(String name) {
        for (ProcessedOption option : getOptions()) {
            if (option.isNegatable() && option.getNegatedName() != null &&
                    option.getNegatedName().equals(name) &&
                    option.isActivated(new ParsedCommand(this))) {
                option.setNegatedByUser(true);
                return option;
            }
        }
        return null;
    }

    /**
     * Find an option by its negated name without checking activator.
     *
     * @param name the negated name to search for
     * @return the matching option, or null if not found
     */
    public ProcessedOption findNegatedOptionNoActivatorCheck(String name) {
        for (ProcessedOption option : getOptions()) {
            if (option.isNegatable() && option.getNegatedName() != null &&
                    option.getNegatedName().equals(name)) {
                option.setNegatedByUser(true);
                return option;
            }
        }
        return null;
    }

    public ProcessedOption findBareLongOption(String name) {
        for (ProcessedOption option : getOptions())
            if (option.name() != null && (option.name().equals(name) || option.hasAlias(name))
                    && option.acceptNameWithoutDashes())
                return option;

        return null;
    }

    public List<TerminalString> findPossibleBareLongNamesWithDash(String name) {
        List<ProcessedOption> opts = getDisplayOptions();
        List<TerminalString> names = new ArrayList<>(opts.size());
        for (ProcessedOption o : opts) {
            if (o.name() != null && o.acceptNameWithoutDashes()
                    && o.getValues().size() == 0
                    && o.isActivated(new ParsedCommand(this))) {
                if (o.name().startsWith(name)) {
                    names.add(o.getRenderedNameWithDashes());
                }
                for (TerminalString alias : o.getRenderedAliasNamesWithDashes()) {
                    if (alias.getCharacters().startsWith(name)) {
                        names.add(alias);
                    }
                }
            }
        }
        return names;
    }

    public ProcessedOption startWithOption(String name) {
        for (ProcessedOption option : getOptions())
            if (option.shortName() != null && name.startsWith(option.shortName()) &&
                    option.isActivated(new ParsedCommand(this)))
                return option;

        return null;
    }

    public ProcessedOption startWithLongOption(String name) {
        for (ProcessedOption option : getOptions())
            if ((name.startsWith(option.name()) || startsWithAlias(option, name)) &&
                    option.isActivated(new ParsedCommand(this)))
                return option;

        return null;
    }

    public ProcessedOption startWithLongOptionNoActivatorCheck(String name) {
        ProcessedOption longestMatch = null;
        int longestLen = -1;
        for (ProcessedOption option : getOptions()) {
            int matchLen = startsWithNameOrAlias(option, name);
            if (matchLen > longestLen) {
                longestMatch = option;
                longestLen = matchLen;
            }
        }
        return longestMatch;
    }

    private static boolean startsWithAlias(ProcessedOption option, String name) {
        for (String alias : option.getAliases()) {
            if (name.startsWith(alias))
                return true;
        }
        return false;
    }

    private static int startsWithNameOrAlias(ProcessedOption option, String name) {
        int longest = -1;
        if (name.startsWith(option.name()))
            longest = option.name().length();
        for (String alias : option.getAliases()) {
            if (name.startsWith(alias) && alias.length() > longest)
                longest = alias.length();
        }
        return longest;
    }

    public void clear() {
        clearOptions();
        if (arguments != null)
            arguments.clear();
        for (ProcessedOption argOpt : argumentOptions)
            argOpt.clear();

        if (parserExceptions instanceof ArrayList)
            parserExceptions.clear();
        else
            parserExceptions = Collections.emptyList();
        completeStatus = null;
    }

    protected void clearOptions() {
        for (ProcessedOption processedOption : getOptions()) {
            processedOption.clear();
        }
    }

    private void doGenerateHelp() {
        //only generate a help option if there is no other option already called help
        if (findOption("help") == null) {
            try {
                ProcessedOption helpOption = ProcessedOptionBuilder
                        .builder()
                        .name("help")
                        .shortName('h')
                        .description("Display this help and exit")
                        .required(false)
                        .optionType(OptionType.BOOLEAN)
                        .type(Boolean.class)
                        .hasValue(false)
                        .overrideRequired(true)
                        .fieldName("generatedHelp")
                        .build();

                helpOption.setDeclarationOrder(optionDeclarationCounter++);
                options.add(helpOption);
                helpOption.setParent(this);
            } catch (OptionParserException e) {
                throw new RuntimeException("Failed to generate help option", e);
            }
        }
    }

    public boolean isGenerateHelpOptionSet() {
        ProcessedOption helpOption = findLongOptionNoActivatorCheck("help");
        return helpOption != null && helpOption.getValue() != null;
    }

    public boolean isFullHelpRequested() {
        ProcessedOption helpOption = findLongOptionNoActivatorCheck("help");
        if (helpOption == null)
            return false;
        // --help=all or --help=full: the = value is stored even for boolean options
        for (String val : helpOption.getValues()) {
            if ("all".equalsIgnoreCase(val) || "full".equalsIgnoreCase(val))
                return true;
        }
        return false;
    }

    private void doGenerateVersion() {
        //only generate a version option if there is no other option already called version
        if (findOption("version") == null) {
            try {
                ProcessedOption versionOption = ProcessedOptionBuilder
                        .builder()
                        .name("version")
                        .shortName('v')
                        .description("Displays version information of the command")
                        .hasValue(false)
                        .required(false)
                        .optionType(OptionType.BOOLEAN)
                        .type(Boolean.class)
                        .overrideRequired(true)
                        .fieldName("generatedVersion")
                        .build();

                versionOption.setDeclarationOrder(optionDeclarationCounter++);
                options.add(versionOption);
                versionOption.setParent(this);
            } catch (OptionParserException e) {
                throw new RuntimeException("Failed to generate version option", e);
            }
        }
    }

    public boolean isGenerateVersionOptionSet() {
        ProcessedOption versionOption = findLongOptionNoActivatorCheck("version");
        return versionOption != null && versionOption.getValue() != null;
    }

    /**
     * Return all option names that not already have a value
     * and is enabled. For negatable options, also includes the negated form.
     */
    public List<TerminalString> getOptionLongNamesWithDash() {
        List<ProcessedOption> opts = getDisplayOptions();
        List<TerminalString> names = new ArrayList<>(opts.size());
        for (ProcessedOption o : opts) {
            if (o.getVisibility() == org.aesh.command.option.OptionVisibility.HIDDEN)
                continue;
            if (o.getValues().size() == 0 &&
                    o.isActivated(new ParsedCommand(this)) &&
                    !isExcludedBySetOption(o)) {
                names.add(o.getRenderedNameWithDashes());
                names.addAll(o.getRenderedAliasNamesWithDashes());
                // Also add the negated form for negatable options
                TerminalString negated = o.getRenderedNegatedNameWithDashes();
                if (negated != null) {
                    names.add(negated);
                }
            }
        }

        return names;
    }

    public List<TerminalString> findPossibleLongNamesWithDash(String name) {
        List<ProcessedOption> opts = getDisplayOptions();
        List<TerminalString> names = new ArrayList<>(opts.size());
        for (ProcessedOption o : opts) {
            if (o.getVisibility() == org.aesh.command.option.OptionVisibility.HIDDEN)
                continue;
            if (isExcludedBySetOption(o))
                continue;
            if (((o.shortName() != null && o.shortName().equals(name) &&
                    !o.isLongNameUsed() && o.getValues().size() == 0) ||
                    (o.name().startsWith(name) && o.getValues().size() == 0)) &&
                    o.isActivated(new ParsedCommand(this)))
                names.add(o.getRenderedNameWithDashes());
            // Check aliases
            if (o.getValues().size() == 0 && o.isActivated(new ParsedCommand(this))) {
                for (String alias : o.getAliases()) {
                    if (alias.startsWith(name)) {
                        names.add(new TerminalString("--" + alias, true));
                    }
                }
            }
            // Also check negated option names for negatable options
            if (o.isNegatable() && o.getNegatedName() != null &&
                    o.getNegatedName().startsWith(name) && o.getValues().size() == 0 &&
                    o.isActivated(new ParsedCommand(this))) {
                TerminalString negated = o.getRenderedNegatedNameWithDashes();
                if (negated != null) {
                    names.add(negated);
                }
            }
        }
        return names;
    }

    private boolean isExcludedBySetOption(ProcessedOption option) {
        if (option.getExclusiveWith().isEmpty())
            return false;
        for (String exclusiveName : option.getExclusiveWith()) {
            ProcessedOption other = findLongOptionNoActivatorCheck(exclusiveName);
            if (other != null && other.getValue() != null)
                return true;
        }
        return false;
    }

    public List<String> findPossibleLongNames(String name) {
        if (name.startsWith("--"))
            name = name.substring(2);
        List<ProcessedOption> opts = getDisplayOptions();
        List<String> names = new ArrayList<>(opts.size());
        for (ProcessedOption o : opts) {
            if (((o.shortName() != null && o.shortName().equals(name) &&
                    !o.isLongNameUsed() && o.getValues().size() == 0) ||
                    (o.name().startsWith(name) && o.getValues().size() == 0)) &&
                    o.isActivated(new ParsedCommand(this)))
                names.add(o.name());
            // Check aliases
            if (o.getValues().size() == 0 && o.isActivated(new ParsedCommand(this))) {
                for (String alias : o.getAliases()) {
                    if (alias.startsWith(name))
                        names.add(alias);
                }
            }
            // Also check negated option names for negatable options
            if (o.isNegatable() && o.getNegatedName() != null &&
                    o.getNegatedName().startsWith(name) && o.getValues().size() == 0 &&
                    o.isActivated(new ParsedCommand(this)))
                names.add(o.getNegatedName());
        }
        return names;
    }

    /**
     * @return returns true if the command has any options with askIfNotSet to true
     *         and its value is not set.
     */
    public boolean hasAskIfNotSet() {
        for (ProcessedOption opt : getOptions()) {
            if (opt.askIfNotSet() && opt.hasValue() && opt.getValues().isEmpty() && !opt.hasDefaultValue())
                return true;
        }
        return false;
    }

    public List<ProcessedOption> getAllAskIfNotSet() {
        List<ProcessedOption> options = new ArrayList<>();
        for (ProcessedOption opt : getOptions()) {
            if (opt.askIfNotSet() && opt.hasValue() && opt.getValues().isEmpty() && !opt.hasDefaultValue())
                options.add(opt);
        }
        for (ProcessedOption argOpt : argumentOptions) {
            if (argOpt.askIfNotSet() && argOpt.hasValue() && argOpt.getValues().isEmpty() && !argOpt.hasDefaultValue())
                options.add(argOpt);
        }
        if (arguments != null && arguments.askIfNotSet() && arguments.hasValue() && arguments.getValues().isEmpty()
                && !arguments.hasDefaultValue())
            options.add(arguments);

        return options;
    }

    /**
     * Returns a description String based on the defined command and options.
     * Useful when printing "help" info etc.
     *
     */
    public String printHelp(String commandName) {
        return printHelp(commandName, false, false);
    }

    public String printHelp(String commandName, boolean supportsHyperlinks) {
        return printHelp(commandName, supportsHyperlinks, false);
    }

    /**
     * Returns a description String based on the defined command and options.
     * Useful when printing "help" info etc.
     *
     * @param commandName the command name to display
     * @param supportsHyperlinks whether the terminal supports OSC 8 hyperlinks
     * @param showAll when true, includes FULL visibility options in output
     */
    public String printHelp(String commandName, boolean supportsHyperlinks, boolean showAll) {
        int maxLength = 0;
        int width = 80;
        DescriptionResolver descriptionResolver = new DescriptionResolver(name, commandName, null, null, null);
        List<ProcessedOption> opts = getDisplayOptions();
        List<ProcessedOption> visibleOpts = new ArrayList<>(opts.size());
        for (ProcessedOption o : opts) {
            if (o.getVisibility() == org.aesh.command.option.OptionVisibility.HIDDEN)
                continue;
            if (!showAll && o.getVisibility() == org.aesh.command.option.OptionVisibility.FULL)
                continue;
            visibleOpts.add(o);
        }
        for (ProcessedOption o : visibleOpts) {
            if (o.getFormattedLength() > maxLength)
                maxLength = o.getFormattedLength();
        }

        StringBuilder sb = new StringBuilder();
        //first line — description
        sb.append(descriptionResolver.resolveCommandDescription(description())).append(Config.getLineSeparator());
        //second line — detailed synopsis
        sb.append("Usage: ");
        if (commandName == null || commandName.length() == 0)
            sb.append(name());
        else
            sb.append(commandName);
        sb.append(buildDetailedSynopsis(visibleOpts));

        List<ProcessedOption> positionalOptions = getPositionalOptionsInDisplayOrder();
        for (ProcessedOption positional : positionalOptions) {
            sb.append(formatArgumentSynopsis(positional));
        }
        sb.append(Config.getLineSeparator());

        //options and arguments — group by helpGroup
        if (visibleOpts.size() > 0) {
            Map<String, List<ProcessedOption>> groups = new LinkedHashMap<>();
            for (ProcessedOption o : visibleOpts) {
                String group = o.getHelpGroup().isEmpty() ? "" : o.getHelpGroup();
                groups.computeIfAbsent(group, k -> new ArrayList<>()).add(o);
            }

            // Print named groups first
            for (Map.Entry<String, List<ProcessedOption>> entry : groups.entrySet()) {
                if (!entry.getKey().isEmpty()) {
                    sb.append(Config.getLineSeparator()).append(entry.getKey()).append(":").append(Config.getLineSeparator());
                    for (ProcessedOption o : entry.getValue())
                        sb.append(o.getFormattedOption(2, maxLength + 4, width, supportsHyperlinks,
                                descriptionResolver.resolveOptionDescription(o)))
                                .append(Config.getLineSeparator());
                }
            }
            // Then default group
            List<ProcessedOption> defaultGroup = groups.get("");
            if (defaultGroup != null && !defaultGroup.isEmpty()) {
                sb.append(Config.getLineSeparator()).append("Options:").append(Config.getLineSeparator());
                for (ProcessedOption o : defaultGroup)
                    sb.append(o.getFormattedOption(2, maxLength + 4, width, supportsHyperlinks,
                            descriptionResolver.resolveOptionDescription(o)))
                            .append(Config.getLineSeparator());
            }
        }
        for (ProcessedOption positional : positionalOptions) {
            sb.append(Config.getLineSeparator())
                    .append(positional.getOptionType() == OptionType.ARGUMENTS ? "Arguments:" : "Argument:")
                    .append(Config.getLineSeparator());
            sb.append(positional.getFormattedOption(2, maxLength + 4, width, supportsHyperlinks,
                    descriptionResolver.resolveOptionDescription(positional)))
                    .append(Config.getLineSeparator());
        }
        // Append documentation link if helpUrl is set
        if (helpUrl != null && helpUrl.length() > 0) {
            sb.append(Config.getLineSeparator());
            if (supportsHyperlinks) {
                sb.append("Documentation: ").append(ANSI.hyperlink(helpUrl, helpUrl));
            } else {
                sb.append("Documentation: ").append(helpUrl);
            }
            sb.append(Config.getLineSeparator());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "ProcessedCommand{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", options=" + getOptions()
                + '}';
    }

    /**
     * Build a detailed synopsis string from visible options.
     * Groups boolean short flags together [-hVx], shows value options individually
     * [--config=<config>], renders mutually exclusive options with pipes
     * [--verbose | --quiet], and shows required options without brackets.
     */
    private String buildDetailedSynopsis(List<ProcessedOption> visibleOpts) {
        if (visibleOpts.isEmpty())
            return "";

        // Collect mutually exclusive groups to avoid showing them individually
        java.util.Set<String> exclusiveHandled = new java.util.HashSet<>();

        // 1. Group boolean short flags
        StringBuilder shortFlags = new StringBuilder();
        for (ProcessedOption o : visibleOpts) {
            if (o.getOptionType() == OptionType.BOOLEAN && o.shortName() != null
                    && !o.isRequired() && o.getExclusiveWith().isEmpty()) {
                shortFlags.append(o.shortName());
            }
        }

        StringBuilder synopsis = new StringBuilder();

        // Emit grouped short flags: [-hVx]
        if (shortFlags.length() > 0) {
            synopsis.append(" [-").append(shortFlags).append("]");
        }

        // 2. Emit remaining options
        for (ProcessedOption o : visibleOpts) {
            // Skip boolean short flags already grouped
            if (o.getOptionType() == OptionType.BOOLEAN && o.shortName() != null
                    && !o.isRequired() && o.getExclusiveWith().isEmpty()) {
                continue;
            }

            String optName = o.shortName() != null ? "-" + o.shortName() : "--" + o.name();

            // Handle mutually exclusive options
            if (!o.getExclusiveWith().isEmpty() && !exclusiveHandled.contains(o.name())) {
                StringBuilder exclusive = new StringBuilder();
                exclusive.append(optName);
                for (String exName : o.getExclusiveWith()) {
                    ProcessedOption exOpt = findLongOptionNoActivatorCheck(exName);
                    if (exOpt != null) {
                        String exOptName = exOpt.shortName() != null ? "-" + exOpt.shortName() : "--" + exOpt.name();
                        exclusive.append(" | ").append(exOptName);
                        exclusiveHandled.add(exOpt.name());
                    }
                }
                exclusiveHandled.add(o.name());
                if (o.isRequired()) {
                    synopsis.append(" (").append(exclusive).append(")");
                } else {
                    synopsis.append(" [").append(exclusive).append("]");
                }
                continue;
            }
            if (exclusiveHandled.contains(o.name()))
                continue;

            // Regular option
            String rendered = optName;
            if (o.getOptionType() == OptionType.GROUP) {
                rendered = optName + "<key>=<value>";
            } else if (o.hasValue() && o.getOptionType() != OptionType.BOOLEAN
                    && (o.type() != Boolean.class && o.type() != boolean.class)
                    && !o.isOptionalValue() && !o.hasFallbackValue()) {
                String placeholder = o.getArgument() != null && !o.getArgument().isEmpty()
                        ? o.getArgument()
                        : o.name();
                rendered = optName + "=<" + placeholder + ">";
            }

            if (o.isRequired()) {
                synopsis.append(" ").append(rendered);
            } else {
                synopsis.append(" [").append(rendered).append("]");
            }
        }

        return synopsis.toString();
    }

    private String formatArgumentSynopsis(ProcessedOption arg) {
        String label = arg.isTypeAssignableByResourcesOrFile()
                ? (arg.getOptionType() == OptionType.ARGUMENTS ? "files" : "file")
                : arg.getDisplayLabel();

        org.aesh.command.option.Arity arity = arg.getArity();
        if (arity != null) {
            StringBuilder sb = new StringBuilder();
            boolean optional = arity.getMin() == 0;
            sb.append(optional ? " [" : " ");
            // Show repeated labels for small fixed arities
            if (arity.getMax() == arity.getMin() && arity.getMax() <= 3) {
                for (int i = 0; i < arity.getMax(); i++) {
                    if (i > 0)
                        sb.append(" ");
                    sb.append("<").append(label).append(">");
                }
            } else {
                sb.append("<").append(label).append(">");
                if (arity.getMax() > 1 || arity.isUnlimited())
                    sb.append("...");
            }
            if (optional)
                sb.append("]");
            return sb.toString();
        }

        // Legacy behavior when no arity is set
        if (arg.getOptionType() == OptionType.ARGUMENTS)
            return " [<" + label + ">]";
        else
            return " <" + label + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ProcessedCommand))
            return false;

        ProcessedCommand that = (ProcessedCommand) o;

        if (!name.equals(that.name))
            return false;
        if (description != null ? !description.equals(that.description) : that.description != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    public boolean anyOptionsSet() {
        for (ProcessedOption o : getOptions()) {
            if (o.hasValue() && o.getValue() != null || !o.hasValue() && o.getValue() != null)
                return true;
        }
        for (ProcessedOption argOpt : argumentOptions) {
            if (argOpt.getValue() != null)
                return true;
        }
        if (hasArguments() && arguments.getValue() != null)
            return true;

        return false;
    }

    public boolean hasLongOption(String optionName) {
        for (ProcessedOption o : getOptions()) {
            if (o.name().equals(optionName))
                return true;
        }
        return false;
    }

    public boolean hasOptions() {
        List<ProcessedOption> opts = getOptions();
        return opts != null && opts.size() > 0;
    }

    //will only return true if the optionName equals an option and it does
    //not start with another option name
    public boolean hasUniqueLongOption(String optionName) {
        if (hasLongOption(optionName)) {
            for (ProcessedOption o : getOptions()) {
                if (o.name().startsWith(optionName) && !o.name().equals(optionName))
                    return false;
            }
            return true;
        }
        return false;
    }

    public void updateInvocationProviders(InvocationProviders invocationProviders) {
        updateOptionsInvocationProviders(invocationProviders);
        for (ProcessedOption argOpt : argumentOptions)
            argOpt.updateInvocationProviders(invocationProviders);
        if (arguments != null) {
            arguments.updateInvocationProviders(invocationProviders);
        }
        if (activator != null)
            activator = invocationProviders.getCommandActivatorProvider().enhanceCommandActivator(activator);
    }

    protected void updateOptionsInvocationProviders(InvocationProviders invocationProviders) {
        for (ProcessedOption option : getOptions()) {
            option.updateInvocationProviders(invocationProviders);
        }
    }

    public void addParserException(CommandLineParserException exception) {
        if (!(parserExceptions instanceof ArrayList))
            parserExceptions = new ArrayList<>();
        parserExceptions.add(exception);
    }

    public List<CommandLineParserException> parserExceptions() {
        return parserExceptions;
    }

    public boolean hasOptionsWithInjectedValues() {
        for (ProcessedOption option : options)
            if (option.getValue() != null)
                return true;
        return false;
    }

    public boolean hasOptionWithOverrideRequired() {
        for (ProcessedOption option : options) {
            if (option.getValue() != null && option.doOverrideRequired())
                return true;
        }
        return false;
    }

    public CompleteStatus completeStatus() {
        return completeStatus;
    }

    public void setCompleteStatus(CompleteStatus completeStatus) {
        this.completeStatus = completeStatus;
    }

    public void setArgument(ProcessedOption arg) {
        this.argument = arg;
        this.argumentOptions.clear();
        addArgument(arg);
    }

    public ProcessedOption getArgument() {
        return argument;
    }

    public boolean hasArgument() {
        return !argumentOptions.isEmpty();
    }

    public boolean hasArgumentWithNoValue() {
        for (ProcessedOption argOpt : argumentOptions) {
            if (argOpt.getValue() == null)
                return true;
        }
        return false;
    }

    public int getPositionalValueCount() {
        int count = 0;
        for (ProcessedOption argOpt : argumentOptions)
            count += argOpt.getValues().size();
        if (arguments != null)
            count += arguments.getValues().size();
        return count;
    }

    public ProcessedOption getPositionalForIndex(int index) {
        // Backward compatibility: commands that only define @Arguments historically
        // accepted all positional values starting from index 0.
        if (argumentOptions.isEmpty() && arguments != null
                && !arguments.isArityFull()
                && (!arguments.hasIndexRange()
                        || (arguments.getIndexRange().getMin() == 1
                                && arguments.getIndexRange().getMax() == Integer.MAX_VALUE))) {
            return arguments;
        }

        ProcessedOption match = null;
        for (ProcessedOption positional : getPositionalOptionsInDisplayOrder()) {
            boolean containsIndex;
            if (positional.hasIndexRange()) {
                containsIndex = positional.getIndexRange().contains(index);
            } else {
                containsIndex = positional.getOptionType() == OptionType.ARGUMENT ? index == 0 : index >= 1;
            }

            if (containsIndex) {
                if (!positional.isArityFull())
                    return positional;
                if (match == null)
                    match = positional;
            }
        }
        if (match != null)
            return match;
        return null;
    }

    public ProcessedOption getPositionalForNextValue() {
        return getPositionalForIndex(getPositionalValueCount());
    }

    public String positionalRangeSummary() {
        List<ProcessedOption> positional = getPositionalOptionsInDisplayOrder();
        if (positional.isEmpty())
            return "none";
        StringBuilder sb = new StringBuilder();
        for (ProcessedOption opt : positional) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(opt.getDisplayLabel()).append("=");
            if (opt.hasIndexRange()) {
                int min = opt.getIndexRange().getMin();
                int max = opt.getIndexRange().getMax();
                if (min == max)
                    sb.append(min);
                else if (max == Integer.MAX_VALUE)
                    sb.append(min).append("..*");
                else
                    sb.append(min).append("..").append(max);
            } else {
                sb.append(opt.getOptionType() == OptionType.ARGUMENT ? "0" : "1..*");
            }
        }
        return sb.toString();
    }

    public List<ProcessedOption> getPositionalOptionsInDisplayOrder() {
        if (argumentOptions.isEmpty() && arguments == null)
            return Collections.emptyList();

        List<ProcessedOption> positional = new ArrayList<>(argumentOptions.size() + 1);
        positional.addAll(argumentOptions);
        if (arguments != null)
            positional.add(arguments);

        positional.sort((left, right) -> {
            int leftMin = left.hasIndexRange() ? left.getIndexRange().getMin()
                    : (left.getOptionType() == OptionType.ARGUMENT ? 0 : 1);
            int rightMin = right.hasIndexRange() ? right.getIndexRange().getMin()
                    : (right.getOptionType() == OptionType.ARGUMENT ? 0 : 1);
            return Integer.compare(leftMin, rightMin);
        });
        return positional;
    }

    public String resolveCommandDescription(String description, String commandName, String fullCommandName,
            String rootCommandName, String parentCommandName, String parentCommandFullName) {
        return new DescriptionResolver(commandName, fullCommandName, rootCommandName, parentCommandName,
                parentCommandFullName).resolveCommandDescription(description);
    }

    public String resolveOptionDescription(ProcessedOption option, String commandName, String fullCommandName,
            String rootCommandName, String parentCommandName, String parentCommandFullName) {
        return new DescriptionResolver(commandName, fullCommandName, rootCommandName, parentCommandName,
                parentCommandFullName).resolveOptionDescription(option);
    }

    private static final class DescriptionResolver {
        private final String commandName;
        private final String commandFullName;
        private final String rootCommandName;
        private final String parentCommandName;
        private final String parentCommandFullName;

        private DescriptionResolver(String commandName, String commandFullName, String rootCommandName,
                String parentCommandName, String parentCommandFullName) {
            String fullName = commandFullName != null ? commandFullName : commandName;
            this.commandFullName = fullName;
            this.commandName = commandName != null ? commandName : lastToken(fullName);
            this.rootCommandName = rootCommandName != null ? rootCommandName : firstToken(fullName);
            this.parentCommandFullName = parentCommandFullName != null ? parentCommandFullName : parentPath(fullName);
            this.parentCommandName = parentCommandName != null ? parentCommandName : lastToken(this.parentCommandFullName);
        }

        private String resolveCommandDescription(String raw) {
            return resolve(raw, null);
        }

        private String resolveOptionDescription(ProcessedOption option) {
            return resolve(option != null ? option.description() : null, option);
        }

        private String resolve(String raw, ProcessedOption option) {
            if (raw == null || raw.isEmpty())
                return raw;

            Matcher matcher = DESCRIPTION_VARIABLE_PATTERN.matcher(raw);
            StringBuffer out = new StringBuffer(raw.length());
            while (matcher.find()) {
                String key = matcher.group(1);
                String replacement = resolveVariable(key, option);
                if (replacement == null)
                    replacement = matcher.group(0);
                matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(out);
            return out.toString();
        }

        private String resolveVariable(String key, ProcessedOption option) {
            switch (key) {
                case "COMMAND-NAME":
                    return commandName;
                case "COMMAND-FULL-NAME":
                    return commandFullName;
                case "ROOT-COMMAND-NAME":
                    return rootCommandName;
                case "PARENT-COMMAND-NAME":
                    return parentCommandName;
                case "PARENT-COMMAND-FULL-NAME":
                    return parentCommandFullName;
                case "DEFAULT-VALUE":
                case "FALLBACK-VALUE":
                    return optionDefaultValue(option);
                case "COMPLETION-CANDIDATES":
                    return optionCompletionCandidates(option);
                default:
                    return null;
            }
        }

        private static String optionDefaultValue(ProcessedOption option) {
            if (option == null || option.getDefaultValues().isEmpty())
                return "";
            return String.join(", ", option.getDefaultValues());
        }

        private static String optionCompletionCandidates(ProcessedOption option) {
            if (option == null)
                return "";
            List<String> candidates = option.getAllowedValues();
            if (candidates == null || candidates.isEmpty())
                return "";
            return String.join(", ", candidates);
        }

        private static String firstToken(String value) {
            if (value == null)
                return null;
            int idx = value.indexOf(' ');
            return idx < 0 ? value : value.substring(0, idx);
        }

        private static String lastToken(String value) {
            if (value == null)
                return null;
            int idx = value.lastIndexOf(' ');
            return idx < 0 ? value : value.substring(idx + 1);
        }

        private static String parentPath(String value) {
            if (value == null)
                return null;
            int idx = value.lastIndexOf(' ');
            if (idx < 0)
                return null;
            return value.substring(0, idx);
        }
    }

    public boolean hasArgumentsWithNoValue() {
        return arguments != null && arguments.getValue() == null;
    }

    public boolean hasSelector() {
        boolean selector = false;
        for (ProcessedOption opt : getOptions()) {
            // if we have an option that's marked with override required and is set
            // it should override selector
            if (opt.doOverrideRequired() && opt.getValue() != null)
                return false;
            if (opt.selectorType() != SelectorType.NO_OP && opt.hasValue())
                selector = true;
        }
        return selector;
    }

    public List<ProcessedOption> getAllSelectors() {
        List<ProcessedOption> options = new ArrayList<>();
        for (ProcessedOption opt : getOptions()) {
            if (opt.selectorType() != SelectorType.NO_OP && opt.hasValue() && opt.getValue() == null)
                options.add(opt);
        }
        for (ProcessedOption argOpt : argumentOptions) {
            if (argOpt.selectorType() != SelectorType.NO_OP
                    && (argOpt.hasValue() || argOpt.getOptionType().equals(OptionType.BOOLEAN)))
                options.add(argOpt);
        }
        if (arguments != null && arguments.selectorType() != SelectorType.NO_OP && arguments.hasValue())
            options.add(arguments);

        return options;
    }
}
