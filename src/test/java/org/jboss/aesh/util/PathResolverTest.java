/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.util;

import org.jboss.aesh.console.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class PathResolverTest {
    private File tempDir;

    @Before
    public void before() throws IOException {
        tempDir = createTempDirectory();
        tempDir.delete();
        tempDir.mkdirs();
    }

    @After
    public void after() {
        FileListerTest.delete(tempDir, true);
    }

    @Test
    public void testPathResolver() {
        File child1 = new File(tempDir + Config.getPathSeparator()+"child1");
        File child11 = new File(child1 + Config.getPathSeparator()+"child11");

        assertEquals(child1, PathResolver.resolvePath(new File(""), child1).get(0));

        assertEquals(child1, PathResolver.resolvePath(new File(".."), child11).get(0));
        assertEquals(child1, PathResolver.resolvePath(new File("../"), child11).get(0));
        assertEquals(tempDir, PathResolver.resolvePath(new File("../.."), child11).get(0));
        assertEquals(tempDir, PathResolver.resolvePath(new File("../../"), child11).get(0));

        if(Config.isOSPOSIXCompatible()) {
            assertEquals(tempDir.getParentFile(), PathResolver.resolvePath(new File("../../../"), child11).get(0));
            assertEquals(tempDir.getParentFile().getParentFile(),
                    PathResolver.resolvePath(new File("../../../../"), child11).get(0));
            assertEquals(tempDir.getParentFile().getParentFile(),
                    PathResolver.resolvePath(new File("../../../../../"), child11).get(0));
            assertEquals(tempDir.getParentFile().getParentFile(),
                    PathResolver.resolvePath(new File("../../../../../.."), child11).get(0));
        }

        assertEquals(child11, PathResolver.resolvePath(new File("../child11"), child11).get(0));
        assertEquals(child1, PathResolver.resolvePath(new File("../../child1"), child11).get(0));
        assertEquals(child11, PathResolver.resolvePath(new File("../../child1/child11"), child11).get(0));
        assertEquals(tempDir, PathResolver.resolvePath(new File("../../../"+tempDir.getName()), child11).get(0));


        assertEquals(child11, PathResolver.resolvePath(new File("child11"), child1).get(0));
        assertEquals(child11, PathResolver.resolvePath(new File("child1/child11"), tempDir).get(0));

        assertEquals(child11, PathResolver.resolvePath(new File("./child11"), child1).get(0));
        assertEquals(child1, PathResolver.resolvePath(new File("./"), child1).get(0));

        assertEquals(child1, PathResolver.resolvePath(new File("../child1/"), child1).get(0));
        assertEquals(child1, PathResolver.resolvePath(new File("../child1/../child1"), child1).get(0));
        assertEquals(child11, PathResolver.resolvePath(new File("../child1/../child1/child11"), child1).get(0));

        assertEquals(child11, PathResolver.resolvePath(new File("../child1/./child11"), child1).get(0));

        assertEquals(child11, PathResolver.resolvePath(new File("./../child1/./child11"), child1).get(0));
        assertEquals(child11, PathResolver.resolvePath(new File("./../child1/./child11/."), child1).get(0));

        System.setProperty("user.home", tempDir.toString()+Config.getPathSeparator()+"home");

        assertEquals(new File(Config.getHomeDir()), PathResolver.resolvePath(new File("~/../home"), child1).get(0));

    }

    public static File createTempDirectory() throws IOException {
        final File temp;

        temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

        if(!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }

        if(!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return (temp);
    }
}
