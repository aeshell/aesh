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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.aesh.terminal.utils.ANSI;

/**
 * Constants and utility methods for table border characters.
 *
 * <pre>
 *   HTL HBH HTI HBH HTR
 *   HBV  h  HCS  h  HBV
 *   TTL TTH TTI TTH TTR
 *   TBV  v  TCS  v  TBV
 *   RSL RSH RSI RSH RSR
 *   TBV  v  TBS  v  TBV
 *   TBL TBH TBI TBH TBR
 *
 *   minimum requirements are HORIZONTAL, VERTICAL, and INTERSECT
 *   HORIZONTAL -> TTH
 *   VERTICAL -> HCS, TCS
 *   INTERSECT -> TTI
 * </pre>
 */
public final class TableCharacters {

    // Header top border
    public static final String HEADER_TOP_LEFT = "HTL";
    public static final String HEADER_TOP_RIGHT = "HTR";
    public static final String HEADER_TOP_INTERSECT = "HTI";

    // Header column separator and border
    public static final String HEADER_COLUMN_SEPARATOR = "HCS";
    public static final String HEADER_BORDER_VERTICAL = "HBV";
    public static final String HEADER_BORDER_HORIZONTAL = "HBH";

    // Table top border
    public static final String TABLE_TOP_LEFT = "TTL";
    public static final String TABLE_TOP_RIGHT = "TTR";
    public static final String TABLE_TOP_HORIZONTAL = "TTH";
    public static final String TABLE_TOP_INTERSECT = "TTI";

    // Table body
    public static final String TABLE_BORDER_VERTICAL = "TBV";
    public static final String TABLE_BORDER_HORIZONTAL = "TBH";
    public static final String TABLE_COLUMN_SEPARATOR = "TCS";

    // Row separator
    public static final String ROW_SEPARATOR_LEFT = "RSL";
    public static final String ROW_SEPARATOR_HORIZONTAL = "RSH";
    public static final String ROW_SEPARATOR_INTERSECT = "RSI";
    public static final String ROW_SEPARATOR_RIGHT = "RSR";

    // Table bottom border
    public static final String TABLE_BOTTOM_LEFT = "TBL";
    public static final String TABLE_BOTTOM_RIGHT = "TBR";
    public static final String TABLE_BOTTOM_INTERSECT = "TBI";
    public static final String TABLE_BOTTOM_HORIZONTAL = "TBH";

    // Shorthand constants
    public static final String VERTICAL = "V";
    public static final String HORIZONTAL = "H";
    public static final String INTERSECT = "X";

    private TableCharacters() {
    }

    /**
     * Expands shorthand V/H/X entries to their full position names.
     *
     * @param map the character map potentially containing shorthand keys
     * @param border whether to also fill border positions
     * @return a new map with shorthands expanded to full position names
     */
    public static Map<String, String> convertToFullNames(Map<String, String> map, boolean border) {
        Map<String, String> rtrn = new HashMap<>(map);
        if (rtrn.containsKey(VERTICAL)) {
            String v = rtrn.get(VERTICAL);
            rtrn.remove(VERTICAL);
            rtrn.putIfAbsent(HEADER_COLUMN_SEPARATOR, v);
            rtrn.putIfAbsent(TABLE_COLUMN_SEPARATOR, v);
            if (border) {
                rtrn.putIfAbsent(HEADER_BORDER_VERTICAL, v);
                rtrn.putIfAbsent(TABLE_BORDER_VERTICAL, v);
            }
        }
        if (rtrn.containsKey(HORIZONTAL)) {
            String h = rtrn.get(HORIZONTAL);
            rtrn.remove(HORIZONTAL);
            rtrn.putIfAbsent(TABLE_TOP_HORIZONTAL, h);
            if (border) {
                rtrn.putIfAbsent(HEADER_BORDER_HORIZONTAL, h);
                rtrn.putIfAbsent(TABLE_BORDER_HORIZONTAL, h);
            }
        }
        if (rtrn.containsKey(INTERSECT)) {
            String i = rtrn.get(INTERSECT);
            rtrn.remove(INTERSECT);
            rtrn.putIfAbsent(TABLE_TOP_INTERSECT, i);
            if (border) {
                rtrn.putIfAbsent(HEADER_TOP_LEFT, i);
                rtrn.putIfAbsent(HEADER_TOP_RIGHT, i);
                rtrn.putIfAbsent(HEADER_TOP_INTERSECT, i);
                rtrn.putIfAbsent(TABLE_TOP_LEFT, i);
                rtrn.putIfAbsent(TABLE_TOP_RIGHT, i);
                rtrn.putIfAbsent(TABLE_TOP_INTERSECT, i);
                rtrn.putIfAbsent(TABLE_BOTTOM_LEFT, i);
                rtrn.putIfAbsent(TABLE_BOTTOM_RIGHT, i);
                rtrn.putIfAbsent(TABLE_BOTTOM_INTERSECT, i);
            }
        }
        return rtrn;
    }

