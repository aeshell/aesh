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
package org.aesh.builtins.tty;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFileAttributes;

import org.aesh.terminal.utils.Config;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AeshPosixFilePermissionsTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testToString() throws IOException {
        Assume.assumeTrue(Config.isOSPOSIXCompatible());
        File file = folder.newFile("perms.txt");
        PosixFileAttributes attributes =
                Files.readAttributes(file.toPath(), PosixFileAttributes.class);

        assertTrue(AeshPosixFilePermissions.toString(attributes).matches("[dl-][rwx-]{9}"));
    }
}
