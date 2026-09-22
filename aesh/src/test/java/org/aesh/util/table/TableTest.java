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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.Test;

/**
 * Tests for the table display utility.
 */
public class TableTest {

    // Simple data class for testing
    static class Person {
        final String name;
        final String email;
        final int age;
        final double score;

        Person(String name, String email, int age, double score) {
            this.name = name;
            this.email = email;
            this.age = age;
            this.score = score;
        }
    }

    private final List<Person> people = Arrays.asList(
            new Person("Alice", "alice@example.com", 30, 95.5),
            new Person("Bob", "bob@example.com", 25, 87.123));

    @Test
    public void testBasicRendering() {
        List<String> headers = Arrays.asList("Name", "Email");
        List<Function<Person, Object>> accessors = Arrays.<Function<Person, Object>> asList(
                p -> p.name, p -> p.email);

        String output = Table.render(80, people, headers, accessors);
        assertNotNull(output);
        assertTrue(output.contains("Alice"));
        assertTrue(output.contains("Bob"));
        assertTrue(output.contains("alice@example.com"));
        assertTrue(output.contains("bob@example.com"));
        assertTrue(output.contains("Name"));
        assertTrue(output.contains("Email"));
    }

    @Test
    public void testIntegerRightAligned() {
        List<String> headers = Arrays.asList("Name", "Age");
        List<Function<Person, Object>> accessors = Arrays.<Function<Person, Object>> asList(
                p -> p.name, p -> p.age);

        String output = Table.render(80, people, headers, accessors);
        // Integers should be right-aligned: there should be spaces before the number
        // For right-aligned numbers, the number appears at the right side of its column
        String[] lines = output.split(System.lineSeparator());
        boolean foundAlice = false;
        for (String line : lines) {
            if (line.contains("Alice")) {
                foundAlice = true;
                // Age column should have right-aligned integer (spaces before 30)
                assertTrue(line.contains(" 30"));
            }
        }
        assertTrue("Should find Alice row", foundAlice);
    }

    @Test
    public void testFloatFormattedToTwoDecimals() {
        List<String> headers = Arrays.asList("Name", "Score");
        List<Function<Person, Object>> accessors = Arrays.<Function<Person, Object>> asList(
                p -> p.name, p -> p.score);

        String output = Table.render(80, people, headers, accessors);
        // Floats should be formatted to 2 decimal places
        assertTrue("Should contain 95.50", output.contains("95.50"));
        assertTrue("Should contain 87.12", output.contains("87.12"));
        // Should NOT contain the full precision
        assertFalse("Should not contain full 87.123", output.contains("87.123"));
    }

    @Test
    public void testNullValueHandling() {
        List<String[]> items = Arrays.asList(
                new String[] { "key1", "value1" },
                new String[] { "key2", null });
        List<String> headers = Arrays.asList("Key", "Value");
        List<Function<String[], Object>> accessors = Arrays.<Function<String[], Object>> asList(
                a -> a[0], a -> a[1]);

        String output = Table.render(80, items, headers, accessors);
        // Null values should render as empty string, not literal "null"
        assertFalse("Null should not render as literal 'null'", output.contains("null"));
        // The table should still render without errors
        assertTrue("Should still contain key1", output.contains("key1"));
        assertTrue("Should still contain value1", output.contains("value1"));
    }

    @Test
    public void testEmptyDataList() {
        List<Person> empty = Collections.emptyList();
        List<String> headers = Arrays.asList("Name", "Email");
        List<Function<Person, Object>> accessors = Arrays.<Function<Person, Object>> asList(
                p -> p.name, p -> p.email);

        String output = Table.render(80, empty, headers, accessors);
        assertNotNull(output);
        assertTrue("Should still contain headers", output.contains("Name"));
        assertTrue("Should still contain headers", output.contains("Email"));
    }

    @Test
    public void testMultiLineHeaders() {
        List<String> headers = Arrays.asList("First" + System.lineSeparator() + "Name", "Email");
        List<Function<Person, Object>> accessors = Arrays.<Function<Person, Object>> asList(
                p -> p.name, p -> p.email);

        String output = Table.render(80, people, headers, accessors);
        assertTrue("Should contain 'First'", output.contains("First"));
        assertTrue("Should contain 'Name'", output.contains("Name"));
        assertTrue("Should contain 'Email'", output.contains("Email"));
    }

