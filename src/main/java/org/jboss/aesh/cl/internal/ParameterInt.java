/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParameterInt {

    private String name;
    private String usage;
    private List<OptionInt> options;

    public ParameterInt(String name, String usage) {
        setName(name);
        setUsage(usage);
        setOptions(new ArrayList<OptionInt>());
    }

    public ParameterInt(String name, String usage, OptionInt[] options) {
        setName(name);
        setUsage(usage);
        setOptions(Arrays.asList(options));
    }

    public ParameterInt(String name, String usage, List<OptionInt> options) {
        setName(name);
        setUsage(usage);
        setOptions(options);
    }

    public List<OptionInt> getOptions() {
        return options;
    }

    public void addOption(char name, String longName, String description, boolean hasValue,
                     String argument, boolean required, boolean hasMultipleValues, Object type) {
        options.add(new OptionInt(name, longName, description,
                hasValue, argument, required, '\u0000', false, hasMultipleValues, type));
    }

    private void setOptions(List<OptionInt> options) {
        this.options = options;
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

    public OptionInt findOption(String name) {
        for(OptionInt option : options)
            if(option.getName().equals(name))
                return option;

        return null;
    }

    public OptionInt findLongOption(String name) {
        for(OptionInt option : options)
            if(option.getLongName().equals(name))
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
}
