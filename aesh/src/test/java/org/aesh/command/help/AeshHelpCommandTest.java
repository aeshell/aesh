package org.aesh.command.help;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
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
import org.aesh.readline.ReadlineConsole;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

import java.io.IOException;

public class AeshHelpCommandTest {

    @Test
   public void testCommandInvocationTest() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();

         CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                 .command(FooCommand.class)
                 .command(BarCommand.class)
                 .command(FooBarCommand.class)
                 .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .commandRegistry(registry)
                        .enableOperatorParser(true)
                        .connection(connection)
                        .setPersistExport(false)
                        .logging(true)
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();
        connection.read("foo -h" + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("--ask       ask me"+Config.getLineSeparator()+Config.getLineSeparator());
        connection.clearOutputBuffer();
        connection.read("bar -h" + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("--ask       ask me"+Config.getLineSeparator()+Config.getLineSeparator());
        connection.read("foobar -h" + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("--ask       ask me"+Config.getLineSeparator()+Config.getLineSeparator());

        console.stop();
    }

   @CommandDefinition(name ="foo", generateHelp = true, description = "")
    private static class FooCommand implements Command {

       @Option(description = "my value")
       private String value;

       @Option(description = "ask me" )
       private String ask;

       @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

   @CommandDefinition(name ="bar", generateHelp = true, description = "")
    private static class BarCommand implements Command {

       @Option(description = "my value")
       private String value;

       @Option(description = "ask me", askIfNotSet = true)
       private String ask;

       @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

   @CommandDefinition(name ="foobar", generateHelp = true, description = "")
    private static class FooBarCommand implements Command {

       @Option(description = "my value")
       private String value;

       @Option(description = "ask me", required = true)
       private String ask;

       @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

}
