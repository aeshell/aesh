package org.aesh.command.completer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.impl.completer.CommandSuggestionProvider;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.console.ReadlineConsole;
import org.aesh.readline.CompositeSuggestionProvider;
import org.aesh.readline.Prompt;
import org.aesh.readline.SuggestionProvider;
import org.aesh.tty.TestConnection;
import org.junit.Test;

public class CommandSuggestionProviderTest {

    @Test
    public void testCommandNameSuggestion() throws CommandRegistryException {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(CacheCommand.class)
                .command(ConnectCommand.class)
                .create();

        CommandSuggestionProvider<CommandInvocation> provider = new CommandSuggestionProvider<>(registry);

        // "ca" should suggest "che" (from "cache")
        assertEquals("che", provider.suggest("ca"));

        // "con" should suggest "nect" (from "connect")
        assertEquals("nect", provider.suggest("con"));

        // "c" is ambiguous (cache and connect) -> null
        assertNull(provider.suggest("c"));

        // Full command name -> null
        assertNull(provider.suggest("cache"));

        // Empty -> null
        assertNull(provider.suggest(""));
        assertNull(provider.suggest(null));
    }

    @Test
    public void testSubcommandSuggestion() throws CommandRegistryException {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(GitCommand.class)
                .create();

        CommandSuggestionProvider<CommandInvocation> provider = new CommandSuggestionProvider<>(registry);

        // "git co" should suggest "mmit" (from "commit")
        assertEquals("mmit", provider.suggest("git co"));

        // "git r" should suggest "ebase" (from "rebase", only one match)
        assertEquals("ebase", provider.suggest("git r"));
    }

    @Test
    public void testOptionSuggestion() throws CommandRegistryException {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(ConnectCommand.class)
                .create();

        CommandSuggestionProvider<CommandInvocation> provider = new CommandSuggestionProvider<>(registry);

        // "connect --ho" should suggest "st=" (from "--host")
        assertEquals("st=", provider.suggest("connect --ho"));

        // "connect --p" is ambiguous (port and password) -> null
        assertNull(provider.suggest("connect --p"));
    }

    @Test
    public void testGroupCommandOptionSuggestion() throws CommandRegistryException {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(GitCommand.class)
                .create();

        CommandSuggestionProvider<CommandInvocation> provider = new CommandSuggestionProvider<>(registry);

        // "git commit --al" should suggest "l" (from "--all", boolean so no =)
        assertEquals("l", provider.suggest("git commit --al"));
    }

    @Test
    public void testCompositeSuggestionProvider() throws CommandRegistryException {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.<CommandInvocation> builder()
                .command(CacheCommand.class)
                .create();

        // History provider that returns null for everything
        SuggestionProvider historyProvider = buffer -> null;
        CommandSuggestionProvider<CommandInvocation> commandProvider = new CommandSuggestionProvider<>(registry);

        CompositeSuggestionProvider composite = new CompositeSuggestionProvider(historyProvider, commandProvider);

        // With no history match, falls through to command provider
        assertEquals("che", composite.suggest("ca"));

        // History provider that returns a match
        SuggestionProvider historyWithMatch = buffer -> {
            if (buffer.equals("ca"))
                return "che --name=test";
            return null;
        };

        CompositeSuggestionProvider compositeWithHistory = new CompositeSuggestionProvider(historyWithMatch, commandProvider);

        // History match takes priority
        assertEquals("che --name=test", compositeWithHistory.suggest("ca"));
    }

    @Test
    public void testIntegrationWithConsole() throws Exception {
        TestConnection connection = new TestConnection(false);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(CacheCommand.class)
                .command(ConnectCommand.class)
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation, OptionActivator, CommandActivator> settings = SettingsBuilder
                .builder()
                .logging(true)
                .enableAlias(false)
                .connection(connection)
                .commandRegistry(registry)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));

        // Set up command suggestion provider using the registry from console
        console.setSuggestionProvider(new CommandSuggestionProvider<>(console.getCommandRegistry()));

        console.start();

        connection.clearOutputBuffer();
        connection.read("ca");

        // Ghost text is rendered with ANSI DIM codes; in non-stripped mode we should see "che" in the output
        Thread.sleep(50);
        String output = connection.getOutputBuffer();
        // The output should contain both the typed text and the ghost suggestion
        // "ca" is typed, then "che" is shown as ghost text (with ANSI codes around it)
        assert output.contains("ca") : "Output should contain typed text 'ca', got: " + output;
        assert output.contains("che") : "Output should contain ghost suggestion 'che', got: " + output;

        console.stop();
    }

    // --- Test command classes ---

    @CommandDefinition(name = "cache", description = "Cache operations")
    public static class CacheCommand implements Command<CommandInvocation> {
        @Option
        private String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "connect", description = "Connect to a server")
    public static class ConnectCommand implements Command<CommandInvocation> {
        @Option
        private String host;

        @Option
        private int port;

        @Option
        private String password;

        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "git", description = "Git operations", groupCommands = { GitCommit.class, GitRebase.class })
    public static class GitCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "commit", description = "Commit changes")
    public static class GitCommit implements Command<CommandInvocation> {
        @Option(hasValue = false)
        private boolean all;

        @Option
        private String message;

        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "rebase", description = "Rebase branch")
    public static class GitRebase implements Command<CommandInvocation> {
        @Option(hasValue = false)
        private boolean force;

        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }
}
