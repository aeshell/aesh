/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLineParserTest extends TestCase {

    public CommandLineParserTest(String name) {
        super(name);
    }

    public void testParseCommandLine1() {

        CommandLineParser parser = ParserGenerator.generateParser(Parser1Test.class);

        try {
            CommandLine cl = parser.parse("test -f -e bar -Df=g /tmp/file.txt");
            assertEquals("f", cl.getOptions().get(0).getName());
            assertEquals("e", cl.getOptions().get(1).getName());
            assertEquals("/tmp/file.txt", cl.getArguments().get(0));

            cl = parser.parse("test -e bar -DXms=128m -DXmx=512m /tmp/file.txt");
            assertEquals("e", cl.getOptions().get(0).getName());
            assertEquals("bar", cl.getOptions().get(0).getValue());
            assertEquals("/tmp/file.txt", cl.getArguments().get(0));

            List<OptionProperty> properties = cl.getOptionProperties("D");
            assertEquals("128m", properties.get(0).getValue());
            assertEquals("512m", properties.get(1).getValue());

            cl = parser.parse("test -e=bar -DXms=128m -DXmx=512m /tmp/file.txt");
            assertEquals("e", cl.getOptions().get(0).getName());
            assertEquals("bar", cl.getOptions().get(0).getValue());

            cl = parser.parse("test --equal=bar -DXms=128m -DXmx=512m /tmp/file.txt");
            assertEquals("e", cl.getOptions().get(0).getName());
            assertEquals("equal", cl.getOptions().get(0).getLongName());
            assertEquals("bar", cl.getOptions().get(0).getValue());

            cl = parser.parse("test --equal \"bar bar2\" -DXms=\"128g \" -DXmx=512g\\ m /tmp/file.txt");
            assertEquals("bar bar2", cl.getOptionValue("equal"));

            assertEquals("Xms", cl.getOptionProperties("D").get(0).getName());
            assertEquals("128g ", cl.getOptionProperties("D").get(0).getValue());
            assertEquals("Xmx", cl.getOptionProperties("D").get(1).getName());
            assertEquals("512g m", cl.getOptionProperties("D").get(1).getValue());

        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        try {
            CommandLine cl = parser.parse("test -a /tmp/file.txt");
            assertTrue(false);
            cl.getArguments();
        }
        catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            CommandLine cl = parser.parse("test -a /tmp/file.txt");
            assertTrue(false);
            cl.getArguments();
        }
        catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        try {
            CommandLine cl = parser.parse("test -e bar --equal bar2 -DXms=128m -DXmx=512m /tmp/file.txt");
            assertTrue(false);
            cl.getArguments();
        }
        catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            CommandLine cl = parser.parse("test -f -Dfoo:bar /tmp/file.txt");
            assertTrue(false);
            cl.getArguments();
        }
        catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            CommandLine cl = parser.parse("test -f foobar /tmp/file.txt");
            assertTrue(false);
            cl.getArguments();
        }
        catch (IllegalArgumentException e) {
            assertTrue(true);
        }

    }

    public void testParseCommandLine2() {

        CommandLineParser parser = ParserGenerator.generateParser(Parser2Test.class);

        try {
            CommandLine cl = parser.parse("test -d --bar Foo.class");
            assertTrue(cl.hasOption('d'));
            assertFalse(cl.hasOption('V'));
            assertEquals("Foo.class", cl.getOptionValue("bar"));
            assertEquals(new ArrayList<String>(), cl.getArguments());

            cl = parser.parse("test -V -d -b com.bar.Bar.class /tmp/file\\ foo.txt /tmp/bah.txt");
            assertTrue(cl.hasOption('V'));
            assertTrue(cl.hasOption('d'));
            assertTrue(cl.hasOption('b'));
            assertEquals("com.bar.Bar.class", cl.getOptionValue("b"));
            assertEquals("/tmp/file foo.txt", cl.getArguments().get(0));
            assertEquals("/tmp/bah.txt", cl.getArguments().get(1));

        }
        catch (IllegalArgumentException e) {
        }
        try {
            CommandLine cl = parser.parse("test -d /tmp/file.txt");
            assertTrue(false);
            cl.getArguments();
        }
        catch (IllegalArgumentException e) {
            assertTrue(true);
        }

    }

    public void testParseCommandLine3() {
        try {
            ParserGenerator.generateParser(Parser3Test.class);
            assertTrue(false);
        }
        catch (IllegalArgumentException iae) {
            assertTrue(true);
        }
    }

    public void testParseCommandLine4() {
        CommandLineParser clp = ParserGenerator.generateParser(Parser4Test.class);

        CommandLine cl = clp.parse("test -o bar1,bar2,bar3 foo");
        assertTrue(cl.hasOption('o'));
        assertEquals("bar1", cl.getOptionValues("o").get(0));
        assertEquals("bar3", cl.getOptionValues("o").get(2));

        cl = clp.parse("test --help bar4,bar5,bar6 foo");
        assertTrue(cl.hasOption("help"));
        assertEquals("bar4", cl.getOptionValues("help").get(0));
        assertEquals("bar6", cl.getOptionValues("h").get(2));

    }
}

@Parameter(usage = "a simple test",
        options = {
                @Option(name = 'f', longName = "foo", description = "enable foo"),
                @Option(name = 'e', longName = "equal", description = "enable equal",
                        hasValue = true, required = true),
                @Option(name = 'D', description = "define properties",
                        hasValue = true, required = true, isProperty = true)
        })
class Parser1Test {}

@Parameter(usage = "more [options] file...",
        options = {
                @Option(name = 'd', longName = "display", hasValue = false, description = "display help instead of ring bell"),
                @Option(name = 'b', longName = "bar", argument = "classname", required = true,
                        hasValue = true, description = "bar bar"),
                @Option(name = 'V', longName = "version",
                        hasValue = false, description = "output version information and exit")
        })
class Parser2Test {}

@Parameter(usage = "this should fail",
        options = {
                @Option()
        })
class Parser3Test {}

@Parameter(usage = "testing multiple values",
        options = {
                @Option(name = 'o', longName="option", hasValue = true, hasMultipleValues = true,
                        valueSeparator = ','),
                @Option(name = 'h', longName="help", hasValue = true, hasMultipleValues = true,
                        valueSeparator = ',')
        })
class Parser4Test {}

