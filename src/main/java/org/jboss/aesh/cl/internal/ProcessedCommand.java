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
package org.jboss.aesh.cl.internal;

import org.jboss.aesh.cl.parser.OptionParserException;
import org.jboss.aesh.cl.populator.AeshCommandPopulator;
import org.jboss.aesh.cl.populator.CommandPopulator;
import org.jboss.aesh.cl.result.NullResultHandler;
import org.jboss.aesh.cl.result.ResultHandler;
import org.jboss.aesh.cl.validator.CommandValidator;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.InvocationProviders;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.terminal.TerminalString;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public final class ProcessedCommand<C extends Command> {

    private String name;
    private String description;
    private CommandValidator validator;
    private ResultHandler resultHandler = new NullResultHandler();
    private CommandPopulator populator;

    private List<ProcessedOption> options;
	private List<ProcessedInputPrompt> inputPrompts;
    private ProcessedOption argument;
    private C command;

    public ProcessedCommand(String name, C command, String description, CommandValidator validator, ResultHandler resultHandler,
                            ProcessedOption argument, List<ProcessedOption> options, CommandPopulator populator ) throws OptionParserException {
        setName(name);
        setDescription(description);
        this.validator = validator;
        this.resultHandler = resultHandler;
        this.argument = argument;
        this.options = new ArrayList<>();
		this.inputPrompts = new ArrayList<>();
        this.command = command;
        if(populator == null)
            this.populator = new AeshCommandPopulator(this.command);
        else
            this.populator = populator;
        setOptions(options);
    }

    public List<ProcessedOption> getOptions() {
        return options;
    }

    public void addOption(ProcessedOption opt) throws OptionParserException {
        this.options.add(new ProcessedOption(verifyThatNamesAreUnique(opt.getShortName(), opt.getName()), opt.getName(),
                opt.getDescription(), opt.getArgument(), opt.isRequired(), opt.getValueSeparator(),
                opt.getDefaultValues(), opt.getType(), opt.getFieldName(), opt.getOptionType(), opt.getConverter(),
                opt.getCompleter(), opt.getValidator(), opt.getActivator(), opt.getRenderer(), opt.doOverrideRequired()));
    }

    private void setOptions(List<ProcessedOption> options) throws OptionParserException {
        for(ProcessedOption opt : options) {
            this.options.add(new ProcessedOption(verifyThatNamesAreUnique(opt.getShortName(), opt.getName()), opt.getName(),
                    opt.getDescription(), opt.getArgument(), opt.isRequired(), opt.getValueSeparator(),
                    opt.getDefaultValues(), opt.getType(), opt.getFieldName(), opt.getOptionType(),
                    opt.getConverter(), opt.getCompleter(), opt.getValidator(), opt.getActivator(), opt.getRenderer(),
                    opt.doOverrideRequired()));
        }
    }

	public List<ProcessedInputPrompt> getInputPrompts() {
		return inputPrompts;
	}

	public void addInputPrompt(ProcessedInputPrompt ip) throws OptionParserException {
		inputPrompts.add(ip);
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

    public CommandPopulator getCommandPopulator() {
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
        for(ProcessedOption option : options)
            if(option.getShortName() != null &&
                    option.getShortName().equals(name) &&
                    option.getActivator().isActivated(this))
                return option;

        return null;
    }

    public ProcessedOption findOptionNoActivatorCheck(String name) {
        for(ProcessedOption option : options)
            if(option.getShortName() != null &&
                    option.getShortName().equals(name))
                return option;

        return null;
    }

    public ProcessedOption findLongOption(String name) {
        for(ProcessedOption option : options)
            if(option.getName() != null &&
                    option.getName().equals(name) &&
                    option.getActivator().isActivated(this))
                return option;

        return null;
    }

    public ProcessedOption findLongOptionNoActivatorCheck(String name) {
        for(ProcessedOption option : options)
            if(option.getName() != null && option.getName().equals(name))
                return option;

        return null;
    }

    public ProcessedOption startWithOption(String name) {
        for(ProcessedOption option : options)
            if(option.getShortName() != null && name.startsWith(option.getShortName()) &&
                    option.getActivator().isActivated(this))
                return option;

        return null;
    }

    public ProcessedOption startWithLongOption(String name) {
        for(ProcessedOption option : options)
            if(name.startsWith(option.getName()) &&
                    option.getActivator().isActivated(this))
                return option;

        return null;
    }

   public void clear() {
       for(ProcessedOption processedOption : options)
           processedOption.clear();
       if(argument != null)
           argument.clear();
    }

    /**
     * Return all option names that not already have a value
     * and is enabled
     */
    public List<TerminalString> getOptionLongNamesWithDash() {
        List<TerminalString> names = new ArrayList<>(options.size());
        for(ProcessedOption o : options) {
            if(o.getValues().size() == 0 &&
                    o.getActivator().isActivated(this))
                names.add(o.getRenderedNameWithDashes());
        }

        return names;
    }

    public List<TerminalString> findPossibleLongNamesWitdDash(String name) {
        List<TerminalString> names = new ArrayList<>(options.size());
        for(ProcessedOption o : options) {
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
        for(ProcessedOption o : getOptions())
            if(o.getFormattedLength() > maxLength)
                maxLength = o.getFormattedLength();

        StringBuilder sb = new StringBuilder();
        if(getOptions().size() > 0)
           sb.append(Config.getLineSeparator()).append("Options:").append(Config.getLineSeparator());
        for(ProcessedOption o : getOptions())
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
                ", options=" + options +
                '}';
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
        return getOptions() != null && getOptions().size() > 0;
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
        for(ProcessedOption option : options)
            option.updateInvocationProviders(invocationProviders);
    }

    public void updateSettings(Settings settings) {
        for(ProcessedOption option : options)
            option.updateAnsiMode(settings.isAnsiConsole());
    }

    public boolean containsArgumentWithDefaultValues() {
        return getArgument() != null && getArgument().hasDefaultValue();
    }
}
