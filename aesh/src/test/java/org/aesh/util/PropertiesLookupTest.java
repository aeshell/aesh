package org.aesh.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.aesh.terminal.utils.Config;
import org.junit.Test;

public class PropertiesLookupTest {

    @Test
    public void testResolveVariable() {
        // Non-variable strings pass through unchanged
        assertEquals("foo", PropertiesLookup.resolveVariable("foo"));
        assertEquals("$foo", PropertiesLookup.resolveVariable("$foo"));
        assertEquals("${foo", PropertiesLookup.resolveVariable("${foo"));
        assertEquals("$foo}", PropertiesLookup.resolveVariable("$foo}"));
        assertNull(PropertiesLookup.resolveVariable(null));
        assertEquals("", PropertiesLookup.resolveVariable(""));
        assertEquals("ab", PropertiesLookup.resolveVariable("ab"));

        // Valid variable references resolve
        // ${foo} — no prefix, tries sys then env
        assertNotNull(PropertiesLookup.resolveVariable("${foo}"));

        // ${sys:user.home} — system property
        assertEquals(System.getProperty("user.home"),
                PropertiesLookup.resolveVariable("${sys:user.home}"));

        // ${java.version} — no prefix, matches system property
        assertEquals(System.getProperty("java.version"),
                PropertiesLookup.resolveVariable("${java.version}"));

        // ${env:} — empty var name, returned as-is
        assertEquals("${env:}", PropertiesLookup.resolveVariable("${env:}"));

        // ${sys:} — empty prop name, returned as-is
        assertEquals("${sys:}", PropertiesLookup.resolveVariable("${sys:}"));

        // ${} — empty content, returned as-is
        assertEquals("${}", PropertiesLookup.resolveVariable("${}"));
    }

    @Test
    public void testSystemProperties() {
        List<String> values = new ArrayList<>();
        values.add("foo");
        values.add("bar.env");
        values = PropertiesLookup.checkForSystemVariables(values);
        assertEquals(2, values.size());
        assertEquals("foo", values.get(0));
        assertEquals("bar.env", values.get(1));

        values.clear();
        values.add("${fooO}");
        values.add("bar.");
        values = PropertiesLookup.checkForSystemVariables(values);
        assertEquals(2, values.size());
        assertEquals("", values.get(0));
        assertEquals("bar.", values.get(1));

        System.setProperty("foo", "bar");
        values.clear();
        values.add("${java.home}");
        values.add("${sys:java.home}");
        values.add("${env:java.home}");
        values.add("${foo}");
        values = PropertiesLookup.checkForSystemVariables(values);
        assertEquals(4, values.size());
        assertEquals(System.getProperty("java.home"), values.get(0));
        assertEquals(System.getProperty("java.home"), values.get(1));
        assertEquals("", values.get(2));
        assertEquals("bar", values.get(3));

        // this should hopefully work on posix, we assume all systems have $USER set
        if (Config.isOSPOSIXCompatible()) {
            values.clear();
            values.add("${USER}");
            values.add("${env:USER}");
            values.add("${user.name}");
            values.add("${sys:user.name}");
            values = PropertiesLookup.checkForSystemVariables(values);
            assertEquals(4, values.size());
            assertNotNull(values.get(0));
            assertEquals(System.getProperty("user.name"), values.get(0));
            assertEquals(values.get(1), values.get(0));
            assertEquals(values.get(1), values.get(2));
            assertEquals(values.get(3), values.get(2));
            assertEquals(System.getProperty("user.name"), values.get(3));
        }
    }
}
