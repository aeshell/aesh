/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.OptionInt;
import org.jboss.aesh.cl.internal.OptionType;
import org.jboss.aesh.cl.internal.ParameterInt;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates a {@link CommandLineParser} based on annotations defined in
 * the specified class.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParserGenerator {

    public static CommandLineParser generateCommandLineParser(Object paramInstance) throws CommandLineParserException {
        return generateCommandLineParser(paramInstance.getClass());
    }

    public static CommandLineParser generateCommandLineParser(Class clazz) throws CommandLineParserException {
        Command command = (Command) clazz.getAnnotation(Command.class);
        if(command == null)
            throw new CommandLineParserException("Commands must be annotated with @Command");

        ParameterInt parameterInt = new ParameterInt(command.name(), command.description());

        for(Field field : clazz.getDeclaredFields()) {
            Option o;
            OptionGroup og;
            OptionList ol;
            Arguments a;
            boolean hasValue = true;
            if(field.getType().equals(Boolean.class) || field.getType().equals(boolean.class))
                hasValue = false;
            if((o = field.getAnnotation(Option.class)) != null) {
                OptionType optionType;
                if(hasValue)
                    optionType = OptionType.NORMAL;
                else
                    optionType = OptionType.BOOLEAN;
                if(o.name() == null || o.name().length() < 1) {
                    if(o.shortName() == '\u0000') {
                        parameterInt.addOption(field.getName().charAt(0), field.getName(), o.description(),
                                o.argument(), o.required(), ',', o.defaultValue(), field.getType(), field.getName(),
                                optionType, o.converter());
                    }
                    else {
                        parameterInt.addOption(o.shortName(), field.getName(), o.description(),
                                o.argument(), o.required(), ',', o.defaultValue(),
                                field.getType(), field.getName(), optionType, o.converter());
                    }

                }
                else {
                    if(o.shortName() == '\u0000') {
                        parameterInt.addOption(o.name().charAt(0), o.name(), o.description(),
                                o.argument(), o.required(), ',', o.defaultValue(),
                                field.getType(), field.getName(), optionType, o.converter());
                    }
                    else {
                        parameterInt.addOption(o.shortName(), o.name(), o.description(),
                                o.argument(), o.required(), ',', o.defaultValue(),
                                field.getType(), field.getName(), optionType, o.converter());
                    }
                }

            }
            else if((ol = field.getAnnotation(OptionList.class)) != null) {
                if(!Collection.class.isAssignableFrom(field.getType()))
                    throw new CommandLineParserException("OptionGroup field must be instance of Collection");
                Class type = Object.class;
                if(field.getGenericType() != null) {
                    ParameterizedType listType = (ParameterizedType) field.getGenericType();
                    type = (Class) listType.getActualTypeArguments()[0];
                }
                if(ol.name() == null || ol.name().length() < 1) {
                    if(ol.shortName() == '\u0000') {
                        parameterInt.addOption(field.getName().charAt(0), field.getName(), ol.description(),
                                "", ol.required(), ol.valueSeparator(), "", type, field.getName(), OptionType.LIST,
                                ol.converter());
                    }
                    else {
                        parameterInt.addOption(ol.shortName(), field.getName(), ol.description(), "",
                                ol.required(), ol.valueSeparator(), "", type, field.getName(), OptionType.LIST,
                                ol.converter());
                    }
                }
                else {
                    if(ol.shortName() == '\u0000')
                        parameterInt.addOption(ol.name().charAt(0), ol.name(), ol.description(), "",
                                ol.required(), ol.valueSeparator(), "", type, field.getName(),
                                OptionType.LIST, ol.converter());
                    else
                        parameterInt.addOption(ol.shortName(), ol.name(), ol.description(), "",
                                ol.required(), ol.valueSeparator(), "", type, field.getName(), OptionType.LIST,
                                ol.converter());
                }
            }
            else if((og = field.getAnnotation(OptionGroup.class)) != null) {
                if(!Map.class.isAssignableFrom(field.getType()))
                    throw new CommandLineParserException("OptionGroup field must be instance of Map");
                Class type = Object.class;
                if(field.getGenericType() != null) {
                    ParameterizedType listType = (ParameterizedType) field.getGenericType();
                    type = (Class) listType.getActualTypeArguments()[1];
                }
                if(og.name() == null || og.name().length() < 1) {
                    if(og.shortName() == '\u0000') {
                        parameterInt.addOption(field.getName().charAt(0), field.getName(), og.description(),
                                "", og.required(), ',', "", type, field.getName(), OptionType.GROUP,
                                og.converter());
                    }
                    else {
                        parameterInt.addOption(og.shortName(), field.getName(), og.description(),
                                "", og.required(), ',', "", type, field.getName(), OptionType.GROUP,
                                og.converter());
                    }
                }
                else {
                    if(og.shortName() == '\u0000')
                        parameterInt.addOption(og.name().charAt(0), og.name(), og.description(),
                                "", og.required(), ',', "", type, field.getName(), OptionType.GROUP,
                                og.converter());
                    else
                        parameterInt.addOption(og.shortName(), og.name(), og.description(),
                                "", og.required(), ',', "", type, field.getName(), OptionType.GROUP,
                                og.converter());
                }
            }

            else if((a = field.getAnnotation(Arguments.class)) != null) {
                if(!Collection.class.isAssignableFrom(field.getType()))
                    throw new CommandLineParserException("Arguments field must be instance of Collection");
                Class type = Object.class;
                if(field.getGenericType() != null) {
                    ParameterizedType listType = (ParameterizedType) field.getGenericType();
                    type = (Class) listType.getActualTypeArguments()[0];
                }
                parameterInt.setArgument(new OptionInt('\u0000',"", a.description(), "", false, a.valueSeparator(),
                        "", type, field.getName(), OptionType.ARGUMENT, a.converter()));
            }
        }

        return new ParserBuilder().parameter(parameterInt).generateParser();
    }

    private boolean inheritCollection(Class clazz) {
        if(clazz.getSuperclass() == null) {
        }
        return false;
    }

}
