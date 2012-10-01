/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.internal;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class OptionInt {

    private String name;
    private String longName;

    private String description;
    private boolean hasValue = true;
    private String value;
    private String argument;
    private Object type;
    private boolean required = false;
    private char valueSeparator;
    private boolean isProperty = false;
    private Map<String,String> properties;

    public OptionInt(char name, String longName, String description, boolean hasValue,
                     String argument, boolean required, char valueSeparator,
                     boolean isProperty, Object type) {
        this.name = String.valueOf(name);
        this.longName = longName;
        this.description = description;
        this.hasValue = hasValue;
        this.argument = argument;
        this.required = required;
        this.valueSeparator = valueSeparator;
        this.isProperty = isProperty;
        this.type = type;

        properties = new HashMap<String, String>();
    }

    public String getName() {
        return name;
    }

    public String getLongName() {
        return longName;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean hasValue() {
        return hasValue;
    }

    public boolean isRequired() {
        return required;
    }

    public Object getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public char getValueSeparator() {
       return valueSeparator;
    }

    public boolean isProperty() {
        return isProperty;
    }

    public String getArgument() {
        return argument;
    }

    public void addProperty(String name, String value) {
        properties.put(name,value);
    }

    public Map<String,String> getProperties() {
        return properties;
    }

}
