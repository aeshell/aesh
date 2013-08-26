/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.exception.OptionParserException;
import org.jboss.aesh.cl.internal.OptionInt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A parsed String based on the provided CommandDefinition and Options defined
 * in a {@link CommandLineParser}.
 *
 * All found options and argument can be queried after.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLine {

    private List<OptionInt> options;
    private OptionInt argument;
    private boolean parserError;
    private CommandLineParserException parserException;

    public CommandLine() {
        options = new ArrayList<OptionInt>();
    }

    public CommandLine(CommandLineParserException parserException) {
        this();
        if(parserException != null)
            setParserException(parserException);
    }

    public CommandLine(OptionInt argument) {
        options = new ArrayList<OptionInt>();
        this.argument = argument;
    }

    public void addOption(OptionInt option) {
        OptionInt existingOption = getOption(option.getShortName());
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

    protected List<OptionInt> getOptions() {
        return options;
    }

    public void addArgumentValue(String arg) {
        argument.addValue(arg);
    }

    public void setArgument(OptionInt argument) {
        this.argument = argument;
    }

    public OptionInt getArgument() {
        return argument;
    }

    public boolean hasOption(char name) {
       return hasOption(String.valueOf(name));
    }

    protected OptionInt getOption(String name) {
        for(OptionInt po : options) {
            if((po.getShortName() != null && po.getShortName().equals(name)) ||
                    (po.getName() != null && po.getName().equals(name)))
                return po;
        }
        return null;
    }

    public boolean hasOption(String name) {
        for(OptionInt po : options) {
            if(po.getShortName().equals(name) ||
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
        for(OptionInt po : options) {
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
        for(OptionInt po : options) {
            if((po.getShortName() != null && po.getShortName().equals(name)) ||
                    (po.getName() != null && po.getName().equals(name)))
                return po.getValues();
        }

        return fallback;
    }

    public Map<String,String> getOptionProperties(String name) {
        for(OptionInt po : options) {
            if((po.getShortName() != null && po.getShortName().equals(name)) ||
                    (po.getName() != null && po.getName().equals(name)))
                return po.getProperties();
        }

        return new HashMap<String, String>();
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
}

