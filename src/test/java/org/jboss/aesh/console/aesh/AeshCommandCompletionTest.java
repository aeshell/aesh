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
package org.jboss.aesh.console.aesh;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.console.command.completer.CompleterInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.readline.Prompt;
import org.jboss.aesh.readline.ReadlineConsole;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.tty.Size;
import org.jboss.aesh.tty.TestConnection;
import org.jboss.aesh.util.Config;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.List;
import org.jboss.aesh.cl.activation.CommandActivator;
import org.jboss.aesh.console.command.CommandException;

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

        Settings settings = new SettingsBuilder()
                .logging(true)
                .connection(connection)
                .commandRegistry(registry)
                .create();


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
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        TestConnection connection = new TestConnection();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(TotoCommand.class)
                .create();

        Settings settings = new SettingsBuilder()
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .logging(true)
                .commandRegistry(registry)
                .create();

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

        Settings settings = new SettingsBuilder()
                .logging(true)
                .connection(connection)
                .commandRegistry(registry)
                .create();

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

         Settings settings = new SettingsBuilder()
                 .logging(true)
                 .connection(connection)
                 .commandRegistry(registry)
                 .create();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("git --");
        connection.read(completeChar.getFirstValue());

        Thread.sleep(80);
        assertEquals("git --help ", connection.getOutputBuffer());
        connection.read(enter.getFirstValue());
        connection.clearOutputBuffer();

        connection.read("git rebase --");
        connection.read(completeChar.getFirstValue());

        assertEquals("git rebase --force ", connection.getOutputBuffer());

        connection.read("--");
        connection.read(completeChar.getFirstValue());
        //outputStream.flush();

        Thread.sleep(80);
        assertEquals("git rebase --force --test ", connection.getOutputBuffer());
        connection.read(enter.getFirstValue());

        connection.read("git rebase --fo");
        connection.read(completeChar.getFirstValue());

        assertEquals("git rebase --force ", connection.getOutputBuffer());

        connection.read("--test bar");
        connection.read(completeChar.getFirstValue());

        assertEquals("git rebase --force --test barFOO ", connection.getOutputBuffer());

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
