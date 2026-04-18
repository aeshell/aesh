package org.aesh.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
