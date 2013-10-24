package org.jboss.aesh.cl;

import org.jboss.aesh.cl.parser.CommandLineCompletionParser;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.parser.ParsedCompleteObject;
import org.jboss.aesh.cl.parser.ParserGenerator;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParseCompleteObjectTest {


    @Test
    public void testParseCompleteObject() throws Exception {
        CommandLineParser clp = ParserGenerator.generateCommandLineParser(ParseCompleteTest1.class);
        CommandLineCompletionParser completeParser = clp.getCompletionParser();

        ParsedCompleteObject pco = completeParser.findCompleteObject("test -e foo1");
        assertEquals("foo1", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertTrue(pco.isOption());

        pco = completeParser.findCompleteObject("test -f false --equal tru");
        assertEquals("tru", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertEquals("equal", pco.getName());
        assertTrue(pco.isOption());
        assertFalse(pco.doDisplayOptions());

        pco = completeParser.findCompleteObject("test -f false --equal file\\ with\\ spaces\\ ");
        assertEquals("file with spaces ", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertEquals("equal", pco.getName());
        assertTrue(pco.isOption());
        assertFalse(pco.doDisplayOptions());

        pco = completeParser.findCompleteObject("test -f=true --equal ");
        assertEquals("", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertEquals("equal", pco.getName());
        assertTrue(pco.isOption());
        assertFalse(pco.doDisplayOptions());

        pco = completeParser.findCompleteObject("test -f true --equ ");
        assertFalse(pco.isArgument());
        assertFalse(pco.doDisplayOptions());
        assertFalse(pco.isOption());
        assertFalse(pco.isCompleteOptionName());

        pco = completeParser.findCompleteObject("test --equal true foo.txt");
        assertEquals("foo.txt", pco.getValue());
        //assertEquals(String.class, pco.getType());
        assertTrue(pco.isArgument());

        pco = completeParser.findCompleteObject("test -e");
        assertTrue(pco.doDisplayOptions());
        assertTrue(pco.isCompleteOptionName());
        assertEquals("e", pco.getName());
        assertEquals("--equal", clp.getCommand().findPossibleLongNamesWitdDash(pco.getName()).get(0).getCharacters());

        pco = completeParser.findCompleteObject("test --eq");
        assertTrue(pco.doDisplayOptions());
        assertFalse(pco.isCompleteOptionName());
        assertEquals("eq", pco.getName());
        assertEquals(4, pco.getOffset());
        assertEquals("--equal", clp.getCommand().findPossibleLongNamesWitdDash(pco.getName()).get(0).getCharacters());

        clp.getCommand().clear();
        pco = completeParser.findCompleteObject("test --");
        assertTrue(pco.doDisplayOptions());
        assertEquals("", pco.getName());
        assertEquals(2, pco.getOffset());
        assertEquals(4, clp.getCommand().getOptionLongNamesWithDash().size());

        pco = completeParser.findCompleteObject("test --equal true  ");
        assertTrue(pco.isArgument());

        pco = completeParser.findCompleteObject("test -f");
        assertTrue(pco.doDisplayOptions());
        assertTrue(pco.isCompleteOptionName());

        pco = completeParser.findCompleteObject("test --equal");
        assertTrue(pco.doDisplayOptions());
        assertTrue(pco.isCompleteOptionName());

        pco = completeParser.findCompleteObject("test --f");
        assertTrue(pco.doDisplayOptions());
        assertFalse(pco.isCompleteOptionName());

        pco = completeParser.findCompleteObject("test ");
        assertTrue(pco.isArgument());
        assertFalse(pco.doDisplayOptions());
        assertFalse(pco.isOption());
        assertTrue(pco.getValue().length() == 0);

        pco = completeParser.findCompleteObject("test a");
        assertTrue(pco.isArgument());
        assertFalse(pco.doDisplayOptions());
        assertFalse(pco.isOption());

        pco = completeParser.findCompleteObject("test a1 b1 ");
        assertTrue(pco.isArgument());
        assertFalse(pco.doDisplayOptions());
        assertFalse(pco.isOption());
        assertTrue(pco.getValue() == null || pco.getValue().length() == 0);

        pco = completeParser.findCompleteObject("test a\\ ");
        assertTrue(pco.isArgument());
        assertFalse(pco.doDisplayOptions());
        assertFalse(pco.isOption());
        assertEquals("a ",  pco.getValue());
    }

    @Test
    public void testParseCompleteObject2() throws Exception {
        CommandLineParser clp = ParserGenerator.generateCommandLineParser(ParseCompleteTest2.class);
        CommandLineCompletionParser completeParser = clp.getCompletionParser();

        ParsedCompleteObject pco = completeParser.findCompleteObject("test -e ");
        assertEquals(Boolean.class, pco.getType());
        assertTrue(pco.isOption());

        pco = completeParser.findCompleteObject("test ");
        assertFalse(pco.isOption());
        assertFalse(pco.isCompleteOptionName());
        assertFalse(pco.isArgument());
        assertTrue(pco.doDisplayOptions());
    }

    @Test
    public void testParseCompleteObject3() throws Exception {
        CommandLineParser clp = ParserGenerator.generateCommandLineParser(ParseCompleteTest3.class);
        CommandLineCompletionParser completeParser = clp.getCompletionParser();

        ParsedCompleteObject pco = completeParser.findCompleteObject("test -v 1 2 3 ");
        assertEquals(String.class, pco.getType());
        assertTrue(pco.isOption());
        assertEquals("",pco.getValue());

        pco = completeParser.findCompleteObject("test -v 1 2 3");
        assertEquals(String.class, pco.getType());
        assertTrue(pco.isOption());
        assertEquals("3",pco.getValue());
    }

}
@CommandDefinition(name = "test", description = "a simple test")
class ParseCompleteTest1 {

    @Option(name = "X", description = "enable X")
    private String X;

    @Option(shortName = 'f', name = "foo", description = "enable foo")
    private Boolean foo;

    @Option(shortName = 'e', name = "equal", description = "enable equal", required = true)
    private String equal;

    @Option(shortName = 'D', description = "define properties", required = true)
    private String define;

    @Arguments
    private List<String> arguments;

}

@CommandDefinition(name = "test", description = "a simple test")
class ParseCompleteTest2 {

    @Option(shortName = 'X', description = "enable X")
    private String X;

    @Option(shortName = 'f', name = "foo", description = "enable foo")
    private String foo;

    @Option(shortName = 'e', name = "equal", description = "enable equal", required = true, defaultValue = "false")
    private Boolean equal;

    @Option(shortName = 'D', description = "define properties",
            required = true)
    private String define;
}

@CommandDefinition(name = "test", description = "a simple test")
class ParseCompleteTest3 {

    @Option(shortName = 'X', description = "enable X")
    private String X;

    @OptionList(shortName = 'v', name = "value", description = "enable equal")
    private List<String> values;

    @Option(shortName = 'D', description = "define properties",
            required = true)
    private String define;
}
