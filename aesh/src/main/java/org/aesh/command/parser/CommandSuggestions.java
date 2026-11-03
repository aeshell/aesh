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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Finds close matches for an unknown command name ("did you mean?").
 * <p>
 * Used by {@link SubcommandNotFoundException} so every entry point (the
 * runner, the interactive console, and direct {@code CommandRuntime} use)
 * reports the same suggestions. Applications with a custom
 * {@code CommandNotFoundHandler} can reuse it instead of writing their own.
 * <p>
 * Matching is case-insensitive edit distance: prefix abbreviations rank
 * first, then the smallest Levenshtein distance. For example {@code exprot}
 * finds {@code export} and {@code rn} finds {@code run}.
 */
public final class CommandSuggestions {

    /** Maximum number of suggestions returned. */
    public static final int MAX_SUGGESTIONS = 3;

    private CommandSuggestions() {
    }

    /**
     * Find close matches for {@code unknown} among {@code available}.
     *
     * @param unknown the mistyped name, may be null
     * @param available the valid names, may be null
     * @return up to {@link #MAX_SUGGESTIONS} matches, best first, never null
     */
    public static List<String> suggest(String unknown, Collection<String> available) {
        return suggest(unknown, available, MAX_SUGGESTIONS);
    }

    /**
     * Find close matches for {@code unknown} among {@code available}.
     *
     * @param unknown the mistyped name, may be null
     * @param available the valid names, may be null
     * @param maxSuggestions maximum number of matches, {@code <= 0} gives none
     * @return up to {@code maxSuggestions} matches, best first, never null
     */
    public static List<String> suggest(String unknown, Collection<String> available, int maxSuggestions) {
        if (unknown == null || unknown.isEmpty() || available == null || available.isEmpty() || maxSuggestions <= 0)
            return Collections.emptyList();
        String typed = unknown.toLowerCase(Locale.ROOT);
        int threshold = Math.max(2, typed.length() / 3);
        List<Scored> scored = new ArrayList<>();
        for (String name : available) {
            if (name == null || name.isEmpty())
                continue;
            String candidate = name.toLowerCase(Locale.ROOT);
            int distance = candidate.startsWith(typed) ? 0 : levenshtein(typed, candidate);
            if (distance <= threshold)
                scored.add(new Scored(name, distance));
        }
        scored.sort((a, b) -> a.distance != b.distance
                ? Integer.compare(a.distance, b.distance)
                : a.name.compareTo(b.name));
        List<String> suggestions = new ArrayList<>(Math.min(maxSuggestions, scored.size()));
        for (int i = 0; i < scored.size() && suggestions.size() < maxSuggestions; i++)
            suggestions.add(scored.get(i).name);
        return Collections.unmodifiableList(suggestions);
    }

    private static int levenshtein(String a, String b) {
        int[] previous = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++)
            previous[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            int[] current = new int[b.length() + 1];
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int substitution = previous[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(substitution, Math.min(previous[j] + 1, current[j - 1] + 1));
            }
            previous = current;
        }
        return previous[b.length()];
    }

    private static final class Scored {
        final String name;
        final int distance;

        Scored(String name, int distance) {
            this.name = name;
            this.distance = distance;
        }
    }
}
