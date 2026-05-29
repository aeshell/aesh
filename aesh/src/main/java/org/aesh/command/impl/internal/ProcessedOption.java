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

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.converter.Converter;
import org.aesh.command.impl.converter.AeshConverterInvocation;
import org.aesh.command.impl.parser.AeshOptionParser;
import org.aesh.command.impl.validator.AeshValidatorInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.parser.OptionParser;
import org.aesh.command.parser.OptionParserException;
import org.aesh.command.renderer.OptionRenderer;
import org.aesh.command.validator.OptionValidator;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.console.AeshContext;
import org.aesh.io.Resource;
import org.aesh.selector.SelectorType;
import org.aesh.terminal.formatting.TerminalString;
import org.aesh.terminal.utils.ANSI;
import org.aesh.util.PropertiesLookup;

/**
 * @author Aesh team
 */
public class ProcessedOption {

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
    private String paramLabel;
    private org.aesh.command.option.Arity arity;
    private org.aesh.command.option.IndexRange indexRange;
    private OptionCompleter completer;
    private Map<String, String> properties;
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
    private boolean acceptNameWithoutDashes = false;
    private SelectorType selectorType;
    private boolean negatable = false;
    private String negationPrefix = "no-";
    private boolean negatedByUser = false;
    private boolean optionalValue = false;
    private String fallbackValue;
    private boolean inherited = false;
    private String descriptionUrl;
    private boolean isUrl = false;
    private List<String> aliases = Collections.emptyList();
    private String helpGroup = "";
    private List<String> exclusiveWith = Collections.emptyList();
    private List<String> allowedValues = Collections.emptyList();
    private org.aesh.command.option.OptionVisibility visibility = org.aesh.command.option.OptionVisibility.BRIEF;
    private int order = Integer.MAX_VALUE;
    private int declarationOrder = Integer.MAX_VALUE;
    private BiConsumer<Object, Object> fieldSetter;
    private Consumer<Object> fieldResetter;
    private java.util.function.Function<Object, Object> fieldGetter;
    protected FieldAccessor fieldAccessor;
    protected Object initialValue;
    protected boolean initialValueCaptured;
    private String mixinFieldName;
    private Field cachedField;
    private Class<?> cachedFieldClass;

    public ProcessedOption(char shortName, String name, String description,
            String argument, boolean required, char valueSeparator, boolean askIfNotSet, boolean acceptNameWithoutDashes,
            SelectorType selectorType,
            List<String> defaultValue, Class<?> type, String fieldName,
            OptionType optionType, Converter converter, OptionCompleter completer,
            OptionValidator optionValidator,
            OptionActivator activator,
            OptionRenderer renderer, OptionParser parser,
            boolean overrideRequired, boolean negatable, String negationPrefix,
            boolean inherited) throws OptionParserException {
        this(shortName, name, description, argument, required, valueSeparator, askIfNotSet, acceptNameWithoutDashes,
                selectorType, defaultValue, type, fieldName, optionType, converter, completer, optionValidator,
                activator, renderer, parser, overrideRequired, negatable, negationPrefix, inherited, null, false, false);
    }

    public ProcessedOption(char shortName, String name, String description,
            String argument, boolean required, char valueSeparator, boolean askIfNotSet, boolean acceptNameWithoutDashes,
            SelectorType selectorType,
            List<String> defaultValue, Class<?> type, String fieldName,
            OptionType optionType, Converter converter, OptionCompleter completer,
            OptionValidator optionValidator,
            OptionActivator activator,
            OptionRenderer renderer, OptionParser parser,
            boolean overrideRequired, boolean negatable, String negationPrefix,
            boolean inherited, String descriptionUrl, boolean isUrl) throws OptionParserException {
        this(shortName, name, description, argument, required, valueSeparator, askIfNotSet, acceptNameWithoutDashes,
                selectorType, defaultValue, type, fieldName, optionType, converter, completer, optionValidator,
                activator, renderer, parser, overrideRequired, negatable, negationPrefix, inherited, descriptionUrl, isUrl,
                false);
    }

