package org.aesh.command;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
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
