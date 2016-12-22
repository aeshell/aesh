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

import org.aesh.console.AeshContext;
import org.aesh.console.Shell;
import org.aesh.console.command.Command;
import org.aesh.console.command.CommandException;
import org.aesh.console.command.invocation.CommandInvocation;
import org.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.aesh.console.settings.Settings;
import org.aesh.console.settings.SettingsBuilder;
import org.aesh.readline.Prompt;
import org.aesh.readline.action.KeyAction;
import org.aesh.util.Config;
import org.aesh.cl.CommandDefinition;
import org.aesh.console.BaseConsoleTest;
import org.aesh.console.command.invocation.CommandInvocationProvider;
import org.aesh.console.command.invocation.CommandInvocationServices;
import org.aesh.console.command.registry.CommandRegistry;
import org.aesh.console.command.CommandResult;
import org.aesh.readline.ReadlineConsole;
import org.aesh.tty.TestConnection;
import org.junit.Test;

import java.io.IOException;
import org.aesh.cl.parser.CommandLineParserException;
import org.aesh.cl.validator.CommandValidatorException;
import org.aesh.cl.validator.OptionValidatorException;
import org.aesh.command.Executor;
import org.aesh.console.command.CommandNotFoundException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandInvocationServiceTest extends BaseConsoleTest {

    @Test
    public void testCommandInvocationExtension() throws IOException, InterruptedException {

        TestConnection connection = new TestConnection();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(new BarCommand())
                .create();

        CommandInvocationServices services = new CommandInvocationServices();
        services.registerDefaultProvider(new FooCommandInvocationProvider());

        Settings settings = new SettingsBuilder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .commandRegistry(registry)
                .commandInvocationServices(services)
                .create();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("bar"+ Config.getLineSeparator());

        connection.clearOutputBuffer();
        Thread.sleep(100);
        connection.assertBuffer("FOO"+Config.getLineSeparator());
        //assertTrue( byteArrayOutputStream.toString().contains("FOO") );
        console.stop();
    }

}

@CommandDefinition(name = "bar", description = "a bar...")
class BarCommand implements Command<FooCommandInvocation> {

    @Override
    public CommandResult execute(FooCommandInvocation commandInvocation) throws CommandException, InterruptedException {
        commandInvocation.println(commandInvocation.getFoo());
        return CommandResult.SUCCESS;
    }
}


class FooCommandInvocation implements CommandInvocation {

    private final CommandInvocation commandInvocation;

    public FooCommandInvocation(CommandInvocation commandInvocation) {
        this.commandInvocation = commandInvocation;
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
    public KeyAction input() throws InterruptedException {
        return commandInvocation.input();
    }

    @Override
    public String inputLine() throws InterruptedException {
        return commandInvocation.inputLine();
    }

    @Override
    public String inputLine(Prompt prompt) throws InterruptedException {
        return commandInvocation.inputLine(prompt);
    }

    @Override
    public int pid() {
        return commandInvocation.pid();
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
    public void executeCommand(String input) throws CommandNotFoundException,
            CommandLineParserException, OptionValidatorException,
            CommandValidatorException, CommandException, InterruptedException {
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

    public String getFoo() {
        return "FOO";
    }

    @Override
    public Executor<? extends CommandInvocation> buildExecutor(String line) throws CommandNotFoundException,
            CommandLineParserException, OptionValidatorException, CommandValidatorException {
        return commandInvocation.buildExecutor(line);
    }
}

class FooCommandInvocationProvider implements CommandInvocationProvider<FooCommandInvocation> {
    @Override
    public FooCommandInvocation enhanceCommandInvocation(CommandInvocation commandInvocation) {
        return new FooCommandInvocation(commandInvocation);
    }
}
