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
package org.aesh.builtins.touch;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
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
public class TouchTest extends AeshTestCommons {

    private Path tempDir;

    @Before
    public void before() throws IOException {
        tempDir = createTempDirectory();
    }

    @After
    public void after() throws IOException {
        deleteRecursiveTempDirectory(tempDir);
    }

    @Test
    public void testTouch() throws IOException, CommandRegistryException {

        prepare(Touch.class, Ls.class);

        String tempPath = tempDir.toFile().getAbsolutePath() + Config.getPathSeparator();

        pushToOutput("touch " + tempPath + "file01.txt");
        assertTrue(new File(tempPath+"file01.txt").exists());

        pushToOutput("touch -a " + tempPath + "file01.txt");
        assertTrue(new File(tempPath+"file01.txt").exists());

        pushToOutput("touch -c " + tempPath + "file02.txt");
        assertFalse(new File(tempPath+"file02.txt").exists());

        pushToOutput("touch -m " + tempPath + "file02.txt");
        assertTrue(new File(tempPath+"file02.txt").exists());

    }

}
