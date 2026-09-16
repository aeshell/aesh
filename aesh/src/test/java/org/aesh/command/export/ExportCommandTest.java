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
package org.aesh.command.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Arguments;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.ReadlineConsole;
import org.aesh.readline.editing.EditMode;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.Key;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * @author Aesh team
 */
public class ExportCommandTest {

    private final Key completeChar = Key.CTRL_I;
    private final Key backSpace = Key.BACKSPACE;

    @Test
    public void testExportCompletionAndCommand() throws IOException, CommandRegistryException, InterruptedException {

        TestConnection connection = new TestConnection();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(FooCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder
                .builder()
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

        connection.read("FOO=/tmp" + Config.getLineSeparator());
        connection.clearOutputBuffer();
        /*
         * connection.read("export ");
         * connection.read(completeChar.getFirstValue());
         * connection.assertBuffer("export FOO=");
         */

        connection.read("export BAR=$F");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("export BAR=$FOO ");

        connection.read(backSpace.getFirstValue());
        connection.read(":/opt" + Config.getLineSeparator());

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

        connection.read("foo" + Config.getLineSeparator());

        //assertTrue(byteArrayOutputStream.toString().contains("/tmp:/opt"));

        console.stop();
    }

    @Test
    public void testExportListener() throws IOException, InterruptedException {

        final boolean[] listenerCalled = { false };
        ExportChangeListener listener = (name, value) -> {
            assertEquals("FOO", name);
            assertEquals("bar", value);
            listenerCalled[0] = true;
        };

        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder().create();

        Settings<CommandInvocation> settings = SettingsBuilder
                .builder()
                .connection(connection)
                .commandRegistry(registry)
                .setPersistExport(false)
                .mode(EditMode.Mode.EMACS)
                .readInputrc(false)
                .logging(true)
                .exportListener(listener)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();
        connection.read("export FOO=bar" + Config.getLineSeparator());
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        assertTrue(listenerCalled[0]);
        console.stop();
    }

    @Test
    public void testUnknownVariablesExpandToEmptyEndToEnd() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(3);
        List<String> seenArgs = Collections.synchronizedList(new ArrayList<>());

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(new ArgsCommand(seenArgs))
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder
                .builder()
                .connection(connection)
                .commandRegistry(registry)
                .setPersistExport(false)
                .mode(EditMode.Mode.EMACS)
                .readInputrc(false)
                .logging(true)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("export FOO=bar" + Config.getLineSeparator());
        // Known var expands, unknown $NOPE vanishes but the line survives
        connection.read("args $FOO $NOPE tail" + Config.getLineSeparator());
        // The user's shape: unset vars empty out instead of killing the line
        connection.read("args $mydata = $1" + Config.getLineSeparator());
        assertTrue("Commands should complete", latch.await(10, TimeUnit.SECONDS));
        assertEquals(Arrays.asList("bar", "tail"), seenArgs.subList(0, 2));
        assertEquals(Collections.singletonList("="), seenArgs.subList(2, 3));
        console.stop();
    }

    @Test
    public void testExitCodeExpansionEndToEnd() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(2);
        List<String> seenArgs = Collections.synchronizedList(new ArrayList<>());

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(new ArgsCommand(seenArgs))
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder
                .builder()
                .connection(connection)
                .commandRegistry(registry)
                .setPersistExport(false)
                .mode(EditMode.Mode.EMACS)
                .readInputrc(false)
                .logging(true)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        // ArgsCommand always returns 1, so $? must expand to "1"
        connection.read("args" + Config.getLineSeparator());
        connection.read("args $? done" + Config.getLineSeparator());
        assertTrue("Commands should complete", latch.await(10, TimeUnit.SECONDS));
        assertTrue("Exit code should expand to 1, got: " + seenArgs,
                seenArgs.contains("1"));
        console.stop();
    }

    @CommandDefinition(name = "args", description = "")
    public static class ArgsCommand implements Command {

        private final List<String> seenArgs;

        ArgsCommand(List<String> seenArgs) {
            this.seenArgs = seenArgs;
        }

        @Arguments(description = "")
        private List<String> args;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {
            if (args != null)
                seenArgs.addAll(args);
            // Always non-zero so a following invocation observes it via $?
            return CommandResult.valueOf(1);
        }
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
