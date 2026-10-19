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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;

/**
 * JSR 269 annotation processor that generates {@code CommandMetadataProvider}
 * implementations for classes annotated with {@link CommandDefinition}.
 *
 * @author Aesh team
 */
@SupportedAnnotationTypes({
        "org.aesh.command.CommandDefinition",
        "org.aesh.command.MethodCommand"
})
@SupportedOptions({
        AeshAnnotationProcessor.OPT_PROJECT,
        AeshAnnotationProcessor.OPT_DISABLE_NATIVE_IMAGE
})
public class AeshAnnotationProcessor extends AbstractProcessor {

    /** Processor option: subdirectory name for native-image config output. */
    static final String OPT_PROJECT = "aeshNativeImageProject";
    /** Processor option: set to "true" to skip native-image config generation. */
    static final String OPT_DISABLE_NATIVE_IMAGE = "aeshNativeImageDisable";

    private Filer filer;
    private Messager messager;
    private Elements elementUtils;
    private Types typeUtils;
    /** Pairs of (binaryClassName, metadataSimpleName) for the registry switch. */
    private final List<String[]> registryEntries = new ArrayList<>();
    /**
     * Method-command providers by owner binary name, each entry a
     * (methodName, metadataClassName) pair for the registry switch.
     */
    private final Map<String, List<String[]>> methodRegistryEntries = new LinkedHashMap<>();
    private String registryPackage;
    private boolean registryGenerated;
    /** Private fields needing reflection config, grouped by declaring class name. */
    private final Map<String, Set<String>> reflectConfigEntries = new LinkedHashMap<>();

