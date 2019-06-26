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
package org.aesh.command.operator;


import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.registry.CommandRegistry;

import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.io.Resource;
import org.aesh.readline.ReadlineConsole;
import org.aesh.tty.TestConnection;
import org.aesh.terminal.utils.Config;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * TODO: Enable this when we have redirection implemented.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
 public class ConsoleRedirectionTest {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();
    //private static FileAttribute fileAttribute = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-x---"));

    private static boolean beforeHasBeenCalled = false;
    private static boolean afterHasBeenCalled = false;

     @Test
     public void endOperator() throws Throwable {
         TestConnection connection = new TestConnection();

         CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                 .command(BeforeCommand.class)
                 .command(AfterCommand.class)
                 .create();

         Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                         OptionActivator, CommandActivator> settings =
                 SettingsBuilder.builder()
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

     @Test
     public void redirectOutOperator() throws Throwable {
         TestConnection connection = new TestConnection();

         CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                 .command(FooCommand.class)
                 .create();

         Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                         OptionActivator, CommandActivator> settings =
                 SettingsBuilder.builder()
                         .logging(true)
                         .connection(connection)
                         .commandRegistry(registry)
                         .build();

         final File foo = new File(tempDir.getRoot()+Config.getPathSeparator()+"foo_redirection.txt");
         ReadlineConsole console = new ReadlineConsole(settings);
         console.start();

         connection.read("foo > "+foo.getCanonicalPath()+Config.getLineSeparator());
         Thread.sleep(50);
         console.stop();

         //lets make sure that foo has been read
         List<String> input = Files.readAllLines(foo.toPath());
         assertEquals("some", input.get(0));
         assertEquals("text", input.get(1));

         Files.delete(foo.toPath());
     }

     @Test
     public void pipeRedirectOutOperator() throws Throwable {
         TestConnection connection = new TestConnection();

         CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                 .command(DisplayCommand.class)
                 .command(PrintCommand.class)
                 .create();

         Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                         OptionActivator, CommandActivator> settings =
                 SettingsBuilder.builder()
                         .logging(true)
                         .connection(connection)
                         .commandRegistry(registry)
                         .build();

         final File foo = new File(tempDir.getRoot()+Config.getPathSeparator()+"foo_redirection_out.txt");
         ReadlineConsole console = new ReadlineConsole(settings);
         console.start();

         connection.read("display hello_aesh" + " | print > " + foo.getCanonicalPath() +Config.getLineSeparator() );
         Thread.sleep(50);

         console.stop();

         //lets make sure that foo has been read
         List<String> input = Files.readAllLines(foo.toPath());
         assertEquals("hello_aesh", input.get(0));

         Files.delete(foo.toPath());
     }

     @Test
     public void redirectInOperator() throws Throwable {
         TestConnection connection = new TestConnection();

         CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                 .command(BarCommand.class)
                 .create();

         Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                         OptionActivator, CommandActivator> settings =
                 SettingsBuilder.builder()
                         .logging(true)
                         .connection(connection)
                         .commandRegistry(registry)
                         .build();

         final File foo = new File(tempDir.getRoot()+Config.getPathSeparator()+"foo_redirection_in.txt");
         PrintWriter writer = new PrintWriter(foo, "UTF-8");
         writer.print("foo bar");
         writer.close();

         ReadlineConsole console = new ReadlineConsole(settings);
         console.start();

         connection.read("bar < "+foo.getCanonicalPath()+Config.getLineSeparator());
         Thread.sleep(50);

         connection.assertBufferEndsWith("foo bar"+Config.getLineSeparator());

         console.stop();
     }

     @Test
     public void redirectInAndPipeOperator() throws Throwable {
         TestConnection connection = new TestConnection();

         CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                 .command(BarCommand.class)
                 .command(ManCommand.class)
                 .create();

         Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                         OptionActivator, CommandActivator> settings =
                 SettingsBuilder.builder()
                         .logging(true)
                         .connection(connection)
                         .commandRegistry(registry)
                         .build();

         final File foo = new File(tempDir.getRoot()+Config.getPathSeparator()+"foo_redirection_in.txt");
         PrintWriter writer = new PrintWriter(foo, "UTF-8");
         writer.print("FOO BAR");
         writer.close();

         ReadlineConsole console = new ReadlineConsole(settings);
         console.start();

         connection.read("bar < "+foo.getCanonicalPath()+" | man"+Config.getLineSeparator() );
         Thread.sleep(50);

         connection.assertBufferEndsWith("FOO BAR"+Config.getLineSeparator());

         console.stop();
     }

     @Test
     public void redirectInPipeAndRedirectOutOperator() throws Throwable {
         TestConnection connection = new TestConnection();

         CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                 .command(BarCommand.class)
                 .command(ManCommand.class)
                 .create();

         Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                         OptionActivator, CommandActivator> settings =
                 SettingsBuilder.builder()
                         .logging(true)
                         .connection(connection)
                         .commandRegistry(registry)
                         .build();

         final File fooOut = new File(tempDir.getRoot()+Config.getPathSeparator()+"foo_redirection_out.txt");
         final File foo = new File(tempDir.getRoot()+Config.getPathSeparator()+"foo_redirection_in.txt");
         PrintWriter writer = new PrintWriter(foo, "UTF-8");
         writer.print("FOO BAR");
         writer.close();

         ReadlineConsole console = new ReadlineConsole(settings);
         console.start();

         connection.read("bar < "+foo.getCanonicalPath()+" | man > "+ fooOut.getCanonicalPath() + Config.getLineSeparator() );
         Thread.sleep(50);

         console.stop();

         //lets make sure that foo has been read
         List<String> output = Files.readAllLines(fooOut.toPath());
         assertEquals("FOO BAR", output.get(0));
     }

     @Test
     public void redirectInAndRedirectOutOperator() throws Throwable {
         TestConnection connection = new TestConnection();

         CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                 .command(BarCommand.class)
                 .create();

         Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                         OptionActivator, CommandActivator> settings =
                 SettingsBuilder.builder()
                         .logging(true)
                         .connection(connection)
                         .commandRegistry(registry)
                         .build();
         final File fooOut = new File(tempDir.getRoot()+Config.getPathSeparator()+"foo_redirection_out.txt");
         final File foo = new File(tempDir.getRoot()+Config.getPathSeparator()+"foo_redirection_in.txt");
         PrintWriter writer = new PrintWriter(foo, "UTF-8");
         writer.print("FOO BAR");
         writer.close();

         ReadlineConsole console = new ReadlineConsole(settings);
         console.start();

         connection.read("bar < "+foo.getCanonicalPath()+" > "+fooOut.getCanonicalPath()+Config.getLineSeparator() );
         Thread.sleep(50);

         console.stop();

         //lets make sure that foo has been read
         List<String> output = Files.readAllLines(fooOut.toPath());
         assertEquals("FOO BAR", output.get(0));
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
     */

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

    @CommandDefinition(name = "foo", description = "")
    public class FooCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.println("some");
            commandInvocation.print("text");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "bar", description = "")
    public class BarCommand implements Command {

        @Argument
        private Resource arg;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            if(arg != null) {
                try {
                    String result = new BufferedReader(new InputStreamReader(arg.read()))
                            .lines().collect(Collectors.joining("\n"));

                    commandInvocation.println(result);
                    return CommandResult.SUCCESS;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            return CommandResult.FAILURE;
        }
    }

    @CommandDefinition(name = "man", description = "")
    public class ManCommand implements Command {

        @Argument
        private Resource arg;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            if(arg != null) {
                try {
                    String result = new BufferedReader(new InputStreamReader(arg.read()))
                            .lines().collect(Collectors.joining("\n"));

                    commandInvocation.println(result);
                    return CommandResult.SUCCESS;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            return CommandResult.FAILURE;
        }
    }

     @CommandDefinition(name = "print", description = "")
     public static class PrintCommand implements Command {

         @Override
         public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
             BufferedReader reader = new BufferedReader(new InputStreamReader(commandInvocation.getConfiguration().getPipedData()));
             reader.lines().forEach(commandInvocation::print);
             return CommandResult.SUCCESS;
         }
     }

     @CommandDefinition(name = "display", description = "")
     public class DisplayCommand implements Command {

         @Argument
         private String arg;

         @Override
         public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
             if (arg != null) {
                 commandInvocation.println(arg);
                 return CommandResult.SUCCESS;
             }
             return CommandResult.FAILURE;
         }
     }

}