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
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.CommandOperation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.invocation.CommandInvocationProvider;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.invocation.CommandInvocationServices;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.command.CommandResult;
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
import java.io.PrintStream;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandNotFoundException;

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
                .outputStream(new PrintStream(byteArrayOutputStream))
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

        outputStream.write(("bar"+ Config.getLineSeparator()).getBytes());
        outputStream.flush();

        Thread.sleep(100);
        assertTrue( byteArrayOutputStream.toString().contains("FOO") );
        aeshConsole.stop();
    }

}

@CommandDefinition(name = "bar", description = "a bar...")
class BarCommand implements Command<FooCommandInvocation> {

    @Override
    public CommandResult execute(FooCommandInvocation commandInvocation) throws CommandException, InterruptedException {
        commandInvocation.getShell().out().println(commandInvocation.getFoo());
        return CommandResult.SUCCESS;
    }
}


class FooCommandInvocation implements CommandInvocation {

    private final CommandInvocation commandInvocation;

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

    @Override
    public AeshContext getAeshContext() {
        return commandInvocation.getAeshContext();
    }

    @Override
    public CommandOperation getInput() throws InterruptedException {
        return commandInvocation.getInput();
    }

    @Override
    public String getInputLine() throws InterruptedException {
        return commandInvocation.getInputLine();
    }

    @Override
    public int getPid() {
        return commandInvocation.getPid();
    }

    @Override
    public void putProcessInBackground() {
        commandInvocation.putProcessInBackground();
    }

    @Override
    public void putProcessInForeground() {
        commandInvocation.putProcessInForeground();
    }

    @Override
    public void executeCommand(String input) {
        commandInvocation.executeCommand(input);
    }

    @Override
    public void print(String msg) {
        commandInvocation.print(msg);
    }

    @Override
    public void println(String msg) {
        commandInvocation.println(msg);
    }

    @Override
    public boolean isEchoing() {
        return commandInvocation.isEchoing();
    }

    @Override
    public void setEcho(boolean interactive) {
        commandInvocation.setEcho(interactive);
    }

    public String getFoo() {
        return "FOO";
    }

    @Override
    public Command getPopulatedCommand(String commandLine) throws CommandNotFoundException, CommandException, CommandLineParserException, OptionValidatorException {
        return commandInvocation.getPopulatedCommand(commandLine);
    }
}

class FooCommandInvocationProvider implements CommandInvocationProvider<FooCommandInvocation> {
    @Override
    public FooCommandInvocation enhanceCommandInvocation(CommandInvocation commandInvocation) {
        return new FooCommandInvocation(commandInvocation);
    }
}
