/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
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

import org.aesh.terminal.utils.Config;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        assertEquals(null, exportManager.getValue("FOO3"));

    }

    @Test
    public void testLoadSystemEnv() throws IOException {

        ExportManager exportManager =
                new ExportManager(new File(Config.getTmpDir()+Config.getPathSeparator()+"aesh_variable_test"), true);

        String result = exportManager.getValue("PATH");
        if (Config.isOSPOSIXCompatible()) {
            assertTrue(result.contains("/usr"));
        }
    }
}
