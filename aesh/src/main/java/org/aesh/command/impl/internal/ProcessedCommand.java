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
package org.aesh.command.impl.internal;

import org.aesh.command.Command;
import org.aesh.command.activator.CommandActivator;
import org.aesh.command.impl.activator.NullCommandActivator;
import org.aesh.command.impl.parser.CompleteStatus;
import org.aesh.command.impl.populator.AeshCommandPopulator;
import org.aesh.command.impl.result.NullResultHandler;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.parser.OptionParserException;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.command.result.ResultHandler;
import org.aesh.command.validator.CommandValidator;
import org.aesh.readline.terminal.formatting.TerminalString;
import org.aesh.readline.util.Parser;
import org.aesh.selector.SelectorType;
import org.aesh.terminal.utils.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ProcessedCommand<C extends Command<CI>, CI extends CommandInvocation> {

    private final String name;
    private final String description;
    private final CommandValidator<C,CI> validator;
    private final ResultHandler resultHandler;
    private final CommandPopulator<Object,CI> populator;
    private final boolean disableParsing;
    private CommandActivator activator;
    private final boolean generateHelp;
    private String version;

    private List<ProcessedOption> options;
    private ProcessedOption arguments;
    private ProcessedOption argument;
    private final C command;
    private final List<String> aliases;
    private List<CommandLineParserException> parserExceptions;
    private CompleteStatus completeStatus;

    public ProcessedCommand(String name, List<String> aliases, C command,
                            String description, CommandValidator<C,CI> validator,
                            ResultHandler resultHandler,
                            boolean generateHelp, boolean disableParsing,
                            String version,
                            ProcessedOption arguments, List<ProcessedOption> options,
                            ProcessedOption argument,
                            CommandPopulator<Object,CI> populator, CommandActivator activator) throws OptionParserException {
        this.name = name;
        this.description = description;
        this.aliases = aliases == null ? Collections.emptyList() : aliases;
        this.validator = validator;
        this.generateHelp = generateHelp;
        this.disableParsing = disableParsing;
        if(resultHandler != null)
            this.resultHandler = resultHandler;
        else
            this.resultHandler = new NullResultHandler();
        this.arguments = arguments;
        this.argument = argument;
        if(argument != null && arguments != null)
            throw new OptionParserException("Argument and Arguments cannot be defined in the same Command");
        this.options = new ArrayList<>();
        this.command = command;
        this.activator = activator == null ? new NullCommandActivator() : activator;
        if(populator == null)
            this.populator = new AeshCommandPopulator<>(this.command);
        else
            this.populator = populator;
        setOptions(options);

        if(generateHelp)
            doGenerateHelp();

        if(version != null && version.length() > 0) {
            this.version = version;
            doGenerateVersion();
        }

        parserExceptions = new ArrayList<>();
    }

    public List<ProcessedOption> getOptions() {
        return options;
    }

    public CommandActivator getActivator() {
        return activator;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void addOption(ProcessedOption opt) throws OptionParserException {
        this.options.add(new ProcessedOption(verifyThatNamesAreUnique(opt.shortName(), opt.name()), opt.name(),
                opt.description(), opt.getArgument(), opt.isRequired(), opt.getValueSeparator(), opt.askIfNotSet(), opt.selectorType(),
                opt.getDefaultValues(), opt.type(), opt.getFieldName(), opt.getOptionType(), opt.converter(),
                opt.completer(), opt.validator(), opt.activator(), opt.getRenderer(), opt.parser(), opt.doOverrideRequired()));

        options.get(options.size()-1).setParent(this);
    }

    private void setOptions(List<ProcessedOption> options) throws OptionParserException {
        for(ProcessedOption opt : options) {
            this.options.add(new ProcessedOption(verifyThatNamesAreUnique(opt.shortName(), opt.name()), opt.name(),
                    opt.description(), opt.getArgument(), opt.isRequired(), opt.getValueSeparator(), opt.askIfNotSet(), opt.selectorType(),
                    opt.getDefaultValues(), opt.type(), opt.getFieldName(), opt.getOptionType(),
                    opt.converter(), opt.completer(), opt.validator(), opt.activator(), opt.getRenderer(),
                    opt.parser(), opt.doOverrideRequired()));

            this.options.get(this.options.size()-1).setParent(this);
        }
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public CommandValidator<C,CI> validator() {
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

    public String version() {
        return version;
    }

    private char verifyThatNamesAreUnique(String name, String longName) throws OptionParserException {
        if(name != null)
            return verifyThatNamesAreUnique(name.charAt(0), longName);
        else
            return verifyThatNamesAreUnique('\u0000', longName);
    }

    private char verifyThatNamesAreUnique(char name, String longName) throws OptionParserException {
        if(longName != null && longName.length() > 0 && findLongOption(longName) != null) {
            throw new OptionParserException("Option --"+longName+" is already added to Param: "+this.toString());
        }
        if(name != '\u0000' && findOption(String.valueOf(name)) != null) {
            throw new OptionParserException("Option -"+name+" is already added to Param: "+this.toString());
        }

        //if name is null, use one based on name
        if(name == '\u0000' && (longName == null || longName.length() == 0))
            throw new OptionParserException("Neither option name and option long name can be both null");

        return name;
    }

    private char findPossibleName(String longName) throws OptionParserException {
        for(int i=0; i < longName.length(); i++) {
            if(findOption(String.valueOf(longName.charAt(i))) == null)
                return longName.charAt(i);
        }
        //all chars are taken
        throw new OptionParserException("All option names are taken, please specify a unique name");
    }

    public ProcessedOption findOption(String name) {
        for (ProcessedOption option : getOptions())
            if(option.shortName() != null &&
                    option.shortName().equals(name) &&
                    option.activator().isActivated(new ParsedCommand(this)))
                return option;

        return null;
    }

    public ProcessedOption findOptionNoActivatorCheck(String name) {
        for (ProcessedOption option : getOptions())
            if(option.shortName() != null &&
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
            ProcessedOption currentOption = findLongOptionNoActivatorCheck(input.substring(2));
            if(currentOption == null && input.contains("="))
                currentOption = startWithLongOptionNoActivatorCheck(input.substring(2));
            if (currentOption != null)
                currentOption.setLongNameUsed(true);
            //need to handle spaces in option names
            else if(Parser.containsNonEscapedSpace(input)) {
                return searchAllOptions(Parser.switchSpacesToEscapedSpacesInWord(input));
            }

            return currentOption;
        }
        else if (input.startsWith("-")) {
            ProcessedOption currentOption = findOption(input.substring(1));
            if(currentOption == null)
                currentOption = startWithOption(input.substring(1));

            if (currentOption != null)
                currentOption.setLongNameUsed(false);

            return currentOption;
        }
        else {
            return null;
        }
    }

    public ProcessedOption findLongOption(String name) {
        for (ProcessedOption option : getOptions())
            if(option.name() != null &&
                    option.name().equals(name) &&
                    option.activator().isActivated(new ParsedCommand(this)))
                return option;

        return null;
    }

    public ProcessedOption findLongOptionNoActivatorCheck(String name) {
        for (ProcessedOption option : getOptions())
            if(option.name() != null && option.name().equals(name))
                return option;

        return null;
    }

    public ProcessedOption startWithOption(String name) {
        for (ProcessedOption option : getOptions())
            if(option.shortName() != null && name.startsWith(option.shortName()) &&
                    option.activator().isActivated(new ParsedCommand(this)))
                return option;

        return null;
    }

    public ProcessedOption startWithLongOption(String name) {
        for (ProcessedOption option : getOptions())
            if(name.startsWith(option.name()) &&
                    option.activator().isActivated(new ParsedCommand(this)))
                return option;

        return null;
    }

    public ProcessedOption startWithLongOptionNoActivatorCheck(String name) {
        ProcessedOption longestMatch = null;
        for (ProcessedOption option : getOptions()) {
            if (name.startsWith(option.name())) {
                if (longestMatch == null) {
                    longestMatch = option;
                } else if (option.name().length() > longestMatch.name().length()) {
                    longestMatch = option;
                }
            }
        }
        return longestMatch;
    }

   public void clear() {
       clearOptions();
       if(arguments != null)
           arguments.clear();
       if(argument != null)
           argument.clear();

       parserExceptions.clear();
       completeStatus = null;
    }

    protected void clearOptions() {
        for (ProcessedOption processedOption : getOptions()) {
            processedOption.clear();
        }
    }

    private void doGenerateHelp() {
        //only generate a help option if there is no other option already called help
        if(findOption("help") == null) {
            try {
                ProcessedOption helpOption = ProcessedOptionBuilder
                        .builder()
                        .name("help")
                        .shortName('h')
                        .description("Displays information of the command and all options")
                        .hasValue(false)
                        .required(false)
                        .optionType(OptionType.BOOLEAN)
                        .type(Boolean.class)
                        .overrideRequired(true)
                        .fieldName("generatedHelp")
                        .build();

                options.add(helpOption);
            }
            catch (OptionParserException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isGenerateHelpOptionSet() {
       ProcessedOption helpOption = findLongOptionNoActivatorCheck("help");
        return helpOption != null && helpOption.getValue() != null;
    }

    private void doGenerateVersion() {
        //only generate a version option if there is no other option already called version
        if(findOption("version") == null) {
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

                options.add(versionOption);
            }
            catch (OptionParserException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isGenerateVersionOptionSet() {
        ProcessedOption versionOption = findLongOptionNoActivatorCheck("version");
        return versionOption != null && versionOption.getValue() != null;
    }

    /**
     * Return all option names that not already have a value
     * and is enabled
     */
    public List<TerminalString> getOptionLongNamesWithDash() {
        List<ProcessedOption> opts = getOptions();
        List<TerminalString> names = new ArrayList<>(opts.size());
        for (ProcessedOption o : opts) {
            if(o.getValues().size() == 0 &&
                    o.activator().isActivated(new ParsedCommand(this)))
                names.add(o.getRenderedNameWithDashes());
        }

        return names;
    }

    public List<TerminalString> findPossibleLongNamesWithDash(String name) {
        List<ProcessedOption> opts = getOptions();
        List<TerminalString> names = new ArrayList<>(opts.size());
        for (ProcessedOption o : opts) {
           if(((o.shortName() != null && o.shortName().equals(name) &&
                   !o.isLongNameUsed() && o.getValues().size() == 0) ||
                   (o.name().startsWith(name) && o.getValues().size() == 0)) &&
                   o.activator().isActivated(new ParsedCommand(this)))
               names.add(o.getRenderedNameWithDashes());
        }
        return names;
    }

    public List<String> findPossibleLongNames(String name) {
        if(name.startsWith("--"))
            name = name.substring(2);
        List<ProcessedOption> opts = getOptions();
        List<String> names = new ArrayList<>(opts.size());
        for (ProcessedOption o : opts) {
           if(((o.shortName() != null && o.shortName().equals(name) &&
                   !o.isLongNameUsed() && o.getValues().size() == 0) ||
                   (o.name().startsWith(name) && o.getValues().size() == 0)) &&
                   o.activator().isActivated(new ParsedCommand(this)))
               names.add(o.name());
        }
        return names;
    }

    /**
     * @return returns true if the command has any options with askIfNotSet to true
     * and its value is not set.
     */
    public boolean hasAskIfNotSet() {
        for(ProcessedOption opt : getOptions()) {
            if(opt.askIfNotSet() && opt.hasValue() && opt.getValues().isEmpty() && !opt.hasDefaultValue())
                return true;
        }
        return false;
    }

    public List<ProcessedOption> getAllAskIfNotSet() {
       List<ProcessedOption>  options = new ArrayList<>();
        for(ProcessedOption opt : getOptions()) {
            if(opt.askIfNotSet() && opt.hasValue() && opt.getValues().isEmpty() && !opt.hasDefaultValue())
                options.add(opt);
        }
        if(argument != null && argument.askIfNotSet() && argument.hasValue() && argument.getValues().isEmpty() && !argument.hasDefaultValue())
            options.add(argument);
        if(arguments != null && arguments.askIfNotSet() && arguments.hasValue() && arguments.getValues().isEmpty() && !arguments.hasDefaultValue())
            options.add(arguments);

        return options;
    }

    /**
     * Returns a description String based on the defined command and options.
     * Useful when printing "help" info etc.
     *
     */
    public String printHelp(String commandName) {
        int maxLength = 0;
        int width = 80;
        List<ProcessedOption> opts = getOptions();
        for (ProcessedOption o : opts) {
            if(o.getFormattedLength() > maxLength)
                maxLength = o.getFormattedLength();
        }

        StringBuilder sb = new StringBuilder();
        //first line
        sb.append("Usage: ");
        if(commandName == null || commandName.length() == 0)
            sb.append(name());
        else
            sb.append(commandName);
        if(opts.size() > 0)
            sb.append(" [<options>]");

        if(argument != null) {
            if(argument.isTypeAssignableByResourcesOrFile())
                sb.append(" <file>");
            else
                sb.append(" <").append(argument.getFieldName()).append(">");
        }

        if(arguments != null) {
            if(arguments.isTypeAssignableByResourcesOrFile())
                sb.append(" [<files>]");
            else
                sb.append(" [<").append(arguments.getFieldName()).append(">]");
        }
        sb.append(Config.getLineSeparator());
        //second line
        sb.append(description()).append(Config.getLineSeparator());

        //options and arguments
        if (opts.size() > 0)
           sb.append(Config.getLineSeparator()).append("Options:").append(Config.getLineSeparator());
        for (ProcessedOption o : opts)
            sb.append(o.getFormattedOption(2, maxLength+4, width)).append(Config.getLineSeparator());
        if(arguments != null) {
            sb.append(Config.getLineSeparator()).append("Arguments:").append(Config.getLineSeparator());
            sb.append(arguments.getFormattedOption(2, maxLength+4, width)).append(Config.getLineSeparator());
        }
        if(argument != null) {
            sb.append(Config.getLineSeparator()).append("Argument:").append(Config.getLineSeparator());
            sb.append(argument.getFormattedOption(2, maxLength+4, width)).append(Config.getLineSeparator());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "ProcessedCommand{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
 ", options=" + getOptions()
                +                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProcessedCommand)) return false;

        ProcessedCommand that = (ProcessedCommand) o;

        if (!name.equals(that.name)) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    public boolean anyOptionsSet() {
        for(ProcessedOption o : getOptions()) {
            if (o.hasValue() && o.getValue() != null || !o.hasValue() && o.getValue() != null)
                return true;
        }
        if(hasArgument() && argument.getValue() != null)
            return true;
        if(hasArguments() && arguments.getValue() != null)
            return true;

        return false;
    }

    public boolean hasLongOption(String optionName) {
        for(ProcessedOption o : getOptions()) {
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
        if(hasLongOption(optionName)) {
            for(ProcessedOption o : getOptions()) {
                if(o.name().startsWith(optionName) && !o.name().equals(optionName))
                    return false;
            }
            return true;
        }
        return false;
    }

    public void updateInvocationProviders(InvocationProviders invocationProviders) {
        updateOptionsInvocationProviders(invocationProviders);
        if (argument != null) {
            argument.updateInvocationProviders(invocationProviders);
        }
        if (arguments != null) {
            arguments.updateInvocationProviders(invocationProviders);
        }
        activator = invocationProviders.getCommandActivatorProvider().enhanceCommandActivator(activator);
    }

    protected void updateOptionsInvocationProviders(InvocationProviders invocationProviders) {
        for (ProcessedOption option : getOptions()) {
            option.updateInvocationProviders(invocationProviders);
        }
    }

    public void addParserException(CommandLineParserException exception) {
        parserExceptions.add(exception);
    }

    public List<CommandLineParserException> parserExceptions() {
        return parserExceptions;
    }

    public boolean hasOptionsWithInjectedValues() {
        for(ProcessedOption option : options)
            if(option.getValue() != null)
                return true;
        return false;
    }

    public boolean hasOptionWithOverrideRequired() {
        for(ProcessedOption option : options) {
            if(option.getValue() != null && option.doOverrideRequired())
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
        this.argument.setParent(this);
    }

    public ProcessedOption getArgument() {
        return argument;
    }

    public boolean hasArgument() {
        return argument != null;
    }

    public boolean hasArgumentWithNoValue() {
        return argument != null && argument.getValue() == null;
    }

    public boolean hasArgumentsWithNoValue() {
        return arguments != null && arguments.getValue() == null;
    }

    public boolean hasSelector() {
        boolean selector = false;
        for(ProcessedOption opt : getOptions()) {
            // if we have an option that's marked with override required and is set
            // it should override selector
            if(opt.doOverrideRequired() && opt.getValue() != null)
                return false;
            if(opt.selectorType() != SelectorType.NO_OP && opt.hasValue())
                selector = true;
        }
        return selector;
     }

    public List<ProcessedOption> getAllSelectors() {
       List<ProcessedOption>  options = new ArrayList<>();
        for(ProcessedOption opt : getOptions()) {
            if(opt.selectorType() != SelectorType.NO_OP && opt.hasValue() && opt.getValue() == null)
                options.add(opt);
        }
        if(argument != null && argument.selectorType() != SelectorType.NO_OP &&
                   (argument.hasValue() || argument.getOptionType().equals(OptionType.BOOLEAN)))
            options.add(argument);
        if(arguments != null && arguments.selectorType() != SelectorType.NO_OP && arguments.hasValue())
            options.add(arguments);

        return options;
    }
}
