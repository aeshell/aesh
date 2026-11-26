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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.impl.completer.BooleanOptionCompleter;
import org.aesh.command.impl.completer.DefaultValueOptionCompleter;
import org.aesh.command.impl.completer.FileOptionCompleter;
import org.aesh.command.impl.internal.CollectionFactory;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.CompletionFallback;
import org.aesh.io.Resource;
import org.junit.Test;

/**
 * Tests for {@link CollectionFactory} and lazy default completer /
 * {@link CompletionFallback} resolution on {@link ProcessedOption} (#789).
 */
public class CollectionFactoryAndLazyDefaultsTest {

    enum Mode {
        FAST,
        SLOW
    }

    @Test
    public void testCollectionFactoryProducesFreshInstances() {
        assertFreshInstance(CollectionFactory.ARRAY_LIST, ArrayList.class);
        assertFreshInstance(CollectionFactory.LINKED_LIST, LinkedList.class);
        assertFreshInstance(CollectionFactory.HASH_SET, HashSet.class);
        assertFreshInstance(CollectionFactory.LINKED_HASH_SET, LinkedHashSet.class);
        assertFreshInstance(CollectionFactory.TREE_SET, TreeSet.class);
        assertFreshInstance(CollectionFactory.HASH_MAP, HashMap.class);
        assertFreshInstance(CollectionFactory.LINKED_HASH_MAP, LinkedHashMap.class);
        assertFreshInstance(CollectionFactory.TREE_MAP, TreeMap.class);
    }

    private static void assertFreshInstance(CollectionFactory factory, Class<?> expectedType) {
        Object first = factory.get();
        Object second = factory.get();
        assertEquals(expectedType, first.getClass());
        assertEquals(expectedType, second.getClass());
        assertNotSame("Each get() must return a fresh collection instance", first, second);
    }

    @Test
    public void testLazyDefaultCompletersAndPrecedence() {
        ProcessedOption boolOpt = ProcessedOption.createDirect(null, "flag", "", boolean.class, "flag",
                OptionType.BOOLEAN, null, null);
        assertSame(BooleanOptionCompleter.INSTANCE, boolOpt.completer());

        ProcessedOption boxedBoolOpt = ProcessedOption.createDirect(null, "boxed", "", Boolean.class, "boxed",
                OptionType.NORMAL, null, null);
        assertSame(BooleanOptionCompleter.INSTANCE, boxedBoolOpt.completer());

        ProcessedOption allowedOpt = ProcessedOption.createDirect(null, "env", "", String.class, "env",
                OptionType.NORMAL, null, null);
        allowedOpt.setAllowedValues(Arrays.asList("dev", "prod"));
        assertTrue(allowedOpt.completer() instanceof DefaultValueOptionCompleter);

        ProcessedOption fileWithAllowed = ProcessedOption.createDirect(null, "cfg", "", File.class, "cfg",
                OptionType.NORMAL, null, null);
        fileWithAllowed.setAllowedValues(Arrays.asList("a.yml", "b.yml"));
        assertTrue("allowedValues takes precedence over FileOptionCompleter",
                fileWithAllowed.completer() instanceof DefaultValueOptionCompleter);

        ProcessedOption fileOpt = ProcessedOption.createDirect(null, "file", "", File.class, "file",
                OptionType.NORMAL, null, null);
        assertTrue(fileOpt.completer() instanceof FileOptionCompleter);

        ProcessedOption pathOpt = ProcessedOption.createDirect(null, "path", "", Path.class, "path",
                OptionType.NORMAL, null, null);
        assertTrue(pathOpt.completer() instanceof FileOptionCompleter);

        ProcessedOption resOpt = ProcessedOption.createDirect(null, "res", "", Resource.class, "res",
                OptionType.NORMAL, null, null);
        assertTrue(resOpt.completer() instanceof FileOptionCompleter);

        ProcessedOption stringOpt = ProcessedOption.createDirect(null, "name", "", String.class, "name",
                OptionType.NORMAL, null, null);
        assertNull(stringOpt.completer());

        OptionCompleter<CompleterInvocation> custom = inv -> {
        };
        ProcessedOption customOpt = ProcessedOption.createDirect(null, "custom", "", File.class, "custom",
                OptionType.NORMAL, null, null);
        customOpt.setCompleter(custom);
        assertSame("Explicit custom completer must win over default", custom, customOpt.completer());
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testLazyCompleteFallbackResolution() throws Exception {
        ProcessedOption strOpt = ProcessedOption.createDirect(null, "name", "", String.class, "name",
                OptionType.NORMAL, null, null);
        assertEquals(CompletionFallback.FILES, strOpt.getCompleteFallback());

        ProcessedOption fileOpt = ProcessedOption.createDirect(null, "file", "", File.class, "file",
                OptionType.NORMAL, null, null);
        assertEquals(CompletionFallback.FILES, fileOpt.getCompleteFallback());

        ProcessedOption pathOpt = ProcessedOption.createDirect(null, "path", "", Path.class, "path",
                OptionType.NORMAL, null, null);
        assertEquals(CompletionFallback.FILES, pathOpt.getCompleteFallback());

        ProcessedOption resOpt = ProcessedOption.createDirect(null, "res", "", Resource.class, "res",
                OptionType.NORMAL, null, null);
        assertEquals(CompletionFallback.FILES, resOpt.getCompleteFallback());

        ProcessedOption enumOpt = ProcessedOption.createDirect(null, "mode", "", Mode.class, "mode",
                OptionType.NORMAL, null, null);
        assertEquals(CompletionFallback.NONE, enumOpt.getCompleteFallback());

        ProcessedOption intOpt = ProcessedOption.createDirect(null, "count", "", int.class, "count",
                OptionType.NORMAL, null, null);
        assertEquals(CompletionFallback.NONE, intOpt.getCompleteFallback());

        // Parent command fallback overrides type heuristic for DEFAULT options,
        // while an explicit option-level fallback overrides the command fallback.
        Command<CommandInvocation> dummy = inv -> CommandResult.SUCCESS;
        ProcessedCommand cmd = ProcessedCommandBuilder.builder().name("test").command(dummy).create();
        cmd.setCompleteFallback(CompletionFallback.NONE);
        cmd.addOptionDirect(strOpt);
        assertEquals(CompletionFallback.NONE, strOpt.getCompleteFallback());

        ProcessedOption explicitDir = ProcessedOption.createDirect(null, "dir", "", String.class, "dir",
                OptionType.NORMAL, null, null);
        explicitDir.setCompleteFallback(CompletionFallback.DIRECTORIES);
        cmd.addOptionDirect(explicitDir);
        assertEquals(CompletionFallback.DIRECTORIES, explicitDir.getCompleteFallback());
    }
}
