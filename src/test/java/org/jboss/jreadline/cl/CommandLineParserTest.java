/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.cl;

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
            CommandLine cl = parser.parse("test -f /tmp/file.txt");
            assertEquals("f", cl.getOptions().get(0).getName());
            assertEquals("/tmp/file.txt", cl.getArguments().get(0));
            cl = parser.parse("test -f -e bar /tmp/file.txt");
            assertEquals("f", cl.getOptions().get(0).getName());
            assertEquals("e", cl.getOptions().get(1).getName());
            assertEquals("bar", cl.getOptions().get(1).getValue());
            assertEquals("/tmp/file.txt", cl.getArguments().get(0));
        }
        catch (CommandLineParserException e) {
        }
        try {
            CommandLine cl = parser.parse("test -a /tmp/file.txt");
            assertTrue(false);
            cl.getArguments();
        }
        catch (CommandLineParserException e) {
            assertTrue(true);
        }


    }
    public void testParseCommandLine2() {

        CommandLineParser parser = ParserGenerator.generateParser(Parser2Test.class);

        try {
            CommandLine cl = parser.parse("test -d --bar Foo.class");
            assertEquals("d", cl.getOptions().get(0).getName());
            assertEquals("Foo.class", cl.getOptions().get(1).getValue());
            assertEquals(new ArrayList<String>(), cl.getArguments());

            cl = parser.parse("test -V -d -b com.bar.Bar.class /tmp/file\\ foo.txt /tmp/bah.txt");
            assertEquals("V", cl.getOptions().get(0).getName());
            assertEquals("d", cl.getOptions().get(1).getName());
            assertEquals("b", cl.getOptions().get(2).getName());
            assertEquals("com.bar.Bar.class", cl.getOptions().get(2).getValue());
            assertEquals("/tmp/file\\ foo.txt", cl.getArguments().get(0));
            assertEquals("/tmp/bah.txt", cl.getArguments().get(1));

        }
        catch (CommandLineParserException e) {
        }
        try {
            CommandLine cl = parser.parse("test -d /tmp/file.txt");
            assertTrue(false);
            cl.getArguments();
        }
        catch (CommandLineParserException e) {
            assertTrue(true);
        }


    }}

@Parameter(usage = "a simple test",
        parser = ParserType.GNU,
        options = {
                @Option(name = "f", longName = "foo", description = "enable foo"),
                @Option(name = "e", description = "enable e", hasValue = true, required = true)
        })
class Parser1Test {}

@Parameter(usage = "more [options] file...",
        parser = ParserType.POSIX,
        options = {
                @Option(name = "d", longName = "display", hasValue = false, description = "display help instead of ring bell"),
                @Option(name = "b", longName = "bar", argument = "classname", required = true,
                        hasValue = true, description = "bar bar"),
                @Option(name = "V", longName = "version",
                        hasValue = false, description = "output version information and exit")
        })
class Parser2Test {}

