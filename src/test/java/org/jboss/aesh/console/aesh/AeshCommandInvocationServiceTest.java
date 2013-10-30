/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.aesh;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandInvocationProvider;
import org.jboss.aesh.console.command.CommandInvocation;
import org.jboss.aesh.console.command.CommandInvocationServices;
import org.jboss.aesh.console.command.CommandRegistry;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.ConsoleCommand;
import org.jboss.aesh.console.operator.ControlOperator;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.terminal.Shell;
import org.jboss.aesh.terminal.TestTerminal;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandInvocationServiceTest extends BaseConsoleTest {

    @Test
    public void testCommandInvocationExtension() throws IOException, InterruptedException {

        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .inputStream(pipedInputStream)
                .outputStream(byteArrayOutputStream)
                .logging(true)
                .create();


        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(new BarCommand())
                .create();

        CommandInvocationServices services = new CommandInvocationServices();
        services.registerProvider("FOO", new FooCommandInvocationProvider());

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
                .settings(settings)
                .commandRegistry(registry)
                .commandInvocationProvider(services)
                .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();
        aeshConsole.setCurrentCommandInvocationProvider("FOO");
        aeshConsole.start();

        outputStream.write(("bar\n").getBytes());

        Thread.sleep(100);
        assertTrue( byteArrayOutputStream.toString().contains("FOO") );
        aeshConsole.stop();
    }

}

@CommandDefinition(name = "bar", description = "a bar...")
class BarCommand implements Command<FooCommandInvocation> {

    @Override
    public CommandResult execute(FooCommandInvocation commandInvocation) throws IOException {
        commandInvocation.getShell().out().println(commandInvocation.getFoo());
        return CommandResult.SUCCESS;
    }
}


class FooCommandInvocation implements CommandInvocation {

    private CommandInvocation commandInvocation;

    public FooCommandInvocation(CommandInvocation commandInvocation) {
        this.commandInvocation = commandInvocation;
    }

    @Override
    public ControlOperator getControlOperator() {
        return commandInvocation.getControlOperator();
    }

    @Override
    public CommandRegistry getCommandRegistry() {
        return commandInvocation.getCommandRegistry();
    }

    @Override
    public void attachConsoleCommand(ConsoleCommand consoleCommand) {
        commandInvocation.attachConsoleCommand(consoleCommand);
    }

    @Override
    public Shell getShell() {
        return commandInvocation.getShell();
    }

    @Override
    public void setPrompt(Prompt prompt) {
        commandInvocation.setPrompt(prompt);
    }

    @Override
    public Prompt getPrompt() {
        return commandInvocation.getPrompt();
    }

    @Override
    public String getHelpInfo(String commandName) {
        return commandInvocation.getHelpInfo(commandName);
    }

    @Override
    public void stop() {
        commandInvocation.stop();
    }

    public String getFoo() {
        return "FOO";
    }
}

class FooCommandInvocationProvider implements CommandInvocationProvider<FooCommandInvocation> {
    @Override
    public FooCommandInvocation enhanceCommandInvocation(CommandInvocation commandInvocation) {
        return new FooCommandInvocation(commandInvocation);
    }
}
