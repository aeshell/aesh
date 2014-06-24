/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.aesh;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.AeshConsoleImpl;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.activator.OptionActivatorProvider;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.KeyOperation;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.terminal.TestTerminal;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandOptionActivatorTest {

    private KeyOperation completeChar =  new KeyOperation(Key.CTRL_I, Operation.COMPLETE);

    @Test
    public void testOptionActivator() throws IOException, InterruptedException {

        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        TestOptionValidatorProvider validatorProvider = new TestOptionValidatorProvider(new FooContext("bar"));

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .logging(true)
                .create();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ValCommand.class)
                .create();

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
                .settings(settings)
                .commandRegistry(registry)
                .optionActivatorProvider(validatorProvider)
                .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();

        aeshConsole.start();
        outputStream.write(("val -").getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.flush();

        Thread.sleep(80);
        assertEquals("val --bar ", ((AeshConsoleImpl) aeshConsole).getBuffer());

        outputStream.write(("123 --").getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.flush();

        Thread.sleep(80);
        assertEquals("val --bar 123 --", ((AeshConsoleImpl) aeshConsole).getBuffer());
        validatorProvider.updateContext("foo");
        outputStream.write(completeChar.getFirstValue());
        outputStream.flush();

        Thread.sleep(80);
        assertEquals("val --bar 123 --foo ", ((AeshConsoleImpl) aeshConsole).getBuffer());

        aeshConsole.stop();
     }

    @CommandDefinition(name = "val", description = "")
    private static class ValCommand implements Command {

        @Option(activator = FooOptionActivator.class)
        private String foo;

        @Option
        private String bar;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            return null;
        }
    }

    private static class TestOptionValidatorProvider implements OptionActivatorProvider {

        private FooContext context;

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
