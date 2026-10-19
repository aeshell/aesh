/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.List;

import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.container.AeshCommandContainer;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.console.DefaultAeshContext;
import org.aesh.terminal.formatting.TerminalString;
import org.junit.Test;

/**
 * Parity tests for {@code @MethodCommand} method commands: the same
 * behavior as field-based declarations (options, required, defaults,
 * aliases, help, completion, errors) through method parameters.
 */
public class MethodCommandTest {

    public static class DeployMethods {
        String seenTarget;
        int seenCount;
        boolean seenVerbose;
        String seenMode = "unset";
        CommandInvocation seenInvocation;

        @MethodCommand(name = "deploy", description = "deploy it", aliases = { "dep" })
        public void deploy(@Option(name = "target", description = "the target") String target,
                @Option(name = "count", description = "how many") int count,
                @Option(name = "verbose", description = "chatty", hasValue = false) boolean verbose,
                @Option(name = "mode", description = "mode", defaultValue = "fast") String mode,
                CommandInvocation invocation) {
            seenTarget = target;
            seenCount = count;
            seenVerbose = verbose;
            seenMode = mode;
            seenInvocation = invocation;
        }

        @MethodCommand(description = "run it")
        public CommandResult run(@Argument(description = "the file") String file) {
            seenTarget = file;
            return CommandResult.SUCCESS;
        }

        @MethodCommand(name = "failer", description = "fails")
        public CommandResult failer() {
            return CommandResult.FAILURE;
        }

        @MethodCommand(name = "boomer", description = "throws")
        public void boomer() throws CommandException {
            throw new CommandException("boom");
        }

        @MethodCommand(name = "need", description = "required opt")
        public void required(@Option(name = "must", description = "required", required = true) String must) {
            seenTarget = must;
        }
    }

    private CommandRuntime<CommandInvocation> runtimeFor(Object instance, String methodName,
            Class<?>... paramTypes) throws Exception {
        Method method = instance.getClass().getMethod(methodName, paramTypes);
        CommandContainer<CommandInvocation> container = new AeshCommandContainerBuilder<CommandInvocation>().create(instance,
                method);
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder().command(container).create();
        return AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();
    }

    @Test
    public void testBasicOptionsAndInvocationPassthrough() throws Exception {
        DeployMethods methods = new DeployMethods();
        CommandRuntime<CommandInvocation> runtime = runtimeFor(methods, "deploy",
                String.class, int.class, boolean.class, String.class, CommandInvocation.class);

        assertEquals(CommandResult.SUCCESS,
                runtime.executeCommand("deploy --target prod --count 3 --verbose"));

        assertEquals("prod", methods.seenTarget);
        assertEquals(3, methods.seenCount);
        assertEquals(true, methods.seenVerbose);
        assertEquals("fast", methods.seenMode);
        assertNotNull(methods.seenInvocation);
    }

    @Test
    public void testAliasAndDefaults() throws Exception {
        DeployMethods methods = new DeployMethods();
        CommandRuntime<CommandInvocation> runtime = runtimeFor(methods, "deploy",
                String.class, int.class, boolean.class, String.class, CommandInvocation.class);

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("dep --target dev"));

