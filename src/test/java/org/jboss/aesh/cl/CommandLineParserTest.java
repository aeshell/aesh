/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import junit.framework.TestCase;
import org.jboss.aesh.cl.exception.CommandLineParserException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLineParserTest extends TestCase {

    public CommandLineParserTest(String name) {
        super(name);
    }

    public void testParseCommandLine1() throws CommandLineParserException {

        CommandLineParser parser = ParserGenerator.generateCommandLineParser(Parser1Test.class);

        try {
            CommandLine cl = parser.parse("test -f -e bar -Df=g /tmp/file.txt");
            assertEquals("f", cl.getOptions().get(0).getShortName());
            assertEquals("e", cl.getOptions().get(1).getShortName());
            assertEquals("/tmp/file.txt", cl.getArgument().getValues().get(0));

            cl = parser.parse("test -e bar -DXms=128m -DXmx=512m --X /tmp/file.txt");
            assertEquals("e", cl.getOptions().get(0).getShortName());
            assertEquals("bar", cl.getOptions().get(0).getValue());
            assertEquals("/tmp/file.txt", cl.getArgument().getValues().get(0));
            assertNotNull(cl.hasOption("X"));

            Map<String,String> properties = cl.getOptionProperties("D");
            assertEquals("128m", properties.get("Xms"));
            assertEquals("512m", properties.get("Xmx"));

            cl = parser.parse("test -e=bar -DXms=128m -DXmx=512m /tmp/file.txt");
            assertEquals("e", cl.getOptions().get(0).getShortName());
            assertEquals("bar", cl.getOptions().get(0).getValue());

            cl = parser.parse("test --equal=bar -DXms=128m -DXmx=512m /tmp/file.txt");
            assertEquals("e", cl.getOptions().get(0).getShortName());
            assertEquals("equal", cl.getOptions().get(0).getName());
            assertEquals("bar", cl.getOptions().get(0).getValue());

            cl = parser.parse("test --equal \"bar bar2\" -DXms=\"128g \" -DXmx=512g\\ m /tmp/file.txt");
            assertEquals("bar bar2", cl.getOptionValue("equal"));

            assertTrue(cl.getOptionProperties("D").containsKey("Xms"));
            assertEquals("128g ", cl.getOptionProperties("D").get("Xms"));
            assertTrue(cl.getOptionProperties("D").containsKey("Xmx"));
            assertEquals("512g m", cl.getOptionProperties("D").get("Xmx"));

        }
        catch (CommandLineParserException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        try {
            CommandLine cl = parser.parse("test -a /tmp/file.txt");
            assertTrue(false);
            cl.getArgument();
        }
        catch (CommandLineParserException e) {
            assertTrue(true);
        }

        try {
            CommandLine cl = parser.parse("test -a /tmp/file.txt");
            assertTrue(false);
            cl.getArgument();
        }
        catch (CommandLineParserException e) {
            assertTrue(true);
        }
        try {
            CommandLine cl = parser.parse("test -e bar --equal bar2 -DXms=128m -DXmx=512m /tmp/file.txt");
            assertTrue(false);
            cl.getArgument();
        }
        catch (CommandLineParserException e) {
            assertTrue(true);
        }

        try {
            CommandLine cl = parser.parse("test -f -Dfoo:bar /tmp/file.txt");
            assertTrue(false);
            cl.getArgument();
        }
        catch (CommandLineParserException e) {
            assertTrue(true);
        }

        try {
            CommandLine cl = parser.parse("test -f foobar /tmp/file.txt");
            assertTrue(false);
            cl.getArgument();
        }
        catch (CommandLineParserException e) {
            assertTrue(true);
        }

    }

    public void testParseCommandLine2() throws CommandLineParserException {

        CommandLineParser parser = ParserGenerator.generateCommandLineParser(Parser2Test.class);

        try {
            CommandLine cl = parser.parse("test -d --bar Foo.class");
            assertTrue(cl.hasOption('d'));
            assertFalse(cl.hasOption('V'));
            assertEquals("Foo.class", cl.getOptionValue("bar"));
            assertEquals(new ArrayList<String>(), cl.getArgument());

            cl = parser.parse("test -V -d -b com.bar.Bar.class /tmp/file\\ foo.txt /tmp/bah.txt");
            assertTrue(cl.hasOption('V'));
            assertTrue(cl.hasOption('d'));
            assertTrue(cl.hasOption('b'));
            assertEquals("com.bar.Bar.class", cl.getOptionValue("b"));
            assertEquals("/tmp/file foo.txt", cl.getArgument().getValues().get(0));
            assertEquals("/tmp/bah.txt", cl.getArgument().getValues().get(1));

        }
        catch (CommandLineParserException e) {
        }
        try {
            CommandLine cl = parser.parse("test -d /tmp/file.txt");
            assertTrue(false);
            cl.getArgument();
        }
        catch (CommandLineParserException e) {
            assertTrue(true);
        }

    }

    public void testParseCommandLine3() {
        /*
        try {
            ParserGenerator.generateParser(Parser3Test.class);
            assertTrue(false);
        }
        catch (CommandLineParserException iae) {
            assertTrue(true);
        }
        */
    }

    public void testParseCommandLine4() throws CommandLineParserException {
        CommandLineParser clp = ParserGenerator.generateCommandLineParser(Parser4Test.class);

        CommandLine cl = clp.parse("test -o bar1,bar2,bar3 foo");
        assertTrue(cl.hasOption('o'));
        assertEquals("bar1", cl.getOptionValues("o").get(0));
        assertEquals("bar3", cl.getOptionValues("o").get(2));

        cl = clp.parse("test --help bar4:bar5:bar6 foo");
        assertTrue(cl.hasOption("help"));
        assertEquals("bar4", cl.getOptionValues("help").get(0));
        assertEquals("bar6", cl.getOptionValues("h").get(2));

        cl = clp.parse("test --bar 1,2,3");
        assertTrue(cl.hasOption("bar"));
        assertEquals(Integer.class, cl.getOption("bar").getType());
    }
}

@CommandDefinition(name = "test", description = "a simple test")
class Parser1Test {

    @Option(name = "X", description = "enable X", hasValue = false)
    private Boolean enableX;

    @Option(shortName = 'f', name = "foo", description = "enable foo", hasValue = false)
    private Boolean foo;

    @Option(shortName = 'e', name = "equal", description = "enable equal", required = true)
    private String equal;

    @OptionGroup(shortName = 'D', description = "define properties", required = true)
    private Map<String,String> define;

    @Arguments
    private List<String> arguments;
}

@CommandDefinition(name = "test", description = "more [options] file...")
class Parser2Test {
    @Option(shortName = 'd', name = "display", description = "display help instead of ring bell")
    private String display;

    @Option(shortName = 'b', name = "bar", argument = "classname", required = true, description = "bar bar")
    private String bar;

    @Option(shortName = 'V', name = "version", description = "output version information and exit")
    private String version;
}

@CommandDefinition(name = "test", description = "this is a command without options")
class Parser3Test {}

@CommandDefinition(name = "test", description = "testing multiple values")
class Parser4Test {
    @OptionList(shortName = 'o', name="option", valueSeparator = ',')
    private List<String> option;

    @OptionList
    private List<Integer> bar;

    @OptionList(valueSeparator = ':')
    private List<String> help;

    @Arguments
    private List<String> arguments;
}

