/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.internal;

import org.jboss.aesh.cl.activation.NullActivator;
import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.cl.completer.BooleanOptionCompleter;
import org.jboss.aesh.cl.completer.FileOptionCompleter;
import org.jboss.aesh.cl.completer.NullOptionCompleter;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.cl.converter.Converter;
import org.jboss.aesh.cl.converter.CLConverterManager;
import org.jboss.aesh.cl.converter.NullConverter;
import org.jboss.aesh.cl.exception.OptionParserException;
import org.jboss.aesh.cl.renderer.NullOptionRenderer;
import org.jboss.aesh.cl.renderer.OptionRenderer;
import org.jboss.aesh.cl.validator.NullValidator;
import org.jboss.aesh.cl.validator.OptionValidator;
import org.jboss.aesh.io.Resource;
import org.jboss.aesh.util.ReflectionUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Build a {@link org.jboss.aesh.cl.internal.ProcessedOption} object using the Builder pattern.
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
    private char valueSeparator = ',';
    private OptionType optionType;
    private Converter converter;
    private String fieldName;
    private OptionCompleter completer;
    private final List<String> defaultValues;
    private OptionValidator validator;
    private OptionActivator activator;
    private OptionRenderer renderer;
    private boolean overrideRequired;

    public ProcessedOptionBuilder() {
        defaultValues = new ArrayList<>();
    }

    /**
     * The short option Name
     */
    public ProcessedOptionBuilder shortName(char n) {
        shortName = n;
        return this;
    }

    /**
     * The name of the option param.
     * The first letter will be used as the short name.
     * If name is not defined, the variable name will be used.
     */
    public ProcessedOptionBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * A description of the param.
     * This text will be printed out as part of a usage info.
     */
    public ProcessedOptionBuilder description(String description) {
        this.description = description;
        return this;
    }

    /**
     * A description on what kind of value is used for this option.
     */
    public ProcessedOptionBuilder argument(String argument) {
        this.argument = argument;
        return this;
    }

    /**
     * Define the Class type of this Option.
     * If this option is a multiple value option this Class must
     * be defined equal to the parameterized class type.
     * Note that the
     *
     * If its a property option the
     * @param type
     * @return
     */
    public ProcessedOptionBuilder type(Class<?> type) {
        this.type = type;
        return this;
    }

    /**
     * Specify if this option is required
     */
    public ProcessedOptionBuilder required(boolean required) {
        this.required = required;
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

    public ProcessedOptionBuilder hasMultipleValues(boolean hasMultipleValues) {
        this.hasMultipleValues = hasMultipleValues;
        return this;
    }

    public ProcessedOptionBuilder addDefaultValue(String defaultValue) {
        this.defaultValues.add(defaultValue);
        return this;
    }

    public ProcessedOptionBuilder addAllDefaultValues(List<String> defaultValues) {
        this.defaultValues.addAll(defaultValues);
        return this;
    }

    public ProcessedOptionBuilder addAllDefaultValues(String[] defaultValues) {
        for(String s : defaultValues)
            this.defaultValues.add(s);
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
        this.completer = completer;
        return this;
    }

    public ProcessedOptionBuilder completer(Class<? extends OptionCompleter> completer) {
        this.completer = initCompleter(completer);
        return this;
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
        this.validator = validator;
        return this;
    }

    public ProcessedOptionBuilder validator(Class<? extends OptionValidator> validator) {
        this.validator = initValidator(validator);
        return this;
    }

    private OptionValidator initValidator(Class<? extends OptionValidator> validator) {
        if(validator != null && validator != NullValidator.class)
            return ReflectionUtil.newInstance(validator);
        else
            return new NullValidator();
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
        if(activator != null && activator != NullActivator.class)
            return ReflectionUtil.newInstance(activator);
        else
            return new NullActivator();
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
        if(renderer != null && renderer != NullOptionRenderer.class)
            return ReflectionUtil.newInstance(renderer);
        else
            return null;
    }

    public ProcessedOptionBuilder overrideRequired(boolean overrideRequired) {
        this.overrideRequired = overrideRequired;
        return this;
    }

    public ProcessedOption create() throws OptionParserException {
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

        if(name == null || (name.length() < 1 && optionType != OptionType.ARGUMENT)) {
            if(fieldName == null || fieldName.length() < 1)
                throw new OptionParserException("Name must be defined to create an Option");
            else
                name = fieldName;
        }
        if(type == null)
            throw new OptionParserException("Type must be defined to create an Option");

        if((shortName == Character.MIN_VALUE) && name.equals("") && optionType != OptionType.ARGUMENT) {
            throw new OptionParserException("Either shortName or name must be set.");
        }

        if(validator == null)
            validator = new NullValidator();

        if(converter == null)
            converter = CLConverterManager.getInstance().getConverter(type);

        if(activator == null)
            activator = new NullActivator();

        if(renderer == null)
            renderer = new NullOptionRenderer();

        return new ProcessedOption(shortName, name, description, argument, required,
                valueSeparator, defaultValues, type, fieldName, optionType, converter,
                completer, validator, activator, renderer, overrideRequired);
    }
}
