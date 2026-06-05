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
import javax.annotation.processing.SupportedSourceVersion;
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
import org.aesh.command.GroupCommandDefinition;

/**
 * JSR 269 annotation processor that generates {@code CommandMetadataProvider}
 * implementations for classes annotated with {@link CommandDefinition} or
 * {@link GroupCommandDefinition}.
 *
 * @author Aesh team
 */
@SupportedAnnotationTypes({
        "org.aesh.command.CommandDefinition",
        "org.aesh.command.GroupCommandDefinition"
})
@SupportedOptions({
        AeshAnnotationProcessor.OPT_PROJECT,
        AeshAnnotationProcessor.OPT_DISABLE_NATIVE_IMAGE
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AeshAnnotationProcessor extends AbstractProcessor {

    /** Processor option: subdirectory name for native-image config output. */
    static final String OPT_PROJECT = "aeshNativeImageProject";
    /** Processor option: set to "true" to skip native-image config generation. */
    static final String OPT_DISABLE_NATIVE_IMAGE = "aeshNativeImageDisable";

    private Filer filer;
    private Messager messager;
    private Elements elementUtils;
    private Types typeUtils;
    private final List<String> generatedProviders = new ArrayList<>();
    /** Pairs of (binaryClassName, metadataSimpleName) for the registry switch. */
    private final List<String[]> registryEntries = new ArrayList<>();
    private String registryPackage;
    private boolean registryGenerated;
    /** Private fields needing reflection config, grouped by declaring class name. */
    private final Map<String, Set<String>> reflectConfigEntries = new LinkedHashMap<>();

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
            if (!registryEntries.isEmpty()) {
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

        for (Element element : roundEnv.getElementsAnnotatedWith(GroupCommandDefinition.class)) {
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

        // Generate the registry source file in the same round as the providers,
        // not in the final round, to avoid javac's "created in the last round" warning (#492).
        if (!registryEntries.isEmpty() && !commandElements.isEmpty() && !registryGenerated) {
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
                javax.lang.model.element.ExecutableElement constructor = (javax.lang.model.element.ExecutableElement) enclosed;
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
        for (VariableElement field : collectFields(element)) {
            validateField(field);
        }

        return valid;
    }

    private void validateField(VariableElement field) {
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

        boolean isGroup = commandElement.getAnnotation(GroupCommandDefinition.class) != null;
        if (!isGroup) {
            // Check if @CommandDefinition has groupCommands via annotation mirror
            // (direct access triggers MirroredTypesException at compile time)
            isGroup = hasGroupCommands(commandElement);
        }

        List<VariableElement> fields = collectFields(commandElement);

        String code = CodeGenerator.generate(
                packageName, typeRefName, metadataClassName, qualifiedName,
                commandElement, fields, isGroup, elementUtils, typeUtils);

        JavaFileObject sourceFile = filer.createSourceFile(fullMetadataName, commandElement);
        try (Writer writer = sourceFile.openWriter()) {
            writer.write(code);
        }

        generatedProviders.add(fullMetadataName);

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
        String registryCode = CodeGenerator.generateRegistry(pkg, registryEntries);
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

        // Only write reflect-config.json if there are private fields
        if (!reflectConfigEntries.isEmpty()) {
            writeReflectConfig(configDir);
        }
    }

    private void writeReflectConfig(String configDir) {
        try {
            javax.tools.FileObject file = filer.createResource(
                    javax.tools.StandardLocation.CLASS_OUTPUT, "",
                    configDir + "/reflect-config.json");
            try (Writer writer = file.openWriter()) {
                writer.write("[\n");
                int classIdx = 0;
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
                writer.write("      {\"pattern\": \"META-INF/services/org.aesh.command.metadata.MetadataRegistry\"}\n");
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
