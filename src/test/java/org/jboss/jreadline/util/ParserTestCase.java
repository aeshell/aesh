package org.jboss.jreadline.util;

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
        assertEquals("o", Parser.findWordClosestToCursor("ls o org/jboss/jreadlineshell/Shell.class", 4) );
        assertEquals("", Parser.findWordClosestToCursor("ls  org/jboss/jreadlineshell/Shell.class", 3) );
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
    }

    public void testFindExcapedSpaceWordCloseToBeginning() {
        assertEquals("ls\\ foo", Parser.findEscapedSpaceWordCloseToBeginning("ls\\ foo "));
        assertEquals("ls", Parser.findEscapedSpaceWordCloseToBeginning("ls foo"));
        assertEquals("ls\\ foo\\ bar", Parser.findEscapedSpaceWordCloseToBeginning("ls\\ foo\\ bar"));
        assertEquals("", Parser.findEscapedSpaceWordCloseToBeginning(" ls\\ foo\\ bar"));
    }

    public void testFindWordClosestToCursorDividedByRedirectOrPipe() {
        assertEquals("foo", Parser.findWordClosestToCursorDividedByRedirectOrPipe("ls > foo", 8));
        assertEquals("foo", Parser.findWordClosestToCursorDividedByRedirectOrPipe("ls | foo", 8));
        assertEquals("fo", Parser.findWordClosestToCursorDividedByRedirectOrPipe("ls > foo", 7));
        assertEquals("fo", Parser.findWordClosestToCursorDividedByRedirectOrPipe("ls | foo", 7));
        assertEquals("foo", Parser.findWordClosestToCursorDividedByRedirectOrPipe("ls > foo ", 9));
        assertEquals("", Parser.findWordClosestToCursorDividedByRedirectOrPipe("ls > foo  ", 10));
        assertEquals("", Parser.findWordClosestToCursorDividedByRedirectOrPipe("ls > ", 5));
        assertEquals("", Parser.findWordClosestToCursorDividedByRedirectOrPipe("ls >  ", 6));
        assertEquals("", Parser.findWordClosestToCursorDividedByRedirectOrPipe("ls > bla > ", 11));
        assertEquals("", Parser.findWordClosestToCursorDividedByRedirectOrPipe("ls | bla > ", 11));
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

}
