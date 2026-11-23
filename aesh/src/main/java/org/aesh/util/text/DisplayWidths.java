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
package org.aesh.util.text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.aesh.terminal.utils.WcWidth;

/**
 * Terminal display-width measurement, truncation, wrapping and padding.
 * Counts columns the way a terminal does: ANSI escape sequences consume
 * no columns, East Asian wide characters and emoji consume two, and
 * combining marks consume none (#786). All traversal is by code point,
 * so splitting a surrogate pair is impossible.
 */
public final class DisplayWidths {

    private static final char ESC = '\u001B';
    private static final String RESET = "\u001B[0m";
    private static final String SHORT_RESET = "\u001B[m";

    private DisplayWidths() {
    }

    /**
     * Visible column width of the text: escape sequences skipped, every
     * other code point measured with {@link WcWidth} (controls count as
     * zero rather than shrinking the column into the negative).
     */
    public static int displayWidth(String text) {
        int width = 0;
        for (int i = 0; i < text.length();) {
            if (text.charAt(i) == ESC) {
                int end = consumeEscape(text, i);
                if (end > i) {
                    i = end;
                    continue;
                }
                width++;
                i++;
                continue;
            }
            int codePoint = text.codePointAt(i);
            int charWidth = WcWidth.width(codePoint);
            if (charWidth > 0) {
                width += charWidth;
            }
            i += Character.charCount(codePoint);
        }
        return width;
    }

