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
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aesh.command.DefaultValueProvider;
import org.aesh.command.HelpColorScheme;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.converter.Converter;
import org.aesh.command.impl.converter.AeshConverterInvocation;
import org.aesh.command.impl.parser.AeshOptionParser;
import org.aesh.command.impl.validator.AeshValidatorInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.CompletionFallback;
import org.aesh.command.option.OptionVisibility;
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
import org.aesh.terminal.utils.ANSIBuilder;
import org.aesh.util.PropertiesLookup;
import org.aesh.util.ReflectionUtil;

/**
 * @author Aesh team
 */
public class ProcessedOption {

    /**
     * The smallest width a description is wrapped at in the help.
     */
    static final int MIN_WRAP_WIDTH = 20;

    /**
     * Lazily initialized logger: creating it eagerly would make the JDK scan
     * the class path for a logging backend on every start (#782).
     */
    private static final class Log {
        static final Logger LOGGER = Logger.getLogger(ProcessedOption.class.getName());
    }

    private String shortName;
    private String name;

    private String description;
    private List<String> values;
    private List<String> defaultValues;
    private Class<?> type;
    private Converter converter;
    private OptionType optionType;
    private char valueSeparator;
    private String fieldName;
    private OptionCompleter completer;
    private ProcessedCommand parent;
    private int declarationOrder = Integer.MAX_VALUE;

    /**
     * Bit-packed option state: 22 booleans plus the {@link OptionVisibility}
     * and {@link CompletionFallback} enums (two bits each), replacing 22
     * boolean fields and 2 references (~26 bytes per option). The flag
     * accessors are trivially JIT-inlinable (#795).
     */
    private int flags = F_LONG_NAME_USED | F_ANSI_MODE;

    private static final int F_REQUIRED = 1 << 0;
    private static final int F_LONG_NAME_USED = 1 << 1;
    private static final int F_ENDS_WITH_SEPARATOR = 1 << 2;
    private static final int F_OVERRIDE_REQUIRED = 1 << 3;
    private static final int F_ANSI_MODE = 1 << 4;
    private static final int F_CURSOR_OPTION = 1 << 5;
    private static final int F_CURSOR_VALUE = 1 << 6;
    private static final int F_ASK_IF_NOT_SET = 1 << 7;
    private static final int F_ACCEPT_NAME_WITHOUT_DASHES = 1 << 8;
    private static final int F_NEGATABLE = 1 << 9;
    private static final int F_NEGATED_BY_USER = 1 << 10;
    private static final int F_OPTIONAL_VALUE = 1 << 11;
    private static final int F_OPTIONAL_WRAPPED = 1 << 12;
    private static final int F_EXPLICIT_EMPTY = 1 << 13;
    private static final int F_EXPLICITLY_GIVEN = 1 << 14;
    private static final int F_ARRAY_TYPE = 1 << 15;
    private static final int F_VARIABLE_DEFAULT_RESOLVED = 1 << 16;
    private static final int F_VARIABLE_FALLBACK_RESOLVED = 1 << 17;
    private static final int F_INHERITED = 1 << 18;
    private static final int F_IS_URL = 1 << 19;
    private static final int F_INITIAL_VALUE_CAPTURED = 1 << 20;
    private static final int F_DISPLAY_PARTS_COMPUTED = 1 << 21;
    private static final int VISIBILITY_SHIFT = 22;
    private static final int VISIBILITY_MASK = 3 << VISIBILITY_SHIFT;
    private static final int FALLBACK_SHIFT = 24;
    private static final int FALLBACK_MASK = 3 << FALLBACK_SHIFT;
    /**
     * Initial-value factory packed into bits 26-29 (#797): 0 = none (the
     * common case, no OptionExtras allocated), 1..8 = the
     * {@link CollectionFactory} constant with that ordinal + 1, 9 = a custom
     * {@code Supplier} stored in {@link OptionExtras#initialValueFactory}.
     */
    private static final int FACTORY_SHIFT = 26;
    private static final int FACTORY_MASK = 0xF << FACTORY_SHIFT;
    private static final int CUSTOM_FACTORY_CODE = 9;
    /** Cached once: {@code CollectionFactory.values()} clones per call. */
    private static final CollectionFactory[] FACTORY_CONSTANTS = CollectionFactory.values();

    private boolean flag(int bit) {
        return (flags & bit) != 0;
    }

    private void flag(int bit, boolean value) {
        if (value)
            flags |= bit;
        else
            flags &= ~bit;
    }

    /**
     * Lazily allocated holder for rarely populated option fields (#795):
     * most options never set any of these, so they cost one null reference
     * instead of ~26 always-present fields. Getter defaults (emptyList,
     * "no-", {@code NO_OP}, {@code Integer.MAX_VALUE}, ...) live in the
     * accessors, so a null holder and a holder with null fields mean the
     * same thing.
     */
    private OptionExtras extras;

    static final class OptionExtras {
        String argument;
        String paramLabel;
        org.aesh.command.option.Arity arity;
        org.aesh.command.option.IndexRange indexRange;
        Map<String, String> properties;
        OptionValidator validator;
        OptionActivator activator;
        OptionRenderer renderer;
        OptionParser parser;
        SelectorType selectorType;
        String negationPrefix;
        String fallbackValue;
        List<String> rawDefaultValues;
        String rawFallbackValue;
        String descriptionUrl;
        List<String> aliases;
        List<String> exclusiveWith;
        List<String> allowedValues;
        String helpGroup;
        int order = Integer.MAX_VALUE;
        BiConsumer<Object, Object> fieldSetter;
        Consumer<Object> fieldResetter;
        Function<Object, Object> fieldGetter;
        /**
         * Precomputed static help label parts. {@code displayNamePart} holds
         * the option text as rendered before the description (e.g.
         * {@code "-f, --file"}, {@code "    --verbose"},
         * {@code "--[no-]quiet"} or {@code "<input>"}),
         * {@code displayPlaceholderPart} the value suffix including
         * delimiters (e.g. {@code "=<path>"}) or null when the option takes
         * no placeholder. Computed once on first use; any setter that changes
         * label inputs invalidates them via
         * {@link #invalidateDisplayLabelParts()}.
         */
        String displayNamePart;
        String displayPlaceholderPart;
        Supplier<Object> initialValueFactory;
    }

    private OptionExtras extras() {
        OptionExtras e = extras;
        if (e == null) {
            e = new OptionExtras();
            extras = e;
        }
        return e;
    }

    private List<String> aliasesOrEmpty() {
        OptionExtras e = extras;
        return e != null && e.aliases != null ? e.aliases : Collections.emptyList();
    }

    protected FieldAccessor fieldAccessor;
    protected Object initialValue;

    /** Whether the initial field value was captured; see {@link #captureInitialValue(Object)}. */
    boolean isInitialValueCaptured() {
        return flag(F_INITIAL_VALUE_CAPTURED);
    }

    void setInitialValueCaptured(boolean captured) {
        flag(F_INITIAL_VALUE_CAPTURED, captured);
    }

    Supplier<Object> initialValueFactory() {
        int code = (flags & FACTORY_MASK) >>> FACTORY_SHIFT;
        if (code == 0)
            return null;
        if (code < CUSTOM_FACTORY_CODE)
            return FACTORY_CONSTANTS[code - 1];
        OptionExtras e = extras;
        return e != null ? e.initialValueFactory : null;
    }

    private String mixinFieldName;
    private AeshConverterInvocation cachedConverterInvocation;

