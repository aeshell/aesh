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
package org.aesh.command.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class CommandSuggestionsTest {

    private static final List<String> CAMEL = Arrays.asList(
            "export", "explain", "run", "config", "debug", "list");

    @Test
    public void transposedLettersFindExport() {
        assertEquals(Collections.singletonList("export"),
                CommandSuggestions.suggest("exprot", CAMEL));
    }

    @Test
    public void missingLetterFindsRun() {
        List<String> suggestions = CommandSuggestions.suggest("rn", CAMEL);
        assertTrue("rn should suggest run, got: " + suggestions,
                suggestions.contains("run"));
    }

    @Test
    public void prefixAbbreviationRanksFirst() {
        List<String> suggestions = CommandSuggestions.suggest("expo", CAMEL);
        assertTrue("expo should suggest export, got: " + suggestions,
                !suggestions.isEmpty() && suggestions.get(0).equals("export"));
    }

    @Test
    public void matchesAreCappedAndOrdered() {
        List<String> many = Arrays.asList("aaa", "aaab", "aaac", "aaad", "zzzz");
        List<String> suggestions = CommandSuggestions.suggest("aaa", many);
        assertEquals(CommandSuggestions.MAX_SUGGESTIONS, suggestions.size());
        assertEquals(Arrays.asList("aaa", "aaab", "aaac"), suggestions);
    }

    @Test
    public void unrelatedNameGivesNothing() {
        assertTrue(CommandSuggestions.suggest("bogus", CAMEL).isEmpty());
    }

    @Test
    public void matchingIgnoresCase() {
        assertEquals(Collections.singletonList("export"),
                CommandSuggestions.suggest("EXPORT", CAMEL));
    }

    @Test
    public void nullAndEmptyGiveNothing() {
        assertTrue(CommandSuggestions.suggest(null, CAMEL).isEmpty());
        assertTrue(CommandSuggestions.suggest("", CAMEL).isEmpty());
        assertTrue(CommandSuggestions.suggest("run", null).isEmpty());
        assertTrue(CommandSuggestions.suggest("run", Collections.emptyList()).isEmpty());
        assertTrue(CommandSuggestions.suggest("run", CAMEL, 0).isEmpty());
    }

    @Test
    public void exceptionMessageSuggestsCloseMatch() {
        SubcommandNotFoundException e = new SubcommandNotFoundException("camel", "exprot", CAMEL);
        assertEquals(Collections.singletonList("export"), e.getSuggestions());
        assertTrue("Message was: " + e.getMessage(),
                e.getMessage().contains("Did you mean: export?"));
    }

    @Test
    public void exceptionMessageListsAvailableWithoutMatch() {
        SubcommandNotFoundException e = new SubcommandNotFoundException("camel", "bogus", CAMEL);
        assertTrue(e.getSuggestions().isEmpty());
        assertTrue("Message was: " + e.getMessage(),
                e.getMessage().contains("Available: "));
        assertTrue("Message was: " + e.getMessage(),
                e.getMessage().contains("See 'camel --help'."));
    }

    @Test
    public void exceptionMessageJoinsMultipleSuggestions() {
        SubcommandNotFoundException e = new SubcommandNotFoundException("camel", "exprot",
                Arrays.asList("export", "explain"),
                Arrays.asList("export", "explain"));
        assertTrue("Message was: " + e.getMessage(),
                e.getMessage().contains("Did you mean: export or explain?"));
    }
}
