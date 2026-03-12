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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.aesh.command.converter.Converter;
import org.aesh.command.impl.converter.BooleanConverter;
import org.aesh.command.impl.converter.ByteConverter;
import org.aesh.command.impl.converter.CharacterConverter;
import org.aesh.command.impl.converter.DoubleConverter;
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

    private final Map<Class, Supplier<Converter>> factories;
    private final ConcurrentMap<Class, Converter> cache = new ConcurrentHashMap<>();

    private static class CLConvertManagerHolder {
        static final CLConverterManager INSTANCE = new CLConverterManager();
    }

    public static CLConverterManager getInstance() {
        return CLConvertManagerHolder.INSTANCE;
    }

    private CLConverterManager() {
        factories = new HashMap<>(22);
        addFactory(Integer.class, int.class, () -> new IntegerConverter());
        addFactory(Boolean.class, boolean.class, () -> new BooleanConverter());
        addFactory(Character.class, char.class, () -> new CharacterConverter());
        addFactory(Double.class, double.class, () -> new DoubleConverter());
        addFactory(Float.class, float.class, () -> new FloatConverter());
        addFactory(Long.class, long.class, () -> new LongConverter());
        addFactory(Short.class, short.class, () -> new ShortConverter());
        addFactory(Byte.class, byte.class, () -> new ByteConverter());
        factories.put(String.class, () -> new StringConverter());
        factories.put(File.class, () -> new FileConverter());
        factories.put(Resource.class, () -> new FileResourceConverter());
        factories.put(URL.class, () -> new URLConverter());
        factories.put(URI.class, () -> new URIConverter());
    }

    private void addFactory(Class<?> boxed, Class<?> primitive, Supplier<Converter> factory) {
        factories.put(boxed, factory);
        factories.put(primitive, factory);
    }

    public boolean hasConverter(Class clazz) {
        return cache.containsKey(clazz) || factories.containsKey(clazz);
    }

    public Converter getConverter(Class clazz) {
        Converter converter = cache.get(clazz);
        if (converter != null)
            return converter;
        Supplier<Converter> factory = factories.get(clazz);
        if (factory == null)
            return null;
        return cache.computeIfAbsent(clazz, k -> factory.get());
    }

    public void setConverter(Class<?> clazz, Converter converter) {
        cache.put(clazz, converter);
    }

    public Set<Class> getConvertedTypes() {
        Set<Class> types = new HashSet<>(factories.keySet());
        types.addAll(cache.keySet());
        return Collections.unmodifiableSet(types);
    }

}
