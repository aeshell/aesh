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
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
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
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AeshAnnotationProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;
    private Elements elementUtils;
    private Types typeUtils;
    private final List<String> generatedProviders = new ArrayList<>();

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
            if (!generatedProviders.isEmpty()) {
                writeServiceFile();
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
        String packageName = "";
        int lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot > 0) {
            packageName = qualifiedName.substring(0, lastDot);
        }
        String simpleName = commandElement.getSimpleName().toString();
        String metadataClassName = simpleName + "_AeshMetadata";
        String fullMetadataName = packageName.isEmpty() ? metadataClassName : packageName + "." + metadataClassName;

        boolean isGroup = commandElement.getAnnotation(GroupCommandDefinition.class) != null;

        List<VariableElement> fields = collectFields(commandElement);

        String code = CodeGenerator.generate(
                packageName, simpleName, metadataClassName, qualifiedName,
                commandElement, fields, isGroup, elementUtils, typeUtils);

        JavaFileObject sourceFile = filer.createSourceFile(fullMetadataName, commandElement);
        try (Writer writer = sourceFile.openWriter()) {
            writer.write(code);
        }

        generatedProviders.add(fullMetadataName);
    }

    private void writeServiceFile() {
        try {
            javax.tools.FileObject serviceFile = filer.createResource(
                    javax.tools.StandardLocation.CLASS_OUTPUT, "",
                    "META-INF/services/org.aesh.command.metadata.CommandMetadataProvider");
            try (Writer writer = serviceFile.openWriter()) {
                for (String provider : generatedProviders) {
                    writer.write(provider);
                    writer.write("\n");
                }
            }
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Failed to write ServiceLoader file: " + e.getMessage());
        }
    }
}
