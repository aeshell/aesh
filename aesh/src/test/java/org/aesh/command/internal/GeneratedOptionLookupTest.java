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

import static org.aesh.command.internal.LookupMapLifecycleTest.assertMaps;
import static org.aesh.command.internal.LookupMapLifecycleTest.mapField;
import static org.aesh.command.internal.LookupMapLifecycleTest.newCommand;
import static org.aesh.command.internal.LookupMapLifecycleTest.option;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.aesh.command.Command;
import org.aesh.command.impl.internal.GeneratedOptionLookup;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.invocation.CommandInvocation;
import org.junit.Test;

/** Verifies generated lookup behavior and fallback to runtime maps. */
public class GeneratedOptionLookupTest {

    @Test
    public void testGeneratedLookupAvoidsMapsAndInvalidatesOnAdd() throws Exception {
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> command = newCommand();
        ProcessedOption verbose = option("verbose", 'v');
        verbose.setNegatable(true);
        verbose.setAcceptNameWithoutDashes(true);
        command.addOptionDirect(verbose);
        command.setGeneratedOptionLookup(new GeneratedOptionLookup() {
            @Override
            public int longOptionIndex(String name) {
                return "verbose".equals(name) ? 0 : -1;
            }

            @Override
            public int shortOptionIndex(String name) {
                return "v".equals(name) ? 0 : -1;
            }

            @Override
            public int negatedOptionIndex(String name) {
                return "no-verbose".equals(name) ? 0 : -1;
            }

            @Override
            public int bareOptionIndex(String name) {
                return "verbose".equals(name) ? 0 : -1;
            }
        });

        assertSame(verbose, command.findLongOptionNoActivatorCheck("verbose"));
        assertSame(verbose, command.findOptionNoActivatorCheck("v"));
        assertSame(verbose, command.findNegatedOptionNoActivatorCheck("no-verbose"));
        assertSame(verbose, command.findBareLongOption("verbose"));
        assertNull(command.findLongOptionNoActivatorCheck("missing"));
        assertMaps(command, false);

        ProcessedOption quiet = option("quiet", 'q');
        command.addOptionDirect(quiet);
        assertNull(mapField(command, "generatedOptionLookup"));
        assertSame(quiet, command.findLongOptionNoActivatorCheck("quiet"));
        assertMaps(command, true);
        assertSame(verbose, command.findLongOptionNoActivatorCheck("verbose"));
    }
}
