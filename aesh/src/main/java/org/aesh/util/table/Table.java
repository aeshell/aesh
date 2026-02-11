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

import static org.aesh.util.table.TableCharacters.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Renders tabular data as formatted text with configurable border styles.
 *
 * <p>
 * Usage via static methods:
 * </p>
 *
 * <pre>
 * String output = Table.render(80, users, Arrays.asList("Name", "Email"),
 *         Arrays.asList(u -&gt; u.getName(), u -&gt; u.getEmail()));
 * </pre>
 *
 * <p>
 * Usage via builder:
 * </p>
 *
 * <pre>
 * String output = Table.&lt;User&gt; builder()
 *         .maxWidth(80)
 *         .style(TableStyle.DUCKDB)
 *         .column("Name", u -&gt; u.getName())
 *         .column("Email", u -&gt; u.getEmail())
 *         .build()
 *         .render(userList);
 * </pre>
 */
public class Table<T> {

    private final int maxWidth;
    private final List<String> headers;
    private final List<Function<T, Object>> accessors;
    private final Map<String, String> characters;

    private Table(int maxWidth, List<String> headers, List<Function<T, Object>> accessors,
            Map<String, String> characters) {
        this.maxWidth = maxWidth;
        this.headers = headers;
        this.accessors = accessors;
        this.characters = characters;
    }

    /**
     * Renders the given values using this table's configuration.
     */
    public String render(List<T> values) {
        return render(maxWidth, values, headers, accessors, characters);
    }

    /**
     * Renders a table using the default DUCKDB style.
     */
    public static <T> String render(int maxWidth, List<T> values,
            List<String> headers, List<Function<T, Object>> accessors) {
        return render(maxWidth, values, headers, accessors, TableStyle.DUCKDB.characters());
    }

    /**
     * Renders a table with the specified border characters.
     */
    public static <T> String render(int maxWidth, List<T> values,
            List<String> headers, List<Function<T, Object>> accessors,
            Map<String, String> characters) {

        characters = isValid(characters) ? characters : TableStyle.SQLITE.characters();
        boolean outsideBorder = hasOutsideBorder(characters);
        List<List<String>> headerRows = lineSplit(headers);
        int columnCount = Math.min(headers.size(), accessors.size());
        List<Object[]> rows = new ArrayList<>();

        // Calculate initial column widths from headers
        int[] columnWidths = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            String header = headers.get(i);
            String[] parts = header.split(System.lineSeparator());
            int max = 0;
            for (String part : parts) {
                if (part.length() > max) {
                    max = part.length();
                }
            }
            columnWidths[i] = max;
        }

        String[] columnFormats = new String[columnCount];

        for (int vIdx = 0; vIdx < values.size(); vIdx++) {
            T value = values.get(vIdx);
            List<Object> rowCells = new ArrayList<>();
            for (int a = 0; a < columnCount; a++) {
                Object c = accessors.get(a).apply(value);
                if (c == null) {
                    c = "null";
                } else if (c instanceof Long || c instanceof Integer) {
                    if (columnFormats[a] == null) {
                        columnFormats[a] = "d";
                    }
                } else if (c instanceof Double || c instanceof Float) {
                    if (columnFormats[a] == null || columnFormats[a].equals("d")) {
                        columnFormats[a] = "f";
                    }
                    c = String.format("%.2f", c);
                } else {
                    columnFormats[a] = "s";
                }
                int cellWidth = columnFormats[a] == null ? "null".length() : c.toString().length();
                if (cellWidth > columnWidths[a]) {
                    columnWidths[a] = cellWidth;
                }
                rowCells.add(c);
            }
            rows.add(rowCells.toArray());
        }
        for (int i = 0; i < columnFormats.length; i++) {
            if (columnFormats[i] == null) {
                columnFormats[i] = "s";
            }
        }

