package org.aesh.command.completer;

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
import org.junit.Ignore;
import org.junit.Test;

/**
 * Comprehensive tests for the dynamic completion output via --aesh-complete.
 * <p>
 * Each test verifies the actual stdout output that shell completion scripts
 * consume. This is the contract between aesh and the shell — any change
 * here directly affects tab completion behavior in all shells.
 */
public class DynamicCompletionTest {

    // ========== Simple command tests ==========

    @Test
    @Ignore("Bug #539: cursorAtPositional filter removes options at empty input position")
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
        // After a single dash, should show short options or long options
        assertTrue("Should show completion candidates", output.length() > 0);
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
    @Ignore("Bug #539: cursorAtPositional filter removes options after subcommand name")
    public void testGroupCommand_SubcommandWithSpace_ShowsSubcommandOptions() {
        // This is the key scenario: "app run <tab>"
        // Should show run's options, not just files
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
        // target has allowedValues = {debug, release}
        assertTrue("Should show 'debug' as allowed value", output.contains("debug"));
        assertTrue("Should show 'release' as allowed value", output.contains("release"));
    }

    @Test
    public void testGroupCommand_RootOptions_Shown() {
        // Root-level options (like --verbose on the group) should be shown
        // when no subcommand is typed yet
        String output = complete(AppGroup.class, "--");
        assertTrue("Should show root --verbose option", output.contains("--verbose"));
    }

    @Test
    public void testGroupCommand_SubcommandHelp_Shows() {
        // generateHelp=true should add --help to subcommands
        String output = complete(AppGroup.class, "run", "--h");
        assertTrue("Should complete --help for subcommand", output.contains("help"));
    }

    // ========== Argument completion tests ==========

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
        // When typing options, should not emit file sentinel
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
    @Ignore("Bug #539: cursorAtPositional filter removes options after subcommand name")
    public void testAeshCompleteFlag_SubcommandOptions() {
        // "app run <tab>" via the --aesh-complete flag
        String output = completeViaFlag(AppGroup.class, "run", "");
        assertTrue("Should show subcommand options via --aesh-complete flag: got [" + output.trim() + "]",
                output.contains("--debug") || output.contains("debug"));
    }

    // ========== Description tests ==========

    @Test
    public void testDescriptions_IncludedInOutput() {
        String output = complete(SimpleCmd.class, "--");
        // Descriptions are tab-separated: "value\tdescription"
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

    // ========== Helpers ==========

    /**
     * Run dynamic completion via the programmatic API (.dynamicComplete(true)).
     */
    private static String complete(Class<? extends Command> cmdClass, String... tokens) {
        return captureStdout(() -> AeshRuntimeRunner.builder()
                .command(cmdClass)
                .dynamicComplete(true)
                .args(tokens)
                .execute());
    }

    /**
     * Run dynamic completion via the --aesh-complete flag path (how shells invoke it).
     */
    private static String completeViaFlag(Class<? extends Command> cmdClass, String... tokens) {
        // Build args: --aesh-complete -- token1 token2 ...
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

    @CommandDefinition(name = "filecmd", description = "File command")
    public static class FileCmd implements Command<CommandInvocation> {
        @Option(shortName = 'o', description = "Output format")
        private String output;

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
}