    public ProcessedOption(char shortName, String name, String description,
            String argument, boolean required, char valueSeparator, boolean askIfNotSet, boolean acceptNameWithoutDashes,
            SelectorType selectorType,
            List<String> defaultValue, Class<?> type, String fieldName,
            OptionType optionType, Converter converter, OptionCompleter completer,
            OptionValidator optionValidator,
            OptionActivator activator,
            OptionRenderer renderer, OptionParser parser,
            boolean overrideRequired, boolean negatable, String negationPrefix,
            boolean inherited, String descriptionUrl, boolean isUrl, boolean optionalValue) throws OptionParserException {

        if (shortName != '\u0000')
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
        this.acceptNameWithoutDashes = acceptNameWithoutDashes;
        if (selectorType != null)
            this.selectorType = selectorType;
        else
            this.selectorType = SelectorType.NO_OP;
        this.parser = parser;

        if (renderer != null)
            this.renderer = renderer;

        this.defaultValues = PropertiesLookup.checkForSystemVariables(defaultValue);
        this.negatable = negatable;
        this.negationPrefix = negationPrefix != null ? negationPrefix : "no-";
        this.inherited = inherited;
        this.descriptionUrl = descriptionUrl;
        this.isUrl = isUrl || "java.net.URL".equals(type.getName()) || "java.net.URI".equals(type.getName());
        this.optionalValue = optionalValue;

        properties = java.util.Collections.emptyMap();
        values = java.util.Collections.emptyList();
    }

    /**
     * Direct factory for generated (annotation-processor) code. Bypasses
     * ProcessedOptionBuilder, PropertiesLookup regex, URL type inference,
     * and all validation — those are resolved at compile time.
     *
     * Returns a {@link GeneratedProcessedOption} when a non-null fieldAccessor
     * is provided, which overrides field access methods with reflection-free
     * fast paths. When fieldAccessor is null (e.g. for synthetic help/version
     * options on the reflection path), returns a base ProcessedOption.
     */
    public static ProcessedOption createDirect(
            String shortName, String name, String description,
            Class<?> type, String fieldName, OptionType optionType,
            Converter converter, FieldAccessor fieldAccessor) {
        ProcessedOption opt = fieldAccessor != null
                ? new GeneratedProcessedOption()
                : new ProcessedOption();
        opt.shortName = shortName;
        opt.name = name;
        opt.description = description;
        opt.type = type;
        opt.fieldName = fieldName;
        opt.optionType = optionType;
        opt.converter = converter;
        opt.fieldAccessor = fieldAccessor;
        return opt;
    }

    /** No-arg constructor for createDirect() and subclasses. Sets only immutable defaults. */
    protected ProcessedOption() {
        this.selectorType = SelectorType.NO_OP;
        this.valueSeparator = ' ';
        this.properties = Collections.emptyMap();
        this.values = Collections.emptyList();
        this.defaultValues = Collections.emptyList();
    }

    // --- Setters used by generated code via createDirect() ---

    public void setRequired(boolean required) {
        this.required = required;
    }

    public void setValueSeparator(char valueSeparator) {
        this.valueSeparator = valueSeparator;
    }

    public void setAskIfNotSet(boolean askIfNotSet) {
        this.askIfNotSet = askIfNotSet;
    }

    public void setAcceptNameWithoutDashes(boolean acceptNameWithoutDashes) {
        this.acceptNameWithoutDashes = acceptNameWithoutDashes;
    }

    public void setOverrideRequired(boolean overrideRequired) {
        this.overrideRequired = overrideRequired;
    }

    public void setNegatable(boolean negatable) {
        this.negatable = negatable;
    }

    public void setNegationPrefix(String negationPrefix) {
        this.negationPrefix = negationPrefix != null ? negationPrefix : "no-";
    }

    public void setInherited(boolean inherited) {
        this.inherited = inherited;
    }

    public void setDescriptionUrl(String descriptionUrl) {
        this.descriptionUrl = descriptionUrl;
    }

    public void setIsUrl(boolean isUrl) {
        this.isUrl = isUrl;
    }

    public void setOptionalValue(boolean optionalValue) {
        this.optionalValue = optionalValue;
    }

    public void setFallbackValue(String fallbackValue) {
        this.fallbackValue = fallbackValue;
    }

    public String getFallbackValue() {
        return fallbackValue;
    }

    public boolean hasFallbackValue() {
        return fallbackValue != null;
    }

    public void setDefaultValues(List<String> defaultValues) {
        this.defaultValues = defaultValues != null ? defaultValues : Collections.emptyList();
    }

    public void setCompleter(OptionCompleter completer) {
        this.completer = completer;
    }

    public void setValidator(OptionValidator validator) {
        this.validator = validator;
    }

    public void setActivator(OptionActivator activator) {
        this.activator = activator;
    }

    public void setRenderer(OptionRenderer renderer) {
        this.renderer = renderer;
    }

    public void setFieldSetter(BiConsumer<Object, Object> fieldSetter) {
        this.fieldSetter = fieldSetter;
    }

    public void setFieldResetter(Consumer<Object> fieldResetter) {
        this.fieldResetter = fieldResetter;
    }

    public BiConsumer<Object, Object> getFieldSetter() {
        return fieldSetter;
    }

    public Consumer<Object> getFieldResetter() {
        return fieldResetter;
    }

