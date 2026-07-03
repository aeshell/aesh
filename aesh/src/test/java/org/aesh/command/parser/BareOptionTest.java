package org.aesh.command.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.AeshCommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.terminal.formatting.TerminalString;
import org.junit.Test;

/**
 * Tests for options with acceptNameWithoutDashes=true.
 * Covers parsing, help rendering, and completion candidate generation (#549).
 */
public class BareOptionTest {

    // ========== Parsing: bare form ==========

    @Test
    public void testParseBareBoolean() throws Exception {
        AeshCommandLineParser<?> parser = createParser(BareCmd.class);
        parser.parse("test staging");
        assertEquals("true", getOptionValue(parser, "staging"));
    }

    @Test
    public void testParseBareBooleanMultiple() throws Exception {
        AeshCommandLineParser<?> parser = createParser(BareCmd.class);
        parser.parse("test staging force");
        assertEquals("true", getOptionValue(parser, "staging"));
        assertEquals("true", getOptionValue(parser, "force"));
    }

    @Test
    public void testParseBareWithValue() throws Exception {
        AeshCommandLineParser<?> parser = createParser(BareCmd.class);
        parser.parse("test replicas=3");
        assertEquals("3", getOptionValue(parser, "replicas"));
    }

    @Test
    public void testParseBareWithSpaceValue() throws Exception {
        AeshCommandLineParser<?> parser = createParser(BareCmd.class);
        parser.parse("test replicas 3");
        assertEquals("3", getOptionValue(parser, "replicas"));
    }

    // ========== Parsing: dashed form (still works) ==========

    @Test
    public void testParseDashedBoolean() throws Exception {
        AeshCommandLineParser<?> parser = createParser(BareCmd.class);
        parser.parse("test --staging");
        assertEquals("true", getOptionValue(parser, "staging"));
    }

    @Test
    public void testParseDashedWithValue() throws Exception {
        AeshCommandLineParser<?> parser = createParser(BareCmd.class);
        parser.parse("test --replicas=3");
        assertEquals("3", getOptionValue(parser, "replicas"));
    }

    @Test
    public void testParseDashedWithSpaceValue() throws Exception {
        AeshCommandLineParser<?> parser = createParser(BareCmd.class);
        parser.parse("test --replicas 3");
        assertEquals("3", getOptionValue(parser, "replicas"));
    }

    // ========== Parsing: mixed bare + dashed + positional ==========

    @Test
    public void testParseMixedBareAndDashed() throws Exception {
        AeshCommandLineParser<?> parser = createParser(BareCmd.class);
        parser.parse("test staging --env prod");
        assertEquals("true", getOptionValue(parser, "staging"));
        assertEquals("prod", getOptionValue(parser, "env"));
    }

    @Test
    public void testParseBareWithPositional() throws Exception {
        AeshCommandLineParser<?> parser = createParser(BareCmd.class);
        parser.parse("test staging myapp.jar");
        assertEquals("true", getOptionValue(parser, "staging"));
        ProcessedOption arg = parser.getProcessedCommand().getArgument();
        assertNotNull(arg);
        assertEquals("myapp.jar", arg.getValue());
    }

    // ========== Help rendering: bare options without dashes ==========

    @Test
    public void testHelpShowsBareWithoutDashes() throws Exception {
        AeshCommandLineParser<?> parser = createParser(BareCmd.class);
        String help = parser.printHelp();
        // Bare options should appear without -- in help
        assertTrue("Help should show 'staging' without dashes", help.contains("staging"));
        assertTrue("Help should show 'force' without dashes", help.contains("force"));
        assertTrue("Help should show 'replicas' without dashes", help.contains("replicas"));
        // Normal dashed option should still have --
        assertTrue("Help should show '--env' with dashes", help.contains("--env"));
    }

