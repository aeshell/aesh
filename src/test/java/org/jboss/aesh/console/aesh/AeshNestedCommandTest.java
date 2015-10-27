package org.jboss.aesh.console.aesh;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertTrue;

/**
 * Created by joe on 4/14/15.
 */
public class AeshNestedCommandTest {

    private volatile boolean fooInBackground = false;
    private volatile boolean fooReturned = false;
    private volatile boolean barCalled = false;

    @Test
    public void testNestedCommand() throws IOException, InterruptedException {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .logging(true)
                .create();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(new CustomCommand())
                .command(new CustomCommandInternal())
                .create();

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
                .settings(settings)
                .commandRegistry(registry)
                .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();

        aeshConsole.start();

        outputStream.write("foo".getBytes());
        outputStream.write(Config.getLineSeparator().getBytes());
        outputStream.flush();

        outputStream.write("bar".getBytes());
        outputStream.write(Config.getLineSeparator().getBytes());
        outputStream.flush();

        Thread.sleep(80);
        while(!fooReturned){
        }
        aeshConsole.stop();

    }

    @CommandDefinition(name = "foo", description = "")
    class CustomCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            try {
                commandInvocation.putProcessInBackground();

                fooInBackground = true;
                while (!barCalled) {
                }

                commandInvocation.putProcessInForeground();

                assertTrue(barCalled);
                if(barCalled){
                    return CommandResult.SUCCESS;
                }else{
                    return CommandResult.FAILURE;
                }
            }finally {
                fooInBackground = false;
                fooReturned = true;
            }

        }

    }

    @CommandDefinition(name = "bar", description = "")
    class CustomCommandInternal implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            try {
                assertTrue(fooInBackground);

                if (fooInBackground) {
                    return CommandResult.SUCCESS;
                } else {
                    return CommandResult.FAILURE;
                }
            }finally{
                barCalled = true;
            }
        }

    }

}
