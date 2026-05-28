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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.console.AeshContext;

/**
 * A {@link ProcessedOption} subclass for the generated (annotation-processor) path.
 * <p>
 * All field access methods assume {@code fieldAccessor} is non-null, eliminating
 * reflection fallback paths ({@code getField()}, {@code resolveMixinInstance()},
 * {@code Field.set/get}, primitive type switches). This results in shorter,
 * branch-free methods that the JIT can inline aggressively.
 * <p>
 * Instances are created automatically by {@link ProcessedOption#createDirect}
 * when a non-null {@link FieldAccessor} is provided.
 *
 * @author Aesh team
 */
public final class GeneratedProcessedOption extends ProcessedOption {

    public GeneratedProcessedOption() {
        super();
    }

    @Override
    public void captureInitialValue(Object instance) {
        if (initialValueCaptured || instance == null || getFieldName() == null)
            return;
        if (type() != null && type().isPrimitive()) {
            initialValueCaptured = true;
            return;
        }
        Object value = fieldAccessor.get(instance);
        if (value != null) {
            initialValue = value;
        }
        initialValueCaptured = true;
    }

    @Override
    public void resetField(Object instance) {
        if (initialValueCaptured && initialValue != null) {
            restoreInitialValue(instance);
            return;
        }
        fieldAccessor.set(instance, typeDefault());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void restoreInitialValue(Object instance) {
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
        fieldAccessor.set(instance, val);
    }

    @Override
    public void injectValueIntoField(Object instance, InvocationProviders invocationProviders,
            AeshContext aeshContext, boolean doValidation) throws OptionValidatorException {
        if (converter() == null || instance == null)
            return;
        injectValueWithSetter(instance, invocationProviders, aeshContext, doValidation);
    }

    @Override
    public Object getFieldValue(Object instance) {
        return fieldAccessor.get(instance);
    }

    @Override
    public void setFieldValue(Object instance, Object value) {
        fieldAccessor.set(instance, value);
    }
}
