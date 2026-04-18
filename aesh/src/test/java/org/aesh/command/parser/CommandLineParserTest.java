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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.DefaultValueProvider;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionGroup;
import org.aesh.command.option.OptionList;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.AeshContext;
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
        assertFalse(p1.foo);
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
        assertFalse(p1.foo);
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

    @GroupCommandDefinition(name = "group", description = "", groupCommands = { ChildTest1.class, ChildTest2.class })
    public class GroupCommandTest<CI extends CommandInvocation> extends TestingCommand<CI> {

        @Option(hasValue = false)
        private boolean help;

    }

    @GroupCommandDefinition(name = "super", description = "", groupCommands = { SubSuperGroupCommandTest.class })
    public class SuperGroupCommandTest<CI extends CommandInvocation> extends TestingCommand<CI> {

        @Option(hasValue = false)
        private boolean help;

    }

    @GroupCommandDefinition(name = "sub", description = "", groupCommands = { ChildTest1.class, ChildTest2.class })
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

    @GroupCommandDefinition(name = "cli", description = "", groupCommands = { ChildTest1.class, ChildTest2.class })
    public class GroupWithOptionsCommand<CI extends CommandInvocation> extends TestingCommand<CI> {

        @Option(shortName = 'c')
        private String config;

        @Option(hasValue = false)
        private boolean verbose;

    }

    @GroupCommandDefinition(name = "groupfail", description = "", groupCommands = { ChildTest1.class })
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
        // Help should include both the normal and negated forms
        assertTrue(help.contains("--verbose"));
        assertTrue(help.contains("--no-verbose"));
        assertTrue(help.contains("--debug"));
        assertTrue(help.contains("--without-debug"));
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

}
