package org.jboss.jreadline.util;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParserTestCase extends TestCase {

    public ParserTestCase(String name) {
        super(name);
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

    }


}
