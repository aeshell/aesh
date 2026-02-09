package org.aesh.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aesh.terminal.utils.Config;
import org.junit.Test;

public class PropertiesLookupTest {

    @Test
    public void testDefaultValues() {
        Pattern systemProperties = PropertiesLookup.systemProperties;

        Matcher m = systemProperties.matcher("foo");
        assertFalse(m.find());
        m = systemProperties.matcher("$foo");
        assertFalse(m.find());
        m = systemProperties.matcher("${foo");
        assertFalse(m.find());
        m = systemProperties.matcher("$foo}");
        assertFalse(m.find());
        m = systemProperties.matcher("${foo}");
        assertTrue(m.find());
        assertEquals("foo", m.group(4));
        assertEquals("foo", m.group(4));
        m = systemProperties.matcher("${java.version}");
        assertTrue(m.find());
        assertEquals("java.version", m.group(4));
        m = systemProperties.matcher("${sys:version}");
        assertTrue(m.find());
        assertEquals("sys:", m.group(3));
        assertEquals("version", m.group(4));
        m = systemProperties.matcher("${env:version}");
        assertTrue(m.find());
        assertEquals("env:", m.group(2));
        assertEquals("version", m.group(4));
        m = systemProperties.matcher("${env:}");
        assertFalse(m.find());

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
