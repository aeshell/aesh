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

import org.aesh.command.converter.Converter;
import org.aesh.io.Resource;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CLConverterManager {

    private Map<Class, Converter> converters;

    private static class CLConvertManagerHolder {
        static final CLConverterManager INSTANCE = new CLConverterManager();
    }

    public static CLConverterManager getInstance() {
        return CLConvertManagerHolder.INSTANCE;
    }

    private CLConverterManager() {
        initMap();
    }

    private void initMap() {
        converters = new HashMap<Class, Converter>();
        converters.put(Integer.class, new IntegerConverter());
        converters.put(int.class, converters.get(Integer.class));
        converters.put(Boolean.class, new BooleanConverter());
        converters.put(boolean.class, converters.get(Boolean.class));
        converters.put(Character.class, new CharacterConverter());
        converters.put(char.class, converters.get(Character.class));
        converters.put(Double.class, new DoubleConverter());
        converters.put(double.class, converters.get(Double.class));
        converters.put(Float.class, new FloatConverter());
        converters.put(float.class, converters.get(Float.class));
        converters.put(Long.class, new LongConverter());
        converters.put(long.class, converters.get(Long.class));
        converters.put(Short.class, new ShortConverter());
        converters.put(short.class, converters.get(Short.class));
        converters.put(Byte.class, new ByteConverter());
        converters.put(byte.class, converters.get(Byte.class));
        converters.put(String.class, new StringConverter());
        converters.put(File.class, new FileConverter());
        converters.put(Resource.class, new FileResourceConverter());
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
        return Collections.unmodifiableSet(converters.keySet());
    }

}
