/*
 * Copyright 2019 Red Hat, Inc.
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
package org.aesh.util.completer;

import static org.junit.Assert.assertTrue;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.container.CommandContainerBuilder;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.parser.CommandLineParserException;
import org.junit.Test;

/**
 * @author Aesh team
 */
public class FileCompleterGeneratorTest {

    @Test
    public void testSimpleCommand() {

        CommandLineParser<CommandInvocation> parser = getParser(TestCommand1.class);

        FileCompleterGenerator completerGenerator = new FileCompleterGenerator();

        String out = completerGenerator.generateCompleterFile(parser);

        assertTrue(out.contains("_complete_test1"));
        assertTrue(out.contains("NO_VALUE_OPTIONS=\"--help -h \""));
        assertTrue(out.contains("VALUE_OPTIONS=\"--override -o --test -t \""));
    }

    @Test
    public void testGroupCommand() {
        CommandLineParser<CommandInvocation> parser = getParser(GutCommand1.class);

        FileCompleterGenerator completerGenerator = new FileCompleterGenerator();

        String out = completerGenerator.generateCompleterFile(parser);

        assertTrue(out.contains("_complete_gut"));
        assertTrue(out.contains("_command_gut"));
        assertTrue(out.contains("_command_help"));
        assertTrue(out.contains("_command_rebase"));
        assertTrue(out.contains("ArrContains"));
        assertTrue(out.contains("ArrContains COMP_WORDS CHILD0 && { _command_help; return $?; }"));

    }

    private CommandLineParser<CommandInvocation> getParser(Class<? extends Command> clazz) {
        if (clazz != null) {
            CommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
            try {
                CommandContainer<CommandInvocation> container = builder.create(clazz);
                return container.getParser();
            } catch (CommandLineParserException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @CommandDefinition(name = "test1", description = "")
    public static class TestCommand1 implements Command {

        @Option(shortName = 'h', hasValue = false)
        private boolean help;

        @Option(shortName = 'o')
        private boolean override;

        @Option(shortName = 't', defaultValue = { "FOO", "BAR" })
        private String test;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "gut", groupCommands = { HelpCommand1.class, RebaseCommand1.class }, description = "")
    public static class GutCommand1 implements Command {

        @Option(shortName = 'h', hasValue = false)
        private boolean help;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "help", description = "")
    public static class HelpCommand1 implements Command {

        @Option(shortName = 'h', hasValue = false)
        private boolean help;

        @Option(shortName = 't', defaultValue = { "FOO", "BAR" })
        private String test;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "rebase", description = "")
    public static class RebaseCommand1 implements Command {

        @Option(shortName = 'h', hasValue = false)
        private boolean help;

        @Option(shortName = 't', defaultValue = { "FOO", "BAR" })
        private String test;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

}
