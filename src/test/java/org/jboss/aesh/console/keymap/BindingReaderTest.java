/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jboss.aesh.console.keymap;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BindingReaderTest {

    static final Object UNICODE = "unicode";
    static final Object NOMATCH = "nomatch";
    static final Object SELF_INSERT = "self-insert";
    static final Object GA = "ga";
    static final Object GAB = "gab";

    @Test
    public void testConsumption() throws Exception {
        KeyMap keyMap = new KeyMap();
        keyMap.setUnicode(UNICODE);
        keyMap.setNomatch(NOMATCH);
        keyMap.bind(SELF_INSERT, KeyMap.range("^@-^?"));
        keyMap.bind(GA, "ga");
        keyMap.bind(GAB, "gab");
        keyMap.unbind("d");
        Tester tester = new Tester();
        tester.reader.setKeyMaps(keyMap);
        "adgau\uD834\uDD21gabf".chars().forEachOrdered(tester.reader::accept);

        assertEquals(7, tester.bindings.size());
        assertEquals(new SimpleImmutableEntry<>(SELF_INSERT, "a"), tester.bindings.get(0));
        assertEquals(new SimpleImmutableEntry<>(NOMATCH, "d"), tester.bindings.get(1));
        assertEquals(new SimpleImmutableEntry<>(GA, "ga"), tester.bindings.get(2));
        assertEquals(new SimpleImmutableEntry<>(SELF_INSERT, "u"), tester.bindings.get(3));
        assertEquals(new SimpleImmutableEntry<>(UNICODE, "\uD834\uDD21"), tester.bindings.get(4));
        assertEquals(new SimpleImmutableEntry<>(GAB, "gab"), tester.bindings.get(5));
        assertEquals(new SimpleImmutableEntry<>(SELF_INSERT, "f"), tester.bindings.get(6));
    }

    @Test
    public void testTimer() throws Exception {
        KeyMap keyMap = new KeyMap();
        keyMap.setUnicode(UNICODE);
        keyMap.setNomatch(NOMATCH);
        keyMap.setAmbigousTimeout(100);
        keyMap.bind(SELF_INSERT, KeyMap.range("^@-^?"));
        keyMap.bind(GA, "ga");
        keyMap.bind(GAB, "gab");
        keyMap.unbind("d");
        Tester tester = new Tester();
        tester.reader.setKeyMaps(keyMap);

        "adga".chars().forEachOrdered(tester.reader::accept);
        Thread.sleep(200);
        "bf".chars().forEachOrdered(tester.reader::accept);

        assertEquals(5, tester.bindings.size());
        assertEquals(new SimpleImmutableEntry<>(SELF_INSERT, "a"), tester.bindings.get(0));
        assertEquals(new SimpleImmutableEntry<>(NOMATCH, "d"), tester.bindings.get(1));
        assertEquals(new SimpleImmutableEntry<>(GA, "ga"), tester.bindings.get(2));
        assertEquals(new SimpleImmutableEntry<>(SELF_INSERT, "b"), tester.bindings.get(3));
        assertEquals(new SimpleImmutableEntry<>(SELF_INSERT, "f"), tester.bindings.get(4));
    }

    static class Tester implements Consumer<Object> {
        List<Map.Entry<Object, String>> bindings = new ArrayList<>();
        BindingReader reader;

        public Tester() {
            reader = new BindingReader(this, Executors.newSingleThreadScheduledExecutor());
        }

        @Override
        public void accept(Object o) {
            bindings.add(new SimpleImmutableEntry<>(o, reader.getLastBinding()));
        }
    }
}
