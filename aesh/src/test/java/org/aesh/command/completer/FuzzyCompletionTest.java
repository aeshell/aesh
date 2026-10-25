package org.aesh.command.completer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.invocation.AeshInvocationProviders;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.ReadlineConsole;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.Key;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Tests for fuzzy/camel-case completion fallback (#510).
 * <p>
 * When prefix matching finds no candidates, the typed word is fuzzy-matched
 * against command, subcommand, and option names. A single match replaces the
 * typed word; multiple matches are listed ranked by score. Exact prefix
 * behavior is unchanged.
 * <p>
 * Replacement assertions press enter and read the executed command line from
 * the execution listener, because the word replacement redraws the line and
 * raw terminal output still contains the original typed word.
 */
public class FuzzyCompletionTest {

    private final Key completeChar = Key.CTRL_I;
    private final Key enter = Key.ENTER;

    @Test
    public void testFuzzyCommandNameSingleMatchReplacesWord() throws Exception {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = setupConsole(connection, true);

        assertEquals("commit", completeAndExecute(connection, "comit"));

        console.stop();
    }

    @Test
    public void testFuzzyDisabledViaSettings() throws Exception {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = setupConsole(connection, false);

        connection.read("comit");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("comit");

        console.stop();
    }

    @Test
    public void testFuzzyCommandNameMultipleMatchesAreListed() throws Exception {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = setupConsole(connection, true);

        connection.read("foa");
        connection.read(completeChar.getFirstValue());
        String output = connection.waitForOutputContaining("foobar", 5000);
        assertTrue("Both fuzzy matches should be listed, got: " + output,
                output.contains("foobar"));
        assertTrue("Both fuzzy matches should be listed, got: " + output,
                output.contains("foobaz"));
        connection.assertBufferEndsWith("foa");

        console.stop();
    }

    @Test
    public void testPrefixCompletionUnchanged() throws Exception {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = setupConsole(connection, true);

        connection.read("com");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("commit ");

        console.stop();
    }

    @Test
    public void testSingleCharacterDoesNotTriggerFuzzy() throws Exception {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = setupConsole(connection, true);

        connection.read("x");
        connection.read(completeChar.getFirstValue());
        connection.assertBuffer("x");

        console.stop();
    }

    @Test
    public void testFuzzyOptionNameTypo() throws Exception {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = setupConsole(connection, true);

        assertEquals("commit --amend", completeAndExecute(connection, "commit --amnd"));

        console.stop();
    }

    @Test
    public void testCamelCaseOptionAbbreviation() throws Exception {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = setupConsole(connection, true);

        assertEquals("camel --getCursorIndex", completeAndExecute(connection, "camel --gCI"));

        console.stop();
    }

    @Test
    public void testFuzzySubcommandName() throws Exception {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = setupConsole(connection, true);

        assertEquals("git commit", completeAndExecute(connection, "git comit"));

        console.stop();
    }

    @Test
    public void testInvocationProvidersFuzzyFlagDefaults() {
        InvocationProviders plain = new AeshInvocationProviders();
        assertFalse("Providers built without settings must not enable fuzzy completion",
                plain.fuzzyCompletion());

        Settings<CommandInvocation> enabled = SettingsBuilder.builder().build();
        assertTrue("Settings default should enable fuzzy completion",
                enabled.fuzzyCompletion());
        assertTrue("Providers built from default settings should enable fuzzy completion",
                new AeshInvocationProviders(enabled).fuzzyCompletion());

        Settings<CommandInvocation> disabled = SettingsBuilder.builder()
                .fuzzyCompletion(false)
                .build();
        assertFalse(disabled.fuzzyCompletion());
        assertFalse(new AeshInvocationProviders(disabled).fuzzyCompletion());
    }

    // ========== Helpers ==========

    /**
     * Type the given text, press tab, press enter, and return the command
     * line reported by the execution listener (trimmed).
     */
    private String completeAndExecute(TestConnection connection, String typed) throws InterruptedException {
        connection.read(typed);
        connection.read(completeChar.getFirstValue());
        connection.read(enter.getFirstValue());
        assertTrue("Command should have been executed", executedLatch.await(5, TimeUnit.SECONDS));
        return executedLine.get().trim();
    }

    private final CountDownLatch executedLatch = new CountDownLatch(1);
    private final AtomicReference<String> executedLine = new AtomicReference<>();

    // ========== Setup ==========

    private ReadlineConsole setupConsole(TestConnection connection, boolean fuzzy) throws Exception {
        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(GitGroupCmd.class)
                .command(GitCommitCmd.class)
                .command(FooBarCmd.class)
                .command(FooBazCmd.class)
                .command(CamelCmd.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .fuzzyCompletion(fuzzy)
                .commandExecutionListener((line, result, durationMs) -> {
                    executedLine.set(line);
                    executedLatch.countDown();
                })
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();
        return console;
    }

    // ========== Test commands ==========

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

    @CommandDefinition(name = "foobar", description = "Foo bar")
    public static class FooBarCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "foobaz", description = "Foo baz")
    public static class FooBazCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "camel", description = "Camel case options")
    public static class CamelCmd implements Command<CommandInvocation> {
        @Option(name = "getCursorIndex", hasValue = false, description = "Get the cursor index")
        boolean getCursorIndex;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }
}