    /**
     * Parses a visual template string into a character map.
     * The template consists of 5-character lines representing different parts of the table border.
     * Supports 3, 5, or 6-line templates for varying levels of detail.
     *
     * @param template a multi-line visual template string
     * @param defaultMap fallback map if the template is invalid
     * @return a map of position names to border characters
     */
    public static Map<String, String> templateToMap(String template, Map<String, String> defaultMap) {
        if (template == null || template.isEmpty()) {
            return new HashMap<>(defaultMap);
        }
        String[] split = template.split(System.lineSeparator());
        // Filter out empty lines (from leading/trailing newlines)
        List<String> lines = new ArrayList<>();
        for (String s : split) {
            if (!s.trim().isEmpty()) {
                lines.add(s.trim());
            }
        }

        for (String s : lines) {
            if (s.length() != 5) {
                return defaultMap;
            }
        }

        if (lines.size() < 3) {
            return defaultMap;
        }
        Map<String, String> map = new HashMap<>();
        if (lines.size() == 3) {
            String row = lines.get(0);
            map.put(HEADER_BORDER_VERTICAL, row.charAt(0) + "");
            map.put(HEADER_COLUMN_SEPARATOR, row.charAt(2) + "");
            row = lines.get(1);
            map.put(TABLE_TOP_LEFT, row.charAt(0) + "");
            map.put(TABLE_TOP_HORIZONTAL, row.charAt(1) + "");
            map.put(TABLE_TOP_INTERSECT, row.charAt(2) + "");
            map.put(TABLE_TOP_RIGHT, row.charAt(4) + "");
            row = lines.get(2);
            map.put(TABLE_BORDER_VERTICAL, row.charAt(0) + "");
            map.put(TABLE_COLUMN_SEPARATOR, row.charAt(2) + "");
        } else if (lines.size() == 5) {
            String line = lines.get(0);
            map.put(HEADER_TOP_LEFT, line.charAt(0) + "");
            map.put(HEADER_BORDER_HORIZONTAL, line.charAt(1) + "");
            map.put(HEADER_TOP_INTERSECT, line.charAt(2) + "");
            map.put(HEADER_TOP_RIGHT, line.charAt(4) + "");
            line = lines.get(1);
            map.put(HEADER_BORDER_VERTICAL, line.charAt(0) + "");
            map.put(HEADER_COLUMN_SEPARATOR, line.charAt(2) + "");
            line = lines.get(2);
            map.put(TABLE_TOP_LEFT, line.charAt(0) + "");
            map.put(TABLE_TOP_HORIZONTAL, line.charAt(1) + "");
            map.put(TABLE_TOP_INTERSECT, line.charAt(2) + "");
            map.put(TABLE_TOP_RIGHT, line.charAt(4) + "");
            line = lines.get(3);
            map.put(TABLE_BORDER_VERTICAL, line.charAt(0) + "");
            map.put(TABLE_COLUMN_SEPARATOR, line.charAt(2) + "");
            line = lines.get(4);
            map.put(TABLE_BOTTOM_LEFT, line.charAt(0) + "");
            map.put(TABLE_BORDER_HORIZONTAL, line.charAt(1) + "");
            map.put(TABLE_BOTTOM_INTERSECT, line.charAt(2) + "");
            map.put(TABLE_BOTTOM_RIGHT, line.charAt(4) + "");
        } else if (lines.size() == 6) {
            String line = lines.get(0);
            map.put(HEADER_TOP_LEFT, line.charAt(0) + "");
            map.put(HEADER_BORDER_HORIZONTAL, line.charAt(1) + "");
            map.put(HEADER_TOP_INTERSECT, line.charAt(2) + "");
            map.put(HEADER_TOP_RIGHT, line.charAt(4) + "");
            line = lines.get(1);
            map.put(HEADER_BORDER_VERTICAL, line.charAt(0) + "");
            map.put(HEADER_COLUMN_SEPARATOR, line.charAt(2) + "");
            line = lines.get(2);
            map.put(TABLE_TOP_LEFT, line.charAt(0) + "");
            map.put(TABLE_TOP_HORIZONTAL, line.charAt(1) + "");
            map.put(TABLE_TOP_INTERSECT, line.charAt(2) + "");
            map.put(TABLE_TOP_RIGHT, line.charAt(4) + "");
            line = lines.get(3);
            map.put(TABLE_BORDER_VERTICAL, line.charAt(0) + "");
            map.put(TABLE_COLUMN_SEPARATOR, line.charAt(2) + "");
            line = lines.get(4);
            map.put(ROW_SEPARATOR_LEFT, line.charAt(0) + "");
            map.put(ROW_SEPARATOR_HORIZONTAL, line.charAt(1) + "");
            map.put(ROW_SEPARATOR_INTERSECT, line.charAt(2) + "");
            map.put(ROW_SEPARATOR_RIGHT, line.charAt(4) + "");
            line = lines.get(5);
            map.put(TABLE_BOTTOM_LEFT, line.charAt(0) + "");
            map.put(TABLE_BORDER_HORIZONTAL, line.charAt(1) + "");
            map.put(TABLE_BOTTOM_INTERSECT, line.charAt(2) + "");
            map.put(TABLE_BOTTOM_RIGHT, line.charAt(4) + "");
        } else {
            return defaultMap;
        }

        return map;
    }

