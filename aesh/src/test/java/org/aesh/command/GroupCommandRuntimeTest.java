package org.aesh.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void testInheritedOptionsViaBuildExecutor() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(InheritGroup.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        Executor<?> executor = runtime.buildExecutor("igroup --verbose sub");
        Execution<?> execution = executor.getExecutions().get(0);
        execution.populateCommand();

        InheritSub sub = (InheritSub) execution.getCommand();
        assertTrue(sub.verbose);
    }

    @Test
    public void testInheritedOptionsAfterSubcommand() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(InheritGroup.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        Executor<?> executor = runtime.buildExecutor("igroup sub --verbose");
        Execution<?> execution = executor.getExecutions().get(0);
        execution.populateCommand();

        InheritSub sub = (InheritSub) execution.getCommand();
        assertTrue("inherited --verbose after subcommand should be true", sub.verbose);
    }

    @CommandDefinition(name = "sub", description = "")
    public static class InheritSub implements Command<CommandInvocation> {
        @Option(hasValue = false, inherited = true)
        boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "igroup", description = "", groupCommands = { InheritSub.class })
    public static class InheritGroup implements Command<CommandInvocation> {
        @Option(hasValue = false, inherited = true)
        boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    private void reset() {
        lastConnect = null;
        lastSubcommand = null;
        lastVerbose = false;
    }

    @Test
    public void testGroupCommandSubcommandAliases() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(AliasGroupCommand.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        // Test: resolve by primary name
        reset();
        runtime.executeCommand("agroup install");
        assertEquals("install", lastSubcommand);

        // Test: resolve by alias
        reset();
        runtime.executeCommand("agroup i");
        assertEquals("install", lastSubcommand);

        // Test: second subcommand by alias
        reset();
        runtime.executeCommand("agroup l");
        assertEquals("list", lastSubcommand);

        // Test: second subcommand by primary name
        reset();
        runtime.executeCommand("agroup list");
        assertEquals("list", lastSubcommand);
    }

    @CommandDefinition(name = "install", aliases = { "i" }, description = "Install something")
    public static class AliasInstallCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            lastSubcommand = "install";
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "list", aliases = { "l" }, description = "List things")
    public static class AliasListCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            lastSubcommand = "list";
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "agroup", description = "", groupCommands = { AliasInstallCommand.class, AliasListCommand.class })
    public static class AliasGroupCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
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

    @CommandDefinition(name = "cli", description = "", groupCommands = { VersionCommand.class, ChildCommand.class })
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
