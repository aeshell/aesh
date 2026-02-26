package org.aesh.command;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.junit.Test;

public class GroupCommandRuntimeTest {

    private static String lastConnect;
    private static String lastSubcommand;
    private static boolean lastVerbose;

    @Test
    public void testGroupCommandWithOptionsBeforeSubcommand() throws CommandRegistryException, CommandException,
            OptionValidatorException, IOException, InterruptedException, CommandLineParserException,
            CommandValidatorException, CommandNotFoundException {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(CliGroupCommand.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        // Test: long option with = value before subcommand
        reset();
        runtime.executeCommand("cli --connect=http://127.0.0.1:11222 version");
        assertEquals("version", lastSubcommand);

        // Test: short option before subcommand
        reset();
        runtime.executeCommand("cli -c http://127.0.0.1:11222 version");
        assertEquals("version", lastSubcommand);

        // Test: subcommand first (normal flow)
        reset();
        runtime.executeCommand("cli version");
        assertEquals("version", lastSubcommand);

        // Test: boolean flag before subcommand
        reset();
        runtime.executeCommand("cli --verbose version");
        assertEquals("version", lastSubcommand);

        // Test: multiple options before subcommand
        reset();
        runtime.executeCommand("cli --connect=http://localhost:11222 --verbose version");
        assertEquals("version", lastSubcommand);

        // Test: child command with its own options
        reset();
        runtime.executeCommand("cli --connect=http://127.0.0.1:11222 child --name test");
        assertEquals("child", lastSubcommand);
    }

    private void reset() {
        lastConnect = null;
        lastSubcommand = null;
        lastVerbose = false;
    }

    @CommandDefinition(name = "version", description = "Show version")
    public static class VersionCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            lastSubcommand = "version";
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "child", description = "A child command")
    public static class ChildCommand implements Command<CommandInvocation> {
        @Option
        private String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            lastSubcommand = "child";
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "cli", description = "", groupCommands = { VersionCommand.class, ChildCommand.class })
    public static class CliGroupCommand implements Command<CommandInvocation> {
        @Option(shortName = 'c')
        private String connect;

        @Option(shortName = 'v', hasValue = false)
        private boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            lastConnect = connect;
            lastVerbose = verbose;
            return CommandResult.SUCCESS;
        }
    }
}
