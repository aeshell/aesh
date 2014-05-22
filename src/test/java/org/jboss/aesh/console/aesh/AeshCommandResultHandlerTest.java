/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.aesh;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.result.ResultHandler;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.terminal.TestTerminal;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandResultHandlerTest {

    @Test
    public void testResultHandler() throws IOException, InterruptedException {
               PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .logging(true)
                .create();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(FooCommand.class)
                .create();

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
                .settings(settings)
                .commandRegistry(registry)
                .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();
        aeshConsole.start();

        outputStream.write(("foo --foo 1 --name aesh"+ Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(80);

        outputStream.write(("foo --foo 1"+ Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(80);

        outputStream.write(("foo --fo 1 --name aesh"+ Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(80);

    }


    @CommandDefinition(name = "foo", description = "", resultHandler = FooResultHandler.class)
    public static class FooCommand implements Command {

        @Option(required = true)
        private String foo;

        @Option
        private String name;

        @Arguments
        private List<String> arguments;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            if(name == null)
                return CommandResult.FAILURE;
            else
                return CommandResult.SUCCESS;
        }

        public String getName() {
            return name;
        }
    }

    public static class FooResultHandler implements ResultHandler {

        private transient int resultCounter = 0;

        @Override
        public void onSuccess() {
            assertEquals(0, resultCounter);
            resultCounter++;
        }

        @Override
        public void onFailure(CommandResult result) {
            assertEquals(1, resultCounter);
            resultCounter++;
        }

        @Override
        public void onValidationFailure(CommandResult result, Exception exception) {
            assertEquals(2, resultCounter);
        }
    }

}
