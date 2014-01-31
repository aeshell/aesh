/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.converter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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
    }

    public boolean hasConverter(Class clazz) {
        return converters.containsKey(clazz);
    }

    public Converter getConverter(Class clazz) {
        return converters.get(clazz);
    }

}
