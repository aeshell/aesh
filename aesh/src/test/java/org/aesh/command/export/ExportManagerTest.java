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
package org.aesh.command.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.aesh.terminal.utils.Config;
import org.junit.Test;

/**
 * @author Aesh team
 */
public class ExportManagerTest {

    @Test
    public void testAddVariable() throws IOException {

        ExportManager exportManager = new ExportManager(
                new File(Config.getTmpDir() + Config.getPathSeparator() + "aesh_variable_test"));

        exportManager.addVariable("export TEST=/foo/bar");
        assertEquals("/foo/bar", exportManager.getValue("TEST"));
        exportManager.addVariable("export FOO=/opt");
        exportManager.addVariable("export FOO2=$FOO");
        assertEquals("/opt", exportManager.getValue("$FOO"));
        assertEquals("/opt", exportManager.getValue("${FOO}"));
        assertEquals("/opt", exportManager.getValue("FOO2"));
        assertEquals("/opt", exportManager.getValue("${FOO2}"));
        assertEquals("/opt:/foo/bar", exportManager.getValue("$FOO:$TEST"));
        assertEquals("/opt:/foo/bar", exportManager.getValue("$FOO2:${TEST}"));
        assertEquals("/opt:/foo/bar:/foo", exportManager.getValue("$FOO2:$TEST:/foo"));
        assertEquals("", exportManager.getValue("$FOO3"));

        exportManager.addVariable("export PATH=$FOO2:$TEST:/foo");
        exportManager.addVariable("export PATH=$PATH:/bar");
        assertEquals("/opt:/foo/bar:/foo:/bar", exportManager.getValue("$PATH"));
        exportManager.addVariable("export FOO2=/bin");
        assertEquals("/bin", exportManager.getValue("${FOO2}"));
        assertEquals("/bin:/foo/bar:/foo:/bar", exportManager.getValue("$PATH"));

        exportManager.addVariable("export TEST=/bla /ha");
        assertEquals("/bla", exportManager.getValue("TEST"));

        assertEquals("ls -la /bla", exportManager.getValue("ls -la $TEST"));
        assertEquals("/bla ls -la /bla", exportManager.getValue("$TEST ls -la $TEST"));
    }

    @Test
    public void testMultipleVariable() throws IOException {

        ExportManager exportManager = new ExportManager(
                new File(Config.getTmpDir() + Config.getPathSeparator() + "aesh_variable_test"));

        exportManager.addVariable("export FOO=/opt");

        assertEquals("/opt /opt", exportManager.getValue("$FOO $FOO"));

        assertEquals("ls /opt /opt", exportManager.getValue("ls $FOO $FOO"));
    }

    @Test
    public void testVariableNotExist() {
        ExportManager exportManager = new ExportManager(
                new File(Config.getTmpDir() + Config.getPathSeparator() + "aesh_variable_test"));
        assertEquals("", exportManager.getValue("$FOO3"));
        assertEquals(null, exportManager.getValue("FOO3"));

    }

    @Test
    public void testLoadSystemEnv() throws IOException {

        ExportManager exportManager = new ExportManager(
                new File(Config.getTmpDir() + Config.getPathSeparator() + "aesh_variable_test"), true);

        String result = exportManager.getValue("PATH");
        if (Config.isOSPOSIXCompatible()) {
            assertTrue(result.contains("/usr"));
        }
    }

    // Regression test for: self-referencing undefined variable should not NPE
    @Test
    public void testSelfReferencingUndefinedVariable() {
        ExportManager exportManager = new ExportManager(
                new File(Config.getTmpDir() + Config.getPathSeparator() + "aesh_variable_test"));

        // PATH is not defined yet; export PATH=$PATH:/new/dir should not throw NPE
        String error = exportManager.addVariable("export PATH=$PATH:/new/dir");
        assertNull("addVariable should succeed (return null)", error);
        // $PATH was undefined, so it expands to empty string
        assertEquals(":/new/dir", exportManager.getValue("PATH"));

        // Now set it again — this time PATH exists
        exportManager.addVariable("export PATH=$PATH:/extra");
        assertEquals(":/new/dir:/extra", exportManager.getValue("PATH"));
    }

