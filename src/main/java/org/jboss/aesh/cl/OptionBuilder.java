/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.internal.OptionInt;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class OptionBuilder {

    private static char name;
    private static String longName;
    private static String description;
    private static String argument;
    private static boolean hasValue = true;
    private static boolean required = false;
    private static boolean isProperty = false;
    private static boolean hasMultipleValues = false;
    private static char valueSeparator;

    private static OptionBuilder builder = new OptionBuilder();

    private OptionBuilder() {
    }

    public static OptionBuilder init() {
        return builder;
    }

    public OptionBuilder name(char n) {
        name = n;
        return builder;
    }

    public OptionBuilder longName(String longName) {
        OptionBuilder.longName = longName;
        return builder;
    }

    public OptionBuilder description(String description) {
        OptionBuilder.description = description;
        return builder;
    }

    public OptionBuilder argument(String argument) {
        OptionBuilder.argument = argument;
        return builder;
    }

    public OptionBuilder hasValue(boolean hasValue) {
        OptionBuilder.hasValue = hasValue;
        return builder;
    }

    public OptionBuilder required(boolean required) {
        OptionBuilder.required = required;
        return builder;
    }

    public OptionBuilder isProperty(boolean isProperty) {
        OptionBuilder.isProperty = isProperty;
        return builder;
    }

    public OptionBuilder hasMultipleValues(boolean hasMultipleValues) {
        OptionBuilder.hasMultipleValues = hasMultipleValues;
        return builder;
    }

    public OptionBuilder valueSeparator(char valueSeparator) {
        OptionBuilder.valueSeparator = valueSeparator;
        return builder;
    }

    public OptionInt create() {
        OptionInt option = new OptionInt(name, longName, description, hasValue, argument, required,
                valueSeparator, isProperty, hasMultipleValues, null);
        reset();
        return option;
    }

    private void reset() {
        name = '\u0000';
        longName = "";
        description = "";
        argument = "";
        hasValue = true;
        required = false;
        isProperty = false;
        hasMultipleValues = false;
        valueSeparator = ',';
    }
}
