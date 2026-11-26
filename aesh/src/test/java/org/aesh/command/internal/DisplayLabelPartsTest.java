/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.aesh.command.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedOption;
import org.junit.Test;

/** Verifies precomputed help label parts and their invalidation. */
public class DisplayLabelPartsTest {

    @Test
    public void testLabelShapes() {
        assertParts("-v, --verbose", null, bool("verbose", 'v'));
        assertParts("    --plain", "=<plain>", normal("plain"));
        assertParts("-f, --input, --file, --path", "=<path>", aliased());
        assertParts("-v, --[disable-]verbose", null, negatable());
        assertParts("    bare", "=<bare>", bare());
        assertParts("    --props", "=<key=value>", group());
        assertParts("<SOURCE>", null, positional("SOURCE", "source"));
        assertParts("", null, positional("", ""));
    }

    @Test
    public void testMutationsInvalidateParts() {
        ProcessedOption opt = normal("plain");
        assertEquals("    --plain=<plain>", formatted(opt));
        opt.setShortName("p");
        assertEquals("-p, --plain=<plain>", formatted(opt));
        opt.setNegatable(true);
        opt.setNegationPrefix("disable-");
        assertEquals("-p, --[disable-]plain=<plain>", formatted(opt));
        opt.setAliases(Collections.singletonList("a"));
        assertEquals("-p, --[disable-]plain, --a=<plain>", formatted(opt));
        opt.setOptionalValue(true);
        assertEquals("-p, --[disable-]plain, --a", formatted(opt));
        assertEquals(formatted(opt).length(), opt.getFormattedLength());
    }

    @Test
    public void testFormattedOutputUsesParts() {
        ProcessedOption opt = normal("plain");
        String formatted = opt.getFormattedOption(2, 30, 80, false, "Target");
        String plain = formatted.replaceAll("\u001B\\[[;\\d]*m", "");
        assertTrue("label must lead, got: " + formatted, plain.contains("    --plain"));
        assertTrue("placeholder must follow, got: " + formatted, plain.contains("=<plain>"));
    }

    private static String formatted(ProcessedOption opt) {
        opt.updateAnsiMode(false);
        return opt.getFormattedOption(0, 30, 80, false, "");
    }

    private static void assertParts(String namePart, String placeholderPart, ProcessedOption opt) {
        int expected = namePart.length() + (placeholderPart != null ? placeholderPart.length() : 0);
        assertEquals("length of " + namePart, expected, opt.getFormattedLength());
        String formatted = opt.getFormattedOption(0, expected + 10, 80, false, "");
        String plain = formatted.replaceAll("\u001B\\[[;\\d]*m", "");
        assertTrue("label text, got: " + formatted, plain.contains(namePart));
    }

    private static ProcessedOption bool(String name, char shortName) {
        return ProcessedOption.createDirect(Character.toString(shortName), name, name, Boolean.class, name,
                OptionType.BOOLEAN, null, null);
    }

    private static ProcessedOption normal(String name) {
        return ProcessedOption.createDirect(null, name, name, String.class, name, OptionType.NORMAL, null, null);
    }

    private static ProcessedOption aliased() {
        ProcessedOption opt = ProcessedOption.createDirect("f", "input", "input", String.class, "input",
                OptionType.NORMAL, null, null);
        opt.setAliases(Arrays.asList("file", "path"));
        opt.setArgument("path");
        return opt;
    }

    private static ProcessedOption negatable() {
        ProcessedOption opt = bool("verbose", 'v');
        opt.setNegatable(true);
        opt.setNegationPrefix("disable-");
        return opt;
    }

    private static ProcessedOption bare() {
        ProcessedOption opt = normal("bare");
        opt.setAcceptNameWithoutDashes(true);
        return opt;
    }

    private static ProcessedOption group() {
        return ProcessedOption.createDirect(null, "props", "props", Map.class, "props",
                OptionType.GROUP, null, null);
    }

    private static ProcessedOption positional(String paramLabel, String fieldName) {
        ProcessedOption opt = ProcessedOption.createDirect(null, "", "", String.class, fieldName,
                OptionType.ARGUMENT, null, null);
        if (paramLabel != null && !paramLabel.isEmpty())
            opt.setParamLabel(paramLabel);
        return opt;
    }
}
