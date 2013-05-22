/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParsedOption {

    private String name;
    private String longName;
    private Class<?> type;
    private List<String> values;
    private List<OptionProperty> properties;

    public ParsedOption(String name, String longName, List<String> values, Class<?> type) {
        this.name = name;
        this.longName = longName;
        this.type = type;
        this.values = new ArrayList<String>();
        this.values.addAll(values);
    }

    public ParsedOption(String name, String longName, OptionProperty property, Class<?> type) {
        this.name = name;
        this.longName = longName;
        this.type = type;
        values = new ArrayList<String>();
        properties = new ArrayList<OptionProperty>();
        properties.add(property);
    }

    public ParsedOption(String name, String longName, String value, Class<?> type) {
        this.name = name;
        this.longName = longName;
        this.type = type;
        values = new ArrayList<String>();
        values.add(value);
    }

    public String getName() {
        return name;
    }

    public String getLongName() {
        return longName;
    }

    public String getDisplayName() {
        if(longName != null)
            return "--"+longName;
        else
            return "-"+name;
    }

    public Class<?> getType() {
        return type;
    }

    public String getValue() {
        if(values.isEmpty())
            return null;
        else
            return values.get(0);
    }

    public List<String> getValues() {
        return values;
    }

    public List<OptionProperty> getProperties() {
        return properties;
    }
}