    /**
     * Seed the initial field value directly, bypassing the accessor read in
     * {@link #captureInitialValue(Object)}. Generated metadata uses this for
     * public/package-private fields it can read without an accessor switch.
     *
     * @param initialValue the initial field value, possibly null
     */
    public void setInitialValue(Object initialValue) {
        this.initialValue = initialValue;
        setInitialValueCaptured(true);
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
        // Rare fields land in the lazy extras holder, and only when the value
        // differs from the accessor default (#795)
        if (argument != null)
            extras().argument = argument;
        flag(F_REQUIRED, required);
        this.valueSeparator = valueSeparator;
        this.type = type;
        this.fieldName = fieldName;
        flag(F_OVERRIDE_REQUIRED, overrideRequired);
        this.optionType = optionType;
        this.converter = converter;
        this.completer = completer;
        if (optionValidator != null)
            extras().validator = optionValidator;
        if (activator != null)
            extras().activator = activator;
        flag(F_ASK_IF_NOT_SET, askIfNotSet);
        flag(F_ACCEPT_NAME_WITHOUT_DASHES, acceptNameWithoutDashes);
        if (selectorType != null && selectorType != SelectorType.NO_OP)
            extras().selectorType = selectorType;
        if (parser != null)
            extras().parser = parser;

        if (renderer != null)
            extras().renderer = renderer;

        if (hasVariableReference(defaultValue)) {
            extras().rawDefaultValues = defaultValue;
            this.defaultValues = PropertiesLookup.checkForSystemVariables(defaultValue);
            flag(F_VARIABLE_DEFAULT_RESOLVED, checkVariableResolved(defaultValue));
        } else {
            this.defaultValues = defaultValue != null ? defaultValue : Collections.emptyList();
        }
        flag(F_NEGATABLE, negatable);
        if (negationPrefix != null)
            extras().negationPrefix = negationPrefix;
        flag(F_INHERITED, inherited);
        if (descriptionUrl != null)
            extras().descriptionUrl = descriptionUrl;
        flag(F_IS_URL, isUrl || "java.net.URL".equals(type.getName()) || "java.net.URI".equals(type.getName()));
        flag(F_OPTIONAL_VALUE, optionalValue);

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

    /**
     * Direct factory for generated (annotation-processor) code using a stateless
     * {@link IndexedFieldAccessor} shared by the metadata provider, eliminating
     * per-option {@link FieldAccessor} allocations (#790).
     */
    public static ProcessedOption createDirect(
            String shortName, String name, String description,
            Class<?> type, String fieldName, OptionType optionType,
            Converter converter, IndexedFieldAccessor indexedAccessor, int accessorIndex) {
        ProcessedOption opt = indexedAccessor != null
                ? new GeneratedProcessedOption(indexedAccessor, accessorIndex)
                : new ProcessedOption();
        opt.shortName = shortName;
        opt.name = name;
        opt.description = description;
        opt.type = type;
        opt.fieldName = fieldName;
        opt.optionType = optionType;
        opt.converter = converter;
        return opt;
    }

    /** No-arg constructor for createDirect() and subclasses. Sets only immutable defaults. */
    protected ProcessedOption() {
        this.valueSeparator = ' ';
        this.values = Collections.emptyList();
        this.defaultValues = Collections.emptyList();
    }

    // --- Setters used by generated code via createDirect() ---

    public void setRequired(boolean required) {
        flag(F_REQUIRED, required);
    }

    /**
     * Change the short name, or drop it with null. Used when a generated
     * option yields its short name to an explicit user option (#770).
     */
    public void setShortName(String shortName) {
        this.shortName = shortName;
        invalidateDisplayLabelParts();
    }

    public void setValueSeparator(char valueSeparator) {
        this.valueSeparator = valueSeparator;
    }

    public void setAskIfNotSet(boolean askIfNotSet) {
        flag(F_ASK_IF_NOT_SET, askIfNotSet);
    }

    public void setAcceptNameWithoutDashes(boolean acceptNameWithoutDashes) {
        flag(F_ACCEPT_NAME_WITHOUT_DASHES, acceptNameWithoutDashes);
        invalidateDisplayLabelParts();
    }

    public void setOverrideRequired(boolean overrideRequired) {
        flag(F_OVERRIDE_REQUIRED, overrideRequired);
    }

    public void setNegatable(boolean negatable) {
        flag(F_NEGATABLE, negatable);
        invalidateDisplayLabelParts();
    }

    public void setNegationPrefix(String negationPrefix) {
        extras().negationPrefix = negationPrefix != null ? negationPrefix : "no-";
        invalidateDisplayLabelParts();
    }

    public void setInherited(boolean inherited) {
        flag(F_INHERITED, inherited);
    }

    public void setDescriptionUrl(String descriptionUrl) {
        if (descriptionUrl != null)
            extras().descriptionUrl = descriptionUrl;
    }

    public void setIsUrl(boolean isUrl) {
        flag(F_IS_URL, isUrl);
    }

    public void setOptionalValue(boolean optionalValue) {
        flag(F_OPTIONAL_VALUE, optionalValue);
        invalidateDisplayLabelParts();
    }

    public void setInitialValueFactory(Supplier<Object> factory) {
        if (factory == null)
            return;
        if (factory instanceof CollectionFactory) {
            // The generated path only ever passes a CollectionFactory
            // constant: pack its ordinal into the free flag bits so the
            // option never allocates an OptionExtras holder (#797)
            flags = (flags & ~FACTORY_MASK)
                    | ((((CollectionFactory) factory).ordinal() + 1) << FACTORY_SHIFT);
            return;
        }
        extras().initialValueFactory = factory;
        flags = (flags & ~FACTORY_MASK) | (CUSTOM_FACTORY_CODE << FACTORY_SHIFT);
    }

    /**
     * Set whether this option's field is wrapped in {@link java.util.Optional}.
     * When true, injected values are wrapped with Optional.of() and reset
     * uses Optional.empty() instead of null.
     */
    public void setOptionalWrapped(boolean optionalWrapped) {
        flag(F_OPTIONAL_WRAPPED, optionalWrapped);
    }

    public boolean isOptionalWrapped() {
        return flag(F_OPTIONAL_WRAPPED);
    }

    public void setFallbackValue(String fallbackValue) {
        OptionExtras e = extras();
        if (fallbackValue != null && fallbackValue.length() > 3
                && fallbackValue.charAt(0) == '$' && fallbackValue.charAt(1) == '{') {
            e.rawFallbackValue = fallbackValue;
            PropertiesLookup.ResolvedValue resolved = PropertiesLookup.resolveWithSource(fallbackValue);
            e.fallbackValue = resolved.value;
            flag(F_VARIABLE_FALLBACK_RESOLVED, resolved.fromVariable);
        } else {
            e.rawFallbackValue = null;
            flag(F_VARIABLE_FALLBACK_RESOLVED, false);
            e.fallbackValue = fallbackValue;
        }
        invalidateDisplayLabelParts();
    }

    public String getFallbackValue() {
        OptionExtras e = extras;
        return e != null ? e.fallbackValue : null;
    }

    /**
     * The declared {@code defaultValue} entries before any {@code ${...}}
     * variable resolution. Non-null only when the declaration contains a
     * variable reference; plain defaults live in {@link #getDefaultValues}.
     * Generated reference docs prefer these so they show the expression
     * (e.g. {@code ${env:API_KEY:-}}) instead of whatever the environment
     * happened to resolve at doc-generation time.
     *
     * @return the raw declared defaults, or null when no variable is involved
     */
    public List<String> getRawDefaultValues() {
        OptionExtras e = extras;
        return e != null ? e.rawDefaultValues : null;
    }

    public boolean hasFallbackValue() {
        return getFallbackValue() != null;
    }

    public void setDefaultValues(List<String> defaultValues) {
        if (defaultValues != null && hasVariableReference(defaultValues)) {
            extras().rawDefaultValues = defaultValues;
            this.defaultValues = PropertiesLookup.checkForSystemVariables(defaultValues);
            flag(F_VARIABLE_DEFAULT_RESOLVED, checkVariableResolved(defaultValues));
        } else {
            OptionExtras e = extras;
            if (e != null)
                e.rawDefaultValues = null;
            flag(F_VARIABLE_DEFAULT_RESOLVED, false);
            this.defaultValues = defaultValues != null ? defaultValues : Collections.emptyList();
        }
    }

    public void setCompleter(OptionCompleter completer) {
        this.completer = completer;
    }

    public void setConverter(Converter converter) {
        this.converter = converter;
    }

    public void setValidator(OptionValidator validator) {
        if (validator != null)
            extras().validator = validator;
    }

    public void setActivator(OptionActivator activator) {
        if (activator != null)
            extras().activator = activator;
    }

    public void setRenderer(OptionRenderer renderer) {
        if (renderer != null)
            extras().renderer = renderer;
    }

    public void setFieldSetter(BiConsumer<Object, Object> fieldSetter) {
        extras().fieldSetter = fieldSetter;
    }

    public void setFieldResetter(Consumer<Object> fieldResetter) {
        extras().fieldResetter = fieldResetter;
    }

    public BiConsumer<Object, Object> getFieldSetter() {
        OptionExtras e = extras;
        return e != null ? e.fieldSetter : null;
    }

    public Consumer<Object> getFieldResetter() {
        OptionExtras e = extras;
        return e != null ? e.fieldResetter : null;
    }

    public void setFieldGetter(Function<Object, Object> fieldGetter) {
        extras().fieldGetter = fieldGetter;
    }

    public Function<Object, Object> getFieldGetter() {
        OptionExtras e = extras;
        return e != null ? e.fieldGetter : null;
    }

    public void setFieldAccessor(FieldAccessor fieldAccessor) {
        this.fieldAccessor = fieldAccessor;
    }

    public FieldAccessor getFieldAccessor() {
        return fieldAccessor;
    }

    public void setAliases(List<String> aliases) {
        extras().aliases = aliases != null ? aliases : Collections.emptyList();
        invalidateDisplayLabelParts();
    }

    public List<String> getAliases() {
        return aliasesOrEmpty();
    }

    public void setHelpGroup(String helpGroup) {
        if (helpGroup != null && !helpGroup.isEmpty())
            extras().helpGroup = helpGroup;
    }

    public String getHelpGroup() {
        OptionExtras e = extras;
        return e != null && e.helpGroup != null ? e.helpGroup : "";
    }

    public void setExclusiveWith(List<String> exclusiveWith) {
        if (exclusiveWith != null && !exclusiveWith.isEmpty())
            extras().exclusiveWith = exclusiveWith;
    }

    public List<String> getExclusiveWith() {
        OptionExtras e = extras;
        return e != null && e.exclusiveWith != null ? e.exclusiveWith : Collections.emptyList();
    }

    public void setAllowedValues(List<String> allowedValues) {
        if (allowedValues != null && !allowedValues.isEmpty())
            extras().allowedValues = allowedValues;
        else {
            OptionExtras e = extras;
            if (e != null)
                e.allowedValues = null;
        }
    }

    public List<String> getAllowedValues() {
        OptionExtras e = extras;
        return e != null && e.allowedValues != null ? e.allowedValues : Collections.emptyList();
    }

    public boolean hasAllowedValues() {
        return !getAllowedValues().isEmpty();
    }

    public void setVisibility(OptionVisibility visibility) {
        int packed;
        switch (visibility != null ? visibility : OptionVisibility.BRIEF) {
            case FULL:
                packed = 1;
                break;
            case HIDDEN:
                packed = 2;
                break;
            case BRIEF:
            default:
                packed = 0;
                break;
        }
        flags = (flags & ~VISIBILITY_MASK) | (packed << VISIBILITY_SHIFT);
    }

    public OptionVisibility getVisibility() {
        switch ((flags & VISIBILITY_MASK) >>> VISIBILITY_SHIFT) {
            case 1:
                return OptionVisibility.FULL;
            case 2:
                return OptionVisibility.HIDDEN;
            default:
                return OptionVisibility.BRIEF;
        }
    }

    public void setOrder(int order) {
        if (order != Integer.MAX_VALUE)
            extras().order = order;
        else {
            OptionExtras e = extras;
            if (e != null)
                e.order = Integer.MAX_VALUE;
        }
    }

    public CompletionFallback getCompleteFallback() {
        int packed = (flags & FALLBACK_MASK) >>> FALLBACK_SHIFT;
        if (packed != 0) {
            switch (packed) {
                case 1:
                    return CompletionFallback.FILES;
                case 2:
                    return CompletionFallback.DIRECTORIES;
                default:
                    return CompletionFallback.NONE;
            }
        }
        if (parent != null && parent.getCompleteFallback() != CompletionFallback.DEFAULT) {
            return parent.getCompleteFallback();
        }
        if (type != null && type.isEnum()) {
            return CompletionFallback.NONE;
        }
        if (type != null && (File.class.isAssignableFrom(type)
                || Path.class.isAssignableFrom(type)
                || Resource.class.isAssignableFrom(type)
                || String.class.isAssignableFrom(type))) {
            return CompletionFallback.FILES;
        }
        return CompletionFallback.NONE;
    }

    public void setCompleteFallback(CompletionFallback completeFallback) {
        int packed;
        switch (completeFallback != null ? completeFallback : CompletionFallback.DEFAULT) {
            case FILES:
                packed = 1;
                break;
            case DIRECTORIES:
                packed = 2;
                break;
            case NONE:
                packed = 3;
                break;
            default:
                packed = 0;
                break;
        }
        flags = (flags & ~FALLBACK_MASK) | (packed << FALLBACK_SHIFT);
    }

    public int getOrder() {
        OptionExtras e = extras;
        return e != null ? e.order : Integer.MAX_VALUE;
    }

    public void setDeclarationOrder(int declarationOrder) {
        this.declarationOrder = declarationOrder;
    }

    public int getDeclarationOrder() {
        return declarationOrder;
    }

    public boolean hasAlias(String name) {
        for (String alias : aliasesOrEmpty()) {
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
            // Support chained mixin paths (e.g., "outer.inner" for nested mixins)
            Object current = commandInstance;
            String[] parts = mixinFieldName.indexOf('.') >= 0
                    ? mixinFieldName.split("\\.")
                    : new String[] { mixinFieldName };
            for (String part : parts) {
                Field mixinField = getField(current.getClass(), part);
                if (mixinField == null)
                    throw new NoSuchFieldException(
                            "Mixin field '" + part + "' not found on " + current.getClass().getName());
                if (!Modifier.isPublic(mixinField.getModifiers()))
                    mixinField.setAccessible(true);
                Object next = mixinField.get(current);
                if (next == null) {
                    next = mixinField.getType().getDeclaredConstructor().newInstance();
                    mixinField.set(current, next);
                }
                current = next;
            }
            return current;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    "Failed to resolve mixin field '" + mixinFieldName + "' on " + commandInstance.getClass().getName(), e);
        }
    }

