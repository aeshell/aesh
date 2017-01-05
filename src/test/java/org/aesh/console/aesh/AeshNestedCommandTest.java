package org.aesh.console.aesh;

import org.aesh.command.CommandDefinition;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.console.settings.Settings;
import org.aesh.console.settings.SettingsBuilder;
import org.aesh.readline.ReadlineConsole;
import org.aesh.util.Config;
import org.aesh.tty.TestConnection;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * TODO: Ignored for now, we might not support nested commands in the 1.0 version...
 *
 * Created by joe on 4/14/15.
 */
@Ignore
public class AeshNestedCommandTest {

    private volatile boolean fooInBackground = false;
    private volatile boolean fooReturned = false;
    private volatile boolean barCalled = false;

    @Test
    public void testNestedCommand() throws IOException, InterruptedException {
        TestConnection connection = new TestConnection();

       CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(new CustomCommand())
                .command(new CustomCommandInternal())
                .create();

        Settings settings = new SettingsBuilder()
                .connection(connection)
                .commandRegistry(registry)
                .logging(true)
                .create();

        ReadlineConsole console = new ReadlineConsole(settings);

        console.start();

        connection.read("foo");
        connection.read(Config.getLineSeparator());
        //outputStream.flush();

        connection.read("bar");
        connection.read(Config.getLineSeparator());
        //outputStream.flush();

        Thread.sleep(80);
        while(!fooReturned){
        }
        console.stop();

    }

    @CommandDefinition(name = "foo", description = "")
    class CustomCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
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
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
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
