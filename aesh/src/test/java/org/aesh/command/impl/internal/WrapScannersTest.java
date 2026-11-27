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
package org.aesh.command.impl.internal;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * The help-rendering scanners that replaced the last {@code --help}-path
 * regex (#801). Each test asserts exact equivalence with the regex oracle
 * it replaced — the regex still works in a test JVM, so it is the strongest
 * possible specification of the scanner contract.
 */
public class WrapScannersTest {

    private static final String[] SPLIT_INPUTS = {
            "", " ", "  ", "a", "a b", "a  b", "a b c", "a ", "a  ", "a b ", "a b  ",
            " a", "  a", " a b", "  a  b  ", "a\tb", "a \t b", "\t", "a\t b\t c",
            "   only-leading", "only-trailing   ", "  both  ", "word",
    };

    @Test
    public void testSplitOnSpaceRunsMatchesRegexSplit() {
        for (String input : SPLIT_INPUTS) {
            List<String> expected = Arrays.asList(input.split(" +"));
            assertEquals("split(\" +\") equivalence for <" + input + ">", expected,
                    ProcessedOption.splitOnSpaceRuns(input));
        }
    }

    @Test
    public void testSplitOnSpaceRunsLeadingEmptyKeptTrailingDropped() {
        // The default split() limit drops every trailing empty word but
        // keeps a leading one; all-separator input yields no words at all.
        assertEquals(Arrays.asList("", "a"), ProcessedOption.splitOnSpaceRuns(" a"));
        assertEquals(Arrays.asList("a"), ProcessedOption.splitOnSpaceRuns("a "));
        assertEquals(Arrays.asList(""), ProcessedOption.splitOnSpaceRuns(""));
        assertEquals(Arrays.asList(), ProcessedOption.splitOnSpaceRuns("   "));
        assertEquals(Arrays.asList(), ProcessedOption.splitOnSpaceRuns(" "));
        // Tabs are word characters, not separators.
        assertEquals(Arrays.asList("a\tb"), ProcessedOption.splitOnSpaceRuns("a\tb"));
        assertEquals(Arrays.asList("a", "\t", "b"), ProcessedOption.splitOnSpaceRuns("a \t b"));
    }

    @Test
    public void testVisualLengthMatchesRegexReplaceAll() {
        String[] inputs = {
                "", "plain", "\u001B[1mbold\u001B[0m", "\u001B[m", "\u001B[1;33;40m",
                "before \u001B[32mgreen\u001B[0m after", "\u001B[0m",
                // Unterminated sequences have no regex match: all chars count.
                "\u001B[1", "\u001B[", "\u001B", "\u001B[1;xm",
                // A failed match at one ESC still lets a later sequence match.
                "\u001B[x\u001B[1m", "\u001B[\u001B[1m",
                "text\u001B[1m", "\u001B[38;5;196mred",
        };
        for (String input : inputs) {
            int expected = input.replaceAll("\u001B\\[[;\\d]*m", "").length();
            assertEquals("visualLength equivalence for <" + input + ">", expected,
                    ProcessedOption.visualLength(input));
        }
    }

    @Test
    public void testWrapTextCollapsesSpaceRunsAndDropsTrailing() {
        // " +", not " ": a run of spaces separates words once, and trailing
        // spaces must not produce an empty wrapped word. Widths must be at
        // least MIN_WRAP_WIDTH (20) or wrapText returns the line verbatim.
        List<String> wrapped = ProcessedOption.wrapText("alpha    beta   gamma", 20, false);
        assertEquals(Arrays.asList("alpha beta gamma"), wrapped);

        // Tabs are word characters: the tab-joined word stays whole.
        wrapped = ProcessedOption.wrapText("aaaaaaaaa\tbbbbbbbb ccccccccc", 20, false);
        assertEquals(Arrays.asList("aaaaaaaaa\tbbbbbbbb", "ccccccccc"), wrapped);
    }
}