    /**
     * Captures the current field value from the command instance so it can be
     * restored on reset. Call once after the command is fully constructed.
     * Primitive values are boxed and therefore always captured; reference
     * types are captured only when non-null.
     */
    public void captureInitialValue(Object instance) {
        if (isInitialValueCaptured() || instance == null || fieldName == null)
            return;
        if (fieldAccessor != null) {
            Object value = fieldAccessor.get(instance);
            if (value != null) {
                initialValue = value;
            }
            setInitialValueCaptured(true);
            return;
        }
        Function<Object, Object> getter = getFieldGetter();
        if (getter != null) {
            Object value = getter.apply(instance);
            if (value != null) {
                initialValue = value;
            }
            setInitialValueCaptured(true);
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
            setInitialValueCaptured(true);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Capture failed, will fall back to null on reset
        }
    }

    public void resetField(Object instance) {
        // If we captured a non-null initial value, restore it
        if (isInitialValueCaptured() && initialValue != null) {
            restoreInitialValue(instance);
            return;
        }
        if (fieldAccessor != null) {
            fieldAccessor.set(instance, typeDefault());
            return;
        }
        Consumer<Object> resetter = getFieldResetter();
        if (resetter != null) {
            resetter.accept(instance);
            return;
        }
        BiConsumer<Object, Object> setter = getFieldSetter();
        if (setter != null) {
            setter.accept(instance, typeDefault());
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
                field.set(target, flag(F_OPTIONAL_WRAPPED) ? java.util.Optional.empty() : null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Field reset failed, continue
        }
    }

    protected Object typeDefault() {
        if (flag(F_OPTIONAL_WRAPPED))
            return java.util.Optional.empty();
        if (flag(F_ARRAY_TYPE) || type == null || !type.isPrimitive())
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
            return '\u0000';
        if (type == float.class)
            return 0.0f;
        if (type == double.class)
            return 0.0d;
        return null;
    }

    /**
     * Defensive copy of an array value so resets restore fresh mutable state
     * instead of sharing one array across executions (#697, #704).
     */
    /**
     * A copy of the initial value of a collection, map or array field, with the same elements, so that one execution
     * does not change the initial value of the next (#697, #704). The copy is of the same class when that class has an
     * accessible no-arg constructor, else an ArrayList, LinkedHashSet or LinkedHashMap. Other values are returned as
     * they are.
     */
    @SuppressWarnings("unchecked")
    protected static Object copyInitialValue(Object value) {
        if (value instanceof Collection) {
            Collection<Object> copy;
            try {
                copy = (Collection<Object>) value.getClass().getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException | RuntimeException e) {
                copy = value instanceof Set ? new LinkedHashSet<>() : new ArrayList<>();
            }
            copy.addAll((Collection<Object>) value);
            return copy;
        } else if (value instanceof Map) {
            Map<Object, Object> copy;
            try {
                copy = (Map<Object, Object>) value.getClass().getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException | RuntimeException e) {
                copy = new LinkedHashMap<>();
            }
            copy.putAll((Map<Object, Object>) value);
            return copy;
        } else if (value != null && value.getClass().isArray()) {
            return copyArrayValue(value);
        }
        return value;
    }

    protected static Object copyArrayValue(Object array) {
        int length = Array.getLength(array);
        Object copy = Array.newInstance(array.getClass().getComponentType(), length);
        System.arraycopy(array, 0, copy, 0, length);
        return copy;
    }

    @SuppressWarnings("unchecked")
    protected void restoreInitialValue(Object instance) {
        Object val = copyInitialValue(initialValue);
        BiConsumer<Object, Object> setter = getFieldSetter();
        if (fieldAccessor != null || setter != null) {
            if (fieldAccessor != null)
                fieldAccessor.set(instance, val);
            else
                setter.accept(instance, val);
            return;
        }
        try {
            Object target = resolveMixinInstance(instance);
            Field field = getField(target.getClass(), fieldName);
            if (field == null)
                return;
            if (!Modifier.isPublic(field.getModifiers()))
                field.setAccessible(true);
            field.set(target, val);
        } catch (ReflectiveOperationException e) {
            Consumer<Object> resetter = getFieldResetter();
            if (resetter != null) {
                resetter.accept(instance);
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
        return flag(F_OPTIONAL_VALUE);
    }

    /**
     * Whether the option was explicitly given without values (bare) with
     * {@code optionalValue} set. Populate uses this to inject an empty
     * collection for list options instead of resetting the field (#705).
     */
    public boolean isExplicitlyGiven() {
        return flag(F_EXPLICITLY_GIVEN);
    }

    /**
     * Marks the option as explicitly given on the command line. Called by
     * the parser when the option token is consumed; never by populate,
     * providers, prompts or selectors (#707).
     */
    public void setExplicitlyGiven(boolean explicitlyGiven) {
        flag(F_EXPLICITLY_GIVEN, explicitlyGiven);
    }

    public boolean isExplicitEmpty() {
        return flag(F_EXPLICIT_EMPTY);
    }

    /**
     * Whether the multi-value field is an array rather than a Collection.
     */
    public boolean isArrayType() {
        return flag(F_ARRAY_TYPE);
    }

    public void setArrayType(boolean arrayType) {
        flag(F_ARRAY_TYPE, arrayType);
    }

    public boolean hasMultipleValues() {
        return optionType == OptionType.LIST || optionType == OptionType.ARGUMENTS || optionType == OptionType.GROUP;
    }

    public boolean isRequired() {
        return flag(F_REQUIRED);
    }

    public boolean doOverrideRequired() {
        return flag(F_OVERRIDE_REQUIRED);
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
        OptionExtras e = extras;
        return e != null ? e.argument : null;
    }

    public void setArgument(String argument) {
        if (argument != null)
            extras().argument = argument;
        invalidateDisplayLabelParts();
    }

    public List<String> getDefaultValues() {
        return defaultValues;
    }

    public void addProperty(String name, String value) {
        OptionExtras e = extras();
        if (e.properties == null)
            e.properties = new LinkedHashMap<>();
        e.properties.put(name, value);
    }

    public Map<String, String> getProperties() {
        OptionExtras e = extras;
        return e != null && e.properties != null ? e.properties : Collections.emptyMap();
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
        String label = getParamLabel();
        return label != null && !label.isEmpty() ? label : fieldName;
    }

    public void setParamLabel(String paramLabel) {
        if (paramLabel != null)
            extras().paramLabel = paramLabel;
        invalidateDisplayLabelParts();
    }

    public String getParamLabel() {
        OptionExtras e = extras;
        return e != null ? e.paramLabel : null;
    }

    public void setArity(org.aesh.command.option.Arity arity) {
        if (arity != null)
            extras().arity = arity;
    }

    public void setArity(String aritySpec) {
        if (aritySpec != null && !aritySpec.isEmpty())
            extras().arity = org.aesh.command.option.Arity.parse(aritySpec);
    }

    public org.aesh.command.option.Arity getArity() {
        OptionExtras e = extras;
        return e != null ? e.arity : null;
    }

    public void setIndex(String index) {
        if (index != null && !index.isEmpty())
            extras().indexRange = org.aesh.command.option.IndexRange.parse(index);
    }

    public org.aesh.command.option.IndexRange getIndexRange() {
        OptionExtras e = extras;
        return e != null ? e.indexRange : null;
    }

    public boolean hasIndexRange() {
        return getIndexRange() != null;
    }

    /**
     * Check if the argument has reached its maximum arity.
     * Returns false if no arity is set (legacy unlimited behavior for @Arguments).
     */
    public boolean isArityFull() {
        org.aesh.command.option.Arity a = getArity();
        if (a == null)
            return false;
        return getValues().size() >= a.getMax();
    }

    public Converter converter() {
        return converter;
    }

    public OptionCompleter completer() {
        if (completer == null) {
            if (type == Boolean.class || type == boolean.class) {
                completer = org.aesh.command.impl.completer.BooleanOptionCompleter.INSTANCE;
            } else if (hasAllowedValues()) {
                completer = new org.aesh.command.impl.completer.DefaultValueOptionCompleter(getAllowedValues());
            } else if (type == File.class || type == Resource.class || type == Path.class) {
                completer = new org.aesh.command.impl.completer.FileOptionCompleter();
            }
        }
        return completer;
    }

    public OptionValidator validator() {
        OptionExtras e = extras;
        return e != null ? e.validator : null;
    }

    public OptionActivator activator() {
        OptionExtras e = extras;
        return e != null ? e.activator : null;
    }

    public boolean isActivated(ParsedCommand parsedCommand) {
        OptionActivator a = activator();
        return a == null || a.isActivated(parsedCommand);
    }

    public OptionParser parser() {
        OptionExtras e = extras;
        if (e != null && e.parser != null)
            return e.parser;
        OptionParser created = new AeshOptionParser();
        extras().parser = created;
        return created;
    }

    public void setParser(OptionParser parser) {
        if (parser != null)
            extras().parser = parser;
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
        OptionExtras e = extras;
        return e != null ? e.renderer : null;
    }

    public boolean isLongNameUsed() {
        return flag(F_LONG_NAME_USED);
    }

    public void setLongNameUsed(boolean longNameUsed) {
        flag(F_LONG_NAME_USED, longNameUsed);
    }

    public void setEndsWithSeparator(boolean endsWithSeparator) {
        flag(F_ENDS_WITH_SEPARATOR, endsWithSeparator);
    }

    public boolean getEndsWithSeparator() {
        return flag(F_ENDS_WITH_SEPARATOR);
    }

    public boolean askIfNotSet() {
        return flag(F_ASK_IF_NOT_SET);
    }

    public boolean acceptNameWithoutDashes() {
        return flag(F_ACCEPT_NAME_WITHOUT_DASHES);
    }

    public SelectorType selectorType() {
        OptionExtras e = extras;
        return e != null && e.selectorType != null ? e.selectorType : SelectorType.NO_OP;
    }

    public void setSelectorType(SelectorType selectorType) {
        if (selectorType != null && selectorType != SelectorType.NO_OP)
            extras().selectorType = selectorType;
    }

    public boolean isNegatable() {
        return flag(F_NEGATABLE);
    }

    public String getNegationPrefix() {
        OptionExtras e = extras;
        return e != null && e.negationPrefix != null ? e.negationPrefix : "no-";
    }

    /**
     * Returns the negated form of this option's name.
     * For example, if name is "verbose" and prefix is "no-", returns "no-verbose".
     * Returns null if this option is not negatable.
     */
    public String getNegatedName() {
        return isNegatable() && name != null ? getNegationPrefix() + name : null;
    }

    /**
     * Returns true if this option was specified in its negated form (e.g., --no-verbose).
     */
    public boolean isNegatedByUser() {
        return flag(F_NEGATED_BY_USER);
    }

    /**
     * Sets whether this option was specified in its negated form.
     */
    public void setNegatedByUser(boolean negatedByUser) {
        flag(F_NEGATED_BY_USER, negatedByUser);
    }

    /**
     * Returns true if this option should be inherited by subcommands.
     * Inherited options are automatically available to subcommands when
     * in sub-command mode.
     */
    public boolean isInherited() {
        return flag(F_INHERITED);
    }

    /**
     * Returns the documentation URL for this option's description.
     */
    public String getDescriptionUrl() {
        OptionExtras e = extras;
        return e != null ? e.descriptionUrl : null;
    }

    /**
     * Returns true if this option's value should be treated as a URL.
     */
    public boolean isUrl() {
        return flag(F_IS_URL);
    }

    /**
     * Returns the option value formatted as a hyperlink when appropriate.
     *
     * @param supportsHyperlinks whether the terminal supports OSC 8 hyperlinks
     * @return the value, optionally wrapped in hyperlink escape sequences
     */
    public String getFormattedValue(boolean supportsHyperlinks) {
        String val = getValue();
        if (val != null && isUrl() && supportsHyperlinks) {
            return ANSI.hyperlink(val, val);
        }
        return val;
    }

    public void clear() {
        if (values instanceof ArrayList)
            values.clear();
        OptionExtras e = extras;
        if (e != null && e.properties instanceof HashMap)
            e.properties.clear();
        flag(F_LONG_NAME_USED, true);
        flag(F_ENDS_WITH_SEPARATOR, false);
        flag(F_CURSOR_OPTION, false);
        flag(F_CURSOR_VALUE, false);
        flag(F_NEGATED_BY_USER, false);
        flag(F_EXPLICIT_EMPTY, false);
        flag(F_EXPLICITLY_GIVEN, false);
        resolveVariables();
    }

    /**
     * Re-resolve {@code ${...}} variable references from raw templates (#520)
     * so that env var / system property changes between parse cycles are picked up.
     * Extracted from {@link #clear()} so it can also be called independently
     * when only variable re-resolution is needed without clearing parsed state (#576).
     */
    public void resolveVariables() {
        OptionExtras e = extras;
        if (e == null)
            return;
        if (e.rawDefaultValues != null) {
            defaultValues = PropertiesLookup.checkForSystemVariables(e.rawDefaultValues);
            flag(F_VARIABLE_DEFAULT_RESOLVED, checkVariableResolved(e.rawDefaultValues));
        }
        if (e.rawFallbackValue != null) {
            PropertiesLookup.ResolvedValue resolved = PropertiesLookup.resolveWithSource(e.rawFallbackValue);
            e.fallbackValue = resolved.value;
            flag(F_VARIABLE_FALLBACK_RESOLVED, resolved.fromVariable);
        }
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
        String prefix = acceptNameWithoutDashes() ? "" : "--";
        String text = prefix + name;
        OptionRenderer r = getRenderer();
        if (r == null || !flag(F_ANSI_MODE))
            return new TerminalString(text, true);
        else {
            String hyperlinkUrl = r.getHyperlinkUrl();
            if (hyperlinkUrl != null) {
                return new TerminalString(text, hyperlinkUrl, r.getColor(), r.getTextType());
            }
            return new TerminalString(text, r.getColor(), r.getTextType());
        }
    }

    public List<TerminalString> getRenderedAliasNamesWithDashes() {
        List<String> aliases = aliasesOrEmpty();
        if (aliases.isEmpty())
            return Collections.emptyList();
        String prefix = acceptNameWithoutDashes() ? "" : "--";
        OptionRenderer r = getRenderer();
        List<TerminalString> result = new ArrayList<>(aliases.size());
        for (String alias : aliases) {
            String text = prefix + alias;
            if (r == null || !flag(F_ANSI_MODE))
                result.add(new TerminalString(text, true));
            else
                result.add(new TerminalString(text, r.getColor(), r.getTextType()));
        }
        return result;
    }

    /**
     * Returns the negated form of the option name with dashes for completion.
     * For example, for option "verbose" with prefix "no-", returns "--no-verbose".
     * Returns null if this option is not negatable.
     */
    public TerminalString getRenderedNegatedNameWithDashes() {
        if (!isNegatable() || name == null) {
            return null;
        }
        // Negatable options are boolean, so they don't have a value suffix
        OptionRenderer r = getRenderer();
        if (r == null || !flag(F_ANSI_MODE))
            return new TerminalString("--" + getNegationPrefix() + name, true);
        else
            return new TerminalString("--" + getNegationPrefix() + name, r.getColor(), r.getTextType());
    }

    private void invalidateDisplayLabelParts() {
        flag(F_DISPLAY_PARTS_COMPUTED, false);
    }

    /**
     * Assemble the static help label parts from the current field values.
     * Single source of truth for label text, still byte-identical to
     * building the label inline per render.
     */
    private void ensureDisplayLabelParts() {
        if (flag(F_DISPLAY_PARTS_COMPUTED))
            return;
        OptionExtras e = extras();
        e.displayNamePart = buildDisplayNamePart(shortName, name, aliasesOrEmpty(), isNegatable(), getNegationPrefix(),
                acceptNameWithoutDashes(), optionType, getDisplayLabel());
        String placeholder = resolveValuePlaceholder(getArgument(), optionType,
                type == Boolean.class || type == boolean.class, isOptionalValue(), hasFallbackValue(),
                name, fieldName);
        e.displayPlaceholderPart = placeholder != null ? "=<" + placeholder + ">" : null;
        flag(F_DISPLAY_PARTS_COMPUTED, true);
    }

    private static String buildDisplayNamePart(String shortName, String name, List<String> aliases,
            boolean negatable, String negationPrefix, boolean acceptBare, OptionType optionType,
            String displayLabel) {
        if ((optionType == OptionType.ARGUMENT || optionType == OptionType.ARGUMENTS)
                && (name == null || name.isEmpty()))
            return displayLabel != null && !displayLabel.isEmpty() ? "<" + displayLabel + ">" : "";
        StringBuilder sb = new StringBuilder();
        if (shortName != null)
            sb.append("-").append(shortName);
        else if (name != null && !name.isEmpty()
                && optionType != OptionType.ARGUMENT && optionType != OptionType.ARGUMENTS)
            // Reserve the short-flag gutter ("-x, ") so long-only options
            // align with options that have a short form (#718).
            sb.append("    ");
        if (name != null && !name.isEmpty()) {
            if (shortName != null)
                sb.append(", ");
            String dashPrefix = acceptBare ? "" : "--";
            if (negatable)
                sb.append("--[").append(negationPrefix != null ? negationPrefix : "no-").append("]").append(name);
            else
                sb.append(dashPrefix).append(name);
            if (aliases != null) {
                for (String alias : aliases)
                    sb.append(", ").append(dashPrefix).append(alias);
            }
        }
        return sb.toString();
    }

    /**
     * Returns the value placeholder for this option. If an explicit argument
     * label is set, uses that. Otherwise, for options that accept values
     * (not booleans, not optionalValue, not fallbackValue), derives a
     * placeholder from the option name.
     */
    private static String resolveValuePlaceholder(String argument, OptionType optionType, boolean typeIsBoolean,
            boolean optionalValue, boolean hasFallbackValue, String name, String fieldName) {
        if (argument != null && !argument.isEmpty())
            return argument;
        if (optionType == OptionType.BOOLEAN)
            return null;
        if (optionType == OptionType.ARGUMENT || optionType == OptionType.ARGUMENTS)
            return null;
        if (optionType == OptionType.GROUP)
            return "key=value";
        if (typeIsBoolean)
            return null;
        if (optionType != OptionType.BOOLEAN && !optionalValue && !hasFallbackValue) {
            String label = name != null && !name.isEmpty() ? name : fieldName;
            return label != null && !label.isEmpty() ? label : null;
        }
        return null;
    }

    public int getFormattedLength() {
        ensureDisplayLabelParts();
        OptionExtras e = extras;
        return e.displayNamePart.length()
                + (e.displayPlaceholderPart != null ? e.displayPlaceholderPart.length() : 0);
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
        HelpColorScheme scheme = flag(F_ANSI_MODE) ? HelpColorScheme.resolve(null) : HelpColorScheme.plain();
        return getFormattedOption(offset, descriptionStart, width, supportsHyperlinks, resolvedDescription, scheme);
    }

    public String getFormattedOption(int offset, int descriptionStart, int width, boolean supportsHyperlinks,
            String resolvedDescription, HelpColorScheme colorScheme) {
        ensureDisplayLabelParts();
        boolean ansi = flag(F_ANSI_MODE);
        HelpColorScheme scheme = ansi
                ? (colorScheme != null ? colorScheme : HelpColorScheme.resolve(null))
                : HelpColorScheme.plain();
        StringBuilder sb = new StringBuilder();
        if (offset > 0)
            appendSpaces(sb, offset);
        OptionExtras e = extras;
        // Emit any leading short-flag gutter spaces before ANSI styling so
        // underline/background attributes never bleed onto blank alignment space.
        int leadingSpaces = 0;
        while (leadingSpaces < e.displayNamePart.length() && e.displayNamePart.charAt(leadingSpaces) == ' ') {
            leadingSpaces++;
        }
        if (leadingSpaces > 0) {
            appendSpaces(sb, leadingSpaces);
        }
        String visibleName = leadingSpaces > 0 ? e.displayNamePart.substring(leadingSpaces) : e.displayNamePart;
        // Style option or positional label in one SGR sequence so required
        // emphasis (bold) is never reset by a separate color code (#788).
        if (isBareDisplayLabel()) {
            String labelEscape = ansi
                    ? (flag(F_REQUIRED) ? scheme.requiredParameterEscape() : scheme.parametersEscape())
                    : "";
            HelpColorScheme.appendStyled(sb, labelEscape, visibleName);
        } else {
            String nameEscape = ansi
                    ? (flag(F_REQUIRED) ? scheme.requiredOptionEscape() : scheme.optionsEscape())
                    : "";
            HelpColorScheme.appendStyled(sb, nameEscape, visibleName);
        }
        if (e.displayPlaceholderPart != null) {
            HelpColorScheme.appendStyled(sb, ansi ? scheme.optionParamsEscape() : "", e.displayPlaceholderPart);
        }
        if (resolvedDescription != null && resolvedDescription.length() > 0) {
            int descOffset = descriptionStart - getFormattedLength() - offset;
            if (descOffset > 1) {
                appendSpaces(sb, descOffset);
            } else {
                // Option name fills or exceeds the column: a single space is
                // not enough of a gap, so wrap the description to the next
                // line like any other overflow.
                sb.append(System.lineSeparator());
                appendSpaces(sb, descriptionStart);
            }

            String descriptionUrl = getDescriptionUrl();
            if (supportsHyperlinks && descriptionUrl != null && descriptionUrl.length() > 0) {
                sb.append(ANSI.hyperlink(descriptionUrl, resolvedDescription));
            } else {
                // Multi-line description: strip common leading whitespace (text block support).
                // A line longer than the width is wrapped at a space. Continuation lines are
                // indented to align with the description column.
                String text = resolvedDescription.indexOf('\n') >= 0
                        ? stripIndent(resolvedDescription)
                        : resolvedDescription;
                String pad = spaces(descriptionStart);
                List<String> lines = wrapText(text, width - descriptionStart);
                for (int i = 0; i < lines.size(); i++) {
                    if (i > 0) {
                        sb.append(System.lineSeparator()).append(pad);
                    }
                    sb.append(lines.get(i));
                }
            }
        }

        return sb.toString();
    }

    private boolean isBareDisplayLabel() {
        return (optionType == OptionType.ARGUMENT || optionType == OptionType.ARGUMENTS)
                && (name == null || name.isEmpty()) && !extras.displayNamePart.isEmpty();
    }

    /**
     * Append {@code count} spaces. Replaces {@code String.format} padding in
     * help rendering, which parses a format string per call.
     *
     * @param sb the builder to append to
     * @param count the number of spaces, nothing appended when not positive
     */
    public static void appendSpaces(StringBuilder sb, int count) {
        for (int i = 0; i < count; i++)
            sb.append(' ');
    }

    /**
     * Append {@code count} spaces to a terminal builder. Replaces
     * {@code String.format} padding in help rendering, which parses a format
     * string per call.
     *
     * @param builder the builder to append to
     * @param count the number of spaces, nothing appended when not positive
     */
    public static void appendSpaces(ANSIBuilder builder, int count) {
        for (int i = 0; i < count; i++)
            builder.append(' ');
    }

    /**
     * A string of {@code count} spaces. Replaces {@code String.format}
     * padding in help rendering, which parses a format string per call.
     *
     * @param count the number of spaces, empty string when not positive
     * @return the space string
     */
    public static String spaces(int count) {
        if (count <= 0)
            return "";
        char[] spaces = new char[count];
        Arrays.fill(spaces, ' ');
        return new String(spaces);
    }

    @SuppressWarnings("unchecked")
    public Object doConvert(String inputValue, InvocationProviders invocationProviders,
            Object command, AeshContext aeshContext, boolean doValidation) throws OptionValidatorException {
        List<String> allowed = getAllowedValues();
        if (doValidation && !allowed.isEmpty()
                && !allowed.contains(inputValue) && !allowed.contains(inputValue.toLowerCase())) {
            throw new OptionValidatorException("Invalid value '" + inputValue
                    + "' for option '" + getDisplayName()
                    + "'. Allowed values: " + String.join(", ", allowed));
        }
        // Reuse cached AeshConverterInvocation to avoid per-value allocation (#579)
        if (cachedConverterInvocation == null) {
            cachedConverterInvocation = new AeshConverterInvocation(inputValue, aeshContext);
        } else {
            cachedConverterInvocation.reset(inputValue, aeshContext);
        }
        Object result = converter.convert(
                invocationProviders.getConverterProvider().enhanceConverterInvocation(
                        cachedConverterInvocation));
        OptionValidator v = validator();
        if (v != null && doValidation) {
            v.validate(invocationProviders.getValidatorProvider().enhanceValidatorInvocation(
                    new AeshValidatorInvocation(result, command, aeshContext)));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public void injectValueIntoField(Object instance, InvocationProviders invocationProviders, AeshContext aeshContext,
            boolean doValidation) throws OptionValidatorException {
        if (converter == null || instance == null)
            return;
        if (fieldAccessor != null || getFieldSetter() != null) {
            injectValueWithSetter(instance, invocationProviders, aeshContext, doValidation);
        } else {
            injectValueWithReflection(resolveMixinInstance(instance), invocationProviders, aeshContext, doValidation);
        }
    }

    protected void setField(Object instance, Object value) {
        if (fieldAccessor != null)
            fieldAccessor.set(instance, value);
        else
            getFieldSetter().accept(instance, value);
    }

    protected void injectValueWithSetter(Object instance, InvocationProviders invocationProviders, AeshContext aeshContext,
            boolean doValidation) throws OptionValidatorException {
        if (optionType == OptionType.NORMAL || optionType == OptionType.BOOLEAN || optionType == OptionType.ARGUMENT) {
            // A repeated option keeps its last value; a negated boolean keeps "false" (#733)
            if (getLastValue() != null)
                setField(instance,
                        wrapIfOptional(doConvert(getLastValue(), invocationProviders, instance, aeshContext, doValidation)));
            else if (defaultValues.size() > 0) {
                setField(instance,
                        wrapIfOptional(
                                doConvert(defaultValues.get(0), invocationProviders, instance, aeshContext, doValidation)));
            }
        } else if (optionType == OptionType.LIST || optionType == OptionType.ARGUMENTS) {
            if (flag(F_ARRAY_TYPE)) {
                List<String> strings = values.size() > 0 ? values : defaultValues;
                setField(instance, wrapIfOptional(toArrayValue(strings, invocationProviders, instance, aeshContext,
                        doValidation)));
                return;
            }
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
            setField(instance, wrapIfOptional(tmpSet));
        } else if (optionType == OptionType.GROUP) {
            Map<String, Object> tmpMap = newHashMap();
            Map<String, String> props = getProperties();
            for (String propertyKey : props.keySet())
                tmpMap.put(propertyKey, doConvert(props.get(propertyKey), invocationProviders, instance,
                        aeshContext, doValidation));
            setField(instance, wrapIfOptional(tmpMap));
        }
    }

    /**
     * Wraps a value in Optional.of() if this option's field is Optional-typed.
     */
    private Object wrapIfOptional(Object value) {
        if (flag(F_OPTIONAL_WRAPPED) && value != null)
            return java.util.Optional.of(value);
        return value;
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
            if (optionType == OptionType.NORMAL || optionType == OptionType.BOOLEAN || optionType == OptionType.ARGUMENT) {
                // A repeated option keeps its last value; a negated boolean keeps "false" (#733)
                if (getLastValue() != null)
                    field.set(instance,
                            wrapIfOptional(
                                    doConvert(getLastValue(), invocationProviders, instance, aeshContext, doValidation)));
                else if (defaultValues.size() > 0) {
                    field.set(instance,
                            wrapIfOptional(
                                    doConvert(defaultValues.get(0), invocationProviders, instance, aeshContext, doValidation)));
                }
            } else if (optionType == OptionType.LIST || optionType == OptionType.ARGUMENTS) {
                if (flag(F_ARRAY_TYPE)) {
                    List<String> strings = values.size() > 0 ? values : defaultValues;
                    field.set(instance, wrapIfOptional(toArrayValue(strings, invocationProviders, instance,
                            aeshContext, doValidation)));
                    return;
                }
                Collection<Object> tmpSet = initializeCollection(field);
                if (values.size() > 0) {
                    for (String in : values)
                        tmpSet.add(doConvert(in, invocationProviders, instance, aeshContext, doValidation));
                } else if (defaultValues.size() > 0) {
                    for (String in : defaultValues)
                        tmpSet.add(doConvert(in, invocationProviders, instance, aeshContext, doValidation));
                }

                field.set(instance, wrapIfOptional(tmpSet));
            } else if (optionType == OptionType.GROUP) {
                Map<String, String> props = getProperties();
                if (flag(F_OPTIONAL_WRAPPED) || field.getType().isInterface()
                        || Modifier.isAbstract(field.getType().getModifiers())) {
                    Map<String, Object> tmpMap = newHashMap();
                    for (String propertyKey : props.keySet())
                        tmpMap.put(propertyKey, doConvert(props.get(propertyKey), invocationProviders, instance,
                                aeshContext, doValidation));
                    field.set(instance, wrapIfOptional(tmpMap));
                } else {
                    Map<String, Object> tmpMap = (Map<String, Object>) field.getType().newInstance();
                    for (String propertyKey : props.keySet())
                        tmpMap.put(propertyKey, doConvert(props.get(propertyKey), invocationProviders, instance,
                                aeshContext, doValidation));
                    field.set(instance, tmpMap);
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Failed to inject value into field: " + fieldName, e);
        }
    }

    /**
     * Converts parsed values (or annotation defaults) for a multi-value option
     * whose field is an array. Each string is converted with the option's
     * converter; primitives unbox via {@link Array#set} (#704).
     */
    private Object toArrayValue(List<String> strings, InvocationProviders invocationProviders, Object instance,
            AeshContext aeshContext, boolean doValidation) throws OptionValidatorException {
        Object array = Array.newInstance(type, strings.size());
        for (int i = 0; i < strings.size(); i++)
            Array.set(array, i, doConvert(strings.get(i), invocationProviders, instance, aeshContext, doValidation));
        return array;
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> initializeCollection(Field field) throws IllegalAccessException, InstantiationException {
        // For Optional-wrapped collections, always use ArrayList
        if (flag(F_OPTIONAL_WRAPPED)) {
            if (Set.class.isAssignableFrom(type))
                return new HashSet<>();
            return new ArrayList<>();
        }
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
        OptionActivator current = activator();
        if (current != null)
            extras().activator = invocationProviders.getOptionActivatorProvider().enhanceOptionActivator(current);
    }

    public void updateAnsiMode(boolean ansiMode) {
        flag(F_ANSI_MODE, ansiMode);
    }

    public boolean isAnsiMode() {
        return flag(F_ANSI_MODE);
    }

    private <S, T> Map<S, T> newHashMap() {
        return new LinkedHashMap<>();
    }

    private Field getField(Class clazz, String fieldName) throws NoSuchFieldException {
        // Shared per-class cache (collected with the defining classloader);
        // the single-entry cache it replaces is subsumed by it.
        return ReflectionUtil.findField(clazz, fieldName);
    }

    public Object getFieldValue(Object instance) {
        if (fieldAccessor != null)
            return fieldAccessor.get(instance);
        Function<Object, Object> getter = getFieldGetter();
        if (getter != null)
            return getter.apply(instance);
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
        BiConsumer<Object, Object> setter = getFieldSetter();
        if (setter != null) {
            setter.accept(instance, value);
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
        return flag(F_CURSOR_OPTION);
    }

    public void setCursorOption(boolean cursorOption) {
        flag(F_CURSOR_OPTION, cursorOption);
    }

    public boolean isCursorValue() {
        return flag(F_CURSOR_VALUE);
    }

    public void setCursorValue(boolean cursorValue) {
        flag(F_CURSOR_VALUE, cursorValue);
    }

    public boolean hasDefaultValue() {
        return getDefaultValues() != null && getDefaultValues().size() > 0;
    }

    /**
     * Check if any value in the list contains a variable reference ({@code ${...}}).
     */
    private static boolean hasVariableReference(List<String> values) {
        if (values == null || values.isEmpty())
            return false;
        for (String v : values) {
            if (v != null && v.length() > 3 && v.charAt(0) == '$' && v.charAt(1) == '{')
                return true;
        }
        return false;
    }

    /**
     * Check if any variable reference in the list resolved from the primary
     * variable (not just the :- fallback).
     */
    private static boolean checkVariableResolved(List<String> values) {
        if (values == null || values.isEmpty())
            return false;
        for (String v : values) {
            if (v != null && v.length() > 3 && v.charAt(0) == '$' && v.charAt(1) == '{') {
                PropertiesLookup.ResolvedValue result = PropertiesLookup.resolveWithSource(v);
                if (result.fromVariable)
                    return true;
            }
        }
        return false;
    }

    /**
     * True when the annotation {@code defaultValue} contains a variable
     * reference (e.g., {@code ${env:VAR}}) and the primary variable resolved
     * (the env/sys var was set). Used to give env var defaults priority over
     * {@link DefaultValueProvider} (#521).
     */
    public boolean isVariableDefaultResolved() {
        return flag(F_VARIABLE_DEFAULT_RESOLVED);
    }

    /**
     * True when the annotation {@code fallbackValue} contains a variable
     * reference and the primary variable resolved.
     */
    public boolean isVariableFallbackResolved() {
        return flag(F_VARIABLE_FALLBACK_RESOLVED);
    }

    /**
     * Apply the fallback value chain when this option was specified but has no value.
     * Resolution order (#507):
     * <ol>
     * <li>{@link DefaultValueProvider#fallbackValue} (dynamic, from config/env)</li>
     * <li>Annotation {@code fallbackValue} (static)</li>
     * <li>Annotation {@code defaultValue} (static, legacy fallback)</li>
     * </ol>
     * <p>
     * Only applies when this option has {@code optionalValue=true} and no value
     * has been set yet. This method is called by both the built-in
     * {@link AeshOptionParser} and by the framework after any custom
     * {@link OptionParser} returns without setting a value (#511).
     */
    public void applyOptionalFallback() {
        if (!isOptionalValue() || getValue() != null)
            return;

        // Bare use with optionalValue: the option was explicitly given without
        // values. Record it so populate can tell "given" (empty) from
        // "absent" (reset) (#705).
        flag(F_EXPLICIT_EMPTY, true);

        // Priority 1: Annotation fallbackValue resolved from env/sys var (#521)
        if (flag(F_VARIABLE_FALLBACK_RESOLVED) && hasFallbackValue()) {
            addValue(getFallbackValue());
            return;
        }

        // Priority 2: Provider fallback (dynamic, from config)
        DefaultValueProvider dvp = parent() != null
                ? parent().getDefaultValueProvider()
                : null;
        if (dvp != null) {
            try {
                String providerFallback = dvp.fallbackValue(this);
                if (providerFallback != null) {
                    addValue(providerFallback);
                    return;
                }
            } catch (Exception e) {
                Log.LOGGER.log(Level.WARNING, "DefaultValueProvider.fallbackValue() failed for --" + name(), e);
            }
        }

        // Priority 3: Annotation fallbackValue (static/fallback portion)
        if (hasFallbackValue()) {
            addValue(getFallbackValue());
        } else if (hasDefaultValue()) {
            // Priority 4: Annotation defaultValue (legacy fallback for optionalValue)
            addValue(getDefaultValues().get(0));
        }
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
                ", arguments='" + getArgument() + '\'' +
                ", type=" + type +
                ", required=" + isRequired() +
                ", valueSeparator=" + valueSeparator +
                ", properties=" + getProperties() +
                '}';
    }

    /**
     * The lines of the text, each line wrapped at a space so that it is not longer than the given width. A word longer
     * than the width keeps a line of its own. The lines keep their leading spaces. With a width smaller than
     * 20 the text is not wrapped.
     */
    public static List<String> wrapText(String text, int width) {
        return wrapText(text, width, false);
    }

    /**
     * Wrap text at the given width, keeping explicit line breaks. When
     * {@code preserveIndented} is set, lines starting with whitespace are
     * emitted verbatim (the usual convention for examples and code blocks),
     * so only prose lines are reflowed. Descriptions always wrap every line;
     * headers and footers preserve indented ones.
     *
     * @param text the text to wrap
     * @param width the maximum line width
     * @param preserveIndented when true, leave indented lines untouched
     * @return the wrapped lines
     */
    public static List<String> wrapText(String text, int width, boolean preserveIndented) {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n", -1)) {
            if (preserveIndented && line.length() > 0
                    && (line.charAt(0) == ' ' || line.charAt(0) == '\t')) {
                lines.add(line);
                continue;
            }
            if (width < MIN_WRAP_WIDTH || line.length() <= width) {
                lines.add(line);
                continue;
            }
            int indent = 0;
            while (indent < line.length() && line.charAt(indent) == ' ')
                indent++;
            String lead = line.substring(0, indent);
            StringBuilder current = new StringBuilder(lead);
            for (String word : splitOnSpaceRuns(line.substring(indent))) {
                if (current.length() > indent && current.length() + 1 + word.length() > width) {
                    lines.add(current.toString());
                    current.setLength(0);
                    current.append(lead);
                }
                if (current.length() > indent)
                    current.append(' ');
                current.append(word);
            }
            lines.add(current.toString());
        }
        return lines;
    }

    /**
     * Split on runs of spaces, mirroring {@code split(" +")} with the default
     * limit: only U+0020 separates, runs collapse, a leading empty word is
     * kept, all trailing empty words are dropped (so an all-spaces input
     * yields no words), and the empty string yields one empty word. A plain
     * scanner because the first {@code Pattern} use in a JVM drags in the
     * regex engine plus its internal lambdas, the LambdaMetafactory
     * bootstrap and the Class-File API — ~125 classes on the
     * {@code --help} path (#801).
     */
    static List<String> splitOnSpaceRuns(String text) {
        List<String> words = new ArrayList<>();
        int n = text.length();
        int start = 0;
        int i = 0;
        while (i < n) {
            if (text.charAt(i) == ' ') {
                words.add(text.substring(start, i));
                while (i < n && text.charAt(i) == ' ')
                    i++;
                start = i;
            } else {
                i++;
            }
        }
        words.add(text.substring(start));
        // Mirror split's limit-0 rule: drop every trailing empty word.
        while (!words.isEmpty() && words.get(words.size() - 1).isEmpty())
            words.remove(words.size() - 1);
        if (words.isEmpty() && n == 0)
            words.add(""); // split of the empty string yields one empty word
        return words;
    }

    /**
     * Number of characters outside complete ANSI SGR sequences
     * ({@code ESC [ ;digits* m}), exactly what
     * {@code replaceAll("\u001B\\[[;\\d]*m", "").length()} returns — including
     * the edge cases: an unterminated {@code ESC[} has no match, so all its
     * characters count. A plain scanner so help rendering compiles no
     * {@code Pattern} (#801).
     */
    static int visualLength(String text) {
        int length = 0;
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (c == '\u001B' && i + 1 < n && text.charAt(i + 1) == '[') {
                int j = i + 2;
                while (j < n && (text.charAt(j) == ';' || (text.charAt(j) >= '0' && text.charAt(j) <= '9')))
                    j++;
                if (j < n && text.charAt(j) == 'm') {
                    i = j; // skip the whole SGR sequence, including the 'm'
                    continue;
                }
            }
            length++;
        }
        return length;
    }

    /**
     * Strip common leading whitespace from a multi-line string.
     * Similar to Java 15's String.stripIndent() but compatible with Java 8+.
     * Removes the minimum number of leading spaces found across all non-blank lines.
     */
    static String stripIndent(String text) {
        String[] lines = text.split("\n", -1);
        // Find minimum indentation across non-blank lines
        int minIndent = Integer.MAX_VALUE;
        for (String line : lines) {
            if (line.trim().isEmpty())
                continue;
            int indent = 0;
            while (indent < line.length() && line.charAt(indent) == ' ') {
                indent++;
            }
            if (indent < minIndent) {
                minIndent = indent;
            }
        }
        if (minIndent == 0 || minIndent == Integer.MAX_VALUE) {
            return text;
        }
        // Strip common indent
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0)
                sb.append('\n');
            if (lines[i].length() > minIndent) {
                sb.append(lines[i].substring(minIndent));
            } else {
                sb.append(lines[i].trim());
            }
        }
        return sb.toString();
    }
}