        StringBuilder topBorderFormat = new StringBuilder();
        StringBuilder bottomBorderFormat = new StringBuilder();
        StringBuilder headerFormat = new StringBuilder();
        StringBuilder rowFormat = new StringBuilder();

        if (outsideBorder) {
            topBorderFormat.append(String.format("%s ", characters.get(HEADER_TOP_LEFT)));
            bottomBorderFormat.append(String.format("%s ", characters.get(TABLE_BOTTOM_LEFT)));
            headerFormat.append(String.format("%s ", characters.get(HEADER_BORDER_VERTICAL)));
            rowFormat.append(String.format("%s ", characters.get(TABLE_BORDER_VERTICAL)));
        }

        for (int i = 0; i < columnCount; i++) {
            if (i > 0) {
                topBorderFormat.append(String.format(" %s ", characters.get(HEADER_TOP_INTERSECT)));
                bottomBorderFormat.append(String.format(" %s ", characters.get(TABLE_BOTTOM_INTERSECT)));
                headerFormat.append(String.format(" %s ", characters.get(HEADER_BORDER_VERTICAL)));
                rowFormat.append(String.format(" %s ", characters.get(TABLE_COLUMN_SEPARATOR)));
            }
            int width = columnWidths[i];
            if (columnFormats[i] == null) {
                columnFormats[i] = "s";
            }
            String format = columnFormats[i];

            headerFormat.append("%" + width + "s");
            topBorderFormat.append("%" + width + "s");
            bottomBorderFormat.append("%" + width + "s");
            switch (format) {
                case "d":
                    rowFormat.append("%" + width + "d");
                    break;
                case "f":
                    rowFormat.append("%" + width + "s");
                    break;
                default:
                    rowFormat.append("%-" + width + "s");
            }
        }

        if (outsideBorder) {
            topBorderFormat.append(String.format(" %s", characters.get(HEADER_TOP_RIGHT)));
            bottomBorderFormat.append(String.format(" %s", characters.get(TABLE_BOTTOM_RIGHT)));
            headerFormat.append(String.format(" %s", characters.get(HEADER_BORDER_VERTICAL)));
            rowFormat.append(String.format(" %s", characters.get(TABLE_BORDER_VERTICAL)));
        }

        topBorderFormat.append("%n");
        bottomBorderFormat.append("%n");
        headerFormat.append("%n");
        rowFormat.append("%n");

        StringBuilder rtrn = new StringBuilder();

        // Top border
        if (outsideBorder) {
            rtrn.append(String.format(topBorderFormat.toString(),
                    Collections.nCopies(columnCount, "").toArray())
                    .replace(" ", characters.get(HEADER_BORDER_HORIZONTAL)));
        }

        // Headers
        if (!headers.isEmpty()) {
            for (int r = 0; r < headerRows.size(); r++) {
                List<String> headerRow = headerRows.get(r);
                StringBuilder row = new StringBuilder();
                if (outsideBorder) {
                    row.append(characters.get(HEADER_BORDER_VERTICAL));
                    row.append(" ");
                }
                for (int c = 0; c < columnCount; c++) {
                    int width = columnWidths[c];
                    String header = headerRow.get(c);
                    int leftPad = (width - header.length()) / 2;
                    int remainder = width - header.length() - leftPad;
                    if (c > 0) {
                        row.append(" ");
                        row.append(characters.get(HEADER_COLUMN_SEPARATOR));
                        row.append(" ");
                    }
                    if (leftPad > 0) {
                        row.append(String.format("%" + leftPad + "s", ""));
                    }
                    row.append(header);
                    if (remainder > 0) {
                        row.append(String.format("%" + remainder + "s", ""));
                    }
                }
                if (outsideBorder) {
                    row.append(" ");
                    row.append(characters.get(HEADER_BORDER_VERTICAL));
                }
                rtrn.append(row.toString());
                rtrn.append(System.lineSeparator());
            }

            // Header-body separator
            if (outsideBorder) {
                rtrn.append(
                        String.format(
                                topBorderFormat.toString(),
                                Collections.nCopies(columnCount, "").toArray())
                                .replace(" ", characters.get(TABLE_TOP_HORIZONTAL))
                                .replace(characters.get(HEADER_TOP_LEFT), characters.get(TABLE_TOP_LEFT))
                                .replace(characters.get(HEADER_TOP_RIGHT), characters.get(TABLE_TOP_RIGHT))
                                .replace(characters.get(HEADER_TOP_INTERSECT),
                                        characters.get(TABLE_TOP_INTERSECT)));
            } else {
                rtrn.append(String.format(headerFormat.toString(),
                        Collections.nCopies(columnCount, "").toArray())
                        .replace(" ", characters.get(TABLE_TOP_HORIZONTAL))
                        .replace(characters.get(HEADER_COLUMN_SEPARATOR),
                                characters.get(TABLE_COLUMN_SEPARATOR)));
            }
        }

