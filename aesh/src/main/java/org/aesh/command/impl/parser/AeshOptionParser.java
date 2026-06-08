/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.parser.OptionParser;
import org.aesh.command.parser.OptionParserException;
import org.aesh.parser.ParsedLineIterator;
import org.aesh.terminal.utils.Parser;

/**
 * @author Aesh team
 */
public class AeshOptionParser implements OptionParser {

    private static final Logger LOGGER = Logger.getLogger(AeshOptionParser.class.getName());
    private static final String EQUALS = "=";
    private static final char DASH = '-';
    private Status status;

    @Override
    public void parse(ParsedLineIterator parsedLineIterator, ProcessedOption option) throws OptionParserException {
        if (option.isProperty()) {
            processProperty(parsedLineIterator, option);
        } else {
            preProcessOption(option, parsedLineIterator);
            // When fallbackValue is set, the option only accepts values via = syntax.
            // A bare --option should NOT consume the next word — it uses the fallback.
            if (option.hasFallbackValue() && option.getValue() == null) {
                applyOptionalFallback(option);
                return;
            }
            while (status != Status.NULL && parsedLineIterator.hasNextWord()) {
                String word = parsedLineIterator.peekWord();
                ProcessedOption nextOption = option.parent().searchAllOptions(word);
                if (nextOption == null) {
                    doParse(parsedLineIterator, option);
                    if (status == null && !option.hasValue()) {
                        //this might happen if we have an option at the "end" that doesn't accept values
                        return;
                    }
                }
                //we have something like: --foo --bar eg, two options after another
                else {
                    if (option.hasValue() && option.getValue() == null && !option.isOptionalValue()) {
                        throw new OptionParserException("Option " + option.name() + " was specified, but no value was given.");
                    }
                    applyOptionalFallback(option);
                    return;
                }
            }
            if (option.hasValue() && option.getValue() == null && !option.isOptionalValue())
                throw new OptionParserException("Option " + option.name() + " was specified, but no value was given.");
            applyOptionalFallback(option);
        }
    }

    private void doParse(ParsedLineIterator iterator, ProcessedOption option) throws OptionParserException {
        if (status == Status.ACTIVE)
            addValueToOption(option, iterator);
        else if (status == Status.NULL)
            preProcessOption(option, iterator);
    }

    private void preProcessOption(ProcessedOption option, ParsedLineIterator iterator) throws OptionParserException {

        String word = iterator.peekWord();
        if (word.indexOf(" ") < word.indexOf("="))
            word = Parser.switchSpacesToEscapedSpacesInWord(word);
        if (option.isLongNameUsed()) {
            String optionPart = (word.length() > 1 && word.charAt(0) == DASH && word.charAt(1) == DASH)
                    ? word.substring(2)
                    : word;
            // Determine which name was actually used: primary, alias, or negated
            String nameToMatch;
            if (option.isNegatedByUser() && option.getNegatedName() != null) {
                nameToMatch = option.getNegatedName();
            } else {
                nameToMatch = resolveMatchedName(option, optionPart);
            }
            if (optionPart.length() != nameToMatch.length())
                processOption(option, optionPart, nameToMatch);
            else if (option.getOptionType() == OptionType.BOOLEAN) {
                // For negatable options, use "false" if specified in negated form
                option.addValue(option.isNegatedByUser() ? "false" : "true");
                status = Status.NULL;
            } else
                status = Status.OPTION_FOUND;

        } else {
            if (word.length() > 2)
                processOption(option, word.substring(1), option.shortName());
            else if (option.getOptionType() == OptionType.BOOLEAN) {
                option.addValue("true");
                //commandLine.addOption(option);
                status = Status.NULL;
            } else
                status = Status.OPTION_FOUND;
        }

        if (status == Status.OPTION_FOUND) {
            if (option.hasValue()) {
                //active = option;
                status = Status.ACTIVE;
            } else {
                //commandLine.addOption(option);
                status = Status.NULL;
            }
        }
        if (iterator.isNextWordCursorWord())
            option.setCursorOption(true);
        //we've parsed the current word, lets pop it
        iterator.pollParsedWord();
    }

