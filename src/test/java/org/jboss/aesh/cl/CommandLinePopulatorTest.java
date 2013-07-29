/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.junit.Test;

import java.util.Map;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLinePopulatorTest {


    @Test
    public void testSimpleObjects() {
        try {
            CommandLineParser parser = ParserGenerator.generateCommandLineParser(TestClass1.class);

            TestClass1 test1 = new TestClass1();

            parser.populateObject(test1, "test -e enable --X -f -i 2 -n=3");

            assertEquals("enable", test1.equal);
            assertTrue(test1.enableX);
            assertTrue(test1.foo);
            assertEquals(2, test1.int1.intValue());
            assertEquals(3, test1.int2);

            parser.populateObject(test1, "test -e enable2");
            assertNull(test1.enableX);
            assertFalse(test1.foo);
        }
        catch (CommandLineParserException e) {
            e.printStackTrace();
        }

    }

}

@Command(name = "test", description = "a simple test")
class TestClass1 {

    @Option(name = "X", description = "enable X")
    public Boolean enableX;

    @Option(shortName = 'f', name = "foo", description = "enable foo")
    public boolean foo;

    @Option(shortName = 'e', name = "equal", description = "enable equal", required = true)
    public String equal;

    @Option(shortName = 'i', name = "int1")
    public Integer int1;

    @Option(shortName = 'n')
    public int int2;

    @OptionGroup(shortName = 'D', description = "define properties")
    public Map<String,String> define;
}
