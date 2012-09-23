/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.cl;

import java.util.ArrayList;
import java.util.List;

/**
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

    public void addOption(ParsedOption option) {
        ParsedOption existingOption = getOption(option.getName());
        if (existingOption == null) {
            options.add(option);
        }
        else {
            if((existingOption.getProperties() == null ||
                    existingOption.getProperties().size() == 0) ||
            (option.getProperties() == null || existingOption.getProperties().size() == 0))
                throw new IllegalArgumentException("Not allowed to specify the same option twice");
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
            if(po.getName().equals(name) ||
                    po.getLongName().equals(name))
                return po;
        }
        return null;
    }

    public boolean hasOption(String name) {
        for(ParsedOption po : options) {
            if(po.getName().equals(name) ||
                    po.getLongName().equals(name))
                return true;
        }
        return false;
    }

    public String getOptionValue(String name) {
        return getOptionValue(name, null);
    }

    public String getOptionValue(String name, String fallback) {
        for(ParsedOption po : options) {
            if(po.getName().equals(name) ||
                    po.getLongName().equals(name))
                return po.getValue();
        }
        return fallback;
    }

    public List<String> getOptionValues(String name) {
        return getOptionValues(name, new ArrayList<String>());
    }

    public List<String> getOptionValues(String name, List<String> fallback) {
        for(ParsedOption po : options) {
            if(po.getName().equals(name) ||
                    po.getLongName().equals(name))
                return po.getValues();
        }

        return fallback;
    }

    public List<OptionProperty> getOptionProperties(String name) {
        for(ParsedOption po : options) {
            if(po.getName().equals(name) ||
                    po.getLongName().equals(name))
                return po.getProperties();
        }

        return new ArrayList<OptionProperty>();
    }
}

