/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.aesh.command.help;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.aesh.AeshRuntimeRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.junit.Test;

/**
 * A group-level {@code -h}/{@code --help} applies to the selected subcommand:
 * {@code group -h sub} behaves like {@code group sub -h} instead of failing
 * on the subcommand's required options (#762).
 */
public class HelpGroupHelpTest {

    @Test
    public void testGroupHelpFlagForwardsToSubcommand() {
        CommandResult[] result = new CommandResult[1];
        String output = captureStdout(() -> result[0] = run(false, "sub", "-h"));
        assertTrue("sub help expected, got:\n" + output, output.contains("Usage: grp sub"));

        CommandResult[] forwarded = new CommandResult[1];
        String forwardedOutput = captureStdout(() -> forwarded[0] = run(false, "-h", "sub"));
        assertEquals("group -h sub must succeed like sub -h", CommandResult.SUCCESS, forwarded[0]);
        assertTrue("group -h sub behaves like sub -h, got:\n" + forwardedOutput,
                forwardedOutput.contains("Usage: grp sub"));
    }

    @Test
    public void testGroupHelpFlagForwardsToSubcommandLazy() {
        CommandResult[] result = new CommandResult[1];
        String output = captureStdout(() -> result[0] = run(true, "sub", "-h"));
        assertEquals(CommandResult.SUCCESS, result[0]);
        assertTrue("sub help expected, got:\n" + output, output.contains("Usage: grp sub"));

        CommandResult[] forwarded = new CommandResult[1];
        String forwardedOutput = captureStdout(() -> forwarded[0] = run(true, "-h", "sub"));
        assertEquals("group -h sub must succeed like sub -h", CommandResult.SUCCESS, forwarded[0]);
        assertTrue("group -h sub behaves like sub -h, got:\n" + forwardedOutput,
                forwardedOutput.contains("Usage: grp sub"));
    }

    @Test
    public void testGroupHelpAloneStillShowsGroupHelp() {
        String output = help(false, "-h");
        assertTrue("group help expected, got:\n" + output, output.contains("Usage: grp "));
    }

    @Test
    public void testMissingRequiredStillFailsWithoutHelp() {
        CommandResult result = run(false, "sub");
        assertEquals("missing required option without -h must fail", CommandResult.USAGE_ERROR, result);
    }

    @Test
    public void testNestedGroupHelpFlagForwardsToLeaf() {
        CommandResult[] result = new CommandResult[1];
        String output = captureStdout(() -> result[0] = run(NestRootCommand.class, false, "-h", "mid", "leaf"));
        assertEquals("nested group -h must succeed", CommandResult.SUCCESS, result[0]);
        assertTrue("leaf help expected, got:\n" + output, output.contains("Usage: nest mid leaf"));
    }

    @Test
    public void testUnknownSubcommandStillFailsWithHelpFlag() {
        // An unresolvable subcommand is not a help request for anything
        CommandResult result = run(GrpCommand.class, false, "-h", "bogus");
        assertEquals("unknown subcommand must still fail", CommandResult.USAGE_ERROR, result);
    }

    private static String help(boolean lazy, String... args) {
        return help(GrpCommand.class, lazy, args);
    }

    private static String help(Class<? extends Command> root, boolean lazy, String... args) {
        return captureStdout(() -> run(root, lazy, args));
    }

    private static CommandResult run(boolean lazy, String... args) {
        return run(GrpCommand.class, lazy, args);
    }

    private static CommandResult run(Class<? extends Command> root, boolean lazy, String... args) {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        builder.setLazyChildResolution(lazy);
        AeshRuntimeRunner runner = AeshRuntimeRunner.builder()
                .containerBuilder(builder)
                .command(root)
                .lazyStartup(false)
                .args(args);
        return runner.execute();
    }

    private static String captureStdout(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return baos.toString();
    }

    @CommandDefinition(name = "sub", description = "Sub command", generateHelp = true)
    public static class SubCommand implements Command<CommandInvocation> {
        @Option(description = "A required value", required = true)
        String level;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "grp", description = "Group", generateHelp = true, groupCommands = { SubCommand.class })
    public static class GrpCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "leaf", description = "Leaf", generateHelp = true)
    public static class LeafCommand implements Command<CommandInvocation> {
        @Option(description = "A required value", required = true)
        String level;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mid", description = "Mid", generateHelp = true, groupCommands = { LeafCommand.class })
    public static class MidGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "nest", description = "Nest", generateHelp = true, groupCommands = { MidGroup.class })
    public static class NestRootCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
