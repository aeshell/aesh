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
import org.jboss.aesh.cl.converter.CLConverter;
import org.jboss.aesh.cl.converter.CLConverterManager;
import org.jboss.aesh.cl.converter.NullConverter;
import org.jboss.aesh.cl.exception.OptionParserException;
import org.jboss.aesh.cl.validator.NullValidator;
import org.jboss.aesh.cl.validator.OptionValidator;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.util.ReflectionUtil;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
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
    private CLConverter converter;
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

     public ProcessedOption(char shortName, String name, String description,
                            String argument, boolean required, char valueSeparator,
                            List<String> defaultValue, Class<?> type, String fieldName,
                            OptionType optionType, CLConverter converter, OptionCompleter completer,
                            OptionValidator optionValidator,
                            OptionActivator activator) throws OptionParserException {
         this(shortName, name, description, argument, required, valueSeparator, defaultValue,
                 type, fieldName, optionType,
                 (Class<? extends CLConverter>) null,(Class<? extends OptionCompleter>) null,
                 (Class<? extends OptionValidator>) null,
                 (Class<? extends OptionActivator>) null);
         this.converter = converter;
         this.completer = completer;
         this.validator = optionValidator;
         this.activator = activator;
         if(this.validator == null)
             this.validator = new NullValidator();
         if(this.activator == null)
             this.activator = new NullActivator();
     }


    public ProcessedOption(char shortName, String name, String description,
                           String argument, boolean required, char valueSeparator,
                           List<String> defaultValue, Class<?> type, String fieldName,
                           OptionType optionType, Class<? extends CLConverter> converter,
                           OptionCompleter completer) throws OptionParserException {
        this(shortName, name, description, argument, required, valueSeparator, defaultValue,
                type, fieldName, optionType, converter, null, null, null);
        this.completer = completer;
    }
    public ProcessedOption(char shortName, String name, String description,
                           String argument, boolean required, char valueSeparator,
                           String[] defaultValue, Class<?> type, String fieldName,
                           OptionType optionType, Class<? extends CLConverter> converter,
                           Class<? extends OptionCompleter> completer,
                           Class<? extends OptionValidator> optionValidator,
                           Class<? extends OptionActivator> activator) throws OptionParserException {
        this(shortName, name, description, argument, required, valueSeparator, Arrays.asList(defaultValue),
                type, fieldName, optionType, converter, completer, optionValidator, activator);
    }

    public ProcessedOption(char shortName, String name, String description,
                           String argument, boolean required, char valueSeparator,
                           List<String> defaultValue, Class<?> type, String fieldName,
                           OptionType optionType, Class<? extends CLConverter> converter,
                           Class<? extends OptionCompleter> completer,
                           Class<? extends OptionValidator> optionValidator,
                           Class<? extends OptionActivator> optionActivator) throws OptionParserException {
        this.shortName = String.valueOf(shortName);
        this.name = name;
        this.description = description;
        this.argument = argument;
        this.required = required;
        this.valueSeparator = valueSeparator;
        this.type = type;
        this.fieldName = fieldName;
        this.optionType = optionType;
        this.converter = initConverter(converter);
        this.completer = initCompleter(completer);
        this.validator = initValidator(optionValidator);
        this.activator = initActivator(optionActivator);

        this.defaultValues = new ArrayList<String>();
        if(defaultValue != null)
            defaultValues.addAll(defaultValue);

        properties = new HashMap<String, String>();
        values = new ArrayList<String>();

        if((shortName == Character.MIN_VALUE) && name.equals("") && optionType != OptionType.ARGUMENT) {
            throw new OptionParserException("Either shortName or long shortName must be set.");
        }
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
        return optionType == OptionType.LIST || optionType == OptionType.ARGUMENT;
    }

    public boolean isRequired() {
        return required;
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

    public CLConverter getConverter() {
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
        if(offset > 0)
            sb.append(String.format("%" + offset+ "s", ""));
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

    private CLConverter initConverter(Class<? extends CLConverter> converterClass) {
        if(converterClass != null && converterClass != NullConverter.class) {
            if( CLConverterManager.getInstance().hasConverter(converterClass))
                return CLConverterManager.getInstance().getConverter(converterClass);
            else
                return ReflectionUtil.newInstance(converterClass);
        }
        else
            return CLConverterManager.getInstance().getConverter(type);
    }

    private OptionCompleter initCompleter(Class<? extends OptionCompleter> completerClass) {

        if(completerClass != null && completerClass != NullOptionCompleter.class) {
                return ReflectionUtil.newInstance(completerClass);
        }
        else {
            try {
                if(type == Boolean.class || type == boolean.class)
                    return BooleanOptionCompleter.class.newInstance();
                else if(type == File.class)
                    return FileOptionCompleter.class.newInstance();
                else
                    return null;

            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private OptionActivator initActivator(Class<? extends OptionActivator> activator) {
        if(activator != null && activator != NullActivator.class)
            return ReflectionUtil.newInstance(activator);
        else
            return new NullActivator();
    }

    private OptionValidator initValidator(Class<? extends OptionValidator> validator) {
        if(validator != null && validator != NullValidator.class)
            return ReflectionUtil.newInstance(validator);
        else
            return new NullValidator();
    }

    private Object doConvert(String inputValue,boolean doValidation) throws OptionValidatorException {
        Object result = converter.convert(inputValue);
        if(validator != null && doValidation)
            validator.validate(result);
        return result;
    }

    public void injectValueIntoField(Object instance) throws OptionValidatorException {
        injectValueIntoField(instance, true);
    }

    public void injectValueIntoField(Object instance, boolean doValidation) throws OptionValidatorException {
        if(converter == null)
            return;
        try {
            Field field = instance.getClass().getDeclaredField(fieldName);
            if(!Modifier.isPublic(field.getModifiers()))
                field.setAccessible(true);
            if(!Modifier.isPublic(instance.getClass().getModifiers())) {
                Constructor constructor = instance.getClass().getDeclaredConstructor();
                if(constructor != null)
                    constructor.setAccessible(true);
            }
            if(optionType == OptionType.NORMAL || optionType == OptionType.BOOLEAN) {
                if(getValue() != null)
                    field.set(instance, doConvert(getValue(), doValidation));
                else if(defaultValues.size() > 0) {
                    field.set(instance, doConvert(defaultValues.get(0), doValidation));
                }
            }
            else if(optionType == OptionType.LIST || optionType == OptionType.ARGUMENT) {
                if(field.getType().isInterface() || Modifier.isAbstract(field.getType().getModifiers())) {
                    if(Set.class.isAssignableFrom(field.getType())) {
                        Set tmpSet = new HashSet<Object>();
                        if(values.size() > 0) {
                            for(String in : values)
                                tmpSet.add(doConvert(in, doValidation));
                        }
                        else if(defaultValues.size() > 0) {
                            for(String in : defaultValues)
                                tmpSet.add(doConvert(in, doValidation));
                        }

                        field.set(instance, tmpSet);
                    }
                    else if(List.class.isAssignableFrom(field.getType())) {
                        List tmpList = new ArrayList();
                        if(values.size() > 0) {
                            for(String in : values)
                                tmpList.add(doConvert(in, doValidation));
                        }
                        else if(defaultValues.size() > 0) {
                            for(String in : values)
                                tmpList.add(doConvert(in, doValidation));
                        }
                        field.set(instance, tmpList);
                    }
                    //todo: should support more that List/Set
                }
                else {
                    Collection tmpInstance = (Collection) field.getType().newInstance();
                    if(values.size() > 0) {
                        for(String in : values)
                            tmpInstance.add(doConvert(in, doValidation));
                    }
                    else if(defaultValues.size() > 0) {
                        for(String in : defaultValues)
                            tmpInstance.add(doConvert(in, doValidation));
                    }
                    field.set(instance, tmpInstance);
                }
            }
            else if(optionType == OptionType.GROUP) {
                if(field.getType().isInterface() || Modifier.isAbstract(field.getType().getModifiers())) {
                    Map<String, Object> tmpMap = newHashMap();
                    for(String propertyKey : properties.keySet())
                        tmpMap.put(propertyKey,doConvert(properties.get(propertyKey), doValidation));
                    field.set(instance, tmpMap);
                 }
                else {
                    Map<String,Object> tmpMap = (Map<String,Object>) field.getType().newInstance();
                    for(String propertyKey : properties.keySet())
                        tmpMap.put(propertyKey,doConvert(properties.get(propertyKey), doValidation));
                    field.set(instance, tmpMap);
                }
            }
        }
        catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    private <String, T> Map<String, T> newHashMap() {
        return new HashMap<String, T>();
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
