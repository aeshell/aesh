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
package org.aesh.command.impl.populator;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.impl.context.CommandContext;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.ParentCommand;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.console.AeshContext;
import org.aesh.selector.SelectorType;

/**
 * @author Aesh team
 */
public class AeshCommandPopulator<O extends Object, CI extends CommandInvocation> implements CommandPopulator<O, CI> {

    private final O instance;

    public AeshCommandPopulator(O instance) {
        this.instance = instance;
    }

    /**
     * Populate a Command instance with the values parsed from a command line
     * If any parser errors are detected it will throw an exception
     *
     * @param processedCommand command line
     * @param mode do validation or not
     * @throws CommandLineParserException any incorrectness in the parser will abort the populate
     */
    @Override
    public void populateObject(ProcessedCommand<Command<CI>, CI> processedCommand, InvocationProviders invocationProviders,
            AeshContext aeshContext, CommandLineParser.Mode mode)
            throws CommandLineParserException, OptionValidatorException {
        if (processedCommand.parserExceptions().size() > 0 && mode == CommandLineParser.Mode.VALIDATE)
            throw processedCommand.parserExceptions().get(0);
        for (ProcessedOption option : processedCommand.getOptions()) {
            if (option.getValues() != null && option.getValues().size() > 0)
                option.injectValueIntoField(getObject(), invocationProviders, aeshContext,
                        mode == CommandLineParser.Mode.VALIDATE);
            else if (option.getDefaultValues().size() > 0 && option.selectorType() == SelectorType.NO_OP) {
                option.injectValueIntoField(getObject(), invocationProviders, aeshContext,
                        mode == CommandLineParser.Mode.VALIDATE);
            } else if (option.getOptionType().equals(OptionType.GROUP) && option.getProperties().size() > 0)
                option.injectValueIntoField(getObject(), invocationProviders, aeshContext,
                        mode == CommandLineParser.Mode.VALIDATE);
            else
                resetField(getObject(), option.getFieldName(), option.hasValue());
        }
        //arguments
        if (processedCommand.getArguments() != null &&
                (processedCommand.getArguments().getValues().size() > 0 ||
                        processedCommand.getArguments().getDefaultValues().size() > 0))
            processedCommand.getArguments().injectValueIntoField(getObject(), invocationProviders, aeshContext,
                    mode == CommandLineParser.Mode.VALIDATE);
        else if (processedCommand.getArguments() != null)
            resetField(getObject(), processedCommand.getArguments().getFieldName(), true);
        //argument
        if (processedCommand.getArgument() != null &&
                (processedCommand.getArgument().getValues().size() > 0 ||
                        processedCommand.getArgument().getDefaultValues().size() > 0))
            processedCommand.getArgument().injectValueIntoField(getObject(), invocationProviders, aeshContext,
                    mode == CommandLineParser.Mode.VALIDATE);
        else if (processedCommand.getArgument() != null)
            resetField(getObject(), processedCommand.getArgument().getFieldName(), true);
    }

    @Override
    public void populateObject(ProcessedCommand<Command<CI>, CI> processedCommand,
            InvocationProviders invocationProviders,
            AeshContext aeshContext,
            CommandLineParser.Mode mode,
            CommandContext commandContext) throws CommandLineParserException, OptionValidatorException {
        // First, do the standard population
        populateObject(processedCommand, invocationProviders, aeshContext, mode);

        // Then inject values if we have a command context
        if (commandContext != null && commandContext.isInSubCommandMode()) {
            // Inject @ParentCommand fields
            injectParentCommands(commandContext);
            // Inject inherited option values
            injectInheritedValues(processedCommand, commandContext, invocationProviders, aeshContext);
        }
    }

    /**
     * Inject parent command instances into fields annotated with @ParentCommand.
     */
    private void injectParentCommands(CommandContext commandContext) {
        List<Field> fields = getAllFields(getObject().getClass());
        for (Field field : fields) {
            if (field.isAnnotationPresent(ParentCommand.class)) {
                Class<?> fieldType = field.getType();

                // Find matching parent command in context stack
                Command<?> parent = commandContext.getParentCommand(fieldType.asSubclass(Command.class));

                if (parent != null) {
                    try {
                        if (!Modifier.isPublic(field.getModifiers())) {
                            field.setAccessible(true);
                        }
                        field.set(getObject(), parent);
                    } catch (IllegalAccessException e) {
                        // Field injection failed, continue with other fields
                    }
                }
            }
        }
    }

