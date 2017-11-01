package org.aesh.command.completer;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.io.FileResource;
import org.aesh.readline.ReadlineConsole;
import org.aesh.readline.terminal.Key;
import org.aesh.tty.TestConnection;
import org.aesh.utils.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.Assert.assertEquals;

public class AeshCommandOperatorCompletionTest {

    private Path tempDir;
    private static FileAttribute fileAttribute = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-x---"));

    private final Key completeChar =  Key.CTRL_I;

    @Before
    public void before() throws IOException {
        tempDir = createTempDirectory();
    }

    @After
    public void after() throws IOException {
        Files.delete(tempDir);
    }


    @Test
    public void testCompletionWithEndOperator() throws IOException, CommandLineParserException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ArgCommand.class)
                .command(FooCommand.class)
                .create();

         Settings settings = SettingsBuilder.builder()
                 .logging(true)
                 .connection(connection)
                 .commandRegistry(registry)
                 .enableOperatorParser(true)
                 .enableExport(false)
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
    public void testCompletionWithRedirectOutOperator() throws IOException, CommandLineParserException {
         TestConnection connection = new TestConnection();

         CommandRegistry registry = new AeshCommandRegistryBuilder()
                 .command(ArgCommand.class)
                 .command(FooCommand.class)
                 .create();

         Settings settings = SettingsBuilder.builder()
                 .logging(true)
                 .connection(connection)
                 .commandRegistry(registry)
                 .enableOperatorParser(true)
                 .enableExport(false)
                 .build();

         final File fooOut = new File(tempDir.toFile()+Config.getPathSeparator()+"foo_redirection_out.txt");
         Files.write(fooOut.toPath(), "first line".getBytes());

         ReadlineConsole console = new ReadlineConsole(settings);
         console.start();
         connection.read("arg > "+tempDir.toFile()+Config.getPathSeparator());
         connection.read(completeChar);
         assertEquals("arg > "+fooOut.toString(), connection.getOutputBuffer());
         //connection.assertBufferEndsWith("arg > "+tempDir.toFile()+Config.getPathSeparator()+fooOut);

         connection.read(Key.ENTER);
         connection.clearOutputBuffer();
         connection.read("arg>"+tempDir.toFile()+Config.getPathSeparator());
         connection.read(completeChar);
         assertEquals("arg>"+fooOut.toString(), connection.getOutputBuffer());
         connection.read(Key.ENTER);

         console.context().setCurrentWorkingDirectory(new FileResource(tempDir));

         connection.clearOutputBuffer();
         connection.read("arg>");
         connection.read(completeChar);
         assertEquals("arg>"+ fooOut.getName(), connection.getOutputBuffer());

         Files.delete(fooOut.toPath());
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

    public static Path createTempDirectory() throws IOException {
        final Path tmp;
        if(Config.isOSPOSIXCompatible())
            tmp = Files.createTempDirectory("temp"+Long.toString(System.nanoTime()), fileAttribute);
        else {
            tmp = Files.createTempDirectory("temp" + Long.toString(System.nanoTime()));
        }

        tmp.toFile().deleteOnExit();

        return tmp;
    }


}
