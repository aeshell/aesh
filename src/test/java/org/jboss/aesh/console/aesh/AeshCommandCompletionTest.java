package org.jboss.aesh.console.aesh;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.AeshConsoleImpl;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.completer.CompleterInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.command.CommandResult;
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
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandCompletionTest {

    private KeyOperation completeChar =  new KeyOperation(Key.CTRL_I, Operation.COMPLETE);

    @Test
    public void testCompletion() throws Exception {

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

        outputStream.write(("foo --name aslak --bar bar\\ 2").getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.flush();

        Thread.sleep(80);
        assertEquals("foo --name aslak --bar bar\\ 2\\ 3\\ 4 ", ((AeshConsoleImpl) aeshConsole).getBuffer());

        outputStream.write(Config.getLineSeparator().getBytes());
        outputStream.flush();
        Thread.sleep(80);

        outputStream.write(("foo --bar bar\\ 2\\ ").getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.flush();

        Thread.sleep(80);
        assertEquals("foo --bar bar\\ 2\\ 3\\ 4 ", ((AeshConsoleImpl) aeshConsole).getBuffer());

        outputStream.write(Config.getLineSeparator().getBytes());
        outputStream.flush();
        Thread.sleep(80);

        outputStream.write(("foo --bar bar").getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.flush();

        Thread.sleep(80);
        assertEquals("foo --bar bar\\ 2 ", ((AeshConsoleImpl) aeshConsole).getBuffer());

        outputStream.write(Config.getLineSeparator().getBytes());
        outputStream.flush();
        Thread.sleep(80);

        outputStream.write(("foo --bar foo ").getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.flush();

        Thread.sleep(80);
        assertEquals("foo --bar foo ", ((AeshConsoleImpl) aeshConsole).getBuffer());
        outputStream.write(("--b").getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.flush();

        Thread.sleep(80);
        assertEquals("foo --bar foo --b", ((AeshConsoleImpl) aeshConsole).getBuffer());

        aeshConsole.stop();
    }

    @Test
    public void testCompletionNoArguments() {

    }

    @CommandDefinition(name = "foo", description = "")
    public static class FooCommand implements Command {

        @Option(completer = FooCompletor.class)
        private String bar;

        @Option
        private String name;

        @Arguments
        private List<String> arguments;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException {
            return CommandResult.SUCCESS;
        }

        public String getName() {
            return name;
        }
    }

    @CommandDefinition(name = "bar", description = "")
    public static class BarCommand implements Command {

        @Option(completer = FooCompletor.class)
        private String bar;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException {
            return CommandResult.SUCCESS;
        }
    }

    public static class FooCompletor implements OptionCompleter {

        @Override
        public void complete(CompleterInvocation completerData) {
            if(completerData.getGivenCompleteValue().equals("bar 2")) {
                if(((FooCommand) completerData.getCommand()).getName().equals("aslak"))
                    completerData.addCompleterValue("bar 2 3 4");
            }
            else if(completerData.getGivenCompleteValue().equals("bar 2 ")) {
                completerData.addCompleterValue("bar 2 3 4");
            }
            else if(completerData.getGivenCompleteValue().equals("bar")) {
                completerData.addCompleterValue("bar 2");
            }
        }
    }
}
