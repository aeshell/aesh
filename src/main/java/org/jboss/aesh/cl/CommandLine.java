/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.exception.OptionParserException;
import org.jboss.aesh.cl.internal.ProcessedOption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A parsed String based on the provided CommandDefinition and Options defined
 * in a {@link org.jboss.aesh.cl.parser.AeshCommandLineParser}.
 *
 * All found options and argument can be queried after.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLine {

    private List<ProcessedOption> options;
    private ProcessedOption argument;
    private boolean parserError;
    private CommandLineParserException parserException;

    public CommandLine() {
        options = new ArrayList<>();
    }

    public CommandLine(CommandLineParserException parserException) {
        this();
        if(parserException != null)
            setParserException(parserException);
    }

    public void addOption(ProcessedOption option) {
        ProcessedOption existingOption = getOption(option.getName());
        if (existingOption == null) {
            options.add(option);
        }
        else {
            if((existingOption.getProperties() == null ||
                    existingOption.getProperties().size() == 0) ||
            (option.getProperties() == null || existingOption.getProperties().size() == 0)) {
                setParserError(true);
                setParserException( new OptionParserException("Not allowed to specify the same option ("+option.getDisplayName()+") twice"));
            }
            else
                existingOption.getProperties().putAll(option.getProperties());
        }

    }

    public List<ProcessedOption> getOptions() {
        return options;
    }

    public void addArgumentValue(String arg) {
        argument.addValue(arg);
    }

    public void setArgument(ProcessedOption argument) {
        this.argument = argument;
    }

    public ProcessedOption getArgument() {
        return argument;
    }

    public boolean hasOption(char name) {
       return hasOption(String.valueOf(name));
    }

    public ProcessedOption getOption(String name) {
        for(ProcessedOption po : options) {
            if((po.getShortName() != null && po.getShortName().equals(name)) ||
                    (po.getName() != null && po.getName().equals(name)))
                return po;
        }
        return null;
    }

    public boolean hasOption(String name) {
        for(ProcessedOption po : options) {
            if((po.getShortName() != null && po.getShortName().equals(name)) ||
                    po.getName().equals(name))
                return true;
        }
        return false;
    }

    public String getOptionValue(char c) {
        return getOptionValue(String.valueOf(c));
    }

    public String getOptionValue(String name) {
        return getOptionValue(name, null);
    }

    public String getOptionValue(String name, String fallback) {
        for(ProcessedOption po : options) {
            if((po.getShortName() != null && po.getShortName().equals(name)) ||
                    (po.getName() != null && po.getName().equals(name)))
                return po.getValue();
        }
        return fallback;
    }

    public List<String> getOptionValues(char c) {
        return getOptionValues(String.valueOf(c), new ArrayList<String>());
    }

    public List<String> getOptionValues(String name) {
        return getOptionValues(name, new ArrayList<String>());
    }

    public List<String> getOptionValues(String name, List<String> fallback) {
        for(ProcessedOption po : options) {
            if((po.getShortName() != null && po.getShortName().equals(name)) ||
                    (po.getName() != null && po.getName().equals(name)))
                return po.getValues();
        }

        return fallback;
    }

    public Map<String,String> getOptionProperties(String name) {
        for(ProcessedOption po : options) {
            if((po.getShortName() != null && po.getShortName().equals(name)) ||
                    (po.getName() != null && po.getName().equals(name)))
                return po.getProperties();
        }

        return new HashMap<>();
    }

    public boolean hasParserError() {
        return parserError;
    }

    public void setParserError(boolean error) {
        this.parserError = error;
    }

    public CommandLineParserException getParserException() {
        return parserException;
    }

    public void setParserException(CommandLineParserException e) {
        this.parserException = e;
        if(parserException != null)
            this.parserError = true;
    }

    public boolean hasOptionWithOverrideRequired() {
        for(ProcessedOption option : options) {
            if(option.doOverrideRequired())
                return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "CommandLine{" +
                "options=" + options +
                ", argument=" + argument +
                ", parserError=" + parserError +
                ", parserException=" + parserException +
                '}';
    }
}

