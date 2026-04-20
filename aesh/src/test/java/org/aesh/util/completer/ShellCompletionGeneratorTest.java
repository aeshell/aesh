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

import java.io.File;

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
import org.aesh.command.option.Argument;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.util.completer.ShellCompletionGenerator.ShellType;
import org.junit.Test;

/**
 * @author Aesh team
 */
public class ShellCompletionGeneratorTest {

    // -- Bash tests --

    @Test
    public void testBashSimpleCommand() {
        String out = generate(ShellType.BASH, SimpleCmd.class, "mycli");

        assertTrue(out.contains("#!/usr/bin/env bash"));
        assertTrue(out.contains("_complete_mycli()"));
        assertTrue(out.contains("_cmd_mycli()"));
        assertTrue(out.contains("--verbose"));
        assertTrue(out.contains("-v"));
        assertTrue(out.contains("--output"));
        assertTrue(out.contains("-o"));
        assertTrue(out.contains("complete -o default -F _complete_mycli mycli"));
    }

    @Test
    public void testBashGroupCommand() {
        String out = generate(ShellType.BASH, GroupCmd.class, "git");

        assertTrue(out.contains("_complete_git()"));
        assertTrue(out.contains("_cmd_git()"));
        assertTrue(out.contains("_cmd_git_commit()"));
        assertTrue(out.contains("_cmd_git_push()"));
        assertTrue(out.contains("commit)"));
        assertTrue(out.contains("push)"));
    }

    @Test
    public void testBashOptionAliases() {
        String out = generate(ShellType.BASH, AliasCmd.class, "java");

        assertTrue("Should contain primary name", out.contains("--enableassertions"));
        assertTrue("Should contain alias", out.contains("--ea"));
    }

    @Test
    public void testBashNegatableOption() {
        String out = generate(ShellType.BASH, NegatableCmd.class, "app");

        assertTrue("Should contain --verbose", out.contains("--verbose"));
        assertTrue("Should contain --no-verbose", out.contains("--no-verbose"));
    }

    @Test
    public void testBashValueCompletion() {
        String out = generate(ShellType.BASH, SimpleCmd.class, "mycli");

        // Value completion should use PREV_WORD (case "$prev"), not CURR_WORD
        assertTrue("Should have prev-word case for value completion", out.contains("case \"$prev\""));
        assertTrue("Should have default values", out.contains("FOO"));
        assertTrue("Should have default values", out.contains("BAR"));
    }

    @Test
    public void testBashFileOption() {
        String out = generate(ShellType.BASH, FileCmd.class, "app");

        assertTrue("Should use _filedir for file options", out.contains("_filedir"));
    }

    @Test
    public void testBashNoPropertyOptions() {
        // Property options (like -Dkey=value) should not appear in completion
        String out = generate(ShellType.BASH, SimpleCmd.class, "mycli");
        // Just verify it generates without error — property options are skipped
        assertTrue(out.contains("_complete_mycli"));
    }

    // -- Zsh tests --

    @Test
    public void testZshSimpleCommand() {
        String out = generate(ShellType.ZSH, SimpleCmd.class, "mycli");

        assertTrue(out.contains("#compdef mycli"));
        assertTrue(out.contains("_mycli()"));
        assertTrue(out.contains("_arguments"));
        assertTrue(out.contains("--verbose"));
        assertTrue(out.contains("--output"));
    }

    @Test
    public void testZshGroupCommand() {
        String out = generate(ShellType.ZSH, GroupCmd.class, "git");

        assertTrue(out.contains("#compdef git"));
        assertTrue(out.contains("_git()"));
        assertTrue(out.contains("_git_commit()"));
        assertTrue(out.contains("_git_push()"));
        assertTrue(out.contains("_describe 'command' commands"));
        assertTrue(out.contains("'commit:"));
        assertTrue(out.contains("'push:"));
    }

    @Test
    public void testZshOptionAliases() {
        String out = generate(ShellType.ZSH, AliasCmd.class, "java");

        assertTrue("Should contain primary name", out.contains("--enableassertions"));
        assertTrue("Should contain alias", out.contains("--ea"));
    }

