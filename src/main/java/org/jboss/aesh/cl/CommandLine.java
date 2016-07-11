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
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.parser.OptionParserException;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.console.command.Command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A parsed String based on the provided CommandDefinition and Options defined
 * in a {@link org.jboss.aesh.cl.parser.AeshCommandLineParser}.
 *
 * All found options and argument can be queried after.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLine<T extends Command> {

    private final List<ProcessedOption> options;
    private ProcessedOption argument;
    private boolean parserError;
    private CommandLineParserException parserException;
    private CommandLineParser<T> parser;

    private CommandLine() {
        options = new ArrayList<>();
    }

    public CommandLine(CommandLineParser<T> parser) {
        this();
        this.parser = parser;
    }

    public CommandLine(CommandLineParserException parserException) {
        this();
        if(parserException != null)
            setParserException(parserException);
    }

    public void addOption(ProcessedOption option) {
        ProcessedOption existingOption = getOption(option.getName());
        if (existingOption == null) {
            options.add(option);
        }
        else {
            if((existingOption.getProperties() == null ||
                    existingOption.getProperties().size() == 0) ||
            (option.getProperties() == null || existingOption.getProperties().size() == 0)) {
                setParserError(true);
                setParserException( new OptionParserException("Not allowed to specify the same option ("+option.getDisplayName()+") twice"));
            }
            else
                existingOption.getProperties().putAll(option.getProperties());
        }

    }

    public CommandLineParser<T> getParser() {
        return parser;
    }

    public List<ProcessedOption> getOptions() {
        return options;
    }

    public void addArgumentValue(String arg) {
        if(argument == null)
            argument = parser.getProcessedCommand().getArgument();
        argument.addValue(arg);
    }

    public void setArgument(ProcessedOption argument) {
        this.argument = argument;
    }

    public ProcessedOption getArgument() {
        return argument;
    }

    public boolean hasOption(char name) {
       return hasOption(String.valueOf(name));
    }

    public ProcessedOption getOption(String name) {
        for(ProcessedOption po : options) {
            if((po.getShortName() != null && po.getShortName().equals(name)) ||
                    (po.getName() != null && po.getName().equals(name)))
                return po;
        }
        return null;
    }

    public boolean hasOption(String name) {
        for(ProcessedOption po : options) {
            if((po.getShortName() != null && po.getShortName().equals(name)) ||
                    po.getName().equals(name))
                return true;
        }
        return false;
    }

    public String getOptionValue(char c) {
        return getOptionValue(String.valueOf(c));
    }

    public String getOptionValue(String name) {
        return getOptionValue(name, null);
    }

    public String getOptionValue(String name, String fallback) {
        for(ProcessedOption po : options) {
            if((po.getShortName() != null && po.getShortName().equals(name)) ||
                    (po.getName() != null && po.getName().equals(name)))
                return po.getValue();
        }
        return fallback;
    }

    public List<String> getOptionValues(char c) {
        return getOptionValues(String.valueOf(c), new ArrayList<String>());
    }

    public List<String> getOptionValues(String name) {
        return getOptionValues(name, new ArrayList<String>());
    }

    public List<String> getOptionValues(String name, List<String> fallback) {
        for(ProcessedOption po : options) {
            if((po.getShortName() != null && po.getShortName().equals(name)) ||
                    (po.getName() != null && po.getName().equals(name)))
                return po.getValues();
        }

        return fallback;
    }

    public Map<String,String> getOptionProperties(String name) {
        for(ProcessedOption po : options) {
            if((po.getShortName() != null && po.getShortName().equals(name)) ||
                    (po.getName() != null && po.getName().equals(name)))
                return po.getProperties();
        }

        return new HashMap<>();
    }

    public boolean hasParserError() {
        return parserError;
    }

    public void setParserError(boolean error) {
        this.parserError = error;
    }

    public CommandLineParserException getParserException() {
        return parserException;
    }

    public void setParserException(CommandLineParserException e) {
        this.parserException = e;
        if(parserException != null)
            this.parserError = true;
    }

    public boolean hasOptionWithOverrideRequired() {
        for(ProcessedOption option : options) {
            if(option.doOverrideRequired())
                return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "CommandLine{" +
                "options=" + options +
                ", argument=" + argument +
                ", parserError=" + parserError +
                ", parserException=" + parserException +
                '}';
    }
}

