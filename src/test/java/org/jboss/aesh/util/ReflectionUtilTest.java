/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.util;

import org.jboss.aesh.cl.converter.Converter;
import org.jboss.aesh.console.command.converter.ConverterInvocation;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ReflectionUtilTest {

    @Test
    public void testNewInstance() {

        String foo = (String) ReflectionUtil.newInstance(String.class);

        assertNotNull(foo);

        Foo1 foo1 = (Foo1) ReflectionUtil.newInstance(Foo1.class);
        assertNotNull(foo1);

        Foo2 foo2 = (Foo2) ReflectionUtil.newInstance(Foo2.class);
        assertNotNull(foo2);

        Foo3 foo3 = (Foo3) ReflectionUtil.newInstance(Foo3.class);
        assertNotNull(foo3);

        class FooConverter implements Converter<FooConverter, ConverterInvocation> {

            @Override
            public FooConverter convert(ConverterInvocation input) {
                return this;
            }
        }

        FooConverter foo4 = (FooConverter) ReflectionUtil.newInstance(FooConverter.class);
        assertNotNull(foo4);

    }

    public class Foo1 {

    }

    public static class Foo2 {

    }

    class Foo3 {

    }
}
