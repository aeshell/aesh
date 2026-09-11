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
package org.aesh.builtins.pushd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import org.aesh.builtins.common.AeshTestCommons;
import org.aesh.command.registry.CommandRegistryException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PushdPopdTest extends AeshTestCommons {

    private Path tempDir;

    @Before
    public void before() throws IOException {
        DirectoryStack.getInstance().clear();
        tempDir = createTempDirectory();
    }

    @After
    public void after() throws IOException {
        DirectoryStack.getInstance().clear();
        deleteRecursiveTempDirectory(tempDir);
    }

    @Test
    public void testPushdPopd() throws IOException, CommandRegistryException {
        prepare(Pushd.class, Popd.class);
        String original = getAeshContext().getCurrentWorkingDirectory().getAbsolutePath();
        String target = tempDir.toFile().getAbsolutePath();

        pushToOutput("pushd " + target);
        assertEquals(target, getAeshContext().getCurrentWorkingDirectory().getAbsolutePath());

        pushToOutput("popd");
        assertEquals(original, getAeshContext().getCurrentWorkingDirectory().getAbsolutePath());

        connection().clearOutputBuffer();
        pushToOutput("popd");
        assertTrue(getStream().contains("popd: directory stack empty"));

        finish();
    }

    @Test
    public void testPopdNoChange() throws IOException, CommandRegistryException {
        prepare(Pushd.class, Popd.class);
        String target = tempDir.toFile().getAbsolutePath();

        pushToOutput("pushd " + target);
        assertEquals(target, getAeshContext().getCurrentWorkingDirectory().getAbsolutePath());

        pushToOutput("popd -n");
        assertEquals(target, getAeshContext().getCurrentWorkingDirectory().getAbsolutePath());

        finish();
    }
}
