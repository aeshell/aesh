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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class PathResolverTest {
    private Path tempDir;

    @Test
    public void test() {

    }

    /*
    @Before
    public void before() throws IOException {
        tempDir = createTempDirectory();
    }

    @After
    public void after() {
        //FileListerTest.delete(tempDir, true);
    }

    @Test
    public void testListFiles() {
        File tmp = tempDir.toFile();
        File child1 = new File(tmp + Config.getPathSeparator()+"child1");
        File child2 = new File(tmp + Config.getPathSeparator()+"child2");
        File child11 = new File(child1 + Config.getPathSeparator()+"child11");
        File child12 = new File(child1 + Config.getPathSeparator()+"child12");
        File child21 = new File(child1 + Config.getPathSeparator()+"child21");
        File child22 = new File(child1 + Config.getPathSeparator()+"child22");

        assertEquals(child1, PathResolver.resolvePath(new File(""), child1).get(0));

        assertEquals(child1, PathResolver.resolvePath(new File(".."), child11).get(0));
        assertEquals(child1, PathResolver.resolvePath(new File("../"), child11).get(0));
        assertEquals(tmp, PathResolver.resolvePath(new File("../.."), child11).get(0));
        assertEquals(tmp, PathResolver.resolvePath(new File("../../"), child11).get(0));

        if(Config.isOSPOSIXCompatible()) {
            assertEquals(tmp.getParentFile(), PathResolver.resolvePath(new File("../../../"), child11).get(0));
            assertEquals(tmp.getParentFile().getParentFile(),
                    PathResolver.resolvePath(new File("../../../../"), child11).get(0));
            assertEquals(tmp.getParentFile().getParentFile(),
                    PathResolver.resolvePath(new File("../../../../../"), child11).get(0));
            assertEquals(tmp.getParentFile().getParentFile(),
                    PathResolver.resolvePath(new File("../../../../../.."), child11).get(0));
        }

        assertEquals(child11, PathResolver.resolvePath(new File("../child11"), child11).get(0));
        assertEquals(child1, PathResolver.resolvePath(new File("../../child1"), child11).get(0));
        assertEquals(child11, PathResolver.resolvePath(new File("../../child1/child11"), child11).get(0));
        assertEquals(tmp, PathResolver.resolvePath(new File("../../../"+tmp.getName()), child11).get(0));


        assertEquals(child11, PathResolver.resolvePath(new File("child11"), child1).get(0));
        assertEquals(child11, PathResolver.resolvePath(new File("child1/child11"), tmp).get(0));

        assertEquals(child11, PathResolver.resolvePath(new File("./child11"), child1).get(0));
        assertEquals(child1, PathResolver.resolvePath(new File("./"), child1).get(0));

        assertEquals(child1, PathResolver.resolvePath(new File("../child1/"), child1).get(0));
        assertEquals(child1, PathResolver.resolvePath(new File("../child1/../child1"), child1).get(0));
        assertEquals(child11, PathResolver.resolvePath(new File("../child1/../child1/child11"), child1).get(0));

        assertEquals(child11, PathResolver.resolvePath(new File("../child1/./child11"), child1).get(0));

        assertEquals(child11, PathResolver.resolvePath(new File("./../child1/./child11"), child1).get(0));
        assertEquals(child11, PathResolver.resolvePath(new File("./../child1/./child11/."), child1).get(0));

        //System.setProperty("user.home", tempDir.toString()+Config.getPathSeparator()+"home");
        //assertEquals(new File(Config.getHomeDir()), PathResolver.resolvePath(new File("~/../home"), child1).get(0));
    }

    @Test
    public void testSearchFiles() {
        File child1 = new File(tempDir + Config.getPathSeparator()+"child1");
        File child2 = new File(tempDir + Config.getPathSeparator()+"child2");
        File child11 = new File(child1 + Config.getPathSeparator()+"child11");
        File child12 = new File(child1 + Config.getPathSeparator()+"child12");
        File child21 = new File(child1 + Config.getPathSeparator()+"child21");
        File child22 = new File(child1 + Config.getPathSeparator()+"child22");

        ArrayList<File> files = new ArrayList<>();
        files.add(child11);
        files.add(child12);

        assertEquals(files, PathResolver.findFiles(child1, "*", false));
    }

    @Test
    public void testNIO() {
        Path path = new File("/home/stalep/java").toPath();

        DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return Files.isDirectory(entry);
            }
        };

        for(int i=0; i < 3; i++) {
            long start = System.nanoTime();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, filter)) {

                System.out.println("finding files took: "+(System.nanoTime()-start));
                ArrayList<Path> list = new ArrayList<>();

                for(Path p : stream) {
                    list.add(p);
                }
                System.out.println("total took: "+(System.nanoTime()-start)+", to find: "+list.size()+" directories");

            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static Path createTempDirectory() throws IOException {
        final Path tmp;
        tmp = Files.createTempDirectory(Config.getTmpDir()+Config.getPathSeparator()+"temp"+Long.toString(System.nanoTime()), null);
        tmp.toFile().deleteOnExit();

        return tmp;
    }
    */

}
