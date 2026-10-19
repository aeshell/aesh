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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.aesh.command.container.CommandContainer;
import org.aesh.command.container.CommandContainerBuilder;
import org.aesh.command.impl.container.AeshCommandContainer;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.metadata.MetadataProviderRegistry;
import org.aesh.command.metadata.MetadataRegistry;
import org.aesh.command.metadata.MethodCommandMetadataProvider;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
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
        public CommandResult run(@Argument(description = "the file", paramLabel = "file") String file) {
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

    @MethodCommand(name = "unnamed", description = "unnamed option")
    public void unnamedOption(@Option(description = "t") String target) {
    }

    @Test
    public void testUnnamedOptionFailsFast() throws Exception {
        try {
            new AeshCommandContainerBuilder<CommandInvocation>().create(this,
                    MethodCommandTest.class.getMethod("unnamedOption", String.class));
            fail("expected failure for unnamed @Option");
        } catch (CommandLineParserException expected) {
            assertTrue(expected.getMessage().contains("explicit name"));
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
            new AeshCommandContainerBuilder<CommandInvocation>().create(new Object(), (java.lang.reflect.Method) null);
            fail("expected failure for null method");
        } catch (CommandLineParserException expected) {
        }
    }

    @Test
    public void testCreateByNameReflectiveFallback() throws Exception {
        DeployMethods methods = new DeployMethods();
        CommandContainer<CommandInvocation> container = new AeshCommandContainerBuilder<CommandInvocation>().create(methods,
                "deploy");
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder().command(container).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();

        assertEquals(CommandResult.SUCCESS,
                runtime.executeCommand("deploy --target prod --count 2"));
        assertEquals("prod", methods.seenTarget);
        assertEquals(2, methods.seenCount);
    }

    @Test
    public void testCreateByNameAlias() throws Exception {
        DeployMethods methods = new DeployMethods();
        CommandContainer<CommandInvocation> container = new AeshCommandContainerBuilder<CommandInvocation>().create(methods,
                "dep");
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder().command(container).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("dep --target prod"));
        assertEquals("prod", methods.seenTarget);
    }

    @Test
    public void testCreateByNameUnknown() {
        try {
            new AeshCommandContainerBuilder<CommandInvocation>().create(new DeployMethods(), "missing");
            fail("expected failure for unknown command name");
        } catch (CommandLineParserException expected) {
            assertTrue(expected.getMessage().contains("missing"));
        }
    }

    public static class DupMethods {
        @MethodCommand(name = "dup", description = "first")
        public void first() {
        }

        @MethodCommand(name = "dup", description = "second")
        public void second() {
        }
    }

    @Test
    public void testCreateByNameAmbiguous() {
        try {
            new AeshCommandContainerBuilder<CommandInvocation>().create(new DupMethods(), "dup");
            fail("expected failure for ambiguous command name");
        } catch (CommandLineParserException expected) {
            assertTrue(expected.getMessage().contains("Ambiguous"));
        }
    }

    private static final AtomicBoolean generatedUsed = new AtomicBoolean(false);

    private static class MarkerProvider implements MethodCommandMetadataProvider {
        @Override
        public Class<?> ownerType() {
            return DeployMethods.class;
        }

        @Override
        public String methodName() {
            return "deploy";
        }

        @Override
        public String commandName() {
            return "marked";
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ProcessedCommand buildProcessedCommand(Object owner) {
            generatedUsed.set(true);
            try {
                return ((ProcessedCommandBuilder) ProcessedCommandBuilder.builder())
                        .name("marked")
                        .description("marked")
                        .command(new NoopCommand())
                        .create();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class NoopCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    private static class MarkerRegistry implements MetadataRegistry {
        private final MethodCommandMetadataProvider provider;

        MarkerRegistry(MethodCommandMetadataProvider provider) {
            this.provider = provider;
        }

        @Override
        public org.aesh.command.metadata.CommandMetadataProvider<?> get(String commandClassName) {
            return null;
        }

        @Override
        public List<MethodCommandMetadataProvider> methodProviders(String ownerClassName) {
            return Collections.singletonList(provider);
        }
    }

    @Test
    public void testCreateByNamePrefersGenerated() throws Exception {
        generatedUsed.set(false);
        MarkerRegistry registry = new MarkerRegistry(new MarkerProvider());
        MetadataProviderRegistry.register(registry);
        try {
            CommandContainer<CommandInvocation> container = new AeshCommandContainerBuilder<CommandInvocation>()
                    .create(new DeployMethods(), "marked");
            CommandRegistry<CommandInvocation> commandRegistry = AeshCommandRegistryBuilder.builder().command(container)
                    .create();
            CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder().commandRegistry(commandRegistry)
                    .build();

            assertEquals(CommandResult.SUCCESS, runtime.executeCommand("marked"));
            assertTrue("generated provider must have built the container", generatedUsed.get());
        } finally {
            MetadataProviderRegistry.reset();
        }
    }

    public static class StaticMethods {
        static String seenTarget;

        @MethodCommand(name = "sdeploy", description = "static deploy", aliases = { "sdep" })
        public static void deploy(@Option(name = "target", description = "the target") String target) {
            seenTarget = target;
        }

        @MethodCommand(name = "priv", description = "private")
        private static void hidden() {
        }
    }

    public abstract static class AbstractMethods {
        @MethodCommand(name = "abs", description = "abstract")
        public abstract void run();
    }

    private CommandRuntime<CommandInvocation> runtimeForContainer(CommandContainer<CommandInvocation> container) {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder().command(container)
                .create();
        return AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();
    }

    @Test
    public void testCreateStaticByNameViaInterface() throws Exception {
        StaticMethods.seenTarget = null;
        // Typed as the interface: the method-command overloads must be part of the API, not just the impl
        CommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        CommandContainer<CommandInvocation> container = builder.create(StaticMethods.class, "sdeploy");
        CommandRuntime<CommandInvocation> runtime = runtimeForContainer(container);

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("sdeploy --target prod"));
        assertEquals("prod", StaticMethods.seenTarget);
    }

    @Test
    public void testCreateStaticByAlias() throws Exception {
        StaticMethods.seenTarget = null;
        CommandContainer<CommandInvocation> container = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(StaticMethods.class, "sdep");
        CommandRuntime<CommandInvocation> runtime = runtimeForContainer(container);

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("sdep --target dev"));
        assertEquals("dev", StaticMethods.seenTarget);
    }

    @Test
    public void testCreateByClassRejectsInstanceMethod() {
        try {
            new AeshCommandContainerBuilder<CommandInvocation>().create(DeployMethods.class, "deploy");
            fail("expected failure: deploy is not static");
        } catch (CommandLineParserException expected) {
            assertTrue(expected.getMessage().contains("No static @MethodCommand"));
        }
    }

    @Test
    public void testCreateByClassUnknown() {
        try {
            new AeshCommandContainerBuilder<CommandInvocation>().create(StaticMethods.class, "missing");
            fail("expected failure for unknown command name");
        } catch (CommandLineParserException expected) {
            assertTrue(expected.getMessage().contains("missing"));
        }
    }

    @Test
    public void testPrivateMethodFailsFast() throws Exception {
        try {
            new AeshCommandContainerBuilder<CommandInvocation>().create(null,
                    StaticMethods.class.getDeclaredMethod("hidden"));
            fail("expected failure for private method");
        } catch (CommandLineParserException expected) {
            assertTrue(expected.getMessage().contains("must not be private"));
        }
    }

    @Test
    public void testAbstractMethodFailsFast() throws Exception {
        AbstractMethods instance = new AbstractMethods() {
            @Override
            public void run() {
            }
        };
        try {
            new AeshCommandContainerBuilder<CommandInvocation>().create(instance,
                    AbstractMethods.class.getMethod("run"));
            fail("expected failure for abstract method");
        } catch (CommandLineParserException expected) {
            assertTrue(expected.getMessage().contains("must not be abstract"));
        }
    }

    public static class MultiMethods {
        List<String> seenIncludes;
        List<String> seenFiles;

        @MethodCommand(name = "build", description = "build it")
        public void build(@OptionList(name = "include", description = "includes") List<String> includes,
                @Arguments(description = "files", paramLabel = "file") List<String> files) {
            seenIncludes = includes;
            seenFiles = files;
        }
    }

    @Test
    public void testOptionListAndArguments() throws Exception {
        MultiMethods methods = new MultiMethods();
        CommandRuntime<CommandInvocation> runtime = runtimeFor(methods, "build", List.class, List.class);

        assertEquals(CommandResult.SUCCESS,
                runtime.executeCommand("build --include a --include b x.txt y.txt"));
        assertEquals(Arrays.asList("a", "b"), methods.seenIncludes);
        assertEquals(Arrays.asList("x.txt", "y.txt"), methods.seenFiles);
    }

    @Test
    public void testOptionListRepeatedExecutionResets() throws Exception {
        MultiMethods methods = new MultiMethods();
        CommandRuntime<CommandInvocation> runtime = runtimeFor(methods, "build", List.class, List.class);

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("build --include a x.txt"));
        // No --include on the second run: reset restores the initial null,
        // exactly like a null-starting List field on a field command
        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("build y.txt"));
        assertEquals(null, methods.seenIncludes);
        assertEquals(Arrays.asList("y.txt"), methods.seenFiles);
        // And values still flow on the third run: no stale state either way
        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("build --include b z.txt"));
        assertEquals(Arrays.asList("b"), methods.seenIncludes);
        assertEquals(Arrays.asList("z.txt"), methods.seenFiles);
    }

    @MethodCommand(name = "badargs", description = "bad args")
    public void badArgs(@Arguments(description = "x") String files) {
    }

    // NOTE: @OptionGroup/@Mixin rejection branches exist in both paths but are
    // not directly testable: both annotations are @Target(FIELD), so javac
    // rejects them on parameters before either path ever sees them. The
    // branches stay as defense-in-depth with a precise message if targets
    // ever widen.

    @MethodCommand(name = "badlist", description = "bad list")
    public void badList(@OptionList(name = "include", description = "x") String includes) {
    }

    @MethodCommand(name = "unnamedlist", description = "unnamed list")
    public void unnamedList(@OptionList(description = "x") List<String> includes) {
    }

    @MethodCommand(name = "nolabel", description = "no label")
    public void noLabel(@Argument(description = "x") String value) {
    }

    @MethodCommand(name = "nolabels", description = "no labels")
    public void noLabels(@Arguments(description = "x") List<String> values) {
    }

    @Test
    public void testMissingParamLabelFailsFast() throws Exception {
        try {
            new AeshCommandContainerBuilder<CommandInvocation>().create(this,
                    MethodCommandTest.class.getMethod("noLabel", String.class));
            fail("expected failure for missing @Argument paramLabel");
        } catch (CommandLineParserException expected) {
            assertTrue(expected.getMessage().contains("explicit paramLabel"));
        }
        try {
            new AeshCommandContainerBuilder<CommandInvocation>().create(this,
                    MethodCommandTest.class.getMethod("noLabels", List.class));
            fail("expected failure for missing @Arguments paramLabel");
        } catch (CommandLineParserException expected) {
            assertTrue(expected.getMessage().contains("explicit paramLabel"));
        }
    }

    @Test
    public void testNonCollectionMultiParamsFailFast() throws Exception {
        try {
            new AeshCommandContainerBuilder<CommandInvocation>().create(this,
                    MethodCommandTest.class.getMethod("badArgs", String.class));
            fail("expected failure for non-Collection @Arguments");
        } catch (CommandLineParserException expected) {
            assertTrue(expected.getMessage().contains("must be an instance of Collection"));
        }
        try {
            new AeshCommandContainerBuilder<CommandInvocation>().create(this,
                    MethodCommandTest.class.getMethod("badList", String.class));
            fail("expected failure for non-Collection @OptionList");
        } catch (CommandLineParserException expected) {
            assertTrue(expected.getMessage().contains("must be an instance of Collection"));
        }
    }

    @Test
    public void testUnnamedOptionListFailsFast() throws Exception {
        try {
            new AeshCommandContainerBuilder<CommandInvocation>().create(this,
                    MethodCommandTest.class.getMethod("unnamedList", List.class));
            fail("expected failure for unnamed @OptionList");
        } catch (CommandLineParserException expected) {
            assertTrue(expected.getMessage().contains("explicit name"));
        }
    }

    @CommandDefinition(name = "g", description = "group")
    public static class MethodGroup implements GroupCommand<CommandInvocation> {
        private final CommandContainer<CommandInvocation> child;

        MethodGroup(CommandContainer<CommandInvocation> child) {
            this.child = child;
        }

        @Override
        public List<Command<CommandInvocation>> getCommands() {
            return Collections.emptyList();
        }

        @Override
        public List<CommandContainer<CommandInvocation>> getParsedCommands() {
            return Collections.singletonList(child);
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testMethodContainerAsGroupChild() throws Exception {
        MultiMethods methods = new MultiMethods();
        CommandContainer<CommandInvocation> methodContainer = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(methods, "build");
        CommandContainer<CommandInvocation> groupContainer = new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new MethodGroup(methodContainer));
        CommandRuntime<CommandInvocation> runtime = runtimeForContainer(groupContainer);

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("g build --include a x.txt"));
        assertEquals(Arrays.asList("a"), methods.seenIncludes);
        assertEquals(Arrays.asList("x.txt"), methods.seenFiles);
    }
}
