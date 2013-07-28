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

    private String shortName;
    private String name;

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

    public OptionInt(char shortName, String name, String description, boolean hasValue,
                     String argument, boolean required, char valueSeparator,
                     boolean isProperty, boolean hasMultipleValues, String defaultValue, Class<?> type) throws OptionParserException {
        this.shortName = String.valueOf(shortName);
        this.name = name;
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

        if((shortName == Character.MIN_VALUE) && name.equals("")) {
            throw new OptionParserException("Either shortName or long shortName must be set.");
        }
    }

    public String getShortName() {
        return shortName;
    }

    public String getName() {
        return name;
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
        if(name != null) {
            return "--"+ name;
        }
        else
            return "-"+ shortName;
    }

    public int getFormattedLength() {
        StringBuilder sb = new StringBuilder();
        if(shortName != null)
            sb.append("-").append(shortName);
        if(name != null) {
            if(sb.toString().trim().length() > 0)
                sb.append(", ");
            sb.append("--").append(name);
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
        if(shortName != null)
            sb.append("-").append(shortName);
        if(name != null) {
            if(sb.toString().trim().length() > 0)
                sb.append(", ");
            sb.append("--").append(name);
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
                "shortName='" + shortName + '\'' +
                ", name='" + name + '\'' +
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
