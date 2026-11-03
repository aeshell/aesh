package org.aesh.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
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

    // --- Bare group with inherited options and lazy children (#714) ---

    @CommandDefinition(name = "lazychild", description = "A lazy child")
    public static class LazyChildCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "lazygroup", description = "", groupCommands = { LazyChildCommand.class })
    public static class LazyInheritGroup implements Command<CommandInvocation> {
        static boolean ran;

        @Option(name = "verbose", hasValue = false, inherited = true, description = "verbose")
        boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            ran = true;
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testBareGroupWithInheritedOptionAndLazyChildren() throws Exception {
        // Used to NPE in propagateInheritedOptions: with lazy children the
        // group has no resolved child parsers. The bare group executes itself.
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        builder.setLazyChildResolution(true);
        CommandContainer<CommandInvocation> group = builder.create(new LazyInheritGroup());
        assertTrue(group.getParser().getChildParsers() == null
                || group.getParser().getChildParsers().isEmpty());

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(group).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(registry).build();

        LazyInheritGroup.ran = false;
        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("lazygroup"));
        assertTrue(LazyInheritGroup.ran);
    }

    @CommandDefinition(name = "leaf", description = "A nested leaf")
    public static class NestLeafCommand implements Command<CommandInvocation> {
        static boolean ran;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            ran = true;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "nestmid", description = "", groupCommands = { NestLeafCommand.class })
    public static class NestMidGroup implements Command<CommandInvocation> {
        static boolean ran;
        static String levelValue;

        @Option(name = "level", inherited = true, description = "level")
        String level;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            ran = true;
            levelValue = level;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "nestroot", description = "", groupCommands = { NestMidGroup.class })
    public static class NestRootGroup implements Command<CommandInvocation> {
        @Option(name = "verbose", hasValue = false, inherited = true, description = "verbose")
        boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testNestedGroupWithoutLeafAndLazyChildren() throws Exception {
        // Selecting the mid group without a leaf recurses through
        // propagateDownSelectedPath; the mid level has no resolved children.
        // The inherited option must carry a value to reach the child loop
        // (null values are skipped before it).
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        builder.setLazyChildResolution(true);
        CommandContainer<CommandInvocation> group = builder.create(new NestRootGroup());

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(group).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(registry).build();

        NestMidGroup.ran = false;
        NestMidGroup.levelValue = null;
        NestLeafCommand.ran = false;
        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("nestroot nestmid --level x"));
        assertTrue(NestMidGroup.ran);
        assertEquals("x", NestMidGroup.levelValue);
        assertTrue(!NestLeafCommand.ran);
    }
}
