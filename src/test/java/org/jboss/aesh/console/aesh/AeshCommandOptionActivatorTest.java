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

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.activator.OptionActivatorProvider;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.readline.ReadlineConsole;
import org.aesh.terminal.Key;
import org.jboss.aesh.tty.TestConnection;
import org.junit.Test;

import java.io.IOException;
import org.jboss.aesh.console.command.CommandException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandOptionActivatorTest {

    @Test
    public void testOptionActivator() throws IOException, InterruptedException {

        TestConnection connection = new TestConnection();
        TestOptionValidatorProvider validatorProvider = new TestOptionValidatorProvider(new FooContext("bar"));

       CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ValCommand.class)
                .create();

        Settings settings = new SettingsBuilder()
                .connection(connection)
                .commandRegistry(registry)
                .logging(true)
                .create();

        ReadlineConsole console = new ReadlineConsole(settings);

        console.start();
        connection.read("val -");
        connection.read(Key.CTRL_I);

        Thread.sleep(80);
        connection.assertBuffer("val --bar");
        //assertEquals("val --bar ", ((AeshConsoleImpl) console).getBuffer());

        connection.read("123 --");
        connection.read(Key.CTRL_I);
        //outputStream.flush();

        Thread.sleep(80);
        //assertEquals("val --bar 123 --", ((AeshConsoleImpl) console).getBuffer());
        validatorProvider.updateContext("foo");
        connection.read(Key.CTRL_I);
        //outputStream.flush();

        Thread.sleep(80);
        //assertEquals("val --bar 123 --foo ", ((AeshConsoleImpl) console).getBuffer());

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

        public void updateContext(String context) {
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

        public void setContext(FooContext context) {
            this.context = context;
        }

        @Override
        public boolean isActivated(ProcessedCommand processedCommand) {
            return (context.getContext().equals("foo"));
        }
    }

    private static class FooContext {
        private String context;

        public FooContext(String context) {
            this.context = context;
        }

        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }
    }
}
