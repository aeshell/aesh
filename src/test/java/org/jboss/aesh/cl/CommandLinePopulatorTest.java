/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
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
            CommandLineParser parser = ParserGenerator.generateCommandLineParser(TestPopulator1.class);

            TestPopulator1 test1 = new TestPopulator1();

            parser.populateObject(test1, "test -e enable --X -f -i 2 -n=3");

            assertEquals("enable", test1.equal);
            assertTrue(test1.getEnableX());
            assertTrue(test1.foo);
            assertEquals(2, test1.getInt1().intValue());
            assertEquals(3, test1.int2);

            parser.populateObject(test1, "test -e enable2");
            assertNull(test1.getEnableX());
            assertFalse(test1.foo);
        }
        catch (CommandLineParserException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testListObjects() {
        CommandLineParser parser = null;
        try {
            parser = ParserGenerator.generateCommandLineParser(TestPopulator2.class);
            TestPopulator2 test2 = new TestPopulator2();

            parser.populateObject(test2, "test -b s1,s2,s3,s4");

            assertNotNull(test2.getBasicList());
            assertEquals(4, test2.getBasicList().size());
        }
        catch (CommandLineParserException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }

}
