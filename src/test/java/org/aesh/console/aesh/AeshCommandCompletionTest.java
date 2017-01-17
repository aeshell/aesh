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
package org.aesh.console.aesh;

import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.option.Option;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.console.settings.Settings;
import org.aesh.console.settings.SettingsBuilder;
import org.aesh.command.option.Arguments;
import org.aesh.command.CommandDefinition;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.CommandResult;
import org.aesh.readline.Prompt;
import org.aesh.readline.ReadlineConsole;
import org.aesh.terminal.Key;
import org.aesh.tty.Size;
import org.aesh.tty.TestConnection;
import org.aesh.util.Config;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import org.aesh.command.activator.CommandActivator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandCompletionTest {

    private final Key completeChar =  Key.CTRL_I;
    private final Key enter =  Key.ENTER;

    @Test
    public void testCompletion() throws Exception {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(FooCommand.class)
                .create();

        Settings settings = SettingsBuilder.builder()
                .logging(true)
                .connection(connection)
                .commandRegistry(registry)
                .build();


        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        connection.read("fo");
        connection.read(completeChar.getFirstValue());

        assertEquals("foo ", connection.getOutputBuffer());

        connection.read("--name aslak --bar ");
        connection.read(completeChar.getFirstValue());

        assertEquals("foo --name aslak --bar bar\\ 2", connection.getOutputBuffer());

        connection.read(completeChar.getFirstValue());

        assertEquals("foo --name aslak --bar bar\\ 2\\ 3\\ 4 ", connection.getOutputBuffer());

        connection.read(Config.getLineSeparator());
        connection.clearOutputBuffer();

        connection.read("foo --bar bar\\ 2\\ ");
        connection.read(completeChar.getFirstValue());

        assertEquals("foo --bar bar\\ 2\\ 3\\ 4 ", connection.getOutputBuffer());

        connection.read(Config.getLineSeparator());
        connection.clearOutputBuffer();

        connection.read("foo --bar bar");
        connection.read(completeChar.getFirstValue());

        assertEquals("foo --bar bar\\ 2 ", connection.getOutputBuffer());

        connection.read(Config.getLineSeparator());
        connection.clearOutputBuffer();

        connection.read("foo --bar foo ");
        connection.read(completeChar.getFirstValue());

        assertEquals("foo --bar foo ", connection.getOutputBuffer());
        connection.read("--b");
        connection.read(completeChar.getFirstValue());

        assertEquals("foo --bar foo --b", connection.getOutputBuffer());

        console.stop();
    }

    @Test
    public void testCommandActivator() throws Exception {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(TotoCommand.class)
                .create();

        Settings settings = SettingsBuilder.builder()
                .connection(connection)
                //.inputStream(pipedInputStream)
                //.outputStream(new PrintStream(byteArrayOutputStream))
                .logging(true)
                .commandRegistry(registry)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        TestCommandActivator.activated = true;
        connection.read("hi");
        connection.read(completeChar.getFirstValue());

        assertEquals("hidden ", connection.getOutputBuffer());

        connection.read(enter.getFirstValue());
        connection.clearOutputBuffer();

        TestCommandActivator.activated = false;
        connection.read("hi");
        connection.read(completeChar.getFirstValue());

        assertEquals("hi", connection.getOutputBuffer());

        console.stop();
    }

    @Test
    public void testCompletionNoArguments() {

    }

    @Test
    public void testRequiredAndActivatorOption() throws IOException, InterruptedException {
        TestConnection connection = new TestConnection(new Size(200,20));

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ArqCommand.class)
                .create();

        Settings settings = SettingsBuilder.builder()
                .logging(true)
                .connection(connection)
                .commandRegistry(registry)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        connection.read("arquillian-container-configuration --container arquillian-tomcat-embedded-7 --containerOption ");
        connection.read(completeChar.getFirstValue());

        Thread.sleep(80);
        assertEquals("arquillian-container-configuration --container arquillian-tomcat-embedded-7 --containerOption managed ",
                connection.getOutputBuffer());

    }

    @Test
    public void testGroupCommand() throws IOException, InterruptedException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(GitCommand.class)
                .create();

         Settings settings = SettingsBuilder.builder()
                 .logging(true)
                 .connection(connection)
                 .commandRegistry(registry)
                 .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("git --");
        connection.read(completeChar.getFirstValue());

        Thread.sleep(80);
        connection.assertBuffer("git --help ");
        connection.read(enter.getFirstValue());
        connection.clearOutputBuffer();

        connection.read("git rebase --");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("git rebase --force ");

        connection.read("--");
        connection.read(completeChar.getFirstValue());
        //outputStream.flush();

        Thread.sleep(80);
        connection.assertBuffer("git rebase --force --test ");
        connection.read(enter.getFirstValue());
        connection.clearOutputBuffer();

        connection.read("git rebase --fo");
        connection.read(completeChar.getFirstValue());

        connection.assertBuffer("git rebase --force ");

        connection.read("--test bar");
        connection.read(completeChar.getFirstValue());

        connection.assertBuffer("git rebase --force --test barFOO ");

        console.stop();
     }

    @CommandDefinition(name = "foo", description = "")
    public static class FooCommand implements Command {

        @Option(completer = FooCompletor.class)
        private String bar;

        @Option
        private String name;

        @Arguments
        private List<String> arguments;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }

        public String getName() {
            return name;
        }
    }

    private static class TestCommandActivator implements CommandActivator {

        static boolean activated;
        @Override
        public boolean isActivated(ProcessedCommand cmd) {
            return activated;
        }
    }

    @CommandDefinition(name = "hidden", description = "", activator = TestCommandActivator.class)
    public static class TotoCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "bar", description = "")
    public static class BarCommand implements Command {

        @Option(completer = FooCompletor.class)
        private String bar;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    public static class FooCompletor implements OptionCompleter {

        @Override
        public void complete(CompleterInvocation completerData) {
            if(completerData.getGivenCompleteValue() == null || completerData.getGivenCompleteValue().length() == 0) {
                if(((FooCommand) completerData.getCommand()).getName().equals("aslak")) {
                    completerData.addCompleterValue("bar 2");
                    completerData.setAppendSpace(false);
                }
            }
            else if(completerData.getGivenCompleteValue().equals("bar 2")) {
                if(((FooCommand) completerData.getCommand()).getName().equals("aslak"))
                    completerData.addCompleterValue("bar 2 3 4");
            }
            else if(completerData.getGivenCompleteValue().equals("bar 2 ")) {
                completerData.addCompleterValue("bar 2 3 4");
            }
            else if(completerData.getGivenCompleteValue().equals("bar")) {
                completerData.addCompleterValue("bar 2");
            }
        }
    }


    @CommandDefinition(name = "arquillian-container-configuration", description = "")
    public static class ArqCommand implements Command {

        @Option(required = true)
        private String container;

        @Option(required = true, activator = ContainerActivator.class, completer = ContainerOptionCompleter.class)
        private String containerOption;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return null;
        }
    }

    public static class ContainerActivator implements OptionActivator {

        @Override
        public boolean isActivated(ProcessedCommand processedCommand) {
            ProcessedOption container = processedCommand.findLongOption("container");
            return container != null && container.getValue() != null;
        }
    }

    public static class ContainerOptionCompleter implements OptionCompleter {

        @Override
        public void complete(CompleterInvocation completerInvocation) {
            if(completerInvocation.getGivenCompleteValue() == null ||
                    completerInvocation.getGivenCompleteValue() == "")
            completerInvocation.addCompleterValue("managed");
        }
    }

    @GroupCommandDefinition(name = "git", description = "", groupCommands = {GitCommit.class, GitRebase.class})
    public static class GitCommand implements Command {

        @Option(hasValue = false)
        private boolean help;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "commit", description = "")
    public static class GitCommit implements Command {

        @Option(shortName = 'a', hasValue = false)
        private boolean all;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "rebase", description = "")
    public static class GitRebase implements Command {

        @Option(hasValue = false)
        private boolean force;

        @Option(completer = RebaseTestCompleter.class, activator = RebaseTestActivator.class)
        private String test;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            assertTrue(force);
            return CommandResult.SUCCESS;
        }

        public class RebaseTestCompleter implements OptionCompleter {

            @Override
            public void complete(CompleterInvocation completerInvocation) {
                assertEquals(true, ((GitRebase) completerInvocation.getCommand()).force);
                completerInvocation.addCompleterValue("barFOO");
            }
        }
    }

    public static class RebaseTestActivator implements OptionActivator {
        @Override
        public boolean isActivated(ProcessedCommand processedCommand) {
            ProcessedOption option = processedCommand.findLongOption("force");
            return option != null && option.getValue() != null && option.getValue().equals("true");
        }
    }
}
