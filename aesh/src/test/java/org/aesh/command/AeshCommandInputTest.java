package org.aesh.command;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
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
import org.aesh.terminal.KeyAction;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

public class AeshCommandInputTest {

    @Test
    public void testCommandInvocationTest() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(FooCommand.class)
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation, OptionActivator, CommandActivator> settings = SettingsBuilder
                .builder()
                .commandRegistry(registry)
                .enableOperatorParser(true)
                .connection(connection)
                .setPersistExport(false)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();
        connection.read("foo" + Config.getLineSeparator());
        Thread.sleep(10);
        connection.read(Key.a);
        Thread.sleep(10);
        connection.read(Key.b);
        Thread.sleep(100);
        connection.read(Key.c);

        console.stop();
    }

    @CommandDefinition(name = "foo", description = "")
    private static class FooCommand implements Command {

        @Option
        private String value;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            KeyAction commandOperation = null;
            commandOperation = commandInvocation.input();
            assertTrue(Key.a.equalTo(commandOperation));

            commandOperation = commandInvocation.input(3, TimeUnit.SECONDS);
            assertTrue(Key.b.equalTo(commandOperation));

            commandOperation = commandInvocation.input(1, TimeUnit.MILLISECONDS);
            assertNull(commandOperation);

            return CommandResult.SUCCESS;
        }
    }

}
