/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.io;

import org.jboss.aesh.console.Config;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class FileResourceTestCase {

    private Path tempDir;
    private static FileAttribute fileAttribute = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-x---"));


    @Before
    public void before() throws IOException {
        tempDir = createTempDirectory();
    }


    @Test
    public void testDefaultFileResource() throws IOException {
        File tmp = tempDir.toFile();
        Resource fr1 = new FileResource(tmp);
        assertFalse(fr1.isLeaf());
        Resource child1 = new FileResource(tmp + Config.getPathSeparator()+"child1");
        FileOutputStream out = (FileOutputStream) child1.write(false);
        out.write("foo is bar".getBytes());
        out.flush();
        out.close();
        assertTrue(child1.isLeaf());

        FileInputStream in = (FileInputStream) child1.read();
        StringBuilder builder = new StringBuilder();
        int c;
        while((c = in.read()) != -1)
            builder.append((char) c);

        assertEquals("foo is bar", builder.toString());

    }

    public static Path createTempDirectory() throws IOException {
        final Path tmp;
        if(Config.isOSPOSIXCompatible())
            tmp = Files.createTempDirectory("temp"+Long.toString(System.nanoTime()), fileAttribute);
        else {
            tmp = Files.createTempDirectory("temp" + Long.toString(System.nanoTime()));
        }

        tmp.toFile().deleteOnExit();

        return tmp;
    }

}