    /**
     * Reported dynamically instead of via {@code @SupportedSourceVersion} so that javac
     * does not warn that the processor supports a source version older than {@code -source}.
     * The generated code only uses Java 8 constructs, so any newer source level is fine.
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            // Write resource files in the final round (resources don't trigger the warning)
            if (!registryEntries.isEmpty() || !methodRegistryEntries.isEmpty()) {
                writeServiceFile();
                writeNativeImageConfigs();
            }
            return false;
        }

        Set<TypeElement> commandElements = new LinkedHashSet<>();

        for (Element element : roundEnv.getElementsAnnotatedWith(CommandDefinition.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                commandElements.add((TypeElement) element);
            }
        }

        for (TypeElement commandElement : commandElements) {
            if (!validate(commandElement)) {
                continue;
            }
            try {
                generateProvider(commandElement);
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Failed to generate metadata provider: " + e.getMessage(), commandElement);
            }
        }

        Map<TypeElement, List<ExecutableElement>> methodElements = new LinkedHashMap<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(
                org.aesh.command.MethodCommand.class)) {
            if (element.getKind() != ElementKind.METHOD)
                continue;
            ExecutableElement method = (ExecutableElement) element;
            Element enclosing = method.getEnclosingElement();
            if (!(enclosing instanceof TypeElement))
                continue;
            methodElements.computeIfAbsent((TypeElement) enclosing, k -> new ArrayList<>()).add(method);
        }

        boolean methodsSeen = false;
        for (Map.Entry<TypeElement, List<ExecutableElement>> entry : methodElements.entrySet()) {
            methodsSeen = true;
            List<ExecutableElement> valid = new ArrayList<>();
            for (ExecutableElement method : entry.getValue()) {
                if (validateMethod(method))
                    valid.add(method);
            }
            valid = withoutDuplicateCommands(entry.getKey(), valid);
            for (ExecutableElement method : valid) {
                try {
                    generateMethodProvider(entry.getKey(), method);
                } catch (IOException e) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Failed to generate method metadata provider: " + e.getMessage(), method);
                }
            }
        }

        // Generate the registry source file in the same round as the providers,
        // not in the final round, to avoid javac's "created in the last round" warning (#492).
        if ((!registryEntries.isEmpty() || !methodRegistryEntries.isEmpty())
                && (!commandElements.isEmpty() || methodsSeen) && !registryGenerated) {
            generateRegistryClass();
            registryGenerated = true;
        }

        return false;
    }

    private boolean validate(TypeElement element) {
        boolean valid = true;

        if (element.getModifiers().contains(Modifier.ABSTRACT)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Command class must not be abstract", element);
            valid = false;
        }

        TypeMirror commandType = elementUtils.getTypeElement(Command.class.getCanonicalName()).asType();
        if (!typeUtils.isAssignable(typeUtils.erasure(element.asType()), typeUtils.erasure(commandType))) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Command class must implement org.aesh.command.Command", element);
            valid = false;
        }

        boolean hasNoArgConstructor = false;
        for (Element enclosed : element.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
                ExecutableElement constructor = (ExecutableElement) enclosed;
                if (constructor.getParameters().isEmpty()
                        && !constructor.getModifiers().contains(Modifier.PRIVATE)) {
                    hasNoArgConstructor = true;
                    break;
                }
            }
        }
        if (!hasNoArgConstructor) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Command class must have an accessible no-arg constructor", element);
            valid = false;
        }

        // Validate field types
        List<VariableElement> fields = collectFields(element);
        for (VariableElement field : fields) {
            validateField(field);
        }

        // Validate positional index ranges don't overlap
        validatePositionalIndexes(element, fields);

        return valid;
    }

    private void validatePositionalIndexes(TypeElement element, List<VariableElement> fields) {
        List<String[]> positionals = new ArrayList<>(); // [fieldName, indexString]
        for (VariableElement field : fields) {
            org.aesh.command.option.Argument arg = field.getAnnotation(org.aesh.command.option.Argument.class);
            if (arg != null && !arg.index().isEmpty()) {
                positionals.add(new String[] { field.getSimpleName().toString(), arg.index() });
            }
            org.aesh.command.option.Arguments args = field.getAnnotation(org.aesh.command.option.Arguments.class);
            if (args != null && !args.index().isEmpty()) {
                positionals.add(new String[] { field.getSimpleName().toString(), args.index() });
            }
        }
        for (int i = 0; i < positionals.size(); i++) {
            org.aesh.command.option.IndexRange left;
            try {
                left = org.aesh.command.option.IndexRange.parse(positionals.get(i)[1]);
            } catch (IllegalArgumentException e) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Invalid index range on field '" + positionals.get(i)[0] + "': " + e.getMessage(), element);
                continue;
            }
            for (int j = i + 1; j < positionals.size(); j++) {
                org.aesh.command.option.IndexRange right;
                try {
                    right = org.aesh.command.option.IndexRange.parse(positionals.get(j)[1]);
                } catch (IllegalArgumentException e) {
                    continue; // already reported above
                }
                if (left.overlaps(right)) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Positional index ranges overlap: field '" + positionals.get(i)[0]
                                    + "' (" + positionals.get(i)[1] + ") and field '"
                                    + positionals.get(j)[0] + "' (" + positionals.get(j)[1] + ")",
                            element);
                }
            }
        }
    }

    private void validateField(VariableElement field) {
        if (field.getModifiers().contains(Modifier.PRIVATE)
                && (hasAeshAnnotation(field)
                        || field.getAnnotation(org.aesh.command.option.Mixin.class) != null
                        || field.getAnnotation(org.aesh.command.option.ParentCommand.class) != null)) {
            messager.printMessage(Diagnostic.Kind.WARNING,
                    "Private field '" + field.getSimpleName()
                            + "' requires reflective access at runtime. "
                            + "Consider using package-private visibility for reflection-free "
                            + "access from the generated metadata class.",
                    field);
        }
        if (field.getAnnotation(org.aesh.command.option.OptionList.class) != null) {
            TypeMirror collectionType = elementUtils
                    .getTypeElement(Collection.class.getCanonicalName()).asType();
            if (!typeUtils.isAssignable(typeUtils.erasure(field.asType()),
                    typeUtils.erasure(collectionType))) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "OptionList field must be instance of Collection", field);
            }
        }
        if (field.getAnnotation(org.aesh.command.option.Arguments.class) != null) {
            TypeMirror collectionType = elementUtils
                    .getTypeElement(Collection.class.getCanonicalName()).asType();
            if (!typeUtils.isAssignable(typeUtils.erasure(field.asType()),
                    typeUtils.erasure(collectionType))) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Arguments field must be instance of Collection", field);
            }
        }
        if (field.getAnnotation(org.aesh.command.option.OptionGroup.class) != null) {
            TypeMirror mapType = elementUtils
                    .getTypeElement(Map.class.getCanonicalName()).asType();
            if (!typeUtils.isAssignable(typeUtils.erasure(field.asType()),
                    typeUtils.erasure(mapType))) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "OptionGroup field must be instance of Map", field);
            }
        }
    }

    private List<VariableElement> collectFields(TypeElement element) {
        List<VariableElement> fields = new ArrayList<>();
        collectFieldsRecursive(element, fields);
        return fields;
    }

    private void collectFieldsRecursive(TypeElement element, List<VariableElement> fields) {
        for (Element enclosed : element.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                fields.add((VariableElement) enclosed);
            }
        }
        TypeMirror superclass = element.getSuperclass();
        if (superclass.getKind() != TypeKind.NONE) {
            Element superElement = typeUtils.asElement(superclass);
            if (superElement instanceof TypeElement) {
                collectFieldsRecursive((TypeElement) superElement, fields);
            }
        }
    }

    private void generateProvider(TypeElement commandElement) throws IOException {
        String qualifiedName = commandElement.getQualifiedName().toString();
        String packageName = elementUtils.getPackageOf(commandElement).getQualifiedName().toString();

        // For inner classes (e.g., Config.Set), we need:
        // - typeRefName: "Config.Set" for use in generated code type references
        // - metadataClassName: "Config_Set_AeshMetadata" to avoid clashing with enclosing class
        String typeRefName;
        String metadataClassName;
        if (packageName.isEmpty()) {
            typeRefName = qualifiedName;
            metadataClassName = qualifiedName.replace('.', '_') + "_AeshMetadata";
        } else {
            // Strip package prefix to get e.g. "Config.Set" for inner classes or "Batch" for top-level
            typeRefName = qualifiedName.substring(packageName.length() + 1);
            metadataClassName = typeRefName.replace('.', '_') + "_AeshMetadata";
        }
        String fullMetadataName = packageName.isEmpty() ? metadataClassName : packageName + "." + metadataClassName;

        // A command is a group if it declares groupCommands={...} in the annotation
        // OR if it implements the GroupCommand interface (#542)
        boolean isGroup = hasGroupCommands(commandElement) || implementsGroupCommand(commandElement);

        List<VariableElement> fields = collectFields(commandElement);

        String code = CodeGenerator.generate(
                packageName, typeRefName, metadataClassName, qualifiedName,
                commandElement, fields, isGroup, elementUtils, typeUtils);

        JavaFileObject sourceFile = filer.createSourceFile(fullMetadataName, commandElement);
        try (Writer writer = sourceFile.openWriter()) {
            writer.write(code);
        }

        // Collect registry entry: binary name (with $ for inner classes) -> fully-qualified metadata class name
        String binaryName = elementUtils.getBinaryName(commandElement).toString();
        registryEntries.add(new String[] { binaryName, fullMetadataName });

        // Use the first package we see as the registry package
        if (registryPackage == null) {
            registryPackage = packageName;
        }

        // Collect private fields for native-image reflect-config.json
        collectPrivateFieldsForReflectConfig(fields);
    }

    private void generateMethodProvider(TypeElement owner, ExecutableElement method) throws IOException {
        String ownerQualified = owner.getQualifiedName().toString();
        String packageName = elementUtils.getPackageOf(owner).getQualifiedName().toString();

        String ownerTypeRef;
        if (packageName.isEmpty()) {
            ownerTypeRef = ownerQualified;
        } else {
            ownerTypeRef = ownerQualified.substring(packageName.length() + 1);
        }
        String ownerSimple = ownerTypeRef.replace('.', '_');
        String methodName = method.getSimpleName().toString();
        // Arity keeps overloads collision-free: same name plus same arity
        // cannot overload in Java, so (name, arity) is unique per owner.
        String metadataClassName = ownerSimple + "_" + methodName + "_"
                + method.getParameters().size() + "_AeshMethodMetadata";
        String fullMetadataName = packageName.isEmpty() ? metadataClassName : packageName + "." + metadataClassName;

        String code = CodeGenerator.generateMethodProvider(
                packageName, ownerTypeRef, metadataClassName, method, elementUtils, typeUtils);

        JavaFileObject sourceFile = filer.createSourceFile(fullMetadataName, method);
        try (Writer writer = sourceFile.openWriter()) {
            writer.write(code);
        }

        String binaryName = elementUtils.getBinaryName(owner).toString();
        methodRegistryEntries.computeIfAbsent(binaryName, k -> new ArrayList<>())
                .add(new String[] { methodName, fullMetadataName });

        if (registryPackage == null) {
            registryPackage = packageName;
        }
    }

    /**
     * Validate one {@code @MethodCommand} method. Mirrors the reflection
     * path's fail-fast rules so both paths reject the same shapes.
     */
    private boolean validateMethod(ExecutableElement method) {
        boolean valid = true;

        if (method.getSimpleName().contentEquals("<init>")) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "@MethodCommand must annotate a method, not a constructor", method);
            return false;
        }
        if (method.getModifiers().contains(Modifier.ABSTRACT)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "@MethodCommand method must not be abstract", method);
            valid = false;
        }
        if (method.getModifiers().contains(Modifier.PRIVATE)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "@MethodCommand method must not be private: generated code calls it directly. "
                            + "Use package-private or public visibility.",
                    method);
            valid = false;
        }

        TypeMirror returnType = method.getReturnType();
        boolean voidLike = returnType.getKind() == TypeKind.VOID
                || returnType.toString().equals("java.lang.Void");
        TypeMirror commandResultType = elementUtils
                .getTypeElement(org.aesh.command.CommandResult.class.getCanonicalName()).asType();
        if (!voidLike && !typeUtils.isAssignable(typeUtils.erasure(returnType),
                typeUtils.erasure(commandResultType))) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "@MethodCommand method must return void or CommandResult", method);
            valid = false;
        }

        TypeMirror invocationType = elementUtils
                .getTypeElement("org.aesh.command.invocation.CommandInvocation").asType();
        TypeMirror collectionType = elementUtils
                .getTypeElement(Collection.class.getCanonicalName()).asType();
        for (VariableElement param : method.getParameters()) {
            boolean isInvocation = typeUtils.isAssignable(typeUtils.erasure(param.asType()),
                    typeUtils.erasure(invocationType));
            boolean hasOption = param.getAnnotation(org.aesh.command.option.Option.class) != null;
            boolean hasArgument = param.getAnnotation(org.aesh.command.option.Argument.class) != null;
            boolean hasOptionList = param.getAnnotation(org.aesh.command.option.OptionList.class) != null;
            boolean hasArguments = param.getAnnotation(org.aesh.command.option.Arguments.class) != null;
            if (isInvocation) {
                if (hasOption || hasArgument || hasOptionList || hasArguments) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "CommandInvocation parameter must not carry"
                                    + " @Option/@Argument/@OptionList/@Arguments: "
                                    + param.getSimpleName(),
                            method);
                    valid = false;
                }
                continue;
            }
            if (param.getAnnotation(org.aesh.command.option.OptionGroup.class) != null
                    || param.getAnnotation(org.aesh.command.option.Mixin.class) != null) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Only @Option, @Argument, @OptionList and @Arguments are supported"
                                + " on @MethodCommand parameters: "
                                + param.getSimpleName(),
                        method);
                valid = false;
                continue;
            }
            if (!hasOption && !hasArgument && !hasOptionList && !hasArguments) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Method parameter must be annotated with @Option, @Argument,"
                                + " @OptionList or @Arguments: "
                                + param.getSimpleName(),
                        method);
                valid = false;
                continue;
            }
            if (hasOption) {
                org.aesh.command.option.Option option = param.getAnnotation(org.aesh.command.option.Option.class);
                if (option.name().length() < 1) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "@Option on @MethodCommand parameters requires an explicit name: "
                                    + param.getSimpleName(),
                            method);
                    valid = false;
                    continue;
                }
            }
            if (hasOptionList) {
                org.aesh.command.option.OptionList optionList = param.getAnnotation(org.aesh.command.option.OptionList.class);
                if (optionList.name().length() < 1) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "@OptionList on @MethodCommand parameters requires an explicit name: "
                                    + param.getSimpleName(),
                            method);
                    valid = false;
                    continue;
                }
                if (!typeUtils.isAssignable(typeUtils.erasure(param.asType()),
                        typeUtils.erasure(collectionType))) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "OptionList parameter must be an instance of Collection: "
                                    + param.getSimpleName(),
                            method);
                    valid = false;
                }
            }
            if (hasArguments && !typeUtils.isAssignable(typeUtils.erasure(param.asType()),
                    typeUtils.erasure(collectionType))) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Arguments parameter must be an instance of Collection: "
                                + param.getSimpleName(),
                        method);
                valid = false;
            }
            if (hasArguments) {
                org.aesh.command.option.Arguments arguments = param.getAnnotation(org.aesh.command.option.Arguments.class);
                if (arguments.paramLabel().length() < 1) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "@Arguments on @MethodCommand parameters requires an explicit paramLabel: "
                                    + param.getSimpleName(),
                            method);
                    valid = false;
                }
            }
            if (hasArgument && typeUtils.isAssignable(typeUtils.erasure(param.asType()),
                    typeUtils.erasure(collectionType))) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Argument parameter can not be an instance of Collection: "
                                + param.getSimpleName(),
                        method);
                valid = false;
            }
            if (hasArgument) {
                org.aesh.command.option.Argument argument = param.getAnnotation(org.aesh.command.option.Argument.class);
                if (argument.paramLabel().length() < 1) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "@Argument on @MethodCommand parameters requires an explicit paramLabel: "
                                    + param.getSimpleName(),
                            method);
                    valid = false;
                }
            }
        }

        if (valid) {
            List<VariableElement> params = new ArrayList<>();
            for (VariableElement param : method.getParameters())
                params.add(param);
            Element enclosing = method.getEnclosingElement();
            if (enclosing instanceof TypeElement)
                validatePositionalIndexes((TypeElement) enclosing, params);
        }

        return valid;
    }

    /**
     * Drop methods whose CLI names or aliases collide within one owner,
     * reporting each collision. Mirrors the fail-fast duplicate rule: the
     * command line cannot address two methods by one name.
     */
    private List<ExecutableElement> withoutDuplicateCommands(TypeElement owner,
            List<ExecutableElement> methods) {
        Map<String, List<ExecutableElement>> byName = new LinkedHashMap<>();
        for (ExecutableElement method : methods) {
            for (String cliName : methodCliNames(method)) {
                byName.computeIfAbsent(cliName, k -> new ArrayList<>()).add(method);
            }
        }
        List<ExecutableElement> duplicates = new ArrayList<>();
        for (Map.Entry<String, List<ExecutableElement>> entry : byName.entrySet()) {
            if (entry.getValue().size() > 1) {
                for (ExecutableElement method : entry.getValue()) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Duplicate command name '" + entry.getKey() + "' on "
                                    + owner.getQualifiedName(),
                            method);
                    if (!duplicates.contains(method))
                        duplicates.add(method);
                }
            }
        }
        List<ExecutableElement> valid = new ArrayList<>(methods);
        valid.removeAll(duplicates);
        return valid;
    }

    private List<String> methodCliNames(ExecutableElement method) {
        List<String> names = new ArrayList<>();
        String name = CodeGenerator.methodCommandName(method);
        names.add(name);
        for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
            String mirrorType = ((TypeElement) mirror.getAnnotationType().asElement())
                    .getQualifiedName().toString();
            if (mirrorType.equals(org.aesh.command.MethodCommand.class.getCanonicalName())) {
                for (java.util.Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror
                        .getElementValues().entrySet()) {
                    if (entry.getKey().getSimpleName().toString().equals("aliases")) {
                        @SuppressWarnings("unchecked")
                        List<? extends AnnotationValue> values = (List<? extends AnnotationValue>) entry.getValue().getValue();
                        for (AnnotationValue value : values)
                            names.add(String.valueOf(value.getValue()));
                    }
                }
            }
        }
        return names;
    }

    @SuppressWarnings("unchecked")
    private boolean hasGroupCommands(TypeElement element) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            String annotationType = ((TypeElement) mirror.getAnnotationType().asElement())
                    .getQualifiedName().toString();
            if (annotationType.equals(CommandDefinition.class.getCanonicalName())) {
                for (java.util.Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror
                        .getElementValues().entrySet()) {
                    if (entry.getKey().getSimpleName().toString().equals("groupCommands")) {
                        List<? extends AnnotationValue> values = (List<? extends AnnotationValue>) entry.getValue().getValue();
                        return !values.isEmpty();
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if the given type element implements {@code org.aesh.command.GroupCommand},
     * either directly or through its supertypes.
     */
    private boolean implementsGroupCommand(TypeElement element) {
        String groupCommandName = "org.aesh.command.GroupCommand";
        for (TypeMirror iface : element.getInterfaces()) {
            // Check raw type name (strip generics)
            String ifaceName = typeUtils.erasure(iface).toString();
            if (ifaceName.equals(groupCommandName))
                return true;
        }
        // Check superclass hierarchy
        TypeMirror superclass = element.getSuperclass();
        if (superclass.getKind() != javax.lang.model.type.TypeKind.NONE) {
            Element superElement = typeUtils.asElement(superclass);
            if (superElement instanceof TypeElement) {
                return implementsGroupCommand((TypeElement) superElement);
            }
        }
        return false;
    }

    private void collectPrivateFieldsForReflectConfig(List<VariableElement> fields) {
        for (VariableElement field : fields) {
            if (field.getModifiers().contains(Modifier.PRIVATE) && hasAeshAnnotation(field)) {
                String declaringClass = ((TypeElement) field.getEnclosingElement()).getQualifiedName().toString();
                reflectConfigEntries.computeIfAbsent(declaringClass, k -> new LinkedHashSet<>())
                        .add(field.getSimpleName().toString());
            }
            if (field.getAnnotation(org.aesh.command.option.Mixin.class) != null) {
                // Private mixin field itself
                if (field.getModifiers().contains(Modifier.PRIVATE)) {
                    String declaringClass = ((TypeElement) field.getEnclosingElement()).getQualifiedName().toString();
                    reflectConfigEntries.computeIfAbsent(declaringClass, k -> new LinkedHashSet<>())
                            .add(field.getSimpleName().toString());
                }
                // Private fields inside the mixin class
                TypeMirror mixinType = field.asType();
                if (mixinType instanceof javax.lang.model.type.DeclaredType) {
                    TypeElement mixinElement = (TypeElement) ((javax.lang.model.type.DeclaredType) mixinType).asElement();
                    collectMixinPrivateFieldsForReflectConfig(mixinElement);
                }
            }
            if (field.getAnnotation(org.aesh.command.option.ParentCommand.class) != null
                    && field.getModifiers().contains(Modifier.PRIVATE)) {
                String declaringClass = ((TypeElement) field.getEnclosingElement()).getQualifiedName().toString();
                reflectConfigEntries.computeIfAbsent(declaringClass, k -> new LinkedHashSet<>())
                        .add(field.getSimpleName().toString());
            }
        }
    }

    private void collectMixinPrivateFieldsForReflectConfig(TypeElement mixinElement) {
        for (Element enclosed : mixinElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement f = (VariableElement) enclosed;
                if (f.getModifiers().contains(Modifier.PRIVATE) && hasAeshAnnotation(f)) {
                    String declaringClass = mixinElement.getQualifiedName().toString();
                    reflectConfigEntries.computeIfAbsent(declaringClass, k -> new LinkedHashSet<>())
                            .add(f.getSimpleName().toString());
                }
                if (f.getAnnotation(org.aesh.command.option.Mixin.class) != null) {
                    if (f.getModifiers().contains(Modifier.PRIVATE)) {
                        reflectConfigEntries
                                .computeIfAbsent(mixinElement.getQualifiedName().toString(), k -> new LinkedHashSet<>())
                                .add(f.getSimpleName().toString());
                    }
                    TypeMirror nestedType = f.asType();
                    if (nestedType instanceof javax.lang.model.type.DeclaredType) {
                        collectMixinPrivateFieldsForReflectConfig(
                                (TypeElement) ((javax.lang.model.type.DeclaredType) nestedType).asElement());
                    }
                }
            }
        }
        // Walk superclass
        TypeMirror superclass = mixinElement.getSuperclass();
        if (superclass.getKind() != TypeKind.NONE && !superclass.toString().equals("java.lang.Object")) {
            if (superclass instanceof javax.lang.model.type.DeclaredType) {
                collectMixinPrivateFieldsForReflectConfig(
                        (TypeElement) ((javax.lang.model.type.DeclaredType) superclass).asElement());
            }
        }
    }

    private boolean hasAeshAnnotation(VariableElement field) {
        return field.getAnnotation(org.aesh.command.option.Option.class) != null
                || field.getAnnotation(org.aesh.command.option.OptionList.class) != null
                || field.getAnnotation(org.aesh.command.option.OptionGroup.class) != null
                || field.getAnnotation(org.aesh.command.option.Argument.class) != null
                || field.getAnnotation(org.aesh.command.option.Arguments.class) != null;
    }

    private void generateRegistryClass() {
        String pkg = registryPackage != null ? registryPackage : "";
        String registryCode = CodeGenerator.generateRegistry(pkg, registryEntries, methodRegistryEntries);
        String fullRegistryName = pkg.isEmpty() ? "_AeshMetadataRegistry" : pkg + "._AeshMetadataRegistry";

        try {
            JavaFileObject sourceFile = filer.createSourceFile(fullRegistryName);
            try (Writer writer = sourceFile.openWriter()) {
                writer.write(registryCode);
            }
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Failed to generate metadata registry: " + e.getMessage());
        }
    }

    private void writeServiceFile() {
        String pkg = registryPackage != null ? registryPackage : "";
        String fullRegistryName = pkg.isEmpty() ? "_AeshMetadataRegistry" : pkg + "._AeshMetadataRegistry";

        try {
            javax.tools.FileObject serviceFile = filer.createResource(
                    javax.tools.StandardLocation.CLASS_OUTPUT, "",
                    "META-INF/services/org.aesh.command.metadata.MetadataRegistry");
            try (Writer writer = serviceFile.openWriter()) {
                writer.write(fullRegistryName);
                writer.write("\n");
            }
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Failed to write ServiceLoader file: " + e.getMessage());
        }

        // Also write META-INF/aesh/registry — a plain-text resource file that
        // MetadataProviderRegistry reads via ClassLoader.getResources() without
        // needing ServiceLoader. This enables native-image builds with
        // -H:-UseServiceLoaderFeature (#540).
        try {
            javax.tools.FileObject registryFile = filer.createResource(
                    javax.tools.StandardLocation.CLASS_OUTPUT, "",
                    "META-INF/aesh/registry");
            try (Writer writer = registryFile.openWriter()) {
                writer.write(fullRegistryName);
                writer.write("\n");
            }
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Failed to write registry resource file: " + e.getMessage());
        }
    }

    private void writeNativeImageConfigs() {
        String disableOpt = processingEnv.getOptions().get(OPT_DISABLE_NATIVE_IMAGE);
        if ("true".equalsIgnoreCase(disableOpt))
            return;

        String project = processingEnv.getOptions().get(OPT_PROJECT);
        if (project == null || project.isEmpty())
            project = "aesh-generated";
        String configDir = "META-INF/native-image/org.aesh/" + project;

        // Always write resource-config.json (ServiceLoader descriptor must be included)
        writeResourceConfig(configDir);

        // Always write reflect-config.json — the registry class must be registered
        // for reflective instantiation so ServiceLoader works with -H:-UseServiceLoaderFeature (#540)
        writeReflectConfig(configDir);
    }

    private void writeReflectConfig(String configDir) {
        String pkg = registryPackage != null ? registryPackage : "";
        String fullRegistryName = pkg.isEmpty() ? "_AeshMetadataRegistry" : pkg + "._AeshMetadataRegistry";

        try {
            javax.tools.FileObject file = filer.createResource(
                    javax.tools.StandardLocation.CLASS_OUTPUT, "",
                    configDir + "/reflect-config.json");
            try (Writer writer = file.openWriter()) {
                writer.write("[\n");
                int classIdx = 0;

                // Register the generated _AeshMetadataRegistry for reflective
                // instantiation so ServiceLoader works with -H:-UseServiceLoaderFeature (#540)
                writer.write("  {\n");
                writer.write("    \"name\": \"" + fullRegistryName + "\",\n");
                writer.write("    \"methods\": [{\"name\": \"<init>\", \"parameterTypes\": []}]\n");
                writer.write("  }");
                classIdx++;

                // Register private fields that need reflective write access
                for (Map.Entry<String, Set<String>> entry : reflectConfigEntries.entrySet()) {
                    if (classIdx > 0)
                        writer.write(",\n");
                    writer.write("  {\n");
                    writer.write("    \"name\": \"" + entry.getKey() + "\",\n");
                    writer.write("    \"fields\": [\n");
                    int fieldIdx = 0;
                    for (String fieldName : entry.getValue()) {
                        if (fieldIdx > 0)
                            writer.write(",\n");
                        writer.write("      {\"name\": \"" + fieldName + "\", \"allowWrite\": true}");
                        fieldIdx++;
                    }
                    writer.write("\n    ]\n");
                    writer.write("  }");
                    classIdx++;
                }
                writer.write("\n]\n");
            }
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Failed to write reflect-config.json: " + e.getMessage());
        }
    }

    private void writeResourceConfig(String configDir) {
        try {
            javax.tools.FileObject file = filer.createResource(
                    javax.tools.StandardLocation.CLASS_OUTPUT, "",
                    configDir + "/resource-config.json");
            try (Writer writer = file.openWriter()) {
                writer.write("{\n");
                writer.write("  \"resources\": {\n");
                writer.write("    \"includes\": [\n");
                writer.write("      {\"pattern\": \"META-INF/services/org.aesh.command.metadata.MetadataRegistry\"},\n");
                writer.write("      {\"pattern\": \"META-INF/aesh/registry\"}\n");
                writer.write("    ]\n");
                writer.write("  }\n");
                writer.write("}\n");
            }
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Failed to write resource-config.json: " + e.getMessage());
        }
    }
}
