/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.builder;

import org.jboss.aesh.cl.activation.NullActivator;
import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.cl.converter.Converter;
import org.jboss.aesh.cl.converter.CLConverterManager;
import org.jboss.aesh.cl.exception.OptionParserException;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.internal.OptionType;
import org.jboss.aesh.cl.renderer.NullOptionRenderer;
import org.jboss.aesh.cl.renderer.OptionRenderer;
import org.jboss.aesh.cl.validator.NullValidator;
import org.jboss.aesh.cl.validator.OptionValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * Build a {@link org.jboss.aesh.cl.internal.ProcessedOption} object using the Builder pattern.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class OptionBuilder {

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
    private List<String> defaultValues;
    private OptionValidator validator;
    private OptionActivator activator;
    private OptionRenderer renderer;
    private boolean overrideRequired;

    public OptionBuilder() {
        defaultValues = new ArrayList<String>();
    }

    /**
     * The short option Name
     */
    public OptionBuilder shortName(char n) {
        shortName = n;
        return this;
    }

    /**
     * The name of the option param.
     * The first letter will be used as the short name.
     * If name is not defined, the variable name will be used.
     */
    public OptionBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * A description of the param.
     * This text will be printed out as part of a usage info.
     */
    public OptionBuilder description(String description) {
        this.description = description;
        return this;
    }

    /**
     * A description on what kind of value is used for this option.
     */
    public OptionBuilder argument(String argument) {
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
    public OptionBuilder type(Class<?> type) {
        this.type = type;
        return this;
    }

    /**
     * Specify if this option is required
     */
    public OptionBuilder required(boolean required) {
        this.required = required;
        return this;
    }

    public OptionBuilder fieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public OptionBuilder hasValue(boolean hasValue) {
        this.hasValue = hasValue;
        return this;
    }

    public OptionBuilder isProperty(boolean isProperty) {
        this.isProperty = isProperty;
        return this;
    }

    public OptionBuilder hasMultipleValues(boolean hasMultipleValues) {
        this.hasMultipleValues = hasMultipleValues;
        return this;
    }

    public OptionBuilder addDefaultValue(String defaultValue) {
        this.defaultValues.add(defaultValue);
        return this;
    }

    public OptionBuilder valueSeparator(char valueSeparator) {
        this.valueSeparator = valueSeparator;
        return this;
    }

    public OptionBuilder optionType(OptionType optionType) {
        this.optionType = optionType;
        return this;
    }

    public OptionBuilder converter(Converter converter) {
        this.converter = converter;
        return this;
    }

    public OptionBuilder completer(OptionCompleter completer) {
        this.completer = completer;
        return this;
    }

    public OptionBuilder validator(OptionValidator validator) {
        this.validator = validator;
        return this;
    }

    public OptionBuilder activator(OptionActivator activator) {
        this.activator = activator;
        return this;
    }

    public OptionBuilder renderer(OptionRenderer renderer) {
        this.renderer = renderer;
        return this;
    }

    public OptionBuilder overrideRequired(boolean overrideRequired) {
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
