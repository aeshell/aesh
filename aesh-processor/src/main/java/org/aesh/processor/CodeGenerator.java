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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.processor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.aesh.command.CommandDefinition;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Mixin;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionGroup;
import org.aesh.command.option.OptionList;
import org.aesh.command.option.ParentCommand;

/**
 * Generates Java source code for {@code CommandMetadataProvider} implementations.
 *
 * @author Aesh team
 */
final class CodeGenerator {

    // Sentinel class names — when annotation value equals these, skip the builder call
    private static final String NULL_COMMAND_VALIDATOR = "org.aesh.command.impl.validator.NullCommandValidator";
    private static final String NULL_RESULT_HANDLER = "org.aesh.command.impl.result.NullResultHandler";
    private static final String NULL_COMMAND_ACTIVATOR = "org.aesh.command.impl.activator.NullCommandActivator";
    private static final String NULL_OPTION_COMPLETER = "org.aesh.command.impl.completer.NullOptionCompleter";
    private static final String NULL_CONVERTER = "org.aesh.command.impl.converter.NullConverter";
    private static final String NULL_VALIDATOR = "org.aesh.command.impl.validator.NullValidator";
    private static final String NULL_ACTIVATOR = "org.aesh.command.impl.activator.NullActivator";
    private static final String NULL_OPTION_RENDERER = "org.aesh.command.impl.renderer.NullOptionRenderer";
    private static final String AESH_OPTION_PARSER = "org.aesh.command.impl.parser.AeshOptionParser";
    private static final String NULL_DEFAULT_VALUE_PROVIDER = "org.aesh.command.impl.provider.NullDefaultValueProvider";
    private static final String NULL_HELP_SECTION_PROVIDER = "org.aesh.command.impl.provider.NullHelpSectionProvider";

    private CodeGenerator() {
    }

    /**
     * Count the number of options (Option, OptionList, OptionGroup) that will be
     * added to the ProcessedCommand, including those from Mixin fields.
     * Used to pre-size the options ArrayList (#575).
     */
    private static int countOptions(List<VariableElement> fields, Elements elementUtils, Types typeUtils) {
        int count = 0;
        for (VariableElement field : fields) {
            if (field.getAnnotation(Option.class) != null
                    || field.getAnnotation(OptionList.class) != null
                    || field.getAnnotation(OptionGroup.class) != null) {
                count++;
            } else if (field.getAnnotation(Mixin.class) != null) {
                TypeMirror mixinType = field.asType();
                if (mixinType instanceof DeclaredType) {
                    TypeElement mixinElement = (TypeElement) ((DeclaredType) mixinType).asElement();
                    count += countMixinOptions(mixinElement);
                }
            }
        }
        return count;
    }