        // Data rows
        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            rtrn.append(String.format(rowFormat.toString(), row));
            if (i < rows.size() - 1 && hasRowSeparator(characters)) {
                rtrn.append(characters.get(ROW_SEPARATOR_LEFT));
                rtrn.append(characters.get(ROW_SEPARATOR_HORIZONTAL));
                for (int c = 0; c < columnCount; c++) {
                    int width = columnWidths[c];
                    if (c > 0) {
                        rtrn.append(characters.get(ROW_SEPARATOR_HORIZONTAL));
                        rtrn.append(characters.get(ROW_SEPARATOR_INTERSECT));
                        rtrn.append(characters.get(ROW_SEPARATOR_HORIZONTAL));
                    }
                    rtrn.append(repeat(characters.get(ROW_SEPARATOR_HORIZONTAL), width));
                }
                rtrn.append(characters.get(ROW_SEPARATOR_HORIZONTAL));
                rtrn.append(characters.get(ROW_SEPARATOR_RIGHT));
                rtrn.append(System.lineSeparator());
            }
        }

        // Bottom border
        if (outsideBorder) {
            rtrn.append(String.format(topBorderFormat.toString(),
                    Collections.nCopies(columnCount, "").toArray())
                    .replace(characters.get(HEADER_TOP_LEFT), characters.get(TABLE_BOTTOM_LEFT))
                    .replace(characters.get(HEADER_TOP_RIGHT), characters.get(TABLE_BOTTOM_RIGHT))
                    .replace(characters.get(HEADER_TOP_INTERSECT), characters.get(TABLE_BOTTOM_INTERSECT))
                    .replace(" ", characters.get(TABLE_BORDER_HORIZONTAL)));
        }

        return rtrn.toString();
    }

    /**
     * Creates a new builder for constructing a Table.
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    private static String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * Builder for constructing Table instances with a fluent API.
     */
    public static class Builder<T> {
        private int maxWidth = 80;
        private final List<String> headers = new ArrayList<>();
        private final List<Function<T, Object>> accessors = new ArrayList<>();
        private Map<String, String> characters = TableStyle.DUCKDB.characters();

        private Builder() {
        }

        /**
         * Sets the maximum width for the table output.
         */
        public Builder<T> maxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        /**
         * Sets the table border style.
         */
        public Builder<T> style(TableStyle style) {
            this.characters = style.characters();
            return this;
        }

        /**
         * Sets custom border characters.
         */
        public Builder<T> characters(Map<String, String> characters) {
            this.characters = characters;
            return this;
        }

        /**
         * Adds a column with the given header and accessor function.
         */
        public Builder<T> column(String header, Function<T, Object> accessor) {
            this.headers.add(header);
            this.accessors.add(accessor);
            return this;
        }

        /**
         * Builds an immutable Table instance.
         */
        public Table<T> build() {
            return new Table<>(maxWidth,
                    Collections.unmodifiableList(new ArrayList<>(headers)),
                    Collections.unmodifiableList(new ArrayList<>(accessors)),
                    characters);
        }
    }
}