    @Test
    public void testZshNegatableOption() {
        String out = generate(ShellType.ZSH, NegatableCmd.class, "app");

        assertTrue("Should contain --no-verbose", out.contains("--no-verbose"));
    }

    @Test
    public void testZshFileOption() {
        String out = generate(ShellType.ZSH, FileCmd.class, "app");

        assertTrue("Should use _files for file options", out.contains("_files"));
    }

    @Test
    public void testZshArguments() {
        String out = generate(ShellType.ZSH, FileCmd.class, "app");

        // File argument should use _files
        assertTrue("Should use _files for file argument", out.contains("_files"));
    }

    // -- Fish tests --

    @Test
    public void testFishSimpleCommand() {
        String out = generate(ShellType.FISH, SimpleCmd.class, "mycli");

        assertTrue(out.contains("complete -c mycli"));
        assertTrue(out.contains("-l verbose"));
        assertTrue(out.contains("-s v"));
        assertTrue(out.contains("-l output"));
        assertTrue(out.contains("-s o"));
    }

    @Test
    public void testFishGroupCommand() {
        String out = generate(ShellType.FISH, GroupCmd.class, "git");

        assertTrue("Should have subcommand entries",
                out.contains("__fish_use_subcommand"));
        assertTrue("Should list commit subcommand", out.contains("-a commit"));
        assertTrue("Should list push subcommand", out.contains("-a push"));
        assertTrue("Should scope options to subcommand",
                out.contains("__fish_seen_subcommand_from commit"));
    }

    @Test
    public void testFishOptionAliases() {
        String out = generate(ShellType.FISH, AliasCmd.class, "java");

        assertTrue("Should contain alias", out.contains("-l ea"));
        assertTrue("Should contain primary name", out.contains("-l enableassertions"));
    }

    @Test
    public void testFishNegatableOption() {
        String out = generate(ShellType.FISH, NegatableCmd.class, "app");

        assertTrue("Should contain negated form", out.contains("-l no-verbose"));
    }

    @Test
    public void testFishValueCompletion() {
        String out = generate(ShellType.FISH, SimpleCmd.class, "mycli");

        assertTrue("Should require argument for value options", out.contains("-r"));
        assertTrue("Should include default values", out.contains("FOO"));
    }

    // -- @Arguments tests --

    @Test
    public void testBashMultiArguments() {
        String out = generate(ShellType.BASH, MultiArgsCmd.class, "cp");

        assertTrue("Should use _filedir for file arguments", out.contains("_filedir"));
    }

    @Test
    public void testZshMultiArguments() {
        String out = generate(ShellType.ZSH, MultiArgsCmd.class, "cp");

        assertTrue("Should use _files for file arguments", out.contains("_files"));
    }

    @Test
    public void testFishMultiArguments() {
        // Fish doesn't have special @Arguments handling in the generator,
        // but verify it doesn't fail
        String out = generate(ShellType.FISH, MultiArgsCmd.class, "cp");

        assertTrue(out.contains("complete -c cp"));
    }

    // -- Dynamic callback completion tests --

    @Test
    public void testBashDynamicCompletion() {
        String out = generateDynamic(ShellType.BASH, SimpleCmd.class, "mycli");

        assertTrue(out.contains("#!/usr/bin/env bash"));
        assertTrue(out.contains("_complete_mycli()"));
        assertTrue("Should call --aesh-complete", out.contains("--aesh-complete"));
        assertTrue("Should pass COMP_WORDS", out.contains("${COMP_WORDS[@]:1}"));
        assertTrue(out.contains("complete -o default -F _complete_mycli mycli"));
    }

    @Test
    public void testZshDynamicCompletion() {
        String out = generateDynamic(ShellType.ZSH, SimpleCmd.class, "mycli");

        assertTrue(out.contains("#compdef mycli"));
        assertTrue(out.contains("_mycli()"));
        assertTrue("Should call --aesh-complete", out.contains("--aesh-complete"));
        assertTrue("Should use words array", out.contains("${words[@]:1}"));
        assertTrue(out.contains("compadd"));
    }