    /**
     * Whether any field (or any field inside a {@code @Mixin}, including
     * superclasses) maps to an option: such a provider implements
     * {@code IndexedFieldAccessor} and needs the shared accessor local (#792).
     */
    private static boolean hasOptionFields(List<VariableElement> fields) {
        for (VariableElement field : fields) {
            if (hasAnnotation(field))
                return true;
            if (field.getAnnotation(Mixin.class) != null) {
                TypeMirror mixinType = field.asType();
                if (mixinType instanceof DeclaredType
                        && hasMixinOptionFields((TypeElement) ((DeclaredType) mixinType).asElement())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasMixinOptionFields(TypeElement typeElement) {
        for (javax.lang.model.element.Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed instanceof VariableElement) {
                VariableElement field = (VariableElement) enclosed;
                if (hasAnnotation(field))
                    return true;
                if (field.getAnnotation(Mixin.class) != null) {
                    TypeMirror mixinType = field.asType();
                    if (mixinType instanceof DeclaredType
                            && hasMixinOptionFields((TypeElement) ((DeclaredType) mixinType).asElement())) {
                        return true;
                    }
                }
            }
        }
        TypeMirror superclass = typeElement.getSuperclass();
        if (superclass.getKind() != TypeKind.NONE && !superclass.toString().equals("java.lang.Object")) {
            if (superclass instanceof DeclaredType) {
                return hasMixinOptionFields((TypeElement) ((DeclaredType) superclass).asElement());
            }
        }
        return false;
    }

    /**
     * If the given type is an enum, returns its constant names (lowercased).
     * Returns null if the type is not an enum or constants cannot be resolved.
     * Used to auto-populate allowedValues at compile time (#591).
     */
    private static String[] getEnumConstantNames(TypeMirror fieldType, Types typeUtils) {
        if (fieldType.getKind() != TypeKind.DECLARED)
            return null;
        javax.lang.model.element.Element element = typeUtils.asElement(fieldType);
        if (element == null || element.getKind() != javax.lang.model.element.ElementKind.ENUM)
            return null;
        TypeElement enumElement = (TypeElement) element;
        List<String> constants = new ArrayList<>();
        for (javax.lang.model.element.Element enclosed : enumElement.getEnclosedElements()) {
            if (enclosed.getKind() == javax.lang.model.element.ElementKind.ENUM_CONSTANT) {
                constants.add(enclosed.getSimpleName().toString().toLowerCase());
            }
        }
        return constants.toArray(new String[0]);
    }

    /**
     * Check if a field has a custom converter (not the default NullConverter).
     */
    private static boolean hasCustomConverter(VariableElement field, Elements elementUtils) {
        String className = getFieldAnnotationClassValue(field, "converter", elementUtils);
        return className != null && !className.equals(NULL_CONVERTER);
    }

    private static int countMixinOptions(TypeElement typeElement) {
        int count = 0;
        for (javax.lang.model.element.Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed instanceof VariableElement) {
                VariableElement field = (VariableElement) enclosed;
                if (field.getAnnotation(Option.class) != null
                        || field.getAnnotation(OptionList.class) != null
                        || field.getAnnotation(OptionGroup.class) != null) {
                    count++;
                } else if (field.getAnnotation(Mixin.class) != null) {
                    TypeMirror mixinType = field.asType();
                    if (mixinType instanceof DeclaredType) {
                        count += countMixinOptions((TypeElement) ((DeclaredType) mixinType).asElement());
                    }
                }
            }
        }
        // Recurse into superclass
        TypeMirror superclass = typeElement.getSuperclass();
        if (superclass.getKind() != javax.lang.model.type.TypeKind.NONE
                && !superclass.toString().equals("java.lang.Object")) {
            if (superclass instanceof DeclaredType) {
                count += countMixinOptions((TypeElement) ((DeclaredType) superclass).asElement());
            }
        }
        return count;
    }

    static String generate(
            String packageName, String simpleName, String metadataClassName,
            String qualifiedCommandName, TypeElement commandElement,
            List<VariableElement> fields, boolean isGroup,
            Elements elementUtils, Types typeUtils) {

        StringBuilder sb = new StringBuilder();

        // Package
        if (!packageName.isEmpty()) {
            sb.append("package ").append(packageName).append(";\n\n");
        }

        // Imports
        sb.append("import org.aesh.command.Command;\n");
        sb.append("import org.aesh.command.impl.internal.ProcessedCommand;\n");
        sb.append("import org.aesh.command.impl.internal.ProcessedOption;\n");
        sb.append("import org.aesh.command.impl.internal.GeneratedOptionLookup;\n");
        sb.append("import org.aesh.command.impl.internal.IndexedFieldAccessor;\n");
        sb.append("import org.aesh.command.impl.internal.OptionType;\n");
        sb.append("import org.aesh.command.metadata.CommandMetadataProvider;\n");
        sb.append("import org.aesh.command.parser.CommandLineParserException;\n\n");

        // Buffer class body so we know which optional interfaces (IndexedFieldAccessor,
        // GeneratedOptionLookup, ParentCommandInjector) this provider implements (#790).
        StringBuilder body = new StringBuilder();

        generatePrivateFieldConstants(body, simpleName, qualifiedCommandName, fields);
        boolean generateHelp = "true".equals(getAnnotationValue(commandElement, "generateHelp", elementUtils));
        String version = getAnnotationValue(commandElement, "version", elementUtils);
        boolean needsGeneratedBoolean = generateHelp || (version != null && !version.isEmpty());
        java.util.LinkedHashSet<String> sharedConverters = collectSharedConverterTypes(
                fields, elementUtils, typeUtils, needsGeneratedBoolean);

        // commandType()
        body.append("    @Override\n");
        body.append("    public Class<").append(simpleName).append("> commandType() {\n");
        body.append("        return ").append(simpleName).append(".class;\n");
        body.append("    }\n\n");

        // newInstance()
        body.append("    @Override\n");
        body.append("    public ").append(simpleName).append(" newInstance() {\n");
        if (hasAccessibleNoArgConstructor(commandElement)) {
            body.append("        return new ").append(simpleName).append("();\n");
        } else {
            // No accessible no-arg constructor: applications pass their own
            // instances to buildProcessedCommand (#785).
            body.append("        throw new UnsupportedOperationException(\"").append(simpleName)
                    .append(" has no accessible no-arg constructor; pass a command instance to buildProcessedCommand\");\n");
        }
        body.append("    }\n\n");

        // isGroupCommand()
        body.append("    @Override\n");
        body.append("    public boolean isGroupCommand() {\n");
        body.append("        return ").append(isGroup).append(";\n");
        body.append("    }\n\n");

        // groupCommandClasses()
        if (isGroup) {
            generateGroupCommandClasses(body, commandElement, elementUtils);
        } else {
            body.append("    @Override\n");
            body.append("    public Class<? extends Command>[] groupCommandClasses() {\n");
            body.append("        return new Class[0];\n");
            body.append("    }\n\n");
        }

        // commandName() — read name via annotation mirror to avoid MirroredTypesException
        // (which can occur when @CommandDefinition has Class[] attributes like groupCommands)
        String cmdName = getAnnotationValue(commandElement, "name", elementUtils);
        body.append("    @Override\n");
        body.append("    public String commandName() {\n");
        body.append("        return ").append(stringLiteral(cmdName)).append(";\n");
        body.append("    }\n\n");

        // commandAliases()
        String[] aliases = getAnnotationStringArrayValue(commandElement, "aliases", elementUtils);
        if (aliases != null && aliases.length > 0) {
            // Cached: registration reads aliases per lookup, callers never mutate (#791)
            body.append("    private static final String[] COMMAND_ALIASES = new String[] {");
            for (int i = 0; i < aliases.length; i++) {
                if (i > 0)
                    body.append(", ");
                body.append(stringLiteral(aliases[i]));
            }
            body.append("};\n");
            body.append("    @Override\n");
            body.append("    public String[] commandAliases() {\n");
            body.append("        return COMMAND_ALIASES;\n");
            body.append("    }\n\n");
        }

        // groupCommandNamesAndAliases() — pre-computed child names/aliases (#575)
        if (isGroup) {
            generateGroupCommandNamesAndAliases(body, commandElement, elementUtils);
        }

        // groupCommandHelpEntries() — pre-computed child name/description/helpGroup (#675)
        if (isGroup) {
            generateGroupCommandHelpEntries(body, commandElement, elementUtils);
        }

        // Collect field accessor and option lookup info
        List<FieldAccessorInfo> accessorInfos = new java.util.ArrayList<>();

        // Collect @ParentCommand fields for type-directed ancestor injection.
        List<FieldAccessorInfo> parentInfos = new java.util.ArrayList<>();
        for (VariableElement field : fields) {
            if (field.getAnnotation(ParentCommand.class) != null) {
                parentInfos.add(new FieldAccessorInfo(-1, field.getSimpleName().toString(),
                        erasedTypeName(field.asType(), typeUtils), null, isPrivateField(field), simpleName));
            }
        }

        // buildProcessedCommand() / buildHelpProcessedCommand() delegate to a
        // single shared build() so every option's metadata is emitted once
        // (#792). Execution-only work (converters, callbacks, field
        // accessors, initial values, lookup wiring) is guarded at runtime by
        // !forHelp, preserving the help-path isolation (#675): a guarded
        // branch never executes, and verification alone initializes nothing.
        body.append("    @Override\n");
        body.append("    public ProcessedCommand buildProcessedCommand(").append(simpleName).append(" instance)");
        body.append(" throws CommandLineParserException {\n");
        body.append("        return build(instance, false);\n");
        body.append("    }\n\n");

        body.append("    @Override\n");
        body.append("    public ProcessedCommand buildHelpProcessedCommand()");
        body.append(" throws CommandLineParserException {\n");
        body.append("        return build(null, true);\n");
        body.append("    }\n\n");

        body.append("    private ProcessedCommand build(").append(simpleName).append(" instance, boolean forHelp)");
        body.append(" throws CommandLineParserException {\n");
        generateBuildProcessedCommand(body, simpleName, commandElement,
                fields, isGroup, elementUtils, typeUtils, accessorInfos, parentInfos, sharedConverters);
        body.append("    }\n\n");

        boolean hasRealFields = hasRealFieldAccessors(accessorInfos);
        boolean hasLookup = hasLookupOptions(accessorInfos);
        boolean hasParents = !parentInfos.isEmpty();

        if (hasRealFields) {
            generateIndexedFieldAccessorMethods(body, accessorInfos);
        }
        if (hasParents) {
            generateParentInjectorMethods(body, simpleName, parentInfos);
        }
        if (hasLookup) {
            generateOptionLookupMethods(body, accessorInfos);
        }

        // Class declaration
        sb.append("/**\n");
        sb.append(" * Generated compile-time metadata provider for {@link ").append(qualifiedCommandName).append("}.\n");
        sb.append(" * Do not edit — this file is regenerated by the aesh annotation processor.\n");
        sb.append(" */\n");
        sb.append("@SuppressWarnings({\"unchecked\", \"rawtypes\"})\n");
        sb.append("public final class ").append(metadataClassName);
        sb.append(" implements CommandMetadataProvider<").append(simpleName).append(">");
        if (hasRealFields)
            sb.append(", IndexedFieldAccessor");
        if (hasLookup)
            sb.append(", GeneratedOptionLookup");
        if (hasParents)
            sb.append(", org.aesh.command.impl.internal.ParentCommandInjector");
        sb.append(" {\n\n");

        sb.append(body);
        sb.append("}\n");

        return sb.toString();
    }

    private static void generateGroupCommandClasses(StringBuilder sb, TypeElement commandElement, Elements elementUtils) {
        // Extract groupCommands() from @CommandDefinition via annotation mirror
        // because accessing it directly would trigger MirroredTypesException at compile time.
        List<String> groupClasses = getGroupCommandClassNames(commandElement, elementUtils);
        if (groupClasses.isEmpty()) {
            sb.append("    @Override\n");
            sb.append("    public Class<? extends Command>[] groupCommandClasses() {\n");
            sb.append("        return new Class[0];\n");
            sb.append("    }\n\n");
            return;
        }
        // Cached: container builds read the group classes per execution fork,
        // and callers only iterate (#791).
        sb.append("    private static final Class[] GROUP_CLASSES = new Class[] {\n");
        for (int i = 0; i < groupClasses.size(); i++) {
            sb.append("        ").append(groupClasses.get(i)).append(".class");
            if (i < groupClasses.size() - 1)
                sb.append(",");
            sb.append("\n");
        }
        sb.append("    };\n\n");
        sb.append("    @Override\n");
        sb.append("    public Class<? extends Command>[] groupCommandClasses() {\n");
        sb.append("        return GROUP_CLASSES;\n");
        sb.append("    }\n\n");
    }

    /**
     * Generate the groupCommandNamesAndAliases() override for group commands.
     * Returns a String[][] where each entry is {primaryName, alias1, alias2, ...},
     * index-aligned with groupCommandClasses(). This avoids per-child
     * MetadataProviderRegistry lookups at runtime (#575).
     */
    private static void generateGroupCommandNamesAndAliases(StringBuilder sb, TypeElement commandElement,
            Elements elementUtils) {
        List<String> groupClasses = getGroupCommandClassNames(commandElement, elementUtils);
        if (groupClasses.isEmpty())
            return;

        // Cached: the fast registration path reads this table on every
        // container build (per execution fork), and callers only iterate (#791).
        sb.append("    private static final String[][] GROUP_NAMES_AND_ALIASES = new String[][] {\n");
        for (int i = 0; i < groupClasses.size(); i++) {
            String childClassName = groupClasses.get(i);
            TypeElement childElement = elementUtils.getTypeElement(childClassName);
            String childName = null;
            String[] childAliases = null;
            if (childElement != null) {
                childName = getAnnotationValue(childElement, "name", elementUtils);
                childAliases = getAnnotationStringArrayValue(childElement, "aliases", elementUtils);
            }
            sb.append("            {");
            if (childName != null) {
                sb.append(stringLiteral(childName));
            } else {
                // Fallback: use simple class name lowercased (shouldn't happen with valid @CommandDefinition)
                String simpleName = childClassName.contains(".")
                        ? childClassName.substring(childClassName.lastIndexOf('.') + 1)
                        : childClassName;
                sb.append(stringLiteral(simpleName.toLowerCase()));
            }
            if (childAliases != null) {
                for (String alias : childAliases) {
                    sb.append(", ").append(stringLiteral(alias));
                }
            }
            sb.append("}");
            if (i < groupClasses.size() - 1)
                sb.append(",");
            sb.append("\n");
        }
        sb.append("    };\n\n");
        sb.append("    @Override\n");
        sb.append("    public String[][] groupCommandNamesAndAliases() {\n");
        sb.append("        return GROUP_NAMES_AND_ALIASES;\n");
        sb.append("    }\n\n");
    }

    /**
     * Generate the groupCommandHelpEntries() override for group commands.
     * Returns a String[][] where each entry is {name, description, helpGroup},
     * index-aligned with groupCommandClasses(). Lazy help listings render
     * sibling subcommands from these literals without building full child
     * metadata (#675).
     */
    private static void generateGroupCommandHelpEntries(StringBuilder sb, TypeElement commandElement,
            Elements elementUtils) {
        List<String> groupClasses = getGroupCommandClassNames(commandElement, elementUtils);
        if (groupClasses.isEmpty())
            return;

        // Cached: lazy help listings render from these literals and callers
        // only iterate (#791).
        sb.append("    private static final String[][] GROUP_HELP_ENTRIES = new String[][] {\n");
        for (int i = 0; i < groupClasses.size(); i++) {
            String childClassName = groupClasses.get(i);
            TypeElement childElement = elementUtils.getTypeElement(childClassName);
            String childName = null;
            String childDescription = "";
            String childHelpGroup = "";
            if (childElement != null) {
                childName = getAnnotationValue(childElement, "name", elementUtils);
                String desc = getAnnotationValue(childElement, "description", elementUtils);
                if (desc != null)
                    childDescription = desc;
                String group = getAnnotationValue(childElement, "helpGroup", elementUtils);
                if (group != null)
                    childHelpGroup = group;
            }
            if (childName == null) {
                // Fallback: use simple class name lowercased (shouldn't happen with valid @CommandDefinition)
                String simpleName = childClassName.contains(".")
                        ? childClassName.substring(childClassName.lastIndexOf('.') + 1)
                        : childClassName;
                childName = simpleName.toLowerCase();
            }
            sb.append("            {").append(stringLiteral(childName)).append(", ")
                    .append(stringLiteral(childDescription)).append(", ")
                    .append(stringLiteral(childHelpGroup)).append("}");
            if (i < groupClasses.size() - 1)
                sb.append(",");
            sb.append("\n");
        }
        sb.append("    };\n\n");
        sb.append("    @Override\n");
        sb.append("    public String[][] groupCommandHelpEntries() {\n");
        sb.append("        return GROUP_HELP_ENTRIES;\n");
        sb.append("    }\n\n");
    }

    @SuppressWarnings("unchecked")
    private static List<String> getGroupCommandClassNames(TypeElement element, Elements elementUtils) {
        List<String> classNames = new java.util.ArrayList<>();
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            String annotationType = ((TypeElement) mirror.getAnnotationType().asElement())
                    .getQualifiedName().toString();
            if (annotationType.equals(CommandDefinition.class.getCanonicalName())) {
                for (java.util.Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror
                        .getElementValues().entrySet()) {
                    if (entry.getKey().getSimpleName().toString().equals("groupCommands")) {
                        List<? extends AnnotationValue> values = (List<? extends AnnotationValue>) entry.getValue()
                                .getValue();
                        for (AnnotationValue av : values) {
                            String childName = groupChildName(av.getValue(), elementUtils);
                            if (childName != null) {
                                classNames.add(childName);
                            }
                        }
                    }
                }
            }
        }
        return classNames;
    }

    /**
     * Qualified name for one {@code groupCommands} entry. Class literals
     * normally arrive as {@link TypeMirror}; javac may hand back a qualified
     * name string for not-yet-completed nested types, resolved when possible.
     * Unresolvable markers ("&lt;error&gt;") are left for javac to report.
     */
    private static String groupChildName(Object value, Elements elementUtils) {
        if (value instanceof TypeMirror) {
            return value.toString();
        }
        if (!(value instanceof String)) {
            return null;
        }
        String name = (String) value;
        if (name.isEmpty() || name.startsWith("<")) {
            return null;
        }
        TypeElement resolved = elementUtils.getTypeElement(name);
        if (resolved == null) {
            int lastDot = name.lastIndexOf('.');
            if (lastDot > 0) {
                // Binary-name fallback for nested classes ("a.Outer$Inner").
                resolved = elementUtils.getTypeElement(
                        name.substring(0, lastDot) + '$' + name.substring(lastDot + 1));
            }
        }
        return resolved == null ? null : resolved.getQualifiedName().toString();
    }

    private static class FieldAccessorInfo {
        final int index;
        final String fieldName;
        final String fieldType;
        final String mixinFieldName;
        /**
         * Whether the generated accessor must go through reflection: true for
         * private fields, for fields the generated class cannot name directly
         * (e.g. a package-visible mixin option in another package), and for
         * options of a private {@code @Mixin} field (#785). The
         * {@code Fields} constants and the try/catch wrapper are driven off
         * this flag.
         */
        final boolean needsReflection;
        final String commandSimpleName;
        /** True for synthetic fields (help/version) that store state in the Accessor itself. */
        final boolean synthetic;
        boolean lookupOption;
        String shortName;
        String optionName;
        String[] optionAliases;
        String negatedName;
        boolean acceptBareName;

        FieldAccessorInfo(int index, String fieldName, String fieldType, String mixinFieldName,
                boolean needsReflection, String commandSimpleName) {
            this(index, fieldName, fieldType, mixinFieldName, needsReflection, commandSimpleName, false);
        }

        FieldAccessorInfo(int index, String fieldName, String fieldType, String mixinFieldName,
                boolean needsReflection, String commandSimpleName, boolean synthetic) {
            this.index = index;
            this.fieldName = fieldName;
            this.fieldType = fieldType;
            this.mixinFieldName = mixinFieldName;
            this.needsReflection = needsReflection;
            this.commandSimpleName = commandSimpleName;
            this.synthetic = synthetic;
        }
    }

    private static void setLookupOption(FieldAccessorInfo info, String shortName, String name,
            String[] aliases, String negatedName, boolean acceptBareName) {
        info.lookupOption = true;
        info.shortName = shortName;
        info.optionName = name;
        info.optionAliases = aliases;
        info.negatedName = negatedName;
        info.acceptBareName = acceptBareName;
    }

    private static void generateBuildProcessedCommand(StringBuilder sb, String simpleName,
            TypeElement commandElement,
            List<VariableElement> fields, boolean isGroup,
            Elements elementUtils, Types typeUtils,
            List<FieldAccessorInfo> accessorInfos, List<FieldAccessorInfo> parentInfos,
            java.util.LinkedHashSet<String> sharedConverters) {

        boolean generateHelp;
        String version;
        // Read all annotation values via annotation mirrors to avoid MirroredTypesException
        // (which occurs when annotations have Class[] attributes like groupCommands)
        String annotationName = CommandDefinition.class.getCanonicalName();
        generateHelp = "true".equals(getAnnotationValue(commandElement, "generateHelp", elementUtils));
        version = getAnnotationValue(commandElement, "version", elementUtils);
        if (version == null)
            version = "";

        // Direct construction: no builder, no EnumSet, no default-value setter
        // churn, no double resolveHelpToggles (#791).
        boolean disableParsing = !isGroup && "true".equals(
                getAnnotationValue(commandElement, "disableParsing", elementUtils));
        boolean stopAtFirstPositional = "true".equals(
                getAnnotationValue(commandElement, "stopAtFirstPositional", elementUtils));
        boolean sortOptions = "true".equals(
                getAnnotationValue(commandElement, "sortOptions", elementUtils));
        String validatorExpr = commandCallbackExpression(commandElement, "validator",
                NULL_COMMAND_VALIDATOR, elementUtils);
        String resultHandlerExpr = commandCallbackExpression(commandElement, "resultHandler",
                NULL_RESULT_HANDLER, elementUtils);

        // Pre-count options so the ArrayList is pre-sized, avoiding resizing (#575)
        int optionCount = countOptions(fields, elementUtils, typeUtils);
        if (generateHelp)
            optionCount++;
        if (!version.isEmpty())
            optionCount++;

        sb.append("        ProcessedCommand processedCommand = ProcessedCommand.createDirect(\n");
        sb.append("                ").append(stringLiteral(
                getAnnotationValue(commandElement, "name", elementUtils))).append(",\n");
        sb.append("                ").append(stringLiteral(
                getAnnotationValue(commandElement, "description", elementUtils))).append(",\n");
        sb.append("                ").append(forHelpGuard("instance")).append(",\n");
        sb.append("                ").append(validatorExpr).append(", ").append(resultHandlerExpr).append(",\n");
        sb.append("                ")
                .append(disableParsing).append(", ").append(stopAtFirstPositional).append(", ")
                .append(sortOptions).append(",\n");
        sb.append("                ").append(optionCount).append(");\n\n");

        // Non-default command attributes, set directly instead of builder-chained (#791)
        emitCommandCallbackSetter(sb, "setActivator", commandElement, "activator",
                NULL_COMMAND_ACTIVATOR, elementUtils);
        String[] aliases = getAnnotationStringArrayValue(commandElement, "aliases", elementUtils);
        if (aliases != null && aliases.length > 0) {
            sb.append("        processedCommand.setAliases(").append(listExpression(aliases)).append(");\n\n");
        }
        emitCommandCallbackSetter(sb, "setDefaultValueProvider", commandElement, "defaultValueProvider",
                NULL_DEFAULT_VALUE_PROVIDER, elementUtils);
        String helpUrl = getAnnotationValue(commandElement, "helpUrl", elementUtils);
        if (helpUrl != null && !helpUrl.isEmpty()) {
            sb.append("        processedCommand.setHelpUrl(").append(stringLiteral(helpUrl)).append(");\n\n");
        }
        emitHelpToggleSources(sb, commandElement, elementUtils);

        // Resolve shared converters as method locals, gated by forHelp so the
        // help path resolves none (#675). Class-level statics would warm every
        // converter type at provider class-init — including for help listings
        // that never parse a value.
        for (String type : sharedConverters) {
            sb.append("        org.aesh.command.converter.Converter converter_")
                    .append(converterConstantSuffix(type))
                    .append(" = forHelp ? null : org.aesh.converter.CLConverterManager.getInstance().getConverter(")
                    .append(type).append(".class);\n");
        }
        if (!sharedConverters.isEmpty())
            sb.append("\n");
        // One gated accessor reference shared by every option (#792): help
        // builds pass null so options get no field accessor at all. Emitted
        // only when the provider implements IndexedFieldAccessor, i.e. when
        // it has option fields; otherwise there are no call sites.
        if (hasOptionFields(fields)) {
            sb.append("        org.aesh.command.impl.internal.IndexedFieldAccessor accessor = forHelp ? null : this;\n\n");
        }

        // Set command-level completeFallback if not DEFAULT (#565)
        String cmdCompleteFallback = getAnnotationValue(commandElement, "completeFallback", elementUtils);
        org.aesh.command.option.CompletionFallback commandFallback = org.aesh.command.option.CompletionFallback.DEFAULT;
        if (cmdCompleteFallback != null && !cmdCompleteFallback.isEmpty()) {
            try {
                commandFallback = org.aesh.command.option.CompletionFallback.valueOf(cmdCompleteFallback);
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (commandFallback != org.aesh.command.option.CompletionFallback.DEFAULT) {
            sb.append("        processedCommand.setCompleteFallback(org.aesh.command.option.CompletionFallback.")
                    .append(commandFallback.name()).append(");\n\n");
        }

        // Set command-level helpGroup if present
        String cmdHelpGroup = getAnnotationValue(commandElement, "helpGroup", elementUtils);
        if (cmdHelpGroup != null && !cmdHelpGroup.isEmpty()) {
            sb.append("        processedCommand.setHelpGroup(")
                    .append(stringLiteral(cmdHelpGroup)).append(");\n\n");
        }

        String[] header = getAnnotationStringArrayValue(commandElement, "header", elementUtils);
        if (header != null && header.length > 0) {
            sb.append("        processedCommand.setHelpHeader(")
                    .append(stringLiteral(String.join("\n", header))).append(");\n\n");
        }
        String[] footer = getAnnotationStringArrayValue(commandElement, "footer", elementUtils);
        if (footer != null && footer.length > 0) {
            sb.append("        processedCommand.setHelpFooter(")
                    .append(stringLiteral(String.join("\n", footer))).append(");\n\n");
        }

        // Set helpSectionProvider if not NullHelpSectionProvider
        String providerClass = getAnnotationClassValue(commandElement, annotationName,
                "helpSectionProvider", elementUtils);
        if (providerClass != null && !providerClass.equals(NULL_HELP_SECTION_PROVIDER)) {
            sb.append("        processedCommand.setHelpSectionProvider(new ")
                    .append(providerClass).append("());\n\n");
        }

        // Generate help/version options first to match reflection path declaration order
        // (reflection: constructor calls doGenerateHelp/doGenerateVersion before processCommand adds user options)
        if (generateHelp) {
            FieldAccessorInfo helpInfo = new FieldAccessorInfo(-1, "generatedHelp", "boolean",
                    null, false, simpleName, true);
            setLookupOption(helpInfo, "h", "help", null, null, false);
            accessorInfos.add(helpInfo);
            sb.append("        {\n");
            sb.append("            ProcessedOption helpOpt = ProcessedOption.createDirect(\n");
            sb.append("                    \"h\", \"help\", \"Display this help and exit\",\n");
            sb.append("                    Boolean.class, \"generatedHelp\", OptionType.BOOLEAN,\n");
            sb.append(
                    "                    converter_java_lang_Boolean, forHelp ? null : new org.aesh.command.impl.internal.SyntheticBooleanAccessor());\n");
            sb.append("            helpOpt.setOverrideRequired(true);\n");
            sb.append("            processedCommand.addOptionDirect(helpOpt);\n");
            sb.append("        }\n");
            // Restore the generateHelp flag so Executions.execute() can check it
            sb.append("        processedCommand.setGenerateHelp(true);\n\n");
        }
        if (version != null && !version.isEmpty()) {
            FieldAccessorInfo versionInfo = new FieldAccessorInfo(-1, "generatedVersion", "boolean",
                    null, false, simpleName, true);
            setLookupOption(versionInfo, "v", "version", null, null, false);
            accessorInfos.add(versionInfo);
            sb.append("        {\n");
            sb.append("            ProcessedOption versionOpt = ProcessedOption.createDirect(\n");
            sb.append("                    \"v\", \"version\", \"Displays version information of the command\",\n");
            sb.append("                    Boolean.class, \"generatedVersion\", OptionType.BOOLEAN,\n");
            sb.append(
                    "                    converter_java_lang_Boolean, forHelp ? null : new org.aesh.command.impl.internal.SyntheticBooleanAccessor());\n");
            sb.append("            versionOpt.setOverrideRequired(true);\n");
            sb.append("            processedCommand.addOptionDirect(versionOpt);\n");
            sb.append("        }\n");
            // Restore the version string so Executions.execute() can use it
            sb.append("        processedCommand.setVersion(").append(stringLiteral(version)).append(");\n\n");
        }

        String commandPackage = elementUtils.getPackageOf(commandElement).getQualifiedName().toString();

        // Process user fields
        for (VariableElement field : fields) {
            generateFieldProcessing(sb, simpleName, field, elementUtils, typeUtils, accessorInfos,
                    commandPackage, false);
        }

        // Runtime lookup wiring is execution-only: help builds keep the
        // mutable lookup maps (#675).
        sb.append("        if (!forHelp) {\n");
        if (parentInfos.isEmpty()) {
            sb.append(
                    "            processedCommand.setParentCommandInjector(org.aesh.command.impl.internal.ParentCommandInjector.NO_OP);\n");
        } else {
            sb.append("            processedCommand.setParentCommandInjector(this);\n");
        }
        if (hasLookupOptions(accessorInfos)) {
            sb.append("            processedCommand.setGeneratedOptionLookup(this);\n");
        } else {
            sb.append("            processedCommand.setGeneratedOptionLookup(GeneratedOptionLookup.EMPTY);\n");
        }
        sb.append("        }\n\n");

        // Generated options bypass addOption()'s per-add check, so yield them
        // to explicit user options in one pass with the same rule as the
        // reflection path (#770). Without generated help/version options
        // there is nothing to yield (#791).
        if (generateHelp || !version.isEmpty()) {
            sb.append("        processedCommand.resolveGeneratedConflicts();\n\n");
        }

        sb.append("        return processedCommand;\n");
    }

    /**
     * Expression for a command-level callback attribute: {@code new X()} when
     * the annotation names a non-default class, else {@code null}.
     */
    private static String commandCallbackExpression(TypeElement element, String attributeName,
            String nullSentinel, Elements elementUtils) {
        String className = getAnnotationClassValue(element,
                CommandDefinition.class.getCanonicalName(), attributeName, elementUtils);
        if (className != null && !className.equals(nullSentinel))
            return "new " + className + "()";
        return "null";
    }

    /** {@code forHelp ? null : expr} — the shared runtime guard (#792). */
    private static String forHelpGuard(String expr) {
        return "forHelp ? null : " + expr;
    }

    /**
     * Emit a command-level callback setter for a non-default value, guarded
     * so the help path never instantiates callbacks (#675, #792).
     */
    private static void emitCommandCallbackSetter(StringBuilder sb, String setterName,
            TypeElement element, String attributeName, String nullSentinel, Elements elementUtils) {
        String expr = commandCallbackExpression(element, attributeName, nullSentinel, elementUtils);
        if (!"null".equals(expr)) {
            sb.append("        if (!forHelp) processedCommand.").append(setterName)
                    .append("(").append(expr).append(");\n\n");
        }
    }

    /**
     * Emit help-toggle sources for explicitly-annotated values only, then one
     * resolution pass so direct provider use sees resolved values. Fleet
     * defaults and parent context are applied by the runtime entry points,
     * which re-resolve idempotently (#791).
     */
    private static void emitHelpToggleSources(StringBuilder sb, TypeElement commandElement, Elements elementUtils) {
        String[][] toggleSources = {
                { "collapseSynopsis", "setCollapseSynopsisSource", "toggle" },
                { "showDefaultValues", "setShowDefaultValuesSource", "toggle" },
                { "defaultOptionsFirst", "setDefaultOptionsFirstSource", "toggle" },
                { "failOnDuplicateOptions", "setFailOnDuplicateOptionsSource", "toggle" },
                { "argumentsSection", "setArgumentsSectionSource", "section" },
                { "headerBeforeSynopsis", "setHeaderBeforeSynopsisSource", "toggle" },
        };
        boolean anySet = false;
        for (String[] source : toggleSources) {
            String annotated = getAnnotationValue(commandElement, source[0], elementUtils);
            String value = "section".equals(source[2])
                    ? argumentsSectionValue(annotated)
                    : toggleValue(annotated);
            String defaultValue = "section".equals(source[2])
                    ? "org.aesh.command.ArgumentsSection.INHERIT"
                    : "org.aesh.command.Toggle.INHERIT";
            if (!value.equals(defaultValue)) {
                sb.append("        processedCommand.").append(source[1]).append("(").append(value).append(");\n");
                anySet = true;
            }
        }
        if (anySet)
            sb.append("        processedCommand.resolveHelpToggles(null, null, null);\n\n");
    }

    private static void generateFieldProcessing(StringBuilder sb, String simpleName, VariableElement field,
            Elements elementUtils, Types typeUtils, List<FieldAccessorInfo> accessorInfos,
            String commandPackage, boolean mixinFieldPrivate) {
        generateFieldProcessing(sb, simpleName, field, null, elementUtils, typeUtils, accessorInfos,
                commandPackage, false);
    }

    private static void generateFieldProcessing(StringBuilder sb, String simpleName, VariableElement field,
            String mixinFieldName, Elements elementUtils, Types typeUtils, List<FieldAccessorInfo> accessorInfos,
            String commandPackage, boolean mixinFieldPrivate) {
        Option o = field.getAnnotation(Option.class);
        OptionList ol = field.getAnnotation(OptionList.class);
        OptionGroup og = field.getAnnotation(OptionGroup.class);
        Arguments args = field.getAnnotation(Arguments.class);
        Argument arg = field.getAnnotation(Argument.class);

        if (o != null) {
            generateOption(sb, simpleName, field, o, mixinFieldName, elementUtils, typeUtils, accessorInfos,
                    null, commandPackage, mixinFieldPrivate);
        } else if (ol != null) {
            generateOptionList(sb, simpleName, field, ol, mixinFieldName, elementUtils, typeUtils, accessorInfos,
                    null, commandPackage, mixinFieldPrivate);
        } else if (og != null) {
            generateOptionGroup(sb, simpleName, field, og, mixinFieldName, elementUtils, typeUtils, accessorInfos,
                    commandPackage, mixinFieldPrivate);
        } else if (args != null) {
            generateArguments(sb, simpleName, field, args, mixinFieldName, elementUtils, typeUtils, accessorInfos,
                    null, commandPackage, mixinFieldPrivate);
        } else if (arg != null) {
            generateArgument(sb, simpleName, field, arg, mixinFieldName, elementUtils, typeUtils, accessorInfos,
                    null, commandPackage, mixinFieldPrivate);
        } else if (field.getAnnotation(Mixin.class) != null) {
            // Chain mixinFieldName for nested mixins: "outer" + "inner" -> "outer.inner"
            String nestedMixinName = mixinFieldName != null
                    ? mixinFieldName + "." + field.getSimpleName().toString()
                    : field.getSimpleName().toString();
            generateMixin(sb, simpleName, field, nestedMixinName, elementUtils, typeUtils, accessorInfos,
                    commandPackage, isPrivateField(field));
        }
    }

    private static void generateMixin(StringBuilder sb, String simpleName, VariableElement mixinField,
            String mixinFieldName, Elements elementUtils, Types typeUtils, List<FieldAccessorInfo> accessorInfos,
            String commandPackage, boolean mixinFieldPrivate) {
        TypeMirror mixinType = mixinField.asType();
        if (!(mixinType instanceof DeclaredType))
            return;
        TypeElement mixinElement = (TypeElement) ((DeclaredType) mixinType).asElement();
        String mixinTypeName = erasedTypeName(mixinType, typeUtils);

        // Mixin instantiation is execution-only: the help build has no
        // command instance and must not construct mixins (#675, #792).
        if (isPrivateField(mixinField)) {
            String constName = fieldConstantRef(fieldConstantName(mixinFieldName));
            sb.append("        if (!forHelp) {\n");
            sb.append("            try {\n");
            sb.append("                if (").append(constName).append(".get(instance) == null) {\n");
            sb.append("                    ").append(constName).append(".set(instance, new ").append(mixinTypeName)
                    .append("());\n");
            sb.append("                }\n");
            sb.append("            } catch (IllegalAccessException e) { throw new RuntimeException(e); }\n");
            sb.append("        }\n\n");
        } else {
            sb.append("        if (!forHelp && instance.").append(mixinFieldName).append(" == null) {\n");
            sb.append("            instance.").append(mixinFieldName).append(" = new ").append(mixinTypeName)
                    .append("();\n");
            sb.append("        }\n\n");
        }

        generateMixinFields(sb, simpleName, mixinFieldName, mixinElement, elementUtils, typeUtils, accessorInfos,
                commandPackage, mixinFieldPrivate);
    }

    private static void generateMixinFields(StringBuilder sb, String simpleName, String mixinFieldName,
            TypeElement typeElement, Elements elementUtils, Types typeUtils, List<FieldAccessorInfo> accessorInfos,
            String commandPackage, boolean mixinFieldPrivate) {
        for (javax.lang.model.element.Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed instanceof VariableElement) {
                generateFieldProcessing(sb, simpleName, (VariableElement) enclosed, mixinFieldName,
                        elementUtils, typeUtils, accessorInfos, commandPackage, mixinFieldPrivate);
            }
        }
        // Recurse into superclass
        TypeMirror superclass = typeElement.getSuperclass();
        if (superclass.getKind() != TypeKind.NONE && !erasedTypeName(superclass, typeUtils).equals("java.lang.Object")) {
            if (superclass instanceof DeclaredType) {
                TypeElement superElement = (TypeElement) ((DeclaredType) superclass).asElement();
                generateMixinFields(sb, simpleName, mixinFieldName, superElement, elementUtils, typeUtils, accessorInfos,
                        commandPackage, mixinFieldPrivate);
            }
        }
    }

    private static void generateOption(StringBuilder sb, String simpleName, VariableElement field, Option o,
            String mixinFieldName, Elements elementUtils, Types typeUtils, List<FieldAccessorInfo> accessorInfos,
            String accessorFieldName, String commandPackage, boolean mixinFieldPrivate) {
        String fieldName = field.getSimpleName().toString();
        TypeMirror effectiveType = field.asType();
        boolean isOptionalWrapped = isOptionalType(effectiveType, typeUtils);
        if (isOptionalWrapped) {
            effectiveType = unwrapOptionalTypeMirror(effectiveType);
        }
        String fieldType = getBoxedTypeName(effectiveType, typeUtils);
        String optionName = o.name().length() < 1 ? fieldName : o.name();
        String optionType = o.hasValue() ? "OptionType.NORMAL" : "OptionType.BOOLEAN";

        // Accessor index
        int accIdx = nextFieldIndex(accessorInfos);
        FieldAccessorInfo info = new FieldAccessorInfo(accIdx,
                accessorFieldName != null ? accessorFieldName : fieldName,
                erasedTypeName(field.asType(), typeUtils),
                mixinFieldName, needsReflectiveAccess(field, commandPackage, mixinFieldPrivate), simpleName);
        setLookupOption(info,
                o.shortName() != '\u0000' ? String.valueOf(o.shortName()) : null,
                optionName, o.aliases(), o.negatable() ? o.negationPrefix() + optionName : null,
                o.acceptNameWithoutDashes());
        accessorInfos.add(info);

        // Emit: ProcessedOption opt_N = ProcessedOption.createDirect(...)
        String var = "opt_" + accIdx;
        sb.append("        {\n");
        sb.append("            ProcessedOption ").append(var).append(" = ProcessedOption.createDirect(\n");
        sb.append("                    ")
                .append(o.shortName() != '\u0000' ? stringLiteral(String.valueOf(o.shortName())) : "null")
                .append(", ").append(stringLiteral(optionName))
                .append(", ").append(stringLiteral(o.description())).append(",\n");
        sb.append("                    ").append(fieldType).append(".class, ")
                .append(stringLiteral(fieldName)).append(", ").append(optionType).append(",\n");
        sb.append("                    ");
        emitConverterExpression(sb, field, "converter", elementUtils, fieldType, effectiveType, typeUtils);
        sb.append(", accessor, ").append(accIdx).append(");\n");
        // Non-default setters
        if (isOptionalWrapped)
            sb.append("            ").append(var).append(".setOptionalWrapped(true);\n");
        if (o.required())
            sb.append("            ").append(var).append(".setRequired(true);\n");
        if (o.askIfNotSet())
            sb.append("            ").append(var).append(".setAskIfNotSet(true);\n");
        if (o.acceptNameWithoutDashes())
            sb.append("            ").append(var).append(".setAcceptNameWithoutDashes(true);\n");
        if (!o.argument().isEmpty())
            sb.append("            ").append(var).append(".setArgument(")
                    .append(stringLiteral(o.argument())).append(");\n");
        if (o.defaultValue().length > 0)
            sb.append("            ").append(var).append(".setDefaultValues(")
                    .append(listExpression(o.defaultValue())).append(");\n");
        emitCompleterSetter(sb, var, field, "completer", elementUtils);
        emitCallbackSetter(sb, var, "setValidator", field, "validator", NULL_VALIDATOR, elementUtils);
        emitCallbackSetter(sb, var, "setActivator", field, "activator", NULL_ACTIVATOR, elementUtils);
        emitCallbackSetter(sb, var, "setRenderer", field, "renderer", NULL_OPTION_RENDERER, elementUtils);
        emitParserSetter(sb, var, field, "parser", elementUtils);
        if (o.overrideRequired())
            sb.append("            ").append(var).append(".setOverrideRequired(true);\n");
        if (o.optionalValue())
            sb.append("            ").append(var).append(".setOptionalValue(true);\n");
        if (!"\u0000".equals(o.fallbackValue())) {
            sb.append("            ").append(var).append(".setFallbackValue(").append(stringLiteral(o.fallbackValue()))
                    .append(");\n");
            // fallbackValue implies optionalValue
            if (!o.optionalValue())
                sb.append("            ").append(var).append(".setOptionalValue(true);\n");
        }
        if (o.negatable())
            sb.append("            ").append(var).append(".setNegatable(true);\n");
        if (!o.negationPrefix().equals("no-"))
            sb.append("            ").append(var).append(".setNegationPrefix(").append(stringLiteral(o.negationPrefix()))
                    .append(");\n");
        if (o.inherited())
            sb.append("            ").append(var).append(".setInherited(true);\n");
        if (!o.descriptionUrl().isEmpty())
            sb.append("            ").append(var).append(".setDescriptionUrl(").append(stringLiteral(o.descriptionUrl()))
                    .append(");\n");
        if (o.url())
            sb.append("            ").append(var).append(".setIsUrl(true);\n");
        if (o.selector() != org.aesh.selector.SelectorType.NO_OP)
            sb.append("            ").append(var).append(".setSelectorType(").append(selectorLiteral(o.selector()))
                    .append(");\n");
        emitCompleteFallbackSetter(sb, var, o.completeFallback());
        emitAliasesSetter(sb, var, o.aliases());
        emitHelpGroupSetter(sb, var, o.helpGroup());
        emitExclusiveWithSetter(sb, var, o.exclusiveWith());
        // Auto-populate allowedValues for enum types at compile time (#591).
        // Skip when a custom converter is set (per #584).
        String[] allowedVals = o.allowedValues();
        if (allowedVals.length == 0 && !hasCustomConverter(field, elementUtils)) {
            String[] enumConstants = getEnumConstantNames(effectiveType, typeUtils);
            if (enumConstants != null)
                allowedVals = enumConstants;
        }
        emitAllowedValuesSetter(sb, var, allowedVals);
        emitVisibilitySetter(sb, var, o.visibility());
        if (o.order() != Integer.MAX_VALUE)
            sb.append("            ").append(var).append(".setOrder(").append(o.order()).append(");\n");
        if (mixinFieldName != null)
            sb.append("            ").append(var).append(".setMixinFieldName(").append(stringLiteral(mixinFieldName))
                    .append(");\n");
        emitInitialValueSetter(sb, var, info);
        sb.append("            processedCommand.addOptionDirect(").append(var).append(");\n");
        sb.append("        }\n\n");
    }

    private static void emitInitialValueSetter(StringBuilder sb, String var, FieldAccessorInfo info) {
        // The accessor switch is unnecessary for fields the generated provider
        // can read directly. Private fields and method-command slot arrays keep
        // the existing capture path (method owners are not Adapter instances).
        if (info.needsReflection || info.synthetic || "Adapter".equals(info.commandSimpleName))
            return;
        // Guarded: the help build has no command instance to read (#675, #792)
        sb.append("            if (!forHelp) ").append(var).append(".setInitialValue(instance.");
        if (info.mixinFieldName != null)
            sb.append(info.mixinFieldName).append(".");
        sb.append(info.fieldName).append(");\n");
    }

    private static void generateOptionList(StringBuilder sb, String simpleName, VariableElement field, OptionList ol,
            String mixinFieldName, Elements elementUtils, Types typeUtils, List<FieldAccessorInfo> accessorInfos,
            String accessorFieldName, String commandPackage, boolean mixinFieldPrivate) {
        String fieldName = field.getSimpleName().toString();
        TypeMirror effectiveType = field.asType();
        boolean isOptionalWrapped = isOptionalType(effectiveType, typeUtils);
        if (isOptionalWrapped) {
            effectiveType = unwrapOptionalTypeMirror(effectiveType);
        }
        boolean isArrayType = effectiveType.getKind() == TypeKind.ARRAY;
        String elementType = getElementTypeName(effectiveType, typeUtils);
        String optionName = ol.name().length() < 1 ? fieldName : ol.name();

        int accIdx = nextFieldIndex(accessorInfos);
        FieldAccessorInfo info = new FieldAccessorInfo(accIdx,
                accessorFieldName != null ? accessorFieldName : fieldName,
                erasedTypeName(field.asType(), typeUtils),
                mixinFieldName, needsReflectiveAccess(field, commandPackage, mixinFieldPrivate), simpleName);
        setLookupOption(info,
                ol.shortName() != '\u0000' ? String.valueOf(ol.shortName()) : null,
                optionName, ol.aliases(), null, ol.acceptNameWithoutDashes());
        accessorInfos.add(info);

        String var = "opt_" + accIdx;
        sb.append("        {\n");
        sb.append("            ProcessedOption ").append(var).append(" = ProcessedOption.createDirect(\n");
        sb.append("                    ")
                .append(ol.shortName() != '\u0000' ? stringLiteral(String.valueOf(ol.shortName())) : "null")
                .append(", ").append(stringLiteral(optionName))
                .append(", ").append(stringLiteral(ol.description())).append(",\n");
        sb.append("                    ").append(elementType).append(".class, ")
                .append(stringLiteral(fieldName)).append(", OptionType.LIST,\n");
        sb.append("                    ");
        emitConverterExpression(sb, field, "converter", elementUtils, elementType);
        sb.append(", accessor, ").append(accIdx).append(");\n");
        sb.append("            ").append(var).append(".setValueSeparator(").append(charLiteral(ol.valueSeparator()))
                .append(");\n");
        emitInitialValueFactory(sb, var, field.asType(), typeUtils);
        if (isOptionalWrapped)
            sb.append("            ").append(var).append(".setOptionalWrapped(true);\n");
        if (isArrayType)
            sb.append("            ").append(var).append(".setArrayType(true);\n");
        if (ol.required())
            sb.append("            ").append(var).append(".setRequired(true);\n");
        if (ol.askIfNotSet())
            sb.append("            ").append(var).append(".setAskIfNotSet(true);\n");
        if (ol.acceptNameWithoutDashes())
            sb.append("            ").append(var).append(".setAcceptNameWithoutDashes(true);\n");
        if (ol.defaultValue().length > 0)
            sb.append("            ").append(var).append(".setDefaultValues(")
                    .append(listExpression(ol.defaultValue())).append(");\n");
        emitCompleterSetter(sb, var, field, "completer", elementUtils);
        emitCallbackSetter(sb, var, "setValidator", field, "validator", NULL_VALIDATOR, elementUtils);
        emitCallbackSetter(sb, var, "setActivator", field, "activator", NULL_ACTIVATOR, elementUtils);
        emitCallbackSetter(sb, var, "setRenderer", field, "renderer", NULL_OPTION_RENDERER, elementUtils);
        emitParserSetter(sb, var, field, "parser", elementUtils);
        if (ol.overrideRequired())
            sb.append("            ").append(var).append(".setOverrideRequired(true);\n");
        if (ol.optionalValue())
            sb.append("            ").append(var).append(".setOptionalValue(true);\n");
        if (!"\u0000".equals(ol.fallbackValue())) {
            sb.append("            ").append(var).append(".setFallbackValue(").append(stringLiteral(ol.fallbackValue()))
                    .append(");\n");
            // fallbackValue implies optionalValue
            if (!ol.optionalValue())
                sb.append("            ").append(var).append(".setOptionalValue(true);\n");
        }
        if (ol.inherited())
            sb.append("            ").append(var).append(".setInherited(true);\n");
        if (!ol.descriptionUrl().isEmpty())
            sb.append("            ").append(var).append(".setDescriptionUrl(").append(stringLiteral(ol.descriptionUrl()))
                    .append(");\n");
        if (ol.url())
            sb.append("            ").append(var).append(".setIsUrl(true);\n");
        if (ol.selector() != org.aesh.selector.SelectorType.NO_OP)
            sb.append("            ").append(var).append(".setSelectorType(").append(selectorLiteral(ol.selector()))
                    .append(");\n");
        emitCompleteFallbackSetter(sb, var, ol.completeFallback());
        emitAliasesSetter(sb, var, ol.aliases());
        emitHelpGroupSetter(sb, var, ol.helpGroup());
        emitExclusiveWithSetter(sb, var, ol.exclusiveWith());
        emitAllowedValuesSetter(sb, var, ol.allowedValues());
        emitVisibilitySetter(sb, var, ol.visibility());
        if (ol.order() != Integer.MAX_VALUE)
            sb.append("            ").append(var).append(".setOrder(").append(ol.order()).append(");\n");
        if (mixinFieldName != null)
            sb.append("            ").append(var).append(".setMixinFieldName(").append(stringLiteral(mixinFieldName))
                    .append(");\n");
        emitInitialValueSetter(sb, var, info);
        sb.append("            processedCommand.addOptionDirect(").append(var).append(");\n");
        sb.append("        }\n\n");
    }

    private static void generateOptionGroup(StringBuilder sb, String simpleName, VariableElement field, OptionGroup og,
            String mixinFieldName, Elements elementUtils, Types typeUtils, List<FieldAccessorInfo> accessorInfos,
            String commandPackage, boolean mixinFieldPrivate) {
        String fieldName = field.getSimpleName().toString();
        TypeMirror effectiveType = field.asType();
        boolean isOptionalWrapped = isOptionalType(effectiveType, typeUtils);
        if (isOptionalWrapped) {
            effectiveType = unwrapOptionalTypeMirror(effectiveType);
        }
        String valueType = getGenericTypeArgument(effectiveType, 1, typeUtils);
        String optionName = og.name().length() < 1
                ? (og.shortName() != '\u0000' ? "" : fieldName)
                : og.name();

        int accIdx = nextFieldIndex(accessorInfos);
        FieldAccessorInfo info = new FieldAccessorInfo(accIdx, fieldName, erasedTypeName(field.asType(), typeUtils),
                mixinFieldName, needsReflectiveAccess(field, commandPackage, mixinFieldPrivate), simpleName);
        setLookupOption(info,
                og.shortName() != '\u0000' ? String.valueOf(og.shortName()) : null,
                optionName, og.aliases(), null, og.acceptNameWithoutDashes());
        accessorInfos.add(info);

        String var = "opt_" + accIdx;
        sb.append("        {\n");
        sb.append("            ProcessedOption ").append(var).append(" = ProcessedOption.createDirect(\n");
        sb.append("                    ")
                .append(og.shortName() != '\u0000' ? stringLiteral(String.valueOf(og.shortName())) : "null")
                .append(", ").append(stringLiteral(optionName))
                .append(", ").append(stringLiteral(og.description())).append(",\n");
        sb.append("                    ").append(valueType).append(".class, ")
                .append(stringLiteral(fieldName)).append(", OptionType.GROUP,\n");
        sb.append("                    ");
        emitConverterExpression(sb, field, "converter", elementUtils, valueType);
        sb.append(", accessor, ").append(accIdx).append(");\n");
        sb.append("            ").append(var).append(".setValueSeparator(',');\n");
        emitInitialValueFactory(sb, var, field.asType(), typeUtils);
        if (isOptionalWrapped)
            sb.append("            ").append(var).append(".setOptionalWrapped(true);\n");
        if (og.required())
            sb.append("            ").append(var).append(".setRequired(true);\n");
        if (og.askIfNotSet())
            sb.append("            ").append(var).append(".setAskIfNotSet(true);\n");
        if (og.acceptNameWithoutDashes())
            sb.append("            ").append(var).append(".setAcceptNameWithoutDashes(true);\n");
        if (og.defaultValue().length > 0)
            sb.append("            ").append(var).append(".setDefaultValues(")
                    .append(listExpression(og.defaultValue())).append(");\n");
        emitCompleterSetter(sb, var, field, "completer", elementUtils);
        emitCallbackSetter(sb, var, "setValidator", field, "validator", NULL_VALIDATOR, elementUtils);
        emitCallbackSetter(sb, var, "setActivator", field, "activator", NULL_ACTIVATOR, elementUtils);
        emitCallbackSetter(sb, var, "setRenderer", field, "renderer", NULL_OPTION_RENDERER, elementUtils);
        emitParserSetter(sb, var, field, "parser", elementUtils);
        if (og.overrideRequired())
            sb.append("            ").append(var).append(".setOverrideRequired(true);\n");
        if (og.inherited())
            sb.append("            ").append(var).append(".setInherited(true);\n");
        if (!og.descriptionUrl().isEmpty())
            sb.append("            ").append(var).append(".setDescriptionUrl(").append(stringLiteral(og.descriptionUrl()))
                    .append(");\n");
        if (og.url())
            sb.append("            ").append(var).append(".setIsUrl(true);\n");
        if (og.selector() != org.aesh.selector.SelectorType.NO_OP)
            sb.append("            ").append(var).append(".setSelectorType(").append(selectorLiteral(og.selector()))
                    .append(");\n");
        emitCompleteFallbackSetter(sb, var, og.completeFallback());
        emitAliasesSetter(sb, var, og.aliases());
        emitHelpGroupSetter(sb, var, og.helpGroup());
        emitExclusiveWithSetter(sb, var, og.exclusiveWith());
        emitAllowedValuesSetter(sb, var, og.allowedValues());
        emitVisibilitySetter(sb, var, og.visibility());
        if (og.order() != Integer.MAX_VALUE)
            sb.append("            ").append(var).append(".setOrder(").append(og.order()).append(");\n");
        if (mixinFieldName != null)
            sb.append("            ").append(var).append(".setMixinFieldName(").append(stringLiteral(mixinFieldName))
                    .append(");\n");
        emitInitialValueSetter(sb, var, info);
        sb.append("            processedCommand.addOptionDirect(").append(var).append(");\n");
        sb.append("        }\n\n");
    }

    private static void generateArguments(StringBuilder sb, String simpleName, VariableElement field, Arguments a,
            String mixinFieldName, Elements elementUtils, Types typeUtils, List<FieldAccessorInfo> accessorInfos,
            String accessorFieldName, String commandPackage, boolean mixinFieldPrivate) {
        String fieldName = field.getSimpleName().toString();
        TypeMirror effectiveType = field.asType();
        boolean isOptionalWrapped = isOptionalType(effectiveType, typeUtils);
        if (isOptionalWrapped) {
            effectiveType = unwrapOptionalTypeMirror(effectiveType);
        }
        boolean isArrayType = effectiveType.getKind() == TypeKind.ARRAY;
        String elementType = getElementTypeName(effectiveType, typeUtils);

        int accIdx = nextFieldIndex(accessorInfos);
        FieldAccessorInfo info = new FieldAccessorInfo(accIdx,
                accessorFieldName != null ? accessorFieldName : fieldName,
                erasedTypeName(field.asType(), typeUtils),
                mixinFieldName, needsReflectiveAccess(field, commandPackage, mixinFieldPrivate), simpleName);
        accessorInfos.add(info);

        String var = "argsOpt_" + accIdx;
        sb.append("        {\n");
        sb.append("            ProcessedOption ").append(var).append(" = ProcessedOption.createDirect(\n");
        sb.append("                    null, \"\", ").append(stringLiteral(a.description())).append(",\n");
        sb.append("                    ").append(elementType).append(".class, ")
                .append(stringLiteral(fieldName)).append(", OptionType.ARGUMENTS,\n");
        sb.append("                    ");
        emitConverterExpression(sb, field, "converter", elementUtils, elementType);
        sb.append(", accessor, ").append(accIdx).append(");\n");
        emitInitialValueFactory(sb, var, field.asType(), typeUtils);
        if (isOptionalWrapped)
            sb.append("            ").append(var).append(".setOptionalWrapped(true);\n");
        if (isArrayType)
            sb.append("            ").append(var).append(".setArrayType(true);\n");
        if (a.required())
            sb.append("            ").append(var).append(".setRequired(true);\n");
        if (a.valueSeparator() != ' ')
            sb.append("            ").append(var).append(".setValueSeparator(").append(charLiteral(a.valueSeparator()))
                    .append(");\n");
        if (a.selector() != org.aesh.selector.SelectorType.NO_OP)
            sb.append("            ").append(var).append(".setSelectorType(").append(selectorLiteral(a.selector()))
                    .append(");\n");
        if (a.askIfNotSet())
            sb.append("            ").append(var).append(".setAskIfNotSet(true);\n");
        if (a.defaultValue().length > 0)
            sb.append("            ").append(var).append(".setDefaultValues(")
                    .append(listExpression(a.defaultValue())).append(");\n");
        if (!a.paramLabel().isEmpty())
            sb.append("            ").append(var).append(".setParamLabel(").append(stringLiteral(a.paramLabel()))
                    .append(");\n");
        if (!a.arity().isEmpty())
            sb.append("            ").append(var).append(".setArity(").append(stringLiteral(a.arity())).append(");\n");
        sb.append("            ").append(var).append(".setIndex(").append(stringLiteral(a.index())).append(");\n");
        emitHelpGroupSetter(sb, var, a.helpGroup());
        if (a.order() != Integer.MAX_VALUE)
            sb.append("            ").append(var).append(".setOrder(").append(a.order()).append(");\n");
        emitCompleterSetter(sb, var, field, "completer", elementUtils);
        emitCallbackSetter(sb, var, "setValidator", field, "validator", NULL_VALIDATOR, elementUtils);
        emitCallbackSetter(sb, var, "setActivator", field, "activator", NULL_ACTIVATOR, elementUtils);
        emitParserSetter(sb, var, field, "parser", elementUtils);
        if (a.url())
            sb.append("            ").append(var).append(".setIsUrl(true);\n");
        emitCompleteFallbackSetter(sb, var, a.completeFallback());
        if (mixinFieldName != null)
            sb.append("            ").append(var).append(".setMixinFieldName(").append(stringLiteral(mixinFieldName))
                    .append(");\n");
        emitInitialValueSetter(sb, var, info);
        sb.append("            processedCommand.setArguments(").append(var).append(");\n");
        sb.append("        }\n\n");
    }

    private static void generateArgument(StringBuilder sb, String simpleName, VariableElement field, Argument arg,
            String mixinFieldName, Elements elementUtils, Types typeUtils, List<FieldAccessorInfo> accessorInfos,
            String accessorFieldName, String commandPackage, boolean mixinFieldPrivate) {
        String fieldName = field.getSimpleName().toString();
        TypeMirror effectiveType = field.asType();
        boolean isOptionalWrapped = isOptionalType(effectiveType, typeUtils);
        if (isOptionalWrapped) {
            effectiveType = unwrapOptionalTypeMirror(effectiveType);
        }
        String fieldType = getBoxedTypeName(effectiveType, typeUtils);

        int accIdx = nextFieldIndex(accessorInfos);
        FieldAccessorInfo info = new FieldAccessorInfo(accIdx,
                accessorFieldName != null ? accessorFieldName : fieldName,
                erasedTypeName(field.asType(), typeUtils),
                mixinFieldName, needsReflectiveAccess(field, commandPackage, mixinFieldPrivate), simpleName);
        accessorInfos.add(info);

        String var = "argOpt_" + accIdx;
        sb.append("        {\n");
        sb.append("            ProcessedOption ").append(var).append(" = ProcessedOption.createDirect(\n");
        sb.append("                    null, \"\", ").append(stringLiteral(arg.description())).append(",\n");
        sb.append("                    ").append(fieldType).append(".class, ")
                .append(stringLiteral(fieldName)).append(", OptionType.ARGUMENT,\n");
        sb.append("                    ");
        emitConverterExpression(sb, field, "converter", elementUtils, fieldType);
        sb.append(", accessor, ").append(accIdx).append(");\n");
        if (isOptionalWrapped)
            sb.append("            ").append(var).append(".setOptionalWrapped(true);\n");
        if (arg.required())
            sb.append("            ").append(var).append(".setRequired(true);\n");
        if (arg.askIfNotSet())
            sb.append("            ").append(var).append(".setAskIfNotSet(true);\n");
        if (arg.selector() != org.aesh.selector.SelectorType.NO_OP)
            sb.append("            ").append(var).append(".setSelectorType(").append(selectorLiteral(arg.selector()))
                    .append(");\n");
        if (arg.defaultValue().length > 0)
            sb.append("            ").append(var).append(".setDefaultValues(")
                    .append(listExpression(arg.defaultValue())).append(");\n");
        if (!arg.paramLabel().isEmpty())
            sb.append("            ").append(var).append(".setParamLabel(").append(stringLiteral(arg.paramLabel()))
                    .append(");\n");
        if (!arg.arity().isEmpty())
            sb.append("            ").append(var).append(".setArity(").append(stringLiteral(arg.arity())).append(");\n");
        sb.append("            ").append(var).append(".setIndex(").append(stringLiteral(arg.index())).append(");\n");
        emitHelpGroupSetter(sb, var, arg.helpGroup());
        if (arg.order() != Integer.MAX_VALUE)
            sb.append("            ").append(var).append(".setOrder(").append(arg.order()).append(");\n");
        emitCompleterSetter(sb, var, field, "completer", elementUtils);
        emitCallbackSetter(sb, var, "setValidator", field, "validator", NULL_VALIDATOR, elementUtils);
        emitCallbackSetter(sb, var, "setActivator", field, "activator", NULL_ACTIVATOR, elementUtils);
        emitCallbackSetter(sb, var, "setRenderer", field, "renderer", NULL_OPTION_RENDERER, elementUtils);
        emitParserSetter(sb, var, field, "parser", elementUtils);
        if (arg.overrideRequired())
            sb.append("            ").append(var).append(".setOverrideRequired(true);\n");
        if (arg.inherited())
            sb.append("            ").append(var).append(".setInherited(true);\n");
        if (arg.url())
            sb.append("            ").append(var).append(".setIsUrl(true);\n");
        emitCompleteFallbackSetter(sb, var, arg.completeFallback());
        if (mixinFieldName != null)
            sb.append("            ").append(var).append(".setMixinFieldName(").append(stringLiteral(mixinFieldName))
                    .append(");\n");
        emitInitialValueSetter(sb, var, info);
        sb.append("            processedCommand.addArgument(").append(var).append(");\n");
        sb.append("        }\n\n");
    }

    private static int nextFieldIndex(List<FieldAccessorInfo> infos) {
        int count = 0;
        for (FieldAccessorInfo info : infos) {
            if (!info.synthetic)
                count++;
        }
        return count;
    }

    private static boolean hasRealFieldAccessors(List<FieldAccessorInfo> infos) {
        for (FieldAccessorInfo info : infos) {
            if (!info.synthetic)
                return true;
        }
        return false;
    }

    private static boolean hasLookupOptions(List<FieldAccessorInfo> infos) {
        for (FieldAccessorInfo info : infos) {
            if (info.lookupOption)
                return true;
        }
        return false;
    }

    private static void generateOptionLookupMethods(StringBuilder sb, List<FieldAccessorInfo> infos) {
        LinkedHashMap<String, Integer> longNames = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> shortNames = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> negatedNames = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> bareNames = new LinkedHashMap<>();
        int optionIndex = 0;
        for (FieldAccessorInfo info : infos) {
            if (!info.lookupOption)
                continue;
            if (info.optionName != null && !info.optionName.isEmpty()) {
                longNames.put(info.optionName, optionIndex);
                if (info.optionAliases != null) {
                    for (String alias : info.optionAliases)
                        longNames.put(alias, optionIndex);
                }
            }
            if (info.shortName != null && !info.shortName.isEmpty())
                shortNames.put(info.shortName, optionIndex);
            if (info.negatedName != null)
                negatedNames.put(info.negatedName, optionIndex);
            if (info.acceptBareName && info.optionName != null) {
                bareNames.put(info.optionName, optionIndex);
                if (info.optionAliases != null) {
                    for (String alias : info.optionAliases)
                        bareNames.put(alias, optionIndex);
                }
            }
            optionIndex++;
        }

        emitLookupMethod(sb, "longOptionIndex", longNames);
        emitLookupMethod(sb, "shortOptionIndex", shortNames);
        emitLookupMethod(sb, "negatedOptionIndex", negatedNames);
        emitLookupMethod(sb, "bareOptionIndex", bareNames);
    }

    private static void emitLookupMethod(StringBuilder sb, String methodName,
            LinkedHashMap<String, Integer> names) {
        if (names.isEmpty())
            return;
        sb.append("    @Override\n");
        sb.append("    public int ").append(methodName).append("(String name) {\n");
        sb.append("        if (name == null)\n");
        sb.append("            return -1;\n");
        sb.append("        switch (name) {\n");
        for (Map.Entry<String, Integer> entry : names.entrySet()) {
            sb.append("            case ").append(stringLiteral(entry.getKey())).append(": return ")
                    .append(entry.getValue()).append(";\n");
        }
        sb.append("            default: return -1;\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
    }

    private static void generateIndexedFieldAccessorMethods(StringBuilder sb, List<FieldAccessorInfo> infos) {
        boolean needsTry = hasReflectiveFields(infos);

        // set(int idx, Object inst, Object val)
        sb.append("    @Override\n");
        sb.append("    public void set(int idx, Object inst, Object val) {\n");
        if (needsTry) {
            sb.append("        try {\n");
        }
        sb.append("        switch (idx) {\n");
        for (FieldAccessorInfo info : infos) {
            if (info.synthetic)
                continue;
            sb.append("            case ").append(info.index).append(": ");
            if (info.mixinFieldName != null) {
                if (info.needsReflection) {
                    sb.append("{ Object m = ").append(fieldConstantRef(fieldConstantName(info.mixinFieldName)))
                            .append(".get(inst); if (m != null) ")
                            .append(fieldConstantRef(fieldConstantName(info.mixinFieldName, info.fieldName)))
                            .append(".set(m, val); }");
                } else {
                    sb.append("if (((").append(info.commandSimpleName).append(") inst).")
                            .append(info.mixinFieldName).append(" != null) ((")
                            .append(info.commandSimpleName).append(") inst).")
                            .append(info.mixinFieldName).append(".").append(info.fieldName)
                            .append(" = (").append(info.fieldType).append(") val;");
                }
            } else {
                if (info.needsReflection) {
                    sb.append(fieldConstantRef(fieldConstantName(info.fieldName))).append(".set(inst, val);");
                } else {
                    sb.append("((").append(info.commandSimpleName).append(") inst).")
                            .append(info.fieldName).append(" = (").append(info.fieldType).append(") val;");
                }
            }
            sb.append(" break;\n");
        }
        sb.append("        }\n");
        if (needsTry) {
            sb.append("        } catch (IllegalAccessException e) { throw new RuntimeException(e); }\n");
        }
        sb.append("    }\n\n");

        // get(int idx, Object inst)
        sb.append("    @Override\n");
        sb.append("    public Object get(int idx, Object inst) {\n");
        if (needsTry) {
            sb.append("        try {\n");
        }
        sb.append("        switch (idx) {\n");
        for (FieldAccessorInfo info : infos) {
            if (info.synthetic)
                continue;
            sb.append("            case ").append(info.index).append(": ");
            if (info.mixinFieldName != null) {
                if (info.needsReflection) {
                    sb.append("{ Object m = ").append(fieldConstantRef(fieldConstantName(info.mixinFieldName)))
                            .append(".get(inst); return m != null ? ")
                            .append(fieldConstantRef(fieldConstantName(info.mixinFieldName, info.fieldName)))
                            .append(".get(m) : null; }");
                } else {
                    sb.append("return ((").append(info.commandSimpleName).append(") inst).")
                            .append(info.mixinFieldName).append(" != null ? ((")
                            .append(info.commandSimpleName).append(") inst).")
                            .append(info.mixinFieldName).append(".").append(info.fieldName).append(" : null;");
                }
            } else {
                if (info.needsReflection) {
                    sb.append("return ").append(fieldConstantRef(fieldConstantName(info.fieldName))).append(".get(inst);");
                } else {
                    sb.append("return ((").append(info.commandSimpleName).append(") inst).")
                            .append(info.fieldName).append(";");
                }
            }
            sb.append("\n");
        }
        sb.append("            default: return null;\n");
        sb.append("        }\n");
        if (needsTry) {
            sb.append("        } catch (IllegalAccessException e) { throw new RuntimeException(e); }\n");
        }
        sb.append("    }\n\n");
    }

    private static void generateParentInjectorMethods(StringBuilder sb, String commandSimpleName,
            List<FieldAccessorInfo> parentInfos) {
        // Legacy direct-parent form, plus the type-directed runtime form.
        sb.append("    @Override\n");
        sb.append("    public void accept(Object cmd, Object parent) {\n");
        for (FieldAccessorInfo parentInfo : parentInfos) {
            sb.append("        if (parent instanceof ").append(parentInfo.fieldType).append(") {");
            if (parentInfo.needsReflection) {
                sb.append(" try { ").append(fieldConstantRef(fieldConstantName(parentInfo.fieldName)))
                        .append(".set(cmd, parent); } catch (IllegalAccessException e) { throw new RuntimeException(e); }");
            } else {
                sb.append(" ((").append(commandSimpleName).append(") cmd).")
                        .append(parentInfo.fieldName).append(" = (").append(parentInfo.fieldType).append(") parent;");
            }
            sb.append(" }\n");
        }
        sb.append("    }\n\n");
        sb.append("    @Override\n");
        sb.append("    public void inject(Object cmd, java.util.function.Function<Class<?>, Object> parentLookup) {\n");
        for (FieldAccessorInfo parentInfo : parentInfos) {
            sb.append("        {").append(parentInfo.fieldType).append(" parent = (")
                    .append(parentInfo.fieldType).append(") parentLookup.apply(")
                    .append(parentInfo.fieldType).append(".class);\n");
            sb.append("            if (parent != null) {");
            if (parentInfo.needsReflection) {
                sb.append(" try { ").append(fieldConstantRef(fieldConstantName(parentInfo.fieldName)))
                        .append(".set(cmd, parent); } catch (IllegalAccessException e) { throw new RuntimeException(e); }");
            } else {
                sb.append(" ((").append(commandSimpleName).append(") cmd).")
                        .append(parentInfo.fieldName).append(" = parent;");
            }
            sb.append(" }\n        }\n");
        }
        sb.append("    }\n\n");
    }

    // --- Helper methods for createDirect() path ---

    /** Emit a converter expression inline (for createDirect() call). */
    private static void emitConverterExpression(StringBuilder sb, VariableElement field,
            String attributeName, Elements elementUtils, String fieldType) {
        String className = getFieldAnnotationClassValue(field, attributeName, elementUtils);
        if (className != null && !className.equals(NULL_CONVERTER)) {
            sb.append(forHelpGuard("new " + className + "()"));
        } else {
            // Shared local, already null on the help path (#675)
            sb.append("converter_").append(converterConstantSuffix(converterLookupType(fieldType)));
        }
    }

    /**
     * Variant of emitConverterExpression that can emit an EnumConverter directly
     * for enum types, avoiding the runtime CLConverterManager lookup (#591).
     */
    private static void emitConverterExpression(StringBuilder sb, VariableElement field,
            String attributeName, Elements elementUtils, String fieldType, TypeMirror effectiveType, Types typeUtils) {
        String className = getFieldAnnotationClassValue(field, attributeName, elementUtils);
        if (className != null && !className.equals(NULL_CONVERTER)) {
            sb.append(forHelpGuard("new " + className + "()"));
            return;
        }
        if (getEnumConstantNames(effectiveType, typeUtils) != null) {
            // Check if the type is an enum -- emit EnumConverter directly (#591).
            sb.append(forHelpGuard(
                    "new org.aesh.command.impl.converter.EnumConverter(" + fieldType + ".class)"));
            return;
        }
        // Shared local, already null on the help path (#675)
        sb.append("converter_").append(converterConstantSuffix(converterLookupType(fieldType)));
    }

    /**
     * Emit completer setter only for an explicit custom completer. Built-in
     * default completers (Boolean, allowedValues, File/Resource/Path) resolve
     * lazily in {@link org.aesh.command.impl.internal.ProcessedOption#completer()} (#789).
     */
    private static void emitCompleterSetter(StringBuilder sb, String var, VariableElement field,
            String attributeName, Elements elementUtils) {
        emitCallbackSetter(sb, var, "setCompleter", field, attributeName, NULL_OPTION_COMPLETER, elementUtils);
    }

    /** Emit parser setter if non-default (for createDirect() path). */
    private static void emitParserSetter(StringBuilder sb, String var, VariableElement field,
            String attributeName, Elements elementUtils) {
        String className = getFieldAnnotationClassValue(field, attributeName, elementUtils);
        if (className != null && !className.equals(AESH_OPTION_PARSER)) {
            sb.append("            if (!forHelp) ").append(var)
                    .append(".setParser(new ").append(className).append("());\n");
        }
    }

    /** Emit a generic callback setter (validator, activator, renderer). */
    private static void emitCallbackSetter(StringBuilder sb, String var, String setterName,
            VariableElement field, String attributeName, String nullSentinel, Elements elementUtils) {
        String className = getFieldAnnotationClassValue(field, attributeName, elementUtils);
        if (className != null && !className.equals(nullSentinel)) {
            sb.append("            if (!forHelp) ").append(var).append(".").append(setterName)
                    .append("(new ").append(className).append("());\n");
        }
    }

    /**
     * Emit setCompleteFallback() only when explicitly overridden on the
     * option/argument annotation. {@code DEFAULT} resolves lazily in
     * {@link org.aesh.command.impl.internal.ProcessedOption#getCompleteFallback()} (#789).
     */
    private static void emitCompleteFallbackSetter(StringBuilder sb, String var,
            org.aesh.command.option.CompletionFallback value) {
        if (value != org.aesh.command.option.CompletionFallback.DEFAULT) {
            sb.append("            ").append(var).append(".setCompleteFallback(org.aesh.command.option.CompletionFallback.")
                    .append(value.name()).append(");\n");
        }
    }

    private static void emitAliasesSetter(StringBuilder sb, String var, String[] aliases) {
        if (aliases != null && aliases.length > 0) {
            sb.append("            ").append(var).append(".setAliases(")
                    .append(listExpression(aliases)).append(");\n");
        }
    }

    private static void emitHelpGroupSetter(StringBuilder sb, String var, String helpGroup) {
        if (helpGroup != null && !helpGroup.isEmpty()) {
            sb.append("            ").append(var).append(".setHelpGroup(").append(stringLiteral(helpGroup)).append(");\n");
        }
    }

    private static void emitExclusiveWithSetter(StringBuilder sb, String var, String[] exclusiveWith) {
        if (exclusiveWith != null && exclusiveWith.length > 0) {
            sb.append("            ").append(var).append(".setExclusiveWith(")
                    .append(listExpression(exclusiveWith)).append(");\n");
        }
    }

    private static void emitAllowedValuesSetter(StringBuilder sb, String var, String[] allowedValues) {
        if (allowedValues != null && allowedValues.length > 0) {
            sb.append("            ").append(var).append(".setAllowedValues(")
                    .append(listExpression(allowedValues)).append(");\n");
        }
    }

    private static void emitVisibilitySetter(StringBuilder sb, String var,
            org.aesh.command.option.OptionVisibility visibility) {
        if (visibility != org.aesh.command.option.OptionVisibility.BRIEF) {
            sb.append("            ").append(var).append(".setVisibility(org.aesh.command.option.OptionVisibility.")
                    .append(visibility.name()).append(");\n");
        }
    }

    private static boolean hasReflectiveFields(List<FieldAccessorInfo> infos) {
        for (FieldAccessorInfo info : infos) {
            if (info.needsReflection && !info.synthetic)
                return true;
        }
        return false;
    }

    private static boolean isPrivateField(VariableElement field) {
        return field.getModifiers().contains(Modifier.PRIVATE);
    }

    /**
     * Whether generated code in the given package can name this field
     * directly: private fields never, otherwise only public fields or fields
     * in the same package (protected grants package access, not access from
     * the unrelated generated class, #785).
     */
    private static boolean isDirectlyReachable(VariableElement field, String commandPackage) {
        Set<Modifier> mods = field.getModifiers();
        if (mods.contains(Modifier.PRIVATE))
            return false;
        if (mods.contains(Modifier.PUBLIC))
            return true;
        return packageNameOf(field).equals(commandPackage);
    }

    /**
     * Whether an accessible no-arg constructor exists (explicit or the
     * default one). Commands without one cannot be instantiated by generated
     * code; their providers still work when the application passes its own
     * instances to {@code buildProcessedCommand} (#785).
     */
    static boolean hasAccessibleNoArgConstructor(TypeElement commandElement) {
        for (Element enclosed : commandElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
                ExecutableElement constructor = (ExecutableElement) enclosed;
                if (constructor.getParameters().isEmpty()
                        && !constructor.getModifiers().contains(Modifier.PRIVATE))
                    return true;
            }
        }
        return false;
    }

    private static String packageNameOf(Element element) {
        while (element != null && !(element instanceof PackageElement))
            element = element.getEnclosingElement();
        return element == null ? "" : ((PackageElement) element).getQualifiedName().toString();
    }

    /**
     * Whether the generated accessor must reach this option field through
     * reflection: private fields always, options of a private
     * {@code @Mixin} field (the mixin instance itself is unreadable), and
     * fields the generated class cannot name directly (e.g. a
     * package-visible mixin option in another package than the command,
     * #785).
     */
    private static boolean needsReflectiveAccess(VariableElement field, String commandPackage,
            boolean mixinFieldPrivate) {
        return isPrivateField(field) || mixinFieldPrivate || !isDirectlyReachable(field, commandPackage);
    }

    private static String fieldConstantName(String fieldName) {
        return "FIELD_" + fieldName.replace('.', '_');
    }

    /** Holder-qualified reference; reflection lives in Fields and must not run at provider clinit (#675). */
    private static String fieldConstantRef(String constName) {
        return "Fields." + constName;
    }

    private static String fieldConstantName(String mixinFieldName, String fieldName) {
        return "FIELD_" + mixinFieldName.replace('.', '_') + "_" + fieldName;
    }

    private static boolean hasAnnotation(VariableElement field) {
        return field.getAnnotation(Option.class) != null
                || field.getAnnotation(OptionList.class) != null
                || field.getAnnotation(OptionGroup.class) != null
                || field.getAnnotation(Argument.class) != null
                || field.getAnnotation(Arguments.class) != null;
    }

    private static String declaringClassName(VariableElement field) {
        return ((TypeElement) field.getEnclosingElement()).getQualifiedName().toString();
    }

    private static void generatePrivateFieldConstants(StringBuilder sb, String simpleName,
            String qualifiedCommandName, List<VariableElement> fields) {
        List<String[]> privateFields = new ArrayList<>();
        int dot = qualifiedCommandName.lastIndexOf('.');
        String commandPackage = dot < 0 ? "" : qualifiedCommandName.substring(0, dot);

        for (VariableElement field : fields) {
            if (isPrivateField(field) && (hasAnnotation(field) || field.getAnnotation(ParentCommand.class) != null)) {
                privateFields.add(new String[] {
                        fieldConstantName(field.getSimpleName().toString()),
                        declaringClassName(field),
                        field.getSimpleName().toString()
                });
            }
            if (field.getAnnotation(Mixin.class) != null) {
                String mixinFieldName = field.getSimpleName().toString();
                TypeMirror mixinType = field.asType();
                if (mixinType instanceof DeclaredType) {
                    TypeElement mixinElement = (TypeElement) ((DeclaredType) mixinType).asElement();
                    List<String[]> mixinPrivateFields = new ArrayList<>();
                    collectMixinPrivateFields(mixinPrivateFields, mixinFieldName, mixinElement, commandPackage,
                            isPrivateField(field));
                    // Only emit the mixin Field constant when the mixin field itself
                    // is private or at least one option inside it needs reflection (#789).
                    if (isPrivateField(field) || !mixinPrivateFields.isEmpty()) {
                        privateFields.add(new String[] {
                                fieldConstantName(mixinFieldName),
                                declaringClassName(field),
                                mixinFieldName
                        });
                        privateFields.addAll(mixinPrivateFields);
                    }
                }
            }
        }

        if (privateFields.isEmpty())
            return;

        // Reflection for private fields lives in a nested holder so it runs
        // on first Accessor use (execution path), never at provider
        // class-init (help path only builds metadata, #675).
        sb.append("    private static final class Fields {\n");
        for (String[] pf : privateFields) {
            sb.append("        static final java.lang.reflect.Field ").append(pf[0]).append(";\n");
        }
        sb.append("        static {\n");
        sb.append("            try {\n");
        for (String[] pf : privateFields) {
            sb.append("                ").append(pf[0]).append(" = ").append(pf[1])
                    .append(".class.getDeclaredField(\"").append(pf[2]).append("\");\n");
            sb.append("                ").append(pf[0]).append(".setAccessible(true);\n");
        }
        sb.append("            } catch (NoSuchFieldException e) {\n");
        sb.append("                throw new RuntimeException(e);\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
    }

    /**
     * Pre-scan fields to find distinct converter types resolved through
     * {@code CLConverterManager} at runtime. Callers emit one method-local
     * lookup per type on the execution path; the help path emits none (#675).
     * <p>
     * {@code @Option} enums without custom converters are excluded: the
     * emitter inlines a direct {@code EnumConverter} for them (#591) and
     * never touches the local.
     */
    private static java.util.LinkedHashSet<String> collectSharedConverterTypes(
            List<VariableElement> fields, Elements elementUtils, Types typeUtils,
            boolean includeGeneratedBoolean) {
        // Collect distinct default converter types (types that use CLConverterManager, not custom converters)
        java.util.LinkedHashSet<String> converterTypes = new java.util.LinkedHashSet<>();
        // Include Boolean only when generated help/version options actually need it (#789).
        if (includeGeneratedBoolean)
            converterTypes.add("java.lang.Boolean");

        for (VariableElement field : fields) {
            if (field.getAnnotation(Mixin.class) != null) {
                TypeMirror mixinType = field.asType();
                if (mixinType instanceof DeclaredType) {
                    TypeElement mixinElement = (TypeElement) ((DeclaredType) mixinType).asElement();
                    collectConverterTypes(converterTypes, mixinElement, elementUtils, typeUtils);
                }
                continue;
            }
            String customConverter = getFieldAnnotationClassValue(field, "converter", elementUtils);
            if (customConverter != null && !customConverter.equals(NULL_CONVERTER))
                continue; // custom converter, not shared
            if (field.getAnnotation(Option.class) != null || field.getAnnotation(Argument.class) != null) {
                TypeMirror effectiveType = field.asType();
                if (isOptionalType(effectiveType, typeUtils))
                    effectiveType = unwrapOptionalTypeMirror(effectiveType);
                if (field.getAnnotation(Option.class) != null
                        && getEnumConstantNames(effectiveType, typeUtils) != null)
                    continue; // direct EnumConverter, no shared lookup (#591)
                converterTypes.add(converterLookupType(getBoxedTypeName(effectiveType, typeUtils)));
            } else if (field.getAnnotation(OptionList.class) != null || field.getAnnotation(Arguments.class) != null) {
                TypeMirror effectiveType = field.asType();
                if (isOptionalType(effectiveType, typeUtils))
                    effectiveType = unwrapOptionalTypeMirror(effectiveType);
                converterTypes.add(converterLookupType(getElementTypeName(effectiveType, typeUtils)));
            } else if (field.getAnnotation(OptionGroup.class) != null) {
                TypeMirror effectiveType = field.asType();
                if (isOptionalType(effectiveType, typeUtils))
                    effectiveType = unwrapOptionalTypeMirror(effectiveType);
                converterTypes.add(converterLookupType(getGenericTypeArgument(effectiveType, 1, typeUtils)));
            }
        }
        return converterTypes;
    }

    /** Share the Boolean converter lookup between primitive boolean and java.lang.Boolean (#789). */
    private static String converterLookupType(String typeName) {
        return "boolean".equals(typeName) ? "java.lang.Boolean" : typeName;
    }

    /** Generate a safe Java identifier suffix from a type name. */
    private static String converterConstantSuffix(String typeName) {
        return typeName.replace('.', '_').replace('$', '_');
    }

    private static void collectConverterTypes(java.util.Set<String> types, TypeElement typeElement,
            Elements elementUtils, Types typeUtils) {
        for (javax.lang.model.element.Element enclosed : typeElement.getEnclosedElements()) {
            if (!(enclosed instanceof VariableElement))
                continue;
            VariableElement f = (VariableElement) enclosed;
            String customConverter = getFieldAnnotationClassValue(f, "converter", elementUtils);
            if (customConverter != null && !customConverter.equals(NULL_CONVERTER))
                continue;
            if (f.getAnnotation(Option.class) != null || f.getAnnotation(Argument.class) != null) {
                TypeMirror effectiveType = f.asType();
                if (isOptionalType(effectiveType, typeUtils))
                    effectiveType = unwrapOptionalTypeMirror(effectiveType);
                if (f.getAnnotation(Option.class) != null
                        && getEnumConstantNames(effectiveType, typeUtils) != null)
                    continue; // direct EnumConverter, no shared lookup (#591)
                types.add(converterLookupType(getBoxedTypeName(effectiveType, typeUtils)));
            } else if (f.getAnnotation(OptionList.class) != null || f.getAnnotation(Arguments.class) != null) {
                TypeMirror effectiveType = f.asType();
                if (isOptionalType(effectiveType, typeUtils))
                    effectiveType = unwrapOptionalTypeMirror(effectiveType);
                types.add(converterLookupType(getElementTypeName(effectiveType, typeUtils)));
            } else if (f.getAnnotation(OptionGroup.class) != null) {
                TypeMirror effectiveType = f.asType();
                if (isOptionalType(effectiveType, typeUtils))
                    effectiveType = unwrapOptionalTypeMirror(effectiveType);
                types.add(converterLookupType(getGenericTypeArgument(effectiveType, 1, typeUtils)));
            }
        }
        // Recurse into superclass
        TypeMirror superclass = typeElement.getSuperclass();
        if (superclass.getKind() != TypeKind.NONE && !erasedTypeName(superclass, typeUtils).equals("java.lang.Object")) {
            if (superclass instanceof DeclaredType) {
                collectConverterTypes(types, (TypeElement) ((DeclaredType) superclass).asElement(),
                        elementUtils, typeUtils);
            }
        }
    }

    private static void collectMixinPrivateFields(List<String[]> privateFields,
            String mixinFieldName, TypeElement mixinElement, String commandPackage, boolean mixinFieldPrivate) {
        String mixinClassName = mixinElement.getQualifiedName().toString();
        for (javax.lang.model.element.Element enclosed : mixinElement.getEnclosedElements()) {
            if (enclosed instanceof VariableElement) {
                VariableElement f = (VariableElement) enclosed;
                if (hasAnnotation(f) && needsReflectiveAccess(f, commandPackage, mixinFieldPrivate)) {
                    privateFields.add(new String[] {
                            fieldConstantName(mixinFieldName, f.getSimpleName().toString()),
                            mixinClassName,
                            f.getSimpleName().toString()
                    });
                }
            }
        }
        TypeMirror superclass = mixinElement.getSuperclass();
        if (superclass.getKind() != TypeKind.NONE && !superclass.toString().equals("java.lang.Object")) {
            if (superclass instanceof DeclaredType) {
                collectMixinPrivateFields(privateFields, mixinFieldName,
                        (TypeElement) ((DeclaredType) superclass).asElement(), commandPackage,
                        mixinFieldPrivate);
            }
        }
    }

    // --- Annotation class value extraction (handles MirroredTypeException) ---

    /**
     * Extract the Class value of a named annotation attribute from the field's
     * option annotation. Returns the fully-qualified class name, or null if not found.
     */
    private static String getFieldAnnotationClassValue(VariableElement field, String attributeName,
            Elements elementUtils) {
        // Check which annotation is on this field and extract from it
        String[] annotationTypes = {
                Option.class.getCanonicalName(),
                OptionList.class.getCanonicalName(),
                OptionGroup.class.getCanonicalName(),
                Arguments.class.getCanonicalName(),
                Argument.class.getCanonicalName()
        };

        for (AnnotationMirror mirror : field.getAnnotationMirrors()) {
            String annotationType = ((TypeElement) mirror.getAnnotationType().asElement())
                    .getQualifiedName().toString();
            for (String expected : annotationTypes) {
                if (annotationType.equals(expected)) {
                    return extractClassAttribute(mirror, attributeName);
                }
            }
        }
        return null;
    }

    private static String getAnnotationClassValue(TypeElement element, String annotationType,
            String attributeName, Elements elementUtils) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            String mirrorType = ((TypeElement) mirror.getAnnotationType().asElement())
                    .getQualifiedName().toString();
            if (mirrorType.equals(annotationType)) {
                return extractClassAttribute(mirror, attributeName);
            }
        }
        return null;
    }

    private static String extractClassAttribute(AnnotationMirror mirror, String attributeName) {
        for (java.util.Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues()
                .entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(attributeName)) {
                Object value = entry.getValue().getValue();
                if (value instanceof TypeMirror) {
                    return ((TypeMirror) value).toString();
                }
            }
        }
        return null;
    }

