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
import org.aesh.command.impl.parser.OptionParserException;
import org.aesh.command.renderer.OptionRenderer;
import org.aesh.command.validator.OptionValidator;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.console.AeshContext;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.impl.converter.AeshConverterInvocation;
import org.aesh.command.impl.validator.AeshValidatorInvocation;
import org.aesh.terminal.formatting.TerminalString;
import org.aesh.util.ANSI;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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

    public ProcessedOption(char shortName, String name, String description,
                           String argument, boolean required, char valueSeparator,
                           List<String> defaultValue, Class<?> type, String fieldName,
                           OptionType optionType, Converter converter, OptionCompleter completer,
                           OptionValidator optionValidator,
                           OptionActivator activator,
                           OptionRenderer renderer, boolean overrideRequired) throws OptionParserException {

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

        if(renderer != null)
            this.renderer = renderer;

        this.defaultValues = defaultValue;

        properties = new HashMap<>();
        values = new ArrayList<>();
    }

    public String getShortName() {
        return shortName;
    }

    public String getName() {
        return name;
    }

    public void addValue(String value) {
        values.add(value);
    }

    public String getValue() {
        if(values.size() > 0)
            return values.get(0);
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
        return optionType == OptionType.LIST || optionType == OptionType.ARGUMENT || optionType == OptionType.GROUP;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean doOverrideRequired() {
        return overrideRequired;
    }

    public Class<?> getType() {
        return type;
    }

    public String getDescription() {
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

    public Converter getConverter() {
        return converter;
    }

    public OptionCompleter getCompleter() {
        return completer;
    }

    public OptionValidator getValidator() {
        return validator;
    }

    public OptionActivator getActivator() {
        return activator;
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

    public void clear() {
        if(values != null)
            values.clear();
        if(properties != null)
            properties.clear();
        longNameUsed = true;
        endsWithSeparator = false;
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
            return new TerminalString("--"+name, true);
        else
            return new TerminalString("--"+name, renderer.getColor(), renderer.getTextType());
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
        if(converter == null)
            return;
        try {
            Field field = getField(instance.getClass(), fieldName);
            if(!Modifier.isPublic(field.getModifiers()))
                field.setAccessible(true);
            if(!Modifier.isPublic(instance.getClass().getModifiers())) {
                Constructor constructor = instance.getClass().getDeclaredConstructor();
                if(constructor != null)
                    constructor.setAccessible(true);
            }
            if(optionType == OptionType.NORMAL || optionType == OptionType.BOOLEAN) {
                if(getValue() != null)
                    field.set(instance, doConvert(getValue(), invocationProviders, instance, aeshContext, doValidation));
                else if(defaultValues.size() > 0) {
                    field.set(instance, doConvert(defaultValues.get(0), invocationProviders, instance, aeshContext, doValidation));
                }
            }
            else if(optionType == OptionType.LIST || optionType == OptionType.ARGUMENT) {
                if(field.getType().isInterface() || Modifier.isAbstract(field.getType().getModifiers())) {
                    if(Set.class.isAssignableFrom(field.getType())) {
                        Set tmpSet = new HashSet<Object>();
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
                    else if(List.class.isAssignableFrom(field.getType())) {
                        List tmpList = new ArrayList();
                        if(values.size() > 0) {
                            for(String in : values)
                                tmpList.add(doConvert(in, invocationProviders, instance, aeshContext, doValidation));
                        }
                        else if(defaultValues.size() > 0) {
                            for(String in : defaultValues)
                                tmpList.add(doConvert(in, invocationProviders, instance, aeshContext, doValidation));
                        }
                        field.set(instance, tmpList);
                    }
                    //todo: should support more that List/Set
                }
                else {
                    Collection tmpInstance = (Collection) field.getType().newInstance();
                    if(values.size() > 0) {
                        for(String in : values)
                            tmpInstance.add(doConvert(in, invocationProviders, instance, aeshContext, doValidation));
                    }
                    else if(defaultValues.size() > 0) {
                        for(String in : defaultValues)
                            tmpInstance.add(doConvert(in, invocationProviders, instance, aeshContext, doValidation));
                    }
                    field.set(instance, tmpInstance);
                }
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

    public void updateInvocationProviders(InvocationProviders invocationProviders) {
        activator = invocationProviders.getOptionActivatorProvider().enhanceOptionActivator(activator);
    }

    public void updateAnsiMode(boolean ansiMode) {
        this.ansiMode = ansiMode;
    }

    private <String, T> Map<String, T> newHashMap() {
        return new HashMap<>();
    }

    private Field getField(Class clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        }
        catch(NoSuchFieldException nsfe) {
            if(clazz.getSuperclass() != null)
                return getField(clazz.getSuperclass(), fieldName);
            else throw nsfe;
        }
    }


    public boolean hasDefaultValue() {
        return getDefaultValues() != null && getDefaultValues().size() > 0;
    }

    @Override
    public String toString() {
        return "ProcessedOption{" +
                "shortName='" + shortName + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", values=" + values +
                ", defaultValues=" + defaultValues +
                ", argument='" + argument + '\'' +
                ", type=" + type +
                ", required=" + required +
                ", valueSeparator=" + valueSeparator +
                ", properties=" + properties +
                '}';
    }
}
