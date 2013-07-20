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
        CommandLineCompletionParser completeParser = new CommandLineCompletionParser(clp);

        ParsedCompleteObject pco = completeParser.findCompleteObject("test -e foo1");
        assertEquals("foo1", pco.getValue());
        assertEquals(Boolean.class, pco.getType());
        assertTrue(pco.isOption());

        pco = completeParser.findCompleteObject("test -f --equal tru");
        assertEquals("tru", pco.getValue());
        assertEquals(Boolean.class, pco.getType());
        assertEquals("equal", pco.getName());
        assertTrue(pco.isOption());
        assertFalse(pco.doDisplayOptions());

        pco = completeParser.findCompleteObject("test -f --equal ");
        assertEquals("", pco.getValue());
        assertEquals(Boolean.class, pco.getType());
        assertEquals("equal", pco.getName());
        assertTrue(pco.isOption());
        assertFalse(pco.doDisplayOptions());

        pco = completeParser.findCompleteObject("test -f --equ ");
        assertFalse(pco.isArgument());
        assertFalse(pco.doDisplayOptions());
        assertFalse(pco.isOption());

        pco = completeParser.findCompleteObject("test --equal true foo.txt");
        assertEquals("foo.txt", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertTrue(pco.isArgument());

        pco = completeParser.findCompleteObject("test -e");
        assertTrue(pco.doDisplayOptions());
        assertEquals("e", pco.getName());
        assertEquals("--equal", clp.getParameters().get(0).findPossibleLongNamesWitdDash(pco.getName()).get(0));

        pco = completeParser.findCompleteObject("test --eq");
        assertTrue(pco.doDisplayOptions());
        assertEquals("eq", pco.getName());
        assertEquals(4, pco.getOffset());
        assertEquals("--equal", clp.getParameters().get(0).findPossibleLongNamesWitdDash(pco.getName()).get(0));

        pco = completeParser.findCompleteObject("test --");
        assertTrue(pco.doDisplayOptions());
        assertEquals("", pco.getName());
        assertEquals(2, pco.getOffset());
        assertEquals(4, clp.getParameters().get(0).getOptionLongNamesWithDash().size());

        pco = completeParser.findCompleteObject("test --equal true  ");
        assertTrue(pco.isArgument());

        pco = completeParser.findCompleteObject("test ");
        assertFalse(pco.isArgument());
        assertFalse(pco.doDisplayOptions());
        assertFalse(pco.isOption());
    }

    public void testParseCompleteObject2() throws Exception {
        CommandLineParser clp = ParserGenerator.generateParser(ParseCompleteTest1.class);
        CommandLineCompletionParser completeParser = new CommandLineCompletionParser(clp);

        ParsedCompleteObject pco = completeParser.findCompleteObject("test -e ");
        assertEquals(Boolean.class, pco.getType());
        assertTrue(pco.isOption());

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

@Parameter(name = "test", usage = "a simple test",
        options = {
                @Option(longName = "X", description = "enable X"),
                @Option(name = 'f', longName = "foo", description = "enable foo"),
                @Option(name = 'e', longName = "equal", description = "enable equal",
                        type = Boolean.class, hasValue = true, required = true, defaultValue = "false"),
                @Option(name = 'D', description = "define properties",
                        hasValue = true, required = true, isProperty = true)
        })
class ParseCompleteTest2 {}
