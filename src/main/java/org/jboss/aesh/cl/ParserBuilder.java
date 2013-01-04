/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.internal.OptionInt;
import org.jboss.aesh.cl.internal.ParameterInt;

import java.util.ArrayList;
import java.util.List;

/**
 * Build a {@link ParameterInt} object using the Builder pattern.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParserBuilder {

    private String name;
    private String usage;
    private List<OptionInt> options;


    public ParserBuilder() {
        options = new ArrayList<OptionInt>();
    }

    public ParserBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ParserBuilder usage(String usage) {
        this.usage = usage;
        return this;
    }

    public ParserBuilder addOption(OptionInt option) {
        this.options.add(option);
        return this;
    }

    public ParserBuilder addOptions(List<OptionInt> options) {
        this.options.addAll(options);
        return this;
    }

    public CommandLineParser generateParser() throws IllegalArgumentException {
        if(name == null || name.length() < 1)
            throw new RuntimeException("The parameter name must be defined");
        return new CommandLineParser( new ParameterInt(name, usage, options));
    }
}
