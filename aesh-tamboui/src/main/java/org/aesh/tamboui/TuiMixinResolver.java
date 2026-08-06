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
package org.aesh.tamboui;

import java.lang.reflect.Field;

/**
 * Utility to find a {@link TuiMixin} field on a command instance.
 * Used by {@link TuiCommand} and {@link TuiAppCommand} to automatically
 * apply TUI options from a {@code @Mixin TuiMixin} field.
 */
final class TuiMixinResolver {

    private TuiMixinResolver() {
    }

    /**
     * Scan the instance's class hierarchy for a field of type {@link TuiMixin}
     * and return its value. Returns null if no TuiMixin field is found.
     */
    static TuiMixin findMixin(Object instance) {
        Class<?> clazz = instance.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (TuiMixin.class.equals(field.getType())) {
                    try {
                        field.setAccessible(true);
                        return (TuiMixin) field.get(instance);
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}
