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

    private static ParserBuilder builder = new ParserBuilder();
    private static String name;
    private static String usage;
    private static List<OptionInt> options;


    private ParserBuilder() {
        reset();
    }

    public static ParserBuilder init() {
        reset();
        return builder;
    }

    public ParserBuilder name(String name) {
        ParserBuilder.name = name;
        return builder;
    }

    public ParserBuilder usage(String usage) {
        ParserBuilder.usage = usage;
        return builder;
    }

    public ParserBuilder addOption(OptionInt option) {
        ParserBuilder.options.add(option);
        return builder;
    }

    public ParserBuilder addOptions(List<OptionInt> options) {
        ParserBuilder.options.addAll(options);
        return builder;
    }

    public CommandLineParser generateParser() {
        CommandLineParser clp = new CommandLineParser(
                new ParameterInt(ParserBuilder.name, ParserBuilder.usage, ParserBuilder.options));

        reset();
        return clp;
    }

    private static void reset() {
        ParserBuilder.name = name;
        ParserBuilder.usage = usage;
        options = new ArrayList<OptionInt>();
    }
}
