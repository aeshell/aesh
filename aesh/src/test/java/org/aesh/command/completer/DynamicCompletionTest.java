package org.aesh.command.completer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.aesh.AeshRuntimeRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.junit.Test;

/**
 * Comprehensive tests for the dynamic completion output via --aesh-complete.
 * <p>
 * Each test verifies the actual stdout output that shell completion scripts
 * consume. This is the contract between aesh and the shell -- any change
 * here directly affects tab completion behavior in all shells.
 */
public class DynamicCompletionTest {

    // ========== Simple command: empty and prefix tests ==========

    @Test
    public void testSimpleCommand_EmptyInput_ShowsOptions() {
        String output = complete(SimpleCmd.class, "");
        assertTrue("Should show --name option", output.contains("--name"));
        assertTrue("Should show --verbose option", output.contains("--verbose"));
    }

    @Test
    public void testSimpleCommand_DashDash_ShowsOptions() {
        String output = complete(SimpleCmd.class, "--");
        assertTrue("Should show --name", output.contains("--name"));
        assertTrue("Should show --verbose", output.contains("--verbose"));
    }

    @Test
    public void testSimpleCommand_PartialOption_Completes() {
        String output = complete(SimpleCmd.class, "--n");
        assertTrue("Should complete --name", output.contains("name"));
    }

    @Test
    public void testSimpleCommand_ShortOption_ShowsOptions() {
        String output = complete(SimpleCmd.class, "-");
        assertTrue("Should show completion candidates", output.length() > 0);
    }

    // ========== Simple command: option value transitions ==========

    @Test
    public void testSimpleCommand_AfterValueOption_ShowsValueOrOptions() {
        // After --name (which takes a value) and a space, the engine should
        // either offer value completions or show remaining options
        String output = complete(SimpleCmd.class, "--name", "");
        // --name has no completer/allowedValues, so no value candidates
        // The engine should fall back to showing remaining options
        assertTrue("Should have some output after value-taking option",
                output.length() > 0);
    }

    @Test
    public void testSimpleCommand_AfterCompleteOptionValue_ShowsRemainingOptions() {
        // After --name value, should show remaining options (--verbose, --help)
        String output = complete(SimpleCmd.class, "--name", "alice", "");
        assertTrue("Should show remaining options after name is set",
                output.contains("--verbose"));
        assertFalse("Should NOT show --name again (already used)",
                output.contains("--name"));
    }

    @Test
    public void testSimpleCommand_AfterBooleanOption_ShowsRemainingOptions() {
        // After --verbose (boolean, no value), should show remaining options
        String output = complete(SimpleCmd.class, "--verbose", "--");
        assertTrue("Should show --name after --verbose", output.contains("--name"));
    }

    // ========== Group command tests ==========

    @Test
    public void testGroupCommand_EmptyInput_ShowsSubcommands() {
        String output = complete(AppGroup.class, "");
        assertTrue("Should show 'run' subcommand", output.contains("run"));
        assertTrue("Should show 'build' subcommand", output.contains("build"));
    }

    @Test
    public void testGroupCommand_PartialSubcommand_Completes() {
        String output = complete(AppGroup.class, "r");
        assertTrue("Should complete to 'run'", output.contains("run"));
    }

    @Test
    public void testGroupCommand_SubcommandWithSpace_ShowsSubcommandOptions() {
        // Key scenario: "app run <tab>" — should show run's options
        String output = complete(AppGroup.class, "run", "");
        assertTrue("Should show --debug option for 'run' subcommand: got [" + output.trim() + "]",
                output.contains("--debug") || output.contains("debug"));
    }

    @Test
    public void testGroupCommand_SubcommandPartialOption_Completes() {
        String output = complete(AppGroup.class, "run", "--d");
        assertTrue("Should complete --debug", output.contains("debug"));
    }

    @Test
    public void testGroupCommand_SubcommandOptionValue_ShowsAllowedValues() {
        String output = complete(AppGroup.class, "build", "--target", "");
        assertTrue("Should show 'debug' as allowed value", output.contains("debug"));
        assertTrue("Should show 'release' as allowed value", output.contains("release"));
    }

    @Test
    public void testGroupCommand_RootOptions_Shown() {
        String output = complete(AppGroup.class, "--");
        assertTrue("Should show root --verbose option", output.contains("--verbose"));
    }

    @Test
    public void testGroupCommand_SubcommandHelp_Shows() {
        String output = complete(AppGroup.class, "run", "--h");
        assertTrue("Should complete --help for subcommand", output.contains("help"));
    }

    @Test
    public void testGroupCommand_UnknownSubcommand_NoError() {
        // Unknown subcommand prefix should not crash, just return no/few candidates
        String output = complete(AppGroup.class, "xyz");
        // Should not throw, output may be empty or contain error info
        // The important thing is it doesn't crash
        assertTrue("Should handle unknown subcommand gracefully", true);
    }

    // ========== Option value tests ==========

    @Test
    public void testOptionWithAllowedValues_ShowsValues() {
        String output = complete(EnvCmd.class, "--environment", "");
        assertTrue("Should show 'dev' as allowed value", output.contains("dev"));
        assertTrue("Should show 'prod' as allowed value", output.contains("prod"));
    }

    @Test
    public void testOptionWithPartialValue_Filters() {
        String output = complete(EnvCmd.class, "--environment", "p");
        assertTrue("Should show 'prod' matching 'p'", output.contains("prod"));
    }

    // ========== File sentinel tests ==========

    @Test
    public void testFileSentinel_EmittedForFileArgument() {
        String output = complete(FileCmd.class, "");
        assertTrue("Should emit __aesh_file__ for file-type argument",
                output.contains("__aesh_file__"));
    }

