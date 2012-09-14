/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.cl.internal;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParameterInt {

    private String usage;
    private OptionInt[] options;

    public ParameterInt(String usage, OptionInt[] options) {
        setUsage(usage);
       setOptions(options);
    }

    public OptionInt[] getOptions() {
        return options;
    }

    private void setOptions(OptionInt[] options) {
        this.options = options;
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

    public OptionInt startWithLongOption(String name) {
        for(OptionInt option : options)
            if(option.getLongName().startsWith(name))
                return option;

        return null;
    }

}
