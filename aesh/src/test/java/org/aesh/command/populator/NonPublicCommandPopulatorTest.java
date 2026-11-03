/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.aesh.command.populator;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
import org.junit.Test;

/**
 * The options and arguments are injected into a command whose class is not public and has no no-arg constructor: a
 * package-private or private class that takes what it needs in its constructor, or an anonymous subclass.
 */
public class NonPublicCommandPopulatorTest {

    @Test
    public void testPackagePrivateClassWithConstructorArgument() throws Exception {
        Greet greet = new Greet("Hello");
        execute(greet, "--name", "world", "--tags", "a,b", "loud");

        assertEquals("world", greet.name);
        assertEquals(Arrays.asList("a", "b"), greet.tags);
        assertEquals("loud", greet.mode);
    }

    @Test
    public void testPrivateClassWithConstructorArgument() throws Exception {
        PrivateGreet greet = new PrivateGreet("Hello");
        execute(greet, "--name", "world");

        assertEquals("world", greet.name);
    }

    @Test
    public void testAnonymousSubclass() throws Exception {
        PublicGreet greet = new PublicGreet("Hello") {
        };
        execute(greet, "--name", "world", "--tags", "a", "quiet");

        assertEquals("world", greet.name);
        assertEquals(Arrays.asList("a"), greet.tags);
        assertEquals("quiet", greet.mode);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void execute(Command command, String... args) throws Exception {
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(AeshCommandRegistryBuilder.<CommandInvocation> builder().command(command).create())
                .build();
        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("greet", args));
    }

    @CommandDefinition(name = "greet", description = "")
    static class Greet implements Command<CommandInvocation> {
        private final String greeting;

        @Option
        String name;

        @OptionList
        List<String> tags;

        @Argument
        String mode;

        Greet(String greeting) {
            this.greeting = greeting;
        }

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return greeting != null ? CommandResult.SUCCESS : CommandResult.FAILURE;
        }
    }

    @CommandDefinition(name = "greet", description = "")
    private static class PrivateGreet implements Command<CommandInvocation> {
        private final String greeting;

        @Option
        String name;

        private PrivateGreet(String greeting) {
            this.greeting = greeting;
        }

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return greeting != null ? CommandResult.SUCCESS : CommandResult.FAILURE;
        }
    }

    @CommandDefinition(name = "greet", description = "")
    public static class PublicGreet extends Greet {
        public PublicGreet(String greeting) {
            super(greeting);
        }
    }
}
