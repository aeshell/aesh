/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.builder.CommandBuilder;
import org.jboss.aesh.cl.builder.OptionBuilder;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.exception.OptionParserException;
import org.jboss.aesh.cl.parser.AeshCommandLineParser;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.parser.ParserGenerator;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.AeshInvocationProviders;
import org.jboss.aesh.console.InvocationProviders;
import org.jboss.aesh.console.command.completer.AeshCompleterInvocationProvider;
import org.jboss.aesh.console.command.converter.AeshConverterInvocationProvider;
import org.jboss.aesh.console.command.validator.AeshValidatorInvocationProvider;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.util.Currency;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLinePopulatorTest {

    private InvocationProviders invocationProviders =
            new AeshInvocationProviders(
                    new AeshConverterInvocationProvider(),
                    new AeshCompleterInvocationProvider(),
                    new AeshValidatorInvocationProvider());

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testSimpleObjects() throws Exception {
        CommandLineParser parser = ParserGenerator.generateCommandLineParser(TestPopulator1.class);

        TestPopulator1 test1 = new TestPopulator1();
        AeshContext aeshContext = new SettingsBuilder().create().getAeshContext();

        parser.getCommandPopulator().populateObject(test1, parser.parse("test -e enable --X -f -i 2 -n=3"), invocationProviders, aeshContext, true);

        assertEquals("enable", test1.equal);
        assertTrue(test1.getEnableX());
        assertTrue(test1.foo);
        assertEquals(2, test1.getInt1().intValue());
        assertEquals(3, test1.int2);
        assertEquals("foo", test1.arguments.get(0));

        parser.getCommandPopulator().populateObject(test1, parser.parse("test -e enable2 --X"), invocationProviders, aeshContext, true);
        assertTrue(test1.getEnableX());
        assertFalse(test1.foo);
        assertEquals(42, test1.getInt1().intValue());

        parser.getCommandPopulator().populateObject(test1, parser.parse("test -e enable2 --X -i 5"), invocationProviders, aeshContext, true);
        assertTrue(test1.getEnableX());
        assertFalse(test1.foo);
        assertEquals(5, test1.getInt1().intValue());

        parser.getCommandPopulator().populateObject(test1, parser.parse("test -e enable2 -Xb"), invocationProviders, aeshContext, true);
        assertTrue(test1.getEnableX());
        assertTrue(test1.bar);
        assertFalse(test1.foo);
        assertEquals(42, test1.getInt1().intValue());

        parser.getCommandPopulator().populateObject(test1, parser.parse("test -e enable2 -X"), invocationProviders, aeshContext, true);
        assertTrue(test1.getEnableX());

        parser.getCommandPopulator().populateObject(test1, parser.parse("test -e enable2\\ "), invocationProviders, aeshContext, true);
        assertEquals("enable2 ", test1.getEqual());
        assertFalse(test1.getEnableX());

    }

    @Test(expected = OptionParserException.class)
    public void testListObjects() throws Exception {
        CommandLineParser parser = ParserGenerator.generateCommandLineParser(TestPopulator2.class);
        TestPopulator2 test2 = new TestPopulator2();
        AeshContext aeshContext = new SettingsBuilder().create().getAeshContext();

        parser.getCommandPopulator().populateObject(test2, parser.parse("test -b s1,s2,s3,s4"), invocationProviders, aeshContext, true);
        assertNotNull(test2.getBasicSet());
        assertEquals(4, test2.getBasicSet().size());
        assertTrue(test2.getBasicSet().contains("s3"));

        parser.getCommandPopulator().populateObject(test2, parser.parse("test -b=s1,s2,s3,s4"), invocationProviders, aeshContext, true);
        assertNotNull(test2.getBasicSet());
        assertEquals(4, test2.getBasicSet().size());
        assertTrue(test2.getBasicSet().contains("s3"));

        parser.getCommandPopulator().populateObject(test2, parser.parse("test -b s1,s2,s3,s4"), invocationProviders, aeshContext, true);
        assertNotNull(test2.getBasicSet());
        assertEquals(4, test2.getBasicSet().size());
        assertTrue(test2.getBasicSet().contains("s3"));

        parser.getCommandPopulator().populateObject(test2, parser.parse("test -b=s1\\ s2\\ s3,s4"), invocationProviders, aeshContext, true);
        assertNotNull(test2.getBasicSet());
        assertEquals(2, test2.getBasicSet().size());
        assertTrue(test2.getBasicSet().contains("s4"));

        parser.getCommandPopulator().populateObject(test2, parser.parse("test -a 1,2,3,4"), invocationProviders, aeshContext, true);
        assertNull(test2.getBasicSet());
        assertNotNull(test2.getBasicList());
        assertEquals(4, test2.getBasicList().size());
        assertEquals((Object) 1, test2.getBasicList().get(0));

        parser.getCommandPopulator().populateObject(test2, parser.parse("test -a=1,2,3,4"), invocationProviders, aeshContext, true);
        assertNull(test2.getBasicSet());
        assertNotNull(test2.getBasicList());
        assertEquals(4, test2.getBasicList().size());
        assertEquals((Object) 1, test2.getBasicList().get(0));


        parser.getCommandPopulator().populateObject(test2, parser.parse("test -a 3,4 --basicSet foo,bar"), invocationProviders, aeshContext, true);

        assertNotNull(test2.getBasicList());
        assertNotNull(test2.getBasicSet());
        assertEquals(2, test2.getBasicList().size());
        assertEquals(2, test2.getBasicSet().size());
        assertTrue(test2.getBasicSet().contains("foo"));

        parser.getCommandPopulator().populateObject(test2, parser.parse("test -a 3,4 --basicSet=foo,bar"), invocationProviders, aeshContext, true);

        assertNotNull(test2.getBasicList());
        assertNotNull(test2.getBasicSet());
        assertEquals(2, test2.getBasicList().size());
        assertEquals(2, test2.getBasicSet().size());
        assertTrue(test2.getBasicSet().contains("foo"));

        parser.getCommandPopulator().populateObject(test2, parser.parse("test "), invocationProviders, aeshContext, true);
        assertNull(test2.getBasicList());
        assertNull(test2.getBasicSet());

        parser.getCommandPopulator().populateObject(test2, parser.parse("test -i 10,12,0"), invocationProviders, aeshContext, true);
        assertNotNull(test2.getImplList());
        assertEquals(3, test2.getImplList().size());
        assertEquals(Short.valueOf("12"), test2.getImplList().get(1));

        //just to verify that we dont accept arguments
        parser.getCommandPopulator().populateObject(test2, parser.parse("test text.txt"), invocationProviders, aeshContext, true);
        exception.expect(OptionParserException.class);

    }

    @Test
    public void testListObjects2() {
        CommandLineParser parser;
        try {
            parser = ParserGenerator.generateCommandLineParser(TestPopulator5.class);
            TestPopulator5 test5 = new TestPopulator5();
            AeshContext aeshContext = new SettingsBuilder().create().getAeshContext();
            parser.getCommandPopulator().populateObject(test5, parser.parse("test --strings foo1 --bar "), invocationProviders, aeshContext, true);

            assertEquals("foo1", test5.getStrings().get(0));

        } catch (CommandLineParserException | OptionValidatorException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = OptionParserException.class)
    public void testGroupObjects() throws Exception {
        CommandLineParser parser = ParserGenerator.generateCommandLineParser(TestPopulator3.class);
        TestPopulator3 test3 = new TestPopulator3();
        AeshContext aeshContext = new SettingsBuilder().create().getAeshContext();

        parser.getCommandPopulator().populateObject(test3, parser.parse("test -bX1=foo -bX2=bar"), invocationProviders, aeshContext, true);

        assertNotNull(test3.getBasicMap());
        assertNull(test3.getIntegerMap());
        assertEquals(2, test3.getBasicMap().size());
        assertTrue(test3.getBasicMap().containsKey("X2"));
        assertEquals("foo", test3.getBasicMap().get("X1"));

        parser.getCommandPopulator().populateObject(test3, parser.parse("test -iI1=42 -iI12=43"), invocationProviders, aeshContext, true);
        assertNotNull(test3.getIntegerMap());
        assertEquals(2, test3.getIntegerMap().size());
        assertEquals(new Integer("42"), test3.getIntegerMap().get("I1"));

        parser.getCommandPopulator().populateObject(test3, parser.parse("test -iI12"), invocationProviders, aeshContext, true);
        exception.expect(OptionParserException.class);

        parser.getCommandPopulator().populateObject(test3, parser.parse("test --integerMapI12="), invocationProviders, aeshContext, true);
        exception.expect(OptionParserException.class);

    }

    @Test
    public void testArguments() throws Exception {
        CommandLineParser  parser = ParserGenerator.generateCommandLineParser(TestPopulator4.class);
        TestPopulator4 test4 = new TestPopulator4();
        AeshContext aeshContext = new SettingsBuilder().create().getAeshContext();

        parser.getCommandPopulator().populateObject(test4, parser.parse("test test2.txt test4.txt"), invocationProviders, aeshContext, true);

        assertNotNull(test4.getArguments());
        assertEquals(2, test4.getArguments().size());
        assertTrue(test4.getArguments().contains(new File("test2.txt")));
    }

    @Test(expected = OptionParserException.class)
    public void testStaticPopulator() throws Exception {
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

    @Test
    public void testSimpleObjectsBuilder() throws Exception {
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

        CommandLineParser parser =  new AeshCommandLineParser( commandBuilder.generateCommand());

        TestPopulator1A test1 = new TestPopulator1A();
        AeshContext aeshContext = new SettingsBuilder().create().getAeshContext();

        parser.getCommandPopulator().populateObject(test1, parser.parse("test -e enable --XX -f -i 2 -n=3"), invocationProviders, aeshContext, true);

        assertEquals("enable", test1.equal);
        assertTrue(test1.getEnableX());
        assertTrue(test1.foo);
        assertEquals(2, test1.getInt1().intValue());
        assertEquals(3, test1.int2);

        parser.getCommandPopulator().populateObject(test1, parser.parse("test -e enable2"), invocationProviders, aeshContext, true);
        assertFalse(test1.getEnableX());
        assertFalse(test1.foo);

        parser.getCommandPopulator().populateObject(test1, parser.parse("test"), invocationProviders, aeshContext, true);
        assertEquals("en", test1.equal);
        assertEquals(12345, test1.int2);
    }

    @Test
    public void testCustomConverter() throws Exception {
        CommandLineParser  parser = ParserGenerator.generateCommandLineParser(TestPopulator5.class);
        TestPopulator5 test5 = new TestPopulator5();
        AeshContext aeshContext = new SettingsBuilder().create().getAeshContext();

        parser.getCommandPopulator().populateObject(test5, parser.parse("test test2.txt test4.txt"), invocationProviders, aeshContext, true);

        assertNotNull(test5.getArguments());
        assertEquals(2, test5.getArguments().size());
        assertTrue(test5.getArguments().contains("test4.txt"));

        parser.getCommandPopulator().populateObject(test5, parser.parse("test --currency NOK"), invocationProviders, aeshContext, true);
        assertNull(test5.getArguments());
        assertEquals(Currency.getInstance("NOK"), test5.getCurrency());

    }

    @Test(expected = OptionValidatorException.class)
    public void testValidator() throws OptionValidatorException {
        try {
            CommandLineParser  parser = ParserGenerator.generateCommandLineParser(TestPopulator5.class);
            TestPopulator5 test5 = new TestPopulator5();
            AeshContext aeshContext = new SettingsBuilder().create().getAeshContext();

            parser.getCommandPopulator().populateObject(test5, parser.parse("test -v 42"), invocationProviders, aeshContext, true);

            assertEquals(new Long(42), test5.getVeryLong());

            parser.getCommandPopulator().populateObject(test5, parser.parse("test --veryLong 101"), invocationProviders, aeshContext, true);
        }
        catch (CommandLineParserException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testValidator2() {
        try {
            CommandLineParser  parser = ParserGenerator.generateCommandLineParser(TestPopulator5.class);
            TestPopulator5 test5 = new TestPopulator5();
            AeshContext aeshContext = new SettingsBuilder().create().getAeshContext();

            parser.getCommandPopulator().populateObject(test5, parser.parse("test --longs 42;43;44 -v 42"), invocationProviders, aeshContext, true);
            assertEquals(3, test5.getLongs().size());
            assertEquals(new Long(42), test5.getLongs().get(0));
            assertEquals(new Long(44), test5.getLongs().get(2));
            assertEquals(new Long(42), test5.getVeryLong());

            parser.getCommandPopulator().populateObject(test5, parser.parse("test --longs 42 --veryLong 42"), invocationProviders, aeshContext, true);
            assertEquals(1, test5.getLongs().size());
            assertEquals(new Long(42), test5.getLongs().get(0));
            assertEquals(new Long(42), test5.getVeryLong());

            parser.getCommandPopulator().populateObject(test5, parser.parse("test --longs 42;43;132"), invocationProviders, aeshContext, true);
            exception.expect(OptionValidatorException.class);

        }
        catch (CommandLineParserException | OptionValidatorException e) {
        }
    }
}
