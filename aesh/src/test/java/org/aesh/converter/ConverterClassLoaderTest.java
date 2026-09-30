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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.console.AeshContext;
import org.junit.Test;

/**
 * The process-global converter cache must not pin reloaded applications:
 * an enum class converted through {@link CLConverterManager} and then
 * dropped must be reclaimable along with its defining classloader (#667).
 */
public class ConverterClassLoaderTest {

    private static ConverterInvocation invocationFor(String input) {
        return new ConverterInvocation() {
            @Override
            public String getInput() {
                return input;
            }

            @Override
            public AeshContext getAeshContext() {
                return null;
            }
        };
    }

    /**
     * Loads one class child-first from the test-classes output, delegating
     * everything else to the parent loader.
     */
    private static Class<?> childFirstLoad(String className) throws Exception {
        URL classesDir = ReloadableEnum.class.getProtectionDomain().getCodeSource().getLocation();
        ClassLoader parent = ConverterClassLoaderTest.class.getClassLoader();
        ClassLoader child = new URLClassLoader(new URL[] { classesDir }, parent) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null && name.equals(className)) {
                        try {
                            loaded = findClass(name);
                        } catch (ClassNotFoundException e) {
                            // fall through to parent below
                        }
                    }
                    if (loaded == null)
                        loaded = super.loadClass(name, false);
                    if (resolve)
                        resolveClass(loaded);
                    return loaded;
                }
            }
        };
        return Class.forName(className, false, child);
    }

    private static void awaitGcClear(String what, java.util.function.BooleanSupplier cleared)
            throws InterruptedException {
        boolean done = false;
        for (int i = 0; i < 100 && !done; i++) {
            System.gc();
            Thread.sleep(50);
            done = cleared.getAsBoolean();
        }
        assertTrue(what + " must be reclaimable", done);
    }

    @Test
    public void testReloadedEnumClassLoaderIsReclaimable() throws Exception {
        Class<?> reloaded = childFirstLoad(ReloadableEnum.class.getName());
        assertTrue("fixture must load isolated", reloaded != ReloadableEnum.class);
        ClassLoader loader = reloaded.getClassLoader();
        java.lang.ref.WeakReference<Class<?>> classRef = new java.lang.ref.WeakReference<>(reloaded);
        java.lang.ref.WeakReference<ClassLoader> loaderRef = new java.lang.ref.WeakReference<>(loader);

        Converter converter = CLConverterManager.getInstance().getConverter(reloaded);
        Object converted = converter.convert(invocationFor("A"));
        assertEquals("A", converted.toString());

        // Drop every strong reference: the converter, the converted
        // constant (pins its class) and the class itself.
        converted = null;
        converter = null;
        if (loader instanceof URLClassLoader)
            ((URLClassLoader) loader).close();
        reloaded = null;
        loader = null;

        awaitGcClear("reloaded enum class", () -> classRef.get() == null);
        awaitGcClear("enum classloader", () -> loaderRef.get() == null);
    }

    @Test(timeout = 30000)
    public void testConcurrentConversionThenReclaim() throws Exception {
        Class<?> reloaded = childFirstLoad(ReloadableEnum.class.getName());
        ClassLoader loader = reloaded.getClassLoader();
        java.lang.ref.WeakReference<Class<?>> classRef = new java.lang.ref.WeakReference<>(reloaded);
        java.lang.ref.WeakReference<ClassLoader> loaderRef = new java.lang.ref.WeakReference<>(loader);

        // Isolated helper: its stack frame (holding the enum class) is gone
        // before reclamation is asserted below.
        runConcurrentConversions(reloaded);

        reloaded = null;
        loader = null;
        awaitGcClear("reloaded enum class", () -> classRef.get() == null);
        awaitGcClear("enum classloader", () -> loaderRef.get() == null);
    }

    private static void runConcurrentConversions(Class<?> enumClass) throws Exception {
        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                futures.add(executor.submit(() -> {
                    try {
                        for (int i = 0; i < 50; i++) {
                            Converter converter = CLConverterManager.getInstance()
                                    .getConverter(enumClass);
                            Object value = converter.convert(invocationFor(i % 2 == 0 ? "A" : "B"));
                            if (!value.toString().equals(i % 2 == 0 ? "A" : "B"))
                                throw new IllegalStateException("wrong value: " + value);
                        }
                    } catch (Throwable e) {
                        failure.compareAndSet(null, e);
                    }
                    return null;
                }));
            }
            for (Future<?> future : futures)
                future.get();
            assertTrue("concurrent conversion must stay quiet, got: " + failure.get(),
                    failure.get() == null);
            // Completed futures retain their lambdas, which capture the
            // enum class: drop them before asserting reclamation.
            futures.clear();
        } finally {
            executor.shutdownNow();
        }
    }
}
