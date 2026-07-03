package org.aesh.command.completer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.console.AeshContext;
import org.aesh.console.DefaultAeshContext;
import org.aesh.console.ReadlineConsole;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.Key;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Completion tests for options with acceptNameWithoutDashes=true (#549).
 * Tests both parser-level complete() and interactive TestConnection completion.
 */
public class BareOptionCompletionTest {

    private final AeshContext aeshContext = new DefaultAeshContext();
    private final Key completeChar = Key.CTRL_I;

    // ========== Parser-level completion tests ==========

    @Test
    public void testBarePrefixSingleMatch() throws Exception {
        // "st" should complete to "staging" (no dashes)
        List<String> candidates = complete("deploy st", 9);
        assertEquals(1, candidates.size());
        assertEquals("aging", candidates.get(0));
    }

    @Test
    public void testBarePrefixProduction() throws Exception {
        // "pr" should complete to "production"
        List<String> candidates = complete("deploy pr", 9);
        assertEquals(1, candidates.size());
        assertEquals("oduction", candidates.get(0));
    }

    @Test
    public void testBarePrefixForce() throws Exception {
        // "fo" should complete to "force"
        List<String> candidates = complete("deploy fo", 9);
        assertEquals(1, candidates.size());
        assertEquals("rce", candidates.get(0));
    }

    @Test
    public void testBarePrefixNoMatch() throws Exception {
        // "xyz" should not match any bare option
        List<String> candidates = complete("deploy xyz", 10);
        // No bare option matches — could fall through to arguments or return empty
        // The key assertion is that it doesn't crash and doesn't return "staging" etc.
        for (String c : candidates) {
            assertFalse("Should not contain staging", c.contains("staging"));
            assertFalse("Should not contain production", c.contains("production"));
        }
    }

    @Test
    public void testDashedPrefixCompletesToDashed() throws Exception {
        // "--st" should complete to "--staging" (with dashes, matching user's style)
        List<String> candidates = complete("deploy --st", 11);
        assertEquals(1, candidates.size());
        assertEquals("aging", candidates.get(0));
    }

    @Test
    public void testDashedPrefixProduction() throws Exception {
        // "--pr" should complete to "--production"
        List<String> candidates = complete("deploy --pr", 11);
        assertEquals(1, candidates.size());
        assertEquals("oduction", candidates.get(0));
    }

    @Test
    public void testDashedPrefixForce() throws Exception {
        // "--fo" should complete to "--force"
        List<String> candidates = complete("deploy --fo", 11);
        assertEquals(1, candidates.size());
        assertEquals("rce", candidates.get(0));
    }

    @Test
    public void testDoubleDashShowsAll() throws Exception {
        // "--" should show all options (both bare rendered as -- and normal --)
        List<String> candidates = complete("deploy --", 9);
        assertTrue("Should have multiple candidates", candidates.size() > 1);
        // Should include dashed forms
        boolean hasStaging = false;
        boolean hasEnv = false;
        boolean hasHelp = false;
        for (String c : candidates) {
            if (c.contains("staging"))
                hasStaging = true;
            if (c.contains("env"))
                hasEnv = true;
            if (c.contains("help"))
                hasHelp = true;
        }
        assertTrue("Should have staging option", hasStaging);
        assertTrue("Should have env option", hasEnv);
        assertTrue("Should have help option", hasHelp);
    }

    @Test
    public void testEmptyShowsMixedBareAndDashed() throws Exception {
        // After "deploy " with <tab>, all options listed
        List<String> candidates = complete("deploy ", 7);
        assertTrue("Should have multiple candidates", candidates.size() > 1);
        boolean hasStagingBare = false;
        boolean hasEnvDashed = false;
        for (String c : candidates) {
            if (c.equals("staging"))
                hasStagingBare = true;
            if (c.equals("--env"))
                hasEnvDashed = true;
        }
        assertTrue("Should have 'staging' (bare, no dashes)", hasStagingBare);
        assertTrue("Should have '--env' (dashed)", hasEnvDashed);
    }

    @Test
    public void testUsedBareOptionExcluded() throws Exception {
        // After "staging ", the "staging" option should not appear again
        List<String> candidates = complete("deploy staging ", 15);
        for (String c : candidates) {
            assertFalse("staging should not appear again", c.equals("staging"));
        }
    }

    @Test
    public void testDashedNormalOptionUnaffected() throws Exception {
        // "--en" should complete to "--env"
        List<String> candidates = complete("deploy --en", 11);
        assertEquals(1, candidates.size());
        assertEquals("v", candidates.get(0));
    }

    @Test
    public void testAfterMultipleBareOptions() throws Exception {
        // After "staging force ", remaining options should be listed
        List<String> candidates = complete("deploy staging force ", 21);
        for (String c : candidates) {
            assertFalse("staging should not appear", c.equals("staging"));
            assertFalse("force should not appear", c.equals("force"));
        }
        // production and --env should still be available
        boolean hasProduction = false;
        boolean hasEnv = false;
        for (String c : candidates) {
            if (c.equals("production"))
                hasProduction = true;
            if (c.equals("--env"))
                hasEnv = true;
        }
        assertTrue("Should have 'production'", hasProduction);
        assertTrue("Should have '--env'", hasEnv);
    }

    // ========== Interactive TestConnection tests ==========

    @Test
    public void testInteractiveBarePrefixCompletion() throws Exception {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = setupConsole(connection);

        connection.clearOutputBuffer();
        connection.read("deploy st");
        connection.read(completeChar.getFirstValue());
        assertEquals("deploy staging ", connection.getOutputBuffer());

        console.stop();
    }

    @Test
    public void testInteractiveDashedPrefixCompletion() throws Exception {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = setupConsole(connection);

        connection.clearOutputBuffer();
        connection.read("deploy --st");
        connection.read(completeChar.getFirstValue());
        assertEquals("deploy --staging ", connection.getOutputBuffer());

        console.stop();
    }

    @Test
    public void testInteractiveBareForceCompletion() throws Exception {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = setupConsole(connection);

        connection.clearOutputBuffer();
        connection.read("deploy fo");
        connection.read(completeChar.getFirstValue());
        assertEquals("deploy force ", connection.getOutputBuffer());

        console.stop();
    }

    @Test
    public void testInteractiveMixedCompletion() throws Exception {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = setupConsole(connection);

        // First complete bare option, then dashed option
        connection.clearOutputBuffer();
        connection.read("deploy staging --en");
        connection.read(completeChar.getFirstValue());
        assertEquals("deploy staging --env ", connection.getOutputBuffer());

        console.stop();
    }

    @Test
    public void testInteractiveAfterBareOptionSet() throws Exception {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = setupConsole(connection);

        // After setting staging, tab should not re-show staging
        connection.clearOutputBuffer();
        connection.read("deploy staging ");
        connection.read(completeChar.getFirstValue());
        String output = connection.getOutputBuffer();
        // Output should list remaining options but not "staging" again
        assertFalse("staging should not appear again after being set",
                output.contains(Config.getLineSeparator() + "staging "));

        console.stop();
    }

    // ========== Helpers ==========

    private List<String> complete(String input, int cursor) throws Exception {
        CommandLineParser<CommandInvocation> clp = new AeshCommandContainerBuilder<>()
                .create(new DeployCmd()).getParser();
        InvocationProviders ip = SettingsBuilder.builder().build().invocationProviders();
        AeshCompleteOperation co = new AeshCompleteOperation(aeshContext, input, cursor);
        clp.complete(co, ip);
        return co.getFormattedCompletionCandidates();
    }

    private ReadlineConsole setupConsole(TestConnection connection)
            throws IOException, CommandRegistryException {
        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(DeployCmd.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder
                .builder()
                .logging(true)
                .enableAlias(false)
                .connection(connection)
                .commandRegistry(registry)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(""));
        console.start();
        return console;
    }

    // ========== Test command ==========

    @CommandDefinition(name = "deploy", description = "Deploy command", generateHelp = true)
    public static class DeployCmd implements Command<CommandInvocation> {
        @Option(name = "staging", acceptNameWithoutDashes = true, hasValue = false, description = "Deploy to staging")
        boolean staging;

        @Option(name = "production", acceptNameWithoutDashes = true, hasValue = false, description = "Deploy to production")
        boolean production;

        @Option(name = "force", acceptNameWithoutDashes = true, hasValue = false, description = "Force deploy")
        boolean force;

        @Option(name = "replicas", acceptNameWithoutDashes = true, description = "Number of replicas", defaultValue = "1")
        int replicas;

        @Option(name = "env", description = "Environment (normal dashed option)")
        String env;

        @Argument(description = "Artifact")
        String artifact;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }
}
