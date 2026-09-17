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
package org.aesh.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;

import org.aesh.terminal.utils.Config;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Aesh team
 */
public class FileResourceTestCase {

    private Path tempDir;
    private static final FileAttribute fileAttribute = PosixFilePermissions
            .asFileAttribute(PosixFilePermissions.fromString("rwxr-x---"));

    @Before
    public void before() throws IOException {
        tempDir = createTempDirectory();
    }

    @Test
    public void testDefaultFileResource() throws IOException {
        File tmp = tempDir.toFile();
        Resource fr1 = new FileResource(tmp);
        assertFalse(fr1.isLeaf());
        Resource child1 = new FileResource(tmp + Config.getPathSeparator() + "child1");
        FileOutputStream out = (FileOutputStream) child1.write(false);
        out.write("foo is bar".getBytes());
        out.flush();
        out.close();
        assertTrue(child1.isLeaf());

        FileInputStream in = (FileInputStream) child1.read();
        StringBuilder builder = new StringBuilder();
        int c;
        while ((c = in.read()) != -1)
            builder.append((char) c);
        in.close();

        assertEquals("foo is bar", builder.toString());

    }

    @Test
    public void testGetParent() throws IOException {
        File tmp = tempDir.toFile();
        assertEquals(tmp.getAbsolutePath(),
                new FileResource(new File(tmp, "child")).getParent().getAbsolutePath());
        assertNull(new FileResource(new File("bare-name")).getParent());
        assertNull(new FileResource(new File(Config.getPathSeparator())).getParent());
    }

    @Test
    public void testMoveRejectsNonFileResource() throws IOException {
        File tmp = tempDir.toFile();
        Resource source = new FileResource(new File(tmp, "src.txt"));
        try {
            source.move(new TestResource("other"));
            fail("move to a non-FileResource must fail");
        } catch (IOException expected) {
        }
    }

    @Test
    public void testListNullFilterReturnsAll() throws IOException {
        File tmp = tempDir.toFile();
        new File(tmp, "a.txt").createNewFile();
        Resource dir = new FileResource(tmp);
        assertEquals(1, dir.list(null).size());
    }

    @Test
    public void testReadDoesNotMutateInstance() throws IOException {
        File tmp = tempDir.toFile();
        Resource home = new FileResource("~" + Config.getPathSeparator() + "probe.txt");
        String before = home.toString();
        try {
            home.read().close();
        } catch (FileNotFoundException expected) {
            // home/probe.txt need not exist; the instance must be unchanged either way
        }
        assertEquals(before, home.toString());
    }

    private static class TestResource implements Resource {
        private final String name;

        TestResource(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getAbsolutePath() {
            return name;
        }

        @Override
        public boolean isLeaf() {
            return true;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public boolean isSymbolicLink() {
            return false;
        }

        @Override
        public Resource readSymbolicLink() {
            return null;
        }

        @Override
        public boolean exists() {
            return false;
        }

        @Override
        public boolean mkdirs() {
            return false;
        }

        @Override
        public boolean delete() {
            return false;
        }

        @Override
        public void move(Resource target) {
        }

        @Override
        public Resource getParent() {
            return null;
        }

        @Override
        public java.util.List<Resource> list() {
            return java.util.Collections.emptyList();
        }

        @Override
        public java.util.List<Resource> list(org.aesh.io.filter.ResourceFilter filter) {
            return java.util.Collections.emptyList();
        }

        @Override
        public java.util.List<Resource> listRoots() {
            return java.util.Collections.emptyList();
        }

        @Override
        public java.util.List<Resource> resolve(Resource cwd) {
            return java.util.Collections.emptyList();
        }

        @Override
        public java.io.InputStream read() {
            return null;
        }

        @Override
        public <A extends java.nio.file.attribute.BasicFileAttributes> A readAttributes(Class<A> type,
                java.nio.file.LinkOption... options) throws java.io.IOException {
            return null;
        }

        @Override
        public java.io.OutputStream write(boolean append) {
            return null;
        }

        @Override
        public Resource newInstance(String path) {
            return new TestResource(path);
        }

        @Override
        public Resource copy(Resource destination) {
            return null;
        }

        @Override
        public boolean setLastModified(long time) {
            return false;
        }

        @Override
        public long lastModified() {
            return 0;
        }

        @Override
        public void setLastAccessed(long time) {
        }

        @Override
        public long lastAccessed() {
            return 0;
        }
    }

    public static Path createTempDirectory() throws IOException {
        final Path tmp;
        if (Config.isOSPOSIXCompatible())
            tmp = Files.createTempDirectory("temp" + Long.toString(System.nanoTime()), fileAttribute);
        else {
            tmp = Files.createTempDirectory("temp" + Long.toString(System.nanoTime()));
        }

        tmp.toFile().deleteOnExit();

        return tmp;
    }

}
