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

}
