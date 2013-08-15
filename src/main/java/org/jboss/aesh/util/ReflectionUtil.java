/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ReflectionUtil {

    public static <T> T newInstance(final Class<T> clazz) {
        if(clazz.isAnonymousClass()) {
            throw new RuntimeException("Can not create new instance of an anonymous class");
        }
        else if(clazz.isInterface())
            throw new RuntimeException("Can not create new instance of an interface");
        else if(clazz.isAnnotation())
            throw new RuntimeException("Can not create new instance of an annotation");

        for(Constructor<?> constructor : clazz.getConstructors()) {
            T object = (T) instantiateWithConstructor(constructor);
            if(object != null)
                return object;
        }

        for(Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            T object = (T) instantiateWithConstructor(constructor);
            if(object != null)
                return object;
        }

        throw new RuntimeException("Could not instantiate class: "+clazz+", no access to constructors.");
    }

    private static <T> T instantiateWithConstructor(Constructor<T> constructor) {
            if(constructor.getParameterTypes().length == 0) {
                if(Modifier.isPrivate( constructor.getModifiers()) ||
                        Modifier.isProtected( constructor.getModifiers())) {
                    constructor.setAccessible(true);
                }
                try {
                    return constructor.newInstance();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            //inner classes require parent class as parameter
            else if(constructor.getParameterTypes().length == 1) {
                if(Modifier.isPrivate( constructor.getModifiers()) ||
                        Modifier.isProtected( constructor.getModifiers())) {
                    constructor.setAccessible(true);
                }

                Constructor paramConstructor = getConstructorWithNoParams( constructor.getParameterTypes()[0]);

                if(paramConstructor != null) {
                    try {
                        return constructor.newInstance(paramConstructor.newInstance());
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        return null;
    }

    private static Constructor getConstructorWithNoParams(Class clazz) {
        for(Constructor constructor : clazz.getConstructors()) {
            if(constructor.getParameterTypes().length == 0) {
                if(Modifier.isPrivate( constructor.getModifiers()) ||
                        Modifier.isProtected( constructor.getModifiers())) {
                    constructor.setAccessible(true);
                }
                return constructor;
            }
        }
        return null;
    }

}
