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
package org.aesh.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * @author Aesh team
 */
public class ReflectionUtil {

    public static <T> T newInstance(final Class<T> clazz) {
        if (clazz.isAnonymousClass() || clazz.isInterface() || clazz.isAnnotation()) {
            throw new RuntimeException("Can not build new instance of an " + clazz.getName());
        }

        T instance = null;
        for (Constructor<?> constructor : clazz.getConstructors()) {
            @SuppressWarnings("unchecked")
            T result = (T) instantiateWithConstructor(constructor);
            instance = result;
            if (instance != null)
                return instance;
        }

        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            @SuppressWarnings("unchecked")
            T result = (T) instantiateWithConstructor(constructor);
            instance = result;
            if (instance != null)
                return instance;
        }

        throw new RuntimeException("Could not instantiate class: " + clazz + ", no access to constructors.");
    }

    private static <T> T instantiateWithConstructor(Constructor<T> constructor) {
        T instance = null;
        if (constructor.getParameterTypes().length == 0) {
            instance = newInstanceWithoutParameterTypes(constructor);
        }

        if (constructor.getParameterTypes().length == 1) {
            instance = newInstanceWithParameterTypes(constructor);
        }

        return instance;
    }

    private static <T> T newInstanceWithoutParameterTypes(Constructor<T> constructor) {
        try {
            setAccessible(constructor);
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T newInstanceWithParameterTypes(Constructor<T> constructor) {
        Constructor<?> paramConstructor = getConstructorWithNoParams(constructor.getParameterTypes()[0]);
        if (paramConstructor == null)
            return null;
        setAccessible(constructor);
        try {
            return constructor.newInstance(paramConstructor.newInstance());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> void setAccessible(Constructor<T> constructor) {
        if (Modifier.isPrivate(constructor.getModifiers()) ||
                Modifier.isProtected(constructor.getModifiers())) {
            constructor.setAccessible(true);
        }
    }

    private static Constructor<?> getConstructorWithNoParams(Class<?> clazz) {
        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (constructor.getParameterTypes().length == 0) {
                setAccessible(constructor);
                return constructor;
            }
        }
        return null;
    }
}
