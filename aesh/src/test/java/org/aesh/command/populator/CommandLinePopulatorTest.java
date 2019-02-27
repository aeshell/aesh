/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command.populator;

import org.aesh.command.Command;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.parser.AeshCommandLineParser;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.parser.CommandLineParserBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.parser.OptionParserException;
import org.aesh.command.impl.populator.AeshCommandPopulator;
import org.aesh.command.parser.RequiredOptionException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.readline.AeshContext;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.impl.activator.AeshCommandActivatorProvider;
import org.aesh.command.impl.activator.AeshOptionActivatorProvider;
import org.aesh.command.impl.completer.AeshCompleterInvocationProvider;
import org.aesh.command.impl.converter.AeshConverterInvocationProvider;
import org.aesh.command.impl.validator.AeshValidatorInvocationProvider;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.parser.CommandLineParserException;
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
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLinePopulatorTest {

    private final InvocationProviders invocationProviders = new AeshInvocationProviders(
            SettingsBuilder.builder()
                    .converterInvocationProvider(new AeshConverterInvocationProvider())
                    .completerInvocationProvider(new AeshCompleterInvocationProvider())
                    .validatorInvocationProvider(new AeshValidatorInvocationProvider())
                    .optionActivatorProvider(new AeshOptionActivatorProvider())
                    .commandActivatorProvider(new AeshCommandActivatorProvider()).build());

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testSimpleObjects() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new TestPopulator1<>()).getParser();

        TestPopulator1 test1 = (TestPopulator1) parser.getCommand();
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        parser.parse("test --equal=foo");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("foo", test1.equal);

        try {
            parser.parse("test --equal eck --int1 ");
            parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
            fail("Should throw an OptionParserException");
        }
        catch (OptionParserException ope) {
            //ignored
        }

        parser.parse("test -e enable --X -f -i 2 -n=3");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        assertEquals("enable", test1.equal);
        assertTrue(test1.getEnableX());
        assertTrue(test1.foo);
        assertEquals(2, test1.getInt1().intValue());
        assertEquals(3, test1.int2);
        assertEquals("foo", test1.arguments.get(0));
        parser.parse("test -e enable2 --X");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertTrue(test1.getEnableX());
        assertFalse(test1.foo);
        assertEquals(42, test1.getInt1().intValue());
        parser.parse("test -e enable2 --X -i 5");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertTrue(test1.getEnableX());
        assertFalse(test1.foo);
        assertEquals(5, test1.getInt1().intValue());
        parser.parse("test -e enable2 -Xb");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertTrue(test1.getEnableX());
        assertTrue(test1.bar);
        assertFalse(test1.foo);
        assertEquals(42, test1.getInt1().intValue());
        parser.parse("test -e enable2 -X");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertTrue(test1.getEnableX());
        parser.parse("test -e enable2\\ ");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("enable2 ", test1.getEqual());
        assertFalse(test1.getEnableX());
        parser.parse("test -e b -bX");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertTrue(test1.getEnableX());
        assertTrue(test1.bar);
        assertEquals("b", test1.getEqual());

        parser.parse("test -e b --complex-value COMPLEX");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("b", test1.getEqual());
        assertEquals("COMPLEX", test1.getComplexValue());
    }

    @Test(expected = OptionParserException.class)
    public void testListObjects() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new TestPopulator2<>()).getParser();
        TestPopulator2 test2 = (TestPopulator2) parser.getCommand();
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        parser.parse("test -b s1,s2,s3,s4");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertNotNull(test2.getBasicSet());
        assertEquals(4, test2.getBasicSet().size());
        assertTrue(test2.getBasicSet().contains("s3"));
        parser.parse("test -b=s1,s2,s3,s4");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertNotNull(test2.getBasicSet());
        assertEquals(4, test2.getBasicSet().size());
        assertTrue(test2.getBasicSet().contains("s3"));
        parser.parse("test -b s1,s2,s3,s4");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertNotNull(test2.getBasicSet());
        assertEquals(4, test2.getBasicSet().size());
        assertTrue(test2.getBasicSet().contains("s3"));
        parser.parse("test -b=s1\\ s2\\ s3,s4");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertNotNull(test2.getBasicSet());
        assertEquals(2, test2.getBasicSet().size());
        assertTrue(test2.getBasicSet().contains("s4"));
        parser.parse("test -a 1,2,3,4");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertNull(test2.getBasicSet());
        assertNotNull(test2.getBasicList());
        assertEquals(4, test2.getBasicList().size());
        assertEquals(1, test2.getBasicList().get(0));
        parser.parse("test -a=1,2,3,4");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertNull(test2.getBasicSet());
        assertNotNull(test2.getBasicList());
        assertEquals(4, test2.getBasicList().size());
        assertEquals(1, test2.getBasicList().get(0));

        parser.parse("test -a 3,4 --basicSet foo,bar");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        assertNotNull(test2.getBasicList());
        assertNotNull(test2.getBasicSet());
        assertEquals(2, test2.getBasicList().size());
        assertEquals(2, test2.getBasicSet().size());
        assertTrue(test2.getBasicSet().contains("foo"));
        parser.parse("test -a 3,4 --basicSet=foo,bar");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        assertNotNull(test2.getBasicList());
        assertNotNull(test2.getBasicSet());
        assertEquals(2, test2.getBasicList().size());
        assertEquals(2, test2.getBasicSet().size());
        assertTrue(test2.getBasicSet().contains("foo"));
        parser.parse("test ");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertNull(test2.getBasicList());
        assertNull(test2.getBasicSet());
        parser.parse("test -i 10,12,0");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertNotNull(test2.getImplList());
        assertEquals(3, test2.getImplList().size());
        assertEquals(Short.valueOf("12"), test2.getImplList().get(1));

        //just to verify that we dont accept arguments
        parser.parse("test text.txt");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        exception.expect(OptionParserException.class);

    }

    @Test
    public void testListObjects2() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new TestPopulator5()).getParser();
        TestPopulator5 test5 = (TestPopulator5) parser.getCommand();
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        parser.parse("test --strings foo1 --bar ");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        assertEquals("foo1", test5.getStrings().get(0));
    }

    @Test(expected = OptionParserException.class)
    public void testGroupObjects() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new TestPopulator3<>()).getParser();
        TestPopulator3<CommandInvocation> test3 = (TestPopulator3<CommandInvocation>) parser.getCommand();
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        parser.parse("test -bX1=foo -bX2=bar");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        assertNotNull(test3.getBasicMap());
        assertNull(test3.getIntegerMap());
        assertEquals(2, test3.getBasicMap().size());
        assertTrue(test3.getBasicMap().containsKey("X2"));
        assertEquals("foo", test3.getBasicMap().get("X1"));
        parser.parse("test -iI12=43 -iI1=41 -iI1=42 -bAF=AB -bFA=BA");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertNotNull(test3.getIntegerMap());
        assertEquals(2, test3.getIntegerMap().size());
        assertEquals(new Integer("42"), test3.getIntegerMap().get("I1"));
        assertEquals("BA", test3.getBasicMap().get("FA"));
        assertEquals("AB", test3.getBasicMap().get("AF"));
        parser.parse("test -iI12");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        exception.expect(OptionParserException.class);
        parser.parse("test --integerMapI12=");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        exception.expect(OptionParserException.class);

    }

    @Test
    public void testArguments() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new TestPopulator4<>()).getParser();
        TestPopulator4 test4 = (TestPopulator4) parser.getCommand();
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        parser.parse("test test2.txt test4.txt");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        assertNotNull(test4.getArguments());
        assertEquals(2, test4.getArguments().size());
        assertTrue(test4.getArguments().toString(), test4.getArguments().contains(new File(aeshContext.getCurrentWorkingDirectory().getAbsolutePath(), "test2.txt")));
    }

    @Test(expected = OptionParserException.class)
    public void testStaticPopulator() throws Exception {
        TestPopulator3<CommandInvocation> test3 = new TestPopulator3<>();
        AeshCommandContainerBuilder.parseAndPopulate(test3, "test -bX1=foo -bX2=bar");

        assertNotNull(test3.getBasicMap());
        assertNull(test3.getIntegerMap());
        assertEquals(2, test3.getBasicMap().size());
        assertTrue(test3.getBasicMap().containsKey("X2"));
        assertEquals("foo", test3.getBasicMap().get("X1"));

        AeshCommandContainerBuilder.parseAndPopulate(test3, "test -iI1=42 -iI12=43");
        assertNotNull(test3.getIntegerMap());
        assertEquals(2, test3.getIntegerMap().size());
        assertEquals(new Integer("42"), test3.getIntegerMap().get("I1"));

        AeshCommandContainerBuilder.parseAndPopulate(test3, "test -iI12");
        exception.expect(OptionParserException.class);

        AeshCommandContainerBuilder.parseAndPopulate(test3, "test --integerMapI12=");
        exception.expect(OptionParserException.class);
    }

    @Test
    public void testSimpleObjectsBuilder() throws Exception {
        TestPopulator1A test1 = new TestPopulator1A();
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> commandBuilder = ProcessedCommandBuilder.builder()
                .name("test")
                .populator(new AeshCommandPopulator<>(test1))
                .description("a simple test");
        commandBuilder
                .addOption(ProcessedOptionBuilder.builder().name("XX").description("enable X").fieldName("enableX")
                        .type(Boolean.class).hasValue(false).build())
                .addOption(ProcessedOptionBuilder.builder().shortName('f').name("foo").description("enable foo").fieldName("foo")
                        .type(boolean.class).hasValue(false).build())
                .addOption(ProcessedOptionBuilder.builder().shortName('e').name("equal").description("enable equal").fieldName("equal")
                        .type(String.class).addDefaultValue("en").addDefaultValue("to").build())
                .addOption(ProcessedOptionBuilder.builder().shortName('i').name("int1").fieldName("int1").type(Integer.class).build())
                .addOption(ProcessedOptionBuilder.builder().shortName('n').fieldName("int2").type(int.class).addDefaultValue("12345").build());

        CommandLineParser<CommandInvocation> parser =  new AeshCommandLineParser<>( commandBuilder.create());

        //TestPopulator1A test1 = (TestPopulator1A) parser.getCommandPopulator().getObject();
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        parser.parse("test -e enable --XX -f -i 2 -n=3");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        assertEquals("enable", test1.equal);
        assertTrue(test1.getEnableX());
        assertTrue(test1.foo);
        assertEquals(2, test1.getInt1().intValue());
        assertEquals(3, test1.int2);
        parser.parse("test -e enable2");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertFalse(test1.getEnableX());
        assertFalse(test1.foo);
        parser.parse("test");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("en", test1.equal);
        assertEquals(12345, test1.int2);
    }

    @Test
    public void testCustomConverter() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new TestPopulator5()).getParser();
        TestPopulator5 test5 = (TestPopulator5) parser.getCommand();
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        parser.parse("test test2.txt test4.txt");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        assertNotNull(test5.getArguments());
        assertEquals(2, test5.getArguments().size());
        assertTrue(test5.getArguments().contains("test4.txt"));
        parser.parse("test --currency NOK");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertNull(test5.getArguments());
        assertEquals(Currency.getInstance("NOK"), test5.getCurrency());

    }

    @Test(expected = OptionValidatorException.class)
    public void testValidator() throws OptionValidatorException {
        try {
            CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new TestPopulator5()).getParser();
            TestPopulator5 test5 = (TestPopulator5) parser.getCommand();
            AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
            parser.parse("test -v 42");
            parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

            assertEquals(new Long(42), test5.getVeryLong());
            parser.parse("test --veryLong 101");
            parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        }
        catch (CommandLineParserException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testValidator2() {
        try {
            CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new TestPopulator5()).getParser();
            TestPopulator5 test5 = (TestPopulator5) parser.getCommand();
            AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
            parser.parse("test --longs 42;43;44 -v 42");
            parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
            assertEquals(3, test5.getLongs().size());
            assertEquals(new Long(42), test5.getLongs().get(0));
            assertEquals(new Long(44), test5.getLongs().get(2));
            assertEquals(new Long(42), test5.getVeryLong());

            parser.parse("test --longs 42 --veryLong 42");
            parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
            assertEquals(1, test5.getLongs().size());
            assertEquals(new Long(42), test5.getLongs().get(0));
            assertEquals(new Long(42), test5.getVeryLong());
            parser.parse("test --longs 42;43;132");
            parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
            exception.expect(OptionValidatorException.class);

        }
        catch (CommandLineParserException | OptionValidatorException ignored) {
        }
    }

    @Test
    public void testRequiredArguments() {
        try {
            CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new TestPopulator4<>()).getParser();
            parser.getCommand();
            AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
            parser.parse("test --veryLong 42");
            parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
            exception.expect(RequiredOptionException.class);
        }
        catch (CommandLineParserException | OptionValidatorException ignored) {
        }

    }

    @Test
    public void testSub() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new SubHelp<>()).getParser();

        SubHelp test1 = (SubHelp) parser.getCommand();
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        parser.parse("subhelp -e enable -h");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        assertEquals("enable", test1.equal);
        assertTrue("enable", test1.doHelp());
        parser.parse("subhelp -e enable");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("enable", test1.equal);
        assertFalse("enable", test1.doHelp());
    }

    @Test
    public void testMyOptionWithValue() throws Exception {
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> cmd = buildCommandLineOptions();
        parseArgLine(cmd, "mycmd --myoption value --abc 123");
        assert "123".equals(cmd.findLongOption("abc").getValue()) : "bad abc value";
        assert "value".equals(cmd.findLongOption("myoption").getValue()) : "bad myoption value";
    }

    @Test(expected = OptionParserException.class)
    public void testMyOptionWithoutValue() throws Exception {
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> cmd = buildCommandLineOptions();
        parseArgLine(cmd, "mycmd --myoption --abc 23");
        assert "23".equals(cmd.findLongOption("abc").getValue()) : "bad abc value";
        assert cmd.findLongOption("myoption").getValue() == null : "bad myoption value";
    }

    @Test
    public void testNoMyOption() throws Exception {
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> cmd = buildCommandLineOptions();
        parseArgLine(cmd, "mycmd --abc 123");
        //assert cl.hasOption("abc") : "Should have abc";
        //assert "123".equals(cl.getOptionValue("abc")) : "bad abc value";
        //assert !cl.hasOption("myoption") : "Should not have myoption";
        assert "123".equals(cmd.findLongOption("abc").getValue()) : "bad abc value";
        assert cmd.findLongOption("myoption").getValue() == null : "bad myoption value";
    }

    // specify --myoption at the end of the cmdline
    @Test
    public void testMyOptionAtEndWithValue() throws Exception {
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> cmd = buildCommandLineOptions();
        parseArgLine(cmd, "mycmd --abc 123 --myoption value");
        assert "123".equals(cmd.findLongOption("abc").getValue()) : "bad abc value";
        assert "value".equals(cmd.findLongOption("myoption").getValue()) : "bad myoption value";
    }

    // specify --myoption at the end of the cmdline
    @Test(expected = OptionParserException.class)
    public void testMyOptionAtEndWithoutValue() throws Exception {
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> cmd = buildCommandLineOptions();
        parseArgLine(cmd, "mycmd --abc 123 --myoption");
        assert "123".equals(cmd.findLongOption("abc").getValue()) : "bad abc value";
        assert cmd.findLongOption("myoption").getValue() == null : "bad myoption value";
    }

    private void parseArgLine(ProcessedCommand<Command<CommandInvocation>, CommandInvocation> options, String argLine) throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = CommandLineParserBuilder.builder().processedCommand(options).create();
        parser.populateObject(argLine, invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        if (options.parserExceptions().size() > 0) {
            throw options.parserExceptions().get(0);
        }
    }


    private ProcessedCommand<Command<CommandInvocation>, CommandInvocation> buildCommandLineOptions() throws Exception {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> cmd = ProcessedCommandBuilder.builder();

        cmd.name("mycmd");

        cmd.addOption(ProcessedOptionBuilder.builder()
                .name("abc")
                .optionType(OptionType.NORMAL)
                .type(String.class)
                .build());
        cmd.addOption(ProcessedOptionBuilder.builder()
                .name("myoption")
                .optionType(OptionType.NORMAL)
                .type(String.class)
                .addDefaultValue("")
                .build());

        return cmd.create();
    }

}