    @Test
    public void testPostgresStyle() {
        List<String> headers = Arrays.asList("Name", "Email");
        List<Function<Person, Object>> accessors = Arrays.<Function<Person, Object>> asList(
                p -> p.name, p -> p.email);
        Map<String, String> chars = TableStyle.POSTGRES.characters();

        // POSTGRES expands shorthands to full names (no outside border),
        // rendering psql-style output directly instead of falling back
        String output = Table.render(80, people, headers, accessors, chars);
        assertNotNull(output);
        assertTrue(output.contains("Alice"));
        // Borderless: column separators but no outside border characters
        assertTrue("POSTGRES should contain | separators", output.contains("|"));
        assertTrue("POSTGRES should contain - separator row", output.contains("-"));
        assertFalse("POSTGRES should not contain + corners", output.contains("+"));
    }

    @Test
    public void testSqliteStyle() {
        List<String> headers = Arrays.asList("Name", "Email");
        List<Function<Person, Object>> accessors = Arrays.<Function<Person, Object>> asList(
                p -> p.name, p -> p.email);

        String output = Table.render(80, people, headers, accessors,
                TableStyle.SQLITE.characters());
        assertNotNull(output);
        // SQLITE has full borders with + corners
        assertTrue("Should contain + border", output.contains("+"));
        assertTrue("Should contain | border", output.contains("|"));
        assertTrue("Should contain - border", output.contains("-"));
    }

    @Test
    public void testDuckdbStyle() {
        List<String> headers = Arrays.asList("Name", "Email");
        List<Function<Person, Object>> accessors = Arrays.<Function<Person, Object>> asList(
                p -> p.name, p -> p.email);

        String output = Table.render(80, people, headers, accessors,
                TableStyle.DUCKDB.characters());
        assertNotNull(output);
        // DUCKDB uses unicode box-drawing characters
        assertTrue("Should contain \u250c (top-left corner)", output.contains("\u250c"));
        assertTrue("Should contain \u2502 (vertical)", output.contains("\u2502"));
        assertTrue("Should contain \u2500 (horizontal)", output.contains("\u2500"));
        assertTrue("Should contain \u2518 (bottom-right corner)", output.contains("\u2518"));
    }

    @Test
    public void testDoubleStyle() {
        List<String> headers = Arrays.asList("Name", "Email");
        List<Function<Person, Object>> accessors = Arrays.<Function<Person, Object>> asList(
                p -> p.name, p -> p.email);

        String output = Table.render(80, people, headers, accessors,
                TableStyle.DOUBLE.characters());
        assertNotNull(output);
        // DOUBLE uses double-line box-drawing characters
        assertTrue("Should contain \u2554 (double top-left)", output.contains("\u2554"));
        assertTrue("Should contain \u2551 (double vertical)", output.contains("\u2551"));
        assertTrue("Should contain \u2550 (double horizontal)", output.contains("\u2550"));
    }

    @Test
    public void testBuilderProducesSameOutputAsStaticMethod() {
        List<String> headers = Arrays.asList("Name", "Email");
        List<Function<Person, Object>> accessors = Arrays.<Function<Person, Object>> asList(
                p -> p.name, p -> p.email);

        String staticOutput = Table.render(80, people, headers, accessors,
                TableStyle.DUCKDB.characters());

        String builderOutput = Table.<Person> builder()
                .maxWidth(80)
                .style(TableStyle.DUCKDB)
                .column("Name", p -> p.name)
                .column("Email", p -> p.email)
                .build()
                .render(people);

        assertEquals("Builder and static method should produce identical output",
                staticOutput, builderOutput);
    }

    @Test
    public void testBuilderWithCustomStyle() {
        String output = Table.<Person> builder()
                .maxWidth(120)
                .style(TableStyle.SQLITE)
                .column("Name", p -> p.name)
                .column("Age", p -> p.age)
                .build()
                .render(people);

        assertNotNull(output);
        assertTrue(output.contains("Alice"));
        assertTrue(output.contains("+"));
    }