        assertEquals("dev", methods.seenTarget);
        assertEquals(0, methods.seenCount);
        assertEquals(false, methods.seenVerbose);
        assertEquals("fast", methods.seenMode);
    }

    @Test
    public void testDerivedName() throws Exception {
        DeployMethods methods = new DeployMethods();
        CommandRuntime<CommandInvocation> runtime = runtimeFor(methods, "run", String.class);

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("run app.txt"));
        assertEquals("app.txt", methods.seenTarget);
    }

    @Test
    public void testCommandResultReturn() throws Exception {
        DeployMethods methods = new DeployMethods();
        CommandRuntime<CommandInvocation> runtime = runtimeFor(methods, "failer");

        assertEquals(CommandResult.FAILURE, runtime.executeCommand("failer"));
    }

    @Test
    public void testMethodExceptionPropagates() throws Exception {
        DeployMethods methods = new DeployMethods();
        CommandRuntime<CommandInvocation> runtime = runtimeFor(methods, "boomer");

        try {
            runtime.executeCommand("boomer");
            fail("expected CommandException");
        } catch (CommandException expected) {
            assertEquals("boom", expected.getMessage());
        }
    }

    @Test
    public void testMissingRequiredFails() throws Exception {
        DeployMethods methods = new DeployMethods();
        CommandRuntime<CommandInvocation> runtime = runtimeFor(methods, "required", String.class);

        try {
            runtime.executeCommand("need");
            fail("expected validation failure for missing required option");
        } catch (org.aesh.command.parser.RequiredOptionException expected) {
            // required violation surfaces as a parse/validation failure, not success
        }
    }

    @Test
    public void testHelpRendersOptions() throws Exception {
        DeployMethods methods = new DeployMethods();
        Method method = DeployMethods.class.getMethod("deploy",
                String.class, int.class, boolean.class, String.class, CommandInvocation.class);
        CommandContainer<CommandInvocation> container = new AeshCommandContainerBuilder<CommandInvocation>().create(methods,
                method);

        String help = container.getParser().printHelp();
        assertTrue(help.contains("--target"));
        assertTrue(help.contains("--count"));
        assertTrue(help.contains("--verbose"));
        assertTrue(help.contains("deploy it"));
    }

    @Test
    public void testCompletionCandidates() throws Exception {
        DeployMethods methods = new DeployMethods();
        Method method = DeployMethods.class.getMethod("deploy",
                String.class, int.class, boolean.class, String.class, CommandInvocation.class);
        CommandContainer<CommandInvocation> container = new AeshCommandContainerBuilder<CommandInvocation>().create(methods,
                method);

        AeshCompleteOperation co = new AeshCompleteOperation(new DefaultAeshContext(), "deploy --", 9);
        container.getParser().complete(co,
                org.aesh.command.settings.SettingsBuilder.builder().build().invocationProviders());
        boolean found = false;
        for (TerminalString candidate : co.getCompletionCandidates()) {
            if (candidate.getCharacters().contains("--target"))
                found = true;
        }
        assertTrue("expected --target among candidates", found);
    }

    @Test
    public void testGroupAttach() throws Exception {
        DeployMethods methods = new DeployMethods();
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        CommandContainer<CommandInvocation> groupContainer = builder.create(ParentGroup.class);
        Method method = DeployMethods.class.getMethod("deploy",
                String.class, int.class, boolean.class, String.class, CommandInvocation.class);
        ((AeshCommandContainer<CommandInvocation>) groupContainer)
                .addChild(builder.create(methods, method));
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder().command(groupContainer).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();

        assertEquals(CommandResult.SUCCESS,
                runtime.executeCommand("parent deploy --target prod"));
        assertEquals("prod", methods.seenTarget);
    }

    @CommandDefinition(name = "parent", description = "parent group")
    public static class ParentGroup implements GroupCommand<CommandInvocation> {
        @Override
        public List<Command<CommandInvocation>> getCommands() {
            return java.util.Collections.emptyList();
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testMissingAnnotationFailsFast() {
        try {
            new AeshCommandContainerBuilder<CommandInvocation>().create(new Object(), toStringMethod());
            fail("expected failure for unannotated method");
        } catch (CommandLineParserException expected) {
        } catch (NoSuchMethodException e) {
            fail("test setup broken: " + e.getMessage());
        }
    }

    private Method toStringMethod() throws NoSuchMethodException {
        return Object.class.getMethod("toString");
    }

    public String badReturn(@Option(name = "x", description = "x") String x) {
        return x;
    }

    @Test
    public void testBadReturnTypeFailsFast() throws Exception {
        try {
            new AeshCommandContainerBuilder<CommandInvocation>().create(this,
                    MethodCommandTest.class.getMethod("badReturn", String.class));
            fail("expected failure for String return type");
        } catch (CommandLineParserException expected) {
        }
    }

    @MethodCommand(name = "unannotated", description = "has a bare param")
    public void bareParam(String value) {
    }

    @Test
    public void testBareParameterFailsFast() throws Exception {
        try {
            new AeshCommandContainerBuilder<CommandInvocation>().create(this,
                    MethodCommandTest.class.getMethod("bareParam", String.class));
            fail("expected failure for unannotated parameter");
        } catch (CommandLineParserException expected) {
        }
    }

    @MethodCommand(name = "instanceless", description = "needs an instance")
    public void needsInstance() {
    }

    @Test
    public void testNullInstanceFailsFastForInstanceMethod() throws Exception {
        try {
            new AeshCommandContainerBuilder<CommandInvocation>().create(null,
                    MethodCommandTest.class.getMethod("needsInstance"));
            fail("expected failure for null instance");
        } catch (CommandLineParserException expected) {
        }
    }

    @MethodCommand(name = "staticcmd", description = "static command")
    public static void staticCommand(@Option(name = "x", description = "x") String x) {
        seenStatic = x;
    }

    private static String seenStatic;

    @Test
    public void testStaticMethodWithNullInstance() throws Exception {
        seenStatic = null;
        Method method = MethodCommandTest.class.getMethod("staticCommand", String.class);
        CommandContainer<CommandInvocation> container = new AeshCommandContainerBuilder<CommandInvocation>().create(null,
                method);
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder().command(container).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("staticcmd --x hello"));
        assertEquals("hello", seenStatic);
    }

    @Test
    public void testNullMethodFailsFast() {
        try {
            new AeshCommandContainerBuilder<CommandInvocation>().create(new Object(), null);
            fail("expected failure for null method");
        } catch (CommandLineParserException expected) {
        }
    }
}
