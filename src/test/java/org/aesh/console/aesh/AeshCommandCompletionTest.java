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
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.parser.CommandLineParserException;
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
import org.aesh.readline.terminal.Key;
import org.aesh.terminal.tty.Size;
import org.aesh.tty.TestConnection;
import org.aesh.utils.Config;
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
    private final Key backspace =  Key.BACKSPACE;
    private final Key enter =  Key.ENTER;

    @Test
    public void testCompletion() throws Exception {
        TestConnection connection = new TestConnection(false);

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

        connection.clearOutputBuffer();
        connection.read("fo");
        connection.read(completeChar.getFirstValue());
        assertEquals("foo ", connection.getOutputBuffer());

        connection.read("-");
        connection.read(completeChar.getFirstValue());
        assertEquals("foo --", connection.getOutputBuffer());

        connection.read(completeChar.getFirstValue());
        assertEquals("foo --\n--bar=  --bool  --name=  \nfoo --", connection.getOutputBuffer());

        connection.clearOutputBuffer();
        connection.read("name aslak --bar");
        connection.read(completeChar.getFirstValue());
        assertEquals("name aslak --bar=", connection.getOutputBuffer());
        connection.read(completeChar.getFirstValue());

        assertEquals("name aslak --bar=bar\\ 2", connection.getOutputBuffer());

        connection.read(completeChar.getFirstValue());

        assertEquals("name aslak --bar=bar\\ 2\\ 3\\ 4 ", connection.getOutputBuffer());

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

        connection.read("foo --bar foo --n");
        connection.read(completeChar.getFirstValue());

        assertEquals("foo --bar foo --name=", connection.getOutputBuffer());
        connection.read("val --");
        connection.read(completeChar.getFirstValue());

        assertEquals("foo --bar foo --name=val --bool ", connection.getOutputBuffer());

        connection.read(Config.getLineSeparator());
        connection.clearOutputBuffer();

        connection.read("foo");
        connection.read(completeChar.getFirstValue());
        assertEquals("foo ", connection.getOutputBuffer());

        connection.read("-n");
        connection.read(completeChar.getFirstValue());
        assertEquals("foo -n", connection.getOutputBuffer());

        connection.read(" ");
        connection.read(completeChar.getFirstValue());
        assertEquals("foo -n two ", connection.getOutputBuffer());

        connection.read(Config.getLineSeparator());
        connection.clearOutputBuffer();

        connection.read("foo -n=");
        connection.read(completeChar.getFirstValue());
        assertEquals("foo -n=two ", connection.getOutputBuffer());
        connection.read(Config.getLineSeparator());
        connection.clearOutputBuffer();

        connection.read("foo --bool");
        connection.read(completeChar.getFirstValue());
        assertEquals("foo --bool ", connection.getOutputBuffer());

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
        // Must sleep, otherwise activated will become false before hidden
        // is executed.
        Thread.sleep(200);
        connection.clearOutputBuffer();

        TestCommandActivator.activated = false;
        connection.read("hi");
        connection.read(completeChar.getFirstValue());

        assertEquals("hi", connection.getOutputBuffer());

        console.stop();
    }

    @Test
    public void testCompletionArgument() throws IOException, CommandLineParserException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ArgCommand.class)
                .command(GroupArgCommand.class)
                .create();

         Settings settings = SettingsBuilder.builder()
                 .logging(true)
                 .connection(connection)
                 .commandRegistry(registry)
                 .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("arg -");
        connection.read(completeChar.getFirstValue());
        assertEquals("arg --", connection.getOutputBuffer());
        connection.read("b");
        connection.read(completeChar.getFirstValue());
        assertEquals("arg --bool=", connection.getOutputBuffer());
        connection.read("t");
        connection.read(completeChar.getFirstValue());
        assertEquals("arg --bool=true ", connection.getOutputBuffer());
        connection.read(completeChar.getFirstValue());
        assertEquals("arg --bool=true ARG ", connection.getOutputBuffer());
        connection.read(backspace);
        connection.clearOutputBuffer();
        connection.read(completeChar.getFirstValue());
        assertEquals(" ", connection.getOutputBuffer());
        connection.read(completeChar.getFirstValue());
        assertEquals(" --input=", connection.getOutputBuffer());
        connection.read("bar ");
        connection.read(completeChar.getFirstValue());
        assertEquals(" --input=bar ", connection.getOutputBuffer());

        connection.read(enter);
        connection.clearOutputBuffer();
        connection.read("arg --input 'foo' --bool=false ");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("arg --input 'foo' --bool=false ARG ");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("arg --input 'foo' --bool=false ARG ");

        connection.read(enter);
        connection.clearOutputBuffer();
        connection.read("arg --input={foo-bar ");
        connection.read(completeChar.getFirstValue());
        assertEquals("arg --input={foo-bar bArg ", connection.getOutputBuffer());

        connection.read(enter);
        connection.clearOutputBuffer();
        connection.read("arg --input={foo-barb");
        connection.read(completeChar.getFirstValue());
        assertEquals("arg --input={foo-barbArg ", connection.getOutputBuffer());

        connection.read(enter);
        connection.clearOutputBuffer();
        connection.read("arg --input={foo-bar b");
        connection.read(completeChar.getFirstValue());
        assertEquals("arg --input={foo-bar bArg ", connection.getOutputBuffer());

        connection.read(enter);
        connection.clearOutputBuffer();
        connection.read("group-arg arg2 --input={foo-bar ");
        connection.read(completeChar.getFirstValue());
        assertEquals("group-arg arg2 --input={foo-bar bArg ", connection.getOutputBuffer());

        connection.read(enter);
        connection.clearOutputBuffer();
        connection.read("arg {foo-bar ");
        connection.read(completeChar.getFirstValue());
        assertEquals("arg {foo-bar YEAH ", connection.getOutputBuffer());

        console.stop();
    }

    @Test
    public void testRequiredAndActivatorOption() throws IOException, InterruptedException, CommandLineParserException {
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
    public void testGroupCommand() throws IOException, InterruptedException, CommandLineParserException {
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

        connection.read("git reb");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("git rebase ");

        connection.read("--");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("git rebase --force ");

        connection.read("--");
        connection.read(completeChar.getFirstValue());
        //outputStream.flush();

        Thread.sleep(80);
        connection.assertBuffer("git rebase --force --test=");
        connection.read(enter.getFirstValue());
        Thread.sleep(80);
        connection.clearOutputBuffer();

        connection.read("git rebase --fo");
        connection.read(completeChar.getFirstValue());

        connection.assertBuffer("git rebase --force ");

        connection.read("--test bar");
        connection.read(completeChar.getFirstValue());

        connection.assertBuffer("git rebase --force --test barFOO ");

        connection.read(Config.getLineSeparator());
        connection.clearOutputBuffer();

        connection.read("git commit ");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("git commit --all ");

        connection.read(Config.getLineSeparator());
        connection.clearOutputBuffer();

        connection.read("gi");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("git ");
        connection.clearOutputBuffer();
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer(Config.getLineSeparator()+"commit  rebase  "+Config.getLineSeparator()+"git ");
        connection.clearOutputBuffer();
        connection.read("commit");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("commit ");

        console.stop();
     }

    @Test
    public void testSuperGroupCommand() throws IOException, InterruptedException, CommandLineParserException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(SuperGitCommand.class)
                .create();

        Settings settings = SettingsBuilder.builder()
                .logging(true)
                .connection(connection)
                .commandRegistry(registry)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();
        connection.read("su");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("super ");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("super git ");
        connection.clearOutputBuffer();
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer(Config.getLineSeparator()+"commit  rebase  "+Config.getLineSeparator()+"super git ");
        connection.clearOutputBuffer();
        connection.read("commit");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("commit ");

        console.stop();
    }

    /**
     * CommandTest4 tests that options should not be completed since it is not activated
     * unless argument is set
     */
     @Test
     public void testCommandTest4() throws IOException, InterruptedException, CommandLineParserException {
         TestConnection connection = new TestConnection();

         CommandRegistry registry = new AeshCommandRegistryBuilder()
                 .command(GitCommand.class)
                 .command(CommandTest4.class)
                 .create();

         Settings settings = SettingsBuilder.builder()
                 .logging(true)
                 .connection(connection)
                 .commandRegistry(registry)
                 .build();

         ReadlineConsole console = new ReadlineConsole(settings);
         console.start();

         connection.read("test");
         connection.read(completeChar.getFirstValue());

         connection.assertBuffer("test ");
         connection.read("--");
         connection.read(completeChar.getFirstValue());
         connection.assertBuffer("test --");
         connection.read(Key.BACKSPACE);
         connection.read(Key.BACKSPACE);
         connection.clearOutputBuffer();
         //argument completion
         connection.read(completeChar.getFirstValue());
         connection.assertBuffer(Config.getLineSeparator()+"one  two  "+Config.getLineSeparator()+"test ");
         connection.clearOutputBuffer();

        //setting argument value
         connection.read("BAR ");
         connection.read(completeChar.getFirstValue());
         connection.assertBuffer("BAR --required=");

         console.stop();
     }

    /**
     * CommandTest4B tests that arguments should not be completed since it is not activated
     * unless option is set
     */
     @Test
     public void testCommandTest4B() throws IOException, InterruptedException, CommandLineParserException {
         TestConnection connection = new TestConnection();

         CommandRegistry registry = new AeshCommandRegistryBuilder()
                 .command(GitCommand.class)
                 .command(CommandTest4B.class)
                 .create();

         Settings settings = SettingsBuilder.builder()
                 .logging(true)
                 .connection(connection)
                 .commandRegistry(registry)
                 .build();

         ReadlineConsole console = new ReadlineConsole(settings);
         console.start();

         connection.read("test");
         connection.read(completeChar.getFirstValue());

         connection.assertBuffer("test ");
         connection.read(completeChar.getFirstValue());
         connection.assertBuffer("test --required=");

         connection.read("BAR ");
         connection.assertBuffer("test --required=BAR ");
         connection.clearOutputBuffer();
         connection.read(completeChar.getFirstValue());
         connection.assertBuffer(Config.getLineSeparator()+"one  two  "+Config.getLineSeparator()+"test --required=BAR ");
         connection.read("three");
         connection.read(completeChar.getFirstValue());
         connection.assertBuffer(Config.getLineSeparator()+"one  two  "+Config.getLineSeparator()+"test --required=BAR three_BAR ");

         console.stop();
     }

    /**
     * CommandTest4B tests that arguments should not be completed since it is not activated
     * unless option is set
     */
     @Test
     public void testCommandTest4C() throws IOException, InterruptedException, CommandLineParserException {
         TestConnection connection = new TestConnection();

         CommandRegistry registry = new AeshCommandRegistryBuilder()
                 .command(GitCommand.class)
                 .command(CommandTest4C.class)
                 .create();

         Settings settings = SettingsBuilder.builder()
                 .logging(true)
                 .connection(connection)
                 .commandRegistry(registry)
                 .build();

         ReadlineConsole console = new ReadlineConsole(settings);
         console.start();

         connection.read("test");
         connection.read(completeChar.getFirstValue());

         connection.assertBuffer("test ");
         connection.read(completeChar.getFirstValue());
         connection.assertBuffer("test --required ");
         connection.clearOutputBuffer();

         connection.read(completeChar.getFirstValue());
         connection.assertBuffer(Config.getLineSeparator()+"one  two  "+Config.getLineSeparator()+"test --required ");
         connection.read("three");
         connection.read(completeChar.getFirstValue());
         connection.assertBuffer(Config.getLineSeparator()+"one  two  "+Config.getLineSeparator()+"test --required three_BAR ");

         console.stop();
     }


     @Test
     public void testCommandTest5() throws IOException, InterruptedException, CommandLineParserException {
         TestConnection connection = new TestConnection();

         CommandRegistry registry = new AeshCommandRegistryBuilder()
                 .command(GitCommand.class)
                 .command(CommandTest5.class)
                 .create();

         Settings settings = SettingsBuilder.builder()
                 .logging(true)
                 .connection(connection)
                 .commandRegistry(registry)
                 .build();

         ReadlineConsole console = new ReadlineConsole(settings);
         console.start();

         connection.read("test");
         connection.read(completeChar.getFirstValue());

         connection.assertBuffer("test ");
         connection.read(completeChar.getFirstValue());
         connection.assertBuffer("test ");

         console.stop();
     }

     @Test
     public void testCommandTest6() throws IOException, InterruptedException, CommandLineParserException {
         TestConnection connection = new TestConnection();

         CommandRegistry registry = new AeshCommandRegistryBuilder()
                 .command(GitCommand.class)
                 .command(CommandTest6.class)
                 .create();

         Settings settings = SettingsBuilder.builder()
                 .logging(true)
                 .connection(connection)
                 .commandRegistry(registry)
                 .build();

         ReadlineConsole console = new ReadlineConsole(settings);
         console.start();

         connection.read("test");
         connection.read(completeChar.getFirstValue());

         connection.assertBuffer("test ");
         connection.read(completeChar.getFirstValue());
         connection.assertBuffer("test --bar");
         connection.clearOutputBuffer();
         connection.read(completeChar.getFirstValue());

         connection.assertBuffer(Config.getLineSeparator()+"--bar  --barbar  "+Config.getLineSeparator()+"test --bar");
         connection.clearOutputBuffer();
         connection.read("b");
         connection.read(completeChar.getFirstValue());
         connection.assertBuffer("bar ");

         console.stop();
     }

     @Test
     public void testCommandTest7() throws IOException, InterruptedException, CommandLineParserException {
         TestConnection connection = new TestConnection();

         CommandRegistry registry = new AeshCommandRegistryBuilder()
                 .command(GitCommand.class)
                 .command(CommandTest7.class)
                 .create();

         Settings settings = SettingsBuilder.builder()
                 .logging(true)
                 .connection(connection)
                 .commandRegistry(registry)
                 .build();

         ReadlineConsole console = new ReadlineConsole(settings);
         console.start();

         connection.read("test --headers={allow-resource-service-restart=true; ");
         connection.read(completeChar.getFirstValue());
         connection.assertBuffer("test --headers={allow-resource-service-restart=true; _FOO ");
         connection.read(completeChar.getFirstValue());
         connection.assertBuffer("test --headers={allow-resource-service-restart=true; _FOO _FOO ");
         connection.read("} ");
         connection.read(completeChar.getFirstValue());
         connection.assertBuffer("test --headers={allow-resource-service-restart=true; _FOO _FOO } --bar=");

         console.stop();
     }

    @Test
    public void testCommandTest8() throws IOException, InterruptedException, CommandLineParserException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(CommandTest8.class)
                .create();

        Settings settings = SettingsBuilder.builder()
                .logging(true)
                .connection(connection)
                .commandRegistry(registry)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("test argvalue ");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("test argvalue --bar=");
        connection.read("FOO ");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("test argvalue --bar=FOO ");
        connection.read(Config.getLineSeparator());
        connection.clearOutputBuffer();
        connection.read("test argvalue1 argvalue2 ");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("test argvalue1 argvalue2 ");

        console.stop();
    }

    @Test
    public void testCommandTest9() throws IOException, InterruptedException, CommandLineParserException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(CommandTest9.class)
                .create();

        Settings settings = SettingsBuilder.builder()
                .logging(true)
                .connection(connection)
                .commandRegistry(registry)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("test argvalue ");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("test argvalue --foo\\ bar=");
        connection.read("F");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("test argvalue --foo\\ bar=FOO ");
        connection.read(Config.getLineSeparator());
        connection.clearOutputBuffer();
        connection.read("test --");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("test --foo\\ bar=");

        console.stop();
    }

    @Test
    public void testCompletionInsideBuffer() throws IOException, CommandLineParserException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ArgCommand.class)
                .command(GitCommand.class)
                .create();

         Settings settings = SettingsBuilder.builder()
                 .logging(true)
                 .connection(connection)
                 .commandRegistry(registry)
                 .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();
        connection.read("arg --input= ");
        connection.read(Key.CTRL_A);
        connection.read(Key.META_f);
        connection.read(Key.BACKSPACE); //buffer should not be:" ar<cursor> --input= "
        connection.clearOutputBuffer();
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("g --input= ");

        connection.read(enter.getFirstValue());
        connection.clearOutputBuffer();

        connection.read("git reb --force ");
        connection.read(Key.CTRL_A);
        connection.read(Key.META_f);
        connection.read(Key.META_f);
        connection.clearOutputBuffer();
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("ase --force ");

        console.stop();
    }

    @Test
    public void testWithEndOperator() throws IOException, CommandLineParserException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ArgCommand.class)
                .command(GitCommand.class)
                .create();

         Settings settings = SettingsBuilder.builder()
                 .logging(true)
                 .connection(connection)
                 .commandRegistry(registry)
                 .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();
        connection.read("arg --input=foo; a");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("arg --input=foo; arg ");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("arg --input=foo; arg ARG ");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("arg --input=foo; arg ARG --");

        console.stop();
    }

    @CommandDefinition(name = "test", description = "")
    public static class CommandTest4 implements Command {

        @Option(shortName = 'r', activator = Test4Activator.class)
        private String required;

        @Argument(completer = Arg4Completer.class)
        private String arg;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    public class Arg4Completer implements OptionCompleter {

        @Override
        public void complete(CompleterInvocation completerInvocation) {
            if(completerInvocation.getGivenCompleteValue() == null ||
                    completerInvocation.getGivenCompleteValue().length() == 0) {
                completerInvocation.addCompleterValue("one");
                completerInvocation.addCompleterValue("two");
            }
            else
                completerInvocation.addCompleterValue(completerInvocation.getGivenCompleteValue()+"_BAR");
        }
    }

    public class Test4Activator implements OptionActivator {

        @Override
        public boolean isActivated(ProcessedCommand processedCommand) {
            //needs an argument to be activated
            return !processedCommand.hasArgumentWithNoValue();
        }
    }

    @CommandDefinition(name = "test", description = "")
    public static class CommandTest4B implements Command {

        @Option(shortName = 'r')
        private String required;

        @Argument(activator = Test4ActivatorB.class, completer = Arg4Completer.class)
        private String arg;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "test", description = "")
    public static class CommandTest4C implements Command {

        @Option(shortName = 'r', hasValue = false)
        private boolean required;

        @Argument(activator = Test4ActivatorB.class, completer = Arg4Completer.class)
        private String arg;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    public class Test4ActivatorB implements OptionActivator {
        @Override
        public boolean isActivated(ProcessedCommand processedCommand) {
            //needs required != null to be activated
            return (processedCommand.findLongOption("required").getValue() != null);
        }
    }

    @CommandDefinition(name = "test", description = "")
    public static class CommandTest5 implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "test", description = "")
    public static class CommandTest6 implements Command {

        @Option(hasValue = false)
        private boolean bar;

        @Option(hasValue = false)
        private boolean barbar;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "test", description = "")
    public static class CommandTest7 implements Command {

        @Option
        private String bar;

        @Option(completer = HeaderCompleter.class)
        private String headers;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "test", description = "")
    public static class CommandTest8 implements Command {

        @Option
        private String bar;

        @Argument
        private String arg;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "test", description = "")
    public static class CommandTest9 implements Command {

        @Option(name = "foo\\ bar", completer = FooBarCompleter.class)
        private String bar;

        @Argument
        private String arg;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    public static class FooBarCompleter implements OptionCompleter {

        @Override
        public void complete(CompleterInvocation completerInvocation) {
            if(completerInvocation.getGivenCompleteValue().length() > 0)
                completerInvocation.addCompleterValue(completerInvocation.getGivenCompleteValue()+"OO");
            else
                completerInvocation.addCompleterValue("BAR");

        }
    }

    public static class HeaderCompleter implements OptionCompleter {
        @Override
        public void complete(CompleterInvocation completerInvocation) {
            if(completerInvocation.getGivenCompleteValue().length() > 0)
                completerInvocation.addCompleterValue(completerInvocation.getGivenCompleteValue()+"_FOO");
            else
                completerInvocation.addCompleterValue("BAR");
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

    @CommandDefinition(name = "foo", description = "")
    public static class FooCommand implements Command {

        @Option(completer = FooCompletor.class)
        private String bar;

        @Option(shortName = 'n', completer = NameTestCompleter.class)
        private String name;

        @Option(shortName = 'b', hasValue = false)
        private boolean bool;

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

    @GroupCommandDefinition(name = "super", description = "", groupCommands = {GitCommand.class})
    public static class SuperGitCommand implements Command {

        @Option(hasValue = false)
        private boolean help;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
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

        @Argument
        private String arg;

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

    @GroupCommandDefinition(name = "group-arg", description = "", groupCommands = {Arg2Command.class})
    public static class GroupArgCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "arg2", description = "")
    public static class Arg2Command implements Command {

        @Option
        private boolean bool;

        @Option(completer = InputTestCompleter.class)
        private String input;

        @Argument(completer = ArgTestCompleter.class)
        private String arg;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "arg", description = "")
    public static class ArgCommand implements Command {

        @Option
        private boolean bool;

        @Option(completer = InputTestCompleter.class)
        private String input;

        @Argument(completer = ArgTestCompleter.class)
        private String arg;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    public static class RebaseTestActivator implements OptionActivator {
        @Override
        public boolean isActivated(ProcessedCommand processedCommand) {
            ProcessedOption option = processedCommand.findLongOption("force");
            return option != null && option.getValue() != null && option.getValue().equals("true");
        }
    }

    public static class NameTestCompleter implements OptionCompleter {
        @Override
        public void complete(CompleterInvocation completerInvocation) {
            completerInvocation.addCompleterValue("two");
        }
    }

    public static class ArgTestCompleter implements OptionCompleter {
        @Override
        public void complete(CompleterInvocation completerInvocation) {
            if(completerInvocation.getGivenCompleteValue().equals("{foo-bar "))
                completerInvocation.addCompleterValue(completerInvocation.getGivenCompleteValue()+"YEAH");
            else if(!completerInvocation.getGivenCompleteValue().equals("ARG"))
                completerInvocation.addCompleterValue(completerInvocation.getGivenCompleteValue()+"ARG");
            else
                completerInvocation.addCompleterValue("ARG");
        }
    }

    public static class InputTestCompleter implements OptionCompleter {
        @Override
        public void complete(CompleterInvocation completerInvocation) {
            if (completerInvocation.getGivenCompleteValue().equals("{foo-barb") ||
                    completerInvocation.getGivenCompleteValue().equals("{foo-bar b")) {
                completerInvocation.addCompleterValue("bArg");
                // 1 before the cursor.
                completerInvocation.setOffset(1);
            } else {

                completerInvocation.addCompleterValue(completerInvocation.getGivenCompleteValue() + "bArg");
            }
        }
    }



}
