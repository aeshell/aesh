package org.jboss.aesh.cl;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParseCompleteObjectTest extends TestCase {

    public ParseCompleteObjectTest(String name) {
        super(name);
    }

    public void testParseCompleteObject() throws Exception {
        CommandLineParser clp = ParserGenerator.generateParser(ParseCompleteTest1.class);

        ParsedCompleteObject pco = clp.findCompleteObject("test -e foo1");
        assertEquals("foo1", pco.getValue());
        assertEquals(Boolean.class, pco.getType());
        assertTrue(pco.isOption());

        pco = clp.findCompleteObject("test -f --equal tru");
        assertEquals("tru", pco.getValue());
        assertEquals(Boolean.class, pco.getType());
        assertEquals("equal", pco.getName());
        assertTrue(pco.isOption());

        pco = clp.findCompleteObject("test --equal true foo.txt");
        assertEquals("foo.txt", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertTrue(pco.isArgument());

        pco = clp.findCompleteObject("test -e");
        assertEquals("equal", pco.getName());
        assertEquals("", pco.getValue());

    }
}
@Parameter(name = "test", usage = "a simple test",
        options = {
                @Option(longName = "X", description = "enable X"),
                @Option(name = 'f', longName = "foo", description = "enable foo"),
                @Option(name = 'e', longName = "equal", description = "enable equal",
                        type = Boolean.class, hasValue = true, required = true),
                @Option(name = 'D', description = "define properties",
                        hasValue = true, required = true, isProperty = true)
        })
class ParseCompleteTest1 {}
