/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.cl.parser;

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