    @Test
    public void testLineSplit() {
        List<String> input = Arrays.asList(
                "foo" + System.lineSeparator() + "foo",
                "bar");
        List<List<String>> result = TableCharacters.lineSplit(input);

        assertEquals(2, result.size());
        assertEquals(Arrays.asList("foo", "bar"), result.get(0));
        assertEquals(Arrays.asList("foo", ""), result.get(1));
    }

    @Test
    public void testLineSplitSingleLine() {
        List<String> input = Arrays.asList("foo", "bar");
        List<List<String>> result = TableCharacters.lineSplit(input);

        assertEquals(1, result.size());
        assertEquals(Arrays.asList("foo", "bar"), result.get(0));
    }

    @Test
    public void testTemplateToMapWith6Lines() {
        String template = "\u2554\u2550\u2566\u2550\u2557" + System.lineSeparator() +
                "\u2551h\u2551h\u2551" + System.lineSeparator() +
                "\u2560\u2550\u256c\u2550\u2563" + System.lineSeparator() +
                "\u2551v\u2551v\u2551" + System.lineSeparator() +
                "\u255f\u2500\u256b\u2500\u2563" + System.lineSeparator() +
                "\u255a\u2550\u2569\u2550\u255d";

        Map<String, String> result = TableCharacters.templateToMap(template,
                TableStyle.DUCKDB.characters());

        assertEquals("\u2554", result.get(TableCharacters.HEADER_TOP_LEFT));
        assertEquals("\u2550", result.get(TableCharacters.HEADER_BORDER_HORIZONTAL));
        assertEquals("\u2566", result.get(TableCharacters.HEADER_TOP_INTERSECT));
        assertEquals("\u2557", result.get(TableCharacters.HEADER_TOP_RIGHT));
        assertEquals("\u2551", result.get(TableCharacters.HEADER_BORDER_VERTICAL));
        assertEquals("\u2551", result.get(TableCharacters.HEADER_COLUMN_SEPARATOR));
        assertEquals("\u2560", result.get(TableCharacters.TABLE_TOP_LEFT));
        assertEquals("\u2550", result.get(TableCharacters.TABLE_TOP_HORIZONTAL));
        assertEquals("\u256c", result.get(TableCharacters.TABLE_TOP_INTERSECT));
        assertEquals("\u2563", result.get(TableCharacters.TABLE_TOP_RIGHT));
        assertEquals("\u255f", result.get(TableCharacters.ROW_SEPARATOR_LEFT));
        assertEquals("\u2500", result.get(TableCharacters.ROW_SEPARATOR_HORIZONTAL));
        assertEquals("\u256b", result.get(TableCharacters.ROW_SEPARATOR_INTERSECT));
        assertEquals("\u2563", result.get(TableCharacters.ROW_SEPARATOR_RIGHT));
        assertEquals("\u255a", result.get(TableCharacters.TABLE_BOTTOM_LEFT));
        assertEquals("\u2550", result.get(TableCharacters.TABLE_BORDER_HORIZONTAL));
        assertEquals("\u2569", result.get(TableCharacters.TABLE_BOTTOM_INTERSECT));
        assertEquals("\u255d", result.get(TableCharacters.TABLE_BOTTOM_RIGHT));
    }

    @Test
    public void testTemplateToMapNullReturnsDefault() {
        Map<String, String> defaultMap = TableStyle.DUCKDB.characters();
        Map<String, String> result = TableCharacters.templateToMap(null, defaultMap);
        assertEquals(defaultMap.size(), result.size());
    }

    @Test
    public void testTemplateToMapEmptyReturnsDefault() {
        Map<String, String> defaultMap = TableStyle.DUCKDB.characters();
        Map<String, String> result = TableCharacters.templateToMap("", defaultMap);
        assertEquals(defaultMap.size(), result.size());
    }

    @Test
    public void testHasOutsideBorder() {
        assertTrue("DUCKDB should have outside border",
                TableCharacters.hasOutsideBorder(TableStyle.DUCKDB.characters()));
        assertTrue("SQLITE should have outside border",
                TableCharacters.hasOutsideBorder(TableStyle.SQLITE.characters()));
        assertTrue("DOUBLE should have outside border",
                TableCharacters.hasOutsideBorder(TableStyle.DOUBLE.characters()));

        // POSTGRES has no outside border keys
        assertFalse("POSTGRES should not have outside border",
                TableCharacters.hasOutsideBorder(TableStyle.POSTGRES.characters()));
    }

