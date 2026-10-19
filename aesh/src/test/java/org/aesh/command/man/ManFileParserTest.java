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
package org.aesh.command.man;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.aesh.command.man.parser.ManFileParser;
import org.junit.Test;

/**
 * Comprehensive test suite for ManFileParser to verify compliance with
 * asciidoctor manpage specification.
 *
 * @author Aesh team
 */
public class ManFileParserTest {

    /**
     * Test parsing a file using the standard asciidoctor == section syntax.
     * According to the asciidoctor spec, sections should use "== Section Title" format.
     */
    @Test
    public void testParseWithEqualsSignSectionSyntax() throws IOException {
        ManFileParser parser = new ManFileParser();
        parser.setInput(new FileInputStream("src/test/resources/asciitest3_with_equals.txt"));
        parser.loadPage(80);

        assertNotNull("Sections should not be null", parser.getSections());
        assertTrue("Should have at least one section", parser.getSections().size() > 0);
        assertEquals("First section should be NAME", "NAME", parser.getSections().get(0).getName());
        assertEquals("testcommand(1)", parser.getName());
    }

    /**
     * Test parsing the traditional underline section syntax.
     * This format uses section name followed by dashes (NAME\n----).
     */
    @Test
    public void testParseWithUnderlineSectionSyntax() throws IOException {
        ManFileParser parser = new ManFileParser();
        parser.setInput(new FileInputStream("src/test/resources/asciitest1.txt"));
        parser.loadPage(80);

        assertEquals("NAME", parser.getSections().get(0).getName());
        assertEquals("SYNOPSIS", parser.getSections().get(1).getName());
        assertEquals("DESCRIPTION", parser.getSections().get(2).getName());
        assertEquals("OPTIONS", parser.getSections().get(3).getName());
        assertEquals("ASCIIDOC(1)", parser.getName());
    }

    /**
     * Test parsing a file that mixes == syntax and underline syntax.
     * Asciidoctor supports both formats in the same document.
     */
    @Test
    public void testParseMixedSectionFormats() throws IOException {
        ManFileParser parser = new ManFileParser();
        parser.setInput(new FileInputStream("src/test/resources/asciitest5_mixed_format.txt"));
        parser.loadPage(80);

        assertNotNull("Sections should not be null", parser.getSections());
        assertTrue("Should parse sections from mixed format file", parser.getSections().size() > 0);
    }

    /**
     * Test minimal valid manpage header.
     * Header should contain: title, :doctype: manpage
     */
    @Test
    public void testMinimalValidHeader() throws IOException {
        ManFileParser parser = new ManFileParser();
        parser.setInput(new FileInputStream("src/test/resources/asciitest4_minimal.txt"));
        parser.loadPage(80);

        assertEquals("minimal(1)", parser.getName());
        assertTrue("Should have NAME section", parser.getSections().size() > 0);
    }

