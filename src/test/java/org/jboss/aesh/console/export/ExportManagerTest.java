/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.export;

import org.jboss.aesh.console.Config;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ExportManagerTest {

    @Test
    public void testAddVariable() throws IOException {

        ExportManager exportManager =
                new ExportManager(new File(Config.getTmpDir()+Config.getPathSeparator()+"aesh_variable_test"));

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

        ExportManager exportManager =
                new ExportManager(new File(Config.getTmpDir()+Config.getPathSeparator()+"aesh_variable_test"));

        exportManager.addVariable("export FOO=/opt");

        assertEquals("/opt /opt", exportManager.getValue("$FOO $FOO"));

        assertEquals("ls /opt /opt", exportManager.getValue("ls $FOO $FOO"));
    }

    @Test
    public void testVariableNotExist() {
        ExportManager exportManager =
            new ExportManager(new File(Config.getTmpDir()+Config.getPathSeparator()+"aesh_variable_test"));
        assertEquals("", exportManager.getValue("$FOO3"));
        assertEquals("", exportManager.getValue("FOO3"));
    }
}