    @Test
    public void testSynopsisShowsBareWithoutDashes() throws Exception {
        AeshCommandLineParser<?> parser = createParser(BareCmd.class);
        String synopsis = parser.getProcessedCommand().buildSynopsisString(false, false);
        // Bare options in synopsis should NOT have -- prefix
        assertFalse("Synopsis should not have '--staging'", synopsis.contains("--staging"));
        assertFalse("Synopsis should not have '--force'", synopsis.contains("--force"));
        assertTrue("Synopsis should have 'staging' (bare)", synopsis.contains("staging"));
        assertTrue("Synopsis should have 'force' (bare)", synopsis.contains("force"));
        // Normal option should still have --
        assertTrue("Synopsis should have '--env'", synopsis.contains("--env"));
    }

    // ========== Completion: bare option listing ==========

    @Test
    public void testGetOptionNamesShowsBareWithoutDashes() throws Exception {
        AeshCommandLineParser<?> parser = createParser(BareCmd.class);
        List<TerminalString> names = parser.getProcessedCommand().getOptionLongNamesWithDash();
        boolean hasBareName = false;
        boolean hasDashedBareName = false;
        boolean hasDashedEnv = false;
        for (TerminalString ts : names) {
            if (ts.getCharacters().equals("staging"))
                hasBareName = true;
            if (ts.getCharacters().equals("--staging"))
                hasDashedBareName = true;
            if (ts.getCharacters().equals("--env"))
                hasDashedEnv = true;
        }
        assertTrue("Should list 'staging' (bare, no dashes)", hasBareName);
        assertFalse("Should NOT list '--staging'", hasDashedBareName);
        assertTrue("Should list '--env' (normal dashed)", hasDashedEnv);
    }

    @Test
    public void testFindPossibleBareLongNames() throws Exception {
        AeshCommandLineParser<?> parser = createParser(BareCmd.class);
        List<TerminalString> matches = parser.getProcessedCommand().findPossibleBareLongNames("st");
        assertEquals("Should find 1 match for 'st'", 1, matches.size());
        assertEquals("Match should be 'staging' (no dashes)", "staging", matches.get(0).getCharacters());
    }

    @Test
    public void testFindPossibleBareLongNamesWithDash() throws Exception {
        AeshCommandLineParser<?> parser = createParser(BareCmd.class);
        List<TerminalString> matches = parser.getProcessedCommand().findPossibleBareLongNamesWithDash("st");
        assertEquals("Should find 1 match for 'st'", 1, matches.size());
        assertEquals("Match should be '--staging' (with dashes)", "--staging", matches.get(0).getCharacters());
    }

    @Test
    public void testFindPossibleBareLongNamesNoMatch() throws Exception {
        AeshCommandLineParser<?> parser = createParser(BareCmd.class);
        List<TerminalString> matches = parser.getProcessedCommand().findPossibleBareLongNames("xyz");
        assertTrue("Should find no matches for 'xyz'", matches.isEmpty());
    }

    @Test
    public void testFindPossibleBareLongNamesMultipleMatches() throws Exception {
        AeshCommandLineParser<?> parser = createParser(BareCmd.class);
        // Both 'force' and no other option start with 'f'
        List<TerminalString> matches = parser.getProcessedCommand().findPossibleBareLongNames("f");
        assertEquals("Should find 1 match for 'f'", 1, matches.size());
        assertEquals("force", matches.get(0).getCharacters());
    }

    // ========== Helpers ==========

    @SuppressWarnings("rawtypes")
    private AeshCommandLineParser<?> createParser(Class<? extends Command> cmdClass) throws Exception {
        return (AeshCommandLineParser<?>) new AeshCommandContainerBuilder<>().create(cmdClass).getParser();
    }

    private String getOptionValue(AeshCommandLineParser<?> parser, String optionName) {
        ProcessedOption opt = parser.getProcessedCommand().findLongOptionNoActivatorCheck(optionName);
        return opt != null ? opt.getValue() : null;
    }

    // ========== Test commands ==========

    @CommandDefinition(name = "test", description = "Bare option test", generateHelp = true)
    public static class BareCmd implements Command<CommandInvocation> {
        @Option(name = "staging", acceptNameWithoutDashes = true, hasValue = false, description = "Deploy to staging")
        boolean staging;

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
