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
package org.aesh.command.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.aesh.command.operator.OperatorType;
import org.aesh.parser.ParsedLine;
import org.aesh.parser.ParsedWord;
import org.aesh.parser.ParserStatus;
import org.junit.Test;

/**
 * The lazy display line must be byte-identical to eager construction,
 * cached after first read, and behaviorally indistinguishable (#662).
 */
public class LazyParsedLineTest {

    private static List<ParsedWord> wordsFor(String commandName, String[] args) {
        List<ParsedWord> words = new ArrayList<>();
        words.add(new ParsedWord(commandName, 0));
        int offset = commandName.length() + 1;
        if (args != null) {
            for (String arg : args) {
                words.add(new ParsedWord(arg, offset));
                offset += arg.length() + 1;
            }
        }
        return words;
    }

    private static String eagerLine(String commandName, String[] args) {
        StringBuilder displayLine = new StringBuilder(commandName);
        if (args != null) {
            for (String arg : args) {
                displayLine.append(' ').append(arg);
            }
        }
        return displayLine.toString();
    }

    private static void assertMatchesEager(String commandName, String[] args) {
        List<ParsedWord> words = wordsFor(commandName, args);
        ParsedLine eager = new ParsedLine(eagerLine(commandName, args), words,
                -1, -1, -1, ParserStatus.OK, "", OperatorType.NONE);
        LazyParsedLine lazy = new LazyParsedLine(commandName, args, wordsFor(commandName, args));

        assertEquals(eager.line(), lazy.line());
        assertSame("materialized line must be cached", lazy.line(), lazy.line());
        assertEquals(eager.cursorAtEnd(), lazy.cursorAtEnd());
        assertEquals(eager.spaceAtEnd(), lazy.spaceAtEnd());
        assertEquals(eager.toString(), lazy.toString());
    }

    @Test
    public void testJoinMatchesEager() {
        assertMatchesEager("net", new String[] { "--host", "example.com", "--count", "3", "host1" });
    }

    @Test
    public void testEmptyArgumentsPreserved() {
        assertMatchesEager("collect", new String[] { "--items", "first", "" });
        LazyParsedLine lazy = new LazyParsedLine("collect",
                new String[] { "--items", "first", "" },
                wordsFor("collect", new String[] { "--items", "first", "" }));
        assertEquals("", lazy.words().get(3).word());
        assertEquals("collect --items first ", lazy.line());
    }

    @Test
    public void testNullAndEmptyArgs() {
        assertMatchesEager("net", null);
        assertMatchesEager("net", new String[0]);
    }

    @Test
    public void testSnapshotIsolationFromCallerArray() {
        String[] args = { "--host", "example.com" };
        LazyParsedLine lazy = new LazyParsedLine("net", args,
                wordsFor("net", new String[] { "--host", "example.com" }));
        args[1] = "mutated";
        assertEquals("net --host example.com", lazy.line());
    }

    @Test
    public void testWordsAndOffsetsUnchanged() {
        String[] args = { "a", "", "bc" };
        LazyParsedLine lazy = new LazyParsedLine("cmd", args, wordsFor("cmd", args));
        assertEquals(4, lazy.words().size());
        assertEquals(0, lazy.words().get(0).lineIndex());
        assertEquals(4, lazy.words().get(1).lineIndex());
        assertEquals(6, lazy.words().get(2).lineIndex());
        assertEquals(7, lazy.words().get(3).lineIndex());
        assertTrue(lazy.hasWords());
    }
}
