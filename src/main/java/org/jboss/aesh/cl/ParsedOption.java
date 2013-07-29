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

    private String shortName;
    private String name;
    private Class<?> type;
    private List<String> values;
    private List<OptionProperty> properties;

    public ParsedOption(String shortName, String name, List<String> values, Class<?> type) {
        this.shortName = shortName;
        this.name = name;
        this.type = type;
        this.values = new ArrayList<String>();
        this.values.addAll(values);
    }

    public ParsedOption(String shortName, String name, OptionProperty property, Class<?> type) {
        this.shortName = shortName;
        this.name = name;
        this.type = type;
        values = new ArrayList<String>();
        properties = new ArrayList<OptionProperty>();
        properties.add(property);
    }

    public ParsedOption(String shortName, String name, String value, Class<?> type) {
        this.shortName = shortName;
        this.name = name;
        this.type = type;
        values = new ArrayList<String>();
        if(type != null && (Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)))
            values.add("true");
        else
            values.add(value);
    }

    public String getShortName() {
        return shortName;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        if(name != null)
            return "--"+ name;
        else
            return "-"+ shortName;
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
