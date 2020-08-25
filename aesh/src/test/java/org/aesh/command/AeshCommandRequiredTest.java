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
package org.aesh.command;

import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.readline.Prompt;
import org.aesh.readline.ReadlineConsole;
import org.aesh.tty.TestConnection;
import org.aesh.terminal.utils.Config;
import org.junit.Test;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandRequiredTest {

    @Test
    public void testOptionRequired() throws Exception {
        TestConnection connection = new TestConnection(false);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(ReqCommand.class)
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .logging(true)
                        .connection(connection)
                        .commandRegistry(registry)
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        connection.read("req "+ Config.getLineSeparator());
        Thread.sleep(200);
        connection.assertBufferEndsWith("Option: --reset-configuration is required for this command."
                +Config.getLineSeparator());

        console.stop();
    }

    @Test
    public void testArgumentAndOptionRequired() throws Exception {
        TestConnection connection = new TestConnection(false);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(ReqCommand2.class)
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .logging(true)
                        .connection(connection)
                        .commandRegistry(registry)
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        connection.read("req2 --reset-configuration=false"+ Config.getLineSeparator());
        Thread.sleep(200);
        connection.assertBufferEndsWith("Argument 'arg' is required for this command."
                +Config.getLineSeparator());

        console.stop();
    }

    @Test
    public void testArgumentAndOptionRequiredGroupCommand() throws Exception {
        TestConnection connection = new TestConnection(false);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(GroupReqCommand.class)
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .logging(true)
                        .connection(connection)
                        .commandRegistry(registry)
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        connection.read("group req2 --reset-configuration=false"+ Config.getLineSeparator());
        Thread.sleep(200);
        connection.assertBufferEndsWith("Argument 'arg' is required for this command."
                +Config.getLineSeparator());

        console.stop();
    }



    @CommandDefinition(name = "req", description = "")
    public static class ReqCommand implements Command {

        @Option(name = "reset-configuration", required = true)
        private boolean resetConfiguration;

        @Option
        private String foo;

        @Argument
        private String arg;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "group", description = "", groupCommands = {ReqCommand2.class})
    public static class GroupReqCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }


    @CommandDefinition(name = "req2", description = "")
    public static class ReqCommand2 implements Command {

        @Option(name = "reset-configuration", required = true)
        private boolean resetConfiguration;

        @Option
        private String foo;

        @Argument(required = true)
        private String arg;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.println("resetConfiguration is: "+resetConfiguration+", arg is:"+arg);
            return CommandResult.SUCCESS;
        }
    }


}