    private static boolean hasAnnotationType(TypeElement element, String annotationType) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            String mirrorType = ((TypeElement) mirror.getAnnotationType().asElement())
                    .getQualifiedName().toString();
            if (mirrorType.equals(annotationType))
                return true;
        }
        return false;
    }

    /**
     * Read a string or boolean annotation value via annotation mirrors.
     * Safe from MirroredTypesException.
     */
    private static String getAnnotationValue(TypeElement element, String attributeName, Elements elementUtils) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            String mirrorType = ((TypeElement) mirror.getAnnotationType().asElement())
                    .getQualifiedName().toString();
            if (mirrorType.equals(CommandDefinition.class.getCanonicalName())) {
                for (java.util.Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror
                        .getElementValues().entrySet()) {
                    if (entry.getKey().getSimpleName().toString().equals(attributeName)) {
                        return String.valueOf(entry.getValue().getValue());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Resolve an annotated {@code Toggle} to generated source. Defaults to
     * {@code INHERIT} when the annotation is absent or unreadable, so help
     * toggles behave the same with and without the processor.
     */
    private static String toggleValue(String annotated) {
        try {
            return "org.aesh.command.Toggle."
                    + org.aesh.command.Toggle.valueOf(annotated).name();
        } catch (IllegalArgumentException | NullPointerException e) {
            return "org.aesh.command.Toggle.INHERIT";
        }
    }

    /**
     * Resolve an annotated {@code ArgumentsSection} to generated source.
     * Defaults to {@code INHERIT} when the annotation is absent or
     * unreadable, so the arguments section behaves the same with and
     * without the processor.
     */
    private static String argumentsSectionValue(String annotated) {
        try {
            return "org.aesh.command.ArgumentsSection."
                    + org.aesh.command.ArgumentsSection.valueOf(annotated).name();
        } catch (IllegalArgumentException | NullPointerException e) {
            return "org.aesh.command.ArgumentsSection.INHERIT";
        }
    }

    /**
     * Read a String[] annotation value via annotation mirrors.
     */
    @SuppressWarnings("unchecked")
    private static String[] getAnnotationStringArrayValue(TypeElement element, String attributeName,
            Elements elementUtils) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            String mirrorType = ((TypeElement) mirror.getAnnotationType().asElement())
                    .getQualifiedName().toString();
            if (mirrorType.equals(CommandDefinition.class.getCanonicalName())) {
                for (java.util.Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror
                        .getElementValues().entrySet()) {
                    if (entry.getKey().getSimpleName().toString().equals(attributeName)) {
                        List<? extends AnnotationValue> values = (List<? extends AnnotationValue>) entry.getValue().getValue();
                        String[] result = new String[values.size()];
                        for (int i = 0; i < values.size(); i++) {
                            result[i] = String.valueOf(values.get(i).getValue());
                        }
                        return result;
                    }
                }
            }
        }
        return new String[0];
    }

    /**
     * Read a string annotation value from a {@code @MethodCommand} mirror.
     * Returns null when absent (e.g. an unset {@code name}, which derives
     * from the method name instead).
     */
    private static String getMethodCommandValue(ExecutableElement method, String attributeName) {
        for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
            String mirrorType = ((TypeElement) mirror.getAnnotationType().asElement())
                    .getQualifiedName().toString();
            if (mirrorType.equals(org.aesh.command.MethodCommand.class.getCanonicalName())) {
                for (java.util.Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror
                        .getElementValues().entrySet()) {
                    if (entry.getKey().getSimpleName().toString().equals(attributeName)) {
                        return String.valueOf(entry.getValue().getValue());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Read a String[] annotation value from a {@code @MethodCommand} mirror.
     */
    @SuppressWarnings("unchecked")
    private static String[] getMethodCommandStringArrayValue(ExecutableElement method, String attributeName) {
        for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
            String mirrorType = ((TypeElement) mirror.getAnnotationType().asElement())
                    .getQualifiedName().toString();
            if (mirrorType.equals(org.aesh.command.MethodCommand.class.getCanonicalName())) {
                for (java.util.Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror
                        .getElementValues().entrySet()) {
                    if (entry.getKey().getSimpleName().toString().equals(attributeName)) {
                        List<? extends AnnotationValue> values = (List<? extends AnnotationValue>) entry.getValue().getValue();
                        String[] result = new String[values.size()];
                        for (int i = 0; i < values.size(); i++) {
                            result[i] = String.valueOf(values.get(i).getValue());
                        }
                        return result;
                    }
                }
            }
        }
        return new String[0];
    }

    /**
     * Emit a setInitialValueFactory() call that creates an empty collection/map
     * of the declared field type without reflection or invokedynamic (#789).
     * Skipped on the help path because help builds never reset command fields.
     */
    private static void emitInitialValueFactory(StringBuilder sb, String var,
            TypeMirror fieldType, Types typeUtils) {
        TypeMirror effectiveType = fieldType;
        if (isOptionalType(effectiveType, typeUtils))
            effectiveType = unwrapOptionalTypeMirror(effectiveType);
        String erasedName = erasedTypeName(effectiveType, typeUtils);
        String factoryExpr = collectionFactoryExpression(erasedName);
        if (factoryExpr != null) {
            // Guarded so the help path never initializes CollectionFactory (#675, #792)
            sb.append("            if (!forHelp) ").append(var)
                    .append(".setInitialValueFactory(").append(factoryExpr).append(");\n");
        }
    }

    private static String collectionFactoryExpression(String erasedTypeName) {
        switch (erasedTypeName) {
            case "java.util.ArrayList":
            case "java.util.List":
            case "java.util.Collection":
                return "org.aesh.command.impl.internal.CollectionFactory.ARRAY_LIST";
            case "java.util.LinkedList":
                return "org.aesh.command.impl.internal.CollectionFactory.LINKED_LIST";
            case "java.util.HashSet":
            case "java.util.Set":
                return "org.aesh.command.impl.internal.CollectionFactory.HASH_SET";
            case "java.util.LinkedHashSet":
                return "org.aesh.command.impl.internal.CollectionFactory.LINKED_HASH_SET";
            case "java.util.TreeSet":
                return "org.aesh.command.impl.internal.CollectionFactory.TREE_SET";
            case "java.util.HashMap":
            case "java.util.Map":
                return "org.aesh.command.impl.internal.CollectionFactory.HASH_MAP";
            case "java.util.LinkedHashMap":
                return "org.aesh.command.impl.internal.CollectionFactory.LINKED_HASH_MAP";
            case "java.util.TreeMap":
                return "org.aesh.command.impl.internal.CollectionFactory.TREE_MAP";
            default:
                return null;
        }
    }

    // --- Type utility methods ---

    /**
     * Returns the erased qualified name of a type, stripping any type-use
     * annotations (#627).
     * <p>
     * {@code TypeMirror.toString()} includes type-use annotations (e.g. a field
     * declared {@code @NotBlank String} yields
     * {@code java.lang.@jakarta.validation.constraints.NotBlank String}).
     * Emitting that into generated code produces uncompilable output, and
     * string comparisons against it silently fail. This helper resolves the
     * {@link TypeElement} qualified name instead, which never carries
     * annotations.
     */
    private static String erasedTypeName(TypeMirror type, Types typeUtils) {
        if (type instanceof DeclaredType) {
            javax.lang.model.element.Element element = ((DeclaredType) type).asElement();
            if (element instanceof TypeElement) {
                return ((TypeElement) element).getQualifiedName().toString();
            }
        }
        if (type.getKind() == TypeKind.ARRAY && type instanceof javax.lang.model.type.ArrayType) {
            return erasedTypeName(((javax.lang.model.type.ArrayType) type).getComponentType(), typeUtils) + "[]";
        }
        // Fallback for type variables, wildcards and error types: erase, then
        // strip any remaining annotations textually.
        String erased;
        try {
            erased = typeUtils.erasure(type).toString();
        } catch (IllegalArgumentException e) {
            erased = type.toString();
        }
        return stripTypeAnnotations(erased);
    }

    /**
     * Removes {@code @Annotation} segments (with optional argument lists) from
     * a type name string. Fallback for type mirrors that cannot be resolved to
     * a {@link TypeElement}.
     */
    private static String stripTypeAnnotations(String typeName) {
        StringBuilder sb = new StringBuilder(typeName.length());
        int i = 0;
        while (i < typeName.length()) {
            char c = typeName.charAt(i);
            if (c != '@') {
                sb.append(c);
                i++;
                continue;
            }
            // Skip the annotation qualified name
            i++;
            while (i < typeName.length()) {
                char d = typeName.charAt(i);
                if (Character.isJavaIdentifierPart(d) || d == '.') {
                    i++;
                } else {
                    break;
                }
            }
            // Skip an optional argument list, honouring string literals
            if (i < typeName.length() && typeName.charAt(i) == '(') {
                int depth = 0;
                boolean inString = false;
                while (i < typeName.length()) {
                    char d = typeName.charAt(i);
                    if (inString) {
                        if (d == '\\') {
                            i += 2;
                            continue;
                        }
                        if (d == '"') {
                            inString = false;
                        }
                        i++;
                    } else if (d == '"') {
                        inString = true;
                        i++;
                    } else if (d == '(') {
                        depth++;
                        i++;
                    } else if (d == ')') {
                        depth--;
                        i++;
                        if (depth == 0) {
                            break;
                        }
                    } else {
                        i++;
                    }
                }
            }
            // Skip the separating whitespace between annotation and type
            while (i < typeName.length() && typeName.charAt(i) == ' ') {
                i++;
            }
        }
        return sb.toString();
    }

    private static boolean isOptionalType(TypeMirror type, Types typeUtils) {
        return erasedTypeName(type, typeUtils).equals("java.util.Optional");
    }

    /**
     * Unwrap Optional&lt;T&gt; to get T from a field's type mirror.
     */
    private static TypeMirror unwrapOptionalTypeMirror(TypeMirror type) {
        if (type instanceof DeclaredType) {
            DeclaredType declaredType = (DeclaredType) type;
            List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
            if (!typeArgs.isEmpty()) {
                return typeArgs.get(0);
            }
        }
        return type;
    }

    private static String getBoxedTypeName(TypeMirror type, Types typeUtils) {
        switch (type.getKind()) {
            case BOOLEAN:
                return "boolean";
            case BYTE:
                return "byte";
            case SHORT:
                return "short";
            case INT:
                return "int";
            case LONG:
                return "long";
            case FLOAT:
                return "float";
            case DOUBLE:
                return "double";
            case CHAR:
                return "char";
            default:
                return erasedTypeName(type, typeUtils);
        }
    }

    private static String getGenericTypeArgument(TypeMirror type, int index, Types typeUtils) {
        if (type instanceof DeclaredType) {
            DeclaredType declaredType = (DeclaredType) type;
            List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
            if (typeArgs.size() > index) {
                return erasedTypeName(typeArgs.get(index), typeUtils);
            }
        }
        return "Object";
    }

    /**
     * Element type name for multi-value fields: the component type for arrays
     * (#704), otherwise the first generic type argument.
     */
    private static String getElementTypeName(TypeMirror effectiveType, Types typeUtils) {
        if (effectiveType.getKind() == TypeKind.ARRAY
                && effectiveType instanceof javax.lang.model.type.ArrayType)
            return erasedTypeName(
                    ((javax.lang.model.type.ArrayType) effectiveType).getComponentType(), typeUtils);
        return getGenericTypeArgument(effectiveType, 0, typeUtils);
    }

    // --- String literal helpers ---

    private static String stringLiteral(String value) {
        if (value == null)
            return "null";
        return "\"" + escapeJavaString(value) + "\"";
    }

    private static String charLiteral(char value) {
        if (value == '\u0000')
            return "'\\u0000'";
        if (value == '\'')
            return "'\\''";
        if (value == '\\')
            return "'\\\\'";
        if (value == '\n')
            return "'\\n'";
        if (value == '\r')
            return "'\\r'";
        if (value == '\t')
            return "'\\t'";
        return "'" + value + "'";
    }

    private static String stringArrayLiteral(String[] values) {
        if (values == null || values.length == 0)
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(stringLiteral(values[i]));
        }
        return sb.toString();
    }

    /**
     * List literal for static option metadata: singleton lists avoid the
     * backing array of single-element {@code Arrays.asList} (#791).
     */
    private static String listExpression(String[] values) {
        if (values != null && values.length == 1)
            return "java.util.Collections.singletonList(" + stringLiteral(values[0]) + ")";
        return "java.util.Arrays.asList(" + stringArrayLiteral(values) + ")";
    }

    private static String stringArrayLiteralAsNew(String[] values) {
        if (values == null || values.length == 0) {
            return "new String[0]";
        }
        StringBuilder sb = new StringBuilder("new String[] {");
        for (int i = 0; i < values.length; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(stringLiteral(values[i]));
        }
        sb.append("}");
        return sb.toString();
    }

    private static String selectorLiteral(org.aesh.selector.SelectorType selectorType) {
        return "org.aesh.selector.SelectorType." + selectorType.name();
    }

    private static String escapeJavaString(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Generate the metadata registry source code.
     * <p>
     * The registry uses a {@code switch} statement on the command class's
     * binary name to lazily instantiate only the requested metadata provider
     * via a direct {@code new} call — no reflection, no lambdas.
     *
     * @param packageName the package for the registry class (may be empty)
     * @param registrySimpleName the simple name of the registry class
     * @param entries list of (binaryClassName, metadataClassName) pairs
     * @return the generated Java source code
     */
    static String generateRegistry(String packageName, String registrySimpleName, List<String[]> entries,
            java.util.Map<String, List<String[]>> methodEntries) {
        StringBuilder sb = new StringBuilder();

        if (!packageName.isEmpty()) {
            sb.append("package ").append(packageName).append(";\n\n");
        }

        sb.append("/**\n");
        sb.append(" * Generated metadata registry — maps command class names to their\n");
        sb.append(" * generated metadata providers via a switch statement.\n");
        sb.append(" * Do not edit — this file is regenerated by the aesh annotation processor.\n");
        sb.append(" */\n");
        sb.append("public final class ").append(registrySimpleName).append(
                " implements org.aesh.command.metadata.MetadataRegistry {\n\n");

        sb.append("    @Override\n");
        sb.append("    public org.aesh.command.metadata.CommandMetadataProvider<?> get(String commandClassName) {\n");
        sb.append("        switch (commandClassName) {\n");
        for (String[] entry : entries) {
            sb.append("            case \"").append(entry[0]).append("\": return new ").append(entry[1]).append("();\n");
        }
        sb.append("            default: return null;\n");
        sb.append("        }\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append(
                "    public java.util.List<org.aesh.command.metadata.MethodCommandMetadataProvider> methodProviders(String ownerClassName) {\n");
        sb.append("        switch (ownerClassName) {\n");
        for (java.util.Map.Entry<String, List<String[]>> owner : methodEntries.entrySet()) {
            sb.append("            case \"").append(owner.getKey()).append("\": return java.util.Arrays.asList(\n");
            List<String[]> providers = owner.getValue();
            for (int i = 0; i < providers.size(); i++) {
                sb.append("                    new ").append(providers.get(i)[1]).append("()");
                sb.append(i < providers.size() - 1 ? ",\n" : "\n");
            }
            sb.append("            );\n");
        }
        sb.append("            default: return java.util.Collections.emptyList();\n");
        sb.append("        }\n");
        sb.append("    }\n");

        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Resolve the CLI command name for a {@code @MethodCommand} method:
     * the explicit annotation name, or the method name verbatim when
     * unset — mirroring the reflection path's derivation rule.
     */
    static String methodCommandName(ExecutableElement method) {
        String name = getMethodCommandValue(method, "name");
        if (name == null || name.isEmpty())
            return method.getSimpleName().toString();
        return name;
    }

    /**
     * Generate a {@code MethodCommandMetadataProvider} implementation for
     * one {@code @MethodCommand}-annotated method. Parameter options reuse
     * {@link #generateOption} and {@link #generateArgument} with the
     * generated adapter as the accessor target; execution is a direct call
     * on the caller-supplied owner, never reflection.
     */
    static String generateMethodProvider(
            String packageName, String ownerTypeRef, String metadataClassName,
            ExecutableElement method, Elements elementUtils, Types typeUtils) {
        StringBuilder sb = new StringBuilder();

        if (!packageName.isEmpty()) {
            sb.append("package ").append(packageName).append(";\n\n");
        }

        sb.append("import org.aesh.command.Command;\n");
        sb.append("import org.aesh.command.CommandException;\n");
        sb.append("import org.aesh.command.CommandResult;\n");
        sb.append("import org.aesh.command.impl.internal.ProcessedCommand;\n");
        sb.append("import org.aesh.command.impl.internal.ProcessedOption;\n");
        sb.append("import org.aesh.command.impl.internal.IndexedFieldAccessor;\n");
        sb.append("import org.aesh.command.impl.internal.OptionType;\n");
        sb.append("import org.aesh.command.invocation.CommandInvocation;\n");
        sb.append("import org.aesh.command.metadata.MethodCommandMetadataProvider;\n");
        sb.append("import org.aesh.command.parser.CommandLineParserException;\n\n");

        String cliName = methodCommandName(method);
        String[] aliases = getMethodCommandStringArrayValue(method, "aliases");
        String description = getMethodCommandValue(method, "description");
        if (description == null)
            description = "";

        StringBuilder body = new StringBuilder();

        body.append("    @Override\n");
        body.append("    public Class<?> ownerType() {\n");
        body.append("        return ").append(ownerTypeRef).append(".class;\n");
        body.append("    }\n\n");

        body.append("    @Override\n");
        body.append("    public String methodName() {\n");
        body.append("        return ").append(stringLiteral(method.getSimpleName().toString())).append(";\n");
        body.append("    }\n\n");

        body.append("    @Override\n");
        body.append("    public String commandName() {\n");
        body.append("        return ").append(stringLiteral(cliName)).append(";\n");
        body.append("    }\n\n");

        if (aliases.length > 0) {
            // Cached: registration reads aliases per lookup, callers never mutate (#791)
            body.append("    private static final String[] COMMAND_ALIASES = new String[] {")
                    .append(stringArrayLiteral(aliases)).append("};\n");
            body.append("    @Override\n");
            body.append("    public String[] commandAliases() {\n");
            body.append("        return COMMAND_ALIASES;\n");
            body.append("    }\n\n");
        }

        List<VariableElement> params = new java.util.ArrayList<VariableElement>(method.getParameters());
        List<FieldAccessorInfo> accessorInfos = new java.util.ArrayList<>();
        java.util.LinkedHashSet<String> sharedConverters = collectSharedConverterTypes(params, elementUtils, typeUtils, false);

        body.append("    @Override\n");
        body.append("    public ProcessedCommand buildProcessedCommand(Object owner)");
        body.append(" throws CommandLineParserException {\n");
        body.append("        return build(owner, false);\n");
        body.append("    }\n\n");

        body.append("    @Override\n");
        body.append("    public ProcessedCommand buildHelpProcessedCommand()");
        body.append(" throws CommandLineParserException {\n");
        body.append("        return build(null, true);\n");
        body.append("    }\n\n");

        // Shared build so parameter metadata is emitted once; exec-only work
        // is guarded by !forHelp (#792, #675).
        body.append("    private ProcessedCommand build(Object owner, boolean forHelp)");
        body.append(" throws CommandLineParserException {\n");
        generateMethodBuildProcessedCommand(body, ownerTypeRef, method, params, cliName, aliases,
                description, elementUtils, typeUtils, accessorInfos, sharedConverters);
        body.append("    }\n\n");

        boolean hasRealFields = hasRealFieldAccessors(accessorInfos);
        if (hasRealFields) {
            generateIndexedFieldAccessorMethods(body, accessorInfos);
        }
        generateMethodAdapter(body, ownerTypeRef, method, params, elementUtils, typeUtils);

        sb.append("/**\n");
        sb.append(" * Generated compile-time metadata provider for method command '")
                .append(cliName).append("'.\n");
        sb.append(" * Do not edit — this file is regenerated by the aesh annotation processor.\n");
        sb.append(" */\n");
        sb.append("@SuppressWarnings({\"unchecked\", \"rawtypes\"})\n");
        sb.append("public final class ").append(metadataClassName);
        sb.append(" implements MethodCommandMetadataProvider");
        if (hasRealFields)
            sb.append(", IndexedFieldAccessor");
        sb.append(" {\n\n");
        sb.append(body);
        sb.append("}\n");

        return sb.toString();
    }

    private static void generateMethodBuildProcessedCommand(StringBuilder sb, String ownerTypeRef,
            ExecutableElement method, List<? extends VariableElement> params,
            String cliName, String[] aliases, String description,
            Elements elementUtils, Types typeUtils,
            List<FieldAccessorInfo> accessorInfos,
            java.util.LinkedHashSet<String> sharedConverters) {
        int optionCount = 0;
        for (VariableElement param : params) {
            if (param.getAnnotation(Option.class) != null)
                optionCount++;
        }
        sb.append("        ProcessedCommand processedCommand = ProcessedCommand.createDirect(\n");
        sb.append("                ").append(stringLiteral(cliName)).append(",\n");
        sb.append("                ").append(stringLiteral(description)).append(",\n");
        sb.append("                ").append(forHelpGuard("new Adapter((" + ownerTypeRef + ") owner)"))
                .append(",\n");
        sb.append("                null, null, false, false, false,\n");
        sb.append("                ").append(optionCount).append(");\n\n");
        if (aliases.length > 0) {
            sb.append("        processedCommand.setAliases(").append(listExpression(aliases)).append(");\n\n");
        }

        // Converter locals gated by forHelp: the help path resolves none (#675)
        for (String type : sharedConverters) {
            sb.append("        org.aesh.command.converter.Converter converter_")
                    .append(converterConstantSuffix(type))
                    .append(" = forHelp ? null : org.aesh.converter.CLConverterManager.getInstance().getConverter(")
                    .append(type).append(".class);\n");
        }
        if (!sharedConverters.isEmpty())
            sb.append("\n");
        boolean hasOptionParams = false;
        for (VariableElement param : params) {
            if (param.getAnnotation(Option.class) != null || param.getAnnotation(Argument.class) != null
                    || param.getAnnotation(OptionList.class) != null || param.getAnnotation(Arguments.class) != null) {
                hasOptionParams = true;
                break;
            }
        }
        if (hasOptionParams) {
            sb.append("        org.aesh.command.impl.internal.IndexedFieldAccessor accessor = forHelp ? null : this;\n\n");
        }

        for (int i = 0; i < params.size(); i++) {
            generateMethodParameter(sb, params.get(i), i, elementUtils, typeUtils, accessorInfos,
                    elementUtils.getPackageOf(method).getQualifiedName().toString(), false);
        }

        sb.append("        return processedCommand;\n");
    }

    /**
     * Map one method parameter to an option, reusing the field emitters
     * with the generated adapter as the accessor target and positional
     * {@code p<i>} fields. {@code CommandInvocation} parameters carry no
     * option and are passed through at execution time.
     */
    private static void generateMethodParameter(StringBuilder sb, VariableElement param, int paramIndex,
            Elements elementUtils, Types typeUtils, List<FieldAccessorInfo> accessorInfos,
            String commandPackage, boolean mixinFieldPrivate) {
        if (isCommandInvocationParam(param, elementUtils, typeUtils))
            return;
        String target = "p" + paramIndex;
        Option o = param.getAnnotation(Option.class);
        Argument arg = param.getAnnotation(Argument.class);
        OptionList ol = param.getAnnotation(OptionList.class);
        Arguments a = param.getAnnotation(Arguments.class);
        if (o != null) {
            generateOption(sb, "Adapter", param, o, null, elementUtils, typeUtils, accessorInfos,
                    target, commandPackage, false);
        } else if (arg != null) {
            generateArgument(sb, "Adapter", param, arg, null, elementUtils, typeUtils, accessorInfos,
                    target, commandPackage, false);
        } else if (ol != null) {
            generateOptionList(sb, "Adapter", param, ol, null, elementUtils, typeUtils, accessorInfos,
                    target, commandPackage, false);
        } else if (a != null) {
            generateArguments(sb, "Adapter", param, a, null, elementUtils, typeUtils, accessorInfos,
                    target, commandPackage, false);
        }
    }

    private static boolean isCommandInvocationParam(VariableElement param,
            Elements elementUtils, Types typeUtils) {
        TypeElement invocationType = elementUtils.getTypeElement("org.aesh.command.invocation.CommandInvocation");
        if (invocationType == null)
            return false;
        return typeUtils.isAssignable(typeUtils.erasure(param.asType()),
                typeUtils.erasure(invocationType.asType()));
    }

    /**
     * Emit the generated adapter: typed parameter fields populated through
     * the generated accessor, and a direct call on the owner — never
     * reflection. Invocation parameters are passed straight through.
     */
    private static void generateMethodAdapter(StringBuilder sb, String ownerTypeRef,
            ExecutableElement method, List<? extends VariableElement> params,
            Elements elementUtils, Types typeUtils) {
        List<? extends VariableElement> methodParams = method.getParameters();
        sb.append("    @SuppressWarnings({\"unchecked\", \"rawtypes\"})\n");
        sb.append("    static final class Adapter implements Command<CommandInvocation> {\n");
        sb.append("        final ").append(ownerTypeRef).append(" owner;\n");
        for (int i = 0; i < methodParams.size(); i++) {
            VariableElement param = methodParams.get(i);
            if (isCommandInvocationParam(param, elementUtils, typeUtils))
                continue;
            sb.append("        ").append(erasedTypeName(param.asType(), typeUtils))
                    .append(" p").append(i).append(";\n");
        }
        sb.append("\n");
        sb.append("        Adapter(").append(ownerTypeRef).append(" owner) {\n");
        sb.append("            this.owner = owner;\n");
        sb.append("        }\n\n");
        sb.append("        @Override\n");
        sb.append("        public CommandResult execute(CommandInvocation invocation)")
                .append(" throws CommandException, InterruptedException {\n");
        sb.append("            try {\n");
        StringBuilder call = new StringBuilder();
        if (method.getModifiers().contains(Modifier.STATIC))
            call.append(ownerTypeRef);
        else
            call.append("owner");
        call.append(".").append(method.getSimpleName().toString()).append("(");
        boolean first = true;
        for (int i = 0; i < methodParams.size(); i++) {
            if (!first)
                call.append(", ");
            first = false;
            if (isCommandInvocationParam(methodParams.get(i), elementUtils, typeUtils))
                call.append("invocation");
            else
                call.append("p").append(i);
        }
        call.append(")");
        TypeMirror returnType = method.getReturnType();
        boolean isVoid = returnType.getKind() == TypeKind.VOID
                || erasedTypeName(returnType, typeUtils).equals("java.lang.Void");
        if (isVoid) {
            sb.append("                ").append(call).append(";\n");
            sb.append("                return CommandResult.SUCCESS;\n");
        } else {
            sb.append("                CommandResult result = ").append(call).append(";\n");
            sb.append("                return result != null ? result : CommandResult.SUCCESS;\n");
        }
        sb.append("            }");
        emitMethodExecuteCatches(sb, method, elementUtils, typeUtils);
        sb.append("        }\n");
        sb.append("    }\n\n");
    }

    /**
     * Emit catch clauses mirroring the reflection adapter's exception
     * mapping: declared {@code CommandException}/{@code InterruptedException}
     * pass through only when the method can actually throw them (otherwise
     * the catch would not compile); unchecked types always pass through
     * and anything else wraps in {@code CommandException}.
     */
    private static void emitMethodExecuteCatches(StringBuilder sb, ExecutableElement method,
            Elements elementUtils, Types typeUtils) {
        boolean catchesCommandException = false;
        boolean catchesInterrupted = false;
        TypeElement commandExceptionType = elementUtils.getTypeElement("org.aesh.command.CommandException");
        TypeElement interruptedType = elementUtils.getTypeElement("java.lang.InterruptedException");
        for (TypeMirror thrown : method.getThrownTypes()) {
            TypeMirror erased = typeUtils.erasure(thrown);
            if (!catchesCommandException && commandExceptionType != null
                    && (typeUtils.isAssignable(erased, typeUtils.erasure(commandExceptionType.asType()))
                            || typeUtils.isAssignable(typeUtils.erasure(commandExceptionType.asType()), erased)))
                catchesCommandException = true;
            if (!catchesInterrupted && interruptedType != null
                    && (typeUtils.isAssignable(erased, typeUtils.erasure(interruptedType.asType()))
                            || typeUtils.isAssignable(typeUtils.erasure(interruptedType.asType()), erased)))
                catchesInterrupted = true;
        }
        if (catchesCommandException)
            sb.append(" catch (CommandException e) {\n                throw e;\n            }");
        if (catchesInterrupted)
            sb.append(" catch (InterruptedException e) {\n                throw e;\n            }");
        sb.append(" catch (RuntimeException e) {\n                throw e;\n            }");
        sb.append(" catch (Error e) {\n                throw e;\n            }");
        sb.append(" catch (Throwable e) {\n");
        sb.append("                throw new CommandException(e);\n            }\n");
    }

}