    /**
     * Inject inherited option values from parent commands into subcommand fields.
     * For each inherited option from parent commands, if the subcommand has a field
     * with the same name and compatible type, and that field was not explicitly set
     * by the user, inject the inherited value.
     */
    private void injectInheritedValues(ProcessedCommand<Command<CI>, CI> processedCommand,
            CommandContext commandContext,
            InvocationProviders invocationProviders,
            AeshContext aeshContext) {
        // Get all inherited options from the context
        java.util.Map<String, ProcessedOption> inheritedOptions = commandContext.getAllInheritedOptions();

        if (inheritedOptions.isEmpty()) {
            return;
        }

        // For each option in the current command, check if there's a matching inherited option
        for (ProcessedOption currentOpt : processedCommand.getOptions()) {
            // Only inject if the option was not explicitly set by the user
            if (currentOpt.getValues() == null || currentOpt.getValues().isEmpty()) {
                // Check if there's an inherited option with the same field name or option name
                ProcessedOption inheritedOpt = inheritedOptions.get(currentOpt.getFieldName());
                if (inheritedOpt == null && currentOpt.name() != null) {
                    inheritedOpt = inheritedOptions.get(currentOpt.name());
                }

                if (inheritedOpt != null) {
                    // Get the inherited value from context
                    Object inheritedValue = commandContext.getInheritedValue(currentOpt.getFieldName(), Object.class);
                    if (inheritedValue == null && currentOpt.name() != null) {
                        inheritedValue = commandContext.getInheritedValue(currentOpt.name(), Object.class);
                    }

                    if (inheritedValue != null) {
                        // Inject the inherited value into the current command's field
                        try {
                            Field field = getField(getObject().getClass(), currentOpt.getFieldName());
                            if (field != null) {
                                if (!Modifier.isPublic(field.getModifiers())) {
                                    field.setAccessible(true);
                                }
                                // Check type compatibility (handle primitive types)
                                if (isTypeCompatible(field.getType(), inheritedValue.getClass())) {
                                    field.set(getObject(), inheritedValue);
                                }
                            }
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            // Ignore, continue with other fields
                        }
                    }
                }
            }
        }

        // Also check for argument inheritance
        if (processedCommand.getArgument() != null) {
            ProcessedOption currentArg = processedCommand.getArgument();
            if (currentArg.getValues() == null || currentArg.getValues().isEmpty()) {
                ProcessedOption inheritedArg = inheritedOptions.get(currentArg.getFieldName());
                if (inheritedArg != null) {
                    Object inheritedValue = commandContext.getInheritedValue(currentArg.getFieldName(), Object.class);
                    if (inheritedValue != null) {
                        try {
                            Field field = getField(getObject().getClass(), currentArg.getFieldName());
                            if (field != null) {
                                if (!Modifier.isPublic(field.getModifiers())) {
                                    field.setAccessible(true);
                                }
                                if (isTypeCompatible(field.getType(), inheritedValue.getClass())) {
                                    field.set(getObject(), inheritedValue);
                                }
                            }
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            // Ignore, continue
                        }
                    }
                }
            }
        }
    }

    /**
     * Check if a value type is compatible with a field type, handling primitive types.
     */
    private boolean isTypeCompatible(Class<?> fieldType, Class<?> valueType) {
        if (fieldType.isAssignableFrom(valueType)) {
            return true;
        }
        // Handle primitive type boxing/unboxing
        if (fieldType.isPrimitive()) {
            if (fieldType == boolean.class && valueType == Boolean.class)
                return true;
            if (fieldType == int.class && valueType == Integer.class)
                return true;
            if (fieldType == long.class && valueType == Long.class)
                return true;
            if (fieldType == short.class && valueType == Short.class)
                return true;
            if (fieldType == byte.class && valueType == Byte.class)
                return true;
            if (fieldType == char.class && valueType == Character.class)
                return true;
            if (fieldType == float.class && valueType == Float.class)
                return true;
            if (fieldType == double.class && valueType == Double.class)
                return true;
        }
        return false;
    }

    /**
     * Get all fields from a class and its superclasses.
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                fields.add(field);
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * Will parse the input line and populate the fields in the instance object specified by
     * the given annotations.
     * The instance object must be annotated with the CommandDefinition annotation @see CommandDefinition
     * Any parser errors will throw an exception
     *
     * @param instance command
     * @param fieldName field
     *        public static void parseAndPopulate(Object instance, String line)
     *        throws CommandLineParserException, OptionValidatorException {
     *        CommandLineParser parser = ParserGenerator.generateCommandLineParser(instance.getClass());
     *        AeshCommandPopulator populator = new AeshCommandPopulator(parser);
     *        populator.populateObject(instance, parser.parse(line));
     *        }
     */

    private void resetField(Object instance, String fieldName, boolean hasValue) {
        try {
            Field field = getField(instance.getClass(), fieldName);
            //for some options, the field might be null. eg generatedHelp
            //if so we ignore it
            if (field == null)
                return;
            if (!Modifier.isPublic(field.getModifiers()))
                field.setAccessible(true);
            if (field.getType().isPrimitive()) {
                if (boolean.class.isAssignableFrom(field.getType()))
                    field.set(instance, false);
                else if (int.class.isAssignableFrom(field.getType()))
                    field.set(instance, 0);
                else if (short.class.isAssignableFrom(field.getType()))
                    field.set(instance, 0);
                else if (char.class.isAssignableFrom(field.getType()))
                    field.set(instance, '\u0000');
                else if (byte.class.isAssignableFrom(field.getType()))
                    field.set(instance, 0);
                else if (long.class.isAssignableFrom(field.getType()))
                    field.set(instance, 0L);
                else if (float.class.isAssignableFrom(field.getType()))
                    field.set(instance, 0.0f);
                else if (double.class.isAssignableFrom(field.getType()))
                    field.set(instance, 0.0d);
            } else if (!hasValue && field.getType().equals(Boolean.class)) {
                field.set(instance, Boolean.FALSE);
            } else
                field.set(instance, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Field reset failed, continue
        }
    }

    private Field getField(Class clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException nsfe) {
            if (clazz.getSuperclass() != null)
                return getField(clazz.getSuperclass(), fieldName);
            else
                return null;
        }
    }

    @Override
    public O getObject() {
        return instance;
    }

}
