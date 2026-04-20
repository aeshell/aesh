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
package org.aesh.command.builder;

import static org.aesh.terminal.utils.Config.getLineSeparator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.aesh.AeshConsoleRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.parser.AeshCommandLineParser;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.terminal.utils.ANSI;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * @author Aesh team
 */
public class CommandLineFormatterTest {

    @Test
    public void formatter() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("man").description("[OPTION...]");

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

        CommandLineParser clp = new AeshCommandLineParser<>(pb.create());

        assertEquals("Usage: man [<options>]" + getLineSeparator() + "[OPTION...]" + getLineSeparator() +
                getLineSeparator() +
                "Options:" + getLineSeparator() +
                "  -d, --debug    emit debugging messages" + getLineSeparator() +
                "  -D, --default  reset all options to their default values" + getLineSeparator(),
                clp.printHelp());
    }

    @Test
    public void formatter2() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("man").description("[OPTION...]");

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
                        .build());

        pb.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('f')
                        .name("file")
                        .hasValue(true)
                        .argument("filename")
                        .description("set the filename")
                        .type(String.class)
                        .build());

        CommandLineParser clp = new AeshCommandLineParser<>(pb.create());

        assertEquals("Usage: man [<options>]" + getLineSeparator() + "[OPTION...]" + getLineSeparator() +
                getLineSeparator() +
                "Options:" + getLineSeparator() +
                "  -d, --debug            emit debugging messages" + getLineSeparator() +
                ANSI.BOLD +
                "  -D, --default" +
                ANSI.BOLD_OFF +
                "          reset all options to their default values" + getLineSeparator() +
                "  -f, --file=<filename>  set the filename" + getLineSeparator(),
                clp.printHelp());
    }

    @Test
    public void groupFormatter() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> git = ProcessedCommandBuilder.builder()
                .name("git").description("[OPTION...]");
        git.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('h')
                        .name("help")
                        .description("display help info")
                        .type(boolean.class)
                        .build());

        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> rebase = ProcessedCommandBuilder.builder()
                .name("rebase").description("[OPTION...]");
        rebase.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('f')
                        .name("foo")
                        .required(true)
                        .description("reset all options to their default values")
                        .type(String.class)
                        .build());

        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> branch = ProcessedCommandBuilder.builder()
                .name("branch").description("branching");
        branch.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('b')
                        .name("bar")
                        .required(true)
                        .description("reset all options to their default values")
                        .type(String.class)
                        .build());

        CommandLineParser<CommandInvocation> clpGit = new AeshCommandLineParser<>(git.create());
        CommandLineParser<CommandInvocation> clpBranch = new AeshCommandLineParser<>(branch.create());
        CommandLineParser<CommandInvocation> clpRebase = new AeshCommandLineParser<>(rebase.create());

        clpGit.updateAnsiMode(false);
        clpBranch.updateAnsiMode(false);
        clpRebase.updateAnsiMode(false);

        clpGit.addChildParser(clpBranch);
        clpGit.addChildParser(clpRebase);

        assertEquals("Usage: git [<options>]" + getLineSeparator() + "[OPTION...]" + getLineSeparator() +
                getLineSeparator() +
                "Options:" + getLineSeparator() +
                "  -h, --help  display help info" + getLineSeparator()
                + getLineSeparator() + "git commands:" + getLineSeparator() +
                "    " + "branch" + "  branching" + getLineSeparator() +
                "    " + "rebase" + "  [OPTION...]" + getLineSeparator(),
                clpGit.printHelp());

    }

    @Test
    public void testChildFormatter() throws InterruptedException {
        TestConnection connection = new TestConnection();

        AeshConsoleRunner runner = AeshConsoleRunner.builder().command(BaseCommand.class).connection(connection);
        runner.start();

        connection.read("base git rebase --help" + getLineSeparator());
        connection.clearOutputBuffer();
        Thread.sleep(10);
        connection.assertBuffer("Usage: base git rebase [<options>] <branch>" + getLineSeparator() +
                "Reapply commits on top of another base tip" + getLineSeparator() +
                getLineSeparator() +
                "Options:" + getLineSeparator() +
                "  --force  force your commits" + getLineSeparator() +
                "  --help   display this help info" + getLineSeparator() +
                "  --test" + getLineSeparator() +
                getLineSeparator() +
                "Argument:" + getLineSeparator() +
                "         the branch you want to rebase on" + getLineSeparator() + getLineSeparator());

        runner.stop();
    }

    @Test
    public void testChildFormatter2() throws InterruptedException {
        TestConnection connection = new TestConnection();

        AeshConsoleRunner runner = AeshConsoleRunner.builder().command(BaseCommand.class).connection(connection);
        runner.start();

        connection.read("base git checkout --help" + getLineSeparator());
        connection.clearOutputBuffer();
        Thread.sleep(10);
        connection.assertBuffer("Usage: base git checkout [<options>] <branch>" + getLineSeparator() +
                "Switch branches or restore working tree files" + getLineSeparator() +
                getLineSeparator() +
                "Options:" + getLineSeparator() +
                "  --quiet  Suppress feedback messages" + getLineSeparator() +
                "  --force  Proceed even if the index or the working tree differs from HEAD" + getLineSeparator() +
                "  --help   display this help info" + getLineSeparator() +
                "  --test" + getLineSeparator() +
                getLineSeparator() +
                "Argument:" + getLineSeparator() +
                "         the branch you want to checkout" + getLineSeparator() + getLineSeparator());

        runner.stop();
    }

    @Test
    public void testHelpGrouping() throws CommandLineParserException {
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("myapp").description("My application");

        pb.addOption(ProcessedOptionBuilder.builder()
                .name("json").description("Output as JSON").type(boolean.class)
                .helpGroup("Output Format").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("xml").description("Output as XML").type(boolean.class)
                .helpGroup("Output Format").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("user").description("Username").type(String.class)
                .helpGroup("Authentication").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("password").description("Password").type(String.class)
                .helpGroup("Authentication").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("verbose").description("Verbose output").type(boolean.class)
                .build());

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pb.create());

        String help = clp.printHelp();

        // Named groups appear before default "Options:"
        int outputFormatIdx = help.indexOf("Output Format:");
        int authIdx = help.indexOf("Authentication:");
        int optionsIdx = help.indexOf("Options:");

        assertTrue("Output Format group should appear", outputFormatIdx > 0);
        assertTrue("Authentication group should appear", authIdx > 0);
        assertTrue("Options group should appear", optionsIdx > 0);
        assertTrue("Output Format should appear before Authentication", outputFormatIdx < authIdx);
        assertTrue("Authentication should appear before Options", authIdx < optionsIdx);

        // Verify options are under their groups
        assertTrue("--json should appear after Output Format", help.indexOf("--json") > outputFormatIdx);
        assertTrue("--xml should appear after Output Format", help.indexOf("--xml") > outputFormatIdx);
        assertTrue("--json should appear before Authentication", help.indexOf("--json") < authIdx);
        assertTrue("--user should appear after Authentication", help.indexOf("--user") > authIdx);
        assertTrue("--password should appear after Authentication", help.indexOf("--password") > authIdx);
        assertTrue("--verbose should appear after Options", help.indexOf("--verbose") > optionsIdx);
    }

    @Test
    public void testHelpGroupingNoGroups() throws CommandLineParserException {
        // When no helpGroup is set, all options appear under "Options:" as before
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("simple").description("Simple command");

        pb.addOption(ProcessedOptionBuilder.builder()
                .name("foo").description("Foo option").type(String.class).build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("bar").description("Bar option").type(String.class).build());

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pb.create());

        assertEquals("Usage: simple [<options>]" + getLineSeparator() + "Simple command" + getLineSeparator() +
                getLineSeparator() +
                "Options:" + getLineSeparator() +
                "  --foo  Foo option" + getLineSeparator() +
                "  --bar  Bar option" + getLineSeparator(),
                clp.printHelp());
    }

    @Test
    public void testHelpGroupingAllGrouped() throws CommandLineParserException {
        // When all options have a helpGroup, no default "Options:" section appears
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> pb = ProcessedCommandBuilder.builder()
                .name("allgrouped").description("All grouped");

        pb.addOption(ProcessedOptionBuilder.builder()
                .name("alpha").description("Alpha").type(String.class)
                .helpGroup("Group A").build());
        pb.addOption(ProcessedOptionBuilder.builder()
                .name("beta").description("Beta").type(String.class)
                .helpGroup("Group B").build());

        CommandLineParser<CommandInvocation> clp = new AeshCommandLineParser<>(pb.create());

        String help = clp.printHelp();
        assertTrue("Group A should appear", help.contains("Group A:"));
        assertTrue("Group B should appear", help.contains("Group B:"));
        assertTrue("Default Options: should not appear when all options are grouped",
                !help.contains("Options:"));
    }

    @Test
    public void testHelpGroupingFromAnnotation() throws CommandLineParserException {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        CommandLineParser<CommandInvocation> clp = builder.create(new GroupedOptionsCommand()).getParser();

        String help = clp.printHelp();

        assertTrue("Output section should appear", help.contains("Output:"));
        assertTrue("Default Options: should appear", help.contains("Options:"));
        assertTrue("--json should appear after Output:", help.indexOf("--json") > help.indexOf("Output:"));
        assertTrue("--verbose should appear after Options:", help.indexOf("--verbose") > help.indexOf("Options:"));
    }

    @Test
    public void testHelpGroupingOptionListFromAnnotation() throws CommandLineParserException {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        CommandLineParser<CommandInvocation> clp = builder.create(new GroupedOptionListCommand()).getParser();

        String help = clp.printHelp();

        assertTrue("Filters section should appear", help.contains("Filters:"));
        assertTrue("--include should appear after Filters:", help.indexOf("--include") > help.indexOf("Filters:"));
    }

    @CommandDefinition(name = "grouped", description = "Grouped options test")
    public static class GroupedOptionsCommand implements Command<CommandInvocation> {
        @Option(description = "Output as JSON", hasValue = false, helpGroup = "Output")
        boolean json;

        @Option(description = "Output as XML", hasValue = false, helpGroup = "Output")
        boolean xml;

        @Option(description = "Verbose output", hasValue = false)
        boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "filterapp", description = "Filter app")
    public static class GroupedOptionListCommand implements Command<CommandInvocation> {
        @OptionList(description = "Include patterns", helpGroup = "Filters")
        List<String> include;

        @Option(description = "Verbose output", hasValue = false)
        boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testCommandHelpGrouping() throws CommandLineParserException {
        // Test that subcommands with helpGroup are grouped in parent's help output
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> parent = ProcessedCommandBuilder.builder()
                .name("cli").description("CLI tool");

        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> build = ProcessedCommandBuilder.builder()
                .name("build").description("Build the project");
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> test = ProcessedCommandBuilder.builder()
                .name("test").description("Run tests");
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> deploy = ProcessedCommandBuilder.builder()
                .name("deploy").description("Deploy artifacts");
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> info = ProcessedCommandBuilder.builder()
                .name("info").description("Show project info");

        CommandLineParser<CommandInvocation> clpParent = new AeshCommandLineParser<>(parent.create());
        CommandLineParser<CommandInvocation> clpBuild = new AeshCommandLineParser<>(build.create());
        CommandLineParser<CommandInvocation> clpTest = new AeshCommandLineParser<>(test.create());
        CommandLineParser<CommandInvocation> clpDeploy = new AeshCommandLineParser<>(deploy.create());
        CommandLineParser<CommandInvocation> clpInfo = new AeshCommandLineParser<>(info.create());

        // Set helpGroup on subcommands
        clpBuild.getProcessedCommand().setHelpGroup("Development");
        clpTest.getProcessedCommand().setHelpGroup("Development");
        clpDeploy.getProcessedCommand().setHelpGroup("Operations");

        clpParent.addChildParser(clpBuild);
        clpParent.addChildParser(clpTest);
        clpParent.addChildParser(clpDeploy);
        clpParent.addChildParser(clpInfo);

        String help = clpParent.printHelp();

        // Named groups appear first, ungrouped commands under "Other:"
        int devIdx = help.indexOf("Development:");
        int opsIdx = help.indexOf("Operations:");
        int otherIdx = help.indexOf("Other:");

        assertTrue("Development group should appear", devIdx > 0);
        assertTrue("Operations group should appear", opsIdx > 0);
        assertTrue("Other group should appear for ungrouped commands", otherIdx > 0);
        assertTrue("Development should appear before Operations", devIdx < opsIdx);
        assertTrue("Operations should appear before Other", opsIdx < otherIdx);

        // Verify commands are under their groups
        assertTrue("build should appear after Development", help.indexOf("build") > devIdx);
        assertTrue("test should appear after Development", help.indexOf("test") > devIdx);
        assertTrue("deploy should appear after Operations", help.indexOf("deploy") > opsIdx);
        assertTrue("info should appear after Other", help.indexOf("info") > otherIdx);
    }

    @Test
    public void testCommandHelpGroupingNoGroups() throws CommandLineParserException {
        // Without helpGroup, subcommands appear under "<name> commands:" as before
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> parent = ProcessedCommandBuilder.builder()
                .name("app").description("App tool");
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> sub1 = ProcessedCommandBuilder.builder()
                .name("sub1").description("Subcommand 1");
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> sub2 = ProcessedCommandBuilder.builder()
                .name("sub2").description("Subcommand 2");

        CommandLineParser<CommandInvocation> clpParent = new AeshCommandLineParser<>(parent.create());
        clpParent.addChildParser(new AeshCommandLineParser<>(sub1.create()));
        clpParent.addChildParser(new AeshCommandLineParser<>(sub2.create()));

        String help = clpParent.printHelp();

        assertTrue("Should use traditional '<name> commands:' heading",
                help.contains("app commands:"));
        assertTrue("Should not have 'Other:' heading", !help.contains("Other:"));
    }

    @Test
    public void testCommandHelpGroupingFromAnnotation() throws CommandLineParserException {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        CommandLineParser<CommandInvocation> clp = builder.create(ToolGroupCommand.class).getParser();

        String help = clp.printHelp();

        assertTrue("Source Control group should appear", help.contains("Source Control:"));
        assertTrue("Build group should appear", help.contains("Build:"));
        assertTrue("gitcmd should appear after Source Control",
                help.indexOf("gitcmd") > help.indexOf("Source Control:"));
        assertTrue("svncmd should appear after Source Control",
                help.indexOf("svncmd") > help.indexOf("Source Control:"));
        assertTrue("compile should appear after Build",
                help.indexOf("compile") > help.indexOf("Build:"));
    }

    @GroupCommandDefinition(name = "tool", description = "Tool suite", groupCommands = { GitSubCmd.class, SvnSubCmd.class,
            CompileSubCmd.class })
    public static class ToolGroupCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "gitcmd", description = "Git operations", helpGroup = "Source Control")
    public static class GitSubCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "svncmd", description = "SVN operations", helpGroup = "Source Control")
    public static class SvnSubCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "compile", description = "Compile project", helpGroup = "Build")
    public static class CompileSubCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "base", description = "", groupCommands = { GitCommand.class })
    public static class BaseCommand implements Command {

        @Option(hasValue = false)
        private boolean help;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "git", description = "", groupCommands = { GitCommit.class, GitRebase.class,
            GitCheckout.class })
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
            if (help)
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
            if (help)
                commandInvocation.println(commandInvocation.getHelpInfo());

            return CommandResult.SUCCESS;
        }
    }

}