    @Test
    public void testHasRowSeparator() {
        assertTrue("DOUBLE should have row separator",
                TableCharacters.hasRowSeparator(TableStyle.DOUBLE.characters()));

        assertFalse("DUCKDB should not have row separator",
                TableCharacters.hasRowSeparator(TableStyle.DUCKDB.characters()));
        assertFalse("POSTGRES should not have row separator",
                TableCharacters.hasRowSeparator(TableStyle.POSTGRES.characters()));
    }

    @Test
    public void testIsValid() {
        assertTrue("DUCKDB should be valid",
                TableCharacters.isValid(TableStyle.DUCKDB.characters()));
        assertTrue("SQLITE should be valid",
                TableCharacters.isValid(TableStyle.SQLITE.characters()));
        assertTrue("DOUBLE should be valid",
                TableCharacters.isValid(TableStyle.DOUBLE.characters()));

        // POSTGRES expands shorthands to full names — valid, no outside border
        assertTrue("POSTGRES should be valid",
                TableCharacters.isValid(TableStyle.POSTGRES.characters()));

        // Empty map should not be valid
        assertFalse("Empty map should not be valid",
                TableCharacters.isValid(new HashMap<String, String>()));
    }

    @Test
    public void testConvertToFullNames() {
        Map<String, String> shorthand = new HashMap<>();
        shorthand.put(TableCharacters.VERTICAL, "|");
        shorthand.put(TableCharacters.HORIZONTAL, "-");
        shorthand.put(TableCharacters.INTERSECT, "+");

        Map<String, String> full = TableCharacters.convertToFullNames(shorthand, true);

        // Shorthand keys should be removed
        assertFalse(full.containsKey(TableCharacters.VERTICAL));
        assertFalse(full.containsKey(TableCharacters.HORIZONTAL));
        assertFalse(full.containsKey(TableCharacters.INTERSECT));

        // Full names should be present
        assertEquals("|", full.get(TableCharacters.HEADER_COLUMN_SEPARATOR));
        assertEquals("|", full.get(TableCharacters.TABLE_COLUMN_SEPARATOR));
        assertEquals("|", full.get(TableCharacters.HEADER_BORDER_VERTICAL));
        assertEquals("|", full.get(TableCharacters.TABLE_BORDER_VERTICAL));
        assertEquals("-", full.get(TableCharacters.TABLE_TOP_HORIZONTAL));
        assertEquals("-", full.get(TableCharacters.HEADER_BORDER_HORIZONTAL));
        assertEquals("-", full.get(TableCharacters.TABLE_BORDER_HORIZONTAL));
        assertEquals("+", full.get(TableCharacters.TABLE_TOP_INTERSECT));
        assertEquals("+", full.get(TableCharacters.HEADER_TOP_LEFT));
    }

    @Test
    public void testConvertToFullNamesWithoutBorder() {
        Map<String, String> shorthand = new HashMap<>();
        shorthand.put(TableCharacters.VERTICAL, "|");
        shorthand.put(TableCharacters.HORIZONTAL, "-");
        shorthand.put(TableCharacters.INTERSECT, "+");

        Map<String, String> full = TableCharacters.convertToFullNames(shorthand, false);

        // Without border, only column separators and top horizontal should be set
        assertEquals("|", full.get(TableCharacters.HEADER_COLUMN_SEPARATOR));
        assertEquals("|", full.get(TableCharacters.TABLE_COLUMN_SEPARATOR));
        assertEquals("-", full.get(TableCharacters.TABLE_TOP_HORIZONTAL));
        assertEquals("+", full.get(TableCharacters.TABLE_TOP_INTERSECT));

        // Border-specific entries should NOT be present
        assertFalse(full.containsKey(TableCharacters.HEADER_BORDER_VERTICAL));
        assertFalse(full.containsKey(TableCharacters.TABLE_BORDER_VERTICAL));
        assertFalse(full.containsKey(TableCharacters.HEADER_TOP_LEFT));
    }

