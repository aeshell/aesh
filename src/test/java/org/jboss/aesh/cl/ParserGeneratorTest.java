/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.parser.ParserGenerator;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParserGeneratorTest {

    @Test
    public void testClassGenerator() throws CommandLineParserException {

        Test1 test1 = new Test1();
        CommandLineParser parser = ParserGenerator.generateCommandLineParser(test1);

        assertEquals("a simple test", parser.getCommand().getDescription());
        List<ProcessedOption> options = parser.getCommand().getOptions();
        assertEquals("f", options.get(0).getShortName());
        assertEquals("foo", options.get(0).getName());
        assertEquals("e", options.get(1).getShortName());
        assertEquals("enable e", options.get(1).getDescription());
        assertTrue(options.get(1).hasValue());
        assertTrue(options.get(1).isRequired());
        assertEquals("bar", options.get(2).getName());
        assertFalse(options.get(2).hasValue());

        Test2 test2 = new Test2();
        parser = ParserGenerator.generateCommandLineParser(test2);
        assertEquals("more [options] file...", parser.getCommand().getDescription());
        options = parser.getCommand().getOptions();
        assertEquals("d", options.get(0).getShortName());
        assertEquals("V", options.get(1).getShortName());

        parser = ParserGenerator.generateCommandLineParser(Test3.class);
        options = parser.getCommand().getOptions();
        assertEquals("t", options.get(0).getShortName());
        assertEquals("e", options.get(1).getShortName());

    }
}

@CommandDefinition(name = "test", description = "a simple test")
class Test1 {
    @Option(shortName = 'f', name = "foo", description = "enable foo")
    private String foo;

    @Option(shortName = 'e', description = "enable e", required = true)
    private String e;

    @Option(description = "has enabled bar", hasValue = false)
    private Boolean bar;

}

@CommandDefinition(name = "test", description = "more [options] file...")
class Test2 {

    @Option(shortName = 'd', description = "display help instead of ring bell")
    private String display;

    @Option(shortName = 'V', description = "output version information and exit")
    private String version;
}

@CommandDefinition(name = "test", description = "more [options] file...")
class Test3 {

    @Option(shortName = 't', name = "target", description = "target directory")
    private String target;

    @Option(shortName = 'e', description = "test run")
    private String test;
}