    /**
     * Test header validation - missing doctype should fail.
     */
    @Test
    public void testHeaderMissingDoctype() {
        String content = "= testcmd(1)\n\n\n\n== NAME\ntestcmd - test";
        ManFileParser parser = new ManFileParser();
        try {
            parser.setInput(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
            parser.loadPage(80);
            fail("Should throw IOException for missing doctype");
        } catch (IOException e) {
            assertTrue("Error message should mention doctype",
                    e.getMessage().contains("doctype"));
        }
    }

    /**
     * Test header validation - incorrect doctype should fail.
     */
    @Test
    public void testHeaderWrongDoctype() {
        String content = "= testcmd(1)\n===========\n:doctype: article\n\n\n== NAME\ntestcmd - test";
        ManFileParser parser = new ManFileParser();
        try {
            parser.setInput(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
            parser.loadPage(80);
            fail("Should throw IOException for wrong doctype");
        } catch (IOException e) {
            assertTrue("Error message should mention manpage doctype",
                    e.getMessage().contains("manpage"));
        }
    }

    /**
     * Test that title is properly extracted from header.
     * Title format: = progname(volume)
     */
    @Test
    public void testTitleExtraction() throws IOException {
        String content = "= mycommand(1)\n:doctype: manpage\n\n\n== NAME\nmycommand - does something";
        ManFileParser parser = new ManFileParser();
        parser.setInput(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        parser.loadPage(80);

        assertEquals("mycommand(1)", parser.getName());
    }

    /**
     * Test parsing NAME section with proper format.
     * According to spec: "comma-separated command name(s) separated from
     * the command's purpose by a dash character"
     */
    @Test
    public void testNameSectionFormat() throws IOException {
        String content = "= cmd(1)\n:doctype: manpage\n\n\n== NAME\ncmd - command description";
        ManFileParser parser = new ManFileParser();
        parser.setInput(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        parser.loadPage(80);

        assertNotNull(parser.getSections());
        assertTrue("Should have NAME section", parser.getSections().size() > 0);
        assertEquals("NAME", parser.getSections().get(0).getName());
    }

    /**
     * Test NAME section with multiple command names (aliases).
     * Format: cmd1, cmd2, cmd3 - description
     */
    @Test
    public void testNameSectionWithAliases() throws IOException {
        String content = "= cmd1(1)\n:doctype: manpage\n\n\n" +
                "== NAME\ncmd1, cmd2, cmd3 - multiple command aliases";
        ManFileParser parser = new ManFileParser();
        parser.setInput(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        parser.loadPage(80);

        assertNotNull(parser.getSections());
        assertTrue("Should parse NAME section with aliases", parser.getSections().size() > 0);
    }

    /**
     * Test OPTIONS section with proper parameter definitions.
     * Format: *-flag, --long-option*=_ARGUMENT_::
     */
    @Test
    public void testOptionsSectionWithParameters() throws IOException {
        ManFileParser parser = new ManFileParser();
        parser.setInput(new FileInputStream("src/test/resources/asciitest1.txt"));
        parser.loadPage(80);

        // OPTIONS is the 4th section (index 3)
        assertEquals("OPTIONS", parser.getSections().get(3).getName());
        assertEquals("Should have 2 options", 2, parser.getSections().get(3).getParameters().size());
    }

    /**
     * Test that section titles are properly extracted regardless of format.
     */
    @Test
    public void testSectionTitleExtraction() throws IOException {
        String content = "= test(1)\n:doctype: manpage\n\n\n" +
                "== DESCRIPTION\nSome description.\n\n\n" +
                "== SEE ALSO\nother(1)";
        ManFileParser parser = new ManFileParser();
        parser.setInput(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        parser.loadPage(80);

        assertTrue("Should have multiple sections", parser.getSections().size() >= 2);
    }

    /**
     * Test empty or whitespace-only sections.
     */
    @Test
    public void testEmptySections() throws IOException {
        String content = "= test(1)\n:doctype: manpage\n\n\n== NAME\ntest - test command\n\n\n== DESCRIPTION\n\n";
        ManFileParser parser = new ManFileParser();
        parser.setInput(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        parser.loadPage(80);

        assertNotNull(parser.getSections());
    }

    /**
     * Test that parser handles anchor links [[anchor]].
     * Asciidoctor allows anchors before section headings.
     */
    @Test
    public void testSectionWithAnchor() throws IOException {
        String content = "= test(1)\n:doctype: manpage\n\n\n" +
                "[[X1]]\n== SPECIAL SECTION\nContent here.";
        ManFileParser parser = new ManFileParser();
        parser.setInput(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        parser.loadPage(80);

        assertNotNull(parser.getSections());
    }

    /**
     * Test complex document with all common sections.
     */
    @Test
    public void testCompleteManpage() throws IOException {
        ManFileParser parser = new ManFileParser();
        parser.setInput(new FileInputStream("src/test/resources/asciitest2.txt"));
        parser.loadPage(80);

        assertEquals("Should have 10 sections", 10, parser.getSections().size());
        assertEquals("NAME", parser.getSections().get(0).getName());
    }

    /**
     * Test that getAsList() produces non-null output.
     */
    @Test
    public void testGetAsList() throws IOException {
        ManFileParser parser = new ManFileParser();
        parser.setInput(new FileInputStream("src/test/resources/asciitest1.txt"));
        parser.loadPage(80);

        assertNotNull("getAsList should not return null", parser.getAsList());
        assertTrue("getAsList should have content", parser.getAsList().size() > 0);
    }

    /**
     * Test that print() produces non-null output.
     */
    @Test
    public void testPrint() throws IOException {
        ManFileParser parser = new ManFileParser();
        parser.setInput(new FileInputStream("src/test/resources/asciitest1.txt"));
        parser.loadPage(80);

        String output = parser.print();
        assertNotNull("print should not return null", output);
        assertTrue("print should have content", output.length() > 0);
    }

    /**
     * Test that parser can be reused with setInput.
     */
    @Test
    public void testParserReuse() throws IOException {
        ManFileParser parser = new ManFileParser();

        // Parse first file
        parser.setInput(new FileInputStream("src/test/resources/asciitest1.txt"));
        parser.loadPage(80);
        String firstName = parser.getName();

        // Parse second file
        parser.setInput(new FileInputStream("src/test/resources/asciitest2.txt"));
        parser.loadPage(80);
        String secondName = parser.getName();

        assertEquals("First file name", "ASCIIDOC(1)", firstName);
        assertEquals("Second file name", "ASCIIDOC(1)", secondName);
    }

    /**
     * Test that loadPage without setInput throws IOException.
     */
    @Test
    public void testLoadPageWithoutInput() {
        ManFileParser parser = new ManFileParser();
        try {
            parser.loadPage(80);
            fail("Should throw IOException when reader is null");
        } catch (IOException e) {
            assertTrue("Error message should mention null reader",
                    e.getMessage().contains("null") || e.getMessage().contains("InputStreamReader"));
        }
    }

    /**
     * Test various column widths for loadPage.
     */
    @Test
    public void testDifferentColumnWidths() throws IOException {
        int[] widths = { 40, 80, 120, 200 };

        for (int width : widths) {
            ManFileParser parser = new ManFileParser();
            parser.setInput(new FileInputStream("src/test/resources/asciitest1.txt"));
            parser.loadPage(width);

            assertNotNull("Should parse with width " + width, parser.getSections());
            assertTrue("Should have sections with width " + width, parser.getSections().size() > 0);
        }
    }
}
