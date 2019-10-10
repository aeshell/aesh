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

import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.converter.Converter;
import org.aesh.command.impl.converter.AeshConverterInvocation;
import org.aesh.command.impl.parser.AeshOptionParser;
import org.aesh.command.parser.OptionParserException;
import org.aesh.command.impl.validator.AeshValidatorInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.parser.OptionParser;
import org.aesh.command.renderer.OptionRenderer;
import org.aesh.command.validator.OptionValidator;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.io.Resource;
import org.aesh.readline.AeshContext;
import org.aesh.io.PipelineResource;
import org.aesh.readline.terminal.formatting.TerminalString;
import org.aesh.selector.SelectorType;
import org.aesh.terminal.utils.ANSI;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public final class ProcessedOption {

    private String shortName;
    private String name;

    private String description;
    private List<String> values;
    private String argument;
    private List<String> defaultValues;
    private Class<?> type;
    private Converter converter;
    private OptionType optionType;
    private boolean required = false;
    private char valueSeparator;
    private String fieldName;
    private OptionCompleter completer;
    private Map<String,String> properties;
    private boolean longNameUsed = true;
    private OptionValidator validator;
    private boolean endsWithSeparator = false;
    private OptionActivator activator;
    private OptionRenderer renderer;
    private boolean overrideRequired = false;
    private boolean ansiMode = true;
    private ProcessedCommand parent;
    private OptionParser parser;
    private boolean cursorOption = false;
    private boolean cursorValue = false;
    private boolean askIfNotSet = false;
    private final SelectorType selectorType;

    public ProcessedOption(char shortName, String name, String description,
                           String argument, boolean required, char valueSeparator, boolean askIfNotSet,
                           SelectorType selectorType,
                           List<String> defaultValue, Class<?> type, String fieldName,
                           OptionType optionType, Converter converter, OptionCompleter completer,
                           OptionValidator optionValidator,
                           OptionActivator activator,
                           OptionRenderer renderer, OptionParser parser,
                           boolean overrideRequired) throws OptionParserException {

        if(shortName != '\u0000')
            this.shortName = String.valueOf(shortName);
        this.name = name;
        this.description = description;
        this.argument = argument;
        this.required = required;
        this.valueSeparator = valueSeparator;
        this.type = type;
        this.fieldName = fieldName;
        this.overrideRequired = overrideRequired;
        this.optionType = optionType;
        this.converter = converter;
        this.completer = completer;
        this.validator = optionValidator;
        this.activator = activator;
        this.askIfNotSet = askIfNotSet;
        if(selectorType != null)
            this.selectorType = selectorType;
        else
            this.selectorType = SelectorType.NO_OP;
        if(parser != null)
            this.parser = parser;
        else
            this.parser = new AeshOptionParser();

        if(renderer != null)
            this.renderer = renderer;

        this.defaultValues = defaultValue;

        properties = new HashMap<>();
        values = new ArrayList<>();
    }

    public String shortName() {
        return shortName;
    }

    public String name() {
        return name;
    }

    public void addValue(String value) {
        values.add(value);
    }

    public void addValues(List<String> values) {
        this.values.addAll(values);
    }

    public String getValue() {
        if(values.size() > 0)
            return values.get(0);
        else
            return null;
    }

    public String getLastValue() {
        if(values.size() > 0)
            return values.get(values.size()-1);
        else
            return null;
    }

    public List<String> getValues() {
        return values;
    }

    public boolean hasValue() {
        return optionType != OptionType.BOOLEAN;
    }

    public boolean hasMultipleValues() {
        return optionType == OptionType.LIST || optionType == OptionType.ARGUMENTS || optionType == OptionType.GROUP;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean doOverrideRequired() {
        return overrideRequired;
    }

    public Class<?> type() {
        return type;
    }

    public String description() {
        return description;
    }

    public char getValueSeparator() {
       return valueSeparator;
    }

    public boolean isProperty() {
        return optionType == OptionType.GROUP;
    }

    public String getArgument() {
        return argument;
    }

    public List<String> getDefaultValues() {
        return defaultValues;
    }

    public void addProperty(String name, String value) {
        properties.put(name, value);
    }

    public Map<String,String> getProperties() {
        return properties;
    }

    public OptionType getOptionType() {
        return optionType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Converter converter() {
        return converter;
    }

    public OptionCompleter completer() {
        return completer;
    }

    public OptionValidator validator() {
        return validator;
    }

    public OptionActivator activator() {
        return activator;
    }

    public OptionParser parser() {
        return parser;
    }

    void setParent(ProcessedCommand parent) {
        this.parent = parent;
    }

    public ProcessedCommand parent() {
        return parent;
    }

    /**
     * might return null if render is not specified
     */
    public OptionRenderer getRenderer() {
        return renderer;
    }

    public boolean isLongNameUsed() {
        return longNameUsed;
    }

    public void setLongNameUsed(boolean longNameUsed) {
        this.longNameUsed = longNameUsed;
    }

    public void setEndsWithSeparator(boolean endsWithSeparator) {
        this.endsWithSeparator = endsWithSeparator;
    }

    public boolean getEndsWithSeparator() {
        return endsWithSeparator;
    }

    public boolean askIfNotSet() {
        return askIfNotSet;
    }

    public SelectorType selectorType() {
        return selectorType;
    }

    public void clear() {
        if(values != null)
            values.clear();
        if(properties != null)
            properties.clear();
        longNameUsed = true;
        endsWithSeparator = false;
        cursorOption = false;
        cursorValue = false;
    }

    public String getDisplayName() {
        if(isLongNameUsed() && name != null) {
            return "--"+ name;
        }
        else if(shortName != null)
            return "-"+ shortName;
        else
            return null;
    }

    public TerminalString getRenderedNameWithDashes() {
        if(renderer == null || !ansiMode)
            //if hasValue append a = after the name
            return new TerminalString( hasValue() ? "--"+name+"=" : "--"+name, true);
        else
            return new TerminalString( hasValue() ? "--"+name+"=" : "--"+name, renderer.getColor(), renderer.getTextType());
    }

    public int getFormattedLength() {
        StringBuilder sb = new StringBuilder();
        if(shortName != null)
            sb.append("-").append(shortName);
        if(name != null) {
            if(sb.toString().trim().length() > 0)
                sb.append(", ");
            sb.append("--").append(name);
        }
        if(argument != null && argument.length() > 0) {
            sb.append("=<").append(argument).append(">");
        }

        return sb.length();
    }

    //TODO: add offset, offset for descriptionstart and break on width
    public String getFormattedOption(int offset, int descriptionStart, int width) {
        StringBuilder sb = new StringBuilder();
        if(required && ansiMode)
            sb.append(ANSI.BOLD);
        if(offset > 0)
            sb.append(String.format("%" + offset+ "s", ""));
        if(shortName != null)
            sb.append("-").append(shortName);
        if(name != null && name.length() > 0) {
            if(shortName != null)
                sb.append(", ");
            sb.append("--").append(name);
        }
        if(argument != null && argument.length() > 0) {
            sb.append("=<").append(argument).append(">");
        }
        if(required && ansiMode)
            sb.append(ANSI.BOLD_OFF);
        if(description != null && description.length() > 0) {
            //int descOffset = descriptionStart - sb.length();
            int descOffset = descriptionStart - getFormattedLength() - offset;
            if(descOffset > 0)
                sb.append(String.format("%"+descOffset+"s", ""));
            else
                sb.append(" ");

            sb.append(description);
        }

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public Object doConvert(String inputValue, InvocationProviders invocationProviders,
            Object command, AeshContext aeshContext, boolean doValidation) throws OptionValidatorException {
        Object result = converter.convert(
        invocationProviders.getConverterProvider().enhanceConverterInvocation(
                new AeshConverterInvocation(inputValue, aeshContext)));
        //Object result =   converter.convert(inputValue);
        if(validator != null && doValidation) {
            validator.validate(invocationProviders.getValidatorProvider().enhanceValidatorInvocation(
                    new AeshValidatorInvocation(result, command, aeshContext)));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public void injectValueIntoField(Object instance, InvocationProviders invocationProviders, AeshContext aeshContext,
                                     boolean doValidation) throws OptionValidatorException {
        if(converter == null || instance == null)
            return;
        try {
            Field field = getField(instance.getClass(), fieldName);
            //for some options, the field might be null. eg generatedHelp
            //if so we ignore it
            if(field == null)
                return;
            if(!Modifier.isPublic(field.getModifiers()))
                field.setAccessible(true);
            if(!Modifier.isPublic(instance.getClass().getModifiers())) {
                Constructor constructor = instance.getClass().getDeclaredConstructor();
                if(constructor != null)
                    constructor.setAccessible(true);
            }
            if(optionType == OptionType.NORMAL || optionType == OptionType.BOOLEAN || optionType == OptionType.ARGUMENT) {
                if(getValue() != null)
                    field.set(instance, doConvert(getValue(), invocationProviders, instance, aeshContext, doValidation));
                else if(defaultValues.size() > 0) {
                    field.set(instance, doConvert(defaultValues.get(0), invocationProviders, instance, aeshContext, doValidation));
                }
            }
            else if(optionType == OptionType.LIST || optionType == OptionType.ARGUMENTS) {
                Collection<Object> tmpSet = initializeCollection(field);
                if(values.size() > 0) {
                    for(String in : values)
                        tmpSet.add(doConvert(in, invocationProviders, instance, aeshContext, doValidation));
                }
                else if(defaultValues.size() > 0) {
                    for(String in : defaultValues)
                        tmpSet.add(doConvert(in, invocationProviders, instance, aeshContext, doValidation));
                }

                field.set(instance, tmpSet);
            }
            else if(optionType == OptionType.GROUP) {
                if(field.getType().isInterface() || Modifier.isAbstract(field.getType().getModifiers())) {
                    Map<String, Object> tmpMap = newHashMap();
                    for(String propertyKey : properties.keySet())
                        tmpMap.put(propertyKey,doConvert(properties.get(propertyKey), invocationProviders, instance, aeshContext, doValidation));
                    field.set(instance, tmpMap);
                 }
                else {
                    Map<String,Object> tmpMap = (Map<String,Object>) field.getType().newInstance();
                    for(String propertyKey : properties.keySet())
                        tmpMap.put(propertyKey,doConvert(properties.get(propertyKey), invocationProviders, instance, aeshContext, doValidation));
                    field.set(instance, tmpMap);
                }
            }
        }
        catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> initializeCollection(Field field) throws IllegalAccessException, InstantiationException {
        if(field.getType().isInterface() || Modifier.isAbstract(field.getType().getModifiers())) {
            if(Set.class.isAssignableFrom(field.getType()))
                return  new HashSet<>();
            else if(List.class.isAssignableFrom(field.getType()))
                return new ArrayList<>();
            else
                return null;
        }
        else
            return (Collection) field.getType().newInstance();
    }

    public void injectResource(PipelineResource resource, Object instance) {
        try {
            Field field = getField(instance.getClass(), fieldName);
            if(!Modifier.isPublic(field.getModifiers()))
                field.setAccessible(true);

            if(optionType == OptionType.ARGUMENT) {
                field.set(instance, resource);
            }
            else {
                Collection<Object> set = initializeCollection(field);
                if(set != null)
                    set.add(resource);
            }

        } catch (NoSuchFieldException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    public void updateInvocationProviders(InvocationProviders invocationProviders) {
        activator = invocationProviders.getOptionActivatorProvider().enhanceOptionActivator(activator);
    }

    public void updateAnsiMode(boolean ansiMode) {
        this.ansiMode = ansiMode;
    }

    private <S, T> Map<S, T> newHashMap() {
        return new HashMap<>();
    }

    private Field getField(Class clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        }
        catch(NoSuchFieldException nsfe) {
            if(clazz.getSuperclass() != null)
                return getField(clazz.getSuperclass(), fieldName);
            else
                return null;
        }
    }

    public boolean isCursorOption() {
        return cursorOption;
    }

    public void setCursorOption(boolean cursorOption) {
        this.cursorOption = cursorOption;
    }

    public boolean isCursorValue() {
        return cursorValue;
    }

    public void setCursorValue(boolean cursorValue) {
        this.cursorValue = cursorValue;
    }

    public boolean hasDefaultValue() {
        return getDefaultValues() != null && getDefaultValues().size() > 0;
    }

    boolean isTypeAssignableByResourcesOrFile() {
        return (Resource.class.isAssignableFrom(type) ||
                   File.class.isAssignableFrom(type) ||
                   Path.class.isAssignableFrom(type));
    }

    @Override
    public String toString() {
        return "ProcessedOption{" +
                "shortName='" + shortName + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", values=" + values +
                ", defaultValues=" + defaultValues +
                ", arguments='" + argument + '\'' +
                ", type=" + type +
                ", required=" + required +
                ", valueSeparator=" + valueSeparator +
                ", properties=" + properties +
                '}';
    }
}
