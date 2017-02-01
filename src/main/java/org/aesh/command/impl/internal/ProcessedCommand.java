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

import org.aesh.command.impl.activator.NullCommandActivator;
import org.aesh.command.impl.parser.CommandLineParserException;
import org.aesh.command.impl.parser.OptionParserException;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.command.impl.result.NullResultHandler;
import org.aesh.command.result.ResultHandler;
import org.aesh.command.validator.CommandValidator;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.Command;
import org.aesh.command.impl.populator.AeshCommandPopulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.aesh.command.activator.CommandActivator;
import org.aesh.terminal.formatting.TerminalString;
import org.aesh.util.Config;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ProcessedCommand<C extends Command> {

    private String name;
    private String description;
    private CommandValidator validator;
    private ResultHandler resultHandler = new NullResultHandler();
    private CommandPopulator<Object, C> populator;
    private CommandActivator activator;

    private List<ProcessedOption> options;
    private ProcessedOption argument;
    private C command;
    private final List<String> aliases;
    private List<CommandLineParserException> parserExceptions;

    public ProcessedCommand(String name, List<String> aliases, C command,
            String description, CommandValidator validator,
            ResultHandler resultHandler,
            ProcessedOption argument, List<ProcessedOption> options,
            CommandPopulator<Object, C> populator, CommandActivator activator) throws OptionParserException {
        setName(name);
        setDescription(description);
        this.aliases = aliases == null ? Collections.emptyList() : aliases;
        this.validator = validator;
        this.resultHandler = resultHandler;
        this.argument = argument;
        this.options = new ArrayList<>();
        this.command = command;
        this.activator = activator == null ? new NullCommandActivator() : activator;
        if(populator == null)
            this.populator = new AeshCommandPopulator<>(this.command);
        else
            this.populator = populator;
        setOptions(options);

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
        this.options.add(new ProcessedOption(verifyThatNamesAreUnique(opt.getShortName(), opt.getName()), opt.getName(),
                opt.getDescription(), opt.getArgument(), opt.isRequired(), opt.getValueSeparator(),
                opt.getDefaultValues(), opt.getType(), opt.getFieldName(), opt.getOptionType(), opt.getConverter(),
                opt.getCompleter(), opt.getValidator(), opt.getActivator(), opt.getRenderer(), opt.doOverrideRequired()));

        options.get(options.size()-1).setParent(this);
    }

    private void setOptions(List<ProcessedOption> options) throws OptionParserException {
        for(ProcessedOption opt : options) {
            this.options.add(new ProcessedOption(verifyThatNamesAreUnique(opt.getShortName(), opt.getName()), opt.getName(),
                    opt.getDescription(), opt.getArgument(), opt.isRequired(), opt.getValueSeparator(),
                    opt.getDefaultValues(), opt.getType(), opt.getFieldName(), opt.getOptionType(),
                    opt.getConverter(), opt.getCompleter(), opt.getValidator(), opt.getActivator(), opt.getRenderer(),
                    opt.doOverrideRequired()));

            options.get(options.size()-1).setParent(this);
        }
    }

    public String getName() {
        return name;
    }

    private void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    private void setDescription(String description) {
        this.description = description;
    }

    public CommandValidator getValidator() {
        return validator;
    }

    public ResultHandler getResultHandler() {
      return resultHandler;
    }

    public boolean hasArgument() {
        return argument != null && argument.hasMultipleValues();
    }

    public ProcessedOption getArgument() {
        return argument;
    }

    public void setArgument(ProcessedOption argument) {
        this.argument = argument;
    }

    public CommandPopulator<Object, C> getCommandPopulator() {
        return populator;
    }

    public C getCommand() {
        return command;
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
            if(option.getShortName() != null &&
                    option.getShortName().equals(name) &&
                    option.getActivator().isActivated(this))
                return option;

        return null;
    }

    public ProcessedOption findOptionNoActivatorCheck(String name) {
        for (ProcessedOption option : getOptions())
            if(option.getShortName() != null &&
                    option.getShortName().equals(name))
                return option;

        return null;
    }

    public ProcessedOption findLongOption(String name) {
        for (ProcessedOption option : getOptions())
            if(option.getName() != null &&
                    option.getName().equals(name) &&
                    option.getActivator().isActivated(this))
                return option;

        return null;
    }

    public ProcessedOption findLongOptionNoActivatorCheck(String name) {
        for (ProcessedOption option : getOptions())
            if(option.getName() != null && option.getName().equals(name))
                return option;

        return null;
    }

    public ProcessedOption startWithOption(String name) {
        for (ProcessedOption option : getOptions())
            if(option.getShortName() != null && name.startsWith(option.getShortName()) &&
                    option.getActivator().isActivated(this))
                return option;

        return null;
    }

    public ProcessedOption startWithLongOption(String name) {
        for (ProcessedOption option : getOptions())
            if(name.startsWith(option.getName()) &&
                    option.getActivator().isActivated(this))
                return option;

        return null;
    }

   public void clear() {
       for (ProcessedOption processedOption : getOptions())
           processedOption.clear();
       if(argument != null)
           argument.clear();

       parserExceptions.clear();
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
                    o.getActivator().isActivated(this))
                names.add(o.getRenderedNameWithDashes());
        }

        return names;
    }

    public List<TerminalString> findPossibleLongNamesWitdDash(String name) {
        List<ProcessedOption> opts = getOptions();
        List<TerminalString> names = new ArrayList<>(opts.size());
        for (ProcessedOption o : opts) {
           if(((o.getShortName() != null && o.getShortName().equals(name) &&
                   !o.isLongNameUsed() && o.getValues().size() == 0) ||
                   (o.getName().startsWith(name) && o.getValues().size() == 0)) &&
                   o.getActivator().isActivated(this))
               names.add(o.getRenderedNameWithDashes());
        }
        return names;
    }

    /**
     * Returns a description String based on the defined command and options.
     * Useful when printing "help" info etc.
     *
     */
    public String printHelp() {
        int maxLength = 0;
        int width = 80;
        List<ProcessedOption> opts = getOptions();
        for (ProcessedOption o : opts)
            if(o.getFormattedLength() > maxLength)
                maxLength = o.getFormattedLength();

        StringBuilder sb = new StringBuilder();
        if (opts.size() > 0)
           sb.append(Config.getLineSeparator()).append("Options:").append(Config.getLineSeparator());
        for (ProcessedOption o : opts)
            sb.append(o.getFormattedOption(2, maxLength+4, width)).append(Config.getLineSeparator());
        if(argument != null) {
            sb.append(Config.getLineSeparator()).append("Arguments:").append(Config.getLineSeparator());
            sb.append(argument.getFormattedOption(2, maxLength+4, width)).append(Config.getLineSeparator());
        }
        return "Usage: "+getName()+" "+ getDescription()+ Config.getLineSeparator()+sb.toString();
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

    public boolean hasLongOption(String optionName) {
        for(ProcessedOption o : getOptions()) {
            if (o.getName().equals(optionName))
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
                if(o.getName().startsWith(optionName) && !o.getName().equals(optionName))
                    return false;
            }
            return true;
        }
        return false;
    }

    public void updateInvocationProviders(InvocationProviders invocationProviders) {
        for (ProcessedOption option : getOptions())
            option.updateInvocationProviders(invocationProviders);
        activator = invocationProviders.getCommandActivatorProvider().enhanceCommandActivator(activator);
    }

    public boolean containsArgumentWithDefaultValues() {
        return getArgument() != null && getArgument().hasDefaultValue();
    }

    public void addParserException(CommandLineParserException exception) {
        parserExceptions.add(exception);
    }

    public List<CommandLineParserException> parserExceptions() {
        return parserExceptions;
    }
}