    @Test
    public void testPrefixWrapsWithAnsi() {
        Map<String, String> map = new HashMap<>();
        map.put(TableCharacters.VERTICAL, "|");
        map.put(TableCharacters.HORIZONTAL, "-");

        String ansiPrefix = "\u001B[0;31m"; // RED_TEXT
        Map<String, String> prefixed = TableCharacters.prefix(ansiPrefix, map);

        assertEquals(ansiPrefix + "|" + "\u001B[0m",
                prefixed.get(TableCharacters.VERTICAL));
        assertEquals(ansiPrefix + "-" + "\u001B[0m",
                prefixed.get(TableCharacters.HORIZONTAL));
    }

    @Test
    public void testThreeColumnTable() {
        List<String> headers = Arrays.asList("Name", "Age", "Score");
        List<Function<Person, Object>> accessors = Arrays.<Function<Person, Object>> asList(
                p -> p.name, p -> p.age, p -> p.score);

        String output = Table.render(120, people, headers, accessors,
                TableStyle.DUCKDB.characters());
        assertNotNull(output);
        assertTrue(output.contains("Alice"));
        assertTrue(output.contains("Bob"));
        // Check that all three columns are present
        String[] lines = output.split(System.lineSeparator());
        // Data rows should have 3 separators (or borders)
        boolean foundDataRow = false;
        for (String line : lines) {
            if (line.contains("Alice")) {
                foundDataRow = true;
                assertTrue("Data row should contain score", line.contains("95.50"));
            }
        }
        assertTrue("Should find a data row", foundDataRow);
    }

