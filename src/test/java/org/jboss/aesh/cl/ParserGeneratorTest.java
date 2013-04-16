/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import junit.framework.TestCase;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.OptionInt;

import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParserGeneratorTest extends TestCase {

    public ParserGeneratorTest(String name) {
        super(name);
    }

    public void testClassGenerator() throws CommandLineParserException {

        CommandLineParser parser = ParserGenerator.generateParser(Test1.class);

        assertEquals("a simple test", parser.getParameters().get(0).getUsage());
        List<OptionInt> options = parser.getParameters().get(0).getOptions();
        assertEquals("f", options.get(0).getName());
        assertEquals("foo", options.get(0).getLongName());
        assertEquals("e", options.get(1).getName());
        assertEquals("enable e", options.get(1).getDescription());
        assertTrue(options.get(1).hasValue());
        assertTrue(options.get(1).isRequired());

        parser = ParserGenerator.generateParser(Test2.class);
        assertEquals("more [options] file...", parser.getParameters().get(0).getUsage());
        options = parser.getParameters().get(0).getOptions();
        assertEquals("d", options.get(0).getName());
        assertEquals("V", options.get(1).getName());

        parser = ParserGenerator.generateParser(Test3.class);
        options = parser.getParameters().get(0).getOptions();
        assertEquals("t", options.get(0).getName());
        assertEquals("e", options.get(1).getName());

    }
}

@Parameter(name = "test", usage = "a simple test",
        options = {
                @Option(name = 'f', longName = "foo", description = "enable foo"),
                @Option(name = 'e', description = "enable e", hasValue = true, required = true)
        })
class Test1 {}

@Parameter(name = "test", usage = "more [options] file...",
        options = {
                @Option(name = 'd', description = "display help instead of ring bell"),
                @Option(name = 'V', description = "output version information and exit")
        })
class Test2 {}

@Parameter(name = "test", usage = "more [options] file...",
        options = {
                @Option(longName = "target", description = "target directory"),
                @Option(longName = "test", description = "test run")
        })
class Test3 {}

