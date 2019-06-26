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
package org.aesh.command.export;

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
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.readline.editing.EditMode;
import org.aesh.readline.terminal.Key;
import org.aesh.terminal.utils.Config;
import org.aesh.readline.ReadlineConsole;
import org.aesh.tty.TestConnection;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ExportCommandTest {

    private final Key completeChar =  Key.CTRL_I;
    private final Key backSpace =  Key.BACKSPACE;

    @Test
    public void testExportCompletionAndCommand() throws IOException, CommandRegistryException, InterruptedException {

        TestConnection connection = new TestConnection();

        CommandRegistry registry =
                AeshCommandRegistryBuilder.builder()
                .command(FooCommand.class)
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .connection(connection)
                        .commandRegistry(registry)
                        .setPersistExport(false)
                        .mode(EditMode.Mode.EMACS)
                        .readInputrc(false)
                        .logging(true)
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("exp");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("export ");
        //outputStream.flush();


        connection.read("FOO=/tmp"+ Config.getLineSeparator());
        connection.clearOutputBuffer();
        /*
        connection.read("export ");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("export FOO=");
        */

        connection.read("export BAR=$F");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("export BAR=$FOO ");

        connection.read(backSpace.getFirstValue());
        connection.read(":/opt"+Config.getLineSeparator());

        connection.clearOutputBuffer();
        connection.read("$B");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("$BAR ");
        //assertTrue(byteArrayOutputStream.toString().contains("BAR=/tmp:/opt"));

        connection.clearOutputBuffer();
        connection.read("$");
        connection.read(completeChar.getFirstValue());
        //assertTrue(byteArrayOutputStream.toString().contains("$FOO"));
        //assertTrue(byteArrayOutputStream.toString().contains("$BAR"));

        connection.read("B");
        connection.read(completeChar.getFirstValue());

        //outputStream.flush();
        //assertEquals("$BAR ", ((AeshConsoleImpl) console).getBuffer());

        connection.read(Config.getLineSeparator());
        //outputStream.flush();

        connection.read("foo"+Config.getLineSeparator());

        //assertTrue(byteArrayOutputStream.toString().contains("/tmp:/opt"));

        console.stop();
    }

     @Test
    public void testExportListener() throws IOException, InterruptedException {

         final boolean[] listenerCalled = {false};
         ExportChangeListener listener = (name, value) -> {
             assertEquals("FOO", name);
             assertEquals("bar", value);
             listenerCalled[0] = true;
         };

         TestConnection connection = new TestConnection();

         CommandRegistry registry = AeshCommandRegistryBuilder.builder().create();

         Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                         OptionActivator, CommandActivator> settings =
                 SettingsBuilder.builder()
                         .connection(connection)
                         .commandRegistry(registry)
                         .setPersistExport(false)
                         .mode(EditMode.Mode.EMACS)
                         .readInputrc(false)
                         .logging(true)
                         .exportListener(listener)
                         .build();

         ReadlineConsole console = new ReadlineConsole(settings);
         console.start();
         connection.read("export FOO=bar"+Config.getLineSeparator());
         Thread.sleep(50);
         assertTrue(listenerCalled[0]);
         console.stop();
     }

     @CommandDefinition(name = "foo", description = "")
     public static class FooCommand implements Command {

         @Override
         public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
             assertEquals("/tmp", commandInvocation.getConfiguration().getAeshContext().exportedVariable("FOO"));
             return CommandResult.SUCCESS;
         }
     }

}
