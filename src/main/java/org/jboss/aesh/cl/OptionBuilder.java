/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.internal.OptionInt;

/**
 * Build a {@link OptionInt} object using the Builder pattern.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class OptionBuilder {

    private char name = '\u0000';
    private String longName = "";
    private String description;
    private String argument;
    private Class<?> type;
    private boolean hasValue = true;
    private boolean required = false;
    private boolean isProperty = false;
    private boolean hasMultipleValues = false;
    private char valueSeparator = ',';

    public OptionBuilder() {
    }

    public OptionBuilder name(char n) {
        name = n;
        return this;
    }

    public OptionBuilder longName(String longName) {
        this.longName = longName;
        return this;
    }

    public OptionBuilder description(String description) {
        this.description = description;
        return this;
    }

    public OptionBuilder argument(String argument) {
        this.argument = argument;
        return this;
    }

    public OptionBuilder type(Class<?> type) {
        this.type = type;
        return this;
    }

    public OptionBuilder hasValue(boolean hasValue) {
        this.hasValue = hasValue;
        return this;
    }

    public OptionBuilder required(boolean required) {
        this.required = required;
        return this;
    }

    public OptionBuilder isProperty(boolean isProperty) {
        this.isProperty = isProperty;
        return this;
    }

    public OptionBuilder hasMultipleValues(boolean hasMultipleValues) {
        this.hasMultipleValues = hasMultipleValues;
        return this;
    }

    public OptionBuilder valueSeparator(char valueSeparator) {
        this.valueSeparator = valueSeparator;
        return this;
    }

    public OptionInt create() {
        return new OptionInt(name, longName, description, hasValue, argument, required,
                valueSeparator, isProperty, hasMultipleValues, type);
    }
}
