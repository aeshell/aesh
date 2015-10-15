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
package org.jboss.aesh.terminal.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Signals helpers.
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public final class Signals {

    private Signals() {
    }

    /**
     *
     * @param name the signal, CONT, STOP, etc...
     * @param handler the callback to run
     *
     * @return an object that needs to be passed to the {@link #unregister(String, Object)}
     *         method to unregister the handler
     */
    public static Object register(String name, Runnable handler) {
        assert name != null;
        assert handler != null;
        return register(name, handler, handler.getClass().getClassLoader());
    }

    public static Object register(String name, final Runnable handler, ClassLoader loader) {
        try {
            Class<?> signalHandlerClass = Class.forName("sun.misc.SignalHandler");
            // Implement signal handler
            Object signalHandler = Proxy.newProxyInstance(loader,
                    new Class<?>[]{signalHandlerClass}, new InvocationHandler() {
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            // only method we are proxying is handle()
                            handler.run();
                            return null;
                        }
                    });
            doRegister(name, signalHandler);
        } catch (Exception e) {
            // Ignore this one too, if the above failed, the signal API is incompatible with what we're expecting
        }
        return null;
    }

    public static Object registerDefault(String name) {
        try {
            Class<?> signalHandlerClass = Class.forName("sun.misc.SignalHandler");
            doRegister(name, signalHandlerClass.getField("SIG_DFL").get(null));
        } catch (Exception e) {
            // Ignore this one too, if the above failed, the signal API is incompatible with what we're expecting
        }
        return null;
    }

    public static Object registerIgnore(String name) {
        try {
            Class<?> signalHandlerClass = Class.forName("sun.misc.SignalHandler");
            doRegister(name, signalHandlerClass.getField("SIG_IGN").get(null));
        } catch (Exception e) {
            // Ignore this one too, if the above failed, the signal API is incompatible with what we're expecting
        }
        return null;
    }

    public static void unregister(String name, Object previous) {
        try {
            // We should make sure the current signal is the one we registered
            if (previous != null) {
                doRegister(name, previous);
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private static Object doRegister(String name, Object handler) throws Exception {
        Class<?> signalClass = Class.forName("sun.misc.Signal");
        Class<?> signalHandlerClass = Class.forName("sun.misc.SignalHandler");
        Object signal = signalClass.getConstructor(String.class).newInstance(name);
        return signalClass.getMethod("handle", signalClass, signalHandlerClass)
                .invoke(null, signal, handler);
    }

}