    /**
     * Wraps each character value in the map with an ANSI prefix and RESET suffix.
     *
     * @param prefix the ANSI escape prefix (e.g. {@code ANSI.RED_TEXT})
     * @param map the character map to prefix
     * @return a new map with prefixed values
     */
    public static Map<String, String> prefix(String prefix, Map<String, String> map) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            result.put(entry.getKey(), prefix + entry.getValue() + ANSI.RESET);
        }
        return result;
    }

    /**
     * Checks whether the character map defines a complete outside border.
     */
    public static boolean hasOutsideBorder(Map<String, String> characters) {
        return Stream.of(HEADER_TOP_LEFT, HEADER_BORDER_HORIZONTAL, HEADER_TOP_INTERSECT, HEADER_TOP_RIGHT,
                HEADER_BORDER_VERTICAL, HEADER_COLUMN_SEPARATOR,
                TABLE_TOP_LEFT, TABLE_TOP_HORIZONTAL, TABLE_TOP_INTERSECT, TABLE_TOP_RIGHT,
                TABLE_BORDER_VERTICAL, TABLE_COLUMN_SEPARATOR,
                TABLE_BOTTOM_LEFT, TABLE_BOTTOM_HORIZONTAL, TABLE_BOTTOM_INTERSECT, TABLE_BOTTOM_RIGHT)
                .allMatch(characters::containsKey);
    }

    /**
     * Checks whether the character map defines row separators.
     */
    public static boolean hasRowSeparator(Map<String, String> characters) {
        return Stream.of(ROW_SEPARATOR_LEFT, ROW_SEPARATOR_HORIZONTAL,
                ROW_SEPARATOR_INTERSECT, ROW_SEPARATOR_RIGHT)
                .allMatch(characters::containsKey);
    }

    /**
     * Checks whether the character map has the minimum required entries for rendering a table.
     */
    public static boolean isValid(Map<String, String> characters) {
        return Stream.of(HEADER_COLUMN_SEPARATOR, TABLE_TOP_HORIZONTAL,
                TABLE_TOP_INTERSECT, TABLE_COLUMN_SEPARATOR)
                .allMatch(characters::containsKey);
    }

    /**
     * Splits a list of potentially multi-line header strings into aligned rows.
     *
     * <pre>
     * ["foo\nfoo","bar"] -&gt; [["foo","bar"], ["foo",""]]
     * </pre>
     *
     * @param input list of header strings, potentially containing line separators
     * @return list of rows, each row being a list of strings
     */
    public static List<List<String>> lineSplit(List<String> input) {
        List<List<String>> split = new ArrayList<>();
        for (String line : input) {
            split.add(Arrays.asList(line.split(System.lineSeparator())));
        }
        int maxRows = 1;
        for (List<String> s : split) {
            if (s.size() > maxRows) {
                maxRows = s.size();
            }
        }
        List<List<String>> rtrn = new ArrayList<>();
        for (int i = 0; i < maxRows; i++) {
            ArrayList<String> row = new ArrayList<>();
            for (List<String> splitRow : split) {
                if (splitRow.size() > i) {
                    row.add(splitRow.get(i));
                } else {
                    row.add("");
                }
            }
            rtrn.add(row);
        }
        return rtrn;
    }
}
