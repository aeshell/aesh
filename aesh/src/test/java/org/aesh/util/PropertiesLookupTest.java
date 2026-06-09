package org.aesh.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.aesh.terminal.utils.Config;
import org.junit.Test;

public class PropertiesLookupTest {

    // --- Basic resolution (existing behavior) ---

    @Test
    public void testResolveVariable_nonVariablePassThrough() {
        assertEquals("foo", PropertiesLookup.resolveVariable("foo"));
        assertEquals("$foo", PropertiesLookup.resolveVariable("$foo"));
        assertEquals("${foo", PropertiesLookup.resolveVariable("${foo"));
        assertEquals("$foo}", PropertiesLookup.resolveVariable("$foo}"));
        assertNull(PropertiesLookup.resolveVariable(null));
        assertEquals("", PropertiesLookup.resolveVariable(""));
        assertEquals("ab", PropertiesLookup.resolveVariable("ab"));
        assertEquals("${}", PropertiesLookup.resolveVariable("${}"));
    }

    @Test
    public void testResolveVariable_systemProperty() {
        assertEquals(System.getProperty("user.home"),
                PropertiesLookup.resolveVariable("${sys:user.home}"));
        assertEquals(System.getProperty("java.version"),
                PropertiesLookup.resolveVariable("${java.version}"));
    }

    @Test
    public void testResolveVariable_envVariable() {
        // On POSIX systems, USER should be set
        if (Config.isOSPOSIXCompatible()) {
            String user = System.getenv("USER");
            assertNotNull(user);
            assertEquals(user, PropertiesLookup.resolveVariable("${env:USER}"));
            assertEquals(user, PropertiesLookup.resolveVariable("${USER}"));
        }
    }

    @Test
    public void testResolveVariable_emptyPrefixName() {
        assertEquals("${env:}", PropertiesLookup.resolveVariable("${env:}"));
        assertEquals("${sys:}", PropertiesLookup.resolveVariable("${sys:}"));
    }

    // --- Fallback syntax (:-) ---

    @Test
    public void testFallback_simpleDefault() {
        // Unset env var with literal fallback
        assertEquals("8080", PropertiesLookup.resolveVariable("${env:AESH_TEST_NONEXISTENT_VAR:-8080}"));
    }

    @Test
    public void testFallback_emptyDefault() {
        // Fallback to empty string
        assertEquals("", PropertiesLookup.resolveVariable("${env:AESH_TEST_NONEXISTENT_VAR:-}"));
    }

    @Test
    public void testFallback_existingVarIgnoresFallback() {
        // Existing sys prop should be used, not the fallback
        String home = System.getProperty("user.home");
        assertEquals(home, PropertiesLookup.resolveVariable("${sys:user.home:-/tmp}"));
    }

    @Test
    public void testFallback_noPrefix() {
        // No prefix: try sys prop, then env, then fallback
        assertEquals("INFO", PropertiesLookup.resolveVariable("${AESH_TEST_NONEXISTENT:-INFO}"));
    }

    @Test
    public void testFallback_sysPrefix() {
        assertEquals("4004", PropertiesLookup.resolveVariable("${sys:aesh.test.nonexistent:-4004}"));
    }

    // --- Nested fallback ---

    @Test
    public void testNestedFallback_innerResolvesToSysProp() {
        // ${env:NONEXIST:-${sys:user.home}}
        // env var doesn't exist -> fall through to sys prop
        String home = System.getProperty("user.home");
        assertEquals(home, PropertiesLookup.resolveVariable("${env:AESH_TEST_NONEXISTENT:-${sys:user.home}}"));
    }

    @Test
    public void testNestedFallback_deepNesting() {
        // ${env:A:-${env:B:-${sys:C:-X}}}
        // All nonexistent -> falls through to literal "X"
        assertEquals("X",
                PropertiesLookup.resolveVariable("${env:AESH_A:-${env:AESH_B:-${sys:aesh.c:-X}}}"));
    }

