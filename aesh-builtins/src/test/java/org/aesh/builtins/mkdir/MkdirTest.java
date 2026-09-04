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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.builtins.mkdir;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.aesh.command.registry.CommandRegistryException;
import org.aesh.builtins.common.AeshTestCommons;
import org.aesh.builtins.ls.Ls;
import org.aesh.terminal.utils.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:00hf11@gmail.com">Helio Frota</a>
 */
public class MkdirTest extends AeshTestCommons {

    private Path tempDir;
    private String aeshRocksDir;
    private String aeshRocksSubDir;

    @Before
    public void before() throws IOException {
        tempDir = createTempDirectory();

        aeshRocksDir = tempDir.toFile().getAbsolutePath() + Config.getPathSeparator() + "aesh_rocks";

        aeshRocksSubDir = tempDir.toFile().getAbsolutePath()
                + Config.getPathSeparator()
                + "aesh_rocks"
                + Config.getPathSeparator()
                + "subdir1"
                + Config.getPathSeparator()
                + "subdir2";

    }

    @Test
    public void testMkdir() throws IOException, CommandRegistryException {

        prepare(Mkdir.class, Ls.class);
        assertFalse(new File(aeshRocksDir).exists());
        pushToOutput("mkdir -v " + aeshRocksDir);
        assertTrue(new File(aeshRocksDir).exists());

        assertFalse(new File(aeshRocksSubDir).exists());
        pushToOutput("mkdir -p " + aeshRocksSubDir);
        assertTrue(new File(aeshRocksSubDir).exists());

        finish();
    }

    @After
    public void after() {
        try {
            Files.delete(new File(aeshRocksSubDir).toPath());
            Files.delete(new File(aeshRocksDir + Config.getPathSeparator() + "subdir1").toPath());
            Files.delete(new File(aeshRocksDir).toPath());
            Files.delete(tempDir);
        }
        catch(IOException ignored) {}
    }
}