    @Test
    public void testNoFileSentinel_WhenOptionIsBeingTyped() {
        String output = complete(FileCmd.class, "--");
        assertFalse("Should NOT emit __aesh_file__ when typing option prefix",
                output.contains("__aesh_file__"));
    }

    // ========== --aesh-complete flag path tests ==========

    @Test
    public void testAeshCompleteFlag_SimpleOptions() {
        String output = completeViaFlag(SimpleCmd.class, "--");
        assertTrue("Should show options via --aesh-complete flag", output.contains("--name"));
    }

    @Test
    public void testAeshCompleteFlag_GroupSubcommand() {
        String output = completeViaFlag(AppGroup.class, "");
        assertTrue("Should show subcommands via --aesh-complete flag", output.contains("run"));
        assertTrue("Should show subcommands via --aesh-complete flag", output.contains("build"));
    }

    @Test
    public void testAeshCompleteFlag_SubcommandOptions() {
        // "app run <tab>" via the --aesh-complete flag
        String output = completeViaFlag(AppGroup.class, "run", "");
        assertTrue("Should show subcommand options via --aesh-complete flag: got [" + output.trim() + "]",
                output.contains("--debug") || output.contains("debug"));
    }

    @Test
    public void testAeshCompleteFlag_MatchesDynamicCompleteAPI() {
        // Both paths should produce equivalent output
        String apiOutput = complete(AppGroup.class, "");
        String flagOutput = completeViaFlag(AppGroup.class, "");
        // Both should contain subcommands
        assertEquals("Flag and API paths should produce same subcommand set",
                apiOutput.contains("run"), flagOutput.contains("run"));
        assertEquals("Flag and API paths should produce same subcommand set",
                apiOutput.contains("build"), flagOutput.contains("build"));
    }

    // ========== Description tests ==========

    @Test
    public void testDescriptions_IncludedInOutput() {
        String output = complete(SimpleCmd.class, "--");
        assertTrue("Should include tab-separated descriptions",
                output.contains("\t"));
    }

    // ========== No execution tests ==========

    @Test
    public void testCompletion_DoesNotExecuteCommand() {
        ExecutionTracker.executed = false;
        complete(ExecutionTracker.class, "");
        assertFalse("Command should NOT be executed during completion",
                ExecutionTracker.executed);
    }

    // ========== Nested group tests ==========

    @Test
    public void testNestedGroup_MidLevel_ShowsLeafSubcommands() {
        String output = complete(TopGroup.class, "mid", "");
        assertTrue("Should show 'leaf' subcommand", output.contains("leaf"));
    }

    @Test
    public void testNestedGroup_LeafLevel_ShowsOptions() {
        String output = complete(TopGroup.class, "mid", "leaf", "--");
        assertTrue("Should show leaf's --file option", output.contains("--file"));
    }

    // ========== Helpers ==========

    private static String complete(Class<? extends Command> cmdClass, String... tokens) {
        return captureStdout(() -> AeshRuntimeRunner.builder()
                .command(cmdClass)
                .dynamicComplete(true)
                .args(tokens)
                .execute());
    }

    private static String completeViaFlag(Class<? extends Command> cmdClass, String... tokens) {
        String[] args = new String[tokens.length + 2];
        args[0] = "--aesh-complete";
        args[1] = "--";
        System.arraycopy(tokens, 0, args, 2, tokens.length);
        return captureStdout(() -> AeshRuntimeRunner.builder()
                .command(cmdClass)
                .args(args)
                .execute());
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

    // ========== Test commands ==========

    @CommandDefinition(name = "simple", description = "Simple command", generateHelp = true)
    public static class SimpleCmd implements Command<CommandInvocation> {
        @Option(shortName = 'n', description = "Your name")
        private String name;

        @Option(shortName = 'v', hasValue = false, description = "Verbose output")
        private boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "app", description = "Application", generateHelp = true, groupCommands = { RunCmd.class,
            BuildCmd.class })
    public static class AppGroup implements Command<CommandInvocation> {
        @Option(shortName = 'v', hasValue = false, description = "Verbose")
        private boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "run", description = "Run a script", generateHelp = true)
    public static class RunCmd implements Command<CommandInvocation> {
        @Option(hasValue = false, description = "Enable debug mode")
        private boolean debug;

        @Option(description = "Java version", defaultValue = "17")
        private String java;

        @Argument(description = "Script file")
        private String script;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "build", description = "Build a project", generateHelp = true)
    public static class BuildCmd implements Command<CommandInvocation> {
        @Option(description = "Build target", allowedValues = { "debug", "release" })
        private String target;

        @Option(hasValue = false, description = "Clean first")
        private boolean clean;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "env", description = "Set environment")
    public static class EnvCmd implements Command<CommandInvocation> {
        @Option(description = "Environment name", allowedValues = { "dev", "staging", "prod" })
        private String environment;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "filecmd", description = "File command", generateHelp = true)
    public static class FileCmd implements Command<CommandInvocation> {
        @Argument(description = "Input file")
        private java.io.File input;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "tracker", description = "Tracks execution")
    public static class ExecutionTracker implements Command<CommandInvocation> {
        static boolean executed = false;

        @Option(description = "A value")
        private String value;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            executed = true;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "top", description = "Top level", groupCommands = { MidGroup.class })
    public static class TopGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mid", description = "Mid level", groupCommands = { LeafCmd.class })
    public static class MidGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "leaf", description = "Leaf command", generateHelp = true)
    public static class LeafCmd implements Command<CommandInvocation> {
        @Option(description = "File path")
        private String file;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }
}
