/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.internal;

import org.jboss.aesh.cl.exception.OptionParserException;
import org.jboss.aesh.console.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParameterInt {

    private String name;
    private String usage;

   private Class<?> argumentType;
    private List<OptionInt> options;

    public ParameterInt(String name, String usage) {
        setName(name);
        setUsage(usage);
        options = new ArrayList<OptionInt>();
    }

    public ParameterInt(String name, String usage, Class<?> argumentType) {
        setName(name);
        setUsage(usage);
        setArgumentType(argumentType);
        options = new ArrayList<OptionInt>();
    }

    public ParameterInt(String name, String usage,
                        Class<?> argumentType, OptionInt[] options) throws OptionParserException {
        setName(name);
        setUsage(usage);
        setArgumentType(argumentType);
        this.options = new ArrayList<OptionInt>();
        setOptions(Arrays.asList(options));
    }

    public ParameterInt(String name, String usage,
                        Class<?> argumentType, List<OptionInt> options) throws OptionParserException {
        setName(name);
        setUsage(usage);
        setArgumentType(argumentType);
        this.options = new ArrayList<OptionInt>();
        setOptions(options);
    }

    public List<OptionInt> getOptions() {
        return options;
    }

    public void addOption(OptionInt opt) throws OptionParserException {
        this.options.add(new OptionInt(verifyThatNamesAreUnique(opt.getName(), opt.getLongName()), opt.getLongName(),
                opt.getDescription(), opt.hasValue(), opt.getArgument(), opt.isRequired(), opt.getValueSeparator(),
                opt.isProperty(), opt.hasMultipleValues() , opt.getType()));
    }

    /**
     * Add an option
     * Name or longName can be null
     * Both argument and type can be null
     *
     * @param name name (short) one char
     * @param longName multi character name
     * @param description a description of the option
     * @param hasValue if this option has value
     * @param argument what kind of argument this option can have
     * @param required is it required?
     * @param valueSeparator separator char
     * @param isProperty is it a property
     * @param hasMultipleValues have it multiple values
     * @param type what kind of type it is (not used)
     */
    public void addOption(char name, String longName, String description, boolean hasValue,
                     String argument, boolean required, char valueSeparator, boolean isProperty,
                     boolean hasMultipleValues, Class<?> type) throws OptionParserException {
        options.add(new OptionInt(verifyThatNamesAreUnique(name, longName), longName, description,
                hasValue, argument, required, valueSeparator, isProperty, hasMultipleValues, type));
    }

    /**
     * Add an option
     * Name or longName can be null
     * Both argument and type can be null
     *
     * @param name name (short) one char
     * @param longName multi character name
     * @param description a description of the option
     * @param hasValue if this option has value
     * @param argument what kind of argument this option can have
     * @param required is it required?
     * @param type what kind of type it is (not used)
     */
    public void addOption(char name, String longName, String description, boolean hasValue,
                     String argument, boolean required, boolean hasMultipleValues, Class<?> type) throws OptionParserException {
        options.add(new OptionInt(verifyThatNamesAreUnique(name, longName), longName, description,
                hasValue, argument, required, '\u0000', false, hasMultipleValues, type));
    }

    /**
     * Add an option
     * Name or longName can be null
     *
     * @param name name (short) one char
     * @param longName multi character name
     * @param description a description of the option
     * @param hasValue if this option has value
     */
    public void addOption(char name, String longName, String description, boolean hasValue) throws OptionParserException {
        addOption(verifyThatNamesAreUnique(name, longName), longName, description, hasValue, null, false, false, null);
    }

    private void setOptions(List<OptionInt> options) throws OptionParserException {
        for(OptionInt opt : options) {
            this.options.add(new OptionInt(verifyThatNamesAreUnique(opt.getName(), opt.getLongName()), opt.getLongName(),
                    opt.getDescription(), opt.hasValue(), opt.getArgument(), opt.isRequired(), opt.getValueSeparator(),
                    opt.isProperty(), opt.hasMultipleValues(), opt.getType()));
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

    public Class<?> getArgumentType() {
        return argumentType;
    }

    public void setArgumentType(Class<?> argumentType) {
        this.argumentType = argumentType;
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

        //if name is null, use one based on longName
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

    public OptionInt findOption(String name) {
        for(OptionInt option : options)
            if(option.getName() != null && option.getName().equals(name))
                return option;

        return null;
    }

    public OptionInt findLongOption(String name) {
        for(OptionInt option : options)
            if(option.getLongName() != null && option.getLongName().equals(name))
                return option;

        return null;
    }

    public OptionInt startWithOption(String name) {
        for(OptionInt option : options)
            if(name.startsWith(option.getName()))
                return option;

        return null;
    }

    public OptionInt startWithLongOption(String name) {
        for(OptionInt option : options)
            if(name.startsWith(option.getLongName()))
                return option;

        return null;
    }

   public void clean() {
       for(OptionInt optionInt : options)
           optionInt.clean();
    }

    public List<String> getOptionLongNamesWithDash() {
        List<String> names = new ArrayList<String>(options.size());
        for(OptionInt o : options)
            names.add("--"+o.getLongName());

        return names;
    }

    public List<String> findPossibleLongNamesWitdDash(String name) {
        List<String> names = new ArrayList<String>(options.size());
        for(OptionInt o : options) {
           if(o.getName().equals(name) || o.getLongName().startsWith(name))
               names.add("--"+o.getLongName());
        }
        return names;
    }

    /**
     * Returns a usage String based on the defined parameter and options.
     * Useful when printing "help" info etc.
     *
     */
    public String printHelp() {
        int maxLength = 0;
        int width = 80;
        for(OptionInt o : getOptions())
            if(o.getFormattedLength() > maxLength)
                maxLength = o.getFormattedLength();

        StringBuilder sb = new StringBuilder();
        for(OptionInt o : getOptions())
            sb.append(o.getFormattedOption(2, maxLength+4, width)).append(Config.getLineSeparator());
        return "Usage: "+getName()+" "+getUsage()+ Config.getLineSeparator()+sb.toString();
    }

    @Override
    public String toString() {
        return "ParameterInt{" +
                "name='" + name + '\'' +
                ", usage='" + usage + '\'' +
                ", argumentType=" + argumentType +
                ", options=" + options +
                '}';
    }
}
