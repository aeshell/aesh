/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.internal;

import org.jboss.aesh.cl.exception.OptionParserException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class OptionInt {

    private String name;
    private String longName;

    private String description;
    private boolean hasValue = true;
    private List<String> values;
    private String argument;
    private String defaultValue;
    private Class<?> type;
    private boolean required = false;
    private char valueSeparator;
    private boolean isProperty = false;
    private boolean hasMultipleValues = false;
    private Map<String,String> properties;

    public OptionInt(char name, String longName, String description, boolean hasValue,
                     String argument, boolean required, char valueSeparator,
                     boolean isProperty, boolean hasMultipleValues, String defaultValue, Class<?> type) throws OptionParserException {
        this.name = String.valueOf(name);
        this.longName = longName;
        this.description = description;
        this.hasValue = hasValue;
        this.argument = argument;
        this.required = required;
        this.valueSeparator = valueSeparator;
        this.isProperty = isProperty;
        this.type = type;
        this.hasMultipleValues = hasMultipleValues;
        this.defaultValue = defaultValue;

        properties = new HashMap<String, String>();
        values = new ArrayList<String>();

        if((name == Character.MIN_VALUE) && longName.equals("")) {
            throw new OptionParserException("Either name or long name must be set.");
        }
    }

    public String getName() {
        return name;
    }

    public String getLongName() {
        return longName;
    }

    public void addValue(String value) {
        values.add(value);
    }

    public String getValue() {
        if(values.size() > 0)
            return values.get(0);
        else
            return null;
    }

    public List<String> getValues() {
        return values;
    }

    public boolean hasValue() {
        return hasValue || hasMultipleValues;
    }

    public boolean hasMultipleValues() {
        return hasMultipleValues;
    }

    public boolean isRequired() {
        return required;
    }

    public Class<?> getType() {
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

    public String getDefaultValue() {
        return defaultValue;
    }

    public void addProperty(String name, String value) {
        properties.put(name,value);
    }

    public Map<String,String> getProperties() {
        return properties;
    }

    public void clean() {
        values.clear();
        properties.clear();
    }

    public String getDisplayName() {
        if(longName != null) {
            return "--"+longName;
        }
        else
            return "-"+name;
    }

    public int getFormattedLength() {
        StringBuilder sb = new StringBuilder();
        if(name != null)
            sb.append("-").append(name);
        if(longName != null) {
            if(sb.toString().trim().length() > 0)
                sb.append(", ");
            sb.append("--").append(longName);
        }
        if(argument != null && argument.length() > 0) {
            sb.append("=<").append(argument).append(">");
        }

        return sb.length();
    }

    //TODO: add offset, offset for descriptionstart and break on width
    public String getFormattedOption(int offset, int descriptionStart, int width) {
        StringBuilder sb = new StringBuilder();
        if(offset > 0)
            sb.append(String.format("%" + offset+ "s", ""));
        if(name != null)
            sb.append("-").append(name);
        if(longName != null) {
            if(sb.toString().trim().length() > 0)
                sb.append(", ");
            sb.append("--").append(longName);
        }
        if(argument != null && argument.length() > 0) {
            sb.append("=<").append(argument).append(">");
        }
        if(description != null && description.length() > 0) {
            //int descOffset = descriptionStart - sb.length();
            int descOffset = descriptionStart - getFormattedLength() - offset;
            if(descOffset > 0)
                sb.append(String.format("%"+descOffset+"s", ""));
            else
                sb.append(" ");

            sb.append(description);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "OptionInt{" +
                "name='" + name + '\'' +
                ", longName='" + longName + '\'' +
                ", description='" + description + '\'' +
                ", hasValue=" + hasValue +
                ", values=" + values +
                ", argument='" + argument + '\'' +
                ", type=" + type +
                ", required=" + required +
                ", valueSeparator=" + valueSeparator +
                ", isProperty=" + isProperty +
                ", hasMultipleValues=" + hasMultipleValues +
                ", properties=" + properties +
                '}';
    }
}
