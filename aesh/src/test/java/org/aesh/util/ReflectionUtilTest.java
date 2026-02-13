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

import static org.junit.Assert.assertNotNull;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;
import org.junit.Test;

/**
 * @author Aesh team
 */
public class ReflectionUtilTest {

    @Test
    public void testNewInstance() {

        String foo = ReflectionUtil.newInstance(String.class);

        assertNotNull(foo);

        Foo1 foo1 = ReflectionUtil.newInstance(Foo1.class);
        assertNotNull(foo1);

        Foo2 foo2 = ReflectionUtil.newInstance(Foo2.class);
        assertNotNull(foo2);

        Foo3 foo3 = ReflectionUtil.newInstance(Foo3.class);
        assertNotNull(foo3);

        class FooConverter implements Converter<FooConverter, ConverterInvocation> {

            @Override
            public FooConverter convert(ConverterInvocation input) {
                return this;
            }
        }

        FooConverter foo4 = ReflectionUtil.newInstance(FooConverter.class);
        assertNotNull(foo4);

    }

    @Test
    public void testNewInstanceWithClassWithManyConstructors() {
        Foo5 foo5 = ReflectionUtil.newInstance(Foo5.class);
    }

    @Test(expected = RuntimeException.class)
    public void testExceptionWithConstructorWithMoreThanOneParam() {
        Foo4 foo4 = ReflectionUtil.newInstance(Foo4.class);
    }

    @Test(expected = RuntimeException.class)
    public void testAbstractClassThrowsRuntimeException() {
        ReflectionUtil.newInstance(AbstractFoo.class);
    }

    @Test
    public void testPrivateConstructorAccessibility() {
        PrivateConstructorFoo foo = ReflectionUtil.newInstance(PrivateConstructorFoo.class);
        assertNotNull(foo);
    }

    class Foo1 {

    }

    static class Foo2 {

    }

    class Foo3 {

    }

    class Foo4 {
        Foo4(String x, String y) {

        }
    }

    class Foo5 {
        Foo5() {

        }

        Foo5(String x) {

        }

        Foo5(String x, String y) {

        }
    }

    static abstract class AbstractFoo {
    }

    static class PrivateConstructorFoo {
        private PrivateConstructorFoo() {
        }
    }
}
