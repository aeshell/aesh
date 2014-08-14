/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.parser;

/**
 * A value object designed to show on which option a complete operation
 * is performed on.
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParsedCompleteObject {

    private String name;
    private String value = "";
    private Class<?> type;
    private final boolean option; //if its not option, its an argument
    private boolean displayOptions = false;
    private boolean argument = false;
    private final boolean displayArguments = false;
    private boolean completeOptionName = false;
    private int offset = 0;
    private CommandLineCompletionParser completionParser;

    public ParsedCompleteObject(boolean displayArguments, CommandLineCompletionParser completionParser) {
        this.option = !displayArguments;
        this.argument = !option;
        this.completionParser = completionParser;
    }

    public ParsedCompleteObject(boolean displayOptions, String name, int offset, CommandLineCompletionParser completionParser) {
        this.displayOptions = displayOptions;
        this.offset = offset;
        this.name = name;
        this.value = "";
        this.type = null;
        this.option = false;
        this.completionParser = completionParser;
    }

    public ParsedCompleteObject(boolean displayOptions, String name, int offset, boolean completeOptionName,
                                CommandLineCompletionParser completionParser) {
        this(displayOptions, name, offset, completionParser);
        this.completeOptionName = completeOptionName;
    }

    public ParsedCompleteObject(String name, String value,
                                Class<?> type, boolean option, boolean completeOptionName,
                                CommandLineCompletionParser completionParser) {
        this(name, value, type, option, completionParser);
        this.completeOptionName = completeOptionName;
    }

    public ParsedCompleteObject(String name, String value,
                                Class<?> type, boolean option, CommandLineCompletionParser completionParser) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.option = option;
        this.argument = !this.option;
        this.offset = value.length();
        this.completionParser = completionParser;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public Class<?> getType() {
        return type;
    }

    public boolean isOption() {
        return option;
    }

    public boolean isArgument() {
        return argument;
    }

    public int getOffset() {
        return offset;
    }

    public boolean doDisplayOptions() {
        return displayOptions;
    }

    public boolean isCompleteOptionName() {
        return completeOptionName;
    }

    public CommandLineCompletionParser getCompletionParser() {
        return completionParser;
    }

    @Override
    public String toString() {
        return "ParsedCompleteObject{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", type=" + type +
                ", option=" + option +
                ", argument=" + argument +
                ", displayOptions=" + displayOptions +
                ", displayArguments=" + displayArguments +
                ", completeOptionName=" + completeOptionName +
                ", offset=" + offset +
                '}';
    }
}