    @Test
    public void testFishDynamicCompletion() {
        String out = generateDynamic(ShellType.FISH, SimpleCmd.class, "mycli");

        assertTrue(out.contains("complete -c mycli"));
        assertTrue("Should call --aesh-complete", out.contains("--aesh-complete"));
        assertTrue("Should use commandline", out.contains("commandline -cop"));
    }

    @Test
    public void testOneShotDynamic() throws CommandLineParserException {
        String out = ShellCompletionGenerator.generateDynamic(ShellType.BASH, SimpleCmd.class, "mycli");

        assertTrue(out.contains("_complete_mycli"));
        assertTrue(out.contains("--aesh-complete"));
    }

    // -- One-shot API test --

    @Test
    public void testOneShot() throws CommandLineParserException {
        String out = ShellCompletionGenerator.generate(ShellType.BASH, SimpleCmd.class, "mycli");

        assertTrue(out.contains("_complete_mycli"));
    }

    // -- Helper --

    private String generate(ShellType type, Class<? extends Command> clazz, String programName) {
        CommandLineParser<CommandInvocation> parser = getParser(clazz);
        return ShellCompletionGenerator.forShell(type).generate(parser, programName);
    }

    private String generateDynamic(ShellType type, Class<? extends Command> clazz, String programName) {
        CommandLineParser<CommandInvocation> parser = getParser(clazz);
        return ShellCompletionGenerator.forShell(type).generateDynamic(parser, programName);
    }

    @SuppressWarnings("unchecked")
    private CommandLineParser<CommandInvocation> getParser(Class<? extends Command> clazz) {
        CommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        try {
            CommandContainer<CommandInvocation> container = builder.create((Class) clazz);
            return container.getParser();
        } catch (CommandLineParserException e) {
            throw new RuntimeException(e);
        }
    }

    // -- Test commands --

    @CommandDefinition(name = "simple", description = "A simple command")
    public static class SimpleCmd implements Command {
        @Option(shortName = 'v', hasValue = false, description = "Enable verbose output")
        private boolean verbose;

        @Option(shortName = 'o', defaultValue = { "FOO", "BAR" }, description = "Output format")
        private String output;

        @Override
        public CommandResult execute(CommandInvocation inv) throws CommandException {
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "mygit", groupCommands = { CommitCmd.class, PushCmd.class }, description = "A git-like tool")
    public static class GroupCmd implements Command {
        @Option(shortName = 'h', hasValue = false, description = "Show help")
        private boolean help;

        @Override
        public CommandResult execute(CommandInvocation inv) throws CommandException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "commit", description = "Record changes")
    public static class CommitCmd implements Command {
        @Option(shortName = 'm', description = "Commit message")
        private String message;

        @Option(hasValue = false, description = "Amend previous commit")
        private boolean amend;

        @Override
        public CommandResult execute(CommandInvocation inv) throws CommandException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "push", description = "Push to remote")
    public static class PushCmd implements Command {
        @Option(description = "Remote name", defaultValue = "origin")
        private String remote;

        @Option(hasValue = false, description = "Force push")
        private boolean force;

        @Override
        public CommandResult execute(CommandInvocation inv) throws CommandException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "runner", description = "Java runner")
    public static class AliasCmd implements Command {
        @Option(name = "enableassertions", aliases = { "ea" }, hasValue = false, description = "Enable assertions")
        private boolean enableAssertions;

        @Override
        public CommandResult execute(CommandInvocation inv) throws CommandException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "app", description = "An app")
    public static class NegatableCmd implements Command {
        @Option(negatable = true, hasValue = false, description = "Verbose output")
        private boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation inv) throws CommandException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "copy", description = "Copy files")
    public static class MultiArgsCmd implements Command {
        @Option(hasValue = false, description = "Recursive")
        private boolean recursive;

        @Arguments(description = "Source and destination files")
        private java.util.List<File> files;

        @Override
        public CommandResult execute(CommandInvocation inv) throws CommandException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "fileapp", description = "File app")
    public static class FileCmd implements Command {
        @Option(shortName = 'c', description = "Config file")
        private File config;

        @Argument(description = "Input file")
        private File input;

        @Override
        public CommandResult execute(CommandInvocation inv) throws CommandException {
            return CommandResult.SUCCESS;
        }
    }
}