    /**
     * Cuts the text to at most {@code maxWidth} visible columns. Escape
     * sequences inside the kept prefix are preserved; if the cut drops a
     * reset that closes styling opened in the prefix, one reset is
     * re-appended so borders and padding stay unstyled.
     */
    public static String truncate(String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (displayWidth(text) <= maxWidth) {
            return text;
        }
        StringBuilder kept = new StringBuilder();
        int width = 0;
        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) == ESC) {
                int end = consumeEscape(text, i);
                if (end < 0) {
                    break;
                }
                kept.append(text, i, end);
                i = end;
                continue;
            }
            int codePoint = text.codePointAt(i);
            int charCount = Character.charCount(codePoint);
            int charWidth = WcWidth.width(codePoint);
            if (charWidth < 0) {
                charWidth = 0;
            }
            if (width + charWidth > maxWidth) {
                break;
            }
            kept.append(text, i, i + charCount);
            width += charWidth;
            i += charCount;
        }
        String tail = text.substring(i);
        if (kept.indexOf(String.valueOf(ESC)) >= 0
                && (tail.contains(RESET) || tail.contains(SHORT_RESET))) {
            kept.append(tail.contains(RESET) ? RESET : SHORT_RESET);
        }
        return kept.toString();
    }

    /**
     * Wraps text to lines of at most {@code maxWidth} visible columns,
     * breaking at spaces where possible and hard-cutting words without
     * them, like the historical character-based wrapping. Escape sequences
     * ride along with their line without consuming columns; styling opened
     * on one line is closed and reopened on the next so no line bleeds
     * into its neighbours. A single token wider than the limit is taken
     * whole rather than split or dropped.
     */
    public static List<String> wrap(String text, int maxWidth) {
        if (maxWidth <= 0 || displayWidth(text) <= maxWidth) {
            return Collections.singletonList(text);
        }
        List<Token> tokens = tokenize(text);
        List<String> lines = new ArrayList<>();
        ArrayDeque<Token> remaining = new ArrayDeque<>(tokens);
        while (!remaining.isEmpty()) {
            List<Token> line = new ArrayList<>();
            int lineWidth = 0;
            List<String> lineOpens = new ArrayList<>();
            while (!remaining.isEmpty()) {
                Token current = remaining.peek();
                if (!current.escape && lineWidth + current.width > maxWidth && !line.isEmpty()) {
                    break;
                }
                remaining.poll();
                line.add(current);
                if (!current.escape) {
                    lineWidth += current.width;
                }
            }
            // Back up to a word boundary when the line overflowed. The scan
            // includes the overflowing token, like the historical wrapping,
            // and the break space itself is dropped.
            if (!remaining.isEmpty()) {
                if (remaining.peek().space) {
                    remaining.poll();
                } else {
                    int space = line.size() - 1;
                    while (space > 0 && !line.get(space).space) {
                        space--;
                    }
                    if (space > 0) {
                        for (int i = line.size() - 1; i > space; i--) {
                            remaining.addFirst(line.remove(i));
                        }
                        line.remove(space);
                    }
                }
            }
            // Recompute the styling this line actually opens: tokens
            // pushed back above must not count as opened here.
            lineOpens.clear();
            for (Token token : line) {
                if (!token.escape) {
                    continue;
                }
                if (token.reset) {
                    lineOpens.clear();
                } else if (token.sgrOpen) {
                    lineOpens.add(token.text);
                }
            }
            if (!remaining.isEmpty() && !lineOpens.isEmpty() && containsReset(remaining)) {
                line.add(new Token(RESET, 0, false, true, true, false));
                for (int i = lineOpens.size() - 1; i >= 0; i--) {
                    remaining.addFirst(
                            new Token(lineOpens.get(i), 0, false, true, false, true));
                }
            }
            lines.add(buildLine(line));
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    private static boolean containsReset(ArrayDeque<Token> tokens) {
        for (Token token : tokens) {
            if (token.reset) {
                return true;
            }
        }
        return false;
    }

    private static String buildLine(List<Token> line) {
        int start = 0;
        while (start < line.size() && isTrimmable(line.get(start))) {
            start++;
        }
        int end = line.size();
        while (end > start && isTrimmable(line.get(end - 1))) {
            end--;
        }
        StringBuilder result = new StringBuilder();
        for (int i = start; i < end; i++) {
            result.append(line.get(i).text);
        }
        return result.toString();
    }

    private static boolean isTrimmable(Token token) {
        return token.space;
    }

    /**
     * Pads with trailing spaces to exactly {@code width} visible columns.
     */
    public static String padRight(String text, int width) {
        return pad(text, width, true);
    }

    /**
     * Pads with leading spaces to exactly {@code width} visible columns.
     */
    public static String padLeft(String text, int width) {
        return pad(text, width, false);
    }

    private static String pad(String text, int width, boolean trailing) {
        int missing = width - displayWidth(text);
        if (missing <= 0) {
            return text;
        }
        StringBuilder padded = new StringBuilder(text.length() + missing);
        if (!trailing) {
            appendSpaces(padded, missing);
        }
        padded.append(text);
        if (trailing) {
            appendSpaces(padded, missing);
        }
        return padded.toString();
    }

    private static void appendSpaces(StringBuilder target, int count) {
        for (int i = 0; i < count; i++) {
            target.append(' ');
        }
    }

    /**
     * Consumes one ANSI escape sequence starting at {@code offset} (which
     * must hold ESC): CSI ({@code ESC [ ... final}) or OSC
     * ({@code ESC ] ... BEL} or {@code ESC ] ... ESC \}). Returns the index
     * just past the sequence, or -1 when nothing terminates it.
     */
    public static int consumeEscape(String text, int offset) {
        if (offset + 1 >= text.length()) {
            return -1;
        }
        char introducer = text.charAt(offset + 1);
        if (introducer == '[') {
            int i = offset + 2;
            while (i < text.length() && isCsiParameter(text.charAt(i))) {
                i++;
            }
            while (i < text.length() && isCsiIntermediate(text.charAt(i))) {
                i++;
            }
            if (i < text.length() && isCsiFinal(text.charAt(i))) {
                return i + 1;
            }
            return -1;
        }
        if (introducer == ']') {
            int i = offset + 2;
            while (i < text.length()) {
                if (text.charAt(i) == '\u0007') {
                    return i + 1;
                }
                if (text.charAt(i) == ESC && i + 1 < text.length() && text.charAt(i + 1) == '\\') {
                    return i + 2;
                }
                i++;
            }
            return -1;
        }
        return -1;
    }

    private static boolean isCsiParameter(char c) {
        return (c >= '0' && c <= '9') || c == ';' || c == '?' || c == '!' || c == '"'
                || c == '$' || c == '\'' || c == '(';
    }

    private static boolean isCsiIntermediate(char c) {
        return c >= ' ' && c <= '/';
    }

    private static boolean isCsiFinal(char c) {
        return c >= '@' && c <= '~';
    }

    private static boolean isSgrReset(String sequence) {
        if (sequence.length() < 3 || sequence.charAt(1) != '['
                || sequence.charAt(sequence.length() - 1) != 'm') {
            return false;
        }
        String inner = sequence.substring(2, sequence.length() - 1);
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c != '0' && c != ';') {
                return false;
            }
        }
        return true;
    }

    private static boolean isSgrOpen(String sequence) {
        return sequence.length() > 2 && sequence.charAt(1) == '['
                && sequence.charAt(sequence.length() - 1) == 'm'
                && !isSgrReset(sequence);
    }

    private static final class Token {
        final String text;
        final int width;
        final boolean space;
        final boolean escape;
        final boolean reset;
        final boolean sgrOpen;

        Token(String text, int width, boolean space,
                boolean escape, boolean reset, boolean sgrOpen) {
            this.text = text;
            this.width = width;
            this.space = space;
            this.escape = escape;
            this.reset = reset;
            this.sgrOpen = sgrOpen;
        }
    }

    private static List<Token> tokenize(String text) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == ESC) {
                int end = consumeEscape(text, i);
                if (end < 0) {
                    // Unterminated sequence: keep historical
                    // cell-per-character behavior for the ESC itself.
                    tokens.add(new Token(String.valueOf(c), 1, false, false, false, false));
                    i++;
                    continue;
                }
                String sequence = text.substring(i, end);
                tokens.add(new Token(sequence, 0, false, true, isSgrReset(sequence), isSgrOpen(sequence)));
                i = end;
                continue;
            }
            int codePoint = text.codePointAt(i);
            int charCount = Character.charCount(codePoint);
            int width = WcWidth.width(codePoint);
            if (width < 0) {
                width = 1; // control characters keep a single column
            }
            tokens.add(new Token(text.substring(i, i + charCount), width,
                    codePoint == ' ', false, false, false));
            i += charCount;
        }
        return tokens;
    }
}
