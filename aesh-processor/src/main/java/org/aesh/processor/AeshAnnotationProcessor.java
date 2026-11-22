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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
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
        AeshAnnotationProcessor.OPT_DISABLE_NATIVE_IMAGE,
        AeshAnnotationProcessor.OPT_REGISTRY_CLASS
})
public class AeshAnnotationProcessor extends AbstractProcessor {

    /** Processor option: subdirectory name for native-image config output. */
    static final String OPT_PROJECT = "aeshNativeImageProject";
    /** Processor option: set to "true" to skip native-image config generation. */
    static final String OPT_DISABLE_NATIVE_IMAGE = "aeshNativeImageDisable";
    /**
     * Processor option: fully-qualified name for the generated metadata
     * registry class (e.g. {@code -AaeshRegistryClass=org.example.MyMetadataRegistry}).
     * When absent, the registry is named {@code _AeshMetadataRegistry} in the
     * package of the first accepted command.
     */
    static final String OPT_REGISTRY_CLASS = "aeshRegistryClass";
    /** Simple name of the generated registry when no explicit name is configured. */
    private static final String DEFAULT_REGISTRY_SIMPLE_NAME = "_AeshMetadataRegistry";
    /** Dotted Java identifiers, e.g. {@code org.example.MyRegistry} or {@code MyRegistry}. */
    private static final Pattern REGISTRY_CLASS_PATTERN = Pattern
            .compile("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*");

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
    /** Explicit registry class name from {@code -AaeshRegistryClass}, or null for the default. */
    private String configuredRegistryClass;
    /** True when the explicit registry name failed validation; outputs are then skipped. */
    private boolean registryOptionInvalid;
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
        String registryOption = processingEnv.getOptions().get(OPT_REGISTRY_CLASS);
        if (registryOption != null && !registryOption.isEmpty()) {
            if (REGISTRY_CLASS_PATTERN.matcher(registryOption).matches()) {
                this.configuredRegistryClass = registryOption;
            } else {
                this.registryOptionInvalid = true;
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Invalid -A" + OPT_REGISTRY_CLASS + " value '" + registryOption
                                + "': must be a fully-qualified class name like org.example.MyMetadataRegistry");
            }
        }
    }

    /**
     * Fully-qualified name of the registry class to generate, or null when
     * the configured name failed validation (the error is reported in
     * {@link #init(ProcessingEnvironment)}).
     */
    private String registryFullName() {
        if (registryOptionInvalid) {
            return null;
        }
        if (configuredRegistryClass != null) {
            return configuredRegistryClass;
        }
        String pkg = registryPackage != null ? registryPackage : "";
        return pkg.isEmpty() ? DEFAULT_REGISTRY_SIMPLE_NAME : pkg + "." + DEFAULT_REGISTRY_SIMPLE_NAME;
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
            if (skipInaccessibleCommand(commandElement)) {
                continue;
            }
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

    /**
     * Emits the skip diagnostic for a command the generated top-level
     * provider cannot name. Returns true when the caller must skip
     * validation and generation for the command.
     */
    private boolean skipInaccessibleCommand(TypeElement commandElement) {
        String commandPackage = elementUtils.getPackageOf(commandElement).getQualifiedName().toString();
        String blockedType = inaccessibleCommandType(commandElement, commandPackage);
        if (blockedType == null) {
            return false;
        }
        if (declaresCommandDefinition(commandElement)) {
            messager.printMessage(Diagnostic.Kind.WARNING,
                    "Skipping generated metadata for "
                            + commandElement.getQualifiedName()
                            + ": " + blockedType
                            + " is inaccessible from a top-level provider; declare it accessible or remove the annotation",
                    commandElement);
        } else {
            messager.printMessage(Diagnostic.Kind.NOTE,
                    "Skipping generated metadata for "
                            + commandElement.getQualifiedName()
                            + ": " + blockedType
                            + " is inaccessible from a top-level provider",
                    commandElement);
        }
        return true;
    }

    /**
     * Qualified name of the type that blocks generated-code access to the
     * command, or null when a top-level provider in the command's package can
     * name it. Walks the whole enclosing chain: a private type anywhere blocks
     * access, and a non-public member type is only nameable from its own
     * package. Local or anonymous endings have no top-level source name and
     * cannot be referenced either.
     */
    private String inaccessibleCommandType(TypeElement commandElement, String packageName) {
        Element current = commandElement;
        while (current instanceof TypeElement) {
            Set<Modifier> mods = current.getModifiers();
            if (mods.contains(Modifier.PRIVATE)) {
                return ((TypeElement) current).getQualifiedName().toString();
            }
            if (!mods.contains(Modifier.PUBLIC) && !packageNameOf(current).equals(packageName)) {
                return ((TypeElement) current).getQualifiedName().toString();
            }
            Element enclosing = current.getEnclosingElement();
            if (!(enclosing instanceof TypeElement) && !(enclosing instanceof PackageElement)) {
                return commandElement.getQualifiedName().toString();
            }
            current = enclosing;
        }
        return null;
    }

    private static String packageNameOf(Element element) {
        Element current = element;
        while (current != null && !(current instanceof PackageElement)) {
            current = current.getEnclosingElement();
        }
        return current == null ? "" : ((PackageElement) current).getQualifiedName().toString();
    }

    /**
     * Whether {@code @CommandDefinition} is declared on the element itself
     * rather than merely inherited. Annotation mirrors only report directly
     * present annotations, unlike {@code getAnnotation} which follows
     * {@code @Inherited}.
     */
    private static boolean declaresCommandDefinition(TypeElement element) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().asElement() instanceof TypeElement
                    && ((TypeElement) mirror.getAnnotationType().asElement()).getQualifiedName()
                            .contentEquals(CommandDefinition.class.getCanonicalName())) {
                return true;
            }
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

        if (!CodeGenerator.hasAccessibleNoArgConstructor(element)) {
            messager.printMessage(Diagnostic.Kind.NOTE,
                    "Command class has no accessible no-arg constructor; the generated"
                            + " newInstance() will throw and command instances must be passed explicitly",
                    element);
        }

        // Validate field types
        List<VariableElement> fields = collectFields(element);
        for (VariableElement field : fields) {
            validateField(field);
        }

        // Reject cyclic @Mixin graphs with a clear error instead of recursing
        // forever in code generation (#720).
        java.util.List<String> mixinCycle = findMixinCycle(element, new ArrayDeque<>());
        if (mixinCycle != null) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Cyclic @Mixin hierarchy detected: " + String.join(" -> ", mixinCycle), element);
            valid = false;
        }

        // Validate positional index ranges don't overlap
        validatePositionalIndexes(element, fields);

        // Reject group children the generated code cannot name, instead of
        // emitting uncompilable providers (#785).
        if (!validateGroupChildren(element)) {
            valid = false;
        }

        return valid;
    }

    /**
     * Validates the {@code groupCommands} entries of a directly present
     * {@code @CommandDefinition} against the package the generated provider
     * lives in. Children the generated code cannot name are reported at the
     * offending annotation value; the provider is then suppressed by the
     * caller. Entries javac itself cannot resolve to a type are left alone:
     * the compiler reports the underlying problem.
     */
    private boolean validateGroupChildren(TypeElement commandElement) {
        boolean valid = true;
        String packageName = elementUtils.getPackageOf(commandElement).getQualifiedName().toString();
        for (AnnotationMirror mirror : commandElement.getAnnotationMirrors()) {
            if (!((TypeElement) mirror.getAnnotationType().asElement()).getQualifiedName()
                    .contentEquals(CommandDefinition.class.getCanonicalName())) {
                continue;
            }
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror
                    .getElementValues().entrySet()) {
                if (!entry.getKey().getSimpleName().contentEquals("groupCommands")) {
                    continue;
                }
                Object raw = entry.getValue().getValue();
                if (!(raw instanceof List)) {
                    continue;
                }
                for (AnnotationValue childValue : (List<AnnotationValue>) raw) {
                    TypeElement child = asCommandType(childValue.getValue());
                    if (child == null) {
                        continue;
                    }
                    String blocked = inaccessibleCommandType(child, packageName);
                    if (blocked != null) {
                        messager.printMessage(Diagnostic.Kind.ERROR,
                                "Group " + commandElement.getQualifiedName() + " references inaccessible command "
                                        + child.getQualifiedName() + ": " + blocked
                                        + " cannot be named by generated code. Make it accessible, remove it from groupCommands, or register the group programmatically.",
                                commandElement, mirror, childValue);
                        valid = false;
                    }
                }
            }
        }
        return valid;
    }

    /**
     * Resolves an annotation value to a command type when possible. Class
     * literals normally arrive as {@link DeclaredType}; javac may hand back
     * the qualified name as a string for nested classes it has not completed.
     * Anything else (including error markers) is left for javac to report.
     */
    private TypeElement asCommandType(Object value) {
        if (value instanceof DeclaredType) {
            Element element = ((DeclaredType) value).asElement();
            return element instanceof TypeElement ? (TypeElement) element : null;
        }
        if (value instanceof String) {
            String name = (String) value;
            if (!name.isEmpty()) {
                TypeElement direct = elementUtils.getTypeElement(name);
                if (direct != null) {
                    return direct;
                }
                int lastDot = name.lastIndexOf('.');
                if (lastDot > 0) {
                    // Binary-name fallback for nested classes ("a.Outer$Inner").
                    return elementUtils.getTypeElement(
                            name.substring(0, lastDot) + '$' + name.substring(lastDot + 1));
                }
            }
        }
        return null;
    }

    /**
     * The mixin cycle reachable from the element as simple names
     * (e.g. {@code [A, B, A]}), or null when the graph is acyclic.
     * Depth-first search with a path stack: a mixin type already on the path
     * is a cycle, while revisits off the path (diamonds) are fine (#720).
     * Superclass edges need no tracking: JVM hierarchies cannot cycle.
     */
    private java.util.List<String> findMixinCycle(TypeElement element, ArrayDeque<TypeElement> path) {
        String name = element.getQualifiedName().toString();
        for (TypeElement visited : path) {
            if (!visited.getQualifiedName().toString().equals(name))
                continue;
            java.util.List<String> cycle = new ArrayList<>();
            boolean inCycle = false;
            for (TypeElement v : path) {
                if (v.getQualifiedName().toString().equals(name))
                    inCycle = true;
                if (inCycle)
                    cycle.add(simpleName(v.getQualifiedName().toString()));
            }
            cycle.add(simpleName(name));
            return cycle;
        }
        path.addLast(element);
        try {
            for (Element enclosed : element.getEnclosedElements()) {
                if (enclosed.getKind() == ElementKind.FIELD
                        && enclosed.getAnnotation(org.aesh.command.option.Mixin.class) != null) {
                    Element mixinElement = typeUtils.asElement(((VariableElement) enclosed).asType());
                    if (mixinElement instanceof TypeElement) {
                        java.util.List<String> found = findMixinCycle((TypeElement) mixinElement, path);
                        if (found != null)
                            return found;
                    }
                }
            }
            return null;
        } finally {
            path.removeLast();
        }
    }

    private static String simpleName(String qualifiedName) {
        int dot = qualifiedName.lastIndexOf('.');
        int dollar = qualifiedName.lastIndexOf('$');
        int sep = Math.max(dot, dollar);
        return sep >= 0 ? qualifiedName.substring(sep + 1) : qualifiedName;
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
            if (field.asType().getKind() != TypeKind.ARRAY
                    && !typeUtils.isAssignable(typeUtils.erasure(field.asType()),
                            typeUtils.erasure(collectionType))) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "OptionList field must be instance of Collection or an array", field);
            }
        }
        if (field.getAnnotation(org.aesh.command.option.Arguments.class) != null) {
            TypeMirror collectionType = elementUtils
                    .getTypeElement(Collection.class.getCanonicalName()).asType();
            if (field.asType().getKind() != TypeKind.ARRAY
                    && !typeUtils.isAssignable(typeUtils.erasure(field.asType()),
                            typeUtils.erasure(collectionType))) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Arguments field must be instance of Collection or an array", field);
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
                if (param.asType().getKind() != TypeKind.ARRAY
                        && !typeUtils.isAssignable(typeUtils.erasure(param.asType()),
                                typeUtils.erasure(collectionType))) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "OptionList parameter must be an instance of Collection or an array: "
                                    + param.getSimpleName(),
                            method);
                    valid = false;
                }
            }
            if (hasArguments && param.asType().getKind() != TypeKind.ARRAY
                    && !typeUtils.isAssignable(typeUtils.erasure(param.asType()),
                            typeUtils.erasure(collectionType))) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Arguments parameter must be an instance of Collection or an array: "
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
            if (hasArgument && (typeUtils.isAssignable(typeUtils.erasure(param.asType()),
                    typeUtils.erasure(collectionType))
                    || param.asType().getKind() == TypeKind.ARRAY)) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Argument parameter can not be an instance of Collection or an array: "
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
        String fullRegistryName = registryFullName();
        if (fullRegistryName == null) {
            return;
        }
        int lastDot = fullRegistryName.lastIndexOf('.');
        String pkg = lastDot < 0 ? "" : fullRegistryName.substring(0, lastDot);
        String simpleName = lastDot < 0 ? fullRegistryName : fullRegistryName.substring(lastDot + 1);
        String registryCode = CodeGenerator.generateRegistry(pkg, simpleName, registryEntries,
                methodRegistryEntries);

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
        String fullRegistryName = registryFullName();
        if (fullRegistryName == null) {
            return;
        }

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
        String fullRegistryName = registryFullName();
        if (fullRegistryName == null) {
            return;
        }

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
