/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import junit.framework.Assert;
import org.jboss.aesh.cl.builder.CommandBuilder;
import org.jboss.aesh.cl.builder.OptionBuilder;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.exception.OptionParserException;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.parser.ParserGenerator;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.util.Currency;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLinePopulatorTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

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

            parser.populateObject(test1, "test -e enable2 --X");
            assertTrue(test1.getEnableX());
            assertFalse(test1.foo);
            assertEquals(42, test1.getInt1().intValue());

            parser.populateObject(test1, "test -e enable2 --X -i 5");
            assertTrue(test1.getEnableX());
            assertFalse(test1.foo);
            assertEquals(5, test1.getInt1().intValue());

            parser.populateObject(test1, "test -e enable2 -Xb");
            assertTrue(test1.getEnableX());
            assertTrue(test1.bar);
            assertFalse(test1.foo);
            assertEquals(42, test1.getInt1().intValue());

            parser.populateObject(test1, "test -e enable2 -X");
            assertTrue(test1.getEnableX());

            parser.populateObject(test1, "test -e enable2\\ ");
            Assert.assertEquals("enable2 ", test1.getEqual());
            assertFalse(test1.getEnableX());

        }
        catch (CommandLineParserException e) {
            e.printStackTrace();
        } catch (OptionValidatorException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testListObjects() {
        try {
            CommandLineParser parser = ParserGenerator.generateCommandLineParser(TestPopulator2.class);
            TestPopulator2 test2 = new TestPopulator2();

            parser.populateObject(test2, "test -b s1,s2,s3,s4");
            assertNotNull(test2.getBasicSet());
            assertEquals(4, test2.getBasicSet().size());
            assertTrue(test2.getBasicSet().contains("s3"));

            parser.populateObject(test2, "test -b s1 s2 s3,s4");
            assertNotNull(test2.getBasicSet());
            assertEquals(4, test2.getBasicSet().size());
            assertTrue(test2.getBasicSet().contains("s3"));

            parser.populateObject(test2, "test -a 1,2,3,4");
            assertNull(test2.getBasicSet());
            assertNotNull(test2.getBasicList());
            assertEquals(4, test2.getBasicList().size());
            assertEquals((Object) 1, test2.getBasicList().get(0));


            parser.populateObject(test2, "test -a 3,4 --basicSet foo,bar");

            assertNotNull(test2.getBasicList());
            assertNotNull(test2.getBasicSet());
            assertEquals(2, test2.getBasicList().size());
            assertEquals(2, test2.getBasicSet().size());
            assertTrue(test2.getBasicSet().contains("foo"));

            parser.populateObject(test2, "test ");
            assertNull(test2.getBasicList());
            assertNull(test2.getBasicSet());

            parser.populateObject(test2, "test -i 10,12,0");
            assertNotNull(test2.getImplList());
            assertEquals(3, test2.getImplList().size());
            assertEquals(Short.valueOf("12"), test2.getImplList().get(1));

            //just to verify that we dont accept arguments
            parser.populateObject(test2, "test text.txt");
            exception.expect(OptionParserException.class);

        }
        catch (CommandLineParserException e) {
        } catch (OptionValidatorException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGroupObjects() {
        try {
            CommandLineParser parser = ParserGenerator.generateCommandLineParser(TestPopulator3.class);
            TestPopulator3 test3 = new TestPopulator3();
            parser.populateObject(test3, "test -bX1=foo -bX2=bar");

            assertNotNull(test3.getBasicMap());
            assertNull(test3.getIntegerMap());
            assertEquals(2, test3.getBasicMap().size());
            assertTrue(test3.getBasicMap().containsKey("X2"));
            assertEquals("foo", test3.getBasicMap().get("X1"));

            parser.populateObject(test3, "test -iI1=42 -iI12=43");
            assertNotNull(test3.getIntegerMap());
            assertEquals(2, test3.getIntegerMap().size());
            assertEquals(new Integer("42"), test3.getIntegerMap().get("I1"));

            parser.populateObject(test3, "test -iI12");
            exception.expect(OptionParserException.class);

            parser.populateObject(test3, "test --integerMapI12=");
            exception.expect(OptionParserException.class);
        }
        catch (CommandLineParserException e) {
        } catch (OptionValidatorException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testArguments() {
        try {
            CommandLineParser  parser = ParserGenerator.generateCommandLineParser(TestPopulator4.class);
            TestPopulator4 test4 = new TestPopulator4();
            parser.populateObject(test4, "test test2.txt test4.txt");

            assertNotNull(test4.getArguments());
            assertEquals(2, test4.getArguments().size());
            assertTrue(test4.getArguments().contains(new File("test2.txt")));
        }
        catch (CommandLineParserException e) {
        }
        catch (OptionValidatorException e) {
        }
    }

    @Test
    public void testStaticPopulator() {
        try {
            TestPopulator3 test3 = new TestPopulator3();
            ParserGenerator.parseAndPopulate(test3, "test -bX1=foo -bX2=bar");

            assertNotNull(test3.getBasicMap());
            assertNull(test3.getIntegerMap());
            assertEquals(2, test3.getBasicMap().size());
            assertTrue(test3.getBasicMap().containsKey("X2"));
            assertEquals("foo", test3.getBasicMap().get("X1"));

            ParserGenerator.parseAndPopulate(test3, "test -iI1=42 -iI12=43");
            assertNotNull(test3.getIntegerMap());
            assertEquals(2, test3.getIntegerMap().size());
            assertEquals(new Integer("42"), test3.getIntegerMap().get("I1"));

            ParserGenerator.parseAndPopulate(test3, "test -iI12");
            exception.expect(OptionParserException.class);

            ParserGenerator.parseAndPopulate(test3, "test --integerMapI12=");
            exception.expect(OptionParserException.class);
        }
        catch (CommandLineParserException e) {
        }
        catch (OptionValidatorException e) {
        }
    }

    @Test
    public void testSimpleObjectsBuilder() {
        try {
            CommandBuilder commandBuilder = new CommandBuilder().name("test").description("a simple test");
            commandBuilder
                    .addOption(new OptionBuilder().name("XX").description("enable X").fieldName("enableX")
                            .type(Boolean.class).hasValue(false).create())
                    .addOption(new OptionBuilder().shortName('f').name("foo").description("enable foo").fieldName("foo")
                            .type(boolean.class).hasValue(false).create())
                    .addOption(new OptionBuilder().shortName('e').name("equal").description("enable equal").fieldName("equal")
                            .type(String.class).addDefaultValue("en").addDefaultValue("to").create())
                    .addOption(new OptionBuilder().shortName('i').name("int1").fieldName("int1").type(Integer.class).create())
                    .addOption(new OptionBuilder().shortName('n').fieldName("int2").type(int.class).addDefaultValue("12345").create());

            CommandLineParser parser =  new CommandLineParser( commandBuilder.generateParameter());

            TestPopulator1A test1 = new TestPopulator1A();

            parser.populateObject(test1, "test -e enable --XX -f -i 2 -n=3");

            assertEquals("enable", test1.equal);
            assertTrue(test1.getEnableX());
            assertTrue(test1.foo);
            assertEquals(2, test1.getInt1().intValue());
            assertEquals(3, test1.int2);

            parser.populateObject(test1, "test -e enable2");
            assertFalse(test1.getEnableX());
            assertFalse(test1.foo);

            parser.populateObject(test1, "test");
            Assert.assertEquals("en", test1.equal);
            Assert.assertEquals(12345, test1.int2);
        }
        catch (CommandLineParserException e) {
            e.printStackTrace();
        } catch (OptionValidatorException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCustomConverter() {
        try {
            CommandLineParser  parser = ParserGenerator.generateCommandLineParser(TestPopulator5.class);
            TestPopulator5 test5 = new TestPopulator5();
            parser.populateObject(test5, "test test2.txt test4.txt");

            assertNotNull(test5.getArguments());
            assertEquals(2, test5.getArguments().size());
            assertTrue(test5.getArguments().contains("test4.txt"));

            parser.populateObject(test5, "test --currency NOK");
            assertNull(test5.getArguments());
            assertEquals(Currency.getInstance("NOK"), test5.getCurrency());

        }
        catch (CommandLineParserException e) {
        } catch (OptionValidatorException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testValidator() {
        try {
            CommandLineParser  parser = ParserGenerator.generateCommandLineParser(TestPopulator5.class);
            TestPopulator5 test5 = new TestPopulator5();
            parser.populateObject(test5, "test -v 42");

            assertEquals(new Long(42), test5.getVeryLong());

            parser.populateObject(test5, "test --veryLong 101");
            exception.expect(OptionValidatorException.class);
        }
        catch (CommandLineParserException e) {
        }
        catch (OptionValidatorException e) {
        }
    }


    @Test
    public void testValidator2() {
        try {
            CommandLineParser  parser = ParserGenerator.generateCommandLineParser(TestPopulator5.class);
            TestPopulator5 test5 = new TestPopulator5();

            parser.populateObject(test5, "test --longs 42;43;44 -v 42");
            assertEquals(3, test5.getLongs().size());
            assertEquals(new Long(42), test5.getLongs().get(0));
            assertEquals(new Long(44), test5.getLongs().get(2));
            assertEquals(new Long(42), test5.getVeryLong());

            parser.populateObject(test5, "test --longs 42 --veryLong 42");
            assertEquals(1, test5.getLongs().size());
            assertEquals(new Long(42), test5.getLongs().get(0));
            assertEquals(new Long(42), test5.getVeryLong());

            parser.populateObject(test5, "test --longs 42;43;132");
            exception.expect(OptionValidatorException.class);

        }
        catch (CommandLineParserException e) {
        }
        catch (OptionValidatorException e) {
        }
    }
}
