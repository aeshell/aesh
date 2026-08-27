package org.aesh.command.help;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.ReadlineConsole;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

public class AeshHelpCommandTest {

    @Test
    public void testCommandInvocationTest() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(2);
        CountDownLatch latch3 = new CountDownLatch(3);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(FooCommand.class)
                .command(BarCommand.class)
                .command(FooBarCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder
                .builder()
                .commandRegistry(registry)
                .enableOperatorParser(true)
                .connection(connection)
                .setPersistExport(false)
                .logging(true)
                .commandExecutionListener((line, result, durationMs) -> {
                    latch1.countDown();
                    latch2.countDown();
                    latch3.countDown();
                })
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();
        connection.read("foo -h" + Config.getLineSeparator());
        assertTrue("First command should complete", latch1.await(5, TimeUnit.SECONDS));
        connection.assertBufferEndsWith("ask me" + Config.getLineSeparator() + Config.getLineSeparator());
        connection.clearOutputBuffer();
        connection.read("bar -h" + Config.getLineSeparator());
        assertTrue("Second command should complete", latch2.await(5, TimeUnit.SECONDS));
        connection.assertBufferEndsWith("ask me" + Config.getLineSeparator() + Config.getLineSeparator());
        connection.read("foobar -h" + Config.getLineSeparator());
        assertTrue("Third command should complete", latch3.await(5, TimeUnit.SECONDS));
        connection.assertBufferEndsWith("ask me" + Config.getLineSeparator() + Config.getLineSeparator());

        console.stop();
    }

    @CommandDefinition(name = "foo", generateHelp = true, description = "")
    private static class FooCommand implements Command {

        @Option(description = "my value")
        private String value;

        @Option(description = "ask me")
        private String ask;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "bar", generateHelp = true, description = "")
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

    @CommandDefinition(name = "foobar", generateHelp = true, description = "")
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
