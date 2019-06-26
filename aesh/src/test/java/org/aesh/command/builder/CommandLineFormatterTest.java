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
package org.aesh.command.builder;

import org.aesh.AeshConsoleRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.parser.CommandLineParserBuilder;
import org.aesh.tty.TestConnection;
import org.aesh.terminal.utils.ANSI;
import org.junit.Test;

import static org.aesh.terminal.utils.Config.getLineSeparator;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLineFormatterTest {


    @Test
    public void formatter() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb =
                ProcessedCommandBuilder.builder().name("man").description("[OPTION...]");

        pb.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('d')
                        .name("debug")
                        .description("emit debugging messages")
                        .type(String.class)
                        .build());

        pb.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('D')
                        .name("default")
                        .description("reset all options to their default values")
                        .type(String.class)
                        .build());

        CommandLineParser clp = CommandLineParserBuilder.builder()
                .processedCommand(pb.create())
                .create();

        assertEquals("Usage: man [<options>]" + getLineSeparator() + "[OPTION...]"+ getLineSeparator()+
                        getLineSeparator()+
                        "Options:"+ getLineSeparator()+
                        "  -d, --debug    emit debugging messages"+ getLineSeparator()+
                        "  -D, --default  reset all options to their default values"+ getLineSeparator(),
                clp.printHelp());
    }

    @Test
    public void formatter2() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb =
                ProcessedCommandBuilder.builder().name("man").description("[OPTION...]");

        pb.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('d')
                        .name("debug")
                        .description("emit debugging messages")
                        .type(String.class)
                        .build());

        pb.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('D')
                        .name("default")
                        .required(true)
                        .description("reset all options to their default values")
                        .type(String.class)
                        .build()
        );

        pb.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('f')
                        .name("file")
                        .hasValue(true)
                        .argument("filename")
                        .description("set the filename")
                        .type(String.class)
                        .build());


        CommandLineParser clp = CommandLineParserBuilder.builder().processedCommand(pb.create()).create();

        assertEquals("Usage: man [<options>]" + getLineSeparator() + "[OPTION...]"+ getLineSeparator()+
                        getLineSeparator()+
                        "Options:"+ getLineSeparator()+
                        "  -d, --debug            emit debugging messages"+ getLineSeparator()+
                        ANSI.BOLD+
                        "  -D, --default"+
                        ANSI.BOLD_OFF+
                        "          reset all options to their default values"+ getLineSeparator()+
                        "  -f, --file=<filename>  set the filename"+ getLineSeparator(),
                clp.printHelp());
    }

    @Test
    public void groupFormatter() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> git =
                ProcessedCommandBuilder.builder().name("git").description("[OPTION...]");
        git.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('h')
                        .name("help")
                        .description("display help info")
                        .type(boolean.class)
                        .build()
        );

        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> rebase =
                ProcessedCommandBuilder.builder().name("rebase").description("[OPTION...]");
        rebase.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('f')
                        .name("foo")
                        .required(true)
                        .description("reset all options to their default values")
                        .type(String.class)
                        .build()
        );

        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> branch =
                ProcessedCommandBuilder.builder().name("branch").description("branching");
        branch.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('b')
                        .name("bar")
                        .required(true)
                        .description("reset all options to their default values")
                        .type(String.class)
                        .build()
        );


        CommandLineParser<CommandInvocation> clpGit = CommandLineParserBuilder.builder().processedCommand(git.create()).create();
        CommandLineParser<CommandInvocation> clpBranch = CommandLineParserBuilder.builder().processedCommand(branch.create()).create();
        CommandLineParser<CommandInvocation> clpRebase = CommandLineParserBuilder.builder().processedCommand(rebase.create()).create();

        clpGit.updateAnsiMode(false);
        clpBranch.updateAnsiMode(false);
        clpRebase.updateAnsiMode(false);

        clpGit.addChildParser(clpBranch);
        clpGit.addChildParser(clpRebase);

         assertEquals("Usage: git [<options>]" + getLineSeparator() + "[OPTION...]" + getLineSeparator() +
                         getLineSeparator() +
                         "Options:" + getLineSeparator() +
                         "  -h, --help  display help info" + getLineSeparator()
                         + getLineSeparator()+"git commands:"+ getLineSeparator()+
                         "    " + "branch" + "  branching" + getLineSeparator() +
                         "    " + "rebase" + "  [OPTION...]"+ getLineSeparator(),
                 clpGit.printHelp());


    }


    @Test
    public void testChildFormatter() throws InterruptedException {
        TestConnection connection = new TestConnection();

        AeshConsoleRunner runner = AeshConsoleRunner.builder().command(BaseCommand.class).connection(connection);
        runner.start();

        connection.read("base git rebase --help"+ getLineSeparator());
        connection.clearOutputBuffer();
        Thread.sleep(10);
        connection.assertBuffer("Usage: base git rebase [<options>] <branch>"+ getLineSeparator()+
                                       "Reapply commits on top of another base tip"+ getLineSeparator()+
                                        getLineSeparator()+
                                        "Options:"+ getLineSeparator()+
                                        "  --force  force your commits"+getLineSeparator()+
                                        "  --help   display this help info"+getLineSeparator()+
                                        "  --test"+getLineSeparator()+
                                        getLineSeparator()+
                                        "Argument:"+getLineSeparator()+
                                        "         the branch you want to rebase on"+getLineSeparator()+getLineSeparator());

        runner.stop();
    }

    @Test
    public void testChildFormatter2() throws InterruptedException {
        TestConnection connection = new TestConnection();

        AeshConsoleRunner runner = AeshConsoleRunner.builder().command(BaseCommand.class).connection(connection);
        runner.start();

        connection.read("base git checkout --help"+ getLineSeparator());
        connection.clearOutputBuffer();
        Thread.sleep(10);
        connection.assertBuffer("Usage: base git checkout [<options>] <branch>"+ getLineSeparator()+
                                       "Switch branches or restore working tree files"+ getLineSeparator()+
                                        getLineSeparator()+
                                        "Options:"+ getLineSeparator()+
                                        "  --quiet  Suppress feedback messages"+getLineSeparator()+
                                        "  --force  Proceed even if the index or the working tree differs from HEAD"+getLineSeparator()+
                                        "  --help   display this help info"+getLineSeparator()+
                                        "  --test"+getLineSeparator()+
                                        getLineSeparator()+
                                        "Argument:"+getLineSeparator()+
                                        "         the branch you want to checkout"+getLineSeparator()+getLineSeparator());

        runner.stop();
    }

    @GroupCommandDefinition(name = "base", description = "", groupCommands = {GitCommand.class})
    public static class BaseCommand implements Command {

        @Option(hasValue = false)
        private boolean help;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }


    @GroupCommandDefinition(name = "git", description = "", groupCommands = {GitCommit.class, GitRebase.class, GitCheckout.class})
    public static class GitCommand implements Command {

        @Option(hasValue = false)
        private boolean help;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "commit", description = "")
    public static class GitCommit implements Command {

        @Option(shortName = 'a', hasValue = false)
        private boolean all;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "rebase", description = "Reapply commits on top of another base tip")
    public static class GitRebase implements Command {

        @Option(hasValue = false, description = "force your commits")
        private boolean force;

        @Option(hasValue = false, description = "display this help info")
        private boolean help;

        @Option
        private String test;

        @Argument(description = "the branch you want to rebase on")
        private String branch;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            if(help)
                commandInvocation.println(commandInvocation.getHelpInfo("base git rebase"));

            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "checkout", description = "Switch branches or restore working tree files")
    public static class GitCheckout implements Command {

        @Option(hasValue = false, description = "Suppress feedback messages")
        private boolean quiet;

        @Option(hasValue = false, description = "Proceed even if the index or the working tree differs from HEAD")
        private boolean force;

        @Option(hasValue = false, description = "display this help info")
        private boolean help;

        @Option
        private String test;

        @Argument(description = "the branch you want to checkout")
        private String branch;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            if(help)
                commandInvocation.println(commandInvocation.getHelpInfo());

            return CommandResult.SUCCESS;
        }
    }

}
