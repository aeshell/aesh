/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.cl;

import junit.framework.TestCase;
import org.jboss.jreadline.cl.internal.OptionInt;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParserGeneratorTest extends TestCase {

    public ParserGeneratorTest(String name) {
        super(name);
    }

    public void testClassGenerator() {

        CommandLineParser parser = ParserGenerator.generateParser(Test1.class);

        assertEquals("a simple test", parser.getParameter().getUsage());
        OptionInt[] options = parser.getParameter().getOptions();
        assertEquals("f", options[0].getName());
        assertEquals("foo", options[0].getLongName());
        assertEquals("e", options[1].getName());
        assertEquals("enable e", options[1].getDescription());
        assertTrue(options[1].hasValue());
        assertTrue(options[1].isRequired());

        parser = ParserGenerator.generateParser(Test2.class);
        assertEquals("more [options] file...", parser.getParameter().getUsage());
        options = parser.getParameter().getOptions();
        assertEquals("d", options[0].getName());
        assertEquals("V", options[1].getName());

    }
}

@Parameter(usage = "a simple test",
        parser = ParserType.GNU,
        options = {
                @Option(name = "f", longName = "foo", description = "enable foo"),
                @Option(name = "e", description = "enable e", hasValue = true, required = true)
        })
class Test1 {}

@Parameter(usage = "more [options] file...",
        parser = ParserType.POSIX,
        options = {
                @Option(name = "d", description = "display help instead of ring bell"),
                @Option(name = "V", description = "output version information and exit")
        })
class Test2 {}

