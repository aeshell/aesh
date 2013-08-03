/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.internal;

import org.jboss.aesh.cl.converter.CLConverter;
import org.jboss.aesh.cl.converter.CLConverterManager;
import org.jboss.aesh.cl.exception.OptionParserException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class OptionInt {

    private String shortName;
    private String name;

    private String description;
    private List<String> values;
    private String argument;
    private String defaultValue;
    private Class<?> type;
    private CLConverter converter;
    private OptionType optionType;
    private boolean required = false;
    private char valueSeparator;
    private Map<String,String> properties;

    public OptionInt(char shortName, String name, String description,
                     String argument, boolean required, char valueSeparator,
                     String defaultValue, Class<?> type,
                     OptionType optionType, CLConverter converter) throws OptionParserException {
        this(shortName, name, description, argument, required, valueSeparator, defaultValue,
                type, optionType, (Class<? extends CLConverter>) null);
        this.converter = converter;
    }

    public OptionInt(char shortName, String name, String description,
                     String argument, boolean required, char valueSeparator,
                     String defaultValue, Class<?> type,
                     OptionType optionType, Class<? extends CLConverter> converter) throws OptionParserException {
        this.shortName = String.valueOf(shortName);
        this.name = name;
        this.description = description;
        this.argument = argument;
        this.required = required;
        this.valueSeparator = valueSeparator;
        this.type = type;
        this.defaultValue = defaultValue;
        this.optionType = optionType;
        this.converter = initConverter(converter);

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
        return optionType == OptionType.LIST;
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

    public String getDefaultValue() {
        return defaultValue;
    }

    public void addProperty(String name, String value) {
        properties.put(name,value);
    }

    public Map<String,String> getProperties() {
        return properties;
    }

    public OptionType getOptionType() {
        return optionType;
    }

    public CLConverter<? extends CLConverter> getConverter() {
        return converter;
    }

    public void clear() {
        if(values != null)
            values.clear();
        if(properties != null)
            properties.clear();
    }

    public String getDisplayName() {
        if(name != null) {
            return "--"+ name;
        }
        else
            return "-"+ shortName;
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

        if(converterClass != null && CLConverterManager.getInstance().hasConverter(converterClass)) {
            return CLConverterManager.getInstance().getConverter(converterClass);
        }
        else
            return CLConverterManager.getInstance().getConverter(type);
    }

    public void injectValueIntoField(Object instance, String fieldName) {
        if(converter == null)
            return;
        try {
            Field field = instance.getClass().getDeclaredField(fieldName);
            if(Modifier.isPrivate(field.getModifiers()))
                field.setAccessible(true);
            if(!Modifier.isPublic(instance.getClass().getModifiers())) {
                Constructor constructor = instance.getClass().getDeclaredConstructor();
                if(constructor != null)
                    constructor.setAccessible(true);
            }
            if(optionType == OptionType.NORMAL || optionType == OptionType.BOOLEAN) {
                field.set(instance, converter.convert(getValue()));
            }
            else if(optionType == OptionType.LIST) {
                if(field.getType().isInterface() || Modifier.isAbstract(field.getType().getModifiers())) {
                    if(Set.class.isAssignableFrom(field.getType())) {
                        Set foo = new HashSet<Object>();
                        for(String in : values)
                            foo.add(converter.convert(in));

                        //field.set(instance, new HashSet());
                    }
                    else if(List.class.isAssignableFrom(field.getType())) {
                        List list = new ArrayList();
                        for(String in : values)
                            list.add(converter.convert(in));

                        //field.set(instance, new ArrayList());
                        //todo: should change this
                    }
                    else
                        field.set(instance, new ArrayList());
                }
                else
                    field.set(instance, field.getClass().newInstance());

            }
            else if(optionType == OptionType.GROUP) {

            }
            else if(optionType == OptionType.ARGUMENT) {

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

    @Override
    public String toString() {
        return "OptionInt{" +
                "shortName='" + shortName + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", values=" + values +
                ", argument='" + argument + '\'' +
                ", type=" + type +
                ", required=" + required +
                ", valueSeparator=" + valueSeparator +
                ", properties=" + properties +
                '}';
    }
}