    public void setFieldGetter(java.util.function.Function<Object, Object> fieldGetter) {
        this.fieldGetter = fieldGetter;
    }

    public java.util.function.Function<Object, Object> getFieldGetter() {
        return fieldGetter;
    }

    public void setFieldAccessor(FieldAccessor fieldAccessor) {
        this.fieldAccessor = fieldAccessor;
    }

    public FieldAccessor getFieldAccessor() {
        return fieldAccessor;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases != null ? aliases : Collections.emptyList();
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setHelpGroup(String helpGroup) {
        this.helpGroup = helpGroup != null ? helpGroup : "";
    }

    public String getHelpGroup() {
        return helpGroup;
    }

    public void setExclusiveWith(List<String> exclusiveWith) {
        this.exclusiveWith = exclusiveWith != null ? exclusiveWith : Collections.emptyList();
    }

    public List<String> getExclusiveWith() {
        return exclusiveWith;
    }

    public void setAllowedValues(List<String> allowedValues) {
        this.allowedValues = allowedValues != null ? allowedValues : Collections.emptyList();
    }

    public List<String> getAllowedValues() {
        return allowedValues;
    }

    public boolean hasAllowedValues() {
        return allowedValues != null && !allowedValues.isEmpty();
    }

    public void setVisibility(org.aesh.command.option.OptionVisibility visibility) {
        this.visibility = visibility != null ? visibility : org.aesh.command.option.OptionVisibility.BRIEF;
    }

    public org.aesh.command.option.OptionVisibility getVisibility() {
        return visibility;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    public void setDeclarationOrder(int declarationOrder) {
        this.declarationOrder = declarationOrder;
    }

    public int getDeclarationOrder() {
        return declarationOrder;
    }

    public boolean hasAlias(String name) {
        for (String alias : aliases) {
            if (alias.equals(name))
                return true;
        }
        return false;
    }

    public void setMixinFieldName(String mixinFieldName) {
        this.mixinFieldName = mixinFieldName;
    }

    public String getMixinFieldName() {
        return mixinFieldName;
    }

    public boolean isMixinOption() {
        return mixinFieldName != null;
    }

    private Object resolveMixinInstance(Object commandInstance) {
        if (mixinFieldName == null)
            return commandInstance;
        try {
            Field mixinField = getField(commandInstance.getClass(), mixinFieldName);
            if (mixinField == null)
                throw new NoSuchFieldException(
                        "Mixin field '" + mixinFieldName + "' not found on " + commandInstance.getClass().getName());
            if (!Modifier.isPublic(mixinField.getModifiers()))
                mixinField.setAccessible(true);
            Object mixinInstance = mixinField.get(commandInstance);
            if (mixinInstance == null) {
                mixinInstance = mixinField.getType().getDeclaredConstructor().newInstance();
                mixinField.set(commandInstance, mixinInstance);
            }
            return mixinInstance;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    "Failed to resolve mixin field '" + mixinFieldName + "' on " + commandInstance.getClass().getName(), e);
        }
    }

    /**
     * Captures the current field value from the command instance so it can be
     * restored on reset. Call once after the command is fully constructed.
     * Only captures non-null reference-type values.
     */
    public void captureInitialValue(Object instance) {
        if (initialValueCaptured || instance == null || fieldName == null)
            return;
        if (type != null && type.isPrimitive()) {
            initialValueCaptured = true;
            return;
        }
        if (fieldAccessor != null) {
            Object value = fieldAccessor.get(instance);
            if (value != null) {
                initialValue = value;
            }
            initialValueCaptured = true;
            return;
        }
        if (fieldGetter != null) {
            Object value = fieldGetter.apply(instance);
            if (value != null) {
                initialValue = value;
            }
            initialValueCaptured = true;
            return;
        }
        try {
            Object target = resolveMixinInstance(instance);
            Field field = getField(target.getClass(), fieldName);
            if (field == null)
                return;
            if (!Modifier.isPublic(field.getModifiers()))
                field.setAccessible(true);
            initialValue = field.get(target);
            initialValueCaptured = true;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Capture failed, will fall back to null on reset
        }
    }

    public void resetField(Object instance) {
        // If we captured a non-null initial value, restore it
        if (initialValueCaptured && initialValue != null) {
            restoreInitialValue(instance);
            return;
        }
        if (fieldAccessor != null) {
            fieldAccessor.set(instance, typeDefault());
            return;
        }
        if (fieldResetter != null) {
            fieldResetter.accept(instance);
            return;
        }
        if (fieldSetter != null) {
            fieldSetter.accept(instance, typeDefault());
            return;
        }
        // Fallback to reflection
        try {
            Object target = resolveMixinInstance(instance);
            Field field = getField(target.getClass(), fieldName);
            if (field == null)
                return;
            if (!Modifier.isPublic(field.getModifiers()))
                field.setAccessible(true);
            if (field.getType().isPrimitive()) {
                if (boolean.class.isAssignableFrom(field.getType()))
                    field.set(target, false);
                else if (int.class.isAssignableFrom(field.getType()))
                    field.set(target, 0);
                else if (short.class.isAssignableFrom(field.getType()))
                    field.set(target, (short) 0);
                else if (char.class.isAssignableFrom(field.getType()))
                    field.set(target, '\u0000');
                else if (byte.class.isAssignableFrom(field.getType()))
                    field.set(target, (byte) 0);
                else if (long.class.isAssignableFrom(field.getType()))
                    field.set(target, 0L);
                else if (float.class.isAssignableFrom(field.getType()))
                    field.set(target, 0.0f);
                else if (double.class.isAssignableFrom(field.getType()))
                    field.set(target, 0.0d);
            } else
                field.set(target, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Field reset failed, continue
        }
    }

    protected Object typeDefault() {
        if (type == null || !type.isPrimitive())
            return null;
        if (type == boolean.class)
            return Boolean.FALSE;
        if (type == int.class)
            return 0;
        if (type == long.class)
            return 0L;
        if (type == short.class)
            return (short) 0;
        if (type == byte.class)
            return (byte) 0;
        if (type == char.class)
            return ' ';
        if (type == float.class)
            return 0.0f;
        if (type == double.class)
            return 0.0d;
        return null;
    }

    @SuppressWarnings("unchecked")
    protected void restoreInitialValue(Object instance) {
        if (fieldAccessor != null || fieldSetter != null) {
            Object val = initialValue;
            if (initialValue instanceof Collection) {
                try {
                    val = initialValue.getClass().getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    val = new ArrayList<>();
                }
            } else if (initialValue instanceof Map) {
                try {
                    val = initialValue.getClass().getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    val = new HashMap<>();
                }
            }
            if (fieldAccessor != null)
                fieldAccessor.set(instance, val);
            else
                fieldSetter.accept(instance, val);
            return;
        }
        try {
            Object target = resolveMixinInstance(instance);
            Field field = getField(target.getClass(), fieldName);
            if (field == null)
                return;
            if (!Modifier.isPublic(field.getModifiers()))
                field.setAccessible(true);
            if (initialValue instanceof Collection) {
                field.set(target, initialValue.getClass().getDeclaredConstructor().newInstance());
            } else if (initialValue instanceof Map) {
                field.set(target, initialValue.getClass().getDeclaredConstructor().newInstance());
            } else {
                field.set(target, initialValue);
            }
        } catch (ReflectiveOperationException e) {
            if (fieldResetter != null) {
                fieldResetter.accept(instance);
            }
        }
    }

    public String shortName() {
        return shortName;
    }

    public String name() {
        return name;
    }

    public void addValue(String value) {
        if (values.isEmpty() && !(values instanceof ArrayList))
            values = new ArrayList<>();
        values.add(value);
    }

    public void addValues(List<String> values) {
        if (this.values.isEmpty() && !(this.values instanceof ArrayList))
            this.values = new ArrayList<>();
        this.values.addAll(values);
    }

    public String getValue() {
        if (values.size() > 0)
            return values.get(0);
        else
            return null;
    }

    public String getLastValue() {
        if (values.size() > 0)
            return values.get(values.size() - 1);
        else
            return null;
    }

    public List<String> getValues() {
        return values;
    }

    public boolean hasValue() {
        return optionType != OptionType.BOOLEAN;
    }

    public boolean isOptionalValue() {
        return optionalValue;
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
        if (properties.isEmpty() && !(properties instanceof HashMap))
            properties = new HashMap<>();
        properties.put(name, value);
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public OptionType getOptionType() {
        return optionType;
    }

    public String getFieldName() {
        return fieldName;
    }

    /**
     * Returns the display label for this argument in help/synopsis.
     * If paramLabel is set, returns that; otherwise returns the fieldName.
     */
    public String getDisplayLabel() {
        return paramLabel != null && !paramLabel.isEmpty() ? paramLabel : fieldName;
    }

    public void setParamLabel(String paramLabel) {
        this.paramLabel = paramLabel;
    }

    public String getParamLabel() {
        return paramLabel;
    }

    public void setArity(org.aesh.command.option.Arity arity) {
        this.arity = arity;
    }

    public void setArity(String aritySpec) {
        if (aritySpec != null && !aritySpec.isEmpty())
            this.arity = org.aesh.command.option.Arity.parse(aritySpec);
    }

    public org.aesh.command.option.Arity getArity() {
        return arity;
    }

    public void setIndex(String index) {
        if (index != null && !index.isEmpty())
            this.indexRange = org.aesh.command.option.IndexRange.parse(index);
    }

    public org.aesh.command.option.IndexRange getIndexRange() {
        return indexRange;
    }

    public boolean hasIndexRange() {
        return indexRange != null;
    }

    /**
     * Check if the argument has reached its maximum arity.
     * Returns false if no arity is set (legacy unlimited behavior for @Arguments).
     */
    public boolean isArityFull() {
        if (arity == null)
            return false;
        return getValues().size() >= arity.getMax();
    }

    public Converter converter() {
        return converter;
    }

    public OptionCompleter completer() {
        if (completer == null && (type == File.class || type == Resource.class || type == Path.class)) {
            completer = new org.aesh.command.impl.completer.FileOptionCompleter();
        }
        return completer;
    }

    public OptionValidator validator() {
        return validator;
    }

    public OptionActivator activator() {
        return activator;
    }

    public boolean isActivated(ParsedCommand parsedCommand) {
        return activator == null || activator.isActivated(parsedCommand);
    }

    public OptionParser parser() {
        if (parser == null)
            parser = new AeshOptionParser();
        return parser;
    }

    public void setParser(OptionParser parser) {
        this.parser = parser;
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

    public boolean acceptNameWithoutDashes() {
        return acceptNameWithoutDashes;
    }

    public SelectorType selectorType() {
        return selectorType;
    }

    public void setSelectorType(SelectorType selectorType) {
        this.selectorType = selectorType != null ? selectorType : SelectorType.NO_OP;
    }

    public boolean isNegatable() {
        return negatable;
    }

    public String getNegationPrefix() {
        return negationPrefix;
    }

    /**
     * Returns the negated form of this option's name.
     * For example, if name is "verbose" and prefix is "no-", returns "no-verbose".
     * Returns null if this option is not negatable.
     */
    public String getNegatedName() {
        return negatable && name != null ? negationPrefix + name : null;
    }

    /**
     * Returns true if this option was specified in its negated form (e.g., --no-verbose).
     */
    public boolean isNegatedByUser() {
        return negatedByUser;
    }

    /**
     * Sets whether this option was specified in its negated form.
     */
    public void setNegatedByUser(boolean negatedByUser) {
        this.negatedByUser = negatedByUser;
    }

    /**
     * Returns true if this option should be inherited by subcommands.
     * Inherited options are automatically available to subcommands when
     * in sub-command mode.
     */
    public boolean isInherited() {
        return inherited;
    }

    /**
     * Returns the documentation URL for this option's description.
     */
    public String getDescriptionUrl() {
        return descriptionUrl;
    }

    /**
     * Returns true if this option's value should be treated as a URL.
     */
    public boolean isUrl() {
        return isUrl;
    }

    /**
     * Returns the option value formatted as a hyperlink when appropriate.
     *
     * @param supportsHyperlinks whether the terminal supports OSC 8 hyperlinks
     * @return the value, optionally wrapped in hyperlink escape sequences
     */
    public String getFormattedValue(boolean supportsHyperlinks) {
        String val = getValue();
        if (val != null && isUrl && supportsHyperlinks) {
            return ANSI.hyperlink(val, val);
        }
        return val;
    }

    public void clear() {
        if (values instanceof ArrayList)
            values.clear();
        if (properties instanceof HashMap)
            properties.clear();
        longNameUsed = true;
        endsWithSeparator = false;
        cursorOption = false;
        cursorValue = false;
        negatedByUser = false;
    }

    public String getDisplayName() {
        if (isLongNameUsed() && name != null) {
            return "--" + name;
        } else if (shortName != null)
            return "-" + shortName;
        else
            return null;
    }

    public TerminalString getRenderedNameWithDashes() {
        String prefix = acceptNameWithoutDashes ? "" : "--";
        String text = prefix + name;
        if (renderer == null || !ansiMode)
            return new TerminalString(text, true);
        else {
            String hyperlinkUrl = renderer.getHyperlinkUrl();
            if (hyperlinkUrl != null) {
                return new TerminalString(text, hyperlinkUrl, renderer.getColor(), renderer.getTextType());
            }
            return new TerminalString(text, renderer.getColor(), renderer.getTextType());
        }
    }

    public List<TerminalString> getRenderedAliasNamesWithDashes() {
        if (aliases.isEmpty())
            return Collections.emptyList();
        List<TerminalString> result = new ArrayList<>(aliases.size());
        for (String alias : aliases) {
            String text = "--" + alias;
            if (renderer == null || !ansiMode)
                result.add(new TerminalString(text, true));
            else
                result.add(new TerminalString(text, renderer.getColor(), renderer.getTextType()));
        }
        return result;
    }

    /**
     * Returns the negated form of the option name with dashes for completion.
     * For example, for option "verbose" with prefix "no-", returns "--no-verbose".
     * Returns null if this option is not negatable.
     */
    public TerminalString getRenderedNegatedNameWithDashes() {
        if (!negatable || name == null) {
            return null;
        }
        // Negatable options are boolean, so they don't have a value suffix
        if (renderer == null || !ansiMode)
            return new TerminalString("--" + negationPrefix + name, true);
        else
            return new TerminalString("--" + negationPrefix + name, renderer.getColor(), renderer.getTextType());
    }

    /**
     * Returns the value placeholder for this option. If an explicit argument
     * label is set, uses that. Otherwise, for options that accept values
     * (not booleans, not optionalValue, not fallbackValue), derives a
     * placeholder from the option name.
     */
    private String getValuePlaceholder() {
        if (argument != null && argument.length() > 0)
            return argument;
        if (optionType == OptionType.BOOLEAN)
            return null;
        if (optionType == OptionType.ARGUMENT || optionType == OptionType.ARGUMENTS)
            return null;
        if (optionType == OptionType.GROUP)
            return "key=value";
        if (type == Boolean.class || type == boolean.class)
            return null;
        if (hasValue() && !isOptionalValue() && !hasFallbackValue()) {
            String label = name != null && !name.isEmpty() ? name : fieldName;
            return label != null && !label.isEmpty() ? label : null;
        }
        return null;
    }

    public int getFormattedLength() {
        StringBuilder sb = new StringBuilder();
        if ((optionType == OptionType.ARGUMENT || optionType == OptionType.ARGUMENTS)
                && (name == null || name.isEmpty())) {
            String label = getDisplayLabel();
            if (label != null && !label.isEmpty())
                sb.append("<").append(label).append(">");
        } else if (shortName != null)
            sb.append("-").append(shortName);
        if (name != null) {
            if (sb.toString().trim().length() > 0)
                sb.append(", ");
            if (negatable) {
                sb.append("--[").append(negationPrefix).append("]").append(name);
            } else {
                sb.append("--").append(name);
            }
            for (String alias : aliases) {
                sb.append(", --").append(alias);
            }
        }
        String placeholder = getValuePlaceholder();
        if (placeholder != null) {
            sb.append("=<").append(placeholder).append(">");
        }

        return sb.length();
    }

    //TODO: add offset, offset for descriptionstart and break on width
    public String getFormattedOption(int offset, int descriptionStart, int width) {
        return getFormattedOption(offset, descriptionStart, width, false);
    }

    public String getFormattedOption(int offset, int descriptionStart, int width, boolean supportsHyperlinks) {
        return getFormattedOption(offset, descriptionStart, width, supportsHyperlinks, description);
    }

    public String getFormattedOption(int offset, int descriptionStart, int width, boolean supportsHyperlinks,
            String resolvedDescription) {
        StringBuilder sb = new StringBuilder();
        if (ansiMode && required)
            sb.append(ANSI.BOLD);
        if (offset > 0)
            sb.append(String.format("%" + offset + "s", ""));
        // Start option name styling
        if (ansiMode)
            sb.append(ANSI.YELLOW_TEXT);
        // For positional arguments (ARGUMENT/ARGUMENTS), show <label> instead of --name
        if ((optionType == OptionType.ARGUMENT || optionType == OptionType.ARGUMENTS)
                && (name == null || name.isEmpty())) {
            String label = getDisplayLabel();
            if (label != null && !label.isEmpty()) {
                if (ansiMode)
                    sb.append(ANSI.CYAN_TEXT);
                sb.append("<").append(label).append(">");
            }
        } else if (shortName != null)
            sb.append("-").append(shortName);
        if (name != null && name.length() > 0) {
            if (shortName != null)
                sb.append(", ");
            if (negatable) {
                sb.append("--[").append(negationPrefix).append("]").append(name);
            } else {
                sb.append("--").append(name);
            }
            for (String alias : aliases) {
                sb.append(", --").append(alias);
            }
        }
        // End option name styling
        if (ansiMode)
            sb.append(ANSI.RESET);
        String placeholder = getValuePlaceholder();
        if (placeholder != null) {
            if (ansiMode)
                sb.append(ANSI.CYAN_TEXT);
            sb.append("=<").append(placeholder).append(">");
            if (ansiMode)
                sb.append(ANSI.RESET);
        }
        if (ansiMode && required)
            sb.append(ANSI.BOLD_OFF);
        if (resolvedDescription != null && resolvedDescription.length() > 0) {
            int descOffset = descriptionStart - getFormattedLength() - offset;
            if (descOffset > 0) {
                sb.append(String.format("%" + descOffset + "s", ""));
            } else {
                // Option name exceeds column width — wrap description to next line
                sb.append(System.lineSeparator());
                sb.append(String.format("%" + descriptionStart + "s", ""));
            }

            if (supportsHyperlinks && descriptionUrl != null && descriptionUrl.length() > 0) {
                sb.append(ANSI.hyperlink(descriptionUrl, resolvedDescription));
            } else {
                // Support multi-line descriptions: indent continuation lines
                if (resolvedDescription.indexOf('\n') >= 0) {
                    String pad = String.format("%" + descriptionStart + "s", "");
                    String[] lines = resolvedDescription.split("\n");
                    for (int i = 0; i < lines.length; i++) {
                        if (i > 0) {
                            sb.append(System.lineSeparator()).append(pad);
                        }
                        sb.append(lines[i]);
                    }
                } else {
                    sb.append(resolvedDescription);
                }
            }
        }

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public Object doConvert(String inputValue, InvocationProviders invocationProviders,
            Object command, AeshContext aeshContext, boolean doValidation) throws OptionValidatorException {
        if (doValidation && hasAllowedValues()
                && !allowedValues.contains(inputValue) && !allowedValues.contains(inputValue.toLowerCase())) {
            throw new OptionValidatorException("Invalid value '" + inputValue
                    + "' for option '" + getDisplayName()
                    + "'. Allowed values: " + String.join(", ", allowedValues));
        }
        Object result = converter.convert(
                invocationProviders.getConverterProvider().enhanceConverterInvocation(
                        new AeshConverterInvocation(inputValue, aeshContext)));
        if (validator != null && doValidation) {
            validator.validate(invocationProviders.getValidatorProvider().enhanceValidatorInvocation(
                    new AeshValidatorInvocation(result, command, aeshContext)));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public void injectValueIntoField(Object instance, InvocationProviders invocationProviders, AeshContext aeshContext,
            boolean doValidation) throws OptionValidatorException {
        if (converter == null || instance == null)
            return;
        if (fieldAccessor != null || fieldSetter != null) {
            injectValueWithSetter(instance, invocationProviders, aeshContext, doValidation);
        } else {
            injectValueWithReflection(resolveMixinInstance(instance), invocationProviders, aeshContext, doValidation);
        }
    }

    private void setField(Object instance, Object value) {
        if (fieldAccessor != null)
            fieldAccessor.set(instance, value);
        else
            fieldSetter.accept(instance, value);
    }

    protected void injectValueWithSetter(Object instance, InvocationProviders invocationProviders, AeshContext aeshContext,
            boolean doValidation) throws OptionValidatorException {
        if (optionType == OptionType.NORMAL || optionType == OptionType.BOOLEAN || optionType == OptionType.ARGUMENT) {
            if (negatedByUser && optionType == OptionType.BOOLEAN) {
                setField(instance, doConvert("false", invocationProviders, instance, aeshContext, doValidation));
            } else if (getValue() != null)
                setField(instance, doConvert(getValue(), invocationProviders, instance, aeshContext, doValidation));
            else if (defaultValues.size() > 0) {
                setField(instance,
                        doConvert(defaultValues.get(0), invocationProviders, instance, aeshContext, doValidation));
            }
        } else if (optionType == OptionType.LIST || optionType == OptionType.ARGUMENTS) {
            Collection<Object> tmpSet;
            if (Set.class.isAssignableFrom(type))
                tmpSet = new HashSet<>();
            else
                tmpSet = new ArrayList<>();
            if (values.size() > 0) {
                for (String in : values)
                    tmpSet.add(doConvert(in, invocationProviders, instance, aeshContext, doValidation));
            } else if (defaultValues.size() > 0) {
                for (String in : defaultValues)
                    tmpSet.add(doConvert(in, invocationProviders, instance, aeshContext, doValidation));
            }
            setField(instance, tmpSet);
        } else if (optionType == OptionType.GROUP) {
            Map<String, Object> tmpMap = newHashMap();
            for (String propertyKey : properties.keySet())
                tmpMap.put(propertyKey, doConvert(properties.get(propertyKey), invocationProviders, instance,
                        aeshContext, doValidation));
            setField(instance, tmpMap);
        }
    }

    private void injectValueWithReflection(Object instance, InvocationProviders invocationProviders, AeshContext aeshContext,
            boolean doValidation) throws OptionValidatorException {
        try {
            Field field = getField(instance.getClass(), fieldName);
            //for some options, the field might be null. eg generatedHelp
            //if so we ignore it
            if (field == null)
                return;
            if (!Modifier.isPublic(field.getModifiers()))
                field.setAccessible(true);
            if (!Modifier.isPublic(instance.getClass().getModifiers())) {
                Constructor constructor = instance.getClass().getDeclaredConstructor();
                if (constructor != null)
                    constructor.setAccessible(true);
            }
            if (optionType == OptionType.NORMAL || optionType == OptionType.BOOLEAN || optionType == OptionType.ARGUMENT) {
                // Handle negatable options - when used in negated form (e.g., --no-verbose), inject false
                if (negatedByUser && optionType == OptionType.BOOLEAN) {
                    field.set(instance, doConvert("false", invocationProviders, instance, aeshContext, doValidation));
                } else if (getValue() != null)
                    field.set(instance, doConvert(getValue(), invocationProviders, instance, aeshContext, doValidation));
                else if (defaultValues.size() > 0) {
                    field.set(instance,
                            doConvert(defaultValues.get(0), invocationProviders, instance, aeshContext, doValidation));
                }
            } else if (optionType == OptionType.LIST || optionType == OptionType.ARGUMENTS) {
                Collection<Object> tmpSet = initializeCollection(field);
                if (values.size() > 0) {
                    for (String in : values)
                        tmpSet.add(doConvert(in, invocationProviders, instance, aeshContext, doValidation));
                } else if (defaultValues.size() > 0) {
                    for (String in : defaultValues)
                        tmpSet.add(doConvert(in, invocationProviders, instance, aeshContext, doValidation));
                }

                field.set(instance, tmpSet);
            } else if (optionType == OptionType.GROUP) {
                if (field.getType().isInterface() || Modifier.isAbstract(field.getType().getModifiers())) {
                    Map<String, Object> tmpMap = newHashMap();
                    for (String propertyKey : properties.keySet())
                        tmpMap.put(propertyKey, doConvert(properties.get(propertyKey), invocationProviders, instance,
                                aeshContext, doValidation));
                    field.set(instance, tmpMap);
                } else {
                    Map<String, Object> tmpMap = (Map<String, Object>) field.getType().newInstance();
                    for (String propertyKey : properties.keySet())
                        tmpMap.put(propertyKey, doConvert(properties.get(propertyKey), invocationProviders, instance,
                                aeshContext, doValidation));
                    field.set(instance, tmpMap);
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InstantiationException e) {
            throw new RuntimeException("Failed to inject value into field: " + fieldName, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> initializeCollection(Field field) throws IllegalAccessException, InstantiationException {
        if (field.getType().isInterface() || Modifier.isAbstract(field.getType().getModifiers())) {
            if (Set.class.isAssignableFrom(field.getType()))
                return new HashSet<>();
            else if (List.class.isAssignableFrom(field.getType()))
                return new ArrayList<>();
            else
                return null;
        } else
            return (Collection) field.getType().newInstance();
    }

    public void updateInvocationProviders(InvocationProviders invocationProviders) {
        if (activator != null)
            activator = invocationProviders.getOptionActivatorProvider().enhanceOptionActivator(activator);
    }

    public void updateAnsiMode(boolean ansiMode) {
        this.ansiMode = ansiMode;
    }

    public boolean isAnsiMode() {
        return ansiMode;
    }

    private <S, T> Map<S, T> newHashMap() {
        return new HashMap<>();
    }

    private Field getField(Class clazz, String fieldName) throws NoSuchFieldException {
        if (fieldName == null || fieldName.isEmpty())
            return null;
        if (cachedField != null && cachedFieldClass == clazz)
            return cachedField;
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getName().equals(fieldName)) {
                    cachedField = f;
                    cachedFieldClass = clazz;
                    return f;
                }
            }
        }
        return null;
    }

    public Object getFieldValue(Object instance) {
        if (fieldAccessor != null)
            return fieldAccessor.get(instance);
        if (fieldGetter != null)
            return fieldGetter.apply(instance);
        try {
            Object target = resolveMixinInstance(instance);
            Field field = getField(target.getClass(), fieldName);
            if (field == null)
                return null;
            if (!Modifier.isPublic(field.getModifiers()))
                field.setAccessible(true);
            return field.get(target);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    public void setFieldValue(Object instance, Object value) {
        if (fieldAccessor != null) {
            fieldAccessor.set(instance, value);
            return;
        }
        if (fieldSetter != null) {
            fieldSetter.accept(instance, value);
            return;
        }
        try {
            Object target = resolveMixinInstance(instance);
            Field field = getField(target.getClass(), fieldName);
            if (field == null)
                return;
            if (!Modifier.isPublic(field.getModifiers()))
                field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // best-effort
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

    public boolean isTypeAssignableByResourcesOrFile() {
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
