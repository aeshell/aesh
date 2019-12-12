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
package org.aesh.command.parser;

import org.aesh.command.impl.activator.AeshCommandActivatorProvider;
import org.aesh.command.impl.activator.AeshOptionActivatorProvider;
import org.aesh.command.impl.completer.AeshCompleterInvocationProvider;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.converter.AeshConverterInvocationProvider;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.CommandDefinition;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.impl.validator.AeshValidatorInvocationProvider;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionGroup;
import org.aesh.command.option.OptionList;
import org.aesh.readline.AeshContext;
import org.aesh.command.settings.SettingsBuilder;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import org.aesh.command.CommandException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLineParserTest {

    private final InvocationProviders invocationProviders = new AeshInvocationProviders(
            SettingsBuilder.builder()
                    .converterInvocationProvider(new AeshConverterInvocationProvider())
                    .completerInvocationProvider(new AeshCompleterInvocationProvider())
                    .validatorInvocationProvider(new AeshValidatorInvocationProvider())
                    .optionActivatorProvider(new AeshOptionActivatorProvider())
                    .commandActivatorProvider(new AeshCommandActivatorProvider()).build());

    @Test
    public void testParseCommandLine1() throws Exception {

        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new Parser1Test<>()).getParser();

        parser.populateObject("test -f -e bar -Df=g /tmp/file.txt", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        Parser1Test p1 = (Parser1Test) parser.getCommand();

        assertTrue(p1.foo);
        assertEquals("bar", p1.equal);
        assertEquals("/tmp/file.txt", p1.arguments.get(0));

        parser.populateObject("test -c10 -f -e=bar -Df=g /tmp/file.txt", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertTrue(p1.foo);
        assertEquals(10, p1.connection);
        assertEquals("bar", p1.equal);
        assertEquals("/tmp/file.txt", p1.arguments.get(0));
        assertEquals("g", p1.define.get("f"));

        parser.populateObject("test -Dg=f /tmp/file.txt -e=bar foo bar", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertFalse(p1.foo);
        assertEquals("f", p1.define.get("g"));
        assertEquals("bar", p1.equal);
        assertEquals("/tmp/file.txt", p1.arguments.get(0));
        assertEquals("foo", p1.arguments.get(1));
        assertEquals("bar", p1.arguments.get(2));

        parser.populateObject("test -e beer -DXms=128m -DXmx=512m --X /tmp/file.txt", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("beer", p1.equal);
        assertEquals("/tmp/file.txt", p1.arguments.get(0));
        assertTrue(p1.enableX);

        assertEquals("128m", p1.define.get("Xms"));
        assertEquals("512m", p1.define.get("Xmx"));

        parser.populateObject("test --equal \"bar bar2\" -DXms=\"128g \" -DXmx=512g\\ m /tmp/file.txt", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("bar bar2", p1.equal);

        assertEquals("128g ", p1.define.get("Xms"));
        assertEquals("512g m", p1.define.get("Xmx"));

        parser.populateObject("test -fX -e bar -Df=g /tmp/file.txt\\ ", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertTrue(p1.foo);
        assertTrue(p1.enableX);
        assertEquals("bar", p1.equal);
        assertEquals("g", p1.define.get("f"));
        assertEquals("/tmp/file.txt ", p1.arguments.get(0));

        parser.populateObject("test -ebar -Df=g /tmp/file.txt", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("bar", p1.equal);
        assertEquals("g", p1.define.get("f"));
        assertEquals("/tmp/file.txt", p1.arguments.get(0));
    }

    @Test
    public void testParseCommandLine1SNoSpace() throws Exception {

        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new Parser1Test<>()).getParser();

        parser.populateObject("test -f -ebar -Df=g /tmp/file.txt", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        Parser1Test p1 = (Parser1Test) parser.getCommand();

        assertTrue(p1.foo);
        assertEquals("bar", p1.equal);
        assertEquals("/tmp/file.txt", p1.arguments.get(0));

        parser.populateObject("test -f -ebar -Df=g /tmp/file.txt", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertTrue(p1.foo);
        assertEquals("bar", p1.equal);
        assertEquals("/tmp/file.txt", p1.arguments.get(0));
        assertEquals("g", p1.define.get("f"));

        parser.populateObject("test -Dg=f /tmp/file.txt -ebar foo bar", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertFalse(p1.foo);
        assertEquals("f", p1.define.get("g"));
        assertEquals("bar", p1.equal);
        assertEquals("/tmp/file.txt", p1.arguments.get(0));
        assertEquals("foo", p1.arguments.get(1));
        assertEquals("bar", p1.arguments.get(2));

        parser.populateObject("test -ebeer -DXms=128m -DXmx=512m --X /tmp/file.txt", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("beer", p1.equal);
        assertEquals("/tmp/file.txt", p1.arguments.get(0));
        assertTrue(p1.enableX);

        assertEquals("128m", p1.define.get("Xms"));
        assertEquals("512m", p1.define.get("Xmx"));

        parser.populateObject("test --equal \"bar bar2\" -DXms=\"128g \" -DXmx=512g\\ m /tmp/file.txt", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("bar bar2", p1.equal);

        assertEquals("128g ", p1.define.get("Xms"));
        assertEquals("512g m", p1.define.get("Xmx"));

        parser.populateObject("test -fX -ebar -Df=g /tmp/file.txt\\ ", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertTrue(p1.foo);
        assertTrue(p1.enableX);
        assertEquals("bar", p1.equal);
        assertEquals("g", p1.define.get("f"));
        assertEquals("/tmp/file.txt ", p1.arguments.get(0));

        parser.populateObject("test -ebar -Df=g /tmp/file.txt", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("bar", p1.equal);
        assertEquals("g", p1.define.get("f"));
        assertEquals("/tmp/file.txt", p1.arguments.get(0));
    }

    @Test
    public void testParseCommandLine2() throws Exception {

        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new Parser2Test<>()).getParser();
        Parser2Test p2 = (Parser2Test) parser.getCommand();

        parser.populateObject("test -d true --bar Foo.class", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("true", p2.display);
        assertNull(p2.version);
        assertEquals("Foo.class", p2.bar);
        assertNull(p2.arguments);

        parser.populateObject("test -V verbose -d false -b com.bar.Bar.class /tmp/file\\ foo.txt /tmp/bah.txt", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("verbose", p2.version);
        assertEquals("false", p2.display);
        assertEquals("com.bar.Bar.class", p2.bar);
        assertEquals("/tmp/file foo.txt", p2.arguments.get(0));
        assertEquals("/tmp/bah.txt", p2.arguments.get(1));

        assertTrue(parser.getProcessedCommand().parserExceptions().isEmpty());

        parser.populateObject("test -Vverbose -dfalse -bcom.bar.Bar.class /tmp/file\\ foo.txt /tmp/bah.txt", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
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
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new GroupCommandTest<>()).getParser();
        ChildTest1 c1 = (ChildTest1) parser.getChildParser("child1").getCommand();
        ChildTest2 c2 = (ChildTest2) parser.getChildParser("child2").getCommand();

        parser.populateObject("group child1 --foo BAR", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("BAR", c1.foo);

        parser.populateObject("group child1 --foo BAR --bar FOO", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("BAR", c1.foo);
        assertEquals("FOO", c1.bar);

        parser.populateObject("group child2 --foo BAR --bar FOO", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertNull(c1.foo);
        assertNull(c1.bar);
        assertEquals("BAR", c2.foo);
        assertEquals("FOO", c2.bar);
    }

    @Test
    public void testParseSuperGroupCommand() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> superParser = new AeshCommandContainerBuilder<>().create(new SuperGroupCommandTest<>()).getParser();
        CommandLineParser subSuperParser = superParser.getChildParser("sub");

        ChildTest1 c1 = (ChildTest1) subSuperParser.getChildParser("child1").getCommand();
        ChildTest2 c2 = (ChildTest2) subSuperParser.getChildParser("child2").getCommand();

        superParser.populateObject("super sub child1 --foo BAR", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("BAR", c1.foo);

        superParser.populateObject("super sub child1 --foo BAR --bar FOO", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("BAR", c1.foo);
        assertEquals("FOO", c1.bar);

        superParser.populateObject("super sub child2 --foo BAR --bar FOO", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertNull(c1.foo);
        assertNull(c1.bar);
        assertEquals("BAR", c2.foo);
        assertEquals("FOO", c2.bar);
    }

    @Test
    public void testParseCommandLine4() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new Parser4Test<>()).getParser();
        Parser4Test p4 = (Parser4Test) parser.getCommand();

        parser.populateObject("test -o bar1,bar2,bar3 foo", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("bar1", p4.option.get(0));
        assertEquals("bar3", p4.option.get(2));
        assertEquals(3, p4.option.size());

        parser.populateObject("test -o=bar1,bar2,bar3 foo", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("bar1", p4.option.get(0));
        assertEquals("bar3", p4.option.get(2));
        assertEquals(3, p4.option.size());
        assertEquals("foo", p4.arguments.get(0));

        parser.populateObject("test --option=bar1,bar2,bar3 foo", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("bar1", p4.option.get(0));
        assertEquals("bar3", p4.option.get(2));
        assertEquals(3, p4.option.size());
        assertEquals("foo", p4.arguments.get(0));

        parser.populateObject("test --help bar4:bar5:bar6 foo", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
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
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new Parser5Test<>()).getParser();
        Parser5Test p5 = (Parser5Test) parser.getCommand();

        parser.populateObject("test  --foo  \"-X1 X2 -X3\" --baz -wrong --bar -q \"-X4 -X5\"", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("-X1", p5.foo.get(0));
        assertEquals("X2", p5.foo.get(1));
        assertEquals("-X3", p5.foo.get(2));
        assertTrue(p5.baz);
        assertTrue(p5.bar);
        assertEquals(2, p5.qux.size());
        assertEquals("-X4", p5.qux.get(0));
        assertEquals("-X5", p5.qux.get(1));


        parser.populateObject("test  --foo  -X1 X2 -X3 --baz -wrong --bar -q -X4 -X5", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
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
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new Parser4Test<>()).getParser();
        Parser4Test p4 = (Parser4Test) parser.getCommand();

        parser.populateObject("test -- foo", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("foo", p4.arguments.get(0));
    }

    @Test
    public void testDisableParsing() throws Exception {
        AeshContext aeshContext = SettingsBuilder.builder().build().aeshContext();
        CommandLineParser<CommandInvocation> parser = new AeshCommandContainerBuilder<>().create(new DisableParsingCommand()).getParser();
        DisableParsingCommand disableParsing = (DisableParsingCommand) parser.getCommand();

        parser.populateObject("test --foo bar he --yay boo \"one two three\"", invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        assertEquals("--foo", disableParsing.args.get(0));
        assertEquals("bar", disableParsing.args.get(1));
        assertEquals("he", disableParsing.args.get(2));
        assertEquals("--yay", disableParsing.args.get(3));
        assertEquals("boo", disableParsing.args.get(4));
        assertEquals("one two three", disableParsing.args.get(5));
    }

    @CommandDefinition(name = "test", description = "a simple test", aliases = {"toto"})
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
        private Map<String,String> define;

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

    @CommandDefinition(name = "test", description = "this is a command without options")
    public class Parser3Test extends TestingCommand {}

    @CommandDefinition(name = "test", description = "testing multiple values")
    public class Parser4Test<CI extends CommandInvocation>  extends TestingCommand<CI> {
        @OptionList(shortName = 'o', name="option", valueSeparator = ',')
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
    public class Parser5Test<CI extends CommandInvocation>  extends TestingCommand<CI> {
        @OptionList(shortName = 'f', name="foo", valueSeparator=' ')
        private List<String> foo;

        @Option(shortName = 'b', name="bar", hasValue = false)
        private Boolean bar;

        @Option(shortName = 'z', name="baz", hasValue = false)
        private Boolean baz;

        @OptionList(shortName = 'q', name="qux", valueSeparator=' ')
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

    @GroupCommandDefinition(name = "group", description = "", groupCommands = {ChildTest1.class, ChildTest2.class})
    public class GroupCommandTest<CI extends CommandInvocation> extends TestingCommand<CI> {

        @Option(hasValue = false)
        private boolean help;

    }

    @GroupCommandDefinition(name = "super", description = "", groupCommands = {SubSuperGroupCommandTest.class})
    public class SuperGroupCommandTest<CI extends CommandInvocation> extends TestingCommand<CI> {

        @Option(hasValue = false)
        private boolean help;

    }

    @GroupCommandDefinition(name = "sub", description = "", groupCommands = {ChildTest1.class, ChildTest2.class})
    public class SubSuperGroupCommandTest extends TestingCommand<CommandInvocation> {

        @Option(hasValue = false)
        private boolean help;

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

    @GroupCommandDefinition(name = "groupfail", description = "", groupCommands = {ChildTest1.class})
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

}
