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

        ParsedCompleteObject pco = completeParser.findCompleteObject("test -e foo1", 100);
        assertEquals("foo1", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertTrue(pco.isOption());

        pco = completeParser.findCompleteObject("test -f false --equal tru", 100);
        assertEquals("tru", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertEquals("equal", pco.getName());
        assertTrue(pco.isOption());
        assertFalse(pco.doDisplayOptions());

        pco = completeParser.findCompleteObject("test -f false --equal file\\ with\\ spaces\\ ", 100);
        assertEquals("file with spaces ", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertEquals("equal", pco.getName());
        assertTrue(pco.isOption());
        assertFalse(pco.doDisplayOptions());

        pco = completeParser.findCompleteObject("test -f=true --equal ", 100);
        assertEquals("", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertEquals("equal", pco.getName());
        assertTrue(pco.isOption());
        assertFalse(pco.doDisplayOptions());

        pco = completeParser.findCompleteObject("test -f true --equ ", 100);
        assertFalse(pco.isArgument());
        assertFalse(pco.doDisplayOptions());
        assertFalse(pco.isOption());
        assertFalse(pco.isCompleteOptionName());

        pco = completeParser.findCompleteObject("test --equal true foo.txt", 100);
        assertEquals("foo.txt", pco.getValue());
        //assertEquals(String.class, pco.getStyle());
        assertTrue(pco.isArgument());

        pco = completeParser.findCompleteObject("test -e", 100);
        assertTrue(pco.doDisplayOptions());
        assertTrue(pco.isCompleteOptionName());
        assertEquals("e", pco.getName());
        clp.getCommand().clear();
        assertEquals("--equal", clp.getCommand().findPossibleLongNamesWitdDash(pco.getName()).get(0).getCharacters());

        pco = completeParser.findCompleteObject("test --eq", 100);
        assertTrue(pco.doDisplayOptions());
        assertFalse(pco.isCompleteOptionName());
        assertEquals("eq", pco.getName());
        assertEquals(4, pco.getOffset());
        clp.getCommand().clear();
        assertEquals("--equal", clp.getCommand().findPossibleLongNamesWitdDash(pco.getName()).get(0).getCharacters());

        clp.getCommand().clear();
        pco = completeParser.findCompleteObject("test --", 100);
        assertTrue(pco.doDisplayOptions());
        assertEquals("", pco.getName());
        assertEquals(2, pco.getOffset());
        assertEquals(4, clp.getCommand().getOptionLongNamesWithDash().size());

        pco = completeParser.findCompleteObject("test --equal true  ", 100);
        assertTrue(pco.isArgument());

        pco = completeParser.findCompleteObject("test -f", 100);
        assertTrue(pco.doDisplayOptions());
        assertTrue(pco.isCompleteOptionName());

        pco = completeParser.findCompleteObject("test --equal", 100);
        assertTrue(pco.doDisplayOptions());
        assertTrue(pco.isCompleteOptionName());

        pco = completeParser.findCompleteObject("test --f", 100);
        assertTrue(pco.doDisplayOptions());
        assertFalse(pco.isCompleteOptionName());

        pco = completeParser.findCompleteObject("test ", 100);
        assertTrue(pco.isArgument());
        assertFalse(pco.doDisplayOptions());
        assertFalse(pco.isOption());
        assertTrue(pco.getValue().length() == 0);

        pco = completeParser.findCompleteObject("test a", 100);
        assertTrue(pco.isArgument());
        assertFalse(pco.doDisplayOptions());
        assertFalse(pco.isOption());

        pco = completeParser.findCompleteObject("test a1 b1 ", 100);
        assertTrue(pco.isArgument());
        assertFalse(pco.doDisplayOptions());
        assertFalse(pco.isOption());
        assertTrue(pco.getValue() == null || pco.getValue().length() == 0);

        pco = completeParser.findCompleteObject("test a\\ ", 100);
        assertTrue(pco.isArgument());
        assertFalse(pco.doDisplayOptions());
        assertFalse(pco.isOption());
        assertEquals("a ",  pco.getValue());
    }

    @Test
    public void testParseCompleteObject2() throws Exception {
        CommandLineParser clp = ParserGenerator.generateCommandLineParser(ParseCompleteTest2.class);
        CommandLineCompletionParser completeParser = clp.getCompletionParser();

        ParsedCompleteObject pco = completeParser.findCompleteObject("test -e ", 100);
        assertEquals(Boolean.class, pco.getType());
        assertTrue(pco.isOption());

        pco = completeParser.findCompleteObject("test ", 100);
        assertFalse(pco.isOption());
        assertFalse(pco.isCompleteOptionName());
        assertFalse(pco.isArgument());
        assertTrue(pco.doDisplayOptions());
    }

    @Test
    public void testParseCompleteObject3() throws Exception {
        CommandLineParser clp = ParserGenerator.generateCommandLineParser(ParseCompleteTest3.class);
        CommandLineCompletionParser completeParser = clp.getCompletionParser();

        ParsedCompleteObject pco = completeParser.findCompleteObject("test -v 1 2 3 ", 100);
        assertEquals(String.class, pco.getType());
        assertTrue(pco.isOption());
        assertEquals("",pco.getValue());

        pco = completeParser.findCompleteObject("test -v 1 2 3", 100);
        assertEquals(String.class, pco.getType());
        assertTrue(pco.isOption());
        assertEquals("3",pco.getValue());
    }

    @Test
    public void testCursorInsideBuffer() throws Exception {
        CommandLineParser clp = ParserGenerator.generateCommandLineParser(ParseCompleteTest1.class);
        CommandLineCompletionParser completeParser = clp.getCompletionParser();

        ParsedCompleteObject pco = completeParser.findCompleteObject("test -e foo1  asdfjeaasdfae", 12);
        assertEquals("foo1", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertTrue(pco.isOption());

        pco = completeParser.findCompleteObject("test --equal tru  -f false ", 16);
        assertEquals("tru", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertEquals("equal", pco.getName());
        assertTrue(pco.isOption());
        assertFalse(pco.doDisplayOptions());

        pco = completeParser.findCompleteObject("test -f false --equal file\\ with\\ spaces\\ ", 100);
        assertEquals("file with spaces ", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertEquals("equal", pco.getName());
        assertTrue(pco.isOption());
        assertFalse(pco.doDisplayOptions());

        pco = completeParser.findCompleteObject("test --equal  -f=true ", 13);
        assertEquals("", pco.getValue());
        assertEquals(String.class, pco.getType());
        assertEquals("equal", pco.getName());
        assertTrue(pco.isOption());
        assertFalse(pco.doDisplayOptions());

        pco = completeParser.findCompleteObject("test --equ  -f true", 11);
        assertFalse(pco.isArgument());
        assertFalse(pco.doDisplayOptions());
        assertFalse(pco.isOption());
        assertFalse(pco.isCompleteOptionName());

        pco = completeParser.findCompleteObject("test --equal true foo.txt  bar.txt", 25);
        assertEquals("foo.txt", pco.getValue());
        //assertEquals(String.class, pco.getStyle());
        assertTrue(pco.isArgument());
    }

    @Test
    public void testCompletionWithNoArguments() throws Exception {
        CommandLineParser clp = ParserGenerator.generateCommandLineParser(ParseCompleteTest2.class);
        CommandLineCompletionParser completeParser = clp.getCompletionParser();

        ParsedCompleteObject pco = completeParser.findCompleteObject("test ", 4);
        assertFalse(pco.isArgument());
        assertTrue(pco.doDisplayOptions());

        pco = completeParser.findCompleteObject("test -X foo1 ", 13);
        assertFalse(pco.isArgument());
        assertTrue(pco.doDisplayOptions());
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
