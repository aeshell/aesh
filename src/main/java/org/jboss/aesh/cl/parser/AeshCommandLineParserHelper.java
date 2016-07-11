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

package org.jboss.aesh.cl.parser;

import org.jboss.aesh.cl.CommandLine;
import org.jboss.aesh.cl.internal.OptionType;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.console.command.Command;

import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandLineParserHelper {

    private static final String EQUALS = "=";
    private final ProcessedCommand processedCommand;
    private CommandLine commandLine;
    private Status status;
    private ProcessedOption active;

    public AeshCommandLineParserHelper(ProcessedCommand processedCommand) {
        this.processedCommand = processedCommand;
        this.status = Status.NULL;
    }

    /**
     * Populate commandLine.
     * Lines are the user input minus command name;
     *
     * @param commandLine
     * @param lines
     * @param ignoreRequirements
     * @return
     */
    public void parse(CommandLine commandLine,
            List<String> lines, boolean ignoreRequirements) {

        status = Status.NULL;
        ProcessedOption currOption = null;
        this.commandLine = commandLine;
        this.active = null;

        for(int i=0; i < lines.size(); i++) {
            String word = lines.get(i);

            currOption = findOption(word);
            //if currOption != null we need to check if we have an option active, if so we end that
            if(active != null && currOption != null) {
                commandLine.addOption(active);
                active = null;
                status = Status.NULL;
            }
            else if(status == Status.OPTION_FOUND) {
                commandLine.addOption(currOption);
                status = Status.NULL;
                currOption = null;
            }

            //we're looking for an option, arg value or adding value to an option list (active)
            if(status == Status.NULL && currOption != null) {
                if(currOption.isLongNameUsed())
                    processOption(currOption, word.substring(2), currOption.getName());
                else
                    processOption(currOption, word.substring(1), currOption.getShortName());

                if(currOption != null) {
                    if(!currOption.hasValue()) {
                        commandLine.addOption(currOption);
                    }
                    else {
                        status = Status.OPTION;
                        if (active != null) {
                            commandLine.addOption(active);
                            active = null;
                        }
                    }
                }
                //we have a value which must be an argument
                else if(status == Status.VALUE) {
                    //do we have an active option list?
                    if(active != null)
                        active.addValue(word);
                    else if (processedCommand.getArgument() == null)
                        commandLine.setParserException(new OptionParserException("An argument was given to a command that does not support it."));
                    else
                        commandLine.addArgumentValue(word);
                    status = Status.NULL;
                }
                else if(status == Status.OPTION_FOUND) {
                    status = Status.NULL;
                    if(active != null)
                        commandLine.addOption(active);
                }
            }
            else if(status == Status.OPTION){
                addValueToOption(currOption, word);
            }
        }

        if(active != null)
            commandLine.addOption(active);

        //verify that options have values and/or add default values
        checkForDefaultValues(commandLine);

        //this will throw and CommandLineParserException if needed
        if(!ignoreRequirements) {
            RequiredOptionException re = checkForMissingRequiredOptions(processedCommand, commandLine);
            if(re != null)
                commandLine.setParserException(re);
        }
        //return this.commandLine;
    }

    private void addValueToOption(ProcessedOption currOption, String word) {
        if(currOption.hasMultipleValues()) {
            if(word.contains(String.valueOf(currOption.getValueSeparator()))) {
                for(String value : word.split(String.valueOf(currOption.getValueSeparator()))) {
                    currOption.addValue(value.trim());
                }
                if(word.endsWith(String.valueOf(currOption.getValueSeparator())))
                    currOption.setEndsWithSeparator(true);
                commandLine.addOption(currOption);
                status = Status.NULL;
            }
            else {
                currOption.addValue(word);
                active = currOption;
            }
        }
        else {
            currOption.addValue(word);
            commandLine.addOption(currOption);
            status = Status.NULL;
        }
    }

    private void checkForDefaultValues(CommandLine<? extends Command> commandLine) {
        for(ProcessedOption option : commandLine.getOptions()) {
            if(option.hasValue() && option.getValue() == null) {
                if(option.hasDefaultValue())
                    for(String value : option.getDefaultValues())
                        option.addValue(value);
            }
            else if(option.getOptionType().equals(OptionType.BOOLEAN))
                option.addValue("true");
        }
    }

    private void checkForPossibleNewOptions() {

    }

    private RequiredOptionException checkForMissingRequiredOptions(ProcessedCommand<Command> command,
                                                                   CommandLine<? extends Command> commandLine) {
        for(ProcessedOption o : command.getOptions())
            if(o.isRequired()) {
                boolean found = false;
                for(ProcessedOption po : commandLine.getOptions()) {
                    if(po.getShortName() != null && o.getShortName() != null &&
                            po.getShortName().equals(o.getShortName()) ||
                            (po.getName() != null && po.getName().equals(o.getName()))) {
                        found = true;
                        break;
                    }
                    else if(po.doOverrideRequired()) {
                        found = true;
                        break;
                    }
                }
                if(!found)
                    return new RequiredOptionException("Option: "+o.getDisplayName()+" is required for this command.");
            }
        return null;
    }

    private ProcessedOption findOption(String word) {
        if (word.startsWith("--")) {
            ProcessedOption currentOption = findLongOption(processedCommand, word.substring(2));
            if (currentOption != null)
                currentOption.setLongNameUsed(true);

            return currentOption;
        }
        else if (word.startsWith("-")) {
            ProcessedOption currentOption = findShortOption(processedCommand, word.substring(1));
            if (currentOption != null)
                currentOption.setLongNameUsed(false);

            return currentOption;
        }
        else {
            status = Status.VALUE;
            return null;
        }
    }

    private ProcessedOption findShortOption(ProcessedCommand command, String line) {
        ProcessedOption option = command.findOption(line);
        //simplest case
        if(option != null) {
            status = Status.OPTION_FOUND;
            return option;
        }

        return command.startWithOption(line);
        //if its a property, we'll parse it later
        /*
        if(option != null) {
            status = Status.OPTION_FOUND;
            if (option.isProperty()) {
                processProperty(option, line, option.getShortName());
            }
            else {
                String rest = line.substring(option.getShortName().length());
                if (option.getOptionType().equals(OptionType.LIST)) {
                    processList(option, rest);
                    return null;
                }
                if (!rest.contains(EQUALS)) {
                    // we might have two or more options in a group
                    // if so, we only allow options (boolean) without value
                    if (rest.length() > 0) {
                        //first we add the first option
                        commandLine.addOption(option);
                        for (char shortName : rest.toCharArray()) {
                            ProcessedOption currOption = command.findOption(String.valueOf(shortName));
                            if (currOption != null) {
                                if (!currOption.hasValue()) {
                                    currOption.setLongNameUsed(false);
                                    currOption.addValue("true");
                                    commandLine.addOption(currOption);
                                }
                                else
                                    commandLine.setParserException(new OptionParserException("Option: -" + shortName +
                                            " can not be grouped with other options since it need to be given a value"));
                            }
                            else
                                commandLine.setParserException(new OptionParserException("Option: -" + shortName + " was not found."));
                        }
                    }
                    else
                        commandLine.setParserException(new OptionParserException("Option: - must be followed by a valid operator"));

                }
                //line contain equals, we need to add a value(s) to the currentOption
                else {
                    addValueToOption(option, line.substring(line.indexOf(EQUALS)+1));
                }
            }
        }

        return null;
        */
    }

    private ProcessedOption findLongOption(ProcessedCommand command, String line) {
        ProcessedOption option = command.findLongOptionNoActivatorCheck(line);
        //simplest case
        if (option != null) {
            status = Status.OPTION_FOUND;
            return option;
        }

        return command.startWithLongOption(line);
    }

    private void processOption(ProcessedOption option, String line, String name) {
        if(option != null) {
            status = Status.OPTION_FOUND;
            if (option.isProperty()) {
                processProperty(option, line, name);
            }
            else {
                String rest = line.substring(name.length());
                if (option.getOptionType().equals(OptionType.LIST)) {
                    processList(option, rest);
                }
                else if (!rest.contains(EQUALS)) {
                    // we might have two or more options in a group
                    // if so, we only allow options (boolean) without value
                    if (rest.length() > 0 && !option.isLongNameUsed()) {
                        //first we add the first option
                        commandLine.addOption(option);
                        for (char shortName : rest.toCharArray()) {
                            ProcessedOption currOption = processedCommand.findOption(String.valueOf(shortName));
                            if (currOption != null) {
                                if (!currOption.hasValue()) {
                                    currOption.setLongNameUsed(false);
                                    currOption.addValue("true");
                                    commandLine.addOption(currOption);
                                }
                                else
                                    commandLine.setParserException(new OptionParserException("Option: -" + shortName +
                                            " can not be grouped with other options since it need to be given a value"));
                            }
                            else
                                commandLine.setParserException(new OptionParserException("Option: -" + shortName + " was not found."));
                        }
                    }
                    else
                        commandLine.setParserException(new OptionParserException("Option: - must be followed by a valid operator"));
                }
                //line contain equals, we need to add a value(s) to the currentOption
                else {
                    addValueToOption(option, line.substring(line.indexOf(EQUALS)+1));
                }
            }
        }
        if(option != null) {
            status = Status.OPTION_FOUND;
            if (option.isProperty()) {
                processProperty(option, line, name);
            }
            else {
                String rest = line.substring(name.length());
                if (option.getOptionType().equals(OptionType.LIST))
                    processList(option, rest);
                else if(rest.contains(EQUALS)) {
                    addValueToOption(option, rest.substring(rest.indexOf(EQUALS)+1));
                }
            }
        }

    }

    private void processList(ProcessedOption currOption, String rest) {
        if(rest.length() > 1 && rest.startsWith("=")) {
            if ( rest.indexOf(currOption.getValueSeparator()) > -1) {
                for (String value : rest.substring(1).split(String.valueOf(currOption.getValueSeparator()))) {
                    currOption.addValue(value.trim());
                }
                if (rest.endsWith(String.valueOf(currOption.getValueSeparator())))
                    currOption.setEndsWithSeparator(true);
            }
            else
                currOption.addValue(rest.substring(1));
            commandLine.addOption(currOption);
            status = Status.NULL;
        }
    }

    private void processProperty(ProcessedOption currOption, String word, String name) {
        if (word.length() < (1 + name.length()) || !word.contains(EQUALS))
            commandLine.setParserException(new OptionParserException(
                    "Option " + currOption.getDisplayName() + ", must be part of a property"));
        else {
            String propertyName = word.substring(name.length(), word.indexOf(EQUALS));
            String value = word.substring(word.indexOf(EQUALS) + 1);
            if (value.length() < 1)
                commandLine.setParserException(new OptionParserException("Option " + currOption.getDisplayName() + ", must have a value"));
            else {
                currOption.addProperty(propertyName, value);
                commandLine.addOption(currOption);
            }
        }
        status = Status.NULL;
    }


    enum Status {
        NULL, OPTION, OPTION_FOUND, VALUE;
    }
}
