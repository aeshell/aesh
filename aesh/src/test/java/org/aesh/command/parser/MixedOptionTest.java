package org.aesh.command.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.parser.AeshCommandLineParser;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.Option;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.console.AeshContext;
import org.aesh.console.DefaultAeshContext;
import org.junit.Test;

/**
 * Tests for mixing annotation-defined options (@Option) with programmatically-
 * built options (ProcessedOptionBuilder) in the same command (#572 follow-up).
 * <p>
 * Verifies that parsing, completion, help, and value population work correctly
 * when options are added dynamically after annotation-based construction.
 */
public class MixedOptionTest {

    private final AeshContext aeshContext = new DefaultAeshContext();

    // ========== Parsing ==========

    @Test
    public void testParsingWithMixedOptions() throws Exception {
        CommandLineParser<CommandInvocation> parser = createParserWithDynamicOption();

        parser.populateObject("mixed --name Alice --dynamic hello",
                invocationProviders(), aeshContext, CommandLineParser.Mode.VALIDATE);

        MixedCmd<?> cmd = (MixedCmd<?>) parser.getCommand();
        assertEquals("Alice", cmd.name);

        // Dynamic option should be parsed — check via ProcessedOption
        ProcessedOption dynOpt = parser.getProcessedCommand().findLongOptionNoActivatorCheck("dynamic");
        assertNotNull("Dynamic option should be found", dynOpt);
        assertEquals("hello", dynOpt.getValue());
    }

    @Test
    public void testParsingAnnotationOptionWithoutDynamic() throws Exception {
        CommandLineParser<CommandInvocation> parser = createParserWithDynamicOption();

        parser.populateObject("mixed --name Bob",
                invocationProviders(), aeshContext, CommandLineParser.Mode.VALIDATE);

        MixedCmd<?> cmd = (MixedCmd<?>) parser.getCommand();
        assertEquals("Bob", cmd.name);

        // Dynamic option not specified — should have no value
        ProcessedOption dynOpt = parser.getProcessedCommand().findLongOptionNoActivatorCheck("dynamic");
        assertNotNull(dynOpt);
        assertEquals(null, dynOpt.getValue());
    }

    @Test
    public void testParsingDynamicOptionWithShortName() throws Exception {
        CommandLineParser<CommandInvocation> parser = createParserWithDynamicOption();

        parser.populateObject("mixed --name Alice -d world",
                invocationProviders(), aeshContext, CommandLineParser.Mode.VALIDATE);

        MixedCmd<?> cmd = (MixedCmd<?>) parser.getCommand();
        assertEquals("Alice", cmd.name);

        ProcessedOption dynOpt = parser.getProcessedCommand().findLongOptionNoActivatorCheck("dynamic");
        assertEquals("world", dynOpt.getValue());
    }

    // ========== Completion ==========

    @Test
    public void testCompletionIncludesBothAnnotationAndDynamicOptions() throws Exception {
        CommandLineParser<CommandInvocation> parser = createParserWithDynamicOption();
        InvocationProviders ip = invocationProviders();
        AeshCompleteOperation co = new AeshCompleteOperation(aeshContext, "mixed --", 8);

        parser.complete(co, ip);

        List<String> candidates = co.getFormattedCompletionCandidates();
        assertTrue("Should have multiple candidates, got: " + candidates,
                candidates.size() >= 2);

        // Check that both annotation-defined and dynamic options are present
        boolean hasName = false;
        boolean hasDynamic = false;
        for (String c : candidates) {
            if (c.contains("name"))
                hasName = true;
            if (c.contains("dynamic"))
                hasDynamic = true;
        }
        assertTrue("Should include --name (annotation-defined)", hasName);
        assertTrue("Should include --dynamic (programmatic)", hasDynamic);
    }

    @Test
    public void testCompletionPartialDynamicOption() throws Exception {
        CommandLineParser<CommandInvocation> parser = createParserWithDynamicOption();
        InvocationProviders ip = invocationProviders();
        AeshCompleteOperation co = new AeshCompleteOperation(aeshContext, "mixed --dyn", 11);

        parser.complete(co, ip);

        List<String> candidates = co.getFormattedCompletionCandidates();
        assertEquals("Should have 1 candidate for --dyn prefix", 1, candidates.size());
        assertTrue("Should complete to --dynamic", candidates.get(0).contains("amic"));
    }

    // ========== Help ==========

    @Test
    public void testHelpIncludesBothOptionTypes() throws Exception {
        CommandLineParser<CommandInvocation> parser = createParserWithDynamicOption();

        String help = parser.printHelp();
        assertTrue("Help should include --name", help.contains("--name"));
        assertTrue("Help should include --dynamic", help.contains("--dynamic"));
        assertTrue("Help should include dynamic description",
                help.contains("Dynamically added"));
    }

    // ========== Option count and ordering ==========

    @Test
    public void testOptionCountAfterDynamicAdd() throws Exception {
        CommandLineParser<CommandInvocation> parser = createParserWithDynamicOption();
        ProcessedCommand<?, ?> pc = parser.getProcessedCommand();

        // Should have: --name (annotation), --verbose (annotation), --dynamic (programmatic)
        // Plus --help if generateHelp=true
        int count = 0;
        boolean foundName = false;
        boolean foundVerbose = false;
        boolean foundDynamic = false;
        for (ProcessedOption opt : pc.getOptions()) {
            count++;
            if ("name".equals(opt.name()))
                foundName = true;
            if ("verbose".equals(opt.name()))
                foundVerbose = true;
            if ("dynamic".equals(opt.name()))
                foundDynamic = true;
        }
        assertTrue("Should have at least 3 options, got: " + count, count >= 3);
        assertTrue("Should have --name", foundName);
        assertTrue("Should have --verbose", foundVerbose);
        assertTrue("Should have --dynamic", foundDynamic);
    }

    // ========== Helpers ==========

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private CommandLineParser<CommandInvocation> createParserWithDynamicOption() throws Exception {
        AeshCommandLineParser parser = (AeshCommandLineParser) new AeshCommandContainerBuilder<>()
                .create(new MixedCmd<>()).getParser();

        // Add a dynamic option after annotation-based construction
        parser.getProcessedCommand().addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('d')
                        .name("dynamic")
                        .type(String.class)
                        .description("Dynamically added option")
                        .build());

        return parser;
    }

    private InvocationProviders invocationProviders() {
        return SettingsBuilder.builder().build().invocationProviders();
    }

    // ========== Test command ==========

    @CommandDefinition(name = "mixed", description = "Mixed options test", generateHelp = true)
    public static class MixedCmd<CI extends CommandInvocation> implements Command<CI> {
        @Option(name = "name", shortName = 'n', description = "Name option")
        String name;

        @Option(name = "verbose", shortName = 'v', hasValue = false, description = "Verbose mode")
        boolean verbose;

        @Override
        public CommandResult execute(CI ci) {
            return CommandResult.SUCCESS;
        }
    }
}