    @Test
    public void testDoubleStyleWithRowSeparators() {
        List<String> headers = Arrays.asList("Name", "Email");
        List<Function<Person, Object>> accessors = Arrays.<Function<Person, Object>> asList(
                p -> p.name, p -> p.email);

        String output = Table.render(80, people, headers, accessors,
                TableStyle.DOUBLE.characters());

        // DOUBLE style has row separators between data rows
        // With 2 data rows, there should be 1 row separator line
        assertTrue("Should contain row separator character \u255f",
                output.contains("\u255f"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMismatchedHeaderAccessorCountThrows() {
        List<String> headers = Arrays.asList("Name", "Email", "Age");
        List<Function<Person, Object>> accessors = Arrays.<Function<Person, Object>> asList(
                p -> p.name, p -> p.email);

        // 3 headers but only 2 accessors should throw
        Table.render(80, people, headers, accessors);
    }

    @Test
    public void testNullAccessorReturnRendersAsEmpty() {
        List<String[]> items = Arrays.asList(
                new String[] { "key1", "value1" },
                new String[] { "key2", null });
        List<String> headers = Arrays.asList("Key", "Value");
        List<Function<String[], Object>> accessors = Arrays.<Function<String[], Object>> asList(
                a -> a[0], a -> a[1]);

        String output = Table.render(80, items, headers, accessors);
        // Null values should render as empty string, not the literal "null"
        assertFalse("Null should not render as literal 'null'", output.contains("null"));
    }

    @Test
    public void testMaxWidthTruncatesColumns() {
        List<Person> items = Arrays.asList(
                new Person("A very long name that exceeds width", "some-long-email@example.com", 30, 95.5));
        List<String> headers = Arrays.asList("Name", "Email");
        List<Function<Person, Object>> accessors = Arrays.<Function<Person, Object>> asList(
                p -> p.name, p -> p.email);

        String output = Table.render(30, items, headers, accessors,
                TableStyle.SQLITE.characters());
        // With maxWidth=30, content should be truncated with ellipsis
        String[] lines = output.split(System.lineSeparator());
        for (String line : lines) {
            assertTrue("No line should exceed maxWidth (30), got length " + line.length(),
                    line.length() <= 30);
        }
    }

    @Test
    public void testInvalidCharactersFallbackToSqlite() {
        Map<String, String> invalid = new HashMap<>();
        // No valid entries at all

        List<String> headers = Arrays.asList("Name");
        List<Function<Person, Object>> accessors = Arrays.<Function<Person, Object>> asList(
                p -> p.name);

        String output = Table.render(80, people, headers, accessors, invalid);
        assertNotNull(output);
        // Should fallback to SQLITE style which uses |, -, +
        assertTrue("Fallback should use SQLITE borders", output.contains("|"));
    }

    @Test
    public void testPlainStyleNoBorders() {
        List<Person> people = Arrays.asList(
                new Person("Alice", "alice@example.com", 30, 95.5),
                new Person("Bob", "bob@example.com", 25, 87.0));

        List<String> headers = Arrays.asList("Name", "Email", "Age");
        List<Function<Person, Object>> accessors = Arrays.<Function<Person, Object>> asList(
                p -> p.name, p -> p.email, p -> p.age);

        String output = Table.render(80, people, headers, accessors,
                TableStyle.PLAIN.characters());
        assertNotNull(output);

        // PLAIN style should have no visible border characters
        assertFalse("PLAIN should not contain pipe characters", output.contains("|"));
        assertFalse("PLAIN should not contain + characters", output.contains("+"));
        assertFalse("PLAIN should not contain box-drawing characters", output.contains("\u2502"));

        // Content should still be present and aligned
        assertTrue("Should contain Alice", output.contains("Alice"));
        assertTrue("Should contain bob@example.com", output.contains("bob@example.com"));
        assertTrue("Should contain header Name", output.contains("Name"));
    }

    @Test
    public void testPlainStyleViaBuilder() {
        List<Person> people = Arrays.asList(
                new Person("Alice", "alice@example.com", 30, 95.5));

        String output = Table.<Person> builder()
                .maxWidth(80)
                .style(TableStyle.PLAIN)
                .column("Name", p -> p.name)
                .column("Email", p -> p.email)
                .build()
                .render(people);

        assertNotNull(output);
        assertFalse("PLAIN via builder should not contain borders", output.contains("|"));
        assertTrue("Should contain Alice", output.contains("Alice"));
    }

    // --- Test: PLAIN must not emit a null row after the header (#633) ---

    @Test
    public void testPlainStyleHasNoNullRow() {
        String output = Table.<Person> builder()
                .maxWidth(80)
                .style(TableStyle.PLAIN)
                .column("ID", p -> p.name)
                .column("Type", p -> p.email)
                .column("Host", p -> String.valueOf(p.age))
                .build()
                .render(Collections.singletonList(
                        new Person("test-01", "TYPE_A", 2, 0.0)));

        assertNotNull(output);
        String[] lines = output.split(System.lineSeparator());
        // Header immediately followed by the single data row: no separator,
        // no blank line, and no null row in between.
        assertEquals("PLAIN output should be exactly header + data lines", 2, lines.length);
        assertTrue("First line should be the header",
                lines[0].contains("ID") && lines[0].contains("Type") && lines[0].contains("Host"));
        assertTrue("Second line should be the data row",
                lines[1].contains("test-01") && lines[1].contains("TYPE_A"));
        for (String line : lines) {
            assertFalse("No line should contain null text: " + line, line.contains("null"));
            assertFalse("No line should be blank", line.trim().isEmpty());
        }
    }

    @Test
    public void testBorderlessCustomMapSeparatorHasNoNull() {
        // A valid borderless map with visible separators (what POSTGRES
        // intends): the header-body separator must render dashes, not nulls.
        Map<String, String> borderless = new HashMap<>();
        borderless.put(TableCharacters.HEADER_COLUMN_SEPARATOR, "|");
        borderless.put(TableCharacters.TABLE_COLUMN_SEPARATOR, "|");
        borderless.put(TableCharacters.TABLE_TOP_HORIZONTAL, "-");
        borderless.put(TableCharacters.TABLE_TOP_INTERSECT, "+");

        String output = Table.<Person> builder()
                .maxWidth(80)
                .characters(borderless)
                .column("Name", p -> p.name)
                .column("Email", p -> p.email)
                .build()
                .render(Collections.singletonList(
                        new Person("Alice", "alice@example.com", 30, 95.5)));

        assertNotNull(output);
        String[] lines = output.split(System.lineSeparator());
        assertEquals("Should be header + separator + data lines", 3, lines.length);
        assertTrue("Separator should render dashes: " + lines[1], lines[1].contains("---"));
        assertTrue("Separator should render column separators: " + lines[1], lines[1].contains("|"));
        for (String line : lines) {
            assertFalse("No line should contain null text: " + line, line.contains("null"));
        }
        assertTrue("Should contain Alice", output.contains("Alice"));
    }
}
