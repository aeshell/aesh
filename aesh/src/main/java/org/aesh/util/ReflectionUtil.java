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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Aesh team
 */
public class ReflectionUtil {

    private static final ConcurrentMap<Class<?>, Constructor<?>> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();

    public static <T> T newInstance(final Class<T> clazz) {
        if (clazz.isAnonymousClass() || clazz.isInterface() || clazz.isAnnotation()) {
            throw new RuntimeException("Can not build new instance of an " + clazz.getName());
        }

        @SuppressWarnings("unchecked")
        Constructor<T> cached = (Constructor<T>) CONSTRUCTOR_CACHE.get(clazz);
        if (cached != null) {
            return invokeConstructor(cached);
        }

        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            @SuppressWarnings("unchecked")
            T result = (T) instantiateWithConstructor(constructor);
            if (result != null) {
                CONSTRUCTOR_CACHE.putIfAbsent(clazz, constructor);
                return result;
            }
        }

        throw new RuntimeException("Could not instantiate class: " + clazz + ", no access to constructors.");
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokeConstructor(Constructor<T> constructor) {
        try {
            if (constructor.getParameterTypes().length == 0) {
                return constructor.newInstance();
            }
            Constructor<?> paramConstructor = getConstructorWithNoParams(constructor.getParameterTypes()[0]);
            if (paramConstructor != null) {
                return constructor.newInstance(paramConstructor.newInstance());
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Could not invoke cached constructor for: " + constructor.getDeclaringClass());
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
