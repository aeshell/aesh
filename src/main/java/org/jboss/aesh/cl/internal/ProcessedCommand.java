/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.internal;

import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.cl.converter.Converter;
import org.jboss.aesh.cl.exception.OptionParserException;
import org.jboss.aesh.cl.renderer.OptionRenderer;
import org.jboss.aesh.cl.result.NullResultHandler;
import org.jboss.aesh.cl.result.ResultHandler;
import org.jboss.aesh.cl.validator.CommandValidator;
import org.jboss.aesh.cl.validator.NullCommandValidator;
import org.jboss.aesh.cl.validator.OptionValidator;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.terminal.TerminalString;
import org.jboss.aesh.util.ReflectionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public final class ProcessedCommand {

    private String name;
    private String description;
    private CommandValidator validator;
    private ResultHandler resultHandler = new NullResultHandler();

    private List<ProcessedOption> options;
    private ProcessedOption argument;

    public ProcessedCommand(String name, String description, CommandValidator validator) {
        setName(name);
        setDescription(description);
        setValidator(validator);
        options = new ArrayList<>();
    }

    public ProcessedCommand(String name, String description,Class<? extends CommandValidator> validator,
                            Class<? extends ResultHandler> resultHandler) {
        setName(name);
        setDescription(description);
        setValidator(initValidator(validator));
        setResultHandler(initResultHandler(resultHandler));
        options = new ArrayList<>();
    }

    public ProcessedCommand(String name, String description, ProcessedOption argument) {
        setName(name);
        setDescription(description);
        this.argument = argument;
        options = new ArrayList<>();
    }

    public ProcessedCommand(String name, String description, CommandValidator validator, ResultHandler resultHandler,
                            ProcessedOption argument, List<ProcessedOption> options) throws OptionParserException {
        setName(name);
        setDescription(description);
        setValidator(validator);
        setResultHandler(resultHandler);
        this.argument = argument;
        this.options = new ArrayList<>();
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
                     Class<? extends Converter> converter,
                     Class<? extends OptionCompleter> completer,
                     Class<? extends OptionValidator> validator,
                     Class<? extends OptionActivator> activator,
                     Class<? extends OptionRenderer> renderer) throws OptionParserException {
        List<String> defaultValues = new ArrayList<>();
        defaultValues.addAll(Arrays.asList(defaultValue));
        options.add(new ProcessedOption(verifyThatNamesAreUnique(name, longName), longName, description,
                argument, required, valueSeparator, defaultValues,
                type, fieldName, optionType, converter, completer, validator, activator, renderer));
    }

    public void addOption(char name, String longName, String description,
                     String argument, boolean required, char valueSeparator,
                     String[] defaultValue, Class<?> type, String fieldName, OptionType optionType,
                     Class<? extends Converter> converter,
                     Class<? extends OptionCompleter> completer,
                     Class<? extends OptionValidator> validator,
                     Class<? extends OptionActivator> activator,
                     Class<? extends OptionRenderer> renderer, boolean overrideRequired) throws OptionParserException {
        List<String> defaultValues = new ArrayList<>();
        defaultValues.addAll(Arrays.asList(defaultValue));
        options.add(new ProcessedOption(verifyThatNamesAreUnique(name, longName), longName, description,
                argument, required, valueSeparator, defaultValues,
                type, fieldName, optionType, converter, completer, validator, activator, renderer, overrideRequired));
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

    private CommandValidator initValidator(Class<? extends CommandValidator> validator) {
        if(validator != null && validator != NullCommandValidator.class)
            return ReflectionUtil.newInstance(validator);
        else
            return new NullCommandValidator();
    }

    private ResultHandler initResultHandler(Class<? extends ResultHandler> resultHandler) {
        if(resultHandler != null && resultHandler != NullResultHandler.class)
            return ReflectionUtil.newInstance(resultHandler);
        else
            return new NullResultHandler();
    }

    public CommandValidator getValidator() {
        return validator;
    }

    private void setValidator(CommandValidator validator) {
        this.validator = validator;
    }

    public ResultHandler getResultHandler() { return resultHandler; }

    private void setResultHandler(ResultHandler resultHandler) {
        this.resultHandler = resultHandler;
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
        for(ProcessedOption o : getOptions())
            sb.append(o.getFormattedOption(2, maxLength+4, width)).append(Config.getLineSeparator());
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
}
