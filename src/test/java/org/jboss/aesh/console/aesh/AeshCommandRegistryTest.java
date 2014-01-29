package org.jboss.aesh.console.aesh;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.AeshConsoleImpl;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.KeyOperation;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.terminal.TestTerminal;
import org.junit.Test;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandRegistryTest {

    private static final String WRITTEN = "hgjfiehk";
    private final KeyOperation completeChar = new KeyOperation(Key.CTRL_I, Operation.COMPLETE);

    @Test
    public void testExceptionThrownFromCommandRegistryShouldNotCrashAesh() throws Exception {

        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder().terminal(new TestTerminal()).inputStream(pipedInputStream)
            .outputStream(new PrintStream(byteArrayOutputStream)).logging(true).create();

        CommandRegistry registry = new CommandRegistry() {

            @Override
            public CommandContainer getCommand(String name, String line) throws CommandNotFoundException {
                throw new IllegalStateException("Should not crash Aesh");
            }

            @Override
            public Set<String> getAllCommandNames() {
                throw new IllegalStateException("Should not crash Aesh");
            }
        };

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder().settings(settings).commandRegistry(registry)
            .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();
        aeshConsole.start();

        outputStream.write(completeChar.getFirstValue());
        outputStream.flush();
        Thread.sleep(80);
        outputStream.write(WRITTEN.getBytes());
        outputStream.flush();
        Thread.sleep(80);

        assertEquals(WRITTEN, ((AeshConsoleImpl) aeshConsole).getBuffer().trim());

        aeshConsole.stop();
    }

    @Test
    public void testCommandRegistryReturningNullShouldNotCrashAesh() throws Exception {

        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .setPersistExport(false)
                .logging(true)
                .create();

        CommandRegistry registry = new CommandRegistry() {

            @Override
            public CommandContainer getCommand(String name, String line) throws CommandNotFoundException {
                return null;
            }

            @Override
            public Set<String> getAllCommandNames() {
                return null;
            }
        };

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
                .settings(settings)
                .commandRegistry(registry)
                .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();
        aeshConsole.start();

        outputStream.write(completeChar.getFirstValue());
        outputStream.flush();
        Thread.sleep(80);
        outputStream.write(WRITTEN.getBytes());
        outputStream.flush();
        Thread.sleep(80);

        assertEquals(WRITTEN, ((AeshConsoleImpl) aeshConsole).getBuffer().trim());

        aeshConsole.stop();
    }

    @Test
    public void testCommandRegistryReturningNormalValues() throws Exception {

        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder().terminal(new TestTerminal()).inputStream(pipedInputStream)
            .outputStream(new PrintStream(byteArrayOutputStream)).logging(true).create();

        CommandRegistry registry = new CommandRegistry() {

            @Override
            public CommandContainer getCommand(String name, String line) throws CommandNotFoundException {
                return null;
            }

            @Override
            public Set<String> getAllCommandNames() {
                return new HashSet<>();
            }
        };

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder().settings(settings).commandRegistry(registry)
            .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();
        aeshConsole.start();

        outputStream.write(completeChar.getFirstValue());
        outputStream.flush();
        outputStream.write(WRITTEN.getBytes());
        outputStream.flush();
        Thread.sleep(100);

        assertEquals(WRITTEN, ((AeshConsoleImpl) aeshConsole).getBuffer().trim());

        aeshConsole.stop();
    }

}
