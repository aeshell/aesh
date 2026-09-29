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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.aesh.command.activator.OptionActivator;
import org.aesh.command.activator.OptionActivatorProvider;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.converter.ConverterInvocationProvider;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.validator.OptionValidator;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.command.validator.ValidatorInvocationProvider;
import org.aesh.console.AeshContext;
import org.junit.Test;

/**
 * A custom {@link InvocationProviders} composite supplied through
 * {@code SettingsBuilder.invocationProviders()} must survive the
 * {@code AeshCommandRuntimeBuilder.settings()} path instead of being
 * rebuilt from (null) individuals (#649).
 */
public class InvocationProvidersSettingsTest {

    @CommandDefinition(name = "echo", description = "echoes its option")
    public static class EchoCommand implements Command<CommandInvocation> {
        @Option(name = "value", description = "value")
        String value;

        static volatile String seen;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            seen = value;
            return CommandResult.SUCCESS;
        }
    }

    public static class PassValidator implements OptionValidator {
        @Override
        @SuppressWarnings("rawtypes")
        public void validate(ValidatorInvocation validatorInvocation) {
        }
    }

    @CommandDefinition(name = "validated", description = "option with a real validator")
    public static class ValidatedCommand implements Command<CommandInvocation> {
        @Option(name = "value", description = "value", validator = PassValidator.class)
        String value;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "gated", description = "option with an activator")
    public static class GatedCommand implements Command<CommandInvocation> {
        @Option(name = "value", description = "value", activator = AlwaysOnActivator.class)
        String value;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class AlwaysOnActivator implements OptionActivator {
        @Override
        public boolean isActivated(
                org.aesh.command.impl.internal.ParsedCommand parsedCommand) {
            return true;
        }
    }

    static class UppercaseConverterProvider implements ConverterInvocationProvider {
        @Override
        public ConverterInvocation enhanceConverterInvocation(ConverterInvocation invocation) {
            return new ConverterInvocation() {
                @Override
                public String getInput() {
                    return invocation.getInput().toUpperCase();
                }

                @Override
                public AeshContext getAeshContext() {
                    return invocation.getAeshContext();
                }
            };
        }
    }

    static class RecordingValidatorProvider implements ValidatorInvocationProvider {
        static volatile boolean called;

        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public ValidatorInvocation enhanceValidatorInvocation(ValidatorInvocation invocation) {
            called = true;
            return invocation;
        }
    }

    static class RecordingOptionActivatorProvider implements OptionActivatorProvider {
        static volatile boolean called;

        @Override
        public OptionActivator enhanceOptionActivator(OptionActivator activator) {
            called = true;
            return activator;
        }
    }

    private static CommandRegistry<CommandInvocation> registryWith(Class<? extends Command>... commands)
            throws Exception {
        AeshCommandRegistryBuilder<CommandInvocation> builder = AeshCommandRegistryBuilder
                .<CommandInvocation> builder();
        for (Class<? extends Command> command : commands)
            builder.command(command);
        return builder.create();
    }

    @Test
    public void testCustomCompositeSurvivesSettingsPath() throws Exception {
        ConverterInvocationProvider customConverter = new UppercaseConverterProvider();
        InvocationProviders composite = new AeshInvocationProviders(customConverter, null, null, null, null);
        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(registryWith(EchoCommand.class))
                .invocationProviders(composite)
                .build();

        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .settings(settings)
                .build();

        assertSame("custom converter must survive the settings path", customConverter,
                runtime.invocationProviders().getConverterProvider());
        assertSame("untouched composite must be exposed as-is", composite, runtime.invocationProviders());
    }

    @Test
    public void testCustomConverterTransformsThroughRuntime() throws Exception {
        EchoCommand.seen = null;
        InvocationProviders composite = new AeshInvocationProviders(new UppercaseConverterProvider(),
                null, null, null, null);
        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(registryWith(EchoCommand.class))
                .invocationProviders(composite)
                .build();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .settings(settings)
                .build();

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("echo --value hello"));
        assertEquals("custom converter must run during population", "HELLO", EchoCommand.seen);
    }

    @Test
    public void testCustomValidatorHookRunsThroughRuntime() throws Exception {
        RecordingValidatorProvider.called = false;
        InvocationProviders composite = new AeshInvocationProviders(null, null,
                new RecordingValidatorProvider(), null, null);
        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(registryWith(ValidatedCommand.class))
                .invocationProviders(composite)
                .build();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .settings(settings)
                .build();

        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("validated --value x"));
        assertTrue("custom validator provider must run during population",
                RecordingValidatorProvider.called);
    }

    @Test
    public void testCustomActivatorHookRunsAtBuild() throws Exception {
        RecordingOptionActivatorProvider.called = false;
        InvocationProviders composite = new AeshInvocationProviders(null, null, null,
                new RecordingOptionActivatorProvider(), null);
        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(registryWith(GatedCommand.class))
                .invocationProviders(composite)
                .build();

        AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .settings(settings)
                .build();

        assertTrue("custom activator provider must run when providers are applied",
                RecordingOptionActivatorProvider.called);
    }

    @Test
    public void testIndividualSetterOverridesCompositeSlot() throws Exception {
        ConverterInvocationProvider baseConverter = new UppercaseConverterProvider();
        ConverterInvocationProvider overrideConverter = new ConverterInvocationProvider() {
        };
        InvocationProviders composite = new AeshInvocationProviders(baseConverter, null, null, null, null);
        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(registryWith(EchoCommand.class))
                .invocationProviders(composite)
                .build();

        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .settings(settings)
                .converterInvocationProvider(overrideConverter)
                .build();

        assertSame("individual setter after settings() wins its slot", overrideConverter,
                runtime.invocationProviders().getConverterProvider());
        assertSame("other slots still come from the composite",
                composite.getValidatorProvider(), runtime.invocationProviders().getValidatorProvider());
    }

    @Test
    public void testSettingsAfterIndividualReplacesAll() throws Exception {
        ConverterInvocationProvider stale = new ConverterInvocationProvider() {
        };
        InvocationProviders composite = new AeshInvocationProviders(new UppercaseConverterProvider(),
                null, null, null, null);
        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .commandRegistry(registryWith(EchoCommand.class))
                .invocationProviders(composite)
                .build();

        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .converterInvocationProvider(stale)
                .settings(settings)
                .build();

        assertSame("settings() replaces the whole composite", composite, runtime.invocationProviders());
    }

    @Test
    public void testDirectCompositeSetter() throws Exception {
        InvocationProviders composite = new AeshInvocationProviders(new UppercaseConverterProvider(),
                null, null, null, null);

        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(registryWith(EchoCommand.class))
                .invocationProviders(composite)
                .build();

        assertSame(composite, runtime.invocationProviders());
    }

    @Test
    public void testDefaultsUnchangedWithoutCustomProviders() throws Exception {
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(registryWith(EchoCommand.class))
                .build();

        assertNotNull(runtime.invocationProviders());
        assertNotNull(runtime.invocationProviders().getConverterProvider());
        assertNotNull(runtime.invocationProviders().getCompleterProvider());
        assertNotNull(runtime.invocationProviders().getValidatorProvider());
        assertNotNull(runtime.invocationProviders().getOptionActivatorProvider());
        assertNotNull(runtime.invocationProviders().getCommandActivatorProvider());

        EchoCommand.seen = null;
        assertEquals(CommandResult.SUCCESS, runtime.executeCommand("echo --value hello"));
        assertEquals("default conversion passes input through", "hello", EchoCommand.seen);
    }
}
