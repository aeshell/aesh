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
package org.aesh.console.redirect;


import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.console.BaseConsoleTest;

import org.aesh.console.settings.Settings;
import org.aesh.console.settings.SettingsBuilder;
import org.aesh.readline.ReadlineConsole;
import org.aesh.tty.TestConnection;
import org.aesh.utils.Config;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.Assert.assertTrue;

/**
 * TODO: Enable this when we have redirection implemented.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
 public class ConsoleRedirectionTest extends BaseConsoleTest {

    private Path tempDir;
    private static FileAttribute fileAttribute = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-x---"));

    static boolean beforeHasBeenCalled = false;
    static boolean afterHasBeenCalled = false;

    @Before
    public void before() throws IOException {
        tempDir = createTempDirectory();
    }

     @Test
     public void endOperator() throws Throwable {
         TestConnection connection = new TestConnection();

         CommandRegistry registry = new AeshCommandRegistryBuilder()
                 .command(BeforeCommand.class)
                 .command(AfterCommand.class)
                 .create();

         Settings settings = SettingsBuilder.builder()
                 .logging(true)
                 .connection(connection)
                 .commandRegistry(registry)
                 .build();

         ReadlineConsole console = new ReadlineConsole(settings);
         console.start();

         connection.read("before; after"+Config.getLineSeparator());
         Thread.sleep(50);
         assertTrue(afterHasBeenCalled);
         console.stop();
     }

     /*
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
     */

     /*
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
     */

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

    @CommandDefinition(name = "before", description = "")
    public class BeforeCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            beforeHasBeenCalled = true;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "after", description = "")
    public class AfterCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            assertTrue(beforeHasBeenCalled);
            afterHasBeenCalled = true;
            return CommandResult.SUCCESS;
        }
    }

}