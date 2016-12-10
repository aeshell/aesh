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
package org.jboss.aesh.console.redirect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;

import org.jboss.aesh.console.BaseConsoleTest;

import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.operator.ControlOperator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
  * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
  */
@Ignore
 public class ConsoleRedirectionTest extends BaseConsoleTest {

     /*
    private Path tempDir;
    private static FileAttribute fileAttribute = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-x---"));

    @Before
    public void before() throws IOException {
        tempDir = createTempDirectory();
    }

     @Test
     public void pipeCommands() throws Throwable {
         invokeTestConsole(new Setup() {
             @Override
             public void call(Console console, OutputStream out) throws IOException {
                 out.write(("ls | find *. -print" + Config.getLineSeparator()).getBytes());
             }
         }, new RedirectionConsoleCallback());
     }

     @Test
     public void redirectIn() throws Throwable {
         final File foo = new File(tempDir.toFile()+Config.getPathSeparator()+"foo_bar2.txt");
         PrintWriter writer = new PrintWriter(foo, "UTF-8");
         writer.print("foo bar");
         writer.close();
         invokeTestConsole(
                 new Setup() {
                     @Override
                     public void call(Console console, OutputStream out) throws IOException {
                         out.write(("ls < " + foo.getCanonicalPath()+Config.getLineSeparator()).getBytes());
                         out.flush();
                     }
                 }, new Verify() {
                     @Override
                     public int call(Console console, ConsoleOperation op) {
                         assertEquals("ls ", op.getBuffer());
                         try {
                             assertTrue(console.getShell().in().getStdIn() != null);
                             assertTrue(console.getShell().in().getStdIn().available() > 0);
                         }
                         catch (IOException e) {
                             fail();
                         }
                         assertEquals(ControlOperator.NONE, op.getControlOperator());
                         java.util.Scanner s = new java.util.Scanner(console.getShell().in().getStdIn()).useDelimiter("\\A");
                         String fileContent = s.hasNext() ? s.next() : "";
                         assertEquals("foo bar", fileContent);
                         return 0;
                     }
                 }
         );
     }

     @Test
     public void redirectIn2() throws Throwable {
         final File foo = new File(tempDir.toFile()+Config.getPathSeparator()+"foo_bar3.txt");
         PrintWriter writer = new PrintWriter(foo, "UTF-8");
         writer.print("foo bar");
         writer.close();
         invokeTestConsole(2, new Setup() {
                     @Override
                     public void call(Console console, OutputStream out) throws IOException {
                         out.write(("ls < " + foo.getCanonicalPath()+" | man" + Config.getLineSeparator()).getBytes());
                         out.flush();
                     }
                 }, new Verify() {
                     private int count = 0;

                     @Override
                     public int call(Console console, ConsoleOperation op) {
                         if (count == 0) {
                             assertEquals("ls ", op.getBuffer());
                             try {
                                 assertTrue(console.getShell().in().getStdIn().available() > 0);
                             }
                             catch (IOException e) {
                                 fail();
                             }
                             assertEquals(ControlOperator.PIPE, op.getControlOperator());
                             java.util.Scanner s = new java.util.Scanner(console.getShell().in().getStdIn()).useDelimiter("\\A");
                             String fileContent = s.hasNext() ? s.next() : "";
                             assertEquals("foo bar", fileContent);
                         }
                         else if (count == 1) {
                             assertEquals(" man", op.getBuffer());
                             assertEquals(ControlOperator.NONE, op.getControlOperator());
                         }
                         count++;
                         return 0;
                     }
                 }
         );
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

     class RedirectionConsoleCallback implements Verify {
         private int count = 0;

         @Override
         public int call(Console console, ConsoleOperation output) {
             if(count == 0) {
                 assertEquals("ls ", output.getBuffer());
                 count++;
             }
             else if(count == 1) {
                 assertEquals(" find *. -print", output.getBuffer());
             }
             return 0;
         }
     }
     */

}