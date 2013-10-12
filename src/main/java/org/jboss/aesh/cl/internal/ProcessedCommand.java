/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.internal;

import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.cl.converter.CLConverter;
import org.jboss.aesh.cl.exception.OptionParserException;
import org.jboss.aesh.cl.validator.OptionValidator;
import org.jboss.aesh.console.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public final class ProcessedCommand {

    private String name;
    private String usage;

    private List<ProcessedOption> options;
    private ProcessedOption argument;

    public ProcessedCommand(String name, String usage) {
        setName(name);
        setUsage(usage);
        options = new ArrayList<ProcessedOption>();
    }

    public ProcessedCommand(String name, String usage, ProcessedOption argument) {
        setName(name);
        setUsage(usage);
        this.argument = argument;
        options = new ArrayList<ProcessedOption>();
    }

    public ProcessedCommand(String name, String usage,
                            ProcessedOption argument, ProcessedOption[] options) throws OptionParserException {
        setName(name);
        setUsage(usage);
        this.argument = argument;
        this.options = new ArrayList<ProcessedOption>();
        setOptions(Arrays.asList(options));
    }

    public ProcessedCommand(String name, String usage,
                            ProcessedOption argument, List<ProcessedOption> options) throws OptionParserException {
        setName(name);
        setUsage(usage);
        this.argument = argument;
        this.options = new ArrayList<ProcessedOption>();
        setOptions(options);
    }

    public List<ProcessedOption> getOptions() {
        return options;
    }

    public void addOption(ProcessedOption opt) throws OptionParserException {
        this.options.add(new ProcessedOption(verifyThatNamesAreUnique(opt.getShortName(), opt.getName()), opt.getName(),
                opt.getDescription(), opt.getArgument(), opt.isRequired(), opt.getValueSeparator(),
                opt.getDefaultValues(), opt.getType(), opt.getFieldName(), opt.getOptionType(), opt.getConverter(),
                opt.getCompleter(), opt.getValidator()));
    }

    /**
     * Add an option
     * Name or name can be null
     * Both argument and type can be null
     *
     * @param name name (short) one char
     * @param longName multi character name
     * @param description a description of the option
     * @param argument what kind of argument this option can have
     * @param required is it required?
     * @param valueSeparator separator char
     * @param defaultValue the default value
     * @param type what kind of type it is (not used)
     */
    public void addOption(char name, String longName, String description,
                     String argument, boolean required, char valueSeparator,
                     String[] defaultValue, Class<?> type, String fieldName, OptionType optionType,
                     Class<? extends CLConverter> converter,
                     Class<? extends OptionCompleter> completer, Class<? extends OptionValidator> validator) throws OptionParserException {
        List<String> defaultValues = new ArrayList<String>();
        defaultValues.addAll(Arrays.asList(defaultValue));
        options.add(new ProcessedOption(verifyThatNamesAreUnique(name, longName), longName, description,
                argument, required, valueSeparator, defaultValues,
                type, fieldName, optionType, converter, completer, validator));
    }

    private void setOptions(List<ProcessedOption> options) throws OptionParserException {
        for(ProcessedOption opt : options) {
            this.options.add(new ProcessedOption(verifyThatNamesAreUnique(opt.getShortName(), opt.getName()), opt.getName(),
                    opt.getDescription(), opt.getArgument(), opt.isRequired(), opt.getValueSeparator(),
                    opt.getDefaultValues(), opt.getType(), opt.getFieldName(), opt.getOptionType(),
                    opt.getConverter(), opt.getCompleter(), opt.getValidator()));
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
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

    private char verifyThatNamesAreUnique(String name, String longName) throws OptionParserException {
        return verifyThatNamesAreUnique(name.charAt(0), longName);
    }

    private char verifyThatNamesAreUnique(char name, String longName) throws OptionParserException {
        if(longName != null && longName.length() > 0 && findLongOption(longName) != null) {
            throw new OptionParserException("Option --"+longName+" is already added to Param: "+this.toString());
        }
        if(name != '\u0000'&& findOption(String.valueOf(name)) != null) {
            throw new OptionParserException("Option -"+name+" is already added to Param: "+this.toString());
        }

        //if name is null, use one based on name
        if(name == '\u0000') {
            if(longName != null && longName.length() > 0)
                return findPossibleName(longName);
            else
                throw new OptionParserException("Neither option name and option long name can be both null");
        }
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
                    option.getValidator().isEnabled(this))
                return option;

        return null;
    }

    public ProcessedOption findLongOption(String name) {
        for(ProcessedOption option : options)
            if(option.getName() != null &&
                    option.getName().equals(name) &&
                    option.getValidator().isEnabled(this))
                return option;

        return null;
    }

    public ProcessedOption startWithOption(String name) {
        for(ProcessedOption option : options)
            if(name.startsWith(option.getShortName()) &&
                    option.getValidator().isEnabled(this))
                return option;

        return null;
    }

    public ProcessedOption startWithLongOption(String name) {
        for(ProcessedOption option : options)
            if(name.startsWith(option.getName()) &&
                option.getValidator().isEnabled(this))
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
    public List<String> getOptionLongNamesWithDash() {
        List<String> names = new ArrayList<String>(options.size());
        for(ProcessedOption o : options) {
            if(o.getValues().size() == 0 &&
                    o.getValidator().isEnabled(this))
                names.add("--"+o.getName());
        }

        return names;
    }

    public List<String> findPossibleLongNamesWitdDash(String name) {
        List<String> names = new ArrayList<String>(options.size());
        for(ProcessedOption o : options) {
           if((o.getShortName().equals(name) || o.getName().startsWith(name)) &&
                   o.getValidator().isEnabled(this))
               names.add("--"+o.getName());
        }
        return names;
    }

    /**
     * Returns a usage String based on the defined command and options.
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
        for(ProcessedOption o : getOptions())
            sb.append(o.getFormattedOption(2, maxLength+4, width)).append(Config.getLineSeparator());
        return "Usage: "+getName()+" "+getUsage()+ Config.getLineSeparator()+sb.toString();
    }

    @Override
    public String toString() {
        return "ProcessedCommand{" +
                "name='" + name + '\'' +
                ", description='" + usage + '\'' +
                ", options=" + options +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProcessedCommand)) return false;

        ProcessedCommand that = (ProcessedCommand) o;

        if (!name.equals(that.name)) return false;
        if (usage != null ? !usage.equals(that.usage) : that.usage != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (usage != null ? usage.hashCode() : 0);
        return result;
    }

    public boolean hasLongOption(String optionName) {
        for(ProcessedOption o : getOptions()) {
            if (o.getName().equals(optionName))
                return true;
        }
        return false;
    }
}
