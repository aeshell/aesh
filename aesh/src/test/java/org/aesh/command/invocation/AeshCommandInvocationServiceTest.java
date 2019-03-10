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
package org.aesh.command.invocation;

import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.command.shell.Shell;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.readline.Prompt;
import org.aesh.readline.action.KeyAction;
import org.aesh.utils.Config;
import org.aesh.command.CommandDefinition;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.CommandResult;
import org.aesh.readline.ReadlineConsole;
import org.aesh.tty.TestConnection;
import org.junit.Test;

import java.io.IOException;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.Executor;
import org.aesh.command.CommandNotFoundException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandInvocationServiceTest {

    @Test
    public void testCommandInvocationExtension() throws IOException, InterruptedException, CommandRegistryException {

        TestConnection connection = new TestConnection();

        CommandRegistry<FooCommandInvocation> registry = AeshCommandRegistryBuilder.<FooCommandInvocation>builder()
                .command(new BarCommand())
                .create();

        Settings<FooCommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator> settings = SettingsBuilder.<FooCommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator>builder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .commandRegistry(registry)
                .commandInvocationProvider(new FooCommandInvocationProvider())
                .build();

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


class FooCommandInvocation<CI extends CommandInvocation> implements CommandInvocation {

    private final CommandInvocation<CI> commandInvocation;

    FooCommandInvocation(CommandInvocation<CI> commandInvocation) {
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
    public String getHelpInfo() {
        return commandInvocation.getHelpInfo();
    }

    @Override
    public void stop() {
        commandInvocation.stop();
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
    public void executeCommand(String input) throws CommandNotFoundException,
            CommandLineParserException, OptionValidatorException,
            CommandValidatorException, CommandException, InterruptedException, IOException {
        commandInvocation.executeCommand(input);
    }

    @Override
    public void print(String msg, boolean paging) {
        commandInvocation.print(msg, paging);
    }

    @Override
    public void println(String msg, boolean paging) {
        commandInvocation.println(msg, paging);
    }

    public String getFoo() {
        return "FOO";
    }

    @Override
    public Executor<CI> buildExecutor(String line) throws CommandNotFoundException,
            CommandLineParserException, OptionValidatorException, CommandValidatorException, IOException {
        return commandInvocation.buildExecutor(line);
    }

    @Override
    public CommandInvocationConfiguration getConfiguration() {
        return null;
    }
}

@SuppressWarnings("unchecked")
class FooCommandInvocationProvider implements CommandInvocationProvider<CommandInvocation> {
    @Override
    public FooCommandInvocation enhanceCommandInvocation(CommandInvocation commandInvocation) {
        return new FooCommandInvocation(commandInvocation);
    }
}
