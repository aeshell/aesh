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
package org.aesh.command.builder;

import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.parser.OptionParserException;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.CommandResult;
import org.aesh.readline.DefaultAeshContext;
import org.aesh.parser.LineParser;
import org.aesh.parser.ParsedLine;
import org.aesh.readline.ReadlineConsole;
import org.aesh.tty.TestConnection;
import org.aesh.terminal.utils.Config;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandDynamicTest {

    @Test
    public void testDynamic() throws Exception {
        TestConnection connection = new TestConnection();

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(createGroupCommand().create())
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation,
                        ValidatorInvocation, OptionActivator, CommandActivator> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .logging(true)
                .build();


        ReadlineConsole console = new ReadlineConsole(settings);
        AeshCompleteOperation co = new AeshCompleteOperation(new DefaultAeshContext(), "gr", 2);
        ParsedLine parsedLine = new LineParser()
                .input(co.getBuffer())
                .cursor(co.getCursor())
                .parseBrackets(true)
                .parse();
         registry.completeCommandName(co, parsedLine);
        assertEquals("group", co.getCompletionCandidates().get(0).toString());
        console.start();

        connection.read("group child1 --foo BAR"+Config.getLineSeparator());

        Thread.sleep(10);

        console.stop();
    }

    private CommandBuilder<GroupCommand> createGroupCommand() throws OptionParserException {
        return CommandBuilder.<GroupCommand>builder()
                .name("group")
                .description("")
                .addOption(ProcessedOptionBuilder.builder().name("bar").type(Boolean.class).build())
                .addChild(
                        CommandBuilder.<Child1Command>builder()
                                .name("child1")
                                .description("")
                                .command(new Child1Command())
                                .addOption(ProcessedOptionBuilder.builder()
                                        .optionType(OptionType.NORMAL)
                                        .name("foo")
                                        .fieldName("foo")
                                        .type(String.class)
                                        .hasValue(true)))
                .command(new GroupCommand());
    }


    public class GroupCommand implements Command<CommandInvocation> {

        private boolean bar;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    public class Child1Command implements Command<CommandInvocation> {

        private String foo;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            assertEquals("BAR", foo);
            return CommandResult.SUCCESS;
        }
    }

}
