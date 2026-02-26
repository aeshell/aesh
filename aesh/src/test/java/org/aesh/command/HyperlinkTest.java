package org.aesh.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.console.ReadlineConsole;
import org.aesh.converter.CLConverterManager;
import org.aesh.terminal.utils.ANSI;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

public class HyperlinkTest {

    @Test
    public void testURLConverter() throws Exception {
        assertTrue(CLConverterManager.getInstance().hasConverter(URL.class));
        assertNotNull(CLConverterManager.getInstance().getConverter(URL.class));
    }

    @Test
    public void testURIConverter() throws Exception {
        assertTrue(CLConverterManager.getInstance().hasConverter(URI.class));
        assertNotNull(CLConverterManager.getInstance().getConverter(URI.class));
    }

    @Test
    public void testURLOptionAutoDetection() throws Exception {
        ProcessedOption urlOption = ProcessedOptionBuilder.builder()
                .name("endpoint")
                .type(URL.class)
                .fieldName("endpoint")
                .build();

        assertTrue("URL-typed option should have isUrl=true", urlOption.isUrl());
    }

    @Test
    public void testURIOptionAutoDetection() throws Exception {
        ProcessedOption uriOption = ProcessedOptionBuilder.builder()
                .name("docLink")
                .type(URI.class)
                .fieldName("docLink")
                .build();

        assertTrue("URI-typed option should have isUrl=true", uriOption.isUrl());
    }

    @Test
    public void testExplicitUrlAttribute() throws Exception {
        ProcessedOption opt = ProcessedOptionBuilder.builder()
                .name("homepage")
                .type(String.class)
                .fieldName("homepage")
                .url(true)
                .build();

        assertTrue("Explicit url=true should set isUrl", opt.isUrl());
    }

    @Test
    public void testStringOptionNotUrl() throws Exception {
        ProcessedOption opt = ProcessedOptionBuilder.builder()
                .name("name")
                .type(String.class)
                .fieldName("name")
                .build();

        assertFalse("String-typed option should not be URL by default", opt.isUrl());
    }

    @Test
    public void testGetFormattedValueWithHyperlinks() throws Exception {
        ProcessedOption opt = ProcessedOptionBuilder.builder()
                .name("endpoint")
                .type(String.class)
                .fieldName("endpoint")
                .url(true)
                .build();

        opt.addValue("https://example.com");

        String formatted = opt.getFormattedValue(true);
        assertTrue("Formatted value should contain hyperlink start",
                formatted.contains(ANSI.buildHyperlinkStart("https://example.com", null)));
        assertTrue("Formatted value should contain hyperlink end",
                formatted.contains(ANSI.buildHyperlinkEnd()));

        String plain = opt.getFormattedValue(false);
        assertEquals("Without hyperlinks, should return plain value",
                "https://example.com", plain);
    }

    @Test
    public void testDescriptionUrl() throws Exception {
        ProcessedOption opt = ProcessedOptionBuilder.builder()
                .name("env")
                .shortName('e')
                .type(String.class)
                .fieldName("environment")
                .description("Target environment")
                .descriptionUrl("https://docs.example.com/environments")
                .build();

        assertEquals("https://docs.example.com/environments", opt.getDescriptionUrl());

        // With hyperlinks supported, description should be wrapped
        String formatted = opt.getFormattedOption(2, 30, 80, true);
        assertTrue("Formatted option with hyperlink support should contain hyperlink",
                formatted.contains(ANSI.buildHyperlinkStart("https://docs.example.com/environments", null)));

        // Without hyperlinks, should be plain text
        String plain = opt.getFormattedOption(2, 30, 80, false);
        assertTrue("Plain formatted option should contain description",
                plain.contains("Target environment"));
        assertFalse("Plain formatted option should not contain hyperlink codes",
                plain.contains(ANSI.buildHyperlinkStart("https://docs.example.com/environments", null)));
    }

