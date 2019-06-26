package org.aesh.command.completer;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.io.FileResource;
import org.aesh.readline.ReadlineConsole;
import org.aesh.readline.terminal.Key;
import org.aesh.terminal.tty.Size;
import org.aesh.tty.TestConnection;
import org.aesh.terminal.utils.Config;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AeshCommandOperatorCompletionTest {

    private final Key completeChar =  Key.CTRL_I;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testCompletionWithEndOperator() throws IOException, CommandRegistryException, InterruptedException {
        TestConnection connection = new TestConnection(new Size(400, 80));

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(ArgCommand.class)
                .command(FooCommand.class)
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .logging(true)
                        .connection(connection)
                        .commandRegistry(registry)
                        .enableOperatorParser(true)
                        .enableExport(false)
                        .enableAlias(false)
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();
        connection.read("arg;");
        connection.clearOutputBuffer();
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer(Config.getLineSeparator()+"arg  foo  "+Config.getLineSeparator()+"arg;");

        console.stop();
    }

     @Test
    public void testCompletionWithRedirectOutOperator() throws IOException, CommandRegistryException, InterruptedException {
         TestConnection connection = new TestConnection();

         CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                 .command(ArgCommand.class)
                 .command(FooCommand.class)
                 .create();

         Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                         OptionActivator, CommandActivator> settings =
                 SettingsBuilder.builder()
                         .logging(true)
                         .connection(connection)
                         .commandRegistry(registry)
                         .enableOperatorParser(true)
                         .enableExport(false)
                         .enableAlias(false)
                         .build();

         final Path tempDir = temporaryFolder.getRoot().toPath();
         final File fooOut = new File(tempDir.toFile()+Config.getPathSeparator()+"foo_redirection_out.txt");
         Files.write(fooOut.toPath(), "first line".getBytes());

         ReadlineConsole console = new ReadlineConsole(settings);
         console.start();
         connection.read("arg > "+tempDir.toFile()+Config.getPathSeparator());
         connection.read(completeChar);
         connection.assertBufferEndsWith(fooOut.getName());

         connection.read(Key.ENTER);
         connection.clearOutputBuffer();
         connection.read("arg>"+tempDir.toFile()+Config.getPathSeparator());
         connection.read(completeChar);
         connection.assertBufferEndsWith(fooOut.getName());
         connection.read(Key.ENTER);

         console.context().setCurrentWorkingDirectory(new FileResource(tempDir));

         connection.clearOutputBuffer();
         connection.read("arg>");
         connection.read(completeChar);
         connection.assertBufferEndsWith(fooOut.getName());

         console.stop();
     }


    @Test
    public void testCompletionWithPipeOperator() throws IOException, CommandRegistryException, InterruptedException {
        TestConnection connection = new TestConnection(new Size(400, 80));

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(ArgCommand.class)
                .command(FooCommand.class)
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .logging(true)
                        .connection(connection)
                        .commandRegistry(registry)
                        .enableOperatorParser(true)
                        .enableExport(false)
                        .enableAlias(false)
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();
        connection.read("arg|");
        connection.clearOutputBuffer();
        connection.read("f");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("foo ");
        connection.read("--bo");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("foo --bool=");
        connection.read("tr");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("foo --bool=true ");
        connection.read("--i");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("foo --bool=true --input=");
        connection.read("foo ");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("foo --bool=true --input=foo ARG ");
        connection.read(Key.ENTER);
        connection.clearOutputBuffer();
        connection.read("arg | ");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("arg | "+Config.getLineSeparator()+"arg  foo  "+Config.getLineSeparator()+"arg | ");

        connection.read("foo");
        connection.clearOutputBuffer();
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer(" ");
        connection.read(Key.ENTER);
        connection.clearOutputBuffer();
        connection.read("|");
        connection.read(Key.LEFT_2);
        connection.clearOutputBuffer();
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer(String.format("%1$sarg  foo  %1$s|", Config.getLineSeparator()));

        console.stop();
    }

    @CommandDefinition(name = "foo", description = "")
    public static class FooCommand implements Command {

        @Option
        private boolean bool;

        @Option(completer = InputTestCompleter.class)
        private String input;

        @Argument(completer = ArgTestCompleter.class)
        private String arg;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "arg", description = "")
    public static class ArgCommand implements Command {

        @Option
        private boolean bool;

        @Option(completer = InputTestCompleter.class)
        private String input;

        @Argument(completer = ArgTestCompleter.class)
        private String arg;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.println("hello aesh!");
            return CommandResult.SUCCESS;
        }
    }

    public static class ArgTestCompleter implements OptionCompleter {
        @Override
        public void complete(CompleterInvocation completerInvocation) {
            if(completerInvocation.getGivenCompleteValue().equals("{foo-bar "))
                completerInvocation.addCompleterValue(completerInvocation.getGivenCompleteValue()+"YEAH");
            else if(!completerInvocation.getGivenCompleteValue().equals("ARG"))
                completerInvocation.addCompleterValue(completerInvocation.getGivenCompleteValue()+"ARG");
            else
                completerInvocation.addCompleterValue("ARG");
        }
    }

    public static class InputTestCompleter implements OptionCompleter {
        @Override
        public void complete(CompleterInvocation completerInvocation) {
            if (completerInvocation.getGivenCompleteValue().equals("{foo-barb") ||
                    completerInvocation.getGivenCompleteValue().equals("{foo-bar b")) {
                completerInvocation.addCompleterValue("bArg");
                // 1 before the cursor.
                completerInvocation.setOffset(1);
            } else {

                completerInvocation.addCompleterValue(completerInvocation.getGivenCompleteValue() + "bArg");
            }
        }
    }

}
