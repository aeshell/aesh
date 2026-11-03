/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.aesh.command.populator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.OptionGroup;
import org.aesh.command.option.OptionList;
import org.junit.Test;

/**
 * A collection or map field that is not given on the command line keeps its initial value: from its initializer, or
 * from the constructor.
 */
public class InitialCollectionValueTest {

    @Test
    public void testInitializerIsKept() throws Exception {
        FilesCommand command = new FilesCommand();
        CommandRuntime<CommandInvocation> runtime = runtime(command);
        runtime.executeCommand("files", new String[0]);

        assertEquals(Arrays.asList("from-initializer.yaml"), command.files);
        assertEquals(Arrays.asList("camel-core"), command.deps);
        assertEquals(Collections.singletonMap("level", "info"), command.props);
    }

    @Test
    public void testConstructorValueIsKept() throws Exception {
        FilesCommand command = new FilesCommand("from-constructor.yaml");
        CommandRuntime<CommandInvocation> runtime = runtime(command);
        runtime.executeCommand("files", new String[0]);

        assertEquals(Arrays.asList("from-constructor.yaml"), command.files);
    }

    @Test
    public void testUnmodifiableInitialValueIsKept() throws Exception {
        FilesCommand command = new FilesCommand();
        command.deps = Collections.unmodifiableList(Arrays.asList("camel-core", "camel-main"));
        CommandRuntime<CommandInvocation> runtime = runtime(command);
        runtime.executeCommand("files", new String[0]);

        assertEquals(Arrays.asList("camel-core", "camel-main"), command.deps);
    }

    @Test
    public void testEachRunStartsFromTheInitialValue() throws Exception {
        FilesCommand command = new FilesCommand();
        CommandRuntime<CommandInvocation> runtime = runtime(command);

        runtime.executeCommand("files", new String[] { "a.yaml", "b.yaml" });
        assertEquals(Arrays.asList("a.yaml", "b.yaml"), command.files);

        // the command changes its list; the next run gets the initial value again, in a list of its own
        command.files.add("changed.yaml");
        runtime.executeCommand("files", new String[0]);
        assertEquals(Arrays.asList("from-initializer.yaml"), command.files);
        List<String> first = command.files;

        runtime.executeCommand("files", new String[0]);
        assertEquals(Arrays.asList("from-initializer.yaml"), command.files);
        assertNotSame(first, command.files);
    }

    private static CommandRuntime<CommandInvocation> runtime(FilesCommand command) throws Exception {
        return AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(AeshCommandRegistryBuilder.<CommandInvocation> builder().command(command).create())
                .build();
    }

    @CommandDefinition(name = "files", description = "")
    public static class FilesCommand implements Command<CommandInvocation> {
        @Arguments
        List<String> files = new ArrayList<>(Arrays.asList("from-initializer.yaml"));

        @OptionList
        List<String> deps = new ArrayList<>(Arrays.asList("camel-core"));

        @OptionGroup(shortName = 'P')
        Map<String, String> props = new LinkedHashMap<>(Collections.singletonMap("level", "info"));

        public FilesCommand() {
        }

        public FilesCommand(String file) {
            this.files = new ArrayList<>(Arrays.asList(file));
        }

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