    private void processOption(ProcessedOption option, String line, String name) throws OptionParserException {
        String rest = line.substring(name.length());
        if (option.getOptionType().equals(OptionType.LIST)) {
            processList(option, rest);
        } else if (!rest.contains(EQUALS)) {
            // Either we have two or more boolean options in a group
            // or the value is appended without the EQUALS
            if (rest.length() > 0 && !option.isLongNameUsed()) {
                option.setLongNameUsed(false);
                if (option.hasValue()) {
                    doAddValueToOption(option, rest);
                    return;
                } else {
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
                        } else
                            throw new OptionParserException("Option: -" + shortName +
                                    " can not be grouped with other options since it need to be given a value");
                    } else
                        throw new OptionParserException("Option: -" + shortName + " was not found.");
                }
            } else
                throw new OptionParserException("Option: - must be followed by a valid operator");
        }
        //line contain equals, we need to add a value(s) to the currentOption
        else {
            doAddValueToOption(option, line.substring(line.indexOf(EQUALS) + 1));
        }
    }

    private void addValueToOption(ProcessedOption currOption, ParsedLineIterator iterator) {
        //we know that the next word is a value and if its the cursor word, we set it...
        if (iterator.isNextWordCursorWord())
            currOption.setCursorValue(true);
        //we know that the option will accept a value, so we can poll the value
        doAddValueToOption(currOption, iterator.pollWord());
        //lets try to parse the rest of the optionList if there are more
        while (status != Status.NULL &&
                iterator.hasNextWord() && iterator.peekWord().charAt(0) == currOption.getValueSeparator()) {
            doAddValueToOption(currOption, iterator.pollWord());
        }
        if (currOption.getValueSeparator() != ' ')
            status = Status.NULL;
    }

    private void doAddValueToOption(ProcessedOption currOption, String word) {
        if (currOption.hasMultipleValues()) {
            if (word.contains(String.valueOf(currOption.getValueSeparator()))) {
                for (String value : word.split(Pattern.quote(String.valueOf(currOption.getValueSeparator())))) {
                    currOption.addValue(value.trim());
                }
                if (word.endsWith(String.valueOf(currOption.getValueSeparator())))
                    currOption.setEndsWithSeparator(true);
                //commandLine.addOption(currOption);
                status = Status.NULL;
            } else {
                currOption.addValue(word);
                //active = currOption;
            }
        } else {
            currOption.addValue(word);
            //commandLine.addOption(currOption);
            status = Status.NULL;
        }
    }

    private void processList(ProcessedOption currOption, String rest) {
        if (rest.length() > 1 && rest.startsWith("=")) {
            if (rest.indexOf(currOption.getValueSeparator()) > -1) {
                for (String value : rest.substring(1).split(Pattern.quote(String.valueOf(currOption.getValueSeparator())))) {
                    currOption.addValue(value.trim());
                }
                if (rest.endsWith(String.valueOf(currOption.getValueSeparator())))
                    currOption.setEndsWithSeparator(true);
            } else
                currOption.addValue(rest.substring(1));
            //commandLine.addOption(currOption);
            status = Status.NULL;
        }
    }

    private void processProperty(ParsedLineIterator iterator, ProcessedOption currOption) throws OptionParserException {
        String word = currOption.isLongNameUsed() ? iterator.pollWord().substring(2) : iterator.pollWord().substring(1);
        String name = currOption.isLongNameUsed() ? currOption.name() : currOption.shortName();
        if (word.length() < (1 + name.length()))
            throw new OptionParserException("Option " + currOption.getDisplayName() + ", must be part of a property");

        // Skip separator '=' between option name and property key (#496).
        // Supports both --manifestFoo=Bar (aesh) and --manifest=Foo=Bar (picocli) syntaxes.
        int afterName = name.length();
        if (afterName < word.length() && word.charAt(afterName) == '=') {
            afterName++;
        }
        String rest = word.substring(afterName);

        if (!rest.contains(EQUALS)) {
            if (rest.isEmpty()) {
                throw new OptionParserException(
                        "Option " + currOption.getDisplayName() + ", must be part of a property");
            }
            if (currOption.hasDefaultValue()) {
                currOption.addProperty(rest, currOption.getDefaultValues().get(0));
            } else {
                throw new OptionParserException(
                        "Option " + currOption.getDisplayName() + ", must be part of a property");
            }
        } else {
            String propertyName = rest.substring(0, rest.indexOf(EQUALS));
            String value = rest.substring(rest.indexOf(EQUALS) + 1);
            if (value.isEmpty() && currOption.hasDefaultValue())
                currOption.addProperty(propertyName, currOption.getDefaultValues().get(0));
            else {
                currOption.addProperty(propertyName, value);
            }
        }
        status = Status.NULL;
    }

    private static String resolveMatchedName(ProcessedOption option, String input) {
        // Check if input matches or starts with an alias
        for (String alias : option.getAliases()) {
            if (input.equals(alias) || input.startsWith(alias + "="))
                return alias;
        }
        return option.name();
    }

    /**
     * Apply the fallback value when an optionalValue option is specified bare.
     * Resolution order (#507):
     * 1. DefaultValueProvider.fallbackValue() (dynamic, from config/env)
     * 2. Annotation fallbackValue (static)
     * 3. Annotation defaultValue (static, legacy fallback)
     */
    private static void applyOptionalFallback(ProcessedOption option) {
        if (!option.isOptionalValue() || option.getValue() != null)
            return;

        // Priority 1: Provider fallback (dynamic)
        org.aesh.command.DefaultValueProvider dvp = option.parent() != null
                ? option.parent().getDefaultValueProvider()
                : null;
        if (dvp != null) {
            try {
                String providerFallback = dvp.fallbackValue(option);
                if (providerFallback != null) {
                    option.addValue(providerFallback);
                    return;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "DefaultValueProvider.fallbackValue() failed for --" + option.name(), e);
            }
        }

        // Priority 2: Annotation fallbackValue (static)
        if (option.hasFallbackValue()) {
            option.addValue(option.getFallbackValue());
        } else if (option.hasDefaultValue()) {
            // Priority 3: Annotation defaultValue (legacy fallback for optionalValue)
            option.addValue(option.getDefaultValues().get(0));
        }
    }

    private enum Status {
        NULL,
        OPTION_FOUND,
        ACTIVE;
    }
}
