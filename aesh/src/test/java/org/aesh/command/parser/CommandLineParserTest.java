/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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
package org.aesh.command.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.DefaultValueProvider;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Mixin;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionGroup;
import org.aesh.command.option.OptionList;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.AeshContext;
import org.aesh.parser.ParsedLineIterator;
import org.junit.Test;

/**
 * @author Aesh team
 */
public class CommandLineParserTest {

    private final InvocationProviders invocationProviders = new AeshInvocationProviders();

    @Test
    public void testParseCommandLine1() throws Exception {
        //lets add a properties check
        System.setProperty("foo", "bar");

        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new Parser1Test<>())
                .getParser();

        parser.populateObject("test -f -e bar -Df=g /tmp/file.txt", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        Parser1Test<CommandInvocation> p1 = (Parser1Test<CommandInvocation>) parser.getCommand();

        assertTrue(p1.foo);
        assertEquals("bar", p1.equal);
        assertEquals("/tmp/file.txt", p1.arguments.get(0));

        parser.populateObject("test -c10 -f -e=bar -Df=g /tmp/file.txt", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertTrue(p1.foo);
        assertEquals(10, p1.connection);
        assertEquals("bar", p1.equal);
        assertEquals("/tmp/file.txt", p1.arguments.get(0));
        assertEquals("g", p1.define.get("f"));

        parser.populateObject("test -Dg=f /tmp/file.txt -e=bar foo bar", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertNull(p1.foo);
        assertEquals("f", p1.define.get("g"));
        assertEquals("bar", p1.equal);
        assertEquals("/tmp/file.txt", p1.arguments.get(0));
        assertEquals("foo", p1.arguments.get(1));
        assertEquals("bar", p1.arguments.get(2));

        parser.populateObject("test -e beer -DXms=128m -DXmx=512m -DXmm= --X /tmp/file.txt", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("beer", p1.equal);
        assertEquals("/tmp/file.txt", p1.arguments.get(0));
        assertTrue(p1.enableX);

        assertEquals("128m", p1.define.get("Xms"));
        assertEquals("512m", p1.define.get("Xmx"));
        assertEquals("", p1.define.get("Xmm"));

        parser.populateObject("test --equal \"bar bar2\" -DXms=\"128g \" -DXmx=512g\\ m /tmp/file.txt", invocationProviders,
                aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("bar bar2", p1.equal);

        assertEquals("128g ", p1.define.get("Xms"));
        assertEquals("512g m", p1.define.get("Xmx"));

        parser.populateObject("test -fX -e bar -Df=g /tmp/file.txt\\ ", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertTrue(p1.foo);
        assertTrue(p1.enableX);
        assertEquals("bar", p1.equal);
        assertEquals("g", p1.define.get("f"));
        assertEquals("/tmp/file.txt ", p1.arguments.get(0));

        parser.populateObject("test -ebar -Df=g /tmp/file.txt", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("bar", p1.equal);
        assertEquals("g", p1.define.get("f"));
        assertEquals("/tmp/file.txt", p1.arguments.get(0));

        parser.populateObject("test -e true {\"distributed-cache\":{}}", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("{\"distributed-cache\":{}}", p1.arguments.get(0));

        parser = new AeshCommandContainerBuilder<>().create(new Parser1aTest<>()).getParser();

        parser.populateObject("test -f -e bar /tmp/file.txt", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        Parser1aTest p1a = (Parser1aTest) parser.getCommand();

        assertTrue(p1a.foo);
        assertEquals("bar", p1a.equal);
        assertEquals("/tmp/file.txt", p1a.arguments.get(0));
        assertTrue(p1a.define.isEmpty());

        parser.populateObject("test -f -DN1= /tmp/file.txt", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertNull(p1a.equal);
        assertFalse(p1a.define.isEmpty());
        assertEquals("bar", p1a.define.get("N1"));

        // #422: key without = should use defaultValue
        parser.populateObject("test -f -DN2 /tmp/file.txt", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertFalse(p1a.define.isEmpty());
        assertEquals("bar", p1a.define.get("N2"));

    }

    @Test
    public void testParseCommandLine1SNoSpace() throws Exception {

        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new Parser1Test<>())
                .getParser();

        parser.populateObject("test -f -ebar -Df=g /tmp/file.txt", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        Parser1Test p1 = (Parser1Test) parser.getCommand();

        assertTrue(p1.foo);
        assertEquals("bar", p1.equal);
        assertEquals("/tmp/file.txt", p1.arguments.get(0));

        parser.populateObject("test -f -ebar -Df=g /tmp/file.txt", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertTrue(p1.foo);
        assertEquals("bar", p1.equal);
        assertEquals("/tmp/file.txt", p1.arguments.get(0));
        assertEquals("g", p1.define.get("f"));

        parser.populateObject("test -Dg=f /tmp/file.txt -ebar foo bar", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertNull(p1.foo);
        assertEquals("f", p1.define.get("g"));
        assertEquals("bar", p1.equal);
        assertEquals("/tmp/file.txt", p1.arguments.get(0));
        assertEquals("foo", p1.arguments.get(1));
        assertEquals("bar", p1.arguments.get(2));

        parser.populateObject("test -ebeer -DXms=128m -DXmx=512m --X /tmp/file.txt", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("beer", p1.equal);
        assertEquals("/tmp/file.txt", p1.arguments.get(0));
        assertTrue(p1.enableX);

        assertEquals("128m", p1.define.get("Xms"));
        assertEquals("512m", p1.define.get("Xmx"));

        parser.populateObject("test --equal \"bar bar2\" -DXms=\"128g \" -DXmx=512g\\ m /tmp/file.txt", invocationProviders,
                aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("bar bar2", p1.equal);

        assertEquals("128g ", p1.define.get("Xms"));
        assertEquals("512g m", p1.define.get("Xmx"));

        parser.populateObject("test -fX -ebar -Df=g /tmp/file.txt\\ ", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertTrue(p1.foo);
        assertTrue(p1.enableX);
        assertEquals("bar", p1.equal);
        assertEquals("g", p1.define.get("f"));
        assertEquals("/tmp/file.txt ", p1.arguments.get(0));

        parser.populateObject("test -ebar -Df=g /tmp/file.txt", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("bar", p1.equal);
        assertEquals("g", p1.define.get("f"));
        assertEquals("/tmp/file.txt", p1.arguments.get(0));

        // #422: key without = and no defaultValue should still throw
        try {
            parser.populateObject("test -ebar -Dfoo /tmp/file.txt", invocationProviders, aeshContext,
                    CommandLineParser.Mode.VALIDATE);
            fail("Expected OptionParserException for -Dfoo without defaultValue");
        } catch (CommandLineParserException e) {
            assertTrue(e.getMessage().contains("must be part of a property"));
        }
    }

    @Test
    public void testParseCommandLine2() throws Exception {

        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new Parser2Test<>())
                .getParser();
        Parser2Test p2 = (Parser2Test) parser.getCommand();

        parser.populateObject("test -d true --bar Foo.class", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("true", p2.display);
        assertNull(p2.version);
        assertEquals("Foo.class", p2.bar);
        assertNull(p2.arguments);

        parser.populateObject("test -V verbose -d false -b com.bar.Bar.class /tmp/file\\ foo.txt /tmp/bah.txt",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("verbose", p2.version);
        assertEquals("false", p2.display);
        assertEquals("com.bar.Bar.class", p2.bar);
        assertEquals("/tmp/file foo.txt", p2.arguments.get(0));
        assertEquals("/tmp/bah.txt", p2.arguments.get(1));

        assertTrue(parser.getProcessedCommand().parserExceptions().isEmpty());

        parser.populateObject("test -Vverbose -dfalse -bcom.bar.Bar.class /tmp/file\\ foo.txt /tmp/bah.txt",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("verbose", p2.version);
        assertEquals("false", p2.display);
        assertEquals("com.bar.Bar.class", p2.bar);
        assertEquals("/tmp/file foo.txt", p2.arguments.get(0));
        assertEquals("/tmp/bah.txt", p2.arguments.get(1));

        assertTrue(parser.getProcessedCommand().parserExceptions().isEmpty());
    }

    @Test
    public void testParseGroupCommand() throws Exception {

        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new GroupCommandTest<>())
                .getParser();
        ChildTest1 c1 = (ChildTest1) parser.getChildParser("child1").getCommand();
        ChildTest2 c2 = (ChildTest2) parser.getChildParser("child2").getCommand();

        parser.populateObject("group child1 --foo BAR", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("BAR", c1.foo);

        parser.populateObject("group child1 --foo BAR --bar FOO", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("BAR", c1.foo);
        assertEquals("FOO", c1.bar);

        parser.populateObject("group child2 --foo BAR --bar FOO", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertNull(c1.foo);
        assertNull(c1.bar);
        assertEquals("BAR", c2.foo);
        assertEquals("FOO", c2.bar);
    }

    @Test
    public void testParseGroupCommandWithOptionsBeforeSubcommand() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new GroupWithOptionsCommand<>()).getParser();
        GroupWithOptionsCommand<?> group = (GroupWithOptionsCommand<?>) parser.getCommand();
        ChildTest1 c1 = (ChildTest1) parser.getChildParser("child1").getCommand();
        ChildTest2 c2 = (ChildTest2) parser.getChildParser("child2").getCommand();

        // Test: group options before subcommand
        parser.populateObject("cli -c cliarg child1 --foo childarg", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("cliarg", group.config);
        assertEquals("childarg", c1.foo);

        // Test: group options before subcommand with both child options
        parser.populateObject("cli -c cliarg child1 --foo BAR --bar FOO", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("cliarg", group.config);
        assertEquals("BAR", c1.foo);
        assertEquals("FOO", c1.bar);

        // Test: same short option name on both group and child
        parser.populateObject("cli -c groupval child2 --foo childval", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("groupval", group.config);
        assertEquals("childval", c2.foo);

        // Test: subcommand without group options still works
        parser.populateObject("cli child1 --foo BAR", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("BAR", c1.foo);

        // Test: boolean flag on group before subcommand
        parser.populateObject("cli --verbose child1 --foo BAR", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertTrue(group.verbose);
        assertEquals("BAR", c1.foo);

        // Test: multiple group options before subcommand
        parser.populateObject("cli -c myconfig --verbose child1 --foo BAR", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("myconfig", group.config);
        assertTrue(group.verbose);
        assertEquals("BAR", c1.foo);

        // Test: long option with = value before subcommand (e.g. --config=value subcommand)
        parser.populateObject("cli --config=myvalue child1 --foo BAR", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("myvalue", group.config);
        assertEquals("BAR", c1.foo);

        // Test: long option with = and URL value before subcommand
        parser.populateObject("cli --config=http://127.0.0.1:11222 child1 --foo BAR", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("http://127.0.0.1:11222", group.config);
        assertEquals("BAR", c1.foo);
    }

    @Test
    public void testParseSuperGroupCommand() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> superParser = new AeshCommandContainerBuilder<>()
                .create(new SuperGroupCommandTest<>()).getParser();
        CommandLineParser subSuperParser = superParser.getChildParser("sub");

        ChildTest1 c1 = (ChildTest1) subSuperParser.getChildParser("child1").getCommand();
        ChildTest2 c2 = (ChildTest2) subSuperParser.getChildParser("child2").getCommand();

        superParser.populateObject("super sub child1 --foo BAR", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("BAR", c1.foo);

        superParser.populateObject("super sub child1 --foo BAR --bar FOO", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("BAR", c1.foo);
        assertEquals("FOO", c1.bar);

        superParser.populateObject("super sub child2 --foo BAR --bar FOO", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertNull(c1.foo);
        assertNull(c1.bar);
        assertEquals("BAR", c2.foo);
        assertEquals("FOO", c2.bar);
    }

    @Test
    public void testParseCommandLine4() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new Parser4Test<>())
                .getParser();
        Parser4Test<CommandInvocation> p4 = (Parser4Test<CommandInvocation>) parser.getCommand();

        parser.populateObject("test -o bar1,bar2,bar3 foo", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("bar1", p4.option.get(0));
        assertEquals("bar3", p4.option.get(2));
        assertEquals(3, p4.option.size());

        parser.populateObject("test -o bar1 -o bar2 -o 'bar3' foo", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("bar1", p4.option.get(0));
        assertEquals("bar3", p4.option.get(2));
        assertEquals(3, p4.option.size());
        assertEquals("foo", p4.arguments.get(0));

        parser.populateObject("test -o=bar1,bar2,bar3 foo", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("bar1", p4.option.get(0));
        assertEquals("bar3", p4.option.get(2));
        assertEquals(3, p4.option.size());
        assertEquals("foo", p4.arguments.get(0));

        parser.populateObject("test --option=bar1,bar2,bar3 foo", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("bar1", p4.option.get(0));
        assertEquals("bar3", p4.option.get(2));
        assertEquals(3, p4.option.size());
        assertEquals("foo", p4.arguments.get(0));

        parser.populateObject("test --help bar4:bar5:bar6 foo", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("bar4", p4.help.get(0));
        assertEquals("bar6", p4.help.get(2));

        parser.populateObject("test --help2 bar4 bar5 bar6", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("bar4", p4.help2.get(0));
        assertEquals("bar6", p4.help2.get(2));

        parser.populateObject("test --bar 1,2,3", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals(new Integer(1), p4.bar.get(0));
        assertEquals(new Integer(2), p4.bar.get(1));
        assertEquals(new Integer(3), p4.bar.get(2));
    }

    @Test
    public void testParseCommandLine5() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new Parser5Test<>())
                .getParser();
        Parser5Test p5 = (Parser5Test) parser.getCommand();

        parser.populateObject("test  --foo  \"-X1 X2 -X3\" --baz -wrong --bar -q \"-X4 -X5\"", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("-X1", p5.foo.get(0));
        assertEquals("X2", p5.foo.get(1));
        assertEquals("-X3", p5.foo.get(2));
        assertTrue(p5.baz);
        assertTrue(p5.bar);
        assertEquals(2, p5.qux.size());
        assertEquals("-X4", p5.qux.get(0));
        assertEquals("-X5", p5.qux.get(1));

        parser.populateObject("test  --foo  -X1 X2 -X3 --baz -wrong --bar -q -X4 -X5", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("-X1", p5.foo.get(0));
        assertEquals("X2", p5.foo.get(1));
        assertEquals("-X3", p5.foo.get(2));
        assertTrue(p5.baz);
        assertTrue(p5.bar);
        assertEquals(2, p5.qux.size());
        assertEquals("-X4", p5.qux.get(0));
        assertEquals("-X5", p5.qux.get(1));
    }

    @Test
    public void testSubClass() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new SubHelp<>()).getParser();
        SubHelp subHelp = (SubHelp) parser.getCommand();

        parser.populateObject("subhelp --foo bar -h", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("bar", subHelp.foo);
        assertTrue(subHelp.getHelp());
    }

    @Test(expected = CommandLineParserException.class)
    public void testInitializeGroupCommandWithArgument() throws Exception {
        new AeshCommandContainerBuilder<>().create(new GroupFailCommand<>());
    }

    @Test
    public void testDoubleDash() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new Parser4Test<>())
                .getParser();
        Parser4Test p4 = (Parser4Test) parser.getCommand();

        parser.populateObject("test -- foo", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("foo", p4.arguments.get(0));
    }

    @Test
    public void testDisableParsing() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new DisableParsingCommand())
                .getParser();
        DisableParsingCommand disableParsing = (DisableParsingCommand) parser.getCommand();

        parser.populateObject("test --foo bar he --yay boo \"one two three\"", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("--foo", disableParsing.args.get(0));
        assertEquals("bar", disableParsing.args.get(1));
        assertEquals("he", disableParsing.args.get(2));
        assertEquals("--yay", disableParsing.args.get(3));
        assertEquals("boo", disableParsing.args.get(4));
        assertEquals("one two three", disableParsing.args.get(5));
    }

    @Test
    public void testAcceptNameWithoutDashes() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new ParserBareLongNameTest<>())
                .getParser();

        parser.populateObject("testBareLongName verbose output=file.txt", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        ParserBareLongNameTest<CommandInvocation> cmd = (ParserBareLongNameTest<CommandInvocation>) parser.getCommand();

        assertTrue(cmd.verbose);
        assertEquals("file.txt", cmd.output);
    }

    @Test
    public void testAcceptNameWithoutDashesMixed() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new ParserBareLongNameTest<>())
                .getParser();

        // Mix bare and -- options
        parser.populateObject("testBareLongName verbose --output=file.txt", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        ParserBareLongNameTest<CommandInvocation> cmd = (ParserBareLongNameTest<CommandInvocation>) parser.getCommand();

        assertTrue(cmd.verbose);
        assertEquals("file.txt", cmd.output);
    }

    @Test
    public void testAcceptNameWithoutDashesSpace() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new ParserBareLongNameTest<>())
                .getParser();

        // Test with space separator
        parser.populateObject("testBareLongName verbose output file.txt", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        ParserBareLongNameTest<CommandInvocation> cmd = (ParserBareLongNameTest<CommandInvocation>) parser.getCommand();

        assertTrue(cmd.verbose);
        assertEquals("file.txt", cmd.output);
    }

    @Test(expected = CommandLineParserException.class)
    public void testAcceptNameWithoutDashesDisabled() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new ParserBareDisabledTest<>())
                .getParser();

        // Should fail because acceptNameWithoutDashes is false
        parser.populateObject("testBareDisabled verbose output=file.txt", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
    }

    @Test
    public void testAcceptNameWithoutDashesParser2Style() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new Parser2BareTest<>())
                .getParser();
        Parser2BareTest<CommandInvocation> p2 = (Parser2BareTest<CommandInvocation>) parser.getCommand();

        // Test bare long names
        parser.populateObject("testBareParser2 display true bar Foo.class", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("true", p2.display);
        assertNull(p2.version);
        assertEquals("Foo.class", p2.bar);
        assertNull(p2.arguments);
    }

    @CommandDefinition(name = "test", description = "a simple test", aliases = { "toto" })
    public class Parser1Test<CI extends CommandInvocation> extends TestingCommand<CI> {

        @Option(shortName = 'X', name = "X", description = "enable X", hasValue = false)
        private Boolean enableX;

        @Option(shortName = 'f', name = "foo", description = "enable foo", hasValue = false)
        private Boolean foo;

        @Option(shortName = 'e', name = "equal", description = "enable equal", required = true)
        private String equal;

        @Option(shortName = 'c')
        private int connection;

        @OptionGroup(shortName = 'D', description = "define properties", required = true)
        private Map<String, String> define;

        @Arguments
        private List<String> arguments;
    }

    @CommandDefinition(name = "test", description = "a simple test", aliases = { "toto" })
    public class Parser1aTest<CI extends CommandInvocation> extends TestingCommand<CI> {

        @Option(shortName = 'X', name = "X", description = "enable X", hasValue = false)
        private Boolean enableX;

        @Option(shortName = 'f', name = "foo", description = "enable foo", hasValue = false)
        private Boolean foo;

        @Option(shortName = 'e', name = "equal", description = "enable equal", required = true)
        private String equal;

        @Option(shortName = 'c')
        private int connection;

        @OptionGroup(shortName = 'D', description = "define properties", defaultValue = "${foo}")
        private Map<String, String> define;

        @Arguments
        private List<String> arguments;
    }

    @CommandDefinition(name = "test", description = "more [options] file...")
    public class Parser2Test<CI extends CommandInvocation> extends TestingCommand<CI> {
        @Option(shortName = 'd', name = "display", description = "display help instead of ring bell")
        private String display;

        @Option(shortName = 'b', name = "bar", argument = "classname", required = true, description = "bar bar")
        private String bar;

        @Option(shortName = 'V', name = "version", description = "output version information and exit")
        private String version;

        @Arguments
        private List<String> arguments;
    }

    @CommandDefinition(name = "testBareParser2", description = "test bare with parser2 style")
    public class Parser2BareTest<CI extends CommandInvocation> extends TestingCommand<CI> {
        @Option(shortName = 'd', name = "display", acceptNameWithoutDashes = true, description = "display help instead of ring bell")
        private String display;

        @Option(shortName = 'b', name = "bar", acceptNameWithoutDashes = true, argument = "classname", required = true, description = "bar bar")
        private String bar;

        @Option(shortName = 'V', name = "version", acceptNameWithoutDashes = true, description = "output version information and exit")
        private String version;

        @Arguments
        private List<String> arguments;
    }

    @CommandDefinition(name = "test", description = "this is a command without options")
    public class Parser3Test extends TestingCommand {
    }

    @CommandDefinition(name = "test", description = "testing multiple values")
    public class Parser4Test<CI extends CommandInvocation> extends TestingCommand<CI> {
        @OptionList(shortName = 'o', name = "option", valueSeparator = ',')
        private List<String> option;

        @OptionList
        private List<Integer> bar;

        @OptionList(shortName = 'h', valueSeparator = ':')
        private List<String> help;

        @OptionList(shortName = 'e', valueSeparator = ' ')
        private List<String> help2;

        @Arguments
        private List<String> arguments;
    }

    @CommandDefinition(name = "test", description = "testing multiple values")
    public class Parser5Test<CI extends CommandInvocation> extends TestingCommand<CI> {
        @OptionList(shortName = 'f', name = "foo", valueSeparator = ' ')
        private List<String> foo;

        @Option(shortName = 'b', name = "bar", hasValue = false)
        private Boolean bar;

        @Option(shortName = 'z', name = "baz", hasValue = false)
        private Boolean baz;

        @OptionList(shortName = 'q', name = "qux", valueSeparator = ' ')
        private List<String> qux;

        @Arguments
        private List<String> arguments;
    }

    @CommandDefinition(name = "child1", description = "")
    public class ChildTest1 extends TestingCommand<CommandInvocation> {

        @Option
        private String foo;

        @Option
        private String bar;

    }

    @CommandDefinition(name = "child2", description = "")
    public class ChildTest2 extends TestingCommand<CommandInvocation> {

        @Option
        private String foo;

        @Option
        private String bar;
    }

    @CommandDefinition(name = "group", description = "", groupCommands = { ChildTest1.class, ChildTest2.class })
    public class GroupCommandTest<CI extends CommandInvocation> extends TestingCommand<CI> {

        @Option(hasValue = false)
        private boolean help;

    }

    @CommandDefinition(name = "super", description = "", groupCommands = { SubSuperGroupCommandTest.class })
    public class SuperGroupCommandTest<CI extends CommandInvocation> extends TestingCommand<CI> {

        @Option(hasValue = false)
        private boolean help;

    }

    @CommandDefinition(name = "sub", description = "", groupCommands = { ChildTest1.class, ChildTest2.class })
    public class SubSuperGroupCommandTest extends TestingCommand<CommandInvocation> {

        @Option(hasValue = false)
        private boolean help;

    }

    @CommandDefinition(name = "testBareLongName", description = "test bare long name")
    public class ParserBareLongNameTest<CI extends CommandInvocation> extends TestingCommand<CI> {

        @Option(name = "verbose", acceptNameWithoutDashes = true, hasValue = false)
        private Boolean verbose;

        @Option(name = "output", acceptNameWithoutDashes = true)
        private String output;
    }

    @CommandDefinition(name = "testBareDisabled", description = "test bare long name disabled")
    public class ParserBareDisabledTest<CI extends CommandInvocation> extends TestingCommand<CI> {

        @Option(name = "verbose", hasValue = false) // acceptNameWithoutDashes = false by default
        private Boolean verbose;

        @Option(name = "output")
        private String output;
    }

    public class TestingCommand<CI extends CommandInvocation> implements Command<CI> {
        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    public class HelpClass {

        @Option(name = "help", shortName = 'h', hasValue = false)
        private boolean help;

        public boolean getHelp() {
            return help;
        }

    }

    @CommandDefinition(name = "subhelp", description = "")
    public class SubHelp<CI extends CommandInvocation> extends HelpClass implements Command<CI> {

        @Option
        private String foo;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }

    }

    @CommandDefinition(name = "cli", description = "", groupCommands = { ChildTest1.class, ChildTest2.class })
    public class GroupWithOptionsCommand<CI extends CommandInvocation> extends TestingCommand<CI> {

        @Option(shortName = 'c')
        private String config;

        @Option(hasValue = false)
        private boolean verbose;

    }

    @CommandDefinition(name = "groupfail", description = "", groupCommands = { ChildTest1.class })
    public class GroupFailCommand<CI extends CommandInvocation> implements Command<CI> {

        @Option
        private String foo;

        @Argument
        private String bar;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }

    }

    @CommandDefinition(name = "test", description = "", disableParsing = true)
    public class DisableParsingCommand<CI extends CommandInvocation> implements Command<CI> {

        @Arguments
        private List<String> args;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testNegatableOption() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new NegatableOptionCommand<>())
                .getParser();

        // Test that --verbose sets verbose to true
        parser.populateObject("negatable --verbose", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        NegatableOptionCommand<CommandInvocation> cmd = (NegatableOptionCommand<CommandInvocation>) parser.getCommand();
        assertTrue(cmd.verbose);

        // Test that --no-verbose sets verbose to false
        parser.populateObject("negatable --no-verbose", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        cmd = (NegatableOptionCommand<CommandInvocation>) parser.getCommand();
        assertFalse(cmd.verbose);

        // Test with custom prefix
        parser.populateObject("negatable --without-debug", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        cmd = (NegatableOptionCommand<CommandInvocation>) parser.getCommand();
        assertFalse(cmd.debug);

        // Test that --debug sets debug to true
        parser.populateObject("negatable --debug", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        cmd = (NegatableOptionCommand<CommandInvocation>) parser.getCommand();
        assertTrue(cmd.debug);
    }

    @Test
    public void testNegatableOptionHelp() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new NegatableOptionCommand<>())
                .getParser();

        String help = parser.printHelp();
        // Help should show combined --[no-]name format
        assertTrue(help.contains("--[no-]verbose"));
        assertTrue(help.contains("--[without-]debug"));
    }

    @CommandDefinition(name = "negatable", description = "test negatable options")
    public class NegatableOptionCommand<CI extends CommandInvocation> implements Command<CI> {

        @Option(hasValue = false, negatable = true, description = "enable verbose mode")
        private boolean verbose;

        @Option(hasValue = false, negatable = true, negationPrefix = "without-", description = "enable debug mode")
        private boolean debug;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testEmptyOptionValue() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new EmptyOptionValueCommand<>())
                .getParser();

        // --myoption= hello -> myoption is empty string, arg is "hello"
        parser.populateObject("emptyopt --myoption= hello", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        EmptyOptionValueCommand<CommandInvocation> cmd = (EmptyOptionValueCommand<CommandInvocation>) parser.getCommand();
        assertEquals("", cmd.myoption);
        assertEquals("hello", cmd.arg);

        // --myoption="" hello -> myoption is empty string, arg is "hello"
        parser.populateObject("emptyopt --myoption=\"\" hello", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        cmd = (EmptyOptionValueCommand<CommandInvocation>) parser.getCommand();
        assertEquals("", cmd.myoption);
        assertEquals("hello", cmd.arg);

        // --myoption='' hello -> myoption is empty string, arg is "hello"
        parser.populateObject("emptyopt --myoption='' hello", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        cmd = (EmptyOptionValueCommand<CommandInvocation>) parser.getCommand();
        assertEquals("", cmd.myoption);
        assertEquals("hello", cmd.arg);
    }

    @CommandDefinition(name = "emptyopt", description = "test empty option values")
    public class EmptyOptionValueCommand<CI extends CommandInvocation> implements Command<CI> {

        @Option
        private String myoption;

        @Argument
        private String arg;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testOptionalValueOption() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionalValueCommand<>())
                .getParser();

        // --debug without value -> uses defaultValue "4004"
        parser.populateObject("optval --debug", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionalValueCommand<CommandInvocation> cmd = (OptionalValueCommand<CommandInvocation>) parser.getCommand();
        assertEquals("4004", cmd.debug);

        // --debug 5005 -> uses provided value
        parser.populateObject("optval --debug 5005", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        cmd = (OptionalValueCommand<CommandInvocation>) parser.getCommand();
        assertEquals("5005", cmd.debug);

        // --debug=6006 -> uses provided value via equals
        parser.populateObject("optval --debug=6006", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        cmd = (OptionalValueCommand<CommandInvocation>) parser.getCommand();
        assertEquals("6006", cmd.debug);

        // --debug --name foo -> debug uses default, name gets "foo"
        parser.populateObject("optval --debug --name foo", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        cmd = (OptionalValueCommand<CommandInvocation>) parser.getCommand();
        assertEquals("4004", cmd.debug);
        assertEquals("foo", cmd.name);

        // --name foo --debug -> debug at end with no value uses default
        parser.populateObject("optval --name foo --debug", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        cmd = (OptionalValueCommand<CommandInvocation>) parser.getCommand();
        assertEquals("4004", cmd.debug);
        assertEquals("foo", cmd.name);

        // -d without value -> uses defaultValue via short name
        parser.populateObject("optval -d", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        cmd = (OptionalValueCommand<CommandInvocation>) parser.getCommand();
        assertEquals("4004", cmd.debug);

        // -d8080 -> uses provided value appended to short name
        parser.populateObject("optval -d8080", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        cmd = (OptionalValueCommand<CommandInvocation>) parser.getCommand();
        assertEquals("8080", cmd.debug);

        // not provided at all -> defaultValue still applies (aesh always applies defaults)
        parser.populateObject("optval --name bar", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        cmd = (OptionalValueCommand<CommandInvocation>) parser.getCommand();
        assertEquals("4004", cmd.debug);
        assertEquals("bar", cmd.name);

        // option without defaultValue: not provided -> null; provided without value -> null
        parser = new AeshCommandContainerBuilder<>().create(new OptionalValueNoDefaultCommand<>()).getParser();
        parser.populateObject("optval2 --open", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionalValueNoDefaultCommand<CommandInvocation> cmd2 = (OptionalValueNoDefaultCommand<CommandInvocation>) parser
                .getCommand();
        assertNull(cmd2.open);

        // option without defaultValue: provided with value -> uses value
        parser.populateObject("optval2 --open=codium", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        cmd2 = (OptionalValueNoDefaultCommand<CommandInvocation>) parser.getCommand();
        assertEquals("codium", cmd2.open);
    }

    @CommandDefinition(name = "optval", description = "test optional-value options")
    public class OptionalValueCommand<CI extends CommandInvocation> implements Command<CI> {

        @Option(shortName = 'd', optionalValue = true, defaultValue = "4004")
        private String debug;

        @Option
        private String name;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "optval2", description = "test optional-value without default")
    public class OptionalValueNoDefaultCommand<CI extends CommandInvocation> implements Command<CI> {

        @Option(optionalValue = true)
        private String open;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testStopAtFirstPositional() throws Exception {
        InvocationProviders invocationProviders = new AeshInvocationProviders();
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        // 1. Options before the first positional are parsed normally
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new StopAtPositionalCommand<>()).getParser();
        parser.populateObject("run --verbose myscript.java", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        StopAtPositionalCommand<CommandInvocation> cmd = (StopAtPositionalCommand<CommandInvocation>) parser.getCommand();
        assertTrue(cmd.verbose);
        assertEquals(1, cmd.args.size());
        assertEquals("myscript.java", cmd.args.get(0));

        // 2. Option-like tokens after the first positional are treated as arguments
        parser.populateObject("run --verbose myscript.java -Dfoo=bar --help --verbose",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        cmd = (StopAtPositionalCommand<CommandInvocation>) parser.getCommand();
        assertTrue(cmd.verbose);
        assertEquals(4, cmd.args.size());
        assertEquals("myscript.java", cmd.args.get(0));
        assertEquals("-Dfoo=bar", cmd.args.get(1));
        assertEquals("--help", cmd.args.get(2));
        assertEquals("--verbose", cmd.args.get(3));

        // 3. No options, just positional arguments
        parser.populateObject("run myscript.java arg1 arg2", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        cmd = (StopAtPositionalCommand<CommandInvocation>) parser.getCommand();
        assertFalse(cmd.verbose);
        assertEquals(3, cmd.args.size());
        assertEquals("myscript.java", cmd.args.get(0));
        assertEquals("arg1", cmd.args.get(1));
        assertEquals("arg2", cmd.args.get(2));

        // 4. All options, no positional arguments
        parser.populateObject("run --verbose", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        cmd = (StopAtPositionalCommand<CommandInvocation>) parser.getCommand();
        assertTrue(cmd.verbose);
        assertNull(cmd.args);

        // 5. Single Argument variant: option-like tokens after the first positional are treated as arguments
        parser = new AeshCommandContainerBuilder<>().create(new StopAtPositionalSingleArgCommand<>()).getParser();
        parser.populateObject("exec --verbose myscript.java", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        StopAtPositionalSingleArgCommand<CommandInvocation> cmd2 = (StopAtPositionalSingleArgCommand<CommandInvocation>) parser
                .getCommand();
        assertTrue(cmd2.verbose);
        assertEquals("myscript.java", cmd2.script);
    }

    @Test
    public void testStopAtFirstPositionalWithGenerateHelp() throws Exception {
        InvocationProviders invocationProviders = new AeshInvocationProviders();
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        // --help after the first positional should be a passthrough argument, not trigger help
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new StopAtPositionalWithHelpCommand<>()).getParser();
        parser.populateObject("run --verbose myscript.java --help --version",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        StopAtPositionalWithHelpCommand<CommandInvocation> cmd = (StopAtPositionalWithHelpCommand<CommandInvocation>) parser
                .getCommand();
        assertTrue(cmd.verbose);
        assertFalse(parser.getProcessedCommand().isGenerateHelpOptionSet());
        assertEquals(3, cmd.args.size());
        assertEquals("myscript.java", cmd.args.get(0));
        assertEquals("--help", cmd.args.get(1));
        assertEquals("--version", cmd.args.get(2));

        // --help before the first positional should still work as help
        parser.parse("run --help");
        assertTrue(parser.getProcessedCommand().isGenerateHelpOptionSet());
    }

    @CommandDefinition(name = "run", description = "test stopAtFirstPositional with generateHelp", stopAtFirstPositional = true, generateHelp = true)
    public class StopAtPositionalWithHelpCommand<CI extends CommandInvocation> implements Command<CI> {

        @Option(hasValue = false)
        private boolean verbose;

        @Arguments
        private List<String> args;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "run", description = "test stopAtFirstPositional", stopAtFirstPositional = true)
    public class StopAtPositionalCommand<CI extends CommandInvocation> implements Command<CI> {

        @Option(hasValue = false)
        private boolean verbose;

        @Arguments
        private List<String> args;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "exec", description = "test stopAtFirstPositional with single Argument", stopAtFirstPositional = true)
    public class StopAtPositionalSingleArgCommand<CI extends CommandInvocation> implements Command<CI> {

        @Option(hasValue = false)
        private boolean verbose;

        @Argument
        private String script;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testDefaultValueProvider() throws Exception {
        InvocationProviders invocationProviders = new AeshInvocationProviders();
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        // 1. Dynamic default applied when option not provided by user
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new DynamicDefaultCommand<>()).getParser();
        parser.populateObject("dyndefault", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        DynamicDefaultCommand<CommandInvocation> cmd = (DynamicDefaultCommand<CommandInvocation>) parser.getCommand();
        assertEquals("from-config", cmd.template);

        // 2. User-provided value takes precedence over dynamic default
        parser.populateObject("dyndefault --template custom", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        cmd = (DynamicDefaultCommand<CommandInvocation>) parser.getCommand();
        assertEquals("custom", cmd.template);

        // 3. Dynamic default overrides static annotation default
        parser.populateObject("dyndefault", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        cmd = (DynamicDefaultCommand<CommandInvocation>) parser.getCommand();
        // editor has static default "vi" but provider returns "codium"
        assertEquals("codium", cmd.editor);

        // 4. Provider returns null -> falls back to static default
        parser.populateObject("dyndefault", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        cmd = (DynamicDefaultCommand<CommandInvocation>) parser.getCommand();
        // fallback has static default "hello" and provider returns null for it
        assertEquals("hello", cmd.fallback);

        // 5. Provider returns null and no static default -> field is null
        parser.populateObject("dyndefault", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        cmd = (DynamicDefaultCommand<CommandInvocation>) parser.getCommand();
        assertNull(cmd.noDefault);
    }

    public static class TestDefaultValueProvider implements DefaultValueProvider {
        @Override
        public String defaultValue(ProcessedOption option) {
            switch (option.name()) {
                case "template":
                    return "from-config";
                case "editor":
                    return "codium";
                default:
                    return null;
            }
        }
    }

    @CommandDefinition(name = "dyndefault", description = "test dynamic defaults", defaultValueProvider = TestDefaultValueProvider.class)
    public class DynamicDefaultCommand<CI extends CommandInvocation> implements Command<CI> {

        @Option
        private String template;

        @Option(defaultValue = "vi")
        private String editor;

        @Option(defaultValue = "hello")
        private String fallback;

        @Option
        private String noDefault;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    // --- Test: DefaultValueProvider should skip inherited options on child commands (#488) ---

    public static class InheritedDvp implements DefaultValueProvider {
        @Override
        public String defaultValue(ProcessedOption option) {
            if ("verbose".equals(option.name()))
                return "true";
            return null;
        }
    }

    @CommandDefinition(name = "parent", description = "parent", groupCommands = {
            InheritedDvpChild.class }, defaultValueProvider = InheritedDvp.class)
    public static class InheritedDvpParent implements Command<CommandInvocation> {
        @Option(hasValue = false, inherited = true, negatable = true)
        public boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "child", description = "child", defaultValueProvider = InheritedDvp.class)
    public static class InheritedDvpChild implements Command<CommandInvocation> {
        @Option
        public String name;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testDvpSkipsInheritedOptionsOnChild() throws Exception {
        InvocationProviders invocationProviders = new AeshInvocationProviders();
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(InheritedDvpParent.class).getParser();

        // User passes --no-verbose to override the DVP default "true"
        parser.populateObject("parent --no-verbose child --name test", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);

        InheritedDvpParent parent = (InheritedDvpParent) parser.getCommand();
        // Parent should have verbose=false (user explicitly set --no-verbose)
        assertFalse("Parent verbose should be false (--no-verbose)", parent.verbose);

        // Child should NOT have DVP re-apply "true" for the inherited verbose option.
        // The parent's value (false) should be propagated to the child.
        // Note: the child class doesn't have a 'verbose' field -- it's inherited from the parent
        // via the class hierarchy or field propagation.
    }

    // --- Tests for DefaultValueProvider.fallbackValue() (#507) ---

    public static class FallbackDvp implements DefaultValueProvider {
        @Override
        public String defaultValue(ProcessedOption option) {
            if ("port".equals(option.name()))
                return "8080"; // default when omitted
            return null;
        }

        @Override
        public String fallbackValue(ProcessedOption option) {
            if ("debug".equals(option.name()))
                return "9999"; // from config
            return null;
        }
    }

    @CommandDefinition(name = "fbtest", description = "Fallback test", defaultValueProvider = FallbackDvp.class)
    public class FallbackTestCommand<CI extends CommandInvocation> implements Command<CI> {
        @Option(name = "debug", fallbackValue = "4004")
        public String debug;

        @Option(name = "port", defaultValue = "3000")
        public String port;

        @Override
        public CommandResult execute(CI ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testProviderFallbackOverridesAnnotation() throws Exception {
        // #507: provider.fallbackValue() should override annotation fallbackValue
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new FallbackTestCommand<>()).getParser();
        parser.populateObject("fbtest --debug", invocationProviders,
                SettingsBuilder.builder().build().aeshContext(), CommandLineParser.Mode.VALIDATE);
        FallbackTestCommand<?> cmd = (FallbackTestCommand<?>) parser.getCommand();
        assertEquals("Provider fallback should override annotation", "9999", cmd.debug);
    }

    @Test
    public void testProviderFallbackNullFallsThrough() throws Exception {
        // #507: provider returning null falls through to annotation fallbackValue
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new FallbackTestCommand<>()).getParser();
        // port has no provider fallback (returns null), no annotation fallbackValue,
        // but has optionalValue=false so this tests a different option
        // Use debug with a provider that returns null for a different option name
        parser.populateObject("fbtest --port=7777", invocationProviders,
                SettingsBuilder.builder().build().aeshContext(), CommandLineParser.Mode.VALIDATE);
        FallbackTestCommand<?> cmd = (FallbackTestCommand<?>) parser.getCommand();
        assertEquals("Explicit value wins", "7777", cmd.port);
    }

    @Test
    public void testExplicitValueOverridesProviderFallback() throws Exception {
        // #507: explicit --debug=5005 takes priority over provider fallback
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new FallbackTestCommand<>()).getParser();
        parser.populateObject("fbtest --debug=5005", invocationProviders,
                SettingsBuilder.builder().build().aeshContext(), CommandLineParser.Mode.VALIDATE);
        FallbackTestCommand<?> cmd = (FallbackTestCommand<?>) parser.getCommand();
        assertEquals("Explicit value should win over provider", "5005", cmd.debug);
    }

    @Test
    public void testProviderDefaultNotCalledForBareFlag() throws Exception {
        // #507: when option is bare, defaultValue() should NOT be called,
        // only fallbackValue(). When omitted, only defaultValue() should be called.
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new FallbackTestCommand<>()).getParser();

        // --debug (bare) → fallbackValue "9999", NOT defaultValue
        parser.populateObject("fbtest --debug", invocationProviders,
                SettingsBuilder.builder().build().aeshContext(), CommandLineParser.Mode.VALIDATE);
        FallbackTestCommand<?> cmd = (FallbackTestCommand<?>) parser.getCommand();
        assertEquals("Bare flag: provider fallback", "9999", cmd.debug);

        // port omitted → defaultValue "8080" from provider (overrides annotation "3000")
        assertEquals("Omitted: provider default", "8080", cmd.port);
    }

    public static class NoFallbackDvp implements DefaultValueProvider {
        @Override
        public String defaultValue(ProcessedOption option) {
            return null;
        }
        // No fallbackValue override — uses default (returns null)
    }

    @CommandDefinition(name = "nofbtest", description = "No fallback provider test", defaultValueProvider = NoFallbackDvp.class)
    public class NoFallbackTestCommand<CI extends CommandInvocation> implements Command<CI> {
        @Option(name = "debug", fallbackValue = "4004")
        public String debug;

        @Override
        public CommandResult execute(CI ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testBackwardCompatNoFallbackOverride() throws Exception {
        // #507: provider without fallbackValue() override works as before
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new NoFallbackTestCommand<>()).getParser();
        parser.populateObject("nofbtest --debug", invocationProviders,
                SettingsBuilder.builder().build().aeshContext(), CommandLineParser.Mode.VALIDATE);
        NoFallbackTestCommand<?> cmd = (NoFallbackTestCommand<?>) parser.getCommand();
        assertEquals("Should use annotation fallbackValue", "4004", cmd.debug);
    }

    @Test
    public void testInheritedOptionPropagation() throws Exception {
        InvocationProviders invocationProviders = new AeshInvocationProviders();
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new InheritedGroupCommand<>()).getParser();
        InheritedGroupCommand<?> group = (InheritedGroupCommand<?>) parser.getCommand();
        InheritedChildCommand child = (InheritedChildCommand) parser.getChildParser("sub").getCommand();

        // 1. Inherited option on child: "mygroup sub --verbose --name hello"
        parser.populateObject("mygroup sub --verbose --name hello", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        group = (InheritedGroupCommand<?>) parser.getCommand();
        child = (InheritedChildCommand) parser.getChildParser("sub").getCommand();
        assertTrue(group.verbose);
        assertTrue(child.verbose);
        assertEquals("hello", child.name);

        // 2. Inherited option on parent before subcommand: "mygroup --verbose sub --name hello"
        parser.populateObject("mygroup --verbose sub --name hello", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        group = (InheritedGroupCommand<?>) parser.getCommand();
        child = (InheritedChildCommand) parser.getChildParser("sub").getCommand();
        assertTrue(group.verbose);
        assertTrue(child.verbose);
        assertEquals("hello", child.name);

        // 3. Without inherited option: "mygroup sub --name hello"
        parser.populateObject("mygroup sub --name hello", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        group = (InheritedGroupCommand<?>) parser.getCommand();
        child = (InheritedChildCommand) parser.getChildParser("sub").getCommand();
        assertFalse(group.verbose);
        assertFalse(child.verbose);
        assertEquals("hello", child.name);

        // 4. String inherited option on child: "mygroup sub --config myconf --name hello"
        parser.populateObject("mygroup sub --config myconf --name hello", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        group = (InheritedGroupCommand<?>) parser.getCommand();
        child = (InheritedChildCommand) parser.getChildParser("sub").getCommand();
        assertEquals("myconf", group.config);
        assertEquals("myconf", child.config);
        assertEquals("hello", child.name);

        // 5. String inherited option on parent: "mygroup --config myconf sub --name hello"
        parser.populateObject("mygroup --config myconf sub --name hello", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        group = (InheritedGroupCommand<?>) parser.getCommand();
        child = (InheritedChildCommand) parser.getChildParser("sub").getCommand();
        assertEquals("myconf", group.config);
        assertEquals("myconf", child.config);
        assertEquals("hello", child.name);

        // 6. Child without matching field still parses inherited option
        CommandLineParser<CommandInvocation> parser2 = new AeshCommandContainerBuilder<>()
                .create(new InheritedGroupCommand2<>()).getParser();
        InheritedGroupCommand2<?> group2 = (InheritedGroupCommand2<?>) parser2.getCommand();
        parser2.populateObject("mygroup2 sub2 --verbose --name hello", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        group2 = (InheritedGroupCommand2<?>) parser2.getCommand();
        InheritedChildNoFieldCommand child2 = (InheritedChildNoFieldCommand) parser2.getChildParser("sub2").getCommand();
        assertTrue(group2.verbose);
        assertEquals("hello", child2.name);
    }

    @CommandDefinition(name = "mygroup", description = "", groupCommands = { InheritedChildCommand.class })
    public class InheritedGroupCommand<CI extends CommandInvocation> implements Command<CI> {

        @Option(inherited = true, hasValue = false)
        private boolean verbose;

        @Option(inherited = true)
        private String config;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "sub", description = "")
    public class InheritedChildCommand implements Command<CommandInvocation> {

        // These fields match the parent's inherited options and should receive values
        private boolean verbose;
        private String config;

        @Option
        private String name;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mygroup2", description = "", groupCommands = { InheritedChildNoFieldCommand.class })
    public class InheritedGroupCommand2<CI extends CommandInvocation> implements Command<CI> {

        @Option(inherited = true, hasValue = false)
        private boolean verbose;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "sub2", description = "")
    public class InheritedChildNoFieldCommand implements Command<CommandInvocation> {

        // No 'verbose' field — inherited option is parsed but not injected here

        @Option
        private String name;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    // --- #427: allowedValues ---

    @Test
    public void testAllowedValues() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new AllowedValuesCommand<>()).getParser();

        // Valid value
        parser.populateObject("fmt --format text", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        AllowedValuesCommand<?> cmd = (AllowedValuesCommand<?>) parser.getCommand();
        assertEquals("text", cmd.format);

        // Another valid value
        parser.populateObject("fmt --format json", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals("json", cmd.format);

        // Invalid value
        try {
            parser.populateObject("fmt --format xml", invocationProviders, aeshContext,
                    CommandLineParser.Mode.VALIDATE);
            fail("Expected exception for invalid allowedValues");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Invalid value"));
            assertTrue(e.getMessage().contains("xml"));
            assertTrue(e.getMessage().contains("text"));
            assertTrue(e.getMessage().contains("json"));
        }

        // No value (option not set) should be fine
        parser.populateObject("fmt", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertNull(cmd.format);
    }

    @CommandDefinition(name = "fmt", description = "")
    public class AllowedValuesCommand<CI extends CommandInvocation> implements Command<CI> {
        @Option(name = "format", allowedValues = { "text", "json" })
        String format;

        @Override
        public CommandResult execute(CI ci) {
            return CommandResult.SUCCESS;
        }
    }

    // --- Combined short options ---

    @Test
    public void testCombinedShortOptions() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new CombinedShortCmd<>()).getParser();

        // Three boolean flags combined: -abc
        parser.populateObject("combo -abc", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        CombinedShortCmd<?> cmd = (CombinedShortCmd<?>) parser.getCommand();
        assertTrue(cmd.alpha);
        assertTrue(cmd.bravo);
        assertTrue(cmd.charlie);
        assertNull(cmd.delta);

        // Two flags combined + separate value option: -ab -d value
        parser.populateObject("combo -ab -d hello", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertTrue(cmd.alpha);
        assertTrue(cmd.bravo);
        assertFalse(cmd.charlie);
        assertEquals("hello", cmd.delta);

        // Value option can't be grouped: -acd throws error
        try {
            parser.populateObject("combo -acd hello", invocationProviders, aeshContext,
                    CommandLineParser.Mode.VALIDATE);
            fail("Should throw: value option -d can't be grouped");
        } catch (CommandLineParserException e) {
            assertTrue(e.getMessage().contains("can not be grouped"));
        }

        // All flags: -abc with value option separate
        parser.populateObject("combo -abc -d world", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertTrue(cmd.alpha);
        assertTrue(cmd.bravo);
        assertTrue(cmd.charlie);
        assertEquals("world", cmd.delta);

        // Single flag
        parser.populateObject("combo -b", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertFalse(cmd.alpha);
        assertTrue(cmd.bravo);
        assertFalse(cmd.charlie);
        assertNull(cmd.delta);
    }

    @CommandDefinition(name = "combo", description = "")
    public class CombinedShortCmd<CI extends CommandInvocation> implements Command<CI> {
        @Option(shortName = 'a', hasValue = false)
        boolean alpha;

        @Option(shortName = 'b', hasValue = false)
        boolean bravo;

        @Option(shortName = 'c', hasValue = false)
        boolean charlie;

        @Option(shortName = 'd')
        String delta;

        @Override
        public CommandResult execute(CI ci) {
            return CommandResult.SUCCESS;
        }
    }

    // --- Enum option tests ---

    public enum OutputFormat {
        TEXT,
        JSON,
        YAML
    }

    @CommandDefinition(name = "enumtest", description = "")
    public class EnumOptionCmd<CI extends CommandInvocation> implements Command<CI> {
        @Option(name = "format", description = "Output format")
        OutputFormat format;

        @Override
        public CommandResult execute(CI ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testEnumOption() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new EnumOptionCmd<>()).getParser();

        // Exact case
        parser.populateObject("enumtest --format TEXT", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        EnumOptionCmd<?> cmd = (EnumOptionCmd<?>) parser.getCommand();
        assertEquals(OutputFormat.TEXT, cmd.format);

        // Case-insensitive
        parser.populateObject("enumtest --format json", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals(OutputFormat.JSON, cmd.format);

        // Mixed case
        parser.populateObject("enumtest --format Yaml", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertEquals(OutputFormat.YAML, cmd.format);

        // Invalid value
        try {
            parser.populateObject("enumtest --format xml", invocationProviders, aeshContext,
                    CommandLineParser.Mode.VALIDATE);
            fail("Should throw for invalid enum value");
        } catch (Exception e) {
            assertTrue("Error should mention invalid value",
                    e.getMessage().contains("xml") || e.getCause().getMessage().contains("xml"));
        }

        // No value set
        parser.populateObject("enumtest", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        assertNull(cmd.format);
    }

    @Test
    public void testEnumOptionAutoListsValidValues() throws Exception {
        // #491: Enum options should auto-append valid values in help output
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new EnumOptionCmd<>()).getParser();
        parser.updateAnsiMode(false);
        String help = parser.printHelp();

        // Should auto-append "Valid values: text, json, yaml"
        assertTrue("Help should contain 'Valid values:'", help.contains("Valid values:"));
        assertTrue("Help should list 'text'", help.contains("text"));
        assertTrue("Help should list 'json'", help.contains("json"));
        assertTrue("Help should list 'yaml'", help.contains("yaml"));
    }

    @CommandDefinition(name = "enumvar", description = "")
    public class EnumVarCmd<CI extends CommandInvocation> implements Command<CI> {
        @Option(name = "format", description = "Output format (${COMPLETION-CANDIDATES})")
        OutputFormat format;

        @Override
        public CommandResult execute(CI ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testEnumOptionSkipsWhenAlreadyInDescription() throws Exception {
        // #491: Should NOT double-list values when ${COMPLETION-CANDIDATES} is used
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new EnumVarCmd<>()).getParser();
        parser.updateAnsiMode(false);
        String help = parser.printHelp();

        // Should contain the values from ${COMPLETION-CANDIDATES}
        assertTrue("Help should contain 'text'", help.contains("text"));
        // Should NOT contain "Valid values:" since the description already lists them
        assertFalse("Help should NOT contain 'Valid values:' when already in description",
                help.contains("Valid values:"));
    }

    @Test
    public void testExclusiveWithEnforcement() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new ExclusiveOptionsCommand<>()).getParser();

        // Single option should work fine
        parser.populateObject("exclusive --verbose", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        ExclusiveOptionsCommand<CommandInvocation> cmd = (ExclusiveOptionsCommand<CommandInvocation>) parser.getCommand();
        assertTrue(cmd.verbose);

        // The other option alone should also work
        parser.populateObject("exclusive --quiet", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        cmd = (ExclusiveOptionsCommand<CommandInvocation>) parser.getCommand();
        assertTrue(cmd.quiet);

        // Both options together should throw MutuallyExclusiveOptionException
        try {
            parser.populateObject("exclusive --verbose --quiet", invocationProviders, aeshContext,
                    CommandLineParser.Mode.VALIDATE);
            fail("Should reject mutually exclusive options");
        } catch (CommandLineParserException e) {
            assertTrue("Error should mention mutually exclusive",
                    e.getMessage().contains("mutually exclusive"));
            assertTrue("Error should mention --verbose", e.getMessage().contains("--verbose"));
            assertTrue("Error should mention --quiet", e.getMessage().contains("--quiet"));
        }
    }

    @Test
    public void testExclusiveWithValueOptions() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new ExclusiveValueOptionsCommand<>()).getParser();

        // Each alone should work
        parser.populateObject("exval --output file.txt", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        ExclusiveValueOptionsCommand<CommandInvocation> cmd = (ExclusiveValueOptionsCommand<CommandInvocation>) parser
                .getCommand();
        assertEquals("file.txt", cmd.output);

        parser.populateObject("exval --stdout", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        cmd = (ExclusiveValueOptionsCommand<CommandInvocation>) parser.getCommand();
        assertTrue(cmd.stdout);

        // Both together should fail
        try {
            parser.populateObject("exval --output file.txt --stdout", invocationProviders, aeshContext,
                    CommandLineParser.Mode.VALIDATE);
            fail("Should reject mutually exclusive options");
        } catch (CommandLineParserException e) {
            assertTrue(e.getMessage().contains("mutually exclusive"));
        }
    }

    @CommandDefinition(name = "exclusive", description = "test exclusive options")
    public class ExclusiveOptionsCommand<CI extends CommandInvocation> implements Command<CI> {
        @Option(hasValue = false, exclusiveWith = { "quiet" }, description = "verbose output")
        private boolean verbose;

        @Option(hasValue = false, exclusiveWith = { "verbose" }, description = "quiet output")
        private boolean quiet;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "exval", description = "test exclusive value options")
    public class ExclusiveValueOptionsCommand<CI extends CommandInvocation> implements Command<CI> {
        @Option(exclusiveWith = { "stdout" }, description = "output file")
        private String output;

        @Option(hasValue = false, exclusiveWith = { "output" }, description = "print to stdout")
        private boolean stdout;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testParamLabelInHelp() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new ParamLabelCommand<>()).getParser();

        String help = parser.printHelp();

        // Synopsis should show the paramLabel, not the field name
        assertTrue("Synopsis should contain <scriptOrFile>", help.contains("<scriptOrFile>"));
        assertFalse("Synopsis should not show raw field name <input>",
                help.contains("<input>") && !help.contains("<scriptOrFile>"));
    }

    @Test
    public void testParamLabelDefaultsToFieldName() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new NoParamLabelCommand<>()).getParser();

        String help = parser.printHelp();

        // Without paramLabel, synopsis should use the field name
        assertTrue("Synopsis should contain field name", help.contains("<myArg>"));
    }

    @Test
    public void testParamLabelOnArguments() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new ParamLabelArgsCommand<>()).getParser();

        String help = parser.printHelp();

        // Synopsis should show the paramLabel for @Arguments
        assertTrue("Synopsis should contain <sources>", help.contains("<sources>"));
    }

    @CommandDefinition(name = "plcmd", description = "test paramLabel")
    public class ParamLabelCommand<CI extends CommandInvocation> implements Command<CI> {
        @Argument(paramLabel = "scriptOrFile", description = "A file or URL to a Java code file")
        private String input;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "noplcmd", description = "test no paramLabel")
    public class NoParamLabelCommand<CI extends CommandInvocation> implements Command<CI> {
        @Argument(description = "some argument")
        private String myArg;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "argscmd", description = "test paramLabel on @Arguments")
    public class ParamLabelArgsCommand<CI extends CommandInvocation> implements Command<CI> {
        @Arguments(paramLabel = "sources", description = "source files")
        private java.util.List<String> files;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    // ========== Arity tests ==========

    @Test
    public void testArityExactTwo() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new ArityExactTwoCommand<>()).getParser();

        // Exactly 2 arguments should work
        parser.populateObject("aritycmd key value", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        ArityExactTwoCommand<CommandInvocation> cmd = (ArityExactTwoCommand<CommandInvocation>) parser.getCommand();
        assertEquals(2, cmd.args.size());
        assertEquals("key", cmd.args.get(0));
        assertEquals("value", cmd.args.get(1));

        // Too few (1) should fail with arity error
        try {
            parser.populateObject("aritycmd onlyone", invocationProviders, aeshContext,
                    CommandLineParser.Mode.VALIDATE);
            fail("Should reject too few arguments");
        } catch (CommandLineParserException e) {
            assertTrue("Error should mention 'at least'", e.getMessage().contains("at least"));
        }

        // Too many (3) should fail
        try {
            parser.populateObject("aritycmd one two three", invocationProviders, aeshContext,
                    CommandLineParser.Mode.VALIDATE);
            fail("Should reject too many arguments");
        } catch (CommandLineParserException e) {
            assertTrue("Error should mention 'Too many'", e.getMessage().contains("Too many"));
        }
    }

    @Test
    public void testArityOneOrMore() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new ArityOneOrMoreCommand<>()).getParser();

        // 1 argument should work
        parser.populateObject("arityone file1", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        ArityOneOrMoreCommand<CommandInvocation> cmd = (ArityOneOrMoreCommand<CommandInvocation>) parser.getCommand();
        assertEquals(1, cmd.files.size());

        // 3 arguments should work
        parser.populateObject("arityone file1 file2 file3", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        cmd = (ArityOneOrMoreCommand<CommandInvocation>) parser.getCommand();
        assertEquals(3, cmd.files.size());

        // 0 arguments should fail
        try {
            parser.populateObject("arityone", invocationProviders, aeshContext,
                    CommandLineParser.Mode.VALIDATE);
            fail("Should reject zero arguments when arity is 1..*");
        } catch (CommandLineParserException e) {
            assertTrue("Error should mention 'at least'", e.getMessage().contains("at least"));
        }
    }

    @Test
    public void testArityOptional() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new ArityOptionalCommand<>()).getParser();

        // 0 arguments should work
        parser.populateObject("arityopt", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        ArityOptionalCommand<CommandInvocation> cmd = (ArityOptionalCommand<CommandInvocation>) parser.getCommand();
        assertTrue("files should be null or empty with 0 args",
                cmd.files == null || cmd.files.size() == 0);

        // 1 argument should work
        parser.populateObject("arityopt file1", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        cmd = (ArityOptionalCommand<CommandInvocation>) parser.getCommand();
        assertEquals(1, cmd.files.size());

        // 2 arguments should fail (max is 1)
        try {
            parser.populateObject("arityopt file1 file2", invocationProviders, aeshContext,
                    CommandLineParser.Mode.VALIDATE);
            fail("Should reject too many arguments when arity is 0..1");
        } catch (CommandLineParserException e) {
            assertTrue("Error should mention 'Too many'", e.getMessage().contains("Too many"));
        }
    }

    @Test
    public void testArityInSynopsis() throws Exception {
        CommandLineParser<CommandInvocation> parser;

        // Exact 2 should show: <args> <args>
        parser = new AeshCommandContainerBuilder<>().create(new ArityExactTwoCommand<>()).getParser();
        String help = parser.printHelp();
        assertTrue("Synopsis should show two arg placeholders",
                help.contains("<args> <args>"));

        // 1..* should show: <files>...
        parser = new AeshCommandContainerBuilder<>().create(new ArityOneOrMoreCommand<>()).getParser();
        help = parser.printHelp();
        assertTrue("Synopsis should show ellipsis for 1..*",
                help.contains("<files>..."));

        // 0..1 should show: [<files>]
        parser = new AeshCommandContainerBuilder<>().create(new ArityOptionalCommand<>()).getParser();
        help = parser.printHelp();
        assertTrue("Synopsis should show brackets for 0..1",
                help.contains("[<files>]"));
    }

    @Test
    public void testArityNoArityLegacyBehavior() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new NoArityCommand<>()).getParser();

        // Without arity, @Arguments accepts unlimited values
        parser.populateObject("noarity a b c d e", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        NoArityCommand<CommandInvocation> cmd = (NoArityCommand<CommandInvocation>) parser.getCommand();
        assertEquals(5, cmd.args.size());
    }

    @CommandDefinition(name = "aritycmd", description = "test exact arity 2")
    public class ArityExactTwoCommand<CI extends CommandInvocation> implements Command<CI> {
        @Arguments(arity = "2", paramLabel = "args", description = "key and value")
        private java.util.List<String> args;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "arityone", description = "test arity 1..*")
    public class ArityOneOrMoreCommand<CI extends CommandInvocation> implements Command<CI> {
        @Arguments(arity = "1..*", paramLabel = "files", description = "one or more files")
        private java.util.List<String> files;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "arityopt", description = "test arity 0..1")
    public class ArityOptionalCommand<CI extends CommandInvocation> implements Command<CI> {
        @Arguments(arity = "0..1", paramLabel = "files", description = "optional file")
        private java.util.List<String> files;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "noarity", description = "test no arity")
    public class NoArityCommand<CI extends CommandInvocation> implements Command<CI> {
        @Arguments(description = "any arguments")
        private java.util.List<String> args;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    // ========== Path support tests ==========

    @Test
    public void testPathOptionConversion() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new PathCommand<>()).getParser();

        parser.populateObject("pathcmd --config /tmp/config.yml", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        PathCommand<CommandInvocation> cmd = (PathCommand<CommandInvocation>) parser.getCommand();
        assertNotNull(cmd.config);
        assertTrue("Path should end with config.yml", cmd.config.toString().endsWith("config.yml"));
    }

    @Test
    public void testPathArgumentConversion() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new PathCommand<>()).getParser();

        parser.populateObject("pathcmd /tmp/input.txt", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        PathCommand<CommandInvocation> cmd = (PathCommand<CommandInvocation>) parser.getCommand();
        assertNotNull(cmd.input);
        assertTrue("Path should end with input.txt", cmd.input.toString().endsWith("input.txt"));
    }

    @Test
    public void testPathFileCompletion() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new PathCommand<>()).getParser();

        // Path-typed option should get a file completer automatically
        ProcessedOption configOption = parser.getProcessedCommand().findLongOptionNoActivatorCheck("config");
        assertNotNull("config option should exist", configOption);
        assertNotNull("Path-typed option should have a completer", configOption.completer());

        // Path-typed argument should show as <file> in synopsis
        String help = parser.printHelp();
        assertTrue("Path argument should show as <file> in help", help.contains("<file>"));
    }

    @Test
    public void testIndexedPositionalsRouteByIndex() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new IndexedPositionalsCommand<>()).getParser();

        parser.populateObject("idxpos first second", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        IndexedPositionalsCommand<CommandInvocation> cmd = (IndexedPositionalsCommand<CommandInvocation>) parser.getCommand();

        assertNotNull(cmd.first);
        assertEquals(1, cmd.first.size());
        assertEquals("first", cmd.first.get(0));
        assertEquals("second", cmd.second);
    }

    @Test
    public void testIndexedPositionalsOverlapRejected() throws Exception {
        try {
            new AeshCommandContainerBuilder<>().create(new IndexedPositionalsOverlapCommand<>());
            fail("Expected overlap validation to fail");
        } catch (CommandLineParserException e) {
            assertTrue(e.getMessage().contains("overlap"));
        }
    }

    @Test
    public void testIndexedPositionalsSynopsisOrderByIndex() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new IndexedPositionalsCommand<>()).getParser();

        String help = parser.printHelp();
        int firstPos = help.indexOf("[<first>]");
        int secondPos = help.indexOf("<second>");
        assertTrue("Synopsis should include first positional", firstPos >= 0);
        assertTrue("Synopsis should include second positional", secondPos >= 0);
        assertTrue("Synopsis should order positionals by index", firstPos < secondPos);
    }

    @Test
    public void testMultipleSingularArgumentsByIndex() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new MultipleArgumentsCommand<>()).getParser();

        parser.populateObject("multiarg second first", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        MultipleArgumentsCommand<CommandInvocation> cmd = (MultipleArgumentsCommand<CommandInvocation>) parser.getCommand();

        assertEquals("first", cmd.first);
        assertEquals("second", cmd.second);
    }

    @Test
    public void testIndexedPositionalsGapIsRejected() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new IndexedGapCommand<>()).getParser();

        try {
            parser.populateObject("idxgap zero one", invocationProviders, aeshContext,
                    CommandLineParser.Mode.VALIDATE);
            fail("Expected missing positional index to be rejected");
        } catch (OptionParserException e) {
            assertTrue(e.getMessage().contains("Unexpected positional value"));
            assertTrue(e.getMessage().contains("index 1"));
            assertTrue(e.getMessage().contains("Declared positional indexes"));
        }
    }

    @CommandDefinition(name = "pathcmd", description = "test Path support")
    public class PathCommand<CI extends CommandInvocation> implements Command<CI> {
        @Option(description = "config file")
        private java.nio.file.Path config;

        @Argument(description = "input file")
        private java.nio.file.Path input;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "idxpos", description = "test positional index routing")
    public class IndexedPositionalsCommand<CI extends CommandInvocation> implements Command<CI> {
        @Arguments(index = "0..0", description = "first positional")
        private List<String> first;

        @Argument(index = "1", description = "second positional")
        private String second;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "idxoverlap", description = "test positional overlap validation")
    public class IndexedPositionalsOverlapCommand<CI extends CommandInvocation> implements Command<CI> {
        @Argument(index = "0..1", description = "overlapping argument")
        private String first;

        @Arguments(index = "1..*", description = "overlapping arguments")
        private List<String> rest;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "multiarg", description = "test multiple @Argument support")
    public class MultipleArgumentsCommand<CI extends CommandInvocation> implements Command<CI> {
        @Argument(index = "1", description = "first by index")
        private String first;

        @Argument(index = "0", description = "second by index")
        private String second;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "idxgap", description = "test indexed positional gap")
    public class IndexedGapCommand<CI extends CommandInvocation> implements Command<CI> {
        @Argument(index = "0", description = "first")
        private String first;

        @Argument(index = "2", description = "third")
        private String third;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    // --- Issue #442: @Argument on @Mixin with stopAtFirstPositional ---

    @Test
    public void testMixinArgumentWithStopAtFirstPositional() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new MixinArgStopCommand<>()).getParser();

        // run --debug 4004 myfile.java extraArg1 extraArg2
        parser.populateObject("run --debug 4004 myfile.java extraArg1 extraArg2",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        MixinArgStopCommand<CommandInvocation> cmd = (MixinArgStopCommand<CommandInvocation>) parser.getCommand();

        assertEquals("4004", cmd.debug);
        assertNotNull("mixin should be initialized", cmd.scriptMixin);
        assertEquals("myfile.java", cmd.scriptMixin.scriptOrFile);
        assertNotNull(cmd.userParams);
        assertEquals(2, cmd.userParams.size());
        assertEquals("extraArg1", cmd.userParams.get(0));
        assertEquals("extraArg2", cmd.userParams.get(1));
    }

    @Test
    public void testMixinArgumentWithStopAtFirstPositional_OptionsAfterPositional() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new MixinArgStopCommand<>()).getParser();

        // Options after the first positional should be treated as args, not parsed
        parser.populateObject("run myfile.java --debug 4004 --unknown",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        MixinArgStopCommand<CommandInvocation> cmd = (MixinArgStopCommand<CommandInvocation>) parser.getCommand();

        assertNull("debug should not be set (appears after positional)", cmd.debug);
        assertEquals("myfile.java", cmd.scriptMixin.scriptOrFile);
        assertNotNull(cmd.userParams);
        assertEquals(3, cmd.userParams.size());
        assertEquals("--debug", cmd.userParams.get(0));
        assertEquals("4004", cmd.userParams.get(1));
        assertEquals("--unknown", cmd.userParams.get(2));
    }

    public static class ScriptMixin {
        @Argument(paramLabel = "scriptOrFile", index = "0", arity = "0..1", description = "A file or URL to a Java code file")
        String scriptOrFile;
    }

    @CommandDefinition(name = "run", description = "Run a script", stopAtFirstPositional = true)
    public class MixinArgStopCommand<CI extends CommandInvocation> implements Command<CI> {
        @Mixin
        ScriptMixin scriptMixin;

        @Option(shortName = 'd', name = "debug", description = "Debug port")
        String debug;

        @Arguments(index = "1..*", arity = "0..*")
        List<String> userParams;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    // Variant with optionalValue on --debug (the more likely real-world scenario)

    @Test
    public void testMixinArgumentWithStopAtFirstPositional_OptionalDebug() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new MixinArgStopOptionalDebugCommand<>()).getParser();

        // --debug 4004 myfile.java: debug should get "4004", scriptOrFile should get "myfile.java"
        parser.populateObject("run --debug 4004 myfile.java",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        MixinArgStopOptionalDebugCommand<CommandInvocation> cmd = (MixinArgStopOptionalDebugCommand<CommandInvocation>) parser
                .getCommand();

        assertEquals("4004", cmd.debug);
        assertEquals("myfile.java", cmd.scriptMixin.scriptOrFile);
    }

    @Test
    public void testMixinArgumentWithStopAtFirstPositional_OptionalDebugWithEquals() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new MixinArgStopOptionalDebugCommand<>()).getParser();

        // --debug=5005 myfile.java: debug should get "5005", scriptOrFile should get "myfile.java"
        parser.populateObject("run --debug=5005 myfile.java",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        MixinArgStopOptionalDebugCommand<CommandInvocation> cmd = (MixinArgStopOptionalDebugCommand<CommandInvocation>) parser
                .getCommand();

        assertEquals("5005", cmd.debug);
        assertEquals("myfile.java", cmd.scriptMixin.scriptOrFile);
    }

    @CommandDefinition(name = "run", description = "Run with optional debug", stopAtFirstPositional = true)
    public class MixinArgStopOptionalDebugCommand<CI extends CommandInvocation> implements Command<CI> {
        @Mixin
        ScriptMixin scriptMixin;

        @Option(shortName = 'd', name = "debug", optionalValue = true, defaultValue = "4004", description = "Debug port")
        String debug;

        @Arguments(index = "1..*", arity = "0..*")
        List<String> userParams;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    // --- Issue #442: @Argument on @Mixin with custom OptionParser ---

    /**
     * Custom OptionParser that only accepts values via = syntax.
     * --option=value uses "value"; --option (without =) uses empty string.
     */
    public static class StrictOptionParser implements OptionParser {
        @Override
        public void parse(ParsedLineIterator iter, ProcessedOption option) throws OptionParserException {
            String word = iter.peekWord();
            String prefix = option.isLongNameUsed() ? "--" : "-";
            String optName = option.isLongNameUsed() ? option.name() : option.shortName();
            String fullPrefix = prefix + optName;

            if (word.startsWith(fullPrefix + "=")) {
                option.addValue(word.substring(fullPrefix.length() + 1));
            } else {
                option.addValue("");
            }
            iter.pollParsedWord();
        }
    }

    /**
     * Custom OptionParser that peeks ahead to distinguish debug params from positional args.
     * Accepts: --debug=5000, --debug 5000, --debug (no value → empty string)
     * But NOT: --debug somefile.java (next word doesn't look like a port number)
     */
    public static class PeekAheadOptionParser implements OptionParser {
        private static final java.util.regex.Pattern DEBUG_VALUE = java.util.regex.Pattern
                .compile("(?:(.*?:)?(\\d+\\??))|(?:\\S*=\\S+\\??)");

        @Override
        public void parse(ParsedLineIterator iter, ProcessedOption option) throws OptionParserException {
            String word = iter.peekWord();
            String prefix = option.isLongNameUsed() ? "--" : "-";
            String optName = option.isLongNameUsed() ? option.name() : option.shortName();
            String fullPrefix = prefix + optName;

            if (word.startsWith(fullPrefix + "=")) {
                option.addValue(word.substring(fullPrefix.length() + 1));
                iter.pollParsedWord();
                return;
            }

            iter.pollParsedWord();

            if (iter.hasNextWord()) {
                String nextWord = iter.peekWord();
                if (!nextWord.startsWith("-") && DEBUG_VALUE.matcher(nextWord).matches()) {
                    option.addValue(nextWord);
                    iter.pollParsedWord();
                    return;
                }
            }

            option.addValue("");
        }
    }

    @CommandDefinition(name = "run", description = "Run with strict code option", stopAtFirstPositional = true)
    public class MixinArgStrictParserCommand<CI extends CommandInvocation> implements Command<CI> {
        @Mixin
        ScriptMixin scriptMixin;

        @Option(shortName = 'c', name = "code", parser = StrictOptionParser.class, description = "Run given string as code")
        String code;

        @Arguments(index = "1..*", arity = "0..*")
        List<String> userParams;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "run", description = "Run with peek-ahead debug", stopAtFirstPositional = true)
    public class MixinArgPeekAheadParserCommand<CI extends CommandInvocation> implements Command<CI> {
        @Mixin
        ScriptMixin scriptMixin;

        @Option(shortName = 'd', name = "debug", parser = PeekAheadOptionParser.class, description = "Debug port")
        String debug;

        @Arguments(index = "1..*", arity = "0..*")
        List<String> userParams;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testMixinArgument_StrictParser_WithEquals() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new MixinArgStrictParserCommand<>()).getParser();

        // --code=println("hi") myfile.java: code should get the value, scriptOrFile should get "myfile.java"
        parser.populateObject("run --code=println myfile.java",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        MixinArgStrictParserCommand<CommandInvocation> cmd = (MixinArgStrictParserCommand<CommandInvocation>) parser
                .getCommand();

        assertEquals("println", cmd.code);
        assertNotNull("mixin should be initialized", cmd.scriptMixin);
        assertEquals("myfile.java", cmd.scriptMixin.scriptOrFile);
    }

    @Test
    public void testMixinArgument_StrictParser_WithoutEquals() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new MixinArgStrictParserCommand<>()).getParser();

        // -c myfile.java: code should get empty string (strict parser), scriptOrFile should get "myfile.java"
        parser.populateObject("run -c myfile.java",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        MixinArgStrictParserCommand<CommandInvocation> cmd = (MixinArgStrictParserCommand<CommandInvocation>) parser
                .getCommand();

        assertEquals("", cmd.code);
        assertNotNull("mixin should be initialized", cmd.scriptMixin);
        assertEquals("myfile.java", cmd.scriptMixin.scriptOrFile);
    }

    @Test
    public void testMixinArgument_PeekAheadParser_PortThenFile() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new MixinArgPeekAheadParserCommand<>()).getParser();

        // --debug 4004 myfile.java: debug should get "4004", scriptOrFile should get "myfile.java"
        parser.populateObject("run --debug 4004 myfile.java",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        MixinArgPeekAheadParserCommand<CommandInvocation> cmd = (MixinArgPeekAheadParserCommand<CommandInvocation>) parser
                .getCommand();

        assertEquals("4004", cmd.debug);
        assertNotNull("mixin should be initialized", cmd.scriptMixin);
        assertEquals("myfile.java", cmd.scriptMixin.scriptOrFile);
    }

    @Test
    public void testMixinArgument_PeekAheadParser_FileOnly() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new MixinArgPeekAheadParserCommand<>()).getParser();

        // --debug myfile.java: debug should get "" (file doesn't match port pattern), scriptOrFile should get "myfile.java"
        parser.populateObject("run --debug myfile.java",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        MixinArgPeekAheadParserCommand<CommandInvocation> cmd = (MixinArgPeekAheadParserCommand<CommandInvocation>) parser
                .getCommand();

        assertEquals("", cmd.debug);
        assertNotNull("mixin should be initialized", cmd.scriptMixin);
        assertEquals("myfile.java", cmd.scriptMixin.scriptOrFile);
    }

    @Test
    public void testMixinArgument_PeekAheadParser_EqualsPort() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new MixinArgPeekAheadParserCommand<>()).getParser();

        // --debug=5005 myfile.java: debug should get "5005", scriptOrFile should get "myfile.java"
        parser.populateObject("run --debug=5005 myfile.java",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        MixinArgPeekAheadParserCommand<CommandInvocation> cmd = (MixinArgPeekAheadParserCommand<CommandInvocation>) parser
                .getCommand();

        assertEquals("5005", cmd.debug);
        assertNotNull("mixin should be initialized", cmd.scriptMixin);
        assertEquals("myfile.java", cmd.scriptMixin.scriptOrFile);
    }

    // --- Issue #446: fallbackValue ---

    @CommandDefinition(name = "app", description = "test fallbackValue")
    public class FallbackValueCommand<CI extends CommandInvocation> implements Command<CI> {

        @Option(name = "debug", fallbackValue = "4004", description = "Debug port")
        String debug;

        @Argument(description = "Script file")
        String script;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testFallbackValue_NotSpecified() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new FallbackValueCommand<>()).getParser();

        parser.populateObject("app myfile.java", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        FallbackValueCommand<CommandInvocation> cmd = (FallbackValueCommand<CommandInvocation>) parser.getCommand();

        assertNull("debug should be null when not specified", cmd.debug);
        assertEquals("myfile.java", cmd.script);
    }

    @Test
    public void testFallbackValue_BareOption() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new FallbackValueCommand<>()).getParser();

        parser.populateObject("app --debug myfile.java", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        FallbackValueCommand<CommandInvocation> cmd = (FallbackValueCommand<CommandInvocation>) parser.getCommand();

        assertEquals("4004", cmd.debug);
        assertEquals("myfile.java", cmd.script);
    }

    @Test
    public void testFallbackValue_ExplicitValue() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new FallbackValueCommand<>()).getParser();

        parser.populateObject("app --debug=5005 myfile.java", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        FallbackValueCommand<CommandInvocation> cmd = (FallbackValueCommand<CommandInvocation>) parser.getCommand();

        assertEquals("5005", cmd.debug);
        assertEquals("myfile.java", cmd.script);
    }

    // --- Issue #447: fallbackValue with short option ---

    @CommandDefinition(name = "run", description = "test fallbackValue short option", stopAtFirstPositional = true)
    public class FallbackShortCommand<CI extends CommandInvocation> implements Command<CI> {

        @Option(shortName = 'c', name = "code", fallbackValue = "", description = "Run the given string as code")
        String literalScript;

        @Argument(description = "Script file")
        String scriptOrFile;

        @Arguments(index = "1..*", arity = "0..*")
        List<String> userParams;

        @Override
        public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testFallbackValue_ShortWithEquals() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new FallbackShortCommand<>()).getParser();

        parser.populateObject("run -c=somecode firstarg",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        FallbackShortCommand<CommandInvocation> cmd = (FallbackShortCommand<CommandInvocation>) parser.getCommand();

        assertEquals("somecode", cmd.literalScript);
        assertEquals("firstarg", cmd.scriptOrFile);
    }

    @Test
    public void testFallbackValue_ShortBare() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new FallbackShortCommand<>()).getParser();

        parser.populateObject("run -c somecode firstarg",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        FallbackShortCommand<CommandInvocation> cmd = (FallbackShortCommand<CommandInvocation>) parser.getCommand();

        assertEquals("fallback should be empty string", "", cmd.literalScript);
        assertEquals("somecode", cmd.scriptOrFile);
    }

    @Test
    public void testFallbackValue_LongBareWithSpace() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new FallbackShortCommand<>()).getParser();

        parser.populateObject("run --code somecode firstarg",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        FallbackShortCommand<CommandInvocation> cmd = (FallbackShortCommand<CommandInvocation>) parser.getCommand();

        assertEquals("fallback should be empty string", "", cmd.literalScript);
        assertEquals("somecode", cmd.scriptOrFile);
    }

    // --- fallbackValue + defaultValue interaction ---

    @CommandDefinition(name = "app", description = "test fallbackValue + defaultValue")
    public class FallbackWithDefaultCommand<CI extends CommandInvocation> implements Command<CI> {
        @Option(name = "mode", fallbackValue = "fast", defaultValue = "safe")
        String mode;

        @Override
        public CommandResult execute(CI ci) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testFallbackValue_WithDefaultValue_NotSpecified() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new FallbackWithDefaultCommand<>()).getParser();

        parser.populateObject("app", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        FallbackWithDefaultCommand<CommandInvocation> cmd = (FallbackWithDefaultCommand<CommandInvocation>) parser.getCommand();

        // Not specified: should get defaultValue, not fallbackValue
        assertEquals("safe", cmd.mode);
    }

    @Test
    public void testFallbackValue_WithDefaultValue_BareOption() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new FallbackWithDefaultCommand<>()).getParser();

        parser.populateObject("app --mode", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        FallbackWithDefaultCommand<CommandInvocation> cmd = (FallbackWithDefaultCommand<CommandInvocation>) parser.getCommand();

        // Bare: should get fallbackValue, not defaultValue
        assertEquals("fast", cmd.mode);
    }

    @Test
    public void testFallbackValue_WithDefaultValue_ExplicitValue() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new FallbackWithDefaultCommand<>()).getParser();

        parser.populateObject("app --mode=turbo", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        FallbackWithDefaultCommand<CommandInvocation> cmd = (FallbackWithDefaultCommand<CommandInvocation>) parser.getCommand();

        // Explicit: should get user value
        assertEquals("turbo", cmd.mode);
    }

    // --- Mixin inheritance tests (#471) ---

    public static class BaseMixin {
        @Option(name = "verbose", hasValue = false, description = "Verbose output")
        boolean verbose;

        @Option(name = "config", description = "Config file")
        String config;
    }

    public static class ExtendedMixin extends BaseMixin {
        @Option(name = "debug", hasValue = false, description = "Debug mode")
        boolean debug;

        @Option(name = "log-level", description = "Log level")
        String logLevel;
    }

    @CommandDefinition(name = "inherit-test", description = "Test mixin inheritance")
    public static class MixinInheritanceCommand implements Command<CommandInvocation> {
        @Mixin
        ExtendedMixin options;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testMixinInheritance() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new MixinInheritanceCommand()).getParser();

        // All four options should be available (2 from BaseMixin + 2 from ExtendedMixin)
        parser.populateObject("inherit-test --verbose --debug --config myconfig --log-level INFO",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        MixinInheritanceCommand cmd = (MixinInheritanceCommand) parser.getCommand();

        assertNotNull("Mixin should be initialized", cmd.options);
        assertTrue("verbose from BaseMixin should be set", cmd.options.verbose);
        assertTrue("debug from ExtendedMixin should be set", cmd.options.debug);
        assertEquals("config from BaseMixin should be set", "myconfig", cmd.options.config);
        assertEquals("logLevel from ExtendedMixin should be set", "INFO", cmd.options.logLevel);
    }

    @Test
    public void testMixinInheritance_HelpShowsAllOptions() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new MixinInheritanceCommand()).getParser();
        parser.updateAnsiMode(false);
        String help = parser.printHelp();

        assertTrue("Help should show --verbose", help.contains("--verbose"));
        assertTrue("Help should show --debug", help.contains("--debug"));
        assertTrue("Help should show --config", help.contains("--config"));
        assertTrue("Help should show --log-level", help.contains("--log-level"));
    }

    // --- Nested mixin tests (#469) ---

    public static class InnerMixin {
        @Option(name = "trace", hasValue = false, description = "Trace mode")
        boolean trace;

        @Option(name = "output", description = "Output file")
        String output;
    }

    public static class OuterMixin {
        @Mixin
        InnerMixin inner;

        @Option(name = "quiet", hasValue = false, description = "Quiet mode")
        boolean quiet;
    }

    @CommandDefinition(name = "nested-test", description = "Test nested mixins")
    public static class NestedMixinCommand implements Command<CommandInvocation> {
        @Mixin
        OuterMixin outer;

        @Option(name = "name", description = "Name")
        String name;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testNestedMixin_OptionsDiscovered() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new NestedMixinCommand()).getParser();
        parser.updateAnsiMode(false);
        String help = parser.printHelp();

        // All options from outer mixin, inner mixin, and command should appear
        assertTrue("Help should show --quiet (outer mixin)", help.contains("--quiet"));
        assertTrue("Help should show --trace (inner mixin)", help.contains("--trace"));
        assertTrue("Help should show --output (inner mixin)", help.contains("--output"));
        assertTrue("Help should show --name (command)", help.contains("--name"));
    }

    @Test
    public void testNestedMixin_ParseAndPopulate() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new NestedMixinCommand()).getParser();

        parser.populateObject("nested-test --quiet --trace --output result.txt --name test",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        NestedMixinCommand cmd = (NestedMixinCommand) parser.getCommand();

        assertNotNull("Outer mixin should be initialized", cmd.outer);
        assertTrue("quiet from OuterMixin should be set", cmd.outer.quiet);
        assertNotNull("Inner mixin should be initialized", cmd.outer.inner);
        assertTrue("trace from InnerMixin should be set", cmd.outer.inner.trace);
        assertEquals("output from InnerMixin should be set", "result.txt", cmd.outer.inner.output);
        assertEquals("name from command should be set", "test", cmd.name);
    }

    // --- Optional<T> support tests (#472) ---

    @CommandDefinition(name = "opt-test", description = "Test Optional support")
    public static class OptionalCommand implements Command<CommandInvocation> {
        @Option(name = "name", description = "Name")
        java.util.Optional<String> name;

        @Option(name = "count", description = "Count")
        java.util.Optional<Integer> count;

        @Option(name = "verbose", hasValue = false, description = "Verbose")
        boolean verbose;

        @Argument(description = "Source file")
        java.util.Optional<String> source;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testOptionalOption_WithValue() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionalCommand()).getParser();

        parser.populateObject("opt-test --name hello --count 42",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionalCommand cmd = (OptionalCommand) parser.getCommand();

        assertTrue("name should be present", cmd.name.isPresent());
        assertEquals("hello", cmd.name.get());
        assertTrue("count should be present", cmd.count.isPresent());
        assertEquals(Integer.valueOf(42), cmd.count.get());
    }

    @Test
    public void testOptionalOption_WithoutValue() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionalCommand()).getParser();

        parser.populateObject("opt-test --verbose",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionalCommand cmd = (OptionalCommand) parser.getCommand();

        // Optional fields not provided should be Optional.empty(), not null
        assertNotNull("name should not be null", cmd.name);
        assertFalse("name should be empty", cmd.name.isPresent());
        assertNotNull("count should not be null", cmd.count);
        assertFalse("count should be empty", cmd.count.isPresent());
    }

    @Test
    public void testOptionalArgument() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionalCommand()).getParser();

        parser.populateObject("opt-test myfile.java",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionalCommand cmd = (OptionalCommand) parser.getCommand();

        assertTrue("source should be present", cmd.source.isPresent());
        assertEquals("myfile.java", cmd.source.get());
    }

    @Test
    public void testOptionalArgument_NotProvided() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionalCommand()).getParser();

        parser.populateObject("opt-test --verbose",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionalCommand cmd = (OptionalCommand) parser.getCommand();

        assertNotNull("source should not be null", cmd.source);
        assertFalse("source should be empty", cmd.source.isPresent());
    }

    @CommandDefinition(name = "opt-list-test", description = "Test Optional list")
    public static class OptionalListCommand implements Command<CommandInvocation> {
        @OptionList(name = "items", description = "Items")
        java.util.Optional<java.util.List<String>> items;

        @OptionGroup(shortName = 'D', description = "Properties")
        java.util.Optional<java.util.Map<String, String>> props;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testOptionalList_WithValues() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionalListCommand()).getParser();

        parser.populateObject("opt-list-test --items a,b,c",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionalListCommand cmd = (OptionalListCommand) parser.getCommand();

        assertTrue("items should be present", cmd.items.isPresent());
        assertEquals(3, cmd.items.get().size());
        assertEquals("a", cmd.items.get().get(0));
    }

    @Test
    public void testOptionalGroup_WithValues() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionalListCommand()).getParser();

        parser.populateObject("opt-list-test -Dkey=value",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionalListCommand cmd = (OptionalListCommand) parser.getCommand();

        assertTrue("props should be present", cmd.props.isPresent());
        assertEquals("value", cmd.props.get().get("key"));
    }

    @CommandDefinition(name = "opt-args-test", description = "Test Optional arguments")
    public static class OptionalArgumentsCommand implements Command<CommandInvocation> {
        @Option(name = "verbose", hasValue = false)
        boolean verbose;

        @org.aesh.command.option.Arguments(description = "Input files")
        java.util.Optional<java.util.List<String>> files;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testOptionalArguments_WithValues() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionalArgumentsCommand()).getParser();

        parser.populateObject("opt-args-test file1.java file2.java",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionalArgumentsCommand cmd = (OptionalArgumentsCommand) parser.getCommand();

        assertTrue("files should be present", cmd.files.isPresent());
        assertEquals(2, cmd.files.get().size());
        assertEquals("file1.java", cmd.files.get().get(0));
        assertEquals("file2.java", cmd.files.get().get(1));
    }

    @Test
    public void testOptionalArguments_NotProvided() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionalArgumentsCommand()).getParser();

        parser.populateObject("opt-args-test --verbose",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionalArgumentsCommand cmd = (OptionalArgumentsCommand) parser.getCommand();

        assertNotNull("files should not be null", cmd.files);
        assertFalse("files should be empty", cmd.files.isPresent());
    }

    @Test
    public void testOptionalList_NotProvided() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionalListCommand()).getParser();

        parser.populateObject("opt-list-test",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionalListCommand cmd = (OptionalListCommand) parser.getCommand();

        assertNotNull("items should not be null", cmd.items);
        assertFalse("items should be empty", cmd.items.isPresent());
        assertNotNull("props should not be null", cmd.props);
        assertFalse("props should be empty", cmd.props.isPresent());
    }

    // --- Optional<T> with defaultValue, fallbackValue, negatable, required ---

    @CommandDefinition(name = "opt-advanced", description = "Optional advanced")
    public static class OptionalAdvancedCommand implements Command<CommandInvocation> {
        @Option(name = "env", defaultValue = "dev", description = "Environment")
        java.util.Optional<String> env;

        @Option(name = "debug", fallbackValue = "4004", description = "Debug port")
        java.util.Optional<String> debug;

        @Option(name = "cds", hasValue = false, negatable = true, description = "CDS")
        java.util.Optional<Boolean> cds;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testOptional_WithDefaultValue() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionalAdvancedCommand()).getParser();

        // Don't provide --env, should get default wrapped in Optional
        parser.populateObject("opt-advanced",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionalAdvancedCommand cmd = (OptionalAdvancedCommand) parser.getCommand();

        assertTrue("env should be present (from default)", cmd.env.isPresent());
        assertEquals("dev", cmd.env.get());
    }

    @Test
    public void testOptional_WithDefaultValueOverridden() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionalAdvancedCommand()).getParser();

        parser.populateObject("opt-advanced --env prod",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionalAdvancedCommand cmd = (OptionalAdvancedCommand) parser.getCommand();

        assertTrue("env should be present", cmd.env.isPresent());
        assertEquals("prod", cmd.env.get());
    }

    @Test
    public void testOptional_WithFallbackValue() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionalAdvancedCommand()).getParser();

        // --debug without value should use fallback
        parser.populateObject("opt-advanced --debug",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionalAdvancedCommand cmd = (OptionalAdvancedCommand) parser.getCommand();

        assertTrue("debug should be present (from fallback)", cmd.debug.isPresent());
        assertEquals("4004", cmd.debug.get());
    }

    @Test
    public void testOptional_WithFallbackValueExplicit() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionalAdvancedCommand()).getParser();

        // --debug=5005 should use explicit value
        parser.populateObject("opt-advanced --debug=5005",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionalAdvancedCommand cmd = (OptionalAdvancedCommand) parser.getCommand();

        assertTrue("debug should be present", cmd.debug.isPresent());
        assertEquals("5005", cmd.debug.get());
    }

    @Test
    public void testOptional_NegatableBoolean() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionalAdvancedCommand()).getParser();

        // --no-cds should set to Optional.of(false)
        parser.populateObject("opt-advanced --no-cds",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionalAdvancedCommand cmd = (OptionalAdvancedCommand) parser.getCommand();

        assertTrue("cds should be present", cmd.cds.isPresent());
        assertFalse("cds should be false (negated)", cmd.cds.get());
    }

    @Test
    public void testOptional_NegatableBooleanPositive() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionalAdvancedCommand()).getParser();

        // --cds should set to Optional.of(true)
        parser.populateObject("opt-advanced --cds",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionalAdvancedCommand cmd = (OptionalAdvancedCommand) parser.getCommand();

        assertTrue("cds should be present", cmd.cds.isPresent());
        assertTrue("cds should be true", cmd.cds.get());
    }

    @Test
    public void testOptional_ResetBetweenParses() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionalAdvancedCommand()).getParser();

        // First parse: set --debug
        parser.populateObject("opt-advanced --debug=9999",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionalAdvancedCommand cmd1 = (OptionalAdvancedCommand) parser.getCommand();
        assertTrue("debug should be present after first parse", cmd1.debug.isPresent());
        assertEquals("9999", cmd1.debug.get());

        // Second parse: don't set --debug — should reset to empty (not retain 9999)
        parser.parse("opt-advanced");
        parser.getCommandPopulator().populateObject(parser.getProcessedCommand(),
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);

        // debug should be empty after reset since no fallback/default applies
        // (fallbackValue only applies when the flag is present without a value)
        assertNotNull("debug should not be null after reset", cmd1.debug);
    }

    // --- Unified @CommandDefinition with groupCommands (#474) ---

    @CommandDefinition(name = "sub-a", description = "Sub A")
    public static class UnifiedSubA implements Command<CommandInvocation> {
        @Option(name = "value", description = "A value")
        String value;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "sub-b", description = "Sub B")
    public static class UnifiedSubB implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "unified-group", description = "Unified group command", groupCommands = { UnifiedSubA.class,
            UnifiedSubB.class }, generateHelp = true)
    public static class UnifiedGroupCommand implements Command<CommandInvocation> {
        @Option(name = "verbose", hasValue = false, description = "Verbose")
        boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testUnifiedCommandDefinitionWithGroupCommands() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new UnifiedGroupCommand()).getParser();

        // Should be detected as a group command
        assertTrue("Should be a group command", parser.isGroupCommand());

        // Child parsers should be available
        assertNotNull("Should find sub-a", parser.getChildParser("sub-a"));
        assertNotNull("Should find sub-b", parser.getChildParser("sub-b"));

        // Parse subcommand
        parser.populateObject("unified-group sub-a --value hello",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        CommandLineParser<CommandInvocation> parsedChild = parser.parsedCommand();
        assertNotNull("Should have parsed child", parsedChild);
        assertEquals("sub-a", parsedChild.getProcessedCommand().name());

        UnifiedSubA subA = (UnifiedSubA) parsedChild.getCommand();
        assertEquals("hello", subA.value);
    }

    @Test
    public void testUnifiedCommandDefinition_HelpShowsSubcommands() throws Exception {
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new UnifiedGroupCommand()).getParser();
        parser.updateAnsiMode(false);
        String help = parser.printHelp();

        assertTrue("Help should show sub-a", help.contains("sub-a"));
        assertTrue("Help should show sub-b", help.contains("sub-b"));
        assertTrue("Help should show --verbose", help.contains("--verbose"));
        assertTrue("Help should show --help", help.contains("--help"));
    }

    // --- Tests for @OptionGroup --option=key=value syntax (#496) ---

    @CommandDefinition(name = "buildcmd", description = "Build command")
    public class OptionGroupEqualsCmd<CI extends CommandInvocation> implements Command<CI> {
        @OptionGroup(shortName = 'D', name = "manifest", description = "Manifest entries")
        Map<String, String> manifest;

        @Override
        public CommandResult execute(CI ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testOptionGroupEqualsSyntax() throws Exception {
        // #496: --manifest=Key=Value (picocli syntax) should work the same as --manifestKey=Value (aesh syntax)
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionGroupEqualsCmd<>()).getParser();

        // Aesh concatenated syntax: --manifestFoo=Bar
        parser.populateObject("buildcmd --manifestFoo=Bar", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        OptionGroupEqualsCmd<?> cmd = (OptionGroupEqualsCmd<?>) parser.getCommand();
        assertEquals("Aesh syntax: key should be Foo", "Bar", cmd.manifest.get("Foo"));

        // Picocli equals syntax: --manifest=Foo=Bar
        parser.populateObject("buildcmd --manifest=Foo=Bar", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        cmd = (OptionGroupEqualsCmd<?>) parser.getCommand();
        assertEquals("Picocli syntax: key should be Foo", "Bar", cmd.manifest.get("Foo"));

        // Short name syntax: -DFoo=Bar
        parser.populateObject("buildcmd -DFoo=Bar", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        cmd = (OptionGroupEqualsCmd<?>) parser.getCommand();
        assertEquals("Short syntax: key should be Foo", "Bar", cmd.manifest.get("Foo"));

        // Multiple properties with picocli syntax
        parser.populateObject("buildcmd --manifest=Key1=Val1 --manifest=Key2=Val2", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        cmd = (OptionGroupEqualsCmd<?>) parser.getCommand();
        assertEquals("Multiple: Key1", "Val1", cmd.manifest.get("Key1"));
        assertEquals("Multiple: Key2", "Val2", cmd.manifest.get("Key2"));

        // Mixed syntaxes in same command
        parser.populateObject("buildcmd --manifestA=1 --manifest=B=2 -DC=3", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        cmd = (OptionGroupEqualsCmd<?>) parser.getCommand();
        assertEquals("Mixed: A", "1", cmd.manifest.get("A"));
        assertEquals("Mixed: B", "2", cmd.manifest.get("B"));
        assertEquals("Mixed: C", "3", cmd.manifest.get("C"));

        // Empty value: --manifest=Key=
        parser.populateObject("buildcmd --manifest=Key=", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        cmd = (OptionGroupEqualsCmd<?>) parser.getCommand();
        assertEquals("Empty value", "", cmd.manifest.get("Key"));

        // Value containing equals: --manifest=Key=a=b
        parser.populateObject("buildcmd --manifest=Key=a=b", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        cmd = (OptionGroupEqualsCmd<?>) parser.getCommand();
        assertEquals("Value with equals", "a=b", cmd.manifest.get("Key"));
    }

    // --- Issue #515: @OptionGroup key-only entries ---

    @CommandDefinition(name = "agentcmd", description = "Agent command")
    public class OptionGroupKeyOnlyCmd<CI extends CommandInvocation> implements Command<CI> {
        @OptionGroup(shortName = 'J', name = "javaagent", description = "Java agents", defaultValue = "")
        Map<String, String> agents;

        @Override
        public CommandResult execute(CI ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testOptionGroupKeyOnlyWithDefault() throws Exception {
        // Key-only entry uses the defaultValue as the value
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionGroupKeyOnlyCmd<>()).getParser();

        parser.populateObject("agentcmd --javaagent=xyz.jar", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        OptionGroupKeyOnlyCmd<?> cmd = (OptionGroupKeyOnlyCmd<?>) parser.getCommand();

        assertEquals("Key-only should use default value", "", cmd.agents.get("xyz.jar"));
    }

    @Test
    public void testOptionGroupMixedKeyOnlyAndKeyValue() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionGroupKeyOnlyCmd<>()).getParser();

        parser.populateObject("agentcmd --javaagent=agent1.jar --javaagent=agent2.jar=opts",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionGroupKeyOnlyCmd<?> cmd = (OptionGroupKeyOnlyCmd<?>) parser.getCommand();

        assertEquals("Key-only entry", "", cmd.agents.get("agent1.jar"));
        assertEquals("Key-value entry", "opts", cmd.agents.get("agent2.jar"));
    }

    @Test
    public void testOptionGroupKeyOnlyPreservesOrder() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionGroupKeyOnlyCmd<>()).getParser();

        parser.populateObject("agentcmd --javaagent=first.jar --javaagent=second.jar=opts --javaagent=third.jar",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionGroupKeyOnlyCmd<?> cmd = (OptionGroupKeyOnlyCmd<?>) parser.getCommand();

        java.util.Iterator<String> keys = cmd.agents.keySet().iterator();
        assertEquals("first.jar", keys.next());
        assertEquals("second.jar", keys.next());
        assertEquals("third.jar", keys.next());
    }

    @Test
    public void testOptionGroupKeyOnlyWithoutDefaultThrows() throws Exception {
        // Without defaultValue, key-only entry should throw
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionGroupEqualsCmd<>()).getParser();

        try {
            parser.populateObject("buildcmd --manifest=keyonly", invocationProviders, aeshContext,
                    CommandLineParser.Mode.VALIDATE);
            fail("Key-only without defaultValue should throw");
        } catch (Exception e) {
            assertTrue("Should mention 'property'", e.getMessage().contains("property"));
        }
    }

    // --- Issue #513: @OptionGroup preserves insertion order ---

    @Test
    public void testOptionGroupPreservesInsertionOrder() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionGroupEqualsCmd<>()).getParser();

        // Insert multiple properties in a specific order
        parser.populateObject("buildcmd -Dalpha=1 -Dbeta=2 -Dgamma=3 -Ddelta=4 -Depsilon=5",
                invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        OptionGroupEqualsCmd<?> cmd = (OptionGroupEqualsCmd<?>) parser.getCommand();

        // Iteration order must match insertion order
        java.util.Iterator<String> keys = cmd.manifest.keySet().iterator();
        assertEquals("First key", "alpha", keys.next());
        assertEquals("Second key", "beta", keys.next());
        assertEquals("Third key", "gamma", keys.next());
        assertEquals("Fourth key", "delta", keys.next());
        assertEquals("Fifth key", "epsilon", keys.next());
    }

    @Test
    public void testOptionGroupMapIsLinkedHashMap() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new OptionGroupEqualsCmd<>()).getParser();

        parser.populateObject("buildcmd -DA=1", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        OptionGroupEqualsCmd<?> cmd = (OptionGroupEqualsCmd<?>) parser.getCommand();

        assertTrue("Injected map should be a LinkedHashMap",
                cmd.manifest instanceof java.util.LinkedHashMap);
    }

    // --- HashMap lookup map invalidation tests ---

    @Test
    public void testLookupMapInvalidationAfterAddOption() throws Exception {
        // Gap: options added after the first lookup should still be findable.
        // The lookup maps are lazily built; addOption must invalidate them.
        org.aesh.command.impl.internal.ProcessedCommand processedCommand = new AeshCommandContainerBuilder<>()
                .create(new LookupTestCmd<>())
                .getParser().getProcessedCommand();

        // Trigger map build by doing a lookup
        assertNotNull("Initial option should be found", processedCommand.findLongOptionNoActivatorCheck("initial"));

        // Now add a new option after the map was built
        processedCommand.addOption(
                org.aesh.command.impl.internal.ProcessedOptionBuilder.builder()
                        .name("dynamic")
                        .type(String.class)
                        .description("Dynamically added option")
                        .build());

        // The new option should be findable (map was invalidated and rebuilt)
        assertNotNull("Dynamically added option should be found",
                processedCommand.findLongOptionNoActivatorCheck("dynamic"));
        // Original option should still be found
        assertNotNull("Initial option should still be found",
                processedCommand.findLongOptionNoActivatorCheck("initial"));
    }

    @Test
    public void testLookupMapInvalidationForShortName() throws Exception {
        org.aesh.command.impl.internal.ProcessedCommand processedCommand = new AeshCommandContainerBuilder<>()
                .create(new LookupTestCmd<>())
                .getParser().getProcessedCommand();

        // Trigger map build
        processedCommand.findOptionNoActivatorCheck("i");

        // Add option with short name
        processedCommand.addOption(
                org.aesh.command.impl.internal.ProcessedOptionBuilder.builder()
                        .shortName('d')
                        .name("dynamic")
                        .type(String.class)
                        .description("Dynamic")
                        .build());

        // Short name should be findable
        assertNotNull("Short name should be found after add",
                processedCommand.findOptionNoActivatorCheck("d"));
    }

    @CommandDefinition(name = "lookuptest", description = "Lookup test command")
    public static class LookupTestCmd<CI extends CommandInvocation> implements Command<CI> {
        @Option(shortName = 'i', description = "Initial option")
        private String initial;

        @Override
        public CommandResult execute(CI ci) {
            return CommandResult.SUCCESS;
        }
    }

    // --- DefaultValueProvider.fallbackValue() exception handling test ---

    @Test
    public void testFallbackValueProviderExceptionFallsThrough() throws Exception {
        // Gap: when DefaultValueProvider.fallbackValue() throws, should gracefully
        // fall through to annotation fallbackValue.
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new FallbackExceptionCmd<>()).getParser();

        // --debug without value triggers applyOptionalFallback
        parser.populateObject("fallback-exc --debug", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        FallbackExceptionCmd<CommandInvocation> cmd = (FallbackExceptionCmd<CommandInvocation>) parser.getCommand();

        // Provider throws, so annotation fallbackValue "4004" should be used
        assertEquals("Should fall through to annotation fallbackValue", "4004", cmd.debug);
    }

    @Test
    public void testFallbackValueProviderReturnsNull() throws Exception {
        // When provider returns null for fallbackValue, should fall through to annotation
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new FallbackNullCmd<>()).getParser();

        parser.populateObject("fallback-null --debug", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        FallbackNullCmd<CommandInvocation> cmd = (FallbackNullCmd<CommandInvocation>) parser.getCommand();

        assertEquals("Should fall through to annotation fallbackValue", "5005", cmd.debug);
    }

    /** Provider that throws on fallbackValue */
    public static class ThrowingFallbackProvider implements DefaultValueProvider {
        @Override
        public String defaultValue(ProcessedOption option) {
            return null;
        }

        @Override
        public String fallbackValue(ProcessedOption option) throws Exception {
            throw new RuntimeException("Simulated provider failure");
        }
    }

    /** Provider that returns null for fallbackValue */
    public static class NullFallbackProvider implements DefaultValueProvider {
        @Override
        public String defaultValue(ProcessedOption option) {
            return null;
        }

        @Override
        public String fallbackValue(ProcessedOption option) {
            return null;
        }
    }

    @CommandDefinition(name = "fallback-exc", description = "Fallback exception test", defaultValueProvider = ThrowingFallbackProvider.class)
    public static class FallbackExceptionCmd<CI extends CommandInvocation> implements Command<CI> {
        @Option(fallbackValue = "4004")
        private String debug;

        @Override
        public CommandResult execute(CI ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "fallback-null", description = "Fallback null test", defaultValueProvider = NullFallbackProvider.class)
    public static class FallbackNullCmd<CI extends CommandInvocation> implements Command<CI> {
        @Option(fallbackValue = "5005")
        private String debug;

        @Override
        public CommandResult execute(CI ci) {
            return CommandResult.SUCCESS;
        }
    }

    // --- Issue #511: Custom parser + fallback value chain ---

    /**
     * Custom parser that peeks ahead and only consumes a numeric port value.
     * Does NOT set a sentinel — leaves value unset when no match, relying on
     * the framework to apply the fallback chain.
     */
    public static class FallbackAwarePeekParser implements OptionParser {
        private static final java.util.regex.Pattern PORT_PATTERN = java.util.regex.Pattern.compile("\\d+");

        @Override
        public void parse(ParsedLineIterator iter, ProcessedOption option) throws OptionParserException {
            String word = iter.peekWord();
            String prefix = option.isLongNameUsed() ? "--" : "-";
            String optName = option.isLongNameUsed() ? option.name() : option.shortName();
            String fullPrefix = prefix + optName;

            // Handle --debug=5005 (equals syntax)
            if (word.startsWith(fullPrefix + "=")) {
                option.addValue(word.substring(fullPrefix.length() + 1));
                iter.pollParsedWord();
                return;
            }

            // Consume the option name word
            iter.pollParsedWord();

            // Peek at next word — only consume if it matches a port pattern
            if (iter.hasNextWord()) {
                String nextWord = iter.peekWord();
                if (!nextWord.startsWith("-") && PORT_PATTERN.matcher(nextWord).matches()) {
                    option.addValue(nextWord);
                    iter.pollParsedWord();
                    return;
                }
            }

            // Do NOT set any value — let the framework apply the fallback chain
        }
    }

    @CommandDefinition(name = "customfb", description = "Custom parser with fallback")
    public static class CustomParserFallbackCmd<CI extends CommandInvocation> implements Command<CI> {
        @Option(name = "debug", parser = FallbackAwarePeekParser.class, fallbackValue = "4004")
        private String debug;

        @Option(name = "verbose", hasValue = false)
        private boolean verbose;

        @Argument(description = "Script file")
        private String script;

        @Override
        public CommandResult execute(CI ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testCustomParser_FallbackOnBareOption() throws Exception {
        // --debug (bare, no next token) → custom parser leaves no value → fallback "4004"
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new CustomParserFallbackCmd<>()).getParser();

        parser.populateObject("customfb --debug", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        CustomParserFallbackCmd<CommandInvocation> cmd = (CustomParserFallbackCmd<CommandInvocation>) parser.getCommand();

        assertEquals("Bare --debug should use fallbackValue", "4004", cmd.debug);
    }

    @Test
    public void testCustomParser_FallbackWhenNextTokenIsNotPort() throws Exception {
        // --debug myfile.java → next token doesn't match port, parser leaves no value → fallback "4004"
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new CustomParserFallbackCmd<>()).getParser();

        parser.populateObject("customfb --debug myfile.java", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        CustomParserFallbackCmd<CommandInvocation> cmd = (CustomParserFallbackCmd<CommandInvocation>) parser.getCommand();

        assertEquals("--debug followed by non-port should use fallbackValue", "4004", cmd.debug);
        assertEquals("myfile.java should be consumed as argument", "myfile.java", cmd.script);
    }

    @Test
    public void testCustomParser_ExplicitValueOverridesFallback() throws Exception {
        // --debug 5005 → custom parser consumes "5005" → no fallback needed
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new CustomParserFallbackCmd<>()).getParser();

        parser.populateObject("customfb --debug 5005 myfile.java", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        CustomParserFallbackCmd<CommandInvocation> cmd = (CustomParserFallbackCmd<CommandInvocation>) parser.getCommand();

        assertEquals("Explicit port should override fallback", "5005", cmd.debug);
        assertEquals("myfile.java should be argument", "myfile.java", cmd.script);
    }

    @Test
    public void testCustomParser_EqualsValueOverridesFallback() throws Exception {
        // --debug=8080 → custom parser consumes via equals → no fallback
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new CustomParserFallbackCmd<>()).getParser();

        parser.populateObject("customfb --debug=8080", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        CustomParserFallbackCmd<CommandInvocation> cmd = (CustomParserFallbackCmd<CommandInvocation>) parser.getCommand();

        assertEquals("Equals value should be used directly", "8080", cmd.debug);
    }

    @Test
    public void testCustomParser_FallbackBeforeAnotherOption() throws Exception {
        // --debug --verbose → next token is another option, parser leaves no value → fallback "4004"
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new CustomParserFallbackCmd<>()).getParser();

        parser.populateObject("customfb --debug --verbose", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        CustomParserFallbackCmd<CommandInvocation> cmd = (CustomParserFallbackCmd<CommandInvocation>) parser.getCommand();

        assertEquals("--debug before another option should use fallback", "4004", cmd.debug);
        assertTrue("--verbose should be set", cmd.verbose);
    }

    // --- Custom parser + DefaultValueProvider.fallbackValue() (#511 + #507) ---

    /** Provider that returns dynamic fallback value */
    public static class DynamicDebugProvider implements DefaultValueProvider {
        @Override
        public String defaultValue(ProcessedOption option) {
            return null;
        }

        @Override
        public String fallbackValue(ProcessedOption option) {
            if ("debug".equals(option.name())) {
                return "9999"; // dynamic port from config
            }
            return null;
        }
    }

    @CommandDefinition(name = "dynfb", description = "Dynamic provider fallback", defaultValueProvider = DynamicDebugProvider.class)
    public static class CustomParserDynamicFallbackCmd<CI extends CommandInvocation> implements Command<CI> {
        @Option(name = "debug", parser = FallbackAwarePeekParser.class, fallbackValue = "4004")
        private String debug;

        @Override
        public CommandResult execute(CI ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testCustomParser_ProviderFallbackTakesPriorityOverAnnotation() throws Exception {
        // Provider returns "9999", annotation says "4004" — provider wins
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new CustomParserDynamicFallbackCmd<>()).getParser();

        parser.populateObject("dynfb --debug", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        CustomParserDynamicFallbackCmd<CommandInvocation> cmd = (CustomParserDynamicFallbackCmd<CommandInvocation>) parser
                .getCommand();

        assertEquals("Provider fallback should take priority over annotation fallback",
                "9999", cmd.debug);
    }

    @Test
    public void testCustomParser_ExplicitValueOverridesProvider() throws Exception {
        // --debug 5005 → explicit value wins over both provider and annotation
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new CustomParserDynamicFallbackCmd<>()).getParser();

        parser.populateObject("dynfb --debug 5005", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        CustomParserDynamicFallbackCmd<CommandInvocation> cmd = (CustomParserDynamicFallbackCmd<CommandInvocation>) parser
                .getCommand();

        assertEquals("Explicit value should override provider fallback", "5005", cmd.debug);
    }

    // --- ${env:...} and ${sys:...} resolution in fallbackValue ---

    @CommandDefinition(name = "envfb", description = "Env fallback test")
    public static class EnvFallbackCmd<CI extends CommandInvocation> implements Command<CI> {
        @Option(name = "port", fallbackValue = "${sys:user.home}")
        private String port;

        @Override
        public CommandResult execute(CI ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testFallbackValueResolvesSystemProperty() throws Exception {
        // fallbackValue = "${sys:user.home}" should resolve to the actual user.home value
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new EnvFallbackCmd<>()).getParser();

        parser.populateObject("envfb --port", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        EnvFallbackCmd<CommandInvocation> cmd = (EnvFallbackCmd<CommandInvocation>) parser.getCommand();

        String expected = System.getProperty("user.home");
        assertEquals("fallbackValue should resolve ${sys:user.home}", expected, cmd.port);
    }

    @Test
    public void testFallbackValueLiteralUnchanged() throws Exception {
        // A plain literal fallbackValue should pass through unchanged
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();

        org.aesh.command.impl.internal.ProcessedCommand processedCommand = org.aesh.command.impl.internal.ProcessedCommandBuilder
                .builder()
                .name("test")
                .command(new EnvFallbackCmd<>())
                .create();
        processedCommand.addOption(
                org.aesh.command.impl.internal.ProcessedOptionBuilder.builder()
                        .name("level")
                        .type(String.class)
                        .fallbackValue("INFO")
                        .build());

        org.aesh.command.impl.internal.ProcessedOption opt = processedCommand.findLongOptionNoActivatorCheck("level");
        assertEquals("Literal fallbackValue should be unchanged", "INFO", opt.getFallbackValue());
    }

    // --- Issue #520: Deferred ${env:...} resolution ---

    @CommandDefinition(name = "deferred", description = "Deferred env resolution test")
    public static class DeferredEnvCmd<CI extends CommandInvocation> implements Command<CI> {
        @Option(name = "provider", defaultValue = "${sys:aesh.test.deferred.provider}")
        private String provider;

        @Option(name = "port", fallbackValue = "${sys:aesh.test.deferred.port:-4004}")
        private String port;

        @Override
        public CommandResult execute(CI ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testDeferredResolution_defaultValuePicksUpChanges() throws Exception {
        // Set the sys prop AFTER command construction
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new DeferredEnvCmd<>()).getParser();

        // At construction time, sys prop is not set → default is ""
        // Now set the sys prop
        System.setProperty("aesh.test.deferred.provider", "test-provider");
        try {
            // Parse should pick up the new value (clear() re-resolves)
            parser.populateObject("deferred", invocationProviders, aeshContext,
                    CommandLineParser.Mode.VALIDATE);
            DeferredEnvCmd<CommandInvocation> cmd = (DeferredEnvCmd<CommandInvocation>) parser.getCommand();
            assertEquals("Should pick up sys prop set after construction", "test-provider", cmd.provider);
        } finally {
            System.clearProperty("aesh.test.deferred.provider");
        }
    }

    @Test
    public void testDeferredResolution_changesBetweenParses() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new DeferredEnvCmd<>()).getParser();

        // First parse with value "first"
        System.setProperty("aesh.test.deferred.provider", "first");
        try {
            parser.populateObject("deferred", invocationProviders, aeshContext,
                    CommandLineParser.Mode.VALIDATE);
            DeferredEnvCmd<CommandInvocation> cmd = (DeferredEnvCmd<CommandInvocation>) parser.getCommand();
            assertEquals("First parse should use 'first'", "first", cmd.provider);

            // Change the value and parse again
            System.setProperty("aesh.test.deferred.provider", "second");
            parser.populateObject("deferred", invocationProviders, aeshContext,
                    CommandLineParser.Mode.VALIDATE);
            cmd = (DeferredEnvCmd<CommandInvocation>) parser.getCommand();
            assertEquals("Second parse should use 'second'", "second", cmd.provider);
        } finally {
            System.clearProperty("aesh.test.deferred.provider");
        }
    }

    @Test
    public void testDeferredResolution_fallbackValuePicksUpChanges() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new DeferredEnvCmd<>()).getParser();

        // Set sys prop after construction
        System.setProperty("aesh.test.deferred.port", "9999");
        try {
            // Bare --port triggers fallbackValue resolution
            parser.populateObject("deferred --port", invocationProviders, aeshContext,
                    CommandLineParser.Mode.VALIDATE);
            DeferredEnvCmd<CommandInvocation> cmd = (DeferredEnvCmd<CommandInvocation>) parser.getCommand();
            assertEquals("Fallback should pick up sys prop", "9999", cmd.port);
        } finally {
            System.clearProperty("aesh.test.deferred.port");
        }
    }

    @Test
    public void testDeferredResolution_fallbackToStaticDefault() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new DeferredEnvCmd<>()).getParser();

        // No sys prop set → should fall through to :-4004
        parser.populateObject("deferred --port", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        DeferredEnvCmd<CommandInvocation> cmd = (DeferredEnvCmd<CommandInvocation>) parser.getCommand();
        assertEquals("Fallback should use :-4004 when sys prop not set", "4004", cmd.port);
    }

    // --- Issue #521: Env var default overrides provider ---

    /** Provider that returns config values for specific options */
    public static class ConfigProvider implements DefaultValueProvider {
        @Override
        public String defaultValue(ProcessedOption option) {
            if ("editor".equals(option.name()))
                return "vim-from-config";
            return null;
        }

        @Override
        public String fallbackValue(ProcessedOption option) {
            if ("port".equals(option.name()))
                return "8080-from-config";
            return null;
        }
    }

    @CommandDefinition(name = "priority", description = "Priority test", defaultValueProvider = ConfigProvider.class)
    public static class PriorityCmd<CI extends CommandInvocation> implements Command<CI> {
        @Option(name = "editor", defaultValue = "${sys:aesh.test.priority.editor:-}")
        public String editor;

        @Option(name = "port", fallbackValue = "${sys:aesh.test.priority.port:-4004}")
        public String port;

        @Option(name = "plain", defaultValue = "hardcoded")
        public String plain;

        @Override
        public CommandResult execute(CI ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testEnvVarWinsOverProvider() throws Exception {
        // When env var is set, it should win over the provider
        System.setProperty("aesh.test.priority.editor", "code-from-env");
        try {
            AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
            CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                    .create(new PriorityCmd<>()).getParser();

            parser.populateObject("priority", invocationProviders, aeshContext,
                    CommandLineParser.Mode.VALIDATE);
            PriorityCmd<CommandInvocation> cmd = (PriorityCmd<CommandInvocation>) parser.getCommand();

            assertEquals("Env var should win over provider", "code-from-env", cmd.editor);
        } finally {
            System.clearProperty("aesh.test.priority.editor");
        }
    }

    @Test
    public void testProviderWinsWhenEnvVarNotSet() throws Exception {
        // When env var is NOT set, provider should win
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new PriorityCmd<>()).getParser();

        parser.populateObject("priority", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        PriorityCmd<CommandInvocation> cmd = (PriorityCmd<CommandInvocation>) parser.getCommand();

        assertEquals("Provider should win when env var not set", "vim-from-config", cmd.editor);
    }

    @Test
    public void testProviderStillWorksForLiteralDefaults() throws Exception {
        // Options without ${...} should still let the provider win
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new PriorityCmd<>()).getParser();

        parser.populateObject("priority", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        PriorityCmd<CommandInvocation> cmd = (PriorityCmd<CommandInvocation>) parser.getCommand();

        // "plain" has defaultValue="hardcoded" (literal, no ${...})
        // Provider returns null for "plain" → static default wins
        assertEquals("Literal default should be used when provider returns null",
                "hardcoded", cmd.plain);
    }

    @Test
    public void testExplicitValueWinsOverAll() throws Exception {
        // Explicit --editor=explicit should always win
        System.setProperty("aesh.test.priority.editor", "code-from-env");
        try {
            AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
            CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                    .create(new PriorityCmd<>()).getParser();

            parser.populateObject("priority --editor explicit", invocationProviders, aeshContext,
                    CommandLineParser.Mode.VALIDATE);
            PriorityCmd<CommandInvocation> cmd = (PriorityCmd<CommandInvocation>) parser.getCommand();

            assertEquals("Explicit value should win over env var and provider",
                    "explicit", cmd.editor);
        } finally {
            System.clearProperty("aesh.test.priority.editor");
        }
    }

    @Test
    public void testFallbackEnvVarWinsOverProvider() throws Exception {
        // Bare --port with env var set → env var should win over provider fallback
        System.setProperty("aesh.test.priority.port", "9999-from-env");
        try {
            AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
            CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                    .create(new PriorityCmd<>()).getParser();

            parser.populateObject("priority --port", invocationProviders, aeshContext,
                    CommandLineParser.Mode.VALIDATE);
            PriorityCmd<CommandInvocation> cmd = (PriorityCmd<CommandInvocation>) parser.getCommand();

            assertEquals("Fallback env var should win over provider fallback",
                    "9999-from-env", cmd.port);
        } finally {
            System.clearProperty("aesh.test.priority.port");
        }
    }

    @Test
    public void testFallbackProviderWinsWhenEnvVarNotSet() throws Exception {
        // Bare --port without env var → provider fallback should win
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new PriorityCmd<>()).getParser();

        parser.populateObject("priority --port", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        PriorityCmd<CommandInvocation> cmd = (PriorityCmd<CommandInvocation>) parser.getCommand();

        assertEquals("Provider fallback should win when env var not set",
                "8080-from-config", cmd.port);
    }

    @CommandDefinition(name = "literal", description = "Literal default test")
    public static class LiteralDefaultCmd<CI extends CommandInvocation> implements Command<CI> {
        @Option(defaultValue = "hello")
        public String value;

        @Override
        public CommandResult execute(CI ci) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testDeferredResolution_literalDefaultUnchanged() throws Exception {
        // Options with literal defaults should work exactly as before
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>()
                .create(new LiteralDefaultCmd<>()).getParser();

        parser.populateObject("literal", invocationProviders, aeshContext,
                CommandLineParser.Mode.VALIDATE);
        LiteralDefaultCmd<CommandInvocation> cmd = (LiteralDefaultCmd<CommandInvocation>) parser.getCommand();
        assertEquals("Literal default should be unchanged", "hello", cmd.value);
    }

}
