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

    private final Map<Class, Converter> converters;

    private static class CLConvertManagerHolder {
        static final CLConverterManager INSTANCE = new CLConverterManager();
    }

    public static CLConverterManager getInstance() {
        return CLConvertManagerHolder.INSTANCE;
    }

    private CLConverterManager() {
        converters = new HashMap<>(22);
        Converter intConverter = new IntegerConverter();
        converters.put(Integer.class, intConverter);
        converters.put(int.class, intConverter);
        Converter boolConverter = new BooleanConverter();
        converters.put(Boolean.class, boolConverter);
        converters.put(boolean.class, boolConverter);
        Converter charConverter = new CharacterConverter();
        converters.put(Character.class, charConverter);
        converters.put(char.class, charConverter);
        Converter doubleConverter = new DoubleConverter();
        converters.put(Double.class, doubleConverter);
        converters.put(double.class, doubleConverter);
        Converter floatConverter = new FloatConverter();
        converters.put(Float.class, floatConverter);
        converters.put(float.class, floatConverter);
        Converter longConverter = new LongConverter();
        converters.put(Long.class, longConverter);
        converters.put(long.class, longConverter);
        Converter shortConverter = new ShortConverter();
        converters.put(Short.class, shortConverter);
        converters.put(short.class, shortConverter);
        Converter byteConverter = new ByteConverter();
        converters.put(Byte.class, byteConverter);
        converters.put(byte.class, byteConverter);
        converters.put(String.class, new StringConverter());
        converters.put(File.class, new FileConverter());
        converters.put(Resource.class, new FileResourceConverter());
        converters.put(URL.class, new URLConverter());
        converters.put(URI.class, new URIConverter());
    }

    public boolean hasConverter(Class clazz) {
        return converters.containsKey(clazz);
    }

    public Converter getConverter(Class clazz) {
        return converters.get(clazz);
    }

    public void setConverter(Class<?> clazz, Converter converter) {
        converters.put(clazz, converter);
    }

    public Set<Class> getConvertedTypes() {
        return Collections.unmodifiableSet(new HashSet<>(converters.keySet()));
    }

}
