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
package org.aesh.converter;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.aesh.command.converter.Converter;
import org.aesh.command.impl.converter.BooleanConverter;
import org.aesh.command.impl.converter.ByteConverter;
import org.aesh.command.impl.converter.CharacterConverter;
import org.aesh.command.impl.converter.DoubleConverter;
import org.aesh.command.impl.converter.EnumConverter;
import org.aesh.command.impl.converter.FileConverter;
import org.aesh.command.impl.converter.FileResourceConverter;
import org.aesh.command.impl.converter.FloatConverter;
import org.aesh.command.impl.converter.IntegerConverter;
import org.aesh.command.impl.converter.LongConverter;
import org.aesh.command.impl.converter.ShortConverter;
import org.aesh.command.impl.converter.StringConverter;
import org.aesh.command.impl.converter.URIConverter;
import org.aesh.command.impl.converter.URLConverter;
import org.aesh.io.Resource;

/**
 * @author Aesh team
 */
public class CLConverterManager {

    /**
     * Explicit overrides via {@link #setConverter} plus already-resolved
     * built-ins. Built-in converters are created lazily per requested type
     * (see {@link #createBuiltinConverter}) so merely looking up one type
     * never loads the other converter classes.
     */
    private final Map<Class, Converter> converters;

    /**
     * Types with built-in converters, mirroring the historical constructor
     * contents exactly (boxed and primitive forms). Used by
     * {@link #hasConverter} and {@link #getConvertedTypes} without
     * instantiating anything.
     */
    private static final Set<Class<?>> BUILTIN_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            Integer.class, int.class, Boolean.class, boolean.class,
            Character.class, char.class, Double.class, double.class,
            Float.class, float.class, Long.class, long.class,
            Short.class, short.class, Byte.class, byte.class,
            String.class, File.class, java.nio.file.Path.class,
            Resource.class, URL.class, URI.class)));

    /**
     * Implicitly computed enum converters, collected automatically with
     * their defining classloader. Unlike the map above, these entries
     * never pin reloaded application classes (#667).
     */
    private static final ClassValue<Converter> enumConverters = new ClassValue<Converter>() {
        @Override
        protected Converter computeValue(Class<?> type) {
            if (!type.isEnum())
                throw new IllegalArgumentException("Not an enum: " + type);
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Converter converter = new EnumConverter(
                    (Class<? extends Enum>) type);
            return converter;
        }
    };

    private static class CLConvertManagerHolder {
        static final CLConverterManager INSTANCE = new CLConverterManager();
    }

    public static CLConverterManager getInstance() {
        return CLConvertManagerHolder.INSTANCE;
    }

    private CLConverterManager() {
        converters = new ConcurrentHashMap<>(22);
    }

    /**
     * Instantiate the built-in converter for the given type, or null if the
     * type has none. Only the requested converter class is loaded.
     */
    private static Converter createBuiltinConverter(Class<?> clazz) {
        if (clazz == Integer.class || clazz == int.class)
            return new IntegerConverter();
        if (clazz == Boolean.class || clazz == boolean.class)
            return new BooleanConverter();
        if (clazz == Character.class || clazz == char.class)
            return new CharacterConverter();
        if (clazz == Double.class || clazz == double.class)
            return new DoubleConverter();
        if (clazz == Float.class || clazz == float.class)
            return new FloatConverter();
        if (clazz == Long.class || clazz == long.class)
            return new LongConverter();
        if (clazz == Short.class || clazz == short.class)
            return new ShortConverter();
        if (clazz == Byte.class || clazz == byte.class)
            return new ByteConverter();
        if (clazz == String.class)
            return new StringConverter();
        if (clazz == File.class)
            return new FileConverter();
        if (clazz == java.nio.file.Path.class)
            return new org.aesh.command.impl.converter.PathConverter();
        if (clazz == Resource.class)
            return new FileResourceConverter();
        if (clazz == URL.class)
            return new URLConverter();
        if (clazz == URI.class)
            return new URIConverter();
        return null;
    }

    public boolean hasConverter(Class clazz) {
        return converters.containsKey(clazz) || BUILTIN_TYPES.contains(clazz);
    }

    @SuppressWarnings("unchecked")
    public Converter getConverter(Class clazz) {
        Converter converter = converters.get(clazz);
        if (converter != null)
            return converter;
        // Implicit enum converters live in the loader-collected cache, so
        // reloaded applications are never pinned through this manager.
        // Explicit setConverter overrides stay in the map above and keep
        // precedence. hasConverter/getConvertedTypes intentionally report
        // only registered and built-in converters.
        if (clazz.isEnum())
            return enumConverters.get(clazz);
        converter = createBuiltinConverter(clazz);
        if (converter == null)
            return null;
        Converter existing = converters.putIfAbsent(clazz, converter);
        return existing != null ? existing : converter;
    }

    public void setConverter(Class<?> clazz, Converter converter) {
        converters.put(clazz, converter);
    }

    public Set<Class> getConvertedTypes() {
        Set<Class> types = new HashSet<>(BUILTIN_TYPES);
        types.addAll(converters.keySet());
        return Collections.unmodifiableSet(types);
    }

}
