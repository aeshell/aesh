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
package org.aesh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandLifecycle;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.DefaultValueProvider;
import org.aesh.command.GroupCommand;
import org.aesh.command.HelpEntry;
import org.aesh.command.HelpSectionProvider;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.activator.OptionActivatorProvider;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.container.CommandContainerBuilder;
import org.aesh.command.impl.LazyRouteResolver;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ParsedCommand;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.metadata.CommandMetadataProvider;
import org.aesh.command.metadata.MetadataProviderRegistry;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.parser.SubcommandNotFoundException;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.util.completer.ShellCompletionGenerator.ShellType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AeshRuntimeRunnerLazyStartupTest {

    @Before
    public void resetConstructionFlags() {
        LazyLeafCommand.constructed = false;
        LazyRootCommand.constructed = false;
        LazySelectedCommand.constructed = false;
        LazySkippedCommand.constructed = false;
        LazyNestedRootCommand.constructed = false;
        LazyNestedMidCommand.constructed = false;
        LazyNestedLeafCommand.constructed = false;
        LazyNestedOtherCommand.constructed = false;
        ProviderRoutedRoot.constructed = false;
        ProviderRoutedMid.constructed = false;
        ProviderRoutedLeaf.constructed = false;
        ProviderRoutedOther.constructed = false;
        ProviderDynRoot.constructed = false;
        ProviderDynGroup.constructed = false;
        ProviderBareGroup.constructed = false;
        ChainEvents.events.clear();
        ChainLeafCommand.sawMcfg = null;
        ChainActivationCounter.enhancements = 0;
        RecordingDefaultProvider.queried.clear();
        DefSelectedCommand.sawSmode = null;
        OwnSelectedCommand.sawMode = null;
        ValGoodCommand.constructed = false;
        InstanceRootCommand.constructions = 0;
        InstanceSelectedCommand.constructed = false;
        InstanceSkippedCommand.constructed = false;
        DynLazyChildCommand.constructed = false;
        HelpRootCommand.constructed = false;
        HelpMidCommand.constructed = false;
        HelpLeafCommand.constructed = false;
        HelpSkippedCommand.constructed = false;
        VersionCommand.constructed = false;
        SectionProvider.constructed = false;
        SectionRootCommand.constructed = false;
        SectionChildCommand.constructed = false;
        DynHelpChildCommand.constructed = false;
        HelpAsmRoot.constructed = false;
        HelpAsmChild.constructed = false;
    }

    @After
    public void resetMetadataRegistry() {
        MetadataProviderRegistry.reset();
    }

    @Test
    public void testCommandConstructionIsDeferredUntilExecute() {
        AeshRuntimeRunner runner = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(LazyLeafCommand.class)
                .args();

        assertFalse(LazyLeafCommand.constructed);
        assertEquals(CommandResult.SUCCESS, runner.execute());
        assertTrue(LazyLeafCommand.constructed);
    }

    @Test
    public void testOnlySelectedChildIsConstructed() {
        AeshRuntimeRunner runner = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(LazyRootCommand.class)
                .args("selected");

        assertFalse(LazyRootCommand.constructed);
        assertFalse(LazySelectedCommand.constructed);
        assertFalse(LazySkippedCommand.constructed);

        assertEquals(CommandResult.SUCCESS, runner.execute());

        assertTrue(LazyRootCommand.constructed);
        assertTrue(LazySelectedCommand.constructed);
        assertFalse(LazySkippedCommand.constructed);
    }

    @Test
    public void testInvalidCommandFailsAtExecute() {
        AeshRuntimeRunner runner = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(InvalidLazyCommand.class)
                .args();

        try {
            runner.execute();
            fail("Expected deferred command construction to fail");
        } catch (RuntimeException expected) {
        }
    }

    @Test
    public void testLazyStartupAfterRegistrationIsRejected() {
        AeshRuntimeRunner runner = AeshRuntimeRunner.builder()
                .command(LazyLeafCommand.class);

        try {
            runner.lazyStartup(true);
            fail("Expected late lazyStartup configuration to be rejected");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void testSecondRootCommandIsRejected() {
        AeshRuntimeRunner runner = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(LazyLeafCommand.class);

        try {
            runner.command(LazyLeafCommand.class);
            fail("Expected a second lazy root command to be rejected");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void testCustomAeshBuilderSupportedInLazyMode() {
        CountingContainerBuilder.calls = 0;

        CommandResult result = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .containerBuilder(new CountingContainerBuilder())
                .command(LazyRootCommand.class)
                .args("selected")
                .execute();

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals(2, CountingContainerBuilder.calls);
        assertTrue(LazySelectedCommand.constructed);
        assertFalse(LazySkippedCommand.constructed);
    }

    @Test
    public void testForeignContainerBuilderIsRejectedInLazyMode() {
        CommandContainerBuilder<CommandInvocation> foreign = new CommandContainerBuilder<CommandInvocation>() {
            @Override
            public CommandContainer<CommandInvocation> create(Command command) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CommandContainer<CommandInvocation> create(Class<? extends Command> command) {
                throw new UnsupportedOperationException();
            }
        };

        try {
            AeshRuntimeRunner.builder().lazyStartup(true).containerBuilder(foreign);
            fail("Expected a foreign container builder to be rejected");
        } catch (IllegalStateException expected) {
        }
        try {
            AeshRuntimeRunner.builder().containerBuilder(foreign).lazyStartup(true);
            fail("Expected a foreign container builder to be rejected");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void testRegistryDefaultValueProviderAppliesOnlyToMaterializedCommands() {
        CommandResult result = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(ProviderDefaultRoot.class)
                .defaultValueProvider(new RecordingDefaultProvider())
                .args("defselected")
                .execute();

        assertEquals(CommandResult.SUCCESS, result);
        assertTrue(RecordingDefaultProvider.queried.contains("defroot"));
        assertTrue(RecordingDefaultProvider.queried.contains("defselected"));
        assertFalse(RecordingDefaultProvider.queried.contains("defskipped"));
        assertEquals("DYN", DefSelectedCommand.sawSmode);
    }

    @Test
    public void testPerCommandDefaultValueProviderTakesPrecedence() {
        CommandResult result = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(OwnDefaultRoot.class)
                .defaultValueProvider(new RecordingDefaultProvider())
                .args("ownselected")
                .execute();

        assertEquals(CommandResult.SUCCESS, result);
        assertTrue(RecordingDefaultProvider.queried.contains("ownroot"));
        assertFalse(RecordingDefaultProvider.queried.contains("ownselected"));
        assertEquals("OWN", OwnSelectedCommand.sawMode);
    }

    @Test
    public void testMalformedSelectedChildSurfacesRealError() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream origErr = System.err;
        System.setErr(new PrintStream(err));
        try {
            CommandResult result = AeshRuntimeRunner.builder()
                    .lazyStartup(true)
                    .command(ValidationRootCommand.class)
                    .args("badchild")
                    .execute();

            assertEquals(CommandResult.USAGE_ERROR, result);
            assertTrue(err.toString().contains("overlap"));
        } finally {
            System.setErr(origErr);
        }
    }

    @Test
    public void testMalformedUnselectedSiblingDoesNotAffectExecution() {
        CommandResult result = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(ValidationRootCommand.class)
                .args("goodchild")
                .execute();

        assertEquals(CommandResult.SUCCESS, result);
        assertTrue(ValGoodCommand.constructed);
    }

    @Test
    public void testMalformedSelectedChildSurfacesRealErrorEager() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream origErr = System.err;
        System.setErr(new PrintStream(err));
        try {
            CommandResult result = AeshRuntimeRunner.builder()
                    .command(ValidationRootCommand.class)
                    .args("badchild")
                    .execute();

            assertEquals(CommandResult.USAGE_ERROR, result);
            assertTrue(err.toString().contains("overlap"));
        } finally {
            System.setErr(origErr);
        }
    }

    @Test
    public void testLazyInstanceRootIsReused() {
        InstanceRootCommand root = new InstanceRootCommand();
        assertEquals(1, InstanceRootCommand.constructions);

        CommandResult result = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(root)
                .args("iselected")
                .execute();

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals(1, InstanceRootCommand.constructions);
        assertTrue(InstanceSelectedCommand.constructed);
        assertFalse(InstanceSkippedCommand.constructed);
    }

    @Test
    public void testLazyStartupDisabledRestoresEagerTiming() {
        AeshRuntimeRunner runner = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(LazyLeafCommand.class);
        assertFalse(LazyLeafCommand.constructed);

        runner.lazyStartup(false);
        assertTrue(LazyLeafCommand.constructed);

        assertEquals(CommandResult.SUCCESS, runner.execute());
    }

    @Test
    public void testLazyExecutionWithOptionsAroundSubcommands() {
        CommandResult result = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(ChainRootCommand.class)
                .args("--rcfg=R", "mid", "--mcfg=M", "leaf")
                .execute();

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("M", ChainLeafCommand.sawMcfg);
    }

    @Test
    public void testLazyHelpRendersFullTree() {
        final CommandResult[] result = new CommandResult[1];
        String output = captureStdout(() -> result[0] = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(LazyRootCommand.class)
                .args("--help")
                .execute());

        assertEquals(CommandResult.SUCCESS, result[0]);
        assertTrue(output.contains("selected"));
        assertTrue(output.contains("skipped"));
    }

    @Test
    public void testLazyGenerateCompletionCoversFullTree() {
        String output = captureStdout(() -> AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(LazyRootCommand.class)
                .generateCompletion(ShellType.BASH)
                .execute());

        assertTrue(output.contains("selected"));
        assertTrue(output.contains("skipped"));
    }

    @Test
    public void testLazyDynamicGroupFallsBack() {
        CommandResult result = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(DynLazyRootCommand.class)
                .args("dynchild")
                .execute();

        assertEquals(CommandResult.SUCCESS, result);
        assertTrue(DynLazyChildCommand.constructed);
    }

    @Test
    public void testLazyHelpRendersWithoutConstruction() {
        final CommandResult[] lazyResult = new CommandResult[1];
        String lazyOutput = captureStdout(() -> lazyResult[0] = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(HelpRootCommand.class)
                .args("--help")
                .execute());

        assertEquals(CommandResult.SUCCESS, lazyResult[0]);
        assertFalse(HelpRootCommand.constructed);
        assertFalse(HelpMidCommand.constructed);
        assertFalse(HelpLeafCommand.constructed);
        assertFalse(HelpSkippedCommand.constructed);

        resetHelpFlags();
        final CommandResult[] eagerResult = new CommandResult[1];
        String eagerOutput = captureStdout(() -> eagerResult[0] = AeshRuntimeRunner.builder()
                .command(HelpRootCommand.class)
                .args("--help")
                .execute());

        assertEquals(CommandResult.SUCCESS, eagerResult[0]);
        assertEquals(eagerOutput, lazyOutput);
    }

    @Test
    public void testLazyHelpWithInstanceRoot() {
        HelpRootCommand root = new HelpRootCommand();
        assertTrue(HelpRootCommand.constructed);

        final CommandResult[] lazyResult = new CommandResult[1];
        String lazyOutput = captureStdout(() -> lazyResult[0] = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(root)
                .args("--help")
                .execute());

        assertEquals(CommandResult.SUCCESS, lazyResult[0]);
        assertFalse(HelpMidCommand.constructed);
        assertFalse(HelpLeafCommand.constructed);
        assertFalse(HelpSkippedCommand.constructed);

        resetHelpFlags();
        HelpRootCommand.constructed = true;
        final CommandResult[] eagerResult = new CommandResult[1];
        String eagerOutput = captureStdout(() -> eagerResult[0] = AeshRuntimeRunner.builder()
                .command(new HelpRootCommand())
                .args("--help")
                .execute());

        assertEquals(CommandResult.SUCCESS, eagerResult[0]);
        assertEquals(eagerOutput, lazyOutput);
    }

    @Test
    public void testLazyNestedHelpRendersWithoutConstruction() {
        assertNestedHelpEqualsEager("helpmid", "--help");
        assertNestedHelpEqualsEager("helpmid", "helpleaf", "--help");
    }

    private static void assertNestedHelpEqualsEager(String... helpArgs) {
        final CommandResult[] lazyResult = new CommandResult[1];
        String lazyOutput = captureStdout(() -> lazyResult[0] = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(HelpRootCommand.class)
                .args(helpArgs)
                .execute());

        assertEquals(CommandResult.SUCCESS, lazyResult[0]);
        assertFalse(HelpRootCommand.constructed);
        assertFalse(HelpMidCommand.constructed);
        assertFalse(HelpLeafCommand.constructed);
        assertFalse(HelpSkippedCommand.constructed);

        resetHelpFlags();
        final CommandResult[] eagerResult = new CommandResult[1];
        String eagerOutput = captureStdout(() -> eagerResult[0] = AeshRuntimeRunner.builder()
                .command(HelpRootCommand.class)
                .args(helpArgs)
                .execute());

        assertEquals(CommandResult.SUCCESS, eagerResult[0]);
        assertEquals(eagerOutput, lazyOutput);
        resetHelpFlags();
    }

    @Test
    public void testLazyVersionRendersWithoutConstruction() {
        final CommandResult[] lazyResult = new CommandResult[1];
        String lazyOutput = captureStdout(() -> lazyResult[0] = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(VersionCommand.class)
                .args("--version")
                .execute());

        assertEquals(CommandResult.SUCCESS, lazyResult[0]);
        assertFalse(VersionCommand.constructed);

        VersionCommand.constructed = false;
        final CommandResult[] eagerResult = new CommandResult[1];
        String eagerOutput = captureStdout(() -> eagerResult[0] = AeshRuntimeRunner.builder()
                .command(VersionCommand.class)
                .args("--version")
                .execute());

        assertEquals(CommandResult.SUCCESS, eagerResult[0]);
        assertEquals(eagerOutput, lazyOutput);
    }

    @Test
    public void testLazyHelpWithCustomSections() {
        final CommandResult[] lazyResult = new CommandResult[1];
        String lazyOutput = captureStdout(() -> lazyResult[0] = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(SectionRootCommand.class)
                .args("--help")
                .execute());

        assertEquals(CommandResult.SUCCESS, lazyResult[0]);
        assertTrue(SectionProvider.constructed);
        assertFalse(SectionRootCommand.constructed);
        assertFalse(SectionChildCommand.constructed);
        assertTrue(lazyOutput.contains("TEST-HEADER"));
        assertTrue(lazyOutput.contains("TEST-FOOTER"));
        assertTrue(lazyOutput.contains("plug"));

        SectionProvider.constructed = false;
        SectionRootCommand.constructed = false;
        SectionChildCommand.constructed = false;
        final CommandResult[] eagerResult = new CommandResult[1];
        String eagerOutput = captureStdout(() -> eagerResult[0] = AeshRuntimeRunner.builder()
                .command(SectionRootCommand.class)
                .args("--help")
                .execute());

        assertEquals(CommandResult.SUCCESS, eagerResult[0]);
        assertEquals(eagerOutput, lazyOutput);
    }

    @Test
    public void testLazyHelpDocFormatFallsBack() {
        final CommandResult[] lazyResult = new CommandResult[1];
        String lazyOutput = captureStdout(() -> lazyResult[0] = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(HelpRootCommand.class)
                .args("--help=markdown")
                .execute());

        resetHelpFlags();
        final CommandResult[] eagerResult = new CommandResult[1];
        String eagerOutput = captureStdout(() -> eagerResult[0] = AeshRuntimeRunner.builder()
                .command(HelpRootCommand.class)
                .args("--help=markdown")
                .execute());

        assertEquals(eagerResult[0], lazyResult[0]);
        assertEquals(eagerOutput, lazyOutput);
    }

    @Test
    public void testLazyHelpDynamicGroupFallsBack() {
        final CommandResult[] lazyResult = new CommandResult[1];
        String lazyOutput = captureStdout(() -> lazyResult[0] = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(DynHelpRootCommand.class)
                .args("--help")
                .execute());

        resetHelpFlags();
        final CommandResult[] eagerResult = new CommandResult[1];
        String eagerOutput = captureStdout(() -> eagerResult[0] = AeshRuntimeRunner.builder()
                .command(DynHelpRootCommand.class)
                .args("--help")
                .execute());

        assertEquals(eagerResult[0], lazyResult[0]);
        assertEquals(eagerOutput, lazyOutput);
    }

    @Test
    public void testLazyHelpViaProvider() {
        MetadataProviderRegistry.register(className -> {
            if (className.equals(HelpAsmRoot.class.getName()))
                return new DelegatingHelpProvider<>(HelpAsmRoot.class, "asmroot",
                        new Class[] { HelpAsmChild.class }, new String[][] { { "asmchild" } });
            return null;
        });

        final CommandResult[] lazyResult = new CommandResult[1];
        String lazyOutput = captureStdout(() -> lazyResult[0] = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(HelpAsmRoot.class)
                .args("--help")
                .execute());

        assertEquals(CommandResult.SUCCESS, lazyResult[0]);
        assertFalse(HelpAsmRoot.constructed);
        assertFalse(HelpAsmChild.constructed);
        assertTrue(lazyOutput.contains("asmchild"));
    }

    private static void resetHelpFlags() {
        HelpRootCommand.constructed = false;
        HelpMidCommand.constructed = false;
        HelpLeafCommand.constructed = false;
        HelpSkippedCommand.constructed = false;
    }

    private static String captureStdout(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return baos.toString();
    }

    @Test
    public void testUnknownSubcommandFailsWithoutConstruction() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream origErr = System.err;
        System.setErr(new PrintStream(err));
        try {
            CommandResult result = AeshRuntimeRunner.builder()
                    .lazyStartup(true)
                    .command(LazyRootCommand.class)
                    .args("bogus")
                    .execute();

            assertEquals(CommandResult.USAGE_ERROR, result);
            assertFalse(LazyRootCommand.constructed);
            assertFalse(LazySelectedCommand.constructed);
            assertFalse(LazySkippedCommand.constructed);
            String output = err.toString();
            assertTrue(output.contains("bogus"));
            assertTrue(output.contains("selected"));
        } finally {
            System.setErr(origErr);
        }
    }

    @Test
    public void testUnknownSubcommandHandlerReceivesAvailableNames() {
        final AtomicReference<String> capturedUnknown = new AtomicReference<>();
        final AtomicReference<Collection<String>> capturedAvailable = new AtomicReference<>();

        CommandResult result = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(LazyRootCommand.class)
                .commandNotFoundHandler(new org.aesh.command.CommandNotFoundHandler() {
                    @Override
                    public void handleCommandNotFound(String line,
                            java.util.function.Consumer<String> output) {
                    }

                    @Override
                    public void handleCommandNotFound(String line,
                            java.util.function.Consumer<String> output,
                            String unknownCommand,
                            Collection<String> availableCommands) {
                        capturedUnknown.set(unknownCommand);
                        capturedAvailable.set(availableCommands);
                    }
                })
                .args("bogus")
                .execute();

        assertEquals(CommandResult.USAGE_ERROR, result);
        assertEquals("bogus", capturedUnknown.get());
        assertNotNull(capturedAvailable.get());
        assertTrue(capturedAvailable.get().contains("selected"));
        assertFalse(LazyRootCommand.constructed);
        assertFalse(LazySelectedCommand.constructed);
        assertFalse(LazySkippedCommand.constructed);
    }

    @Test
    public void testDeepNestedUnknownSubcommandFailsWithoutConstruction() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream origErr = System.err;
        System.setErr(new PrintStream(err));
        try {
            CommandResult result = AeshRuntimeRunner.builder()
                    .lazyStartup(true)
                    .command(LazyNestedRootCommand.class)
                    .args("mid", "bogusleaf")
                    .execute();

            assertEquals(CommandResult.USAGE_ERROR, result);
            assertFalse(LazyNestedRootCommand.constructed);
            assertFalse(LazyNestedMidCommand.constructed);
            assertFalse(LazyNestedLeafCommand.constructed);
            assertFalse(LazyNestedOtherCommand.constructed);
            String output = err.toString();
            assertTrue(output.contains("bogusleaf"));
            assertTrue(output.contains("leaf"));
        } finally {
            System.setErr(origErr);
        }
    }

    @Test
    public void testSubcommandAliasExecutesWithoutConstructingSiblings() {
        CommandResult result = AeshRuntimeRunner.builder()
                .lazyStartup(true)
                .command(LazyRootCommand.class)
                .args("sel")
                .execute();

        assertEquals(CommandResult.SUCCESS, result);
        assertTrue(LazyRootCommand.constructed);
        assertTrue(LazySelectedCommand.constructed);
        assertFalse(LazySkippedCommand.constructed);
    }

    @Test
    public void testResolverValidatesWithoutConstruction() throws Exception {
        LazyRouteResolver.validate(LazyRootCommand.class, new String[] { "selected" });
        assertFalse(LazyRootCommand.constructed);
        assertFalse(LazySelectedCommand.constructed);
        assertFalse(LazySkippedCommand.constructed);

        try {
            LazyRouteResolver.validate(LazyRootCommand.class, new String[] { "bogus" });
            fail("Expected unknown subcommand to fail validation");
        } catch (SubcommandNotFoundException e) {
            assertEquals("bogus", e.getUnknownSubcommand());
            assertTrue(e.getAvailableSubcommands().contains("selected"));
        }
        assertFalse(LazyRootCommand.constructed);
        assertFalse(LazySelectedCommand.constructed);
        assertFalse(LazySkippedCommand.constructed);
    }

    @Test
    public void testProviderRouteResolvesWithoutConstruction() throws Exception {
        registerProviderFixtures();

        LazyRouteResolver.validate(ProviderRoutedRoot.class, new String[] { "m", "leaf" });
        assertFalse(ProviderRoutedRoot.constructed);
        assertFalse(ProviderRoutedMid.constructed);
        assertFalse(ProviderRoutedLeaf.constructed);
        assertFalse(ProviderRoutedOther.constructed);

        try {
            LazyRouteResolver.validate(ProviderRoutedRoot.class, new String[] { "bogus" });
            fail("Expected unknown subcommand to fail validation");
        } catch (SubcommandNotFoundException e) {
            assertEquals("bogus", e.getUnknownSubcommand());
            assertTrue(e.getAvailableSubcommands().contains("mid"));
            assertTrue(e.getAvailableSubcommands().contains("other"));
        }

        try {
            LazyRouteResolver.validate(ProviderRoutedRoot.class, new String[] { "mid", "bogus" });
            fail("Expected unknown nested subcommand to fail validation");
        } catch (SubcommandNotFoundException e) {
            assertEquals("mid", e.getParentCommand());
            assertEquals("bogus", e.getUnknownSubcommand());
            assertTrue(e.getAvailableSubcommands().contains("leaf"));
        }
        assertFalse(ProviderRoutedRoot.constructed);
        assertFalse(ProviderRoutedMid.constructed);
        assertFalse(ProviderRoutedLeaf.constructed);
        assertFalse(ProviderRoutedOther.constructed);
    }

    @Test
    public void testProviderRouteSkipsDynamicGroups() throws Exception {
        registerProviderFixtures();

        LazyRouteResolver.validate(ProviderDynRoot.class, new String[] { "dyn", "anything" });
        assertFalse(ProviderDynGroup.constructed);

        try {
            LazyRouteResolver.validate(ProviderDynRoot.class, new String[] { "bogus" });
            fail("Expected unknown subcommand to fail validation");
        } catch (SubcommandNotFoundException e) {
            assertEquals("bogus", e.getUnknownSubcommand());
            assertTrue(e.getAvailableSubcommands().contains("dyn"));
        }
    }

    @Test
    public void testProviderRouteWithoutNamesIsUndetermined() throws Exception {
        registerProviderFixtures();

        LazyRouteResolver.validate(ProviderBareGroup.class, new String[] { "bogus" });
        assertFalse(ProviderBareGroup.constructed);
    }

    @Test
    public void testAncestorChainMaterializesInOrder() throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> containerBuilder = new AeshCommandContainerBuilder<>();
        containerBuilder.setLazyChildResolution(true);
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .containerBuilder(containerBuilder)
                .command(ChainRootCommand.class)
                .create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(registry)
                .build();

        assertEquals(CommandResult.SUCCESS,
                runtime.executeCommand("chainroot --rcfg=R mid --mcfg=M leaf"));

        assertEquals(Arrays.asList(
                "construct:root", "beforeParse:root",
                "construct:mid", "beforeParse:mid",
                "construct:leaf", "beforeParse:leaf",
                "afterParse:root", "afterParse:leaf"),
                ChainEvents.events);

        CommandLineParser<CommandInvocation> mid = registry.getCommand("chainroot", "")
                .getParser().getChildParser("mid");
        assertEquals("R", ((ChainMidCommand) mid.getProcessedCommand().getCommand()).rcfg);
        assertEquals("M", ChainLeafCommand.sawMcfg);
    }

    @Test
    public void testInvocationProvidersReachDepthThree() throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> containerBuilder = new AeshCommandContainerBuilder<>();
        containerBuilder.setLazyChildResolution(true);
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .containerBuilder(containerBuilder)
                .command(ProviderChainRoot.class)
                .create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(registry)
                .optionActivatorProvider(new ChainActivationCounter())
                .build();

        runtime.executeCommand("proot pmid pleaf");

        assertEquals(3, ChainActivationCounter.enhancements);
    }

    private static void registerProviderFixtures() {
        MetadataProviderRegistry.register(className -> {
            if (className.equals(ProviderRoutedRoot.class.getName()))
                return new ProviderRoutedRootProvider();
            if (className.equals(ProviderRoutedMid.class.getName()))
                return new ProviderRoutedMidProvider();
            if (className.equals(ProviderRoutedLeaf.class.getName()))
                return new ProviderRoutedLeafProvider();
            if (className.equals(ProviderRoutedOther.class.getName()))
                return new ProviderRoutedOtherProvider();
            if (className.equals(ProviderDynRoot.class.getName()))
                return new ProviderDynRootProvider();
            if (className.equals(ProviderDynGroup.class.getName()))
                return new ProviderDynGroupProvider();
            if (className.equals(ProviderBareGroup.class.getName()))
                return new ProviderBareGroupProvider();
            return null;
        });
    }

    @CommandDefinition(name = "lazy-leaf", description = "lazy leaf command")
    public static class LazyLeafCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public LazyLeafCommand() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "lazy-root", description = "lazy root command", generateHelp = true, groupCommands = {
            LazySelectedCommand.class, LazySkippedCommand.class })
    public static class LazyRootCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public LazyRootCommand() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "selected", description = "selected lazy child", aliases = { "sel" })
    public static class LazySelectedCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public LazySelectedCommand() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "skipped", description = "unselected lazy child")
    public static class LazySkippedCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public LazySkippedCommand() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class InvalidLazyCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "lazy-nested-root", description = "lazy nested root", groupCommands = {
            LazyNestedMidCommand.class, LazyNestedOtherCommand.class })
    public static class LazyNestedRootCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public LazyNestedRootCommand() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mid", description = "lazy nested mid", groupCommands = {
            LazyNestedLeafCommand.class })
    public static class LazyNestedMidCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public LazyNestedMidCommand() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "leaf", description = "lazy nested leaf")
    public static class LazyNestedLeafCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public LazyNestedLeafCommand() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "other", description = "lazy nested other")
    public static class LazyNestedOtherCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public LazyNestedOtherCommand() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class ProviderRoutedRoot implements Command<CommandInvocation> {
        static boolean constructed;

        public ProviderRoutedRoot() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class ProviderRoutedMid implements Command<CommandInvocation> {
        static boolean constructed;

        public ProviderRoutedMid() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class ProviderRoutedLeaf implements Command<CommandInvocation> {
        static boolean constructed;

        public ProviderRoutedLeaf() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class ProviderRoutedOther implements Command<CommandInvocation> {
        static boolean constructed;

        public ProviderRoutedOther() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class ProviderDynRoot implements Command<CommandInvocation> {
        static boolean constructed;

        public ProviderDynRoot() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class ProviderDynGroup implements GroupCommand<CommandInvocation> {
        static boolean constructed;

        public ProviderDynGroup() {
            constructed = true;
        }

        @Override
        public List<Command<CommandInvocation>> getCommands() {
            return Collections.emptyList();
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class ProviderBareGroup implements Command<CommandInvocation> {
        static boolean constructed;

        public ProviderBareGroup() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "helproot", description = "help root", generateHelp = true, groupCommands = {
            HelpMidCommand.class, HelpSkippedCommand.class })
    public static class HelpRootCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public HelpRootCommand() {
            constructed = true;
        }

        @Option(name = "mode", aliases = { "m" }, defaultValue = "fast", description = "Operation mode")
        private String mode;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "helpmid", description = "help mid", generateHelp = true, groupCommands = {
            HelpLeafCommand.class })
    public static class HelpMidCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public HelpMidCommand() {
            constructed = true;
        }

        @Option(name = "level", description = "Nesting level")
        private String level;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "helpleaf", description = "help leaf", generateHelp = true)
    public static class HelpLeafCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public HelpLeafCommand() {
            constructed = true;
        }

        @Argument(description = "Target path")
        private String target;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "helpskipped", description = "help skipped", generateHelp = true)
    public static class HelpSkippedCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public HelpSkippedCommand() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "vercmd", description = "version command", version = "3.1")
    public static class VersionCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public VersionCommand() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class SectionProvider implements HelpSectionProvider {
        static boolean constructed;

        public SectionProvider() {
            constructed = true;
        }

        @Override
        public String getHeader() {
            return "TEST-HEADER";
        }

        @Override
        public String getFooter() {
            return "TEST-FOOTER";
        }

        @Override
        public Map<String, List<HelpEntry>> getAdditionalSections() {
            return Collections.singletonMap("Plugins",
                    Collections.singletonList(new HelpEntry("plug", "A plugin")));
        }
    }

    @CommandDefinition(name = "secroot", description = "section root", generateHelp = true, helpSectionProvider = SectionProvider.class, groupCommands = {
            SectionChildCommand.class })
    public static class SectionRootCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public SectionRootCommand() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "secchild", description = "section child", generateHelp = true)
    public static class SectionChildCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public SectionChildCommand() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "dynhelproot", description = "dynamic help root", generateHelp = true)
    public static class DynHelpRootCommand implements GroupCommand<CommandInvocation> {
        @Override
        public List<Command<CommandInvocation>> getCommands() {
            return Collections.singletonList(new DynHelpChildCommand());
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "dynhelpchild", description = "dynamic help child")
    public static class DynHelpChildCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public DynHelpChildCommand() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "asmroot", description = "assembly root", generateHelp = true, groupCommands = {
            HelpAsmChild.class })
    public static class HelpAsmRoot implements Command<CommandInvocation> {
        static boolean constructed;

        public HelpAsmRoot() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "asmchild", description = "assembly child", generateHelp = true)
    public static class HelpAsmChild implements Command<CommandInvocation> {
        static boolean constructed;

        public HelpAsmChild() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    private static class DelegatingHelpProvider<C extends Command> implements CommandMetadataProvider<C> {
        private final Class<C> commandType;
        private final String name;
        private final Class<? extends Command>[] children;
        private final String[][] names;

        DelegatingHelpProvider(Class<C> commandType, String name,
                Class<? extends Command>[] children, String[][] names) {
            this.commandType = commandType;
            this.name = name;
            this.children = children;
            this.names = names;
        }

        @Override
        public Class<C> commandType() {
            return commandType;
        }

        @Override
        public C newInstance() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProcessedCommand buildProcessedCommand(C instance) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isGroupCommand() {
            return children.length > 0;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Command>[] groupCommandClasses() {
            return children;
        }

        @Override
        public String commandName() {
            return name;
        }

        @Override
        public String[][] groupCommandNamesAndAliases() {
            return names;
        }

        @Override
        @SuppressWarnings("unchecked")
        public ProcessedCommand buildHelpProcessedCommand() {
            try {
                return new AeshCommandContainerBuilder<CommandInvocation>()
                        .buildHelpProcessedCommand(commandType);
            } catch (CommandLineParserException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private abstract static class RouteStubProvider<C extends Command>
            implements CommandMetadataProvider<C> {
        @Override
        public C newInstance() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProcessedCommand buildProcessedCommand(C instance) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Class<? extends Command>[] groupCommandClasses() {
            return new Class[0];
        }
    }

    private static class ProviderRoutedRootProvider extends RouteStubProvider<ProviderRoutedRoot> {
        @Override
        public Class<ProviderRoutedRoot> commandType() {
            return ProviderRoutedRoot.class;
        }

        @Override
        public boolean isGroupCommand() {
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Command>[] groupCommandClasses() {
            return new Class[] { ProviderRoutedMid.class, ProviderRoutedOther.class };
        }

        @Override
        public String commandName() {
            return "proot";
        }

        @Override
        public String[][] groupCommandNamesAndAliases() {
            return new String[][] { { "mid", "m" }, { "other" } };
        }
    }

    private static class ProviderRoutedMidProvider extends RouteStubProvider<ProviderRoutedMid> {
        @Override
        public Class<ProviderRoutedMid> commandType() {
            return ProviderRoutedMid.class;
        }

        @Override
        public boolean isGroupCommand() {
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Command>[] groupCommandClasses() {
            return new Class[] { ProviderRoutedLeaf.class };
        }

        @Override
        public String commandName() {
            return "mid";
        }

        @Override
        public String[][] groupCommandNamesAndAliases() {
            return new String[][] { { "leaf" } };
        }
    }

    private static class ProviderRoutedLeafProvider extends RouteStubProvider<ProviderRoutedLeaf> {
        @Override
        public Class<ProviderRoutedLeaf> commandType() {
            return ProviderRoutedLeaf.class;
        }

        @Override
        public boolean isGroupCommand() {
            return false;
        }

        @Override
        public String commandName() {
            return "leaf";
        }
    }

    private static class ProviderRoutedOtherProvider extends RouteStubProvider<ProviderRoutedOther> {
        @Override
        public Class<ProviderRoutedOther> commandType() {
            return ProviderRoutedOther.class;
        }

        @Override
        public boolean isGroupCommand() {
            return false;
        }

        @Override
        public String commandName() {
            return "other";
        }
    }

    private static class ProviderDynRootProvider extends RouteStubProvider<ProviderDynRoot> {
        @Override
        public Class<ProviderDynRoot> commandType() {
            return ProviderDynRoot.class;
        }

        @Override
        public boolean isGroupCommand() {
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Command>[] groupCommandClasses() {
            return new Class[] { ProviderDynGroup.class };
        }

        @Override
        public String commandName() {
            return "dynroot";
        }

        @Override
        public String[][] groupCommandNamesAndAliases() {
            return new String[][] { { "dyn" } };
        }
    }

    private static class ProviderDynGroupProvider extends RouteStubProvider<ProviderDynGroup> {
        @Override
        public Class<ProviderDynGroup> commandType() {
            return ProviderDynGroup.class;
        }

        @Override
        public boolean isGroupCommand() {
            return true;
        }

        @Override
        public String commandName() {
            return "dyn";
        }
    }

    private static class ProviderBareGroupProvider extends RouteStubProvider<ProviderBareGroup> {
        @Override
        public Class<ProviderBareGroup> commandType() {
            return ProviderBareGroup.class;
        }

        @Override
        public boolean isGroupCommand() {
            return true;
        }

        @Override
        public String commandName() {
            return "bare";
        }
    }

    private static class ChainEvents {
        private static final List<String> events = new ArrayList<>();
    }

    @CommandDefinition(name = "chainroot", description = "chain root", groupCommands = {
            ChainMidCommand.class, ChainOtherCommand.class })
    public static class ChainRootCommand implements Command<CommandInvocation>, CommandLifecycle {
        @Option(name = "rcfg", inherited = true, description = "root config")
        private String rcfg;

        public ChainRootCommand() {
            ChainEvents.events.add("construct:root");
        }

        @Override
        public void beforeParse() {
            ChainEvents.events.add("beforeParse:root");
        }

        @Override
        public void afterParse() {
            ChainEvents.events.add("afterParse:root");
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mid", description = "chain mid", groupCommands = {
            ChainLeafCommand.class })
    public static class ChainMidCommand implements Command<CommandInvocation>, CommandLifecycle {
        @Option(name = "rcfg", inherited = true, description = "root config passthrough")
        private String rcfg;
        @Option(name = "mcfg", inherited = true, description = "mid config")
        private String mcfg;

        public ChainMidCommand() {
            ChainEvents.events.add("construct:mid");
        }

        @Override
        public void beforeParse() {
            ChainEvents.events.add("beforeParse:mid");
        }

        @Override
        public void afterParse() {
            ChainEvents.events.add("afterParse:mid");
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "leaf", description = "chain leaf")
    public static class ChainLeafCommand implements Command<CommandInvocation>, CommandLifecycle {
        private String rcfg;
        private String mcfg;

        static String sawMcfg;

        public ChainLeafCommand() {
            ChainEvents.events.add("construct:leaf");
        }

        @Override
        public void beforeParse() {
            ChainEvents.events.add("beforeParse:leaf");
        }

        @Override
        public void afterParse() {
            ChainEvents.events.add("afterParse:leaf");
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            sawMcfg = mcfg;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "other", description = "chain other")
    public static class ChainOtherCommand implements Command<CommandInvocation> {
        public ChainOtherCommand() {
            ChainEvents.events.add("construct:other");
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "proot", description = "provider chain root", groupCommands = {
            ProviderChainMid.class, ProviderChainOther.class })
    public static class ProviderChainRoot implements Command<CommandInvocation> {
        @Option(name = "ropt", activator = ChainActivator.class, description = "root option")
        private String ropt;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "pmid", description = "provider chain mid", groupCommands = {
            ProviderChainLeaf.class })
    public static class ProviderChainMid implements Command<CommandInvocation> {
        @Option(name = "mopt", activator = ChainActivator.class, description = "mid option")
        private String mopt;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "pleaf", description = "provider chain leaf")
    public static class ProviderChainLeaf implements Command<CommandInvocation> {
        @Option(name = "lopt", activator = ChainActivator.class, description = "leaf option")
        private String lopt;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "pother", description = "provider chain other")
    public static class ProviderChainOther implements Command<CommandInvocation> {
        @Option(name = "oopt", activator = ChainActivator.class, description = "other option")
        private String oopt;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class ChainActivator implements OptionActivator {
        @Override
        public boolean isActivated(ParsedCommand parsedCommand) {
            return true;
        }
    }

    private static class ChainActivationCounter implements OptionActivatorProvider {
        static int enhancements;

        @Override
        public OptionActivator enhanceOptionActivator(OptionActivator activator) {
            enhancements++;
            return activator;
        }
    }

    @CommandDefinition(name = "defroot", description = "default provider root", groupCommands = {
            DefSelectedCommand.class, DefSkippedCommand.class })
    public static class ProviderDefaultRoot implements Command<CommandInvocation> {
        @Option(name = "rmode", description = "root mode")
        private String rmode;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "defselected", description = "default provider selected")
    public static class DefSelectedCommand implements Command<CommandInvocation> {
        @Option(name = "smode", description = "selected mode")
        private String smode;

        static String sawSmode;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            sawSmode = smode;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "defskipped", description = "default provider skipped")
    public static class DefSkippedCommand implements Command<CommandInvocation> {
        @Option(name = "kmode", description = "skipped mode")
        private String kmode;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "ownroot", description = "own provider root", groupCommands = {
            OwnSelectedCommand.class })
    public static class OwnDefaultRoot implements Command<CommandInvocation> {
        @Option(name = "cfg", description = "root config")
        private String cfg;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "ownselected", description = "own provider selected", defaultValueProvider = FixedDefaultProvider.class)
    public static class OwnSelectedCommand implements Command<CommandInvocation> {
        @Option(name = "mode", description = "selected mode")
        private String mode;

        static String sawMode;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            sawMode = mode;
            return CommandResult.SUCCESS;
        }
    }

    private static class RecordingDefaultProvider implements DefaultValueProvider {
        static final List<String> queried = new ArrayList<>();

        @Override
        public String defaultValue(ProcessedOption option) {
            if (option.name().equals("rmode") || option.name().equals("smode")
                    || option.name().equals("kmode") || option.name().equals("cfg")) {
                queried.add(option.parent().name());
                return "DYN";
            }
            return null;
        }
    }

    public static class FixedDefaultProvider implements DefaultValueProvider {
        @Override
        public String defaultValue(ProcessedOption option) {
            return option.name().equals("mode") ? "OWN" : null;
        }
    }

    public static class CountingContainerBuilder extends AeshCommandContainerBuilder<CommandInvocation> {
        static int calls;

        @Override
        public CommandContainer<CommandInvocation> create(Class<? extends Command> command)
                throws CommandLineParserException {
            calls++;
            return super.create(command);
        }
    }

    @CommandDefinition(name = "instroot", description = "instance root", groupCommands = {
            InstanceSelectedCommand.class, InstanceSkippedCommand.class })
    public static class InstanceRootCommand implements Command<CommandInvocation> {
        static int constructions;

        public InstanceRootCommand() {
            constructions++;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "iselected", description = "instance selected")
    public static class InstanceSelectedCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public InstanceSelectedCommand() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "iskipped", description = "instance skipped")
    public static class InstanceSkippedCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public InstanceSkippedCommand() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "dynlazy", description = "dynamic lazy group")
    public static class DynLazyRootCommand implements GroupCommand<CommandInvocation> {
        @Override
        public List<Command<CommandInvocation>> getCommands() {
            return Collections.singletonList(new DynLazyChildCommand());
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "dynchild", description = "dynamic lazy child")
    public static class DynLazyChildCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public DynLazyChildCommand() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "valroot", description = "validation root", groupCommands = {
            ValGoodCommand.class, ValBadCommand.class })
    public static class ValidationRootCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "goodchild", description = "valid child")
    public static class ValGoodCommand implements Command<CommandInvocation> {
        static boolean constructed;

        public ValGoodCommand() {
            constructed = true;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "badchild", description = "malformed child")
    public static class ValBadCommand implements Command<CommandInvocation> {
        @Argument(index = "0..1", description = "overlapping argument")
        private String first;

        @Arguments(index = "1..*", description = "overlapping arguments")
        private List<String> rest;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }
}