    @Test
    public void testNestedFallback_outerResolves() {
        // When the outer variable resolves, the inner is never evaluated
        System.setProperty("aesh.test.outer", "OUTER_VALUE");
        try {
            assertEquals("OUTER_VALUE",
                    PropertiesLookup.resolveVariable("${sys:aesh.test.outer:-${sys:user.home}}"));
        } finally {
            System.clearProperty("aesh.test.outer");
        }
    }

    @Test
    public void testNestedFallback_picocliStyle() {
        // Mimics jbang's picocli pattern: ${JBANG_EDITOR:-${default.edit.open:-}}
        // Both nonexistent -> empty string
        assertEquals("",
                PropertiesLookup.resolveVariable("${env:JBANG_EDITOR_TEST:-${AESH_DEFAULT_EDIT:-}}"));
    }

    // --- Escaping ---

    @Test
    public void testEscaping_dollarDollar() {
        // $${env:HOME} should produce literal ${env:HOME}
        assertEquals("${env:HOME}", PropertiesLookup.resolveVariable("$${env:HOME}"));
    }

    @Test
    public void testEscaping_shortString() {
        // Too short to be an escape
        assertEquals("$$", PropertiesLookup.resolveVariable("$$"));
        assertEquals("$$a", PropertiesLookup.resolveVariable("$$a"));
    }

    // --- Null vs empty ---

    @Test
    public void testFindEnvironmentVariable_unsetReturnsNull() {
        assertNull(PropertiesLookup.findEnvironmentVariable("AESH_THIS_VAR_SHOULD_NOT_EXIST_12345"));
    }

    @Test
    public void testFindSystemProperty_unsetReturnsNull() {
        assertNull(PropertiesLookup.findSystemProperty("aesh.this.prop.should.not.exist.12345"));
    }

    @Test
    public void testFallback_unsetTriggersDefault() {
        // Unset var -> fallback
        assertEquals("default_val",
                PropertiesLookup.resolveVariable("${env:AESH_UNSET_FOR_TEST:-default_val}"));
    }

    @Test
    public void testNoFallback_unsetReturnsEmpty() {
        // Unset var without fallback -> empty string (backward compat)
        assertEquals("", PropertiesLookup.resolveVariable("${env:AESH_UNSET_FOR_TEST}"));
    }

    // --- checkForSystemVariables (list method) ---

    @Test
    public void testCheckForSystemVariables_plainValues() {
        List<String> values = new ArrayList<>();
        values.add("foo");
        values.add("bar.env");
        values = PropertiesLookup.checkForSystemVariables(values);
        assertEquals(2, values.size());
        assertEquals("foo", values.get(0));
        assertEquals("bar.env", values.get(1));
    }

    @Test
    public void testCheckForSystemVariables_withVariables() {
        System.setProperty("foo", "bar");
        try {
            List<String> values = new ArrayList<>();
            values.add("${sys:java.home}");
            values.add("${foo}");
            values.add("literal");
            values = PropertiesLookup.checkForSystemVariables(values);
            assertEquals(3, values.size());
            assertEquals(System.getProperty("java.home"), values.get(0));
            assertEquals("bar", values.get(1));
            assertEquals("literal", values.get(2));
        } finally {
            System.clearProperty("foo");
        }
    }

    @Test
    public void testCheckForSystemVariables_withFallback() {
        List<String> values = new ArrayList<>();
        values.add("${env:NONEXISTENT_AESH_TEST:-fallback_val}");
        values = PropertiesLookup.checkForSystemVariables(values);
        assertEquals(1, values.size());
        assertEquals("fallback_val", values.get(0));
    }

    // --- POSIX env var tests ---

    @Test
    public void testPosixEnvVars() {
        if (Config.isOSPOSIXCompatible()) {
            List<String> values = new ArrayList<>();
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
        }
    }
}
