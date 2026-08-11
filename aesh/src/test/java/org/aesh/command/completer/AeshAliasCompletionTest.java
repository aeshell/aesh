package org.aesh.command.completer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.ReadlineConsole;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.Key;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Tests for alias expansion during tab completion (#572).
 * Verifies that typing an alias followed by tab produces the same
 * completions as typing the expanded command.
 */
public class AeshAliasCompletionTest {

    private final Key completeChar = Key.CTRL_I;

    @Test
    public void testAliasCompletionShowsSubcommandOptions() throws Exception {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = setupConsole(connection);

        // Define alias: gc = git commit
        connection.read("alias gc='git commit'" + Config.getLineSeparator());
        Thread.sleep(200);
        connection.clearOutputBuffer();

        // Type "gc --" and tab — should show commit's options
        connection.read("gc --");
        connection.read(completeChar.getFirstValue());
        Thread.sleep(100);

        String output = connection.getOutputBuffer();
        assertTrue("Should show --all option, got: " + output,
                output.contains("all"));
        assertTrue("Should show --message option, got: " + output,
                output.contains("message"));

        console.stop();
    }

    @Test
    public void testAliasCompletionPartialOption() throws Exception {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = setupConsole(connection);

        // Define alias
        connection.read("alias gc='git commit'" + Config.getLineSeparator());
        Thread.sleep(200);
        connection.clearOutputBuffer();

        // Type "gc --m" and tab — should complete to "--message "
        connection.read("gc --m");
        connection.read(completeChar.getFirstValue());
        Thread.sleep(100);

        String output = connection.getOutputBuffer();
        assertTrue("Should complete --message, got: " + output,
                output.contains("gc --message"));

        console.stop();
    }

    @Test
    public void testAliasSingleCommandCompletion() throws Exception {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = setupConsole(connection);

        // Define alias: g = git (group command)
        connection.read("alias g='git'" + Config.getLineSeparator());
        Thread.sleep(200);
        connection.clearOutputBuffer();

        // Type "g " and tab — should show subcommands
        connection.read("g ");
        connection.read(completeChar.getFirstValue());
        Thread.sleep(100);

        String output = connection.getOutputBuffer();
        assertTrue("Should show 'commit' subcommand, got: " + output,
                output.contains("commit"));

        console.stop();
    }

    @Test
    public void testNoAliasNoFalseExpansion() throws Exception {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = setupConsole(connection);

        connection.clearOutputBuffer();

        // Type "unknown " and tab — no alias, no command, no candidates
        connection.read("unknown ");
        connection.read(completeChar.getFirstValue());
        Thread.sleep(100);

        String output = connection.getOutputBuffer();
        // Should not contain any option completions
        assertEquals("Should have no completions for unknown command",
                "unknown ", output);

        console.stop();
    }

    @Test
    public void testCommandTakesPriorityOverAlias() throws Exception {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = setupConsole(connection);

        // Define alias: git = something-else (alias has same name as real command)
        connection.read("alias git='unknown'" + Config.getLineSeparator());
        Thread.sleep(200);
        connection.clearOutputBuffer();

        // Type "git " and tab — real command should take priority
        connection.read("git ");
        connection.read(completeChar.getFirstValue());
        Thread.sleep(100);

        String output = connection.getOutputBuffer();
        // Should show git's subcommands (commit), not alias expansion
        assertTrue("Real command should take priority, got: " + output,
                output.contains("commit"));

        console.stop();
    }

    // ========== Setup ==========

    private ReadlineConsole setupConsole(TestConnection connection) throws Exception {
        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(GitGroupCmd.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .enableAlias(true)
                .persistAlias(false)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();
        return console;
    }

    // ========== Test commands: git-like group ==========

    @CommandDefinition(name = "git", description = "Git", generateHelp = true, groupCommands = { GitCommitCmd.class })
    public static class GitGroupCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "commit", description = "Commit changes", generateHelp = true)
    public static class GitCommitCmd implements Command<CommandInvocation> {
        @Option(name = "all", shortName = 'a', hasValue = false, description = "Stage all modified files")
        boolean all;

        @Option(name = "message", shortName = 'm', description = "Commit message")
        String message;

        @Option(name = "amend", hasValue = false, description = "Amend previous commit")
        boolean amend;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }
}
