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
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.parser.OptionParser;
import org.aesh.command.parser.OptionParserException;
import org.aesh.parser.ParsedLineIterator;
import org.aesh.readline.util.Parser;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshOptionParser implements OptionParser {

    private static final String EQUALS = "=";
    private Status status;

    @Override
    public void parse(ParsedLineIterator parsedLineIterator, ProcessedOption option) throws OptionParserException {
        if(option.isProperty()) {
            processProperty(parsedLineIterator, option);
        }
        else {
            preProcessOption(option, parsedLineIterator);
            while(status != Status.NULL && parsedLineIterator.hasNextWord()) {
                String word = parsedLineIterator.peekWord();
                ProcessedOption nextOption = option.parent().searchAllOptions(word);
                if(nextOption == null)
                    doParse(parsedLineIterator, option);
                //we have something like: --foo --bar eg, two options after another
                else {
                    //TODO: we need to do something better here
                    if(option.hasValue() && option.getValue() == null) {
                        throw new OptionParserException("Option "+option.name()+" was specified, but no value was given.");
                    }
                    return;
                }
            }
            if(option.hasValue() && (option.getValue() == null || option.getValue().length() == 0))
                throw new OptionParserException("Option "+option.name()+" was specified, but no value was given.");
        }
    }

    private void doParse(ParsedLineIterator iterator, ProcessedOption option) throws OptionParserException {
            if(status == Status.ACTIVE)
                addValueToOption(option, iterator);
            else if(status == Status.NULL)
                preProcessOption(option, iterator);
    }

    private void preProcessOption(ProcessedOption option, ParsedLineIterator iterator) throws OptionParserException {

        String word = iterator.peekWord();
        if(word.indexOf(" ") < word.indexOf("="))
            word = Parser.switchSpacesToEscapedSpacesInWord(word);
        if(option.isLongNameUsed()) {
            if(word.length()-2 != option.name().length())
                processOption(option, word.substring(2), option.name());
            else if(option.getOptionType() == OptionType.BOOLEAN) {
                option.addValue("true");
                status = Status.NULL;
            }
            else
                status = Status.OPTION_FOUND;

        }
        else {
            if(word.length() > 2)
                processOption(option, word.substring(1), option.shortName());
            else if(option.getOptionType() == OptionType.BOOLEAN) {
                option.addValue("true");
                //commandLine.addOption(option);
                status = Status.NULL;
            }
             else
                status = Status.OPTION_FOUND;
        }

        if(status == Status.OPTION_FOUND) {
            if(option.hasValue()) {
                //active = option;
                status = Status.ACTIVE;
            }
            else {
                //commandLine.addOption(option);
                status = Status.NULL;
            }
        }
        if(iterator.isNextWordCursorWord())
            option.setCursorOption(true);
        //we've parsed the current word, lets pop it
        iterator.pollParsedWord();
    }

    private void processOption(ProcessedOption option, String line, String name) throws OptionParserException {
        String rest = line.substring(name.length());
        if (option.getOptionType().equals(OptionType.LIST)) {
            processList(option, rest);
        }
        else if (!rest.contains(EQUALS)) {
            // Either we have two or more boolean options in a group
            // or the value is appended without the EQUALS
            if (rest.length() > 0 && !option.isLongNameUsed()) {
                option.setLongNameUsed(false);
                if (option.hasValue()) {
                    doAddValueToOption(option, rest);
                    return;
                } else  {
                    // we add the first option
                    option.addValue("true");
                }

                for (char shortName : rest.toCharArray()) {
                    ProcessedOption currOption = option.parent().findOption(String.valueOf(shortName));
                    if (currOption != null) {
                        if (!currOption.hasValue()) {
                            currOption.setLongNameUsed(false);
                            currOption.addValue("true");
                            //commandLine.addOption(currOption);
                        }
                        else
                            throw new OptionParserException("Option: -"+shortName+
                                    " can not be grouped with other options since it need to be given a value");
                    }
                    else
                        throw new OptionParserException("Option: -" + shortName + " was not found.");
                }
            }
            else
                throw new OptionParserException("Option: - must be followed by a valid operator");
        }
        //line contain equals, we need to add a value(s) to the currentOption
        else {
            doAddValueToOption(option, line.substring(line.indexOf(EQUALS)+1));
        }
    }

    private void addValueToOption(ProcessedOption currOption, ParsedLineIterator iterator) {
        //we know that the next word is a value and if its the cursor word, we set it...
        if(iterator.isNextWordCursorWord())
            currOption.setCursorValue(true);
        //we know that the option will accept a value, so we can poll the value
        String word = iterator.pollWord();
        doAddValueToOption(currOption, word);
    }

    private void doAddValueToOption(ProcessedOption currOption, String word) {
        if(currOption.hasMultipleValues()) {
            if(word.contains(String.valueOf(currOption.getValueSeparator()))) {
                for(String value : word.split(String.valueOf(currOption.getValueSeparator()))) {
                    currOption.addValue(value.trim());
                }
                if(word.endsWith(String.valueOf(currOption.getValueSeparator())))
                    currOption.setEndsWithSeparator(true);
                //commandLine.addOption(currOption);
                status = Status.NULL;
            }
            else {
                currOption.addValue(word);
                //active = currOption;
            }
        }
        else {
            currOption.addValue(word);
            //commandLine.addOption(currOption);
            status = Status.NULL;
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
            //commandLine.addOption(currOption);
            status = Status.NULL;
        }
    }

    private void processProperty(ParsedLineIterator iterator, ProcessedOption currOption) throws OptionParserException {
        String word = currOption.isLongNameUsed() ? iterator.pollWord().substring(2) : iterator.pollWord().substring(1);
        String name = currOption.isLongNameUsed() ? currOption.name() : currOption.shortName();
        if (word.length() < (1 + name.length()) || !word.contains(EQUALS))
            throw new OptionParserException("Option "+currOption.getDisplayName()+", must be part of a property");
        else {
            String propertyName =
                    word.substring(name.length(), word.indexOf(EQUALS));
            String value = word.substring(word.indexOf(EQUALS) + 1);
            if (value.length() < 1)
                throw new OptionParserException("Option " + currOption.getDisplayName() + ", must have a value");
            else {
                currOption.addProperty(propertyName, value);
                //commandLine.addOption(currOption);
            }
        }
        status = Status.NULL;
    }

    private enum Status {
        NULL, OPTION_FOUND, ACTIVE;
    }
}
