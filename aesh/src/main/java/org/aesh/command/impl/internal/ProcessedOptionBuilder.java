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
package org.aesh.command.impl.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.converter.Converter;
import org.aesh.command.impl.completer.BooleanOptionCompleter;
import org.aesh.command.impl.completer.NullOptionCompleter;
import org.aesh.command.impl.converter.NullConverter;
import org.aesh.command.impl.renderer.NullOptionRenderer;
import org.aesh.command.impl.validator.NullValidator;
import org.aesh.command.parser.OptionParser;
import org.aesh.command.parser.OptionParserException;
import org.aesh.command.renderer.OptionRenderer;
import org.aesh.command.validator.OptionValidator;
import org.aesh.converter.CLConverterManager;
import org.aesh.selector.SelectorType;
import org.aesh.util.ReflectionUtil;

/**
 * Build a {@link ProcessedOption} object using the Builder pattern.
 *
 * @author Aesh team
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
    private List<String> defaultValues;
    private OptionValidator validator;
    private OptionActivator activator;
    private OptionRenderer renderer;
    private boolean overrideRequired;
    private OptionParser parser;
    private boolean askIfNotSet = false;
    private boolean acceptNameWithoutDashes = false;
    private SelectorType selectorType;
    private boolean optionalValue = false;
    private boolean negatable = false;
    private String negationPrefix = "no-";
    private boolean inherited = false;
    private String descriptionUrl;
    private boolean isUrl = false;
    private BiConsumer<Object, Object> fieldSetter;
    private java.util.function.Consumer<Object> fieldResetter;
    private java.util.function.Function<Object, Object> fieldGetter;
    private String mixinFieldName;
    private List<String> aliases;
    private String helpGroup = "";
    private List<String> exclusiveWith;

    private ProcessedOptionBuilder() {
        defaultValues = java.util.Collections.emptyList();
    }

    public static ProcessedOptionBuilder builder() {
        return new ProcessedOptionBuilder();
    }

    public ProcessedOptionBuilder shortName(char n) {
        this.shortName = n;
        return this;
    }

    public ProcessedOptionBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ProcessedOptionBuilder askIfNotSet(boolean askIfNotSet) {
        this.askIfNotSet = askIfNotSet;
        return this;
    }

    public ProcessedOptionBuilder description(String description) {
        this.description = description;
        return this;
    }

    public ProcessedOptionBuilder argument(String argument) {
        this.argument = argument;
        return this;
    }

    public ProcessedOptionBuilder type(Class<?> type) {
        this.type = type;
        return this;
    }

    public ProcessedOptionBuilder required(boolean required) {
        this.required = required;
        return this;
    }

    public ProcessedOptionBuilder acceptNameWithoutDashes(boolean acceptNameWithoutDashes) {
        this.acceptNameWithoutDashes = acceptNameWithoutDashes;
        return this;
    }

    public ProcessedOptionBuilder selector(SelectorType selectorType) {
        this.selectorType = selectorType;
        return this;
    }

    public ProcessedOptionBuilder fieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public ProcessedOptionBuilder hasValue(boolean hasValue) {
        this.hasValue = hasValue;
        return this;
    }

    public ProcessedOptionBuilder isProperty(boolean isProperty) {
        this.isProperty = isProperty;
        return this;
    }

    public ProcessedOptionBuilder hasMultipleValues(boolean multipleValues) {
        this.hasMultipleValues = multipleValues;
        return this;
    }

    public ProcessedOptionBuilder addDefaultValue(String defaultValue) {
        if (!(this.defaultValues instanceof ArrayList))
            this.defaultValues = new ArrayList<>();
        this.defaultValues.add(defaultValue);
        return this;
    }

    public ProcessedOptionBuilder addAllDefaultValues(List<String> defaultValues) {
        if (defaultValues != null && !defaultValues.isEmpty()) {
            if (!(this.defaultValues instanceof ArrayList))
                this.defaultValues = new ArrayList<>(defaultValues.size());
            this.defaultValues.addAll(defaultValues);
        }
        return this;
    }

    public ProcessedOptionBuilder addAllDefaultValues(String[] defaultValues) {
        if (defaultValues != null && defaultValues.length > 0) {
            if (!(this.defaultValues instanceof ArrayList))
                this.defaultValues = new ArrayList<>(defaultValues.length);
            this.defaultValues.addAll(Arrays.asList(defaultValues));
        }
        return this;
    }

    public ProcessedOptionBuilder valueSeparator(char valueSeparator) {
        this.valueSeparator = valueSeparator;
        return this;
    }

    public ProcessedOptionBuilder optionType(OptionType optionType) {
        this.optionType = optionType;
        return this;
    }

    public ProcessedOptionBuilder converter(Converter converter) {
        this.converter = converter;
        return this;
    }

    public ProcessedOptionBuilder converter(Class<? extends Converter> converter) {
        this.converter = initConverter(converter);
        return this;
    }

    private Converter initConverter(Class<? extends Converter> converterClass) {
        if (converterClass != null && !converterClass.equals(NullConverter.class)) {
            Converter converter = CLConverterManager.getInstance().getConverter(converterClass);
            return converter != null ? converter : ReflectionUtil.newInstance(converterClass);
        } else
            return CLConverterManager.getInstance().getConverter(type);
    }

    public ProcessedOptionBuilder completer(OptionCompleter completer) {
        this.completer = completer;
        return this;
    }

    public ProcessedOptionBuilder completer(Class<? extends OptionCompleter> completer) {
        this.completer = initCompleter(completer);
        return this;
    }

    private OptionCompleter initCompleter(Class<? extends OptionCompleter> completerClass) {

        if (completerClass != null && !completerClass.equals(NullOptionCompleter.class)) {
            return ReflectionUtil.newInstance(completerClass);
        } else {
            // File/Resource completers are deferred — ProcessedOption.completer() lazy-creates them
            if (type == Boolean.class || type == boolean.class)
                return new BooleanOptionCompleter();
            else
                return null;
        }
    }

    public ProcessedOptionBuilder validator(OptionValidator validator) {
        this.validator = validator;
        return this;
    }

    public ProcessedOptionBuilder validator(Class<? extends OptionValidator> validator) {
        this.validator = initValidator(validator);
        return this;
    }

    private OptionValidator initValidator(Class<? extends OptionValidator> validator) {
        if (validator != null && validator != NullValidator.class)
            return ReflectionUtil.newInstance(validator);
        else
            return null;
    }

    public ProcessedOptionBuilder activator(OptionActivator activator) {
        this.activator = activator;
        return this;
    }

    public ProcessedOptionBuilder activator(Class<? extends OptionActivator> activator) {
        this.activator = initActivator(activator);
        return this;
    }

    private OptionActivator initActivator(Class<? extends OptionActivator> activator) {
        if (activator != null && activator != org.aesh.command.impl.activator.NullActivator.class)
            return ReflectionUtil.newInstance(activator);
        else
            return null;
    }

    public ProcessedOptionBuilder renderer(OptionRenderer renderer) {
        this.renderer = renderer;
        return this;
    }

    public ProcessedOptionBuilder renderer(Class<? extends OptionRenderer> renderer) {
        this.renderer = initRenderer(renderer);
        return this;
    }

    private OptionRenderer initRenderer(Class<? extends OptionRenderer> renderer) {
        if (renderer != null && renderer != NullOptionRenderer.class)
            return ReflectionUtil.newInstance(renderer);
        else
            return null;
    }

    public ProcessedOptionBuilder overrideRequired(boolean overrideRequired) {
        this.overrideRequired = overrideRequired;
        return this;
    }

    public ProcessedOptionBuilder parser(OptionParser parser) {
        this.parser = parser;
        return this;
    }

    public ProcessedOptionBuilder parser(Class<? extends OptionParser> parser) {
        this.parser = initParser(parser);
        return this;
    }

    private OptionParser initParser(Class<? extends OptionParser> parser) {
        if (parser != null)
            return ReflectionUtil.newInstance(parser);
        else
            return null;
    }

    /**
     * Set whether this option accepts an optional value (arity 0..1).
     * When used without a value, the defaultValue is applied.
     */
    public ProcessedOptionBuilder optionalValue(boolean optionalValue) {
        this.optionalValue = optionalValue;
        return this;
    }

    public ProcessedOptionBuilder negatable(boolean negatable) {
        this.negatable = negatable;
        return this;
    }

    public ProcessedOptionBuilder negationPrefix(String negationPrefix) {
        this.negationPrefix = negationPrefix;
        return this;
    }

    public ProcessedOptionBuilder inherited(boolean inherited) {
        this.inherited = inherited;
        return this;
    }

    public ProcessedOptionBuilder descriptionUrl(String descriptionUrl) {
        this.descriptionUrl = descriptionUrl;
        return this;
    }

    public ProcessedOptionBuilder url(boolean isUrl) {
        this.isUrl = isUrl;
        return this;
    }

    public ProcessedOptionBuilder fieldSetter(BiConsumer<Object, Object> fieldSetter) {
        this.fieldSetter = fieldSetter;
        return this;
    }

    public ProcessedOptionBuilder fieldResetter(java.util.function.Consumer<Object> fieldResetter) {
        this.fieldResetter = fieldResetter;
        return this;
    }

    public ProcessedOptionBuilder fieldGetter(java.util.function.Function<Object, Object> fieldGetter) {
        this.fieldGetter = fieldGetter;
        return this;
    }

    public ProcessedOptionBuilder mixinFieldName(String mixinFieldName) {
        this.mixinFieldName = mixinFieldName;
        return this;
    }

    public ProcessedOptionBuilder aliases(List<String> aliases) {
        this.aliases = aliases;
        return this;
    }

    public ProcessedOptionBuilder aliases(String... aliases) {
        this.aliases = aliases != null && aliases.length > 0
                ? java.util.Arrays.asList(aliases)
                : null;
        return this;
    }

    public ProcessedOptionBuilder helpGroup(String helpGroup) {
        this.helpGroup = helpGroup != null ? helpGroup : "";
        return this;
    }

    public ProcessedOptionBuilder exclusiveWith(String... exclusiveWith) {
        this.exclusiveWith = exclusiveWith != null && exclusiveWith.length > 0
                ? java.util.Arrays.asList(exclusiveWith)
                : null;
        return this;
    }

    public ProcessedOption build() throws OptionParserException {
        if (optionType == null) {
            if (!hasValue)
                optionType = OptionType.BOOLEAN;
            else if (isProperty)
                optionType = OptionType.GROUP;
            else if (hasMultipleValues)
                optionType = OptionType.LIST;
            else
                optionType = OptionType.NORMAL;
        }

        if ((name == null || name.length() < 1) &&
                optionType != OptionType.ARGUMENTS && optionType != OptionType.ARGUMENT) {
            if (fieldName == null || fieldName.length() < 1)
                throw new OptionParserException("Name must be defined to build an Option");
            else
                name = fieldName;
        }
        //by default fieldName will be given the same name as the option name
        if (fieldName == null)
            fieldName = name;

        if (type == null)
            throw new OptionParserException("Type must be defined to build an Option");

        if ((shortName == Character.MIN_VALUE) && "".equals(name) &&
                optionType != OptionType.ARGUMENTS && optionType != OptionType.ARGUMENT) {
            throw new OptionParserException("Either shortName or name must be set.");
        }

        if (converter == null)
            converter = CLConverterManager.getInstance().getConverter(type);

        // parser left null here — ProcessedOption.parser() lazy-creates AeshOptionParser

        //if(renderer == null)
        //    renderer = new NullOptionRenderer();

        // Validate that negatable is only used with boolean types
        if (negatable && type != Boolean.class && type != boolean.class) {
            throw new OptionParserException("Option '" + name + "' is marked as negatable but is not a boolean type");
        }

        // Validate that optionalValue requires hasValue (NORMAL type)
        if (optionalValue && optionType != OptionType.NORMAL) {
            throw new OptionParserException("Option '" + name + "' is marked as optionalValue but does not accept values");
        }

        ProcessedOption option = new ProcessedOption(shortName, name, description, argument, required,
                valueSeparator, askIfNotSet, acceptNameWithoutDashes, selectorType, defaultValues, type, fieldName, optionType,
                converter,
                completer, validator, activator, renderer, parser, overrideRequired, negatable, negationPrefix, inherited,
                descriptionUrl, isUrl, optionalValue);
        if (fieldSetter != null)
            option.setFieldSetter(fieldSetter);
        if (fieldResetter != null)
            option.setFieldResetter(fieldResetter);
        if (fieldGetter != null)
            option.setFieldGetter(fieldGetter);
        if (mixinFieldName != null)
            option.setMixinFieldName(mixinFieldName);
        if (aliases != null)
            option.setAliases(aliases);
        if (helpGroup != null && !helpGroup.isEmpty())
            option.setHelpGroup(helpGroup);
        if (exclusiveWith != null)
            option.setExclusiveWith(exclusiveWith);
        return option;
    }
}
