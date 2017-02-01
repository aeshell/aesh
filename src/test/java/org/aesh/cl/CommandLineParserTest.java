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
package org.aesh.cl;

import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.parser.CommandLine;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.CommandDefinition;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.impl.parser.CommandLineParserException;
import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionGroup;
import org.aesh.command.option.OptionList;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import org.aesh.command.CommandException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLineParserTest {

    @Test
    public void testParseCommandLine1() throws CommandLineParserException {

        CommandLineParser<Parser1Test> parser = new AeshCommandContainerBuilder<Parser1Test>().create(Parser1Test.class).getParser();

        CommandLine<Parser1Test> cl = parser.parse("test -f -e bar -Df=g /tmp/file.txt");
        assertEquals("f", cl.getOptions().get(0).shortName());
        assertEquals("e", cl.getOptions().get(1).shortName());
        assertEquals("/tmp/file.txt", cl.getArgument().getValues().get(0));

        cl = parser.parse("test -f -e=bar -Df=g /tmp/file.txt");
        assertEquals("f", cl.getOptions().get(0).shortName());
        assertEquals("e", cl.getOptions().get(1).shortName());
        assertEquals("/tmp/file.txt", cl.getArgument().getValues().get(0));

        cl = parser.parse("test -f -Df=g /tmp/file.txt -e=bar foo bar");
        assertEquals("f", cl.getOptions().get(0).shortName());
        assertEquals("D", cl.getOptions().get(1).shortName());
        assertEquals("e", cl.getOptions().get(2).shortName());
        assertEquals("/tmp/file.txt", cl.getArgument().getValues().get(0));
        assertEquals("foo", cl.getArgument().getValues().get(1));
        assertEquals("bar", cl.getArgument().getValues().get(2));

        cl = parser.parse("test -e bar -DXms=128m -DXmx=512m --X /tmp/file.txt");
        assertEquals("e", cl.getOptions().get(0).shortName());
        assertEquals("bar", cl.getOptions().get(0).getValue());
        assertEquals("/tmp/file.txt", cl.getArgument().getValues().get(0));
        assertNotNull(cl.hasOption("X"));

        Map<String,String> properties = cl.getOptionProperties("D");
        assertEquals("128m", properties.get("Xms"));
        assertEquals("512m", properties.get("Xmx"));

        cl = parser.parse("test -e=bar -DXms=128m -DXmx=512m /tmp/file.txt");
        assertEquals("e", cl.getOptions().get(0).shortName());
        assertEquals("bar", cl.getOptions().get(0).getValue());

        cl = parser.parse("toto -e=bar -DXms=128m -DXmx=512m /tmp/file.txt");
        assertEquals("e", cl.getOptions().get(0).shortName());
        assertEquals("bar", cl.getOptions().get(0).getValue());

        cl = parser.parse("test --equal=bar -DXms=128m -DXmx=512m /tmp/file.txt");
        assertEquals("e", cl.getOptions().get(0).shortName());
        assertEquals("equal", cl.getOptions().get(0).name());
        assertEquals("bar", cl.getOptions().get(0).getValue());

        cl = parser.parse("test --equal \"bar bar2\" -DXms=\"128g \" -DXmx=512g\\ m /tmp/file.txt");
        assertEquals("bar bar2", cl.getOptionValue("equal"));

        assertTrue(cl.getOptionProperties("D").containsKey("Xms"));
        assertEquals("128g ", cl.getOptionProperties("D").get("Xms"));
        assertTrue(cl.getOptionProperties("D").containsKey("Xmx"));
        assertEquals("512g m", cl.getOptionProperties("D").get("Xmx"));

        cl = parser.parse("test -fX -e bar -Df=g /tmp/file.txt\\ ");
        assertEquals("f", cl.getOptions().get(0).shortName());
        assertEquals("X", cl.getOptions().get(1).shortName());
        assertEquals("e", cl.getOptions().get(2).shortName());
        assertEquals("D", cl.getOptions().get(3).shortName());
        assertEquals("/tmp/file.txt ", cl.getArgument().getValues().get(0));
        assertFalse(cl.hasParserError());

        cl = parser.parse("test -f -e bar -Df=g -X");
        assertEquals("f", cl.getOptions().get(0).shortName());
        assertEquals("e", cl.getOptions().get(1).shortName());
        assertEquals("D", cl.getOptions().get(2).shortName());
        assertEquals("X", cl.getOptions().get(3).shortName());
        assertEquals("true", cl.getOptionValue('X'));
        assertFalse(cl.hasParserError());

        cl = parser.parse("test -fXe -Df=g /tmp/file.txt");
        assertEquals("f", cl.getOptions().get(0).shortName());
        assertEquals("X", cl.getOptions().get(1).shortName());
        assertEquals("D", cl.getOptions().get(2).shortName());
        assertEquals("/tmp/file.txt", cl.getArgument().getValues().get(0));
        assertTrue(cl.hasParserError());

        cl = parser.parse("test -a /tmp/file.txt");
        assertTrue(cl.hasParserError());

        cl = parser.parse("test -a /tmp/file.txt");
        assertTrue(cl.hasParserError());
        cl.getArgument();

        cl = parser.parse("test -e bar --equal bar2 -DXms=128m -DXmx=512m /tmp/file.txt");
        assertTrue(cl.hasParserError());
        cl.getArgument();

        cl = parser.parse("test -f -Dfoo:bar /tmp/file.txt");
        assertTrue(cl.hasParserError());
        cl.getArgument();

        cl = parser.parse("test -f foobar /tmp/file.txt");
        assertTrue(cl.hasParserError());
        cl.getArgument();

    }

    @Test
    public void testParseCommandLine2() throws CommandLineParserException {

        CommandLineParser<Parser2Test> parser = new AeshCommandContainerBuilder<Parser2Test>().create(Parser2Test.class).getParser();

        CommandLine cl = parser.parse("test -d true --bar Foo.class");
        assertTrue(cl.hasOption('d'));
        assertFalse(cl.hasOption('V'));
        assertEquals("Foo.class", cl.getOptionValue("bar"));
        assertNotNull(cl.getArgument());

        cl = parser.parse("test -V verbose -d false -b com.bar.Bar.class /tmp/file\\ foo.txt /tmp/bah.txt");
        assertTrue(cl.hasOption('V'));
        assertTrue(cl.hasOption('d'));
        assertTrue(cl.hasOption('b'));
        assertEquals("com.bar.Bar.class", cl.getOptionValue("b"));
        assertEquals("/tmp/file foo.txt", cl.getArgument().getValues().get(0));
        assertEquals("/tmp/bah.txt", cl.getArgument().getValues().get(1));

        cl = parser.parse("test -d /tmp/file.txt");
        assertTrue(cl.hasParserError());
        cl.getArgument();

    }

    @Test
    public void testParseGroupCommand() throws CommandLineParserException {
        CommandLineParser<GroupCommandTest> parser = new AeshCommandContainerBuilder<GroupCommandTest>().create(GroupCommandTest.class).getParser();

        CommandLine cl = parser.parse("group child1 --foo BAR");
        assertTrue(cl.hasOption("foo"));
        assertEquals("BAR", cl.getOptionValue("foo"));

        cl = parser.parse("group child1 --foo BAR --bar FOO");
        assertTrue(cl.hasOption("foo"));
        assertTrue(cl.hasOption("bar"));
        assertEquals("BAR", cl.getOptionValue("foo"));
        assertEquals("FOO", cl.getOptionValue("bar"));
    }

    public void testParseCommandLine3() {
        /*
        try {
            ParserGenerator.generateParser(Parser3Test.class);
            assertTrue(false);
        }
        catch (CommandLineParserException iae) {
            assertTrue(true);
        }
        */
    }

    @Test
    public void testParseCommandLine4() throws CommandLineParserException {
        CommandLineParser clp = new AeshCommandContainerBuilder<Parser4Test>().create(Parser4Test.class).getParser();

        CommandLine cl = clp.parse("test -o bar1,bar2,bar3 foo");
        assertTrue(cl.hasOption('o'));
        assertEquals("bar1", cl.getOptionValues("o").get(0));
        assertEquals("bar3", cl.getOptionValues("o").get(2));
        assertEquals(3, cl.getOptionValues("o").size());

        cl = clp.parse("test -o=bar1,bar2,bar3 foo");
        assertTrue(cl.hasOption('o'));
        assertEquals("bar1", cl.getOptionValues("o").get(0));
        assertEquals("bar3", cl.getOptionValues("o").get(2));
        assertEquals(3, cl.getOptionValues("o").size());

        cl = clp.parse("test --option=bar1,bar2,bar3 foo");
        assertTrue(cl.hasOption('o'));
        assertEquals("bar1", cl.getOptionValues("o").get(0));
        assertEquals("bar3", cl.getOptionValues("o").get(2));
        assertEquals(3, cl.getOptionValues("o").size());

        cl = clp.parse("test --help bar4:bar5:bar6 foo");
        assertTrue(cl.hasOption("help"));
        assertEquals("bar4", cl.getOptionValues("help").get(0));
        assertEquals("bar6", cl.getOptionValues("h").get(2));

        cl = clp.parse("test --help2 bar4 bar5 bar6");
        assertTrue(cl.hasOption("help2"));
        assertEquals("bar4", cl.getOptionValues("help2").get(0));
        assertEquals("bar6", cl.getOptionValues("e").get(2));

        cl = clp.parse("test --bar 1,2,3");
        assertTrue(cl.hasOption("bar"));
        assertEquals(Integer.class, cl.getOption("bar").type());
    }

    @Test
    public void testParseCommandLine5() throws CommandLineParserException {
        CommandLineParser<Parser5Test> clp = new AeshCommandContainerBuilder<Parser5Test>().create(Parser5Test.class).getParser();

        CommandLine cl = clp.parse("test  --foo  \"-X1 X2 -X3\" --baz -wrong --bar -q \"-X4 -X5\"",true);
        assertTrue(cl.hasOption("foo"));
        assertEquals(3, cl.getOptionValues("foo").size());
        assertEquals("-X1", cl.getOptionValues("foo").get(0));
        assertEquals("X2", cl.getOptionValues("foo").get(1));
        assertEquals("-X3", cl.getOptionValues("foo").get(2));
        assertTrue(cl.hasOption("bar"));
        assertTrue(cl.hasOption("baz"));
        assertFalse(cl.hasOption("wrong"));
        assertTrue(cl.hasOption("qux"));
        assertEquals(2, cl.getOptionValues("qux").size());
        assertEquals("-X4", cl.getOptionValues("qux").get(0));
        assertEquals("-X5", cl.getOptionValues("qux").get(1));


        cl = clp.parse("test  --foo -X1 X2 -X3 --baz -wrong --bar -q -X4 -X5",true);
        assertTrue(cl.hasOption("foo"));
        assertEquals(3, cl.getOptionValues("foo").size());
        assertEquals("-X1", cl.getOptionValues("foo").get(0));
        assertEquals("X2", cl.getOptionValues("foo").get(1));
        assertEquals("-X3", cl.getOptionValues("foo").get(2));
        assertTrue(cl.hasOption("bar"));
        assertTrue(cl.hasOption("baz"));
        assertFalse(cl.hasOption("wrong"));
        assertTrue(cl.hasOption("qux"));
        assertEquals(2, cl.getOptionValues("qux").size());
        assertEquals("-X4", cl.getOptionValues("qux").get(0));
        assertEquals("-X5", cl.getOptionValues("qux").get(1));

    }

    @Test
    public void testSubClass() throws CommandLineParserException {
        CommandLineParser<SubHelp> clp = new AeshCommandContainerBuilder<SubHelp>().create(SubHelp.class).getParser();

        CommandLine cl = clp.parse("subhelp --foo bar -h",true);
        assertTrue(cl.hasOption("foo"));
        assertTrue(cl.hasOption("h"));
        assertEquals("bar", cl.getOptionValue("foo"));
    }

    @CommandDefinition(name = "test", description = "a simple test", aliases = {"toto"})
    public class Parser1Test extends TestingCommand {

        @Option(shortName = 'X', name = "X", description = "enable X", hasValue = false)
        private Boolean enableX;

        @Option(shortName = 'f', name = "foo", description = "enable foo", hasValue = false)
        private Boolean foo;

        @Option(shortName = 'e', name = "equal", description = "enable equal", required = true)
        private String equal;

        @OptionGroup(shortName = 'D', description = "define properties", required = true)
        private Map<String,String> define;

        @Arguments
        private List<String> arguments;
    }

    @CommandDefinition(name = "test", description = "more [options] file...")
    public class Parser2Test extends TestingCommand {
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
    public class Parser4Test  extends TestingCommand{
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
    public class Parser5Test  extends TestingCommand{
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
    public class ChildTest1 extends TestingCommand {

        @Option
        private String foo;

        @Option
        private String bar;

    }

    @GroupCommandDefinition(name = "group", description = "", groupCommands = {ChildTest1.class})
    public class GroupCommandTest extends TestingCommand {

        @Option(hasValue = false)
        private boolean help;

    }

    public class TestingCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }


    public class HelpClass {

        @Option(name = "help", shortName = 'h', hasValue = false)
        private boolean help;
    }

    @CommandDefinition(name = "subhelp", description = "")
    public class SubHelp extends HelpClass implements Command {

        @Option
        private String foo;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }
}