    // Regression test for: listAllVariables with system env should use correct map
    @Test
    public void testListAllVariablesWithSystemEnv() {
        ExportManager exportManager = new ExportManager(
                new File(Config.getTmpDir() + Config.getPathSeparator() + "aesh_variable_test"), true);

        exportManager.addVariable("export MY_CUSTOM_VAR=hello");

        String listing = exportManager.listAllVariables();

        // Should contain our custom variable
        assertTrue("Should contain custom variable", listing.contains("MY_CUSTOM_VAR=hello"));

        // If on a POSIX system, PATH should be listed with its actual value, not "null"
        if (Config.isOSPOSIXCompatible()) {
            assertTrue("System env PATH should appear in listing", listing.contains("PATH="));
            assertFalse("PATH value should not be 'null'", listing.contains("PATH=null"));
        }
    }

    @Test
    public void testPersistAndReload() throws IOException {
        File exportFile = new File(Config.getTmpDir() + Config.getPathSeparator() + "aesh_persist_test");
        try {
            ExportManager exportManager = new ExportManager(exportFile);
            exportManager.addVariable("export FOO=bar");
            exportManager.addVariable("export BAZ=qux");
            exportManager.persistVariables();

            assertTrue("Export file should exist after persist", exportFile.isFile());

            // Reload from file
            ExportManager reloaded = new ExportManager(exportFile);
            assertEquals("bar", reloaded.getValue("FOO"));
            assertEquals("qux", reloaded.getValue("BAZ"));
        } finally {
            exportFile.delete();
        }
    }

    @Test
    public void testFindAllMatchingKeys() {
        ExportManager exportManager = new ExportManager(
                new File(Config.getTmpDir() + Config.getPathSeparator() + "aesh_variable_test"));

        exportManager.addVariable("export FOO=1");
        exportManager.addVariable("export FOOBAR=2");
        exportManager.addVariable("export BAZ=3");

        java.util.List<String> matches = exportManager.findAllMatchingKeys("FOO");
        assertEquals(2, matches.size());
        assertTrue(matches.contains("FOO"));
        assertTrue(matches.contains("FOOBAR"));

        // With $ prefix
        matches = exportManager.findAllMatchingKeys("$FOO");
        assertEquals(2, matches.size());
        assertTrue(matches.contains("$FOO"));
        assertTrue(matches.contains("$FOOBAR"));

        // No matches
        matches = exportManager.findAllMatchingKeys("XYZ");
        assertEquals(0, matches.size());
    }

    @Test
    public void testGetAllNamesAndNamesWithEquals() {
        ExportManager exportManager = new ExportManager(
                new File(Config.getTmpDir() + Config.getPathSeparator() + "aesh_variable_test"));

        exportManager.addVariable("export A=1");
        exportManager.addVariable("export B=2");

        java.util.List<String> names = exportManager.getAllNames();
        assertTrue(names.contains("A"));
        assertTrue(names.contains("B"));

        java.util.List<String> namesWithEquals = exportManager.getAllNamesWithEquals();
        assertTrue(namesWithEquals.contains("A="));
        assertTrue(namesWithEquals.contains("B="));
    }

    @Test
    public void testInvalidExportFormat() {
        ExportManager exportManager = new ExportManager(
                new File(Config.getTmpDir() + Config.getPathSeparator() + "aesh_variable_test"));

        // Invalid format should return usage message
        String result = exportManager.addVariable("not_an_export");
        assertNotNull(result);
        assertTrue(result.contains("usage"));

        // Valid format returns null
        String valid = exportManager.addVariable("export VALID=yes");
        assertNull(valid);
    }

    @Test
    public void testGetValueIgnoreCase() {
        ExportManager exportManager = new ExportManager(
                new File(Config.getTmpDir() + Config.getPathSeparator() + "aesh_variable_test"));

        exportManager.addVariable("export MyVar=hello");
        assertEquals("hello", exportManager.getValueIgnoreCase("myvar"));
        assertEquals("hello", exportManager.getValueIgnoreCase("MYVAR"));
        assertEquals("hello", exportManager.getValueIgnoreCase("MyVar"));
        assertEquals("", exportManager.getValueIgnoreCase("nonexistent"));
    }
}
