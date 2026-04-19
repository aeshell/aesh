package org.aesh.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.junit.Test;

public class AeshCommandRuntimeTest {

    private static StringBuilder builder = new StringBuilder();

    @Test
    public void executeCommands() throws CommandRegistryException, CommandException, OptionValidatorException, IOException,
            InterruptedException, CommandLineParserException, CommandValidatorException, CommandNotFoundException {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder().command(TestCommand.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();

        runtime.executeCommand("test foo", "test bar", "test bart");

        assertEquals("foobarbart", builder.toString());

    }

    @Test
    public void buildExecutorWithArgs() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(OptCommand.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        Executor<?> executor = runtime.buildExecutor("opt", new String[] { "-n", "Alice", "hello" });
        assertNotNull(executor);
        assertEquals(1, executor.getExecutions().size());

        Execution<?> execution = executor.getExecutions().get(0);
        execution.populateCommand();
        Command<?> cmd = execution.getCommand();
        assertNotNull(cmd);

        OptCommand optCmd = (OptCommand) cmd;
        assertEquals("Alice", optCmd.name);
        assertEquals("hello", optCmd.arg);
    }

    @Test
    public void buildExecutorWithSpecialChars() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(OptCommand.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        Executor<?> executor = runtime.buildExecutor("opt",
                new String[] { "-n", "Hello \"World\"", "arg with spaces" });
        Execution<?> execution = executor.getExecutions().get(0);
        execution.populateCommand();

        OptCommand optCmd = (OptCommand) execution.getCommand();
        assertEquals("Hello \"World\"", optCmd.name);
        assertEquals("arg with spaces", optCmd.arg);
    }

    @Test
    public void buildExecutorWithNoArgs() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(OptCommand.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        Executor<?> executor = runtime.buildExecutor("opt", null);
        assertNotNull(executor);
        assertEquals(1, executor.getExecutions().size());
    }

    @Test
    public void testBeforeParseLifecycle() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(LifecycleCommand.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        // First execution: set verbose
        runtime.executeCommand("lifecycle --verbose");
        assertTrue(LifecycleCommand.globalVerbose);

        // Second execution: no --verbose, but beforeParse should have reset it
        runtime.executeCommand("lifecycle");
        assertFalse(LifecycleCommand.globalVerbose);
    }

    @Test
    public void testBeforeParseWithBuildExecutor() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(LifecycleCommand.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        // First call: set verbose via pre-tokenized args
        Executor<?> executor1 = runtime.buildExecutor("lifecycle", new String[] { "--verbose" });
        executor1.getExecutions().get(0).populateCommand();
        LifecycleCommand cmd1 = (LifecycleCommand) executor1.getExecutions().get(0).getCommand();
        assertTrue(cmd1.verbose);
        // buildExecutor doesn't run execute(), so set global state manually
        LifecycleCommand.globalVerbose = cmd1.verbose;
        assertTrue(LifecycleCommand.globalVerbose);

        // Second call: no --verbose, beforeParse should reset global state
        Executor<?> executor2 = runtime.buildExecutor("lifecycle", new String[] {});
        executor2.getExecutions().get(0).populateCommand();
        LifecycleCommand cmd2 = (LifecycleCommand) executor2.getExecutions().get(0).getCommand();
        assertFalse(cmd2.verbose);
        assertFalse(LifecycleCommand.globalVerbose);
    }

    @CommandDefinition(name = "lifecycle", description = "")
    public static class LifecycleCommand implements Command<CommandInvocation>, CommandLifecycle {
        @Option(hasValue = false)
        boolean verbose;

        static boolean globalVerbose = false;

        @Override
        public void beforeParse() {
            globalVerbose = false;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            globalVerbose = verbose;
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testAfterParseLifecycle() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(AfterParseCommand.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        Executor<?> executor = runtime.buildExecutor("afterparse",
                new String[] { "script.java", "arg1", "arg2" });
        executor.getExecutions().get(0).populateCommand();
        AfterParseCommand cmd = (AfterParseCommand) executor.getExecutions().get(0).getCommand();

        // afterParse() should have split: first arg -> scriptFile, rest stays in params
        assertEquals("script.java", cmd.scriptFile);
        assertEquals(2, cmd.params.size());
        assertEquals("arg1", cmd.params.get(0));
        assertEquals("arg2", cmd.params.get(1));
    }

    @CommandDefinition(name = "afterparse", description = "")
    public static class AfterParseCommand implements Command<CommandInvocation>, CommandLifecycle {
        @org.aesh.command.option.Arguments
        java.util.List<String> params;

        String scriptFile;

        @Override
        public void afterParse() {
            if (params != null && !params.isEmpty()) {
                scriptFile = params.remove(0);
            }
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testBooleanWrapperResetsToNull() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(BooleanWrapperCommand.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        // First: set --flag
        Executor<?> executor1 = runtime.buildExecutor("boolwrap", new String[] { "--flag" });
        executor1.getExecutions().get(0).populateCommand();
        BooleanWrapperCommand cmd1 = (BooleanWrapperCommand) executor1.getExecutions().get(0).getCommand();
        assertEquals(Boolean.TRUE, cmd1.flag);

        // Second: no --flag, should reset to null (not FALSE)
        Executor<?> executor2 = runtime.buildExecutor("boolwrap", new String[] {});
        executor2.getExecutions().get(0).populateCommand();
        BooleanWrapperCommand cmd2 = (BooleanWrapperCommand) executor2.getExecutions().get(0).getCommand();
        assertNull(cmd2.flag);
    }

    @CommandDefinition(name = "boolwrap", description = "")
    public static class BooleanWrapperCommand implements Command<CommandInvocation> {
        @Option(hasValue = false)
        Boolean flag;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testInitializedListResetsToEmptyNotNull() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(InitializedListCommand.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        // First: provide arguments
        Executor<?> executor1 = runtime.buildExecutor("initlist", new String[] { "a", "b" });
        executor1.getExecutions().get(0).populateCommand();
        InitializedListCommand cmd1 = (InitializedListCommand) executor1.getExecutions().get(0).getCommand();
        assertEquals(2, cmd1.params.size());

        // Second: no arguments — should reset to empty list, not null
        Executor<?> executor2 = runtime.buildExecutor("initlist", new String[] {});
        executor2.getExecutions().get(0).populateCommand();
        InitializedListCommand cmd2 = (InitializedListCommand) executor2.getExecutions().get(0).getCommand();
        assertNotNull(cmd2.params);
        assertEquals(0, cmd2.params.size());
    }

    @Test
    public void testInitializedStringResetsToInitialValue() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(InitializedStringCommand.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        // First: override the default
        Executor<?> executor1 = runtime.buildExecutor("initstr", new String[] { "--name", "override" });
        executor1.getExecutions().get(0).populateCommand();
        InitializedStringCommand cmd1 = (InitializedStringCommand) executor1.getExecutions().get(0).getCommand();
        assertEquals("override", cmd1.name);

        // Second: no --name — should restore to "default", not null
        Executor<?> executor2 = runtime.buildExecutor("initstr", new String[] {});
        executor2.getExecutions().get(0).populateCommand();
        InitializedStringCommand cmd2 = (InitializedStringCommand) executor2.getExecutions().get(0).getCommand();
        assertEquals("default", cmd2.name);
    }

    @CommandDefinition(name = "initlist", description = "")
    public static class InitializedListCommand implements Command<CommandInvocation> {
        @org.aesh.command.option.Arguments
        java.util.List<String> params = new java.util.ArrayList<>();

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "initstr", description = "")
    public static class InitializedStringCommand implements Command<CommandInvocation> {
        @Option
        String name = "default";

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testArgumentAndArgumentsCoexist() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(SplitArgsCommand.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        // First positional goes to @Argument, rest go to @Arguments
        Executor<?> executor = runtime.buildExecutor("splitargs",
                new String[] { "script.java", "arg1", "arg2" });
        executor.getExecutions().get(0).populateCommand();
        SplitArgsCommand cmd = (SplitArgsCommand) executor.getExecutions().get(0).getCommand();
        assertEquals("script.java", cmd.scriptFile);
        assertNotNull(cmd.params);
        assertEquals(2, cmd.params.size());
        assertEquals("arg1", cmd.params.get(0));
        assertEquals("arg2", cmd.params.get(1));
    }

    @Test
    public void testArgumentAndArgumentsOnlyFirst() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(SplitArgsCommand.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        // Only one positional — goes to @Argument, @Arguments stays empty
        Executor<?> executor = runtime.buildExecutor("splitargs",
                new String[] { "script.java" });
        executor.getExecutions().get(0).populateCommand();
        SplitArgsCommand cmd = (SplitArgsCommand) executor.getExecutions().get(0).getCommand();
        assertEquals("script.java", cmd.scriptFile);
        assertNotNull(cmd.params);
        assertEquals(0, cmd.params.size());
    }

    @Test
    public void testArgumentAndArgumentsNone() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(SplitArgsCommand.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        // No positionals — both empty/null
        Executor<?> executor = runtime.buildExecutor("splitargs", new String[] {});
        executor.getExecutions().get(0).populateCommand();
        SplitArgsCommand cmd = (SplitArgsCommand) executor.getExecutions().get(0).getCommand();
        assertNull(cmd.scriptFile);
    }

    @CommandDefinition(name = "splitargs", description = "")
    public static class SplitArgsCommand implements Command<CommandInvocation> {
        @Argument(description = "Script file")
        String scriptFile;

        @org.aesh.command.option.Arguments(description = "Script arguments")
        java.util.List<String> params = new java.util.ArrayList<>();

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "opt", description = "")
    public static class OptCommand implements Command<CommandInvocation> {
        @Option(shortName = 'n')
        String name;

        @Argument
        String arg;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "test", description = "")
    public static class TestCommand implements Command<CommandInvocation> {

        @Argument
        private String arg;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            builder.append(arg);
            return CommandResult.SUCCESS;
        }
    }

    // --- Mixin tests ---

    @Test
    public void testMixinOptionsAreParsed() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(MixinCommand.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        Executor<?> executor = runtime.buildExecutor("mixcmd",
                new String[] { "--verbose", "--output", "result.txt" });
        executor.getExecutions().get(0).populateCommand();
        MixinCommand cmd = (MixinCommand) executor.getExecutions().get(0).getCommand();

        assertNotNull(cmd.logging);
        assertTrue(cmd.logging.verbose);
        assertEquals("result.txt", cmd.output);
    }

    @Test
    public void testMixinResetBetweenParses() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(MixinCommand.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        // First parse: set verbose
        runtime.executeCommand("mixcmd --verbose --output first.txt");

        // Second parse: no verbose — mixin field should be reset
        Executor<?> executor = runtime.buildExecutor("mixcmd",
                new String[] { "--output", "second.txt" });
        executor.getExecutions().get(0).populateCommand();
        MixinCommand cmd = (MixinCommand) executor.getExecutions().get(0).getCommand();

        assertNotNull(cmd.logging);
        assertFalse(cmd.logging.verbose);
        assertEquals("second.txt", cmd.output);
    }

    @Test
    public void testMixinWithInitializedField() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(MixinWithDefaultsCommand.class).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        // Parse with level override
        Executor<?> executor1 = runtime.buildExecutor("mixdefaults",
                new String[] { "--level", "DEBUG" });
        executor1.getExecutions().get(0).populateCommand();
        MixinWithDefaultsCommand cmd1 = (MixinWithDefaultsCommand) executor1.getExecutions().get(0).getCommand();

        assertNotNull(cmd1.config);
        assertEquals("DEBUG", cmd1.config.level);

        // Second parse without level — should reset to initializer value "INFO"
        runtime.executeCommand("mixdefaults --level DEBUG");
        Executor<?> executor2 = runtime.buildExecutor("mixdefaults", new String[] {});
        executor2.getExecutions().get(0).populateCommand();
        MixinWithDefaultsCommand cmd2 = (MixinWithDefaultsCommand) executor2.getExecutions().get(0).getCommand();

        assertNotNull(cmd2.config);
        assertEquals("INFO", cmd2.config.level);
    }

    public static class LoggingMixin {
        @Option(hasValue = false, description = "Enable verbose output")
        boolean verbose;
    }

    @CommandDefinition(name = "mixcmd", description = "")
    public static class MixinCommand implements Command<CommandInvocation> {
        @org.aesh.command.option.Mixin
        LoggingMixin logging;

        @Option(description = "Output file")
        String output;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class ConfigMixin {
        @Option(description = "Log level")
        String level = "INFO";
    }

    @CommandDefinition(name = "mixdefaults", description = "")
    public static class MixinWithDefaultsCommand implements Command<CommandInvocation> {
        @org.aesh.command.option.Mixin
        ConfigMixin config;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }
}
