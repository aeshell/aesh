/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.exception.CommandLineParserException;
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

        //Maps option name to field name
        Map<String, String> fieldMap = new HashMap<String, String>();


        for(Field field : clazz.getDeclaredFields()) {
            Option o;
            OptionGroup og;
            OptionList ol;
            Arguments a;
            boolean hasValue = true;
            if(field.getType().equals(Boolean.class) || field.getType().equals(boolean.class))
                hasValue = false;
            if((o = field.getAnnotation(Option.class)) != null) {
                if(o.name() == null || o.name().length() < 1) {
                    if(o.shortName() == '\u0000') {
                        parameterInt.addOption(field.getName().charAt(0), field.getName(), o.description(),
                                hasValue, o.argument(), o.required(), ',', false, false, o.defaultValue(), field.getType());
                    }
                    else {
                        parameterInt.addOption(o.shortName(), field.getName(), o.description(),
                                hasValue, o.argument(), o.required(), ',', false, false, o.defaultValue(), field.getType());
                    }
                   fieldMap.put(field.getName(), field.getName());

                }
                else {
                    if(o.shortName() == '\u0000') {
                        parameterInt.addOption(o.name().charAt(0), o.name(), o.description(),
                                hasValue, o.argument(), o.required(), ',', false, false, o.defaultValue(), field.getType());
                    }
                    else {
                        parameterInt.addOption(o.shortName(), o.name(), o.description(),
                                hasValue, o.argument(), o.required(), ',', false, false, o.defaultValue(), field.getType());
                    }
                    fieldMap.put(o.name(), field.getName());
                }

            }
            else if((ol = field.getAnnotation(OptionList.class)) != null) {
                if(!Collection.class.isAssignableFrom(field.getType()))
                    throw new CommandLineParserException("OptionGroup field must be instance of Collection");
                if(ol.name() == null || ol.name().length() < 1) {
                    if(ol.shortName() == '\u0000') {
                        parameterInt.addOption(field.getName().charAt(0), field.getName(), ol.description(),
                                hasValue, "", ol.required(), ol.valueSeparator(), false, true, "", field.getType());
                    }
                    else {
                        parameterInt.addOption(ol.shortName(), field.getName(), ol.description(),
                                hasValue, "", ol.required(), ol.valueSeparator(), false, true, "", field.getType());
                    }
                    fieldMap.put(field.getName(), field.getName());
                }
                else {
                    if(ol.shortName() == '\u0000')
                        parameterInt.addOption(ol.name().charAt(0), ol.name(), ol.description(),
                                hasValue, "", ol.required(), ol.valueSeparator(), false, true, "", field.getType());
                    else
                        parameterInt.addOption(ol.shortName(), ol.name(), ol.description(),
                                hasValue, "", ol.required(), ol.valueSeparator(), false, true, "", field.getType());
                    fieldMap.put(ol.name(), field.getName());
                }
            }
            else if((og = field.getAnnotation(OptionGroup.class)) != null) {
                if(!Map.class.isAssignableFrom(field.getType()))
                    throw new CommandLineParserException("OptionGroup field must be instance of Map");
                if(og.name() == null || og.name().length() < 1) {
                    if(og.shortName() == '\u0000') {
                        parameterInt.addOption(field.getName().charAt(0), field.getName(), og.description(),
                                hasValue, "", og.required(), ',', true, true, "", field.getType());
                    }
                    else {
                        parameterInt.addOption(og.shortName(), field.getName(), og.description(),
                                hasValue, "", og.required(), ',', true, true, "", field.getType());
                    }
                    fieldMap.put(field.getName(), field.getName());
                }
                else {
                    if(og.shortName() == '\u0000')
                        parameterInt.addOption(og.name().charAt(0), og.name(), og.description(),
                                hasValue, "", og.required(), ',', true, true, "", field.getType());
                    else
                        parameterInt.addOption(og.shortName(), og.name(), og.description(),
                                hasValue, "", og.required(), ',', true, true, "", field.getType());
                    fieldMap.put(og.name(), field.getName());
                }
            }


            else if((a = field.getAnnotation(Arguments.class)) != null) {
                if(!field.getDeclaringClass().isAssignableFrom(Collection.class))
                    throw new CommandLineParserException("Arguments field must be instance of Collection");
               //TODO:
            }
        }

        return new ParserBuilder().parameter(parameterInt).fieldMap(fieldMap).generateParser();

    }

    private boolean inheritCollection(Class clazz) {
        if(clazz.getSuperclass() == null) {
        }
        return false;
    }

}
