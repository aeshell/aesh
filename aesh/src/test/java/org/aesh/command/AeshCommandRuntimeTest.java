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
}
