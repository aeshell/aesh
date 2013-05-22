/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.OptionInt;
import org.jboss.aesh.cl.internal.ParameterInt;

import java.util.ArrayList;
import java.util.List;

/**
 * Build a {@link ParameterInt} object using the Builder pattern.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParameterBuilder {

    private String name;
    private String usage;
    private Class<?> argumentType = String.class;
    private List<OptionInt> options;


    public ParameterBuilder() {
        options = new ArrayList<OptionInt>();
    }

    public ParameterBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ParameterBuilder usage(String usage) {
        this.usage = usage;
        return this;
    }

    public ParameterBuilder argumentType(Class<?> type) {
        this.argumentType = type;
        return this;
    }

    public ParameterBuilder addOption(OptionInt option) {
        this.options.add(option);
        return this;
    }

    public ParameterBuilder addOptions(List<OptionInt> options) {
        this.options.addAll(options);
        return this;
    }

    public ParameterInt generateParameter() throws CommandLineParserException {
        if(name == null || name.length() < 1)
            throw new CommandLineParserException("The parameter name must be defined");
        return  new ParameterInt(name, usage, argumentType, options);
    }
}
