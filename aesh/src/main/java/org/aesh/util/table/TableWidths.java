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
package org.aesh.util.table;

import org.aesh.terminal.utils.WcWidth;

/**
 * Terminal display-width measurement, truncation and padding for table
 * content. Counts columns the way a terminal does: ANSI escape sequences
 * consume no columns, East Asian wide characters and emoji consume two,
 * and combining marks consume none (#786). All traversal is by code
 * point, so truncation can never split a surrogate pair.
 */
final class TableWidths {

    private static final char ESC = '\u001B';
    private static final String RESET = "\u001B[0m";
    private static final String SHORT_RESET = "\u001B[m";

    private TableWidths() {
    }

    /**
     * Visible column width of the text: escape sequences skipped, every
     * other code point measured with {@link WcWidth} (controls count as
     * zero rather than shrinking the column into the negative).
     */
    static int displayWidth(String text) {
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
    static String truncate(String text, int maxWidth) {
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
     * Pads with trailing spaces to exactly {@code width} visible columns.
     */
    static String padRight(String text, int width) {
        return pad(text, width, true);
    }

    /**
     * Pads with leading spaces to exactly {@code width} visible columns.
     */
    static String padLeft(String text, int width) {
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
    private static int consumeEscape(String text, int offset) {
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
}
