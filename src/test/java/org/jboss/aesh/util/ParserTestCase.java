/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.util;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParserTestCase extends TestCase {

    public ParserTestCase(String name) {
        super(name);
    }

    public void testFindStartsWith() {
        List<String> completionList = new ArrayList<String>(3);
        completionList.add("foobar");
        completionList.add("foobaz");
        completionList.add("foobor");
        completionList.add("foob");

        assertEquals("foob", Parser.findStartsWith(completionList));

        completionList.clear();
        completionList.add("foo");
        completionList.add("bar");
        assertEquals("", Parser.findStartsWith(completionList));
    }

    public void testFindClosestWordToCursor() {
        assertEquals("", Parser.findWordClosestToCursor(" ", 1));
        assertEquals("foo", Parser.findWordClosestToCursor("foo bar", 3));
        assertEquals("bar", Parser.findWordClosestToCursor("foo bar", 6));
        assertEquals("foobar", Parser.findWordClosestToCursor("foobar", 6));
        assertEquals("foo", Parser.findWordClosestToCursor("foobar", 2));
        assertEquals("", Parser.findWordClosestToCursor("ls  ", 3));
        assertEquals("foo", Parser.findWordClosestToCursor("ls  foo", 6));
        assertEquals("foo", Parser.findWordClosestToCursor("ls  foo bar", 6) );
        assertEquals("bar", Parser.findWordClosestToCursor("ls  foo bar", 10) );
        assertEquals("ba", Parser.findWordClosestToCursor("ls  foo bar", 9) );
        assertEquals("foo", Parser.findWordClosestToCursor("ls foo ", 6) );
        assertEquals("o", Parser.findWordClosestToCursor("ls o org/jboss/aeshell/Shell.class", 4) );
        assertEquals("", Parser.findWordClosestToCursor("ls  org/jboss/aeshell/Shell.class", 3) );
    }

    public void testFindClosestWordWithEscapedSpaceToCursor() {
        assertEquals("foo bar", Parser.findWordClosestToCursor("foo\\ bar", 7));
        assertEquals("foo ba", Parser.findWordClosestToCursor("foo\\ bar", 6));
        assertEquals("foo bar", Parser.findWordClosestToCursor("ls  foo\\ bar", 11) );
    }

    public void testFindEscapedSpaceWordCloseToEnd() {
        assertEquals("ls\\ foo", Parser.findEscapedSpaceWordCloseToEnd(" ls\\ foo"));
        assertEquals("foo\\ bar", Parser.findEscapedSpaceWordCloseToEnd("ls foo\\ bar"));
        assertEquals("bar", Parser.findEscapedSpaceWordCloseToEnd("ls foo bar"));
        assertEquals("ls\\ foo\\ bar", Parser.findEscapedSpaceWordCloseToEnd("ls\\ foo\\ bar"));
        assertEquals("\\ ls\\ foo\\ bar", Parser.findEscapedSpaceWordCloseToEnd("\\ ls\\ foo\\ bar"));
        assertEquals("ls\\ foo\\ bar", Parser.findEscapedSpaceWordCloseToEnd(" ls\\ foo\\ bar"));
        assertEquals("ls\\ foo\\ bar\\ ", Parser.findEscapedSpaceWordCloseToEnd(" ls\\ foo\\ bar\\ "));
        assertEquals("", Parser.findEscapedSpaceWordCloseToEnd(" ls\\ foo\\ bar\\  "));
    }

    public void testFindEscapedSpaceWord() {
        assertTrue(Parser.doWordContainOnlyEscapedSpace("foo\\ bar"));
        assertTrue(Parser.doWordContainOnlyEscapedSpace("foo\\ bar\\ "));
        assertTrue(Parser.doWordContainOnlyEscapedSpace("\\ foo\\ bar\\ "));
        assertFalse(Parser.doWordContainOnlyEscapedSpace(" foo\\ bar\\ "));
        assertFalse(Parser.doWordContainOnlyEscapedSpace("foo bar\\ "));
        assertFalse(Parser.doWordContainOnlyEscapedSpace("foo bar"));
    }

    public void testChangeWordWithSpaces() {
        assertEquals("foo bar", Parser.switchEscapedSpacesToSpacesInWord("foo\\ bar") );
        assertEquals(" foo bar", Parser.switchEscapedSpacesToSpacesInWord("\\ foo\\ bar") );
        assertEquals(" foo bar ", Parser.switchEscapedSpacesToSpacesInWord("\\ foo\\ bar\\ ") );
        assertEquals(" foo bar", Parser.switchEscapedSpacesToSpacesInWord("\\ foo bar") );

        assertEquals("foo\\ bar", Parser.switchSpacesToEscapedSpacesInWord("foo bar"));
        assertEquals("\\ foo\\ bar", Parser.switchSpacesToEscapedSpacesInWord(" foo bar"));
        assertEquals("\\ foo\\ bar\\ ", Parser.switchSpacesToEscapedSpacesInWord(" foo bar "));
    }

    public void testFindAllWords() {
        List<String> words = Parser.findAllWords("   foo bar\\ baz 12345 ");
        assertEquals("foo", words.get(0));
        assertEquals("bar baz", words.get(1));
        assertEquals("12345", words.get(2));

        words = Parser.findAllWords("man < foo\\ bar ");
        assertEquals("man", words.get(0));
        assertEquals("<", words.get(1));
        assertEquals("foo bar", words.get(2));
    }

    public void testFindAllQuotedWords() {
        List<String> words = Parser.findAllWords("foo bar \"baz 12345\"");
        assertEquals("foo", words.get(0));
        assertEquals("bar", words.get(1));
        assertEquals("baz 12345", words.get(2));

        words = Parser.findAllWords("java -cp \"foo/bar\" \"Example\"");
        assertEquals("foo/bar", words.get(2));
        assertEquals("Example", words.get(3));

        words = Parser.findAllWords("'foo/bar/' Example\\ 1");
        assertEquals("foo/bar/", words.get(0));
        assertEquals("Example 1", words.get(1));

        words = Parser.findAllWords("man -f='foo/bar/' Example\\ 1 foo");
        assertEquals("man", words.get(0));
        assertEquals("-f=foo/bar/", words.get(1));
        assertEquals("Example 1", words.get(2));
        assertEquals("foo", words.get(3));


        try {
            Parser.findAllWords("man -f='foo/bar/ Example\\ 1");
            assertTrue(false);
        }
        catch (IllegalArgumentException iae) {
            assertTrue(true);
        }

        try {
            Parser.findAllWords("man -f='foo/bar/' Example\\ 1\"");
            assertTrue(false);
        }
        catch (IllegalArgumentException iae) {
            assertTrue(true);
        }
    }

    public void testSplitBySizeKeepWords() {
        String words = "foo to bar is how it is i guess";
        List<String> out = Parser.splitBySizeKeepWords(words, 10);
        assertEquals("foo to bar", out.get(0));
        assertEquals("is how it", out.get(1));
        assertEquals("is i guess", out.get(2));

        words = "It is an error to use a backslash prior to any alphabetic";
        out = Parser.splitBySizeKeepWords(words, 20);
        assertEquals("It is an error to", out.get(0));
        assertEquals("use a backslash", out.get(1));
        assertEquals("prior to any", out.get(2));
        assertEquals("alphabetic", out.get(3));
    }


    public void testTrim() {
        assertEquals("foo", Parser.trim("  foo "));
        assertEquals("bar foo", Parser.trim("bar foo "));
        assertEquals("bar foo", Parser.trim(" bar foo"));
        assertEquals("\\ foo\\ ", Parser.trim("\\ foo\\  "));
    }

    public void testFindFirstWord() {
        assertEquals("foo", Parser.findFirstWord(" foo \\ bar"));
        assertEquals("foo", Parser.findFirstWord(" foo bar baz"));
        assertEquals("foo", Parser.findFirstWord("foo bar baz"));
        assertEquals("foobar", Parser.findFirstWord("foobar baz"));
        assertEquals("foobarbaz", Parser.findFirstWord("foobarbaz"));
    }

}
