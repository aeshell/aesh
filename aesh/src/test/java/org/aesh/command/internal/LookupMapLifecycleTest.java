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
package org.aesh.command.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Field;

import org.aesh.command.Command;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.invocation.CommandInvocation;
import org.junit.Test;

/** Verifies that lookup indexes are built only when first needed. */
public class LookupMapLifecycleTest {

    private static final String[] MAP_FIELDS = { "longNameMap", "shortNameMap", "negatedNameMap", "bareNameMap" };

    @Test
    public void testDirectOptionsDeferMapConstructionUntilFirstLookup() throws Exception {
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> command = newCommand();
        ProcessedOption verbose = option("verbose", 'v');
        command.addOptionDirect(verbose);

        assertMaps(command, false);
        assertSame(verbose, command.findLongOptionNoActivatorCheck("verbose"));
        assertMaps(command, true);

        ProcessedOption quiet = option("quiet", 'q');
        command.addOptionDirect(quiet);
        assertMaps(command, false);
        assertSame(quiet, command.findOptionNoActivatorCheck("q"));
        assertMaps(command, true);
        assertSame(verbose, command.findLongOptionNoActivatorCheck("verbose"));
    }

    @Test
    public void testBuilderOptionsArePresentWhenFirstMapBuilds() throws Exception {
        ProcessedOption verbose = option("verbose", 'v');
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> command = ProcessedCommandBuilder
                .<Command<CommandInvocation>, CommandInvocation> builder()
                .name("test")
                .description("test")
                .generateHelp(false)
                .version("")
                .addOption(verbose)
                .create();

        assertMaps(command, false);
        assertSame(verbose, command.findLongOptionNoActivatorCheck("verbose"));
        assertMaps(command, true);
    }

    private static ProcessedCommand<Command<CommandInvocation>, CommandInvocation> newCommand() throws Exception {
        return ProcessedCommandBuilder.<Command<CommandInvocation>, CommandInvocation> builder()
                .name("test")
                .description("test")
                .generateHelp(false)
                .version("")
                .create();
    }

    private static ProcessedOption option(String name, char shortName) {
        return ProcessedOption.createDirect(Character.toString(shortName), name, name, Boolean.class, name,
                OptionType.BOOLEAN, null, null);
    }

    private static void assertMaps(ProcessedCommand<?, ?> command, boolean built) throws Exception {
        for (String name : MAP_FIELDS) {
            Object value = mapField(command, name);
            if (built)
                assertNotNull(name + " should be built on first lookup", value);
            else
                assertNull(name + " should not be built before first lookup", value);
        }
    }

    private static Object mapField(ProcessedCommand<?, ?> command, String name) throws Exception {
        Field field = ProcessedCommand.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(command);
    }
}
