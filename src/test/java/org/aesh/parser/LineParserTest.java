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

package org.aesh.parser;

import org.aesh.command.operator.OperatorType;
import org.junit.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class LineParserTest {

    @Test
    public void testfindCurrentWordFromCursor() {
        LineParser lineParser = new LineParser();
        assertEquals("", lineParser.parseLine(" ", 1).selectedWord().word());
        assertEquals("foo", lineParser.parseLine("foo bar", 3).selectedWord().word());
        assertEquals("bar", lineParser.parseLine("foo bar", 6).selectedWord().word());
        assertEquals("foobar", lineParser.parseLine("foobar", 6).selectedWord().word());
        assertEquals("fo", lineParser.parseLine("foobar", 2).selectedWordToCursor().word());
        assertEquals("", lineParser.parseLine("ls  ", 3).selectedWord().word());
        assertEquals("foo", lineParser.parseLine("ls  foo", 6).selectedWord().word());
        assertEquals("foo", lineParser.parseLine("ls  foo bar", 6).selectedWord().word());
        assertEquals("bar", lineParser.parseLine("ls  foo bar", 11).selectedWordToCursor().word());
        assertEquals("ba", lineParser.parseLine("ls  foo bar", 10).selectedWordToCursor().word());
        assertEquals("b", lineParser.parseLine("ls  foo bar", 9).selectedWordToCursor().word());
        assertEquals("foo", lineParser.parseLine("ls foo ", 6).selectedWordToCursor().word());
        assertEquals("o", lineParser.parseLine("ls o org/jboss/aeshell/Shell.class", 4).selectedWord().word());
        assertEquals("", lineParser.parseLine("ls  org/jboss/aeshell/Shell.class", 3).selectedWord().word());
    }

    @Test
    public void testFindCurrentWordWithEscapedSpaceToCursor() {
        LineParser lineParser = new LineParser();
        assertEquals("foo bar", lineParser.parseLine("foo\\ bar", 8).selectedWordToCursor().word());
        assertEquals("foo ba", lineParser.parseLine("foo\\ bar", 7).selectedWordToCursor().word());
        assertEquals("foo bar", lineParser.parseLine("ls  foo\\ bar", 12).selectedWordToCursor().word());
    }

    @Test
    public void testFindClosestWholeWordToCursor() {
        LineParser lineParser = new LineParser();
        assertEquals("foo", lineParser.parseLine("ls  foo bar", 6).selectedWord().word());

        assertEquals("", lineParser.parseLine(" ", 1).selectedWord().word());
        assertEquals("foo", lineParser.parseLine("foo bar", 1).selectedWord().word());
        assertEquals("foo", lineParser.parseLine("foo bar", 3).selectedWord().word());
        assertEquals("foobar", lineParser.parseLine("foobar", 6).selectedWord().word());
        assertEquals("foobar", lineParser.parseLine("foobar", 2).selectedWord().word());
        assertEquals("", lineParser.parseLine("ls  ", 3).selectedWord().word());

        assertEquals("o", lineParser.parseLine("ls o org/jboss/aeshell/Shell.class", 4).selectedWord().word());
        assertEquals("", lineParser.parseLine("ls  org/jboss/aeshell/Shell.class", 3).selectedWord().word());

        assertEquals("foo", lineParser.parseLine("foo bar foo", 3).selectedWord().word());
    }

    @Test
    public void testFindClosestWholeWordToCursorEscapedSpace() {
        LineParser lineParser = new LineParser();
        assertEquals("foo bar", lineParser.parseLine("foo\\ bar", 7).selectedWord().word());
        assertEquals("foo bar", lineParser.parseLine("ls  foo\\ bar", 11).selectedWord().word());
    }

       @Test
    public void testOriginalInput() {
           LineParser lineParser = new LineParser();
        String input = "echo foo -i bar";
        ParsedLine line = lineParser.parseLine(input);
        assertEquals(input, line.line());
    }

    @Test
    public void testFindAllWords() {
        LineParser lineParser = new LineParser();
        ParsedLine line = lineParser.parseLine("", 0);
        assertEquals(-1, line.wordCursor());
        assertEquals(0, line.cursor());
        assertEquals("", line.selectedWord().word());

        line = lineParser.parseLine("   foo bar\\ baz 12345 ", 5);
        assertEquals("foo", line.words().get(0).word());
        assertEquals("bar baz", line.words().get(1).word());
        assertEquals("12345", line.words().get(2).word());
        assertEquals("foo", line.selectedWord().word());
        assertEquals(2, line.wordCursor());
        assertTrue(line.spaceAtEnd());

        line = lineParser.parseLine("man < foo\\ bar ", 14);
        assertEquals("man", line.words().get(0).word());
        assertEquals("<", line.words().get(1).word());
        assertEquals("foo bar", line.words().get(2).word());
        assertEquals("foo bar", line.selectedWord().word());
        assertEquals(7, line.wordCursor());

        line = lineParser.parseLine("cd A\\ Directory\\ With\\ Spaces", 2);
        assertEquals("cd", line.words().get(0).word());
        assertEquals("A Directory With Spaces", line.words().get(1).word());
        assertEquals("cd", line.selectedWord().word());
        assertEquals(2, line.wordCursor());

        line = lineParser.parseLine("cd A\\ ",5);
        assertEquals("cd", line.words().get(0).word());
        assertEquals("A ", line.words().get(1).word());
        assertEquals("A ", line.selectedWord().word());
        assertEquals(1, line.wordCursor());

        line = lineParser.parseLine("cd A\\", 4);
        assertEquals("cd", line.words().get(0).word());
        assertEquals("A\\", line.words().get(1).word());
        assertEquals("A\\", line.selectedWord().word());
        assertEquals(1, line.wordCursor());

        line = lineParser.parseLine("ls --files /tmp/A\\ ");
        assertEquals("ls", line.words().get(0).word());
        assertEquals("--files", line.words().get(1).word());
        assertEquals("/tmp/A ", line.words().get(2).word());

        line = lineParser.parseLine("..\\..\\..\\..\\..\\..\\..\\temp\\foo.txt");
        assertEquals("..\\..\\..\\..\\..\\..\\..\\temp\\foo.txt", line.words().get(0).word());
    }

    @Test
    public void testFindAllQuotedWords() {
        LineParser lineParser = new LineParser();
        ParsedLine line = lineParser.parseLine("foo bar \"baz 12345\" ", 19);
        assertEquals("foo", line.words().get(0).word());
        assertEquals(0, line.words().get(0).lineIndex());
        assertEquals("bar", line.words().get(1).word());
        assertEquals(4, line.words().get(1).lineIndex());
        assertEquals("baz 12345", line.words().get(2).word());
        assertEquals(9, line.words().get(2).lineIndex());
        assertEquals("", line.selectedWord().word());
        assertEquals(0, line.wordCursor());
        assertFalse(line.cursorAtEnd());

        line = lineParser.parseLine("java -cp \"foo/bar\" \"Example\"", 32);
        assertEquals("foo/bar", line.words().get(2).word());
        assertEquals("Example", line.words().get(3).word());
        assertTrue(line.cursorAtEnd());

        line = lineParser.parseLine("'foo/bar/' Example\\ 1");
        assertEquals("foo/bar/", line.words().get(0).word());
        assertEquals("Example 1", line.words().get(1).word());

        line = lineParser.parseLine("man -f='foo bar/' Example\\ 1 foo");
        assertEquals("man", line.words().get(0).word());
        assertEquals("-f=foo bar/", line.words().get(1).word());
        assertEquals("Example 1", line.words().get(2).word());
        assertEquals("foo", line.words().get(3).word());

        line = lineParser.parseLine("man -f='foo/bar/ Example\\ 1");
        assertEquals(ParserStatus.UNCLOSED_QUOTE, line.status());

        line = lineParser.parseLine("man -f='foo/bar/' Example\\ 1\"");
        assertEquals(ParserStatus.UNCLOSED_QUOTE, line.status());

        line = lineParser.parseLine("-s \'redirectUris=[\"http://localhost:8080/blah/*\"]\'");
        assertEquals("-s", line.words().get(0).word());
        assertEquals("redirectUris=[\"http://localhost:8080/blah/*\"]", line.words().get(1).word());
    }

    @Test
    public void testFindAllTernaryQuotedWords() {
        LineParser lineParser = new LineParser();
        ParsedLine line = lineParser.parseLine("\"\"  \"\"");
        assertEquals("  ", line.words().get(0).word());
        line = lineParser.parseLine("\"\"  foo bar \"\"");
        assertEquals("  foo bar ", line.words().get(0).word());

        line = lineParser.parseLine("\"\"  \"foo bar\" \"\"");
        assertEquals("  \"foo bar\" ", line.words().get(0).word());

        line = lineParser.parseLine("gah bah-bah  \"\"  \"foo bar\" \"\" boo");
        assertEquals("gah", line.words().get(0).word());
        assertEquals("bah-bah", line.words().get(1).word());
        assertEquals("  \"foo bar\" ", line.words().get(2).word());
        assertEquals("boo", line.words().get(3).word());

        line = lineParser.parseLine(" \"\"/s-ramp/wsdl/Operation[xp2:matches(@name, 'submit.*')]\"\"");
        assertEquals("/s-ramp/wsdl/Operation[xp2:matches(@name, 'submit.*')]", line.words().get(0).word());

        line = lineParser.parseLine(" \"\"/s-ramp/ext/${type} \\ \"\"");
        assertEquals("/s-ramp/ext/${type} \\ ", line.words().get(0).word());
    }

    @Test
    public void testParsedLineIterator() {
        LineParser lineParser = new LineParser();
        ParsedLine line = lineParser.parseLine("foo bar");
        ParsedLineIterator iterator = line.iterator();
        int counter = 0;
        while(iterator.hasNextWord()) {
            if(counter == 0)
                assertEquals("foo", iterator.pollWord());
            else if(counter == 1)
                assertEquals("bar", iterator.pollWord());

            counter++;
        }

        line = lineParser.parseLine("");
        iterator = line.iterator();
        assertFalse(iterator.hasNextWord());
        assertNull(iterator.pollWord());

        line = lineParser.parseLine("\\ foo ba bar");
        iterator = line.iterator();

        assertEquals(" foo", iterator.peekWord());
        assertEquals(" foo", iterator.pollWord());
        assertFalse(iterator.finished());
        assertEquals('b', iterator.pollChar());
        assertEquals('a', iterator.pollChar());
        assertEquals(' ', iterator.pollChar());
        assertFalse(iterator.finished());
        assertEquals("bar", iterator.peekWord());
        assertEquals("bar", iterator.pollWord());
        assertTrue(iterator.finished());

        line = lineParser.parseLine("\\ foo ba bar");
        iterator = line.iterator();
        assertEquals('\\', iterator.pollChar());
        assertEquals(' ', iterator.pollChar());
        assertEquals('f', iterator.pollChar());
        assertEquals(" foo", iterator.pollWord());
        assertEquals("ba", iterator.pollWord());
        assertEquals('b', iterator.pollChar());
        assertEquals('a', iterator.pollChar());
        assertEquals('r', iterator.pollChar());
        assertTrue(iterator.finished());

        line = lineParser.parseLine("\\ foo ba bar");
        iterator = line.iterator();
        assertEquals(" foo", iterator.pollWord());

    }

    @Test
    public void testParsedLineIterator2() {
        LineParser lineParser = new LineParser();
        ParsedLine line = lineParser.parseLine("foo bar");
        ParsedLineIterator iterator = line.iterator();

        assertEquals("foo bar", iterator.stringFromCurrentPosition());
        iterator.updateIteratorPosition(3);
        assertEquals(' ', iterator.pollChar());
        assertEquals("bar", iterator.stringFromCurrentPosition());
        assertEquals("bar", iterator.pollWord());

        line = lineParser.parseLine("command --opt1={ myProp1=99, myProp2=100} --opt2");
        iterator = line.iterator();
        assertEquals("command", iterator.pollWord());
        assertEquals('-', iterator.peekChar());
        assertEquals("--opt1={", iterator.peekWord());
        iterator.updateIteratorPosition(33);
        assertEquals("--opt2", iterator.peekWord());
        assertEquals(' ', iterator.peekChar());

        line = lineParser.parseLine("--headers={t=x; t=y}");
        iterator = line.iterator();
        assertEquals("--headers={t=x;", iterator.pollWord());
        assertEquals('t', iterator.peekChar());
        iterator.updateIteratorPosition(3);
        assertEquals('}', iterator.peekChar());
        assertEquals("t=y}", iterator.pollWord());
        assertFalse(iterator.hasNextWord());
        assertNull("", iterator.pollWord());

        line = lineParser.parseLine("--headers={t=x; t=y}");
        iterator = line.iterator();
        iterator.pollParsedWord();
        iterator.updateIteratorPosition(4);
        assertFalse(iterator.hasNextChar());
        assertEquals('\u0000', iterator.pollChar());
        assertNull("", iterator.pollWord());

        line = lineParser.parseLine("--headers={t=x; t=y}");
        iterator = line.iterator();
        iterator.pollParsedWord();
        iterator.updateIteratorPosition(40);
        assertFalse(iterator.hasNextChar());
        assertEquals('\u0000', iterator.pollChar());
        assertNull("", iterator.pollWord());

        line = lineParser.parseLine("--headers={t=x; t=y}");
        iterator = line.iterator();
        iterator.updateIteratorPosition(20);
        assertNull(iterator.peekWord());
     }

     @Test
     public void testCurlyBrackets() {
         LineParser lineParser = new LineParser();
         ParsedLine line = lineParser.parseLine("foo bar {baz 12345} ", 19, true);
         assertEquals("foo", line.words().get(0).word());
         assertEquals(0, line.words().get(0).lineIndex());
         assertEquals("bar", line.words().get(1).word());
         assertEquals(4, line.words().get(1).lineIndex());
         assertEquals("{baz 12345}", line.words().get(2).word());
         assertEquals(8, line.words().get(2).lineIndex());
         assertEquals("{baz 12345}", line.selectedWord().word());
         assertEquals(11, line.wordCursor());

         line = lineParser.parseLine("cmd1 --option1=bar{x1; x2}", 19, true);
         assertEquals("cmd1", line.words().get(0).word());
         assertEquals("--option1=bar{x1; x2}", line.words().get(1).word());
         line = lineParser.parseLine("cmd1 --option1=bar{x1; x2}", 19, false);
         assertEquals("--option1=bar{x1;", line.words().get(1).word());
     }

     @Test
    public void testOperatorParsing() {
         Set<OperatorType> operators = EnumSet.allOf(OperatorType.class);
         LineParser lineParser = new LineParser();
         List<ParsedLine> lines = lineParser.parseLine("foo | bar", 19, true, operators);

         assertEquals("foo", lines.get(0).words().get(0).word());
         assertEquals("foo ", lines.get(0).line());
         assertEquals(OperatorType.PIPE, lines.get(0).operator());
         assertEquals("bar", lines.get(1).words().get(0).word());
         assertEquals(OperatorType.NONE, lines.get(1).operator());

         lines = lineParser.parseLine("foo | bar |car", 19, true, operators);
         assertEquals("foo", lines.get(0).words().get(0).word());
         assertEquals("foo ", lines.get(0).line());
         assertEquals(OperatorType.PIPE, lines.get(0).operator());
         assertEquals("bar", lines.get(1).words().get(0).word());
         assertEquals(OperatorType.PIPE, lines.get(1).operator());
         assertEquals("car", lines.get(2).words().get(0).word());
         assertEquals(OperatorType.NONE, lines.get(2).operator());

         lines = lineParser.parseLine("foo --option1 value1 > bar -o \"ba ra\";car! ", 19, true, operators);
         assertEquals("foo", lines.get(0).words().get(0).word());
         assertEquals("value1", lines.get(0).words().get(2).word());
         assertEquals("foo --option1 value1 ", lines.get(0).line());
         assertEquals(OperatorType.REDIRECT_OUT, lines.get(0).operator());
         assertEquals("bar", lines.get(1).words().get(0).word());
         assertEquals("ba ra", lines.get(1).words().get(2).word());
         assertEquals(OperatorType.END, lines.get(1).operator());
         assertEquals("car!", lines.get(2).words().get(0).word());
         assertEquals(OperatorType.NONE, lines.get(2).operator());

         lines = lineParser.parseLine("foo --option1 value1 >> bar -o \"ba ra\"&&car! ", 27, true, operators);
         assertEquals("foo", lines.get(0).words().get(0).word());
         assertEquals("value1", lines.get(0).words().get(2).word());
         assertEquals("foo --option1 value1 ", lines.get(0).line());
         assertEquals(OperatorType.APPEND_OUT, lines.get(0).operator());
         assertEquals("bar", lines.get(1).words().get(0).word());
         assertEquals("bar", lines.get(1).selectedWordToCursor().word());
         assertEquals("ba ra", lines.get(1).words().get(2).word());
         assertEquals(OperatorType.AND, lines.get(1).operator());
         assertEquals("car!", lines.get(2).words().get(0).word());
         assertEquals(OperatorType.NONE, lines.get(2).operator());

         lines = lineParser.parseLine("foo get >> test1.txt ; foo get >> test1.txt", 19, true, operators);
         assertEquals("foo", lines.get(0).words().get(0).word());
         assertEquals("test1.txt", lines.get(1).words().get(0).word());
         assertEquals("foo", lines.get(2).words().get(0).word());

         lines = lineParser.parseLine("foo get >> test1.txt; foo get >> test1.txt", 19, true, operators);
         assertEquals("foo", lines.get(0).words().get(0).word());
         assertEquals("test1.txt", lines.get(1).words().get(0).word());
         assertEquals("foo", lines.get(2).words().get(0).word());
       }
}
