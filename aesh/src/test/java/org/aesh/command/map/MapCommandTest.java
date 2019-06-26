/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
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
package org.aesh.command.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.parser.OptionParserException;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.readline.Prompt;
import org.aesh.readline.ReadlineConsole;
import org.aesh.readline.terminal.Key;
import org.aesh.tty.TestConnection;
import org.aesh.terminal.utils.Config;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author jdenise@redhat.com
 */
@SuppressWarnings("unchecked")
public class MapCommandTest {
    private final Key completeChar = Key.CTRL_I;

    static class DynCommand1 extends MapCommand<CommandInvocation> {
        Map<String, Object> options;
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            options = getValues();
            return CommandResult.SUCCESS;
        }

    }

    static class DynamicOptionsProvider implements MapProcessedOptionProvider {
        private List<ProcessedOption> options = Collections.emptyList();
        @Override
        public List<ProcessedOption> getOptions(List<ProcessedOption> currentOptions) {
            return options;
        }
    }

    static class DynamicOptionsCountProvider implements MapProcessedOptionProvider {

        private int count = 0;
        private List<ProcessedOption> options = Collections.emptyList();

        @Override
        public List<ProcessedOption> getOptions(List<ProcessedOption> currentOptions) {
            count += 1;
            return options;
        }
    }

    @Test
    public void testCompletion() throws Exception {
        TestConnection connection = new TestConnection(false);

        // Build dynamic command.
        DynCommand1 cmd = new DynCommand1();
        DynamicOptionsProvider provider = new DynamicOptionsProvider();

        MapProcessedCommandBuilder builder = MapProcessedCommandBuilder.builder();
        builder.command(cmd);
        // Retrieve dynamic options during completion.
        builder.lookupAtCompletionOnly(true);
        builder.name("dyn1");
        builder.optionProvider(provider);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(builder.create())
                .create();

        Settings settings = SettingsBuilder.builder()
                .logging(true)
                .connection(connection)
                .commandRegistry(registry)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        // First test without any dynamic option provided.
        connection.clearOutputBuffer();
        connection.read("d");
        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 ", connection.getOutputBuffer());

        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 ", connection.getOutputBuffer());

        connection.read("--");
        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 --", connection.getOutputBuffer());

        // Then add dynamic options
        provider.options = getOptions();

        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 --opt-dyn", connection.getOutputBuffer());

        connection.read("1");
        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 --opt-dyn1-withvalue=", connection.getOutputBuffer());

        connection.read("cdcsdc ");
        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 --opt-dyn1-withvalue=cdcsdc --opt-dyn", connection.getOutputBuffer());

        connection.read("2");
        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 --opt-dyn1-withvalue=cdcsdc --opt-dyn2-withvalue=", connection.getOutputBuffer());

        connection.read("xxx ");
        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 --opt-dyn1-withvalue=cdcsdc --opt-dyn2-withvalue=xxx --opt-dyn3-novalue ", connection.getOutputBuffer());

        // No completion if the options already exist in the buffer.
        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 --opt-dyn1-withvalue=cdcsdc --opt-dyn2-withvalue=xxx --opt-dyn3-novalue ", connection.getOutputBuffer());

        // Execute command.
        connection.read(Config.getLineSeparator());
        connection.clearOutputBuffer();
        Thread.sleep(200);
        {
            String val = (String) cmd.options.get("opt-dyn1-withvalue");
            assertEquals("cdcsdc", val);
        }
        {
            String val = (String) cmd.options.get("opt-dyn2-withvalue");
            assertEquals("xxx", val);
        }
        assertTrue(cmd.contains("opt-dyn3-novalue"));

        // Invalid option
        connection.read("dyn1 --opt-dyn3-novalue--");
        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 --opt-dyn3-novalue--", connection.getOutputBuffer());
    }

    @Test
    public void testCompletionWithStaticOptions() throws Exception {
        TestConnection connection = new TestConnection(false);

        // Build dynamic command.
        DynCommand1 cmd = new DynCommand1();
        DynamicOptionsProvider provider = new DynamicOptionsProvider();

        MapProcessedCommandBuilder builder = MapProcessedCommandBuilder.builder();
        builder.command(cmd);
        // Retrieve dynamic options during completion.
        builder.lookupAtCompletionOnly(true);
        builder.name("dyn1");

        {
            ProcessedOptionBuilder optBuilder = ProcessedOptionBuilder.builder();
            optBuilder.name("verbose");
            optBuilder.hasValue(false);
            optBuilder.type(Boolean.class);
            builder.addOption(optBuilder.build());
        }

        {
            ProcessedOptionBuilder optBuilder = ProcessedOptionBuilder.builder();
            optBuilder.name("dir");
            optBuilder.hasValue(true);
            optBuilder.type(String.class);
            builder.addOption(optBuilder.build());
        }
        builder.optionProvider(provider);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(builder.create())
                .create();

        Settings settings = SettingsBuilder.builder()
                .logging(true)
                .connection(connection)
                .commandRegistry(registry)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        // First test without any dynamic option provided.
        connection.clearOutputBuffer();
        connection.read("d");
        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 ", connection.getOutputBuffer());

        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 --", connection.getOutputBuffer());

        connection.read("v");
        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 --verbose ", connection.getOutputBuffer());

        connection.read("--");
        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 --verbose --dir=", connection.getOutputBuffer());

        connection.read("toto");
        // Execute command.
        connection.read(Config.getLineSeparator());
        connection.clearOutputBuffer();
        Thread.sleep(200);
        {
            String val = (String) cmd.options.get("dir");
            assertEquals("toto", val);
        }
        assertTrue(cmd.contains("verbose"));

        // Enable dynamic commands
        provider.options = getOptions();

        connection.read("dyn1 --verbose");
        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 --verbose ", connection.getOutputBuffer());

        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 --verbose --", connection.getOutputBuffer());

        connection.read("opt-dyn1");
        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 --verbose --opt-dyn1-withvalue=", connection.getOutputBuffer());

        connection.read("xxx ");
        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 --verbose --opt-dyn1-withvalue=xxx --", connection.getOutputBuffer());

        connection.read("d");
        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 --verbose --opt-dyn1-withvalue=xxx --dir=", connection.getOutputBuffer());

        connection.read("tutu ");
        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 --verbose --opt-dyn1-withvalue=xxx --dir=tutu --opt-dyn", connection.getOutputBuffer());

        connection.read("2-withvalue=yyy --");
        connection.read(completeChar.getFirstValue());
        assertEquals("dyn1 --verbose --opt-dyn1-withvalue=xxx --dir=tutu --opt-dyn2-withvalue=yyy --opt-dyn3-novalue ", connection.getOutputBuffer());

        // Execute command.
        connection.read(Config.getLineSeparator());
        connection.clearOutputBuffer();
        Thread.sleep(200);
        {
            String val = (String) cmd.options.get("dir");
            assertEquals("tutu", val);
        }
        {
            String val = (String) cmd.options.get("opt-dyn1-withvalue");
            assertEquals("xxx", val);
        }
        {
            String val = (String) cmd.options.get("opt-dyn2-withvalue");
            assertEquals("yyy", val);
        }
        assertTrue(cmd.contains("verbose"));
        assertTrue(cmd.contains("opt-dyn3-novalue"));
    }

    @Test
    public void testExecution() throws Exception {
        TestConnection connection = new TestConnection(false);

        // Build dynamic command.
        DynCommand1 cmd = new DynCommand1();
        DynamicOptionsProvider provider = new DynamicOptionsProvider();

        MapProcessedCommandBuilder builder = MapProcessedCommandBuilder.builder();
        builder.command(cmd);
        // Retrieve dynamic options at execution time too, required to check for required option.
        builder.lookupAtCompletionOnly(false);
        builder.name("dyn1");

        {
            ProcessedOptionBuilder optBuilder = ProcessedOptionBuilder.builder();
            optBuilder.name("verbose");
            optBuilder.hasValue(false);
            optBuilder.type(Boolean.class);
            builder.addOption(optBuilder.build());
        }

        {
            ProcessedOptionBuilder optBuilder = ProcessedOptionBuilder.builder();
            optBuilder.name("dir");
            optBuilder.hasValue(true);
            optBuilder.type(String.class);
            builder.addOption(optBuilder.build());
        }
        builder.optionProvider(provider);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(builder.create())
                .create();

        Settings settings = SettingsBuilder.builder()
                .logging(true)
                .connection(connection)
                .commandRegistry(registry)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        // First test without any dynamic option provided.
        connection.clearOutputBuffer();
        connection.read("dyn1");
        // Execute command.
        connection.read(Config.getLineSeparator());
        connection.clearOutputBuffer();
        Thread.sleep(200);
        assertFalse(cmd.contains("verbose"));
        assertFalse(cmd.contains("dir"));

        connection.read("dyn1 --verbose");
        // Execute command.
        connection.read(Config.getLineSeparator());
        connection.clearOutputBuffer();
        Thread.sleep(200);
        assertTrue(cmd.contains("verbose"));
        assertFalse(cmd.contains("dir"));

        connection.read("dyn1 --dir=toto");
        // Execute command.
        connection.read(Config.getLineSeparator());
        connection.clearOutputBuffer();
        Thread.sleep(200);
        assertFalse(cmd.contains("verbose"));
        assertTrue(cmd.contains("dir"));
        assertFalse(cmd.contains("opt-dyn1-withvalue"));

        // add dynamic options
        provider.options = getOptions();

        connection.read("dyn1 --opt-dyn1-withvalue=foo");
        // Execute command.
        connection.read(Config.getLineSeparator());
        connection.clearOutputBuffer();
        Thread.sleep(200);
        assertFalse(cmd.contains("verbose"));
        assertFalse(cmd.contains("dir"));
        assertTrue(cmd.contains("opt-dyn1-withvalue"));
        assertFalse(cmd.contains("opt-dyn2-withvalue"));
        assertFalse(cmd.contains("opt-dyn3-novalue"));

        // Update to a new set if options.
        provider.options = getOptionsRequired();
        connection.read("dyn1");
        // Execute command.
        connection.read(Config.getLineSeparator());
        Thread.sleep(200);
        assertTrue(connection.getOutputBuffer().contains("Option: --opt-dyn1-required is required for this command"));
        connection.clearOutputBuffer();

        connection.read("dyn1 --opt-dyn1-required=xxx");
        // Execute command.
        connection.read(Config.getLineSeparator());
        Thread.sleep(200);
        assertTrue(connection.getOutputBuffer().contains("Option: --opt-dyn2-required is required for this command"));
        connection.clearOutputBuffer();

        connection.read("dyn1 --opt-dyn1-required=xxx --opt-dyn2-required=yyy");
        // Execute command.
        connection.read(Config.getLineSeparator());
        connection.clearOutputBuffer();
        Thread.sleep(200);
        assertTrue(connection.getOutputBuffer(), cmd.contains("opt-dyn1-required"));
        assertTrue(connection.getOutputBuffer(), cmd.contains("opt-dyn2-required"));
        assertFalse(cmd.contains("opt-dyn1-withvalue"));
        assertFalse(cmd.contains("opt-dyn2-withvalue"));
        assertFalse(cmd.contains("opt-dyn3-novalue"));
    }

    @Test
    public void clearedOptionTest() throws Exception {
        TestConnection connection = new TestConnection(false);

        // Build dynamic command.
        DynCommand1 cmd = new DynCommand1();
        DynamicOptionsCountProvider provider = new DynamicOptionsCountProvider();
        provider.options = getOptions();

        MapProcessedCommandBuilder builder = MapProcessedCommandBuilder.builder();
        builder.command(cmd);
        builder.lookupAtCompletionOnly(false);
        builder.name("dyn1");
        builder.optionProvider(provider);
        MapProcessedCommand processedCmd = builder.create();
        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(processedCmd)
                .create();

        Settings settings = SettingsBuilder.builder()
                .logging(true)
                .connection(connection)
                .commandRegistry(registry)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        connection.clearOutputBuffer();
        connection.read("dyn1 --opt-dyn1-withvalue=");
        // Execute command provising XXX value.
        connection.read(Config.getLineSeparator());
        connection.clearOutputBuffer();
        Thread.sleep(200);
        ProcessedOption opt = processedCmd.findLongOption("opt-dyn1-withvalue");
        assertEquals("", opt.getValue());
        assertEquals(1, opt.getValues().size());

        connection.clearOutputBuffer();
        connection.read("dyn1 --opt-dyn1-withvalue=XXX");
        // Execute command provising XXX value.
        connection.read(Config.getLineSeparator());
        connection.clearOutputBuffer();
        Thread.sleep(200);
        opt = processedCmd.findLongOption("opt-dyn1-withvalue");
        assertEquals("XXX", opt.getValue());
        assertEquals(1, opt.getValues().size());

    }

    @Test
    public void optionRetrievalTest() throws Exception {
        TestConnection connection = new TestConnection(false);

        // Build dynamic command.
        DynCommand1 cmd = new DynCommand1();
        DynamicOptionsCountProvider provider = new DynamicOptionsCountProvider();
        provider.options = getOptions();

        MapProcessedCommandBuilder builder = MapProcessedCommandBuilder.builder();
        builder.command(cmd);
        // Retrieve dynamic options during completion.
        builder.lookupAtCompletionOnly(true);
        builder.name("dyn1");
        builder.optionProvider(provider);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(builder.create())
                .create();

        Settings settings = SettingsBuilder.builder()
                .logging(true)
                .connection(connection)
                .commandRegistry(registry)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();

        connection.clearOutputBuffer();
        connection.read("dyn1 --");
        connection.read(completeChar.getFirstValue());
        assertEquals(provider.count, 1);
    }

    private static List<ProcessedOption> getOptionsRequired() throws OptionParserException {
        List<ProcessedOption> options = new ArrayList<>();
        {
            ProcessedOptionBuilder builder = ProcessedOptionBuilder.builder();
            builder.name("opt-dyn1-required");
            builder.hasValue(true);
            builder.required(true);
            builder.type(String.class);
            options.add(builder.build());
        }
        {
            ProcessedOptionBuilder builder = ProcessedOptionBuilder.builder();
            builder.name("opt-dyn2-required");
            builder.hasValue(true);
            builder.required(true);
            builder.type(String.class);
            options.add(builder.build());
        }
        return options;
    }

    private static List<ProcessedOption> getOptions() throws OptionParserException {
        List<ProcessedOption> options = new ArrayList<>();
        {
            ProcessedOptionBuilder builder = ProcessedOptionBuilder.builder();
            builder.name("opt-dyn1-withvalue");
            builder.hasValue(true);
            builder.type(String.class);
            options.add(builder.build());
        }
        {
            ProcessedOptionBuilder builder = ProcessedOptionBuilder.builder();
            builder.name("opt-dyn2-withvalue");
            builder.hasValue(true);
            builder.type(String.class);
            options.add(builder.build());
        }
        {
            ProcessedOptionBuilder builder = ProcessedOptionBuilder.builder();
            builder.name("opt-dyn3-novalue");
            builder.hasValue(false);
            builder.type(Boolean.class);
            options.add(builder.build());
        }
        return options;
    }
}
