/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.exception.OptionParserException;

import java.util.ArrayList;
import java.util.List;

/**
 * A parsed String based on the provided Command and Options defined
 * in a {@link CommandLineParser}.
 *
 * All found options and arguments can be queried after.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLine {

    private List<ParsedOption> options;
    private List<String> arguments;

    public CommandLine() {
        options = new ArrayList<ParsedOption>();
        arguments = new ArrayList<String>();
    }

    public void addOption(ParsedOption option) throws OptionParserException {
        ParsedOption existingOption = getOption(option.getShortName());
        if (existingOption == null) {
            options.add(option);
        }
        else {
            if((existingOption.getProperties() == null ||
                    existingOption.getProperties().size() == 0) ||
            (option.getProperties() == null || existingOption.getProperties().size() == 0))
                throw new OptionParserException("Not allowed to specify the same option ("+option.getDisplayName()+") twice");
            else
                existingOption.getProperties().addAll(option.getProperties());
        }

    }

    protected List<ParsedOption> getOptions() {
        return options;
    }

    public void addArgument(String arg) {
        arguments.add(arg);
    }

    public List<String> getArguments() {
        return arguments;
    }

    public boolean hasOption(char name) {
       return hasOption(String.valueOf(name));
    }

    private ParsedOption getOption(String name) {
        for(ParsedOption po : options) {
            if((po.getShortName() != null && po.getShortName().equals(name)) ||
                    (po.getName() != null && po.getName().equals(name)))
                return po;
        }
        return null;
    }

    public boolean hasOption(String name) {
        for(ParsedOption po : options) {
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
        for(ParsedOption po : options) {
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
        for(ParsedOption po : options) {
            if((po.getShortName() != null && po.getShortName().equals(name)) ||
                    (po.getName() != null && po.getName().equals(name)))
                return po.getValues();
        }

        return fallback;
    }

    public List<OptionProperty> getOptionProperties(String name) {
        for(ParsedOption po : options) {
            if((po.getShortName() != null && po.getShortName().equals(name)) ||
                    (po.getName() != null && po.getName().equals(name)))
                return po.getProperties();
        }

        return new ArrayList<OptionProperty>();
    }
}

