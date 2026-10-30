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
package org.aesh.command.impl.completer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.aesh.readline.fuzzy.FuzzyResult;
import org.aesh.readline.fuzzy.FuzzyScheme;
import org.aesh.readline.fuzzy.FuzzyScorer;
import org.aesh.terminal.formatting.TerminalString;
import org.aesh.terminal.utils.Parser;

/**
 * Fuzzy fallback matcher for tab completion.
 * <p>
 * When prefix matching finds no candidates, this matcher scores the full
 * candidate set against the typed word using the fzf-derived
 * {@link FuzzyScorer} with the {@link FuzzyScheme#DEFAULT} scheme. Matching
 * is case-insensitive and camelCase transitions are rewarded, so an
 * abbreviation like {@code gCI} matches {@code getCursorIndex} and a typo
 * like {@code comit} matches {@code commit}.
 * <p>
 * Results are sorted by score, best match first, with ties kept in
 * candidate order. Inputs shorter than {@link #MIN_INPUT_LENGTH} are not
 * fuzzy-matched to avoid low-quality matches for single characters.
 *
 * @see FuzzyScorer
 */
public final class FuzzyCompletionMatcher {

    /**
     * Minimum length of the typed word for fuzzy matching to kick in.
     */
    public static final int MIN_INPUT_LENGTH = 2;

    private static final FuzzyCompletionMatcher INSTANCE = new FuzzyCompletionMatcher();

    private final FuzzyScorer scorer;

    private FuzzyCompletionMatcher() {
        scorer = new FuzzyScorer(FuzzyScheme.DEFAULT, false);
    }

    /**
     * Get the shared matcher instance.
     *
     * @return the shared matcher
     */
    public static FuzzyCompletionMatcher getInstance() {
        return INSTANCE;
    }

    /**
     * Check whether the typed word is long enough for fuzzy matching.
     *
     * @param input the typed word
     * @return true if fuzzy matching may run for this input
     */
    public static boolean isEligible(String input) {
        return input != null && input.length() >= MIN_INPUT_LENGTH;
    }

    /**
     * Fuzzy-match the typed word against all candidates.
     *
     * @param input the typed word
     * @param candidates the full candidate set
     * @return matching candidates sorted by score (best first), never null
     */
    public List<TerminalString> fuzzyMatches(String input, Collection<String> candidates) {
        List<TerminalString> matches = new ArrayList<>();
        if (!isEligible(input) || candidates == null || candidates.isEmpty())
            return matches;

        int[] pattern = Parser.toCodePoints(input);
        List<ScoredCandidate> scored = new ArrayList<>(candidates.size());
        for (String candidate : candidates) {
            if (candidate == null || candidate.isEmpty())
                continue;
            FuzzyResult result = scorer.score(Parser.toCodePoints(candidate), pattern, false);
            if (result.isMatch())
                scored.add(new ScoredCandidate(candidate, result.score));
        }

        scored.sort((a, b) -> Integer.compare(b.score, a.score));

        for (ScoredCandidate candidate : scored)
            matches.add(new TerminalString(candidate.name, true));
        return matches;
    }

    private static final class ScoredCandidate {
        private final String name;
        private final int score;

        private ScoredCandidate(String name, int score) {
            this.name = name;
            this.score = score;
        }
    }
}
