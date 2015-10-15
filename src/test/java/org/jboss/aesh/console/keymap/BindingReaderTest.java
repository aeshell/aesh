/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jboss.aesh.console.keymap;

import java.io.PipedReader;
import java.io.PipedWriter;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.aesh.terminal.utils.NonBlockingReader;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BindingReaderTest {

    enum Operation {
        UNICODE,
        NOMATCH,
        INSERT,
        GA,
        GAB
    }

    @Test
    public void testConsumption() throws Exception {
        KeyMap<Operation> keyMap = new KeyMap<>();
        keyMap.setUnicode(Operation.UNICODE);
        keyMap.setNomatch(Operation.NOMATCH);
        keyMap.bind(Operation.INSERT, KeyMap.range("^@-^?"));
        keyMap.bind(Operation.GA, "ga");
        keyMap.bind(Operation.GAB, "gab");
        keyMap.unbind("d");
        
        PipedReader in = new PipedReader();
        PipedWriter out = new PipedWriter(in);
        BindingReader reader = new BindingReader(new NonBlockingReader("test", in));
        List<Map.Entry<Operation, String>> bindings = new ArrayList<>();

        out.write("adgau\uD834\uDD21gabf");
        for (int i = 0; i < 7; i++) {
            Operation b = reader.readBinding(keyMap);
            bindings.add(new SimpleImmutableEntry<>(b, reader.getLastBinding()));
        }

        assertEquals(7, bindings.size());
        assertEquals(new SimpleImmutableEntry<>(Operation.INSERT, "a"), bindings.get(0));
        assertEquals(new SimpleImmutableEntry<>(Operation.NOMATCH, "d"), bindings.get(1));
        assertEquals(new SimpleImmutableEntry<>(Operation.GA, "ga"), bindings.get(2));
        assertEquals(new SimpleImmutableEntry<>(Operation.INSERT, "u"), bindings.get(3));
        assertEquals(new SimpleImmutableEntry<>(Operation.UNICODE, "\uD834\uDD21"), bindings.get(4));
        assertEquals(new SimpleImmutableEntry<>(Operation.GAB, "gab"), bindings.get(5));
        assertEquals(new SimpleImmutableEntry<>(Operation.INSERT, "f"), bindings.get(6));
    }

    @Test
    public void testTimer() throws Exception {
        KeyMap<Operation> keyMap = new KeyMap<>();
        keyMap.setUnicode(Operation.UNICODE);
        keyMap.setNomatch(Operation.NOMATCH);
        keyMap.setAmbigousTimeout(100);
        keyMap.bind(Operation.INSERT, KeyMap.range("^@-^?"));
        keyMap.bind(Operation.GA, "ga");
        keyMap.bind(Operation.GAB, "gab");
        keyMap.unbind("d");

        PipedReader in = new PipedReader();
        PipedWriter out = new PipedWriter(in);
        BindingReader reader = new BindingReader(new NonBlockingReader("test", in));
        List<Map.Entry<Object, String>> bindings = new ArrayList<>();

        out.write("adga");
        for (int i = 0; i < 2; i++) {
            Operation b = reader.readBinding(keyMap);
            bindings.add(new SimpleImmutableEntry<>(b, reader.getLastBinding()));
        }
        long t0 = System.currentTimeMillis();
        {
            Operation b = reader.readBinding(keyMap);
            bindings.add(new SimpleImmutableEntry<>(b, reader.getLastBinding()));
        }
        long t1 = System.currentTimeMillis();
        assertTrue(t1 - t0 >= 100);

        out.write("bf");
        for (int i = 0; i < 2; i++) {
            Operation b = reader.readBinding(keyMap);
            bindings.add(new SimpleImmutableEntry<>(b, reader.getLastBinding()));
        }

        assertEquals(5, bindings.size());
        assertEquals(new SimpleImmutableEntry<>(Operation.INSERT, "a"), bindings.get(0));
        assertEquals(new SimpleImmutableEntry<>(Operation.NOMATCH, "d"), bindings.get(1));
        assertEquals(new SimpleImmutableEntry<>(Operation.GA, "ga"), bindings.get(2));
        assertEquals(new SimpleImmutableEntry<>(Operation.INSERT, "b"), bindings.get(3));
        assertEquals(new SimpleImmutableEntry<>(Operation.INSERT, "f"), bindings.get(4));
    }

}
