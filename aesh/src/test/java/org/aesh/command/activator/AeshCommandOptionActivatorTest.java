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
package org.aesh.command.activator;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.impl.internal.ParsedCommand;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.console.ReadlineConsole;
import org.aesh.terminal.Key;
import org.aesh.tty.TestConnection;
import org.junit.Test;

import java.io.IOException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandOptionActivatorTest {

    @Test
    public void testOptionActivator() throws IOException, InterruptedException, CommandRegistryException {

        TestConnection connection = new TestConnection();
        TestOptionValidatorProvider validatorProvider = new TestOptionValidatorProvider(new FooContext("bar"));

       CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(ValCommand.class)
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .connection(connection)
                        .commandRegistry(registry)
                        .optionActivatorProvider(validatorProvider)
                        .logging(true)
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);

        console.start();
        connection.read("val --");
        connection.read(Key.CTRL_I);

        Thread.sleep(80);
        connection.assertBuffer("val --bar=");

        connection.read("123 --");
        connection.read(Key.CTRL_I);

        Thread.sleep(80);
        connection.assertBuffer("val --bar=123 --");
        validatorProvider.updateContext("foo");
        connection.read(Key.CTRL_I);
        Thread.sleep(80);
        connection.assertBuffer("val --bar=123 --foo=");

        console.stop();
     }

    @CommandDefinition(name = "val", description = "")
    private static class ValCommand implements Command {

        @Option(activator = FooOptionActivator.class)
        private String foo;

        @Option
        private String bar;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return null;
        }
    }

    private static class TestOptionValidatorProvider implements OptionActivatorProvider {

        private final FooContext context;

        TestOptionValidatorProvider(FooContext context) {
            this.context = context;
        }

        void updateContext(String context) {
            this.context.setContext(context);
        }

        @Override
        public OptionActivator enhanceOptionActivator(OptionActivator optionActivator) {
            if(optionActivator instanceof FooOptionActivator)
                ((FooOptionActivator) optionActivator).setContext(context);
            return optionActivator;
        }
    }

    private static class FooOptionActivator implements OptionActivator {

        private FooContext context;

        void setContext(FooContext context) {
            this.context = context;
        }

        @Override
        public boolean isActivated(ParsedCommand parsedCommand) {
            return (context.getContext().equals("foo"));
        }
    }

    private static class FooContext {
        private String context;

        FooContext(String context) {
            this.context = context;
        }

        String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }
    }
}
