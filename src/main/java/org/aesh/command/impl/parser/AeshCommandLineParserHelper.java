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

package org.aesh.command.impl.parser;

import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.Command;

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
     * @param commandLine command
     * @param lines input
     * @param ignoreRequirements should we ignore requirements
     */
    public void parse(CommandLine commandLine,
            List<String> lines, boolean ignoreRequirements) {

        status = Status.NULL;
        this.commandLine = commandLine;
        this.active = null;

        for (String word : lines) {
            ProcessedOption currOption = findOption(word);
            if (status == Status.ACTIVE) {
                if (currOption == null) {
                    //add value to active
                    addValueToOption(active, word);
                    //if addValueToOption have added the option to commandline
                    //we need to set active = null
                    if (status == Status.NULL)
                        active = null;
                } else {
                    //first add the current active option to commandLine
                    //the process the option
                    commandLine.addOption(active);
                    active = null;
                    status = Status.NULL;

                    preProcessOption(currOption, word);
                }
            } else if (status == Status.NULL) {
                if (currOption == null) {
                    //add the current word as argument

                    commandLine.addArgumentValue(word);
                } else {
                    preProcessOption(currOption, word);
                }
            }
        }

        //remember to add the last option at the end of the parse
        if(active != null)
            commandLine.addOption(active);
        //verify that options have values and/or add default values
        if(!ignoreRequirements)
            checkForDefaultValues(commandLine);
        //this will throw and CommandLineParserException if needed
        if(!ignoreRequirements) {
            RequiredOptionException re = checkForMissingRequiredOptions(processedCommand, commandLine);
            if(re != null)
                commandLine.setParserException(re);
        }
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
            return null;
        }
    }

    private ProcessedOption findShortOption(ProcessedCommand command, String line) {
        ProcessedOption option = command.findOption(line);
        //simplest case
        if(option != null) {
            return option;
        }

        return command.startWithOption(line);
    }

    private ProcessedOption findLongOption(ProcessedCommand command, String line) {
        ProcessedOption option = command.findLongOptionNoActivatorCheck(line);
        //simplest case
        if (option != null) {
            return option;
        }

        return command.startWithLongOption(line);
    }

    private void preProcessOption(ProcessedOption option, String line) {
        if(option.isLongNameUsed()) {
            if(line.length()-2 != option.getName().length())
                processOption(option, line.substring(2), option.getName());
            else if(option.getOptionType() == OptionType.BOOLEAN) {
                option.addValue("true");
                commandLine.addOption(option);
                status = Status.NULL;
            }
            else
                status = Status.OPTION_FOUND;

        }
        else {
            if(line.length() > 2)
                processOption(option, line.substring(1), option.getShortName());
            else if(option.getOptionType() == OptionType.BOOLEAN) {
                option.addValue("true");
                commandLine.addOption(option);
                status = Status.NULL;
            }
             else
                status = Status.OPTION_FOUND;
        }

        if(status == Status.OPTION_FOUND) {
            if(option.hasValue()) {
                active = option;
                status = Status.ACTIVE;
            }
            else {
                commandLine.addOption(option);
                status = Status.NULL;
            }
        }
    }

    private void processOption(ProcessedOption option, String line, String name) {
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

    private enum Status {
        NULL, OPTION_FOUND, ACTIVE;
    }
}
