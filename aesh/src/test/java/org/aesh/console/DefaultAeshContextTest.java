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
package org.aesh.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import java.io.File;

import org.aesh.io.FileResource;
import org.aesh.io.Resource;
import org.junit.Test;

/**
 * Tests for {@link DefaultAeshContext}: the default working directory
 * materializes lazily on first read so constructing the context neither
 * touches {@code Config} (whose class-init loads the NIO filesystem and
 * stream machinery) nor stats the directory (#794).
 */
public class DefaultAeshContextTest {

    @Test
    public void testDefaultWorkingDirectoryResolvesLazily() {
        DefaultAeshContext context = new DefaultAeshContext();
        Resource cwd = context.getCurrentWorkingDirectory();
        assertNotNull(cwd);
        assertEquals(System.getProperty("user.dir"), cwd.getAbsolutePath());
        // Repeated reads return the same materialized instance
        assertSame(cwd, context.getCurrentWorkingDirectory());
    }

    @Test
    public void testCachedDefaultInstanceResolvesLazily() {
        DefaultAeshContext first = DefaultAeshContext.getDefault();
        Resource cwd = first.getCurrentWorkingDirectory();
        assertEquals(new File(System.getProperty("user.dir")), new File(cwd.getAbsolutePath()));
        assertSame(first, DefaultAeshContext.getDefault());
    }

    @Test
    public void testExplicitResourceConstructorStillValidatesEagerly() {
        File missing = new File(System.getProperty("user.dir"), "no-such-dir-__definitely_missing__");
        assertThrows(IllegalArgumentException.class,
                () -> new DefaultAeshContext(new FileResource(missing)));
    }

    @Test
    public void testSetCurrentWorkingDirectoryReplacesResolvedDefault() {
        DefaultAeshContext context = new DefaultAeshContext();
        File temp = new File(System.getProperty("java.io.tmpdir"));
        context.setCurrentWorkingDirectory(new FileResource(temp));
        assertEquals(temp, new File(context.getCurrentWorkingDirectory().getAbsolutePath()));
    }
}
