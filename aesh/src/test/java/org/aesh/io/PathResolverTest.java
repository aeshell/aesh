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
package org.aesh.io;

import org.aesh.terminal.utils.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class PathResolverTest {
    private Path tempDir;
    private static final FileAttribute fileAttribute = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-x---"));

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
        assertEquals(child1, PathResolver.resolvePath(new File(".."+Config.getPathSeparator()), child11).get(0));
        assertEquals(tmp, PathResolver.resolvePath(new File(".."+Config.getPathSeparator()+".."), child11).get(0));
        assertEquals(tmp, PathResolver.resolvePath(new File(".."+Config.getPathSeparator()+
                ".."+Config.getPathSeparator()), child11).get(0));

        if(Config.isOSPOSIXCompatible()) {
            assertEquals(tmp.getParentFile(), PathResolver.resolvePath(new File("../../../"), child11).get(0));
            assertEquals(tmp.getParentFile().getParentFile(),
                    PathResolver.resolvePath(new File("../../../../"), child11).get(0));
//            assertEquals(tmp.getParentFile().getParentFile(),
//                    PathResolver.resolvePath(new File("../../../../../"), child11).get(0));
//            assertEquals(tmp.getParentFile().getParentFile(),
//                    PathResolver.resolvePath(new File("../../../../../.."), child11).get(0));
        }

        assertEquals(child11, PathResolver.resolvePath(new File(".."+Config.getPathSeparator()+"child11"), child11).get(0));
        assertEquals(child1, PathResolver.resolvePath(new File(".."+Config.getPathSeparator()+".."+Config.getPathSeparator()+"child1"),
                child11).get(0));
        assertEquals(child11, PathResolver.resolvePath(new File(".."+Config.getPathSeparator()+".."+
                Config.getPathSeparator()+"child1"+Config.getPathSeparator()+"child11"), child11).get(0));
        assertEquals(tmp, PathResolver.resolvePath(new File(".."+Config.getPathSeparator()+".."+
                Config.getPathSeparator()+".."+Config.getPathSeparator()+tmp.getName()), child11).get(0));


        assertEquals(child11, PathResolver.resolvePath(new File("child11"), child1).get(0));
        assertEquals(child11, PathResolver.resolvePath(new File("child1"+Config.getPathSeparator()+"child11"), tmp).get(0));

        assertEquals(child11, PathResolver.resolvePath(new File("."+Config.getPathSeparator()+"child11"), child1).get(0));

        assertEquals(child1, PathResolver.resolvePath(
                new File(".."+Config.getPathSeparator()+"child1"+Config.getPathSeparator()), child1).get(0));
        assertEquals(child1, PathResolver.resolvePath(new File(".."+Config.getPathSeparator()+"child1"+
                Config.getPathSeparator()+".."+Config.getPathSeparator()+"child1"), child1).get(0));
        assertEquals(child11, PathResolver.resolvePath(new File(".."+Config.getPathSeparator()+"child1"+
                Config.getPathSeparator()+".."+Config.getPathSeparator()+"child1"+Config.getPathSeparator()+"child11"), child1).get(0));

        assertEquals(child11, PathResolver.resolvePath(new File(".."+Config.getPathSeparator()+
                "child1"+Config.getPathSeparator()+"."+Config.getPathSeparator()+"child11"), child1).get(0));

        assertEquals(child11, PathResolver.resolvePath(new File("."+Config.getPathSeparator()+".."+
                Config.getPathSeparator()+"child1"+Config.getPathSeparator()+"."+Config.getPathSeparator()+"child11"), child1).get(0));

        System.setProperty("user.home", tempDir.toString()+Config.getPathSeparator()+"home");
        assertEquals(new File(Config.getHomeDir()), PathResolver.resolvePath(new File("~/../home"), child1).get(0));

        assertEquals(new File(Config.getHomeDir()), PathResolver.resolvePath(new File("~"), child1).get(0));
        assertEquals(new File(child1, "~a"), PathResolver.resolvePath(new File("~a"), child1).get(0));
    }

    @Test
    public void testWildcards() throws IOException {
        File tmp = tempDir.toFile();
        File child1 = new File(tempDir + Config.getPathSeparator()+"child1");
        File child2 = new File(tempDir + Config.getPathSeparator()+"child2");
        File child3 = new File(tempDir + Config.getPathSeparator()+"child3");

        if(Config.isOSPOSIXCompatible()) {
            Files.createDirectory(child1.toPath(), fileAttribute).toFile().deleteOnExit();
            Files.createDirectory(child2.toPath(), fileAttribute).toFile().deleteOnExit();
            Files.createDirectory(child3.toPath(), fileAttribute).toFile().deleteOnExit();
        }
        else {
            Files.createDirectory(child1.toPath()).toFile().deleteOnExit();
            Files.createDirectory(child2.toPath()).toFile().deleteOnExit();
            Files.createDirectory(child3.toPath()).toFile().deleteOnExit();
        }

        List<File> files = PathResolver.resolvePath(new File("*"), tmp);

        assertEquals(1, files.size());
    }

    @Test
    public void testSearchFiles() throws IOException {
        File child1 = new File(tempDir + Config.getPathSeparator()+"child1");
        File child2 = new File(tempDir + Config.getPathSeparator()+"child2");
        File child11 = new File(child1 + Config.getPathSeparator()+"child11");
        File child111 = new File(child11 + Config.getPathSeparator()+"child111");
        File child12 = new File(child1 + Config.getPathSeparator()+"child12");
        File child21 = new File(child2 + Config.getPathSeparator()+"child21");
        File child22 = new File(child2 + Config.getPathSeparator()+"child22");

        if(Config.isOSPOSIXCompatible()) {
            Files.createDirectory(child1.toPath(), fileAttribute).toFile().deleteOnExit();
            Files.createDirectory(child2.toPath(), fileAttribute).toFile().deleteOnExit();
            Files.createDirectory(child11.toPath(), fileAttribute).toFile().deleteOnExit();
            Files.createDirectory(child12.toPath(), fileAttribute).toFile().deleteOnExit();
            Files.createDirectory(child21.toPath(), fileAttribute).toFile().deleteOnExit();
            Files.createDirectory(child22.toPath(), fileAttribute).toFile().deleteOnExit();
            Files.createDirectory(child111.toPath(), fileAttribute).toFile().deleteOnExit();
        }
        else {
            Files.createDirectory(child1.toPath()).toFile().deleteOnExit();
            Files.createDirectory(child2.toPath()).toFile().deleteOnExit();
            Files.createDirectory(child11.toPath()).toFile().deleteOnExit();
            Files.createDirectory(child12.toPath()).toFile().deleteOnExit();
            Files.createDirectory(child21.toPath()).toFile().deleteOnExit();
            Files.createDirectory(child22.toPath()).toFile().deleteOnExit();
            Files.createDirectory(child111.toPath()).toFile().deleteOnExit();
        }

        ArrayList<File> expected = new ArrayList<>();
        expected.add(child11);
        expected.add(child12);

        //test1
        List<File> actual = PathResolver.resolvePath(
                new File(child1.getAbsolutePath() + Config.getPathSeparator() + "*"),
                new File(Config.getPathSeparator()));

        assertEquals(1, actual.size());
        //for(File f : expected)
        //    assertTrue(actual.contains(f));

        //test2
        actual = PathResolver.resolvePath(
                new File(child1.getAbsolutePath()+Config.getPathSeparator()+"child*"),
                new File(Config.getPathSeparator()));

        assertEquals(expected.size(), actual.size());
        for(File f : expected)
            assertTrue( actual.contains(f));

        //test3
        actual = PathResolver.resolvePath(
                new File(tempDir.toFile().getName()+Config.getPathSeparator()+"child*"),
                new File(Config.getTmpDir()));

        assertEquals(expected.size(), actual.size());
        assertTrue( actual.contains(child1));
        assertTrue( actual.contains(child2));

        //test4
        actual = PathResolver.resolvePath(
                new File(child1.getAbsolutePath()+Config.getPathSeparator()+"child*"+Config.getPathSeparator()+"child111"),
                new File(Config.getPathSeparator()));

        assertEquals(1, actual.size());
        assertTrue(actual.contains(child111));
    }


    @Test
    public void testResolveWithWindowsRootPath() throws IOException {
        Resource root = new FileResource("C:\\users\\me");
        List<Resource> restult = root.resolve(root);
        assertEquals(1, restult.size());
        assertEquals(root.getAbsolutePath(), restult.get(0).getAbsolutePath());
    }

    /* private testing...
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
    */

    public static Path createTempDirectory() throws IOException {
        final Path tmp;
        if(Config.isOSPOSIXCompatible())
            tmp = Files.createTempDirectory("temp"+Long.toString(System.nanoTime()), fileAttribute);
        else {
            tmp = Files.createTempDirectory("temp"+Long.toString(System.nanoTime()));
        }

        tmp.toFile().deleteOnExit();

        return tmp;
    }

}
