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

import org.jboss.aesh.cl.builder.CommandBuilder;
import org.jboss.aesh.cl.internal.OptionType;
import org.jboss.aesh.cl.internal.ProcessedOptionBuilder;
import org.jboss.aesh.cl.parser.OptionParserException;
import org.jboss.aesh.complete.AeshCompleteOperation;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.DefaultAeshContext;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.readline.ReadlineConsole;
import org.jboss.aesh.tty.TestConnection;
import org.jboss.aesh.util.Config;
import org.junit.Test;

import java.util.HashMap;
import org.jboss.aesh.console.command.CommandException;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandDynamicTest {

    @Test
    public void testDynamic() throws Exception {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(createGroupCommand().create())
                .create();

        Settings settings = new SettingsBuilder()
                .connection(connection)
                .commandRegistry(registry)
                .logging(true)
                .create();


        ReadlineConsole console = new ReadlineConsole(settings);
        AeshCompleteOperation co = new AeshCompleteOperation(new DefaultAeshContext(), "gr", 2);
        registry.completeCommandName(co);
        assertEquals("group", co.getCompletionCandidates().get(0).toString());
        console.start();

        connection.read("group child1 --foo BAR");
        connection.read(Config.getLineSeparator());
        //outputStream.flush();

        Thread.sleep(80);

        console.stop();
    }

    private CommandBuilder createGroupCommand() throws OptionParserException {
        CommandBuilder builder = new CommandBuilder()
                .name("group")
                .description("")
                .addOption(new ProcessedOptionBuilder().name("foo").type(Boolean.class).create())
                .addChild(
                        new CommandBuilder()
                                .name("child1")
                                .description("")
                                .command(new Child1Command())
                                .addOption(new ProcessedOptionBuilder()
                                        .optionType(OptionType.NORMAL)
                                        .name("foo")
                                        .fieldName("foo")
                                        .type(String.class)
                                        .hasValue(true)))
                .command(new GroupCommand());

        return builder;
    }


    public class GroupCommand implements Command<CommandInvocation> {

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


    public class CommandAdapter implements Command<CommandInvocation> {

        private String name;

        private HashMap<String, String> fields;

        public CommandAdapter(String name, HashMap<String, String> fields) {
            this.name = name;
            this.fields = fields;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            StringBuilder builder = new StringBuilder();
            commandInvocation.println("creating data packet we're sending over the wire:");
            for(String key : fields.keySet()) {
                if(fields.get(key) != null) {
                    if(builder.length() > 0)
                        builder.append(Config.getLineSeparator());
                    builder.append(key).append(": ").append(fields.get(key));
                }
            }

            commandInvocation.println("Sending: " + builder.toString());
            return CommandResult.SUCCESS;
        }

        public String getField(String fieldName) {
            return fields.get(fieldName);
        }

        public String getName() {
            return name;
        }

        public void clearValues() {
            for(String key : fields.keySet())
                fields.put(key, null);
        }
    }

}