    @Test
    public void testHelpUrlInPrintHelp() throws Exception {
        ProcessedCommand<?, ?> cmd = ProcessedCommandBuilder.builder()
                .name("deploy")
                .description("Deploy an application")
                .helpUrl("https://docs.example.com/deploy")
                .create();

        // Without hyperlinks
        String helpPlain = cmd.printHelp("deploy", false);
        assertTrue("Help should contain Documentation line",
                helpPlain.contains("Documentation: https://docs.example.com/deploy"));

        // With hyperlinks
        String helpHyperlink = cmd.printHelp("deploy", true);
        assertTrue("Help with hyperlinks should contain hyperlink codes",
                helpHyperlink.contains(ANSI.hyperlink("https://docs.example.com/deploy", "https://docs.example.com/deploy")));
    }

    @Test
    public void testHelpUrlOmittedWhenEmpty() throws Exception {
        ProcessedCommand<?, ?> cmd = ProcessedCommandBuilder.builder()
                .name("test")
                .description("A test command")
                .create();

        String help = cmd.printHelp("test");
        assertFalse("Help should not contain Documentation line when helpUrl is empty",
                help.contains("Documentation:"));
    }

    @Test
    public void testCommandDefinitionHelpUrl() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(HelpUrlCommand.class)
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation, OptionActivator, CommandActivator> settings = SettingsBuilder
                .builder()
                .commandRegistry(registry)
                .enableOperatorParser(true)
                .connection(connection)
                .setPersistExport(false)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();
        connection.read("helpurl -h" + Config.getLineSeparator());
        Thread.sleep(100);

        String output = connection.getOutputBuffer();
        assertTrue("Help output should contain documentation URL",
                output.contains("Documentation: https://docs.example.com/helpurl"));

        console.stop();
    }

    @Test
    public void testCommandWithUrlOption() throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(UrlOptionCommand.class)
                .create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry)
                .build();

        runtime.executeCommand("urlopt --endpoint https://api.example.com");
    }

    @Test
    public void testCommandWithDescriptionUrl() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(DescUrlCommand.class)
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation, OptionActivator, CommandActivator> settings = SettingsBuilder
                .builder()
                .commandRegistry(registry)
                .enableOperatorParser(true)
                .connection(connection)
                .setPersistExport(false)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();
        connection.read("descurl -h" + Config.getLineSeparator());
        Thread.sleep(100);

        String output = connection.getOutputBuffer();
        assertTrue("Help output should contain the option description",
                output.contains("Target environment"));

        console.stop();
    }

    @Test
    public void testPrintHyperlinkFromCommand() throws Exception {
        // Use TestConnection without stripping ANSI codes to verify hyperlink output
        TestConnection connection = new TestConnection(false);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(HyperlinkOutputCommand.class)
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation, OptionActivator, CommandActivator> settings = SettingsBuilder
                .builder()
                .commandRegistry(registry)
                .enableOperatorParser(true)
                .connection(connection)
                .setPersistExport(false)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();
        connection.read("hyperlinkout" + Config.getLineSeparator());
        Thread.sleep(100);

        // The command calls printHyperlink. Since TestConnection's device
        // is vt100 which doesn't support hyperlinks, it should fall back to plain text.
        String output = connection.getOutputBuffer();
        assertTrue("Output should contain the link text", output.contains("Click here"));

        console.stop();
    }

    // ========== Test Command Classes ==========

    @CommandDefinition(name = "helpurl", generateHelp = true, description = "A command with help URL", helpUrl = "https://docs.example.com/helpurl")
    public static class HelpUrlCommand implements Command<CommandInvocation> {
        @Option(description = "a value")
        private String value;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "urlopt", description = "Command with URL option")
    public static class UrlOptionCommand implements Command<CommandInvocation> {
        @Option(description = "API endpoint", url = true)
        private String endpoint;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "descurl", generateHelp = true, description = "Command with description URL")
    public static class DescUrlCommand implements Command<CommandInvocation> {
        @Option(shortName = 'e', description = "Target environment", descriptionUrl = "https://docs.example.com/environments")
        private String environment;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "hyperlinkout", description = "Command that outputs hyperlinks")
    public static class HyperlinkOutputCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.printHyperlink("https://example.com", "Click here");
            return CommandResult.SUCCESS;
        }
    }
}
