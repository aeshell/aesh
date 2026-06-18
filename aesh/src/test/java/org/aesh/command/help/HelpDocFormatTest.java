package org.aesh.command.help;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.DocFormat;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.AeshCommandLineParser;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.ReadlineConsole;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Tests for {@code --help=<format>} support (#529).
 * Verifies that {@code --help} can accept an optional format value
 * to produce skill, markdown, or asciidoc documentation instead of
 * terminal help.
 */
public class HelpDocFormatTest {

    // ---- Unit tests for ProcessedCommand.getHelpDocFormat() ----

    @Test
    public void testGetHelpDocFormatSkill() throws Exception {
        ProcessedCommand<?, ?> cmd = parseCommand("mycmd --help=skill");
        assertNotNull("getHelpDocFormat() should return SKILL", cmd.getHelpDocFormat());
        assertTrue(cmd.getHelpDocFormat() == DocFormat.SKILL);
        assertTrue(cmd.isGenerateHelpOptionSet());
        assertFalse("isFullHelpRequested should be false for skill", cmd.isFullHelpRequested());
    }

    @Test
    public void testGetHelpDocFormatMarkdown() throws Exception {
        ProcessedCommand<?, ?> cmd = parseCommand("mycmd --help=markdown");
        assertTrue(cmd.getHelpDocFormat() == DocFormat.MARKDOWN);
    }

    @Test
    public void testGetHelpDocFormatAsciidoc() throws Exception {
        ProcessedCommand<?, ?> cmd = parseCommand("mycmd --help=asciidoc");
        assertTrue(cmd.getHelpDocFormat() == DocFormat.ASCIIDOC);
    }

    @Test
    public void testGetHelpDocFormatShortAliasMd() throws Exception {
        ProcessedCommand<?, ?> cmd = parseCommand("mycmd --help=md");
        assertTrue("--help=md should resolve to MARKDOWN", cmd.getHelpDocFormat() == DocFormat.MARKDOWN);
    }

    @Test
    public void testGetHelpDocFormatShortAliasAdoc() throws Exception {
        ProcessedCommand<?, ?> cmd = parseCommand("mycmd --help=adoc");
        assertTrue("--help=adoc should resolve to ASCIIDOC", cmd.getHelpDocFormat() == DocFormat.ASCIIDOC);
    }

    @Test
    public void testGetHelpDocFormatCaseInsensitive() throws Exception {
        ProcessedCommand<?, ?> cmd = parseCommand("mycmd --help=SKILL");
        assertTrue(cmd.getHelpDocFormat() == DocFormat.SKILL);

        cmd = parseCommand("mycmd --help=Markdown");
        assertTrue(cmd.getHelpDocFormat() == DocFormat.MARKDOWN);
    }

    @Test
    public void testGetHelpDocFormatBareHelp() throws Exception {
        ProcessedCommand<?, ?> cmd = parseCommand("mycmd --help");
        assertNull("bare --help should return null doc format", cmd.getHelpDocFormat());
        assertTrue(cmd.isGenerateHelpOptionSet());
    }

    @Test
    public void testGetHelpDocFormatAll() throws Exception {
        ProcessedCommand<?, ?> cmd = parseCommand("mycmd --help=all");
        assertNull("--help=all should return null (not a doc format)", cmd.getHelpDocFormat());
        assertTrue("--help=all should still trigger isFullHelpRequested", cmd.isFullHelpRequested());
    }

    @Test
    public void testGetHelpDocFormatFull() throws Exception {
        ProcessedCommand<?, ?> cmd = parseCommand("mycmd --help=full");
        assertNull("--help=full should return null (not a doc format)", cmd.getHelpDocFormat());
        assertTrue("--help=full should still trigger isFullHelpRequested", cmd.isFullHelpRequested());
    }

    @Test
    public void testGetHelpDocFormatUnknown() throws Exception {
        ProcessedCommand<?, ?> cmd = parseCommand("mycmd --help=json");
        assertNull("--help=json (unknown) should return null", cmd.getHelpDocFormat());
        assertTrue("--help=json should still be detected as help set", cmd.isGenerateHelpOptionSet());
    }

    // ---- Integration tests via ReadlineConsole ----

    @Test
    public void testHelpSkillFormatIntegration() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = buildConsole(connection, DocTestCommand.class);
        console.start();

        connection.read("mycmd --help=skill" + Config.getLineSeparator());
        Thread.sleep(200);
        String output = connection.getOutputBuffer();
        // Skill format starts with YAML front matter
        assertTrue("Skill format should contain YAML front matter delimiter",
                output.contains("---"));
        assertTrue("Skill format should contain command name",
                output.contains("mycmd"));

        console.stop();
    }

    @Test
    public void testHelpMarkdownFormatIntegration() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = buildConsole(connection, DocTestCommand.class);
        console.start();

        connection.read("mycmd --help=markdown" + Config.getLineSeparator());
        Thread.sleep(200);
        String output = connection.getOutputBuffer();
        // Markdown format uses # headings with uppercase command name
        assertTrue("Markdown format should contain # heading",
                output.contains("# MYCMD"));

        console.stop();
    }

    @Test
    public void testHelpAsciidocFormatIntegration() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = buildConsole(connection, DocTestCommand.class);
        console.start();

        connection.read("mycmd --help=asciidoc" + Config.getLineSeparator());
        Thread.sleep(200);
        String output = connection.getOutputBuffer();
        // AsciiDoc format uses = headings with uppercase command name
        assertTrue("AsciiDoc format should contain = heading",
                output.contains("= MYCMD"));

        console.stop();
    }

    @Test
    public void testHelpShortAliasMdIntegration() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = buildConsole(connection, DocTestCommand.class);
        console.start();

        connection.read("mycmd --help=md" + Config.getLineSeparator());
        Thread.sleep(200);
        String output = connection.getOutputBuffer();
        assertTrue("--help=md should produce markdown with # heading",
                output.contains("# MYCMD"));

        console.stop();
    }

    @Test
    public void testHelpShortAliasAdocIntegration() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = buildConsole(connection, DocTestCommand.class);
        console.start();

        connection.read("mycmd --help=adoc" + Config.getLineSeparator());
        Thread.sleep(200);
        String output = connection.getOutputBuffer();
        assertTrue("--help=adoc should produce asciidoc with = heading",
                output.contains("= MYCMD"));

        console.stop();
    }

    @Test
    public void testBareHelpUnchanged() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = buildConsole(connection, DocTestCommand.class);
        console.start();

        connection.read("mycmd --help" + Config.getLineSeparator());
        Thread.sleep(200);
        String output = connection.getOutputBuffer();
        // Terminal help uses "Usage:" prefix, not YAML front matter or # headings
        assertTrue("Bare --help should show terminal format with Usage:",
                output.contains("Usage:"));
        assertFalse("Bare --help should NOT contain YAML front matter at start",
                output.startsWith("---"));

        console.stop();
    }

    @Test
    public void testHelpAllStillWorks() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = buildConsole(connection, DocTestCommand.class);
        console.start();

        connection.read("mycmd --help=all" + Config.getLineSeparator());
        Thread.sleep(200);
        String output = connection.getOutputBuffer();
        // --help=all should still produce terminal format
        assertTrue("--help=all should show terminal format with Usage:",
                output.contains("Usage:"));

        console.stop();
    }

    @Test
    public void testHelpUnknownFormatFallsBack() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = buildConsole(connection, DocTestCommand.class);
        console.start();

        connection.read("mycmd --help=json" + Config.getLineSeparator());
        Thread.sleep(200);
        String output = connection.getOutputBuffer();
        // Unknown format should fall through to terminal help
        assertTrue("--help=json should fall back to terminal format with Usage:",
                output.contains("Usage:"));

        console.stop();
    }

    @Test
    public void testSubcommandHelpSkill() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(AppGroupCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder
                .builder()
                .commandRegistry(registry)
                .enableOperatorParser(true)
                .connection(connection)
                .setPersistExport(false)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("app run --help=skill" + Config.getLineSeparator());
        Thread.sleep(200);
        String output = connection.getOutputBuffer();
        // Should generate skill docs for 'run' subcommand, not 'app'
        assertTrue("Subcommand skill docs should contain YAML front matter",
                output.contains("---"));
        assertTrue("Subcommand skill docs should reference 'run' command",
                output.contains("run"));

        console.stop();
    }

    @Test
    public void testSubcommandBareHelp() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(AppGroupCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder
                .builder()
                .commandRegistry(registry)
                .enableOperatorParser(true)
                .connection(connection)
                .setPersistExport(false)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("app run --help" + Config.getLineSeparator());
        Thread.sleep(200);
        String output = connection.getOutputBuffer();
        // Bare --help for subcommand should show terminal format
        assertTrue("Subcommand bare --help should show terminal format",
                output.contains("Usage:"));

        console.stop();
    }

    @Test
    public void testHelpWithRequiredOption() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();
        ReadlineConsole console = buildConsole(connection, RequiredOptionCommand.class);
        console.start();

        // --help=skill should work even when required options are not provided
        connection.read("reqcmd --help=skill" + Config.getLineSeparator());
        Thread.sleep(200);
        String output = connection.getOutputBuffer();
        assertTrue("--help=skill should work with required options not set",
                output.contains("---"));
        assertTrue(output.contains("reqcmd"));

        console.stop();
    }

    // ---- Helper methods ----

    @SuppressWarnings("unchecked")
    private ProcessedCommand<?, ?> parseCommand(String line) throws Exception {
        org.aesh.command.container.CommandContainer container = new org.aesh.command.impl.container.AeshCommandContainerBuilder<>()
                .create(DocTestCommand.class);
        AeshCommandLineParser<?> parser = (AeshCommandLineParser<?>) container.getParser();
        parser.parse(line);
        return parser.getProcessedCommand();
    }

    @SuppressWarnings("unchecked")
    private ReadlineConsole buildConsole(TestConnection connection, Class<? extends Command> cmdClass)
            throws CommandRegistryException, IOException {
        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(cmdClass)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder
                .builder()
                .commandRegistry(registry)
                .enableOperatorParser(true)
                .connection(connection)
                .setPersistExport(false)
                .logging(true)
                .build();

        return new ReadlineConsole(settings);
    }

    // ---- Test commands ----

    @CommandDefinition(name = "mycmd", generateHelp = true, description = "A test command for doc format")
    public static class DocTestCommand implements Command<CommandInvocation> {

        @Option(description = "output file path")
        private String output;

        @Option(description = "enable verbose mode")
        private boolean verbose;

        @Argument(description = "input file")
        private String input;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "reqcmd", generateHelp = true, description = "Command with required option")
    public static class RequiredOptionCommand implements Command<CommandInvocation> {

        @Option(description = "required config", required = true)
        private String config;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "app", generateHelp = true, description = "Application group", groupCommands = {
            RunSubCommand.class })
    public static class AppGroupCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "run", generateHelp = true, description = "Run a task")
    public static class RunSubCommand implements Command<CommandInvocation> {

        @Option(description = "task name")
        private String task;

        @Option(description = "run in dry-run mode")
        private boolean dryRun;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }
}
