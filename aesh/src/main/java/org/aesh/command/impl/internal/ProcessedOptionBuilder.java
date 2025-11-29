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
package org.aesh.command.impl.internal;

import org.aesh.command.impl.activator.NullActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.impl.completer.BooleanOptionCompleter;
import org.aesh.command.impl.completer.FileOptionCompleter;
import org.aesh.command.impl.completer.NullOptionCompleter;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.converter.Converter;
import org.aesh.converter.CLConverterManager;
import org.aesh.command.impl.converter.NullConverter;
import org.aesh.command.impl.parser.AeshOptionParser;
import org.aesh.command.parser.OptionParserException;
import org.aesh.command.impl.renderer.NullOptionRenderer;
import org.aesh.command.parser.OptionParser;
import org.aesh.command.renderer.OptionRenderer;
import org.aesh.command.impl.validator.NullValidator;
import org.aesh.command.validator.OptionValidator;
import org.aesh.io.Resource;
import org.aesh.selector.SelectorType;
import org.aesh.util.ReflectionUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Build a {@link ProcessedOption} object using the Builder pattern.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ProcessedOptionBuilder {

    private char shortName;
    private String name;
    private String description;
    private String argument;
    private Class<?> type;
    private boolean hasValue = true;
    private boolean required = false;
    private boolean isProperty = false;
    private boolean hasMultipleValues = false;
    private char valueSeparator = ' ';
    private OptionType optionType;
    private Converter converter;
    private String fieldName;
    private OptionCompleter completer;
    private final List<String> defaultValues;
    private OptionValidator validator;
    private OptionActivator activator;
    private OptionRenderer renderer;
    private boolean overrideRequired;
    private OptionParser parser;
    private boolean askIfNotSet = false;
    private SelectorType selectorType;

    private ProcessedOptionBuilder() {
        defaultValues = new ArrayList<>();
    }

    public static ProcessedOptionBuilder builder() {
        return new ProcessedOptionBuilder();
    }

    private ProcessedOptionBuilder apply(Consumer<ProcessedOptionBuilder> consumer) {
        consumer.accept(this);
        return this;
    }

    /**
     * The short option Name
     */
    public ProcessedOptionBuilder shortName(char n) {
        return apply(c -> c.shortName = n);
    }

    /**
     * The name of the option param.
     * The first letter will be used as the short name.
     * If name is not defined, the variable name will be used.
     */
    public ProcessedOptionBuilder name(String name) {
        return apply(c -> c.name = name);
    }

    /**
     * A description of the param.
     * This text will be printed out as part of a usage info.
     */
    public ProcessedOptionBuilder description(String description) {
        return apply(c -> c.description = description);
    }

    /**
     * A description on what kind of value is used for this option.
     */
    public ProcessedOptionBuilder argument(String argument) {
        return apply(c -> c.argument = argument);
    }

    /**
     * Define the Class type of this Option.
     * If this option is a multiple value option this Class must
     * be defined equal to the parameterized class type.
     * Note that the
     *
     * If its a property option the
     * @param type class
     * @return this
     */
    public ProcessedOptionBuilder type(Class<?> type) {
        return apply(c -> c.type = type);
    }

    /**
     * Specify if this option is required
     */
    public ProcessedOptionBuilder required(boolean required) {
        return apply(c -> c.required = required);
    }

    public ProcessedOptionBuilder askIfNotSet(boolean askIfNotSet) {
        return apply(c -> c.askIfNotSet = askIfNotSet);
    }

    public ProcessedOptionBuilder selector(SelectorType selectorType) {
        return apply(c -> c.selectorType = selectorType);
    }

    public ProcessedOptionBuilder fieldName(String fieldName) {
        return apply(c -> c.fieldName = fieldName);
    }

    public ProcessedOptionBuilder hasValue(boolean hasValue) {
        return apply(c -> c.hasValue = hasValue);
    }

    public ProcessedOptionBuilder isProperty(boolean isProperty) {
        return apply(c -> c.isProperty = isProperty);
    }

    public ProcessedOptionBuilder hasMultipleValues(boolean multipleValues) {
        return apply(c -> c.hasMultipleValues = multipleValues);
    }

    public ProcessedOptionBuilder addDefaultValue(String defaultValue) {
        return apply(c -> c.defaultValues.add(defaultValue));
    }

    public ProcessedOptionBuilder addAllDefaultValues(List<String> defaultValues) {
        return apply(c -> c.defaultValues.addAll(defaultValues));
    }

    public ProcessedOptionBuilder addAllDefaultValues(String[] defaultValues) {
        return apply(c -> c.defaultValues.addAll(Arrays.asList(defaultValues)));
    }

    public ProcessedOptionBuilder valueSeparator(char valueSeparator) {
        return apply(c -> c.valueSeparator = valueSeparator);
    }

    public ProcessedOptionBuilder optionType(OptionType optionType) {
        return apply(c -> c.optionType = optionType);
    }

    public ProcessedOptionBuilder converter(Converter converter) {
        return apply(c -> c.converter = converter);
    }

    public ProcessedOptionBuilder converter(Class<? extends Converter> converter) {
        return apply(c -> c.converter = initConverter(converter));
    }

    private Converter initConverter(Class<? extends Converter> converterClass) {
        if(converterClass != null && !converterClass.equals(NullConverter.class)) {
            if( CLConverterManager.getInstance().hasConverter(converterClass))
                return CLConverterManager.getInstance().getConverter(converterClass);
            else
                return ReflectionUtil.newInstance(converterClass);
        }
        else
            return CLConverterManager.getInstance().getConverter(type);
    }

    public ProcessedOptionBuilder completer(OptionCompleter completer) {
        return apply(c -> c.completer = completer);
    }

    public ProcessedOptionBuilder completer(Class<? extends OptionCompleter> completer) {
        return apply(c -> c.completer = initCompleter(completer));
    }

    private OptionCompleter initCompleter(Class<? extends OptionCompleter> completerClass) {

        if(completerClass != null && !completerClass.equals(NullOptionCompleter.class)) {
                return ReflectionUtil.newInstance(completerClass);
        }
        else {
            try {
                if(type == Boolean.class || type == boolean.class)
                    return BooleanOptionCompleter.class.newInstance();
                else if(type == File.class || type == Resource.class)
                    return FileOptionCompleter.class.newInstance();
                else
                    return null;

            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public ProcessedOptionBuilder validator(OptionValidator validator) {
        return apply(c -> c.validator = validator);
    }

    public ProcessedOptionBuilder validator(Class<? extends OptionValidator> validator) {
        return apply(c -> c.validator = initValidator(validator));
    }

    private OptionValidator initValidator(Class<? extends OptionValidator> validator) {
        if(validator != null && validator != NullValidator.class)
            return ReflectionUtil.newInstance(validator);
        else
            return new NullValidator();
    }

    public ProcessedOptionBuilder activator(OptionActivator activator) {
        return apply(c -> c.activator = activator);
    }

    public ProcessedOptionBuilder activator(Class<? extends OptionActivator> activator) {
        return apply(c -> c.activator = initActivator(activator));
    }

    private OptionActivator initActivator(Class<? extends OptionActivator> activator) {
        if(activator != null && activator != NullActivator.class)
            return ReflectionUtil.newInstance(activator);
        else
            return new NullActivator();
    }

    public ProcessedOptionBuilder renderer(OptionRenderer renderer) {
        return apply(c -> c.renderer = renderer);
    }

    public ProcessedOptionBuilder renderer(Class<? extends OptionRenderer> renderer) {
        return apply(c -> c.renderer = initRenderer(renderer));
    }

    private OptionRenderer initRenderer(Class<? extends OptionRenderer> renderer) {
        if(renderer != null && renderer != NullOptionRenderer.class)
            return ReflectionUtil.newInstance(renderer);
        else
            return null;
    }

    public ProcessedOptionBuilder overrideRequired(boolean overrideRequired) {
        return apply(c -> c.overrideRequired = overrideRequired);
    }

    public ProcessedOptionBuilder parser(OptionParser parser) {
        return apply(c -> c.parser = parser);
    }

    public ProcessedOptionBuilder parser(Class<? extends OptionParser> parser) {
        return apply(c -> c.parser = initParser(parser));
    }

    private OptionParser initParser(Class<? extends OptionParser> parser) {
        if(parser != null)
            return ReflectionUtil.newInstance(parser);
        else
            return null;
    }

     public ProcessedOption build() throws OptionParserException {
        if(optionType == null) {
            if(!hasValue)
                optionType = OptionType.BOOLEAN;
            else if(isProperty)
                optionType = OptionType.GROUP;
            else if(hasMultipleValues)
                optionType = OptionType.LIST;
            else
                optionType = OptionType.NORMAL;
        }

        if((name == null || name.length() < 1) &&
                optionType != OptionType.ARGUMENTS && optionType != OptionType.ARGUMENT) {
            if(fieldName == null || fieldName.length() < 1)
                throw new OptionParserException("Name must be defined to build an Option");
            else
                name = fieldName;
        }
        //by default fieldName will be given the same name as the option name
        if(fieldName == null)
            fieldName = name;

        if(type == null)
            throw new OptionParserException("Type must be defined to build an Option");

        if((shortName == Character.MIN_VALUE) && "".equals(name) &&
                optionType != OptionType.ARGUMENTS && optionType != OptionType.ARGUMENT) {
            throw new OptionParserException("Either shortName or name must be set.");
        }

        if(validator == null)
            validator = new NullValidator();

        if(converter == null)
            converter = CLConverterManager.getInstance().getConverter(type);

        if(activator == null)
            activator = new NullActivator();

        if(parser == null)
            parser = new AeshOptionParser();

        //if(renderer == null)
        //    renderer = new NullOptionRenderer();

        return new ProcessedOption(shortName, name, description, argument, required,
                valueSeparator, askIfNotSet, selectorType, defaultValues, type, fieldName, optionType, converter,
                completer, validator, activator, renderer, parser, overrideRequired);
    }
}
